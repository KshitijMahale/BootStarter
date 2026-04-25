package com.example.bootstarter.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

public class VirtualFileCopyService {

    public void copyRecursively(Project project, VirtualFile sourceDir, VirtualFile targetDir, ConflictPolicy conflictPolicy) {
        ApplicationManager.getApplication().invokeAndWait(() ->
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    copyDirContents(sourceDir, targetDir, conflictPolicy);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
        );
    }

    private void copyDirContents(VirtualFile srcDir, VirtualFile targetDir, ConflictPolicy conflictPolicy) throws IOException {
        for (VirtualFile child : srcDir.getChildren()) {
            if (child.isDirectory()) {
                VirtualFile targetChildDir = targetDir.findChild(child.getName());
                if (targetChildDir == null) {
                    targetChildDir = targetDir.createChildDirectory(this, child.getName());
                } else if (!targetChildDir.isDirectory()) {
                    if (conflictPolicy == ConflictPolicy.SKIP) {
                        continue;
                    }
                    targetChildDir.delete(this);
                    targetChildDir = targetDir.createChildDirectory(this, child.getName());
                }
                copyDirContents(child, targetChildDir, conflictPolicy);
            } else {
                VirtualFile targetFile = targetDir.findChild(child.getName());
                if (targetFile == null) {
                    targetFile = targetDir.createChildData(this, child.getName());
                } else if (targetFile.isDirectory()) {
                    if (conflictPolicy == ConflictPolicy.SKIP) {
                        continue;
                    }
                    targetFile.delete(this);
                    targetFile = targetDir.createChildData(this, child.getName());
                } else if (conflictPolicy == ConflictPolicy.SKIP) {
                    continue;
                }
                targetFile.setBinaryContent(child.contentsToByteArray());
            }
        }
    }

    public VirtualFile refreshAndFind(java.nio.file.Path path) {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
    }
}

