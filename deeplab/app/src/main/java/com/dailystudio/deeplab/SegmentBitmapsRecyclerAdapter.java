package com.dailystudio.deeplab;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.dailystudio.app.ui.AbsArrayRecyclerAdapter;
import com.dailystudio.development.Logger;
import com.yarolegovich.discretescrollview.DSVOrientation;

public class SegmentBitmapsRecyclerAdapter
        extends AbsArrayRecyclerAdapter<SegmentBitmap, SegmentBitmapViewHolder> {

    private DSVOrientation mOrientation = DSVOrientation.VERTICAL;

    public SegmentBitmapsRecyclerAdapter(Context context) {
        super(context);
    }

    @Override
    public SegmentBitmapViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Logger.debug("orientation of holder: %s",
                (mOrientation == DSVOrientation.VERTICAL ? "VERTICAL" : "HORIZONTAL"));
        View view = mLayoutInflater.inflate(
                mOrientation == DSVOrientation.VERTICAL ?
                        R.layout.layout_segment_bitmap_v : R.layout.layout_segment_bitmap_h,
                null);

        return new SegmentBitmapViewHolder(view);
    }

    public void setOrientation(DSVOrientation orientation) {
        mOrientation = orientation;

        notifyDataSetChanged();
    }

}
