package com.dailystudio.objectdetection.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.dailystudio.dataobject.DatabaseObjectKeys;
import com.dailystudio.dataobject.query.ExpressionToken;
import com.dailystudio.datetime.dataobject.AbsTimeCapsuleModel;
import com.dailystudio.development.Logger;

import java.util.List;

/**
 * Created by nanye on 18/1/12.
 */

public class DetectedImageDatabaseModel extends AbsTimeCapsuleModel<DetectedImage> {

    private final static String GET_DETECTED_IMAGE = "get-styled-image";

    public DetectedImageDatabaseModel(@NonNull Class<DetectedImage> objectClass) {
        super(objectClass);
    }

    @Override
    protected ExpressionToken objectExistenceToken(DatabaseObjectKeys keys) {
        if (keys == null
                || keys.hasValue(DetectedImage.COLUMN_SOURCE) == false) {
            return null;
        }

        String srcPath = (String) keys.getValue(DetectedImage.COLUMN_SOURCE);

        ExpressionToken selToken =
                DetectedImage.COLUMN_SOURCE.eq(srcPath);

        return selToken;
    }

    @Override
    protected void applyArgsOnObject(DetectedImage object, DatabaseObjectKeys keys) {
        if (object == null
                || keys == null
                || keys.hasValue(DetectedImage.COLUMN_SOURCE) == false
                || keys.hasValue(DetectedImage.COLUMN_DETECTED_PATH) == false
                || keys.hasValue(DetectedImage.COLUMN_ORIENTATION, true) == false) {
            return;
        }

        object.setTime(System.currentTimeMillis());
        object.setSourcePath((String)keys.getValue(DetectedImage.COLUMN_SOURCE));
        object.setStyledPath((String)keys.getValue(DetectedImage.COLUMN_DETECTED_PATH));
        object.setOrientation((DetectedImage.Orientation) keys.getValue(DetectedImage.COLUMN_ORIENTATION));
    }

    @Override
    protected ExpressionToken objectsToken(DatabaseObjectKeys keys, @NonNull String tokenType) {
        if (GET_DETECTED_IMAGE.equals(tokenType)) {
            if (keys != null
                    && keys.hasValue(DetectedImage.COLUMN_SOURCE)) {
                final String srcPath = (String) keys.getValue(DetectedImage.COLUMN_SOURCE);

                return DetectedImage.COLUMN_SOURCE.eq(srcPath);
            } else {
                return null;
            }
        }

        return super.objectsToken(keys, tokenType);
    }

    @Override
    protected ExpressionToken objectsDeletionToken(DatabaseObjectKeys keys) {
        if (keys == null
                || keys.hasValue(DetectedImage.COLUMN_SOURCE) == false) {
            return null;
        }

        final String srcPath = (String) keys.getValue(DetectedImage.COLUMN_SOURCE);

        ExpressionToken selToken =
                DetectedImage.COLUMN_SOURCE.eq(srcPath);

        return selToken;
    }

    private final static DetectedImageDatabaseModel INSTANCE =
            new DetectedImageDatabaseModel(DetectedImage.class);

    public final static DetectedImage saveDetectedImage(Context context,
                                                        String srcPath,
                                                        String detectedPath,
                                                        DetectedImage.Orientation orientation) {
        Logger.debug("save detected image: srcPath = %s, detectedPath = %s, orientation = %s",
                srcPath, detectedPath, orientation);
        if (context == null
                || TextUtils.isEmpty(srcPath)
                || TextUtils.isEmpty(detectedPath)
                || orientation == null) {
            return null;
        }

        DatabaseObjectKeys keys = new DatabaseObjectKeys();

        keys.putValue(DetectedImage.COLUMN_SOURCE, srcPath);
        keys.putValue(DetectedImage.COLUMN_DETECTED_PATH, detectedPath);
        keys.putValue(DetectedImage.COLUMN_ORIENTATION, orientation);

        return INSTANCE.addOrUpdateObject(context, keys);
    }

    public final static DetectedImage getDetectedImage(Context context,
                                                       String srcPath) {
        DatabaseObjectKeys keys = new DatabaseObjectKeys();

        keys.putValue(DetectedImage.COLUMN_SOURCE, srcPath);

        List<DetectedImage> images =
                INSTANCE.listObjects(context, keys, GET_DETECTED_IMAGE);
        if (images == null || images.size() <= 0) {
            return null;
        }

        return images.get(0);
    }

}
