package com.metrix.architecture.designer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public class MetadataHeadingsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Object> listData;
    private String screenType;
    private Activity activity;
    private int listItemResourceID;
    private int tableLayoutResourceID;
    private int tableRowResourceID;
    private int defaultColorResourceID;
    private int screenID;
    private int headingResourceLayoutID;
    private int headingSubviewResourceID;
    private int sliverID;
    private LayoutInflater layoutInflater;
    private String uniqueIDKey;
    private String headerFieldName;
    private MetrixRecyclerViewListener rvListener;
    private RecyclerView recyclerView;

    private static final int ITEM = 1;
    private static final int SEPARATOR = 2;

    public void updateData(List<HashMap<String, String>> listData) {
        if (MetrixStringHelper.isNullOrEmpty(uniqueIDKey)) {
            this.listData = MetrixListScreenManager.indexListData(listData, headerFieldName);
            notifyDataSetChanged();
        } else {
            final List<Object> newData = MetrixListScreenManager.indexListData(listData, headerFieldName);
            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new MetadataDiff<>(uniqueIDKey, this.listData, newData));
            final Parcelable rvState = recyclerView != null ? recyclerView.getLayoutManager().onSaveInstanceState() : null;
            this.listData = newData;
            result.dispatchUpdatesTo(this);
            if (rvState != null && recyclerView != null)
                recyclerView.getLayoutManager().onRestoreInstanceState(rvState);
        }
    }

    public List<Object> getListData() {
        return listData;
    }

    public MetadataHeadingsRecyclerViewAdapter(Activity activity, List<HashMap<String, String>> table, int listItemResourceID,
                                               int tableLayoutResourceID, int tableRowResourceID, int defaultColorResourceID,
                                               String headerFieldName, int headingResourceLayoutID, int headingSubviewResourceID,
                                               int sliverID, String uniqueIDKey, MetrixRecyclerViewListener listener) {
        String activityName = activity.getClass().getSimpleName();
        int screenId = MetrixScreenManager.getScreenId(activityName);

        this.screenType = MetrixScreenManager.getScreenType(screenId);
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.listItemResourceID = listItemResourceID;
        this.tableLayoutResourceID = tableLayoutResourceID;
        this.tableRowResourceID = tableRowResourceID;
        this.defaultColorResourceID = defaultColorResourceID;
        this.screenID = screenId;
        this.uniqueIDKey = uniqueIDKey;
        this.rvListener = listener;
        this.headerFieldName = headerFieldName;
        this.headingResourceLayoutID = headingResourceLayoutID;
        this.headingSubviewResourceID = headingSubviewResourceID;
        this.listData = MetrixListScreenManager.indexListData(table, headerFieldName);
        this.sliverID = sliverID;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM) {
            View vi = layoutInflater.inflate(listItemResourceID, parent, false);
            final AbstractMap.SimpleEntry<View, MetrixListScreenManager.MetadataViewHolder> thisPair = MetrixListScreenManager.generateListItem(screenType, activity, layoutInflater,
                    vi, tableLayoutResourceID, tableRowResourceID, defaultColorResourceID, 0, sliverID, screenID);
            return new MetadataViewHolder(thisPair.getKey(), thisPair.getValue());
        } else {
            return new SeparatorViewHolder(layoutInflater.inflate(headingResourceLayoutID, parent, false), headingSubviewResourceID);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SeparatorViewHolder) {
            ((SeparatorViewHolder) holder).tvTitle.setText((String) listData.get(position));
        } else {
            final HashMap<String, String> dataRow = (HashMap<String, String>) listData.get(position);
            final MetadataViewHolder vh = (MetadataViewHolder) holder;
            MetrixListScreenManager.populateListItemView(screenType, activity, vh.origVH, dataRow,
                    null, 0, null, screenID);

            if (MetrixStringHelper.valueIsEqual(screenType, "LIST_CHECKBOX") && vh.origVH.mBox != null) {
                final CheckBox cb = vh.origVH.mBox.get();
                if (cb != null) {
                    String chkState = dataRow.get("checkboxState");
                    if (chkState.compareToIgnoreCase("Y") == 0)
                        cb.setChecked(true);
                    else
                        cb.setChecked(false);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return listData.get(position) instanceof String ? SEPARATOR : ITEM;
    }

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvTitle;

        public SeparatorViewHolder(View itemView, int titleResId) {
            super(itemView);
            tvTitle = itemView.findViewById(titleResId);
            int colorToUse = 0;
            String secondaryColor = MetrixSkinManager.getSecondaryColor();
            if (!MetrixStringHelper.isNullOrEmpty(secondaryColor)) {
                colorToUse = Color.parseColor(secondaryColor);
                tvTitle.setTextColor(colorToUse);
            }
        }
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder {
        private final MetrixListScreenManager.MetadataViewHolder origVH;

        public MetadataViewHolder(View view, MetrixListScreenManager.MetadataViewHolder origVH) {
            super(view);
            this.origVH = origVH;
            view.setTag(MetadataRecyclerViewAdapter.SCRIPT_EXECUTABLE);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (rvListener != null) {
                        final int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            if (listData.get(position) instanceof String) return;
                            final HashMap<String, String> dataRow = (HashMap<String, String>) listData.get(position);
                            rvListener.onListItemClick(position, dataRow, itemView);
                        }
                    }
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (rvListener != null) {
                        final int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            if (listData.get(position) instanceof String) return true;
                            final HashMap<String, String> dataRow = (HashMap<String, String>) listData.get(position);
                            rvListener.onListItemLongClick(position, dataRow, itemView);
                        }
                    }
                    return true;
                }
            });

            if (MetrixStringHelper.valueIsEqual(screenType, "LIST_CHECKBOX")) {
                if (this.origVH.mBox != null) {
                    final CheckBox cb = this.origVH.mBox.get();
                    if (cb != null) {
                        cb.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final int position = getAdapterPosition();
                                if (position != RecyclerView.NO_POSITION) {
                                    if (listData.get(position) instanceof String) return;
                                    final HashMap<String, String> dataRow = (HashMap<String, String>) listData.get(position);

                                    String checkState = "";
                                    if (((CheckBox) v).isChecked()) {
                                        checkState = "Y";
                                    } else {
                                        checkState = "N";
                                    }

                                    dataRow.put("checkboxState", checkState);
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}
