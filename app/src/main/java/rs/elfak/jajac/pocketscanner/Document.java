package rs.elfak.jajac.pocketscanner;

import android.graphics.Bitmap;

import com.google.firebase.database.Exclude;

public class Document {

    private DocumentState state = DocumentState.PENDING;

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
    public DocumentState getState() {
        return state;
    }

    public void setState(DocumentState state) {
        this.state = state;
    }

    @Exclude
    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getDaysRelevant() {
        return this.daysRelevant;
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
