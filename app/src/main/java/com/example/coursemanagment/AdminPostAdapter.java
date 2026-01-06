package com.example.coursemanagment;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.MyViewHolder> {
    Context context;
    ArrayList<Post> list;
    AdminForumActivity activity;

    public AdminPostAdapter(Context context, ArrayList<Post> list, AdminForumActivity activity) {
        this.context = context;
        this.list = list;
        this.activity = activity;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.activity_item_admin_post, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Post post = list.get(position);

        // --- Remplissage des données ---
        holder.tvTitle.setText(post.title);
        holder.tvAuthor.setText(String.format(Locale.getDefault(), "%s (%s)", post.authorName, post.authorRole));
        holder.tvSubject.setText(post.subject != null ? post.subject : "General");
        holder.tvStatus.setText(post.status);
        holder.tvCommentCount.setText(String.format(Locale.getDefault(), "%d %s", post.commentCount, context.getString(R.string.comments)));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(post.timestamp)));

        // Couleurs de statut
        int statusColor = 0xFF757575;
        if ("PENDING".equals(post.status)) statusColor = 0xFFFF9800;
        else if ("PUBLISHED".equals(post.status)) statusColor = 0xFF4CAF50;
        else if ("HIDDEN".equals(post.status)) statusColor = 0xFFF44336;
        holder.tvStatus.setTextColor(statusColor);

        // Visibilité des boutons
        if ("PENDING".equals(post.status)) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnHide.setVisibility(View.GONE);
        } else if ("PUBLISHED".equals(post.status)) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnHide.setVisibility(View.VISIBLE);
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnHide.setVisibility(View.GONE);
        }

        // --- Listeners des Boutons Admin (Actions) ---
        holder.btnApprove.setOnClickListener(v -> {
            activity.approvePost(post);
        });
        holder.btnHide.setOnClickListener(v -> {
            activity.hidePost(post);
        });
        holder.btnDelete.setOnClickListener(v -> {
            activity.deletePost(post);
        });

        // --- LA SOLUTION MULTI-CLICK ---
        View.OnClickListener openDetailsListener = v -> {
            Log.d("AdminPostAdapter", "CLICK DETECTED on post: " + post.title);
            try {
                Intent intent = new Intent(context, PostDetailsActivity.class);
                intent.putExtra("post", post);
                intent.putExtra("postId", post.postId);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("AdminPostAdapter", "Erreur", e);
            }
        };

        holder.cardView.setOnClickListener(openDetailsListener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvAuthor, tvSubject, tvStatus, tvDate, tvCommentCount;
        Button btnApprove, btnHide, btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardAdminPost);
            tvTitle = itemView.findViewById(R.id.tvAdminPostTitle);
            tvAuthor = itemView.findViewById(R.id.tvAdminPostAuthor);
            tvSubject = itemView.findViewById(R.id.tvAdminPostSubject);
            tvStatus = itemView.findViewById(R.id.tvAdminPostStatus);
            tvDate = itemView.findViewById(R.id.tvAdminPostDate);
            tvCommentCount = itemView.findViewById(R.id.tvAdminPostComments);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnHide = itemView.findViewById(R.id.btnHide);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
