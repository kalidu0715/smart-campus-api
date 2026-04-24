package uk.ac.westminster.smartcampus.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.westminster.smartcampus.data.InMemoryStore;
import uk.ac.westminster.smartcampus.errors.SensorUnavailableException;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;

/**
 * Part 4.2 - Sub-resource for sensor reading history.
 * Reached via SensorResource sub-resource locator only (no @Path on class).
 *
 * A successful POST updates the parent sensor's currentValue as a side-effect,
 * ensuring data consistency across the API.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {
  private static final Logger LOGGER = Logger.getLogger(SensorReadingResource.class.getName());

  private final String sensorId;
  private final InMemoryStore store = InMemoryStore.getInstance();

  public SensorReadingResource(String sensorId) {
    this.sensorId = sensorId;
  }

  private Map<String, String> msg(String message) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("message", message);
    return m;
  }

  /** GET /api/v1/sensors/{sensorId}/readings - full reading history */
  @GET
  public Response listReadings() {
    Sensor sensor = store.sensors().get(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(msg("Sensor not found: " + sensorId)).build();
    }
    List<SensorReading> readings = store.readingsFor(sensorId);
    return Response.ok(readings).build();
  }

  /**
   * POST /api/v1/sensors/{sensorId}/readings
   * Part 5.3: Throws SensorUnavailableException (403) for MAINTENANCE or OFFLINE sensors.
   * Part 4.2 side-effect: store.addReading() updates sensor.currentValue automatically.
   */
  @POST
  public Response addReading(SensorReading reading) {
    Sensor sensor = store.sensors().get(sensorId);
    if (sensor == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(msg("Sensor not found: " + sensorId)).build();
    }
    String status = sensor.getStatus();
    if ("MAINTENANCE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
      throw new SensorUnavailableException(sensorId, status);
    }
    if (reading == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(msg("Reading body is required")).build();
    }
    if (reading.getId() == null || reading.getId().isBlank()) {
      reading.setId(UUID.randomUUID().toString());
    }
    if (reading.getTimestamp() == 0L) {
      reading.setTimestamp(System.currentTimeMillis());
    }
    // Persists reading AND updates sensor.currentValue as a side-effect
    store.addReading(sensorId, reading);
    LOGGER.log(Level.INFO, "Added reading {0} for sensor {1}",
        new Object[]{reading.getId(), sensorId});
    return Response.status(Response.Status.CREATED).entity(reading).build();
  }

  /** GET /api/v1/sensors/{sensorId}/readings/{readingId} */
  @GET
  @Path("/{readingId}")
  public Response getReading(@PathParam("readingId") String readingId) {
    return store.readingsFor(sensorId).stream()
        .filter(r -> r.getId().equals(readingId))
        .findFirst()
        .map(r -> Response.ok(r).build())
        .orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(msg("Reading not found: " + readingId)).build());
  }
}
