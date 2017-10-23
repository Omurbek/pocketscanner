package rs.elfak.jajac.pocketscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.jajac.pocketscanner.R;

import java.util.List;

public class PagesRecyclerViewAdapter extends RecyclerView.Adapter<PagesRecyclerViewAdapter.PageViewHolder> {

    private Context mContext;
    private final List<Page> mItems;
    private OnPageClickListener mListener;

    public interface OnPageClickListener {
        void onPageClicked(int pageIndex);
    }

    public PagesRecyclerViewAdapter(Context context, List<Page> items, OnPageClickListener listener) {
        mContext = context;
        mItems = items;
        mListener = listener;
    }

    @Override
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_list_item, parent, false);
        return new PageViewHolder(view, parent);
    }

    @Override
    public void onBindViewHolder(final PageViewHolder holder, int position) {
        int bitmapWidth = mItems.get(position).getWidth();
        int bitmapHeight = mItems.get(position).getHeight();
        int viewSize = (int) mContext.getResources().getDimension(R.dimen.page_item_img_size);
        int resizedWidth, resizedHeight;

        if (bitmapWidth > bitmapHeight) {
            double ratio = (double) bitmapWidth / (double) viewSize;
            resizedWidth = viewSize;
            resizedHeight = (int) (bitmapHeight / ratio);
        } else {
            double ratio = (double) bitmapHeight / (double) viewSize;
            resizedHeight = viewSize;
            resizedWidth = (int) (bitmapWidth / ratio);
        }

        Page page = mItems.get(position);

        Bitmap smallBitmap = Bitmap.createScaledBitmap(page.getBitmap(), resizedWidth, resizedHeight, false);

        holder.mItem = page;
        holder.mImgView.setImageBitmap(smallBitmap);

        switch (page.getState()) {
            case Page.STATE_ERROR:
                holder.mStatusText.setText(mContext.getString(R.string.page_item_error_text));
                holder.mProgressBar.setVisibility(View.INVISIBLE);
                break;
            case Page.STATE_PENDING:
                holder.mStatusText.setText(mContext.getString(R.string.page_item_pending_text));
                holder.mProgressBar.setVisibility(View.INVISIBLE);
                break;
            case Page.STATE_DETECTING_TEXT:
                holder.mStatusText.setText(mContext.getString(R.string.page_item_detecting_text));
                holder.mProgressBar.setVisibility(View.VISIBLE);
                break;
            case Page.STATE_TRANSLATING:
                holder.mStatusText.setText(mContext.getString(R.string.page_item_translating_text));
                holder.mProgressBar.setVisibility(View.VISIBLE);
                break;
            case Page.STATE_FINISHED:
                holder.mStatusText.setText(mContext.getString(R.string.page_item_finished_text));
                holder.mProgressBar.setVisibility(View.INVISIBLE);
                break;
        }

        holder.mItemView.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onPageClicked(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class PageViewHolder extends RecyclerView.ViewHolder {
        public final View mItemView;
        public final ImageView mImgView;
        public final TextView mStatusText;
        public final ProgressBar mProgressBar;
        public Page mItem;

        public PageViewHolder(View itemView, ViewGroup parent) {
            super(itemView);
            mItemView = itemView;

            mImgView = itemView.findViewById(R.id.page_item_img);
            mStatusText = itemView.findViewById(R.id.page_item_status_txt);
            mProgressBar = itemView.findViewById(R.id.page_item_progress);
        }
    }

}
