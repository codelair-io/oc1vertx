package se.redbridge.codeone;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusyworkResource {

  @RequestMapping("/work")
  public Response work(@RequestParam(value = "input", defaultValue = "") String input) {
    return processRequest(input);
  }

  private Response processRequest(String input) {
    final long start = System.currentTimeMillis();
    return input != null && !input.isEmpty()
      ? new Response(ResponseCode.OK, input.chars().sorted().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString(), System.currentTimeMillis() - start)
      : new Response(ResponseCode.INVALID_MESSAGE, "Null message is invalid", System.currentTimeMillis() - start);
  }
}
