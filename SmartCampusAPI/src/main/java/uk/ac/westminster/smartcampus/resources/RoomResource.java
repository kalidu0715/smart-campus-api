package uk.ac.westminster.smartcampus.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.westminster.smartcampus.data.InMemoryStore;
import uk.ac.westminster.smartcampus.errors.RoomNotEmptyException;
import uk.ac.westminster.smartcampus.model.Room;

/**
 * Part 2 - Room Management Resource.
 * GET /rooms, POST /rooms, GET /rooms/{id}, DELETE /rooms/{id}
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {
  private static final Logger LOGGER = Logger.getLogger(RoomResource.class.getName());
  private final InMemoryStore store = InMemoryStore.getInstance();

  private Map<String, String> msg(String message) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("message", message);
    return m;
  }

  /** Part 2.1 - GET /api/v1/rooms */
  @GET
  public List<Room> listRooms() {
    return new ArrayList<>(store.rooms().values());
  }

  /** Part 2.1 - POST /api/v1/rooms */
  @POST
  public Response createRoom(Room room, @Context UriInfo uriInfo) {
    if (room == null || room.getId() == null || room.getId().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(msg("Room id is required")).build();
    }
    if (store.rooms().containsKey(room.getId())) {
      return Response.status(Response.Status.CONFLICT)
          .entity(msg("Room already exists: " + room.getId())).build();
    }
    if (room.getSensorIds() == null) room.setSensorIds(new ArrayList<>());
    store.rooms().put(room.getId(), room);
    URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
    LOGGER.log(Level.INFO, "Created room {0}", room.getId());
    return Response.created(location).entity(room).build();
  }

  /** Part 2.1 - GET /api/v1/rooms/{roomId} */
  @GET
  @Path("/{roomId}")
  public Response getRoom(@PathParam("roomId") String roomId) {
    Room room = store.rooms().get(roomId);
    if (room == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(msg("Room not found: " + roomId)).build();
    }
    return Response.ok(room).build();
  }

  /**
   * Part 2.2 - DELETE /api/v1/rooms/{roomId}
   * Business rule: cannot delete if sensors are still assigned (throws 409).
   * Idempotent: first call removes room (204), subsequent calls return 404.
   * Server state is identical after both calls (room is absent), satisfying
   * idempotency per RFC 7231 which concerns server state, not response codes.
   */
  @DELETE
  @Path("/{roomId}")
  public Response deleteRoom(@PathParam("roomId") String roomId) {
    Room room = store.rooms().get(roomId);
    if (room == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(msg("Room not found: " + roomId)).build();
    }
    if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
      throw new RoomNotEmptyException(roomId);
    }
    store.rooms().remove(roomId);
    LOGGER.log(Level.INFO, "Deleted room {0}", roomId);
    return Response.noContent().build(); // 204 No Content
  }
}
