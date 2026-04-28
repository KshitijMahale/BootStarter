package com.example.bootstarter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringInitializrMetadataServiceTest {

    private static final String MIN_DEPENDENCIES_CATALOG = """
            {
              "dependencies": {
                "web": {"name": "Spring Web"}
              }
            }
            """;

    @Test
    void parsesClientMetadataForAllDynamicFields() {
        String json = """
                {
                  "type": {"values": [
                    {"id": "maven-project", "name": "Maven Project", "default": true},
                    {"id": "gradle-project", "name": "Gradle Project"}
                  ]},
                  "language": {"values": [
                    {"id": "java", "default": true},
                    {"id": "kotlin"}
                  ]},
                  "bootVersion": {"values": [
                    {"id": "3.5.1", "default": true},
                    {"id": "3.4.7"}
                  ]},
                  "javaVersion": {"values": [
                    {"id": "26", "default": true},
                    {"id": "25"},
                    {"id": "21"},
                    {"id": "17"}
                  ]},
                  "packaging": {"values": [
                    {"id": "jar", "default": true},
                    {"id": "war"}
                  ]},
                  "dependencies": {"values": [
                    {"name": "Web", "values": [
                      {"id": "web", "name": "Spring Web"}
                    ]},
                    {"name": "SQL", "values": [
                      {"id": "data-jpa", "name": "Spring Data JPA"}
                    ]}
                  ]}
                }
                """;

        SpringInitializrMetadata metadata = SpringInitializrMetadataService.parseMetadata(json);

        assertEquals(2, metadata.getTypes().size());
        assertEquals("maven-project", metadata.getTypes().get(0));
        assertEquals("java", metadata.getLanguages().get(0));
        assertEquals("3.5.1", metadata.getBootVersions().get(0));
        assertEquals("26", metadata.getJavaVersions().get(0));
        assertEquals("jar", metadata.getPackagings().get(0));
        assertEquals("web", metadata.getDependencyLabelToId().get("Spring Web"));
        assertEquals("data-jpa", metadata.getDependencyLabelToId().get("Spring Data JPA"));
        assertEquals("maven-project", metadata.getDefaultType());
        assertEquals("java", metadata.getDefaultLanguage());
        assertEquals("3.5.1", metadata.getDefaultBootVersion());
        assertEquals("26", metadata.getDefaultJavaVersion());
        assertEquals("jar", metadata.getDefaultPackaging());
    }

    @Test
    void disambiguatesDuplicateDependencyNames() {
        String json = """
                {
                  "dependencies": {"values": [
                    {"values": [
                      {"id": "aws-messaging", "name": "Messaging"},
                      {"id": "messaging", "name": "Messaging"}
                    ]}
                  ]}
                }
                """;

        SpringInitializrMetadata metadata = SpringInitializrMetadataService.parseMetadata(json);

        assertTrue(metadata.getDependencyLabelToId().containsKey("Messaging"));
        assertTrue(metadata.getDependencyLabelToId().containsKey("Messaging (messaging)"));
    }

    @Test
    void parsesFlatDependenciesCatalogPayload() {
        String json = """
                {
                  "dependencies": {
                    "web": {"name": "Spring Web"},
                    "websocket": {"name": "WebSocket"},
                    "redis": {"id": "redis", "name": "Spring Data Redis"}
                  }
                }
                """;

        var dependencies = SpringInitializrMetadataService.parseDependencyCatalog(json);

        assertEquals("web", dependencies.get("Spring Web"));
        assertEquals("websocket", dependencies.get("WebSocket"));
        assertEquals("redis", dependencies.get("Spring Data Redis"));
    }

    @Test
    void parsesSectionLevelDefaultsAndPrimitiveValues() {
        String json = """
                {
                  "type": {
                    "default": "maven-project",
                    "values": ["maven-project", "gradle-project"]
                  },
                  "javaVersion": {
                    "default": "17",
                    "values": ["26", "25", "21", "17"]
                  },
                  "bootVersion": {
                    "default": "4.0.6.RELEASE",
                    "values": ["4.1.0.BUILD-SNAPSHOT", "4.0.6.RELEASE", "3.5.14.RELEASE"]
                  }
                }
                """;

        SpringInitializrMetadata metadata = SpringInitializrMetadataService.parseMetadata(json);

        assertEquals("maven-project", metadata.getDefaultType());
        assertEquals("17", metadata.getDefaultJavaVersion());
        assertEquals("4.0.6.RELEASE", metadata.getDefaultBootVersion());
        assertEquals("26", metadata.getJavaVersions().get(0));
        assertEquals("4.1.0.BUILD-SNAPSHOT", metadata.getBootVersions().get(0));
    }

    @Test
    void usesFallbackMetadataWhenPrimaryPayloadIsInvalid() throws Exception {
        String fallback = """
                {
                  "bootVersion": {"default": "4.0.6.RELEASE", "values": ["4.0.6.RELEASE", "3.5.14.RELEASE"]},
                  "javaVersion": {"default": "17", "values": ["21", "17"]},
                  "type": {"values": [{"id": "maven-project"}]},
                  "language": {"values": [{"id": "java"}]},
                  "packaging": {"values": [{"id": "jar"}]},
                  "dependencies": {"values": [{"values": [{"id": "web", "name": "Spring Web"}]}]}
                }
                """;

        StubClient client = new StubClient("not-json", fallback, MIN_DEPENDENCIES_CATALOG);
        SpringInitializrMetadataService service = new SpringInitializrMetadataService(client);

        SpringInitializrMetadata metadata = service.fetchMetadata();

        assertEquals("21", metadata.getJavaVersions().get(0));
        assertEquals("4.0.6.RELEASE", metadata.getBootVersions().get(0));
        assertTrue(client.fallbackCalled);
    }

    @Test
    void prefersFallbackWhenPrimaryMetadataMissesVersionOptions() throws Exception {
        String incompletePrimary = """
                {
                  "type": {"values": [{"id": "maven-project"}]},
                  "language": {"values": [{"id": "java"}]},
                  "packaging": {"values": [{"id": "jar"}]}
                }
                """;

        String fallback = """
                {
                  "bootVersion": {"default": "4.0.6.RELEASE", "values": ["4.0.6.RELEASE"]},
                  "javaVersion": {"default": "17", "values": ["25", "21", "17"]},
                  "type": {"values": [{"id": "maven-project"}]},
                  "language": {"values": [{"id": "java"}]},
                  "packaging": {"values": [{"id": "jar"}]}
                }
                """;

        StubClient client = new StubClient(incompletePrimary, fallback, MIN_DEPENDENCIES_CATALOG);
        SpringInitializrMetadataService service = new SpringInitializrMetadataService(client);

        SpringInitializrMetadata metadata = service.fetchMetadata();

        assertEquals("25", metadata.getJavaVersions().get(0));
        assertEquals("4.0.6.RELEASE", metadata.getBootVersions().get(0));
        assertTrue(client.fallbackCalled);
    }

    private static final class StubClient extends SpringInitializrClient {
        private final String metadataJson;
        private final String fallbackMetadataJson;
        private final String dependenciesJson;
        private boolean fallbackCalled;

        private StubClient(String metadataJson, String fallbackMetadataJson, String dependenciesJson) {
            this.metadataJson = metadataJson;
            this.fallbackMetadataJson = fallbackMetadataJson;
            this.dependenciesJson = dependenciesJson;
        }

        @Override
        public String fetchMetadataJson() {
            return metadataJson;
        }

        @Override
        public String fetchMetadataJsonFallback() {
            fallbackCalled = true;
            return fallbackMetadataJson;
        }

        @Override
        public String fetchDependenciesJson() {
            return dependenciesJson;
        }
    }
}
