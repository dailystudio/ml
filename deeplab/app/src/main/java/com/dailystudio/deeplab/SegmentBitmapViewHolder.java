package com.dailystudio.deeplab;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dailystudio.app.ui.AbsArrayItemViewHolder;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class SegmentBitmapViewHolder extends AbsArrayItemViewHolder<SegmentBitmap> {

    private final static DisplayImageOptions DEFAULT_IMAGE_LOADER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .showImageOnLoading(null)
                    .resetViewBeforeLoading(true)
                    .build();

    private ImageView mImageView;
    private TextView mLabelView;

    public SegmentBitmapViewHolder(View itemView) {
        super(itemView);

        setupViews(itemView);
    }

    private void setupViews(View itemView) {
        if (itemView == null) {
            return;
        }

        mImageView = itemView.findViewById(R.id.image);
        mLabelView = itemView.findViewById(R.id.label);
    }

    @Override
    public void bindItem(final Context context, SegmentBitmap segmentBitmap) {
        if (mImageView != null) {
            if (segmentBitmap.bitmapUri != null) {
                    ImageLoader.getInstance().displayImage(
                            segmentBitmap.bitmapUri.toString(),
                            mImageView,
                            DEFAULT_IMAGE_LOADER_OPTIONS);
            } else {
                mImageView.setImageBitmap(segmentBitmap.bitmap);
            }
        }

        if (mLabelView != null) {
            mLabelView.setText(segmentBitmap.labelResId);
        }
    }

}
