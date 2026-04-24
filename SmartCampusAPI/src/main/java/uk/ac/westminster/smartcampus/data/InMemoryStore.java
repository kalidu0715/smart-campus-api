package uk.ac.westminster.smartcampus.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;

/**
 * Singleton in-memory data store using ConcurrentHashMap for thread safety.
 * Because JAX-RS resource classes are request-scoped (new instance per request),
 * all shared state must reside here as a singleton, not in resource class fields.
 * ConcurrentHashMap prevents race conditions when concurrent requests modify data.
 */
public final class InMemoryStore {
  private static final Logger LOGGER = Logger.getLogger(InMemoryStore.class.getName());
  private static final InMemoryStore INSTANCE = new InMemoryStore();

  private final Map<String, Room> rooms = new ConcurrentHashMap<>();
  private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
  private final Map<String, List<SensorReading>> readingsBySensorId = new ConcurrentHashMap<>();

  private InMemoryStore() {
    seedData();
  }

  public static InMemoryStore getInstance() {
    return INSTANCE;
  }

  public Map<String, Room> rooms() { return rooms; }
  public Map<String, Sensor> sensors() { return sensors; }

  public List<SensorReading> readingsFor(String sensorId) {
    return readingsBySensorId.computeIfAbsent(
        sensorId, k -> Collections.synchronizedList(new ArrayList<>()));
  }

  public void addReading(String sensorId, SensorReading reading) {
    readingsFor(sensorId).add(reading);
    // Side-effect: update parent sensor currentValue (Part 4.2)
    Sensor s = sensors.get(sensorId);
    if (s != null) s.setCurrentValue(reading.getValue());
  }

  private void seedData() {
    // Seed rooms
    Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
    Room r2 = new Room("LAB-101", "Computing Lab Alpha", 30);
    rooms.put(r1.getId(), r1);
    rooms.put(r2.getId(), r2);

    // Seed sensors
    Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
    Sensor s2 = new Sensor("CO2-002",  "CO2",         "ACTIVE", 412.0, "LIB-301");
    Sensor s3 = new Sensor("OCC-003",  "Occupancy",   "MAINTENANCE", 0.0, "LAB-101");
    sensors.put(s1.getId(), s1);
    sensors.put(s2.getId(), s2);
    sensors.put(s3.getId(), s3);

    // Link sensors to rooms
    r1.getSensorIds().add("TEMP-001");
    r1.getSensorIds().add("CO2-002");
    r2.getSensorIds().add("OCC-003");

    // Seed some historical readings
    addReading("TEMP-001", new SensorReading("READ-001", System.currentTimeMillis() - 60000, 21.0));
    addReading("TEMP-001", new SensorReading("READ-002", System.currentTimeMillis(), 22.5));
    addReading("CO2-002",  new SensorReading("READ-003", System.currentTimeMillis() - 30000, 400.0));
    addReading("CO2-002",  new SensorReading("READ-004", System.currentTimeMillis(), 412.0));

    LOGGER.info("InMemoryStore seeded with 2 rooms, 3 sensors, 4 readings.");
  }
}
