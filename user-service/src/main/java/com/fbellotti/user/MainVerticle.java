package com.fbellotti.user;

import com.fbellotti.user.database.UserDatabaseVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    Future<String> dbVerticleDeployment = Future.future();
    vertx.deployVerticle(new UserDatabaseVerticle(), dbVerticleDeployment.completer());

    dbVerticleDeployment.compose(id -> {
      Future<String> httpVerticleDeployment = Future.future();
      vertx.deployVerticle(
        "com.fbellotti.user.http.HttpServerVerticle",
      new DeploymentOptions().setInstances(2),
      httpVerticleDeployment.completer());
      return httpVerticleDeployment;
    }).setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });
  }
}
