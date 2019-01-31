package com.dailystudio.deeplab;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.dailystudio.app.loader.AbsAsyncDataLoader;
import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.deeplab.ml.DeeplabInterface;
import com.dailystudio.deeplab.ml.DeeplabModel;
import com.dailystudio.deeplab.ml.ImageUtils;
import com.dailystudio.deeplab.utils.FilePickUtils;
import com.dailystudio.development.Logger;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class SegmentBitmapsLoader extends AbsAsyncDataLoader<List<SegmentBitmap>> {

    private Uri mImageUri;

    public SegmentBitmapsLoader(Context context, Uri imageUri) {
        super(context);

        mImageUri = imageUri;
    }

    @Nullable
    @Override
    public List<SegmentBitmap> loadInBackground() {
        final Context context = getContext();
        if (context == null) {
            return null;
        }

        final Resources res = context.getResources();
        if (res == null) {
            return null;
        }

        if (mImageUri == null) {
            return null;
        }

        DeeplabInterface deeplabInterface = DeeplabModel.getInstance();

        final String filePath = FilePickUtils.getPath(context, mImageUri);
        Logger.debug("file to mask: %s", filePath);
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        boolean vertical = checkAndReportDimen(filePath);

        final int dw = res.getDimensionPixelSize(
                vertical ? R.dimen.image_width_v : R.dimen.image_width_h);
        final int dh = res.getDimensionPixelSize(
                vertical ? R.dimen.image_height_v : R.dimen.image_height_h);
        Logger.debug("display image dimen: [%d x %d]", dw, dh);

        Bitmap bitmap = decodeBitmapFromFile(filePath, dw, dh);
        if (bitmap == null) {
            return null;
        }

        List<SegmentBitmap> bitmaps = new ArrayList<>();

        bitmaps.add(new SegmentBitmap(R.string.label_original, bitmap));

        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Logger.debug("decoded file dimen: [%d x %d]", w, h);

        EventBus.getDefault().post(new ImageDimenEvent(mImageUri, w, h));

        float resizeRatio = (float) deeplabInterface.getInputSize() / Math.max(bitmap.getWidth(), bitmap.getHeight());
        int rw = Math.round(w * resizeRatio);
        int rh = Math.round(h * resizeRatio);

        Logger.debug("resize bitmap: ratio = %f, [%d x %d] -> [%d x %d]",
                resizeRatio, w, h, rw, rh);

        Bitmap resized = ImageUtils.tfResizeBilinear(bitmap, rw, rh);

        Bitmap mask = deeplabInterface.segment(resized);
        if (mask != null) {
            mask = BitmapUtils.createClippedBitmap(mask,
                    (mask.getWidth() - rw) / 2,
                    (mask.getHeight() - rh) / 2,
                    rw, rh);
            mask = BitmapUtils.scaleBitmap(mask, w, h);
            bitmaps.add(new SegmentBitmap(R.string.label_mask, mask));

            final Bitmap cropped = cropBitmapWithMask(bitmap, mask);
            bitmaps.add(new SegmentBitmap(R.string.label_cropped, cropped));
        } else {
            bitmaps.add(new SegmentBitmap(R.string.label_mask, (Bitmap)null));
            bitmaps.add(new SegmentBitmap(R.string.label_cropped, (Bitmap)null));
        }

        return bitmaps;
    }

    private boolean checkAndReportDimen(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        final int width = options.outWidth;
        final int height = options.outHeight;
        Logger.debug("original image dimen: %d x %d", width, height);

        EventBus.getDefault().post(new ImageDimenEvent(mImageUri, width, height));

        return (height > width);
    }


    private Bitmap cropBitmapWithMask(Bitmap original, Bitmap mask) {
        if (original == null
                || mask == null) {
            return null;
        }

        final int w = original.getWidth();
        final int h = original.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(cropped);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(original, 0, 0, null);
        canvas.drawBitmap(mask, 0, 0, paint);
        paint.setXfermode(null);

        return cropped;
    }

    public static Bitmap decodeBitmapFromFile(String filePath,
                                              int reqWidth,
                                              int reqHeight) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
