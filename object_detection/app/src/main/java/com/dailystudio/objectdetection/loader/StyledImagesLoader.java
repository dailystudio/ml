package com.dailystudio.objectdetection.loader;

import android.content.Context;

import com.dailystudio.app.dataobject.loader.DatabaseObjectsLoader;
import com.dailystudio.dataobject.query.OrderingToken;
import com.dailystudio.dataobject.query.Query;
import com.dailystudio.objectdetection.database.DetectedImage;

public class StyledImagesLoader extends DatabaseObjectsLoader<DetectedImage> {

    public StyledImagesLoader(Context context) {
        super(context);
    }

    @Override
    protected Class<DetectedImage> getObjectClass() {
        return DetectedImage.class;
    }

    @Override
    protected Query getQuery(Class<DetectedImage> klass) {
        Query query = super.getQuery(klass);

        OrderingToken orderByToken =
                DetectedImage.COLUMN_TIME.orderByDescending();
        if (orderByToken != null) {
            query.setOrderBy(orderByToken);
        }

        return query;
    }

}
