# Echo Module

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Quarkus](https://img.shields.io/badge/Quarkus-Framework-4695EB.svg)](https://quarkus.io/)

A lightweight, pass-through document processor for the PipeStream pipeline framework. The echo module serves as both a reference implementation and a practical tool for testing and validating pipeline configurations.

## What is it?

The echo module is the simplest possible pipeline processor - it receives documents and returns them unchanged, while adding metadata tags that track the document's journey through the pipeline. Think of it as a "hello world" for pipeline development, but with real utility.

**Why does this exist?**
- **Validate pipeline connectivity** - Quickly verify that your pipeline is correctly wired
- **Debug document flow** - Track documents as they move through processing stages
- **Reference implementation** - See how to build a compliant pipeline module
- **Load testing** - Test pipeline throughput without complex processing logic

## Quick Start

### Running Locally

```bash
# Development mode with hot reload
./gradlew quarkusDev

# The service will start on http://localhost:39000/echo
```

The module automatically registers with Consul for service discovery. In development mode, it expects Consul at `localhost:8500`.

### Using in Your Pipeline

Add the echo module to your pipeline configuration as a processing step. Documents passing through will be enriched with metadata tags that track their processing:

```yaml
# Example pipeline configuration
steps:
  - name: validation
    module: echo
    # Documents receive tags like processed_by_echo, echo_timestamp, etc.
```

## How It Works

### Core Behavior

The echo module implements three gRPC endpoints defined in the `PipeStepProcessor` service:

**1. processData** - The main processing endpoint
```java
public Uni<ModuleProcessResponse> processData(ModuleProcessRequest request)
```

This is where documents flow through. The implementation is intentionally simple:

```java
// From EchoServiceImpl.java:107-163
@Override
@ProcessingBuffered(type = PipeDoc.class, enabled = "${processing.buffer.enabled:false}")
public Uni<ModuleProcessResponse> processData(ModuleProcessRequest request) {
    // 1. Receives document and metadata from the request
    // 2. Preserves existing document content unchanged
    // 3. Adds processing metadata tags to SearchMetadata.Tags field
    // 4. Returns enriched document in the response
}
```

**Why this matters:** This demonstrates the contract that all pipeline processors follow - receive a document, process it, return results. The echo module fulfills this contract in the simplest way possible, making it easy to understand the pattern.

**Metadata tags added:**
- `processed_by_echo` - Identifies which service processed the document
- `echo_timestamp` - When processing occurred (ISO-8601 format)
- `echo_module_version` - Module version for tracking
- `echo_stream_id` / `echo_step_name` - Pipeline context (when available)

These tags accumulate as documents flow through multiple echo instances, creating an audit trail.

---

**2. getServiceRegistration** - Service discovery and health checks
```java
public Uni<ServiceRegistrationMetadata> getServiceRegistration(RegistrationRequest request)
```

Returns metadata about the service (name, version, capabilities) and can optionally validate health by processing a test document:

```java
// From EchoServiceImpl.java:135-153
@Override
public Uni<ServiceRegistrationMetadata> getServiceRegistration(RegistrationRequest request) {
    // If request includes a test document:
    //   1. Processes it through processData()
    //   2. Validates the result
    //   3. Sets healthCheckPassed accordingly
    //
    // Returns comprehensive service metadata including:
    //   - Module name, version, capabilities
    //   - System info (OS, JVM version)
    //   - Health check status
}
```

**Why this matters:** The PipeStream framework uses this for automatic service discovery. When the echo module starts, it registers itself with the registry service, making it available for pipeline orchestration. The optional health check ensures the module is functioning correctly before accepting traffic.

---

**3. testProcessData** - Testing endpoint with synthetic data
```java
public Uni<ModuleProcessResponse> testProcessData(ModuleProcessRequest request)
```

A specialized testing endpoint that generates test documents on-demand:

```java
// From EchoServiceImpl.java:202-186
@Override
public Uni<ModuleProcessResponse> testProcessData(ModuleProcessRequest request) {
    // If no document provided, automatically generates a complex test document
    // Processes it through normal processData() flow
    // Prefixes all logs with [TEST] marker
    // Returns result with test validation message
}
```

**Why this matters:** This enables automated health checks without requiring test data to be passed in. Useful for smoke tests during deployment where you want to verify the service works but don't have real documents available.

### Architecture Choices

**Why gRPC?** The PipeStream framework uses gRPC for high-performance, strongly-typed communication between services. This enables efficient streaming of large documents (up to 2GB) with minimal overhead.

**Why Mutiny?** Quarkus's reactive programming model (via Mutiny) allows the echo module to handle many concurrent requests efficiently without blocking threads. The `Uni<T>` return type represents a single asynchronous result.

**Why Consul?** Service discovery via Consul allows pipeline modules to find each other dynamically. No hardcoded hostnames or ports - services register themselves and discover dependencies at runtime.

## Configuration

The module is configured via `src/main/resources/application.properties`. Key settings:

```properties
# Module identity - how other services find this module
module.name=echo
quarkus.application.name=echo

# Network configuration
quarkus.http.port=39000              # HTTP and gRPC server port
quarkus.http.root-path=/echo         # Base path for HTTP endpoints

# Service discovery
quarkus.stork.registration-service.service-discovery.type=consul
quarkus.stork.registration-service.service-discovery.consul-host=${CONSUL_HOST:consul}

# Module registration
module.registration.enabled=true     # Auto-register on startup
module.registration.module-name=echo
```

**Configuration philosophy:** The echo module follows "convention over configuration" - it works out of the box with sensible defaults. Override only what you need via environment variables or property overrides.

**Environment variables:**
- `CONSUL_HOST` - Consul server hostname (default: `consul`)
- `CONSUL_PORT` - Consul server port (default: `8500`)

## Development

### Project Structure

```
src/main/java/ai/pipestream/module/echo/
└── EchoServiceImpl.java              # The entire module (188 lines)

src/main/resources/
└── application.properties             # Configuration

src/test/java/                         # Unit and component tests
src/integrationTest/java/              # Integration tests against packaged app
```

**Why so simple?** The echo module intentionally demonstrates how little code is needed for a functioning pipeline module. The entire business logic is in a single class with three methods. This makes it an excellent starting point for building your own modules.

### Building a Custom Module

To create your own pipeline module using echo as a template:

**1. Copy the structure:**
```bash
git clone https://github.com/ai-pipestream/module-echo.git my-module
cd my-module
```

**2. Update module identity:**
```properties
# In application.properties
module.name=my-module
quarkus.application.name=my-module
module.registration.module-name=my-module
```

**3. Implement your processing logic:**
```java
@Override
public Uni<ModuleProcessResponse> processData(ModuleProcessRequest request) {
    PipeDoc inputDoc = request.getDocument();

    // Your processing logic here
    // Examples: extract text, enrich metadata, call external APIs, etc.

    PipeDoc outputDoc = /* your processed document */;

    return Uni.createFrom().item(
        ModuleProcessResponse.newBuilder()
            .setSuccess(true)
            .setOutputDoc(outputDoc)
            .build()
    );
}
```

**Key patterns to follow:**
- Always return a `ModuleProcessResponse` with success status
- Preserve document ID and core structure
- Add your metadata to `SearchMetadata.Tags` (don't remove existing tags)
- Log significant operations for debugging
- Handle errors gracefully and set `success=false` with error details

### Testing

The echo module includes comprehensive tests demonstrating different testing approaches:

```bash
# Unit tests - fast, mocked dependencies
./gradlew test

# Integration tests - real gRPC channels, packaged app
./gradlew integrationTest
```

**Test structure:**
- `EchoServiceTestBase` - Shared test logic for both unit and integration tests
- `EchoServiceTest` - Unit tests with Quarkus DI
- `EchoServiceIT` - Integration tests with real gRPC client

**Testing philosophy:** The base test class contains the actual test logic, while concrete test classes provide the service instance (either injected or via gRPC channel). This ensures the same tests run in both environments.

## Deployment

### Docker

The Gradle build can generate a Docker image:

```bash
./gradlew build -Dquarkus.container-image.build=true
```

Run the container:

```bash
docker run -p 39000:39000 \
  -e CONSUL_HOST=consul \
  ai.pipestream.module/module-echo:latest
```

**What happens at runtime:**
1. Service starts and binds to port 39000
2. Connects to Consul at `CONSUL_HOST:8500`
3. Registers itself as an available pipeline module
4. Begins accepting gRPC requests
5. Health endpoints available at `/echo/health/*`

### Kubernetes

For production deployments, the echo module needs:

**Resource requirements:**
- **CPU:** 250m minimum, 1000m limit (module is lightweight)
- **Memory:** 512Mi minimum, 4Gi limit (2GB+ needed for large message support)
- **Direct Memory:** Configure `-XX:MaxDirectMemorySize=2g` for gRPC buffer

**Health checks:**
```yaml
livenessProbe:
  httpGet:
    path: /echo/health/live
    port: 39000
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /echo/health/ready
    port: 39000
  initialDelaySeconds: 5
  periodSeconds: 5
```

**Why these settings?** The echo module supports processing very large documents (up to 2GB via gRPC). While the processing itself is trivial, the memory footprint scales with document size. The health endpoints use Quarkus's built-in health check framework.

## Monitoring and Operations

### Health Endpoints

```bash
# Liveness - is the service running?
curl http://localhost:39000/echo/health/live

# Readiness - is the service ready to accept traffic?
curl http://localhost:39000/echo/health/ready
```

**Liveness vs Readiness:**
- Liveness checks if the process is alive (restart if failing)
- Readiness checks if the service can handle requests (remove from load balancer if failing)

### Logs

Enable debug logging to see document flow:

```properties
quarkus.log.category."ai.pipestream".level=DEBUG
```

Example log output:
```
DEBUG [ai.pi.mo.ec.EchoServiceImpl] Echo service received document: doc-12345
DEBUG [ai.pi.mo.ec.EchoServiceImpl] Echo service returning success: true
```

### Common Issues

**Port already in use:**
The default port 39000 may conflict with other services. Override with:
```bash
./gradlew quarkusDev -Dquarkus.http.port=39001
```

**Can't connect to Consul:**
In development, Consul DevServices should start automatically. If you see connection errors, verify Docker is running (DevServices requires Docker).

**Large message failures:**
If processing fails for documents >100MB, you may need to increase the max message size:
```properties
quarkus.grpc.server.max-inbound-message-size=2147483647  # 2GB
```

Also ensure JVM has sufficient heap: `-Xmx4g`

## API Reference

Complete API documentation available via JavaDoc on all public methods in `EchoServiceImpl.java`. The source code is well-documented with:
- Method purpose and behavior
- Parameter descriptions
- Return value semantics
- Usage examples and patterns

For interactive API exploration, run the service and visit:
```
http://localhost:39000/echo/swagger-ui
```

## Contributing

Contributions welcome! The echo module is intentionally simple to make it easy to understand and modify. When contributing:

1. Maintain the simplicity - this is a reference implementation
2. Add tests for any behavior changes
3. Update JavaDoc for public API changes
4. Keep the README focused on "why" not just "what"

## License

MIT License - see [LICENSE](LICENSE) file.

---

**Version:** 0.1.2-SNAPSHOT
**Maintained by:** PipeStream Team
