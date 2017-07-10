package com.fbellotti.user;

import com.fbellotti.user.database.UserDatabaseService;
import com.fbellotti.user.database.UserDatabaseVerticle;
import com.fbellotti.user.http.HttpServerVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
@RunWith(VertxUnitRunner.class)
public class ApiTest {

  private Vertx vertx;
  private WebClient webClient;

  @Before
  public void prepare(TestContext context) {
    vertx = Vertx.vertx();

    JsonObject dbConf = new JsonObject()
      .put(UserDatabaseVerticle.CONFIG_USERDB_URL, "mongodb://localhost:27017")
      .put(UserDatabaseVerticle.CONFIG_USERDB_DBNAME, "test2");

    vertx.deployVerticle(new UserDatabaseVerticle(),
      new DeploymentOptions().setConfig(dbConf), context.asyncAssertSuccess());

    vertx.deployVerticle(new HttpServerVerticle(), context.asyncAssertSuccess());

    webClient = WebClient.create(vertx, new WebClientOptions()
      .setDefaultHost("localhost")
      .setDefaultPort(8080));
  }

  @Test
  public void executeRequests(TestContext context) {
    Async async = context.async();

    JsonObject user = new JsonObject()
      .put("_id", "111")
      .put("username", "florian");

    Future<JsonObject> postRequest = Future.future();
    webClient.post("/users")
      .as(BodyCodec.jsonObject())
      .sendJsonObject(user, ar -> {
        if (ar.succeeded()) {
          System.out.println("w");
          HttpResponse<JsonObject> postResponse = ar.result();
          postRequest.complete(postResponse.body());
        } else {
          context.fail(ar.cause());
        }
      });

    Future<JsonObject> getRequest = Future.future();
    postRequest.compose(h -> {
      webClient.get("/users/111")
        .as(BodyCodec.jsonObject())
        .send(ar -> {
          if (ar.succeeded()) {
            HttpResponse<JsonObject> getResponse = ar.result();
            getRequest.complete(getResponse.body());
          } else {
            context.fail(ar.cause());
          }
        });
    }, getRequest);

    Future<JsonObject> deleteRequest = Future.future();
    getRequest.compose(h -> {
      webClient.delete("/users/111")
        .as(BodyCodec.jsonObject())
        .send(ar -> {
          if (ar.succeeded()) {
            HttpResponse<JsonObject> getResponse = ar.result();
            deleteRequest.complete(getResponse.body());
          } else {
            context.fail(ar.cause());
          }
        });
    }, deleteRequest);

    deleteRequest.compose(response -> {
      async.complete();
    }, Future.failedFuture("Oh?"));
  }

  @After
  public void finish(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }
}
