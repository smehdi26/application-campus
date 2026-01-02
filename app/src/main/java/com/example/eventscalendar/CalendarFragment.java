package com.example.eventscalendar;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.coursemanagment.R;

import java.util.ArrayList;
import java.util.List;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView selectedDateText;
    private FloatingActionButton btnAddEvent;
    private Button btnListView;

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        // 1. Initialisation des vues de base
        calendarView = root.findViewById(R.id.calendarView);
        selectedDateText = root.findViewById(R.id.selectedDateText);
        btnAddEvent = root.findViewById(R.id.btnAddEvent);
        btnListView = root.findViewById(R.id.btnListView);
        rvEvents = root.findViewById(R.id.rvEvents);

        // 2. Initialisation des statistiques (Exams, Events, Defense, Clubs)
        setupStatistics(root);

        // 3. Initialisation de la Barre de Navigation (Navbar)
        setupNavbar(root);

        // 4. Configuration de la liste (RecyclerView)
        eventList = new ArrayList<>();
        eventList.add(new EventModel("Project Defense", "09:00", "Room B-204", "Defense", "19/11/2025"));
        eventAdapter = new EventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setAdapter(eventAdapter);

        // 5. Action du bouton rouge "+"
        btnAddEvent.setOnClickListener(v -> showAddEventPopup());

        // 6. Action du bouton "List View"
        btnListView.setOnClickListener(v -> {
            if (calendarView.getVisibility() == View.VISIBLE) {
                calendarView.setVisibility(View.GONE);
                btnListView.setText("Calendar View");
            } else {
                calendarView.setVisibility(View.VISIBLE);
                btnListView.setText("List View");
            }
        });

        // 7. Clic sur le calendrier
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
            selectedDateText.setText("Events on " + dayOfMonth + " November " + year);
            filterEventsByDate(dateStr);
        });

        return root;
    }

    private void setupNavbar(View root) {
        // Récupération de la navbar incluse dans le layout
        View navbar = root.findViewById(R.id.navbar);
        if (navbar == null) return;

        // Mise en évidence de l'onglet "Events" (Couleur Esprit Red)
        TextView navEvents = navbar.findViewById(R.id.navEvents);
        int activeColor = Color.parseColor("#E91E63"); // Rouge Esprit
        navEvents.setTextColor(activeColor);
        navEvents.setCompoundDrawableTintList(ColorStateList.valueOf(activeColor));

        // Navigation vers Courses (Supposons que c'est une Activity ou un Fragment)
        navbar.findViewById(R.id.navCourses).setOnClickListener(v -> {
            // Si vous utilisez des fragments dans MainActivity :
            // getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new CoursesFragment()).commit();
            Toast.makeText(getContext(), "Navigating to Courses...", Toast.LENGTH_SHORT).show();
        });

        // Navigation vers Profile
        navbar.findViewById(R.id.navProfile).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Navigating to Profile...", Toast.LENGTH_SHORT).show();
        });

        // Ajoutez les autres clics (navCovoiturage, navMap, navForums) ici
    }

    private void setupStatistics(View root) {
        View statExams = root.findViewById(R.id.statExams);
        View statEvents = root.findViewById(R.id.statEvents);
        View statDefense = root.findViewById(R.id.statDefense);
        View statClubs = root.findViewById(R.id.statClubs);

        // Exams
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText("2");
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText("Exams");

        // Events
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText("Events");

        // Defense
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statDefense.findViewById(R.id.tvStatLabel)).setText("Defense");

        // Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText("Clubs");
    }

    private void filterEventsByDate(String date) {
        Toast.makeText(getContext(), "Loading events for " + date, Toast.LENGTH_SHORT).show();
    }

    private void showAddEventPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Personal Event");
        builder.setMessage("Add your personal events and sync with Google Calendar.");
        builder.setPositiveButton("Add Event", (dialog, which) -> {
            Toast.makeText(getContext(), "Event Syncing...", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"));
    }
}
