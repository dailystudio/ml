package com.dailystudio.deeplab;

import android.graphics.Bitmap;
import android.net.Uri;

public class SegmentBitmap {

    public Bitmap bitmap;
    public int labelResId;
    public Uri bitmapUri;

    public SegmentBitmap(int label, Bitmap bitmap) {
        this.bitmap = bitmap;
        this.labelResId = label;
        this.bitmapUri = null;
    }

    public SegmentBitmap(int label, Uri uri) {
        this.bitmap = null;
        this.labelResId = label;
        this.bitmapUri = uri;
    }

}
