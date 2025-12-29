package com.example.coursemanagment;

public class Material {
    public String key; // NEW: To store the database ID
    public String name;
    public String url;

    public Material() {}

    public Material(String name, String url) {
        this.name = name;
        this.url = url;
    }
}