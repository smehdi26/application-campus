package com.example.coursemanagment;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class StudentSelectionAdapter extends RecyclerView.Adapter<StudentSelectionAdapter.ViewHolder> {

    Context context;
    ArrayList<User> students;
    public Set<String> selectedUserIds = new HashSet<>(); // Stores IDs of checked students

    public StudentSelectionAdapter(Context context, ArrayList<User> students) {
        this.context = context;
        this.students = students;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_student_select, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = students.get(position);
        holder.name.setText(student.firstName + " " + student.lastName);

        // Logic to show status
        if (student.classId == null || student.classId.isEmpty()) {
            holder.status.setText("Unassigned");
            holder.status.setTextColor(Color.GRAY);
        } else {
            // Note: In a real app, you'd fetch the Class Name using the ID.
            // For now, we just warn the admin.
            holder.status.setText("Currently in another class");
            holder.status.setTextColor(Color.RED);
        }

        // Checkbox Logic
        // Avoid triggering listener during binding
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedUserIds.contains(student.uid));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUserIds.add(student.uid);
            } else {
                selectedUserIds.remove(student.uid);
            }
        });
    }

    @Override
    public int getItemCount() { return students.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, status;
        CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvStudentName);
            status = itemView.findViewById(R.id.tvStudentStatus);
            checkBox = itemView.findViewById(R.id.cbSelectStudent);
        }
    }
}