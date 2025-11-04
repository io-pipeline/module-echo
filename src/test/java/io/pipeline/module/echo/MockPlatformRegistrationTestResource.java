//package io.pipeline.module.echo;
//
//import io.pipeline.grpc.wiremock.MockServiceFactory;
//import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
//import java.util.Map;
//
///**
// * Test resource that starts the mock platform registration service
// * and configures it to be discoverable by the echo service.
// */
//public class MockPlatformRegistrationTestResource implements QuarkusTestResourceLifecycleManager {
//
//    @Override
//    public Map<String, String> start() {
//        // Start the mock platform registration service
//        MockServiceFactory.startMockPlatformRegistrationService();
//
//        // Get the mock server port
//        int mockPort = MockServiceFactory.getMockServer().getGrpcPort();
//
//        // Return configuration that points the echo service to the mock
//        return Map.of(
//            // Point the platform registration service to our mock
//            "quarkus.grpc.clients.platform-registration-service.host", "localhost",
//            "quarkus.grpc.clients.platform-registration-service.port", String.valueOf(mockPort),
//
//            // Configure Stork to use static discovery for the mock service
//            "quarkus.stork.platform-registration-service.service-discovery.type", "static",
//            "quarkus.stork.platform-registration-service.service-discovery.host", "localhost",
//            "quarkus.stork.platform-registration-service.service-discovery.port", String.valueOf(mockPort),
//
//            // Enable module registration
//            "module.registration.enabled", "true",
//            "module.registration.module-name", "echo",
//            "module.registration.host", "localhost",
//            "module.registration.port", "39000"
//        );
//    }
//
//    @Override
//    public void stop() {
//        // Stop the mock services
//        MockServiceFactory.stopMockPlatformRegistrationService();
//    }
//}