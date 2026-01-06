package com.example.coursemanagment.covoiturage.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.User;
import com.example.coursemanagment.covoiturage.adapters.RideAdapter;
import com.example.coursemanagment.covoiturage.models.Booking;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.example.coursemanagment.covoiturage.utils.FirebaseRideHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;

public class BrowseRidesFragment extends Fragment {

    private EditText etSearch, etFilterDate, etMinPrice, etMaxPrice;
    private ImageButton btnClearSearch;
    private Button btnToggleFilters;
    private LinearLayout filterPanel;
    private RecyclerView rvRides;
    private RideAdapter rideAdapter;
    private ArrayList<Ride> allRides;
    private ArrayList<Ride> filteredRides;
    private boolean filtersVisible = false;
    private boolean bookingSuccess = false;
    private String selectedRideId = null;
    private FirebaseRideHelper firebaseHelper;
    private FirebaseAuth auth;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_rides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnToggleFilters = view.findViewById(R.id.btnToggleFilters);
        filterPanel = view.findViewById(R.id.filterPanel);
        etFilterDate = view.findViewById(R.id.etFilterDate);
        etMinPrice = view.findViewById(R.id.etMinPrice);
        etMaxPrice = view.findViewById(R.id.etMaxPrice);
        rvRides = view.findViewById(R.id.rvRides);

        // Initialize Firebase
        firebaseHelper = new FirebaseRideHelper();
        auth = FirebaseAuth.getInstance();
        allRides = new ArrayList<>();
        filteredRides = new ArrayList<>();

