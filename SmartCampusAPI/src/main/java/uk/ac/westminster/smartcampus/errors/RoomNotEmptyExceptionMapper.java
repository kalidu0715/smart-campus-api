package uk.ac.westminster.smartcampus.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Part 5.1 - Maps RoomNotEmptyException to HTTP 409 Conflict. */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
  private static final Logger LOGGER = Logger.getLogger(RoomNotEmptyExceptionMapper.class.getName());

  @Override
  public Response toResponse(RoomNotEmptyException e) {
    LOGGER.log(Level.INFO, "Room conflict: {0}", e.getMessage());
    return Response.status(Response.Status.CONFLICT)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiError(409, "ROOM_NOT_EMPTY", e.getMessage(), System.currentTimeMillis()))
        .build();
  }
}
