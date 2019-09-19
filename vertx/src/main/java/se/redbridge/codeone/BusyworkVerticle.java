package se.redbridge.codeone;

import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BusyworkVerticle extends AbstractVerticle {
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);
    router.get("/work").handler(context -> {
      Response data = processRequest(context.request());
      final HttpServerResponse response = context.response();
      response.putHeader("content-type", "application/json");
      response.end(Json.encodePrettily(data));
    });
    router.get("/numbers").handler(context -> {
      Response data = generatePrimes(context.request());
      final HttpServerResponse response = context.response();
      response.putHeader("content-type", "application/json");
      response.end(Json.encodePrettily(data));
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

  private Response generatePrimes(HttpServerRequest request) {
    final int ceiling = Integer.parseInt(request.getParam("ceiling"));

    final var startTime = System.currentTimeMillis();
    final var collect = primeUpTo(ceiling).stream().map(Object::toString).collect(Collectors.joining(" "));
    final var runTime = System.currentTimeMillis() - startTime;

    return new Response(ResponseCode.OK, collect, runTime);
  }

  private List<Integer> primeUpTo(final int n) {
    return IntStream.rangeClosed(2, n).filter(this::isPrime).boxed().collect(Collectors.toList());
  }

  private boolean isPrime(final int number) {
    for (int i = 2; i * i < number; i++) {
      if (number % i == 0) {
        return false;
      }
    }
    return true;
  }


  private Response processRequest(HttpServerRequest request) {
    final long start = System.currentTimeMillis();
    final String input = request.getParam("input");
    return !StringUtil.isNullOrEmpty(input)
      ? new Response(ResponseCode.OK, input.chars().sorted().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString(), System.currentTimeMillis() - start)
      : new Response(ResponseCode.INVALID_MESSAGE, "Null message is invalid", System.currentTimeMillis() - start);
  }
}
