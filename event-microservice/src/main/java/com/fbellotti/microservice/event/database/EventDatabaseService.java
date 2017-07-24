package com.fbellotti.microservice.event.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * A service interface managing event storage operations.
 * This service is an event bus service (aka. service proxy).
 *
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
@VertxGen
@ProxyGen
public interface EventDatabaseService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "event-storage-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.event.storage";

  /**
   * Save an event into the persistence.
   *
   * @param event         event data object
   * @param resultHandler async result handler
   */
  @Fluent
  EventDatabaseService createEvent(JsonObject event, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   * Retrieve the event with a certain {@code id}.
   *
   * @param id            event id
   * @param resultHandler async result handler
   */
  @Fluent
  EventDatabaseService retrieveEvent(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   * Update the event with a certain {@code id}.
   *
   * @param id            event id
   * @param event         event data object
   * @param resultHandler async result handler
   */
  @Fluent
  EventDatabaseService updateEvent(String id, JsonObject event, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   * Delete the event with a certain {@code id}.
   *
   * @param id            event id
   * @param resultHandler async result handler
   */
  @Fluent
  EventDatabaseService deleteEvent(String id, Handler<AsyncResult<Void>> resultHandler);

}
