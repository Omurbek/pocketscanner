package rs.elfak.jajac.pocketscanner;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.jajac.pocketscanner.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CornersActivity extends AppCompatActivity {

    private static final String TAG = "CornersActivity";

    private boolean isProcessed;
    private FrameLayout container;
    private ImageView imageView;
    private CornersView cornersView;
    private FloatingActionButton backBtn;
    private FloatingActionButton finishBtn;
    private ProgressDialog progressDialog;

    private Bitmap bitmap;
    private List<Point> corners;
    private DocumentsHolder documentsHolder;
    private DocumentFinder documentFinder;
    private double previewRatio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.activity_corners);

        // Draw this activity full screen, ignoring the status bar (draw below it)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        container = findViewById(R.id.activity_corners_container);
        imageView = findViewById(R.id.activity_corners_img);
        cornersView = findViewById(R.id.activity_corners_corners_container);
        backBtn = findViewById(R.id.activity_corners_back_btn);
        finishBtn = findViewById(R.id.activity_corners_finish_btn);

        backBtn.setOnClickListener(view -> onBack());
        finishBtn.setOnClickListener(view -> onFinish());

        documentFinder = new DocumentFinder(9, 0.02, true, 10.0, 20.0, 3);
        documentsHolder = DocumentsHolder.getInstance();
        bitmap = documentsHolder.getLastDocumentBitmap();

        // .post() executes the call back after the activity renders
        container.post(this::setScaledBitmapAndCorners);
    }

    private void hideStatusBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void setScaledBitmapAndCorners() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Processing...");
        progressDialog.show();

        previewRatio = (double) bitmap.getWidth() / (double) container.getWidth();

        new FindCornersTask(container.getWidth()).execute();
    }

    private void onBack() {
        documentsHolder.removeLastDocument();
        bitmap.recycle();
        onBackPressed();
    }

    private void onFinish() {
        if (!isProcessed) {
            startProcessingTask();
        } else {
            broadcastNewDocument();
        }
    }

    private void broadcastNewDocument() {
        Intent intent = new Intent("new-document");
        LocalBroadcastManager.getInstance(CornersActivity.this).sendBroadcast(intent);
        finish();
    }

    private void startProcessingTask() {
        getScaledCorners();
        CropTransformBinarizeTask task = new CropTransformBinarizeTask(documentsHolder.getLastDocumentBitmap(), corners);
        task.execute();
    }

    // scales the preview corners' locations based on
    // the difference in preview:bitmap ratio
    private void getScaledCorners() {
        corners = cornersView.getPoints();
        for (Point point : corners) {
            point.x *= previewRatio;
            point.y *= previewRatio;
        }
    }

    public class FindCornersTask extends AsyncTask<Void, Bitmap, Void> {

        private int containerWidth;

        public FindCornersTask(int containerWidth) {
            this.containerWidth = containerWidth;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
            imageView.setImageBitmap(values[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    containerWidth, (int)(bitmap.getHeight() / previewRatio), true);
            this.publishProgress(bitmap);
            corners = documentFinder.findCorners(bitmap);
            sortCorners();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            cornersView.setPoints(corners);
            cornersView.setVisibility(View.VISIBLE);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    bitmap.getWidth(), bitmap.getHeight());
            layoutParams.gravity = Gravity.CENTER;
            cornersView.setLayoutParams(layoutParams);

            progressDialog.dismiss();
        }

        private void sortCorners() {
            Collections.sort(corners, (left, right) -> Integer.compare(left.y, right.y));
            if (corners.get(0).x > corners.get(1).x) {
                Point temp = corners.get(0);
                corners.set(0, corners.get(1));
                corners.set(1, temp);
            }
            if (corners.get(2).x < corners.get(3).x) {
                Point temp = corners.get(2);
                corners.set(2, corners.get(3));
                corners.set(3, temp);
            }
        }
    }

    public class CropTransformBinarizeTask extends AsyncTask<Void, Bitmap, Bitmap> {

        private Bitmap image;
        private List<Point> corners;

        public CropTransformBinarizeTask(Bitmap image, List<Point> corners) {
            this.image = image;
            this.corners = corners;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Point topLeft = corners.get(0);
            Point topRight = corners.get(1);
            Point bottomRight = corners.get(2);
            Point bottomLeft = corners.get(3);

            int maxWidth = (int) Math.max(
                    Math.sqrt(Math.pow(topRight.x - topLeft.x, 2) + Math.pow(topRight.y - topLeft.y, 2)),
                    Math.sqrt(Math.pow(bottomRight.x - bottomLeft.x, 2) + Math.pow(bottomRight.y - bottomLeft.y, 2))
            );

            int maxHeight = (int) Math.max(
                    Math.sqrt(Math.pow(topLeft.x - bottomLeft.x, 2) + Math.pow(topLeft.y - bottomLeft.y, 2)),
                    Math.sqrt(Math.pow(topRight.x - bottomRight.x, 2) + Math.pow(topRight.y - bottomRight.y, 2))
            );

            List<Point> transformedCorners = new ArrayList<>();
            transformedCorners.add(new Point(0, 0));
            transformedCorners.add(new Point(maxWidth - 1, 0));
            transformedCorners.add(new Point(maxWidth - 1, maxHeight - 1));
            transformedCorners.add(new Point(0, maxHeight - 1));

            Mat imageMat = new Mat();
            Mat grayMat = new Mat();
            Mat imageRect = getMatOfPoints(corners);
            Mat transformRect = getMatOfPoints(transformedCorners);
            Utils.bitmapToMat(image, imageMat);

            Mat transformMat = Imgproc.getPerspectiveTransform(imageRect, transformRect);
            Imgproc.warpPerspective(imageMat, imageMat, transformMat, new Size(maxWidth, maxHeight));

            Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.threshold(grayMat, grayMat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

            image = Bitmap.createBitmap(imageMat.width(), imageMat.height(), image.getConfig());

            Utils.matToBitmap(grayMat, image);

            imageRect.release();
            transformRect.release();
            transformMat.release();
            imageMat.release();
            grayMat.release();

            DocumentsHolder.getInstance().setLastDocumentBitmap(image);
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            isProcessed = true;
            cornersView.setVisibility(View.INVISIBLE);
            imageView.setImageBitmap(bitmap);
        }

        private MatOfPoint2f getMatOfPoints(List<Point> points) {
            List<org.opencv.core.Point> opencvPoints = new ArrayList<>();
            for (Point point : points) {
                opencvPoints.add(new org.opencv.core.Point(point.x, point.y));
            }
            MatOfPoint2f mat = new MatOfPoint2f();
            mat.fromList(opencvPoints);
            return mat;
        }
    }
}
