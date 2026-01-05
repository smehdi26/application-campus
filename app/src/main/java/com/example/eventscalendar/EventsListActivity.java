// C:/Users/lenovo/AndroidStudioProjects/application-campus/app/src/main/java/com/example/eventscalendar/EventsListActivity.java

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



import com.google.android.material.floatingactionbutton.FloatingActionButton;



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







public class EventsListActivity extends AppCompatActivity {







    private RecyclerView rvAllEvents;



    private FloatingActionButton btnAddEvent;



        private GroupedEventAdapter eventAdapter;



        private List<Object> items;



        private DatabaseReference mDatabase; // Realtime Database instance



    



        private static final int ADD_EVENT_REQUEST_CODE = 1;



    



        @Override



        protected void onCreate(Bundle savedInstanceState) {



            super.onCreate(savedInstanceState);



            setContentView(R.layout.activity_events_list);



    



            // Initialize Realtime Database



            mDatabase = FirebaseDatabase.getInstance().getReference("events");



    



            // Initialisation des vues



            rvAllEvents = findViewById(R.id.rvAllEvents);



            btnAddEvent = findViewById(R.id.btnAddEvent);



    



            setupNavbar();



            setupEventsList();



    



            btnAddEvent.setOnClickListener(v -> {



                Intent intent = new Intent(this, AddEventActivity.class);



                startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);



            });



        }



    



        private void setupEventsList() {



            items = new ArrayList<>();



            eventAdapter = new GroupedEventAdapter(items);



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



                



                // Sort events by date



                Collections.sort(eventList, (e1, e2) -> {



                    try {



                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);



                        Date date1 = sdf.parse(e1.getDate());



                        Date date2 = sdf.parse(e2.getDate());



                        return date1.compareTo(date2);



                    } catch (ParseException e) {



                        return 0;



                    }



                });







                // Group events by month



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



                        // Handle date parsing error



                    }



                }



                



                // Create the final list with headers



                List<Object> finalItems = new ArrayList<>();



                for (Map.Entry<String, List<EventModel>> entry : groupedEvents.entrySet()) {



                    finalItems.add(entry.getKey());



                    finalItems.addAll(entry.getValue());



                }







                eventAdapter.updateItems(finalItems);



                updateStatistics(eventList);



                Toast.makeText(EventsListActivity.this, "Fetched " + eventList.size() + " events from Firebase", Toast.LENGTH_SHORT).show();



            }







            @Override



            public void onCancelled(@NonNull DatabaseError error) {



                Toast.makeText(EventsListActivity.this, "Error fetching events from Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();



            }



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







            EventModel newEvent = new EventModel(title, time, location, category, date, description);







            String eventId = mDatabase.push().getKey();



            if (eventId != null) {



                newEvent.setId(eventId);



                mDatabase.child(eventId).setValue(newEvent)



                        .addOnSuccessListener(aVoid -> {



                            Toast.makeText(this, "Event Added to Firebase: " + title, Toast.LENGTH_SHORT).show();



                        })



                        .addOnFailureListener(e -> {



                            Toast.makeText(this, "Error adding event to Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();



                        });



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



            btnCalendarView.setOnClickListener(v -> finish());



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



            startActivity(new Intent(this, ProfileActivity.class));



            finish();



        });







        findViewById(R.id.navCourses).setOnClickListener(v -> {



            startActivity(new Intent(this, CoursesActivity.class));



            finish();



        });



    }



}
