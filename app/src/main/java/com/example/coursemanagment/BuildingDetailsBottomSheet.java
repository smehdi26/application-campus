package com.example.coursemanagment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class BuildingDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String A_ID = "id";
    private static final String A_NAME = "name";
    private static final String A_TYPE = "type";
    private static final String A_DESC = "desc";
    private static final String A_FLOORS = "floors";
    private static final String A_LAT = "lat";
    private static final String A_LNG = "lng";
    private static final String A_FAC = "fac";
    private static final String A_IMG = "img";

    public static BuildingDetailsBottomSheet newInstance(Building b) {
        BuildingDetailsBottomSheet f = new BuildingDetailsBottomSheet();
        Bundle args = new Bundle();

        args.putString(A_ID, b.id);
        args.putString(A_NAME, b.name);
        args.putString(A_TYPE, b.type);
        args.putString(A_DESC, b.description);
        args.putInt(A_FLOORS, b.floors);
        args.putDouble(A_LAT, b.lat);
        args.putDouble(A_LNG, b.lng);

        // ✅ NEW FIELD NAME
        if (b.facilities != null) args.putStringArrayList(A_FAC, new ArrayList<>(b.facilities));
        if (b.images != null) args.putStringArrayList(A_IMG, new ArrayList<>(b.images));

        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottomsheet_building_details, container, false);

        Bundle a = getArguments();
        if (a == null) return v;

        String id = a.getString(A_ID, "");
        String name = a.getString(A_NAME, "");
        String type = a.getString(A_TYPE, "");
        String desc = a.getString(A_DESC, "");
        int floors = a.getInt(A_FLOORS, 0);
        double lat = a.getDouble(A_LAT, 0);
        double lng = a.getDouble(A_LNG, 0);

        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvType = v.findViewById(R.id.tvType);
        TextView tvDesc = v.findViewById(R.id.tvDescription);
        TextView tvFloors = v.findViewById(R.id.tvFloors);
        View typeBadge = v.findViewById(R.id.viewTypeBadge);
        ImageButton btnClose = v.findViewById(R.id.btnClose);
        MaterialButton btnMoreInfo = v.findViewById(R.id.btnMoreInfo);

        tvName.setText(name);
        tvType.setText(type == null ? "" : capitalize(type));
        tvDesc.setText(desc);
        tvFloors.setText("Floors: " + floors);

        // Type badge tint
        int colorRes = typeBgColorRes(type);
        typeBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes));

        // Images
        ViewPager2 pager = v.findViewById(R.id.pagerImages);
        ImagePagerAdapter adapter = new ImagePagerAdapter(requireContext());
        pager.setAdapter(adapter);
        ArrayList<String> imgs = a.getStringArrayList(A_IMG);
        adapter.setUrls(imgs);

        // ✅ Faculties/Facilities chips (fixed ID + fallback)
        ChipGroup chipGroup = v.findViewById(R.id.chipFacilities);
        if (chipGroup == null) {
            // fallback if your XML uses lowercase id chipfaculties
            chipGroup = v.findViewById(R.id.chipFacilities);
        }

        if (chipGroup != null) {
            chipGroup.removeAllViews();
            ArrayList<String> fac = a.getStringArrayList(A_FAC);
            if (fac != null) {
                for (String fStr : fac) {
                    if (fStr == null || fStr.trim().isEmpty()) continue;
                    Chip c = new Chip(requireContext());
                    c.setText(fStr.trim());
                    c.setChipBackgroundColorResource(R.color.bg_light_gray);
                    chipGroup.addView(c);
                }
            }
        }

        btnClose.setOnClickListener(x -> dismiss());

        // keep this or replace later
        btnMoreInfo.setOnClickListener(x -> dismiss());

        return v;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private int typeBgColorRes(String type) {
        if (type == null) return R.color.bg_light_gray;
        switch (type.toLowerCase()) {
            case "library": return R.color.type_library_bg;
            case "lab": return R.color.type_lab_bg;
            case "admin": return R.color.type_admin_bg;
            case "cafeteria": return R.color.type_cafeteria_bg;
            case "sports": return R.color.type_sports_bg;
            case "faculty":
            default: return R.color.type_faculty_bg;
        }
    }
}
