package com.fbellotti.user.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.mongo.HashSaltStyle;
import io.vertx.ext.auth.mongo.HashStrategy;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class UserDatabaseVerticle extends AbstractVerticle {

  public static final String CONFIG_USERDB_QUEUE = "userdb.queue";
  public static final String CONFIG_USERDB_URL = "userdb.url";
  public static final String CONFIG_USERDB_DBNAME = "userdb.name";

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    MongoClient mongo = MongoClient.createShared(vertx, new JsonObject()
      .put("connection_string", config().getString(CONFIG_USERDB_URL, "mongodb://localhost:27017"))
      .put("db_name", config().getString(CONFIG_USERDB_DBNAME, "test"))
    );

    MongoAuth authProvider = MongoAuth.create(mongo, new JsonObject());

    // Secure api
    JWTAuth jwtAuth = JWTAuth.create(vertx, new JsonObject()
      .put("keyStore", new JsonObject()
        .put("path", "keystore.jceks")
        .put("type", "jceks")
        .put("password", "secret")));

    UserDatabaseService.create(mongo, authProvider, ready -> {
      if (ready.succeeded()) {
        ProxyHelper.registerService(UserDatabaseService.class, vertx, ready.result(), CONFIG_USERDB_QUEUE);
        startFuture.complete();
      } else {
        startFuture.fail(ready.cause());
      }
    });
  }
}
