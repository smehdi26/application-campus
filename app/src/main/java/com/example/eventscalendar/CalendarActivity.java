package com.example.eventscalendar;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursemanagment.CoursesActivity;
import com.example.coursemanagment.ProfileActivity;
import com.example.coursemanagment.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;

public class CalendarActivity extends AppCompatActivity {

    private com.applandeo.materialcalendarview.CalendarView calendarView;
    private TextView selectedDateText;
    private FloatingActionButton btnAddEvent;
    private Button btnListView;

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList; // This list will hold the currently displayed (filtered) events
    private List<EventModel> allEventsList; // This list will hold all events fetched from the database

    private DatabaseReference mDatabase; // Declare DatabaseReference

    private static final int ADD_EVENT_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_calendar);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference("events"); // 'events' is the root node for your events

        // 1. Initialisation des vues de base
        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnListView = findViewById(R.id.btnListView);
        rvEvents = findViewById(R.id.rvEvents);

        // Set initial date to today
        SimpleDateFormat sdf = new SimpleDateFormat("dd LLLL yyyy", Locale.FRENCH);
        String formattedDate = sdf.format(new Date());
        selectedDateText.setText(getString(R.string.events_on, formattedDate));

        // 2. Configuration de la liste (RecyclerView)
        eventList = new ArrayList<>(); // Initialize the list for the adapter
        allEventsList = new ArrayList<>(); // Initialize the list to hold all events
        eventAdapter = new EventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
        
        // 3. Initialisation et mise à jour des statistiques (initial call with empty list)
        updateStatistics(new ArrayList<>()); // Pass an empty list initially

        // Load events from Realtime Database
        loadEvents();

        // 4. Initialisation de la Barre de Navigation (Navbar)
        setupNavbar();

        // 5. Action du bouton rouge "+"
        btnAddEvent.setOnClickListener(v -> showAddEventPopup());

        // 6. Clic sur le calendrier
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar calendar = eventDay.getCalendar(); // Get calendar from EventDay
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd LLLL yyyy", Locale.FRENCH); // For display
            selectedDateText.setText(getString(R.string.events_on, displayFormat.format(calendar.getTime())));

            SimpleDateFormat filterFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US); // For filtering
            String dateStr = filterFormat.format(calendar.getTime());
            filterEventsByDate(dateStr);
        });    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_EVENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String title = data.getStringExtra("event_title");
            String category = data.getStringExtra("event_category");
            String date = data.getStringExtra("event_date");
            String time = data.getStringExtra("event_time");
            String location = data.getStringExtra("event_location");
            String description = data.getStringExtra("event_description"); // Retrieve description

            EventModel newEvent = new EventModel(title, time, location, category, date, description); // Pass description to constructor

            // Save event to Realtime Database
            String eventId = mDatabase.push().getKey(); // Generate a unique ID
            if (eventId != null) {
                newEvent.setId(eventId);
                mDatabase.child(eventId).setValue(newEvent)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Event Added: " + title, Toast.LENGTH_SHORT).show();
                            // loadEvents() will be called by the ValueEventListener implicitly
                            // No need to explicitly call loadEvents() here as the listener will trigger on data change.
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
                allEventsList.clear(); // Clear old data from the master list
                // Map to store event counts per day per category: Date string -> Category -> Count
                Map<String, Map<String, Integer>> dailyEventCategoryCounts = new HashMap<>();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    EventModel event = eventSnapshot.getValue(EventModel.class);
                    if (event != null) {
                        allEventsList.add(event); // Add to master list
                        
                        String eventDate = event.getDate(); // e.g., "dd/MM/yyyy"
                        String eventCategory = event.getCategory();

                        dailyEventCategoryCounts
                                .computeIfAbsent(eventDate, k -> new HashMap<>())
                                .merge(eventCategory, 1, Integer::sum);
                    }
                }
                
                // Update overall statistics
                updateStatistics(allEventsList);

                // --- Highlighting Logic ---
                List<EventDay> eventsWithColoredDots = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

                for (Map.Entry<String, Map<String, Integer>> entry : dailyEventCategoryCounts.entrySet()) {
                    String dateStr = entry.getKey();
                    Map<String, Integer> categoryCounts = entry.getValue();

                    String dominantCategory = null;
                    int maxCount = 0;

                    // Find the dominant category for the day
                    for (Map.Entry<String, Integer> categoryEntry : categoryCounts.entrySet()) {
                        if (categoryEntry.getValue() > maxCount) {
                            maxCount = categoryEntry.getValue();
                            dominantCategory = categoryEntry.getKey();
                        }
                    }

                    // Assign drawable based on dominant category
                    int drawableRes = R.drawable.bg_dot_exams; // Default/fallback (will not be used if categories match)
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
                            drawableRes = R.drawable.bg_badge_gray; // Fallback for unknown categories
                        }
                    }

                    try {
                        Date date = sdf.parse(dateStr);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date);
                        eventsWithColoredDots.add(new EventDay(calendar, drawableRes));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        // Handle parsing error if date format is incorrect
                    }
                }
                calendarView.setEvents(eventsWithColoredDots);
                // --- End Highlighting Logic ---

                // Now, re-filter for today's date to update the displayed list
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
        View statEvents = findViewById(R.id.statEvents); // This is actually for Conferences
        View statSoutenances = findViewById(R.id.statSoutenances); // This is actually for Soutenances
        View statClubs = findViewById(R.id.statClubs);

        // Update Exams
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText(String.valueOf(examsCount));
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_exams));

        // Update Conferences
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText(String.valueOf(conferencesCount));
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_conferences));

        // Update Soutenances
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setText(String.valueOf(soutenancesCount));
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statSoutenances.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_soutenances));

        // Update Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText(String.valueOf(clubsCount));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_clubs));
    }

    private void filterEventsByDate(String date) {
        List<EventModel> filteredEvents = new ArrayList<>();
        for (EventModel event : allEventsList) { // Filter from the master list
            if (event.getDate().equals(date)) { // Assuming event.getDate() returns "dd/MM/yyyy"
                filteredEvents.add(event);
            }
        }
        eventAdapter.updateEvents(filteredEvents);
        updateStatistics(filteredEvents); // Update statistics based on filtered events
    }

    private void showAddEventPopup() {
        Intent intent = new Intent(this, AddEventActivity.class);
        startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);
    }
}
