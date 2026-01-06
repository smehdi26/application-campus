package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditCourseActivity extends AppCompatActivity {

    EditText etName, etCode, etProf, etTime, etLoc, etDesc;
    Button btnUpdate;
    DatabaseReference mDatabase;
    Course course;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);

        // 1. Get the course passed from the previous screen
        course = (Course) getIntent().getSerializableExtra("course_data");

        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");

        // 2. Link Views
        etName = findViewById(R.id.etEditCourseName);
        etCode = findViewById(R.id.etEditCourseCode);
        etProf = findViewById(R.id.etEditProfessor);
        etTime = findViewById(R.id.etEditTiming);
        etLoc = findViewById(R.id.etEditLocation);
        etDesc = findViewById(R.id.etEditDescription);
        btnUpdate = findViewById(R.id.btnUpdateCourse);

        // 3. Pre-fill the data
        if (course != null) {
            etName.setText(course.courseName);
            etCode.setText(course.courseCode);
            etProf.setText(course.teacherName);
            etTime.setText(course.timing);
            etLoc.setText(course.location);
            etDesc.setText(course.description);
        }

        // 4. Update Button Logic
        btnUpdate.setOnClickListener(v -> updateCourse());
    }

    private void updateCourse() {
        if (course == null || course.courseId == null) {
            Toast.makeText(this, "Error: Course data is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a Map to update only specific fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("courseName", etName.getText().toString());
        updates.put("courseCode", etCode.getText().toString());
        updates.put("professor", etProf.getText().toString());
        updates.put("timing", etTime.getText().toString());
        updates.put("location", etLoc.getText().toString());
        updates.put("description", etDesc.getText().toString());

        // Update Firebase using the unique Course ID
        mDatabase.child(course.courseId).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditCourseActivity.this, "Course Updated", Toast.LENGTH_SHORT).show();

                // Go back to the Course List (Clears the stack so we don't see old data)
                Intent intent = new Intent(EditCourseActivity.this, CoursesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                Toast.makeText(EditCourseActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}