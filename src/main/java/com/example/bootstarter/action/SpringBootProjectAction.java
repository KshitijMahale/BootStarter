package com.example.bootstarter.action;

import com.example.bootstarter.model.SpringBootProjectRequest;
import com.example.bootstarter.service.ConflictPolicy;
import com.example.bootstarter.service.SpringBootProjectGenerationTask;
import com.example.bootstarter.ui.SpringBootProjectDialog;
import com.example.bootstarter.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class SpringBootProjectAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            NotificationUtil.error(project, "Cannot resolve current project directory.");
            return;
        }

        VirtualFile root = project.getBaseDir();
        if (root == null) {
            NotificationUtil.error(project, "Cannot resolve current project root VirtualFile.");
            return;
        }

        SpringBootProjectDialog dialog = new SpringBootProjectDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }

        SpringBootProjectRequest request = dialog.toRequest();

        ConflictPolicy conflictPolicy = ConflictPolicy.OVERWRITE;
        if (root.getChildren().length > 0) {
            int choice = Messages.showYesNoCancelDialog(
                    project,
                    "Current project directory is not empty. Choose how to handle file conflicts.",
                    "Directory Not Empty",
                    "Overwrite",
                    "Skip Existing",
                    "Cancel",
                    Messages.getWarningIcon()
            );
            if (choice == Messages.CANCEL) {
                return;
            }
            conflictPolicy = (choice == Messages.NO) ? ConflictPolicy.SKIP : ConflictPolicy.OVERWRITE;
        }

        new SpringBootProjectGenerationTask(project, root, request, conflictPolicy).queue();
    }
}

