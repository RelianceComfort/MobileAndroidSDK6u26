package com.metrix.architecture.designer;

import android.app.Activity;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public class MetadataRecyclerViewAdapter extends RecyclerView.Adapter<MetadataRecyclerViewAdapter.MetadataViewHolder> {
    private List<HashMap<String, String>> listData;
    private String screenType;
    private Activity activity;
    private int listItemResourceID;
    private int tableLayoutResourceID;
    private int tableRowResourceID;
    private int defaultColorResourceID;
    private int checkboxOrImageResourceID;
    private String fieldNameForCheckboxOrImage;
    private int defaultImageResourceID;
    private int sliverID;
    private HashMap<String, Integer> fieldValueMapForImageLookup;
    private int screenID;
    private LayoutInflater layoutInflater;
    private String uniqueIDKey;
    private MetrixRecyclerViewListener rvListener;
    private MetrixRecyclerViewClickListener clickListener;
    private MetrixRecyclerViewLongClickListener longClickListener;
    private RecyclerView recyclerView;

    public static final String SCRIPT_EXECUTABLE = "~*SCRIPT_EXECUTABLE*~";

    public void updateData(List<HashMap<String, String>> listData) {
        if (MetrixStringHelper.isNullOrEmpty(uniqueIDKey)) {
            this.listData = listData;
            notifyDataSetChanged();
        } else {
            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new MetadataDiff<>(uniqueIDKey, this.listData, listData));
            final Parcelable rvState = recyclerView != null ? recyclerView.getLayoutManager().onSaveInstanceState() : null;
            this.listData = listData;
            result.dispatchUpdatesTo(this);
            if (rvState != null && recyclerView != null)
                recyclerView.getLayoutManager().onRestoreInstanceState(rvState);
        }
    }

    public List<HashMap<String, String>> getListData() {
        return listData;
    }

    /**
     * The constructor for the MetadataRecyclerViewAdapter.
     *
     * @param activity                    The current activity.
     * @param table                       The hash map containing the values returned from the query.
     * @param listItemResourceID          The basic top-level container layout for a list item.
     * @param tableLayoutResourceID       The id of the table_layout element of the list item
     *                                    inside of which rows of data will be added.
     * @param tableRowResourceID          The layout for a single empty row of data within a list item.
     * @param defaultColorResourceID      The color to use on colored list item fields if the app
     *                                    has no secondary color applied from skin metadata.
     * @param imageOrCheckboxResourceID   The resource ID for the image view or checkbox
     *                                    contained within the layout defined by listItemResourceID.
     * @param fieldNameForImageOrCheckbox The (table_name.column_name) mapped to the
     *                                    image/checkbox on the list item.
     * @param defaultImageResourceID      The resource ID for the image to use if no data-based image can be found.
     * @param fieldValueMapForImageLookup For a screenType that uses images, if this parameter is defined,
     *                                    the app will use the current value on fieldNameForCheckboxOrImage in combination with this map
     *                                    to determine what bitmap to apply. If this screenType uses images and this parameter is not defined,
     *                                    the app will operate on the assumption that fieldNameForCheckboxOrImage itself specifies an image_id.
     * @param uniqueIDKey                 The key to uniquely identify a single list item in the data set. This is required to dispatch minimum
     *                                    updates to the RecyclerView adapter. This also enables animations when there is a data set change.
     * @param rvListener                  A listener to delegate click and long click events to the caller
     */
    public MetadataRecyclerViewAdapter(Activity activity, List<HashMap<String, String>> table, int listItemResourceID, int tableLayoutResourceID,
                                       int tableRowResourceID, int defaultColorResourceID, int imageOrCheckboxResourceID,
                                       String fieldNameForImageOrCheckbox, int defaultImageResourceID, int sliverID, HashMap<String, Integer> fieldValueMapForImageLookup,
                                       String uniqueIDKey, MetrixRecyclerViewListener rvListener) {

        String activityName = activity.getClass().getSimpleName();
        int screenId = MetrixScreenManager.getScreenId(activityName);

        this.listData = table;
        this.screenType = MetrixScreenManager.getScreenType(screenId);
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.listItemResourceID = listItemResourceID;
        this.tableLayoutResourceID = tableLayoutResourceID;
        this.tableRowResourceID = tableRowResourceID;
        this.defaultColorResourceID = defaultColorResourceID;
        this.checkboxOrImageResourceID = imageOrCheckboxResourceID;
        this.fieldNameForCheckboxOrImage = fieldNameForImageOrCheckbox;
        this.defaultImageResourceID = defaultImageResourceID;
        this.fieldValueMapForImageLookup = fieldValueMapForImageLookup;
        this.screenID = screenId;
        this.uniqueIDKey = uniqueIDKey;
        this.rvListener = rvListener;
        this.sliverID = sliverID;
    }

    /**
     * The constructor for the MetadataRecyclerViewAdapter.
     *
     * @param activity                    The current activity.
     * @param table                       The hash map containing the values returned from the query.
     * @param listItemResourceID          The basic top-level container layout for a list item.
     * @param tableLayoutResourceID       The id of the table_layout element of the list item
     *                                    inside of which rows of data will be added.
     * @param tableRowResourceID          The layout for a single empty row of data within a list item.
     * @param defaultColorResourceID      The color to use on colored list item fields if the app
     *                                    has no secondary color applied from skin metadata.
     * @param imageOrCheckboxResourceID   The resource ID for the image view or checkbox
     *                                    contained within the layout defined by listItemResourceID.
     * @param fieldNameForImageOrCheckbox The (table_name.column_name) mapped to the
     *                                    image/checkbox on the list item.
     * @param defaultImageResourceID      The resource ID for the image to use if no data-based image can be found.
     * @param fieldValueMapForImageLookup For a screenType that uses images, if this parameter is defined,
     *                                    the app will use the current value on fieldNameForCheckboxOrImage in combination with this map
     *                                    to determine what bitmap to apply. If this screenType uses images and this parameter is not defined,
     *                                    the app will operate on the assumption that fieldNameForCheckboxOrImage itself specifies an image_id.
     * @param screenId                    The current screen id
     * @param uniqueIDKey                 The key to uniquely identify a single list item in the data set. This is required to dispatch minimum
     *                                    updates to the RecyclerView adapter. This also enables animations when there is a data set change.
     * @param rvListener                  A listener to delegate click and long click events to the caller
     */
    public MetadataRecyclerViewAdapter(Activity activity, List<HashMap<String, String>> table, int listItemResourceID, int tableLayoutResourceID,
                                       int tableRowResourceID, int defaultColorResourceID, int imageOrCheckboxResourceID,
                                       String fieldNameForImageOrCheckbox, int defaultImageResourceID, int sliverID, HashMap<String, Integer> fieldValueMapForImageLookup,
                                       int screenId, String uniqueIDKey, MetrixRecyclerViewListener rvListener) {
        this.listData = table;
        this.screenType = MetrixScreenManager.getScreenType(screenId);
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.listItemResourceID = listItemResourceID;
        this.tableLayoutResourceID = tableLayoutResourceID;
        this.tableRowResourceID = tableRowResourceID;
        this.defaultColorResourceID = defaultColorResourceID;
        this.checkboxOrImageResourceID = imageOrCheckboxResourceID;
        this.fieldNameForCheckboxOrImage = fieldNameForImageOrCheckbox;
        this.defaultImageResourceID = defaultImageResourceID;
        this.fieldValueMapForImageLookup = fieldValueMapForImageLookup;
        this.screenID = screenId;
        this.uniqueIDKey = uniqueIDKey;
        this.rvListener = rvListener;
        this.sliverID = sliverID;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public MetadataRecyclerViewAdapter.MetadataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vi = layoutInflater.inflate(listItemResourceID, parent, false);
        final AbstractMap.SimpleEntry<View, MetrixListScreenManager.MetadataViewHolder> thisPair = MetrixListScreenManager.generateListItem(screenType, activity, layoutInflater,
                vi, tableLayoutResourceID, tableRowResourceID, defaultColorResourceID, checkboxOrImageResourceID, sliverID, screenID);
        return new MetadataViewHolder(thisPair.getKey(), thisPair.getValue());
    }

    @Override
    public void onBindViewHolder(@NonNull MetadataRecyclerViewAdapter.MetadataViewHolder holder, int position) {
        HashMap<String, String> dataRow = listData.get(position);
        MetrixListScreenManager.populateListItemView(screenType, activity, holder.origVH, dataRow,
                fieldNameForCheckboxOrImage, defaultImageResourceID, fieldValueMapForImageLookup, screenID);

        if (MetrixStringHelper.valueIsEqual(screenType, "LIST_CHECKBOX") && holder.origVH.mBox != null) {
            final CheckBox cb = holder.origVH.mBox.get();
            if (cb != null) {
                String chkState = dataRow.get("checkboxState");
                if (chkState.compareToIgnoreCase("Y") == 0)
                    cb.setChecked(true);
                else
                    cb.setChecked(false);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public void setClickListener(MetrixRecyclerViewClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setLongClickListener(MetrixRecyclerViewLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
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
                    final int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        final HashMap<String, String> dataRow = listData.get(position);
                        if (rvListener != null)
                            rvListener.onListItemClick(position, dataRow, itemView);
                        if (clickListener != null)
                            clickListener.onItemClick(position, dataRow, itemView);
                    }
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        final HashMap<String, String> dataRow = listData.get(position);
                        if (rvListener != null)
                            rvListener.onListItemLongClick(position, dataRow, itemView);
                        if (longClickListener != null)
                            longClickListener.onItemLongClick(position, dataRow, itemView);
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
                                    final HashMap<String, String> dataRow = listData.get(position);

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

    public interface MetrixRecyclerViewClickListener {
        void onItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view);
    }

    public interface MetrixRecyclerViewLongClickListener {
        void onItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view);
    }
}
