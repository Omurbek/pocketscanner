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
import android.widget.SeekBar;
import android.widget.Toast;

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

    private GoogleMap mGoogleMap;
    private MapView mMapView;
    private Circle mCircle;
    private BubbleSeekBar mRadiusSeekBar;
    private float mRadius = 1.0f;
    private Map<String, Marker> mMarkers = new HashMap<>();

    private boolean mLocationUpdatesBound;
    private LocationService mLocationService;
    private Location mLocation;

    DatabaseReference mDocumentsDb = FirebaseDatabase.getInstance().getReference("documents");
    GeoFire mDocumentsGeoFire = new GeoFire(FirebaseDatabase.getInstance().getReference("documentsGeoFire"));
    GeoQuery mDocumentsGeoQuery;

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            onNewLocation(new Location(latitude, longitude));
        }
    };

    private OnProgressChangedListener mRadiusSeekbarChangeListener = new OnProgressChangedListener() {
        @Override
        public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
            mRadius = progress;
            if (mDocumentsGeoQuery != null) {
                mCircle.setRadius(mRadius * 1000);
                mDocumentsGeoQuery.setRadius(mRadius);
            }
        }

        @Override
        public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

        }

        @Override
        public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

        }
    };

    private GeoQueryEventListener mDocumentsGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            addDocumentMarker(key, new Location(location.latitude, location.longitude));
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

        mRadiusSeekBar = findViewById(R.id.activity_map_radius);
        mMapView = (MapView) findViewById(R.id.activity_map_mapview);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(MapActivity.this);

        mGoogleMap = googleMap;
        mGoogleMap.setOnMarkerClickListener(marker -> onMarkerClicked(marker));

        try {
            mGoogleMap.setMyLocationEnabled(true);
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
        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
        GeoLocation geoLoc = new GeoLocation(location.getLatitude(), location.getLongitude());
        if (mLocation == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15.0f));
            mCircle = mGoogleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(mRadius * 1000)
                    .strokeWidth(10)
                    .strokeColor(Color.argb(80, 69, 90, 100))
                    .fillColor(Color.argb(40, 255, 171, 0))
            );
            mDocumentsGeoQuery = mDocumentsGeoFire.queryAtLocation(geoLoc, mRadius);
            mDocumentsGeoQuery.addGeoQueryEventListener(mDocumentsGeoQueryListener);
            mRadiusSeekBar.setOnProgressChangedListener(mRadiusSeekbarChangeListener);
        } else {
            mCircle.setCenter(center);
            mDocumentsGeoQuery.setCenter(geoLoc);
        }
        mLocation = location;
    }

    private void addDocumentMarker(String key, Location location) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
        final Marker marker = mGoogleMap.addMarker(markerOptions);
        marker.setTag(key);
        mMarkers.put(key, marker);
    }

    private void removeDocumentMarker(String key) {
        mMarkers.remove(key);
    }

    private void onOpenDocument(String key) {
        Intent intent = new Intent(MapActivity.this, PageActivity.class);
        intent.putExtra("page-key", key);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();

        Intent locationUpdateIntent = new Intent(MapActivity.this, LocationService.class);
        bindService(locationUpdateIntent, mLocationUpdatesConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MapActivity.this);
        // Register a receiver for location updates
        localBroadcastManager.registerReceiver(mLocationReceiver,
                new IntentFilter(LocationService.LOCATION_RECEIVED_INTENT_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MapActivity.this);
        // Unregister the receiver for location updates
        localBroadcastManager.unregisterReceiver(mLocationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();

        if (mLocationUpdatesBound) {
            unbindService(mLocationUpdatesConnection);
            mLocationUpdatesBound = false;
        }

        if (mDocumentsGeoQuery != null) {
            mDocumentsGeoQuery.removeAllListeners();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private ServiceConnection mLocationUpdatesConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mLocationService = binder.getService();
            mLocationUpdatesBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationUpdatesBound = false;
        }
    };

}
