package com.example.jajac.pocketsummary;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Toast;

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
import static io.fotoapparat.parameter.selector.SizeSelectors.biggestSize;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private CameraView mCameraView;
    private FloatingActionButton mBackBtn;
    private FloatingActionButton mCaptureBtn;
    private FloatingActionButton mFinishBtn;

    private Fotoapparat mFotoapparat;
    private BitmapsHolder mBitmapsHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraView = findViewById(R.id.activity_camera_camera_view);
        mBackBtn = findViewById(R.id.activity_camera_back_btn);
        mCaptureBtn = findViewById(R.id.activity_camera_capture_btn);
        mFinishBtn = findViewById(R.id.activity_camera_finish_btn);

        mFotoapparat = Fotoapparat.with(this).into(mCameraView)
                .previewScaleType(ScaleType.CENTER_INSIDE)
                .previewSize(biggestSize())
                .photoSize(biggestSize())
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

        mBitmapsHolder = BitmapsHolder.getInstance();

        mBackBtn.setOnClickListener(view -> onBack());
        mCaptureBtn.setOnClickListener(view -> onCapture());
        mFinishBtn.setOnClickListener(view -> onFinish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFotoapparat.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFotoapparat.stop();
    }

    private void onBack() {
        onBackPressed();
    }

    private void onCapture() {
        PhotoResult photoResult = mFotoapparat.takePicture();
        photoResult
                .toBitmap()
                .whenAvailable(result -> {
                    mBitmapsHolder.addBitmap(result.bitmap);
                    Intent intent = new Intent(CameraActivity.this, CornersActivity.class);
                    startActivity(intent);
                });
    }
    
    private void onFinish() {
        Toast.makeText(this, "Finish!", Toast.LENGTH_SHORT).show();
    }

    private Size getPreviewSize(Collection<Size> sizes) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // swap width and height because camera sensor's default orientation
        // is landscape and we prefer portrait mode in this case
        Size screenSize = new Size(displayMetrics.heightPixels, displayMetrics.widthPixels);
        if (sizes.contains(screenSize)) {
            return screenSize;
        }
        return Collections.max(sizes, (left, right) -> Integer.compare(left.width, right.width));
    }

    private Size getPhotoSize(Collection<Size> sizes) {
        int maxSize = 4 * 1024 * 1024;
        double ratio = 4.0 / 3.0;
        sizes.removeIf(size -> size.width * size.height > maxSize);
        sizes.removeIf(size -> Math.max(size.width, size.height) / Math.min(size.width, size.height) - ratio > 0.05);
        return Collections.max(sizes, (left, right) -> Integer.compare(left.width, right.width));
//        return Collections.max(sizes, (left, right) -> Integer.compare(
//                left.width * left.height, right.width * right.height));
    }
}
