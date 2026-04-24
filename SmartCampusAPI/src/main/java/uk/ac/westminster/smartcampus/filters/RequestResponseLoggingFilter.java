package uk.ac.westminster.smartcampus.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.5 - Request and Response logging filter.
 * Implements both interfaces so one class handles the full request/response cycle.
 *
 * Using filters for logging rather than manual Logger.info() in every resource:
 * - Single point of change: format/behaviour updated in one class, not 20 methods
 * - Zero risk of forgetting to add logging to new endpoints
 * - Keeps resource classes focused solely on business logic (separation of concerns)
 * - Applies automatically to all current and future endpoints
 */
@Provider
public class RequestResponseLoggingFilter
    implements ContainerRequestFilter, ContainerResponseFilter {
  private static final Logger LOGGER =
      Logger.getLogger(RequestResponseLoggingFilter.class.getName());

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    LOGGER.log(Level.INFO, "[REQUEST]  {0} {1}",
        new Object[]{requestContext.getMethod(),
                     requestContext.getUriInfo().getRequestUri()});
  }

  @Override
  public void filter(ContainerRequestContext requestContext,
                     ContainerResponseContext responseContext) throws IOException {
    LOGGER.log(Level.INFO, "[RESPONSE] {0} {1} -> HTTP {2}",
        new Object[]{requestContext.getMethod(),
                     requestContext.getUriInfo().getRequestUri(),
                     responseContext.getStatus()});
  }
}
