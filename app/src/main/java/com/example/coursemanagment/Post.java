package com.example.coursemanagment;

import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Post implements Serializable {
    public String postId;
    public String title;
    public String content;
    public String authorId;
    public String authorName;
    public String authorRole;
    public String subject; // Subject/Tag (e.g., "Mobile Development", "Database Systems")
    public String status; // PENDING, PUBLISHED, HIDDEN
    public String type; // DISCUSSION, ANNOUNCEMENT
    public String attachmentUrl; // Link to file (Drive, etc.)
    public String attachmentName; // Name of attached file
    public String targetClassId; // For announcements targeting specific class (optional)
    public long timestamp;
    public long lastReplyTimestamp; // For "Last reply: X ago"
    public int commentCount;
    public int viewCount; // For popularity sorting
    public boolean isNew; // For red dot indicator
    public boolean isOfficialAnswer; // For teacher's official answer
    public boolean isPinned; // Pinned posts appear at the top
    public Map<String, Map<String, Boolean>> reactions; // emoji -> {userId: true/false}

    public Post() {
        this.reactions = new HashMap<>();
    }

    public Post(String postId, String title, String content, String authorId, String authorName, String authorRole, long timestamp) {
        this.postId = postId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.timestamp = timestamp;
        this.commentCount = 0;
        this.viewCount = 0;
        this.subject = "General";
        this.status = "PUBLISHED";
        this.type = "DISCUSSION";
        this.lastReplyTimestamp = timestamp;
        this.isNew = false;
        this.isOfficialAnswer = false;
        this.isPinned = false;
        this.reactions = new HashMap<>();
    }
}
