package com.example.jajac.pocketsummary;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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

    private CameraView mCameraView;
    private FloatingActionButton mBackBtn;
    private FloatingActionButton mCaptureBtn;
    private FloatingActionButton mFinishBtn;

    private Fotoapparat mFotoapparat;
    private DocumentHolder mDocumentHolder;

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

        mDocumentHolder = DocumentHolder.getInstance();

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
                    mDocumentHolder.addPage(new Page(result.bitmap));
                    Intent intent = new Intent(CameraActivity.this, CornersActivity.class);
                    intent.putExtra("camera", true);
                    startActivity(intent);
                });
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
}
