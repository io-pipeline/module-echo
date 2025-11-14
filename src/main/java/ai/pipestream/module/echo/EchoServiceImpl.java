package ai.pipestream.module.echo;

import ai.pipestream.api.annotation.GrpcServiceRegistration;
import ai.pipestream.api.annotation.ProcessingBuffered;
import ai.pipestream.data.module.*;
import ai.pipestream.data.util.proto.PipeDocTestDataFactory;
import ai.pipestream.data.v1.PipeDoc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Echo Service Implementation for Pipestream Pipeline Processing.
 *
 * <p>This service implements the {@link MutinyPipeStepProcessorGrpc.PipeStepProcessorImplBase}
 * interface to provide a simple echo functionality for document processing pipelines. It receives
 * {@link PipeDoc} documents, enriches them with metadata tags, and returns them back to the
 * calling pipeline.
 *
 * <h2>Primary Functions</h2>
 * <ul>
 *   <li><b>Document Processing</b>: Echoes documents back with added metadata tags</li>
 *   <li><b>Service Registration</b>: Registers with the Pipestream service registry</li>
 *   <li><b>Health Checks</b>: Provides health validation through test processing</li>
 *   <li><b>Testing Support</b>: Offers test mode with synthetic data generation</li>
 * </ul>
 *
 * <h2>Metadata Enhancement</h2>
 * <p>During processing, the service adds the following metadata tags to documents:
 * <ul>
 *   <li><code>processed_by_echo</code>: Application name identifier</li>
 *   <li><code>echo_timestamp</code>: ISO-8601 formatted processing timestamp</li>
 *   <li><code>echo_module_version</code>: Module version (currently 1.0.0)</li>
 *   <li><code>echo_stream_id</code>: Pipeline stream identifier (if available)</li>
 *   <li><code>echo_step_name</code>: Pipeline step name (if available)</li>
 * </ul>
 *
 * <h2>Service Registration</h2>
 * <p>The service is automatically registered with the Pipestream platform using the
 * {@link GrpcServiceRegistration} annotation with the following metadata:
 * <ul>
 *   <li>Category: testing</li>
 *   <li>Complexity: simple</li>
 * </ul>
 *
 * <h2>Processing Buffer Support</h2>
 * <p>The service supports optional processing buffering through the {@link ProcessingBuffered}
 * annotation, which can be enabled via the {@code processing.buffer.enabled} configuration
 * property for high-throughput scenarios.
 *
 * <h2>Thread Safety</h2>
 * <p>This service is designed to be thread-safe and can handle concurrent requests using
 * Quarkus's reactive Mutiny framework for non-blocking asynchronous operations.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // The service is automatically instantiated and registered by Quarkus
 * // Client usage through gRPC:
 * ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
 *     .setDocument(pipeDoc)
 *     .setMetadata(serviceMetadata)
 *     .build();
 *
 * Uni<ModuleProcessResponse> response = echoService.processData(request);
 * }</pre>
 *
 * @author Rokkon Team
 * @version 1.0.0
 * @see MutinyPipeStepProcessorGrpc.PipeStepProcessorImplBase
 * @see PipeDoc
 * @see ModuleProcessRequest
 * @see ModuleProcessResponse
 * @since 1.0.0
 */
@GrpcService
@Singleton
@GrpcServiceRegistration(
    metadata = {"category=testing", "complexity=simple"}
)
public class EchoServiceImpl extends MutinyPipeStepProcessorGrpc.PipeStepProcessorImplBase {

    /**
     * Logger instance for the Echo service.
     * Used for debugging and monitoring service operations.
     */
    private static final Logger LOG = Logger.getLogger(EchoServiceImpl.class);

    /**
     * Application name injected from Quarkus configuration.
     * Used as the module identifier in metadata tags and service registration.
     * Corresponds to the {@code quarkus.application.name} property.
     */
    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    /**
     * Factory for generating test PipeDoc instances.
     * Used in test processing mode to create synthetic documents when
     * no document is provided in the request.
     */
    @Inject
    PipeDocTestDataFactory pipeDocTestDataFactory;

