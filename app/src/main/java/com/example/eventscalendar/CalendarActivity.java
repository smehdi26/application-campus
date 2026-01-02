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

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateText;
    private FloatingActionButton btnAddEvent;
    private Button btnListView;

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;

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

        // 2. Initialisation des statistiques (Exams, Events, Defense, Clubs)
        setupStatistics();

        // 3. Initialisation de la Barre de Navigation (Navbar)
        setupNavbar();

        // 4. Configuration de la liste (RecyclerView)
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);

        // 5. Action du bouton rouge "+"
        btnAddEvent.setOnClickListener(v -> showAddEventPopup());

        // 6. Clic sur le calendrier
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            String formattedDate_ = sdf.format(calendar.getTime());
            selectedDateText.setText(getString(R.string.events_on, formattedDate_));

            String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
            filterEventsByDate(dateStr);
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

    private void setupStatistics() {
        View statExams = findViewById(R.id.statExams);
        View statEvents = findViewById(R.id.statEvents);
        View statDefense = findViewById(R.id.statDefense);
        View statClubs = findViewById(R.id.statClubs);

        // Exams
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText("Exams");

        // Events
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText("Events");

        // Defense
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statDefense.findViewById(R.id.tvStatLabel)).setText("Defense");

        // Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText("Clubs");
    }

    private void filterEventsByDate(String date) {
        Toast.makeText(this, "Loading events for " + date, Toast.LENGTH_SHORT).show();
    }

    private void showAddEventPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Personal Event");
        builder.setMessage("Add your personal events and sync with Google Calendar.");
        builder.setPositiveButton("Add Event", (dialog, which) -> {
            Toast.makeText(this, "Event Syncing...", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"));
    }
}
