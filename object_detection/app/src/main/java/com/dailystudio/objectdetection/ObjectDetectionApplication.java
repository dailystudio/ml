package com.dailystudio.objectdetection;

import com.dailystudio.app.DevBricksApplication;
import com.facebook.stetho.Stetho;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class ObjectDetectionApplication extends DevBricksApplication {


    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.USE_STETHO) {
            Stetho.initializeWithDefaults(this);
        }

        ImageLoaderConfiguration config =
                new ImageLoaderConfiguration.Builder(this).build();

        ImageLoader.getInstance().init(config);
    }

    @Override
    protected boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
