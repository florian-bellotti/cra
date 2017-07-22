package com.fbellotti.microservice.gateway;

import com.fbellotti.vertx.api.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * A verticle for global API gateway.
 * This API gateway uses HTTP-HTTP pattern. It's also responsible for
 * load balance and failure handling.
 *
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class APIGatewayVerticle extends RestAPIVerticle {

  private static final Logger logger = LoggerFactory.getLogger(APIGatewayVerticle.class);
  private static final int DEFAULT_PORT = 8787;

  private JWTAuth jwtAuth;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    // Get HTTP host and port from configuration, or use default value
    String host = config().getString("api.gateway.http.address", "localhost");
    int port = config().getInteger("api.gateway.http.port", DEFAULT_PORT);

    // Create JWTAuth instance
    jwtAuth = JWTAuth.create(vertx, new JsonObject()
      .put("keyStore", new JsonObject()
        .put("path", "keystore.jceks")
        .put("type", "jceks")
        .put("password", "secret")));

    Router router = Router.router(vertx);
    enableLocalSession(router);                       // Cookie and session handler
    router.route().handler(BodyHandler.create());     // Body handler
    router.get("/api/v").handler(this::apiVersion);   // Version handler
    router.route().handler(JWTAuthHandler.create(jwtAuth, "/login")); // Set auth callback handler
    router.post("/login").handler(this::loginHandler);
    router.post("/logout").handler(this::logoutHandler);
    router.route("/api/*").handler(this::dispatchRequests);             // Api dispatcher
    router.route("/*").handler(StaticHandler.create());                 // Static content

    // Enable HTTPS
    HttpServerOptions httpServerOptions = new HttpServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(new JksOptions().setPath("server.jks").setPassword("secret"));

    // Create http server
    vertx.createHttpServer(httpServerOptions)
      .requestHandler(router::accept)
      .listen(port, host, ar -> {
        if (ar.succeeded()) {
          publishApiGateway(host, port);
          future.complete();
          logger.info("API Gateway is running on port " + port);
          // publish log
          publishGatewayLog("api_gateway_init_success:" + port);
        } else {
          future.fail(ar.cause());
        }
      });
  }

  /**
   * This method get all endpoints to dispatch request
   * to the matching service.
   *
   * @param context Routing context instance
   */
  private void dispatchRequests(RoutingContext context) {
    int initialOffset = 5; // length of `/api/`
    // Run with circuit breaker in order to deal with failure
    circuitBreaker.execute(future -> {
      getAllEndpoints().setHandler(ar -> {
        if (ar.succeeded()) {
          List<Record> recordList = ar.result();
          // Get relative path and retrieve prefix to dispatch client
          String path = context.request().uri();
          if (path.length() <= initialOffset) {
            notFound(context);
            future.complete();
            return;
          }

          String prefix = (path.substring(initialOffset).split("/"))[0];
          String newPath = path.substring(initialOffset + prefix.length());

          // Get one relevant HTTP client, may not exist
          Optional<Record> client = recordList.stream()
            .filter(record -> record.getMetadata().getString("api.name") != null)
            .filter(record -> record.getMetadata().getString("api.name").equals(prefix))
            .findAny(); // simple load balance

          if (client.isPresent()) {
            doDispatch(context, newPath, discovery.getReference(client.get()).get(), future);
          } else {
            notFound(context);
            future.complete();
          }
        } else {
          future.fail(ar.cause());
        }
      });
    }).setHandler(ar -> {
      if (ar.failed()) {
        badGateway(ar.cause(), context);
      }
    });
  }

  /**
   * Dispatch the request to the downstream REST layers.
   *
   * @param context Routing context instance
   * @param path    Relative path
   * @param client  Relevant HTTP client
   */
  private void doDispatch(RoutingContext context, String path, HttpClient client, Future<Object> cbFuture) {
    HttpClientRequest toReq = client
      .request(context.request().method(), path, response -> {
        response.bodyHandler(body -> {
          if (response.statusCode() >= 500) { // api endpoint server error, circuit breaker should fail
            cbFuture.fail(response.statusCode() + ": " + body.toString());
          } else {
            HttpServerResponse toRsp = context.response()
              .setStatusCode(response.statusCode());
            response.headers().forEach(header -> {
              toRsp.putHeader(header.getKey(), header.getValue());
            });
            // send response
            toRsp.end(body);
            cbFuture.complete();
          }
          ServiceDiscovery.releaseServiceObject(discovery, client);
        });
      });
    // set headers
    context.request().headers().forEach(header -> {
      toReq.putHeader(header.getKey(), header.getValue());
    });
    if (context.user() != null) {
      toReq.putHeader("user-principal", context.user().principal().encode());
    }
    // send request
    if (context.getBody() == null) {
      toReq.end();
    } else {
      toReq.end(context.getBody());
    }
  }

  /**
   * Return the current API version (which is v1)
   *
   * @param context Routing context instance
   */
  private void apiVersion(RoutingContext context) {
    context.response()
      .end(new JsonObject().put("version", "v1").encodePrettily());
  }

  /**
   * Get all REST endpoints from the service discovery infrastructure.
   *
   * @return Async result
   */
  private Future<List<Record>> getAllEndpoints() {
    Future<List<Record>> future = Future.future();
    discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
      future.completer());
    return future;
  }

  /**
   * Authenticate the user. If username and password are valid, it
   * generate a JWT token.
   *
   * @param context Routing context instance
   */
  private void loginHandler(RoutingContext context) {
    JsonObject creds = new JsonObject()
      .put("username", context.getBodyAsJson().getString("username"))
      .put("password", context.getBodyAsJson().getString("password"));

    // TODO : Authentication
    context.response().setStatusCode(500).end();
  }

  /**
   * Clear user and destroy session
   *
   * @param context Routing context instance
   */
  private void logoutHandler(RoutingContext context) {
    context.clearUser();
    context.session().destroy();
    context.response().setStatusCode(204).end();
  }
}
