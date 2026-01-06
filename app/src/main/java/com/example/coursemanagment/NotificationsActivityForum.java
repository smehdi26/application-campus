package com.example.coursemanagment;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;

public class NotificationsActivityForum extends AppCompatActivity {

    RecyclerView recyclerView;
    NotificationAdapterForum adapter;
    ArrayList<NotificationForum> notificationsList;
    ImageView btnBack;
    LinearLayout emptyState;
    String currentUserId;
    private ValueEventListener notificationsListener;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_forum);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerNotifications);
        btnBack = findViewById(R.id.btnBack);
        emptyState = findViewById(R.id.emptyState);

        btnBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsList = new ArrayList<>();
        adapter = new NotificationAdapterForum(this, notificationsList, currentUserId);
        recyclerView.setAdapter(adapter);

        loadNotifications();
        markAllAsRead();
    }

    private void markAllAsRead() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId)
                .child("Notifications")
                .orderByChild("isRead")
                .equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().child("isRead").setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Log error
                    }
                });
    }

    private void loadNotifications() {
        if (notificationsListener != null) {
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId)
                    .child("Notifications")
                    .removeEventListener(notificationsListener);
        }

        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationsList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationForum notificationForum = child.getValue(NotificationForum.class);
                    if (notificationForum != null) {
                        notificationsList.add(notificationForum);
                    }
                }
                
                // Sort by timestamp descending (newest first)
                Collections.sort(notificationsList, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
                
                adapter.notifyDataSetChanged();
                
                // Show/hide empty state
                if (notificationsList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        };

        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId)
                .child("Notifications")
                .addValueEventListener(notificationsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsListener != null) {
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId)
                    .child("Notifications")
                    .removeEventListener(notificationsListener);
            notificationsListener = null;
        }
    }
}
