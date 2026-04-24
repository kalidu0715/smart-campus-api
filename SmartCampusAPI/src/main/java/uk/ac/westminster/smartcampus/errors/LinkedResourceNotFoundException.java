package uk.ac.westminster.smartcampus.errors;

/**
 * Part 5.2 - Thrown when a sensor references a roomId that does not exist.
 * Mapped to 422 Unprocessable Entity (more precise than 404 because the endpoint
 * itself is found; the problem is a broken reference inside the JSON payload).
 */
public class LinkedResourceNotFoundException extends RuntimeException {
  public LinkedResourceNotFoundException(String message) {
    super(message);
  }
}
