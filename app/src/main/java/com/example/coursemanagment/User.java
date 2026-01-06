package com.example.coursemanagment;

import com.example.eventscalendar.EventModel; // New import
import java.util.HashMap; // New import
import java.util.Map; // New import

public class User {
    public String uid; // We need to store the UID inside the object now
    public String firstName, lastName, email, role;
    public String classId = ""; // Empty means Unassigned
    public Map<String, EventModel> interestedEvents = new HashMap<>(); // Changed to Map<String, EventModel>

    public User() {}

    public User(String uid, String firstName, String lastName, String email, String role) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.classId = "";
        // 'interestedEvents' will be initialized by HashMap's default constructor
    }
}