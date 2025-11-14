# Module Echo

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Quarkus](https://img.shields.io/badge/Quarkus-powered-blue.svg)](https://quarkus.io/)

A lightweight gRPC-based echo module for the Pipestream document processing pipeline. This module serves as both a testing/validation tool and a reference implementation for building Pipestream pipeline modules.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)

## Overview

Module Echo is a simple yet powerful Pipestream module that receives documents, enriches them with metadata, and echoes them back. It demonstrates the core patterns and practices for building Pipestream pipeline modules while providing a valuable tool for testing and validating pipeline configurations.

### Use Cases

- **Pipeline Testing**: Validate pipeline connectivity and data flow
- **Metadata Enrichment**: Add processing timestamps and tracking information
- **Health Monitoring**: Test module registration and health check mechanisms
- **Reference Implementation**: Learn how to build Pipestream modules
- **Development**: Use as a base for creating new pipeline modules

## Features

### Core Functionality

- **Document Echo Processing**: Receives PipeDoc documents and returns them with added metadata
- **Metadata Enrichment**: Automatically adds processing timestamps, module version, and pipeline context
- **gRPC Service**: High-performance Protocol Buffers-based communication
- **Service Registration**: Automatic registration with Pipestream service registry
- **Health Checks**: Built-in health monitoring and validation
- **Processing Buffer Support**: Optional buffering for high-throughput scenarios

### Technical Features

- **Reactive Programming**: Built on Mutiny for non-blocking, asynchronous operations
- **Service Discovery**: Consul-based service discovery and registration
- **Large Message Support**: Handles messages up to 2GB in size
- **Comprehensive Logging**: Detailed debug logging for troubleshooting
- **RESTful Health Endpoints**: Standard health and readiness checks
- **OpenAPI Documentation**: Auto-generated API documentation via Swagger UI

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────┐
│          Pipestream Pipeline                │
│  ┌─────────────────────────────────────┐   │
│  │     Module Process Request          │   │
│  │  (PipeDoc + ServiceMetadata)        │   │
│  └──────────────┬──────────────────────┘   │
│                 │                            │
│                 ▼                            │
│  ┌─────────────────────────────────────┐   │
│  │      Echo Service (gRPC)            │   │
│  │  ┌──────────────────────────────┐   │   │
│  │  │  processData()               │   │   │
│  │  │  - Receive document          │   │   │
│  │  │  - Add metadata tags         │   │   │
│  │  │  - Return enriched doc       │   │   │
│  │  └──────────────────────────────┘   │   │
│  │                                      │   │
│  │  ┌──────────────────────────────┐   │   │
│  │  │  getServiceRegistration()    │   │   │
│  │  │  - Register with platform    │   │   │
│  │  │  - Perform health checks     │   │   │
│  │  └──────────────────────────────┘   │   │
│  │                                      │   │
│  │  ┌──────────────────────────────┐   │   │
│  │  │  testProcessData()           │   │   │
│  │  │  - Test mode processing      │   │   │
│  │  │  - Generate test documents   │   │   │
│  │  └──────────────────────────────┘   │   │
│  └─────────────────────────────────────┘   │
│                 │                            │
│                 ▼                            │
│  ┌─────────────────────────────────────┐   │
│  │     Module Process Response          │   │
│  │  (Enriched PipeDoc + Logs)          │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### Technology Stack

- **Framework**: Quarkus 3.x (Supersonic Subatomic Java)
- **gRPC**: High-performance RPC framework
- **Reactive**: SmallRye Mutiny for reactive streams
- **Service Discovery**: Consul integration via Stork
- **Build Tool**: Gradle with version catalog
- **JVM**: Java 21 (LTS)
- **Protocol Buffers**: v3 for data serialization

### Processing Flow

1. **Request Reception**: gRPC server receives `ModuleProcessRequest`
2. **Document Extraction**: Extract PipeDoc and metadata from request
3. **Tag Building**: Create or enhance SearchMetadata tags with:
   - `processed_by_echo`: Module identifier
   - `echo_timestamp`: Processing timestamp (ISO-8601)
   - `echo_module_version`: Module version
   - `echo_stream_id`: Pipeline stream identifier (if available)
   - `echo_step_name`: Pipeline step name (if available)
4. **Document Assembly**: Rebuild PipeDoc with enriched metadata
5. **Response Creation**: Build `ModuleProcessResponse` with success status and logs
6. **Return**: Send enriched document back to pipeline

## Quick Start

### Prerequisites

- Java 21 or later
- Gradle 8.x (included via wrapper)
- Docker (optional, for containerized deployment)
- Consul (for service discovery, can use dev services)

### Building the Module

```bash
# Clone the repository
git clone https://github.com/ai-pipestream/module-echo.git
cd module-echo

# Build the project
./gradlew build

# Run tests
./gradlew test
```

### Running in Development Mode

```bash
# Start in dev mode with hot-reload
./gradlew quarkusDev
```

The module will be available at:
- gRPC: `localhost:39000`
- Health: `http://localhost:39000/echo/health`
- Swagger UI: `http://localhost:39000/echo/swagger-ui`

### Running in Production

