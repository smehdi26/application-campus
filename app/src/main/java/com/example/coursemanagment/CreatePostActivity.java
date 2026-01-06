package com.example.coursemanagment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class CreatePostActivity extends AppCompatActivity {

    EditText etPostTitle, etAttachmentUrl, etAttachmentName;
    EditText rtPostContent;
    Spinner spinnerSubject, spinnerPostType;
    Button btnPublish;
    ImageView btnBack;
    LinearLayout layoutPostType;
    TextView tvModerationInfo;

    DatabaseReference postsRef;
    String currentUserId;
    String currentUserRole;

    Post existingPost; // For editing
    boolean moderationEnabled = true;

    private static final int TITLE_MIN = 5;
    private static final int TITLE_MAX = 80;
    private static final int CONTENT_MIN = 15;
    private static final int CONTENT_MAX = 2000;
    private boolean isSubmitting = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postsRef = FirebaseDatabase.getInstance().getReference("Forum").child("Posts");

        etPostTitle = findViewById(R.id.etPostTitle);
        rtPostContent = findViewById(R.id.rtPostContent);
        etAttachmentUrl = findViewById(R.id.etAttachmentUrl);
        etAttachmentName = findViewById(R.id.etAttachmentName);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        spinnerPostType = findViewById(R.id.spinnerPostType);
        layoutPostType = findViewById(R.id.layoutPostType);
        tvModerationInfo = findViewById(R.id.tvModerationInfo);
        btnPublish = findViewById(R.id.btnPublish);
        btnBack = findViewById(R.id.btnBack);

        loadUserRole();

        String[] subjects = {
                getString(R.string.subject_academic),
                getString(R.string.subject_clubs),
                getString(R.string.subject_internships),
                getString(R.string.subject_life),
                getString(R.string.subject_transport),
                getString(R.string.subject_exams),
                getString(R.string.subject_help),
                getString(R.string.subject_general)
        };
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjects);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectAdapter);

        String[] postTypes = { getString(R.string.discussion), getString(R.string.announcement) };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, postTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostType.setAdapter(typeAdapter);

        existingPost = (Post) getIntent().getSerializableExtra("post");
        if (existingPost != null) {
            etPostTitle.setText(existingPost.title);
            if (existingPost.content != null) rtPostContent.setText(existingPost.content);
            if (existingPost.attachmentUrl != null) etAttachmentUrl.setText(existingPost.attachmentUrl);
            if (existingPost.attachmentName != null) etAttachmentName.setText(existingPost.attachmentName);

            if (existingPost.subject != null) {
                for (int i = 0; i < subjects.length; i++) {
                    if (subjects[i].equals(existingPost.subject)) {
                        spinnerSubject.setSelection(i);
                        break;
                    }
                }
            }
            if (existingPost.type != null) {
                spinnerPostType.setSelection("ANNOUNCEMENT".equals(existingPost.type) ? 1 : 0);
            }
            btnPublish.setText(getString(R.string.update));
        }

        setupLiveValidation();

        btnBack.setOnClickListener(v -> finish());

        btnPublish.setOnClickListener(v -> {
            if (isSubmitting) return;
            if (!validateForm()) return;

            isSubmitting = true;
            setSubmittingUi(true);

            if (existingPost != null) {
                updatePost();
            } else {
                createPost();
            }
        });
    }

    private void setupLiveValidation() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateTitle(false);
                validateContent(false);
                validateAttachment(false);
            }
        };
        etPostTitle.addTextChangedListener(watcher);
        rtPostContent.addTextChangedListener(watcher);
        etAttachmentUrl.addTextChangedListener(watcher);
        etAttachmentName.addTextChangedListener(watcher);
        etPostTitle.setMaxLines(2);
    }

    private boolean validateForm() {
        boolean ok = true;
        ok &= validateTitle(true);
        ok &= validateContent(true);
        ok &= validateSubject(true);
        ok &= validateAttachment(true);
        return ok;
    }

    private boolean validateTitle(boolean showToast) {
        String title = safeText(etPostTitle);
        if (title.isEmpty()) {
            etPostTitle.setError(getString(R.string.required));
            return false;
        }
        if (title.length() < TITLE_MIN) {
            etPostTitle.setError(getString(R.string.error_title_short));
            return false;
        }
        if (title.length() > TITLE_MAX) {
            etPostTitle.setError("Titre trop long");
            return false;
        }
        if (!title.matches(".*[A-Za-zÀ-ÿ0-9].*")) {
            etPostTitle.setError("Titre invalide");
            return false;
        }
        etPostTitle.setError(null);
        return true;
    }

    private boolean validateContent(boolean showToast) {
        String content = safeText(rtPostContent);
        if (content.isEmpty()) {
            rtPostContent.setError(getString(R.string.error_content_empty));
            return false;
        }
        if (content.length() < CONTENT_MIN) {
            rtPostContent.setError("Contenu trop court");
            return false;
        }
        if (content.length() > CONTENT_MAX) {
            rtPostContent.setError("Contenu trop long");
            return false;
        }
        rtPostContent.setError(null);
        return true;
    }

    private boolean validateSubject(boolean showToast) {
        if (spinnerSubject == null || spinnerSubject.getSelectedItem() == null) {
            if (showToast) Toast.makeText(this, "Veuillez choisir un sujet", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validateAttachment(boolean showToast) {
        String url = safeText(etAttachmentUrl);
        String name = safeText(etAttachmentName);
        if (url.isEmpty()) {
            etAttachmentUrl.setError(null);
            return true;
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            etAttachmentUrl.setError(getString(R.string.error_invalid_url));
            return false;
        }
        if (name.isEmpty()) {
            etAttachmentName.setError("Nom de pièce jointe requis");
            return false;
        }
        etAttachmentUrl.setError(null);
        etAttachmentName.setError(null);
        return true;
    }

    private String safeText(EditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }

    private void setSubmittingUi(boolean submitting) {
        btnPublish.setEnabled(!submitting);
        btnBack.setEnabled(!submitting);
        btnPublish.setAlpha(submitting ? 0.7f : 1f);
    }

    private void doneSubmittingUi() {
        isSubmitting = false;
        setSubmittingUi(false);
    }

    private void loadUserRole() {
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                currentUserRole = user.role;
                                setupUIForRole();
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupUIForRole() {
        if ("Teacher".equalsIgnoreCase(currentUserRole) || "Admin".equalsIgnoreCase(currentUserRole)) {
            layoutPostType.setVisibility(View.VISIBLE);
        } else {
            layoutPostType.setVisibility(View.GONE);
        }
        if (moderationEnabled && "Student".equalsIgnoreCase(currentUserRole)) {
            tvModerationInfo.setVisibility(View.VISIBLE);
        } else {
            tvModerationInfo.setVisibility(View.GONE);
        }
    }

    private void createPost() {
        String title = safeText(etPostTitle);
        String content = safeText(rtPostContent);
        String subject = spinnerSubject.getSelectedItem().toString();
        String attachmentUrl = safeText(etAttachmentUrl);
        String attachmentName = safeText(etAttachmentName);

        final String postType = (layoutPostType.getVisibility() == View.VISIBLE && spinnerPostType.getSelectedItemPosition() == 1)
                ? "ANNOUNCEMENT" : "DISCUSSION";

        final String status;
        if (moderationEnabled && "Student".equalsIgnoreCase(currentUserRole)) {
            status = "PENDING";
        } else {
            status = "PUBLISHED";
        }

        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            doneSubmittingUi();
                            return;
                        }

                        String postId = postsRef.push().getKey();
                        Post post = new Post(postId, title, content, currentUserId, user.firstName + " " + user.lastName, user.role, System.currentTimeMillis());
                        post.subject = subject;
                        post.type = postType;
                        post.status = status;
                        post.lastReplyTimestamp = System.currentTimeMillis();
                        if (!attachmentUrl.isEmpty()) {
                            post.attachmentUrl = attachmentUrl;
                            post.attachmentName = attachmentName;
                        }

                        postsRef.child(postId).setValue(post)
                                .addOnSuccessListener(aVoid -> {
                                    doneSubmittingUi();
                                    Toast.makeText(CreatePostActivity.this, getString(R.string.post_created), Toast.LENGTH_SHORT).show();
                                    
                                    if ("PUBLISHED".equals(status)) {
                                        sendNotificationsToAll(postId, title, user.firstName + " " + user.lastName);
                                    }
                                    
                                    finish();
                                })
                                .addOnFailureListener(e -> doneSubmittingUi());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { doneSubmittingUi(); }
                });
    }

    private void sendNotificationsToAll(String postId, String title, String authorName) {
        FirebaseDatabase.getInstance().getReference("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String targetUserId = userSnapshot.getKey();
                            if (targetUserId == null || targetUserId.equals(currentUserId)) continue;

                            String message = authorName + " a publié un nouveau post : " + title;
                            NotificationForum notificationForum = new NotificationForum(
                                    targetUserId, currentUserId, authorName, "NEW_POST", postId, title, message
                            );
                            
                            updates.put("/Users/" + targetUserId + "/Notifications/" + notificationForum.notificationId, notificationForum);
                        }
                        FirebaseDatabase.getInstance().getReference().updateChildren(updates);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updatePost() {
        String title = safeText(etPostTitle);
        String content = safeText(rtPostContent);
        String subject = spinnerSubject.getSelectedItem().toString();
        String attachmentUrl = safeText(etAttachmentUrl);
        String attachmentName = safeText(etAttachmentName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("content", content);
        updates.put("subject", subject);
        updates.put("attachmentUrl", attachmentUrl.isEmpty() ? null : attachmentUrl);
        updates.put("attachmentName", attachmentUrl.isEmpty() ? null : attachmentName);
        updates.put("type", (layoutPostType.getVisibility() == View.VISIBLE && spinnerPostType.getSelectedItemPosition() == 1) ? "ANNOUNCEMENT" : "DISCUSSION");

        postsRef.child(existingPost.postId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    doneSubmittingUi();
                    Toast.makeText(CreatePostActivity.this, getString(R.string.post_updated), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> doneSubmittingUi());
    }
}
