package com.example.jajac.pocketsummary;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;


public class FindDocumentTask extends ProgressTask<Bitmap, Void, Void> {

    public FindDocumentTask(Context context) {
        super(context);
    }

    @Override
    protected Void doInBackground(Bitmap... bitmaps) {
        SystemClock.sleep(5000);
        return null;
    }
}
