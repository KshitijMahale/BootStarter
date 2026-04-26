package com.example.bootstarter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringInitializrMetadataServiceTest {

    @Test
    void parsesClientMetadataForAllDynamicFields() {
        String json = """
                {
                  "type": {"values": [
                    {"id": "maven-project", "name": "Maven Project"},
                    {"id": "gradle-project", "name": "Gradle Project"}
                  ]},
                  "language": {"values": [
                    {"id": "java"},
                    {"id": "kotlin"}
                  ]},
                  "bootVersion": {"values": [
                    {"id": "3.5.1"},
                    {"id": "3.4.7"}
                  ]},
                  "javaVersion": {"values": [
                    {"id": "17"},
                    {"id": "21"}
                  ]},
                  "packaging": {"values": [
                    {"id": "jar"},
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
        assertEquals("17", metadata.getJavaVersions().get(0));
        assertEquals("jar", metadata.getPackagings().get(0));
        assertEquals("web", metadata.getDependencyLabelToId().get("Spring Web"));
        assertEquals("data-jpa", metadata.getDependencyLabelToId().get("Spring Data JPA"));
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
}
