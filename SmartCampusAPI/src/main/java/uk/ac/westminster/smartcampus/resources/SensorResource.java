package uk.ac.westminster.smartcampus.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.westminster.smartcampus.data.InMemoryStore;
import uk.ac.westminster.smartcampus.errors.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;

/**
 * Part 3 - Sensor Resource.
 * GET /sensors, GET /sensors?type=X, POST /sensors, GET /sensors/{id}, DELETE /sensors/{id}
 * Sub-resource locator: /sensors/{id}/readings -> SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {
  private static final Logger LOGGER = Logger.getLogger(SensorResource.class.getName());
  private final InMemoryStore store = InMemoryStore.getInstance();

  private Map<String, String> msg(String message) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("message", message);
    return m;
  }

  /**
   * Part 3.2 - GET /api/v1/sensors with optional ?type= filter.
   * @QueryParam is superior to path-based filtering (/sensors/type/CO2) because:
   * query params are optional, composable (add &status=ACTIVE easily), and do not
   * create false resource hierarchy. Path params should only identify resources.
   */
  @GET
  public List<Sensor> listSensors(@QueryParam("type") String type) {
    List<Sensor> all = new ArrayList<>(store.sensors().values());
    if (type == null || type.isBlank()) return all;
    String needle = type.trim().toLowerCase(Locale.ROOT);
    List<Sensor> filtered = new ArrayList<>();
    for (Sensor s : all) {
      if (s.getType() != null && s.getType().trim().toLowerCase(Locale.ROOT).equals(needle)) {
        filtered.add(s);
      }
    }
    return filtered;
  }

  /**
   * Part 3.1 - POST /api/v1/sensors
   * @Consumes(APPLICATION_JSON): If client sends text/plain or application/xml,
   * JAX-RS returns HTTP 415 Unsupported Media Type before the method is even invoked.
   * Validates that the roomId foreign key references an existing room.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
    if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(msg("Sensor id is required")).build();
    }
    if (store.sensors().containsKey(sensor.getId())) {
      return Response.status(Response.Status.CONFLICT)
          .entity(msg("Sensor already exists: " + sensor.getId())).build();
    }
    if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(msg("Sensor roomId is required")).build();
    }
    Room room = store.rooms().get(sensor.getRoomId());
    if (room == null) {
      throw new LinkedResourceNotFoundException(
          "Cannot create sensor '" + sensor.getId() + "': roomId '"
          + sensor.getRoomId() + "' does not exist.");
    }
    if (sensor.getStatus() == null || sensor.getStatus().isBlank()) sensor.setStatus("ACTIVE");
    store.sensors().put(sensor.getId(), sensor);
    if (!room.getSensorIds().contains(sensor.getId())) room.getSensorIds().add(sensor.getId());
    URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
    LOGGER.log(Level.INFO, "Created sensor {0} in room {1}",
        new Object[]{sensor.getId(), sensor.getRoomId()});
    return Response.created(location).entity(sensor).build();
  }

  /** GET /api/v1/sensors/{sensorId} */
  @GET
  @Path("/{sensorId}")
  public Response getSensor(@PathParam("sensorId") String sensorId) {
    Sensor sensor = store.sensors().get(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(msg("Sensor not found: " + sensorId)).build();
    }
    return Response.ok(sensor).build();
  }

  /** DELETE /api/v1/sensors/{sensorId} - also unlinks from parent room */
  @DELETE
  @Path("/{sensorId}")
  public Response deleteSensor(@PathParam("sensorId") String sensorId) {
    Sensor sensor = store.sensors().get(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(msg("Sensor not found: " + sensorId)).build();
    }
    // Unlink from parent room to keep room.sensorIds consistent
    Room room = store.rooms().get(sensor.getRoomId());
    if (room != null) room.getSensorIds().remove(sensorId);
    store.sensors().remove(sensorId);
    LOGGER.log(Level.INFO, "Deleted sensor {0}", sensorId);
    return Response.noContent().build();
  }

  /**
   * Part 4.1 - Sub-Resource Locator for /sensors/{sensorId}/readings
   * Delegates to SensorReadingResource. No HTTP method annotation - JAX-RS
   * continues path matching on the returned object.
   *
   * Benefits: SensorResource stays focused on sensor lifecycle.
   * SensorReadingResource handles reading history independently.
   * Each class is separately testable and maintainable.
   */
  @Path("/{sensorId}/readings")
  public SensorReadingResource readingsSubResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
  }
}
