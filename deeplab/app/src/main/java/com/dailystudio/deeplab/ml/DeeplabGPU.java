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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

public class DeeplabGPU implements DeeplabInterface{

    private final static String MODEL_PATH = "deeplabv3_257_mv_gpu.tflite";

    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private final static int INPUT_SIZE = 257;
    private final static int NUM_CLASSES = 21;

    private volatile Interpreter sTfInterpreter = null;


    private ByteBuffer mImageData;
    private int[][] mSegmentBits;
    private int[] mSegmentColors;

    private final static Random RANDOM = new Random(System.currentTimeMillis());

    @Override
    public boolean initialize(Context context) {
        if (context == null) {
            return false;
        }

        MappedByteBuffer buffer = loadModelFile(context, MODEL_PATH);
        if (buffer == null) {
            return false;
        }

        Interpreter.Options options = new Interpreter.Options();

//        GpuDelegate delegate = new GpuDelegate();
//        options.addDelegate(delegate);

        sTfInterpreter = new Interpreter(buffer, options);

        debugInputs(sTfInterpreter);
        debugOutputs(sTfInterpreter);

        mImageData =
                ByteBuffer.allocateDirect(
                        1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        mImageData.order(ByteOrder.nativeOrder());

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

        return (sTfInterpreter != null);
    }

    @Override
    public boolean isInitialized() {
        return (sTfInterpreter != null);
    }

    @Override
    public int getInputSize() {
        return INPUT_SIZE;
    }

    @Override
    public Bitmap segment(Bitmap bitmap) {
        if (sTfInterpreter == null) {
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

        int[] mIntValues = new int[w * h];
        float[][][][] mOutputs = new float[1][w][h][21];

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

        final long start = System.currentTimeMillis();

        Logger.debug("start inference = %s", mImageData);
        sTfInterpreter.run(mImageData, mOutputs);

//        Logger.debug("inference done, outputs = %s", ArrayUtils.floatArrayToString(mOutputs));
        final long end = System.currentTimeMillis();
        Logger.debug("%d millis per core segment call.", (end - start));

        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        fillZeroes(mSegmentBits);
        float maxVal = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mSegmentBits[x][y] = 0;

                for (int c = 0; c < NUM_CLASSES; c++) {
                    if (c == 0 || mOutputs[0][y][x][c] > maxVal) {
                        maxVal = mOutputs[0][y][x][c];
                        mSegmentBits[x][y] = c;
                    }
                }

                output.setPixel(x, y, mSegmentColors[mSegmentBits[x][y]]);
            }
//            Logger.debug("segment map[%d]: %s",
//                    y,
//                    ArrayUtils.intArrayToString(segmap[y], ","));
        }


        return output;
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
