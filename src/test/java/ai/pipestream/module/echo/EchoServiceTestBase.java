package ai.pipestream.module.echo;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import ai.pipestream.data.module.*;
import ai.pipestream.data.util.proto.PipeDocTestDataFactory;
import ai.pipestream.data.v1.PipeDoc;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

/**
 * Abstract base class for Echo Service tests providing comprehensive test coverage.
 *
 * <p>This base class implements a complete test suite for the Echo Service, covering all
 * major functionality areas including document processing, service registration, health
 * checks, metadata handling, and edge cases. Concrete test classes extend this base
 * and provide the necessary dependencies through abstract methods.
 *
 * <h2>Test Architecture</h2>
 * <p>This class follows the Template Method pattern, where the test logic is defined
 * in the base class while concrete implementations provide environment-specific
 * dependencies. This approach enables:
 * <ul>
 *   <li>Code reuse across different test scenarios and profiles</li>
 *   <li>Consistent test coverage regardless of test configuration</li>
 *   <li>Easy addition of new test scenarios that apply to all configurations</li>
 *   <li>Isolation of test logic from infrastructure concerns</li>
 * </ul>
 *
 * <h2>Test Coverage Areas</h2>
 * <ul>
 *   <li><b>Document Processing</b>: Standard processing with complete documents</li>
 *   <li><b>Edge Cases</b>: Processing without documents, large documents</li>
 *   <li><b>Service Registration</b>: Registration with and without health checks</li>
 *   <li><b>Metadata Handling</b>: Tag propagation and preservation</li>
 *   <li><b>Custom Data</b>: Preservation of existing document metadata</li>
 *   <li><b>Test Mode</b>: Validation of test processing functionality</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>To use this base class, create a concrete test class that extends it and
 * implements the required abstract methods:
 *
 * <pre>{@code
 * @QuarkusTest
 * public class MyEchoServiceTest extends EchoServiceTestBase {
 *     @GrpcClient
 *     MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub echoService;
 *
 *     @Inject
 *     PipeDocTestDataFactory testDataFactory;
 *
 *     @ConfigProperty(name = "quarkus.application.name")
 *     String applicationName;
 *
 *     @Override
 *     protected MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub getEchoService() {
 *         return echoService;
 *     }
 *
 *     @Override
 *     protected PipeDocTestDataFactory getTestDataFactory() {
 *         return testDataFactory;
 *     }
 *
 *     @Override
 *     protected String getApplicationName() {
 *         return applicationName;
 *     }
 * }
 * }</pre>
 *
 * @see EchoServiceTest
 * @see EchoServiceImpl
 * @see MutinyPipeStepProcessorGrpc
 */
public abstract class EchoServiceTestBase {

    /**
     * Provides the gRPC service stub for testing.
     *
     * @return the Echo Service stub for making gRPC calls
     */
    protected abstract MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub getEchoService();

    /**
     * Provides the test data factory for generating test documents.
     *
     * @return the factory for creating test PipeDoc instances
     */
    protected abstract PipeDocTestDataFactory getTestDataFactory();

    /**
     * Provides the application name for verification in tests.
     *
     * @return the configured application name
     */
    protected abstract String getApplicationName();

