package com.example.eventscalendar;

import com.applandeo.materialcalendarview.CalendarView; // Corrected import
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Added for onActivityResult
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.coursemanagment.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.eventscalendar.GoogleCalendarHelper; // New import
import com.example.coursemanagment.User;
import com.applandeo.materialcalendarview.EventDay;
import android.app.Activity; // New: For Activity.RESULT_OK
import android.content.Intent; // New: For Intent
import android.content.res.ColorStateList; // New: For ColorStateList
import android.graphics.Color; // New: For Color
import android.os.Bundle; // New: For Bundle
import android.view.LayoutInflater; // New: For LayoutInflater
import android.view.View; // New: For View
import android.view.ViewGroup; // New: For ViewGroup
import android.widget.Button; // New: For Button

import com.applandeo.materialcalendarview.listeners.OnDayClickListener;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;


public class CalendarFragment extends Fragment implements EventAdapter.OnEventRegisterClickListener { // Added interface

    private com.applandeo.materialcalendarview.CalendarView calendarView; // Changed type
    private TextView selectedDateText;
    private FloatingActionButton btnAddEvent;
    private Button btnListView;

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;
    private List<EventModel> allEventsList; // New for Firebase loading

    private FirebaseAuth mAuth; // New
    private DatabaseReference mDatabase; // New (for events)
    private DatabaseReference mUsersDatabase; // New (for users)

    private GoogleCalendarHelper googleCalendarHelper; // New
    private static final int ADD_EVENT_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_SIGN_IN_GCH = GoogleCalendarHelper.REQUEST_CODE_SIGN_IN; // Define request codes for GoogleCalendarHelper
    private static final int REQUEST_AUTHORIZATION_GCH = GoogleCalendarHelper.REQUEST_AUTHORIZATION;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        mAuth = FirebaseAuth.getInstance(); // Init Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("events"); // Init Firebase
        mUsersDatabase = FirebaseDatabase.getInstance().getReference("Users"); // Init Firebase


        // 1. Initialisation des vues de base
        calendarView = root.findViewById(R.id.calendarView);
        selectedDateText = root.findViewById(R.id.selectedDateText);
        btnAddEvent = root.findViewById(R.id.btnAddEvent);
        btnListView = root.findViewById(R.id.btnListView);
        rvEvents = root.findViewById(R.id.rvEvents);

