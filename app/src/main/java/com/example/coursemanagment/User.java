package com.example.coursemanagment;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String uid; // We need to store the UID inside the object now
    public String firstName, lastName, email, role;
    public String classId = ""; // Empty means Unassigned
    public List<String> interestedEvents = new ArrayList<>(); // Dedicated field for calendar events

    public User() {}

    public User(String uid, String firstName, String lastName, String email, String role) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.classId = "";
        // 'interestedEvents' will be initialized by ArrayList's default constructor
    }
}