package uk.ac.westminster.smartcampus.errors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 - Global safety net: catches any unhandled Throwable and returns HTTP 500.
 *
 * Security rationale: Never expose raw Java stack traces externally. They reveal:
 * 1. Internal package/class structure (aids targeted attacks)
 * 2. Library names and versions (allows CVE lookups)
 * 3. Business logic method names (leaks implementation details)
 * 4. Potential SQL fragments or schema info from DB errors
 * All real detail is logged server-side only; consumers receive a generic message.
 */
@Provider
public class GlobalThrowableExceptionMapper implements ExceptionMapper<Throwable> {
  private static final Logger LOGGER =
      Logger.getLogger(GlobalThrowableExceptionMapper.class.getName());

  @Override
  public Response toResponse(Throwable exception) {
    // Pass JAX-RS WebApplicationExceptions through with their correct status codes
    if (exception instanceof WebApplicationException webEx) {
      Response resp = webEx.getResponse();
      int status = (resp == null) ? 500 : resp.getStatus();
      // If the response already carries a JSON entity, return it as-is
      if (resp != null && resp.hasEntity()) {
        return Response.fromResponse(resp).type(MediaType.APPLICATION_JSON).build();
      }
      String msg = webEx.getMessage() != null ? webEx.getMessage() : "Request failed.";
      return Response.status(status)
          .type(MediaType.APPLICATION_JSON)
          .entity(new ApiError(status, "HTTP_" + status, msg, System.currentTimeMillis()))
          .build();
    }

    // Truly unexpected errors: log full stack trace server-side, return generic 500
    LOGGER.log(Level.SEVERE, "Unhandled error caught by global safety net", exception);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiError(
            500, "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact the API administrator.",
            System.currentTimeMillis()))
        .build();
  }
}
