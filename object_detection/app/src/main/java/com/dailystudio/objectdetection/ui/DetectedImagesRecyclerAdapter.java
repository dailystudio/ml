package com.dailystudio.objectdetection.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.dailystudio.app.ui.AbsArrayRecyclerAdapter;
import com.dailystudio.objectdetection.R;
import com.dailystudio.objectdetection.database.DetectedImage;

/**
 * Created by nanye on 16/6/9.
 */
public class DetectedImagesRecyclerAdapter
        extends AbsArrayRecyclerAdapter<DetectedImage, DetectedImageViewHolder> {

    public DetectedImagesRecyclerAdapter(Context context) {
        super(context);
    }

    @Override
    public DetectedImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.layout_detected_image , null);

        return new DetectedImageViewHolder(view);
    }

}
