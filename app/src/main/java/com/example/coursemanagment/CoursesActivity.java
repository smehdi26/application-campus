package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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
    TextView tvStatCourses, tvStatAssignments, tvStatGrade, tvHeaderClassName; // Added Header Text
    LinearLayout layoutStats;

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
        setContentView(R.layout.activity_courses);

        setupNavbar();

        // Bind UI
        recyclerView = findViewById(R.id.recyclerViewCourses);
        fabAdd = findViewById(R.id.fabAddCourse);

        // Header Class Name
        tvHeaderClassName = findViewById(R.id.tvHeaderClassName);

        // Stats UI
        layoutStats = findViewById(R.id.layoutStats);
        tvStatCourses = findViewById(R.id.tvStatCourses);
        tvStatAssignments = findViewById(R.id.tvStatAssignments);
        tvStatGrade = findViewById(R.id.tvStatGrade);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        courseList = new ArrayList<>();
        classList = new ArrayList<>();

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

                            // --- NEW: DISPLAY CLASS NAME LOGIC ---
                            if ("Student".equalsIgnoreCase(user.role)) {
                                fetchAndDisplayClassName(user.classId);
                            } else {
                                // For Admin/Teacher show their role
                                tvHeaderClassName.setText(user.role);
                            }

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

    // --- NEW HELPER: Fetch Class Name from Database ---
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

    // --- ADMIN VIEW ---
    private void setupAdminView() {
        fabAdd.setVisibility(View.VISIBLE);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AdminAddClassActivity.class)));
        layoutStats.setVisibility(View.GONE);

        ClassAdminAdapter classAdapter = new ClassAdminAdapter(this, classList);
        recyclerView.setAdapter(classAdapter);

        FirebaseDatabase.getInstance().getReference("Classes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classList.clear();
                for(DataSnapshot ds : snapshot.getChildren()){
                    Classroom c = ds.getValue(Classroom.class);
                    if(c != null) {
                        c.id = ds.getKey();
                        classList.add(c);
                    }
                }
                classAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- USER VIEW ---
    private void setupUserView(User user) {
        fabAdd.setVisibility(View.GONE);
        CourseAdapter courseAdapter = new CourseAdapter(this, courseList);
        recyclerView.setAdapter(courseAdapter);
        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");

        if ("Student".equals(user.role)) {
            layoutStats.setVisibility(View.VISIBLE);
            loadCoursesByClassId(user.classId, courseAdapter);
        } else if ("Teacher".equals(user.role)) {
            layoutStats.setVisibility(View.GONE);
            loadCoursesByTeacherId(user.uid, courseAdapter);
        }
    }

    private void loadCoursesByClassId(String classId, CourseAdapter adapter) {
        if (classId == null || classId.isEmpty()) return;
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
                        c.courseId = ds.getKey();
                        courseList.add(c);
                    }
                }
                adapter.notifyDataSetChanged();

                if (layoutStats.getVisibility() == View.VISIBLE) {
                    calculateStats(courseList);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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
    }
}