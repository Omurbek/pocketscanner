package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

class BitmapsHolder {

    private static BitmapsHolder mInstance;
    private List<Bitmap> mBitmaps = new ArrayList<>();

    private BitmapsHolder() {}

    public static synchronized BitmapsHolder getInstance() {
        if (mInstance == null) {
            mInstance = new BitmapsHolder();
        }
        return mInstance;
    }

    public void addBitmap(Bitmap bitmap) {
        mBitmaps.add(bitmap);
    }

    public Bitmap getLast() {
        return mBitmaps.get(mBitmaps.size() - 1);
    }

    public void removeLast() {
        mBitmaps.remove(mBitmaps.size() - 1);
    }

}
