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

    public void setBlocks(List<TextPiece> blocks) {
        this.mBlocks = blocks;
    }

    @Exclude
    public String getFullText() {
        String result = null;
        if (mBlocks != null && mBlocks.size() > 0) {
            result = "";
            for (int i = 0; i < mBlocks.size() - 1; i++) {
                result += mBlocks.get(i).getText() + "\n\n\n";
            }
            result += mBlocks.get(mBlocks.size() - 1).getText();
        }
        return result;
    }

    public void setTranslation(String translation) {
        String[] pieces = translation.split("\n\n\n");
        for (int i = 0; i < pieces.length; i++) {
            mTranslatedBlocks.add(new TextPiece(mBlocks.get(i).getBoundingBox(), pieces[i]));
        }
    }

    public String getTranslation() {
        String fullText = "";

        if (mTranslatedBlocks.size() > 0) {
            for (int i = 0; i < mTranslatedBlocks.size() - 1; i++) {
                fullText += mTranslatedBlocks.get(i).getText() + "\n\n";
            }
            fullText += mTranslatedBlocks.get(mTranslatedBlocks.size() - 1).getText();
        }
        return fullText;
    }

}
