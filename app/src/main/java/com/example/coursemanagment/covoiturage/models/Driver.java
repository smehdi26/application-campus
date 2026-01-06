package com.example.coursemanagment.covoiturage.models;

import java.io.Serializable;

public class Driver implements Serializable {
    public String id;
    public String name;
    public double rating;
    public int trips;

    public Driver() {}

    public Driver(String id, String name, double rating, int trips) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.trips = trips;
    }
}

