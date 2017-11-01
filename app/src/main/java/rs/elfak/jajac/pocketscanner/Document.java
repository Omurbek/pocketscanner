package rs.elfak.jajac.pocketscanner;

import android.graphics.Bitmap;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Document {

    public static final int STATE_ERROR = -1;
    public static final int STATE_PENDING = 0;
    public static final int STATE_DETECTING_TEXT = 1;
    public static final int STATE_TRANSLATING = 2;
    public static final int STATE_FINISHED = 3;

    private int state = STATE_PENDING;

    private Bitmap bitmap;

    private Location location;
    private int daysRelevant;
    private DocumentType type;

    private String text;
    private String translation;

    public Document() {
        // Needed for firebase .setValue(Document.class)
    }

    public Document(Bitmap bitmap, Location location) {
        this.bitmap = bitmap;
        this.location = location;
        this.daysRelevant = 1;
        this.type = DocumentType.INFORMATION;
    }

    @Exclude
    public int getState() {
        return state;
    }

    public void setState(int mState) {
        this.state = mState;
    }

    @Exclude
    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap mBitmap) {
        this.bitmap = mBitmap;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getDaysRelevant() {
        return daysRelevant;
    }

    public void setDaysRelevant(int daysRelevant) {
        this.daysRelevant = daysRelevant;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    @Exclude
    public int getWidth() {
        return bitmap.getWidth();
    }

    @Exclude
    public int getHeight() {
        return bitmap.getHeight();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

}