    /**
     * Tests standard document processing with complete request data.
     *
     * <p>This test verifies the core functionality of the Echo Service by:
     * <ul>
     *   <li>Creating a complex test document with realistic data</li>
     *   <li>Building complete service metadata with pipeline context</li>
     *   <li>Configuring processing parameters</li>
     *   <li>Executing the processData operation</li>
     *   <li>Validating the response contains the processed document</li>
     *   <li>Verifying processing logs are generated</li>
     * </ul>
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Response success status is true</li>
     *   <li>Output document is present in response</li>
     *   <li>Document ID matches input document ID</li>
     *   <li>Document body content is preserved</li>
     *   <li>Processing logs are not empty</li>
     *   <li>Logs contain success confirmation message</li>
     * </ul>
     */
    @Test
    void testProcessData() {
        // Create a test document
        PipeDoc testDoc = getTestDataFactory().createComplexDocument(33);

        // Create service metadata
        ServiceMetadata metadata = ServiceMetadata.newBuilder()
                .setPipelineName("test-pipeline")
                .setPipeStepName("echo-step")
                .setStreamId(UUID.randomUUID().toString())
                .setCurrentHopNumber(1)
                .putContextParams("tenant", "test-tenant")
                .build();

        // Create configuration
        ProcessConfiguration config = ProcessConfiguration.newBuilder()
                .setCustomJsonConfig(Struct.newBuilder()
                        .putFields("mode", Value.newBuilder().setStringValue("echo").build())
                        .build())
                .putConfigParams("mode", "echo")
                .build();

        // Create request
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(testDoc)
                .setMetadata(metadata)
                .setConfig(config)
                .build();

        // Execute and verify
        var response = getEchoService().processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Response should be successful", response.getSuccess(), is(true));
        assertThat("Response should have output document", response.hasOutputDoc(), is(true));
        assertThat("Output document ID should match input document ID", response.getOutputDoc().getDocId(), equalTo(testDoc.getDocId()));
        assertThat("Output document body should match input document body", response.getOutputDoc().getSearchMetadata().getBody(), equalTo(testDoc.getSearchMetadata().getBody()));
        assertThat("Processor logs should not be empty", response.getProcessorLogsList(), is(not(empty())));
        assertThat("Processor logs should contain success message", response.getProcessorLogsList(), hasItem(containsString("successfully processed")));
    }

    /**
     * Tests processing behavior when no document is provided in the request.
     *
     * <p>This test validates that the Echo Service handles requests gracefully even
     * when documents are missing, which is important for resilience and error handling.
     *
     * <p><b>Test Scenario:</b> Request with metadata but no document
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Response is still successful (service is tolerant)</li>
     *   <li>No output document is present in response</li>
     *   <li>Processing logs are still generated</li>
     *   <li>Success message appears in logs</li>
     * </ul>
     */
    @Test
    void testProcessDataWithoutDocument() {
        // Test with no document - should still succeed (echo service is tolerant)
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setMetadata(ServiceMetadata.newBuilder()
                        .setPipelineName("test-pipeline")
                        .setPipeStepName("echo-step")
                        .build())
                .setConfig(ProcessConfiguration.newBuilder().build())
                // No document set
                .build();

        var response = getEchoService().processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Response should be successful even without document", response.getSuccess(), is(true));
        assertThat("Response should not have output document", response.hasOutputDoc(), is(false));
        assertThat("Processor logs should not be empty", response.getProcessorLogsList(), is(not(empty())));
        assertThat("Processor logs should contain success message", response.getProcessorLogsList(), hasItem(containsString("successfully processed")));
    }

