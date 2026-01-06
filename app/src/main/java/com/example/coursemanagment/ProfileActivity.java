package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class ProfileActivity extends AppCompatActivity {

    TextView tvFullName, tvEmail, tvRole;
    LinearLayout btnEditProfile, btnMyCourses, btnManageUsers; // Added btnManageUsers
    Button btnLogout;

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        TextView navCovoiturage = findViewById(R.id.navCovoiturage);

        navCovoiturage.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    com.example.coursemanagment.covoiturage.activities.CovoiturageActivity.class
            );
            startActivity(intent);
        });


        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyCourses = findViewById(R.id.btnMyCourses);
        btnManageUsers = findViewById(R.id.btnManageUsers); // Find new button
        btnLogout = findViewById(R.id.btnLogout);

        setupNavbar();
        loadUserProfileAndRole();

        // Listeners
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        btnMyCourses.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Manage Users Click
        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllUsersActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfileAndRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            mDatabase.child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            tvFullName.setText(user.firstName + " " + user.lastName);
                            tvEmail.setText(user.email);
                            tvRole.setText(user.role);

                            // --- ROLE UI TOGGLE ---
                            if ("Admin".equalsIgnoreCase(user.role)) {
                                // Admin: Show Manage Users, Hide My Courses
                                btnManageUsers.setVisibility(View.VISIBLE);
                                btnMyCourses.setVisibility(View.GONE);
                            } else {
                                // User: Show My Courses, Hide Manage Users
                                btnManageUsers.setVisibility(View.GONE);
                                btnMyCourses.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void setupNavbar() {
        TextView navProfile = findViewById(R.id.navProfile);
        int redColor = ContextCompat.getColor(this, R.color.esprit_red);
        navProfile.setTextColor(redColor);
        for (android.graphics.drawable.Drawable d : navProfile.getCompoundDrawables()) if (d!=null) d.setTint(redColor);

        findViewById(R.id.navCourses).setOnClickListener(v -> {
            startActivity(new Intent(this, CoursesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navEvents).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.eventscalendar.CalendarActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }
}