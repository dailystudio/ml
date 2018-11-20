package com.dailystudio.objectdetection.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

import com.dailystudio.development.Logger;
import com.dailystudio.objectdetection.utils.ImageUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ObjectDetectionModel {

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    private static Classifier sDetector = null;

    private static Bitmap croppedBitmap = null;

    private static Matrix frameToCropTransform;
    private static Matrix cropToFrameTransform;


    public synchronized static boolean isInitialized() {
        return (sDetector != null);
    }

    public static boolean initialize(Context context) {
        if (context == null) {
            return false;
        }

        boolean success = false;
        try {
            sDetector = TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);

            success = true;
        } catch (final IOException e) {
            Logger.error("Initializing classifier failed: %s", e.toString());

            sDetector = null;
            success = false;
        }

        croppedBitmap = Bitmap.createBitmap(
                TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                Bitmap.Config.ARGB_8888);

        return success;
    }

    public static List<Classifier.Recognition> detectImage(Bitmap bitmap) {
        return detectImage(bitmap, 0);
    }

    public static List<Classifier.Recognition> detectImage(Bitmap bitmap, float minimumConfidence) {
        if (bitmap == null) {
            return null;
        }

        if (minimumConfidence <= 0
                || minimumConfidence > 1) {
            minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        }

        if (!ObjectDetectionModel.isInitialized()) {
            Logger.warn("object detection model is NOT initialized yet.");

            return null;
        }

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        width, height,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        0, true);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(bitmap, frameToCropTransform, null);

        List<Classifier.Recognition> results = sDetector.recognizeImage(croppedBitmap);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<>();

        for (final Classifier.Recognition recognition: results) {
            final RectF loc = recognition.getLocation();
            if (loc != null && recognition.getConfidence() >= minimumConfidence) {
                cropToFrameTransform.mapRect(loc);
                recognition.setLocation(loc);
                mappedRecognitions.add(recognition);
                Logger.debug("add satisfied recognition: %s", recognition);
            } else {
                Logger.warn("skip unsatisfied recognition: %s", recognition);
            }
        }

        return mappedRecognitions;
    }

}
