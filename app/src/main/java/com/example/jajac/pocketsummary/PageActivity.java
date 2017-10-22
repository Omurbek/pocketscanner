package com.example.jajac.pocketsummary;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PageActivity extends AppCompatActivity {

    private int mPageIndex;

    private TextView mTextView;
    private LinearLayout mShareBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = findViewById(R.id.activity_page_text);
        mShareBtn = findViewById(R.id.activity_page_btn_share);
        mShareBtn.setOnClickListener(v -> onShareClicked());

        mPageIndex = getIntent().getIntExtra("page", 0);

        showTranslation();
    }

    private void showTranslation() {
        Page page = DocumentHolder.getInstance().getPage(mPageIndex);

        mTextView.setText(page.getTranslation() + "\n\n" + page.getTranslation() + "\n\n" + page.getTranslation());
    }

    private void onShareClicked() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(R.layout.dialog_share);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

}
