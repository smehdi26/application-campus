package com.example.coursemanagment;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {

    public static void sendNotification(String userId, String title, String message) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(userId);
        String date = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());

        Notification notification = new Notification(title, message, date);
        notifRef.push().setValue(notification);
    }

    // Send to ALL students in a specific class (For Course Materials / Assignments)
    public static void sendToClass(String classId, String title, String message) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.orderByChild("classId").equalTo(classId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String uid = ds.getKey();
                    // Send to each student found in this class
                    sendNotification(uid, title, message);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}