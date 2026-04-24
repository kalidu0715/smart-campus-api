package uk.ac.westminster.smartcampus.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Part 1.2 - Discovery endpoint at GET /api/v1
 * Returns API metadata, version, contact info, and HATEOAS resource links.
 *
 * HATEOAS benefit: Clients discover all API entry points from a single root URL
 * rather than hard-coding paths from static docs. If paths change server-side,
 * clients following links automatically adapt without code changes.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {
  private static final Logger LOGGER = Logger.getLogger(DiscoveryResource.class.getName());

  @GET
  public Map<String, Object> discover() {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("api",     "Smart Campus Sensor and Room Management API");
    root.put("version", "v1");
    root.put("status",  "operational");

    Map<String, String> contact = new LinkedHashMap<>();
    contact.put("module", "5COSC022W Client-Server Architectures");
    contact.put("owner",  "Facilities Management Team");
    contact.put("email",  "smartcampus@westminster.ac.uk");
    root.put("contact", contact);

    Map<String, String> resources = new LinkedHashMap<>();
    resources.put("rooms",   "/api/v1/rooms");
    resources.put("sensors", "/api/v1/sensors");
    root.put("resources", resources);

    // HATEOAS _links map
    Map<String, String> links = new LinkedHashMap<>();
    links.put("self",    "/api/v1");
    links.put("rooms",   "/api/v1/rooms");
    links.put("sensors", "/api/v1/sensors");
    root.put("_links", links);

    return root;
  }
}
