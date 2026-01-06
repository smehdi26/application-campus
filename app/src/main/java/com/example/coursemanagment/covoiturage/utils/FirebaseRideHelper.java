package com.example.coursemanagment.covoiturage.utils;

import androidx.annotation.NonNull;
import com.example.coursemanagment.User;
import com.example.coursemanagment.covoiturage.models.Booking;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.MutableData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirebaseRideHelper {
    
    private static final String RIDES_PATH = "Rides";
    private static final String USERS_PATH = "Users";
    
    private DatabaseReference ridesRef;
    private DatabaseReference usersRef;
    private FirebaseAuth auth;
    
    public FirebaseRideHelper() {
        ridesRef = FirebaseDatabase.getInstance().getReference(RIDES_PATH);
        usersRef = FirebaseDatabase.getInstance().getReference(USERS_PATH);
        auth = FirebaseAuth.getInstance();
    }
    
    // Create a new ride
    public void createRide(Ride ride, DatabaseReference.CompletionListener listener) {
        String rideId = ridesRef.push().getKey();
        if (rideId != null) {
            ride.id = rideId;
            // Ensure seatsLeft is initialized in the DB (older flows might skip it)
            if (ride.totalSeats > 0) {
                ride.seatsLeft = ride.totalSeats;
            }
            ridesRef.child(rideId).setValue(ride, listener);
        }
    }
    
    // Update a ride
    public void updateRide(String rideId, Ride ride, DatabaseReference.CompletionListener listener) {
        ridesRef.child(rideId).setValue(ride, listener);
    }
    
    // Delete a ride
    public void deleteRide(String rideId, DatabaseReference.CompletionListener listener) {
        ridesRef.child(rideId).removeValue(listener);
    }
    
    // Get all rides
    public void getAllRides(ValueEventListener listener) {
        ridesRef.addValueEventListener(listener);
    }
    
    // Get rides by owner
    public void getRidesByOwner(String ownerId, ValueEventListener listener) {
        ridesRef.orderByChild("ownerId").equalTo(ownerId).addValueEventListener(listener);
    }
    
    // Get a single ride
    public void getRide(String rideId, ValueEventListener listener) {
        ridesRef.child(rideId).addListenerForSingleValueEvent(listener);
    }
    
    // Book a ride
    public void bookRide(String rideId, Booking booking, DatabaseReference.CompletionListener listener) {
        String bookingId = ridesRef.child(rideId).child("bookings").push().getKey();
        if (bookingId == null) {
            if (listener != null) {
                listener.onComplete(DatabaseError.fromException(new Exception("Unable to create booking id")), null);
            }
            return;
        }

        booking.id = bookingId;
        DatabaseReference rideRef = ridesRef.child(rideId);

        // Run a transaction so seatsLeft and bookings stay in sync and cannot overbook
        rideRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Ride ride = currentData.getValue(Ride.class);
                if (ride == null) {
                    return Transaction.abort();
                }

                int seatsLeft = ride.seatsLeft > 0 ? ride.seatsLeft : ride.totalSeats;
                if (seatsLeft < booking.seatsBooked) {
                    // Not enough seats, abort the transaction
                    return Transaction.abort();
                }

                // Decrease seats and add the booking in the same atomic operation
                currentData.child("seatsLeft").setValue(seatsLeft - booking.seatsBooked);
                currentData.child("bookings").child(bookingId).setValue(booking);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (!committed && error == null) {
                    // Aborted because seats were insufficient
                    error = DatabaseError.fromException(new Exception("Not enough seats left"));
                }

                if (listener != null) {
                    listener.onComplete(error, rideRef);
                }
            }
        });
    }
    
    // Cancel a booking
    public void cancelBooking(String rideId, String bookingId, int seatsBooked, DatabaseReference.CompletionListener listener) {
        ridesRef.child(rideId).child("bookings").child(bookingId).removeValue((error, ref) -> {
            if (error == null) {
                // Update seats left
                updateSeatsLeft(rideId, seatsBooked, listener);
            } else {
                listener.onComplete(error, ref);
            }
        });
    }
    
    // Update a booking
    public void updateBooking(String rideId, Booking booking, int seatChange, DatabaseReference.CompletionListener listener) {
        DatabaseReference rideRef = ridesRef.child(rideId);

        // Use a transaction to avoid overbooking when increasing seats
        rideRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Ride ride = currentData.getValue(Ride.class);
                if (ride == null || booking.id == null) {
                    return Transaction.abort();
                }

                int seatsLeft = ride.seatsLeft > 0 ? ride.seatsLeft : ride.totalSeats;

                // If user is trying to book more seats than available, abort
                if (seatChange > 0 && seatsLeft < seatChange) {
                    return Transaction.abort();
                }

                // Update seatsLeft and booking atomically
                currentData.child("seatsLeft").setValue(Math.max(0, seatsLeft - seatChange));
                currentData.child("bookings").child(booking.id).setValue(booking);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (!committed && error == null) {
                    error = DatabaseError.fromException(new Exception("Not enough seats left"));
                }
                if (listener != null) {
                    listener.onComplete(error, rideRef);
                }
            }
        });
    }
    
    // Track cancellation for timeout system
    public void trackCancellation(String userId) {
        DatabaseReference cancelRef = FirebaseDatabase.getInstance().getReference("Cancellations").child(userId);
        long currentTime = System.currentTimeMillis();
        cancelRef.push().setValue(currentTime);
        
        // Clean up old cancellations (older than 3 days)
        cancelRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long threeDaysAgo = currentTime - (3 * 24 * 60 * 60 * 1000L);
                int count = 0;
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Long timestamp = ds.getValue(Long.class);
                    if (timestamp != null && timestamp > threeDaysAgo) {
                        count++;
                    } else {
                        ds.getRef().removeValue();
                    }
                }
                
                // Check if user should be timed out (5+ cancellations in 3 days)
                if (count >= 5) {
                    // Store timeout status
                    FirebaseDatabase.getInstance().getReference("UserTimeouts")
                        .child(userId)
                        .setValue(true);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    // Check if user is timed out
    public void checkTimeout(String userId, ValueEventListener listener) {
        FirebaseDatabase.getInstance().getReference("UserTimeouts")
            .child(userId)
            .addListenerForSingleValueEvent(listener);
    }
    
    // Update seats left
    private void updateSeatsLeft(String rideId, int change, DatabaseReference.CompletionListener listener) {
        ridesRef.child(rideId).child("seatsLeft").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentSeats = snapshot.getValue(Integer.class);
                if (currentSeats != null) {
                    int newSeats = Math.max(0, currentSeats + change);
                    ridesRef.child(rideId).child("seatsLeft").setValue(newSeats, (error, ref) -> {
                        if (listener != null) {
                            listener.onComplete(error, ref);
                        }
                    });
                } else if (listener != null) {
                    listener.onComplete(null, null);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onComplete(error, null);
                }
            }
        });
    }
    
    // Get user info
    public void getUserInfo(String uid, ValueEventListener listener) {
        usersRef.child(uid).addListenerForSingleValueEvent(listener);
    }
    
    // Get active users count
    public void getActiveUsersCount(ValueEventListener listener) {
        usersRef.addListenerForSingleValueEvent(listener);
    }
    
    // Convert DataSnapshot to Ride
    public static Ride snapshotToRide(DataSnapshot snapshot) {
        Ride ride = snapshot.getValue(Ride.class);
        if (ride != null) {
            ride.id = snapshot.getKey();
            // Convert bookings map
            if (snapshot.child("bookings").exists()) {
                ride.bookings = new HashMap<>();
                int seatsBookedTotal = 0;
                for (DataSnapshot bookingSnap : snapshot.child("bookings").getChildren()) {
                    Booking booking = bookingSnap.getValue(Booking.class);
                    if (booking != null) {
                        booking.id = bookingSnap.getKey();
                        ride.bookings.put(booking.id, booking);
                        seatsBookedTotal += booking.seatsBooked;
                    }
                }
                // If seatsLeft wasn't stored (older rides), derive it from totalSeats - booked
                if (ride.seatsLeft <= 0 && ride.totalSeats > 0) {
                    ride.seatsLeft = Math.max(ride.totalSeats - seatsBookedTotal, 0);
                }
            } else if (ride.seatsLeft <= 0 && ride.totalSeats > 0) {
                // No bookings and seatsLeft missing -> fall back to totalSeats
                ride.seatsLeft = ride.totalSeats;
            }
        }
        return ride;
    }
    
    // Convert DataSnapshot list to ArrayList
    public static ArrayList<Ride> snapshotListToRides(DataSnapshot snapshot) {
        ArrayList<Ride> rides = new ArrayList<>();
        for (DataSnapshot ds : snapshot.getChildren()) {
            Ride ride = snapshotToRide(ds);
            if (ride != null) {
                rides.add(ride);
            }
        }
        return rides;
    }
    
    // Get current user ID
    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
}

