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
 * Echo Service implementation for the PipeStream pipeline processing framework.
 * <p>
 * This module provides a simple echo service that accepts documents, adds metadata tags,
 * and returns the enriched document. It serves as a reference implementation and testing
 * module for the PipeStream document processing pipeline.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Document pass-through with metadata enrichment</li>
 *   <li>Automatic service registration with health checks</li>
 *   <li>Processing buffer support for batch operations</li>
 *   <li>Test mode with sample data generation</li>
 * </ul>
 *
 * <h2>Metadata Added:</h2>
 * The service adds the following tags to each processed document:
 * <ul>
 *   <li><code>processed_by_echo</code> - The application name</li>
 *   <li><code>echo_timestamp</code> - ISO-8601 timestamp of processing</li>
 *   <li><code>echo_module_version</code> - Version of the echo module</li>
 *   <li><code>echo_stream_id</code> - Stream ID from request metadata (if available)</li>
 *   <li><code>echo_step_name</code> - Pipeline step name from request metadata (if available)</li>
 * </ul>
 *
 * @author PipeStream Team
 * @version 1.0.0
 * @see MutinyPipeStepProcessorGrpc.PipeStepProcessorImplBase
 * @since 1.0.0
 */
@GrpcService
@Singleton
@GrpcServiceRegistration(
    metadata = {"category=testing", "complexity=simple"}
)
public class EchoServiceImpl extends MutinyPipeStepProcessorGrpc.PipeStepProcessorImplBase {

    private static final Logger LOG = Logger.getLogger(EchoServiceImpl.class);

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @Inject
    PipeDocTestDataFactory pipeDocTestDataFactory;

    /**
     * Processes a document by echoing it back with enriched metadata.
     * <p>
     * This method accepts a {@link ModuleProcessRequest} containing a document and optional metadata,
     * enriches the document with processing information in the form of tags, and returns it in the
     * response. The original document structure is preserved while metadata is added to the
     * {@code SearchMetadata.Tags} field.
     * </p>
     *
     * <h3>Processing Behavior:</h3>
     * <ul>
     *   <li>If a document is present, it is echoed back with added metadata tags</li>
     *   <li>Existing tags are preserved and new echo-specific tags are appended</li>
     *   <li>Processing logs are added to track the operation</li>
     *   <li>The response always indicates success unless an error occurs</li>
     * </ul>
     *
     * <h3>Added Metadata Tags:</h3>
     * <ul>
     *   <li><code>processed_by_echo</code> - Application name from configuration</li>
     *   <li><code>echo_timestamp</code> - ISO-8601 formatted current timestamp</li>
     *   <li><code>echo_module_version</code> - Current module version (1.0.0)</li>
     *   <li><code>echo_stream_id</code> - Stream ID from request metadata (if present)</li>
     *   <li><code>echo_step_name</code> - Pipeline step name from request metadata (if present)</li>
     * </ul>
     *
     * @param request the {@link ModuleProcessRequest} containing the document to process and optional metadata.
     *                May contain:
     *                <ul>
     *                  <li>{@code document} - The PipeDoc to process (optional)</li>
     *                  <li>{@code metadata} - Service metadata including stream ID and step name (optional)</li>
     *                </ul>
     * @return a {@link Uni} emitting a {@link ModuleProcessResponse} containing:
     *         <ul>
     *           <li>{@code success} - Always true for this echo implementation</li>
     *           <li>{@code outputDoc} - The input document enriched with metadata (if input document was present)</li>
     *           <li>{@code processorLogs} - List of processing log messages</li>
     *         </ul>
     * @see ProcessingBuffered
     * @see ModuleProcessRequest
     * @see ModuleProcessResponse
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
     * Provides service registration metadata and performs optional health checks.
     * <p>
     * This method returns comprehensive metadata about the echo service, including version information,
     * capabilities, and system details. If the request includes a test request, a health check is
     * performed by processing the test data and validating the service's functionality.
     * </p>
     *
     * <h3>Registration Metadata Includes:</h3>
     * <ul>
     *   <li><strong>Module Information:</strong> Name, version, display name, description, owner</li>
     *   <li><strong>Tags:</strong> pipeline-module, echo, processor</li>
     *   <li><strong>System Info:</strong> Operating system, JVM version</li>
     *   <li><strong>Implementation Details:</strong> Language (Java), SDK version</li>
     *   <li><strong>Timestamps:</strong> Registration timestamp with nanosecond precision</li>
     *   <li><strong>Health Status:</strong> Result of health check if test request is provided</li>
     * </ul>
     *
     * <h3>Health Check Behavior:</h3>
     * <p>
     * If {@code request.hasTestRequest()} returns true, the method will:
     * </p>
     * <ol>
     *   <li>Process the test request using {@link #processData(ModuleProcessRequest)}</li>
     *   <li>Set {@code healthCheckPassed} based on the processing result</li>
     *   <li>Include detailed health check messages</li>
     *   <li>Catch and report any exceptions during health check</li>
     * </ol>
     *
     * @param request the {@link RegistrationRequest} which may optionally contain:
     *                <ul>
     *                  <li>{@code testRequest} - A test document for health check validation (optional)</li>
     *                </ul>
     * @return a {@link Uni} emitting {@link ServiceRegistrationMetadata} containing:
     *         <ul>
     *           <li>Complete service metadata and capabilities</li>
     *           <li>Health check results (if test request was provided)</li>
     *           <li>System and runtime information</li>
     *         </ul>
     * @see RegistrationRequest
     * @see ServiceRegistrationMetadata
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
     * Test version of the document processing method with automatic test data generation.
     * <p>
     * This method provides a testing endpoint that behaves similarly to {@link #processData(ModuleProcessRequest)}
     * but with additional test-specific features. If no document is provided in the request, a complex test
     * document is automatically generated using {@link PipeDocTestDataFactory}.
     * </p>
     *
     * <h3>Test Mode Features:</h3>
     * <ul>
     *   <li>Automatic test document generation if none provided</li>
     *   <li>Default test metadata (test-stream, test-step, test-pipeline)</li>
     *   <li>All processor logs are prefixed with [TEST] marker</li>
     *   <li>Additional test validation log message appended</li>
     * </ul>
     *
     * <h3>Generated Test Document:</h3>
     * <p>
     * When no document is provided, the method generates a complex test document with ID 10101
     * and default test metadata including:
     * </p>
     * <ul>
     *   <li>Stream ID: test-stream</li>
     *   <li>Step Name: test-step</li>
     *   <li>Pipeline Name: test-pipeline</li>
     * </ul>
     *
     * <h3>Usage:</h3>
     * <p>
     * This method is particularly useful for:
     * </p>
     * <ul>
     *   <li>Automated health checks</li>
     *   <li>Integration testing</li>
     *   <li>Validation of pipeline configuration</li>
     *   <li>Smoke tests during deployment</li>
     * </ul>
     *
     * @param request the {@link ModuleProcessRequest} which may contain a test document,
     *                or can be null/empty to trigger automatic test data generation
     * @return a {@link Uni} emitting a {@link ModuleProcessResponse} with all processor logs
     *         prefixed with [TEST] and an additional test validation message
     * @see #processData(ModuleProcessRequest)
     * @see PipeDocTestDataFactory#createComplexDocument(long)
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
