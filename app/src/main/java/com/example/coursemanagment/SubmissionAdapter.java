package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.ViewHolder> {

    Context context;
    ArrayList<Submission> list;
    String courseId;
    String assignmentId;

    // Updated Constructor to accept IDs
    public SubmissionAdapter(Context context, ArrayList<Submission> list, String courseId, String assignmentId) {
        this.context = context;
        this.list = list;
        this.courseId = courseId;
        this.assignmentId = assignmentId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_submission, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Submission sub = list.get(position);
        holder.name.setText(sub.studentName);
        holder.date.setText(sub.date);

        // --- GRADE LOGIC ---
        String grade = sub.grade != null ? sub.grade : "Pending";
        holder.grade.setText(grade);

        // --- COLOR CODING ---
        applyGradeColor(holder.grade, grade);

        // --- CLICK TO GRADE (Teacher/Admin) ---
        holder.grade.setOnClickListener(v -> showGradingDialog(sub));

        // Click to Open File
        holder.root.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(sub.link));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Invalid Link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGradingDialog(Submission sub) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Grade for " + sub.studentName);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter score (0-100)");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String score = input.getText().toString();
            if (!score.isEmpty()) {
                String finalGrade = score + "/100"; // Format: 85/100

                // Update Firebase
                FirebaseDatabase.getInstance().getReference("Courses")
                        .child(courseId)
                        .child("assignments")
                        .child(assignmentId)
                        .child("submissions")
                        .child(sub.studentId)
                        .child("grade")
                        .setValue(finalGrade);

                Toast.makeText(context, "Graded!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void applyGradeColor(TextView tv, String gradeText) {
        // Reset default
        tv.setBackgroundResource(R.drawable.bg_badge_gray);
        tv.setTextColor(Color.parseColor("#555555")); // Grey

        if (gradeText.equals("Pending")) return;

        try {
            // Extract number from "85/100" -> "85"
            String numberOnly = gradeText.split("/")[0];
            int score = Integer.parseInt(numberOnly);

            if (score >= 75) {
                // GREEN
                tv.setTextColor(Color.parseColor("#388E3C"));
                tv.setBackgroundResource(R.drawable.bg_role_badge); // Reusing light red/pink shape, ideally create a green one, but text color matters most
            } else if (score >= 60) {
                // YELLOW/ORANGE
                tv.setTextColor(Color.parseColor("#FBC02D"));
            } else {
                // RED
                tv.setTextColor(Color.parseColor("#D32F2F"));
            }
        } catch (Exception e) {
            // Keep default if parsing fails
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, grade;
        LinearLayout root;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvSubStudentName);
            date = itemView.findViewById(R.id.tvSubDate);
            grade = itemView.findViewById(R.id.tvSubGrade);
            root = itemView.findViewById(R.id.rootSubmission);
        }
    }
}