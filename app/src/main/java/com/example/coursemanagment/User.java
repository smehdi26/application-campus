package com.example.coursemanagment;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.HashMap;
import java.util.Map;
import com.example.eventscalendar.EventModel; // New import

@IgnoreExtraProperties
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