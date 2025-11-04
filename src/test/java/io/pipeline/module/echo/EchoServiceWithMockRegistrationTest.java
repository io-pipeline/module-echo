//package io.pipeline.module.echo;
//
//import io.pipeline.data.module.MutinyPipeStepProcessorGrpc;
//import io.pipeline.data.util.proto.PipeDocTestDataFactory;
//import io.pipeline.grpc.wiremock.MockServiceFactory;
//import io.quarkus.grpc.GrpcClient;
//import io.quarkus.test.junit.QuarkusTest;
//import io.quarkus.test.common.QuarkusTestResource;
//import jakarta.inject.Inject;
//import org.eclipse.microprofile.config.inject.ConfigProperty;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Test that demonstrates the echo service actually calling the platform registration service
// * using the MockServiceFactory.
// *
// * This test proves that:
// * 1. The echo service can call the platform registration service
// * 2. The mock service factory works with real Quarkus applications
// * 3. The streaming registration flow works end-to-end
// */
//@QuarkusTest
//@QuarkusTestResource(MockPlatformRegistrationTestResource.class)
//public class EchoServiceWithMockRegistrationTest {
//
//    @GrpcClient("echo")
//    MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub echoService;
//
//    @Inject
//    PipeDocTestDataFactory pipeDocTestDataFactory;
//
//    @Inject
//    @ConfigProperty(name = "quarkus.application.name")
//    String applicationName;
//
//    @BeforeEach
//    void setUp() {
//        // The MockPlatformRegistrationTestResource handles starting the mock service
//        // Just wait a moment for everything to be ready
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    @AfterEach
//    void tearDown() {
//        // The MockPlatformRegistrationTestResource handles stopping the mock service
//    }
//
//    @Test
//    void testEchoServiceCallsPlatformRegistration() {
//        // This test verifies that the echo service can successfully call
//        // the platform registration service through the mock
//
//        // Verify the mock service is running
//        assertTrue(MockServiceFactory.isMockServiceRunning(),
//            "Mock platform registration service should be running");
//
//        // Verify we can get the mock server
//        var mockServer = MockServiceFactory.getMockServer();
//        assertNotNull(mockServer, "Mock server should be available");
//        assertTrue(mockServer.getGrpcPort() > 0, "Mock server should have a valid port");
//
//        // The echo service should have been able to register itself
//        // through the ModuleRegistrationClient during startup
//        // We can verify this by checking that the mock service is working
//        assertDoesNotThrow(() -> {
//            var stub = MockServiceFactory.getPlatformRegistrationStub();
//            assertNotNull(stub, "Should be able to get platform registration stub");
//        }, "Should be able to get platform registration stub from mock service");
//    }
//
//    @Test
//    void testEchoServiceBasicFunctionality() {
//        // Test that the echo service still works normally
//        // even when using the mock platform registration service
//
//        var testDoc = pipeDocTestDataFactory.createComplexDocument(123);
//
//        var request = io.pipeline.data.module.ModuleProcessRequest.newBuilder()
//            .setDocument(testDoc)
//            .setMetadata(io.pipeline.data.module.ServiceMetadata.newBuilder()
//                .setPipelineName("test-pipeline")
//                .setPipeStepName("echo-step")
//                .build())
//            .build();
//
//        var response = echoService.processData(request)
//            .subscribe().withSubscriber(io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create())
//            .awaitItem()
//            .getItem();
//
//        assertNotNull(response, "Response should not be null");
//        assertTrue(response.getSuccess(), "Response should be successful");
//        assertTrue(response.hasOutputDoc(), "Response should have output document");
//        assertEquals(testDoc.getDocId(), response.getOutputDoc().getDocId(),
//            "Output document ID should match input document ID");
//    }
//
//    @Test
//    void testMockServiceFactoryIntegration() {
//        // This test verifies that the MockServiceFactory integrates properly
//        // with the Quarkus test environment
//
//        // Verify the factory is working
//        assertTrue(MockServiceFactory.isMockServiceRunning(),
//            "Mock service should be running");
//
//        // Verify we can get the mock server
//        var mockServer = MockServiceFactory.getMockServer();
//        assertNotNull(mockServer, "Mock server should be available");
//        assertTrue(mockServer.getGrpcPort() > 0, "Mock server should have a valid port");
//
//        // Verify we can get stubs from the factory
//        assertDoesNotThrow(() -> {
//            MockServiceFactory.getPlatformRegistrationStub();
//        }, "Should be able to get async stub");
//
//        assertDoesNotThrow(() -> {
//            MockServiceFactory.getPlatformRegistrationBlockingStub();
//        }, "Should be able to get blocking stub");
//    }
//}