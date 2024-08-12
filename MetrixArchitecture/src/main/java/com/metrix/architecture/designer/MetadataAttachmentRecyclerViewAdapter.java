package com.metrix.architecture.designer;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Parcelable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelperBase;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public class MetadataAttachmentRecyclerViewAdapter extends RecyclerView.Adapter<MetadataAttachmentRecyclerViewAdapter.AttachmentMetadataViewHolder> {
    private List<HashMap<String, String>> listData;
    private LruCache<String, Bitmap> memoryCache;
    private Activity activity;
    private MetrixAttachmentManager metrixAttachmentManager;
    private int squareDimensionToUse;
    private int listItemResourceID;
    private int tableLayoutResourceID;
    private int tableRowResourceID;
    private int thumbnailResourceID;
    private int defaultColorResourceID;
    private int screenID;
    private LayoutInflater layoutInflater;
    private String uniqueIDKey;
    private MetrixRecyclerViewListener rvListener;
    private MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener clickListener;
    private MetadataRecyclerViewAdapter.MetrixRecyclerViewLongClickListener longClickListener;
    private MetrixRecyclerViewImageClickListener imageClickListener;
    private RecyclerView recyclerView;
    private static HashMap<String, Object> resourceCache;

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
     * @param screenId                    The current screen id
     * @param uniqueIDKey                 The key to uniquely identify a single list item in the data set. This is required to dispatch minimum
     *                                    updates to the RecyclerView adapter. This also enables animations when there is a data set change.
     * @param rvListener                  A listener to delegate click and long click events to the caller
     */
    public MetadataAttachmentRecyclerViewAdapter(Activity activity, LruCache<String, Bitmap> memoryCache, List<HashMap<String, String>> table,
           int listItemResourceID, int tableLayoutResourceID, int tableRowResourceID, int thumbnailResourceID, int defaultColorResourceID,
           int screenId, String uniqueIDKey, MetrixRecyclerViewListener rvListener, MetrixRecyclerViewImageClickListener imgListener) {
        this.listData = table;
        this.memoryCache = memoryCache;
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.listItemResourceID = listItemResourceID;
        this.tableLayoutResourceID = tableLayoutResourceID;
        this.tableRowResourceID = tableRowResourceID;
        this.thumbnailResourceID = thumbnailResourceID;
        this.defaultColorResourceID = defaultColorResourceID;
        this.screenID = screenId;
        this.uniqueIDKey = uniqueIDKey;
        this.rvListener = rvListener;
        this.imageClickListener = imgListener;

        metrixAttachmentManager = MetrixAttachmentManager.getInstance();

        WindowManager wm = (WindowManager) MobileApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point squareToUse = new Point();
        display.getSize(squareToUse);
        squareDimensionToUse = squareToUse.y / 4;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public AttachmentMetadataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vi = layoutInflater.inflate(listItemResourceID, parent, false);
        final AbstractMap.SimpleEntry<View, MetrixListScreenManager.AttachmentMetadataViewHolder> thisPair = MetrixListScreenManager.generateAttachmentListItem(activity,
                layoutInflater, vi, tableLayoutResourceID, tableRowResourceID, thumbnailResourceID, defaultColorResourceID, screenID);
        return new AttachmentMetadataViewHolder(thisPair.getKey(), thisPair.getValue());
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentMetadataViewHolder holder, int position) {
        try {
            try {
                if (resourceCache == null)
                    resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");
            } catch(Exception e) {
                LogManager.getInstance().error(e);
            }
            HashMap<String, String> dataRow = listData.get(position);

            // This will handle the text fields, using Designer metadata
            MetrixListScreenManager.populateAttachmentListItemView(holder.origVH, dataRow, screenID);

            // Every Attachment API List screen in metadata will have an attachment.attachment_name column
            // Use this to determine what thumbnail/placeholder to display
            String fileName = dataRow.get("attachment.attachment_name");
            String attachmentPath = metrixAttachmentManager.getAttachmentPath() + "/" + fileName;
            String key = dataRow.get(uniqueIDKey);
            ImageView imgThumbnail = holder.origVH.mThumbnail.get();

            Bitmap bitmap = MetrixAttachmentHelper.getBitmapFromMemCache(key, memoryCache);
            ///test
            String onDemand = dataRow.get("attachment.on_demand");

            if(!MetrixAttachmentHelper.isAttachmentExists(attachmentPath) && onDemand.equals("Y")) {
                Resources res = activity.getResources();
                bitmap = MetrixAttachmentHelperBase.attachmentDrawableToBitmap(activity, res.getDrawable((int)resourceCache.get("R.drawable.download")));
            }
            if (bitmap != null)
                imgThumbnail.setImageBitmap(bitmap);
            else
                MetrixAttachmentHelper.showGridPreview(activity, attachmentPath, imgThumbnail, squareDimensionToUse, squareDimensionToUse, memoryCache, key);

            imgThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //MetrixDialogAssistant.showAlertDialog("On_Demand Click", "Clicked", "Ok", null, null, null, activity);
                    if (imageClickListener != null)
                        imageClickListener.onListImageClick(position, dataRow, view);
                }
            });
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public void clearAllImages() {
        if (memoryCache != null) {
            memoryCache.evictAll();
            memoryCache = null;
        }
    }

    public void setMemoryCache(LruCache<String, Bitmap> memoryCache) {
        this.memoryCache = memoryCache;
    }

    public void setClickListener(MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setLongClickListener(MetadataRecyclerViewAdapter.MetrixRecyclerViewLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public class AttachmentMetadataViewHolder extends RecyclerView.ViewHolder {
        private final MetrixListScreenManager.AttachmentMetadataViewHolder origVH;

        public AttachmentMetadataViewHolder(View view, MetrixListScreenManager.AttachmentMetadataViewHolder origVH) {
            super(view);
            this.origVH = origVH;

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
        }
    }

    public interface MetrixRecyclerViewImageClickListener {
        void onListImageClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view);
    }
}
