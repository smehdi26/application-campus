package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Import CardView
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminDashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    UserAdapter adapter;
    ArrayList<User> list;
    DatabaseReference mDatabase;
    Button btnLogout;
    CardView btnManageClasses, btnManageForum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerUsers);
        btnLogout = findViewById(R.id.btnAdminLogout);
        btnManageClasses = findViewById(R.id.btnManageClasses);
        btnManageForum = findViewById(R.id.btnManageForum);

        mDatabase = FirebaseDatabase.getInstance().getReference("Users");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new UserAdapter(this, list);
        recyclerView.setAdapter(adapter);

        if (btnManageClasses != null) {
            btnManageClasses.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, AdminClassListActivity.class);
                startActivity(intent);
            });
        }

        if (btnManageForum != null) {
            btnManageForum.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, AdminForumActivity.class);
                startActivity(intent);
            });
        }

        // Fetch Users Logic
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    // Add user to list
                    list.add(user);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Logout Logic
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}