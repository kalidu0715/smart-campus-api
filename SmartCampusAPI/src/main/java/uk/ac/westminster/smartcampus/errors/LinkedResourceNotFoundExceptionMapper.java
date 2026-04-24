package uk.ac.westminster.smartcampus.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Part 5.2 - Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity. */
@Provider
public class LinkedResourceNotFoundExceptionMapper
    implements ExceptionMapper<LinkedResourceNotFoundException> {
  private static final Logger LOGGER =
      Logger.getLogger(LinkedResourceNotFoundExceptionMapper.class.getName());

  @Override
  public Response toResponse(LinkedResourceNotFoundException e) {
    LOGGER.log(Level.INFO, "Linked resource not found: {0}", e.getMessage());
    return Response.status(422)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiError(422, "UNPROCESSABLE_ENTITY", e.getMessage(), System.currentTimeMillis()))
        .build();
  }
}
