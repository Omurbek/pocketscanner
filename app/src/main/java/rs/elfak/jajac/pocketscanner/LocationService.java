package rs.elfak.jajac.pocketscanner;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;


public class LocationService extends Service {

    private static final String TAG = "LocationService";

    public static final String LOCATION_RECEIVED_INTENT_ACTION = "com.elfak.jajac.pocketscanner.location-received";

    private final IBinder mLocalBinder = new LocalBinder();

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;


    @Override
    public void onCreate() {
        super.onCreate();
        // Called when the service is bound for the first time,
        // we can start timed operations here
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(LocationService.this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                android.location.Location location = locationResult.getLastLocation();
                onNewLocation(location.getLatitude(), location.getLongitude());
            }
        };

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Not handling the permission exception here because this service shouldn't
        // ever be started and bound without location permission
        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Called when the last bound Activity is unbound from
        // this service, so we stop timed operations here
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        return super.onUnbind(intent);
    }

    private void onNewLocation(double latitude, double longitude) {
        Intent intent = new Intent(LOCATION_RECEIVED_INTENT_ACTION);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }
}
