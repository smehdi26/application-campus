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
import com.example.coursemanagment.covoiturage.models.Booking;
import com.example.coursemanagment.covoiturage.models.Ride;
import java.util.ArrayList;
import java.util.Map;

public class BookedRideAdapter extends RecyclerView.Adapter<BookedRideAdapter.BookedRideViewHolder> {

    private Context context;
    private ArrayList<Ride> rideList;
    private Map<String, Booking> bookingMap;
    private OnModifyClickListener modifyListener;
    private OnCancelClickListener cancelListener;

    public interface OnModifyClickListener {
        void onModifyClick(Ride ride, Booking booking);
    }

    public interface OnCancelClickListener {
        void onCancelClick(Ride ride, Booking booking);
    }

    public BookedRideAdapter(Context context, ArrayList<Ride> rideList, Map<String, Booking> bookingMap,
                            OnModifyClickListener modifyListener, OnCancelClickListener cancelListener) {
        this.context = context;
        this.rideList = rideList;
        this.bookingMap = bookingMap;
        this.modifyListener = modifyListener;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public BookedRideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booked_ride, parent, false);
        return new BookedRideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookedRideViewHolder holder, int position) {
        Ride ride = rideList.get(position);
        Booking booking = bookingMap.get(ride.id);
        
        if (booking != null) {
            holder.bind(ride, booking);
        }
    }

    @Override
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    public class BookedRideViewHolder extends RecyclerView.ViewHolder {
        TextView tvOwnerInitial, tvOwnerName, tvOwnerPhone;
        TextView tvDeparture, tvDestination;
        TextView tvDate, tvTime, tvVehicle, tvPrice, tvMySeats, tvSeatsLeft;
        Button btnModify, btnCancel;

        public BookedRideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOwnerInitial = itemView.findViewById(R.id.tvOwnerInitial);
            tvOwnerName = itemView.findViewById(R.id.tvOwnerName);
            tvOwnerPhone = itemView.findViewById(R.id.tvOwnerPhone);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvVehicle = itemView.findViewById(R.id.tvVehicle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvMySeats = itemView.findViewById(R.id.tvMySeats);
            tvSeatsLeft = itemView.findViewById(R.id.tvSeatsLeft);
            btnModify = itemView.findViewById(R.id.btnModifyBooking);
            btnCancel = itemView.findViewById(R.id.btnCancelBooking);
        }

        public void bind(Ride ride, Booking booking) {
            // Owner info
            String initial = ride.ownerName != null && ride.ownerName.length() > 0 
                ? String.valueOf(ride.ownerName.charAt(0)).toUpperCase() 
                : "?";
            tvOwnerInitial.setText(initial);
            tvOwnerName.setText(ride.ownerName != null ? ride.ownerName : "Unknown");
            tvOwnerPhone.setText("📞 " + ride.ownerPhone);

            // Route
            tvDeparture.setText(ride.departure);
            tvDestination.setText(ride.destination);

            // Details
            tvDate.setText(ride.date);
            tvTime.setText(ride.time);
            tvVehicle.setText(ride.vehicle);
            tvPrice.setText(String.format("%.1f TND", ride.price));
            tvMySeats.setText("Your seats: " + booking.seatsBooked);
            tvSeatsLeft.setText(ride.seatsLeft + " seats left");

            // Buttons
            btnModify.setOnClickListener(v -> {
                if (modifyListener != null) {
                    modifyListener.onModifyClick(ride, booking);
                }
            });

            btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancelClick(ride, booking);
                }
            });
        }
    }
}

