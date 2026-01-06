package com.example.coursemanagment.covoiturage.models;

import java.io.Serializable;

public class Booking implements Serializable {
    public String id;
    public String rideId;
    public String passengerId; // Firebase UID
    public String passengerName; // Full name
    public String passengerPhone; // Phone number
    public String passengerRole; // Student, Teacher, or Admin
    public int seatsBooked;

    public Booking() {}

    public Booking(String id, String rideId, String passengerId, String passengerName,
                   String passengerPhone, String passengerRole, int seatsBooked) {
        this.id = id;
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        this.passengerPhone = passengerPhone;
        this.passengerRole = passengerRole;
        this.seatsBooked = seatsBooked;
    }
}

