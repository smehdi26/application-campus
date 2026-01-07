package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView; // Import ImageView
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AdminClassListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton fab;
    ClassAdminAdapter adapter;
    ArrayList<Classroom> list;
    DatabaseReference mDatabase;
    ImageView btnBack; // Define variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_class_list);

        recyclerView = findViewById(R.id.recyclerClasses);
        fab = findViewById(R.id.fabAddClass);
        btnBack = findViewById(R.id.btnBack); // Link ID

        mDatabase = FirebaseDatabase.getInstance().getReference("Classes");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new ClassAdminAdapter(this, list);
        recyclerView.setAdapter(adapter);

        // --- BACK BUTTON LOGIC ---
        btnBack.setOnClickListener(v -> finish());

        fab.setOnClickListener(v -> startActivity(new Intent(this, AdminAddClassActivity.class)));

        // Load Classes
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Classroom c = ds.getValue(Classroom.class);
                    if(c != null) {
                        list.add(c);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}