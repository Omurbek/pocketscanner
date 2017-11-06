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


public class MainActivity extends AppCompatActivity implements
        DocumentsRecyclerViewAdapter.OnDocumentClickListener {

    public static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final int REQUEST_CHOOSE_FROM_GALLERY = 4;

    private LinearLayout exploreBtn;
    private LinearLayout cameraBtn;
    private LinearLayout galleryBtn;
    private RecyclerView documentsRecyclerView;
    private TextView documentsEmptyText;
    private Spinner languageFromSpinner;
    private Spinner languageToSpinner;

    private DocumentsRecyclerViewAdapter documentsAdapter;

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
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
            documentsAdapter.notifyDataSetChanged();
        }
    };

    private AdapterView.OnItemSelectedListener onLanguageChangedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SharedPreferences.Editor prefEditor = PreferenceManager
                    .getDefaultSharedPreferences(MainActivity.this).edit();
            String[] languages = getResources().getStringArray(com.example.jajac.pocketscanner.R.array.language_values);
            String value = languages[position];
            if (parent == languageFromSpinner) {
                prefEditor.putString("language_from", value);
            } else if (parent == languageToSpinner) {
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

        languageFromSpinner = findViewById(R.id.activity_main_language_from_spinner);
        languageToSpinner = findViewById(R.id.activity_main_language_to_spinner);
        languageFromSpinner.setOnItemSelectedListener(onLanguageChangedListener);
        languageToSpinner.setOnItemSelectedListener(onLanguageChangedListener);

        exploreBtn = findViewById(R.id.activity_main_btn_map);
        exploreBtn.setOnClickListener(view -> onExploreClicked());

        cameraBtn = findViewById(R.id.activity_main_btn_camera);
        cameraBtn.setOnClickListener(view -> onCameraClicked());

        galleryBtn = findViewById(R.id.activity_main_btn_gallery);
        galleryBtn.setOnClickListener(view -> onGalleryClicked());

        documentsEmptyText = findViewById(R.id.activity_main_pages_empty);

        documentsRecyclerView = findViewById(R.id.activity_main_pages_list);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.list_divider));
        documentsRecyclerView.addItemDecoration(divider);
        documentsAdapter = new DocumentsRecyclerViewAdapter(this, DocumentsHolder.getInstance().getAllDocuments(), this);
        documentsRecyclerView.setAdapter(documentsAdapter);

        loadLanguagePreferences();

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                mBroadcastReceiver, new IntentFilter("new-document"));
    }

    private void loadLanguagePreferences() {
        List<String> languageValues = Arrays.asList(getResources().getStringArray(R.array.language_values));
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String fromVal = pref.getString("language_from", getString(R.string.language_from_default_value));
        String toVal = pref.getString("language_to", getString(R.string.language_to_default_value));
        int fromIndex = languageValues.indexOf(fromVal);
        int toIndex = languageValues.indexOf(toVal);
        languageFromSpinner.setSelection(fromIndex);
        languageToSpinner.setSelection(toIndex);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (documentsAdapter.getItemCount() > 0) {
            documentsEmptyText.setVisibility(View.GONE);
            processNewDocuments();
        } else {
            documentsEmptyText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, loaderCallback);
        } else {
            Log.d(TAG, "Internal OpenCV found. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void processNewDocuments() {
        DocumentsHolder docHolder = DocumentsHolder.getInstance();
        String[] langValues = getResources().getStringArray(R.array.language_values);
        String fromLangVal = langValues[languageFromSpinner.getSelectedItemPosition()];
        String toLangVal = langValues[languageToSpinner.getSelectedItemPosition()];
        for (int i = 0; i < docHolder.getDocumentCount(); i++) {
            if (docHolder.getDocumentAt(i).getState() != DocumentState.FINISHED) {
                new RecognizeTextAndTranslateTask(i, fromLangVal, toLangVal).execute();
            }
        }
    }

    private void onExploreClicked() {
        String locationPerm = Manifest.permission.ACCESS_FINE_LOCATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(locationPerm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{locationPerm}, REQUEST_LOCATION_PERMISSION);
        } else {
            openMap();
        }
    }

    private void onCameraClicked() {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "Required device camera.", Toast.LENGTH_SHORT).show();
        }

        String cameraPerm = Manifest.permission.CAMERA;
        String locationPerm = Manifest.permission.ACCESS_FINE_LOCATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(cameraPerm) != PackageManager.PERMISSION_GRANTED ||
                 checkSelfPermission(locationPerm) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{cameraPerm, locationPerm},
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
                break;
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openMap();
                } else {
                    Toast.makeText(this, "Need location permission.", Toast.LENGTH_SHORT).show();
                }
                break;
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

    private void openMap() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
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
                DocumentsHolder.getInstance().addDocument(new Document(bitmap, new Location(43.3209, 21.8957)));
                Intent intent = new Intent(MainActivity.this, CornersActivity.class);
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
    public void onDocumentClicked(int docIndex) {
        Intent intent = new Intent(MainActivity.this, DocumentActivity.class);
        intent.putExtra("document-index", docIndex);
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

    public class RecognizeTextAndTranslateTask extends AsyncTask<Void, Void, Integer> {

        public static final int TEXT_ERROR = 0;
        public static final int TEXT_OK = 1;

        private int docIndex;
        private String fromLang;
        private String toLang;

        public RecognizeTextAndTranslateTask(int docIndex, String fromLang, String toLang) {
            this.docIndex = docIndex;
            this.fromLang = fromLang;
            this.toLang = toLang;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DocumentsHolder.getInstance().setDocumentState(docIndex, DocumentState.FINDING_TEXT);
            documentsAdapter.notifyItemChanged(docIndex);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(MainActivity.this).build();
            try {
                if (!textRecognizer.isOperational()) {
                    Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                    return TEXT_ERROR;
                }

                Bitmap bitmap = DocumentsHolder.getInstance().getDocumentBitmap(docIndex);
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> detectedBlocks = textRecognizer.detect(frame);
                List<TextBlock> textBlocks = new ArrayList<>();
                for (int i = 0; i < detectedBlocks.size(); i++) {
                    textBlocks.add(detectedBlocks.valueAt(i));
                }

                if (textBlocks.size() == 0) {
                    return TEXT_ERROR;
                }

                sortBlocksByPosition(textBlocks); // top to bottom, left to right
                String detectedText = buildStringResult(textBlocks);
                DocumentsHolder.getInstance().setDocumentText(docIndex, detectedText);
                return TEXT_OK;
            } finally {
                textRecognizer.release();
            }
        }

        private void sortBlocksByPosition(List<TextBlock> textBlocks) {
            Collections.sort(textBlocks, (left, right) -> {
                int verticalDiff = left.getBoundingBox().top - right.getBoundingBox().top;
                int horizontalDiff = left.getBoundingBox().left - right.getBoundingBox().left;
                if (verticalDiff != 0) {
                    return verticalDiff;
                }
                return horizontalDiff;
            });
        }

        private String buildStringResult(List<TextBlock> textBlocks) {
            String detectedText = "";
            for (TextBlock tb : textBlocks) {
                detectedText += tb.getValue() + "\n\n";
            }
            return detectedText;
        }

        @Override
        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);
            if (status == TEXT_ERROR) {
                DocumentsHolder.getInstance().getDocumentAt(docIndex).setState(DocumentState.NO_TEXT);
                documentsAdapter.notifyItemChanged(docIndex);
            } else if (status == TEXT_OK) {
                TranslateTask translateTask = new TranslateTask(docIndex, fromLang, toLang);
                translateTask.execute();
            }
        }
    }

    public class TranslateTask extends AsyncTask<Void, Void, Void> {

        private int docIndex;
        private String fromLang;
        private String toLang;

        public TranslateTask(int index, String fromLang, String toLang) {
            docIndex = index;
            this.fromLang = fromLang;
            this.toLang = toLang;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DocumentsHolder.getInstance().setDocumentState(docIndex, DocumentState.TRANSLATING);
            documentsAdapter.notifyItemChanged(docIndex);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("https")
                    .authority("api.microsofttranslator.com")
                    .appendPath("V2")
                    .appendPath("Http.svc")
                    .appendPath("Translate")
                    .appendQueryParameter("text", DocumentsHolder.getInstance().getDocumentAt(docIndex).getText())
                    .appendQueryParameter("from", fromLang)
                    .appendQueryParameter("to", toLang);

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
                    connection.setRequestProperty("Ocp-Apim-Subscription-Key",
                            getString(R.string.microsoft_translator_api_key));
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String fullResult = streamToString(connection.getInputStream(), 10000);
                        String translation = fullResult.substring(68, fullResult.length() - 9);
                        DocumentsHolder.getInstance().getDocumentAt(docIndex).setTranslation(translation);
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
            DocumentsHolder.getInstance().getDocumentAt(docIndex).setState(DocumentState.FINISHED);
            documentsAdapter.notifyItemChanged(docIndex);
        }

        public String streamToString(InputStream stream, int maxReadSize) throws IOException {
            Reader reader = new InputStreamReader(stream, "UTF-8");
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
