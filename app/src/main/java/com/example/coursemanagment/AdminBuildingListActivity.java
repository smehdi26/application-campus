package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class AdminBuildingListActivity extends AppCompatActivity implements BuildingAdminAdapter.Listener {

    private RecyclerView recycler;
    private View emptyView;
    private FloatingActionButton fabAdd;

    private final ArrayList<Building> buildings = new ArrayList<>();
    private BuildingAdminAdapter adapter;

    private DatabaseReference refBuildings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_building_list);

        recycler = findViewById(R.id.recyclerBuildings);
        emptyView = findViewById(R.id.emptyViewBuildings);
        fabAdd = findViewById(R.id.fabAddBuilding);

        adapter = new BuildingAdminAdapter(buildings, this, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        refBuildings = FirebaseDatabase.getInstance().getReference("Buildings");

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AdminAddEditBuildingActivity.class)));

        listenBuildings();
    }

    private void listenBuildings() {
        refBuildings.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                buildings.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Building b = ds.getValue(Building.class);
                    if (b == null) continue;

                    if (b.id == null || b.id.trim().isEmpty()) b.id = ds.getKey();
                    if (b.facilities == null) b.facilities = new ArrayList<>();
                    if (b.images == null) b.images = new ArrayList<>();

                    buildings.add(b);
                }
                adapter.notifyDataSetChanged();
                refreshEmpty();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminBuildingListActivity.this, "Failed to load buildings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshEmpty() {
        boolean empty = buildings.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override public void onEdit(Building b) {
        Intent i = new Intent(this, AdminAddEditBuildingActivity.class);
        i.putExtra("buildingId", b.id);
        startActivity(i);
    }

    @Override public void onDelete(Building b) {
        new AlertDialog.Builder(this)
                .setTitle("Delete building?")
                .setMessage("Delete \"" + (b.name == null ? "" : b.name) + "\" permanently?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (b.id == null) return;
                    refBuildings.child(b.id).removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onOpen(Building b) {
        onEdit(b);
    }
}
