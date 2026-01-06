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
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyViewHolder> {
    Context context;
    ArrayList<Notification> list;
    String currentUserId;

    public NotificationAdapter(Context context, ArrayList<Notification> list, String currentUserId) {
        this.context = context;
        this.list = list;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_notification, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Notification notification = list.get(position);

        if (notification.triggerUserName != null && !notification.triggerUserName.isEmpty()) {
            String initial = String.valueOf(notification.triggerUserName.charAt(0)).toUpperCase(java.util.Locale.getDefault());
            holder.tvAvatar.setText(initial);
        }

        holder.tvMessage.setText(notification.message);
        holder.tvPostTitle.setText(notification.postTitle);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(notification.timestamp)));

        // Set visual state based on read status
        holder.itemView.setAlpha(notification.isRead ? 0.6f : 1.0f);

        holder.itemView.setOnClickListener(v -> {
            if (currentUserId == null || currentUserId.isEmpty() || notification.notificationId == null || notification.notificationId.isEmpty()) return;

            // 1. Immediate UI Feedback & Firebase update
            if (!notification.isRead) {
                notification.isRead = true;
                notifyItemChanged(position);
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(currentUserId).child("Notifications")
                        .child(notification.notificationId).child("isRead").setValue(true);
            }

            // 2. Fetch the post from Firebase
            if (notification.postId != null && !notification.postId.isEmpty()) {
                holder.itemView.setEnabled(false); // Prevent multiple clicks
                FirebaseDatabase.getInstance().getReference("Forum").child("Posts")
                        .child(notification.postId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                holder.itemView.setEnabled(true);
                                if (snapshot.exists()) {
                                    Post post = snapshot.getValue(Post.class);
                                    if (post != null) {
                                        post.postId = snapshot.getKey();
                                        Intent intent = new Intent(context, PostDetailsActivity.class);
                                        intent.putExtra("post", post); // Pass the full Post object
                                        context.startActivity(intent);
                                    }
                                } else {
                                    Toast.makeText(context, "Post no longer exists", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                holder.itemView.setEnabled(true);
                                Toast.makeText(context, "Failed to load post: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (currentUserId != null && !currentUserId.isEmpty() && notification.notificationId != null && !notification.notificationId.isEmpty()) {
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(currentUserId).child("Notifications")
                        .child(notification.notificationId).removeValue();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvPostTitle, tvTime, tvAvatar;
        ImageView btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvNotificationAvatar);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvPostTitle = itemView.findViewById(R.id.tvNotificationPostTitle);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
        }
    }
}
