package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    EditText etFirst, etLast, etEmail, etPass;
    Button btnRegister;
    RadioGroup rgRole;
    RadioButton rbStudent, rbTeacher;

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        etFirst = findViewById(R.id.etFirstName);
        etLast = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etRegEmail);
        etPass = findViewById(R.id.etRegPassword);

        btnRegister = findViewById(R.id.btnRegister);
        rgRole = findViewById(R.id.rgRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);

        btnRegister.setOnClickListener(v -> registerUser());

        setupPasswordVisibility();
    }

    private void setupPasswordVisibility() {
        etPass.setOnTouchListener((v, event) -> {
            final int RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPass.getRight() - etPass.getCompoundDrawables()[RIGHT].getBounds().width())) {
                    if (isPasswordVisible) {
                        etPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        etPass.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock, 0, android.R.drawable.ic_menu_view, 0);
                    } else {
                        etPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        etPass.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock, 0, android.R.drawable.ic_menu_view, 0);
                    }
                    isPasswordVisible = !isPasswordVisible;
                    return true;
                }
            }
            return false;
        });
    }

    private void registerUser() {
        String first = etFirst.getText().toString();
        String last = etLast.getText().toString();
        String email = etEmail.getText().toString();
        String pass = etPass.getText().toString();

        if (TextUtils.isEmpty(first) || TextUtils.isEmpty(last) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String role;
        if (rbTeacher.isChecked()) {
            role = "Teacher";
        } else {
            role = "Student"; // Default to Student if neither or Student is checked
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            Log.d(TAG, "Firebase authentication successful for uid: " + uid);

                            User user = new User(uid, first, last, email, role); // Pass selected role

                            mDatabase.child(uid).setValue(user)
                                    .addOnCompleteListener(saveTask -> {
                                        if (saveTask.isSuccessful()) {
                                            Log.d(TAG, "User details saved to database for uid: " + uid);
                                            mAuth.sendPasswordResetEmail(email)
                                                    .addOnCompleteListener(emailTask -> {
                                                        if (emailTask.isSuccessful()) {
                                                            Log.d(TAG, "Password reset email sent to " + email);
                                                            runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "User Created. Password reset email sent.", Toast.LENGTH_LONG).show());
                                                        } else {
                                                            Log.e(TAG, "Failed to send password reset email: " + emailTask.getException().getMessage(), emailTask.getException());
                                                            runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "User Created. Failed to send password reset email.", Toast.LENGTH_LONG).show());
                                                        }
                                                    });
                                            // Redirect to AllUsersActivity
                                            Intent intent = new Intent(RegisterActivity.this, AllUsersActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Log.e(TAG, "Failed to save user details to database: " + saveTask.getException().getMessage(), saveTask.getException());
                                            runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Failed to save details: " + saveTask.getException().getMessage(), Toast.LENGTH_LONG).show());
                                        }
                                    });

                        } else {
                            Log.e(TAG, "Firebase authentication failed: " + task.getException().getMessage(), task.getException());
                            runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }
                });
    }
}
