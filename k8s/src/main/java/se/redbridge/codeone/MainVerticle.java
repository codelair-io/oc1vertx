package se.redbridge.codeone;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Collections;

public class MainVerticle extends AbstractVerticle {
  private Logger log = LoggerFactory.getLogger(AbstractVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    CompositeFuture.all(Collections.singletonList(deploy(BusyworkVerticle.class.getName()))).setHandler(result -> {
      if (result.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(result.cause());
      }
    });
  }

  private Future<Void> deploy(String name) {

    final Future<Void> future = Future.future();
    vertx.deployVerticle(name, result -> {
      if (result.failed()) {
        log.error("Failed to deploy verticle " + name);
        future.fail(result.cause());
      } else {
        log.info("Deployed verticle " + name);
        future.complete();
      }
    });

    return future;
  }
}
