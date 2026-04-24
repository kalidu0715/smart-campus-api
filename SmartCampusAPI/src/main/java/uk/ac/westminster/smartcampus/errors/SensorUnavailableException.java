package uk.ac.westminster.smartcampus.errors;

/**
 * Part 5.3 - Thrown when posting a reading to a sensor in MAINTENANCE or OFFLINE state.
 * Mapped to 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
  public SensorUnavailableException(String sensorId, String status) {
    super("Sensor '" + sensorId + "' is currently " + status +
          " and cannot accept new readings.");
  }
}
