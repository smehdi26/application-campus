package com.example.coursemanagment.covoiturage.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.coursemanagment.AdminClassListActivity;
import com.example.coursemanagment.AllUsersActivity;
import com.example.coursemanagment.CoursesActivity;
import com.example.coursemanagment.EditProfileActivity;
import com.example.coursemanagment.ForumActivity;
import com.example.coursemanagment.LoginActivity;
import com.example.coursemanagment.MapsActivity;
import com.example.coursemanagment.R;
import com.example.coursemanagment.User;
import com.example.coursemanagment.covoiturage.fragments.BrowseRidesFragment;
import com.example.coursemanagment.covoiturage.fragments.CreateRideFragment;
import com.example.coursemanagment.covoiturage.fragments.HomeFragment;
import com.example.coursemanagment.covoiturage.fragments.MyBookedRidesFragment;
import com.example.coursemanagment.covoiturage.fragments.MyRideOfferFragment;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class CovoiturageActivity extends AppCompatActivity {

    private Button btnHome, btnBrowse, btnCreate, btnMyRide, btnMyBookings;
    private String activeView = "home";
    private Ride createdRide = null;
    private MyRideOfferFragment myRideOfferFragment;

    // Drawer
    private DrawerLayout drawerLayout;

    // Drawer profile views
    private TextView tvFullName, tvEmail, tvRole;
    private View btnEditProfile, btnMyCourses, btnManageUsers, btnManageClasses, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_covoiturage);

        drawerLayout = findViewById(R.id.drawerLayout);

        // Open drawer when tapping header
        TextView header = findViewById(R.id.header);
        if (header != null) {
            header.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        btnHome = findViewById(R.id.btnHome);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnCreate = findViewById(R.id.btnCreate);
        btnMyRide = findViewById(R.id.btnMyRide);
        btnMyBookings = findViewById(R.id.btnMyBookings);

        setupTabButtons();
        setupNavbar();
        setupDrawerProfile();

        if (savedInstanceState == null) {
            switchToFragment("home");
        }
    }

    private void setupTabButtons() {
        btnHome.setOnClickListener(v -> switchToFragment("home"));
        btnBrowse.setOnClickListener(v -> switchToFragment("search"));
        btnCreate.setOnClickListener(v -> switchToFragment("create"));
        btnMyRide.setOnClickListener(v -> switchToFragment("myride"));
        btnMyBookings.setOnClickListener(v -> switchToFragment("mybookings"));
    }

    // ===================== Drawer Profile =====================
    private void setupDrawerProfile() {
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyCourses = findViewById(R.id.btnMyCourses);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageClasses = findViewById(R.id.btnManageClasses);
        btnLogout = findViewById(R.id.btnLogout);

        // if drawer_profile isn't included correctly, these can be null
        if (btnLogout == null) return;

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(CovoiturageActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                closeDrawer();
            });
        }

        if (btnMyCourses != null) {
            btnMyCourses.setOnClickListener(v -> {
                startActivity(new Intent(this, CoursesActivity.class));
                overridePendingTransition(0, 0);
                closeDrawer();
                finish();
            });
        }

        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                startActivity(new Intent(this, AllUsersActivity.class));
                closeDrawer();
            });
        }

        if (btnManageClasses != null) {
            btnManageClasses.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminClassListActivity.class));
                closeDrawer();
            });
        }

        loadUserProfileAndRoleIntoDrawer();
    }

    private void loadUserProfileAndRoleIntoDrawer() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Users");
        String uid = currentUser.getUid();

        mDatabase.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                User user = snapshot.getValue(User.class);
                if (user == null) return;

                if (tvFullName != null) tvFullName.setText(user.firstName + " " + user.lastName);
                if (tvEmail != null) tvEmail.setText(user.email);
                if (tvRole != null) tvRole.setText(user.role);

                boolean isAdmin = user.role != null && user.role.equalsIgnoreCase("Admin");

                if (btnManageUsers != null) btnManageUsers.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                if (btnManageClasses != null) btnManageClasses.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                if (btnMyCourses != null) btnMyCourses.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void closeDrawer() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
    }

    // ===================== Navbar =====================
    private void setupNavbar() {
        TextView navCovoiturage = findViewById(R.id.navCovoiturage);
        int redColor = ContextCompat.getColor(this, R.color.covoiturage_red_primary);
        navCovoiturage.setTextColor(redColor);
        for (android.graphics.drawable.Drawable d : navCovoiturage.getCompoundDrawables()) {
            if (d != null) d.setTint(redColor);
        }

        findViewById(R.id.navCourses).setOnClickListener(v -> {
            startActivity(new Intent(this, CoursesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navMap).setOnClickListener(v -> {
            startActivity(new Intent(this, MapsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        // ✅ open drawer instead of opening ProfileActivity
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        findViewById(R.id.navEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.eventscalendar.CalendarActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navForums).setOnClickListener(v -> {
            startActivity(new Intent(this, ForumActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    // ===================== Tabs / Fragments =====================
    private void switchToFragment(String view) {
        activeView = view;
        updateTabStyles();

        Fragment fragment = null;

        switch (view) {
            case "home":
                fragment = new HomeFragment();
                break;
            case "search":
                fragment = new BrowseRidesFragment();
                break;
            case "create":
                CreateRideFragment createFragment = new CreateRideFragment();
                createFragment.setOnRideCreatedListener(ride -> {
                    createdRide = ride;
                    switchToFragment("myride");
                });
                fragment = createFragment;
                break;
            case "myride":
                myRideOfferFragment = new MyRideOfferFragment();
                myRideOfferFragment.setRideData(createdRide);
                myRideOfferFragment.setOnCreateRideListener(() -> switchToFragment("create"));
                fragment = myRideOfferFragment;
                break;
            case "mybookings":
                fragment = new MyBookedRidesFragment();
                break;
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.covoiturageContainer, fragment)
                    .commit();
        }
    }

    private void updateTabStyles() {
        btnHome.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnHome.setTextColor(ContextCompat.getColor(this, R.color.covoiturage_dark_gray));
        btnBrowse.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnBrowse.setTextColor(ContextCompat.getColor(this, R.color.covoiturage_dark_gray));
        btnCreate.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnCreate.setTextColor(ContextCompat.getColor(this, R.color.covoiturage_dark_gray));
        btnMyRide.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnMyRide.setTextColor(ContextCompat.getColor(this, R.color.covoiturage_dark_gray));
        btnMyBookings.setBackgroundResource(R.drawable.bg_tab_inactive);
        btnMyBookings.setTextColor(ContextCompat.getColor(this, R.color.covoiturage_dark_gray));

        Button activeButton = null;
        switch (activeView) {
            case "home": activeButton = btnHome; break;
            case "search": activeButton = btnBrowse; break;
            case "create": activeButton = btnCreate; break;
            case "myride": activeButton = btnMyRide; break;
            case "mybookings": activeButton = btnMyBookings; break;
        }

        if (activeButton != null) {
            activeButton.setBackgroundResource(R.drawable.bg_tab_active);
            activeButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
