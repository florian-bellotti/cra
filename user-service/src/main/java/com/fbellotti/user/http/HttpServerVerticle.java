package com.fbellotti.user.http;

import com.fbellotti.user.database.UserDatabaseService;
import com.fbellotti.user.database.UserDatabaseVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.handler.BodyHandler;

import com.fbellotti.user.model.Error;
import com.fbellotti.user.model.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  private UserDatabaseService dbService;
  private JWTAuth jwtAuth;

  @Override
  public void start(Future<Void> startFuture) {
    String userDbQueue = config().getString(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, "userdb.queue");
    dbService = UserDatabaseService.createProxy(vertx, userDbQueue);

    HttpServerOptions httpServerOptions = new HttpServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(new JksOptions()
        .setPath("server-keystore.jks")
        .setPassword("secret"));

    // Secure api
    jwtAuth = JWTAuth.create(vertx, new JsonObject()
      .put("keyStore", new JsonObject()
        .put("path", "keystore.jceks")
        .put("type", "jceks")
        .put("password", "secret")));

    // Defines routes
    Router routerApi = Router.router(vertx);
    Router router = Router.router(vertx);
    routerApi.route().handler(JWTAuthHandler.create(jwtAuth, "/users/openSession"));
    routerApi.get("/").handler(this::getAllUser);
    routerApi.get("/:id").handler(this::readUser);
    routerApi.post().handler(BodyHandler.create());
    routerApi.post("/").handler(this::createUser);
    routerApi.post("/openSession").handler(this::openSession);
    routerApi.put().handler(BodyHandler.create());
    routerApi.put("/:id").handler(this::updateUser);
    routerApi.delete("/:id").handler(this::deleteUser);
    router.mountSubRouter("/users", routerApi);

    // Create the http server
    vertx.createHttpServer(httpServerOptions)
      .requestHandler(router::accept)
      .listen(8080, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + 8080);
          startFuture.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          startFuture.fail(ar.cause());
        }
      });
  }

  private void getAllUser(RoutingContext rc) {
    dbService.findAllUsers(res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(res.result()));
      } else {
        LOGGER.error("Failed to find all users", res.cause());
        executionError(rc, res);
      }
    });
  }

  private void readUser(RoutingContext rc) {
    dbService.findUserById(rc.request().getParam("id"), res -> {
      if (res.succeeded()) {
        if (res.result() == null || res.result().getString("_id") == null) {
          rc.response()
            .setStatusCode(404)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(new Error("NOT_FOUND", "User not found")));
        } else {
          rc.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(res.result()));
        }
      } else {
        LOGGER.error("Failed to find all users", res.cause());
        executionError(rc, res);
      }
    });
  }

  private void createUser(RoutingContext rc) {
    if (rc.user().principal().getBoolean("canCreate", false)) {
      User user = Json.decodeValue(rc.getBodyAsString(), User.class);

      // Check the received user
      Error error = checkUser(user);
      if (error != null) {
        rc.response()
          .setStatusCode(400)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(error));
      }

      dbService.createUser(rc.getBodyAsJson(), res -> {
        if (res.succeeded()) {
          rc.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(res.result()));
        } else {
          LOGGER.error("Failed to create user " + user.getUsername(), res.cause());
          executionError(rc, res);
        }
      });
    } else {
      rc.response()
        .setStatusCode(401)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end();
    }
  }

  private void openSession(RoutingContext rc) {
    User user = Json.decodeValue(rc.getBodyAsString(), User.class);

    // Check the received user
    Error error = checkUser(user);
    if (error != null) {
      rc.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(error));
    }

    JsonObject creds = new JsonObject()
      .put("username", user.getUsername())
      .put("password", user.getPassword());


    dbService.authenticate(creds, res -> {
      if (res.succeeded()) {
        io.vertx.ext.auth.User use;
        String token = jwtAuth.generateToken(
        new JsonObject()
          .put("username", user.getUsername())
          .put("canCreate", true)
          .put("canDelete", true)
          .put("canUpdate", true),
          new JWTOptions()
            .setSubject("Wiki API")
            .setIssuer("Vert.x"));
        rc.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(new JsonObject().put("token", token).encodePrettily());
      } else {
        LOGGER.error("Failed to authenticate user " + user.getUsername(), res.cause());
        executionError(rc, res);
      }
    });
  }

  private void updateUser(RoutingContext rc) {
    String id = rc.request().getParam("id");

    dbService.updateUserById(id, rc.getBodyAsJson(), res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(204)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      } else {
        LOGGER.error("Failed to update user " + id, res.cause());
        executionError(rc, res);
      }
    });
  }

  private void deleteUser(RoutingContext rc) {
    String id = rc.request().getParam("id");

    dbService.deleteUserById(id, res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(204)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      } else {
        LOGGER.error("Failed to delete user " + id, res.cause());
        executionError(rc, res);
      }
    });
  }

  private Error checkUser(User user) {
    if (user == null) {
      return new Error("NULL_USER", "User is null");
    }

    if (user.getUsername() == null || "".equals(user.getUsername())) {
      return new Error("NULL_USERNAME", "The username is null or empty");
    }

    if (user.getPassword() == null || "".equals(user.getPassword())) {
      return new Error("NULL_PASSWORD", "The password is null or empty");
    }

    return null;
  }

  private void executionError(RoutingContext rc, AsyncResult res) {
    rc.response()
      .setStatusCode(500)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(new Error("EXECUTION_ERROR", res.cause().getMessage())));
  }
}
