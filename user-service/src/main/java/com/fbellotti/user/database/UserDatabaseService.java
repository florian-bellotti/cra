package com.fbellotti.user.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
@ProxyGen
public interface UserDatabaseService {

  @Fluent
  UserDatabaseService findAllUsers(Handler<AsyncResult<JsonArray>> resultHandler);

  @Fluent
  UserDatabaseService createUser(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  UserDatabaseService authenticate(JsonObject user, Handler<AsyncResult<String>> resultHandler);

  @Fluent
  UserDatabaseService findUserById(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  UserDatabaseService updateUserById(String id, JsonObject user, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  UserDatabaseService deleteUserById(String id, Handler<AsyncResult<Void>> resultHandler);

  static UserDatabaseService create(MongoClient mongo, MongoAuth authProvider,
                                    Handler<AsyncResult<UserDatabaseService>> readyHandler) {
    return new UserDatabaseServiceImpl(mongo, authProvider, readyHandler);
  }

  static UserDatabaseService createProxy(Vertx vertx, String address) {
    return new UserDatabaseServiceVertxEBProxy(vertx, address) ;
  }
}
