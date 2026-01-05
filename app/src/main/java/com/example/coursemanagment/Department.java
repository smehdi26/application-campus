package com.example.coursemanagment;

public class Department {
    public String id;
    public String name;

    public Department() {}

    public Department(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Helper for Spinner
    @Override
    public String toString() {
        return name;
    }
}