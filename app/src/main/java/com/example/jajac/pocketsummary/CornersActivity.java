package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class CornersActivity extends AppCompatActivity {

    private static final String TAG = "CornersActivity";

    private ImageView mImage;

    private Bitmap mBitmap;
    private ArrayList<Point> mCorners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corners);

        // Draw this activity full screen, ignoring the status bar (draw below it)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        mImage = findViewById(R.id.activity_corners_img);

        mCorners = getIntent().getParcelableArrayListExtra("corners");
        loadBitmap();
        mImage.setImageBitmap(mBitmap);

        CropTransformBinarizeTask task = new CropTransformBinarizeTask(mBitmap, mCorners);
        task.execute();
    }

    private void loadBitmap() {
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

//    private void drawCorners() {
//        Canvas c = new Canvas(mBitmap);
//        Paint p = new Paint();
//        p.setColor(Color.RED);
//
//        for (Point point : mCorners) {
//            c.drawCircle(point.x, point.y, 15, p);
//        }
//    }

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
            sortCorners();
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
            mImage.setImageBitmap(bitmap);
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
