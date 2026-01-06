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
import android.util.Log; // Import Log

public class GroupedEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Object> items;
    private EventAdapter.OnEventRegisterClickListener listener; // New member
    private static final int VIEW_TYPE_MONTH = 0;
    private static final int VIEW_TYPE_EVENT = 1;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mEventsDatabase;
    private static final String TAG = "GroupedEventAdapter"; // Tag for logging

    public GroupedEventAdapter(List<Object> items, EventAdapter.OnEventRegisterClickListener listener) { // Modified constructor
        this.items = items;
        this.listener = listener; // Initialize listener
        this.mAuth = FirebaseAuth.getInstance();
        this.mUsersDatabase = FirebaseDatabase.getInstance().getReference("Users");
        this.mEventsDatabase = FirebaseDatabase.getInstance().getReference("events");
    }
    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return VIEW_TYPE_MONTH;
        } else {
            return VIEW_TYPE_EVENT;
        }
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MONTH) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_header, parent, false);
            return new MonthViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_MONTH) {
            MonthViewHolder monthViewHolder = (MonthViewHolder) holder;
            monthViewHolder.month.setText((String) items.get(position));
        } else {
            EventViewHolder eventViewHolder = (EventViewHolder) holder;
            EventModel event = (EventModel) items.get(position);
            eventViewHolder.title.setText(event.getTitle());
            eventViewHolder.dateTime.setText(event.getDate() + " " + event.getTime());
            eventViewHolder.location.setText(event.getLocation());
            eventViewHolder.category.setText(event.getCategory());
            String category = event.getCategory();
            if (category.equals("Soutenances")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_soutenances);
                eventViewHolder.category.setTextColor(Color.parseColor("#FFA000"));
            } else if (category.equals("Exams") || category.equals("Examens")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_exams);
                eventViewHolder.category.setTextColor(Color.parseColor("#D32F2F"));
            } else if (category.equals("Clubs")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_clubs);
                eventViewHolder.category.setTextColor(Color.parseColor("#00796B"));
            } else if (category.equals("Conferences") || category.equals("Conférences")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_conferences);
                eventViewHolder.category.setTextColor(Color.parseColor("#1976D2"));
            } else {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_badge_gray);
                eventViewHolder.category.setTextColor(Color.GRAY);
            }
            checkUserRole(eventViewHolder, event);
            eventViewHolder.btnDeleteEvent.setOnClickListener(v -> {
                Log.d(TAG, "Delete event clicked for: " + event.getTitle());
                mEventsDatabase.child(event.getId()).removeValue();
                Toast.makeText(v.getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
            });
            eventViewHolder.btnEditEvent.setOnClickListener(v -> {
                Log.d(TAG, "Edit event clicked for: " + event.getTitle());
                Context context = v.getContext();
                Intent intent = new Intent(context, EditEventActivity.class);
                intent.putExtra("eventId", event.getId());
                intent.putExtra("eventTitle", event.getTitle());
                intent.putExtra("eventCategory", event.getCategory());
                intent.putExtra("eventDate", event.getDate());
                intent.putExtra("eventTime", event.getTime());
                intent.putExtra("eventLocation", event.getLocation());
                intent.putExtra("eventDescription", event.getDescription());
                if (context instanceof EventsListActivity) {
                    ((EventsListActivity) context).startActivityForResult(intent, EventsListActivity.EDIT_EVENT_REQUEST_CODE);
                }
            });
        }
    }
    private void checkUserRole(EventViewHolder holder, EventModel event) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            mUsersDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            if ("Admin".equalsIgnoreCase(user.role)) {
                                holder.adminOptions.setVisibility(View.VISIBLE);
                                holder.btnRegisterEvent.setVisibility(View.GONE);
                                Log.d(TAG, "User is Admin. Admin options visible, Register button gone.");
                            } else {
                                holder.adminOptions.setVisibility(View.GONE);
                                holder.btnRegisterEvent.setVisibility(View.VISIBLE);
                                Log.d(TAG, "User is not Admin. Admin options gone, Register button visible.");
                            }

                            if (user.interestedEvents != null && user.interestedEvents.containsKey(event.getId())) {
                                holder.btnRegisterEvent.setText("Registered");
                                holder.btnRegisterEvent.setEnabled(false);
                                Log.d(TAG, "Event " + event.getTitle() + " already registered by user.");
                            } else {
                                holder.btnRegisterEvent.setText("Register");
                                holder.btnRegisterEvent.setEnabled(true);
                                Log.d(TAG, "Event " + event.getTitle() + " not yet registered by user.");
                            }

                            holder.btnRegisterEvent.setOnClickListener(v -> {
                                Log.d(TAG, "Register button clicked for event: " + event.getTitle());
                                if (listener != null) {
                                    listener.onRegisterClick(event);
                                }
                            });
                        } else {
                            Log.e(TAG, "User object is null after fetching from Firebase.");
                        }
                    } else {
                        Log.e(TAG, "User snapshot does not exist for UID: " + uid);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.adminOptions.setVisibility(View.GONE);
                    holder.btnRegisterEvent.setVisibility(View.GONE); // Hide register button on error too
                    Log.e(TAG, "Firebase fetch for user cancelled: " + error.getMessage());
                }
            });
        } else {
            holder.adminOptions.setVisibility(View.GONE);
            holder.btnRegisterEvent.setVisibility(View.GONE);
            Log.d(TAG, "No current Firebase user. Hiding admin options and register button.");
        }
    }
    @Override
    public int getItemCount() {
        return items.size();
    }
    public void updateItems(List<Object> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, dateTime, location, category;
        LinearLayout adminOptions;
        Button btnEditEvent, btnDeleteEvent, btnRegisterEvent;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvEventTitle);
            dateTime = itemView.findViewById(R.id.tvEventDateTime);
            location = itemView.findViewById(R.id.tvEventLocation);
            category = itemView.findViewById(R.id.tvCategoryBadge);
            adminOptions = itemView.findViewById(R.id.admin_options);
            btnEditEvent = itemView.findViewById(R.id.btnEditEvent);
            btnDeleteEvent = itemView.findViewById(R.id.btnDeleteEvent);
            btnRegisterEvent = itemView.findViewById(R.id.btnRegisterEvent);
        }
    }
    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView month;
        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            month = itemView.findViewById(R.id.tvMonthHeader);
        }
    }
}