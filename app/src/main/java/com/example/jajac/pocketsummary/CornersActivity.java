package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CornersActivity extends AppCompatActivity {

    private static final String TAG = "CornersActivity";

    private ImageView mImageView;
    private CornersView mCornersView;
    private FloatingActionButton mBackBtn;
    private FloatingActionButton mFinishBtn;

    private Bitmap mBitmap;
    private List<Point> mCorners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corners);

        findViewById(R.id.activity_corners_container)
                .getViewTreeObserver().addOnGlobalLayoutListener(() -> onLayoutDrawn());

        // Draw this activity full screen, ignoring the status bar (draw below it)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        mImageView = findViewById(R.id.activity_corners_img);
        mCornersView = findViewById(R.id.activity_corners_corners_container);
        mBackBtn = findViewById(R.id.activity_corners_back_btn);
        mFinishBtn = findViewById(R.id.activity_corners_finish_btn);

        mCorners = getIntent().getParcelableArrayListExtra("corners");
        sortCorners();
        loadBitmapFromDisk();
        mImageView.setImageBitmap(mBitmap);

        mBackBtn.setOnClickListener(view -> onBack());
        mFinishBtn.setOnClickListener(view -> onFinish());
    }

    private void loadBitmapFromDisk() {
        try {
            String filename = getIntent().getStringExtra("bitmap");
            FileInputStream fis = this.openFileInput(filename);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            mBitmap = BitmapFactory.decodeStream(fis, null, options);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sortCorners() {
        Collections.sort(mCorners, (left, right) -> Integer.compare(left.y, right.y));
        if (mCorners.get(0).x > mCorners.get(1).x) {
            Point temp = mCorners.get(0);
            mCorners.set(0, mCorners.get(1));
            mCorners.set(1, temp);
        }
        if (mCorners.get(2).x < mCorners.get(3).x) {
            Point temp = mCorners.get(2);
            mCorners.set(2, mCorners.get(3));
            mCorners.set(3, temp);
        }
    }

    private void onLayoutDrawn() {
        mCornersView.setPoints(mCorners);
        mCornersView.setVisibility(View.VISIBLE);
    }

    private void onBack() {
        mBitmap.recycle();
        onBackPressed();
    }

    private void onFinish() {
        mCorners = mCornersView.getPoints();
        CropTransformBinarizeTask task = new CropTransformBinarizeTask(mBitmap, mCorners);
        task.execute();
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
            Mat imageRect = getMatOfPoints(corners);
            Mat transformRect = getMatOfPoints(transformedCorners);
            Utils.bitmapToMat(image, imageMat);

            Mat transformMat = Imgproc.getPerspectiveTransform(imageRect, transformRect);
            Imgproc.warpPerspective(imageMat, imageMat, transformMat, new Size(maxWidth, maxHeight));

            image = Bitmap.createBitmap(imageMat.width(), imageMat.height(), image.getConfig());

            Utils.matToBitmap(imageMat, image);

            imageRect.release();
            transformRect.release();
            transformMat.release();
            imageMat.release();

            return image;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mCornersView.setVisibility(View.INVISIBLE);
            mImageView.setImageBitmap(bitmap);
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
