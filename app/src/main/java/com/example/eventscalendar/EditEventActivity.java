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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    private EditText editTextEventTitle;
    private Spinner spinnerEventCategory;
    private EditText editTextEventDate;
    private EditText editTextEventTime;
    private EditText editTextEventLocation;
    private EditText editTextEventDescription;
    private Button buttonSaveEvent;

    private SimpleDateFormat dateFormatter;
    private SimpleDateFormat timeFormatter;

    private String eventId;

    private DatabaseReference mEventsDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        mEventsDatabase = FirebaseDatabase.getInstance().getReference("events");

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

        // Get data from intent
        Intent intent = getIntent();
        eventId = intent.getStringExtra("eventId");
        editTextEventTitle.setText(intent.getStringExtra("eventTitle"));
        editTextEventDate.setText(intent.getStringExtra("eventDate"));
        editTextEventTime.setText(intent.getStringExtra("eventTime"));
        editTextEventLocation.setText(intent.getStringExtra("eventLocation"));
        editTextEventDescription.setText(intent.getStringExtra("eventDescription"));

        String category = intent.getStringExtra("eventCategory");
        if (category != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(category)) {
                    spinnerEventCategory.setSelection(i);
                    break;
                }
            }
        }
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
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID is missing. Cannot update.", Toast.LENGTH_LONG).show();
            return;
        }
        String title = editTextEventTitle.getText().toString().trim();
        String category = spinnerEventCategory.getSelectedItem().toString();
        String date = editTextEventDate.getText().toString().trim();
        String time = editTextEventTime.getText().toString().trim();
        String location = editTextEventLocation.getText().toString().trim();
        String description = editTextEventDescription.getText().toString().trim();

        if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill in title, date, and time", Toast.LENGTH_SHORT).show();
            return;
        }

        EventModel updatedEvent = new EventModel(title, time, location, category, date, description);
        updatedEvent.setId(eventId);

        mEventsDatabase.child(eventId).setValue(updatedEvent).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditEventActivity.this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(EditEventActivity.this, "Failed to update event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
