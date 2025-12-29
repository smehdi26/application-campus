package com.example.coursemanagment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    EditText etFirst, etLast, etEmail, etNewPass, etCurrentPass;
    Button btnSave, btnCancel;
    LinearLayout btnBack;

    DatabaseReference mDatabase;
    FirebaseUser firebaseUser;
    String uid;
    String currentEmailDb;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFirst = findViewById(R.id.etEditFirst);
        etLast = findViewById(R.id.etEditLast);
        etEmail = findViewById(R.id.etEditEmail);
        etNewPass = findViewById(R.id.etNewPassword);
        etCurrentPass = findViewById(R.id.etCurrentPassword);

        btnSave = findViewById(R.id.btnSaveChanges);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.btnBack);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = firebaseUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        pd = new ProgressDialog(this);
        pd.setMessage("Updating Profile...");

        loadCurrentData();

        btnSave.setOnClickListener(v -> attemptUpdate());
        btnCancel.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCurrentData() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    etFirst.setText(user.firstName);
                    etLast.setText(user.lastName);
                    etEmail.setText(user.email);
                    currentEmailDb = user.email;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void attemptUpdate() {
        String newFirst = etFirst.getText().toString().trim();
        String newLast = etLast.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPass = etNewPass.getText().toString().trim();
        String currentPass = etCurrentPass.getText().toString().trim();

        if (TextUtils.isEmpty(newFirst) || TextUtils.isEmpty(newLast) || TextUtils.isEmpty(newEmail)) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isEmailChanged = !newEmail.equals(currentEmailDb);
        boolean isPassChanged = !newPass.isEmpty();

        if (isEmailChanged || isPassChanged) {
            if (TextUtils.isEmpty(currentPass)) {
                etCurrentPass.setError("Required");
                etCurrentPass.requestFocus();
                return;
            }
            // Start the Chain of Updates
            pd.show();
            step1_ReAuthenticate(newFirst, newLast, newEmail, newPass, currentPass);
        } else {
            // Just update text in DB
            updateDatabaseOnly(newFirst, newLast);
        }
    }

    // STEP 1: Verify the user is who they say they are
    private void step1_ReAuthenticate(String first, String last, String email, String newPass, String currentPass) {
        AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPass);

        firebaseUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Auth OK, move to Step 2
                step2_UpdateEmail(first, last, email, newPass);
            } else {
                pd.dismiss();
                Toast.makeText(this, "Wrong Current Password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // STEP 2: Update Email in Authentication System
    private void step2_UpdateEmail(String first, String last, String email, String newPass) {
        if (!email.equals(currentEmailDb)) {
            firebaseUser.updateEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Email OK, move to Step 3
                    step3_UpdatePassword(first, last, email, newPass);
                } else {
                    pd.dismiss();
                    Toast.makeText(this, "Email Update Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Email didn't change, skip to Step 3
            step3_UpdatePassword(first, last, email, newPass);
        }
    }

    // STEP 3: Update Password in Authentication System
    private void step3_UpdatePassword(String first, String last, String email, String newPass) {
        if (!newPass.isEmpty()) {
            firebaseUser.updatePassword(newPass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Password OK, move to Step 4
                    step4_UpdateDatabase(first, last, email);
                } else {
                    pd.dismiss();
                    Toast.makeText(this, "Password Update Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Password didn't change, skip to Step 4
            step4_UpdateDatabase(first, last, email);
        }
    }

    // STEP 4: Finally, update the Realtime Database to match Auth
    private void step4_UpdateDatabase(String first, String last, String email) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);
        updates.put("email", email);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Account Updated Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDatabaseOnly(String first, String last) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
    }
}