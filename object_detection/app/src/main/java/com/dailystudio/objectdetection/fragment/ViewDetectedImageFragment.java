package com.dailystudio.objectdetection.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dailystudio.app.fragment.AbsLoaderFragment;
import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.Constants;
import com.dailystudio.objectdetection.R;
import com.dailystudio.objectdetection.database.DetectedImage;
import com.dailystudio.objectdetection.loader.LoaderIds;
import com.dailystudio.objectdetection.loader.ViewDetectedImageLoader;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Created by nanye on 18/2/26.
 */

public class ViewDetectedImageFragment extends AbsLoaderFragment<DetectedImage> {

    private String mSrcPath;
    private ImageView mImageView;

    private DetectedImage mCurrentImage = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_detected_image, null);

        setupViews(view);

        return view;
    }

    @Override
    public void bindIntent(Intent intent) {
        super.bindIntent(intent);

        if (intent == null) {
            return;
        }

        String srcPath = intent.getStringExtra(Constants.EXTRA_SRC_PATH);
        if (TextUtils.isEmpty(srcPath)) {
            Logger.warn("srcPath is missing.");

            return;
        }

        mSrcPath = srcPath;

        restartLoader();
    }

    private void setupViews(View fragmentView) {
        if (fragmentView == null) {
            return;
        }

        mImageView = fragmentView.findViewById(R.id.detected_image);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.LOADER_VIEW_IMAGE;
    }

    @Override
    protected Bundle createLoaderArguments() {
        return new Bundle();
    }

    @Override
    public Loader<DetectedImage> onCreateLoader(int id, Bundle args) {
        return new ViewDetectedImageLoader(getContext(), mSrcPath);
    }

    @Override
    public void onLoadFinished(Loader<DetectedImage> loader, DetectedImage image) {
        super.onLoadFinished(loader, image);

        mCurrentImage = image;

        syncImageDisplay();
    }

    private void syncImageDisplay() {
        if (mImageView != null
                && mCurrentImage != null) {
            final String imagePath = mCurrentImage.getDetectedPath();

            ImageLoader.getInstance().displayImage(
                    "file://" + imagePath,
                    mImageView,
                    Constants.DEFAULT_IMAGE_LOADER_OPTIONS);

            if (mCurrentImage.getOrientation() == DetectedImage.Orientation.LANDSCAPE) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        }
    }
}
