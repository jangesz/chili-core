import Configuration.AuthServerSettings;
import Configuration.Config;
import Configuration.RealmSettings;
import Protocol.Game.CharacterRequest;
import Protocol.RealmRegister;
import Utilities.Serializer;
import Utilities.Token;
import Utilities.TokenFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * @author Robin Duda
 *         tests the API from realm->authentication server.
 */

@RunWith(VertxUnitRunner.class)
public class RealmAuthenticationTest {
    private Vertx vertx;
    private AuthServerSettings authconfig;
    private RealmSettings realmconfig;
    private TokenFactory factory;
    private String verticle;

    @Before
    public void setUp(TestContext context) {
        Async async = context.async();
        this.vertx = Vertx.vertx();
        factory = new TokenFactory("null".getBytes());

        Config.reload();
        authconfig = Config.instance().getAuthSettings();
        realmconfig = Config.instance().getGameServerSettings().getRealms().get(0);

        vertx.deployVerticle(new Authentication.Server(), handler -> {
            verticle = handler.result();
            async.complete();
        });
    }

    @After
    public void tearDown(TestContext context) {
        vertx.undeploy(verticle, context.asyncAssertSuccess());
    }

    static HttpClient registerRealm(AuthServerSettings authserver, RealmSettings realm, Future<Object> future) throws IOException {
        realm.load();

        return Vertx.vertx().createHttpClient().websocket(authserver.getRealmPort(), "localhost", "/", socket -> {
            socket.handler(data -> future.complete());
            socket.write(Buffer.buffer(Serializer.pack(new RealmRegister(realm))));
        });
    }

    @Test
    public void shouldRegisterWithRealm(TestContext context) {
        Async async = context.async();
        Future<WebSocket> future = Future.future();

        future.setHandler(complete -> {
            if (future.failed())
                context.fail();

            async.complete();
        });
        registerRealm(future, realmconfig);
    }

    @Test
    public void shouldFailToRegisterWithRealm(TestContext context) {
        Async async = context.async();
        Future<WebSocket> future = Future.future();
        future.setHandler(complete -> {

            if (future.succeeded())
                context.fail();

            async.complete();
        });

        realmconfig.getAuthentication().setToken(new Token(factory, "null"));
        registerRealm(future, realmconfig);
    }

    private void registerRealm(Future<WebSocket> future, RealmSettings settings) {
        vertx.createHttpClient().websocket(authconfig.getRealmPort(), "localhost", "/", socket -> {

            socket.handler(message -> {
                RealmRegister response = (RealmRegister) Serializer.unpack(message.toString(), RealmRegister.class);

                if (response.getRegistered()) {
                    future.complete(socket);
                } else
                    future.fail("Failed to register.");
            });

            socket.write(Buffer.buffer(Serializer.pack(new RealmRegister(settings))));
        });
    }

    @Ignore
    public void shouldQueryForCharacter(TestContext context) {
        Async async = context.async();
        Future<JsonObject> future = Future.future();

        future.setHandler(json -> {

            async.complete();
        });

        send(new CharacterRequest(), future);
    }

    private void send(Object object, Future<JsonObject> future) {
        Future<WebSocket> register = Future.future();

        register.setHandler(connection -> {
            if (connection.succeeded()) {
                WebSocket websocket = connection.result();

                websocket.handler(event -> {
                    future.complete(event.toJsonObject());
                });

                websocket.write(Buffer.buffer(Serializer.pack(object)));
            } else {
                future.fail("Failed to register.");
            }
        });
        registerRealm(register, realmconfig);
    }
}
