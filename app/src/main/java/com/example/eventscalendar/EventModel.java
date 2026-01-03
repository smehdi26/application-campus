package com.example.eventscalendar;
public class EventModel {
    private String title;
    private String time;
    private String location;
    private String category; // Ex: "Soutenances", "Exams", "Clubs"
    private String date;     // Format: "yyyy-MM-dd"

    public EventModel(String title, String time, String location, String category, String date) {
        this.title = title;
        this.time = time;
        this.location = location;
        this.category = category;
        this.date = date;
    }

    // Getters
    public String getTitle() { return title; }
    public String getTime() { return time; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
}

