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
    Button btnAddCourse;
    LinearLayout btnBack; // Define variable

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
        btnBack = findViewById(R.id.btnBack); // Link ID

        // --- BACK BUTTON LOGIC ---
        btnBack.setOnClickListener(v -> finish());

        if(currentClass != null) {
            tvClassName.setText(currentClass.name + " (" + currentClass.code + ")");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new CourseAdapter(this, list);
        recyclerView.setAdapter(adapter);

        // Click Add Course
        btnAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(AdminClassDetailsActivity.this, AdminAddCourseActivity.class);
            intent.putExtra("class_id", currentClass.id); // Pass Class ID
            startActivity(intent);
        });

        loadCourses();
    }

    private void loadCourses() {
        // Query courses that belong to THIS classId
        Query query = mDatabase.orderByChild("classId").equalTo(currentClass.id);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Course c = ds.getValue(Course.class);
                    if(c != null) {
                        c.courseId = ds.getKey(); // Critical Fix for ID
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