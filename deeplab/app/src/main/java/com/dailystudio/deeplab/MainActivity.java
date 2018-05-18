package com.dailystudio.deeplab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.ImageView;

import com.dailystudio.deeplab.ml.DeeplabModel;
import com.dailystudio.deeplab.ml.ImageUtils;
import com.dailystudio.development.Logger;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class MainActivity extends AppCompatActivity {

    private final static String DEAULT_IMAGE = "/sdcard/deeplab/test.jpg";

    public final static DisplayImageOptions DEFAULT_IMAGE_LOADER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .showImageOnLoading(null)
                    .resetViewBeforeLoading(true)
                    .build();

    private class SegmentImageAsyncTask extends AsyncTask<Context, Void, Bitmap> {

        private String mImagePath;

        private SegmentImageAsyncTask(String imagePath) {
            mImagePath = imagePath;
        }

        @Override
        protected Bitmap doInBackground(Context... contexts) {
            if (contexts == null
                    || contexts.length <= 0) {
                return null;
            }

            final Context context = contexts[0];
            if (TextUtils.isEmpty(mImagePath)) {
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(mImagePath);
            if (bitmap == null) {
                return null;
            }

            final int w = bitmap.getWidth();
            final int h = bitmap.getHeight();

            float resizeRatio = (float) DeeplabModel.INPUT_SIZE / Math.max(bitmap.getWidth(), bitmap.getHeight());
            int rw = Math.round(w * resizeRatio);
            int rh = Math.round(h * resizeRatio);

            Logger.debug("resize bitmap: ratio = %f, [%d x %d] -> [%d x %d]",
                    resizeRatio, w, h, rw, rh);

            Bitmap resized = ImageUtils.tfResizeBilinear(bitmap, rw, rh);

            return DeeplabModel.segment(resized);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (mSegmentImageView != null) {
                mSegmentImageView.setImageBitmap(bitmap);
            }
        }
    }

    private ImageView mSrcImageView;
    private ImageView mSegmentImageView;
    private ImageView mOverlayImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setupViews();
    }

    private void setupViews() {
        mSrcImageView = findViewById(R.id.src_img);
        if (mSrcImageView != null) {
            ImageLoader.getInstance().clearDiskCache();
            ImageLoader.getInstance().clearMemoryCache();
            ImageLoader.getInstance().displayImage(
                    "file://" + DEAULT_IMAGE,
                    mSrcImageView,
                    DEFAULT_IMAGE_LOADER_OPTIONS);
        }

        mSegmentImageView = findViewById(R.id.segment_img);
        mOverlayImageView = findViewById(R.id.overlay_img);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new SegmentImageAsyncTask(DEAULT_IMAGE).execute(getApplication());
    }

}
