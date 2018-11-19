package com.dailystudio.objectdetection;

import com.dailystudio.app.DevBricksApplication;
import com.facebook.stetho.Stetho;

public class ObjectDetectionApplication extends DevBricksApplication {


    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.USE_STETHO) {
            Stetho.initializeWithDefaults(this);
        }
    }

    @Override
    protected boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
