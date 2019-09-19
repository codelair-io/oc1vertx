package se.redbridge.codeone;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/numbers")
@ApplicationScoped
public class NumbersResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response generatePrimeUpTo(@QueryParam("ceiling") @DefaultValue("100") final int ceiling) {
    final long startTime = System.currentTimeMillis();
    final String collect = primeUpTo(ceiling).stream().map(Object::toString).collect(Collectors.joining(" "));
    final long runTime = System.currentTimeMillis() - startTime;

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
}
