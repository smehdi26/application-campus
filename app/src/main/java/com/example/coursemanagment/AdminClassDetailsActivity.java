package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AdminClassDetailsActivity extends AppCompatActivity {

    TextView tvClassName;
    RecyclerView recyclerView;
    Button btnAddCourse, btnManageClassInfo; // New Button
    LinearLayout btnBack;

    DatabaseReference mDatabase;
    ArrayList<Course> list;
    CourseAdapter adapter;
    Classroom currentClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_class_details);

        currentClass = (Classroom) getIntent().getSerializableExtra("class_data");

        tvClassName = findViewById(R.id.tvHeaderClassName);
        recyclerView = findViewById(R.id.recyclerAdminCourses);
        btnAddCourse = findViewById(R.id.btnAdminAddCourse);
        btnManageClassInfo = findViewById(R.id.btnManageClassInfo); // Link ID
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        if(currentClass != null) {
            tvClassName.setText(currentClass.name + " (" + currentClass.code + ")");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new CourseAdapter(this, list);
        recyclerView.setAdapter(adapter);

        // --- 1. EDIT CLASS & STUDENTS LOGIC ---
        btnManageClassInfo.setOnClickListener(v -> {
            Intent intent = new Intent(AdminClassDetailsActivity.this, AdminAddClassActivity.class);
            intent.putExtra("class_data", currentClass); // Pass class data to edit
            startActivity(intent);
            finish(); // Close details so when they come back, info (like Name) is refreshed
        });

        // --- 2. ADD COURSE LOGIC ---
        btnAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(AdminClassDetailsActivity.this, AdminAddCourseActivity.class);
            intent.putExtra("class_id", currentClass.id);
            startActivity(intent);
        });

        loadCourses();
    }

    private void loadCourses() {
        Query query = mDatabase.orderByChild("classId").equalTo(currentClass.id);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Course c = ds.getValue(Course.class);
                    if(c != null) {
                        c.courseId = ds.getKey();
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