package com.fbellotti.microservice.event.http;

import com.fbellotti.microservice.event.database.EventDatabaseService;
import com.fbellotti.microservice.event.model.Event;
import com.fbellotti.vertx.api.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * A verticle supplies REST endpoint for event service.
 *
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class EventRestVerticle extends RestAPIVerticle {

  private static final String SERVICE_NAME = "event-rest-service";
  private static final String EVENT_CREATE = "/events";
  private static final String EVENT_RETRIEVE = "/events/:id";
  private static final String EVENT_UPDATE = "/events/:id";
  private static final String EVENT_DELETE = "/events/:id";

  private final EventDatabaseService service;

  public EventRestVerticle(EventDatabaseService service) {
    this.service = service;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post(EVENT_CREATE).handler(this::create);
    router.get(EVENT_RETRIEVE).handler(this::retrieve);
    router.put(EVENT_UPDATE).handler(this::update);
    router.delete(EVENT_DELETE).handler(this::delete);

    String host = config().getString("event.http.address", "0.0.0.0");
    int port = config().getInteger("event.http.port", 8090);

    // create HTTP server and publish REST service
    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void create(RoutingContext context) {
    Event event = new Event(context.getBodyAsJson());
    String check = event.checkEvent();

    if (check == null) {
      service.createEvent(context.getBodyAsJson(), resultHandler(context, Json::encodePrettily));
    } else {
      badRequest(context, check);
    }
  }

  private void retrieve(RoutingContext context) {
    String eventId = context.request().getParam("id");
    service.retrieveEvent(eventId, resultHandlerNonEmpty(context));
  }

  private void update(RoutingContext context) {
    String eventId = context.request().getParam("id");
    Event event = new Event(context.getBodyAsJson());
    String check = event.checkEvent();

    if (check == null) {
      service.updateEvent(eventId, context.getBodyAsJson(), resultHandler(context, Json::encodePrettily));
    } else {
      badRequest(context, check);
    }
  }

  private void delete(RoutingContext context) {
    String eventId = context.request().getParam("id");
    service.deleteEvent(eventId, deleteResultHandler(context));
  }
}