```bash
# Build the uber-jar
./gradlew quarkusBuild

# Run the application
java -jar build/quarkus-app/quarkus-run.jar
```

### Docker Deployment

```bash
# Build Docker image
./gradlew build -Dquarkus.container-image.build=true

# Run container
docker run -p 39000:39000 \
  -e CONSUL_HOST=consul \
  -e CONSUL_PORT=8500 \
  ai.pipestream.module/module-echo:latest
```

## Configuration

### Core Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.application.name` | `echo` | Module identifier |
| `quarkus.http.port` | `39000` | HTTP/gRPC server port |
| `quarkus.http.host` | `0.0.0.0` | Server bind address |
| `quarkus.http.root-path` | `/echo` | Base context path |
| `module.registration.enabled` | `true` | Enable auto-registration |
| `processing.buffer.enabled` | `false` | Enable processing buffer |

### Service Discovery (Consul)

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.stork.registration-service.service-discovery.type` | `consul` | Discovery mechanism |
| `quarkus.stork.registration-service.service-discovery.consul-host` | `consul` | Consul host |
| `quarkus.stork.registration-service.service-discovery.consul-port` | `8500` | Consul port |

### gRPC Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.grpc.server.use-separate-server` | `false` | Share HTTP/gRPC port |
| `quarkus.grpc.server.max-inbound-message-size` | `2147483647` | Max message size (2GB) |

### Environment Variables

- `CONSUL_HOST`: Consul server hostname (default: `consul`)
- `CONSUL_PORT`: Consul server port (default: `8500`)
- `GPG_SIGNING_KEY`: GPG key for artifact signing (publishing only)
- `GPG_SIGNING_PASSPHRASE`: GPG passphrase (publishing only)
- `MAVEN_CENTRAL_USERNAME`: Maven Central credentials (publishing only)
- `MAVEN_CENTRAL_PASSWORD`: Maven Central credentials (publishing only)

### Processing Buffer

Enable processing buffer for high-throughput scenarios:

```properties
processing.buffer.enabled=true
```

This enables the `@ProcessingBuffered` annotation behavior for batching and optimized processing.

## API Documentation

### gRPC Service: PipeStepProcessor

The module implements the `PipeStepProcessor` service with three main RPC methods:

#### processData

Processes a document and returns an enriched version with metadata.

**Request**: `ModuleProcessRequest`
```protobuf
message ModuleProcessRequest {
  PipeDoc document = 1;
  ServiceMetadata metadata = 2;
}
```

**Response**: `ModuleProcessResponse`
```protobuf
message ModuleProcessResponse {
  bool success = 1;
  PipeDoc outputDoc = 2;
  repeated string processorLogs = 3;
  ErrorDetails errorDetails = 4;
}
```

**Added Metadata Tags**:
- `processed_by_echo`: Application name
- `echo_timestamp`: ISO-8601 timestamp
- `echo_module_version`: Version string (1.0.0)
- `echo_stream_id`: Stream identifier (if present in request)
- `echo_step_name`: Pipeline step name (if present in request)

#### getServiceRegistration

Registers the module with the Pipestream platform and performs health checks.

**Request**: `RegistrationRequest`
```protobuf
message RegistrationRequest {
  ModuleProcessRequest testRequest = 1;  // Optional test payload
}
```

**Response**: `ServiceRegistrationMetadata`
```protobuf
message ServiceRegistrationMetadata {
  string moduleName = 1;
  string version = 2;
  string displayName = 3;
  string description = 4;
  repeated string tags = 5;
  bool healthCheckPassed = 6;
  string healthCheckMessage = 7;
  // ... additional fields
}
```

**Registration Metadata**:
- Module Name: `echo`
- Version: `1.0.0`
- Display Name: `Echo Service`
- Owner: `Rokkon Team`
- Tags: `pipeline-module`, `echo`, `processor`

#### testProcessData

Test version of processData that generates test documents if none provided.

**Request**: `ModuleProcessRequest` (optional document)

**Response**: `ModuleProcessResponse` with `[TEST]` prefixed logs

### REST Endpoints

#### Health Check
```bash
GET http://localhost:39000/echo/health
```

Response:
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Echo Service",
      "status": "UP"
    }
  ]
}
```

#### OpenAPI/Swagger UI
```
http://localhost:39000/echo/swagger-ui
```

Interactive API documentation and testing interface.

## Development

### Project Structure

```
module-echo/
├── src/
│   ├── main/
│   │   ├── java/ai/pipestream/module/echo/
│   │   │   └── EchoServiceImpl.java          # Main service implementation
│   │   └── resources/
│   │       └── application.properties         # Configuration
│   ├── test/
│   │   └── java/ai/pipestream/module/echo/
│   │       ├── EchoServiceTest.java          # Unit tests
│   │       ├── EchoServiceTestBase.java      # Test base class
│   │       ├── EchoServiceNoRegistrationTest.java
│   │       ├── ProcessingBufferAccessTest.java
│   │       └── ...                            # Test profiles and utilities
│   └── integrationTest/
│       └── java/ai/pipestream/module/echo/
│           ├── EchoServiceIT.java            # Integration tests
│           └── EchoServiceGrpcIT.java        # gRPC integration tests
├── build.gradle                               # Build configuration
├── settings.gradle                            # Gradle settings
└── README.md                                  # This file
```

### Building from Source

```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Build Quarkus runner jar
./gradlew quarkusBuild

