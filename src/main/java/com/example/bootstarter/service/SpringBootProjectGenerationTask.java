package com.example.bootstarter.service;

import com.example.bootstarter.model.SpringBootProjectRequest;
import com.example.bootstarter.util.NotificationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringBootProjectGenerationTask extends Task.Backgroundable {

    private static final Pattern JSON_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    private final Project project;
    private final VirtualFile projectRoot;
    private final SpringBootProjectRequest request;
    private final ConflictPolicy conflictPolicy;

    private final SpringInitializrRequestBuilder requestBuilder = new SpringInitializrRequestBuilder();
    private final SpringInitializrClient client = new SpringInitializrClient();
    private final ZipExtractionService extractionService = new ZipExtractionService();
    private final VirtualFileCopyService copyService = new VirtualFileCopyService();
    private final ProjectRefreshService refreshService = new ProjectRefreshService();

    public SpringBootProjectGenerationTask(Project project,
                                           VirtualFile projectRoot,
                                           SpringBootProjectRequest request,
                                           ConflictPolicy conflictPolicy) {
        super(project, "Generating Spring Boot project", true);
        this.project = project;
        this.projectRoot = projectRoot;
        this.request = request;
        this.conflictPolicy = conflictPolicy;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        Path extractedPath = null;
        try {
            indicator.setIndeterminate(false);

            indicator.setText("Building spring initializr request...");
            indicator.setFraction(0.1);
            String url = requestBuilder.buildUrl(request);

            indicator.setText("Downloading starter ZIP...");
            indicator.setFraction(0.3);
            DownloadResult downloadResult = downloadWithBootVersionFallback(url, indicator);
            byte[] zip = downloadResult.zip;

            indicator.setText("Extracting project...");
            indicator.setFraction(0.5);
            extractedPath = extractionService.extractToTemp(zip);

            indicator.setText("Copying files into current project...");
            indicator.setFraction(0.7);

            VirtualFile extractedVf = copyService.refreshAndFind(extractedPath);
            if (extractedVf == null) {
                throw new IOException("Unable to map extracted directory to VirtualFile.");
            }

            VirtualFile actualProjectRoot = resolveSingleTopFolder(extractedVf);
            copyService.copyRecursively(project, actualProjectRoot, projectRoot, conflictPolicy);

            indicator.setText("Refreshing project...");
            indicator.setFraction(0.9);
            refreshService.refresh(project, projectRoot);

            indicator.setFraction(1.0);
            if (downloadResult.usedFallback) {
                NotificationUtil.info(project, "Spring Boot project generated successfully using server default Boot version.");
            } else {
                NotificationUtil.info(project, "Spring Boot project generated successfully.");
            }
        } catch (Exception ex) {
            NotificationUtil.error(project, "Failed to generate Spring Boot project: " + toUserMessage(ex));
        } finally {
            if (extractedPath != null) {
                cleanupSilently(extractedPath.getParent());
            }
        }
    }

    private DownloadResult downloadWithBootVersionFallback(String initialUrl,
                                                           ProgressIndicator indicator) throws IOException, InterruptedException {
        try {
            return new DownloadResult(client.downloadStarterZip(initialUrl), false);
        } catch (SpringInitializrHttpException ex) {
            if (shouldRetryWithoutBootVersion(ex)) {
                indicator.setText("Selected Boot version is unsupported. Retrying with server default...");
                SpringBootProjectRequest retryRequest = copyRequestWithoutBootVersion(request);
                String fallbackUrl = requestBuilder.buildUrl(retryRequest);
                return new DownloadResult(client.downloadStarterZip(fallbackUrl), true);
            }
            throw ex;
        }
    }

    private boolean shouldRetryWithoutBootVersion(SpringInitializrHttpException ex) {
        if (ex.getStatusCode() != 400) {
            return false;
        }
        String body = ex.getResponseBody();
        return body != null && body.contains("Invalid Spring Boot version");
    }

    private SpringBootProjectRequest copyRequestWithoutBootVersion(SpringBootProjectRequest source) {
        SpringBootProjectRequest copy = new SpringBootProjectRequest();
        copy.setType(source.getType());
        copy.setLanguage(source.getLanguage());
        copy.setSpringBootVersion(null);
        copy.setGroupId(source.getGroupId());
        copy.setArtifactId(source.getArtifactId());
        copy.setName(source.getName());
        copy.setPackageName(source.getPackageName());
        copy.setPackaging(source.getPackaging());
        copy.setJavaVersion(source.getJavaVersion());
        copy.setDependencies(source.getDependencies());
        return copy;
    }

    private String toUserMessage(Exception ex) {
        if (ex instanceof SpringInitializrHttpException httpEx) {
            String parsedMessage = extractMessageFromJson(httpEx.getResponseBody());
            if (parsedMessage != null) {
                return "Spring Initializr error (HTTP " + httpEx.getStatusCode() + "): " + parsedMessage;
            }
            return "Spring Initializr error (HTTP " + httpEx.getStatusCode() + ").";
        }
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private String extractMessageFromJson(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        Matcher matcher = JSON_MESSAGE_PATTERN.matcher(responseBody);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("\\\"", "\"");
    }

    private VirtualFile resolveSingleTopFolder(VirtualFile extractedRoot) {
        VirtualFile[] children = extractedRoot.getChildren();
        if (children.length == 1 && children[0].isDirectory()) {
            return children[0];
        }
        return extractedRoot;
    }

    private void cleanupSilently(Path root) {
        if (root == null) return;
        try (var walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static class DownloadResult {
        private final byte[] zip;
        private final boolean usedFallback;

        private DownloadResult(byte[] zip, boolean usedFallback) {
            this.zip = zip;
            this.usedFallback = usedFallback;
        }
    }
}
