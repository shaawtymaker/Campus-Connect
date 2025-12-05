package com.harsh.shah.threads.clone.model;

public class NotificationModel {
    private String notificationId;
    private String type; // FOLLOW, LIKE, REPLY
    private String fromUserId;
    private String timestamp;
    private String threadId;
    private String message;

    public NotificationModel() {
    }

    public NotificationModel(String notificationId, String type, String fromUserId, String timestamp, String threadId, String message) {
        this.notificationId = notificationId;
        this.type = type;
        this.fromUserId = fromUserId;
        this.timestamp = timestamp;
        this.threadId = threadId;
        this.message = message;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
