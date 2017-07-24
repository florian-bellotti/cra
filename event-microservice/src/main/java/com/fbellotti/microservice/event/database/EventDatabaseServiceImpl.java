package com.fbellotti.microservice.event.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class EventDatabaseServiceImpl implements EventDatabaseService {

  private static final String COLLECTION = "event";

  private final MongoClient mongo;

  public EventDatabaseServiceImpl(Vertx vertx, JsonObject config) {
    this.mongo = MongoClient.createNonShared(vertx, config);
  }

  @Override
  public EventDatabaseService createEvent(JsonObject event, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongo.save(COLLECTION, event, res -> {
      if (res.succeeded()) {
        event.put("id", res.result());
        resultHandler.handle(Future.succeededFuture(event));
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public EventDatabaseService retrieveEvent(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", id);
    mongo.find(COLLECTION, query, res -> {
      if (res.succeeded()) {
        if (res.result() == null || res.result().isEmpty()) {
          resultHandler.handle(Future.succeededFuture());
        } else {
          resultHandler.handle(Future.succeededFuture(res.result().get(0)));
        }
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public EventDatabaseService updateEvent(String id, JsonObject event, Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", id);
    JsonObject set = new JsonObject().put("$set", event);
    mongo.updateCollection(COLLECTION, query, set, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture(event));
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public EventDatabaseService deleteEvent(String id, Handler<AsyncResult<Void>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", id);
    mongo.removeDocument(COLLECTION, query, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }
}
