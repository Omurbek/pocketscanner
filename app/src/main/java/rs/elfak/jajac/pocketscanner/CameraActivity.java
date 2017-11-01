package rs.elfak.jajac.pocketscanner;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.jajac.pocketscanner.R;

import java.util.Collection;
import java.util.Collections;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.parameter.Size;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.parameter.selector.FlashSelectors.autoFlash;
import static io.fotoapparat.parameter.selector.FlashSelectors.autoRedEye;
import static io.fotoapparat.parameter.selector.FlashSelectors.torch;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.autoFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.continuousFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.fixed;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.back;
import static io.fotoapparat.parameter.selector.Selectors.firstAvailable;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private CameraView cameraView;
    private FloatingActionButton backBtn;
    private FloatingActionButton captureBtn;
    private FloatingActionButton finishBtn;

    private Fotoapparat fotoapparat;
    private DocumentsHolder documentsHolder;

    private boolean isLocationUpdatesBound;
    private LocationService locationService;
    private Location userLocation;

    private BroadcastReceiver userLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            userLocation = new Location(latitude, longitude);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.activity_camera_camera_view);
        backBtn = findViewById(R.id.activity_camera_back_btn);
        captureBtn = findViewById(R.id.activity_camera_capture_btn);
        finishBtn = findViewById(R.id.activity_camera_finish_btn);

        fotoapparat = Fotoapparat.with(this).into(cameraView)
                .previewScaleType(ScaleType.CENTER_INSIDE)
                .previewSize(this::getPreviewSize)
                .photoSize(this::getPhotoSize)
                .lensPosition(back())
                .focusMode(firstAvailable(
                        continuousFocus(),
                        autoFocus(),
                        fixed()
                ))
                .flash(firstAvailable(
                        autoRedEye(),
                        autoFlash(),
                        torch()
                )).build();

        documentsHolder = DocumentsHolder.getInstance();

        backBtn.setOnClickListener(view -> onBack());
        captureBtn.setOnClickListener(view -> onCapture());
        finishBtn.setOnClickListener(view -> onFinish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        fotoapparat.start();

        Intent locationUpdateIntent = new Intent(CameraActivity.this, LocationService.class);
        bindService(locationUpdateIntent, locationUpdatesConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(CameraActivity.this);
        // Register a receiver for userLocation updates
        localBroadcastManager.registerReceiver(userLocationReceiver,
                new IntentFilter(LocationService.LOCATION_RECEIVED_INTENT_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(CameraActivity.this);
        localBroadcastManager.unregisterReceiver(userLocationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        fotoapparat.stop();

        if (isLocationUpdatesBound) {
            unbindService(locationUpdatesConnection);
            isLocationUpdatesBound = false;
        }
    }

    private void onBack() {
        onBackPressed();
    }

    private void onCapture() {
        PhotoResult photoResult = fotoapparat.takePicture();
        photoResult
                .toBitmap()
                .whenAvailable(result -> {
                    onReceivedBitmap(result.bitmap);
                });
    }

    private void onReceivedBitmap(Bitmap bitmap) {
        if (isDevicePortraitMode()) {
            bitmap = getRotatedBitmap(bitmap);
        }
        documentsHolder.addDocument(new Document(bitmap, userLocation));
        startCornersActivity();
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }

    private void startCornersActivity() {
        Intent intent = new Intent(CameraActivity.this, CornersActivity.class);
        startActivity(intent);
    }

    private boolean isDevicePortraitMode() {
        boolean isOrientationPortrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;

        return isOrientationPortrait;
    }
    
    private void onFinish() {
        finish();
    }

    private Size getPreviewSize(Collection<Size> sizes) {
        return Collections.max(sizes, (left, right) -> Integer.compare(left.width, right.width));
    }

    private Size getPhotoSize(Collection<Size> sizes) {
        double ratio = 16.0 / 9.0;
        sizes.removeIf(size -> Math.abs((double) size.width / (double) size.height - ratio) > 0.02);
        return Collections.max(sizes, (left, right) -> Integer.compare(left.width, right.width));
    }

    private ServiceConnection locationUpdatesConnection = new ServiceConnection() {
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
