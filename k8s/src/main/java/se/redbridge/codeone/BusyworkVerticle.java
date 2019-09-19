package se.redbridge.codeone;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import java.lang.management.ManagementFactory;

public class BusyworkVerticle extends AbstractVerticle {
  private HealthCheckHandler healthCheckHandler;
  private Counter counter;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);

    healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("runtime", future -> {
      long jvmUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
      if (jvmUpTime > 2 * 60 * 1000L) {
        future.complete(Status.KO()); // nothing should live that long ;)
      } else {
        future.complete(Status.OK()); // still ok, alive shorter than 2 minute.
      }
    });

    MeterRegistry registry = BackendRegistries.getDefaultNow();
    counter = Counter
      .builder("total.calls.to.work")
      .description("How many calls have been done to the /work-endpoint.")
      .register(registry);


  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);

    router.get("/health").handler(healthCheckHandler);
    router.route("/metrics").handler(PrometheusScrapingHandler.create());

    router.get("/work").handler(context -> {
      Response data = processRequest(context.request());
      final HttpServerResponse response = context.response();
      response.putHeader("content-type", "application/json");
      response.end(Json.encodePrettily(data));
      counter.increment();
    });
    vertx.createHttpServer().requestHandler(router).listen(8080, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        System.out.println("HTTP server started on port 8080");
      } else {
        startFuture.fail(http.cause());
      }
    });
  }

  private Response processRequest(HttpServerRequest request) {
    final long start = System.currentTimeMillis();
    final String input = request.getParam("input");
    return !StringUtil.isNullOrEmpty(input)
      ? new Response(ResponseCode.OK, input.chars().sorted().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString(), System.currentTimeMillis() - start)
      : new Response(ResponseCode.INVALID_MESSAGE, "Null message is invalid", System.currentTimeMillis() - start);
  }
}
