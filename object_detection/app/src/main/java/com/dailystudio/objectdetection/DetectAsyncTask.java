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
import com.dailystudio.app.utils.TextUtils;
import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.api.Classifier;
import com.dailystudio.objectdetection.api.ObjectDetectionModel;
import com.dailystudio.objectdetection.database.DetectedImage;
import com.dailystudio.objectdetection.database.DetectedImageDatabaseModel;
import com.dailystudio.objectdetection.ui.ImageDetectionEvent;

import org.greenrobot.eventbus.EventBus;

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

        notifyDetectionState(ImageDetectionEvent.State.DECODING);
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

        notifyDetectionState(ImageDetectionEvent.State.DETECTING);

        final long startOfAnalysis = System.currentTimeMillis();

        List<Classifier.Recognition> results =
                ObjectDetectionModel.detectImage(bitmap, .2f);

        final long startOfTagging = System.currentTimeMillis();
        final String outputPath = Directories.getDetectedFilePath(String.format("%d.jpg",
                startOfTagging));

        notifyDetectionState(ImageDetectionEvent.State.TAGGING);

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
        final int width = taggedBitmap.getWidth();
        final int height = taggedBitmap.getHeight();

        final Canvas canvas = new Canvas(taggedBitmap);

        final int corner = res.getDimensionPixelSize(R.dimen.detect_info_round_corner);

        final int N = recognitions.size();
        int colorIndex = 0;

        final Paint framePaint = new Paint();
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(10.0f);

        Classifier.Recognition r;
        for (int i = 0; i < N; i++) {
            r = recognitions.get(i);
            framePaint.setColor(res.getColor(FRAME_COLORS[colorIndex],
                    context.getTheme()));

            final RectF location = r.getLocation();
            canvas.drawRoundRect(location, corner, corner, framePaint);

            colorIndex++;
            if (colorIndex >= FRAME_COLORS.length) {
                break;
            }
        }

        final Paint textPaint = new Paint();
        int legendFontSize = res.getDimensionPixelSize(R.dimen.detect_info_font_size);
        int legendIndSize = res.getDimensionPixelSize(R.dimen.detect_info_font_size);
        int legendFramePadding = res.getDimensionPixelSize(R.dimen.detect_info_padding);
        textPaint.setTextSize(legendFontSize);

        RectF legendIndFrame = new RectF();

        final float legendTextWidth = width * .3f;
        final float legendHeight = legendIndSize * 1.2f;
        final float legendEnd = width * .95f;
        final float legendIndStart = legendEnd - legendTextWidth - legendIndSize * 1.2f;
        float legendBottom = height * .95f;
        float legendTop = legendBottom - colorIndex * legendHeight;

        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        framePaint.setColor(res.getColor(R.color.semi_black,
                context.getTheme()));

        canvas.drawRoundRect(legendIndStart - legendFramePadding, legendTop - legendFramePadding,
                legendEnd + legendFramePadding, legendBottom + legendFramePadding, corner, corner, framePaint);
        float legendTextStart;
        float legendTextTop;
        float legendIndTop;

        colorIndex = 0;

        String legendText;
        for (int i = 0; i < N; i++) {
            r = recognitions.get(i);

            legendIndTop = legendTop + 0.1f * legendIndSize;
            legendTextStart = legendEnd - legendTextWidth;
            legendTextTop = legendIndTop + (legendIndSize - (textPaint.descent() + textPaint.ascent())) / 2;

            legendIndFrame.set(legendIndStart, legendIndTop,
                    legendIndStart + legendIndSize, legendIndTop + legendIndSize);

            textPaint.setColor(res.getColor(FRAME_COLORS[colorIndex],
                    context.getTheme()));

            legendText = String.format("%s (%3.1f%%)",
                    TextUtils.capitalize(r.getTitle()),
                    r.getConfidence() * 100);
            canvas.drawRoundRect(legendIndFrame, corner, corner, textPaint);
            canvas.drawText(legendText, legendTextStart, legendTextTop,
                    textPaint);
            colorIndex++;
            if (colorIndex >= FRAME_COLORS.length) {
                break;
            }

            legendTop += legendHeight;
        }

        return taggedBitmap;
    }

    @Override
    protected void onPostExecute(List<Classifier.Recognition> recognitions) {
        super.onPostExecute(recognitions);

        notifyDetectionState(ImageDetectionEvent.State.DONE);

        Logger.debug("recognitions: %s", recognitions);
    }

    private void notifyDetectionState(ImageDetectionEvent.State state) {
        EventBus.getDefault().post(new ImageDetectionEvent(state));
    }

}
