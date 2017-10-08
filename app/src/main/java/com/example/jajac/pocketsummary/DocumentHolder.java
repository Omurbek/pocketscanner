package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

class DocumentHolder {

    private static DocumentHolder mInstance;
    private List<Page> mPages = new ArrayList<>();

    private DocumentHolder() {}

    public static synchronized DocumentHolder getInstance() {
        if (mInstance == null) {
            mInstance = new DocumentHolder();
        }
        return mInstance;
    }

    public List<Page> getAllPages() {
        return this.mPages;
    }

    public void addPage(Page bitmap) {
        mPages.add(bitmap);
    }

    public Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    public void removeLastPage() {
        mPages.remove(mPages.size() - 1);
    }

    public Bitmap getLastPageBitmap() {
        return getLastPage().getBitmap();
    }

}
