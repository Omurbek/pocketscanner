package com.example.jajac.pocketsummary;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class PagesRecyclerViewAdapter extends RecyclerView.Adapter<PagesRecyclerViewAdapter.PageViewHolder> {

    private Context mContext;
    private final List<Bitmap> mItems;

    public PagesRecyclerViewAdapter(Context context, List<Bitmap> items) {
        mContext = context;
        mItems = items;
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

        Bitmap smallBitmap = Bitmap.createScaledBitmap(mItems.get(position), resizedWidth, resizedHeight, false);

        holder.mItem = mItems.get(position);
        holder.mImgView.setImageBitmap(smallBitmap);
        holder.mStatusText.setText(mContext.getString(R.string.page_item_pending_text));
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
        public Bitmap mItem;

        public PageViewHolder(View itemView, ViewGroup parent) {
            super(itemView);
            mItemView = itemView;

            mImgView = itemView.findViewById(R.id.page_item_img);
            mStatusText = itemView.findViewById(R.id.page_item_status_txt);
            mProgressBar = itemView.findViewById(R.id.page_item_progress);
        }
    }

}
