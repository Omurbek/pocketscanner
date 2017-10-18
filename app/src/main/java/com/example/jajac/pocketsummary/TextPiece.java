package com.example.jajac.pocketsummary;

import android.graphics.Rect;

public class TextPiece {

    private Rect mBoundingBox;
    private String mText;

    public TextPiece(Rect boundingBox, String text) {
        this.mBoundingBox = boundingBox;
        this.mText = text;
    }

    public Rect getBoundingBox() {
        return mBoundingBox;
    }

    public void setBoundingBox(Rect boundingBox) {
        this.mBoundingBox = boundingBox;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

}
