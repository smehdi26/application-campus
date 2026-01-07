package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursemanagment.covoiturage.activities.CovoiturageActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class CoursesActivity extends AppCompatActivity {

    // UI Components
    RecyclerView recyclerView, recyclerFilter;
    FloatingActionButton fabAdd;
    TextView tvStatCourses, tvStatAssignments, tvStatGrade, tvHeaderClassName;
    LinearLayout layoutStats;
    ImageView btnNotifications;

    // Data Lists
    ArrayList<Course> courseList;
    ArrayList<Classroom> classList;

    // Admin Filter Data
    ArrayList<Classroom> allClassList;
    ArrayList<Classroom> displayedClassList;
    ArrayList<Department> deptList;
    DepartmentFilterAdapter filterAdapter;
    String currentFilterId = "";

    // Firebase
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        setupNavbar();

        // Bind Views
        recyclerView = findViewById(R.id.recyclerViewCourses);
        recyclerFilter = findViewById(R.id.recyclerDeptFilter);
        fabAdd = findViewById(R.id.fabAddCourse);

        tvHeaderClassName = findViewById(R.id.tvHeaderClassName);
        layoutStats = findViewById(R.id.layoutStats);
        tvStatCourses = findViewById(R.id.tvStatCourses);
        tvStatAssignments = findViewById(R.id.tvStatAssignments);
        tvStatGrade = findViewById(R.id.tvStatGrade);
        btnNotifications = findViewById(R.id.btnNotifications);

        // Setup Lists
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup Filter (Horizontal)
        recyclerFilter.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Initialize Arrays
        courseList = new ArrayList<>();
        classList = new ArrayList<>();
        allClassList = new ArrayList<>();
        displayedClassList = new ArrayList<>();
        deptList = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();

        // Notification Click
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        // Start Loading
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

                            // --- HEADER DISPLAY LOGIC ---
                            if ("Student".equalsIgnoreCase(user.role)) {
                                fetchAndDisplayClassName(user.classId);
                            } else {
                                tvHeaderClassName.setText(user.role);
                            }

                            // --- VIEW SELECTION ---
                            if ("Admin".equalsIgnoreCase(user.role)) {
                                setupAdminView();
                            } else {
                                setupUserView(user);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- HELPER: Get Class Name for Student Header ---
    private void fetchAndDisplayClassName(String classId) {
        if (classId == null || classId.isEmpty()) {
            tvHeaderClassName.setText("Unassigned");
            return;
        }

        FirebaseDatabase.getInstance().getReference("Classes").child(classId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String className = snapshot.child("name").getValue(String.class);
                            tvHeaderClassName.setText(className != null ? className : "Unknown Class");
                        } else {
                            tvHeaderClassName.setText("Unassigned");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- ADMIN VIEW (Manage Classes) ---
    private void setupAdminView() {
        fabAdd.setVisibility(View.VISIBLE);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AdminAddClassActivity.class)));

        // Hide Student Stats
        layoutStats.setVisibility(View.GONE);

        // Show Filters
        recyclerFilter.setVisibility(View.VISIBLE);

        // 1. Setup Filter Adapter
        filterAdapter = new DepartmentFilterAdapter(this, deptList, deptId -> {
            currentFilterId = deptId;
            applyAdminFilter();
        });
        recyclerFilter.setAdapter(filterAdapter);
        loadDepartments();

        // 2. Setup Class List Adapter
        ClassAdminAdapter classAdapter = new ClassAdminAdapter(this, displayedClassList);
        recyclerView.setAdapter(classAdapter);

        // 3. Load Classes
        FirebaseDatabase.getInstance().getReference("Classes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allClassList.clear();
                for(DataSnapshot ds : snapshot.getChildren()){
                    Classroom c = ds.getValue(Classroom.class);
                    if(c != null) {
                        c.id = ds.getKey();
                        allClassList.add(c);
                    }
                }
                applyAdminFilter(); // Filter and update adapter
                classAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDepartments() {
        FirebaseDatabase.getInstance().getReference("Departments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                deptList.clear();
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

    private void applyAdminFilter() {
        displayedClassList.clear();
        if (currentFilterId.isEmpty()) {
            displayedClassList.addAll(allClassList);
        } else {
            for (Classroom c : allClassList) {
                if (c.departmentId != null && c.departmentId.equals(currentFilterId)) {
                    displayedClassList.add(c);
                }
            }
        }
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    // --- USER VIEW (Student/Teacher - View Courses) ---
    private void setupUserView(User user) {
        fabAdd.setVisibility(View.GONE);
        recyclerFilter.setVisibility(View.GONE);

        CourseAdapter courseAdapter = new CourseAdapter(this, courseList);
        recyclerView.setAdapter(courseAdapter);
        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");

        if ("Student".equals(user.role)) {
            // Student: Show Stats
            layoutStats.setVisibility(View.VISIBLE);

            if (user.classId != null && !user.classId.isEmpty()) {
                loadCoursesByClassId(user.classId, courseAdapter);
            } else {
                Toast.makeText(this, "You are not assigned to a class.", Toast.LENGTH_LONG).show();
            }
        } else if ("Teacher".equals(user.role)) {
            // Teacher: Hide Stats
            layoutStats.setVisibility(View.GONE);
            loadCoursesByTeacherId(user.uid, courseAdapter);
        }
    }

    private void loadCoursesByClassId(String classId, CourseAdapter adapter) {
        runQuery(mDatabase.orderByChild("classId").equalTo(classId), adapter);
    }

    private void loadCoursesByTeacherId(String teacherId, CourseAdapter adapter) {
        runQuery(mDatabase.orderByChild("teacherId").equalTo(teacherId), adapter);
    }

    private void runQuery(Query query, CourseAdapter adapter) {
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Course c = ds.getValue(Course.class);
                    if(c != null) {
                        // --- CRITICAL FIX: CAPTURE ID ---
                        c.courseId = ds.getKey();
                        courseList.add(c);
                    }
                }
                adapter.notifyDataSetChanged();

                // Update stats if visible (Student only)
                if (layoutStats.getVisibility() == View.VISIBLE) {
                    calculateStats(courseList);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- STATS LOGIC ---
    private void calculateStats(ArrayList<Course> courses) {
        if (courses.isEmpty()) {
            tvStatCourses.setText("0");
            tvStatAssignments.setText("0");
            tvStatGrade.setText("-%");
            return;
        }

        tvStatCourses.setText(String.valueOf(courses.size()));

        String myUid = mAuth.getCurrentUser().getUid();

        final int[] pendingCount = {0};
        final double[] totalScore = {0.0};
        final int[] gradedCount = {0};
        final int[] coursesProcessed = {0};

        for (Course c : courses) {
            mDatabase.child(c.courseId).child("assignments").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot assignSnap : snapshot.getChildren()) {
                        if (!assignSnap.child("submissions").child(myUid).exists()) {
                            pendingCount[0]++;
                        } else {
                            String grade = assignSnap.child("submissions").child(myUid).child("grade").getValue(String.class);
                            if (grade != null && !grade.equals("Pending")) {
                                try {
                                    String scoreStr = grade.split("/")[0];
                                    totalScore[0] += Double.parseDouble(scoreStr);
                                    gradedCount[0]++;
                                } catch (Exception e) {}
                            }
                        }
                    }
                    coursesProcessed[0]++;
                    if (coursesProcessed[0] == courses.size()) {
                        tvStatAssignments.setText(String.valueOf(pendingCount[0]));
                        if (gradedCount[0] > 0) {
                            int avg = (int) (totalScore[0] / gradedCount[0]);
                            tvStatGrade.setText(avg + "%");
                        } else {
                            tvStatGrade.setText("-%");
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    // --- NAVBAR ---
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
        findViewById(R.id.navCovoiturage).setOnClickListener(v -> {
            Intent intent = new Intent(this, CovoiturageActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navForums).setOnClickListener(v -> {
            Intent intent = new Intent(this, ForumActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }
}