# Build Docker image
./gradlew build -Dquarkus.container-image.build=true
```

### Code Style

The project follows standard Java conventions:
- Indentation: 4 spaces
- Line length: 120 characters
- Encoding: UTF-8
- Java version: 21

### Dependencies

The module uses the Pipestream BOM (Bill of Materials) for dependency management:

```gradle
implementation platform("ai.pipestream:pipeline-bom:${pipelineBomVersion}")
```

Key dependencies:
- `quarkus-grpc`: gRPC server and client
- `quarkus-smallrye-health`: Health checks
- `quarkus-smallrye-stork`: Service discovery
- `pipestream-api`: Pipestream annotations and interfaces
- `pipestream-grpc-stubs`: Protocol Buffer definitions
- `pipestream-data-util`: Data utilities and factories

## Testing

### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests EchoServiceTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Integration Tests

```bash
# Run integration tests
./gradlew integrationTest

# Run all tests (unit + integration)
./gradlew check
```

### Test Profiles

The module includes several test profiles for different scenarios:

- **Default**: Standard testing with service registration enabled
- **NoRegistration**: Testing without service registration (`NoRegistrationTestProfile`)
- **ProcessingBuffer**: Testing with processing buffer enabled (`ProcessingBufferEnabledTestProfile`)

### Test Coverage

Key test classes:
- `EchoServiceTest`: Core service functionality
- `EchoServiceNoRegistrationTest`: No-registration mode
- `ProcessingBufferAccessTest`: Buffer processing validation
- `EchoServiceIT`: End-to-end integration tests
- `EchoServiceGrpcIT`: gRPC protocol tests

### Manual Testing with grpcurl

```bash
# Test service registration
grpcurl -plaintext -d '{}' localhost:39000 \
  ai.pipestream.data.module.PipeStepProcessor/GetServiceRegistration

# Test document processing (requires valid PipeDoc)
grpcurl -plaintext -d '{"document":{"docId":"test-123"}}' localhost:39000 \
  ai.pipestream.data.module.PipeStepProcessor/ProcessData
```

## Deployment

### Kubernetes Deployment

Example Kubernetes manifest:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: module-echo
spec:
  replicas: 3
  selector:
    matchLabels:
      app: module-echo
  template:
    metadata:
      labels:
        app: module-echo
    spec:
      containers:
      - name: echo
        image: ai.pipestream.module/module-echo:latest
        ports:
        - containerPort: 39000
          name: grpc
        env:
        - name: CONSUL_HOST
          value: "consul.default.svc.cluster.local"
        - name: CONSUL_PORT
          value: "8500"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
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
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: module-echo
spec:
  selector:
    app: module-echo
  ports:
  - port: 39000
    targetPort: 39000
    name: grpc
```

### Docker Compose

```yaml
version: '3.8'
services:
  module-echo:
    image: ai.pipestream.module/module-echo:latest
    ports:
      - "39000:39000"
    environment:
      - CONSUL_HOST=consul
      - CONSUL_PORT=8500
    depends_on:
      - consul
    networks:
      - pipestream

  consul:
    image: consul:latest
    ports:
      - "8500:8500"
    networks:
      - pipestream

networks:
  pipestream:
    driver: bridge
```

### Production Considerations

1. **Memory**: Allocate at least 1GB heap, 4GB recommended for large messages
2. **CPU**: 2+ cores recommended for production workloads
3. **Network**: Ensure Consul connectivity for service discovery
4. **Monitoring**: Enable metrics and logging aggregation
5. **Scaling**: Module is stateless and can be horizontally scaled

## Publishing

### Maven Local

```bash
./gradlew publishToMavenLocal
```

### GitHub Packages

```bash
export GITHUB_TOKEN=your_token
export GITHUB_ACTOR=your_username
./gradlew publish
```

### Maven Central

```bash
export GPG_SIGNING_KEY=your_key
export GPG_SIGNING_PASSPHRASE=your_passphrase
export MAVEN_CENTRAL_USERNAME=your_username
export MAVEN_CENTRAL_PASSWORD=your_password
./gradlew publishAllPublicationsToCentralPortal
```

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add/update tests as needed
5. Ensure all tests pass (`./gradlew check`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Coding Standards

- Follow Java naming conventions
- Add Javadoc comments for public APIs
- Write unit tests for new functionality
- Keep methods focused and concise
- Use meaningful variable names

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues, questions, or contributions:
- GitHub Issues: [https://github.com/ai-pipestream/module-echo/issues](https://github.com/ai-pipestream/module-echo/issues)
- Documentation: [Pipestream Documentation](https://github.com/ai-pipestream/module-echo)

## Acknowledgments

- Built with [Quarkus](https://quarkus.io/)
- Part of the [Pipestream](https://github.com/ai-pipestream) ecosystem
- Developed by the Rokkon Team
