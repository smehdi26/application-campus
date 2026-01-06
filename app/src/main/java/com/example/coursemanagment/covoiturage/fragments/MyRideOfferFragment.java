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
import com.example.coursemanagment.User;
import com.example.coursemanagment.covoiturage.adapters.BookingAdapter;
import com.example.coursemanagment.covoiturage.models.Booking;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.example.coursemanagment.covoiturage.utils.FirebaseRideHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;

public class MyRideOfferFragment extends Fragment {

    private Ride rideData;
    private LinearLayout emptyState, activeRideContent;
    private Button btnCreateRide, btnModifyRide, btnDeleteRide;
    private TextView tvMyRideDeparture, tvMyRideDestination, tvMyRideDate, tvMyRideTime;
    private TextView tvMyRideVehicle, tvMyRidePrice, tvMyRideNotes, tvSeatsStatus, tvBookingsTitle;
    private LinearLayout seatsContainer;
    private RecyclerView rvBookings;
    private BookingAdapter bookingAdapter;
    private OnCreateRideListener onCreateRideListener;
    private FirebaseRideHelper firebaseHelper;
    private FirebaseAuth auth;
    private String currentUserId;

    public interface OnCreateRideListener {
        void onCreateRide();
    }

    public void setOnCreateRideListener(OnCreateRideListener listener) {
        this.onCreateRideListener = listener;
    }

