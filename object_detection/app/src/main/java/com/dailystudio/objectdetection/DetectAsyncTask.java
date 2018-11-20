package com.dailystudio.objectdetection;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;

import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.api.Classifier;
import com.dailystudio.objectdetection.api.ObjectDetectionModel;
import com.dailystudio.objectdetection.database.DetectedImage;
import com.dailystudio.objectdetection.database.DetectedImageDatabaseModel;

import java.util.List;

public class DetectAsyncTask extends AsyncTask<Context, Void, List<Classifier.Recognition>> {

    private final static int MAX_OUTPUT_SIZE = 1920;

    private final static int[] FRAME_COLORS = {
            R.color.md_red_400,
            R.color.md_orange_400,
            R.color.md_amber_400,
            R.color.md_green_400,
            R.color.md_teal_400,
            R.color.md_blue_400,
            R.color.md_purple_400,
    };

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
            bitmap = BitmapUtils.scaleBitmapRatioLocked(bitmap, MAX_OUTPUT_SIZE, MAX_OUTPUT_SIZE);
        } catch (OutOfMemoryError e) {
            Logger.error("decode and crop image[%s] failed: %s",
                    mFilePath,
                    e.toString());

            bitmap = null;
        }

        if (bitmap == null) {
            return null;
        }


        DetectedImage.Orientation orientation = DetectedImage.Orientation.LANDSCAPE;
        if (bitmap.getWidth() < bitmap.getHeight()) {
            orientation = DetectedImage.Orientation.PORTRAIT;
        }

        final long startOfAnalysis = System.currentTimeMillis();

        List<Classifier.Recognition> results =
                ObjectDetectionModel.detectImage(bitmap, .2f);

        final long startOfTagging = System.currentTimeMillis();
        final String outputPath = Directories.getDetectedFilePath(String.format("%d.jpg",
                startOfTagging));

        final Bitmap tagBitmap = tagRecognitionOnBitmap(context, bitmap, results);
        if (tagBitmap != null) {
            BitmapUtils.saveBitmap(tagBitmap, outputPath);

            DetectedImageDatabaseModel.saveDetectedImage(context,
                    mFilePath,
                    outputPath,
                    orientation);
        }

        final long end = System.currentTimeMillis();

        final long duration = (end - start);
        final long durationOfTagging = end - startOfTagging;
        final long durationOfAnalysis = startOfTagging - startOfAnalysis;
        final long durationOfDecode = startOfAnalysis - start;
        Logger.debug("detection is accomplished in %sms [decode: %dms, detect: %dms, tag: %dms].",
                duration, durationOfDecode, durationOfAnalysis, durationOfTagging);

        return results;
    }

    private Bitmap tagRecognitionOnBitmap(Context context, Bitmap origBitmap, List<Classifier.Recognition> recognitions) {
        if (context == null
                || origBitmap == null
                || recognitions == null
                || recognitions.size() <= 0) {
            return origBitmap;
        }

        final Resources res = context.getResources();
        if (res == null) {
            return origBitmap;
        }

        Bitmap taggedBitmap = origBitmap.copy(Bitmap.Config.ARGB_8888, true);

        final Canvas canvas = new Canvas(taggedBitmap);
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20.0f);

        int colorIndex = 0;
        for (final Classifier.Recognition r : recognitions) {
            paint.setColor(res.getColor(FRAME_COLORS[colorIndex],
                    context.getTheme()));

            final RectF location = r.getLocation();
            canvas.drawRect(location, paint);

            colorIndex++;
            if (colorIndex >= FRAME_COLORS.length) {
                break;
            }
        }

        return taggedBitmap;
    }

    @Override
    protected void onPostExecute(List<Classifier.Recognition> recognitions) {
        super.onPostExecute(recognitions);

        Logger.debug("recognitions: %s", recognitions);
    }


}
