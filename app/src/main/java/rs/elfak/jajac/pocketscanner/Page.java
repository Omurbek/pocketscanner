package rs.elfak.jajac.pocketscanner;

import android.graphics.Bitmap;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Page {

    public static final int STATE_ERROR = -1;
    public static final int STATE_PENDING = 0;
    public static final int STATE_DETECTING_TEXT = 1;
    public static final int STATE_TRANSLATING = 2;
    public static final int STATE_FINISHED = 3;

    private int mState = STATE_PENDING;

    private Bitmap mBitmap;

    private Location mLocation;
    private int mDaysRelevant;

    private List<TextPiece> mBlocks;
    private List<TextPiece> mTranslatedBlocks = new ArrayList<>();
    private String mOriginal;
    private String mTranslation;

    public Page() {

    }

    public Page(Bitmap bitmap, Location location) {
        mBitmap = bitmap;
        mLocation = location;
    }

    @Exclude
    public int getState() {
        return mState;
    }

    public void setState(int mState) {
        this.mState = mState;
    }

    @Exclude
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location location) {
        this.mLocation = location;
    }

    public int getDaysRelevant() {
        return mDaysRelevant;
    }

    public void setDaysRelevant(int daysRelevant) {
        this.mDaysRelevant = daysRelevant;
    }

    @Exclude
    public int getWidth() {
        return mBitmap.getWidth();
    }

    @Exclude
    public int getHeight() {
        return mBitmap.getHeight();
    }

    @Exclude
    public List<TextPiece> getBlocks() {
        return mBlocks;
    }

    public void setBlocksAndMakeOriginal(List<TextPiece> blocks) {
        String result = null;
        if (blocks != null && blocks.size() > 0) {
            this.mBlocks = blocks;
            result = "";
            for (int i = 0; i < mBlocks.size() - 1; i++) {
                result += mBlocks.get(i).getText() + "\n\n";
            }
            result += mBlocks.get(mBlocks.size() - 1).getText();
        }
        mOriginal = result;
    }

    public String getOriginal() {
        return mOriginal;
    }

    public void setOriginal(String original) {
        this.mOriginal = original;
    }

    public void setTranslationTextAndBlocks(String translation) {
        mTranslation = translation;
        if (mBlocks != null) {
            String[] pieces = translation.split("\n\n");
            for (int i = 0; i < pieces.length; i++) {
                mTranslatedBlocks.add(new TextPiece(mBlocks.get(i).getBoundingBox(), pieces[i]));
            }
        }
    }

    public void setTranslation(String translation) {
        this.mTranslation = translation;
    }

    public String getTranslation() {
        return mTranslation;
    }

}
