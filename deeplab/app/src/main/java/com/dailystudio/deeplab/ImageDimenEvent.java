package com.dailystudio.deeplab;

import android.net.Uri;

public class ImageDimenEvent {

    public Uri imageUri;
    public int width;
    public int height;

    public ImageDimenEvent(Uri uri, int w, int h) {
        imageUri = uri;
        width = w;
        height = h;
    }

    @Override
    public String toString() {
        return String.format("image dimen: %s, [%d x %d]",
                imageUri, width, height);
    }

}
