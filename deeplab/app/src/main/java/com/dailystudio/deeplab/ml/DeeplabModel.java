package com.dailystudio.deeplab.ml;

public class DeeplabModel {

    private final static Boolean USE_GPU = true;

    private static DeeplabInterface sInterface = null;

    public synchronized static DeeplabInterface getInstance() {
        if (sInterface != null) {
            return sInterface;
        }

        if (USE_GPU) {
            sInterface = new DeeplabGPU();
        } else {
            sInterface = new DeeplabMobile();
        }

        return sInterface;
    }

}
