package com.dailystudio.objectdetection.loader;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.dailystudio.app.loader.AbsAsyncDataLoader;
import com.dailystudio.objectdetection.database.DetectedImage;
import com.dailystudio.objectdetection.database.DetectedImageDatabaseModel;

public class ViewDetectedImageLoader extends AbsAsyncDataLoader<DetectedImage> {

    private String mSrcPath;

    public ViewDetectedImageLoader(Context context, String srcPath) {
        super(context);

        mSrcPath = srcPath;
    }

    @Nullable
    @Override
    public DetectedImage loadInBackground() {
        if (TextUtils.isEmpty(mSrcPath)) {
            return null;
        }

        return DetectedImageDatabaseModel.getDetectedImage(
                getContext(), mSrcPath);
    }

}
