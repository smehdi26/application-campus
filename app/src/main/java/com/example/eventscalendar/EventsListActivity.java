// C:/Users/lenovo/AndroidStudioProjects/application-campus/app/src/main/java/com/example/eventscalendar/EventsListActivity.java

package com.example.eventscalendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.coursemanagment.CoursesActivity;
import com.example.coursemanagment.ProfileActivity;
import com.example.coursemanagment.R;

public class EventsListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);

        setupNavbar();
        setupEventsList();
    }

    private void setupEventsList() {
        // Ici tu initialiseras ton RecyclerView rvAllEvents
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