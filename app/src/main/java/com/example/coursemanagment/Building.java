package com.example.coursemanagment;

import java.util.ArrayList;

public class Building {
    public String id;
    public String name;
    public String type;        // faculty/library/lab/cafeteria/sports/admin
    public String description;
    public int floors;

    public double lat;
    public double lng;

    // ✅ Faculties list (NOT facilities)
    public ArrayList<String> facilities;

    // ✅ Image URLs (ImgBB upload will fill these)
    public ArrayList<String> images;

    public Building() {
        // required for Firebase
    }
}
