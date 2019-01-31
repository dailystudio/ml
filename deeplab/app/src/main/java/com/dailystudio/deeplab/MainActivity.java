package com.dailystudio.deeplab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.dailystudio.app.activity.ActionBarFragmentActivity;
import com.dailystudio.app.utils.ActivityLauncher;
import com.dailystudio.app.utils.ArrayUtils;
import com.dailystudio.deeplab.ml.DeeplabModel;
import com.dailystudio.development.Logger;

public class MainActivity extends ActionBarFragmentActivity {

    private final static int REQUEST_REQUIRED_PERMISSION = 0x01;
    private final static int REQUEST_PICK_IMAGE = 0x02;

    private class InitializeModelAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            final boolean ret = DeeplabModel.getInstance().initialize(
                    getApplicationContext());
            Logger.debug("initialize deeplab model: %s", ret);

            return ret;
        }

    }

    private FloatingActionButton mFabPickImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setupViews();
    }

    private void setupViews() {
        mFabPickImage = findViewById(R.id.fab_pick_image);
        if (mFabPickImage != null) {
            mFabPickImage.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent;

                    if (Build.VERSION.SDK_INT >= 19) {
                        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    } else {
                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                    }

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("image/*");

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    ActivityLauncher.launchActivityForResult(MainActivity.this,
                            Intent.createChooser(intent, getString(R.string.app_name)),
                            REQUEST_PICK_IMAGE);
                }

            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        syncUIWithPermissions(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        syncUIWithPermissions(false);
    }

    private void syncUIWithPermissions(boolean requestIfNeed) {
        final boolean granted = checkRequiredPermissions(requestIfNeed);

        setPickImageEnabled(granted);
        if (granted && !DeeplabModel.getInstance().isInitialized()) {
            initModel();
        }
    }

    private boolean checkRequiredPermissions() {
        return checkRequiredPermissions(false);
    }

    private boolean checkRequiredPermissions(boolean requestIfNeed) {
        final boolean writeStoragePermGranted =
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;

        Logger.debug("storage permission granted: %s", writeStoragePermGranted);

        if (!writeStoragePermGranted
                && requestIfNeed) {
            requestRequiredPermissions();
        }

        return writeStoragePermGranted;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                },
                REQUEST_REQUIRED_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Logger.debug("requestCode = 0x%02x, permission = [%s], grant = [%s]",
                requestCode,
                ArrayUtils.stringArrayToString(permissions, ","),
                ArrayUtils.intArrayToString(grantResults));
        if (requestCode == REQUEST_REQUIRED_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Logger.debug("permission granted, initialize model.");
                initModel();

                if (mFabPickImage != null) {
                    mFabPickImage.setEnabled(true);
                    mFabPickImage.setBackgroundTintList(
                            ColorStateList.valueOf(getColor(R.color.colorAccent)));
                }
            } else {
                Logger.debug("permission denied, disable fab.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.debug("requestCode = %d, resultCode = %d, data = %s",
                requestCode,
                resultCode,
                data);
        if (requestCode == REQUEST_PICK_IMAGE
                && resultCode == RESULT_OK) {
            Uri pickedImageUri = data.getData();
            Logger.debug("picked: %s", pickedImageUri);

            if (pickedImageUri != null) {
                if(Build.VERSION.SDK_INT >= 19){
                    final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    getContentResolver()
                            .takePersistableUriPermission(pickedImageUri, takeFlags);
                }

                segmentImage(pickedImageUri);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void segmentImage(Uri pickedImageUri) {
        Fragment fragment = findFragment(R.id.fragment_segment_bitmaps);
        if (fragment instanceof SegmentBitmapsFragment) {
            ((SegmentBitmapsFragment)fragment).segmentBitmap(pickedImageUri);
        }
    }

    private void initModel() {
        new InitializeModelAsyncTask().execute((Void)null);
    }

    private void setPickImageEnabled(boolean enabled) {
        if (mFabPickImage != null) {
            mFabPickImage.setEnabled(enabled);

            int resId = enabled ? R.color.colorAccent : R.color.light_gray;
            mFabPickImage.setBackgroundTintList(
                    ColorStateList.valueOf(getColor(resId)));
        }
    }

}
