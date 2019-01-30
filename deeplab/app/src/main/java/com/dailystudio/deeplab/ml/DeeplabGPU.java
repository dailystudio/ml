package com.dailystudio.deeplab.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;

import com.dailystudio.app.utils.ArrayUtils;
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

public class DeeplabGPU implements DeeplabInterface{

    private final static String MODEL_PATH = "deeplabv3_257_mv_gpu.tflite";
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    private volatile Interpreter sTfInterpreter = null;

    private final static String INPUT_NAME = "ImageTensor";
    private final static String OUTPUT_NAME = "SemanticPredictions";

    public final static int INPUT_SIZE = 257;

    @Override
    public boolean initialize(Context context) {
        if (context == null) {
            return false;
        }

        MappedByteBuffer buffer = loadModelFile(context, MODEL_PATH);
        if (buffer == null) {
            return false;
        }

        GpuDelegate delegate = new GpuDelegate();
        Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);

        sTfInterpreter = new Interpreter(buffer, options);

        debugInputs(sTfInterpreter);
        debugOutputs(sTfInterpreter);

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
    public Bitmap segment(final Bitmap bitmap) {
        if (sTfInterpreter == null) {
            Logger.warn("tf model is NOT initialized.");
            return null;
        }

        if (bitmap == null) {
            return null;
        }

        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Logger.debug("bitmap: %d x %d,", w, h);

        if (w > INPUT_SIZE || h > INPUT_SIZE) {
            Logger.warn("invalid bitmap size: %d x %d [should be: %d x %d]",
                    w, h,
                    INPUT_SIZE, INPUT_SIZE);

            return null;
        }

        ByteBuffer img =
                ByteBuffer.allocateDirect(
                        1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        img.order(ByteOrder.nativeOrder());
        img.rewind();

        int[] mIntValues = new int[w * h];
        byte[] mFlatIntValues = new byte[w * h * 3];
        int[] mOutputs = new int[w * h];

        bitmap.getPixels(mIntValues, 0, w, 0, 0, w, h);

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = mIntValues[pixel++];
                img.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                img.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                img.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
//        for (int i = 0; i < mIntValues.length; ++i) {
//            final int val = mIntValues[i];
//            mFlatIntValues[i * 3 + 0] = (byte)((val >> 16) & 0xFF);
//            mFlatIntValues[i * 3 + 1] = (byte)((val >> 8) & 0xFF);
//            mFlatIntValues[i * 3 + 2] = (byte)(val & 0xFF);
//        }

        final long start = System.currentTimeMillis();

        Logger.debug("start inference = %s", img);
        sTfInterpreter.run(img, mOutputs);

        Logger.debug("inference done, outputs = %s", ArrayUtils.intArrayToString(mOutputs));
        final long end = System.currentTimeMillis();
        Logger.debug("%d millis per core segment call.", (end - start));

        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                output.setPixel(x, y, mOutputs[y * w + x] == 0 ? Color.TRANSPARENT : Color.BLACK);
            }
        }

        return output;
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
