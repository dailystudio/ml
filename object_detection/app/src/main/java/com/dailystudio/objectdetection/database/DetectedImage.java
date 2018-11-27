package com.dailystudio.objectdetection.database;

import android.content.Context;
import android.text.TextUtils;

import com.dailystudio.dataobject.Column;
import com.dailystudio.dataobject.Template;
import com.dailystudio.dataobject.TextColumn;
import com.dailystudio.datetime.dataobject.TimeCapsule;
import com.dailystudio.development.Logger;

/**
 * Created by nanye on 18/1/12.
 */

public class DetectedImage extends TimeCapsule {

    public enum Orientation {
        LANDSCAPE,
        PORTRAIT,
    }

    public static final Column COLUMN_SOURCE = new TextColumn("source", false);
    public static final Column COLUMN_DETECTED_PATH = new TextColumn("detected_path", false);
    public static final Column COLUMN_ORIENTATION = new TextColumn("orientation", false);

    private final static Column[] sCloumns = {
            COLUMN_SOURCE,
            COLUMN_DETECTED_PATH,
            COLUMN_ORIENTATION,
    };

    public DetectedImage(Context context) {
        super(context);

        initMembers();
    }

    public DetectedImage(Context context, int version) {
        super(context, version);

        initMembers();
    }

    private void initMembers() {
        final Template templ = getTemplate();

        templ.addColumns(sCloumns);
    }

    public void setSourcePath(String srcPath) {
        setValue(COLUMN_SOURCE, srcPath);
    }

    public String getSourcePath() {
        return getTextValue(COLUMN_SOURCE);
    }

    public void setStyledPath(String styledPath) {
        setValue(COLUMN_DETECTED_PATH, styledPath);
    }

    public String getDetectedPath() {
        return getTextValue(COLUMN_DETECTED_PATH);
    }

    public void setOrientation(Orientation orientation) {
        setValue(COLUMN_ORIENTATION, String.valueOf(orientation));
    }

    public Orientation getOrientation() {
        final String str = getTextValue(COLUMN_ORIENTATION);
        if (TextUtils.isEmpty(str)) {
            return Orientation.LANDSCAPE;
        }

        Orientation orientation;
        try {
            orientation = Orientation.valueOf(str);
        } catch (Exception e) {
            Logger.error("parse orientation from string[%s] failed: %s",
                    str, e.toString());

            orientation = Orientation.LANDSCAPE;
        }

        return orientation;
    }

    @Override
    public String toString() {
        return String.format("%s(0x%08x): source(%s), detected(%s), orientation(%s)",
                getClass().getSimpleName(),
                hashCode(),
                getSourcePath(),
                getDetectedPath(),
                getOrientation());
    }

}
