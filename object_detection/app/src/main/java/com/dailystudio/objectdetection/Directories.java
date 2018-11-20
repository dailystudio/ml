package com.dailystudio.objectdetection;

import android.content.Context;
import android.text.TextUtils;

import com.dailystudio.GlobalContextWrapper;
import com.dailystudio.app.utils.FileUtils;

import java.io.File;

public class Directories {

    private final static String DIRECTORY_NULL = "";

    private final static String DETECTED = "detected";

    public static String getRootDir() {
        final Context context = GlobalContextWrapper.getContext();
        if (context == null) {
            return null;
        }

        File rootDir = context.getExternalFilesDir(DIRECTORY_NULL);
        if (rootDir == null) {
            return null;
        }

        return rootDir.toString();
    }

    public static String getDetectedDir() {
        String rootDir = getRootDir();
        if (TextUtils.isEmpty(rootDir)) {
            return null;
        }

        File assetsDir = new File(rootDir, DETECTED);

        return assetsDir.toString();
    }

    public static String getDetectedFilePath(String filename) {
        String detectedDir = getDetectedDir();
        if (!FileUtils.isFileExisted(detectedDir)) {
            FileUtils.checkOrCreateNoMediaDirectory(detectedDir);
        }

        File downloadFile = new File(detectedDir, filename);

        return downloadFile.toString();
    }

}
