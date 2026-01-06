package com.example.coursemanagment.covoiturage.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coursemanagment.R;
import com.example.coursemanagment.covoiturage.models.Booking;
import java.util.ArrayList;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private Context context;
    private ArrayList<Booking> bookingList;

    public BookingAdapter(Context context, ArrayList<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvPassengerInitial, tvPassengerName, tvSeatsBooked, tvPhone;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerInitial = itemView.findViewById(R.id.tvPassengerInitial);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvSeatsBooked = itemView.findViewById(R.id.tvSeatsBooked);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }

        public void bind(Booking booking) {
            String initial = booking.passengerName != null && booking.passengerName.length() > 0 
                ? String.valueOf(booking.passengerName.charAt(0)).toUpperCase() 
                : "?";
            
            tvPassengerInitial.setText(initial);
            tvPassengerName.setText(booking.passengerName);
            tvSeatsBooked.setText("👥 " + booking.seatsBooked + " seat" + (booking.seatsBooked > 1 ? "s" : ""));
            tvPhone.setText("📞 " + booking.passengerPhone);
        }
    }
}

