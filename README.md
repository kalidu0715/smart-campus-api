# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W – Client-Server Architectures (2025/26)  
**University:** University of Westminster  
**Technology:** JAX-RS (Jersey 2.41) · Apache Tomcat 9 · Java 17  
**Base URL:** `http://localhost:8080/smart-campus-api/api/v1`

---

## Table of Contents

1. [API Design Overview](#1-api-design-overview)
2. [Project Structure](#2-project-structure)
3. [How to Build and Run](#3-how-to-build-and-run)
4. [API Endpoints Reference](#4-api-endpoints-reference)
5. [Sample curl Commands](#5-sample-curl-commands)
6. [Report – Question Answers](#6-report--question-answers)

---

## 1. API Design Overview

This API manages the physical and sensor infrastructure of a university Smart Campus. It provides a fully RESTful interface for facilities managers and automated building systems to interact with campus data.

### Core Entities

| Entity | Description |
|---|---|
| **Room** | A physical campus room with an ID, name, capacity, and list of assigned sensor IDs |
| **Sensor** | A hardware sensor of a given type (Temperature, CO2, Occupancy) deployed in a room |
| **SensorReading** | A historical measurement captured by a sensor at a specific timestamp |

### Resource Hierarchy

```
/api/v1                              ← Discovery endpoint (HATEOAS)
/api/v1/rooms                        ← Room collection
/api/v1/rooms/{roomId}               ← Individual room
/api/v1/sensors                      ← Sensor collection
/api/v1/sensors/{sensorId}           ← Individual sensor
/api/v1/sensors/{sensorId}/readings  ← Sensor reading history (sub-resource)
```

### Design Principles Applied

- Resource-based URIs with consistent, meaningful naming
- Correct HTTP verbs: GET (read), POST (create), DELETE (remove)
- Meaningful HTTP status codes: 200, 201, 204, 403, 404, 409, 422, 500
- Structured JSON error bodies on all failure paths — no raw stack traces
- HATEOAS discovery endpoint at root
- Sub-Resource Locator pattern for reading history
- Thread-safe `ConcurrentHashMap` singleton for in-memory storage
- Centralised cross-cutting concerns via JAX-RS filters and exception mappers
- No database, no Spring Boot — pure JAX-RS as required by the specification

---

## 2. Project Structure

```
SmartCampusAPI/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── uk/ac/westminster/smartcampus/
        │       ├── SmartCampusApplication.java          ← @ApplicationPath("/api/v1")
        │       ├── data/
        │       │   └── InMemoryStore.java               ← Singleton ConcurrentHashMap store
        │       ├── model/
        │       │   ├── Room.java
        │       │   ├── Sensor.java
        │       │   └── SensorReading.java
        │       ├── resources/
        │       │   ├── DiscoveryResource.java           ← GET /api/v1
        │       │   ├── RoomResource.java                ← /api/v1/rooms
        │       │   ├── SensorResource.java              ← /api/v1/sensors
        │       │   └── SensorReadingResource.java       ← /api/v1/sensors/{id}/readings
        │       ├── errors/
        │       │   ├── ApiError.java                    ← Standard error response body
        │       │   ├── RoomNotEmptyException.java        ← 409 Conflict
        │       │   ├── RoomNotEmptyExceptionMapper.java
        │       │   ├── LinkedResourceNotFoundException.java  ← 422 Unprocessable Entity
        │       │   ├── LinkedResourceNotFoundExceptionMapper.java
        │       │   ├── SensorUnavailableException.java  ← 403 Forbidden
        │       │   ├── SensorUnavailableExceptionMapper.java
        │       │   └── GlobalThrowableExceptionMapper.java  ← 500 catch-all
        │       └── filters/
        │           └── RequestResponseLoggingFilter.java ← Request & response logging
        └── webapp/
            ├── META-INF/
            │   └── context.xml
            └── WEB-INF/
                └── web.xml
```

---

## 3. How to Build and Run

### Prerequisites

- **Java 17+** — [Download from Adoptium](https://adoptium.net)
- **Apache Maven 3.6+** — [Download from Maven](https://maven.apache.org/download.cgi)
- **Apache Tomcat 9.0** — [Download from Tomcat](https://tomcat.apache.org/download-90.cgi)
- **NetBeans IDE** (recommended) — [Download from Apache](https://netbeans.apache.org)

Verify installations:
```bash
java -version
mvn -version
```

### Option A — Run with NetBeans (Recommended)

1. Clone or download this repository
2. Open NetBeans → **File → Open Project** → select the `SmartCampusAPI` folder
3. Right-click the project → **Clean and Build**
4. Right-click the project → **Run**
5. NetBeans will deploy the WAR to its bundled Tomcat and open a browser

### Option B — Build manually with Maven + Tomcat

**Step 1 — Clone the repository:**
```bash
git clone https://github.com/<your-username>/SmartCampusAPI.git
cd SmartCampusAPI
```

**Step 2 — Build the WAR file:**
```bash
mvn clean package
```
This produces `target/smart-campus-api.war`.

**Step 3 — Deploy to Tomcat:**
```bash
# Copy WAR to Tomcat webapps directory
cp target/smart-campus-api.war /path/to/tomcat/webapps/

# Start Tomcat
/path/to/tomcat/bin/startup.sh        # Linux/macOS
/path/to/tomcat/bin/startup.bat       # Windows
```

**Step 4 — Verify the server is running:**
```bash
curl http://localhost:8080/smart-campus-api/api/v1
```

You should receive a JSON response with API metadata.

### Pre-seeded Data

The API starts with the following data so you can test immediately:

| Type | ID | Details |
|---|---|---|
| Room | `LIB-301` | Library Quiet Study, capacity 50 |
| Room | `LAB-101` | Computing Lab Alpha, capacity 30 |
| Sensor | `TEMP-001` | Temperature, **ACTIVE**, in LIB-301 |
| Sensor | `CO2-002` | CO2, **ACTIVE**, in LIB-301 |
| Sensor | `OCC-003` | Occupancy, **MAINTENANCE**, in LAB-101 |

---

## 4. API Endpoints Reference

### Discovery

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1` | API metadata and HATEOAS links | 200 |

### Rooms

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a new room | 201 |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID | 200 / 404 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room (blocked if sensors assigned) | 204 / 404 / 409 |

### Sensors

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/sensors` | List all sensors | 200 |
| GET | `/api/v1/sensors?type=CO2` | Filter sensors by type | 200 |
| POST | `/api/v1/sensors` | Register new sensor (validates roomId) | 201 / 400 / 422 |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID | 200 / 404 |
| DELETE | `/api/v1/sensors/{sensorId}` | Delete sensor and unlink from room | 204 / 404 |

### Sensor Readings (Sub-Resource)

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get full reading history | 200 / 404 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add new reading (updates currentValue) | 201 / 403 / 404 |
| GET | `/api/v1/sensors/{sensorId}/readings/{readingId}` | Get single reading | 200 / 404 |

### Error Responses

| Status | When |
|---|---|
| 400 Bad Request | Missing required fields |
| 403 Forbidden | Posting to MAINTENANCE or OFFLINE sensor |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Deleting a room that has sensors |
| 422 Unprocessable Entity | Sensor references a non-existent roomId |
| 500 Internal Server Error | Unexpected runtime error (safe generic message) |

---

## 5. Sample curl Commands

> Replace `localhost:8080` with your server address if different.

### 1. Get API discovery metadata
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1 \
  -H "Accept: application/json"
```

### 2. Get all rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Accept: application/json"
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"id":"ENG-201","name":"Engineering Studio","capacity":40}'
```

### 4. Attempt to delete a room that has sensors (expect 409)
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301 \
  -H "Accept: application/json"
```

### 5. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

### 6. Register a new sensor with a valid roomId
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"id":"TEMP-005","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LAB-101"}'
```

### 7. Register a sensor with a non-existent roomId (expect 422)
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

### 8. Get reading history for a sensor
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Accept: application/json"
```

### 9. Post a new reading to an active sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"value":26.3}'
```

### 10. Post a reading to a MAINTENANCE sensor (expect 403)
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-003/readings \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"value":5.0}'
```

---

## 6. Report – Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle and In-Memory Data Management

**Question: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronise your in-memory data structures to prevent data loss or race conditions.**

By default, JAX-RS follows a **request-scoped lifecycle**: the runtime creates a brand-new instance of each resource class for every incoming HTTP request and discards it once the response has been sent. This is the default behaviour specified in JAX-RS 2.1 (Java EE / `javax` namespace) and is intentional — it makes each resource object naturally isolated between concurrent requests at the instance level, since no two requests share the same object.

However, this creates a direct and critical problem for in-memory state management. If shared data structures such as `HashMap` or `ArrayList` were declared as instance fields inside a resource class, each request would receive its own private, empty collection. Any data written during one request would be lost the moment the request ended, making persistence between calls entirely impossible.

To solve this, all shared state in this implementation lives in the `InMemoryStore` class, which is implemented as a **Singleton** using the static final instance pattern. Every resource class calls `InMemoryStore.getInstance()` to obtain the same shared reference. This ensures all requests read from and write to the same data collections.

Thread safety is addressed using `ConcurrentHashMap` for both the rooms and sensors maps. Unlike a plain `HashMap`, `ConcurrentHashMap` uses internal lock striping — it partitions the map into segments and locks only the relevant segment during a write, allowing multiple concurrent reads to proceed without blocking. This prevents race conditions such as two simultaneous `POST /sensors` requests corrupting the same room's `sensorIds` list through a lost-update scenario. For the readings lists, `Collections.synchronizedList(new ArrayList<>())` wraps each list to ensure that individual add operations are atomic.

---

### Part 1.2 — HATEOAS and Hypermedia in RESTful Design

**Question: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**

HATEOAS — Hypermedia As The Engine Of Application State — is the principle that API responses should embed links guiding the client to related resources and available next actions, making the API self-describing at runtime. Roy Fielding, who defined REST in his doctoral dissertation, identified hypermedia as a mandatory constraint of the architectural style, not merely a best practice.

The fundamental benefit over static documentation is **decoupling**. When a client relies on static documentation, it hard-codes URLs such as `/api/v1/rooms` directly into its source code. If the server team renames or restructures a path, every client application breaks and must be updated and redeployed. With HATEOAS, the client begins from a single well-known entry point — in this case `GET /api/v1` — and discovers all other resource URIs from the `_links` section of each response. The client follows links rather than constructing URLs from memory.

Concrete benefits include:

**Discoverability:** A developer exploring the API for the first time can navigate the entire resource graph from the root endpoint without consulting any external document. Each response tells them what they can do next.

**Reduced brittleness:** Server-side refactoring — renaming paths, adding versioned routes, restructuring hierarchies — does not break clients that navigate by links rather than hard-coded strings.

**Evolvability:** New resource links can be added to responses over time without requiring client updates, enabling independent deployment of server and client teams.

In this implementation, the discovery endpoint at `GET /api/v1` returns a `_links` map containing entries for `self`, `rooms`, and `sensors`, giving clients everything they need to navigate the full API programmatically.

---

### Part 2.1 — Returning IDs vs Full Objects in List Responses

**Question: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.**

**Returning only IDs** produces the smallest possible payload — each item is a short string identifier. This is ideal when the client only needs to know how many resources exist, is building a select-list of options, or will fetch details selectively for a small subset. The critical downside is the **N+1 request problem**: a client that needs details for all rooms must make one list request plus one `GET /rooms/{id}` call per room. Across a slow or high-latency network, this compounds dramatically. For a campus with 500 rooms, that is 501 HTTP round-trips just to render a dashboard.

**Returning full objects** is more bandwidth-intensive per response, since each item carries all fields. However, it eliminates the N+1 problem entirely. A client rendering a facilities overview page can display names, capacities, and sensor assignments from a single network call. For a campus management context — where operators routinely need to see all rooms and their current state simultaneously — this is the correct trade-off.

This implementation returns full room objects by default. For very large datasets, the appropriate extension would be **pagination** using query parameters such as `?page=1&size=20`, which combines the benefits of both approaches: complete objects per page without unbounded payload size.

---

### Part 2.2 — Idempotency of DELETE

**Question: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.**

Yes, the DELETE operation is idempotent in this implementation, in accordance with RFC 7231 Section 4.2.2 which defines idempotency as: making multiple identical requests produces the **same server state** as making it once.

The behaviour across repeated identical DELETE calls on a room is:

- **First DELETE** on `/api/v1/rooms/ENG-201` (with no sensors): the room is removed from the `InMemoryStore`. The server returns `204 No Content`.
- **Second identical DELETE** on `/api/v1/rooms/ENG-201`: the room no longer exists in the store. The server returns `404 Not Found`.

The server state after both calls is identical — the room is absent in both cases. The HTTP response status code differs between calls (`204` then `404`), but RFC 7231 explicitly states that idempotency is a property of **server state effects**, not of the HTTP response itself.

This matters practically because HTTP clients, proxies, and load balancers may retry requests on network failure. An idempotent DELETE can be safely retried without risk of unintended side effects — the system will not enter a corrupted or inconsistent state regardless of how many times the request is sent.

Contrast this with POST, which is explicitly non-idempotent: two identical `POST /rooms` requests would attempt to create two rooms with the same ID, with the second receiving `409 Conflict`.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

**Question: Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml, when @Consumes(APPLICATION_JSON) is declared. How does JAX-RS handle this mismatch?**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation instructs the JAX-RS runtime to match the annotated resource method **only** when the incoming request carries a `Content-Type: application/json` header. Content negotiation happens at the framework level, before the method body is ever executed.

If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime — Jersey in this implementation — will find no method that declares `@Consumes` for that media type and will automatically return **HTTP 415 Unsupported Media Type** without invoking any application code. No custom error handling is needed; the container enforces this contract entirely.

This has several important consequences:

**Security:** The application is never exposed to potentially malicious or malformed non-JSON payloads. A client cannot bypass JSON validation by sending XML or plain text.

**Correctness:** Without the annotation, Jersey would attempt to deserialise any content body regardless of type. If the body is not valid JSON, Jackson would throw a `JsonParseException` at deserialisation time. Without a specific mapper for that exception, the global `ExceptionMapper<Throwable>` would catch it and return a 500, which is less informative and less correct than 415.

**Client guidance:** A 415 response is immediately actionable — it tells the client precisely what is wrong with the request format, not that something unknown failed on the server.

---

### Part 3.2 — @QueryParam vs Path-Based Filtering

**Question: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?**

The distinction maps directly to a fundamental REST principle: **path parameters identify resources; query parameters modify the view of a resource**.

**Path-based filtering** (`/api/v1/sensors/type/CO2`) treats "type equals CO2" as part of the resource identity, implying there is a resource at the path `/sensors/type` containing sub-resources named by sensor type. This is semantically incorrect — there is only one sensors collection; the type is a filter applied to it, not a resource in its own right. It also creates several practical problems:

- **No optionality:** The path either has the type segment or it does not — there is no way to express "return all sensors" from the same URL structure.
- **Poor composability:** Adding a second filter (e.g., status = ACTIVE) would require inventing a new URL segment convention such as `/sensors/type/CO2/status/ACTIVE`, which becomes unwieldy and non-standard.
- **Caching confusion:** Different paths imply different resources. CDNs and proxies may cache `/sensors` and `/sensors/type/CO2` independently, leading to stale data inconsistencies.

**Query parameter filtering** (`/api/v1/sensors?type=CO2`) is superior for four concrete reasons:

1. **Optionality:** Omitting `?type` returns all sensors. The resource identity — `/api/v1/sensors` — remains stable regardless of filter state.
2. **Composability:** Multiple filters combine naturally without changing the URL structure: `?type=CO2&status=ACTIVE&minValue=400`.
3. **Semantic correctness:** The sensors collection is consistently identified by `/api/v1/sensors`. Filters are metadata about the request, not identifiers of a resource.
4. **Standards alignment:** HTTP/1.1 and REST conventions treat query strings as modifiers of the requested representation, which is precisely the semantics of filtering.

---

### Part 4.1 — Sub-Resource Locator Pattern

**Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?**

The Sub-Resource Locator pattern is a JAX-RS mechanism where a resource method carries no HTTP method annotation but returns an object instance. The JAX-RS runtime then continues path matching against that returned object, effectively delegating further request handling to a separate class.

In this implementation, `SensorResource.readingsSubResource()` is annotated only with `@Path("/{sensorId}/readings")` and returns a `new SensorReadingResource(sensorId)`. Jersey continues routing — matching `@GET` or `@POST` — against that returned instance.

The architectural benefits are significant:

**Single Responsibility:** `SensorResource` is responsible exclusively for sensor lifecycle management — listing, creating, retrieving, and deleting sensors. `SensorReadingResource` is entirely focused on reading history. Neither class is polluted with the other's concerns. This is a direct application of the Single Responsibility Principle from SOLID design.

**Independent testability:** Each class can be unit-tested in isolation with no dependency on the full JAX-RS runtime. A test for `SensorReadingResource` can simply instantiate it with a sensorId and call methods directly.

**Maintainability at scale:** A real campus API might have dozens of resource types and multiple levels of nesting — rooms containing sensors containing readings containing alerts. Defining every path in a single monolithic controller class would produce a file of thousands of lines where individual endpoint logic is impossible to locate or reason about. The locator pattern keeps each file cohesive and small.

**Context injection:** The locator method receives path parameters — `sensorId` in this case — and passes them to the sub-resource constructor. The sub-resource always has the context it needs without querying global state.

**Parallel team development:** In a real project, different developers can own `SensorResource` and `SensorReadingResource` simultaneously without merge conflicts, because the classes are entirely separate files.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Reference

**Question: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**

HTTP status codes carry precise semantic definitions established in RFC 7231 (general codes) and RFC 4918 (422):

**404 Not Found** means the **request URI** could not be resolved to a known server resource. When a client sends `POST /api/v1/sensors`, the URI `/api/v1/sensors` is perfectly valid and found — the Jersey servlet processes it correctly and reaches the `createSensor()` method. Returning 404 would mislead the client into believing the endpoint itself does not exist, prompting them to check their URL. This is factually incorrect and sends the client in the wrong diagnostic direction.

**422 Unprocessable Entity** means: "The server understands the content type of the request entity and the syntax of the request entity is correct, but it was unable to process the contained instructions." In this scenario, the JSON body `{"roomId":"FAKE-999",...}` is syntactically valid — it is parseable JSON with correct field names. The semantic problem is that the value of `roomId` references a room that does not exist in the system — a business rule violation, not a syntax error.

The practical distinction matters enormously for client developers:

- A 404 tells the client: "your URL is wrong — check the path"
- A 422 tells the client: "your URL is correct and we received your JSON, but the data inside it violates a constraint — fix the roomId value"

This precision enables clients to implement correct, targeted error handling. A client receiving 422 knows to prompt the user to select a valid room, not to re-check the API endpoint address. It also aligns with the principle that each status code should provide actionable, unambiguous information.

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

**Question: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**

Exposing raw Java stack traces to external consumers constitutes an **Information Disclosure** vulnerability, catalogued as CWE-209 (Generation of Error Message Containing Sensitive Information). It violates the security principle of least privilege — external parties should receive the minimum information necessary, which in the case of an error is simply that something went wrong.

A stack trace can provide an attacker with the following intelligence:

**1. Application architecture and file paths:** Lines such as `at uk.ac.westminster.smartcampus.resources.SensorResource.createSensor(SensorResource.java:67)` reveal the exact package hierarchy, class names, method names, and source file line numbers. An attacker now knows the internal structure of the codebase without access to the source.

**2. Library names and exact versions:** Stack frames from third-party dependencies — for example `at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:235)` — identify the framework name and version in use. The attacker can cross-reference these against public CVE databases (NVD, Mitre) to find known, unpatched vulnerabilities in that precise version and craft targeted exploits.

**3. Business logic details:** Method names appearing in the trace — such as `validateRoomCapacity()`, `checkSensorAvailability()`, or `applyPricingRules()` — reveal domain logic and system behaviour that can be used to probe for edge cases or bypass controls.

**4. Database and infrastructure details:** If an exception originates from a database driver or connection pool, the stack trace may contain SQL query fragments, table names, column names, connection pool configuration class names, and internal IP addresses of database servers, enabling SQL injection attempts tuned to the actual schema.

**5. System-level information:** Some exception types include operating system paths, environment variable names, or JVM configuration details in their messages.

The `GlobalThrowableExceptionMapper` in this implementation addresses all of these risks. It catches every unhandled `Throwable`, logs the complete stack trace server-side where only authorised personnel have access, and returns a generic `500 Internal Server Error` JSON body containing only a safe generic message. The external consumer learns nothing about internal structure, and the development team retains full diagnostic capability through server logs.
