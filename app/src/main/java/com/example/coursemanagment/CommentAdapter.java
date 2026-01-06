package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.MyViewHolder> {
    Context context;
    ArrayList<Comment> list;
    String currentUserId;
    String currentUserRole;
    String postId;
    PostDetailsActivity activity;

    public CommentAdapter(Context context, ArrayList<Comment> list, String currentUserId, String currentUserRole, String postId, PostDetailsActivity activity) {
        this.context = context;
        this.list = list;
        this.currentUserId = currentUserId;
        this.currentUserRole = currentUserRole;
        this.postId = postId;
        this.activity = activity;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_comment, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Comment comment = list.get(position);
        
        // Indentation pour les réponses
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.layoutComment.getLayoutParams();
        params.setMarginStart((comment.parentCommentId != null && !comment.parentCommentId.isEmpty()) ? 40 : 0);
        holder.layoutComment.setLayoutParams(params);

        // Gestion améliorée des mentions
        if (comment.mentionedUserName != null && !comment.mentionedUserName.isEmpty()) {
            String mention = "@" + comment.mentionedUserName.split(" ")[0];
            String fullText = mention + " " + comment.content;
            SpannableString ss = new SpannableString(fullText);
            
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View textView) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("userId", comment.mentionedUserId);
                    context.startActivity(intent);
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setFakeBoldText(true);
                    ds.setColor(context.getResources().getColor(R.color.esprit_red));
                }
            };
            
            ss.setSpan(clickableSpan, 0, mention.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvContent.setText(ss);
            holder.tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            holder.tvContent.setHighlightColor(Color.TRANSPARENT);
        } else {
            holder.tvContent.setText(comment.content);
        }

        // Style de l'auteur (Rouge Esprit pour les profs)
        holder.tvAuthor.setText(String.format(Locale.getDefault(), "%s (%s)", comment.authorName, comment.authorRole));
        if ("Teacher".equalsIgnoreCase(comment.authorRole)) {
            holder.tvAuthor.setTextColor(context.getResources().getColor(R.color.esprit_red));
        } else {
            holder.tvAuthor.setTextColor(Color.parseColor("#111827"));
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(comment.timestamp)));

        // --- REACTIONS ---
        setupReactions(holder, comment);

        holder.btnReply.setOnClickListener(v -> {
            if (activity != null) activity.replyToComment(comment);
        });

        // Suppression
        boolean canDelete = "Admin".equalsIgnoreCase(currentUserRole) || currentUserId.equals(comment.authorId);
        holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        if (canDelete) {
            holder.btnDelete.setOnClickListener(v -> showDeleteDialog(comment));
        }
    }

    private void setupReactions(MyViewHolder holder, Comment comment) {
        Button btnAddReaction = holder.itemView.findViewById(R.id.commentBtnAddReaction);
        LinearLayout userReactionsContainer = holder.itemView.findViewById(R.id.commentUserReactionsContainer);
        
        if (btnAddReaction != null) {
            btnAddReaction.setOnClickListener(v -> {
                EmojiPickerDialog dialog = new EmojiPickerDialog(context);
                dialog.setListener(emoji -> {
                    addReaction(comment, emoji, currentUserId);
                    dialog.dismiss();
                });
                dialog.show();
            });
        }
        
        if (userReactionsContainer != null) {
            userReactionsContainer.removeAllViews();
            if (comment.reactions != null) {
                for (Map.Entry<String, Map<String, Boolean>> entry : comment.reactions.entrySet()) {
                    String emoji = entry.getKey();
                    Map<String, Boolean> reactions = entry.getValue();
                    if (reactions != null && !reactions.isEmpty()) {
                        Button btn = new Button(context);
                        btn.setText(String.format(Locale.getDefault(), "%s %d", emoji, reactions.size()));
                        btn.setTextSize(9);
                        btn.setPadding(4, 1, 4, 1);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMarginStart(3);
                        btn.setLayoutParams(lp);
                        
                        int bgColor = reactions.getOrDefault(currentUserId, false) ? Color.parseColor("#FF6B9D") : Color.parseColor("#E0E0E0");
                        int txtColor = reactions.getOrDefault(currentUserId, false) ? Color.WHITE : Color.BLACK;
                        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
                        btn.setTextColor(txtColor);
                        
                        btn.setOnClickListener(v -> toggleReaction(comment, emoji, currentUserId));
                        userReactionsContainer.addView(btn);
                    }
                }
            }
        }
    }

    private void showDeleteDialog(Comment comment) {
        new AlertDialog.Builder(context)
            .setTitle(R.string.delete_comment)
            .setMessage(R.string.confirm_delete_comment)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                FirebaseDatabase.getInstance().getReference("Forum/Posts").child(postId)
                    .child("Comments").child(comment.commentId).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, R.string.comment_deleted, Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void addReaction(Comment comment, String emoji, String currentUserId) {
        if (comment.reactions == null) comment.reactions = new java.util.HashMap<>();
        for (String otherEmoji : comment.reactions.keySet()) {
            comment.reactions.get(otherEmoji).remove(currentUserId);
            FirebaseDatabase.getInstance().getReference("Forum/Posts").child(postId)
                    .child("Comments/" + comment.commentId + "/reactions/" + otherEmoji + "/" + currentUserId).removeValue();
        }
        Map<String, Boolean> emojiReactions = comment.reactions.getOrDefault(emoji, new java.util.HashMap<>());
        emojiReactions.put(currentUserId, true);
        comment.reactions.put(emoji, emojiReactions);
        FirebaseDatabase.getInstance().getReference("Forum/Posts").child(postId)
                .child("Comments/" + comment.commentId + "/reactions/" + emoji + "/" + currentUserId).setValue(true);
        notifyDataSetChanged();
    }

    private void toggleReaction(Comment comment, String emoji, String currentUserId) {
        if (comment.reactions == null) comment.reactions = new java.util.HashMap<>();
        Map<String, Boolean> emojiReactions = comment.reactions.getOrDefault(emoji, new java.util.HashMap<>());
        if (emojiReactions.getOrDefault(currentUserId, false)) {
            emojiReactions.remove(currentUserId);
            FirebaseDatabase.getInstance().getReference("Forum/Posts").child(postId)
                    .child("Comments/" + comment.commentId + "/reactions/" + emoji + "/" + currentUserId).removeValue();
        } else {
            addReaction(comment, emoji, currentUserId);
            return;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvAuthor, tvDate;
        ImageView btnDelete, btnReply;
        LinearLayout layoutComment;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutComment = itemView.findViewById(R.id.layoutComment);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
            btnReply = itemView.findViewById(R.id.btnReplyComment);
        }
    }
}
