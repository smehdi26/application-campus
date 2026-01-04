package com.example.coursemanagment;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class TeacherStudentListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    UserAdapter adapter; // Reusing your existing UserAdapter
    ArrayList<User> list;
    DatabaseReference mDatabase;
    LinearLayout btnBack;
    String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_student_list);

        classId = getIntent().getStringExtra("class_id");

        recyclerView = findViewById(R.id.recyclerClassStudents);
        btnBack = findViewById(R.id.btnBack);
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new UserAdapter(this, list);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        if (classId != null && !classId.isEmpty()) {
            loadStudents();
        } else {
            Toast.makeText(this, "Class ID missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStudents() {
        // Query users where 'classId' matches the current course's class
        Query query = mDatabase.orderByChild("classId").equalTo(classId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    // Double check to ensure we only show Students (optional)
                    if (user != null && "Student".equalsIgnoreCase(user.role)) {
                        list.add(user);
                    }
                }
                adapter.notifyDataSetChanged();

                if (list.isEmpty()) {
                    Toast.makeText(TeacherStudentListActivity.this, "No students found in this class.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}