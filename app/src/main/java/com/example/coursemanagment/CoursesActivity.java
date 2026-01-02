package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class CoursesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton fabAdd;

    // We need lists for both scenarios
    ArrayList<Course> courseList;
    ArrayList<Classroom> classList;

    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses); // Make sure this XML has the FAB button!

        setupNavbar();

        recyclerView = findViewById(R.id.recyclerViewCourses);
        fabAdd = findViewById(R.id.fabAddCourse); // Ensure this ID exists in XML

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAuth = FirebaseAuth.getInstance();

        checkRoleAndLoad();
    }

    private void checkRoleAndLoad() {
        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            if ("Admin".equalsIgnoreCase(user.role)) {
                                // --- ADMIN VIEW (Manage Classes) ---
                                setupAdminView();
                            } else {
                                // --- STUDENT/TEACHER VIEW (My Courses) ---
                                setupUserView(user);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupAdminView() {
        // Admin sees Classes, not Courses directly
        fabAdd.setVisibility(View.VISIBLE);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AdminAddClassActivity.class)));

        classList = new ArrayList<>();
        ClassAdminAdapter classAdapter = new ClassAdminAdapter(this, classList);
        recyclerView.setAdapter(classAdapter);

        FirebaseDatabase.getInstance().getReference("Classes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classList.clear();
                for(DataSnapshot ds : snapshot.getChildren()){
                    Classroom c = ds.getValue(Classroom.class);
                    classList.add(c);
                }
                classAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupUserView(User user) {
        fabAdd.setVisibility(View.GONE); // Users can't add classes/courses here

        courseList = new ArrayList<>();
        CourseAdapter courseAdapter = new CourseAdapter(this, courseList);
        recyclerView.setAdapter(courseAdapter);
        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");

        if ("Student".equals(user.role)) {
            loadCoursesByClassId(user.classId, courseAdapter);
        } else if ("Teacher".equals(user.role)) {
            loadCoursesByTeacherId(user.uid, courseAdapter);
        }
    }

    private void loadCoursesByClassId(String classId, CourseAdapter adapter) {
        if (classId == null) return;
        mDatabase.orderByChild("classId").equalTo(classId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Course c = ds.getValue(Course.class);
                    if(c!=null) { c.courseId = ds.getKey(); courseList.add(c); }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCoursesByTeacherId(String teacherId, CourseAdapter adapter) {
        mDatabase.orderByChild("teacherId").equalTo(teacherId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Course c = ds.getValue(Course.class);
                    if(c!=null) { c.courseId = ds.getKey(); courseList.add(c); }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupNavbar() {
        TextView navCourses = findViewById(R.id.navCourses);
        int redColor = ContextCompat.getColor(this, R.color.esprit_red);
        navCourses.setTextColor(redColor);
        for (android.graphics.drawable.Drawable d : navCourses.getCompoundDrawables()) if(d!=null) d.setTint(redColor);

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navEvents).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.eventscalendar.CalendarActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }
}