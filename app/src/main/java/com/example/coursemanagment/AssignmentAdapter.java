package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.MyViewHolder> {

    Context context;
    ArrayList<Assignment> list;
    String userRole;
    String courseId;
    String currentUserId;

    // Interfaces for callbacks
    public interface AssignmentActionListener {
        void onSubmitClick(Assignment assignment);
        void onEditClick(Assignment assignment); // NEW: Callback for editing
    }
    AssignmentActionListener actionListener;

    public AssignmentAdapter(Context context, ArrayList<Assignment> list, String userRole, String courseId, AssignmentActionListener listener) {
        this.context = context;
        this.list = list;
        this.userRole = userRole;
        this.courseId = courseId;
        this.actionListener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void setUserRole(String role) {
        this.userRole = role;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_assignment, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Assignment assignment = list.get(position);
        holder.tvTitle.setText(assignment.title);
        holder.tvDate.setText("Due: " + assignment.dueDate);

        // --- TEACHER / ADMIN VIEW ---
        if ("Admin".equalsIgnoreCase(userRole) || "Teacher".equalsIgnoreCase(userRole)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE); // Show Edit
            holder.btnSubmit.setVisibility(View.GONE);
            holder.layoutInfo.setVisibility(View.GONE);
            holder.tvBadge.setVisibility(View.GONE);

            // Drill Down
            holder.root.setOnClickListener(v -> {
                Intent intent = new Intent(context, AssignmentSubmissionsActivity.class);
                intent.putExtra("course_id", courseId);
                intent.putExtra("assign_id", assignment.id);
                intent.putExtra("assign_title", assignment.title);
                context.startActivity(intent);
            });

            // EDIT
            holder.btnEdit.setOnClickListener(v -> actionListener.onEditClick(assignment));

            // DELETE
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context).setTitle("Delete").setMessage("Are you sure?")
                        .setPositiveButton("Yes", (d, w) -> {
                            FirebaseDatabase.getInstance().getReference("Courses").child(courseId)
                                    .child("assignments").child(assignment.id).removeValue();
                        }).setNegativeButton("No", null).show();
            });
        }
        // --- STUDENT VIEW ---
        else {
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.root.setOnClickListener(null);

            DatabaseReference subRef = FirebaseDatabase.getInstance().getReference("Courses")
                    .child(courseId).child("assignments").child(assignment.id).child("submissions").child(currentUserId);

            subRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // ... (Existing logic for showing Submitted/Graded view) ...
                        Submission sub = snapshot.getValue(Submission.class);
                        holder.btnSubmit.setVisibility(View.GONE);
                        holder.tvBadge.setVisibility(View.VISIBLE);
                        holder.tvBadge.setText("Submitted");
                        holder.layoutInfo.setVisibility(View.VISIBLE);
                        if(sub != null) {
                            String grade = sub.grade != null ? sub.grade : "Pending";
                            holder.tvGrade.setText(grade);
                            holder.tvSubDate.setText(sub.date);
                            // Apply colors (reuse previous logic here)
                            if(grade.equals("Pending")) holder.tvGrade.setTextColor(Color.parseColor("#555555"));
                            holder.tvLink.setOnClickListener(v -> { /* open link */ });
                        }
                    } else {
                        // Not Submitted
                        holder.layoutInfo.setVisibility(View.GONE);
                        holder.tvBadge.setText("Pending");
                        holder.tvBadge.setVisibility(View.VISIBLE);

                        // --- DEADLINE LOGIC ---
                        long now = System.currentTimeMillis();
                        if (now > assignment.timestamp) {
                            // Late!
                            holder.btnSubmit.setVisibility(View.GONE); // Hide button
                            holder.tvBadge.setText("Missed Deadline");
                            holder.tvBadge.setTextColor(Color.RED);
                        } else {
                            // On Time
                            holder.btnSubmit.setVisibility(View.VISIBLE);
                            holder.tvBadge.setTextColor(Color.parseColor("#555555"));
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            holder.btnSubmit.setOnClickListener(v -> actionListener.onSubmitClick(assignment));
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvBadge, tvGrade, tvLink, tvSubDate;
        ImageView btnDelete, btnEdit; // Added btnEdit
        Button btnSubmit;
        LinearLayout layoutInfo;
        LinearLayout root;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAssignTitle);
            tvDate = itemView.findViewById(R.id.tvAssignDate);
            tvBadge = itemView.findViewById(R.id.tvStatusBadge);
            btnDelete = itemView.findViewById(R.id.btnDeleteAssign);
            btnEdit = itemView.findViewById(R.id.btnEditAssign); // Bind ID
            btnSubmit = itemView.findViewById(R.id.btnSubmitAssignment);
            layoutInfo = itemView.findViewById(R.id.layoutSubmissionInfo);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvLink = itemView.findViewById(R.id.tvStudentLink);
            tvSubDate = itemView.findViewById(R.id.tvSubmissionDate);
            root = itemView.findViewById(R.id.itemAssignmentRoot);
        }
    }
}