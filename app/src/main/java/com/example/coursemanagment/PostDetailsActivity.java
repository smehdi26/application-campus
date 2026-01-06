package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PostDetailsActivity extends AppCompatActivity implements MentionAdapter.OnUserClickListener {

    Post post;
    TextView tvPostTitle, tvPostContent, tvPostAuthor, tvPostDate, tvAttachmentName;
    ImageView btnBack, btnEditPost, btnDeletePost, btnPinPost;
    RecyclerView recyclerComments;
    EditText etComment;
    Button btnPostComment;
    CommentAdapter adapter;
    ArrayList<Comment> commentList;
    DatabaseReference commentsRef;
    DatabaseReference postRef;
    String currentUserId;
    String currentUserRole;
    String currentUserName;
    LinearLayout layoutAttachment;
    ProgressBar progressBar;

    // Variables for replies
    private String selectedParentCommentId = null;
    private String selectedMentionedUserName = null;
    private String selectedMentionedUserId = null;

    // For mentions
    private RecyclerView recyclerMentions;
    private MentionAdapter mentionAdapter;
    private List<User> allUsers;
    private List<User> filteredMentionUsers;

    private ValueEventListener postListener;

    // ---------------- Validation rules ----------------
    private static final int COMMENT_MIN = 2;
    private static final int COMMENT_MAX = 500;

    private boolean isSubmittingComment = false;

    // Option
    private static final boolean BLOCK_LINKS_IN_COMMENTS = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        post = (Post) getIntent().getSerializableExtra("post");
        if (post == null) {
            Toast.makeText(this, R.string.error_post_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        postRef = FirebaseDatabase.getInstance().getReference("Forum")
                .child("Posts").child(post.postId);
        commentsRef = postRef.child("Comments");

        initViews();
        setupRecyclerView();
        setupPostData(); // Initial setup from Intent
        setupListeners();
        setupNavbar();

        loadUserRoleAndSetupUI();
        loadComments();
        loadAllUsers();

        incrementViewCount();
        listenToPostChanges();

        setupLiveValidation();
    }

    private void initViews() {
        tvPostTitle = findViewById(R.id.tvPostTitle);
        tvPostContent = findViewById(R.id.tvPostContent);
        tvPostAuthor = findViewById(R.id.tvPostAuthor);
        tvPostDate = findViewById(R.id.tvPostDate);
        btnBack = findViewById(R.id.btnBack);
        btnEditPost = findViewById(R.id.btnEditPost);
        btnDeletePost = findViewById(R.id.btnDeletePost);
        btnPinPost = findViewById(R.id.btnPinPost);
        recyclerComments = findViewById(R.id.recyclerComments);
        etComment = findViewById(R.id.etComment);
        btnPostComment = findViewById(R.id.btnPostComment);
        layoutAttachment = findViewById(R.id.layoutAttachment);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        progressBar = findViewById(R.id.progressBar);
        recyclerMentions = findViewById(R.id.recyclerMentions);
    }

    private void setupRecyclerView() {
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        adapter = new CommentAdapter(this, commentList, currentUserId, currentUserRole, post.postId, this);
        recyclerComments.setAdapter(adapter);

        recyclerMentions.setLayoutManager(new LinearLayoutManager(this));
        allUsers = new ArrayList<>();
        filteredMentionUsers = new ArrayList<>();
        mentionAdapter = new MentionAdapter(filteredMentionUsers, this);
        recyclerMentions.setAdapter(mentionAdapter);
    }

    private void setupPostData() {
        if (post == null) return;

        tvPostTitle.setText(post.title);
        tvPostContent.setText(post.content);

        String role = (post.authorRole == null) ? "" : post.authorRole;
        tvPostAuthor.setText(post.authorName + " (" + role + ")");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvPostDate.setText(sdf.format(new Date(post.timestamp)));

        if (post.attachmentUrl != null && !post.attachmentUrl.isEmpty()) {
            layoutAttachment.setVisibility(View.VISIBLE);
            tvAttachmentName.setText(post.attachmentName);
        } else {
            layoutAttachment.setVisibility(View.GONE);
        }

        updateButtonsVisibility();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPostComment.setOnClickListener(v -> postComment());

        btnEditPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            intent.putExtra("post", post);
            startActivity(intent);
        });

        btnDeletePost.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_post)
                    .setMessage(R.string.confirm_delete_post)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        postRef.removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, R.string.post_deleted, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(PostDetailsActivity.this,
                                                "Failed to delete post: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        btnPinPost.setOnClickListener(v -> {
            boolean newPinnedState = !post.isPinned;
            postRef.child("isPinned").setValue(newPinnedState)
                    .addOnSuccessListener(aVoid -> {
                        post.isPinned = newPinnedState;
                        updatePinButton();
                        Toast.makeText(this, newPinnedState ? "Post pinned" : "Post unpinned", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update pin status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        layoutAttachment.setOnClickListener(v -> {
            if (post.attachmentUrl != null && !post.attachmentUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(post.attachmentUrl));
                startActivity(browserIntent);
            }
        });

        // Mention search
        etComment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String commentText = s.toString();
                int atIndex = commentText.lastIndexOf('@');
                if (atIndex != -1 && atIndex < commentText.length() - 1) {
                    String query = commentText.substring(atIndex + 1).toLowerCase(Locale.getDefault());
                    filterAndShowMentions(query);
                } else {
                    recyclerMentions.setVisibility(View.GONE);
                    if (!commentText.contains("@")) {
                        selectedMentionedUserId = null;
                        selectedMentionedUserName = null;
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupLiveValidation() {
        etComment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateComment(false);
            }
        });
    }

    private void setSubmittingCommentUi(boolean submitting) {
        btnPostComment.setEnabled(!submitting);
        btnPostComment.setAlpha(submitting ? 0.7f : 1f);
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n");
    }

    private boolean validateComment(boolean showToast) {
        String content = normalizeText(etComment.getText().toString());

        if (content.isEmpty()) {
            etComment.setError(getString(R.string.required));
            if (showToast) Toast.makeText(this, getString(R.string.required), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!content.matches(".*[A-Za-zÀ-ÿ0-9].*")) {
            etComment.setError("Commentaire invalide");
            if (showToast) Toast.makeText(this, "Commentaire invalide", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (content.length() < COMMENT_MIN) {
            etComment.setError("Trop court (min " + COMMENT_MIN + ")");
            if (showToast) Toast.makeText(this, "Commentaire trop court", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (content.length() > COMMENT_MAX) {
            etComment.setError("Trop long (max " + COMMENT_MAX + ")");
            if (showToast) Toast.makeText(this, "Commentaire trop long", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (content.contains("@")) {
            int atIndex = content.lastIndexOf('@');
            boolean typingMention = atIndex != -1 && atIndex < content.length() - 1;
            if (typingMention && (selectedMentionedUserId == null || selectedMentionedUserId.isEmpty())) {
                etComment.setError("Choisissez un utilisateur dans la liste de mention");
                if (showToast) Toast.makeText(this, "Choisissez un utilisateur à mentionner", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (BLOCK_LINKS_IN_COMMENTS) {
            if (content.contains("http://") || content.contains("https://") || content.contains("www.")) {
                etComment.setError("Les liens ne sont pas autorisés dans les commentaires");
                if (showToast) Toast.makeText(this, "Lien interdit dans les commentaires", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        etComment.setError(null);
        return true;
    }

    private void listenToPostChanges() {
        postListener = postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post updatedPost = snapshot.getValue(Post.class);
                if (updatedPost != null) {
                    updatedPost.postId = snapshot.getKey();
                    post = updatedPost;
                    setupPostData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostDetailsActivity", "Failed to listen to post: " + error.getMessage());
            }
        });
    }

    private void incrementViewCount() {
        postRef.child("viewCount").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer views = currentData.getValue(Integer.class);
                if (views == null) currentData.setValue(1);
                else currentData.setValue(views + 1);
                return Transaction.success(currentData);
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
        });
    }

    private void loadUserRoleAndSetupUI() {
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                currentUserRole = (user.role != null) ? user.role : "";
                                String firstName = (user.firstName != null) ? user.firstName : "";
                                String lastName = (user.lastName != null) ? user.lastName : "";
                                currentUserName = (firstName + " " + lastName).trim();

                                if (adapter != null) {
                                    adapter.currentUserRole = currentUserRole;
                                    adapter.notifyDataSetChanged();
                                }
                                updateButtonsVisibility();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PostDetailsActivity.this,
                                "Failed to load user role: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateButtonsVisibility() {
        if (post == null) return;
        boolean canModify = "Admin".equalsIgnoreCase(currentUserRole) || Objects.equals(currentUserId, post.authorId);
        boolean canPin = "Admin".equalsIgnoreCase(currentUserRole) || "Teacher".equalsIgnoreCase(currentUserRole);

        btnEditPost.setVisibility(canModify ? View.VISIBLE : View.GONE);
        btnDeletePost.setVisibility(canModify ? View.VISIBLE : View.GONE);
        btnPinPost.setVisibility(canPin ? View.VISIBLE : View.GONE);
        updatePinButton();
    }

    private void updatePinButton() {
        if (post == null) return;
        if (post.isPinned) {
            btnPinPost.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnPinPost.setContentDescription("Unpin post");
        } else {
            btnPinPost.setImageResource(android.R.drawable.ic_menu_add);
            btnPinPost.setContentDescription("Pin post");
        }
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Comment comment = dataSnapshot.getValue(Comment.class);
                    if (comment != null) {
                        comment.commentId = dataSnapshot.getKey();
                        commentList.add(comment);
                    }
                }
                Collections.sort(commentList, (c1, c2) -> Long.compare(c1.timestamp, c2.timestamp));
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PostDetailsActivity.this,
                        "Failed to load comments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void postComment() {
        if (isSubmittingComment) return;

        if (!validateComment(true)) return;

        isSubmittingComment = true;
        setSubmittingCommentUi(true);

        String content = normalizeText(etComment.getText().toString());

        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            isSubmittingComment = false;
                            setSubmittingCommentUi(false);
                            Toast.makeText(PostDetailsActivity.this, "Utilisateur introuvable", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String commentId = commentsRef.push().getKey();
                        if (commentId == null) {
                            isSubmittingComment = false;
                            setSubmittingCommentUi(false);
                            Toast.makeText(PostDetailsActivity.this, "Erreur ID commentaire", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Comment comment = new Comment(commentId, post.postId, content, currentUserId,
                                user.firstName + " " + user.lastName, user.role, System.currentTimeMillis());

                        if (selectedParentCommentId != null) {
                            comment.parentCommentId = selectedParentCommentId;
                            comment.mentionedUserName = selectedMentionedUserName;
                        }

                        if (selectedMentionedUserId != null) {
                            comment.mentionedUserId = selectedMentionedUserId;
                            comment.mentionedUserName = selectedMentionedUserName;
                        }

                        commentsRef.child(commentId).setValue(comment)
                                .addOnSuccessListener(aVoid -> {
                                    etComment.setText("");
                                    etComment.setError(null);
                                    etComment.setHint(R.string.write_comment);

                                    selectedParentCommentId = null;
                                    selectedMentionedUserName = null;
                                    selectedMentionedUserId = null;
                                    recyclerMentions.setVisibility(View.GONE);

                                    postRef.child("commentCount").runTransaction(new Transaction.Handler() {
                                        @NonNull
                                        @Override
                                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                            Integer count = currentData.getValue(Integer.class);
                                            if (count == null) currentData.setValue(1);
                                            else currentData.setValue(count + 1);
                                            return Transaction.success(currentData);
                                        }
                                        @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
                                    });
                                    postRef.child("lastReplyTimestamp").setValue(ServerValue.TIMESTAMP);

                                    sendNotificationToAuthor(user.firstName + " " + user.lastName);

                                    if (comment.mentionedUserId != null && !comment.mentionedUserId.isEmpty()) {
                                        sendMentionNotification(comment.mentionedUserId, comment.authorName, post.title, post.postId);
                                    }

                                    Toast.makeText(PostDetailsActivity.this, R.string.comment_posted, Toast.LENGTH_SHORT).show();

                                    isSubmittingComment = false;
                                    setSubmittingCommentUi(false);

                                })
                                .addOnFailureListener(e -> {
                                    isSubmittingComment = false;
                                    setSubmittingCommentUi(false);
                                    Toast.makeText(PostDetailsActivity.this,
                                            "Failed to post comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isSubmittingComment = false;
                        setSubmittingCommentUi(false);
                        Toast.makeText(PostDetailsActivity.this,
                                "Failed to get user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAllUsers() {
        FirebaseDatabase.getInstance().getReference("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allUsers.clear();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null && !Objects.equals(user.uid, currentUserId)) {
                                allUsers.add(user);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PostDetailsActivity", "Failed to load users: " + error.getMessage());
                    }
                });
    }

    private void filterAndShowMentions(String query) {
        filteredMentionUsers.clear();
        if (query.isEmpty()) {
            recyclerMentions.setVisibility(View.GONE);
            return;
        }

        for (User user : allUsers) {
            String fullName = (user.firstName + " " + user.lastName).toLowerCase(Locale.getDefault());
            String role = (user.role != null) ? user.role.toLowerCase(Locale.getDefault()) : "";
            if (fullName.contains(query) || role.contains(query)) {
                filteredMentionUsers.add(user);
            }
        }

        if (filteredMentionUsers.isEmpty()) {
            recyclerMentions.setVisibility(View.GONE);
        } else {
            mentionAdapter.updateUsers(filteredMentionUsers);
            recyclerMentions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUserClick(User user) {
        String currentText = etComment.getText().toString();
        int atIndex = currentText.lastIndexOf('@');
        if (atIndex != -1) {
            String newText = currentText.substring(0, atIndex + 1)
                    + user.firstName + " " + user.lastName + " ";
            etComment.setText(newText);
            etComment.setSelection(newText.length());
            selectedMentionedUserId = user.uid;
            selectedMentionedUserName = user.firstName + " " + user.lastName;
            recyclerMentions.setVisibility(View.GONE);
            etComment.setError(null);
        }
    }

    private void sendNotificationToAuthor(String senderName) {
        if (Objects.equals(post.authorId, currentUserId)) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(post.authorId).child("Notifications");
        String notifId = notifRef.push().getKey();

        NotificationForum notificationForum = new NotificationForum(
                post.authorId,
                currentUserId,
                senderName,
                "NEW_REPLY",
                post.postId,
                post.title,
                senderName + " a répondu à votre post."
        );
        notificationForum.notificationId = notifId;
        if (notifId != null) notifRef.child(notifId).setValue(notificationForum);
    }

    private void sendMentionNotification(String mentionedUserId, String mentionerName, String postTitle, String postId) {
        if (Objects.equals(mentionedUserId, currentUserId)) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(mentionedUserId).child("Notifications");
        String notifId = notifRef.push().getKey();

        NotificationForum notificationForum = new NotificationForum(
                mentionedUserId,
                currentUserId,
                mentionerName,
                "MENTION",
                postId,
                postTitle,
                mentionerName + " vous a mentionné dans un post: " + postTitle
        );
        notificationForum.notificationId = notifId;
        if (notifId != null) notifRef.child(notifId).setValue(notificationForum);
    }

    public void replyToComment(Comment comment) {
        this.selectedParentCommentId = comment.commentId;
        this.selectedMentionedUserName = comment.authorName;
        etComment.requestFocus();
        etComment.setHint("Replying to " + comment.authorName + "...");

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void setupNavbar() {
        findViewById(R.id.navForums).setOnClickListener(v -> finish());
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postRef != null && postListener != null) {
            postRef.removeEventListener(postListener);
        }
    }
}
