package com.example.coursemanagment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.coursemanagment.covoiturage.activities.CovoiturageActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsActivity extends AppCompatActivity {

    private static final int REQ_LOC = 1001;

    // Drawer
    private DrawerLayout drawerLayout;

    // Drawer profile views
    private TextView tvFullName, tvEmail, tvRole;
    private View btnEditProfile, btnMyCourses, btnManageUsers, btnManageClasses, btnLogout;

    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;

    private EditText etSearch;
    private ChipGroup chipGroup;

    private View popupCard;
    private TextView tvPopupName, tvPopupType, tvPopupDesc, tvPopupMeta;
    private ImageButton btnPopupClose;
    private MaterialButton btnViewDetails;
    private MaterialButton btnGetDirections;

    private ImageButton btnManageBuildings;

    private MaterialCardView routeCard;
    private TextView tvRouteSummary, tvRouteSteps;
    private MaterialButton btnClearRoute;

    private final List<Building> allBuildings = new ArrayList<>();
    private final List<Marker> buildingMarkers = new ArrayList<>();

    private Marker selectedMarker = null;
    private Building selectedBuilding = null;

    private Polyline routePolyline = null;
    private Polyline routeShadow = null;
    private boolean routingMode = false;

    private final ExecutorService netExec = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ---------- QR launcher ----------
    private final ActivityResultLauncher<Intent> qrLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                String qr = result.getData().getStringExtra(QrScannerActivity.EXTRA_QR);
                if (qr == null || qr.trim().isEmpty()) return;
                openBuildingByQr(qr.trim());
            }
    );

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_maps);

        // Drawer init
        drawerLayout = findViewById(R.id.drawerLayout);
        View openDrawer = findViewById(R.id.btnOpenDrawer);
        if (openDrawer == null) openDrawer = findViewById(R.id.header);
        if (openDrawer != null) {
            openDrawer.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        setupNavbar();

        mapView = findViewById(R.id.mapView);
        etSearch = findViewById(R.id.etSearch);
        chipGroup = findViewById(R.id.chipGroup);

        if (chipGroup != null) chipGroup.check(R.id.chipAll);

        popupCard = findViewById(R.id.popupCard);
        tvPopupName = findViewById(R.id.tvPopupName);
        tvPopupType = findViewById(R.id.tvPopupType);
        tvPopupDesc = findViewById(R.id.tvPopupDesc);
        tvPopupMeta = findViewById(R.id.tvPopupMeta);
        btnPopupClose = findViewById(R.id.btnPopupClose);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        btnGetDirections = findViewById(R.id.btnGetDirections);

        routeCard = findViewById(R.id.routeCard);
        tvRouteSummary = findViewById(R.id.tvRouteSummary);
        tvRouteSteps = findViewById(R.id.tvRouteSteps);
        btnClearRoute = findViewById(R.id.btnClearRoute);

        View btnManageBuildingsCard = findViewById(R.id.btnManageBuildingsCard);
        btnManageBuildings = findViewById(R.id.btnManageBuildings);

        if (btnManageBuildingsCard != null) btnManageBuildingsCard.setVisibility(View.GONE);

        if (btnManageBuildings != null) {
            btnManageBuildings.setOnClickListener(v ->
                    startActivity(new Intent(MapsActivity.this, AdminBuildingListActivity.class))
            );
        }

        btnPopupClose.setOnClickListener(v -> hidePopup(true));

        btnViewDetails.setOnClickListener(v -> {
            if (selectedBuilding != null) {
                BuildingDetailsBottomSheet.newInstance(selectedBuilding)
                        .show(getSupportFragmentManager(), "building_details");
            }
        });

        if (btnGetDirections != null) {
            btnGetDirections.setOnClickListener(v -> {
                Building b = selectedBuilding;
                if (b == null) return;

                hidePopup(false);
                startRoutingTo(b);
            });
        }

        if (btnClearRoute != null) {
            btnClearRoute.setOnClickListener(v -> clearRoute());
        }

        findViewById(R.id.btnQr).setOnClickListener(v ->
                qrLauncher.launch(new Intent(this, QrScannerActivity.class))
        );

        // Drawer profile setup
        setupDrawerProfile();

        setupAdminManageButton();
        setupSearchAndFilters();
        setupMap();
        listenBuildingsFromFirebase();
    }

    // ===================== Drawer Profile =====================
    private void setupDrawerProfile() {
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnMyCourses = findViewById(R.id.btnMyCourses);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageClasses = findViewById(R.id.btnManageClasses);
        btnLogout = findViewById(R.id.btnLogout);

        // If drawer_profile isn't included for some reason, these can be null
        if (btnLogout == null) return;

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                closeDrawer();
            });
        }

        if (btnMyCourses != null) {
            btnMyCourses.setOnClickListener(v -> {
                startActivity(new Intent(this, CoursesActivity.class));
                closeDrawer();
            });
        }

        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                startActivity(new Intent(this, AllUsersActivity.class));
                closeDrawer();
            });
        }

        if (btnManageClasses != null) {
            btnManageClasses.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminClassListActivity.class));
                closeDrawer();
            });
        }

        loadUserProfileAndRoleIntoDrawer();
    }

    private void loadUserProfileAndRoleIntoDrawer() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Users");
        String uid = currentUser.getUid();

        mDatabase.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                User user = snapshot.getValue(User.class);
                if (user == null) return;

                if (tvFullName != null) tvFullName.setText(user.firstName + " " + user.lastName);
                if (tvEmail != null) tvEmail.setText(user.email);
                if (tvRole != null) tvRole.setText(user.role);

                boolean isAdmin = user.role != null && user.role.equalsIgnoreCase("Admin");

                if (btnManageUsers != null) btnManageUsers.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                if (btnManageClasses != null) btnManageClasses.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                if (btnMyCourses != null) btnMyCourses.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void closeDrawer() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
    }

    // ===================== Map =====================
    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        GeoPoint esprit = new GeoPoint(36.8983, 10.1896);
        mapController.setZoom(17.0);
        mapController.setCenter(esprit);

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        myLocationOverlay.runOnFirstFix(() -> {
            GeoPoint me = myLocationOverlay.getMyLocation();
            if (me != null) {
                runOnUiThread(() -> {
                    mapController.setCenter(me);
                    mapController.setZoom(18.0);
                    mapView.invalidate();
                });
            }
        });

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            if (myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
            }
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOC);
    }

    // ---------- Firebase ----------
    private void listenBuildingsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Buildings");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBuildings.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Building b = ds.getValue(Building.class);
                    if (b == null) continue;

                    if (b.id == null || b.id.isEmpty()) b.id = ds.getKey();
                    if (b.facilities == null) b.facilities = new ArrayList<>();
                    if (b.images == null) b.images = new ArrayList<>();

                    allBuildings.add(b);
                }
                renderMarkers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, "Failed to load buildings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- Markers ----------
    private void renderMarkers() {
        for (Marker m : buildingMarkers) mapView.getOverlays().remove(m);
        buildingMarkers.clear();

        for (Building b : filteredBuildings()) {
            Marker m = new Marker(mapView);
            m.setPosition(new GeoPoint(b.lat, b.lng));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(b.name);
            m.setRelatedObject(b);

            int color = getColorForType(b.type);
            m.setIcon(createPinDrawable(color, R.drawable.ic_place));

            m.setOnMarkerClickListener((marker, map) -> {
                Building clicked = (Building) marker.getRelatedObject();
                selectedMarker = marker;
                selectedBuilding = clicked;
                showPopup(clicked);
                return true;
            });

            if (routingMode && selectedBuilding != null && selectedBuilding.id != null
                    && b.id != null && b.id.equals(selectedBuilding.id)) {
                selectedMarker = m;
            }

            buildingMarkers.add(m);
            mapView.getOverlays().add(m);
        }

        applyRoutingVisibility();
        mapView.invalidate();
    }

    private Drawable createPinDrawable(int bgColor, int iconRes) {
        View v = LayoutInflater.from(this).inflate(R.layout.view_map_pin, null, false);

        MaterialCardView card = v.findViewById(R.id.pinCard);
        ImageView icon = v.findViewById(R.id.pinIcon);

        card.setCardBackgroundColor(bgColor);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);

        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(spec, spec);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

        Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);

        return new BitmapDrawable(getResources(), b);
    }

    private int getColorForType(String type) {
        if (type == null) return ContextCompat.getColor(this, R.color.esprit_red);
        switch (type.toLowerCase(Locale.US)) {
            case "faculty": return Color.parseColor("#E30613");
            case "library": return Color.parseColor("#1E40AF");
            case "lab": return Color.parseColor("#D97706");
            case "cafeteria": return Color.parseColor("#059669");
            case "sports": return Color.parseColor("#DB2777");
            case "admin": return Color.parseColor("#4F46E5");
            default: return ContextCompat.getColor(this, R.color.esprit_red);
        }
    }

    // ---------- Search + Chips ----------
    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderMarkers(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean isSearch = actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (isSearch) {
                focusFirstMatch();
                return true;
            }
            return false;
        });

        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> renderMarkers());
        }
    }

    private void focusFirstMatch() {
        String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.US);
        for (Building candidate : filteredBuildings()) {
            if (q.isEmpty() || (candidate.name != null && candidate.name.toLowerCase(Locale.US).contains(q))) {
                focusAndShow(candidate);
                return;
            }
        }
    }

    private void focusAndShow(Building b) {
        selectedBuilding = b;
        GeoPoint p = new GeoPoint(b.lat, b.lng);
        mapController.setCenter(p);
        mapController.setZoom(18.0);
        showPopup(b);
        mapView.invalidate();
    }

    private List<Building> filteredBuildings() {
        String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.US);
        String type = selectedTypeFilter();

        List<Building> out = new ArrayList<>();
        for (Building b : allBuildings) {
            if (b == null) continue;

            boolean okType = type.equals("all") || (b.type != null && b.type.equalsIgnoreCase(type));
            if (!okType) continue;

            if (q.isEmpty()) {
                out.add(b);
                continue;
            }

            boolean okSearch = (b.name != null && b.name.toLowerCase(Locale.US).contains(q))
                    || (b.type != null && b.type.toLowerCase(Locale.US).contains(q));
            if (okSearch) out.add(b);
        }
        return out;
    }

    private String selectedTypeFilter() {
        if (chipGroup == null) return "all";
        int id = chipGroup.getCheckedChipId();

        if (id == R.id.chipAll) return "all";
        if (id == R.id.chipFaculty) return "faculty";
        if (id == R.id.chipLab) return "lab";
        if (id == R.id.chipLibrary) return "library";
        if (id == R.id.chipCafeteria) return "cafeteria";
        if (id == R.id.chipSports) return "sports";

        return "all";
    }

    // ---------- Popup ----------
    private void showPopup(Building b) {
        popupCard.setVisibility(View.VISIBLE);
        tvPopupName.setText(b.name == null ? "" : b.name);
        tvPopupType.setText(b.type == null ? "" : capitalize(b.type));
        tvPopupDesc.setText(b.description == null ? "" : b.description);

        String fac = "";
        if (b.facilities != null && !b.facilities.isEmpty()) {
            int n = Math.min(2, b.facilities.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append(b.facilities.get(i));
            }
            fac = sb.toString();
            if (b.facilities.size() > 2) fac += "...";
        }
        tvPopupMeta.setText("Floors: " + b.floors + "\nFacilities: " + fac);
    }

    private void hidePopup(boolean clearSelection) {
        popupCard.setVisibility(View.GONE);
        if (clearSelection) {
            selectedBuilding = null;
            selectedMarker = null;
        }
    }

    // ---------- QR ----------
    private void openBuildingByQr(String qr) {
        for (Building b : allBuildings) {
            if (b == null) continue;
            if (b.id != null && qr.equalsIgnoreCase(b.id)) {
                hidePopup(true);
                focusAndShow(b);
                return;
            }
        }
        Toast.makeText(this, "Unknown building QR: " + qr, Toast.LENGTH_SHORT).show();
    }

    private static boolean validCoord(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) return false;
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    // ---------- Routing ----------
    private void startRoutingTo(Building b) {
        if (b == null) {
            Toast.makeText(this, "No building selected", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedBuilding = b;

        GeoPoint me = (myLocationOverlay == null) ? null : myLocationOverlay.getMyLocation();
        if (me == null) {
            Toast.makeText(this, "Current location not ready yet (turn GPS on)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validCoord(me.getLatitude(), me.getLongitude())) {
            Toast.makeText(this, "Invalid current location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validCoord(b.lat, b.lng)) {
            Toast.makeText(this, "Invalid building coordinates in DB", Toast.LENGTH_SHORT).show();
            return;
        }

        routingMode = true;
        applyRoutingVisibility();
        fetchAndDrawRoute_OSRM(me, new GeoPoint(b.lat, b.lng));
    }

    private void applyRoutingVisibility() {
        if (!routingMode) {
            for (Marker m : buildingMarkers) {
                if (!mapView.getOverlays().contains(m)) mapView.getOverlays().add(m);
            }
            mapView.invalidate();
            return;
        }

        String keepId = (selectedBuilding != null) ? selectedBuilding.id : null;

        for (Marker m : buildingMarkers) {
            Building b = (Building) m.getRelatedObject();
            boolean keep = keepId != null && b != null && b.id != null && keepId.equals(b.id);

            if (keep) {
                if (!mapView.getOverlays().contains(m)) mapView.getOverlays().add(m);
                selectedMarker = m;
            } else {
                mapView.getOverlays().remove(m);
            }
        }
        mapView.invalidate();
    }

    private void clearRoute() {
        routingMode = false;

        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
            routePolyline = null;
        }
        if (routeShadow != null) {
            mapView.getOverlays().remove(routeShadow);
            routeShadow = null;
        }

        if (routeCard != null) routeCard.setVisibility(View.GONE);

        applyRoutingVisibility();
    }

    private void fetchAndDrawRoute_OSRM(GeoPoint origin, GeoPoint dest) {
        final String urlStr = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson&steps=true",
                origin.getLongitude(), origin.getLatitude(),
                dest.getLongitude(), dest.getLatitude()
        );

        netExec.execute(() -> {
            try {
                String json = httpGet(urlStr);

                JSONObject root = new JSONObject(json);
                JSONArray routes = root.getJSONArray("routes");
                if (routes.length() == 0) throw new RuntimeException("No routes");

                JSONObject route = routes.getJSONObject(0);
                double distanceM = route.getDouble("distance");
                double durationS = route.getDouble("duration");

                JSONObject geom = route.getJSONObject("geometry");
                JSONArray coords = geom.getJSONArray("coordinates");

                List<GeoPoint> points = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    double lon = c.getDouble(0);
                    double lat = c.getDouble(1);
                    points.add(new GeoPoint(lat, lon));
                }

                List<String> stepsText = new ArrayList<>();
                JSONArray legs = route.getJSONArray("legs");
                if (legs.length() > 0) {
                    JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
                    for (int i = 0; i < steps.length(); i++) {
                        JSONObject s = steps.getJSONObject(i);
                        JSONObject man = s.getJSONObject("maneuver");
                        String type = man.optString("type", "");
                        String modifier = man.optString("modifier", "");
                        String name = s.optString("name", "");
                        double dist = s.optDouble("distance", 0);

                        String instruction = buildInstruction(type, modifier, name);
                        stepsText.add((i + 1) + ". " + instruction + " (" + Math.round(dist) + " m)");
                    }
                }

                mainHandler.post(() -> {
                    drawRoute(points);
                    showRouteInfo(distanceM, durationS, stepsText);
                });

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(MapsActivity.this, "Routing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void drawRoute(List<GeoPoint> points) {
        if (routePolyline != null) mapView.getOverlays().remove(routePolyline);
        if (routeShadow != null) mapView.getOverlays().remove(routeShadow);

        routeShadow = new Polyline();
        routeShadow.setPoints(points);
        routeShadow.getOutlinePaint().setAntiAlias(true);
        routeShadow.getOutlinePaint().setStrokeWidth(16f);
        routeShadow.getOutlinePaint().setColor(Color.parseColor("#33000000"));
        routeShadow.getOutlinePaint().setStrokeCap(android.graphics.Paint.Cap.ROUND);
        routeShadow.getOutlinePaint().setStrokeJoin(android.graphics.Paint.Join.ROUND);

        routePolyline = new Polyline();
        routePolyline.setPoints(points);
        routePolyline.getOutlinePaint().setAntiAlias(true);
        routePolyline.getOutlinePaint().setStrokeWidth(10f);
        routePolyline.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.esprit_red));
        routePolyline.getOutlinePaint().setStrokeCap(android.graphics.Paint.Cap.ROUND);
        routePolyline.getOutlinePaint().setStrokeJoin(android.graphics.Paint.Join.ROUND);

        mapView.getOverlays().add(routeShadow);
        mapView.getOverlays().add(routePolyline);

        mapView.invalidate();

        if (!points.isEmpty()) {
            mapController.setCenter(points.get(points.size() / 2));
            mapController.setZoom(18.0);
        }
    }

    private void showRouteInfo(double distanceM, double durationS, List<String> steps) {
        if (routeCard == null || tvRouteSummary == null || tvRouteSteps == null) return;

        routeCard.setVisibility(View.VISIBLE);

        String distance = (distanceM >= 1000)
                ? String.format(Locale.US, "%.2f km", distanceM / 1000.0)
                : Math.round(distanceM) + " m";

        long mins = Math.round(durationS / 60.0);
        tvRouteSummary.setText("Route • " + distance + " • " + mins + " min");

        if (steps == null || steps.isEmpty()) {
            tvRouteSteps.setText("No step-by-step instructions.");
        } else {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(8, steps.size());
            for (int i = 0; i < limit; i++) sb.append(steps.get(i)).append("\n");
            if (steps.size() > limit) sb.append("...");
            tvRouteSteps.setText(sb.toString().trim());
        }
    }

    private static String buildInstruction(String type, String modifier, String name) {
        String base;
        switch (type) {
            case "depart": base = "Start"; break;
            case "arrive": base = "Arrive"; break;
            case "turn": base = "Turn"; break;
            case "new name": base = "Continue"; break;
            case "roundabout": base = "Enter roundabout"; break;
            default: base = type.isEmpty() ? "Continue" : type;
        }

        String mod = modifier == null ? "" : modifier.replace('_', ' ');
        String street = (name == null || name.trim().isEmpty()) ? "" : (" onto " + name);

        if (!mod.isEmpty() && base.equals("Turn")) return "Turn " + mod + street;
        if (base.equals("Arrive")) return "Arrive at destination";
        if (base.equals("Start")) return "Start" + street;
        return base + (mod.isEmpty() ? "" : (" " + mod)) + street;
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestMethod("GET");

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) throw new RuntimeException("HTTP " + code);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ---------- Admin button ----------
    private void setupAdminManageButton() {
        View btnManageBuildingsCard = findViewById(R.id.btnManageBuildingsCard);
        if (btnManageBuildings == null || btnManageBuildingsCard == null) return;

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);
                        boolean isAdmin = role != null && role.equalsIgnoreCase("Admin");
                        btnManageBuildingsCard.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ---------- Navbar ----------
    private void setupNavbar() {
        TextView navProfile = findViewById(R.id.navProfile);
        int redColor = ContextCompat.getColor(this, R.color.esprit_red);
        navProfile.setTextColor(redColor);
        for (android.graphics.drawable.Drawable d : navProfile.getCompoundDrawables()) if (d != null) d.setTint(redColor);

        findViewById(R.id.navCourses).setOnClickListener(v -> {
            startActivity(new Intent(this, CoursesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navEvents).setOnClickListener(v -> {
            Intent intent = ConfirmCalendarIntent();
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        // ✅ NOW: profile opens drawer instead of opening ProfileActivity
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        findViewById(R.id.navForums).setOnClickListener(v -> {
            startActivity(new Intent(this, ForumActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navCovoiturage).setOnClickListener(v -> {
            startActivity(new Intent(this, CovoiturageActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    // helper for calendar (keeps your original intent path)
    private Intent ConfirmCalendarIntent() {
        return new Intent(this, com.example.eventscalendar.CalendarActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOC) {
            boolean granted = false;
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) granted = true;

            if (granted && myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
