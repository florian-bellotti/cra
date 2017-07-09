package com.fbellotti.user.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class UserDatabaseServiceImpl implements UserDatabaseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserDatabaseServiceImpl.class);

  private final MongoClient mongo;

  public UserDatabaseServiceImpl(MongoClient mongo, Handler<AsyncResult<UserDatabaseService>> readyHandler) {
    this.mongo = mongo;
    readyHandler.handle(Future.succeededFuture(this));
  }

  @Override
  public UserDatabaseService findAllUsers(Handler<AsyncResult<JsonArray>> resultHandler) {
    mongo.find("users", new JsonObject(), res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture(new JsonArray(res.result())));
      } else {
        LOGGER.error("Failed to find all users", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public UserDatabaseService createUser(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongo.save("users", user, res -> {
      if (res.succeeded()) {
        user.put("_id", res.result());
        resultHandler.handle(Future.succeededFuture(user));
      } else {
        LOGGER.error("Failed to create user " + user.getString("username"), res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public UserDatabaseService findUserById(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongo.find("users", new JsonObject().put("_id", id), res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          resultHandler.handle(Future.succeededFuture(res.result().get(0)));
        } else {
          resultHandler.handle(Future.succeededFuture());
        }
      } else {
        LOGGER.error("Failed to find user " + id, res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public UserDatabaseService updateUserById(String id, JsonObject user, Handler<AsyncResult<Void>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", id);
    mongo.updateCollection("users", query, new JsonObject().put("$set", user), res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Failed to update user " + id, res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public UserDatabaseService deleteUserById(String id, Handler<AsyncResult<Void>> resultHandler) {
    mongo.removeDocument("users", new JsonObject().put("_id", id), res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Failed to remove user " + id, res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }
}
