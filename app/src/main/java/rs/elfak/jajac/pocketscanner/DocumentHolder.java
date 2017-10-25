package rs.elfak.jajac.pocketscanner;

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

    public void setPageBlocks(int index, List<TextPiece> blocks) {
        mPages.get(index).setBlocksAndMakeOriginal(blocks);
    }

    public List<TextPiece> getPageBlocks(int index) {
        return mPages.get(index).getBlocks();
    }

}
