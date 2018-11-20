package com.dailystudio.objectdetection;

import android.content.Context;

import com.nostra13.universalimageloader.core.DisplayImageOptions;

public class Constants {

    private final static String FILE_PROVIDER_AUTHORITY_SUFFIX =
            ".fileprovider";

    public final static String getFileProvideAuthority(Context context) {
        if (context == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(context.getPackageName());

        builder.append(FILE_PROVIDER_AUTHORITY_SUFFIX);

        return builder.toString();
    }

    public static final String EXTRA_SRC_PATH =
            "freestyles.intent.EXTRA_SRC_PATH";

    public final static DisplayImageOptions DEFAULT_IMAGE_LOADER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .showImageOnLoading(null)
                    .resetViewBeforeLoading(true)
                    .build();

    public final static DisplayImageOptions PREVIEW_IMAGE_LOADER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();

    public final static DisplayImageOptions STYLE_THUMB_IMAGE_LOADER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .showImageOnLoading(R.mipmap.ic_launcher)
                    .build();

}
