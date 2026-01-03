package com.example.eventscalendar;
public class EventModel {
    private String id; // Unique ID for Realtime Database
    private String title;
    private String time;
    private String location;
    private String category; // Ex: "Soutenances", "Exams", "Clubs"
    private String date;     // Format: "yyyy-MM-dd"
    private String description; // Add description field

    // Required no-argument constructor for Realtime Database
    public EventModel() {
        // Default constructor required for calls to DataSnapshot.getValue(EventModel.class)
    }

    public EventModel(String title, String time, String location, String category, String date) {
        this.title = title;
        this.time = time;
        this.location = location;
        this.category = category;
        this.date = date;
    }

    public EventModel(String title, String time, String location, String category, String date, String description) {
        this.title = title;
        this.time = time;
        this.location = location;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getTime() { return time; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getDescription() { return description; }

    // Setters (required for Realtime Database deserialization if not set in constructor)
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setTime(String time) { this.time = time; }
    public void setLocation(String location) { this.location = location; }
    public void setCategory(String category) { this.category = category; }
    public void setDate(String date) { this.date = date; }
    public void setDescription(String description) { this.description = description; }
}

