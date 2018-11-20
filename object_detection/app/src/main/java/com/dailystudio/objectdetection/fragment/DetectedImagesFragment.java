package com.dailystudio.objectdetection.fragment;

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
import com.dailystudio.objectdetection.R;
import com.dailystudio.objectdetection.database.DetectedImage;
import com.dailystudio.objectdetection.loader.LoaderIds;
import com.dailystudio.objectdetection.loader.StyledImagesLoader;
import com.dailystudio.objectdetection.ui.DetectedImageViewHolder;
import com.dailystudio.objectdetection.ui.DetectedImagesRecyclerAdapter;
import com.dailystudio.objectdetection.ui.ImageSelectedEvent;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by nanye on 18/2/26.
 */

public class DetectedImagesFragment extends AbsArrayRecyclerViewFragment<DetectedImage, DetectedImageViewHolder> {


    private DiscreteScrollView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detected_images, null);

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

            mRecyclerView.addOnItemChangedListener(new DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder>() {

                @Override
                public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition) {
                    Logger.debug("change to pos: %d", adapterPosition);
                    EventBus.getDefault().post(new ImageSelectedEvent(adapterPosition));
                }

            });
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setShowLoadingView(false);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.LOADER_DETECTED_IMAGES;
    }

    @Override
    protected Bundle createLoaderArguments() {
        return new Bundle();
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter() {
        return new DetectedImagesRecyclerAdapter(getContext());
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
    public Loader<List<DetectedImage>> onCreateLoader(int id, Bundle args) {
        return new StyledImagesLoader(getContext());
    }

    public DetectedImage getImageOnPosition(int position) {
        RecyclerView.Adapter adapter = getAdapter();

        if (adapter instanceof AbsArrayRecyclerAdapter == false) {
            return null;
        }

        AbsArrayRecyclerAdapter recyclerAdapter =
                (AbsArrayRecyclerAdapter)adapter;

        if (position < 0 || position >= adapter.getItemCount()) {
            return null;
        }

        return (DetectedImage) recyclerAdapter.getItem(position);
    }

}
