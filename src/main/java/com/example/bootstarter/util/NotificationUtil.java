package com.example.bootstarter.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public final class NotificationUtil {
    private static final String GROUP_ID = "SpringBootCommunityNotifications";

    private NotificationUtil() {}

    public static void info(Project project, String message) {
        Notifications.Bus.notify(
                new Notification(GROUP_ID, "Spring Boot Community", message, NotificationType.INFORMATION),
                project
        );
    }

    public static void error(Project project, String message) {
        Notifications.Bus.notify(
                new Notification(GROUP_ID, "Spring Boot Community", message, NotificationType.ERROR),
                project
        );
    }
}

