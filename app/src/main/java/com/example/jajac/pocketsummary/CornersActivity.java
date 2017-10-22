package com.example.jajac.pocketsummary;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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

    private boolean mIsCameraPicture;
    private boolean mProcessed;
    private FrameLayout mContainer;
    private ImageView mImageView;
    private CornersView mCornersView;
    private FloatingActionButton mBackBtn;
    private FloatingActionButton mFinishBtn;
    private ProgressDialog mProgressDialog;

    private Bitmap mBitmap;
    private List<Point> mCorners;
    private DocumentHolder mDocumentHolder;
    private DocumentFinder mDocumentFinder;
    private double mPreviewRatio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_corners);

        // Draw this activity full screen, ignoring the status bar (draw below it)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        mIsCameraPicture = getIntent().getBooleanExtra("camera", false);

        mContainer = findViewById(R.id.activity_corners_container);
        mImageView = findViewById(R.id.activity_corners_img);
        mCornersView = findViewById(R.id.activity_corners_corners_container);
        mBackBtn = findViewById(R.id.activity_corners_back_btn);
        mFinishBtn = findViewById(R.id.activity_corners_finish_btn);

        mBackBtn.setOnClickListener(view -> onBack());
        mFinishBtn.setOnClickListener(view -> onFinish());

        mDocumentFinder = new DocumentFinder(9, 0.04, true);
        mDocumentHolder = DocumentHolder.getInstance();
        mBitmap = mDocumentHolder.getLastPageBitmap();

        mContainer.post(this::setScaledBitmapAndCorners);
    }

    private void setScaledBitmapAndCorners() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Processing...");
        mProgressDialog.show();

        new FixRotationGetCornersTask().execute();
    }

    private void onBack() {
        if (!mProcessed) {
            mDocumentHolder.removeLastPage();
            mBitmap.recycle();
        }
        onBackPressed();
    }

    private void onFinish() {
        // if the image is not yet processed,
        // it means corner locations are set and approved
        if (!mProcessed) {
            mCorners = mCornersView.getPoints();
            for (Point point : mCorners) {
                point.x *= mPreviewRatio;
                point.y *= mPreviewRatio;
            }
            CropTransformBinarizeTask task = new CropTransformBinarizeTask(mDocumentHolder.getLastPageBitmap(), mCorners);
            task.execute();
        } else {
            Intent intent = new Intent("new-page");
            LocalBroadcastManager.getInstance(CornersActivity.this).sendBroadcast(intent);
            finish();
        }
    }

    public class FixRotationGetCornersTask extends AsyncTask<Void, Bitmap, Void> {

        private int mContainerWidth;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mContainerWidth = mContainer.getWidth();
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
            mImageView.setImageBitmap(values[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {
            fixRotation();
            mPreviewRatio = (double) mBitmap.getWidth() / (double) mContainerWidth;
            mBitmap = Bitmap.createScaledBitmap(mBitmap,
                    mContainerWidth, (int)(mBitmap.getHeight() / mPreviewRatio), true);
            this.publishProgress(mBitmap);
            mCorners = mDocumentFinder.findCorners(mBitmap);
            sortCorners();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mCornersView.setPoints(mCorners);
            mCornersView.setVisibility(View.VISIBLE);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    mBitmap.getWidth(), mBitmap.getHeight());
            layoutParams.gravity = Gravity.CENTER;
            mCornersView.setLayoutParams(layoutParams);

            mProgressDialog.dismiss();
        }

        private void fixRotation() {
            boolean isOrientationPortrait = getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT;

            if (mIsCameraPicture && isOrientationPortrait) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                mDocumentHolder.removeLastPage();
                mDocumentHolder.addPage(new Page(mBitmap));
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
//            Imgproc.adaptiveThreshold(grayMat, grayMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
//                    Imgproc.THRESH_BINARY, 3, 10);
            Imgproc.threshold(grayMat, grayMat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

            image = Bitmap.createBitmap(imageMat.width(), imageMat.height(), image.getConfig());

//            Utils.matToBitmap(imageMat, image);
            Utils.matToBitmap(grayMat, image);

            imageRect.release();
            transformRect.release();
            transformMat.release();
            imageMat.release();

            DocumentHolder.getInstance().removeLastPage();
            DocumentHolder.getInstance().addPage(new Page(image));
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mProcessed = true;
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
