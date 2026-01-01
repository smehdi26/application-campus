package com.example.eventscalendar;

import android.app.AlertDialog;
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
import androidx.annotation.Nullable;
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

    // Pour la gestion de la liste des événements
    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;

    // Pour les statistiques en bas
    private TextView tvExamsCount, tvEventsCount, tvDefenseCount, tvClubsCount;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialisation des vues de base
        calendarView = root.findViewById(R.id.calendarView);
        selectedDateText = root.findViewById(R.id.selectedDateText);
        btnAddEvent = root.findViewById(R.id.btnAddEvent);
        btnListView = root.findViewById(R.id.btnListView);
        rvEvents = root.findViewById(R.id.rvEvents);

        // Initialisation des vues de statistiques (via les includes)
        setupStatistics(root);

        // Configuration de la liste (RecyclerView)
        eventList = new ArrayList<>();
        // Ajout d'une donnée de test pour l'affichage initial (comme sur l'image)
        eventList.add(new EventModel("Project Defense", "09:00", "Room B-204", "Defense", "19/11/2025"));

        eventAdapter = new EventAdapter(eventList);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setAdapter(eventAdapter);

        // 1. Action du bouton rouge "+"
        btnAddEvent.setOnClickListener(v -> showAddEventPopup());

        // 2. Action du bouton "List View"
        btnListView.setOnClickListener(v -> {
            // Cache le calendrier pour ne montrer que la liste
            if (calendarView.getVisibility() == View.VISIBLE) {
                calendarView.setVisibility(View.GONE);
                btnListView.setText("Calendar View");
            } else {
                calendarView.setVisibility(View.VISIBLE);
                btnListView.setText("List View");
            }
        });

        // 3. Clic sur un jour du calendrier (Interactivité)
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
            selectedDateText.setText("Events on " + dayOfMonth + " November " + year);

            // Logique : Filtrer la liste ici
            filterEventsByDate(dateStr);
        });

        return root;
    }

    private void setupStatistics(View root) {
        // Accès aux IDs à l'intérieur des <include>
        View statExams = root.findViewById(R.id.statExams);
        View statEvents = root.findViewById(R.id.statEvents);
        View statDefense = root.findViewById(R.id.statDefense);
        View statClubs = root.findViewById(R.id.statClubs);

        // Configuration Exams (Rouge)
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText("2");
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText("Exams");

        // Configuration Events (Bleu)
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText("Events");

        // Configuration Defense (Orange)
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statDefense.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statDefense.findViewById(R.id.tvStatLabel)).setText("Defense");

        // Configuration Clubs (Vert)
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText("Clubs");
    }

    private void filterEventsByDate(String date) {
        // Dans un vrai projet, vous feriez une requête Firebase ou SQLite ici
        // Pour l'exemple, on simule une mise à jour de la liste
        Toast.makeText(getContext(), "Loading events for " + date, Toast.LENGTH_SHORT).show();
        // eventAdapter.updateList(newList);
    }

    private void showAddEventPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Personal Event");
        builder.setMessage("Add your personal events and sync with Google Calendar.");

        builder.setPositiveButton("Add Event", (dialog, which) -> {
            Toast.makeText(getContext(), "Event Syncing...", Toast.LENGTH_SHORT).show();
            // Logique d'ajout à Google Calendar ici
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Style du bouton comme sur l'image
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#E91E63"));
    }
}
