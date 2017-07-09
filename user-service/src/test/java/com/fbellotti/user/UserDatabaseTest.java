package com.fbellotti.user;

import com.fbellotti.user.database.UserDatabaseService;
import com.fbellotti.user.database.UserDatabaseVerticle;
import com.fbellotti.user.model.User;
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
      User createdUser = jsonCreatedUser.mapTo(User.class);
      service.findUserById(createdUser.getId(), context.asyncAssertSuccess(jsonUser -> {
        User user = jsonUser.mapTo(User.class);
        context.assertEquals("florian", user.getUsername());
        newUser.put("password", "test");
        service.updateUserById(createdUser.getId(), newUser, context.asyncAssertSuccess(response ->
          service.findUserById(createdUser.getId(), context.asyncAssertSuccess(jsonUserV2 -> {
            User userV2 = jsonUserV2.mapTo(User.class);
            context.assertEquals("florian", userV2.getUsername());
            context.assertEquals("test", userV2.getPassword());

            service.deleteUserById(createdUser.getId(), context.asyncAssertSuccess(deleteResponse ->
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
