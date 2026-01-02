package com.example.eventscalendar;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateText;
    private FloatingActionButton btnAddEvent;
    private Button btnListView;

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;

    private static final int ADD_EVENT_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_calendar);

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
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
        
        // 3. Initialisation et mise à jour des statistiques
        updateStatistics(eventList);

        // 4. Initialisation de la Barre de Navigation (Navbar)
        setupNavbar();

        // 5. Action du bouton rouge "+"
        btnAddEvent.setOnClickListener(v -> showAddEventPopup());

        // 6. Clic sur le calendrier
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            String formattedDate_ = sdf.format(calendar.getTime());
            selectedDateText.setText(getString(R.string.events_on, formattedDate_));

            String dateStr = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            filterEventsByDate(dateStr);
        });
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

            EventModel newEvent = new EventModel(title, time, location, category, date);
            eventList.add(newEvent);
            eventAdapter.notifyDataSetChanged(); // Update the RecyclerView
            updateStatistics(eventList); // Update statistics after adding a new event
            Toast.makeText(this, "Event Added: " + title, Toast.LENGTH_SHORT).show();
            // TODO: In a real app, you might want to save this to a database
        }
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
            } else if (category.equals("Soutenances") || category.equals("Defense")) {
                soutenancesCount++;
            } else if (category.equals("Clubs")) {
                clubsCount++;
            }
        }

        View statExams = findViewById(R.id.statExams);
        View statEvents = findViewById(R.id.statEvents); // This is actually for Conferences
        View statDefense = findViewById(R.id.statDefense); // This is actually for Soutenances
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
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setText(String.valueOf(soutenancesCount));
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statDefense.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_soutenances));

        // Update Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText(String.valueOf(clubsCount));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_clubs));
    }

    private void filterEventsByDate(String date) {
        List<EventModel> filteredEvents = new ArrayList<>();
        for (EventModel event : eventList) {
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
