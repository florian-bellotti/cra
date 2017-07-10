package com.fbellotti.user.http;

import com.fbellotti.user.database.UserDatabaseService;
import com.fbellotti.user.database.UserDatabaseVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.BodyHandler;

import com.fbellotti.user.model.Error;
import com.fbellotti.user.model.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class HttpServerVerticle extends AbstractVerticle {


  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  private UserDatabaseService dbService;

  @Override
  public void start(Future<Void> startFuture) {
    String userDbQueue = config().getString(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, "userdb.queue");
    dbService = UserDatabaseService.createProxy(vertx, userDbQueue);

    // Defines routes
    Router routerApi = Router.router(vertx);
    Router router = Router.router(vertx);
    routerApi.get("/").handler(this::getAllUser);
    routerApi.get("/:id").handler(this::readUser);
    routerApi.post().handler(BodyHandler.create());
    routerApi.post("/").handler(this::createUser);
    routerApi.put().handler(BodyHandler.create());
    routerApi.put("/:id").handler(this::updateUser);
    routerApi.delete("/:id").handler(this::deleteUser);
    router.mountSubRouter("/users", routerApi);

    // Create the http server
    vertx.createHttpServer()
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
        executionError(rc);
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
        executionError(rc);
      }
    });
  }

  private void createUser(RoutingContext rc) {
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
        executionError(rc);
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
        executionError(rc);
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
        executionError(rc);
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

  private void executionError(RoutingContext rc) {
    rc.response()
      .setStatusCode(500)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
  }
}
