package com.example.coursemanagment;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.api.IMapController;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class LocationPickerDialog extends DialogFragment {

    public interface Listener {
        void onPicked(double lat, double lng);
    }

    private Listener listener;

    public LocationPickerDialog setListener(Listener l) {
        this.listener = l;
        return this;
    }

    private MapView mapView;
    private IMapController controller;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker pickedMarker;
    private GeoPoint pickedPoint;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = new Dialog(requireContext());
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_location_picker);
        d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = d.findViewById(R.id.mapPick);
        ImageButton btnClose = d.findViewById(R.id.btnClosePick);
        MaterialButton btnConfirm = d.findViewById(R.id.btnConfirmPick);
        TextView tvHint = d.findViewById(R.id.tvHintPick);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        controller = mapView.getController();

        GeoPoint esprit = new GeoPoint(36.8983, 10.1896);
        controller.setZoom(18.0);
        controller.setCenter(esprit);

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        myLocationOverlay.runOnFirstFix(() -> {
            GeoPoint me = myLocationOverlay.getMyLocation();
            if (me != null) {
                requireActivity().runOnUiThread(() -> controller.setCenter(me));
            }
        });

        // Tap to choose point
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                GeoPoint p = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
                setPicked(p);
                tvHint.setText("Picked: " + p.getLatitude() + ", " + p.getLongitude());
            }
            return false;
        });

        btnConfirm.setOnClickListener(v -> {
            if (pickedPoint != null && listener != null) {
                listener.onPicked(pickedPoint.getLatitude(), pickedPoint.getLongitude());
            }
            dismiss();
        });

        btnClose.setOnClickListener(v -> dismiss());

        return d;
    }

    private void setPicked(GeoPoint p) {
        pickedPoint = p;
        if (pickedMarker != null) mapView.getOverlays().remove(pickedMarker);

        pickedMarker = new Marker(mapView);
        pickedMarker.setPosition(p);
        pickedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(pickedMarker);

        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
