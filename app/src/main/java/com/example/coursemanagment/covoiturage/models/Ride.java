package com.example.coursemanagment.covoiturage.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Ride implements Serializable {

    public String id;
    public String ownerId; // Firebase UID of the ride creator
    public String ownerName; // Full name of owner
    public String ownerPhone; // Phone number of owner
    public String departure;
    public String destination;
    public String date;
    public String time;
    public int totalSeats; // Total seats available (default 4)
    public int seatsLeft; // Seats remaining
    public double price;
    public String vehicle; // Car model and color
    public String notes; // Optional additional notes
    public Map<String, Booking> bookings; // Map of bookingId -> Booking

    public Ride() {
        this.bookings = new HashMap<>();
        this.totalSeats = 4;
        this.seatsLeft = 4;
    }

    public Ride(String id, String ownerId, String ownerName, String ownerPhone,
                String departure, String destination, String date, String time,
                int totalSeats, double price, String vehicle, String notes) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerPhone = ownerPhone;
        this.departure = departure;
        this.destination = destination;
        this.date = date;
        this.time = time;
        this.totalSeats = totalSeats;
        this.seatsLeft = totalSeats;
        this.price = price;
        this.vehicle = vehicle;
        this.notes = notes != null ? notes : "";
        this.bookings = new HashMap<>();
    }

    // Helper method to check if ride is full
    public boolean isFull() {
        return seatsLeft <= 0;
    }
}
