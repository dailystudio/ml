package com.dailystudio.deeplab.ml;

public class DeeplabModel {

    private final static Boolean USE_TF_LITE = true;

    private static DeeplabInterface sInterface = null;

    public synchronized static DeeplabInterface getInstance() {
        if (sInterface != null) {
            return sInterface;
        }

        if (USE_TF_LITE) {
            sInterface = new DeepLabLite();
        } else {
            sInterface = new DeeplabMobile();
        }

        return sInterface;
    }

}
