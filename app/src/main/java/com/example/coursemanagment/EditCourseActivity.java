package com.example.coursemanagment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditCourseActivity extends AppCompatActivity {

    EditText etName, etCode, etLoc, etDesc;
    Spinner spinnerTeacher;
    Button btnUpdate;
    LinearLayout btnAddTime, layoutTimeContainer, btnBack;

    DatabaseReference mDatabase, mUsersRef;
    Course course;

    List<User> teacherList = new ArrayList<>();
    List<String> teacherNames = new ArrayList<>();

    // Multi-time logic
    ArrayList<String> selectedTimes = new ArrayList<>();
    Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);

        course = (Course) getIntent().getSerializableExtra("course_data");

        mDatabase = FirebaseDatabase.getInstance().getReference("Courses");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Views
        etName = findViewById(R.id.etEditCourseName);
        etCode = findViewById(R.id.etEditCourseCode);
        etLoc = findViewById(R.id.etEditLocation);
        etDesc = findViewById(R.id.etEditDescription);
        spinnerTeacher = findViewById(R.id.spinnerEditTeacher);
        btnUpdate = findViewById(R.id.btnUpdateCourse);

        // Time Views
        btnAddTime = findViewById(R.id.btnAddTime);
        layoutTimeContainer = findViewById(R.id.layoutTimeContainer);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // 1. Pre-fill existing data
        if (course != null) {
            etName.setText(course.courseName);
            etCode.setText(course.courseCode);
            etLoc.setText(course.location);
            etDesc.setText(course.description);

            // --- PARSE EXISTING TIMES ---
            if (course.timing != null && !course.timing.isEmpty()) {
                // Split string by new line
                String[] times = course.timing.split("\n");
                Collections.addAll(selectedTimes, times);
                refreshTimeViews();
            }
        }

        loadTeachers();

        // 2. Add New Time Logic
        btnAddTime.setOnClickListener(v -> showDateTimePicker());

        // 3. Update Logic
        btnUpdate.setOnClickListener(v -> updateCourse());
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            new TimePickerDialog(this, (tView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM - HH:mm", Locale.getDefault());
                String timeString = sdf.format(calendar.getTime());

                selectedTimes.add(timeString);
                refreshTimeViews();

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void refreshTimeViews() {
        layoutTimeContainer.removeAllViews();

        for (int i = 0; i < selectedTimes.size(); i++) {
            String time = selectedTimes.get(i);
            int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            TextView tv = new TextView(this);
            tv.setText("• " + time);
            tv.setTextSize(16);
            tv.setTextColor(getResources().getColor(R.color.black));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tv.setLayoutParams(params);

            ImageView imgDelete = new ImageView(this);
            imgDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            imgDelete.setColorFilter(getResources().getColor(R.color.esprit_red));
            imgDelete.setPadding(10, 0, 10, 0);

            imgDelete.setOnClickListener(v -> {
                selectedTimes.remove(index);
                refreshTimeViews();
            });

            row.addView(tv);
            row.addView(imgDelete);
            layoutTimeContainer.addView(row);
        }
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
                        if (teacherList.get(i).uid.equals(course.teacherId)) {
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

        // JOIN TIMES INTO ONE STRING
        String timeString = TextUtils.join("\n", selectedTimes);

        Map<String, Object> updates = new HashMap<>();
        updates.put("courseName", etName.getText().toString());
        updates.put("courseCode", etCode.getText().toString());
        updates.put("timing", timeString); // Save joined string
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
                finish();
            } else {
                Toast.makeText(EditCourseActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}