package com.dailystudio.deeplab;

import android.os.AsyncTask;

import com.dailystudio.app.DevBricksApplication;
import com.dailystudio.deeplab.ml.DeeplabModel;
import com.dailystudio.development.Logger;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class DeeplabApplication extends DevBricksApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        initTfModel();
        ImageLoaderConfiguration config =
                new ImageLoaderConfiguration.Builder(this).build();

        ImageLoader.getInstance().init(config);
    }

    private void initTfModel() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                final  boolean ret = DeeplabModel.initialize();
                Logger.debug("init deeplab model: %s", ret);
                return null;
            }

        }.execute((Void)null);
    }

    @Override
    protected boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }

}
