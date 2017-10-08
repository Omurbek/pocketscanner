package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;

public class Page {

    private static final int STATE_PENDING = 0;
    private static final int STATE_PROCESSING = 1;
    private static final int STATE_FINISHED = 2;

    private int mState = STATE_PENDING;
    private Bitmap mBitmap;

    public Page(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public int getState() {
        return mState;
    }

    public void setState(int mState) {
        this.mState = mState;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    public int getWidth() {
        return mBitmap.getWidth();
    }

    public int getHeight() {
        return mBitmap.getHeight();
    }

}