    public void setRideData(Ride ride) {
        this.rideData = ride;
        if (getView() != null) {
            updateUI();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_ride_offer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyState = view.findViewById(R.id.emptyState);
        activeRideContent = view.findViewById(R.id.activeRideContent);
        btnCreateRide = view.findViewById(R.id.btnCreateRide);
        tvMyRideDeparture = view.findViewById(R.id.tvMyRideDeparture);
        tvMyRideDestination = view.findViewById(R.id.tvMyRideDestination);
        tvMyRideDate = view.findViewById(R.id.tvMyRideDate);
        tvMyRideTime = view.findViewById(R.id.tvMyRideTime);
        tvMyRideVehicle = view.findViewById(R.id.tvMyRideVehicle);
        tvMyRidePrice = view.findViewById(R.id.tvMyRidePrice);
        tvMyRideNotes = view.findViewById(R.id.tvMyRideNotes);
        tvSeatsStatus = view.findViewById(R.id.tvSeatsStatus);
        tvBookingsTitle = view.findViewById(R.id.tvBookingsTitle);
        seatsContainer = view.findViewById(R.id.seatsContainer);
        rvBookings = view.findViewById(R.id.rvBookings);

        firebaseHelper = new FirebaseRideHelper();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        btnCreateRide = view.findViewById(R.id.btnCreateRide);
        btnModifyRide = view.findViewById(R.id.btnModifyRide);
        btnDeleteRide = view.findViewById(R.id.btnDeleteRide);

        btnCreateRide.setOnClickListener(v -> {
            if (onCreateRideListener != null) {
                onCreateRideListener.onCreateRide();
            }
        });

        if (btnModifyRide != null) {
            btnModifyRide.setOnClickListener(v -> showModifyRideDialog());
        }

        if (btnDeleteRide != null) {
            btnDeleteRide.setOnClickListener(v -> showDeleteRideDialog());
        }

        loadMyRide();
    }

    private void updateUI() {
        if (rideData == null || rideData.departure == null || rideData.departure.isEmpty()) {
            // Show empty state
            emptyState.setVisibility(View.VISIBLE);
            activeRideContent.setVisibility(View.GONE);
        } else {
            // Show active ride
            emptyState.setVisibility(View.GONE);
            activeRideContent.setVisibility(View.VISIBLE);
            displayRideData();
        }
    }

    private void displayRideData() {
        tvMyRideDeparture.setText(rideData.departure);
        tvMyRideDestination.setText(rideData.destination);
        tvMyRideDate.setText(rideData.date);
        tvMyRideTime.setText(rideData.time);
        tvMyRideVehicle.setText(rideData.vehicle);
        tvMyRidePrice.setText(String.format("%.1f TND", rideData.price));
        tvMyRideNotes.setText(rideData.notes != null && !rideData.notes.isEmpty() ? rideData.notes : "No additional notes");

        // Setup seats visualization
        setupSeatsVisualization();

        // Setup bookings
        setupBookings();
    }

    private void setupSeatsVisualization() {
        seatsContainer.removeAllViews();
        int totalSeats = rideData.totalSeats;
        int seatsLeft = rideData.seatsLeft;
        int bookedSeats = totalSeats - seatsLeft;

        tvSeatsStatus.setText(seatsLeft + "/" + totalSeats + " available");

        for (int i = 0; i < totalSeats; i++) {
            View seatView = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
            );
            params.setMargins(4, 0, 4, 0);
            seatView.setLayoutParams(params);

            if (i < bookedSeats) {
                seatView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.covoiturage_red_primary));
            } else {
                seatView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.covoiturage_light_gray_bg));
            }

            seatView.setMinimumHeight(60);
            seatsContainer.addView(seatView);
        }
    }

    private void setupBookings() {
        ArrayList<Booking> bookings = getMockBookings();
        
        if (bookings.isEmpty()) {
            tvBookingsTitle.setText("Passenger Bookings (0)");
            // Show empty bookings state
            rvBookings.setVisibility(View.GONE);
        } else {
            tvBookingsTitle.setText("Passenger Bookings (" + bookings.size() + ")");
            rvBookings.setVisibility(View.VISIBLE);
            bookingAdapter = new BookingAdapter(getContext(), bookings);
            rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
            rvBookings.setAdapter(bookingAdapter);
        }
    }

    private ArrayList<Booking> getMockBookings() {
        ArrayList<Booking> bookings = new ArrayList<>();
        if (rideData != null && rideData.id != null && rideData.bookings != null) {
            // Get bookings from ride data
            bookings.addAll(rideData.bookings.values());
        }
        return bookings;
    }

    private void loadMyRide() {
        if (currentUserId == null) {
            updateUI();
            return;
        }

        firebaseHelper.getRidesByOwner(currentUserId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    // Get the first ride (assuming one ride per owner for now)
                    DataSnapshot firstRide = snapshot.getChildren().iterator().next();
                    rideData = FirebaseRideHelper.snapshotToRide(firstRide);
                    updateUI();
                } else {
                    rideData = null;
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading ride", Toast.LENGTH_SHORT).show();
                rideData = null;
                updateUI();
            }
        });
    }

    private void showModifyRideDialog() {
        if (rideData == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_create_ride, null);
        builder.setView(dialogView);

        EditText etDeparture = dialogView.findViewById(R.id.etDeparture);
        EditText etDestination = dialogView.findViewById(R.id.etDestination);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etTime = dialogView.findViewById(R.id.etTime);
        EditText etSeats = dialogView.findViewById(R.id.etSeats);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etVehicle = dialogView.findViewById(R.id.etVehicle);
        EditText etNotes = dialogView.findViewById(R.id.etNotes);
        EditText etPhoneNumber = dialogView.findViewById(R.id.etPhone);
        Button btnCreateRide = dialogView.findViewById(R.id.btnCreateRide);

        // Hide the "Create Ride Offer" button since we're modifying, not creating
        if (btnCreateRide != null) {
            btnCreateRide.setVisibility(View.GONE);
        }

        // Pre-fill current values
        etDeparture.setText(rideData.departure);
        etDestination.setText(rideData.destination);
        etDate.setText(rideData.date);
        etTime.setText(rideData.time);
        etSeats.setText(String.valueOf(rideData.totalSeats));
        etPrice.setText(String.valueOf(rideData.price));
        etVehicle.setText(rideData.vehicle);
        etNotes.setText(rideData.notes);
        if (etPhoneNumber != null) {
            etPhoneNumber.setText(rideData.ownerPhone);
        }

        // Setup date picker
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new android.app.DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    etDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Setup time picker
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new android.app.TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    etTime.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true).show();
        });

        AlertDialog dialog = builder.setTitle("Modify Ride")
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", (d, which) -> d.dismiss())
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                // Validate and save
                if (validateAndSaveRide(etDeparture, etDestination, etDate, etTime, etSeats, etPrice, etVehicle, etNotes, etPhoneNumber)) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private boolean validateAndSaveRide(EditText etDeparture, EditText etDestination, EditText etDate,
                                       EditText etTime, EditText etSeats, EditText etPrice,
                                       EditText etVehicle, EditText etNotes, EditText etPhoneNumber) {
        String departure = etDeparture.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String vehicle = etVehicle.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String phone = etPhoneNumber != null ? etPhoneNumber.getText().toString().trim() : rideData.ownerPhone;

        if (TextUtils.isEmpty(departure) || TextUtils.isEmpty(destination) ||
            TextUtils.isEmpty(date) || TextUtils.isEmpty(time) ||
            TextUtils.isEmpty(seatsStr) || TextUtils.isEmpty(priceStr) ||
            TextUtils.isEmpty(vehicle) || TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        int totalSeats = Integer.parseInt(seatsStr);
        double price = Double.parseDouble(priceStr);
        int currentBookedSeats = rideData.totalSeats - rideData.seatsLeft;

        if (totalSeats < currentBookedSeats) {
            Toast.makeText(getContext(), "Total seats cannot be less than already booked seats (" + currentBookedSeats + ")", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Update ride
        rideData.departure = departure;
        rideData.destination = destination;
        rideData.date = date;
        rideData.time = time;
        rideData.totalSeats = totalSeats;
        rideData.seatsLeft = totalSeats - currentBookedSeats;
        rideData.price = price;
        rideData.vehicle = vehicle;
        rideData.notes = notes;
        rideData.ownerPhone = phone;

        firebaseHelper.updateRide(rideData.id, rideData, (error, ref) -> {
            if (error == null) {
                Toast.makeText(getContext(), "Ride updated successfully!", Toast.LENGTH_SHORT).show();
                loadMyRide();
            } else {
                Toast.makeText(getContext(), "Failed to update ride: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    private void showDeleteRideDialog() {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Ride")
            .setMessage("Are you sure you want to delete this ride? All bookings will be cancelled.")
            .setPositiveButton("Delete", (dialog, which) -> {
                firebaseHelper.deleteRide(rideData.id, (error, ref) -> {
                    if (error == null) {
                        Toast.makeText(getContext(), "Ride deleted successfully", Toast.LENGTH_SHORT).show();
                        rideData = null;
                        updateUI();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete ride: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}

