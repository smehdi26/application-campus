package com.example.coursemanagment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditCourseActivity extends AppCompatActivity {

    EditText etName, etCode, etTime, etLoc, etDesc;
    Spinner spinnerTeacher;
    Button btnUpdate;
    LinearLayout btnBack; // NEW: Back Button Layout

    DatabaseReference mDatabase, mUsersRef;
    Course course;

    List<User> teacherList = new ArrayList<>();
    List<String> teacherNames = new ArrayList<>();

    Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);

        course = (Course) getIntent().getSerializableExtra("course_data");

        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Link Views
        etName = findViewById(R.id.etEditCourseName);
        etCode = findViewById(R.id.etEditCourseCode);
        spinnerTeacher = findViewById(R.id.spinnerEditTeacher);
        etTime = findViewById(R.id.etEditTiming);
        etLoc = findViewById(R.id.etEditLocation);
        etDesc = findViewById(R.id.etEditDescription);
        btnUpdate = findViewById(R.id.btnUpdateCourse);
        btnBack = findViewById(R.id.btnBack); // Find Back Button

        // --- BACK BUTTON LOGIC ---
        btnBack.setOnClickListener(v -> finish());

        if (course != null) {
            etName.setText(course.courseName);
            etCode.setText(course.courseCode);
            etTime.setText(course.timing);
            etLoc.setText(course.location);
            etDesc.setText(course.description);
        }

        loadTeachers();

        etTime.setOnClickListener(v -> showDateTimePicker());

        btnUpdate.setOnClickListener(v -> updateCourse());
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy - HH:mm", Locale.getDefault());
                etTime.setText(sdf.format(calendar.getTime()));

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadTeachers() {
        mUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                teacherList.clear();
                teacherNames.clear();
                teacherNames.add("No Teacher (Unassigned)");
                teacherList.add(null);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && "Teacher".equalsIgnoreCase(user.role)) {
                        user.uid = ds.getKey();
                        teacherList.add(user);
                        teacherNames.add(user.firstName + " " + user.lastName);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditCourseActivity.this, android.R.layout.simple_spinner_item, teacherNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTeacher.setAdapter(adapter);

                if (course != null && course.teacherId != null) {
                    for (int i = 1; i < teacherList.size(); i++) {
                        if (teacherList.get(i) != null && teacherList.get(i).uid.equals(course.teacherId)) {
                            spinnerTeacher.setSelection(i);
                            break;
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCourse() {
        if (course == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("courseName", etName.getText().toString());
        updates.put("courseCode", etCode.getText().toString());
        updates.put("timing", etTime.getText().toString());
        updates.put("location", etLoc.getText().toString());
        updates.put("description", etDesc.getText().toString());

        int selectedPos = spinnerTeacher.getSelectedItemPosition();
        if (selectedPos > 0) {
            User selectedTeacher = teacherList.get(selectedPos);
            updates.put("teacherId", selectedTeacher.uid);
            updates.put("teacherName", selectedTeacher.firstName + " " + selectedTeacher.lastName);
        } else {
            updates.put("teacherId", "");
            updates.put("teacherName", "Unassigned");
        }

        mDatabase.child(course.courseId).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditCourseActivity.this, "Course Updated", Toast.LENGTH_SHORT).show();
                // We use an intent to reload the details page with fresh data or just finish
                // Ideally, sending an intent back is safer to refresh the view immediately
                Intent intent = new Intent(EditCourseActivity.this, CourseDetailsActivity.class);

                // IMPORTANT: We must update the 'course' object before sending it back,
                // otherwise the Details page will show old data until restarted.
                course.courseName = etName.getText().toString();
                course.courseCode = etCode.getText().toString();
                course.timing = etTime.getText().toString();
                course.location = etLoc.getText().toString();
                course.description = etDesc.getText().toString();
                if(selectedPos > 0) {
                    course.teacherName = teacherList.get(selectedPos).firstName + " " + teacherList.get(selectedPos).lastName;
                    course.teacherId = teacherList.get(selectedPos).uid;
                } else {
                    course.teacherName = "Unassigned";
                    course.teacherId = "";
                }

                intent.putExtra("course_data", course);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditCourseActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}