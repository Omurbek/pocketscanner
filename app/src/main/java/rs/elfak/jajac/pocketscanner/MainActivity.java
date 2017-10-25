package rs.elfak.jajac.pocketscanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jajac.pocketscanner.R;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements PagesRecyclerViewAdapter.OnPageClickListener {

    public static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_CHOOSE_FROM_GALLERY = 3;

    private LinearLayout mMapBtn;
    private LinearLayout mCameraBtn;
    private LinearLayout mGalleryBtn;
    private LinearLayout mProcessBtn;
    private RecyclerView mPagesRecyclerView;
    private TextView mPagesEmptyText;
    private Spinner mLanguageFromSpinner;
    private Spinner mLanguageToSpinner;

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

    private AdapterView.OnItemSelectedListener mOnLanguageChangedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SharedPreferences.Editor prefEditor = PreferenceManager
                    .getDefaultSharedPreferences(MainActivity.this).edit();
            String[] languages = getResources().getStringArray(com.example.jajac.pocketscanner.R.array.language_values);
            String value = languages[position];
            if (parent == mLanguageFromSpinner) {
                prefEditor.putString("language_from", value);
            } else if (parent == mLanguageToSpinner) {
                prefEditor.putString("language_to", value);
            }
            prefEditor.apply();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            return;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mLanguageFromSpinner = findViewById(R.id.activity_main_language_from_spinner);
        mLanguageToSpinner = findViewById(R.id.activity_main_language_to_spinner);
        mLanguageFromSpinner.setOnItemSelectedListener(mOnLanguageChangedListener);
        mLanguageToSpinner.setOnItemSelectedListener(mOnLanguageChangedListener);

        mMapBtn = findViewById(R.id.activity_main_btn_map);
        mMapBtn.setOnClickListener(view -> onMapClicked());

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
        mPagesAdapter = new PagesRecyclerViewAdapter(this, DocumentHolder.getInstance().getAllPages(), this);
        mPagesRecyclerView.setAdapter(mPagesAdapter);

        loadLanguagePreferences();

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                mBroadcastReceiver, new IntentFilter("new-page"));
    }

    private void loadLanguagePreferences() {
        List<String> languageValues = Arrays.asList(getResources().getStringArray(R.array.language_values));
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String fromVal = pref.getString("language_from", getString(R.string.language_from_default_value));
        String toVal = pref.getString("language_to", getString(R.string.language_to_default_value));
        int fromIndex = languageValues.indexOf(fromVal);
        int toIndex = languageValues.indexOf(toVal);
        mLanguageFromSpinner.setSelection(fromIndex);
        mLanguageToSpinner.setSelection(toIndex);
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

    private void onMapClicked() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
    }

    private void onCameraClicked() {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "Required device camera.", Toast.LENGTH_SHORT).show();
        }

        String cameraPerm = Manifest.permission.CAMERA;
        String writeStoragePerm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String locationPerm = Manifest.permission.ACCESS_FINE_LOCATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(cameraPerm) != PackageManager.PERMISSION_GRANTED ||
                 checkSelfPermission(writeStoragePerm) != PackageManager.PERMISSION_GRANTED ||
                 checkSelfPermission(locationPerm) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{cameraPerm, writeStoragePerm, locationPerm},
                    REQUEST_CAMERA_PERMISSION);
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
        String[] langValues = getResources().getStringArray(R.array.language_values);
        String fromLangVal = langValues[mLanguageFromSpinner.getSelectedItemPosition()];
        String toLangVal = langValues[mLanguageToSpinner.getSelectedItemPosition()];
        for (int i = 0; i < docHolder.getPageCount(); i++) {
            if (docHolder.getPage(i).getState() == Page.STATE_PENDING) {
                new DetectTextAndTranslateTask(i, fromLangVal, toLangVal).execute();
            }
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
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUEST_CHOOSE_FROM_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CHOOSE_FROM_GALLERY) {
            if (data != null && data.getData() != null) {
                String fullFilePath = getFullFilePathFromURI(MainActivity.this, data.getData());
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(fullFilePath, options);
                DocumentHolder.getInstance().addPage(new Page(bitmap, new Location(43.3209, 21.8957)));
                Intent intent = new Intent(MainActivity.this, CornersActivity.class);
                intent.putExtra("camera", false);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onPageClicked(int pageIndex) {
        Intent intent = new Intent(MainActivity.this, PageActivity.class);
        intent.putExtra("page-index", pageIndex);
        startActivity(intent);
    }

    private String getFullFilePathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public class DetectTextAndTranslateTask extends AsyncTask<Void, Void, Integer> {

        public static final int TEXT_ERROR = 0;
        public static final int TEXT_OK = 1;

        private int mPageIndex;
        private String mFromLang;
        private String mToLang;

        public DetectTextAndTranslateTask(int index, String fromLang, String toLang) {
            mPageIndex = index;
            mFromLang = fromLang;
            mToLang = toLang;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DocumentHolder.getInstance().setPageState(mPageIndex, Page.STATE_DETECTING_TEXT);
            mPagesAdapter.notifyItemChanged(mPageIndex);
        }

        @Override
        protected Integer doInBackground(Void... params) {
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

                if (textBlocks.size() == 0) {
                    return TEXT_ERROR;
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

                List<TextPiece> textPieces = new ArrayList<>();
                for (TextBlock tb : textBlocks) {
                    textPieces.add(new TextPiece(tb.getBoundingBox(), tb.getValue()));
                }

                DocumentHolder.getInstance().setPageBlocks(mPageIndex, textPieces);
                return TEXT_OK;
            } finally {
                textRecognizer.release();
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);
            if (status == TEXT_ERROR) {
                DocumentHolder.getInstance().getPage(mPageIndex).setState(Page.STATE_ERROR);
                mPagesAdapter.notifyItemChanged(mPageIndex);
            } else if (status == TEXT_OK) {
                TranslateTask translateTask = new TranslateTask(mPageIndex, mFromLang, mToLang);
                translateTask.execute();
            }
        }
    }

    public class TranslateTask extends AsyncTask<Void, Void, Void> {

        private int mPageIndex;
        private String mFromLang;
        private String mToLang;

        public TranslateTask(int index, String fromLang, String toLang) {
            mPageIndex = index;
            mFromLang = fromLang;
            mToLang = toLang;
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
                    .appendQueryParameter("text", DocumentHolder.getInstance().getPage(mPageIndex).getOriginal())
                    .appendQueryParameter("from", mFromLang)
                    .appendQueryParameter("to", mToLang);

            String urlString = builder.build().toString();
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (url != null) {
                HttpsURLConnection connection = null;
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
                        String fullResult = streamToString(connection.getInputStream(), 10000);
                        fullResult = fullResult.substring(68, fullResult.length() - 9);
                        DocumentHolder.getInstance().getPage(mPageIndex).setTranslationTextAndBlocks(fullResult);
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
