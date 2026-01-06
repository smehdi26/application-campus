package com.example.coursemanagment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.MentionViewHolder> {

    private List<User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public MentionAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateUsers(List<User> newUsers) {
        this.users.clear();
        this.users.addAll(newUsers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MentionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_mention, parent, false);
        return new MentionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MentionViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvUserName.setText(user.firstName + " " + user.lastName);
        holder.tvUserRole.setText(user.role);
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class MentionViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserRole;

        MentionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvMentionUserName);
            tvUserRole = itemView.findViewById(R.id.tvMentionUserRole);
        }
    }
}
