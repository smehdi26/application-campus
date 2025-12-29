package com.example.coursemanagment;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class AdminAddCourseActivity extends AppCompatActivity {

    EditText etName, etCode, etTime, etLoc;
    Spinner spinner;
    Button btnSave;

    DatabaseReference mCoursesRef, mUsersRef;
    String classId;

    List<User> teacherList = new ArrayList<>();
    List<String> teacherNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_course);

        classId = getIntent().getStringExtra("class_id");
        mCoursesRef = FirebaseDatabase.getInstance().getReference("Courses");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        etName = findViewById(R.id.etAC_Name);
        etCode = findViewById(R.id.etAC_Code);
        etTime = findViewById(R.id.etAC_Time);
        etLoc = findViewById(R.id.etAC_Loc);
        spinner = findViewById(R.id.spinnerAC_Teacher);
        btnSave = findViewById(R.id.btnAC_Save);

        loadTeachers();

        btnSave.setOnClickListener(v -> saveCourse());
    }

    private void loadTeachers() {
        mUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                teacherList.clear();
                teacherNames.clear();
                teacherNames.add("Select Teacher");
                teacherList.add(null);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && "Teacher".equalsIgnoreCase(user.role)) {
                        user.uid = ds.getKey(); // Ensure ID is captured
                        teacherList.add(user);
                        teacherNames.add(user.firstName + " " + user.lastName);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminAddCourseActivity.this, android.R.layout.simple_spinner_item, teacherNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveCourse() {
        String name = etName.getText().toString();
        String code = etCode.getText().toString();
        String time = etTime.getText().toString();
        String loc = etLoc.getText().toString();
        int pos = spinner.getSelectedItemPosition();

        if (TextUtils.isEmpty(name) || pos == 0) {
            Toast.makeText(this, "Name and Teacher are required", Toast.LENGTH_SHORT).show();
            return;
        }

        User teacher = teacherList.get(pos);
        String courseId = mCoursesRef.push().getKey();

        Course newCourse = new Course(courseId, name, code, "No Desc", time, loc, classId, teacher.uid, teacher.firstName + " " + teacher.lastName);

        mCoursesRef.child(courseId).setValue(newCourse).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Course Created!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}