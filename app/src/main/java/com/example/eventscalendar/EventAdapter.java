package com.example.eventscalendar;

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
        if (event.getCategory().equals("Defense")) {
            holder.category.setBackgroundResource(R.drawable.bg_tag_defense);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
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
