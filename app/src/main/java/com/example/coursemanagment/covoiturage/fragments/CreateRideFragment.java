package com.example.coursemanagment.covoiturage.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.fragment.app.Fragment;
import com.example.coursemanagment.R;
import com.example.coursemanagment.User;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.example.coursemanagment.covoiturage.utils.FirebaseRideHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;

public class CreateRideFragment extends Fragment {

    private EditText etDeparture, etDestination, etDate, etTime, etSeats, etPrice, etVehicle, etPhone, etNotes;
    private Button btnCreateRide;
    private LinearLayout formContainer;
    private OnRideCreatedListener listener;
    private FirebaseRideHelper firebaseHelper;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private String currentUserId;
    private User currentUser;

    public interface OnRideCreatedListener {
        void onRideCreated(Ride ride);
    }

    public void setOnRideCreatedListener(OnRideCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_ride, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etDeparture = view.findViewById(R.id.etDeparture);
        etDestination = view.findViewById(R.id.etDestination);
        etDate = view.findViewById(R.id.etDate);
        etTime = view.findViewById(R.id.etTime);
        etSeats = view.findViewById(R.id.etSeats);
        etPrice = view.findViewById(R.id.etPrice);
        etVehicle = view.findViewById(R.id.etVehicle);
        etPhone = view.findViewById(R.id.etPhone);
        etNotes = view.findViewById(R.id.etNotes);
        btnCreateRide = view.findViewById(R.id.btnCreateRide);
        formContainer = view.findViewById(R.id.formContainer);

        // Initialize Firebase
        firebaseHelper = new FirebaseRideHelper();
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // Set default seats
        etSeats.setText("4");

        // Load current user info
        loadCurrentUser();

        setupDatePicker();
        setupTimePicker();
        setupSubmitButton();
    }

    private void loadCurrentUser() {
        if (currentUserId != null) {
            usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentUser = snapshot.getValue(User.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    etDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupTimePicker() {
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    etTime.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            );
            timePickerDialog.show();
        });
    }

    private void setupSubmitButton() {
        btnCreateRide.setOnClickListener(v -> {
            // Check if user already has an active ride
            checkExistingRide(() -> {
                if (validateForm()) {
                    createRide();
                }
            });
        });
    }
    
    private void checkExistingRide(Runnable onNoExistingRide) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Please login to create a ride", Toast.LENGTH_SHORT).show();
            return;
        }
        
        firebaseHelper.getRidesByOwner(currentUserId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    // User already has an active ride
                    new AlertDialog.Builder(getContext())
                        .setTitle("Active Ride Exists")
                        .setMessage("You already have an active ride offer. Please delete or modify your existing ride before creating a new one.")
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    // No existing ride, proceed with creation
                    onNoExistingRide.run();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // On error, allow creation (don't block user)
                onNoExistingRide.run();
            }
        });
    }

    private boolean validateForm() {
        if (TextUtils.isEmpty(etDeparture.getText())) {
            etDeparture.setError("Departure location is required");
            return false;
        }
        if (TextUtils.isEmpty(etDestination.getText())) {
            etDestination.setError("Destination is required");
            return false;
        }
        if (TextUtils.isEmpty(etDate.getText())) {
            etDate.setError("Date is required");
            return false;
        }
        if (TextUtils.isEmpty(etTime.getText())) {
            etTime.setError("Time is required");
            return false;
        }
        if (TextUtils.isEmpty(etSeats.getText())) {
            etSeats.setError("Seats is required");
            return false;
        }
        int seats = Integer.parseInt(etSeats.getText().toString());
        if (seats < 1 || seats > 8) {
            etSeats.setError("Seats must be between 1 and 8");
            return false;
        }
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Price is required");
            return false;
        }
        if (TextUtils.isEmpty(etVehicle.getText())) {
            etVehicle.setError("Vehicle information is required");
            return false;
        }
        if (TextUtils.isEmpty(etPhone.getText())) {
            etPhone.setError("Phone number is required");
            return false;
        }
        return true;
    }

    private void createRide() {
        if (currentUser == null || currentUserId == null) {
            Toast.makeText(getContext(), "User information not loaded. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create ride object
        String ownerName = currentUser.firstName + " " + currentUser.lastName;
        int totalSeats = Integer.parseInt(etSeats.getText().toString());
        
        Ride ride = new Ride(
            null, // id will be set by Firebase
            currentUserId,
            ownerName,
            etPhone.getText().toString(),
            etDeparture.getText().toString(),
            etDestination.getText().toString(),
            etDate.getText().toString(),
            etTime.getText().toString(),
            totalSeats,
            Double.parseDouble(etPrice.getText().toString()),
            etVehicle.getText().toString(),
            etNotes.getText().toString()
        );

        // Save to Firebase
        firebaseHelper.createRide(ride, (error, ref) -> {
            if (error == null) {
                // Show success message
                showSuccessState();

                // Notify listener
                if (listener != null) {
                    listener.onRideCreated(ride);
                }

                // Clear form after 2 seconds
                new Handler().postDelayed(() -> {
                    clearForm();
                    hideSuccessState();
                }, 2000);
            } else {
                Toast.makeText(getContext(), "Failed to create ride: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessState() {
        if (formContainer != null) {
            formContainer.setVisibility(View.GONE);
        }
        Toast.makeText(getContext(), "Ride Created Successfully! Your ride offer has been posted and is now visible to other users.", Toast.LENGTH_LONG).show();
    }

    private void hideSuccessState() {
        if (formContainer != null) {
            formContainer.setVisibility(View.VISIBLE);
        }
    }

    private void clearForm() {
        etDeparture.setText("");
        etDestination.setText("");
        etDate.setText("");
        etTime.setText("");
        etSeats.setText("4");
        etPrice.setText("");
        etVehicle.setText("");
        etPhone.setText("");
        etNotes.setText("");
    }
}

