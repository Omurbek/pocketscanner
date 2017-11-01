package rs.elfak.jajac.pocketscanner;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

class DocumentsHolder {

    private static DocumentsHolder instance;
    private List<Document> documents = new ArrayList<>();

    private DocumentsHolder() {}

    public static synchronized DocumentsHolder getInstance() {
        if (instance == null) {
            instance = new DocumentsHolder();
        }
        return instance;
    }

    public List<Document> getAllDocuments() {
        return this.documents;
    }

    public int getDocumentCount() {
        return this.documents.size();
    }

    public void addDocument(Document doc) {
        documents.add(doc);
    }

    public Document getLastDocument() {
        return documents.get(documents.size() - 1);
    }

    public Document getDocumentAt(int index) {
        return documents.get(index);
    }

    public void removeLastDocument() {
        documents.remove(documents.size() - 1);
    }

    public Bitmap getDocumentBitmap(int index) {
        return getDocumentAt(index).getBitmap();
    }

    public Bitmap getLastDocumentBitmap() {
        return getLastDocument().getBitmap();
    }

    public void setDocumentState(int index, int state) {
        documents.get(index).setState(state);
    }

    public void setDocumentText(int index, String text) {
        documents.get(index).setText(text);
    }

    public void setLastDocumentBitmap(Bitmap bitmap) {
        this.getLastDocument().setBitmap(bitmap);
    }

}
