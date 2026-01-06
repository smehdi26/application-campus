package com.example.coursemanagment.covoiturage.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.covoiturage.models.Ride;
import java.util.ArrayList;

public class RecentRideAdapter extends RecyclerView.Adapter<RecentRideAdapter.RecentRideViewHolder> {

    private Context context;
    private ArrayList<Ride> rideList;

    public RecentRideAdapter(Context context, ArrayList<Ride> rideList) {
        this.context = context;
        this.rideList = rideList;
    }

    @NonNull
    @Override
    public RecentRideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_ride, parent, false);
        return new RecentRideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentRideViewHolder holder, int position) {
        Ride ride = rideList.get(position);
        holder.bind(ride);
    }

    @Override
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    public class RecentRideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvPrice, tvDateTime, tvDeparture, tvDestination, tvSeats;

        public RecentRideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvSeats = itemView.findViewById(R.id.tvSeats);
        }

        public void bind(Ride ride) {
            tvDriverName.setText(ride.ownerName != null ? ride.ownerName : "Unknown");
            tvPrice.setText(String.format("%.1f TND", ride.price));
            tvDateTime.setText(ride.date + " at " + ride.time);
            tvDeparture.setText("📍 " + ride.departure);
            tvDestination.setText(ride.destination);
            if (ride.isFull()) {
                tvSeats.setText("👥 Full");
            } else {
                tvSeats.setText("👥 " + ride.seatsLeft + " seats available");
            }
        }
    }
}

