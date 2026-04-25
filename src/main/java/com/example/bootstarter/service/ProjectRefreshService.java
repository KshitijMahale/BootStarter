package com.example.bootstarter.service;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

public class ProjectRefreshService {

    public void refresh(Project project, VirtualFile root) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFileManager.getInstance().syncRefresh();
            root.refresh(true, true);
            ProjectView.getInstance(project).refresh();
        });
    }
}

