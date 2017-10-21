package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class PageActivity extends AppCompatActivity {

    private int mPageIndex;

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = findViewById(R.id.activity_page_text);

        mPageIndex = getIntent().getIntExtra("page", 0);

        drawImage();
    }

    private void drawImage() {
        Page page = DocumentHolder.getInstance().getPage(mPageIndex);

        mTextView.setText(page.getTranslation());
    }

}