        setupRecyclerView();
        setupSearch();
        setupFilters();
        loadRidesFromFirebase();
        loadCurrentUserRole();
    }

    private void setupRecyclerView() {
        rideAdapter = new RideAdapter(getContext(), filteredRides,
            ride -> {
                // Handle booking
                handleBooking(ride);
            },
            ride -> {
                // Admin delete
                confirmDeleteRide(ride);
            });
        rideAdapter.setAdminMode(isAdmin);
        rvRides.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRides.setAdapter(rideAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterRides();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            filterRides();
        });
    }

    private void setupFilters() {
        btnToggleFilters.setOnClickListener(v -> {
            filtersVisible = !filtersVisible;
            filterPanel.setVisibility(filtersVisible ? View.VISIBLE : View.GONE);
        });

        etFilterDate.setOnClickListener(v -> showDatePicker());

        etMinPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRides();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        etMaxPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRides();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, year, month, dayOfMonth) -> {
                String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etFilterDate.setText(date);
                filterRides();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void filterRides() {
        filteredRides.clear();
        String searchTerm = etSearch.getText().toString().toLowerCase();
        String filterDate = etFilterDate.getText().toString();
        String minPriceStr = etMinPrice.getText().toString();
        String maxPriceStr = etMaxPrice.getText().toString();

        double minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
        double maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);

        for (Ride ride : allRides) {
            boolean matchesSearch = searchTerm.isEmpty() ||
                ride.departure.toLowerCase().contains(searchTerm) ||
                ride.destination.toLowerCase().contains(searchTerm);

            boolean matchesDate = filterDate.isEmpty() || ride.date.equals(filterDate);
            boolean matchesPrice = ride.price >= minPrice && ride.price <= maxPrice;

            if (matchesSearch && matchesDate && matchesPrice) {
                filteredRides.add(ride);
            }
        }

        rideAdapter.notifyDataSetChanged();
    }

    private void handleBooking(Ride ride) {
        if (ride.isFull()) {
            Toast.makeText(getContext(), "This ride is full!", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Please login to book a ride", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is timed out
        firebaseHelper.checkTimeout(currentUserId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User is timed out
                    new AlertDialog.Builder(getContext())
                        .setTitle("⏰ Account Temporarily Suspended")
                        .setMessage("You have cancelled 5 or more rides in the last 3 days. Your booking privileges have been temporarily suspended.")
                        .setPositiveButton("OK", null)
                        .setCancelable(false)
                        .show();
                } else {
                    // User is not timed out, proceed with booking
                    proceedWithBooking(ride, currentUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // On error, allow booking (don't block user)
                proceedWithBooking(ride, currentUserId);
            }
        });
    }

    private void proceedWithBooking(Ride ride, String currentUserId) {
        // Check if user already booked this ride
        if (ride.bookings != null) {
            for (Booking booking : ride.bookings.values()) {
                if (booking.passengerId != null && booking.passengerId.equals(currentUserId)) {
                    Toast.makeText(getContext(), "You have already booked this ride!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // Get current user info to create booking
        firebaseHelper.getUserInfo(currentUserId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    // Show dialog to get phone number and seats
                    showBookingDialog(ride, user, currentUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBookingDialog(Ride ride, User user, String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_booking, null);
        builder.setView(dialogView);

        EditText etBookingPhone = dialogView.findViewById(R.id.etBookingPhone);
        EditText etBookingSeats = dialogView.findViewById(R.id.etBookingSeats);
        TextView tvAvailableSeats = dialogView.findViewById(R.id.tvAvailableSeats);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelBooking);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmBooking);

        // Set available seats info
        tvAvailableSeats.setText("Available: " + ride.seatsLeft + " seat" + (ride.seatsLeft > 1 ? "s" : ""));

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

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

            int seats = Integer.parseInt(seatsStr);
            if (seats < 1 || seats > ride.seatsLeft) {
                etBookingSeats.setError("Must be between 1 and " + ride.seatsLeft);
                return;
            }

            // Create booking
            String passengerName = user.firstName + " " + user.lastName;
            Booking booking = new Booking(null, ride.id, userId, passengerName, phone, user.role, seats);

            firebaseHelper.bookRide(ride.id, booking, (error, ref) -> {
                dialog.dismiss();
                if (error == null) {
                    selectedRideId = ride.id;
                    bookingSuccess = true;

                    // Show success notification
                    showBookingSuccessNotification(ride);

                    // Reload rides to update seats
                    loadRidesFromFirebase();
                } else if (error.getMessage() != null && error.getMessage().contains("Not enough seats")) {
                    Toast.makeText(getContext(), "Not enough seats left for this ride.", Toast.LENGTH_SHORT).show();
                    // Refresh list to show current availability
                    loadRidesFromFirebase();
                } else {
                    Toast.makeText(getContext(), "Failed to book ride: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showBookingSuccessNotification(Ride ride) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("✅ Booking Confirmed!")
            .setMessage("Your ride with " + ride.ownerName + 
                "\nFrom: " + ride.departure + 
                "\nTo: " + ride.destination +
                "\nDate: " + ride.date + " at " + ride.time +
                "\nPrice: " + String.format("%.1f TND", ride.price) +
                "\n\nYou can view your booked rides in 'My Booked Rides' section.")
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }

    private void loadRidesFromFirebase() {
        firebaseHelper.getAllRides(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRides = FirebaseRideHelper.snapshotListToRides(snapshot);
                filterRides();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading rides: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentUserRole() {
        if (auth.getCurrentUser() == null) return;

        firebaseHelper.getUserInfo(auth.getCurrentUser().getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                boolean admin = user != null && user.role != null && user.role.equalsIgnoreCase("admin");
                isAdmin = admin;
                if (rideAdapter != null) {
                    rideAdapter.setAdminMode(admin);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void confirmDeleteRide(Ride ride) {
        if (!isAdmin) return;

        new AlertDialog.Builder(getContext())
            .setTitle("Delete Ride")
            .setMessage("Are you sure you want to delete this ride?")
            .setPositiveButton("Delete", (dialog, which) -> {
                firebaseHelper.deleteRide(ride.id, (error, ref) -> {
                    if (error == null) {
                        Toast.makeText(getContext(), "Ride deleted", Toast.LENGTH_SHORT).show();
                        loadRidesFromFirebase();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete ride: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

}

