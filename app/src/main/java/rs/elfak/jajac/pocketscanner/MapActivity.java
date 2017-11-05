package rs.elfak.jajac.pocketscanner;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.jajac.pocketscanner.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.xw.repo.BubbleSeekBar;
import com.xw.repo.BubbleSeekBar.OnProgressChangedListener;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private GoogleMap googleMap;
    private MapView mapView;
    private Circle circle;
    private BubbleSeekBar radiusSeekBar;
    private float radius = 1.0f;
    private Map<String, Marker> markers = new HashMap<>();

    private boolean isLocationUpdatesBound;
    private LocationService locationService;
    private Location userLocation;

    DatabaseReference documentsDb = FirebaseDatabase.getInstance().getReference("documents");
    GeoFire documentsGeoFire = new GeoFire(FirebaseDatabase.getInstance().getReference("documentsGeoFire"));
    GeoQuery documentsGeoQuery;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            onNewLocation(new Location(latitude, longitude));
        }
    };

    private OnProgressChangedListener radiusSeekbarChangeListener = new OnProgressChangedListener() {
        @Override
        public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
            radius = progress;
            if (documentsGeoQuery != null) {
                circle.setRadius(radius * 1000);
                documentsGeoQuery.setRadius(radius);
            }
        }

        @Override
        public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

        }

        @Override
        public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

        }
    };

    private GeoQueryEventListener documentsGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            onDocumentEntered(key);
        }

        @Override
        public void onKeyExited(String key) {
            removeDocumentMarker(key);
        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        radiusSeekBar = findViewById(R.id.activity_map_radius);
        mapView = findViewById(R.id.activity_map_mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(MapActivity.this);

        this.googleMap = googleMap;
        this.googleMap.setOnMarkerClickListener(marker -> onMarkerClicked(marker));

        try {
            this.googleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            // Not handling the exception here because we receive locations from the service
            // that will only be enabled if we have location permission
            e.printStackTrace();
        }
    }

    private boolean onMarkerClicked(Marker marker) {
        String key = (String) marker.getTag();
        onOpenDocument(key);
        return true;
    }

    private void onNewLocation(Location location) {
        LatLng center = new LatLng(location.getLat(), location.getLon());
        GeoLocation geoLoc = new GeoLocation(location.getLat(), location.getLon());
        if (userLocation == null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15.0f));
            circle = googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius * 1000)
                    .strokeWidth(10)
                    .strokeColor(Color.argb(80, 69, 90, 100))
                    .fillColor(Color.argb(40, 255, 171, 0))
            );
            documentsGeoQuery = documentsGeoFire.queryAtLocation(geoLoc, radius);
            documentsGeoQuery.addGeoQueryEventListener(documentsGeoQueryListener);
            radiusSeekBar.setOnProgressChangedListener(radiusSeekbarChangeListener);
        } else {
            circle.setCenter(center);
            documentsGeoQuery.setCenter(geoLoc);
        }
        userLocation = location;
    }

    private void onDocumentEntered(String key) {
        documentsDb.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Document doc = dataSnapshot.getValue(Document.class);
                addDocumentMarker(key, doc);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addDocumentMarker(String key, Document doc) {
        double docLatitude = doc.getLocation().getLat();
        double docLongitude = doc.getLocation().getLon();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(docLatitude, docLongitude));
        float markerColor = BitmapDescriptorFactory.HUE_AZURE;
        if (doc.getType() == DocumentType.INFORMATION) {
            markerColor = BitmapDescriptorFactory.HUE_AZURE;
        } else if (doc.getType() == DocumentType.WARNING) {
            markerColor = BitmapDescriptorFactory.HUE_RED;
        }
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(markerColor));

        Marker marker = googleMap.addMarker(markerOptions);
        marker.setTag(key);
        markers.put(key, marker);
    }

    private void removeDocumentMarker(String key) {
        markers.remove(key);
    }

    private void onOpenDocument(String key) {
        Intent intent = new Intent(MapActivity.this, DocumentActivity.class);
        intent.putExtra("document-key", key);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();

        Intent locationUpdateIntent = new Intent(MapActivity.this, LocationService.class);
        bindService(locationUpdateIntent, mLocationUpdatesConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MapActivity.this);
        // Register a receiver for location updates
        localBroadcastManager.registerReceiver(locationReceiver,
                new IntentFilter(LocationService.LOCATION_RECEIVED_INTENT_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MapActivity.this);
        // Unregister the receiver for location updates
        localBroadcastManager.unregisterReceiver(locationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();

        if (isLocationUpdatesBound) {
            unbindService(mLocationUpdatesConnection);
            isLocationUpdatesBound = false;
        }

        if (documentsGeoQuery != null) {
            documentsGeoQuery.removeAllListeners();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private ServiceConnection mLocationUpdatesConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            isLocationUpdatesBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isLocationUpdatesBound = false;
        }
    };

}
