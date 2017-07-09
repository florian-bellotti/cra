package com.fbellotti.user;

import com.fbellotti.user.database.UserDatabaseService;
import com.fbellotti.user.database.UserDatabaseVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
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
public class UserVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {
    String userDbQueue = config().getString(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, "wikidb.queue");
    UserDatabaseService dbService = UserDatabaseService.createProxy(vertx, userDbQueue);

    // Defines routes
    Router routerApi = Router.router(vertx);
    Router router = Router.router(vertx);
    routerApi.route("/*").handler(BodyHandler.create());
    routerApi.get("/").handler(this::getAllUser);
    routerApi.get("/:id").handler(this::readUser);
    routerApi.post("/").handler(this::createUser);
    routerApi.put("/:id").handler(this::updateUser);
    routerApi.delete("/:id").handler(this::deleteUser);
    router.mountSubRouter("/users", routerApi);

    // Create the http server
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }

  private void getAllUser(RoutingContext rc) {
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "findAllUsers");

    vertx.eventBus().send(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, new JsonObject(), options, reply -> {
      if (reply.succeeded()) {
        rc.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(reply));
      } else {
        reply.cause().printStackTrace();
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
      }
    });
  }

  private void readUser(RoutingContext rc) {
    JsonObject request = new JsonObject()
      .put("id", rc.request().getParam("id"));
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "findUserById");

    vertx.eventBus().send(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, request, options, reply -> {
      if (reply.succeeded()) {
        rc.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(reply));
      } else {
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
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

    JsonObject request = new JsonObject()
      .put("user", rc.getBodyAsJson());
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "createUser");

    vertx.eventBus().send(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, request, options, reply -> {
      if (reply.succeeded()) {
        rc.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(reply.result()));
      } else {
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
      }
    });
  }


  private void updateUser(RoutingContext rc) {
    JsonObject request = new JsonObject()
      .put("id", rc.request().getParam("id"))
      .put("user", rc.getBodyAsJson());
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "updateUserById");

    vertx.eventBus().send(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, request, options, reply -> {
      if (reply.succeeded()) {
        rc.response()
          .setStatusCode(204)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      } else {
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
      }
    });
  }

  private void deleteUser(RoutingContext rc) {
    JsonObject request = new JsonObject().put("id", rc.request().getParam("id"));
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "deleteUserById");

    vertx.eventBus().send(UserDatabaseVerticle.CONFIG_USERDB_QUEUE, request, options, reply -> {
      if (reply.succeeded()) {
        rc.response()
          .setStatusCode(204)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      } else {
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
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
}
