package com.example.eventscalendar;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.example.coursemanagment.R;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<EventModel> events;

    public EventAdapter(List<EventModel> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = events.get(position);
        holder.title.setText(event.getTitle());
        holder.time.setText(event.getTime());
        holder.location.setText(event.getLocation());
        holder.category.setText(event.getCategory());

        // Change la couleur du badge selon la catégorie
        String category = event.getCategory();
        if (category.equals("Soutenances")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_soutenances);
            holder.category.setTextColor(Color.parseColor("#FFA000"));
        } else if (category.equals("Exams") || category.equals("Examens")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_exams);
            holder.category.setTextColor(Color.parseColor("#D32F2F"));
        } else if (category.equals("Clubs")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_clubs);
            holder.category.setTextColor(Color.parseColor("#00796B"));
        } else if (category.equals("Conferences") || category.equals("Conférences")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_conferences);
            holder.category.setTextColor(Color.parseColor("#1976D2"));
        } else {
            // Default style
            holder.category.setBackgroundResource(R.drawable.bg_badge_gray);
            holder.category.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateEvents(List<EventModel> newEvents) {
        this.events.clear();
        this.events.addAll(newEvents);
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, time, location, category;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvEventTitle);
            time = itemView.findViewById(R.id.tvEventTime);
            location = itemView.findViewById(R.id.tvEventLocation);
            category = itemView.findViewById(R.id.tvCategoryBadge);
        }
    }
}
