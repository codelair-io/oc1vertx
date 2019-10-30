package se.redbridge.codeone;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/work")
@ApplicationScoped
public class BusyworkResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response open(@QueryParam("input") @DefaultValue("") String input) {
        return processRequest(input);
    }

    private Response processRequest(String input) {
        final long start = System.currentTimeMillis();
        return input != null && !input.isEmpty()
                ? new Response(ResponseCode.OK, input.chars().sorted().collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString(), System.currentTimeMillis() - start)
                : new Response(ResponseCode.INVALID_MESSAGE, "Null message is invalid", System.currentTimeMillis() - start);
    }
}
