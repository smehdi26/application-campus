package com.example.eventscalendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.coursemanagment.CoursesActivity;
import com.example.coursemanagment.ProfileActivity;
import com.example.coursemanagment.R;

public class CalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // On utilise ton layout complet
        setContentView(R.layout.fragment_calendar);

        // On active la barre de navigation
        setupNavbar();

        // Note : Si tu veux que le calendrier fonctionne (clics, etc.),
        // il faudra ajouter la logique ici ou appeler une méthode du fragment.
    }

    private void setupNavbar() {
        // 1. Récupérer la navbar incluse dans fragment_calendar.xml
        // L'ID 'navbar' correspond au <include android:id="@+id/navbar" ... />

        // 2. Colorer l'icône Events en rouge
        TextView navEvents = findViewById(R.id.navEvents);
        if (navEvents != null) {
            int redColor = ContextCompat.getColor(this, R.color.esprit_red);
            navEvents.setTextColor(redColor);
            for (android.graphics.drawable.Drawable d : navEvents.getCompoundDrawables()) {
                if (d != null) d.setTint(redColor);
            }
        }

        // 3. Navigation vers Profile
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        // 4. Navigation vers Courses
        findViewById(R.id.navCourses).setOnClickListener(v -> {
            startActivity(new Intent(this, CoursesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}
