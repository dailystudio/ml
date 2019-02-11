package com.dailystudio.deeplab.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;

import com.dailystudio.app.utils.ArrayUtils;
import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.development.Logger;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.experimental.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

public class DeepLabLite implements DeeplabInterface {

    private final static String MODEL_PATH = "deeplabv3_257_mv_gpu.tflite";
    private final static boolean USE_GPU = false;

    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private final static int INPUT_SIZE = 257;
    private final static int NUM_CLASSES = 21;
    private final static int COLOR_CHANNELS = 3;
    private final static int BYTES_PER_POINT = 4;

    MappedByteBuffer mModelBuffer;

    private ByteBuffer mImageData;
    private ByteBuffer mOutputs;
    private int[][] mSegmentBits;
    private int[] mSegmentColors;

    private final static Random RANDOM = new Random(System.currentTimeMillis());

    @Override
    public boolean initialize(Context context) {
        if (context == null) {
            return false;
        }

        mModelBuffer = loadModelFile(context, MODEL_PATH);
        if (mModelBuffer == null) {
            return false;
        }

        mImageData = ByteBuffer.allocateDirect(
                        1 * INPUT_SIZE * INPUT_SIZE * COLOR_CHANNELS * BYTES_PER_POINT);
        mImageData.order(ByteOrder.nativeOrder());

        mOutputs = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * NUM_CLASSES * BYTES_PER_POINT);
        mOutputs.order(ByteOrder.nativeOrder());

        mSegmentBits = new int[INPUT_SIZE][INPUT_SIZE];
        mSegmentColors = new int[NUM_CLASSES];
        for (int i = 0; i < NUM_CLASSES; i++) {
            if (i == 0) {
                mSegmentColors[i] = Color.TRANSPARENT;
            } else {
                mSegmentColors[i] = Color.rgb(
                        (int)(255 * RANDOM.nextFloat()),
                        (int)(255 * RANDOM.nextFloat()),
                        (int)(255 * RANDOM.nextFloat()));
            }
        }

        return (mModelBuffer != null);
    }

    @Override
    public boolean isInitialized() {
        return (mModelBuffer != null);
    }

    @Override
    public int getInputSize() {
        return INPUT_SIZE;
    }

    @Override
    public Bitmap segment(Bitmap bitmap) {
        if (mModelBuffer == null) {
            Logger.warn("tf model is NOT initialized.");
            return null;
        }

        if (bitmap == null) {
            return null;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Logger.debug("bitmap: %d x %d,", w, h);

        if (w > INPUT_SIZE || h > INPUT_SIZE) {
            Logger.warn("invalid bitmap size: %d x %d [should be: %d x %d]",
                    w, h,
                    INPUT_SIZE, INPUT_SIZE);

            return null;
        }

        if (w < INPUT_SIZE || h < INPUT_SIZE) {
            bitmap = BitmapUtils.extendBitmap(
                    bitmap, INPUT_SIZE, INPUT_SIZE, Color.BLACK);

            w = bitmap.getWidth();
            h = bitmap.getHeight();
            Logger.debug("extend bitmap: %d x %d,", w, h);
        }

        mImageData.rewind();
        mOutputs.rewind();

        int[] mIntValues = new int[w * h];

        bitmap.getPixels(mIntValues, 0, w, 0, 0, w, h);

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                if (pixel >= mIntValues.length) {
                    break;
                }

                final int val = mIntValues[pixel++];
                mImageData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                mImageData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                mImageData.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }


        Interpreter.Options options = new Interpreter.Options();

        if (USE_GPU) {
            GpuDelegate delegate = new GpuDelegate();
            options.addDelegate(delegate);
        }

        Interpreter interpreter = new Interpreter(mModelBuffer, options);

        debugInputs(interpreter);
        debugOutputs(interpreter);


        final long start = System.currentTimeMillis();

        Logger.debug("inference starts %d", start);
        interpreter.run(mImageData, mOutputs);
        final long end = System.currentTimeMillis();
        Logger.debug("inference finishes at %d", end);

        Logger.debug("%d millis per core segment call.", (end - start));

        Bitmap maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        fillZeroes(mSegmentBits);
        float maxVal = 0;
        float val = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mSegmentBits[x][y] = 0;

                for (int c = 0; c < NUM_CLASSES; c++) {
                    val = mOutputs.getFloat((y * w * NUM_CLASSES + x * NUM_CLASSES + c) * BYTES_PER_POINT);
                    if (c == 0 || val > maxVal) {
                        maxVal = val;
                        mSegmentBits[x][y] = c;
                    }
                }

                maskBitmap.setPixel(x, y, mSegmentColors[mSegmentBits[x][y]]);
            }
        }


        return maskBitmap;
    }

    private void fillZeroes(int[][] array) {
        if (array == null) {
            return;
        }

        int r;
        for (r = 0; r < array.length; r++) {
            Arrays.fill(array[r], 0);
        }
    }

    private static void debugInputs(Interpreter interpreter) {
        if (interpreter == null) {
            return;
        }

        final int numOfInputs = interpreter.getInputTensorCount();
        Logger.debug("[TF-LITE-MODEL] input tensors: [%d]",numOfInputs);

        for (int i = 0; i < numOfInputs; i++) {
            Tensor t = interpreter.getInputTensor(i);
            Logger.debug("[TF-LITE-MODEL] input tensor[%d[: shape[%s]",
                    i,
                    ArrayUtils.intArrayToString(t.shape()));
        }
    }

    private static void debugOutputs(Interpreter interpreter) {
        if (interpreter == null) {
            return;
        }

        final int numOfOutputs = interpreter.getOutputTensorCount();
        Logger.debug("[TF-LITE-MODEL] output tensors: [%d]",numOfOutputs);

        for (int i = 0; i < numOfOutputs; i++) {
            Tensor t = interpreter.getOutputTensor(i);
            Logger.debug("[TF-LITE-MODEL] output tensor[%d[: shape[%s]",
                    i,
                    ArrayUtils.intArrayToString(t.shape()));
        }
    }

    private static MappedByteBuffer loadModelFile(Context context, String modelFile) {
        if (context == null
                || TextUtils.isEmpty(modelFile)) {
            return null;
        }

        MappedByteBuffer buffer = null;

        try {
            AssetFileDescriptor df = context.getAssets().openFd(modelFile);

            FileInputStream inputStream = new FileInputStream(df.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = df.getStartOffset();
            long declaredLength = df.getDeclaredLength();

            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            Logger.debug("load tflite model from [%s] failed: %s",
                    modelFile,
                    e.toString());

            buffer = null;
        }

        return buffer;
    }

}
