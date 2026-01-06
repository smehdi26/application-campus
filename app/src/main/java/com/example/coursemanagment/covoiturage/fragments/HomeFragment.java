package com.example.coursemanagment.covoiturage.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.covoiturage.adapters.RecentRideAdapter;
import com.example.coursemanagment.covoiturage.models.Ride;
import com.example.coursemanagment.covoiturage.utils.FirebaseRideHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HomeFragment extends Fragment {

    private TextView tvTotalRides, tvActiveUsers;
    private RecyclerView rvRecentRides;
    private LinearLayout statsCardsContainer;
    private RecentRideAdapter recentRidesAdapter;
    private FirebaseRideHelper firebaseHelper;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalRides = view.findViewById(R.id.tvTotalRides);
        tvActiveUsers = view.findViewById(R.id.tvActiveUsers);
        rvRecentRides = view.findViewById(R.id.rvRecentRides);
        statsCardsContainer = view.findViewById(R.id.statsCardsContainer);

        // Hide statistics section only
        view.findViewById(R.id.tvStatisticsTitle).setVisibility(View.GONE);
        view.findViewById(R.id.chartsContainer).setVisibility(View.GONE);

        // Initialize Firebase
        firebaseHelper = new FirebaseRideHelper();

        setupStats();
        setupRecentRides();
    }

    private void setupStats() {
        // Load total rides count from Firebase
        firebaseHelper.getAllRides(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalRides = (int) snapshot.getChildrenCount();
                tvTotalRides.setText(String.valueOf(totalRides));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvTotalRides.setText("0");
            }
        });

        // Load active users count from Firebase
        firebaseHelper.getActiveUsersCount(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeUsers = (int) snapshot.getChildrenCount();
                tvActiveUsers.setText(String.valueOf(activeUsers));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvActiveUsers.setText("0");
            }
        });
    }

    private void setupRecentRides() {
        // Load recent rides from Firebase
        firebaseHelper.getAllRides(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Ride> allRides = FirebaseRideHelper.snapshotListToRides(snapshot);
                
                // Sort by date (most recent first) - you can enhance this sorting
                Collections.sort(allRides, (r1, r2) -> {
                    // Simple date comparison (YYYY-MM-DD format)
                    if (r1.date != null && r2.date != null) {
                        return r2.date.compareTo(r1.date);
                    }
                    return 0;
                });

                // Get the 5 most recent rides
                ArrayList<Ride> recentRides = new ArrayList<>();
                int count = Math.min(5, allRides.size());
                for (int i = 0; i < count; i++) {
                    recentRides.add(allRides.get(i));
                }

                recentRidesAdapter = new RecentRideAdapter(getContext(), recentRides);
                rvRecentRides.setLayoutManager(new LinearLayoutManager(getContext()));
                rvRecentRides.setAdapter(recentRidesAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Show empty list on error
                recentRidesAdapter = new RecentRideAdapter(getContext(), new ArrayList<>());
                rvRecentRides.setLayoutManager(new LinearLayoutManager(getContext()));
                rvRecentRides.setAdapter(recentRidesAdapter);
            }
        });
    }
}
