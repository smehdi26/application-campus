package com.example.coursemanagment.covoiturage.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.covoiturage.adapters.BookedRideAdapter;
import com.example.coursemanagment.covoiturage.models.Booking;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.example.coursemanagment.covoiturage.utils.FirebaseRideHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyBookedRidesFragment extends Fragment {

    private RecyclerView rvBookedRides;
    private LinearLayout emptyState;
    private BookedRideAdapter adapter;
    private FirebaseRideHelper firebaseHelper;
    private FirebaseAuth auth;
    private String currentUserId;
    private ArrayList<Ride> bookedRides;
    private Map<String, Booking> bookingMap; // rideId -> Booking

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_booked_rides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvBookedRides = view.findViewById(R.id.rvBookedRides);
        emptyState = view.findViewById(R.id.emptyState);

        firebaseHelper = new FirebaseRideHelper();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        bookedRides = new ArrayList<>();
        bookingMap = new HashMap<>();

        rvBookedRides.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookedRideAdapter(getContext(), bookedRides, bookingMap, this::onModifyBooking, this::onCancelBooking);
        rvBookedRides.setAdapter(adapter);

        loadBookedRides();
    }

    private void loadBookedRides() {
        if (currentUserId == null) {
            showEmptyState();
            return;
        }

        firebaseHelper.getAllRides(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookedRides.clear();
                bookingMap.clear();

                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    Ride ride = FirebaseRideHelper.snapshotToRide(rideSnap);
                    if (ride != null && ride.bookings != null) {
                        // Check if current user has a booking in this ride
                        for (Booking booking : ride.bookings.values()) {
                            if (booking.passengerId != null && booking.passengerId.equals(currentUserId)) {
                                bookedRides.add(ride);
                                bookingMap.put(ride.id, booking);
                                break;
                            }
                        }
                    }
                }

                if (bookedRides.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading booked rides", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        rvBookedRides.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyState.setVisibility(View.GONE);
        rvBookedRides.setVisibility(View.VISIBLE);
    }

    private void onModifyBooking(Ride ride, Booking booking) {
        showModifyBookingDialog(ride, booking);
    }

    private void showModifyBookingDialog(Ride ride, Booking booking) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_booking, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etBookingPhone = dialogView.findViewById(R.id.etBookingPhone);
        EditText etBookingSeats = dialogView.findViewById(R.id.etBookingSeats);
        TextView tvAvailableSeats = dialogView.findViewById(R.id.tvAvailableSeats);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelBooking);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmBooking);

        // Update title for modification
        if (tvTitle != null) {
            tvTitle.setText("Modify Booking Info");
        }

        // Update button text
        btnConfirm.setText("Confirm Modification");

        // Pre-fill current values
        etBookingPhone.setText(booking.passengerPhone);
        etBookingSeats.setText(String.valueOf(booking.seatsBooked));

        // Calculate available seats (current seats + seats being modified)
        int availableSeats = ride.seatsLeft + booking.seatsBooked;
        tvAvailableSeats.setText("Available: " + availableSeats + " seat" + (availableSeats > 1 ? "s" : ""));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String phone = etBookingPhone.getText().toString().trim();
            String seatsStr = etBookingSeats.getText().toString().trim();

            if (TextUtils.isEmpty(phone)) {
                etBookingPhone.setError("Phone number is required");
                return;
            }

            if (TextUtils.isEmpty(seatsStr)) {
                etBookingSeats.setError("Number of seats is required");
                return;
            }

            int newSeats = Integer.parseInt(seatsStr);
            int seatChange = newSeats - booking.seatsBooked;

            if (newSeats < 1 || newSeats > availableSeats) {
                etBookingSeats.setError("Must be between 1 and " + availableSeats);
                return;
            }

            // Update booking
            booking.passengerPhone = phone;
            booking.seatsBooked = newSeats;

            // Update in Firebase
            firebaseHelper.updateBooking(ride.id, booking, seatChange, (error, ref) -> {
                dialog.dismiss();
                if (error == null) {
                    Toast.makeText(getContext(), "Booking updated successfully!", Toast.LENGTH_SHORT).show();
                    loadBookedRides();
                } else if (error.getMessage() != null && error.getMessage().contains("Not enough seats")) {
                    Toast.makeText(getContext(), "Not enough seats left for this change.", Toast.LENGTH_SHORT).show();
                    loadBookedRides();
                } else {
                    Toast.makeText(getContext(), "Failed to update booking: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void onCancelBooking(Ride ride, Booking booking) {
        new AlertDialog.Builder(getContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Yes", (dialog, which) -> {
                firebaseHelper.cancelBooking(ride.id, booking.id, booking.seatsBooked, (error, ref) -> {
                    if (error == null) {
                        // Track cancellation
                        firebaseHelper.trackCancellation(currentUserId);
                        
                        Toast.makeText(getContext(), "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                        loadBookedRides();
                    } else {
                        Toast.makeText(getContext(), "Failed to cancel booking: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("No", null)
            .show();
    }
}

