package com.example.jajac.pocketsummary;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.OrientationHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.parameter.Size;
import io.fotoapparat.parameter.selector.SelectorFunction;
import io.fotoapparat.parameter.selector.SizeSelectors;
import io.fotoapparat.photo.BitmapPhoto;
import io.fotoapparat.result.PendingResult;
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
    private DocumentFinder mDocumentFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraView = findViewById(R.id.activity_camera_camera_view);
        mBackBtn = findViewById(R.id.activity_camera_back_btn);
        mCaptureBtn = findViewById(R.id.activity_camera_capture_btn);
        mFinishBtn = findViewById(R.id.activity_camera_finish_btn);

        mFotoapparat = Fotoapparat.with(this).into(mCameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
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

        mDocumentFinder = new DocumentFinder(5, 0.04, true);

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
        boolean isOrientationPortrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        PhotoResult photoResult = mFotoapparat.takePicture();

        ProgressDialog progressDialog = new ProgressDialog(CameraActivity.this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        photoResult
                .toBitmap()
                .whenAvailable(result -> {
                    Bitmap capturedBitmap = result.bitmap;
                    if (isOrientationPortrait) {
                        capturedBitmap = getRotatedBitmap(result.bitmap);
                    }
                    processImage(capturedBitmap);
                    progressDialog.dismiss();
                });
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void processImage(Bitmap image) {
        ArrayList<Point> corners = mDocumentFinder.findCorners(image);
        if (corners == null) {
            Toast.makeText(this, "Please try that again.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream fos = this.openFileOutput("image.jpg", Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            image.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(CameraActivity.this, CornersActivity.class);
        intent.putParcelableArrayListExtra("corners", corners);
        intent.putExtra("bitmap", "image.jpg");
        startActivity(intent);
    }
    
    private void onFinish() {
        Toast.makeText(this, "Finish!", Toast.LENGTH_SHORT).show();
    }

    private Size getPreviewSize(Collection<Size> sizes) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // inverse width and height because camera sensor's default orientation
        // is landscape and we prefer portrait mode in this case
        Size screenSize = new Size(displayMetrics.heightPixels, displayMetrics.widthPixels);
        if (sizes.contains(screenSize)) {
            return screenSize;
        }
        return Collections.max(sizes, (left, right) -> Integer.compare(left.width, right.width));
    }

    private Size getPhotoSize(Collection<Size> sizes) {
        int twoMegapixels = 2 * 1024 * 1024;
        double ratio = 16.0 / 9.0;
        sizes.removeIf(size -> size.width * size.height > twoMegapixels);
        sizes.removeIf(size -> Math.max(size.width, size.height) / Math.min(size.width, size.height) - ratio > 0.05);
        return Collections.max(sizes, (left, right) -> Integer.compare(left.width, right.width));
    }
}
