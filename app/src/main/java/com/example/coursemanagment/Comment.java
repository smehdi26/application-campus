package com.example.coursemanagment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Comment implements Serializable {
    public String commentId;
    public String postId;
    public String parentCommentId; // For threading: null = top level, otherwise = reply to this comment
    public String content;
    public String authorId;
    public String authorName;
    public String authorRole;
    public String mentionedUserId; // For @mentions (e.g., @professor)
    public String mentionedUserName; // For display
    public long timestamp;
    public int reportCount; // Number of reports
    public boolean isOfficialAnswer; // For teacher's official answer
    public Map<String, Map<String, Boolean>> reactions; // emoji -> {userId: true/false}

    public Comment() {
        this.reactions = new HashMap<>();
    }

    public Comment(String commentId, String postId, String content, String authorId, String authorName, String authorRole, long timestamp) {
        this.commentId = commentId;
        this.postId = postId;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.timestamp = timestamp;
        this.parentCommentId = null; // Top level comment
        this.reportCount = 0;
        this.isOfficialAnswer = false;
        this.reactions = new HashMap<>();
    }
}
