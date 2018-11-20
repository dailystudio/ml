package com.dailystudio.objectdetection;

import android.os.Bundle;

import com.dailystudio.app.activity.ActionBarFragmentActivity;

/**
 * Created by nanye on 18/3/6.
 */

public class ViewDetectedImageActivity extends ActionBarFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_detected_image);

        setupViews();
    }

    private void setupViews() {
    }

}
