//package io.pipeline.module.echo;
//
//import io.quarkus.test.junit.QuarkusTestProfile;
//import java.util.Map;
//
///**
// * Test profile that configures the echo service to use the mock platform registration service.
// *
// * This profile:
// * 1. Enables module registration (which is normally disabled in tests)
// * 2. Points the registration service to the mock service
// * 3. Disables Consul service discovery
// */
//public class EchoServiceWithMockRegistrationTestProfile implements QuarkusTestProfile {
//
//    @Override
//    public Map<String, String> getConfigOverrides() {
//        Map<String, String> config = new java.util.HashMap<>();
//
//        // Enable module registration for this test
//        config.put("module.registration.enabled", "true");
//        config.put("module.registration.module-name", "echo");
//        config.put("module.registration.host", "localhost");
//        config.put("module.registration.port", "39000");
//
//        // Point to the mock platform registration service
//        config.put("quarkus.grpc.clients.platform-registration-service.host", "localhost");
//        config.put("quarkus.grpc.clients.platform-registration-service.port", "0"); // Will be overridden by mock
//
//        // Disable Consul service discovery
//        config.put("quarkus.stork.platform-registration-service.service-discovery.type", "static");
//        config.put("quarkus.stork.platform-registration-service.service-discovery.host", "localhost");
//        config.put("quarkus.stork.platform-registration-service.service-discovery.port", "0"); // Will be overridden by mock
//
//        // Enable mock services
//        config.put("pipeline.test.mock-services.enabled", "true");
//        config.put("pipeline.test.mock-services.platform-registration.enabled", "true");
//
//        return config;
//    }
//
//    @Override
//    public String getConfigProfile() {
//        return "test";
//    }
//}