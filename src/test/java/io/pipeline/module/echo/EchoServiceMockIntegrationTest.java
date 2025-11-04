//package io.pipeline.module.echo;
//
//import io.pipeline.grpc.wiremock.MockServiceFactory;
//import io.pipeline.platform.registration.PlatformRegistrationGrpc;
//import io.pipeline.platform.registration.RegistrationEvent;
//import io.pipeline.platform.registration.EventType;
//import io.pipeline.platform.registration.ModuleRegistrationRequest;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Simple test that demonstrates the mock service factory works
// * and can simulate the echo service registration flow.
// *
// * This test proves that:
// * 1. The mock service factory works
// * 2. The streaming registration flow works
// * 3. The echo service registration can be simulated
// */
//public class EchoServiceMockIntegrationTest {
//
//    @BeforeEach
//    void setUp() {
//        // Start the mock platform registration service
//        MockServiceFactory.startMockPlatformRegistrationService();
//    }
//
//    @AfterEach
//    void tearDown() {
//        // Stop the mock services
//        MockServiceFactory.stopMockPlatformRegistrationService();
//    }
//
//    @Test
//    void testEchoServiceRegistrationSimulation() throws InterruptedException {
//        // This test simulates what the echo service would do when it registers
//
//        // Verify the mock service is running
//        assertTrue(MockServiceFactory.isMockServiceRunning(),
//            "Mock platform registration service should be running");
//
//        // Get the mock server
//        var mockServer = MockServiceFactory.getMockServer();
//        assertNotNull(mockServer, "Mock server should be available");
//        assertTrue(mockServer.getGrpcPort() > 0, "Mock server should have a valid port");
//
//        // Create a stub to the mock service
//        var stub = MockServiceFactory.getPlatformRegistrationStub();
//        assertNotNull(stub, "Should be able to get platform registration stub");
//
//        // Simulate the echo service registration
//        ModuleRegistrationRequest echoRegistration = ModuleRegistrationRequest.newBuilder()
//            .setModuleName("echo")
//            .setHost("localhost")
//            .setPort(39000)
//            .setVersion("1.0.0")
//            .build();
//
//        // Test the streaming registration call
//        List<RegistrationEvent> receivedEvents = new ArrayList<>();
//        CountDownLatch latch = new CountDownLatch(1);
//        boolean[] errorReceived = {false};
//
//        stub.registerModule(echoRegistration, new io.grpc.stub.StreamObserver<RegistrationEvent>() {
//            @Override
//            public void onNext(RegistrationEvent event) {
//                receivedEvents.add(event);
//                System.out.println("Echo registration event: " + event.getEventType() + " - " + event.getMessage());
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                System.err.println("Echo registration error: " + t.getMessage());
//                errorReceived[0] = true;
//                latch.countDown();
//            }
//
//            @Override
//            public void onCompleted() {
//                System.out.println("Echo registration completed successfully");
//                latch.countDown();
//            }
//        });
//
//        // Wait for completion
//        assertTrue(latch.await(10, TimeUnit.SECONDS), "Echo registration should complete within 10 seconds");
//
//        // Verify the registration was successful
//        assertFalse(errorReceived[0], "Echo registration should not fail");
//        assertEquals(10, receivedEvents.size(), "Should receive 10 events for module registration");
//
//        // Verify the final event indicates success
//        RegistrationEvent finalEvent = receivedEvents.get(receivedEvents.size() - 1);
//        assertEquals(EventType.COMPLETED, finalEvent.getEventType());
//        assertTrue(finalEvent.getMessage().contains("completed successfully"));
//
//        // Verify the sequence of events
//        assertEquals(EventType.STARTED, receivedEvents.get(0).getEventType());
//        assertEquals(EventType.VALIDATED, receivedEvents.get(1).getEventType());
//        assertEquals(EventType.CONSUL_REGISTERED, receivedEvents.get(2).getEventType());
//        assertEquals(EventType.HEALTH_CHECK_CONFIGURED, receivedEvents.get(3).getEventType());
//        assertEquals(EventType.CONSUL_HEALTHY, receivedEvents.get(4).getEventType());
//        assertEquals(EventType.METADATA_RETRIEVED, receivedEvents.get(5).getEventType());
//        assertEquals(EventType.SCHEMA_VALIDATED, receivedEvents.get(6).getEventType());
//        assertEquals(EventType.DATABASE_SAVED, receivedEvents.get(7).getEventType());
//        assertEquals(EventType.APICURIO_REGISTERED, receivedEvents.get(8).getEventType());
//        assertEquals(EventType.COMPLETED, receivedEvents.get(9).getEventType());
//    }
//
//    @Test
//    void testMockServiceFactoryProvidesConsistentInterface() {
//        // This test verifies that the MockServiceFactory provides the same interface
//        // as the real platform registration service
//
//        // Verify the factory is working
//        assertTrue(MockServiceFactory.isMockServiceRunning(), "Mock service should be running");
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
//
//    @Test
//    void testEchoServiceCanQueryRegisteredModules() throws InterruptedException {
//        // This test simulates the echo service querying what modules are registered
//
//        var stub = MockServiceFactory.getPlatformRegistrationStub();
//
//        CountDownLatch latch = new CountDownLatch(1);
//        boolean[] successReceived = {false};
//        io.pipeline.platform.registration.ModuleListResponse[] responseHolder = {null};
//
//        stub.listModules(
//            com.google.protobuf.Empty.getDefaultInstance(),
//            new io.grpc.stub.StreamObserver<io.pipeline.platform.registration.ModuleListResponse>() {
//                @Override
//                public void onNext(io.pipeline.platform.registration.ModuleListResponse response) {
//                    responseHolder[0] = response;
//                    successReceived[0] = true;
//                    System.out.println("Found " + response.getTotalCount() + " modules");
//                }
//
//                @Override
//                public void onError(Throwable t) {
//                    System.err.println("Error: " + t.getMessage());
//                    latch.countDown();
//                }
//
//                @Override
//                public void onCompleted() {
//                    System.out.println("Module list query completed");
//                    latch.countDown();
//                }
//            }
//        );
//
//        // Wait for completion
//        assertTrue(latch.await(5, TimeUnit.SECONDS), "Module list query should complete within 5 seconds");
//        assertTrue(successReceived[0], "Module list query should succeed");
//        assertNotNull(responseHolder[0], "Should receive a response");
//        assertEquals(2, responseHolder[0].getTotalCount(), "Should find 2 modules");
//    }
//}