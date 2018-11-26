package com.dailystudio.objectdetection.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;

import com.dailystudio.app.ui.AbsArrayItemViewHolder;
import com.dailystudio.app.utils.ActivityLauncher;
import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.Constants;
import com.dailystudio.objectdetection.R;
import com.dailystudio.objectdetection.ViewDetectedImageActivity;
import com.dailystudio.objectdetection.database.DetectedImage;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;

/**
 * Created by nanye on 18/1/4.
 */

public class DetectedImageViewHolder extends AbsArrayItemViewHolder<DetectedImage> {

    private ImageView mStyledThumb;
    private View mShareView;

    public DetectedImageViewHolder(View itemView) {
        super(itemView);

        setupViews(itemView);
    }

    private void setupViews(View itemView) {
        if (itemView == null) {
            return;
        }

        mStyledThumb = itemView.findViewById(R.id.detected_thumb);

        mShareView = itemView.findViewById(R.id.share_image);
        if (mShareView != null) {
            mShareView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Object o = v.getTag();
                    Logger.debug("share image: %s", o);
                    if (o instanceof DetectedImage == false) {
                        return;
                    }

                    final Context context = v.getContext();
                    if (context == null) {
                        return;
                    }

                    DetectedImage DetectedImage = (DetectedImage)o;

                    File sharedFile = new File(DetectedImage.getDetectedPath());

                    Uri imageUri = FileProvider.getUriForFile(context,
                            Constants.getFileProvideAuthority(context),
                            sharedFile);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/*");

                    share.putExtra(Intent.EXTRA_TEXT,
                            context.getString(R.string.prompt_image_share));
                    share.putExtra(Intent.EXTRA_STREAM, imageUri);

                    ActivityLauncher.launchActivity(context, Intent.createChooser(share, "Share Image"));
                }

            });
        }

    }

    @Override
    public void bindItem(final Context context, final DetectedImage image) {
        if (image == null) {
            return;
        }

        final String styledPath = image.getDetectedPath();

        if (mShareView != null) {
            mShareView.setTag(image);
        }

        if (mStyledThumb != null) {
            ImageLoader.getInstance().displayImage(
                    "file://" + styledPath,
                    mStyledThumb,
                    Constants.DEFAULT_IMAGE_LOADER_OPTIONS);

            mStyledThumb.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Logger.debug("click on image: %s", image);

                    Intent i = new Intent();

                    i.setClass(context.getApplicationContext(),
                            ViewDetectedImageActivity.class);
                    i.putExtra(Constants.EXTRA_SRC_PATH, image.getSourcePath());

                    ActivityLauncher.launchActivity(context, i);
                }

            });

        }
    }

}
