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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.util.Log; // Import Log

public class EventsListActivity extends AppCompatActivity implements EventAdapter.OnEventRegisterClickListener { // Added interface
    private RecyclerView rvAllEvents;
    private FloatingActionButton btnAddEvent;
    private GroupedEventAdapter eventAdapter;
    private List<Object> allItems;
    private DatabaseReference mDatabase;
    private DatabaseReference mUsersDatabase; // New for interestedEvents
    private FirebaseAuth mAuth;
    private Spinner monthSpinner;
    private String selectedMonth = "All Events";
    private static final int ADD_EVENT_REQUEST_CODE = 1;
    public static final int EDIT_EVENT_REQUEST_CODE = 2;
    private static final int REQUEST_CODE_SIGN_IN = 100; // These are from GoogleCalendarHelper
    private static final int REQUEST_AUTHORIZATION = 101; // These are from GoogleCalendarHelper
    private GoogleCalendarHelper googleCalendarHelper;
    private static final String TAG = "EventsListActivity"; // Tag for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("events");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference("Users"); // Initialized
        rvAllEvents = findViewById(R.id.rvAllEvents);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        monthSpinner = findViewById(R.id.monthSpinner);
        setupNavbar();
        setupEventsList();
        checkUserRole();
        btnAddEvent.setOnClickListener(v -> {
            Log.d(TAG, "Add Event button clicked.");
            Intent intent = new Intent(this, AddEventActivity.class);
            startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);
        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Month selected: " + selectedMonth);
                filterEvents(selectedMonth);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMonth = "All Events";
                Log.d(TAG, "Nothing selected, filtering for all events.");
                filterEvents(selectedMonth);
            }
        });
    }

    @Override
    public void onRegisterClick(EventModel event) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // 1. Save to Firebase interestedEvents
            mUsersDatabase.child(uid).child("interestedEvents").child(event.getId()).setValue(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event '" + event.getTitle() + "' registered!", Toast.LENGTH_SHORT).show();
                        // 2. Add to Google Calendar
                        googleCalendarHelper = new GoogleCalendarHelper(this, event);
                        googleCalendarHelper.signInAndAddEvent();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to register event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(this, "You need to be logged in to register for events.", Toast.LENGTH_SHORT).show();
        }
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
                            Log.d(TAG, "User is Admin. Add Event button visible.");
                        } else {
                            btnAddEvent.setVisibility(View.GONE);
                            Log.d(TAG, "User is not Admin. Add Event button gone.");
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    btnAddEvent.setVisibility(View.GONE);
                    Log.e(TAG, "Firebase fetch for user role cancelled: " + error.getMessage());
                }
            });
        } else {
            btnAddEvent.setVisibility(View.GONE);
            Log.d(TAG, "No current Firebase user. Add Event button gone.");
        }
    }
    private void setupEventsList() {
        allItems = new ArrayList<>();
        eventAdapter = new GroupedEventAdapter(new ArrayList<>(), this);
        rvAllEvents.setLayoutManager(new LinearLayoutManager(this));
        rvAllEvents.setAdapter(eventAdapter);
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<EventModel> eventList = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    EventModel event = eventSnapshot.getValue(EventModel.class);
                    if (event != null) {
                        eventList.add(event);
                    }
                }
                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                        Date date1 = sdf.parse(e1.getDate());
                        Date date2 = sdf.parse(e2.getDate());
                        return date1.compareTo(date2);
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date for event sorting: " + e.getMessage());
                        return 0;
                    }
                });
                Map<String, List<EventModel>> groupedEvents = new LinkedHashMap<>();
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.FRENCH);
                for (EventModel event : eventList) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                        Date date = sdf.parse(event.getDate());
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        String month = monthFormat.format(cal.getTime());
                        if (!groupedEvents.containsKey(month)) {
                            groupedEvents.put(month, new ArrayList<>());
                        }
                        groupedEvents.get(month).add(event);
                    } catch (ParseException e) {
                        Log.e(TAG, "Error grouping events by month: " + e.getMessage());
                    }
                }
                allItems.clear();
                for (Map.Entry<String, List<EventModel>> entry : groupedEvents.entrySet()) {
                    allItems.add(entry.getKey());
                    allItems.addAll(entry.getValue());
                }
                List<String> months = new ArrayList<>();
                months.add("All Events");
                months.addAll(groupedEvents.keySet());
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(EventsListActivity.this, android.R.layout.simple_spinner_item, months);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                monthSpinner.setAdapter(spinnerAdapter);

                int selectionIndex = months.indexOf(selectedMonth);
                if (selectionIndex != -1) {
                    monthSpinner.setSelection(selectionIndex);
                }

                filterEvents(selectedMonth);
                updateStatistics(eventList);
                Log.d(TAG, "Fetched " + eventList.size() + " events successfully.");
                Toast.makeText(EventsListActivity.this, "Fetched " + eventList.size() + " events", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching events from Firebase: " + error.getMessage());
                Toast.makeText(EventsListActivity.this, "Error fetching events: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void filterEvents(String selectedMonth) {
        this.selectedMonth = selectedMonth;
        if (selectedMonth.equals("All Events")) {
            eventAdapter.updateItems(allItems);
            Log.d(TAG, "Filtering events: Showing all events.");
            return;
        }
        List<Object> filteredItems = new ArrayList<>();
        boolean monthFound = false;
        for (Object item : allItems) {
            if (item instanceof String && item.equals(selectedMonth)) {
                monthFound = true;
                filteredItems.add(item);
                Log.d(TAG, "Filtering events: Found month header " + selectedMonth);
            } else if (monthFound && item instanceof EventModel) {
                filteredItems.add(item);
            } else if (monthFound && item instanceof String) {
                // Next month header found, so stop
                Log.d(TAG, "Filtering events: Next month header found, stopping.");
                break;
            }
        }
        eventAdapter.updateItems(filteredItems);
        Log.d(TAG, "Filtering events: Displaying " + filteredItems.size() + " filtered items for " + selectedMonth);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);

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
                            Log.d(TAG, "New event added to Firebase: " + newEvent.getTitle());
                            Toast.makeText(this, "Event Added", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding new event to Firebase: " + e.getMessage());
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } else if (requestCode == EDIT_EVENT_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "Event edit result received. Adapter will refresh.");
            // Event was edited, the ValueEventListener will handle the refresh.
        } else if (requestCode == REQUEST_CODE_SIGN_IN) { // REQUEST_CODE_SIGN_IN
            Log.d(TAG, "REQUEST_CODE_SIGN_IN result.");
            if (resultCode == RESULT_OK) {
                if (googleCalendarHelper != null) {
                    googleCalendarHelper.handleSignInResult(data);
                    Log.d(TAG, "Google Sign-In successful, calling handleSignInResult.");
                } else {
                    Log.e(TAG, "googleCalendarHelper is null after successful sign-in.");
                }
            } else {
                Log.d(TAG, "Google Sign-In failed.");
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_AUTHORIZATION) {
            Log.d(TAG, "REQUEST_AUTHORIZATION result.");
            if(resultCode == RESULT_OK) {
                if (googleCalendarHelper != null) {
                    googleCalendarHelper.signInAndAddEvent();
                    Log.d(TAG, "Authorization successful, retrying signInAndAddEvent.");
                } else {
                    Log.e(TAG, "googleCalendarHelper is null after successful authorization.");
                }
            } else {
                Log.d(TAG, "Authorization failed.");
                Toast.makeText(this, "Authorization failed.", Toast.LENGTH_SHORT).show();
            }
        }
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
        View statConferences = findViewById(R.id.statConferences);
        View statSoutenances = findViewById(R.id.statSoutenances);
        View statClubs = findViewById(R.id.statClubs);
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText(String.valueOf(examsCount));
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_exams));
        ((TextView) statConferences.findViewById(R.id.tvStatCount)).setText(String.valueOf(conferencesCount));
        ((TextView) statConferences.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statConferences.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_conferences));
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setText(String.valueOf(soutenancesCount));
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statSoutenances.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_soutenances));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText(String.valueOf(clubsCount));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_clubs));
    }
    private void setupNavbar() {
        Button btnCalendarView = findViewById(R.id.btnCalendarView);
        if (btnCalendarView != null) {
            btnCalendarView.setOnClickListener(v -> {
                Log.d(TAG, "Calendar View button clicked.");
                finish();
            });
        }
        TextView navEvents = findViewById(R.id.navEvents);
        if (navEvents != null) {
            int redColor = ContextCompat.getColor(this, R.color.esprit_red);
            navEvents.setTextColor(redColor);
            for (android.graphics.drawable.Drawable d : navEvents.getCompoundDrawables()) {
                if (d != null) d.setTint(redColor);
            }
        }
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Log.d(TAG, "Profile button clicked.");
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
        findViewById(R.id.navCourses).setOnClickListener(v -> {
            Log.d(TAG, "Courses button clicked.");
            startActivity(new Intent(this, CoursesActivity.class));
            finish();
        });
    }

}