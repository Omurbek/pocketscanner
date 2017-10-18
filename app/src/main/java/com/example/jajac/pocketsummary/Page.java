package com.example.jajac.pocketsummary;

import android.graphics.Bitmap;

import java.util.List;

public class Page {

    public static final int STATE_PENDING = 0;
    public static final int STATE_DETECTING_TEXT = 1;
    public static final int STATE_TRANSLATING = 2;
    public static final int STATE_FINISHED = 3;

    private int mState = STATE_PENDING;
    private Bitmap mBitmap;

    private List<TextPiece> mBlocks;
    private List<TextPiece> mTranslatedBlocks;

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

    public List<TextPiece> getBlocks() {
        return mBlocks;
    }

    public void setBlocks(List<TextPiece> blocks) {
        this.mBlocks = blocks;
    }

    public String getFullText() {
        String result = "";
        for (int i = 0; i < mBlocks.size() - 1; i++) {
            result += mBlocks.get(i).getText() + "\n\n\n";
        }
        result += mBlocks.get(mBlocks.size() - 1).getText();
        return result;
    }

    public void setTranslation(String translation) {
        String[] pieces = translation.split("\n\n\n");
        for (int i = 0; i < pieces.length; i++) {
            mTranslatedBlocks.add(new TextPiece(mBlocks.get(i).getBoundingBox(), pieces[i]));
        }
    }

}
