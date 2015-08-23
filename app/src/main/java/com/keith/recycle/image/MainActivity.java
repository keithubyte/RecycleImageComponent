package com.keith.recycle.image;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

public class MainActivity extends Activity {

    RecyclerView mRecyclerView;
    ImageAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Images.init();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mAdapter = new ImageAdapter(this, Images.SIZES, Images.URIS);
        mLayoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.HORIZONTAL);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }
}
