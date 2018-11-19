package com.dailystudio.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.api.Classifier;
import com.dailystudio.objectdetection.api.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.util.List;

public class DetectAsyncTask extends AsyncTask<Context, Void, List<Classifier.Recognition>> {

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    private String mFilePath;

    private static Classifier sDetector = null;

    public DetectAsyncTask(String filePath) {
        mFilePath = filePath;
    }

    @Override
    protected List<Classifier.Recognition> doInBackground(Context... contexts) {
        if (contexts == null
                || contexts.length <= 0) {
            return null;
        }

        final Context context = contexts[0];

        if (sDetector == null) {
            sDetector = createDetector(context);
        }

        if (sDetector == null) {
            return null;
        }

        final long start = System.currentTimeMillis();
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(mFilePath);
            bitmap = BitmapUtils.scaleBitmap(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
        } catch (OutOfMemoryError e) {
            Logger.error("decode and crop image[%s] failed: %s",
                    mFilePath,
                    e.toString());

            bitmap = null;
        }

        if (bitmap == null) {
            return null;
        }

        final long startOfAnalysis = System.currentTimeMillis();

        List<Classifier.Recognition> results =
                sDetector.recognizeImage(bitmap);
        final long end = System.currentTimeMillis();

        final long duration = (end - start);
        final long durationOfAnalysis = end - startOfAnalysis;
        final long durationOfDecode = duration - durationOfAnalysis;
        Logger.debug("detection is accomplished in %sms [decode: %dms, detect: %dms].",
                duration, durationOfDecode, durationOfAnalysis);

        return results;
    }

    @Override
    protected void onPostExecute(List<Classifier.Recognition> recognitions) {
        super.onPostExecute(recognitions);

        Logger.debug("recognitions: %s", recognitions);
    }

    private Classifier createDetector(Context context) {
        if (context == null) {
            return null;
        }

        Classifier detector;
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            Logger.error("Initializing classifier failed: %s", e.toString());

            detector = null;
        }

        return detector;
    }


}
