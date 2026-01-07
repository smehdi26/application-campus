package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CourseDetailsActivity extends AppCompatActivity {

    TextView tvName, tvCode, tvDesc, tvProf, tvTime, tvLoc;
    LinearLayout btnBack;
    Button btnEdit, btnDelete, btnViewStudents;
    ImageView btnAddLink, btnAddAssignment;

    RecyclerView recyclerMaterials, recyclerAssignments;
    MaterialAdapter materialAdapter;
    AssignmentAdapter assignmentAdapter;

    ArrayList<Material> materialList;
    ArrayList<Assignment> assignmentList;

    DatabaseReference mDatabase, mUsersRef;
    FirebaseAuth mAuth;
    Course course;

    String currentUserRole = "Student";
    String currentUserName = "Student";

    Calendar calendar = Calendar.getInstance();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

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
        btnViewStudents = findViewById(R.id.btnViewStudents);
        btnAddLink = findViewById(R.id.btnAddLink);
        btnAddAssignment = findViewById(R.id.btnAddAssignment);

        recyclerMaterials = findViewById(R.id.recyclerMaterials);
        recyclerAssignments = findViewById(R.id.recyclerAssignments);

        // --- SETUP RECYCLERVIEWS ---
        recyclerMaterials.setLayoutManager(new LinearLayoutManager(this));
        materialList = new ArrayList<>();
        materialAdapter = new MaterialAdapter(this, materialList, currentUserRole, "");
        recyclerMaterials.setAdapter(materialAdapter);

        recyclerAssignments.setLayoutManager(new LinearLayoutManager(this));
        assignmentList = new ArrayList<>();

        assignmentAdapter = new AssignmentAdapter(this, assignmentList, currentUserRole, "", new AssignmentAdapter.AssignmentActionListener() {
            @Override
            public void onSubmitClick(Assignment assignment) {
                showSubmitHomeworkDialog(assignment);
            }

            @Override
            public void onEditClick(Assignment assignment) {
                showEditAssignmentDialog(assignment);
            }
        });
        recyclerAssignments.setAdapter(assignmentAdapter);

        btnBack.setOnClickListener(v -> finish());

        // Get initial data from Intent
        course = (Course) getIntent().getSerializableExtra("course_data");

        if (course != null) {
            if (course.courseId == null) return;

            // Initial UI Set
            updateCourseUI(course);

            // Update Adapters with ID
            materialAdapter = new MaterialAdapter(this, materialList, currentUserRole, course.courseId);
            recyclerMaterials.setAdapter(materialAdapter);

            assignmentAdapter = new AssignmentAdapter(this, assignmentList, currentUserRole, course.courseId, new AssignmentAdapter.AssignmentActionListener() {
                @Override public void onSubmitClick(Assignment a) { showSubmitHomeworkDialog(a); }
                @Override public void onEditClick(Assignment a) { showEditAssignmentDialog(a); }
            });
            recyclerAssignments.setAdapter(assignmentAdapter);

            // --- LOAD DATA ---
            loadMaterials();
            checkRoleAndSetupUI();

            // --- NEW: LISTEN FOR LIVE UPDATES ---
            loadLiveCourseData();

            // Listeners
            btnAddLink.setOnClickListener(v -> showAddLinkDialog(true));
            btnAddAssignment.setOnClickListener(v -> showCreateAssignmentDialog());

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Delete").setMessage("Sure?")
                        .setPositiveButton("Yes", (d,w)-> {
                            mDatabase.child(course.courseId).removeValue(); finish();
                        }).setNegativeButton("No", null).show();
            });

            btnEdit.setOnClickListener(v -> {
                Intent i = new Intent(this, EditCourseActivity.class);
                i.putExtra("course_data", course); startActivity(i);
            });

            btnViewStudents.setOnClickListener(v -> {
                Intent intent = new Intent(CourseDetailsActivity.this, TeacherStudentListActivity.class);
                intent.putExtra("class_id", course.classId);
                startActivity(intent);
            });
        }
    }

    // --- NEW: LIVE DATA LISTENER ---
    private void loadLiveCourseData() {
        mDatabase.child(course.courseId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Course updatedCourse = snapshot.getValue(Course.class);
                if (updatedCourse != null) {
                    // Keep the ID (Firebase doesn't return key in getValue)
                    updatedCourse.courseId = snapshot.getKey();

                    // Update local object so if we click Edit again, we have fresh data
                    course = updatedCourse;

                    // Refresh UI Text
                    updateCourseUI(course);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCourseUI(Course c) {
        tvName.setText(c.courseName);
        tvCode.setText(c.courseCode);
        tvDesc.setText(c.description);
        tvProf.setText("👤 " + c.teacherName);
        tvTime.setText("🕒 " + c.timing); // Handles new lines automatically
        tvLoc.setText("📍 " + c.location);
    }

    // ... (REST OF THE CODE REMAINS THE SAME) ...
    // Copy the existing methods below: checkRoleAndSetupUI, updateButtonsVisibility, Dialogs, etc.

    private void checkRoleAndSetupUI() {
        String uid = mAuth.getCurrentUser().getUid();
        mUsersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUserRole = snapshot.child("role").getValue(String.class);
                    String f = snapshot.child("firstName").getValue(String.class);
                    String l = snapshot.child("lastName").getValue(String.class);
                    currentUserName = f + " " + l;

                    if (materialAdapter != null) materialAdapter.setUserRole(currentUserRole);
                    if (assignmentAdapter != null) assignmentAdapter.setUserRole(currentUserRole);

                    updateButtonsVisibility();
                    loadAssignments();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateButtonsVisibility() {
        if ("Admin".equalsIgnoreCase(currentUserRole)) {
            btnEdit.setVisibility(View.VISIBLE); btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE); btnDelete.setVisibility(View.GONE);
        }
        if ("Student".equalsIgnoreCase(currentUserRole)) {
            btnAddLink.setVisibility(View.GONE); btnViewStudents.setVisibility(View.GONE);
            btnAddAssignment.setVisibility(View.GONE);
        } else {
            btnAddLink.setVisibility(View.VISIBLE); btnViewStudents.setVisibility(View.VISIBLE);
            btnAddAssignment.setVisibility(View.VISIBLE);
        }
    }

    private void showCreateAssignmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Assignment");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50,40,50,10);
        final EditText etTitle = new EditText(this); etTitle.setHint("Title"); layout.addView(etTitle);
        final TextView tvDate = new TextView(this); tvDate.setText("Tap to Select Deadline"); tvDate.setPadding(0,20,0,20); tvDate.setTextSize(16);
        layout.addView(tvDate);

        final long[] selectedTimestamp = {0};
        tvDate.setOnClickListener(v -> showDateTimePicker(tvDate, selectedTimestamp));

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = etTitle.getText().toString();
            if(!TextUtils.isEmpty(title) && selectedTimestamp[0] != 0) {
                createAssignmentTask(title, tvDate.getText().toString(), selectedTimestamp[0]);
            } else {
                Toast.makeText(this, "Title and Date required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null); builder.show();
    }

    private void createAssignmentTask(String title, String dueDate, long timestamp) {
        String id = mDatabase.push().getKey();
        Assignment a = new Assignment(id, title, dueDate, timestamp);
        mDatabase.child(course.courseId).child("assignments").child(id).setValue(a);
        NotificationHelper.sendToClass(course.classId, "New Assignment", "New homework: '" + title + "' added in " + course.courseName);
        Toast.makeText(this, "Created!", Toast.LENGTH_SHORT).show();
    }

    private void showEditAssignmentDialog(Assignment assignment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Assignment");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50,40,50,10);
        final EditText etTitle = new EditText(this); etTitle.setText(assignment.title); layout.addView(etTitle);
        final TextView tvDate = new TextView(this); tvDate.setText(assignment.dueDate); tvDate.setPadding(0,20,0,20); tvDate.setTextSize(16);
        layout.addView(tvDate);

        final long[] selectedTimestamp = {assignment.timestamp};
        tvDate.setOnClickListener(v -> showDateTimePicker(tvDate, selectedTimestamp));

        builder.setView(layout);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String title = etTitle.getText().toString();
            if(!TextUtils.isEmpty(title)) {
                Assignment updated = new Assignment(assignment.id, title, tvDate.getText().toString(), selectedTimestamp[0]);
                mDatabase.child(course.courseId).child("assignments").child(assignment.id).setValue(updated);
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null); builder.show();
    }

    private void showDateTimePicker(TextView displayView, long[] timestampStorage) {
        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            new android.app.TimePickerDialog(this, (tView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                timestampStorage[0] = calendar.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale.getDefault());
                displayView.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showSubmitHomeworkDialog(Assignment assignment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit: " + assignment.title);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50,40,50,10);
        final EditText etLink = new EditText(this); etLink.setHint("Paste Work Link"); layout.addView(etLink);
        builder.setView(layout);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String link = etLink.getText().toString();
            if(!TextUtils.isEmpty(link)) {
                submitAssignmentToFirebase(assignment.id, link);
            }
        });
        builder.setNegativeButton("Cancel", null); builder.show();
    }

    private void submitAssignmentToFirebase(String assignmentId, String link) {
        String uid = mAuth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        Submission sub = new Submission(currentUserName, uid, link, date, "Pending");
        mDatabase.child(course.courseId).child("assignments").child(assignmentId).child("submissions").child(uid).setValue(sub);
        Toast.makeText(this, "Submitted!", Toast.LENGTH_SHORT).show();
        assignmentAdapter.notifyDataSetChanged();
    }

    private void showAddLinkDialog(boolean isMaterial) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Material");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50,40,50,10);
        final EditText etName = new EditText(this); etName.setHint("Title"); layout.addView(etName);
        final EditText etUrl = new EditText(this); etUrl.setHint("Link"); layout.addView(etUrl);
        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            saveMaterial(etName.getText().toString(), etUrl.getText().toString());
        });
        builder.setNegativeButton("Cancel", null); builder.show();
    }

    private void saveMaterial(String name, String url) {
        if (TextUtils.isEmpty(name)) return;
        Material material = new Material(name, url);
        mDatabase.child(course.courseId).child("materials").push().setValue(material);
        NotificationHelper.sendToClass(course.classId, "New Material", "New material: '" + name + "' added in " + course.courseName);
        Toast.makeText(this, "Added!", Toast.LENGTH_SHORT).show();
    }

    private void loadMaterials() {
        mDatabase.child(course.courseId).child("materials").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                materialList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Material m = ds.getValue(Material.class);
                    if(m!=null) { m.key = ds.getKey(); materialList.add(m); }
                }
                materialAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAssignments() {
        mDatabase.child(course.courseId).child("assignments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignmentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Assignment a = ds.getValue(Assignment.class);
                    if (a!=null) {
                        a.id = ds.getKey();
                        assignmentList.add(a);
                    }
                }
                assignmentAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}