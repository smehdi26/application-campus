package com.example.coursemanagment;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AdminAddClassActivity extends AppCompatActivity {

    EditText etName, etCode;
    Spinner spinnerDept;
    ImageView btnAddDept;
    Button btnSave;
    RecyclerView recyclerStudents;
    LinearLayout btnBack;

    DatabaseReference mClassesRef, mUsersRef, mDeptRef;
    ArrayList<User> studentList = new ArrayList<>();
    ArrayList<Department> deptList = new ArrayList<>();

    StudentSelectionAdapter adapter;
    Classroom existingClass;
    String currentClassId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_class);

        etName = findViewById(R.id.etClassName);
        etCode = findViewById(R.id.etClassCode);
        spinnerDept = findViewById(R.id.spinnerDept);
        btnAddDept = findViewById(R.id.btnAddDept);
        recyclerStudents = findViewById(R.id.recyclerSelectStudents);
        btnSave = findViewById(R.id.btnSaveClass);
        btnBack = findViewById(R.id.btnBack);

        mClassesRef = FirebaseDatabase.getInstance().getReference("Classes");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");
        mDeptRef = FirebaseDatabase.getInstance().getReference("Departments");

        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentSelectionAdapter(this, studentList);
        recyclerStudents.setAdapter(adapter);

        existingClass = (Classroom) getIntent().getSerializableExtra("class_data");
        if (existingClass != null) {
            etName.setText(existingClass.name);
            etCode.setText(existingClass.code);
            currentClassId = existingClass.id;
        } else {
            currentClassId = mClassesRef.push().getKey();
        }

        btnBack.setOnClickListener(v -> finish());

        loadDepartments();
        loadStudents();

        btnAddDept.setOnClickListener(v -> showAddDeptDialog());
        btnSave.setOnClickListener(v -> saveClass());
    }

    private void loadDepartments() {
        mDeptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                deptList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    deptList.add(ds.getValue(Department.class));
                }
                ArrayAdapter<Department> deptAdapter = new ArrayAdapter<>(AdminAddClassActivity.this, android.R.layout.simple_spinner_item, deptList);
                deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDept.setAdapter(deptAdapter);

                if (existingClass != null && existingClass.departmentId != null) {
                    for (int i = 0; i < deptList.size(); i++) {
                        if (deptList.get(i).id.equals(existingClass.departmentId)) {
                            spinnerDept.setSelection(i); break;
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddDeptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Department");
        final EditText input = new EditText(this);
        input.setHint("Name");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString();
            if (!TextUtils.isEmpty(name)) {
                String id = mDeptRef.push().getKey();
                mDeptRef.child(id).setValue(new Department(id, name));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
                            boolean isUnassigned = (user.classId == null || user.classId.isEmpty());
                            boolean isInThisClass = (currentClassId.equals(user.classId));
                            if (isUnassigned || isInThisClass) {
                                studentList.add(user);
                                if (isInThisClass) adapter.selectedUserIds.add(user.uid);
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
        Department selectedDept = (Department) spinnerDept.getSelectedItem();

        if (TextUtils.isEmpty(name) || selectedDept == null) {
            Toast.makeText(this, "Fill fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Classroom newClass = new Classroom(currentClassId, name, code, selectedDept.id);
        mClassesRef.child(currentClassId).setValue(newClass);

        for (User student : studentList) {
            boolean isSelected = adapter.selectedUserIds.contains(student.uid);
            if (isSelected) {
                mUsersRef.child(student.uid).child("classId").setValue(currentClassId);

                // --- TRIGGER NOTIFICATION ---
                NotificationHelper.sendNotification(student.uid, "Class Enrollment", "You have been added to: " + name);

            } else if (currentClassId.equals(student.classId)) {
                mUsersRef.child(student.uid).child("classId").setValue("");
            }
        }
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}