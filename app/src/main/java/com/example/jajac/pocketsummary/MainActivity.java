package com.example.jajac.pocketsummary;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;

    private LinearLayout mCameraBtn;
    private LinearLayout mGalleryBtn;
    private LinearLayout mProcessBtn;
    private RecyclerView mPagesRecyclerView;
    private TextView mPagesEmptyText;

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

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPagesAdapter.notifyDataSetChanged();
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

        mProcessBtn = findViewById(R.id.activity_main_btn_process);
        mProcessBtn.setOnClickListener(view -> onProcessClicked());

        mPagesEmptyText = findViewById(R.id.activity_main_pages_empty);

        mPagesRecyclerView = findViewById(R.id.activity_main_pages_list);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.list_divider));
        mPagesRecyclerView.addItemDecoration(divider);
        mPagesAdapter = new PagesRecyclerViewAdapter(this, DocumentHolder.getInstance().getAllPages());
        mPagesRecyclerView.setAdapter(mPagesAdapter);

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                mBroadcastReceiver, new IntentFilter("new-page"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPagesAdapter.getItemCount() > 0) {
            mPagesEmptyText.setVisibility(View.GONE);
        } else {
            mPagesEmptyText.setVisibility(View.VISIBLE);
        }
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

    private void onProcessClicked() {
        DocumentHolder docHolder = DocumentHolder.getInstance();
        for (int i = 0; i < docHolder.getPageCount(); i++) {
            new DetectTextTask(i).execute();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mBroadcastReceiver);
    }

    public class DetectTextTask extends AsyncTask<Void, Void, Void> {

        private int mPageIndex;

        public DetectTextTask(int index) {
            mPageIndex = index;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DocumentHolder.getInstance().setPageState(mPageIndex, Page.STATE_DETECTING_TEXT);
            mPagesAdapter.notifyItemChanged(mPageIndex);
        }

        @Override
        protected Void doInBackground(Void... params) {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(MainActivity.this).build();
            try {
                if (!textRecognizer.isOperational()) {
                    Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                }

                Bitmap bitmap = DocumentHolder.getInstance().getPageBitmap(mPageIndex);
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> detectedBlocks = textRecognizer.detect(frame);

                List<TextBlock> textBlocks = new ArrayList<>();
                for (int i = 0; i < detectedBlocks.size(); i++) {
                    textBlocks.add(detectedBlocks.valueAt(i));
                }

                // sort blocks top-to-bottom, left-to-right
                Collections.sort(textBlocks, (left, right) -> {
                    int verticalDiff = left.getBoundingBox().top - right.getBoundingBox().top;
                    int horizontalDiff = left.getBoundingBox().left - right.getBoundingBox().left;
                    if (verticalDiff != 0) {
                        return verticalDiff;
                    }
                    return horizontalDiff;
                });

                List<String> blocks = new ArrayList<>();
                for (TextBlock tb : textBlocks) {
                    blocks.add(tb.getValue());
                }
                DocumentHolder.getInstance().setPageBlocks(mPageIndex, blocks);
            } finally {
                textRecognizer.release();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            TranslateTask translateTask = new TranslateTask(mPageIndex);
            translateTask.execute();
        }
    }

    public class TranslateTask extends AsyncTask<Void, Void, Void> {

        private int mPageIndex;

        public TranslateTask(int index) {
            mPageIndex = index;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DocumentHolder.getInstance().setPageState(mPageIndex, Page.STATE_TRANSLATING);
            mPagesAdapter.notifyItemChanged(mPageIndex);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("https")
                    .authority("api.microsofttranslator.com")
                    .appendPath("V2")
                    .appendPath("Http.svc")
                    .appendPath("Translate")
                    .appendQueryParameter("text", "It works! Page: " + String.valueOf(mPageIndex))
                    .appendQueryParameter("from", "en")
                    .appendQueryParameter("to", "sr");

            String urlString = builder.build().toString();
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (url != null) {
                InputStream stream = null;
                HttpsURLConnection connection = null;
                String result = null;
                try {
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setReadTimeout(3000);
                    connection.setConnectTimeout(3000);
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.setRequestProperty("Ocp-Apim-Subscription-Key", "13b8ffca8e9e4ffabbf4c9e3947b5145");
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String str = streamToString(connection.getInputStream(), 10000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            DocumentHolder.getInstance().getPage(mPageIndex).setState(Page.STATE_FINISHED);
            mPagesAdapter.notifyItemChanged(mPageIndex);
        }

        public String streamToString(InputStream stream, int maxReadSize) throws IOException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] rawBuffer = new char[maxReadSize];
            int readSize;
            StringBuffer buffer = new StringBuffer();
            while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
                if (readSize > maxReadSize) {
                    readSize = maxReadSize;
                }
                buffer.append(rawBuffer, 0, readSize);
                maxReadSize -= readSize;
            }
            return buffer.toString();
        }
    }
}
