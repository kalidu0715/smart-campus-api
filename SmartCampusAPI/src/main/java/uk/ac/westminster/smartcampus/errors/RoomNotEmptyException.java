package uk.ac.westminster.smartcampus.errors;

/** Part 5.1 - Thrown when deleting a room that still has sensors. Mapped to 409 Conflict. */
public class RoomNotEmptyException extends RuntimeException {
  public RoomNotEmptyException(String roomId) {
    super("Room '" + roomId + "' cannot be deleted because it has active sensors assigned to it.");
  }
}
