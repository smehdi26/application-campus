package com.example.coursemanagment;

import java.io.Serializable;

public class NotificationForum implements Serializable {
    public String notificationId;
    public String userId; // User who receives notification
    public String triggeredBy; // User who triggered notification
    public String triggerUserName; // Name of user who triggered
    public String type; // "NEW_POST", "NEW_REPLY", "NEW_REACTION"
    public String postId; // Related post ID
    public String commentId; // Related comment ID (if applicable)
    public String postTitle; // Post title for display
    public String message; // NotificationForum message
    public long timestamp;
    public boolean isRead; // Mark as read/unread

    public NotificationForum() {}

    public NotificationForum(String userId, String triggeredBy, String triggerUserName, String type,
                             String postId, String postTitle, String message) {
        this.notificationId = System.currentTimeMillis() + "_" + userId + "_" + triggeredBy;
        this.userId = userId;
        this.triggeredBy = triggeredBy;
        this.triggerUserName = triggerUserName;
        this.type = type;
        this.postId = postId;
        this.postTitle = postTitle;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }
}
