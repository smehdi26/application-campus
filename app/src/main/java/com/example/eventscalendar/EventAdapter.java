package com.example.eventscalendar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<EventModel> events;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mEventsDatabase;
    public EventAdapter(List<EventModel> events) {
        this.events = events;
        this.mAuth = FirebaseAuth.getInstance();
        this.mUsersDatabase = FirebaseDatabase.getInstance().getReference("Users");
        this.mEventsDatabase = FirebaseDatabase.getInstance().getReference("events");
    }
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = events.get(position);
        holder.title.setText(event.getTitle());
        holder.dateTime.setText(event.getDate() + " " + event.getTime());
        holder.location.setText(event.getLocation());
        holder.category.setText(event.getCategory());
        String category = event.getCategory();
        if (category.equals("Soutenances")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_soutenances);
            holder.category.setTextColor(Color.parseColor("#FFA000"));
        } else if (category.equals("Exams") || category.equals("Examens")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_exams);
            holder.category.setTextColor(Color.parseColor("#D32F2F"));
        } else if (category.equals("Clubs")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_clubs);
            holder.category.setTextColor(Color.parseColor("#00796B"));
        } else if (category.equals("Conferences") || category.equals("Conférences")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_conferences);
            holder.category.setTextColor(Color.parseColor("#1976D2"));
        } else {
            holder.category.setBackgroundResource(R.drawable.bg_badge_gray);
            holder.category.setTextColor(Color.GRAY);
        }
        checkUserRole(holder);
        holder.btnDeleteEvent.setOnClickListener(v -> {
            mEventsDatabase.child(event.getId()).removeValue();
            Toast.makeText(v.getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
        });
                    holder.btnEditEvent.setOnClickListener(v -> {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, EditEventActivity.class);
                        intent.putExtra("eventId", event.getId());
                        intent.putExtra("eventTitle", event.getTitle());
                        intent.putExtra("eventCategory", event.getCategory());
                        intent.putExtra("eventDate", event.getDate());
                        intent.putExtra("eventTime", event.getTime());
                        intent.putExtra("eventLocation", event.getLocation());
                        intent.putExtra("eventDescription", event.getDescription());
                        context.startActivity(intent);
                    });    }
    private void checkUserRole(EventViewHolder holder) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            mUsersDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && "Admin".equalsIgnoreCase(user.role)) {
                            holder.adminOptions.setVisibility(View.VISIBLE);
                        } else {
                            holder.adminOptions.setVisibility(View.GONE);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.adminOptions.setVisibility(View.GONE);
                }
            });
        } else {
            holder.adminOptions.setVisibility(View.GONE);
        }
    }
    @Override
    public int getItemCount() {
        return events.size();
    }
    public void updateEvents(List<EventModel> newEvents) {
        this.events.clear();
        this.events.addAll(newEvents);
        notifyDataSetChanged();
    }
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, dateTime, location, category;
        LinearLayout adminOptions;
        Button btnEditEvent, btnDeleteEvent;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvEventTitle);
            dateTime = itemView.findViewById(R.id.tvEventDateTime);
            location = itemView.findViewById(R.id.tvEventLocation);
            category = itemView.findViewById(R.id.tvCategoryBadge);
            adminOptions = itemView.findViewById(R.id.admin_options);
            btnEditEvent = itemView.findViewById(R.id.btnEditEvent);
            btnDeleteEvent = itemView.findViewById(R.id.btnDeleteEvent);
        }
    }
}
