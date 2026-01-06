package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyViewHolder> {
    Context context;
    ArrayList<Post> list;
    String currentUserId;
    String currentUserRole;

    public PostAdapter(Context context, ArrayList<Post> list, String currentUserId, String currentUserRole) {
        this.context = context;
        this.list = list;
        this.currentUserId = currentUserId;
        this.currentUserRole = currentUserRole;
    }

    public void updateList(ArrayList<Post> newList) {
        this.list.clear();
        this.list.addAll(newList);
        notifyDataSetChanged();
    }

    public void updateUserRole(String userRole) {
        this.currentUserRole = userRole;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_post, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Post post = list.get(position);

        holder.tvTitle.setText(post.title);
        holder.tvContent.setText(post.content);

        // Badge Pinned / Announcement
        if (post.isPinned || "ANNOUNCEMENT".equals(post.type)) {
            holder.tvPinnedBadge.setVisibility(View.VISIBLE);
            holder.ivPinIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.tvPinnedBadge.setVisibility(View.GONE);
            holder.ivPinIndicator.setVisibility(View.GONE);
        }

        // Auteur avec distinction pour les profs
        String authorName = post.authorName;
        if ("Teacher".equalsIgnoreCase(post.authorRole)) {
            authorName = "⭐ " + authorName;
            holder.tvAuthor.setTextColor(context.getResources().getColor(R.color.esprit_red));
        } else {
            holder.tvAuthor.setTextColor(Color.parseColor("#6B7280"));
        }
        holder.tvAuthor.setText(String.format(Locale.getDefault(), "%s • %s", authorName, getRelativeTime(post.timestamp)));

        // Clic sur l'auteur pour voir son profil
        holder.tvAuthor.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", post.authorId);
            context.startActivity(intent);
        });

        // Couleur dynamique par sujet Esprit
        setSubjectStyle(holder.tvSubjectTag, post.subject);

        holder.tvCommentCount.setText(String.format(Locale.getDefault(), "%d replies", post.commentCount));
        holder.tvViewCount.setText(post.viewCount + " views");

        if (post.lastReplyTimestamp > post.timestamp) {
            holder.tvLastReply.setText(String.format(Locale.getDefault(), "Last reply: %s", getRelativeTime(post.lastReplyTimestamp)));
            holder.tvLastReply.setVisibility(View.VISIBLE);
        } else {
            holder.tvLastReply.setVisibility(View.GONE);
        }
        
        checkFavoriteStatus(post, holder.btnFavorite);
        holder.btnFavorite.setOnClickListener(v -> toggleFavorite(post, holder.btnFavorite));

        if (post.authorName != null && !post.authorName.isEmpty()) {
            holder.tvAvatarInitial.setText(String.valueOf(post.authorName.charAt(0)).toUpperCase());
        }

        holder.viewNewIndicator.setVisibility(post.isNew ? View.VISIBLE : View.GONE);

        // Reactions
        Button btnAddReaction = holder.itemView.findViewById(R.id.btnAddReaction);
        LinearLayout userReactionsContainer = holder.itemView.findViewById(R.id.userReactionsContainer);
        
        if (btnAddReaction != null) {
            btnAddReaction.setOnClickListener(v -> {
                EmojiPickerDialog dialog = new EmojiPickerDialog(context);
                dialog.setListener(emoji -> {
                    addReaction(post, emoji, currentUserId);
                    dialog.dismiss();
                });
                dialog.show();
            });
        }
        
        if (userReactionsContainer != null) {
            userReactionsContainer.removeAllViews();
            if (post.reactions != null) {
                for (Map.Entry<String, Map<String, Boolean>> entry : post.reactions.entrySet()) {
                    String emoji = entry.getKey();
                    Map<String, Boolean> reactions = entry.getValue();
                    if (reactions != null && reactions.size() > 0) {
                        Button btn = new Button(context);
                        btn.setText(String.format(Locale.getDefault(), "%s %d", emoji, reactions.size()));
                        btn.setTextSize(10);
                        btn.setPadding(6, 2, 6, 2);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMarginStart(4);
                        btn.setLayoutParams(lp);
                        
                        if (reactions.getOrDefault(currentUserId, false)) {
                            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF6B9D")));
                            btn.setTextColor(Color.WHITE);
                        } else {
                            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                            btn.setTextColor(Color.BLACK);
                        }
                        
                        btn.setOnClickListener(v -> toggleReaction(post, emoji, currentUserId));
                        userReactionsContainer.addView(btn);
                    }
                }
            }
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailsActivity.class);
            intent.putExtra("post", post);
            intent.putExtra("postId", post.postId);
            context.startActivity(intent);
        });
    }

    private void setSubjectStyle(TextView tv, String subject) {
        if (subject == null || subject.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }
        tv.setVisibility(View.VISIBLE);
        tv.setText(subject);

        String color;
        switch (subject) {
            case "Stages & PFE": color = "#4CAF50"; break; // Vert
            case "Examens & Résultats": color = "#FF9800"; break; // Orange
            case "Académique": color = "#2196F3"; break; // Bleu
            case "Clubs & Événements": color = "#9C27B0"; break; // Violet
            case "Entraide Étudiante": color = "#00BCD4"; break; // Cyan
            case "Logement & Transport": color = "#795548"; break; // Marron
            default: color = "#6B7280"; break; // Gris
        }
        tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(color)));
        tv.setTextColor(Color.WHITE);
    }

    private String getRelativeTime(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    private void addReaction(Post post, String emoji, String currentUserId) {
        if (post.reactions == null) post.reactions = new java.util.HashMap<>();
        for (String otherEmoji : post.reactions.keySet()) {
            post.reactions.get(otherEmoji).remove(currentUserId);
            FirebaseDatabase.getInstance().getReference("Forum/Posts")
                    .child(post.postId).child("reactions/" + otherEmoji + "/" + currentUserId).removeValue();
        }
        Map<String, Boolean> emojiReactions = post.reactions.getOrDefault(emoji, new java.util.HashMap<>());
        emojiReactions.put(currentUserId, true);
        post.reactions.put(emoji, emojiReactions);
        FirebaseDatabase.getInstance().getReference("Forum/Posts")
                .child(post.postId).child("reactions/" + emoji + "/" + currentUserId).setValue(true);
        notifyDataSetChanged();
    }

    private void toggleReaction(Post post, String emoji, String currentUserId) {
        if (post.reactions == null) post.reactions = new java.util.HashMap<>();
        Map<String, Boolean> emojiReactions = post.reactions.getOrDefault(emoji, new java.util.HashMap<>());
        if (emojiReactions.getOrDefault(currentUserId, false)) {
            emojiReactions.remove(currentUserId);
            FirebaseDatabase.getInstance().getReference("Forum/Posts")
                    .child(post.postId).child("reactions/" + emoji + "/" + currentUserId).removeValue();
        } else {
            addReaction(post, emoji, currentUserId);
            return;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return list.size(); }

    private void checkFavoriteStatus(Post post, android.widget.ImageView btnFavorite) {
        if (currentUserId == null || post.postId == null) return;
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("Favorites").child(post.postId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    btnFavorite.setImageResource(snapshot.exists() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }
    
    private void toggleFavorite(Post post, android.widget.ImageView btnFavorite) {
        if (currentUserId == null || post.postId == null) return;
        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("Favorites").child(post.postId);
        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    favRef.removeValue();
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
                } else {
                    favRef.setValue(true);
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvTitle, tvContent, tvAuthor, tvSubjectTag, tvCommentCount, tvLastReply, tvAvatarInitial, tvPinnedBadge, tvViewCount;
        View viewNewIndicator;
        android.widget.ImageView btnFavorite, ivPinIndicator;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardPost);
            tvTitle = itemView.findViewById(R.id.tvPostTitle);
            tvContent = itemView.findViewById(R.id.tvPostContent);
            tvAuthor = itemView.findViewById(R.id.tvPostAuthor);
            tvSubjectTag = itemView.findViewById(R.id.tvSubjectTag);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvLastReply = itemView.findViewById(R.id.tvLastReply);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvPinnedBadge = itemView.findViewById(R.id.tvPinnedBadge);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
            viewNewIndicator = itemView.findViewById(R.id.viewNewIndicator);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            ivPinIndicator = itemView.findViewById(R.id.ivPinIndicator);
        }
    }
}
