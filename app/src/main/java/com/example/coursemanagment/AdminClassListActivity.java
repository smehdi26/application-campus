package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AdminClassListActivity extends AppCompatActivity {

    RecyclerView recyclerClasses, recyclerFilter;
    FloatingActionButton fab;
    ImageView btnBack;

    ClassAdminAdapter classAdapter;
    DepartmentFilterAdapter filterAdapter;

    ArrayList<Classroom> allClassList = new ArrayList<>(); // Stores ALL classes
    ArrayList<Classroom> displayedClassList = new ArrayList<>(); // Stores FILTERED classes
    ArrayList<Department> deptList = new ArrayList<>();

    DatabaseReference mClassesRef, mDeptRef;
    String currentFilterId = ""; // "" means Show All

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_class_list);

        mClassesRef = FirebaseDatabase.getInstance().getReference("Classes");
        mDeptRef = FirebaseDatabase.getInstance().getReference("Departments");

        recyclerClasses = findViewById(R.id.recyclerClasses);
        recyclerFilter = findViewById(R.id.recyclerDeptFilter);
        fab = findViewById(R.id.fabAddClass);
        btnBack = findViewById(R.id.btnBack);

        // 1. Setup Class List
        recyclerClasses.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new ClassAdminAdapter(this, displayedClassList);
        recyclerClasses.setAdapter(classAdapter);

        // 2. Setup Filter List (Horizontal)
        recyclerFilter.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Pass the activity context and the listener callback
        filterAdapter = new DepartmentFilterAdapter(this, deptList, new DepartmentFilterAdapter.OnDeptInteractionListener() {
            @Override
            public void onFilterChange(String deptId) {
                currentFilterId = deptId;
                applyFilter();
            }
        });
        recyclerFilter.setAdapter(filterAdapter);

        btnBack.setOnClickListener(v -> finish());
        fab.setOnClickListener(v -> startActivity(new Intent(this, AdminAddClassActivity.class)));

        loadDepartments();
        loadClasses();
    }

    private void loadDepartments() {
        mDeptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                deptList.clear();
                // We DO NOT add a fake "All" button anymore.
                // Users deselect a tag to see "All".

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Department d = ds.getValue(Department.class);
                    if(d != null) deptList.add(d);
                }
                filterAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadClasses() {
        mClassesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allClassList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Classroom c = ds.getValue(Classroom.class);
                    if (c != null) {
                        c.id = ds.getKey();
                        allClassList.add(c);
                    }
                }
                // Once data loads, apply whatever filter is active (or show all)
                applyFilter();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilter() {
        displayedClassList.clear();

        if (currentFilterId.isEmpty()) {
            // No filter -> Add everything
            displayedClassList.addAll(allClassList);
        } else {
            // Filter is active -> Check ID
            for (Classroom c : allClassList) {
                if (c.departmentId != null && c.departmentId.equals(currentFilterId)) {
                    displayedClassList.add(c);
                }
            }
        }
        classAdapter.notifyDataSetChanged();
    }
}