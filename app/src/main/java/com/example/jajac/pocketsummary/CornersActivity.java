package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

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
        drawCorners();

        mImage.setImageBitmap(mBitmap);
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

    private void drawCorners() {
        Canvas c = new Canvas(mBitmap);
        Paint p = new Paint();
        p.setColor(Color.RED);

        for (Point point : mCorners) {
            c.drawCircle(point.x, point.y, 15, p);
        }
    }
}
