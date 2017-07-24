package com.fbellotti.microservice.event;

import com.fbellotti.microservice.event.database.EventDatabaseService;
import com.fbellotti.microservice.event.database.EventDatabaseServiceImpl;
import com.fbellotti.microservice.event.http.EventRestVerticle;
import com.fbellotti.vertx.api.BaseMicroserviceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

import static com.fbellotti.microservice.event.database.EventDatabaseService.*;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class EventVerticle extends BaseMicroserviceVerticle {

  private EventDatabaseService eventDatabaseService;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    eventDatabaseService = new EventDatabaseServiceImpl(vertx, config());
    ProxyHelper.registerService(EventDatabaseService.class, vertx, eventDatabaseService, SERVICE_ADDRESS);

    // publish service and deploy REST verticle
    publishEventBusService(SERVICE_NAME, SERVICE_ADDRESS, EventDatabaseService.class)
      .compose(servicePublished -> deployRestVerticle(eventDatabaseService))
      .setHandler(future.completer());
  }

  private Future<Void> deployRestVerticle(EventDatabaseService service) {
    Future<String> future = Future.future();
    vertx.deployVerticle(new EventRestVerticle(service),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }
}
