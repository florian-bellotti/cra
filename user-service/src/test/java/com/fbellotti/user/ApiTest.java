package com.fbellotti.user;

import com.fbellotti.user.database.UserDatabaseVerticle;
import com.fbellotti.user.http.HttpServerVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.junit.After;
import org.junit.Assert;
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
      .setDefaultPort(8080)
      .setSsl(true)
      .setTrustOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret")));
  }

  @Test
  public void executeRequests(TestContext context) {
    Async async = context.async();

    JsonObject user = new JsonObject()
      .put("_id", "111")
      .put("username", "florian")
      .put("password", "florian");

    Future<JsonObject> postRequest = Future.future();
    webClient.post("/users")
      .as(BodyCodec.jsonObject())
      .sendJsonObject(user, ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> postResponse = ar.result();
          postRequest.complete(postResponse.body());
        } else {
          context.fail(ar.cause());
        }
      });

    Future<JsonArray> getAllRequest = Future.future();
    postRequest.compose(h -> webClient.get("/users")
      .as(BodyCodec.jsonArray())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonArray> getAllResponse = ar.result();
          Assert.assertEquals("[{\"_id\":\"111\",\"username\":\"florian\",\"password\":\"florian\"}]",
            getAllResponse.body().encode());
          getAllRequest.complete(getAllResponse.body());
        } else {
          context.fail(ar.cause());
        }
      }), getAllRequest);

    Future<JsonObject> getRequest = Future.future();
    getAllRequest.compose(h -> webClient
      .get("/users/111")
      .as(BodyCodec.jsonObject())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> getResponse = ar.result();
          Assert.assertEquals("{\"_id\":\"111\",\"username\":\"florian\",\"password\":\"florian\"}",
            getResponse.body().encode());
          getRequest.complete(getResponse.body());
        } else {
          context.fail(ar.cause());
        }
      }), getRequest);

    Future<JsonObject> deleteRequest = Future.future();
    getRequest.compose(h -> webClient
      .delete("/users/111")
      .as(BodyCodec.jsonObject())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> deleteResponse = ar.result();
          deleteRequest.complete(deleteResponse.body());
        } else {
          context.fail(ar.cause());
        }
      }), deleteRequest);

    deleteRequest.compose(response -> async.complete(),
      Future.failedFuture("Oh?"));
  }

  @After
  public void finish(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }
}