    /**
     * Processes a document by echoing it back with enriched metadata.
     *
     * <p>This method is the primary processing endpoint for the echo module. It receives a
     * {@link ModuleProcessRequest} containing a {@link PipeDoc} document and optional
     * {@link ServiceMetadata}, processes the document by adding metadata tags, and returns
     * a {@link ModuleProcessResponse} with the enriched document.
     *
     * <h3>Processing Flow</h3>
     * <ol>
     *   <li>Receives the incoming request and extracts the document</li>
     *   <li>Logs the document ID for debugging purposes</li>
     *   <li>Creates or enhances the document's SearchMetadata with tags:
     *     <ul>
     *       <li>{@code processed_by_echo}: Application name</li>
     *       <li>{@code echo_timestamp}: Current timestamp in ISO-8601 format</li>
     *       <li>{@code echo_module_version}: Module version (1.0.0)</li>
     *       <li>{@code echo_stream_id}: Stream ID from request metadata (if present)</li>
     *       <li>{@code echo_step_name}: Pipeline step name from request metadata (if present)</li>
     *     </ul>
     *   </li>
     *   <li>Rebuilds the document with the enhanced metadata</li>
     *   <li>Creates a success response with the processed document and logging information</li>
     *   <li>Returns the response wrapped in a reactive {@link Uni}</li>
     * </ol>
     *
     * <h3>Processing Buffer Support</h3>
     * <p>This method is annotated with {@link ProcessingBuffered}, which enables optional
     * buffering for high-throughput scenarios. The buffering behavior can be controlled
     * via the {@code processing.buffer.enabled} configuration property.
     *
     * <h3>Reactive Processing</h3>
     * <p>The method returns a {@link Uni} (reactive stream), making it non-blocking and
     * suitable for high-concurrency scenarios. The actual processing is synchronous within
     * the method, but the result is wrapped in a reactive type for seamless integration
     * with Quarkus's reactive architecture.
     *
     * <h3>Error Handling</h3>
     * <p>This implementation always returns a success response. In production modules,
     * error handling should be implemented to catch and report processing failures
     * through the {@link ModuleProcessResponse}'s error details.
     *
     * @param request the processing request containing the document to process and
     *                optional service metadata. Must not be null.
     * @return a {@link Uni} containing the {@link ModuleProcessResponse} with the
     *         enriched document, success status, and processing logs
     * @see ModuleProcessRequest
     * @see ModuleProcessResponse
     * @see ProcessingBuffered
     * @see PipeDoc
     */
    @Override
    @ProcessingBuffered(type = PipeDoc.class, enabled = "${processing.buffer.enabled:false}")
    public Uni<ModuleProcessResponse> processData(ModuleProcessRequest request) {
        LOG.debugf("Echo service received document: %s", 
                 request.hasDocument() ? request.getDocument().getDocId() : "no document");

        // Build response with success status
        ModuleProcessResponse.Builder responseBuilder = ModuleProcessResponse.newBuilder()
                .setSuccess(true)
                .addProcessorLogs("Echo service successfully processed document");

        // If there's a document, add metadata and echo it back
        if (request.hasDocument()) {
            PipeDoc originalDoc = request.getDocument();
            PipeDoc.Builder docBuilder = originalDoc.toBuilder();

            // Get existing search metadata or create new one
            ai.pipestream.data.v1.SearchMetadata.Builder searchMetadataBuilder = 
                originalDoc.hasSearchMetadata() 
                    ? originalDoc.getSearchMetadata().toBuilder()
                    : ai.pipestream.data.v1.SearchMetadata.newBuilder();

            // Add or update tags with processing metadata
            ai.pipestream.data.v1.Tags.Builder tagsBuilder = searchMetadataBuilder.hasTags()
                    ? searchMetadataBuilder.getTags().toBuilder() 
                    : ai.pipestream.data.v1.Tags.newBuilder();

            // Add echo module metadata
            tagsBuilder.putTagData("processed_by_echo", applicationName);
            tagsBuilder.putTagData("echo_timestamp", Instant.now().toString());
            tagsBuilder.putTagData("echo_module_version", "1.0.0");

            // Add request metadata if available
            if (request.hasMetadata()) {
                ServiceMetadata metadata = request.getMetadata();
                if (!metadata.getStreamId().isEmpty()) {
                    tagsBuilder.putTagData("echo_stream_id", metadata.getStreamId());
                }
                if (!metadata.getPipeStepName().isEmpty()) {
                    tagsBuilder.putTagData("echo_step_name", metadata.getPipeStepName());
                }
            }

            // Set the updated tags in search metadata
            searchMetadataBuilder.setTags(tagsBuilder.build());
            
            // Update the document with modified search metadata
            docBuilder.setSearchMetadata(searchMetadataBuilder.build());

            // Set the updated document in the response
            responseBuilder.setOutputDoc(docBuilder.build());
            responseBuilder.addProcessorLogs("Echo service added metadata to document");
        }

        ModuleProcessResponse response = responseBuilder.build();
        LOG.debugf("Echo service returning success: %s", response.getSuccess());

        return Uni.createFrom().item(response);
    }

