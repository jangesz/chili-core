import Configuration.Config;
import Utilities.DefaultLogger;
import Utilities.Logger;
import io.vertx.core.*;

/**
 * Created by Robin on 2016-04-07.
 */
public class Launcher extends AbstractVerticle {
    private Vertx vertx;

    public void init(Vertx vertx, Context context) {
        this.vertx = vertx;
        Config.Load();
    }

    public void start(final Future<Void> future) {
        Future<Void> logging = Future.future();
        startServer(logging, new Logging.Server());

        logging.setHandler(result -> {
            if (result.succeeded()) {
                startAll(future);
            } else
                future.fail(result.cause());
        });
    }

    private void startAll(Future<Void> future) {
        Future<Void> web = Future.future();
        Future<Void> authentication = Future.future();
        Future<Void> game = Future.future();

        CompositeFuture.all(web, authentication, game).setHandler(result -> {
            if (result.succeeded()) {
                future.complete();
            } else
                future.fail(result.cause());
        });

        startServer(web, new Website.Server());
        startServer(authentication, new Authentication.Server());
        startServer(game, new Game.Server());
    }

    private void startServer(Future<Void> future, Verticle verticle) {
        vertx.deployVerticle(verticle, result -> {
            if (result.succeeded())
                future.complete();
            else
                future.fail(result.cause());
        });
    }

    public void stop(Future<Void> future) {
        future.complete();
    }

}
