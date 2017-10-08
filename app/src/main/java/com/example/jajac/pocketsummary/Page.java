package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;

import java.util.List;

public class Page {

    public static final int STATE_PENDING = 0;
    public static final int STATE_PROCESSING = 1;
    public static final int STATE_FINISHED = 2;

    private int mState = STATE_PENDING;
    private Bitmap mBitmap;

    private List<String> mWords;

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

    public List<String> getWords() {
        return mWords;
    }

    public void setWords(List<String> mWords) {
        this.mWords = mWords;
    }

}
