// C:/Users/lenovo/AndroidStudioProjects/application-campus/app/src/main/java/com/example/eventscalendar/EventsListActivity.java

package com.example.eventscalendar;



import android.content.Intent;



import android.graphics.Color;



import android.os.Bundle;



import android.view.View;



import android.widget.Button;



import android.widget.TextView;



import android.widget.Toast;



import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;



import com.example.coursemanagment.CoursesActivity;

import com.example.coursemanagment.ProfileActivity;

import com.example.coursemanagment.R;

import com.google.android.material.floatingactionbutton.FloatingActionButton;



import java.util.ArrayList;

import java.util.List;

import java.util.Locale;



public class EventsListActivity extends AppCompatActivity {



    private RecyclerView rvAllEvents;

    private FloatingActionButton btnAddEvent;

    private EventAdapter eventAdapter;

    private List<EventModel> eventList;



    private static final int ADD_EVENT_REQUEST_CODE = 1;



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_events_list);



        // Initialisation des vues

        rvAllEvents = findViewById(R.id.rvAllEvents);

        btnAddEvent = findViewById(R.id.btnAddEvent);



        setupNavbar();

        setupEventsList(); // This now initializes eventList and adapter



        btnAddEvent.setOnClickListener(v -> {

            Intent intent = new Intent(this, AddEventActivity.class);

            startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);

        });



        updateStatistics(eventList); // Initial update of statistics

    }



    private void setupEventsList() {

        eventList = new ArrayList<>();

        // Initialiser la liste avec des événements existants si nécessaire

        eventList.add(new EventModel("Project Defense", "09:00", "B-204", "Soutenances", "02/01/2026"));

        eventList.add(new EventModel("AI Lecture", "10:00", "Amphi A", "Conferences", "02/01/2026"));

        eventList.add(new EventModel("Club Meeting", "14:00", "Club Room", "Clubs", "03/01/2026"));

        eventList.add(new EventModel("Math Exam", "08:00", "Hall C", "Exams", "04/01/2026"));



        eventAdapter = new EventAdapter(eventList);

        rvAllEvents.setLayoutManager(new LinearLayoutManager(this));

        rvAllEvents.setAdapter(eventAdapter);

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

            eventAdapter.notifyDataSetChanged();

            updateStatistics(eventList); // Update statistics after adding a new event

            Toast.makeText(this, "Event Added: " + title, Toast.LENGTH_SHORT).show();

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



    private void setupNavbar() {

        // Bouton pour revenir au calendrier

        Button btnCalendarView = findViewById(R.id.btnCalendarView);

        if (btnCalendarView != null) {

            btnCalendarView.setOnClickListener(v -> finish()); // Retourne simplement à l'activité précédente (Calendar)

        }



        // Colorer l'icône Events en rouge

        TextView navEvents = findViewById(R.id.navEvents);

        if (navEvents != null) {

            int redColor = ContextCompat.getColor(this, R.color.esprit_red);

            navEvents.setTextColor(redColor);

            for (android.graphics.drawable.Drawable d : navEvents.getCompoundDrawables()) {

                if (d != null) d.setTint(redColor);

            }

        }



        // Navigation

        findViewById(R.id.navProfile).setOnClickListener(v -> {

            startActivity(new Intent(this, ProfileActivity.class));

            finish();

        });



        findViewById(R.id.navCourses).setOnClickListener(v -> {

            startActivity(new Intent(this, CoursesActivity.class));

            finish();

        });

    }

}
