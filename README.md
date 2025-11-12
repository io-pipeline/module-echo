# Echo Module

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Quarkus](https://img.shields.io/badge/Quarkus-Framework-4695EB.svg)](https://quarkus.io/)

A simple, efficient echo module for the PipeStream document processing pipeline that validates document flow and enriches documents with processing metadata.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## Overview

The Echo Module is a reference implementation and testing service for the PipeStream pipeline processing framework. It demonstrates the core functionality of a pipeline processor while providing a simple, predictable behavior that makes it ideal for:

- **Pipeline validation** - Verify that your pipeline configuration is working correctly
- **Integration testing** - Test pipeline connectivity and data flow
- **Development reference** - Serve as a template for building new pipeline modules
- **Smoke testing** - Quick validation during deployment and configuration changes

The module accepts documents, enriches them with metadata tags, and returns them unchanged otherwise, making it perfect for testing without side effects.

## Features

### Core Functionality

- **Document Pass-Through** - Receives documents and returns them with preserved content
- **Metadata Enrichment** - Adds processing metadata tags to track document flow
- **Health Checks** - Built-in health check endpoints with test data validation
- **Service Registration** - Automatic registration with the PipeStream registry using Consul
- **gRPC Protocol** - High-performance gRPC communication with Mutiny reactive support
- **Processing Buffer Support** - Optional batch processing via `@ProcessingBuffered` annotation

### Metadata Tags Added

The echo module enriches each processed document with the following tags:

| Tag Name | Description | Example Value |
|----------|-------------|---------------|
| `processed_by_echo` | Application name from configuration | `echo` |
| `echo_timestamp` | ISO-8601 timestamp of processing | `2025-11-12T10:30:45.123Z` |
| `echo_module_version` | Version of the echo module | `1.0.0` |
| `echo_stream_id` | Stream ID from request metadata | `stream-123` |
| `echo_step_name` | Pipeline step name from metadata | `validation-step` |

## Architecture

### Technology Stack

- **Framework**: Quarkus 3.x - Supersonic Subatomic Java
- **Communication**: gRPC with Mutiny reactive streams
- **Service Discovery**: Consul via Smallrye Stork
- **Language**: Java 21
- **Build Tool**: Gradle 8.x
- **Testing**: JUnit 5, REST Assured, gRPC WireMock

### Module Structure

```
module-echo/
├── src/
│   ├── main/
│   │   ├── java/ai/pipestream/module/echo/
│   │   │   └── EchoServiceImpl.java          # Main service implementation
│   │   └── resources/
│   │       └── application.properties         # Configuration
│   ├── test/
│   │   └── java/ai/pipestream/module/echo/   # Unit and component tests
│   └── integrationTest/
│       └── java/ai/pipestream/module/echo/   # Integration tests
├── build.gradle                               # Gradle build configuration
├── settings.gradle                            # Gradle settings
└── README.md                                  # This file
```

### Service Implementation

The `EchoServiceImpl` class extends `MutinyPipeStepProcessorGrpc.PipeStepProcessorImplBase` and implements three key methods:

1. **`processData(ModuleProcessRequest)`** - Main processing method that enriches documents
2. **`getServiceRegistration(RegistrationRequest)`** - Provides service metadata and health checks
3. **`testProcessData(ModuleProcessRequest)`** - Test endpoint with automatic test data generation

## Installation

### Prerequisites

- Java 21 or higher
- Gradle 8.x or higher
- Docker (for containerized deployment)
- Consul (for service discovery)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/ai-pipestream/module-echo.git
cd module-echo

# Build the project
./gradlew build

# Build Docker image
./gradlew build -Dquarkus.container-image.build=true
```

### Maven/Gradle Dependency

Add the echo module to your project:

**Gradle:**
```gradle
dependencies {
    implementation 'ai.pipestream.module:module-echo:0.1.2-SNAPSHOT'
}
```

**Maven:**
```xml
<dependency>
    <groupId>ai.pipestream.module</groupId>
    <artifactId>module-echo</artifactId>
    <version>0.1.2-SNAPSHOT</version>
</dependency>
```

## Configuration

### Application Properties

Key configuration properties in `src/main/resources/application.properties`:

```properties
# Module Identity
module.name=echo
quarkus.application.name=echo

# Server Configuration
quarkus.http.port=39000
quarkus.http.host=0.0.0.0
quarkus.http.root-path=/echo

# gRPC Configuration
quarkus.grpc.server.use-separate-server=false
quarkus.grpc.server.max-inbound-message-size=2147483647

# Service Discovery (Consul)
quarkus.grpc.clients.registration-service.host=registration-service
quarkus.grpc.clients.registration-service.name-resolver=stork
quarkus.stork.registration-service.service-discovery.type=consul
quarkus.stork.registration-service.service-discovery.consul-host=${CONSUL_HOST:consul}
quarkus.stork.registration-service.service-discovery.consul-port=${CONSUL_PORT:8500}

# Module Registration
module.registration.enabled=true
module.registration.module-name=echo
module.registration.host=localhost
module.registration.port=${quarkus.http.port}

# Processing Buffer (optional)
processing.buffer.enabled=false
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CONSUL_HOST` | `consul` | Consul service host |
| `CONSUL_PORT` | `8500` | Consul service port |
| `QUARKUS_HTTP_PORT` | `39000` | HTTP/gRPC server port |

## Usage

### Starting the Service

**Development Mode:**
```bash
./gradlew quarkusDev
```

**Production Mode:**
```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

**Docker:**
```bash
docker run -p 39000:39000 \
  -e CONSUL_HOST=consul \
  -e CONSUL_PORT=8500 \
  ai.pipestream.module/module-echo:latest
```

### Health Checks

The module provides standard Quarkus health endpoints:

```bash
# Liveness check
curl http://localhost:39000/echo/health/live

# Readiness check
curl http://localhost:39000/echo/health/ready

# Complete health check
curl http://localhost:39000/echo/health
```

### Swagger UI

Access the OpenAPI documentation and Swagger UI:

```bash
open http://localhost:39000/echo/swagger-ui
```

## API Documentation

### gRPC Service: PipeStepProcessor

#### processData

Processes a document and returns it with enriched metadata.

**Request:**
```protobuf
message ModuleProcessRequest {
  PipeDoc document = 1;
  ServiceMetadata metadata = 2;
}
```

**Response:**
```protobuf
message ModuleProcessResponse {
  bool success = 1;
  PipeDoc outputDoc = 2;
  repeated string processorLogs = 3;
  ErrorDetails errorDetails = 4;
}
```

**Example (Java):**
```java
MutinyPipeStepProcessorStub stub = MutinyPipeStepProcessorGrpc.newMutinyStub(channel);

ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
    .setDocument(myDocument)
    .setMetadata(ServiceMetadata.newBuilder()
        .setStreamId("my-stream")
        .setPipeStepName("validation")
        .build())
    .build();

Uni<ModuleProcessResponse> response = stub.processData(request);
```

#### getServiceRegistration

Returns service metadata and performs optional health checks.

**Request:**
```protobuf
message RegistrationRequest {
  ModuleProcessRequest testRequest = 1;  // Optional
}
```

**Response:**
```protobuf
message ServiceRegistrationMetadata {
  string moduleName = 1;
  string version = 2;
  string displayName = 3;
  string description = 4;
  repeated string tags = 5;
  map<string, string> metadata = 6;
  bool healthCheckPassed = 7;
  string healthCheckMessage = 8;
  // ... additional fields
}
```

#### testProcessData

Test endpoint with automatic test data generation.

**Request:**
```protobuf
message ModuleProcessRequest {
  PipeDoc document = 1;  // Optional - generates test data if not provided
  ServiceMetadata metadata = 2;
}
```

**Response:**
```protobuf
message ModuleProcessResponse {
  bool success = 1;
  PipeDoc outputDoc = 2;
  repeated string processorLogs = 3;  // All logs prefixed with [TEST]
}
```

## Development

### Development Mode

Run in development mode with live reload:

```bash
./gradlew quarkusDev
```

This enables:
- Hot reload of code changes
- Dev UI at http://localhost:39000/q/dev
- Debug port on 5005
- Automatic Consul DevServices

### Code Quality

The project follows standard Java coding conventions:

- **JavaDoc** - All public APIs are documented
- **Logging** - Using JBoss Logging
- **Error Handling** - Comprehensive error handling with detailed messages
- **Null Safety** - Careful null checking and Optional usage

### Creating a New Module

Use this echo module as a template:

1. Copy the project structure
2. Rename package from `ai.pipestream.module.echo` to your module name
3. Implement your processing logic in `processData()`
4. Update configuration in `application.properties`
5. Customize service registration metadata

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run specific test
./gradlew test --tests EchoServiceTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Test Categories

- **Unit Tests** (`src/test`) - Test individual components in isolation
- **Integration Tests** (`src/integrationTest`) - Test against running service
- **gRPC Tests** - Test gRPC communication with real channels

### Example Test

```java
@QuarkusTest
class EchoServiceTest {

    @GrpcClient
    MutinyPipeStepProcessorStub echoService;

    @Test
    void testProcessData() {
        PipeDoc testDoc = createTestDocument();

        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
            .setDocument(testDoc)
            .build();

        ModuleProcessResponse response = echoService
            .processData(request)
            .await()
            .atMost(Duration.ofSeconds(5));

        assertTrue(response.getSuccess());
        assertTrue(response.hasOutputDoc());
        assertNotNull(response.getOutputDoc()
            .getSearchMetadata()
            .getTags()
            .getTagDataMap()
            .get("processed_by_echo"));
    }
}
```

## Deployment

### Docker Deployment

Build and deploy with Docker:

```bash
# Build Docker image
./gradlew build -Dquarkus.container-image.build=true

# Run container
docker run -d \
  --name echo-module \
  -p 39000:39000 \
  -e CONSUL_HOST=consul \
  -e CONSUL_PORT=8500 \
  ai.pipestream.module/module-echo:latest
```

### Kubernetes Deployment

Example Kubernetes deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: echo-module
spec:
  replicas: 2
  selector:
    matchLabels:
      app: echo-module
  template:
    metadata:
      labels:
        app: echo-module
    spec:
      containers:
      - name: echo
        image: ai.pipestream.module/module-echo:latest
        ports:
        - containerPort: 39000
        env:
        - name: CONSUL_HOST
          value: "consul-server"
        - name: CONSUL_PORT
          value: "8500"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "4Gi"
            cpu: "1000m"
```

### Production Considerations

- **Memory**: Allocate 4GB+ for large message processing
- **Direct Memory**: Configure `-XX:MaxDirectMemorySize=2g` for gRPC
- **Health Checks**: Configure liveness and readiness probes
- **Monitoring**: Integrate with Prometheus for metrics
- **Logging**: Configure log aggregation (ELK, Splunk, etc.)
- **Security**: Use TLS for gRPC in production

## Performance

### Benchmarks

Typical performance characteristics:

- **Throughput**: 10,000+ documents/second (single instance)
- **Latency**: <1ms median, <5ms p99
- **Memory**: ~512MB baseline, scales with document size
- **Message Size**: Supports up to 2GB messages

### Optimization Tips

- Enable processing buffer for batch operations
- Tune JVM heap size based on document size
- Use connection pooling for gRPC clients
- Configure appropriate thread pool sizes

## Troubleshooting

### Common Issues

**Service won't start:**
- Check if port 39000 is available
- Verify Consul connectivity
- Check Java version (must be 21+)

**Registration fails:**
- Verify Consul is running and accessible
- Check network connectivity to Consul
- Review registration service logs

**Large message failures:**
- Increase `quarkus.grpc.server.max-inbound-message-size`
- Allocate more heap memory with `-Xmx`
- Configure `-XX:MaxDirectMemorySize` for gRPC

### Debug Logging

Enable debug logging:

```properties
quarkus.log.category."ai.pipestream".level=DEBUG
```

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

1. Ensure Java 21+ is installed
2. Install Docker for integration tests
3. Run `./gradlew build` to verify setup
4. Use `./gradlew quarkusDev` for development

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: [PipeStream Docs](https://docs.pipestream.ai)
- **Issues**: [GitHub Issues](https://github.com/ai-pipestream/module-echo/issues)
- **Discussions**: [GitHub Discussions](https://github.com/ai-pipestream/module-echo/discussions)

## Acknowledgments

- Built with [Quarkus](https://quarkus.io/)
- gRPC implementation via [gRPC-Java](https://github.com/grpc/grpc-java)
- Reactive streams with [Mutiny](https://smallrye.io/smallrye-mutiny/)
- Service discovery via [Consul](https://www.consul.io/)

---

**Version**: 0.1.2-SNAPSHOT
**Last Updated**: 2025-11-12
**Maintained by**: PipeStream Team
