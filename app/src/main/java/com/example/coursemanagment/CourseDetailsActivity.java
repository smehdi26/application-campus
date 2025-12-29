package com.example.coursemanagment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CourseDetailsActivity extends AppCompatActivity {

    TextView tvName, tvCode, tvDesc, tvProf, tvTime, tvLoc;
    LinearLayout btnBack;
    Button btnEdit, btnDelete;
    ImageView btnAddLink;

    RecyclerView recyclerMaterials;
    MaterialAdapter adapter;
    ArrayList<Material> materialList;

    DatabaseReference mDatabase, mUsersRef;
    FirebaseAuth mAuth;
    Course course;
    String currentUserRole = "Student"; // Default to safest role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_details);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Link Views
        tvName = findViewById(R.id.tvDetailName);
        tvCode = findViewById(R.id.tvDetailCode);
        tvDesc = findViewById(R.id.tvDetailDesc);
        tvProf = findViewById(R.id.tvDetailProf);
        tvTime = findViewById(R.id.tvDetailTime);
        tvLoc = findViewById(R.id.tvDetailLoc);

        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEditCourse);
        btnDelete = findViewById(R.id.btnDeleteCourse);
        btnAddLink = findViewById(R.id.btnAddLink);
        recyclerMaterials = findViewById(R.id.recyclerMaterials);

        // --- 1. SETUP RECYCLERVIEW ONCE ---
        recyclerMaterials.setLayoutManager(new LinearLayoutManager(this));
        materialList = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());

        // Get Data from Intent
        course = (Course) getIntent().getSerializableExtra("course_data");

        if (course != null) {
            // --- CRITICAL SAFETY CHECK ---
            if (course.courseId == null || course.courseId.isEmpty()) {
                Toast.makeText(this, "Error: Course ID is missing! Check CoursesActivity.", Toast.LENGTH_LONG).show();
                // We cannot proceed without an ID, so we stop here or disable features
                return;
            }

            tvName.setText(course.courseName);
            tvCode.setText(course.courseCode);
            tvDesc.setText(course.description);
            tvProf.setText("👤 " + course.teacherName);
            tvTime.setText("🕒 " + course.timing);
            tvLoc.setText("📍 " + course.location);

            // Initialize Adapter with "Student" role initially.
            adapter = new MaterialAdapter(this, materialList, currentUserRole, course.courseId);
            recyclerMaterials.setAdapter(adapter);

            // --- 2. LOAD DATA IMMEDIATELY ---
            loadMaterials();

            // --- 3. CHECK ROLE & UPDATE UI ---
            checkRoleAndSetupUI();

            // --- 4. LISTENERS ---
            btnAddLink.setOnClickListener(v -> showAddLinkDialog());

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(CourseDetailsActivity.this)
                        .setTitle("Delete Course")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            mDatabase.child(course.courseId).removeValue();
                            finish();
                        })
                        .setNegativeButton("No", null).show();
            });

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(CourseDetailsActivity.this, EditCourseActivity.class);
                intent.putExtra("course_data", course);
                startActivity(intent);
            });
        }
    }

    private void checkRoleAndSetupUI() {
        String uid = mAuth.getCurrentUser().getUid();
        mUsersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUserRole = snapshot.child("role").getValue(String.class);

                    // IMPORTANT: Update the EXISTING adapter.
                    if (adapter != null) {
                        adapter.setUserRole(currentUserRole);
                    }

                    updateButtonsVisibility();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateButtonsVisibility() {
        // 1. Course Edit/Delete -> ADMIN ONLY
        if ("Admin".equalsIgnoreCase(currentUserRole)) {
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        // 2. Add Material -> ADMIN OR TEACHER
        if ("Student".equalsIgnoreCase(currentUserRole)) {
            btnAddLink.setVisibility(View.GONE);
        } else {
            btnAddLink.setVisibility(View.VISIBLE);
        }
    }

    private void showAddLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Material Link");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        final EditText etName = new EditText(this); etName.setHint("Name"); layout.addView(etName);
        final EditText etUrl = new EditText(this); etUrl.setHint("URL"); layout.addView(etUrl);
        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            saveMaterial(etName.getText().toString(), etUrl.getText().toString());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveMaterial(String name, String url) {
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(url)) {
            Material material = new Material(name, url);
            mDatabase.child(course.courseId).child("materials").push().setValue(material);
            Toast.makeText(this, "Added!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMaterials() {
        mDatabase.child(course.courseId).child("materials").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                materialList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Material material = postSnapshot.getValue(Material.class);
                    if (material != null) {
                        material.key = postSnapshot.getKey();
                        materialList.add(material);
                    }
                }
                // Notify adapter data changed
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}