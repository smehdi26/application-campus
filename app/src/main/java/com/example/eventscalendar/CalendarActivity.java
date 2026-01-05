package com.example.eventscalendar;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.example.coursemanagment.CoursesActivity;
import com.example.coursemanagment.ProfileActivity;
import com.example.coursemanagment.R;
import com.example.coursemanagment.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class CalendarActivity extends AppCompatActivity {
    private com.applandeo.materialcalendarview.CalendarView calendarView;
    private TextView selectedDateText;
    private FloatingActionButton btnAddEvent;
    private Button btnListView;
    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;
    private List<EventModel> allEventsList;
    private DatabaseReference mDatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseAuth mAuth;
    private static final int ADD_EVENT_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_calendar);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("events");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference("Users");
        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnListView = findViewById(R.id.btnListView);
        rvEvents = findViewById(R.id.rvEvents);
        SimpleDateFormat sdf = new SimpleDateFormat("dd LLLL yyyy", Locale.FRENCH);
        String formattedDate = sdf.format(new Date());
        selectedDateText.setText(getString(R.string.events_on, formattedDate));
        eventList = new ArrayList<>();
        allEventsList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
        updateStatistics(new ArrayList<>());
        loadEvents();
        setupNavbar();
        checkUserRole();
        btnAddEvent.setOnClickListener(v -> showAddEventPopup());
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar calendar = eventDay.getCalendar();
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd LLLL yyyy", Locale.FRENCH);
            selectedDateText.setText(getString(R.string.events_on, displayFormat.format(calendar.getTime())));
            SimpleDateFormat filterFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            String dateStr = filterFormat.format(calendar.getTime());
            filterEventsByDate(dateStr);
        });
    }
    private void checkUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            mUsersDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && "Admin".equalsIgnoreCase(user.role)) {
                            btnAddEvent.setVisibility(View.VISIBLE);
                        } else {
                            btnAddEvent.setVisibility(View.GONE);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    btnAddEvent.setVisibility(View.GONE);
                }
            });
        } else {
            btnAddEvent.setVisibility(View.GONE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_EVENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String title = data.getStringExtra("event_title");
            String category = data.getStringExtra("event_category");
            String date = data.getStringExtra("event_date");
            String time = data.getStringExtra("event_time");
            String location = data.getStringExtra("event_location");
            String description = data.getStringExtra("event_description");
            EventModel newEvent = new EventModel(title, time, location, category, date, description);
            String eventId = mDatabase.push().getKey();
            if (eventId != null) {
                newEvent.setId(eventId);
                mDatabase.child(eventId).setValue(newEvent)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Event Added: " + title, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        }
    }
    private void loadEvents() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEventsList.clear();
                Map<String, Map<String, Integer>> dailyEventCategoryCounts = new HashMap<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    EventModel event = eventSnapshot.getValue(EventModel.class);
                    if (event != null) {
                        allEventsList.add(event);
                        String eventDate = event.getDate();
                        String eventCategory = event.getCategory();
                        dailyEventCategoryCounts
                                .computeIfAbsent(eventDate, k -> new HashMap<>())
                                .merge(eventCategory, 1, Integer::sum);
                    }
                }
                updateStatistics(allEventsList);
                List<EventDay> eventsWithColoredDots = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                for (Map.Entry<String, Map<String, Integer>> entry : dailyEventCategoryCounts.entrySet()) {
                    String dateStr = entry.getKey();
                    Map<String, Integer> categoryCounts = entry.getValue();
                    String dominantCategory = null;
                    int maxCount = 0;
                    for (Map.Entry<String, Integer> categoryEntry : categoryCounts.entrySet()) {
                        if (categoryEntry.getValue() > maxCount) {
                            maxCount = categoryEntry.getValue();
                            dominantCategory = categoryEntry.getKey();
                        }
                    }
                    int drawableRes = R.drawable.bg_dot_exams;
                    if (dominantCategory != null) {
                        if (dominantCategory.equals("Exams") || dominantCategory.equals("Examens")) {
                            drawableRes = R.drawable.bg_dot_exams;
                        } else if (dominantCategory.equals("Conferences") || dominantCategory.equals("Conférences")) {
                            drawableRes = R.drawable.bg_dot_conference;
                        } else if (dominantCategory.equals("Soutenances")) {
                            drawableRes = R.drawable.bg_dot_soutenances;
                        } else if (dominantCategory.equals("Clubs")) {
                            drawableRes = R.drawable.bg_dot_clubs;
                        } else {
                            drawableRes = R.drawable.bg_badge_gray;
                        }
                    }
                    try {
                        Date date = sdf.parse(dateStr);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date);
                        eventsWithColoredDots.add(new EventDay(calendar, drawableRes));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                calendarView.setEvents(eventsWithColoredDots);
                Calendar today = Calendar.getInstance();
                String formattedTodayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(today.getTime());
                filterEventsByDate(formattedTodayDate);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CalendarActivity.this, "Failed to load events: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void setupNavbar() {
        Button btnListView = findViewById(R.id.btnListView);
        btnListView.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventsListActivity.class);
            startActivity(intent);
        });
        TextView navEvents = findViewById(R.id.navEvents);
        if (navEvents != null) {
            int redColor = ContextCompat.getColor(this, R.color.esprit_red);
            navEvents.setTextColor(redColor);
            for (android.graphics.drawable.Drawable d : navEvents.getCompoundDrawables()) {
                if (d != null) d.setTint(redColor);
            }
        }
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navCourses).setOnClickListener(v -> {
            startActivity(new Intent(this, CoursesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
    private void updateStatistics(List<EventModel> eventsToCount) {
        int examsCount = 0;
        int conferencesCount = 0;
        int soutenancesCount = 0;
        int clubsCount = 0;
        for (EventModel event : eventsToCount) {
            String category = event.getCategory();
            if (category.equals("Exams") || category.equals("Examens")) {
                examsCount++;
            } else if (category.equals("Conferences") || category.equals("Conférences")) {
                conferencesCount++;
            } else if (category.equals("Soutenances")) {
                soutenancesCount++;
            } else if (category.equals("Clubs")) {
                clubsCount++;
            }
        }
        View statExams = findViewById(R.id.statExams);
        View statEvents = findViewById(R.id.statEvents);
        View statSoutenances = findViewById(R.id.statSoutenances);
        View statClubs = findViewById(R.id.statClubs);
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText(String.valueOf(examsCount));
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_exams));
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText(String.valueOf(conferencesCount));
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_conferences));
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setText(String.valueOf(soutenancesCount));
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statSoutenances.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_soutenances));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText(String.valueOf(clubsCount));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_clubs));
    }
    private void filterEventsByDate(String date) {
        List<EventModel> filteredEvents = new ArrayList<>();
        for (EventModel event : allEventsList) {
            if (event.getDate().equals(date)) {
                filteredEvents.add(event);
            }
        }
        eventAdapter.updateEvents(filteredEvents);
        updateStatistics(filteredEvents);
    }
    private void showAddEventPopup() {
        Intent intent = new Intent(this, AddEventActivity.class);
        startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);
    }
}
