package com.example.coursemanagment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AdminAddClassActivity extends AppCompatActivity {

    EditText etName, etCode;
    Button btnSave;
    RecyclerView recyclerStudents;
    LinearLayout btnBack; // Define Back Button layout

    DatabaseReference mClassesRef, mUsersRef;
    ArrayList<User> studentList = new ArrayList<>();
    StudentSelectionAdapter adapter;
    Classroom existingClass;
    String currentClassId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_class);

        // Initialize Views
        etName = findViewById(R.id.etClassName);
        etCode = findViewById(R.id.etClassCode);
        recyclerStudents = findViewById(R.id.recyclerSelectStudents);
        btnSave = findViewById(R.id.btnSaveClass);
        btnBack = findViewById(R.id.btnBack); // Find Back Button

        mClassesRef = FirebaseDatabase.getInstance().getReference("Classes");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentSelectionAdapter(this, studentList);
        recyclerStudents.setAdapter(adapter);

        // Check if editing
        existingClass = (Classroom) getIntent().getSerializableExtra("class_data");
        if (existingClass != null) {
            etName.setText(existingClass.name);
            etCode.setText(existingClass.code);
            currentClassId = existingClass.id;
        } else {
            currentClassId = mClassesRef.push().getKey();
        }

        // Back Button Click
        btnBack.setOnClickListener(v -> finish());

        loadStudents();

        btnSave.setOnClickListener(v -> saveClass());
    }

    private void loadStudents() {
        mUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.uid = ds.getKey();

                        if ("Student".equalsIgnoreCase(user.role)) {

                            // --- NEW FILTERING LOGIC ---
                            // Show the student IF:
                            // 1. They are completely unassigned (classId is null or empty)
                            // OR
                            // 2. They are already in THIS class (so we can see/uncheck them)

                            boolean isUnassigned = (user.classId == null || user.classId.isEmpty());
                            boolean isInThisClass = (currentClassId.equals(user.classId));

                            if (isUnassigned || isInThisClass) {
                                studentList.add(user);

                                // Pre-check the box if they are in this class
                                if (isInThisClass) {
                                    adapter.selectedUserIds.add(user.uid);
                                }
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveClass() {
        String name = etName.getText().toString();
        String code = etCode.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (adapter.selectedUserIds.size() > 35) {
            Toast.makeText(this, "Max 35 students allowed!", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Save Class Info
        Classroom newClass = new Classroom(currentClassId, name, code);
        mClassesRef.child(currentClassId).setValue(newClass);

        // 2. Update Students
        for (User student : studentList) {
            boolean isSelected = adapter.selectedUserIds.contains(student.uid);

            // If checked -> Assign to this class
            if (isSelected) {
                mUsersRef.child(student.uid).child("classId").setValue(currentClassId);
            }
            // If UNchecked (and they were previously in this class) -> Unassign them
            else if (currentClassId.equals(student.classId)) {
                mUsersRef.child(student.uid).child("classId").setValue("");
            }
        }

        Toast.makeText(this, "Class Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}