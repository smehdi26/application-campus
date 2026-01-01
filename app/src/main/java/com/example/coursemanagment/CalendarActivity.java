package com.example.coursemanagment;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.coursemanagment.databinding.ActivityCalendarBinding;
import com.example.coursemanagment.databinding.DialogAddEventBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {

    private ActivityCalendarBinding binding;
    private List<Event> allEvents = new ArrayList<>();
    private EventAdapter adapter;
    private boolean isListView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMockData();
        setupCalendar();
        setupRecyclerView();
        updateCounters();

        binding.btnAdd.setOnClickListener(v -> showAddEventDialog());
        binding.btnListView.setOnClickListener(v -> toggleView());

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateSelectedEventDetails(year, month, dayOfMonth);
        });
    }

    private void setupMockData() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.NOVEMBER, 15);
        allEvents.add(new Event("Android Exam", "10:00 AM", "Room 302", cal.getTimeInMillis(), "Exams"));
        
        cal.set(2025, Calendar.NOVEMBER, 20);
        allEvents.add(new Event("Hackathon 2025", "09:00 AM", "Main Hall", cal.getTimeInMillis(), "Events"));
        
        cal.set(2025, Calendar.NOVEMBER, 25);
        allEvents.add(new Event("PFE Defense", "14:00 PM", "Lab A", cal.getTimeInMillis(), "Defense"));
        
        cal.set(2025, Calendar.NOVEMBER, 10);
        allEvents.add(new Event("Google Club Meeting", "17:00 PM", "Room 101", cal.getTimeInMillis(), "Clubs"));
    }

    private void setupCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.NOVEMBER, 1);
        binding.calendarView.setDate(cal.getTimeInMillis());
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(allEvents);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEvents.setAdapter(adapter);
    }

    private void toggleView() {
        isListView = !isListView;
        if (isListView) {
            binding.calendarView.setVisibility(View.GONE);
            binding.cardSelectedEvent.setVisibility(View.GONE);
            binding.rvEvents.setVisibility(View.VISIBLE);
            binding.btnListView.setText("Calendar View");
        } else {
            binding.calendarView.setVisibility(View.VISIBLE);
            binding.cardSelectedEvent.setVisibility(View.VISIBLE);
            binding.rvEvents.setVisibility(View.GONE);
            binding.btnListView.setText("List View");
        }
    }

    private void updateSelectedEventDetails(int year, int month, int dayOfMonth) {
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.set(year, month, dayOfMonth, 0, 0, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        Event foundEvent = null;
        for (Event event : allEvents) {
            Calendar eventCal = Calendar.getInstance();
            eventCal.setTimeInMillis(event.getDateInMillis());
            if (eventCal.get(Calendar.YEAR) == year &&
                eventCal.get(Calendar.MONTH) == month &&
                eventCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth) {
                foundEvent = event;
                break;
            }
        }

        if (foundEvent != null) {
            binding.tvDetailTitle.setText(foundEvent.getTitle());
            binding.tvDetailTimeLocation.setText(foundEvent.getTime() + " | " + foundEvent.getLocation());
        } else {
            binding.tvDetailTitle.setText("No event selected");
            binding.tvDetailTimeLocation.setText("Tap on a date with an event to see details.");
        }
    }

    private void updateCounters() {
        int exams = 0, events = 0, defense = 0, clubs = 0;
        for (Event e : allEvents) {
            switch (e.getCategory()) {
                case "Exams": exams++; break;
                case "Events": events++; break;
                case "Defense": defense++; break;
                case "Clubs": clubs++; break;
            }
        }
        binding.tvExamsCount.setText("Exams: " + exams);
        binding.tvEventsCount.setText("Events: " + events);
        binding.tvDefenseCount.setText("Defense: " + defense);
        binding.tvClubsCount.setText("Clubs: " + clubs);
    }

    private void showAddEventDialog() {
        DialogAddEventBinding dialogBinding = DialogAddEventBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setTitle("Add Personal Event")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Add Event", (dialog, which) -> {
                    String title = dialogBinding.etEventTitle.getText().toString();
                    if (!title.isEmpty()) {
                        Toast.makeText(this, "Event added: " + title, Toast.LENGTH_SHORT).show();
                        // Logic to add to list could go here
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
