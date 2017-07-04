package com.fbellotti.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOG = LoggerFactory.getLogger(UserVerticle.class);

  @Override
  public void start() {
    // Defines routes
    Router router = Router.router(vertx);
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


    rc.response()
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(products.values()));
  }

  private void readUser(RoutingContext rc) {

  }

  private void createUser(RoutingContext rc) {
    final User user = Json.decodeValue(rc.getBodyAsString(), User.class);

    // Check the received user
    Error error = checkUser(user);
    if (error != null) {
      rc.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(error));
    }

    try {
      // Insert the user in database


    } catch (Exception e) {
      LOG.error("Failed to create user " + user.getUserName() + e);
      // Return an error
      rc.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(new Error("EXECUTION_ERROR", "Failed during the request execution")));
    }

    // Return the added user
    rc.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(user));
  }


  private void updateUser(RoutingContext rc) {

  }

  private void deleteUser(RoutingContext rc) {

  }

  private Error checkUser(User user) {
    if (user == null) {
      return new Error("NULL_USER", "User is null");
    }

    if (user.getUserName() == null || "".equals(user.getUserName())) {
      return new Error("NULL_USERNAME", "The username is null or empty");
    }

    if (user.getPassword() == null || "".equals(user.getPassword())) {
      return new Error("NULL_PASSWORD", "The password is null or empty");
    }

    return null;
  }
}
