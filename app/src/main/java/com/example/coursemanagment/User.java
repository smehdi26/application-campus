package com.example.coursemanagment;

public class User {
    public String uid;
    public String firstName, lastName, email, role;

    // --- THIS FIELD WAS LIKELY MISSING ---
    public String classId = "";

    public User() {}

    public User(String uid, String firstName, String lastName, String email, String role) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.classId = "";
    }
}