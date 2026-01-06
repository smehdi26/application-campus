package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import FAB
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AllUsersActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    UserAdapter adapter;
    ArrayList<User> list;
    DatabaseReference mDatabase;
    ImageView btnBack;
    FloatingActionButton fabAddUser; // Declare FAB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        recyclerView = findViewById(R.id.recyclerAllUsers);
        btnBack = findViewById(R.id.btnBack);
        fabAddUser = findViewById(R.id.fabAddUser); // Find FAB

        btnBack.setOnClickListener(v -> finish());

        fabAddUser.setOnClickListener(v -> {
            // Launch RegisterActivity for admin to add new users
            Intent intent = new Intent(AllUsersActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        mDatabase = FirebaseDatabase.getInstance().getReference("Users");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new UserAdapter(this, list);
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        list.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}