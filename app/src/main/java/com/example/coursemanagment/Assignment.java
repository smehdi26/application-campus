package com.example.coursemanagment;

public class Assignment {
    public String id;
    public String title;
    public String dueDate; // Display String (e.g., "Monday, 01/01/2026 12:00")
    public long timestamp; // NEW: Machine time for logic

    public Assignment() {}

    public Assignment(String id, String title, String dueDate, long timestamp) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.timestamp = timestamp;
    }
}