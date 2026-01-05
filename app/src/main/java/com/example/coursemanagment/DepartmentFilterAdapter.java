package com.example.coursemanagment;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;

public class DepartmentFilterAdapter extends RecyclerView.Adapter<DepartmentFilterAdapter.ViewHolder> {

    Context context;
    ArrayList<Department> list;
    OnDeptInteractionListener listener;
    String selectedDeptId = ""; // Empty string means "All"

    // Interface to communicate back to Activity
    public interface OnDeptInteractionListener {
        void onFilterChange(String deptId); // Pass "" for All, or ID for specific
    }

    public DepartmentFilterAdapter(Context context, ArrayList<Department> list, OnDeptInteractionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_department_tag, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Department dept = list.get(position);
        holder.tvName.setText(dept.name);

        // --- 1. COLOR LOGIC (Selection State) ---
        if (dept.id.equals(selectedDeptId)) {
            // Selected -> Red
            holder.tvName.setBackgroundResource(R.drawable.bg_role_badge);
            holder.tvName.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            // Not Selected -> Gray
            holder.tvName.setBackgroundResource(R.drawable.bg_badge_gray);
            holder.tvName.setTextColor(Color.parseColor("#555555"));
        }

        // --- 2. CLICK LOGIC (Toggle) ---
        holder.itemView.setOnClickListener(v -> {
            if (dept.id.equals(selectedDeptId)) {
                // Already selected? Unselect it (Show All)
                selectedDeptId = "";
            } else {
                // Select this one
                selectedDeptId = dept.id;
            }
            notifyDataSetChanged(); // Refresh colors
            listener.onFilterChange(selectedDeptId); // Tell Activity to filter
        });

        // --- 3. LONG CLICK (Delete) ---
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Department")
                    .setMessage("Delete '" + dept.name + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Delete from Firebase
                        FirebaseDatabase.getInstance().getReference("Departments")
                                .child(dept.id).removeValue();

                        // If we deleted the currently selected one, reset filter to All
                        if(dept.id.equals(selectedDeptId)) {
                            selectedDeptId = "";
                            listener.onFilterChange("");
                        }
                        Toast.makeText(context, "Department Deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Consumes the click so regular click doesn't fire
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeptTag);
        }
    }
}