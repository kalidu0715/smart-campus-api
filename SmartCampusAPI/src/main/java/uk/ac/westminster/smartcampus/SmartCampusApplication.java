package uk.ac.westminster.smartcampus;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import java.util.logging.Logger;

/**
 * Part 1.1 - JAX-RS Application bootstrap.
 * @ApplicationPath sets the versioned API entry point.
 *
 * Default JAX-RS lifecycle: resource classes are REQUEST-SCOPED by default -
 * a new instance is created for every HTTP request. This means instance fields
 * cannot hold shared state. All shared data lives in InMemoryStore (singleton)
 * using ConcurrentHashMap to prevent race conditions under concurrent requests.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
  private static final Logger LOGGER = Logger.getLogger(SmartCampusApplication.class.getName());

  public SmartCampusApplication() {
    packages("uk.ac.westminster.smartcampus");
    LOGGER.info("Smart Campus API initialised at /api/v1");
  }
}
