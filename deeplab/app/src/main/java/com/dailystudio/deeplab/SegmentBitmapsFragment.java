package com.dailystudio.deeplab;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dailystudio.app.fragment.AbsArrayRecyclerViewFragment;
import com.dailystudio.app.ui.AbsArrayRecyclerAdapter;
import com.dailystudio.development.Logger;
import com.yarolegovich.discretescrollview.DSVOrientation;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class SegmentBitmapsFragment extends AbsArrayRecyclerViewFragment<SegmentBitmap, SegmentBitmapViewHolder> {

    private final static int LOADER_ID = 0x525;

    private DiscreteScrollView mRecyclerView;

    private Uri mImageUri = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
        setShowLoadingView(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_segment_bitmaps, null);

        setupViews(view);

        return view;
    }

    private void setupViews(View fragmentView) {
        if (fragmentView == null) {
            return;
        }

        mRecyclerView = fragmentView.findViewById(android.R.id.list);
        if (mRecyclerView != null) {
            mRecyclerView.setSlideOnFling(true);
            mRecyclerView.setItemTransformer(new ScaleTransformer.Builder()
                    .setMaxScale(1.0f)
                    .setMinScale(0.8f)
                    .build());
        }
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter() {
        return new SegmentBitmapsRecyclerAdapter(getContext());
    }

    @Override
    protected RecyclerView.LayoutManager onCreateLayoutManager() {
        return null;
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateItemDecoration() {
        return null;
    }

    @Override
    protected int getLoaderId() {
        return LOADER_ID;
    }

    @Override
    protected Bundle createLoaderArguments() {
        return new Bundle();
    }

    @Override
    public Loader<List<SegmentBitmap>> onCreateLoader(int id, Bundle args) {
        return new SegmentBitmapsLoader(getContext(), mImageUri);
    }

    public void segmentBitmap(Uri pickedImageUri) {
        mImageUri = pickedImageUri;

        fastDisplayOrigin();

        restartLoader();
    }

    private void fastDisplayOrigin() {
        if (mRecyclerView != null) {
            mRecyclerView.scrollToPosition(0);
            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            if (adapter instanceof AbsArrayRecyclerAdapter) {
                AbsArrayRecyclerAdapter bitmapsAdapter =
                        (AbsArrayRecyclerAdapter)adapter;

                bitmapsAdapter.clear();
                bitmapsAdapter.add(new SegmentBitmap(R.string.label_original, mImageUri));
                bitmapsAdapter.add(new SegmentBitmap(R.string.label_mask, (Bitmap)null));
                bitmapsAdapter.add(new SegmentBitmap(R.string.label_cropped, (Bitmap)null));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageDimenEvent(ImageDimenEvent event) {
        Logger.debug("new dimen event: %s", event);

        if (event.imageUri == mImageUri) {
            if (mRecyclerView == null) {
                return;
            }

            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            if (adapter instanceof SegmentBitmapsRecyclerAdapter == false) {
                return;
            }

            DSVOrientation viewOrientation = (event.height > event.width ?
                    DSVOrientation.HORIZONTAL : DSVOrientation.VERTICAL);
            DSVOrientation itemOrientation = (event.height > event.width ?
                    DSVOrientation.VERTICAL : DSVOrientation.HORIZONTAL);

            mRecyclerView.setOrientation(viewOrientation);
            ((SegmentBitmapsRecyclerAdapter)adapter).setOrientation(itemOrientation);
        }
    }

}
