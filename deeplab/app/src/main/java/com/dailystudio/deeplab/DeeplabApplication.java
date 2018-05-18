package com.dailystudio.deeplab;

import com.dailystudio.app.DevBricksApplication;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class DeeplabApplication extends DevBricksApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        ImageLoaderConfiguration config =
                new ImageLoaderConfiguration.Builder(this).build();

        ImageLoader.getInstance().init(config);
    }

   @Override
    protected boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }

}
