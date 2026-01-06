package com.example.coursemanagment.covoiturage.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.covoiturage.models.Ride;
import java.util.ArrayList;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private Context context;
    private ArrayList<Ride> rideList;
    private OnRideClickListener listener;
    private OnRideDeleteListener deleteListener;
    private boolean isAdminMode = false;

    public interface OnRideClickListener {
        void onRideClick(Ride ride);
    }

    public interface OnRideDeleteListener {
        void onRideDelete(Ride ride);
    }

    public RideAdapter(Context context, ArrayList<Ride> rideList, OnRideClickListener listener, OnRideDeleteListener deleteListener) {
        this.context = context;
        this.rideList = rideList;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    public void setAdminMode(boolean adminMode) {
        this.isAdminMode = adminMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ride_card, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rideList.get(position);
        holder.bind(ride);
    }

    @Override
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    public class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverInitial, tvDriverName, tvDriverRating, tvDriverTrips, tvPrice;
        TextView tvDeparture, tvDestination;
        TextView tvDate, tvTime, tvSeats, tvVehicle;
        Button btnBook;
        Button btnDelete;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverInitial = itemView.findViewById(R.id.tvDriverInitial);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvDriverRating = itemView.findViewById(R.id.tvDriverRating);
            tvDriverTrips = itemView.findViewById(R.id.tvDriverTrips);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            tvVehicle = itemView.findViewById(R.id.tvVehicle);
            btnBook = itemView.findViewById(R.id.btnBookRide);
            btnDelete = itemView.findViewById(R.id.btnDeleteRide);
        }

        public void bind(Ride ride) {
            // Set driver initial
            String initial = ride.ownerName != null && ride.ownerName.length() > 0 
                ? String.valueOf(ride.ownerName.charAt(0)).toUpperCase() 
                : "?";
            tvDriverInitial.setText(initial);
            
            tvDriverName.setText(ride.ownerName != null ? ride.ownerName : "Unknown");
            // Hide rating and trips for now (can be added later if needed)
            tvDriverRating.setVisibility(View.GONE);
            tvDriverTrips.setVisibility(View.GONE);
            tvPrice.setText(String.format("%.1f TND", ride.price));
            tvDeparture.setText(ride.departure);
            tvDestination.setText(ride.destination);
            tvDate.setText(ride.date);
            tvTime.setText(ride.time);
            if (ride.isFull()) {
                tvSeats.setText("Full");
                btnBook.setEnabled(false);
                btnBook.setText("Full");
            } else {
                tvSeats.setText(ride.seatsLeft + " seats available");
                btnBook.setEnabled(true);
                btnBook.setText("Book This Ride");
            }
            tvVehicle.setText(ride.vehicle);

            btnBook.setOnClickListener(v -> {
                if (listener != null && !ride.isFull()) {
                    listener.onRideClick(ride);
                }
            });

            // Admin-only delete control
            if (btnDelete != null) {
                btnDelete.setVisibility(isAdminMode ? View.VISIBLE : View.GONE);
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onRideDelete(ride);
                    }
                });
            }
        }
    }
}

