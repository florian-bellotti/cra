package com.fbellotti.user;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fbellotti.user.model.Error;
import com.fbellotti.user.model.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class UserVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(UserVerticle.class);
  private MongoClient mongo;

  @Override
  public void start() {
    JsonObject config = Vertx.currentContext().config();
    String uri = config.getString("mongo_uri");
    if (uri == null) {
      uri = "mongodb://localhost:27017";
    }
    String db = config.getString("mongo_db");
    if (db == null) {
      db = "test";
    }

    System.out.println("uri : " + uri);
    System.out.println("uri : " + uri);
    System.out.println("db : " + db);
    JsonObject mongoconfig = new JsonObject()
      .put("connection_string", uri)
      .put("db_name", db);

    mongo = MongoClient.createShared(vertx, mongoconfig);

    // Defines routes
    Router router = Router.router(vertx);
    router.route("/*").handler(BodyHandler.create());
    router.get("/").handler(this::getAllUser);
    router.get("/:id").handler(this::readUser);
    router.post("/").handler(this::createUser);
    router.put("/:id").handler(this::updateUser);
    router.delete("/:id").handler(this::deleteUser);

    // Create the http server
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }

  private void getAllUser(RoutingContext rc) {
    // get all in database
    mongo.find("users", new JsonObject(), res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(res.result()));
      } else {
        res.cause().printStackTrace();
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
      }
    });
  }

  private void readUser(RoutingContext rc) {
    mongo.find("users", new JsonObject().put("_id", rc.request().getParam("id")), res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(res.result()));
      } else {
        res.cause().printStackTrace();
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

    mongo.save("users", rc.getBodyAsJson(), res -> {
      if (res.succeeded()) {
        user.setId(res.result());
        rc.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(user));
      } else {
        res.cause().printStackTrace();
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
      }
    });
  }


  private void updateUser(RoutingContext rc) {
    JsonObject query = new JsonObject().put("_id", rc.request().getParam("id"));

    mongo.updateCollection("users", query, new JsonObject().put("$set", rc.getBodyAsJson()), res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(204)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      } else {
        res.cause().printStackTrace();
        rc.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
      }
    });
  }

  private void deleteUser(RoutingContext rc) {
    mongo.removeDocument("users", new JsonObject().put("_id", rc.request().getParam("id")), res -> {
      if (res.succeeded()) {
        rc.response()
          .setStatusCode(204)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      } else {
        res.cause().printStackTrace();
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
