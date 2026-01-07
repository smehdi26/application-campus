package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ClassAdminAdapter extends RecyclerView.Adapter<ClassAdminAdapter.ViewHolder> {

    Context context;
    ArrayList<Classroom> list;

    public ClassAdminAdapter(Context context, ArrayList<Classroom> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_class_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Classroom classroom = list.get(position);
        holder.name.setText(classroom.name);
        holder.code.setText("Code: " + classroom.code);
        holder.info.setText("Max Students: " + classroom.maxStudents);

        // --- DELETE LOGIC ---
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Class")
                    .setMessage("Are you sure? This will unassign all students currently in this class.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        deleteClassAndUnassignStudents(classroom.id);
                    })
                    .setNegativeButton("No", null).show();
        });

        // Edit Logic -> NOW GOES TO ADMIN CLASS DETAILS
        holder.itemView.setOnClickListener(v -> {
            // Change destination to AdminClassDetailsActivity
            Intent intent = new Intent(context, AdminClassDetailsActivity.class);
            intent.putExtra("class_data", classroom);
            context.startActivity(intent);
        });
    }

    private void deleteClassAndUnassignStudents(String classId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        DatabaseReference classesRef = FirebaseDatabase.getInstance().getReference("Classes");

        // 1. Find all students belonging to this class
        Query query = usersRef.orderByChild("classId").equalTo(classId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Loop through found students and clear their classId
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    userSnapshot.getRef().child("classId").setValue("");
                }

                // 2. Delete the class itself
                classesRef.child(classId).removeValue()
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(context, "Class deleted & Students freed", Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error updating students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, code, info;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvClassName);
            code = itemView.findViewById(R.id.tvClassCode);
            info = itemView.findViewById(R.id.tvTeacherName);
            btnDelete = itemView.findViewById(R.id.btnDeleteClass);
        }
    }
}