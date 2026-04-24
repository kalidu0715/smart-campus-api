package uk.ac.westminster.smartcampus.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Part 5.3 - Maps SensorUnavailableException to HTTP 403 Forbidden. */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
  private static final Logger LOGGER =
      Logger.getLogger(SensorUnavailableExceptionMapper.class.getName());

  @Override
  public Response toResponse(SensorUnavailableException e) {
    LOGGER.log(Level.INFO, "Sensor unavailable: {0}", e.getMessage());
    return Response.status(Response.Status.FORBIDDEN)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiError(403, "SENSOR_UNAVAILABLE", e.getMessage(), System.currentTimeMillis()))
        .build();
  }
}
