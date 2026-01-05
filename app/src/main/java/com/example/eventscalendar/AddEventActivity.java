package com.example.eventscalendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coursemanagment.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private EditText editTextEventTitle;
    private Spinner spinnerEventCategory;
    private EditText editTextEventDate;
    private EditText editTextEventTime;
    private EditText editTextEventLocation;
    private EditText editTextEventDescription;
    private Button buttonSaveEvent;

    private SimpleDateFormat dateFormatter;
    private SimpleDateFormat timeFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        editTextEventTitle = findViewById(R.id.editTextEventTitle);
        spinnerEventCategory = findViewById(R.id.spinnerEventCategory);
        editTextEventDate = findViewById(R.id.editTextEventDate);
        editTextEventTime = findViewById(R.id.editTextEventTime);
        editTextEventLocation = findViewById(R.id.editTextEventLocation);
        editTextEventDescription = findViewById(R.id.editTextEventDescription);
        buttonSaveEvent = findViewById(R.id.buttonSaveEvent);

        // Setup Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.event_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEventCategory.setAdapter(adapter);

        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Set up DatePicker
        editTextEventDate.setOnClickListener(v -> showDatePickerDialog());

        // Set up TimePicker
        editTextEventTime.setOnClickListener(v -> showTimePickerDialog());

        buttonSaveEvent.setOnClickListener(v -> saveEvent());
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, monthOfYear, dayOfMonth);
                    editTextEventDate.setText(dateFormatter.format(selectedDate.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minuteOfHour);
                    editTextEventTime.setText(timeFormatter.format(selectedTime.getTime()));
                }, hour, minute, true); // true for 24-hour format
        timePickerDialog.show();
    }

    private void saveEvent() {
        String title = editTextEventTitle.getText().toString().trim();
        String category = spinnerEventCategory.getSelectedItem().toString();
        String date = editTextEventDate.getText().toString().trim();
        String time = editTextEventTime.getText().toString().trim();
        String location = editTextEventLocation.getText().toString().trim();
        String description = editTextEventDescription.getText().toString().trim();

        if (title.isEmpty()) {
            editTextEventTitle.setError("Le titre est obligatoire");
            editTextEventTitle.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            Toast.makeText(this, "La date est obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            java.util.Date eventDate = dateFormatter.parse(date);
            if (eventDate != null && eventDate.before(new java.util.Date())) {
                Toast.makeText(this, "La date de l'événement ne peut pas être dans le passé", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (java.text.ParseException e) {
            Toast.makeText(this, "Format de date invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "Le temps est obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }

        if (location.isEmpty()) {
            editTextEventLocation.setError("Le lieu est obligatoire");
            editTextEventLocation.requestFocus();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("event_title", title);
        resultIntent.putExtra("event_category", category);
        resultIntent.putExtra("event_date", date); 
        resultIntent.putExtra("event_time", time); 
        resultIntent.putExtra("event_location", location);
        resultIntent.putExtra("event_description", description);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
