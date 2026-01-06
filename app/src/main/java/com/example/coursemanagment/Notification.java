package com.example.coursemanagment;

public class Notification {
    public String title;
    public String message;
    public String date;

    public Notification() {}

    public Notification(String title, String message, String date) {
        this.title = title;
        this.message = message;
        this.date = date;
    }
}