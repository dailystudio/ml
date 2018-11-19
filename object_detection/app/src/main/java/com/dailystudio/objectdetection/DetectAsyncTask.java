package com.dailystudio.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.api.Classifier;

import java.util.List;

public class DetectAsyncTask extends AsyncTask<Context, Void, List<Classifier.Recognition>> {

    private String mFilePath;

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

        final long start = System.currentTimeMillis();
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(mFilePath);
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
                ObjectDetectionModel.detectImage(bitmap);
        final long end = System.currentTimeMillis();

        final long duration = (end - start);
        final long durationOfAnalysis = end - startOfAnalysis;
        final long durationOfDecode = duration - durationOfAnalysis;
        Logger.debug("detection is accomplished in %sms [decode: %dms, detect: %dms].",
                duration, durationOfDecode, durationOfAnalysis);

        return results;
    }

    private Bitmap tagRecognitionOnBitmap(Bitmap original, List<Classifier.Recognition> recognitions) {
        return null;
    }

    @Override
    protected void onPostExecute(List<Classifier.Recognition> recognitions) {
        super.onPostExecute(recognitions);

        Logger.debug("recognitions: %s", recognitions);
    }


}
