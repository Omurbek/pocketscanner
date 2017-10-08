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

    public int getPageCount() {
        return this.mPages.size();
    }

    public void addPage(Page bitmap) {
        mPages.add(bitmap);
    }

    public Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    public Page getPage(int index) {
        return mPages.get(index);
    }

    public void removeLastPage() {
        mPages.remove(mPages.size() - 1);
    }

    public Bitmap getPageBitmap(int index) {
        return getPage(index).getBitmap();
    }

    public Bitmap getLastPageBitmap() {
        return getLastPage().getBitmap();
    }

    public void setPageState(int index, int state) {
        mPages.get(index).setState(state);
    }

    public void setPageWords(int index, List<String> words) {
        mPages.get(index).setWords(words);
    }

    public List<String> getPageWords(int index) {
        return mPages.get(index).getWords();
    }

}
