package com.example.coursemanagment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

        // Check if Security Update is needed (Email or Password changed)
        if (!newEmail.equals(currentEmailDb) || !newPass.isEmpty()) {

            if (TextUtils.isEmpty(currentPass)) {
                etCurrentPass.setError("Required");
                return;
            }

            // 1. Re-Authenticate
            AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPass);
            firebaseUser.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    // 2. Update Email if changed
                    if (!newEmail.equals(currentEmailDb)) {
                        firebaseUser.updateEmail(newEmail);
                    }

                    // 3. Update Password if entered
                    if (!newPass.isEmpty()) {
                        firebaseUser.updatePassword(newPass);
                    }

                    // 4. Update Database
                    updateDatabase(newFirst, newLast, newEmail);

                } else {
                    Toast.makeText(this, "Wrong Current Password", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            // No security changes, just update names in DB
            updateDatabase(newFirst, newLast, newEmail);
        }
    }

    private void updateDatabase(String first, String last, String email) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);
        updates.put("email", email);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}