        // Set initial date to today
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH); // Changed to FRENCH for consistency
        String formattedDate = sdf.format(new Date());
        selectedDateText.setText(getString(R.string.events_on, formattedDate));


        // 2. Initialisation des statistiques (Exams, Events, Soutenances, Clubs)
        // setupStatistics(root); // Temporarily comment out as it relies on allEventsList
        setupStatisticsEmpty(root); // New placeholder, will be replaced with actual updateStatistics


        // 3. Initialisation de la Barre de Navigation (Navbar)
        setupNavbar(root);

        // 4. Configuration de la liste (RecyclerView)
        eventList = new ArrayList<>();
        allEventsList = new ArrayList<>(); // Initialize
        eventAdapter = new EventAdapter(eventList, this); // Pass 'this' as listener
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setAdapter(eventAdapter);

        // Load events from Firebase
        loadEvents();

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
        // IMPORTANT: The existing CalendarView.setOnDateChangeListener is for the default Android CalendarView.
        // The layout actually uses com.applandeo.materialcalendarview.CalendarView, which has OnDayClickListener.
        // So, this part needs to be updated. I will replace calendarView.setOnDateChangeListener
        // with calendarView.setOnDayClickListener and adapt the logic.

        // Old: calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> { ... });
        // New:
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar calendar = eventDay.getCalendar();
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd LLLL yyyy", Locale.FRENCH);
            selectedDateText.setText(getString(R.string.events_on, displayFormat.format(calendar.getTime())));
            SimpleDateFormat filterFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US); // Use Locale.US for consistency with EventModel
            String dateStr = filterFormat.format(calendar.getTime());
            filterEventsByDate(dateStr);
        });


        // Check user role for add event button visibility
        checkUserRole();

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
        View statSoutenances = root.findViewById(R.id.statSoutenances);
        View statClubs = root.findViewById(R.id.statClubs);

        // Exams
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText("2");
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText("Exams");

        // Events
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText("Events");

        // Soutenances
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statSoutenances.findViewById(R.id.tvStatLabel)).setText("Soutenances");

        // Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText("1");
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText("Clubs");
    }



    private void showAddEventPopup() {
        Intent intent = new Intent(getActivity(), AddEventActivity.class); // Use getActivity() for Fragment
        startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);
    }


    // New: Placeholder for statistics
    private void setupStatisticsEmpty(View root) {
        View statExams = root.findViewById(R.id.statExams);
        View statEvents = root.findViewById(R.id.statEvents);
        View statSoutenances = root.findViewById(R.id.statSoutenances);
        View statClubs = root.findViewById(R.id.statClubs);

        // Exams
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_exams));

        // Events
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_conferences));

        // Soutenances
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statSoutenances.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_soutenances));

        // Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText("0");
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_clubs));
    }


    // New: onActivityResult to handle AddEventActivity and GoogleCalendarHelper results
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle results from GoogleCalendarHelper's sign-in or authorization
        if (requestCode == REQUEST_CODE_SIGN_IN_GCH || requestCode == REQUEST_AUTHORIZATION_GCH) {
            if (googleCalendarHelper != null) { // Ensure helper is initialized before handling result
                googleCalendarHelper.handleSignInResult(data);
            }
            return; // Consume the result
        }

        if (requestCode == ADD_EVENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) { // Use Activity.RESULT_OK
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
                            Toast.makeText(getContext(), "Event Added to Firebase: " + title, Toast.LENGTH_SHORT).show();
                            // Now, add to Google Calendar
                            if (getActivity() != null) { // Add null check
                                googleCalendarHelper = new GoogleCalendarHelper(getActivity(), newEvent); // Instantiate with the new event
                            } else {
                                Toast.makeText(getContext(), "Error: Fragment not attached to Activity for Google Calendar.", Toast.LENGTH_LONG).show();
                                return; // Prevent further execution if Activity is null
                            }
                            googleCalendarHelper.signInAndAddEvent();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to add event to Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        }
    }


    // New: onRegisterClick from EventAdapter.OnEventRegisterClickListener
    @Override
    public void onRegisterClick(EventModel event) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // 1. Save to Firebase interestedEvents
            mUsersDatabase.child(uid).child("interestedEvents").child(event.getId()).setValue(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Event '" + event.getTitle() + "' registered!", Toast.LENGTH_SHORT).show();
                        // 2. Add to Google Calendar
                        if (getActivity() != null) { // Add null check
                            googleCalendarHelper = new GoogleCalendarHelper(getActivity(), event);
                        } else {
                            Toast.makeText(getContext(), "Error: Fragment not attached to Activity for Google Calendar.", Toast.LENGTH_LONG).show();
                            return; // Prevent further execution if Activity is null
                        }
                        googleCalendarHelper.signInAndAddEvent();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to register event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(getContext(), "You need to be logged in to register for events.", Toast.LENGTH_SHORT).show();
        }
    }


    // New: checkUserRole (for btnAddEvent visibility)
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


    // New: loadEvents from Firebase
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
                // Cast needed for CalendarView
                ((com.applandeo.materialcalendarview.CalendarView) calendarView).setEvents(eventsWithColoredDots);
                Calendar today = Calendar.getInstance();
                String formattedTodayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(today.getTime());
                filterEventsByDate(formattedTodayDate);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load events: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // New: filterEventsByDate
    private void filterEventsByDate(String date) {
        eventList.clear(); // Clear current list before adding filtered
        for (EventModel event : allEventsList) {
            if (event.getDate().equals(date)) {
                eventList.add(event);
            }
        }
        eventAdapter.updateEvents(eventList); // Update the adapter with filtered events
        updateStatistics(eventList); // Update statistics based on filtered events
    }


    // New: updateStatistics
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

        if (getView() == null) return; // Ensure view is available

        View root = getView();
        View statExams = root.findViewById(R.id.statExams);
        View statEvents = root.findViewById(R.id.statEvents);
        View statSoutenances = root.findViewById(R.id.statSoutenances);
        View statClubs = root.findViewById(R.id.statClubs);

        // Exams
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setText(String.valueOf(examsCount));
        ((TextView) statExams.findViewById(R.id.tvStatCount)).setTextColor(Color.RED);
        ((TextView) statExams.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_exams));

        // Events
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setText(String.valueOf(conferencesCount));
        ((TextView) statEvents.findViewById(R.id.tvStatCount)).setTextColor(Color.BLUE);
        ((TextView) statEvents.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_conferences));

        // Soutenances
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setText(String.valueOf(soutenancesCount));
        ((TextView) statSoutenances.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#FFA000"));
        ((TextView) statSoutenances.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_soutenances));

        // Clubs
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setText(String.valueOf(clubsCount));
        ((TextView) statClubs.findViewById(R.id.tvStatCount)).setTextColor(Color.parseColor("#009688"));
        ((TextView) statClubs.findViewById(R.id.tvStatLabel)).setText(getString(R.string.category_clubs));
    }
}