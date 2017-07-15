package com.fbellotti.user;

import com.fbellotti.user.database.UserDatabaseService;
import com.fbellotti.user.database.UserDatabaseVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
@RunWith(VertxUnitRunner.class)
public class UserDatabaseTest {

  private Vertx vertx;
  private UserDatabaseService service;

  @Before
  public void prepare(TestContext context) throws InterruptedException {
    vertx = Vertx.vertx();

    JsonObject conf = new JsonObject()
      .put(UserDatabaseVerticle.CONFIG_USERDB_URL, "mongodb://localhost:27017")
      .put(UserDatabaseVerticle.CONFIG_USERDB_DBNAME, "test2");

    vertx.deployVerticle(new UserDatabaseVerticle(), new DeploymentOptions().setConfig(conf),
      context.asyncAssertSuccess(id ->
        service = UserDatabaseService.createProxy(vertx, UserDatabaseVerticle.CONFIG_USERDB_QUEUE)
      )
    );
  }

  @Test
  public void crud_operations(TestContext context) {
    Async async = context.async();

    JsonObject newUser = new JsonObject()
      .put("username", "florian");

    service.createUser(newUser, context.asyncAssertSuccess(jsonCreatedUser -> {
      String createdUserId = jsonCreatedUser.getString("_id");
      service.findUserById(createdUserId, context.asyncAssertSuccess(jsonUser -> {
        context.assertEquals("florian", jsonUser.getString("username"));
        newUser.put("password", "test");
        service.updateUserById(createdUserId, newUser, context.asyncAssertSuccess(response ->
          service.findUserById(createdUserId, context.asyncAssertSuccess(jsonUserV2 -> {
            context.assertEquals("florian", jsonUser.getString("username"));

            service.deleteUserById(createdUserId, context.asyncAssertSuccess(deleteResponse ->
              service.findAllUsers(context.asyncAssertSuccess(jsonAll -> {
                context.assertEquals("[]", jsonAll.encode());
                async.complete();
              }))
            ));
          }))
        ));
      }));
    }));
    async.awaitSuccess(5000);
  }

  @After
  public void finish(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }
}
