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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursemanagment.covoiturage.activities.CovoiturageActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class CoursesActivity extends AppCompatActivity {

    // Drawer
    private DrawerLayout drawerLayout;

    // Drawer profile views (from drawer_profile.xml)
    private TextView tvFullName, tvEmail, tvRole;
    private View btnEditProfile, btnMyCourses, btnManageUsers, btnManageClasses, btnLogout;

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

        drawerLayout = findViewById(R.id.drawerLayout);

        // Open drawer when clicking header (red square / title)
        View header = findViewById(R.id.header);
        View btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        View headerTitleArea = findViewById(R.id.headerTitleArea);

        View.OnClickListener openDrawer = v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        };

        if (header != null) header.setOnClickListener(openDrawer);
        if (btnOpenDrawer != null) btnOpenDrawer.setOnClickListener(openDrawer);
        if (headerTitleArea != null) headerTitleArea.setOnClickListener(openDrawer);

        setupNavbar();
        setupDrawerProfile();

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

    // ===================== Drawer Profile =====================
    private void setupDrawerProfile() {
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyCourses = findViewById(R.id.btnMyCourses);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageClasses = findViewById(R.id.btnManageClasses);
        btnLogout = findViewById(R.id.btnLogout);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CoursesActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                closeDrawer();
            });
        }

        // ✅ We're already in CoursesActivity, so just close drawer
        if (btnMyCourses != null) {
            btnMyCourses.setOnClickListener(v -> closeDrawer());
        }

        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                startActivity(new Intent(this, AllUsersActivity.class));
                closeDrawer();
            });
        }

        if (btnManageClasses != null) {
            btnManageClasses.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminClassListActivity.class));
                closeDrawer();
            });
        }

        loadUserProfileAndRoleIntoDrawer();
    }

    private void loadUserProfileAndRoleIntoDrawer() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        User user = snapshot.getValue(User.class);
                        if (user == null) return;

                        if (tvFullName != null) tvFullName.setText(user.firstName + " " + user.lastName);
                        if (tvEmail != null) tvEmail.setText(user.email);
                        if (tvRole != null) tvRole.setText(user.role);

                        boolean isAdmin = user.role != null && user.role.equalsIgnoreCase("Admin");
                        if (btnManageUsers != null) btnManageUsers.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                        if (btnManageClasses != null) btnManageClasses.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

                        // in your drawer design you used "My Courses" for non-admins
                        if (btnMyCourses != null) btnMyCourses.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void closeDrawer() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ===================== Existing logic =====================
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

    private void setupAdminView() {
        fabAdd.setVisibility(View.VISIBLE);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AdminAddClassActivity.class)));

        layoutStats.setVisibility(View.GONE);
        recyclerFilter.setVisibility(View.VISIBLE);

        filterAdapter = new DepartmentFilterAdapter(this, deptList, deptId -> {
            currentFilterId = deptId;
            applyAdminFilter();
        });
        recyclerFilter.setAdapter(filterAdapter);
        loadDepartments();

        ClassAdminAdapter classAdapter = new ClassAdminAdapter(this, displayedClassList);
        recyclerView.setAdapter(classAdapter);

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
                applyAdminFilter();
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

    private void setupUserView(User user) {
        fabAdd.setVisibility(View.GONE);
        recyclerFilter.setVisibility(View.GONE);

        CourseAdapter courseAdapter = new CourseAdapter(this, courseList);
        recyclerView.setAdapter(courseAdapter);
        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");

        if ("Student".equals(user.role)) {
            layoutStats.setVisibility(View.VISIBLE);

            if (user.classId != null && !user.classId.isEmpty()) {
                loadCoursesByClassId(user.classId, courseAdapter);
            } else {
                Toast.makeText(this, "You are not assigned to a class.", Toast.LENGTH_LONG).show();
            }
        } else if ("Teacher".equals(user.role)) {
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

    // ===================== NAVBAR (UPDATED: Profile opens drawer) =====================
    private void setupNavbar() {
        TextView navCourses = findViewById(R.id.navCourses);
        int redColor = ContextCompat.getColor(this, R.color.esprit_red);
        navCourses.setTextColor(redColor);
        for (android.graphics.drawable.Drawable d : navCourses.getCompoundDrawables()) if(d!=null) d.setTint(redColor);

        // ✅ Profile => open drawer
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        findViewById(R.id.navMap).setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
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