    /**
     * Handles service registration and health check requests from the Pipestream platform.
     *
     * <p>This method implements the service registration protocol for the Pipestream ecosystem.
     * It provides comprehensive metadata about the echo module and optionally performs health
     * checks by processing a test request if one is provided.
     *
     * <h3>Registration Metadata</h3>
     * <p>The method returns a {@link ServiceRegistrationMetadata} response containing:
     * <ul>
     *   <li><b>Module Information</b>:
     *     <ul>
     *       <li>Module Name: Application name from configuration</li>
     *       <li>Version: 1.0.0</li>
     *       <li>Display Name: "Echo Service"</li>
     *       <li>Description: Brief description of the module's purpose</li>
     *       <li>Owner: "Rokkon Team"</li>
     *     </ul>
     *   </li>
     *   <li><b>Tags</b>: pipeline-module, echo, processor</li>
     *   <li><b>Registration Timestamp</b>: Current time in Protocol Buffers Timestamp format</li>
     *   <li><b>System Information</b>:
     *     <ul>
     *       <li>Server Info: OS name and version</li>
     *       <li>SDK Version: 1.0.0</li>
     *       <li>Implementation Language: Java</li>
     *       <li>JVM Version: Current Java version</li>
     *     </ul>
     *   </li>
     *   <li><b>Health Check Results</b>: Status and message from optional test processing</li>
     * </ul>
     *
     * <h3>Health Check Protocol</h3>
     * <p>If the {@link RegistrationRequest} contains a test request, this method performs
     * a health check by:
     * <ol>
     *   <li>Processing the test request through {@link #processData(ModuleProcessRequest)}</li>
     *   <li>Evaluating the success status of the processing response</li>
     *   <li>Setting appropriate health check status and message in the registration response</li>
     *   <li>Handling any exceptions that occur during health check processing</li>
     * </ol>
     *
     * <p>If no test request is provided, the service is assumed to be healthy and returns
     * a positive health status.
     *
     * <h3>Error Recovery</h3>
     * <p>If the health check fails with an exception, the method recovers gracefully by:
     * <ul>
     *   <li>Logging the error for diagnostics</li>
     *   <li>Setting health check status to failed</li>
     *   <li>Including the exception message in the health check message</li>
     *   <li>Returning a complete registration response (not throwing the exception)</li>
     * </ul>
     *
     * <h3>Use Cases</h3>
     * <ul>
     *   <li><b>Initial Registration</b>: Called when the module first connects to the platform</li>
     *   <li><b>Health Monitoring</b>: Periodically called to verify module health</li>
     *   <li><b>Service Discovery</b>: Provides metadata for service catalogs and routing</li>
     * </ul>
     *
     * @param request the registration request, optionally containing a test processing request
     *                for health validation. Must not be null.
     * @return a {@link Uni} containing the {@link ServiceRegistrationMetadata} with complete
     *         module information, system metadata, and health check results
     * @see ServiceRegistrationMetadata
     * @see RegistrationRequest
     * @see #processData(ModuleProcessRequest)
     */
    @Override
    public Uni<ServiceRegistrationMetadata> getServiceRegistration(RegistrationRequest request) {
        LOG.debug("Echo service registration requested");

        // Build a more comprehensive registration response with metadata
        ServiceRegistrationMetadata.Builder responseBuilder = ServiceRegistrationMetadata.newBuilder()
                .setModuleName(applicationName)
                .setVersion("1.0.0")
                .setDisplayName("Echo Service")
                .setDescription("A simple echo module that returns documents with added metadata")
                .setOwner("Rokkon Team")
                .addTags("pipeline-module")
                .addTags("echo")
                .addTags("processor")
                .setRegistrationTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build());

        // Add server info and SDK version
        responseBuilder
                .setServerInfo(System.getProperty("os.name") + " " + System.getProperty("os.version"))
                .setSdkVersion("1.0.0");

        // Add metadata
        responseBuilder
                .putMetadata("implementation_language", "Java")
                .putMetadata("jvm_version", System.getProperty("java.version"));

