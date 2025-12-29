package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MyViewHolder> {

    Context context;
    ArrayList<Material> list;
    String userRole;
    String courseId;

    public MaterialAdapter(Context context, ArrayList<Material> list, String userRole, String courseId) {
        this.context = context;
        this.list = list;
        this.userRole = userRole;
        this.courseId = courseId;
    }

    // --- NEW METHOD: Update Role dynamically ---
    public void setUserRole(String role) {
        this.userRole = role;
        notifyDataSetChanged(); // This forces the list to redraw with correct permissions
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_material, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Material material = list.get(position);
        holder.tvName.setText(material.name);

        // --- VISIBILITY LOGIC ---
        // Hide delete button for Students
        if ("Student".equalsIgnoreCase(userRole)) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            // Show for Admin and Teacher
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        // Click to Open
        holder.root.setOnClickListener(v -> {
            if (material.url != null && !material.url.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(material.url));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Delete Logic
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Material")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseDatabase.getInstance().getReference("Courses")
                                .child(courseId)
                                .child("materials")
                                .child(material.key)
                                .removeValue();
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null).show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        LinearLayout root;
        ImageView btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            root = itemView.findViewById(R.id.itemMaterialRoot);
            btnDelete = itemView.findViewById(R.id.btnDeleteMaterial);
        }
    }
}