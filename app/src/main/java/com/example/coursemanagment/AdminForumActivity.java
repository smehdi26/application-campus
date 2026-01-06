package com.example.coursemanagment;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Locale;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;

public class AdminForumActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AdminPostAdapter adapter;
    ArrayList<Post> list;
    ArrayList<Post> filteredList;
    DatabaseReference postsRef;
    String selectedFilter = "ALL"; // ALL, PENDING, PUBLISHED, HIDDEN
    
    TextView filterAll, filterPending, filterPublished, filterHidden;
    EditText etSearch;
    ImageView btnBack;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_forum);

        postsRef = FirebaseDatabase.getInstance().getReference("Forum").child("Posts");

        recyclerView = findViewById(R.id.recyclerAdminPosts);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        
        // Filters
        filterAll = findViewById(R.id.filterAll);
        filterPending = findViewById(R.id.filterPending);
        filterPublished = findViewById(R.id.filterPublished);
        filterHidden = findViewById(R.id.filterHidden);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new AdminPostAdapter(this, filteredList, this);
        recyclerView.setAdapter(adapter);

        setupFilters();
        loadPosts();

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupFilters() {
        if (filterAll != null) filterAll.setOnClickListener(v -> selectFilter("ALL", filterAll));
        if (filterPending != null) filterPending.setOnClickListener(v -> selectFilter("PENDING", filterPending));
        if (filterPublished != null) filterPublished.setOnClickListener(v -> selectFilter("PUBLISHED", filterPublished));
        if (filterHidden != null) filterHidden.setOnClickListener(v -> selectFilter("HIDDEN", filterHidden));
    }

    private void selectFilter(String filter, TextView selectedView) {
        selectedFilter = filter;
        
        resetFilterStyle(filterAll);
        resetFilterStyle(filterPending);
        resetFilterStyle(filterPublished);
        resetFilterStyle(filterHidden);
        
        if (selectedView != null) {
            selectedView.setBackgroundColor(ContextCompat.getColor(this, R.color.esprit_red));
            selectedView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
        
        filterPosts();
    }

    private void resetFilterStyle(TextView filterView) {
        if (filterView != null) {
            filterView.setBackgroundColor(0xFFE0E0E0);
            filterView.setTextColor(0xFF333333);
        }
    }

    private void filterPosts() {
        filteredList.clear();
        String searchQuery = (etSearch != null && etSearch.getText() != null) 
            ? etSearch.getText().toString().toLowerCase(Locale.getDefault()).trim() 
            : "";
        
        for (Post post : list) {
            boolean matchesFilter = false;
            boolean matchesSearch = searchQuery.isEmpty() || 
                post.title.toLowerCase(Locale.getDefault()).contains(searchQuery) ||
                post.content.toLowerCase(Locale.getDefault()).contains(searchQuery) ||
                (post.authorName != null && post.authorName.toLowerCase(Locale.getDefault()).contains(searchQuery));
            
            switch (selectedFilter) {
                case "ALL":
                    matchesFilter = true;
                    break;
                case "PENDING":
                    matchesFilter = "PENDING".equals(post.status);
                    break;
                case "PUBLISHED":
                    matchesFilter = "PUBLISHED".equals(post.status);
                    break;
                case "HIDDEN":
                    matchesFilter = "HIDDEN".equals(post.status);
                    break;
            }
            
            if (matchesFilter && matchesSearch) {
                filteredList.add(post);
            }
        }
        
        Collections.sort(filteredList, (p1, p2) -> Long.compare(p2.timestamp, p1.timestamp));
        adapter.notifyDataSetChanged();
    }

    private void loadPosts() {
        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.postId = dataSnapshot.getKey();
                        list.add(post);
                    }
                }
                filterPosts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterPosts();
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    public void approvePost(Post post) {
        postsRef.child(post.postId).child("status").setValue("PUBLISHED")
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, getString(R.string.post_approved), Toast.LENGTH_SHORT).show();
            });
    }

    public void hidePost(Post post) {
        postsRef.child(post.postId).child("status").setValue("HIDDEN")
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, getString(R.string.post_hidden), Toast.LENGTH_SHORT).show();
            });
    }

    public void deletePost(Post post) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_post))
            .setMessage(getString(R.string.confirm_delete_post))
            .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                postsRef.child(post.postId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.post_deleted), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }
}
