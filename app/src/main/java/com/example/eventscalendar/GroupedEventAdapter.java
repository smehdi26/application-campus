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

public class GroupedEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Object> items;
    private static final int VIEW_TYPE_MONTH = 0;
    private static final int VIEW_TYPE_EVENT = 1;

    public GroupedEventAdapter(List<Object> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return VIEW_TYPE_MONTH;
        } else {
            return VIEW_TYPE_EVENT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MONTH) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_header, parent, false);
            return new MonthViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_MONTH) {
            MonthViewHolder monthViewHolder = (MonthViewHolder) holder;
            monthViewHolder.month.setText((String) items.get(position));
        } else {
            EventViewHolder eventViewHolder = (EventViewHolder) holder;
            EventModel event = (EventModel) items.get(position);
            eventViewHolder.title.setText(event.getTitle());
            eventViewHolder.dateTime.setText(event.getDate() + " " + event.getTime());
            eventViewHolder.location.setText(event.getLocation());
            eventViewHolder.category.setText(event.getCategory());

            // Change la couleur du badge selon la catégorie
            String category = event.getCategory();
            if (category.equals("Soutenances")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_soutenances);
                eventViewHolder.category.setTextColor(Color.parseColor("#FFA000"));
            } else if (category.equals("Exams") || category.equals("Examens")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_exams);
                eventViewHolder.category.setTextColor(Color.parseColor("#D32F2F"));
            } else if (category.equals("Clubs")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_clubs);
                eventViewHolder.category.setTextColor(Color.parseColor("#00796B"));
            } else if (category.equals("Conferences") || category.equals("Conférences")) {
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_tag_conferences);
                eventViewHolder.category.setTextColor(Color.parseColor("#1976D2"));
            } else {
                // Default style
                eventViewHolder.category.setBackgroundResource(R.drawable.bg_badge_gray);
                eventViewHolder.category.setTextColor(Color.GRAY);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<Object> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, dateTime, location, category;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvEventTitle);
            dateTime = itemView.findViewById(R.id.tvEventDateTime);
            location = itemView.findViewById(R.id.tvEventLocation);
            category = itemView.findViewById(R.id.tvCategoryBadge);
        }
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView month;
        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            month = itemView.findViewById(R.id.tvMonthHeader);
        }
    }
}