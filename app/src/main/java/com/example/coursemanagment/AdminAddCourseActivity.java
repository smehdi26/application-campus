package com.example.coursemanagment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminAddCourseActivity extends AppCompatActivity {

    EditText etName, etCode, etLoc;
    Spinner spinner;
    Button btnSave;
    LinearLayout btnBack, btnAddTime, layoutTimeContainer;

    DatabaseReference mCoursesRef, mUsersRef;
    String classId;

    List<User> teacherList = new ArrayList<>();
    List<String> teacherNames = new ArrayList<>();

    // Store multiple times here
    ArrayList<String> selectedTimes = new ArrayList<>();
    Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_course);

        classId = getIntent().getStringExtra("class_id");
        mCoursesRef = FirebaseDatabase.getInstance().getReference("Courses");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        etName = findViewById(R.id.etAC_Name);
        etCode = findViewById(R.id.etAC_Code);
        etLoc = findViewById(R.id.etAC_Loc);
        spinner = findViewById(R.id.spinnerAC_Teacher);
        btnSave = findViewById(R.id.btnAC_Save);
        btnBack = findViewById(R.id.btnBack);

        // Time Views
        btnAddTime = findViewById(R.id.btnAddTime);
        layoutTimeContainer = findViewById(R.id.layoutTimeContainer);

        btnBack.setOnClickListener(v -> finish());
        loadTeachers();

        // Add Time Click
        btnAddTime.setOnClickListener(v -> showDateTimePicker());

        btnSave.setOnClickListener(v -> saveCourse());
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

                // Add to list and update UI
                selectedTimes.add(timeString);
                refreshTimeViews();

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void refreshTimeViews() {
        layoutTimeContainer.removeAllViews(); // Clear current list

        for (int i = 0; i < selectedTimes.size(); i++) {
            String time = selectedTimes.get(i);
            int index = i;

            // Create a simple row view programmatically
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            TextView tv = new TextView(this);
            tv.setText("• " + time);
            tv.setTextSize(16);
            tv.setTextColor(getResources().getColor(R.color.black));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tv.setLayoutParams(params);

            // Delete Icon
            ImageView imgDelete = new ImageView(this);
            imgDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            imgDelete.setColorFilter(getResources().getColor(R.color.esprit_red));
            imgDelete.setPadding(10, 0, 10, 0);

            // Remove time on click
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
                teacherNames.add("Select Teacher");
                teacherList.add(null);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && "Teacher".equalsIgnoreCase(user.role)) {
                        user.uid = ds.getKey();
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
        String loc = etLoc.getText().toString();
        int pos = spinner.getSelectedItemPosition();

        if (TextUtils.isEmpty(name) || pos == 0 || selectedTimes.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and add at least one time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine all times into one string separated by new line
        String timeString = TextUtils.join("\n", selectedTimes);

        User teacher = teacherList.get(pos);
        String courseId = mCoursesRef.push().getKey();

        Course newCourse = new Course(courseId, name, code, "No Desc", timeString, loc, classId, teacher.uid, teacher.firstName + " " + teacher.lastName);

        mCoursesRef.child(courseId).setValue(newCourse).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Course Created!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}