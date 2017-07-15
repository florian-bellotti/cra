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

    String token = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImZsb2FyaXR0YW4iLCJjYW5DcmVhdGUiOnRydWUsImNhbkRlbGV0ZSI6dHJ1ZSwiY2FuVXBkYXRlIjp0cnVlLCJjYW5HZXQiOnRydWUsImNhbkdldEFsbCI6dHJ1ZSwiaWF0IjoxNTAwMTM4MTA3LCJpc3MiOiJWZXJ0LngiLCJzdWIiOiJXaWtpIEFQSSJ9.ZKK3geujOcG_HwL_JNIpjUoG06op6WKDIZytBy3M9lE";

    JsonObject user = new JsonObject()
      .put("username", "florian")
      .put("password", "florian");

    Future<JsonObject> postRequest = Future.future();
    webClient.post("/users")
      .putHeader("Authorization", token)
      .as(BodyCodec.jsonObject())
      .sendJsonObject(user, ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> postResponse = ar.result();
          user.put("_id", postResponse.body().getString("_id"));
          postRequest.complete(postResponse.body());
        } else {
          context.fail(ar.cause());
        }
      });

    Future<JsonArray> getAllRequest = Future.future();
    postRequest.compose(h -> webClient.get("/users")
      .putHeader("Authorization", token)
      .as(BodyCodec.jsonArray())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonArray> getAllResponse = ar.result();
          try {
            String username = getAllResponse.body().getJsonObject(0).getString("username");
            Assert.assertEquals("florian", username);
            getAllRequest.complete(getAllResponse.body());
          } catch (Exception e) {
            e.printStackTrace();
            context.fail(ar.cause());
          }
        } else {
          context.fail(ar.cause());
        }
      }), getAllRequest);

    Future<JsonObject> getRequest = Future.future();
    getAllRequest.compose(h -> webClient
      .get("/users/" + user.getString("_id"))
      .putHeader("Authorization", token)
      .as(BodyCodec.jsonObject())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> getResponse = ar.result();
          try {
            String username = getResponse.body().getString("username");
            Assert.assertEquals("florian", username);
            getRequest.complete(getResponse.body());
          } catch (Exception e) {
            e.printStackTrace();
            context.fail(ar.cause());
          }
        } else {
          context.fail(ar.cause());
        }
      }), getRequest);

    Future<JsonObject> deleteRequest = Future.future();
    getRequest.compose(h -> webClient
      .delete("/users/" + user.getString("_id"))
      .putHeader("Authorization", token)
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
