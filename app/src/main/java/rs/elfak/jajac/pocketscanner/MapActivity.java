package rs.elfak.jajac.pocketscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.jajac.pocketscanner.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private GoogleMap mGoogleMap;
    private MapView mMapView;
    private Circle mCircle;
    private float mRadius = 0;
    private Map<String, Marker> mMarkers = new HashMap<>();
    private Location mLocation;

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            onNewLocation(new Location(latitude, longitude));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
        return true;
    }

    private void onNewLocation(Location location) {
        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
        if (mLocation == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15.0f));

            mCircle = mGoogleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(mRadius)
                    .strokeWidth(10)
            );
        } else {
            mCircle.setCenter(center);
        }
        mLocation = location;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
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
}
