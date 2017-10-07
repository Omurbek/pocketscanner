package com.example.jajac.pocketsummary;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;

    private ImageButton mCameraBtn;
    private ImageButton mGalleryBtn;
    private RecyclerView mPagesRecyclerView;

    private PagesRecyclerViewAdapter mPagesAdapter;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mCameraBtn = findViewById(R.id.activity_main_btn_camera);
        mCameraBtn.setOnClickListener(view -> onCameraClicked());

        mGalleryBtn = findViewById(R.id.activity_main_btn_gallery);
        mGalleryBtn.setOnClickListener(view -> onGalleryClicked());

        mPagesRecyclerView = findViewById(R.id.activity_main_pages_list);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.list_divider));
        mPagesRecyclerView.addItemDecoration(divider);
        mPagesAdapter = new PagesRecyclerViewAdapter(this, BitmapsHolder.getInstance().getAll());
        mPagesRecyclerView.setAdapter(mPagesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "Internal OpenCV found. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        // TODO: Remove this, it's only for testing
        onCameraClicked();
    }

    private void onCameraClicked() {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "Required device camera.", Toast.LENGTH_SHORT).show();
        }

        String cameraPerm = Manifest.permission.CAMERA;
        String writeStoragePerm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(cameraPerm) != PackageManager.PERMISSION_GRANTED ||
                 checkSelfPermission(writeStoragePerm) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{cameraPerm, writeStoragePerm}, REQUEST_CAMERA_PERMISSION);
        } else {
            onUseCamera();
        }
    }

    private void onGalleryClicked() {
        String readStoragePerm = Manifest.permission.READ_EXTERNAL_STORAGE;
        String writeStoragePerm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(readStoragePerm) != PackageManager.PERMISSION_GRANTED ||
                 checkSelfPermission(writeStoragePerm) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{readStoragePerm, writeStoragePerm}, REQUEST_STORAGE_PERMISSION);
        } else {
            onUseGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onUseCamera();
                } else {
                    Toast.makeText(this, "Need camera permission.", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onUseGallery();
                } else {
                    Toast.makeText(this, "Need storage permission.", Toast.LENGTH_SHORT).show();
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onUseCamera() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        startActivity(intent);
    }

    private void onUseGallery() {
        Toast.makeText(this, "To be maybe implemented later.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode != RESULT_OK) {
            return;
        }
    }
}
