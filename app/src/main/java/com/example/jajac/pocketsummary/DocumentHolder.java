package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

class DocumentHolder {

    private static DocumentHolder mInstance;
    private List<Bitmap> mPages = new ArrayList<>();

    private DocumentHolder() {}

    public static synchronized DocumentHolder getInstance() {
        if (mInstance == null) {
            mInstance = new DocumentHolder();
        }
        return mInstance;
    }

    public List<Bitmap> getAllPages() {
        return this.mPages;
    }

    public void addPage(Bitmap bitmap) {
        mPages.add(bitmap);
    }

    public Bitmap getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    public void removeLastPage() {
        mPages.remove(mPages.size() - 1);
    }

}
