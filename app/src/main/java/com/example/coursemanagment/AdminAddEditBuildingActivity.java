package com.example.coursemanagment;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AdminAddEditBuildingActivity extends AppCompatActivity {

    // ====== CHANGE THIS ======
    // Create a free ImgBB key and paste it here
    // https://api.imgbb.com/
    private static final String IMGBB_API_KEY = "bf45a67e165fe14899cfb108b14ed760";

    private static final int REQ_LOC = 7001;

    // Firebase
    private DatabaseReference buildingsRef;

    // UI
    private TextInputEditText etName, etDesc, etFloors, etLat, etLng;
    private MaterialAutoCompleteTextView ddType, ddFaculty;
    private MaterialButton btnAddFaculty, btnPickLocation, btnAddImage, btnSave, btnDelete;
    private ChipGroup chipfacilities;

    // State
    private String buildingId = null;
    private final List<String> selectedfacilities = new ArrayList<>();
    private final List<String> imageUrls = new ArrayList<>();

    private double pickedLat = Double.NaN;
    private double pickedLng = Double.NaN;

    private final String[] TYPE_ENUM = new String[]{
            "faculty", "lab", "library", "cafeteria", "sports", "admin"
    };

    // Example faculty enum list – edit names to match your school
    private final String[] FACULTY_ENUM = new String[]{
            "Computer Science",
            "Engineering",
            "Business",
            "Design",
            "Networks",
            "Data/AI",
            "Mathematics",
            "Languages"
    };

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                uploadToImgBB(uri);
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid user agent
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_admin_add_edit_building);

        buildingsRef = FirebaseDatabase.getInstance().getReference("Buildings");

        bindViews();
        setupDropdowns();
        setupListeners();

        // Are we editing?
        buildingId = getIntent().getStringExtra("buildingId");
        if (buildingId != null && !buildingId.trim().isEmpty()) {
            loadBuilding(buildingId);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void bindViews() {
        etName = findViewById(R.id.etName);
        etDesc = findViewById(R.id.etDesc);
        etFloors = findViewById(R.id.etFloors);
        etLat = findViewById(R.id.etLat);
        etLng = findViewById(R.id.etLng);

        ddType = findViewById(R.id.ddType);
        ddFaculty = findViewById(R.id.ddFaculty);

        btnAddFaculty = findViewById(R.id.btnAddFaculty);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnAddImage = findViewById(R.id.btnAddImage);

        chipfacilities = findViewById(R.id.chipFaculties);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void setupDropdowns() {
        ddType.setSimpleItems(TYPE_ENUM);
        ddFaculty.setSimpleItems(FACULTY_ENUM);
    }

    private void setupListeners() {
        btnAddFaculty.setOnClickListener(v -> {
            String selected = ddFaculty.getText() == null ? "" : ddFaculty.getText().toString().trim();
            if (selected.isEmpty()) {
                toast("Choose a faculty first");
                return;
            }
            addFacultyChip(selected);
            ddFaculty.setText("");
        });

        btnPickLocation.setOnClickListener(v -> showLocationPickerDialog());

        btnAddImage.setOnClickListener(v -> {
            if (IMGBB_API_KEY == null || IMGBB_API_KEY.trim().isEmpty() || IMGBB_API_KEY.contains("PASTE_")) {
                toast("Add your ImgBB API key in AdminAddEditBuildingActivity.java first");
                return;
            }
            pickImageLauncher.launch("image/*");
        });

        btnSave.setOnClickListener(v -> saveBuilding());

        btnDelete.setOnClickListener(v -> {
            if (buildingId == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Delete building?")
                    .setMessage("This will remove the building from the database.")
                    .setPositiveButton("Delete", (d, which) -> deleteBuilding())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ---------- facilities Chips ----------

    private void addFacultyChip(String name) {
        // avoid duplicates (case-insensitive)
        for (String s : selectedfacilities) {
            if (s.equalsIgnoreCase(name)) {
                toast("Already added");
                return;
            }
        }
        selectedfacilities.add(name);

        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(android.R.color.darker_gray);
        chip.setTextColor(Color.WHITE);

        chip.setOnCloseIconClickListener(v -> {
            chipfacilities.removeView(chip);
            // remove from list
            for (int i = 0; i < selectedfacilities.size(); i++) {
                if (selectedfacilities.get(i).equalsIgnoreCase(name)) {
                    selectedfacilities.remove(i);
                    break;
                }
            }
        });

        chipfacilities.addView(chip);
    }

    private void setfacilitiesChips(List<String> facilities) {
        chipfacilities.removeAllViews();
        selectedfacilities.clear();
        if (facilities == null) return;
        for (String f : facilities) {
            if (f != null && !f.trim().isEmpty()) addFacultyChip(f.trim());
        }
    }

    // ---------- Load / Save / Delete ----------

    private void loadBuilding(String id) {
        buildingsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Building b = snapshot.getValue(Building.class);
                if (b == null) {
                    toast("Building not found");
                    finish();
                    return;
                }

                buildingId = snapshot.getKey();

                if (b.name != null) etName.setText(b.name);
                if (b.description != null) etDesc.setText(b.description);
                ddType.setText(b.type == null ? "" : b.type, false);

                etFloors.setText(String.valueOf(b.floors));

                pickedLat = b.lat;
                pickedLng = b.lng;
                etLat.setText(String.format(Locale.US, "%.6f", b.lat));
                etLng.setText(String.format(Locale.US, "%.6f", b.lng));

                setfacilitiesChips(b.facilities);

                imageUrls.clear();
                if (b.images != null) imageUrls.addAll(b.images);

                updateImagesCount();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                toast("Failed to load building");
            }
        });
    }

    private void saveBuilding() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String type = ddType.getText() == null ? "" : ddType.getText().toString().trim();
        String desc = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();
        String floorsStr = etFloors.getText() == null ? "" : etFloors.getText().toString().trim();

        if (name.isEmpty()) { toast("Name required"); return; }
        if (type.isEmpty()) { toast("Type required"); return; }

        // validate type is enum
        if (!isInEnum(type, TYPE_ENUM)) {
            toast("Invalid type");
            return;
        }

        int floors = 0;
        try {
            floors = floorsStr.isEmpty() ? 0 : Integer.parseInt(floorsStr);
        } catch (Exception e) {
            toast("Floors must be a number");
            return;
        }

        if (Double.isNaN(pickedLat) || Double.isNaN(pickedLng)) {
            toast("Pick location on map");
            return;
        }

        // Build object
        Building b = new Building();
        b.id = buildingId; // key
        b.name = name;
        b.type = type.toLowerCase(Locale.US);
        b.description = desc;
        b.floors = floors;
        b.lat = pickedLat;
        b.lng = pickedLng;

        // facilities saved in "facilities" in your project (same field used in Map popup)
        b.facilities = new ArrayList<>(selectedfacilities);

        // images
        b.images = new ArrayList<>(imageUrls);

        // IMPORTANT: no QR CODE field stored

        DatabaseReference target;
        if (buildingId == null || buildingId.trim().isEmpty()) {
            target = buildingsRef.push();
            buildingId = target.getKey();
            b.id = buildingId;
        } else {
            target = buildingsRef.child(buildingId);
        }

        setLoading(true);
        target.setValue(b, (error, ref) -> {
            setLoading(false);
            if (error != null) {
                toast("Save failed: " + error.getMessage());
            } else {
                toast("Saved");
                finish();
            }
        });
    }

    private void deleteBuilding() {
        if (buildingId == null) return;
        setLoading(true);
        buildingsRef.child(buildingId).removeValue((error, ref) -> {
            setLoading(false);
            if (error != null) toast("Delete failed: " + error.getMessage());
            else {
                toast("Deleted");
                finish();
            }
        });
    }

    private boolean isInEnum(String v, String[] arr) {
        for (String s : arr) {
            if (s.equalsIgnoreCase(v)) return true;
        }
        return false;
    }

    private void setLoading(boolean loading) {
        btnSave.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
        btnPickLocation.setEnabled(!loading);
        btnAddImage.setEnabled(!loading);
        btnAddFaculty.setEnabled(!loading);
    }

    // ---------- Location Picker Dialog (popup map) ----------

    private void showLocationPickerDialog() {
        ensureLocationPermission();

        MapView mv = new MapView(this);
        mv.setTileSource(TileSourceFactory.MAPNIK);
        mv.setMultiTouchControls(true);
        mv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(420)
        ));

        IMapController controller = mv.getController();

        // Start point: picked -> last known -> ESPRIT
        GeoPoint start = getBestStartPoint();
        controller.setCenter(start);
        controller.setZoom(18.0);

        // Marker
        Marker marker = new Marker(mv);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setPosition(start);
        marker.setTitle("Selected location");
        mv.getOverlays().add(marker);

        // ✅ Show user location + auto-center on first GPS fix
        MyLocationNewOverlay locOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mv);
        locOverlay.enableMyLocation();
        mv.getOverlays().add(locOverlay);

        locOverlay.runOnFirstFix(() -> {
            GeoPoint me = locOverlay.getMyLocation();
            if (me != null) {
                runOnUiThread(() -> {
                    controller.setCenter(me);
                    controller.setZoom(19.0);
                    marker.setPosition(me);   // put marker on your current location too
                    mv.invalidate();
                });
            }
        });

        // Tap / long press to move marker
        org.osmdroid.views.overlay.MapEventsOverlay events =
                new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
                    @Override
                    public boolean singleTapConfirmedHelper(GeoPoint p) {
                        marker.setPosition(p);
                        mv.invalidate();
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(GeoPoint p) {
                        marker.setPosition(p);
                        mv.invalidate();
                        return true;
                    }
                });
        mv.getOverlays().add(events);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Pick building location")
                .setView(mv)
                .setPositiveButton("Use this location", (d, which) -> {
                    GeoPoint p = marker.getPosition();
                    pickedLat = p.getLatitude();
                    pickedLng = p.getLongitude();
                    etLat.setText(String.format(Locale.US, "%.6f", pickedLat));
                    etLng.setText(String.format(Locale.US, "%.6f", pickedLng));

                    // cleanup overlay
                    locOverlay.disableMyLocation();
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    locOverlay.disableMyLocation();
                })
                .create();

        dialog.show();
    }

    private GeoPoint getBestStartPoint() {
        // if already picked, start there
        if (!Double.isNaN(pickedLat) && !Double.isNaN(pickedLng)) {
            return new GeoPoint(pickedLat, pickedLng);
        }
        // try last known user location
        GeoPoint me = getLastKnownPoint();
        if (me != null) return me;

        // fallback: ESPRIT
        return new GeoPoint(36.8983, 10.1896);
    }

    private GeoPoint getLastKnownPoint() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm == null) return null;

            Location best = null;
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l == null) continue;
                if (best == null || l.getAccuracy() < best.getAccuracy()) best = l;
            }
            if (best == null) return null;
            return new GeoPoint(best.getLatitude(), best.getLongitude());
        } catch (Exception ignored) {
            return null;
        }
    }

    private void ensureLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) return;

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOC
        );
    }

    // ---------- ImgBB Upload ----------

    private void uploadToImgBB(Uri uri) {
        setLoading(true);
        toast("Uploading...");

        new Thread(() -> {
            try {
                String base64 = uriToBase64(uri);
                String payload = "key=" + URLEncoder.encode(IMGBB_API_KEY, "UTF-8")
                        + "&image=" + URLEncoder.encode(base64, "UTF-8");

                URL url = new URL("https://api.imgbb.com/1/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String resp = readAll(is);

                if (code < 200 || code >= 300) {
                    throw new RuntimeException("HTTP " + code + ": " + resp);
                }

                JSONObject root = new JSONObject(resp);
                JSONObject data = root.getJSONObject("data");
                String imageUrl = data.getString("url");

                runOnUiThread(() -> {
                    imageUrls.add(imageUrl);
                    updateImagesCount();
                    toast("Image uploaded");
                    setLoading(false);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    toast("Upload failed: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    private void updateImagesCount() {
        // Your XML has tvImagesCount
        try {
            TextInputEditText dummy = null; // keep compiler calm about imports
            // just update the textview by id
            android.widget.TextView tv = findViewById(R.id.tvImagesCount);
            tv.setText(imageUrls.size() + " images");
        } catch (Exception ignored) {}
    }

    private String uriToBase64(Uri uri) throws Exception {
        ContentResolver cr = getContentResolver();
        InputStream is = cr.openInputStream(uri);
        if (is == null) throw new RuntimeException("Cannot open image");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        is.close();

        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private String readAll(InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    // ---------- Misc ----------

    private void toast(String msg) {
        Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.BOTTOM, 0, dp(90));
        t.show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // Permission result (location picker)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOC) {
            boolean ok = false;
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) ok = true;
            if (!ok) toast("Location permission denied (map picker will use ESPRIT default)");
        }
    }
}
