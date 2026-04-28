package com.example.bootstarter.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;

public final class NotificationUtil {
    private static final String GROUP_ID = "SpringBootCommunityNotifications";

    private NotificationUtil() {}

    public static void info(Project project, String message) {
        notify(project, message, NotificationType.INFORMATION);
    }

    public static void error(Project project, String message) {
        notify(project, message, NotificationType.ERROR);
    }

    private static void notify(Project project, String message, NotificationType type) {
        try {
            if (!canNotify(project)) {
                return;
            }
            Notifications.Bus.notify(new Notification(GROUP_ID, "Spring Boot Community", message, type), project);
        } catch (ProcessCanceledException ignored) {
            // App/project is shutting down.
        } catch (RuntimeException ignored) {
            // Avoid noisy failures from disposed services during shutdown.
        }
    }

    private static boolean canNotify(Project project) {
        var app = ApplicationManager.getApplication();
        if (app == null || app.isDisposed()) {
            return false;
        }
        return project == null || !project.isDisposed();
    }
}

