package se.redbridge.codeone;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NumbersResource {

  @RequestMapping("/numbers")
  public Response generatePrimeUpTo(@RequestParam(value = "ceiling", defaultValue = "100") final int ceiling) {
    final var startTime = System.currentTimeMillis();
    final var collect = primeUpTo(ceiling).stream()
      .map(Object::toString)
      .collect(Collectors.joining(" "));
    final var runTime = System.currentTimeMillis() - startTime;

    return new Response(ResponseCode.OK, collect, runTime);
  }

  private List<Integer> primeUpTo(final int n) {
    return IntStream.rangeClosed(2, n)
      .filter(this::isPrime)
      .boxed()
      .collect(Collectors.toList());
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
