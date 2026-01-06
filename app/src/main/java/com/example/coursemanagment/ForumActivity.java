package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class ForumActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PostAdapter adapter;
    ArrayList<Post> list;
    ArrayList<Post> filteredList;
    DatabaseReference mDatabase;
    FloatingActionButton fabCreatePost;
    View emptyState;

    String currentUserId;
    String currentUserRole = ""; 
    String selectedFilter = "All";
    String currentSortMode = "Recent"; // Options: Recent, Popular, Hot

    private ValueEventListener postsListener;

    ChipGroup chipGroupSubjectFilters;
    Chip chipFilterAll, chipFilterAcademic, chipFilterClubs, chipFilterInternships, chipFilterLife, chipFilterTransport, chipFilterExams, chipFilterHelp;
    EditText etSearch;
    ImageView btnOpenNotifications, btnSort, btnShowFavorites;

    private Button btnPrevPage, btnNextPage;
    private TextView tvPageNumber;

    private int currentPage = 1;
    private final int postsPerPage = 6;
    private int totalPages = 1;

    private boolean showingFavoritesOnly = false;
    private final ArrayList<String> favoritePostIds = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        setContentView(R.layout.activity_forum);

        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("Forum").child("Posts");

        recyclerView = findViewById(R.id.recyclerPosts);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        emptyState = findViewById(R.id.emptyState);
        btnOpenNotifications = findViewById(R.id.btnOpenNotifications);
        btnSort = findViewById(R.id.btnSort);
        btnShowFavorites = findViewById(R.id.btnShowFavorites);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageNumber = findViewById(R.id.tvPageNumber);

        chipGroupSubjectFilters = findViewById(R.id.chipGroupSubjectFilters);
        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterAcademic = findViewById(R.id.chipFilterAcademic);
        chipFilterClubs = findViewById(R.id.chipFilterClubs);
        chipFilterInternships = findViewById(R.id.chipFilterInternships);
        chipFilterLife = findViewById(R.id.chipFilterLife);
        chipFilterTransport = findViewById(R.id.chipFilterTransport);
        chipFilterExams = findViewById(R.id.chipFilterExams);
        chipFilterHelp = findViewById(R.id.chipFilterHelp);

        etSearch = findViewById(R.id.etSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new PostAdapter(this, new ArrayList<>(), currentUserId, currentUserRole);
        recyclerView.setAdapter(adapter);

        setupSearch();
        setupFilters();
        listenForNotifications();

        fabCreatePost.setOnClickListener(v ->
                startActivity(new Intent(ForumActivity.this, CreatePostActivity.class)));

        btnOpenNotifications.setOnClickListener(v ->
                startActivity(new Intent(ForumActivity.this, NotificationsActivity.class)));

        btnSort.setOnClickListener(v -> showSortBottomSheet());
        
        btnShowFavorites.setOnClickListener(v -> toggleFavoritesFilter());

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                filterPosts();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                filterPosts();
            }
        });

        loadUserRole();
        loadPosts();
    }

    private void toggleFavoritesFilter() {
        showingFavoritesOnly = !showingFavoritesOnly;
        if (showingFavoritesOnly) {
            btnShowFavorites.setImageResource(android.R.drawable.btn_star_big_on);
            loadFavoritesAndFilter();
        } else {
            btnShowFavorites.setImageResource(android.R.drawable.btn_star_big_off);
            filterPosts();
        }
    }

    private void loadFavoritesAndFilter() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId).child("Favorites")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favoritePostIds.clear();
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            favoritePostIds.add(postSnapshot.getKey());
                        }
                        filterPosts();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showSortBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_forum_sort, null);
        
        view.findViewById(R.id.sortRecent).setOnClickListener(v -> {
            currentSortMode = "Recent";
            sortAndFilter();
            bottomSheetDialog.dismiss();
        });
        
        view.findViewById(R.id.sortPopular).setOnClickListener(v -> {
            currentSortMode = "Popular";
            sortAndFilter();
            bottomSheetDialog.dismiss();
        });
        
        view.findViewById(R.id.sortHot).setOnClickListener(v -> {
            currentSortMode = "Hot";
            sortAndFilter();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void sortAndFilter() {
        Collections.sort(list, (p1, p2) -> {
            if (p1.isPinned != p2.isPinned) return p1.isPinned ? -1 : 1;
            
            switch (currentSortMode) {
                case "Popular":
                    return Integer.compare(p2.viewCount, p1.viewCount);
                case "Hot":
                    int score1 = p1.commentCount * 2 + p1.viewCount;
                    int score2 = p2.commentCount * 2 + p2.viewCount;
                    return Integer.compare(score2, score1);
                case "Recent":
                default:
                    return Long.compare(p2.lastReplyTimestamp, p1.lastReplyTimestamp);
            }
        });
        
        currentPage = 1;
        filterPosts();
    }

    private void listenForNotifications() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId).child("Notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long unreadCount = 0;
                        for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                            Boolean isRead = notificationSnapshot.child("isRead").getValue(Boolean.class);
                            if (isRead == null || !isRead) {
                                unreadCount++;
                            }
                        }
                        TextView badge = findViewById(R.id.badgeNotifications);
                        androidx.cardview.widget.CardView badgeContainer = findViewById(R.id.badgeContainer);
                        if (badge != null && badgeContainer != null) {
                            if (unreadCount > 0) {
                                badge.setText(String.valueOf(Math.min(unreadCount, 99)));
                                badgeContainer.setVisibility(View.VISIBLE);
                                android.view.animation.Animation pulseAnimation = android.view.animation.AnimationUtils.loadAnimation(ForumActivity.this, R.anim.pulse_badge);
                                badgeContainer.startAnimation(pulseAnimation);
                            } else {
                                badgeContainer.clearAnimation();
                                badgeContainer.setVisibility(View.GONE);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupFilters() {
        chipGroupSubjectFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipFilterAll) {
                    selectedFilter = "All";
                } else if (checkedId == R.id.chipFilterAcademic) {
                    selectedFilter = getString(R.string.subject_academic);
                } else if (checkedId == R.id.chipFilterClubs) {
                    selectedFilter = getString(R.string.subject_clubs);
                } else if (checkedId == R.id.chipFilterInternships) {
                    selectedFilter = getString(R.string.subject_internships);
                } else if (checkedId == R.id.chipFilterLife) {
                    selectedFilter = getString(R.string.subject_life);
                } else if (checkedId == R.id.chipFilterTransport) {
                    selectedFilter = getString(R.string.subject_transport);
                } else if (checkedId == R.id.chipFilterExams) {
                    selectedFilter = getString(R.string.subject_exams);
                } else if (checkedId == R.id.chipFilterHelp) {
                    selectedFilter = getString(R.string.subject_help);
                }
                currentPage = 1;
                filterPosts();
            }
        });
        chipFilterAll.setChecked(true);
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 1;
                filterPosts();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterPosts() {
        filteredList.clear();
        String searchQuery = (etSearch != null && etSearch.getText() != null)
            ? etSearch.getText().toString().toLowerCase(Locale.getDefault()).trim()
            : "";

        ArrayList<Post> subjectFiltered = new ArrayList<>();
        if ("All".equals(selectedFilter)) {
            subjectFiltered.addAll(list);
        } else {
            for (Post post : list) {
                if (post.subject != null && post.subject.equals(selectedFilter)) {
                    subjectFiltered.add(post);
                }
            }
        }

        for (Post post : subjectFiltered) {
            if ("PUBLISHED".equals(post.status)) {
                boolean matchesSearch = searchQuery.isEmpty()
                        || (post.title != null && post.title.toLowerCase(Locale.getDefault()).contains(searchQuery))
                        || (post.content != null && post.content.toLowerCase(Locale.getDefault()).contains(searchQuery))
                        || (post.authorName != null && post.authorName.toLowerCase(Locale.getDefault()).contains(searchQuery))
                        || (post.subject != null && post.subject.toLowerCase(Locale.getDefault()).contains(searchQuery));

                boolean matchesFavorites = !showingFavoritesOnly || favoritePostIds.contains(post.postId);

                if (matchesSearch && matchesFavorites) {
                    filteredList.add(post);
                }
            }
        }

        totalPages = (int) Math.ceil((double) filteredList.size() / postsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int startIndex = (currentPage - 1) * postsPerPage;
        int endIndex = Math.min(startIndex + postsPerPage, filteredList.size());

        ArrayList<Post> paginatedList = new ArrayList<>();
        if (!filteredList.isEmpty() && startIndex < filteredList.size()) {
            paginatedList.addAll(filteredList.subList(startIndex, endIndex));
        }

        adapter.updateList(paginatedList);
        recyclerView.scrollToPosition(0);
        tvPageNumber.setText(getString(R.string.page_number, currentPage, totalPages));
        btnPrevPage.setEnabled(currentPage > 1);
        btnNextPage.setEnabled(currentPage < totalPages);

        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserRole() {
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        User user = snapshot.getValue(User.class);
                        if (user == null) return;
                        currentUserRole = (user.role != null) ? user.role : "";
                        adapter.updateUserRole(currentUserRole);
                        if ("Admin".equalsIgnoreCase(currentUserRole) || "Teacher".equalsIgnoreCase(currentUserRole)) {
                            fabCreatePost.setVisibility(View.VISIBLE);
                        } else {
                            fabCreatePost.setVisibility(View.GONE);
                        }
                        setupNavbar();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadPosts() {
        if (postsListener != null) mDatabase.removeEventListener(postsListener);
        postsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.postId = dataSnapshot.getKey();
                        if (post.subject == null || post.subject.isEmpty()) post.subject = "General";
                        if (post.status == null || post.status.isEmpty()) post.status = "PUBLISHED";
                        if (post.type == null || post.type.isEmpty()) post.type = "DISCUSSION";
                        list.add(post);
                    }
                }
                sortAndFilter();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.orderByChild("lastReplyTimestamp").limitToLast(50).addValueEventListener(postsListener);
    }

    private void setupNavbar() {
        TextView navForum = findViewById(R.id.navForums);
        if (navForum == null) return;
        int redColor = androidx.core.content.ContextCompat.getColor(this, R.color.esprit_red);
        navForum.setTextColor(redColor);
        for (android.graphics.drawable.Drawable d : navForum.getCompoundDrawables()) {
            if (d != null) d.setTint(redColor);
        }
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navCourses).setOnClickListener(v -> {
            startActivity(new Intent(this, CoursesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        if ("Admin".equalsIgnoreCase(currentUserRole)) {
            fabCreatePost.setOnLongClickListener(v -> {
                startActivity(new Intent(this, AdminForumActivity.class));
                return true;
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postsListener != null) mDatabase.removeEventListener(postsListener);
    }
}
