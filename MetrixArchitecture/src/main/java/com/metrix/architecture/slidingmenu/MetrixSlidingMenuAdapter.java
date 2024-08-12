package com.metrix.architecture.slidingmenu;

import java.util.ArrayList;

import com.metrix.architecture.utilities.MetrixStringHelper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MetrixSlidingMenuAdapter extends RecyclerView.Adapter<MetrixSlidingMenuAdapter.SlidingMenuVH> {
    private ArrayList<MetrixSlidingMenuItem> mSlidingMenuItems; 
    private int mSlidingMenuLayoutId;
    private int mSlidingItemNameViewId;
    private int mSlidingItemCountViewId;
    private int mSlidingItemIconId;
    private OnItemClickListener listener;

    public MetrixSlidingMenuAdapter(int slidingMenuLayoutId, int slidingItemNameViewId, int slidingItemCountViewId, int slidingItemIconId, ArrayList<MetrixSlidingMenuItem> items, OnItemClickListener listener) {
        this.mSlidingMenuLayoutId = slidingMenuLayoutId;
        this.mSlidingItemNameViewId = slidingItemNameViewId;
        this.mSlidingItemCountViewId = slidingItemCountViewId;
        this.mSlidingItemIconId = slidingItemIconId;
        this.mSlidingMenuItems = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SlidingMenuVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(mSlidingMenuLayoutId, parent, false);
        return new SlidingMenuVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlidingMenuVH holder, int position) {
        final MetrixSlidingMenuItem item = mSlidingMenuItems.get(position);
        if (holder.textViewItemName != null)
            holder.textViewItemName.setText(item.getTitle());

        if (holder.imageViewItemIcon != null)
            holder.imageViewItemIcon.setImageResource(item.getIconResourceId());

        final String count = item.getCount();
        if (holder.textViewItemCount != null) {
            if (!MetrixStringHelper.isNullOrEmpty(count)) {
                if (MetrixStringHelper.valueIsEqual(count, "0")) {
                    holder.textViewItemCount.setVisibility(View.GONE);
                } else {
                    holder.textViewItemCount.setVisibility(View.VISIBLE);
                    holder.textViewItemCount.setText(count);
                }
            } else
                holder.textViewItemCount.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int index) {
        return index; 
    }

    @Override
    public int getItemCount() {
        return mSlidingMenuItems.size();
    }

    class SlidingMenuVH extends RecyclerView.ViewHolder {
        private final TextView textViewItemName;
        private final TextView textViewItemCount;
        private final ImageView imageViewItemIcon;

        SlidingMenuVH(View itemView) {
            super(itemView);
            textViewItemName = itemView.findViewById(mSlidingItemNameViewId);
            textViewItemCount = itemView.findViewById(mSlidingItemCountViewId);
            imageViewItemIcon = itemView.findViewById(mSlidingItemIconId);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        final int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION)
                            listener.onSlidingMenuItemClick(mSlidingMenuItems.get(position));
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onSlidingMenuItemClick(MetrixSlidingMenuItem clickedItem);
    }
}