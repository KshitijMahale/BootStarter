package com.example.bootstarter.service;

import com.example.bootstarter.model.SpringBootProjectRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringInitializrRequestBuilderTest {

    @Test
    void buildsExpectedUrl() {
        SpringBootProjectRequest r = new SpringBootProjectRequest();
        r.setType("maven-project");
        r.setLanguage("java");
        r.setSpringBootVersion("3.3.5");
        r.setGroupId("com.example");
        r.setArtifactId("demo");
        r.setName("demo");
        r.setPackageName("com.example.demo");
        r.setPackaging("jar");
        r.setJavaVersion("17");
        r.setDependencies(List.of("web", "data-jpa"));

        String url = new SpringInitializrRequestBuilder().buildUrl(r);

        assertTrue(url.startsWith("https://start.spring.io/starter.zip?"));
        assertTrue(url.contains("dependencies=web%2Cdata-jpa"));
        assertTrue(url.contains("type=maven-project"));
    }
}

