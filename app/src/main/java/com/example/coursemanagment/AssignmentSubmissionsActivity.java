package com.example.coursemanagment;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AssignmentSubmissionsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    SubmissionAdapter adapter;
    ArrayList<Submission> list;
    DatabaseReference mDatabase;
    LinearLayout btnBack;
    TextView tvHeaderTitle;
    String courseId, assignmentId, assignmentTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_student_list); // Reusing the list layout

        // Get Data from Intent
        courseId = getIntent().getStringExtra("course_id");
        assignmentId = getIntent().getStringExtra("assign_id");
        assignmentTitle = getIntent().getStringExtra("assign_title");

        // Link Views
        recyclerView = findViewById(R.id.recyclerClassStudents);
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        // Set Title dynamically
        if (assignmentTitle != null) {
            tvHeaderTitle.setText("Submissions: " + assignmentTitle);
        } else {
            tvHeaderTitle.setText("Submissions");
        }

        btnBack.setOnClickListener(v -> finish());

        // Setup Firebase Path: Courses -> [ID] -> assignments -> [ID] -> submissions
        mDatabase = FirebaseDatabase.getInstance().getReference("Courses")
                .child(courseId).child("assignments").child(assignmentId).child("submissions");

        // Setup List
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();

        // IMPORTANT: Pass courseId and assignmentId to the Adapter so it can save grades
        adapter = new SubmissionAdapter(this, list, courseId, assignmentId);

        recyclerView.setAdapter(adapter);

        loadSubmissions();
    }

    private void loadSubmissions() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Submission s = ds.getValue(Submission.class);
                    if(s != null) {
                        list.add(s);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}