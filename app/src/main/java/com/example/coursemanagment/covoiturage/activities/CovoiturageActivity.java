package com.example.coursemanagment.covoiturage.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.coursemanagment.CoursesActivity;
import com.example.coursemanagment.ProfileActivity;
import com.example.coursemanagment.R;
import com.example.coursemanagment.covoiturage.fragments.BrowseRidesFragment;
import com.example.coursemanagment.covoiturage.fragments.CreateRideFragment;
import com.example.coursemanagment.covoiturage.fragments.HomeFragment;
import com.example.coursemanagment.covoiturage.fragments.MyBookedRidesFragment;
import com.example.coursemanagment.covoiturage.fragments.MyRideOfferFragment;
import com.example.coursemanagment.covoiturage.models.Ride;

public class CovoiturageActivity extends AppCompatActivity {

    private Button btnHome, btnBrowse, btnCreate, btnMyRide, btnMyBookings;
    private String activeView = "home";
    private Ride createdRide = null;
    private MyRideOfferFragment myRideOfferFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_covoiturage);

        btnHome = findViewById(R.id.btnHome);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnCreate = findViewById(R.id.btnCreate);
        btnMyRide = findViewById(R.id.btnMyRide);
        btnMyBookings = findViewById(R.id.btnMyBookings);

        setupTabButtons();
        setupNavbar();
        
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

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

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
        // Reset all buttons
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

        // Set active button
        Button activeButton = null;
        switch (activeView) {
            case "home":
                activeButton = btnHome;
                break;
            case "search":
                activeButton = btnBrowse;
                break;
            case "create":
                activeButton = btnCreate;
                break;
            case "myride":
                activeButton = btnMyRide;
                break;
            case "mybookings":
                activeButton = btnMyBookings;
                break;
        }

        if (activeButton != null) {
            activeButton.setBackgroundResource(R.drawable.bg_tab_active);
            activeButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }
}
