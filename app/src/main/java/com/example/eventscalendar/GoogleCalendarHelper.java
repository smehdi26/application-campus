package com.example.eventscalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
// import androidx.fragment.app.Fragment; // Removed import for Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Log; // Import Log

public class GoogleCalendarHelper {

    public static final int REQUEST_CODE_SIGN_IN = 100; // Made public
    public static final int REQUEST_AUTHORIZATION = 101; // Made public
    private final GoogleSignInClient mGoogleSignInClient;
    private final Activity mActivity; // Reverted from Fragment
    private EventModel mEvent;
    private static final String TAG = "GoogleCalendarHelper"; // Tag for logging

    public GoogleCalendarHelper(Activity activity, EventModel event) { // Reverted constructor parameter
        mActivity = activity;
        mEvent = event;
        Log.d(TAG, "Initializing GoogleCalendarHelper for event: " + event.getTitle());
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso); // Use activity for context
    }    public void signInAndAddEvent() {
        Log.d(TAG, "signInAndAddEvent called.");
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mActivity);
        if (account == null) {
            Log.d(TAG, "No last signed-in account found, initiating sign-in flow.");
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            mActivity.startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
        } else {
            Log.d(TAG, "Last signed-in account found: " + account.getEmail() + ". Attempting to add event directly.");
            addEventToGoogleCalendar(account);
        }
    }

    public void handleSignInResult(Intent data) {
        Log.d(TAG, "handleSignInResult called.");
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(account -> {
                    Log.d(TAG, "Sign-in successful in handleSignInResult for: " + account.getEmail());
                    addEventToGoogleCalendar(account);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sign-in failed in handleSignInResult: " + e.getMessage(), e);
                    Toast.makeText(mActivity, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addEventToGoogleCalendar(GoogleSignInAccount account) {
        Log.d(TAG, "addEventToGoogleCalendar called for account: " + account.getEmail() + " and event: " + mEvent.getTitle());
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                mActivity, Collections.singleton(CalendarScopes.CALENDAR));
        credential.setSelectedAccount(account.getAccount());

        com.google.api.services.calendar.Calendar service =
                new com.google.api.services.calendar.Calendar.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName("Course Managment")
                        .build();

        new AddEventTask(service, mEvent, mActivity).execute();
    }

    private static class AddEventTask extends AsyncTask<Void, Void, String> {
        private final com.google.api.services.calendar.Calendar mService;
        private final EventModel mEventModel;
        private final Activity mActivity; // Reverted from Fragment

        AddEventTask(com.google.api.services.calendar.Calendar service, EventModel eventModel, Activity activity) { // Reverted parameter
            mService = service;
            mEventModel = eventModel;
            mActivity = activity; // Assign activity
            Log.d(TAG, "AddEventTask initialized for event: " + eventModel.getTitle());
        }

        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "AddEventTask doInBackground started for event: " + mEventModel.getTitle());
            try {
                Event event = new Event()
                        .setSummary(mEventModel.getTitle())
                        .setLocation(mEventModel.getLocation())
                        .setDescription(mEventModel.getDescription());
                Log.d(TAG, "Event object created: " + event.getSummary());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date startDate = sdf.parse(mEventModel.getDate() + " " + mEventModel.getTime());
                Date endDate = new Date(startDate.getTime() + 3600000); // 1 hour duration
                Log.d(TAG, "Parsed start date: " + startDate);

                DateTime startDateTime = new DateTime(startDate, TimeZone.getDefault());
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime)
                        .setTimeZone(TimeZone.getDefault().getID());
                event.setStart(start);

                DateTime endDateTime = new DateTime(endDate, TimeZone.getDefault());
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime)
                        .setTimeZone(TimeZone.getDefault().getID());
                event.setEnd(end);
                Log.d(TAG, "Event start and end times set.");

                String calendarId = "primary";
                Log.d(TAG, "Attempting to insert event into calendar ID: " + calendarId);
                mService.events().insert(calendarId, event).execute();
                Log.d(TAG, "Event successfully inserted into Google Calendar.");
                return "Event added to Google Calendar";
            } catch (UserRecoverableAuthIOException userRecoverableException) {
                Log.w(TAG, "User recoverable authorization exception: " + userRecoverableException.getMessage());
                mActivity.startActivityForResult(userRecoverableException.getIntent(), REQUEST_AUTHORIZATION);
                return "Authorization required. Please grant access.";
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date/time for Google Calendar event: " + e.getMessage(), e);
                return "Error with event date/time format: " + e.getMessage();
            }
            catch (IOException e) {
                Log.e(TAG, "Error adding event to Google Calendar: " + e.getMessage(), e);
                return "Error adding event: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, "AddEventTask onPostExecute: " + result);
            Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show(); // Changed to LENGTH_LONG for error messages
        }
    }
}
