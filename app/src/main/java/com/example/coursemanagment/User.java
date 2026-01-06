package com.example.coursemanagment;

import com.google.firebase.database.Exclude;
import java.util.Map;

public class User {
    public String uid; // We need to store the UID inside the object now
    public String firstName, lastName, email, role;
    public String classId = ""; // Empty means Unassigned
    public int reputation = 0; // NEW: Reputation points for gamification

    @Exclude
    public Map<String, Object> notifications; // To ignore "Notifications" field from direct User mapping

    public User() {}

    public User(String uid, String firstName, String lastName, String email, String role) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.classId = "";
        this.reputation = 0;
    }
}