    /**
     * Tests service registration without health check validation.
     *
     * <p>This test verifies the basic registration flow where no test request
     * is provided for health validation. The service should return complete
     * registration metadata and assume healthy status.
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Module name is correctly reported</li>
     *   <li>JSON config schema is not present (echo doesn't require config)</li>
     *   <li>Health check passes without test request</li>
     *   <li>Health check message indicates service is healthy</li>
     * </ul>
     */
    @Test
    void testGetServiceRegistrationWithoutHealthCheck() {
        // Call without test request
        RegistrationRequest request = RegistrationRequest.newBuilder().build();

        var registration = getEchoService().getServiceRegistration(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Module name should be 'echo'", registration.getModuleName(), equalTo("echo"));
        // Echo service has no JSON schema - it accepts any input
        assertThat("JSON config schema should not be present", registration.hasJsonConfigSchema(), is(false));
        // Should be healthy without test
        assertThat("Health check should pass", registration.getHealthCheckPassed(), is(true));
        assertThat("Health check message should indicate service is healthy", registration.getHealthCheckMessage(), containsString("Service is healthy"));
    }

    /**
     * Tests service registration with health check validation.
     *
     * <p>This test verifies the full registration flow including health validation
     * by providing a test document for processing. The service should process the
     * test document and report health status based on the result.
     *
     * <p><b>Test Scenario:</b> Registration with test request for health validation
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Module name is correctly reported</li>
     *   <li>JSON config schema is not present</li>
     *   <li>Health check passes with successful test processing</li>
     *   <li>Health check message confirms service is functioning correctly</li>
     * </ul>
     */
    @Test
    void testGetServiceRegistrationWithHealthCheck() {
        // Create a test document for health check
        PipeDoc testDoc = getTestDataFactory().createComplexDocument(20203);

        ModuleProcessRequest processRequest = ModuleProcessRequest.newBuilder()
                .setDocument(testDoc)
                .setMetadata(ServiceMetadata.newBuilder()
                        .setPipelineName("health-check")
                        .setPipeStepName("echo-health")
                        .build())
                .setConfig(ProcessConfiguration.newBuilder().build())
                .build();

        // Call with test request for health check
        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setTestRequest(processRequest)
                .build();

        var registration = getEchoService().getServiceRegistration(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Module name should be 'echo'", registration.getModuleName(), equalTo("echo"));
        assertThat("JSON config schema should not be present", registration.hasJsonConfigSchema(), is(false));
        // Health check should pass
        assertThat("Health check should pass with test request", registration.getHealthCheckPassed(), is(true));
        assertThat("Health check message should indicate service is functioning correctly", 
                  registration.getHealthCheckMessage(), containsString("healthy and functioning correctly"));
    }

    /**
     * Tests that pipeline metadata is properly extracted and added to document tags.
     *
     * <p>This test validates the core metadata enrichment functionality of the Echo Service.
     * It verifies that metadata from the service context (stream ID, step name, etc.) is
     * correctly extracted and added to the document's SearchMetadata tags.
     *
     * <p><b>Test Data:</b>
     * <ul>
     *   <li>Pipeline name: metadata-test</li>
     *   <li>Step name: echo-metadata</li>
     *   <li>Stream ID: stream-123</li>
     *   <li>Hop number: 5</li>
     *   <li>Context params: tenant, region</li>
     * </ul>
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Response is successful</li>
     *   <li>Output document contains enriched metadata</li>
     *   <li>Tag {@code processed_by_echo} contains application name</li>
     *   <li>Tag {@code echo_stream_id} matches input stream ID</li>
     *   <li>Tag {@code echo_step_name} matches input step name</li>
     * </ul>
     */
    @Test
    void testMetadataPropagation() {
        // Test that metadata is properly propagated
        ServiceMetadata metadata = ServiceMetadata.newBuilder()
                .setPipelineName("metadata-test")
                .setPipeStepName("echo-metadata")
                .setStreamId("stream-123")
                .setCurrentHopNumber(5)
                .putContextParams("tenant", "test-tenant")
                .putContextParams("region", "us-east-1")
                .build();

        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(getTestDataFactory().createComplexDocument(100))
                .setMetadata(metadata)
                .build();

        var response = getEchoService().processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Response should be successful", response.getSuccess(), is(true));
        assertThat("Response should have output document", response.hasOutputDoc(), is(true));

        var tags = response.getOutputDoc().getSearchMetadata().getTags().getTagDataMap();
        assertThat(tags, hasEntry("processed_by_echo", getApplicationName()));
        assertThat(tags, hasEntry("echo_stream_id", "stream-123"));
        assertThat(tags, hasEntry("echo_step_name", "echo-metadata"));
    }

    /**
     * Tests processing of large documents to verify memory and message size handling.
     *
     * <p>This test validates that the Echo Service can handle large documents without
     * running into memory issues or exceeding gRPC message size limits. It creates a
     * document with substantial body content (1000 lines) and ensures it processes
     * successfully.
     *
     * <p><b>Test Configuration:</b>
     * <ul>
     *   <li>Document with 1000 lines of text</li>
     *   <li>Expected body size: >10,000 characters</li>
     * </ul>
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Response is successful</li>
     *   <li>Output document is present</li>
     *   <li>Large body content is preserved (>10,000 characters)</li>
     * </ul>
     */
    @Test
    void testLargeDocument() {
        // Test with a large document
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeBody.append("This is line ").append(i).append(" of a large document. ");
        }

        PipeDoc largeDoc = PipeDoc.newBuilder()
                .setDocId("large-doc")
                .setSearchMetadata(ai.pipestream.data.v1.SearchMetadata.newBuilder()
                        .setBody(largeBody.toString())
                        .setTitle("Large Document Test")
                        .build())
                .build();

        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(largeDoc)
                .setMetadata(ServiceMetadata.newBuilder()
                        .setPipelineName("large-doc-test")
                        .setPipeStepName("echo")
                        .build())
                .setConfig(ProcessConfiguration.newBuilder().build())
                .build();

        var response = getEchoService().processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Response should be successful", response.getSuccess(), is(true));
        assertThat("Response should have output document", response.hasOutputDoc(), is(true));
        assertThat("Output document should have large body", 
                  response.getOutputDoc().getSearchMetadata().getBody().length(), 
                  greaterThan(10000));
    }

    /**
     * Tests preservation of existing custom data fields during processing.
     *
     * <p>This test validates that the Echo Service preserves any existing custom_data
     * fields in the document's SearchMetadata while adding its own metadata tags.
     * This is critical for pipeline integrity - modules must not overwrite data
     * from previous processing steps.
     *
     * <p><b>Test Data:</b>
     * <ul>
     *   <li>Existing string field: existing_field = "existing_value"</li>
     *   <li>Existing number field: existing_number = 42.0</li>
     * </ul>
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Response is successful</li>
     *   <li>Original custom_data fields are preserved unchanged</li>
     *   <li>New echo metadata tags are added separately</li>
     *   <li>Echo timestamp tag is present</li>
     * </ul>
     */
    @Test
    void testExistingCustomData() {
        // Test that existing custom_data is preserved and extended
        Struct existingCustomData = Struct.newBuilder()
                .putFields("existing_field", Value.newBuilder().setStringValue("existing_value").build())
                .putFields("existing_number", Value.newBuilder().setNumberValue(42.0).build())
                .build();

        PipeDoc docWithCustomData = PipeDoc.newBuilder()
                .setDocId("custom-data-doc")
                .setSearchMetadata(ai.pipestream.data.v1.SearchMetadata.newBuilder()
                        .setBody("Document with existing custom data")
                        .setCustomFields(existingCustomData)
                        .build())
                .build();

        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(docWithCustomData)
                .setMetadata(ServiceMetadata.newBuilder()
                        .setPipelineName("custom-data-test")
                        .setPipeStepName("echo-custom")
                        .build())
                .build();

        var response = getEchoService().processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Response should be successful", response.getSuccess(), is(true));
        assertThat("Response should have output document", response.hasOutputDoc(), is(true));

        var customData = response.getOutputDoc().getSearchMetadata().getCustomFields().getFieldsMap();
        // Original data should be preserved
        assertThat("Original field should be preserved", customData, hasKey("existing_field"));
        assertThat("Original field value should be preserved", 
                  customData.get("existing_field").getStringValue(), equalTo("existing_value"));
        assertThat("Original number field should be preserved", customData, hasKey("existing_number"));
        assertThat("Original number value should be preserved", 
                  customData.get("existing_number").getNumberValue(), equalTo(42.0));

        // New echo data should be added to tags
        var tags = response.getOutputDoc().getSearchMetadata().getTags().getTagDataMap();
        assertThat("Echo processor marker should be added to tags", tags, hasKey("processed_by_echo"));
        assertThat("Echo timestamp should be added to tags", tags, hasKey("echo_timestamp"));
    }

    /**
     * Tests the test processing mode with automatic test data generation.
     *
     * <p>This test validates the {@code testProcessData} method which is designed
     * for testing and health checking. When called with null or no document, it
     * should automatically generate a test document, process it, and mark all
     * logs with [TEST] prefix.
     *
     * <p><b>Test Scenario:</b> Call testProcessData with null request
     *
     * <p><b>Expected Behavior:</b>
     * <ul>
     *   <li>Service generates a test document automatically</li>
     *   <li>Document is processed successfully</li>
     *   <li>All logs are marked with [TEST] prefix</li>
     *   <li>Validation completion message is added</li>
     * </ul>
     *
     * <p><b>Assertions:</b>
     * <ul>
     *   <li>Test response is successful</li>
     *   <li>Output document is present</li>
     *   <li>Document ID has test prefix</li>
     *   <li>Processor logs contain [TEST] markers</li>
     *   <li>Logs confirm test validation completed</li>
     * </ul>
     */
    @Test
    void testTestProcessData() {
        // Test the testProcessData method
        var response = getEchoService().testProcessData(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat("Test response should be successful", response.getSuccess(), is(true));
        assertThat("Test response should have output document", response.hasOutputDoc(), is(true));
        assertThat("Test document ID should have expected prefix", response.getOutputDoc().getDocId(), startsWith("test-doc-"));
        assertThat("Processor logs should contain test marker", response.getProcessorLogsList(), hasItem(containsString("[TEST]")));
        assertThat("Processor logs should contain validation success message", 
                  response.getProcessorLogsList(), hasItem(containsString("test validation completed successfully")));
    }


}
