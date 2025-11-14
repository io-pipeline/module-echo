package ai.pipestream.module.echo;

import ai.pipestream.data.module.MutinyPipeStepProcessorGrpc;
import ai.pipestream.data.util.proto.PipeDocTestDataFactory;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Concrete test class for Echo Service with default Quarkus test configuration.
 *
 * <p>This class extends {@link EchoServiceTestBase} and runs all inherited tests
 * in a standard Quarkus test environment with default configuration. It provides
 * the necessary dependencies (gRPC client, test data factory, application name)
 * through Quarkus's dependency injection.
 *
 * <h2>Test Configuration</h2>
 * <p>This test runs with:
 * <ul>
 *   <li>Full Quarkus application context</li>
 *   <li>Embedded gRPC server on test port</li>
 *   <li>Service registration enabled by default</li>
 *   <li>Standard test configuration profile</li>
 * </ul>
 *
 * <h2>Inherited Tests</h2>
 * <p>All test methods are inherited from {@link EchoServiceTestBase}, including:
 * <ul>
 *   <li>Document processing tests</li>
 *   <li>Service registration tests</li>
 *   <li>Metadata propagation tests</li>
 *   <li>Edge case handling tests</li>
 *   <li>Large document tests</li>
 *   <li>Test mode validation</li>
 * </ul>
 *
 * <h2>Dependency Injection</h2>
 * <p>The class uses Quarkus's CDI to inject:
 * <ul>
 *   <li>{@code @GrpcClient}: The gRPC service stub for making test calls</li>
 *   <li>{@code @Inject}: Test data factory and configuration properties</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Run this test class to verify Echo Service functionality in a standard
 * test environment. For alternative test configurations (e.g., without service
 * registration), see {@link EchoServiceNoRegistrationTest}.
 *
 * @see EchoServiceTestBase
 * @see EchoServiceImpl
 * @see EchoServiceNoRegistrationTest
 */
@QuarkusTest
class EchoServiceTest extends EchoServiceTestBase {

    /**
     * gRPC client stub for the Echo Service, injected by Quarkus.
     * Used to make gRPC calls to the service under test.
     */
    @GrpcClient
    MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub echoService;

    /**
     * Factory for generating test documents, injected by Quarkus CDI.
     */
    @Inject
    PipeDocTestDataFactory pipeDocTestDataFactory;

    /**
     * Application name from configuration, injected by Quarkus.
     * Used for verification in metadata propagation tests.
     */
    @Inject
    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    /**
     * Provides the configured application name for test verification.
     *
     * @return the application name from configuration
     */
    @Override
    protected String getApplicationName() {
        return applicationName;
    }

    /**
     * Provides the gRPC service stub for making test calls.
     *
     * @return the injected Echo Service gRPC client stub
     */
    @Override
    protected MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub getEchoService() {
        // Return the echo service for testing
        return echoService;
    }

    /**
     * Provides the test data factory for generating test documents.
     *
     * @return the injected test data factory
     */
    @Override
    protected PipeDocTestDataFactory getTestDataFactory() {
        return pipeDocTestDataFactory;
    }


}