        // If test request is provided, perform health check
        if (request.hasTestRequest()) {
            LOG.debug("Performing health check with test request");
            return processData(request.getTestRequest())
                .map(processResponse -> {
                    if (processResponse.getSuccess()) {
                        responseBuilder
                            .setHealthCheckPassed(true)
                            .setHealthCheckMessage("Echo module is healthy and functioning correctly");
                    } else {
                        responseBuilder
                            .setHealthCheckPassed(false)
                            .setHealthCheckMessage("Echo module health check failed: " + 
                                (processResponse.hasErrorDetails() ? 
                                    processResponse.getErrorDetails().toString() : 
                                    "Unknown error"));
                    }
                    return responseBuilder.build();
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.error("Health check failed with exception", error);
                    return responseBuilder
                        .setHealthCheckPassed(false)
                        .setHealthCheckMessage("Health check failed with exception: " + error.getMessage())
                        .build();
                });
        } else {
            // No test request provided, assume healthy
            responseBuilder
                .setHealthCheckPassed(true)
                .setHealthCheckMessage("Service is healthy");
            return Uni.createFrom().item(responseBuilder.build());
        }
    }

    /**
     * Processes documents in test mode with automatic test data generation and enhanced logging.
     *
     * <p>This method provides a specialized version of document processing designed for testing
     * and validation scenarios. It extends the standard {@link #processData(ModuleProcessRequest)}
     * functionality by automatically generating test documents when none are provided and adding
     * test-specific markers to all processing logs.
     *
     * <h3>Test Mode Behavior</h3>
     * <p>The method operates in the following manner:
     * <ol>
     *   <li><b>Document Validation</b>: Checks if the request contains a document</li>
     *   <li><b>Test Data Generation</b>: If no document is provided or request is null:
     *     <ul>
     *       <li>Generates a complex test document using {@link PipeDocTestDataFactory}</li>
     *       <li>Creates synthetic {@link ServiceMetadata} with test identifiers</li>
     *       <li>Builds a complete {@link ModuleProcessRequest} with test data</li>
     *     </ul>
     *   </li>
     *   <li><b>Processing</b>: Delegates to {@link #processData(ModuleProcessRequest)} for
     *       standard document processing with metadata enrichment</li>
     *   <li><b>Log Enhancement</b>: Transforms the response by:
     *     <ul>
     *       <li>Prefixing all existing logs with {@code [TEST]} marker</li>
     *       <li>Adding a final validation log confirming test completion</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <h3>Generated Test Data</h3>
     * <p>When auto-generating test data, the method creates:
     * <ul>
     *   <li><b>Test Document</b>: Complex PipeDoc with ID 10101</li>
     *   <li><b>Test Metadata</b>:
     *     <ul>
     *       <li>Stream ID: "test-stream"</li>
     *       <li>Pipeline Step Name: "test-step"</li>
     *       <li>Pipeline Name: "test-pipeline"</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>Use Cases</h3>
     * <ul>
     *   <li><b>Health Checks</b>: Validate module functionality without external data</li>
     *   <li><b>Integration Testing</b>: Test end-to-end processing with synthetic data</li>
     *   <li><b>Development</b>: Quick validation during development without setting up real pipelines</li>
     *   <li><b>Debugging</b>: Isolate processing logic from data issues using controlled test data</li>
     * </ul>
     *
     * <h3>Response Transformation</h3>
     * <p>All logs in the {@link ModuleProcessResponse} are transformed to include test markers,
     * making it easy to identify test mode operations in log aggregation systems and monitoring
     * dashboards. The transformation preserves all original log content while adding the
     * {@code [TEST]} prefix.
     *
     * <h3>Example Output Logs</h3>
     * <pre>
     * [TEST] Echo service successfully processed document
     * [TEST] Echo service added metadata to document
     * [TEST] Echo module test validation completed successfully
     * </pre>
     *
     * @param request the processing request, optionally containing a document to process.
     *                If null or missing a document, a test document will be auto-generated.
     * @return a {@link Uni} containing the {@link ModuleProcessResponse} with processed document,
     *         success status, and test-marked processing logs
     * @see #processData(ModuleProcessRequest)
     * @see PipeDocTestDataFactory
     * @see ModuleProcessRequest
     * @see ModuleProcessResponse
     */
    @Override
    public Uni<ModuleProcessResponse> testProcessData(ModuleProcessRequest request) {
        LOG.debug("TestProcessData called - executing test version of processing");

        // For test processing, create a test document if none provided
        if (request == null || !request.hasDocument()) {
            PipeDoc testDoc = pipeDocTestDataFactory.createComplexDocument(10101);

            ServiceMetadata testMetadata = ServiceMetadata.newBuilder()
                    .setStreamId("test-stream")
                    .setPipeStepName("test-step")
                    .setPipelineName("test-pipeline")
                    .build();

            request = ModuleProcessRequest.newBuilder()
                    .setDocument(testDoc)
                    .setMetadata(testMetadata)
                    .build();
        }

        // Process normally but with test flag in logs
        return processData(request)
                .onItem().transform(response -> {
                    // Add test marker to logs
                    ModuleProcessResponse.Builder builder = response.toBuilder();
                    for (int i = 0; i < builder.getProcessorLogsCount(); i++) {
                        builder.setProcessorLogs(i, "[TEST] " + builder.getProcessorLogs(i));
                    }
                    builder.addProcessorLogs("[TEST] Echo module test validation completed successfully");
                    return builder.build();
                });
    }
}
