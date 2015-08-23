package com.keith.recycle.image;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * Created by keith on 15/8/23.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageItemHolder> {

    Context context;
    Pair[] pairs;
    String[] imageUris;

    public ImageAdapter(Context context, Pair[] pairs, String[] imageUris) {
        this.context = context;
        this.pairs = pairs;
        this.imageUris = imageUris;
    }


    @Override
    public ImageItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = View.inflate(viewGroup.getContext(), R.layout.item, null);
        //RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(pairs[i].width, pairs[i].height);
        //view.setLayoutParams(layoutParams);
        return new ImageItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageItemHolder imageItemHolder, int i) {
        Picasso.with(context).load(imageUris[i]).into(imageItemHolder.itemImage);
    }

    @Override
    public int getItemCount() {
        return imageUris.length;
    }

    public static class ImageItemHolder extends RecyclerView.ViewHolder {

        public ImageView itemImage;

        public ImageItemHolder(View itemView) {
            super(itemView);
            itemImage = (ImageView) itemView.findViewById(R.id.item_image);
        }
    }
}
