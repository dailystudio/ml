package com.dailystudio.objectdetection;

import com.dailystudio.app.DevBricksApplication;
import com.facebook.stetho.Stetho;

public class ObejctDetectionApplication extends DevBricksApplication {


    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.USE_STETHO) {
            Stetho.initializeWithDefaults(this);
        }

        new DetectAsyncTask("/sdcard/detect_input.jpg").execute(this);
    }

    @Override
    protected boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
