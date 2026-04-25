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

public class SpringBootProjectGenerationTask extends Task.Backgroundable {

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
        super(project, "Generating Spring Boot Project", true);
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

            indicator.setText("Building Spring Initializr request...");
            indicator.setFraction(0.1);
            String url = requestBuilder.buildUrl(request);

            indicator.setText("Downloading starter ZIP...");
            indicator.setFraction(0.3);
            byte[] zip = client.downloadStarterZip(url);

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
            NotificationUtil.info(project, "Spring Boot project generated successfully.");
        } catch (Exception ex) {
            NotificationUtil.error(project, "Failed to generate Spring Boot project: " + ex.getMessage());
        } finally {
            if (extractedPath != null) {
                cleanupSilently(extractedPath.getParent());
            }
        }
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
}

