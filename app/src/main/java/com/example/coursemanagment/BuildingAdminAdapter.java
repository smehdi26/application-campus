package com.example.coursemanagment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Locale;

public class BuildingAdminAdapter extends RecyclerView.Adapter<BuildingAdminAdapter.VH> {

    public interface Listener {
        void onEdit(Building b);
        void onDelete(Building b);
        void onOpen(Building b);
    }

    private final List<Building> data;
    private final Context ctx;
    private final Listener listener;

    public BuildingAdminAdapter(List<Building> data, Context ctx, Listener listener) {
        this.data = data;
        this.ctx = ctx;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_building_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Building b = data.get(position);

        h.tvName.setText(b.name == null ? "Unnamed building" : b.name);

        String type = (b.type == null) ? "unknown" : b.type.trim().toLowerCase(Locale.US);
        h.chipType.setText(type);

        int typeColor = getColorForType(type);
        h.chipType.setChipBackgroundColor(ColorStateList.valueOf(lighten(typeColor)));
        h.chipType.setTextColor(typeColor);

        String thumb = (b.images != null && !b.images.isEmpty()) ? b.images.get(0) : null;
        if (thumb != null && !thumb.trim().isEmpty()) {
            Glide.with(ctx).load(thumb).centerCrop().into(h.ivThumb);
            h.ivThumb.clearColorFilter();
        } else {
            h.ivThumb.setImageResource(R.drawable.ic_place);
            h.ivThumb.setColorFilter(ContextCompat.getColor(ctx, R.color.esprit_red));
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(b));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(b));
        h.root.setOnClickListener(v -> listener.onOpen(b));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View root;
        ImageView ivThumb;
        TextView tvName;
        Chip chipType;
        ImageButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            ivThumb = itemView.findViewById(R.id.ivThumb);
            tvName = itemView.findViewById(R.id.tvName);
            chipType = itemView.findViewById(R.id.chipType);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private int getColorForType(String type) {
        switch (type) {
            case "faculty": return Color.parseColor("#E30613");
            case "library": return Color.parseColor("#1E40AF");
            case "lab": return Color.parseColor("#D97706");
            case "cafeteria": return Color.parseColor("#059669");
            case "sports": return Color.parseColor("#DB2777");
            case "admin": return Color.parseColor("#4F46E5");
            default: return Color.parseColor("#E30613");
        }
    }

    private int lighten(int color) {
        int r = Math.min(255, (int)(Color.red(color) * 0.12 + 255 * 0.88));
        int g = Math.min(255, (int)(Color.green(color) * 0.12 + 255 * 0.88));
        int b = Math.min(255, (int)(Color.blue(color) * 0.12 + 255 * 0.88));
        return Color.rgb(r, g, b);
    }
}
