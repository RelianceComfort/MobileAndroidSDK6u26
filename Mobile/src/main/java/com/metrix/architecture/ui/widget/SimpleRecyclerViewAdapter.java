package com.metrix.architecture.ui.widget;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrix.architecture.designer.MetadataDiff;
import com.metrix.architecture.designer.MetadataHeadingsRecyclerViewAdapter;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.system.Lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    protected final int listItemResource;
    private final String[] from;
    private final int[] to;
    private final int[] hiddenViews;
    protected String skinBasedSecondaryColor;
    protected String skinBasedHyperlinkColor;
    protected String headerColumn;
    private final String uniqueIDKey;
    protected List<Object> data;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private RecyclerView recyclerView;
    protected ListItemThemeApplier listItemThemeApplier;

    protected static final int ITEM_TYPE = 1;
    protected static final int HEADER_TYPE = 2;

    public SimpleRecyclerViewAdapter(@NonNull List<? extends Map<String, ?>> data, int resource,
                                     String[] from, int[] to, int[] hiddenViews, String uniqueIDKey) {
        this.data = new ArrayList<>(data);
        this.listItemResource = resource;
        this.from = from;
        this.to = to;
        this.hiddenViews = hiddenViews;
        this.skinBasedSecondaryColor = MetrixSkinManager.getSecondaryColor();
        this.skinBasedHyperlinkColor = MetrixSkinManager.getHyperlinkColor();
        this.uniqueIDKey = uniqueIDKey;
        this.listItemThemeApplier = new SimpleThemeApplier();
    }

    public SimpleRecyclerViewAdapter(@NonNull List<HashMap<String, String>> data, int resource,
                                     String[] from, int[] to, int[] hiddenViews, String uniqueIDKey, String headerColumn) {
        if (MetrixStringHelper.isNullOrEmpty(headerColumn))
            throw new IllegalArgumentException("SimpleRecyclerViewAdapter: headerColumn cannot be null or empty");
        this.headerColumn = headerColumn;
        this.data = MetrixListScreenManager.indexListData(data, headerColumn);
        this.listItemResource = resource;
        this.from = from;
        this.to = to;
        this.hiddenViews = hiddenViews;
        this.skinBasedSecondaryColor = MetrixSkinManager.getSecondaryColor();
        this.skinBasedHyperlinkColor = MetrixSkinManager.getHyperlinkColor();
        this.uniqueIDKey = uniqueIDKey;
        this.listItemThemeApplier = new SimpleThemeApplier();
    }

    public void resolveColorsToUse(@NonNull Activity sourceActivity) {
        // reverse application of skin-based colors if we are in a lookup in the Designer
        if (sourceActivity instanceof Lookup) {
            Lookup thisLookup = (Lookup)sourceActivity;
            if (thisLookup.mIsFromDesigner) {
                this.skinBasedSecondaryColor = "#8427E2";
                this.skinBasedHyperlinkColor = "#4169e1";
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(listItemResource, parent, false);
            return new SimpleViewHolder(view);
        } else {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_seperator, parent, false);
            return new MetadataHeadingsRecyclerViewAdapter.SeparatorViewHolder(view, R.id.list_item_seperator__header);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SimpleViewHolder) {
            bindViews(((SimpleViewHolder) holder).views, (HashMap<String, ?>) data.get(position), position);
        } else {
            ((MetadataHeadingsRecyclerViewAdapter.SeparatorViewHolder) holder).tvTitle.setText((String) data.get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position) instanceof String ? HEADER_TYPE : ITEM_TYPE;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(List<? extends Map<String, ?>> data) {
        final List<Object> newData = MetrixStringHelper.isNullOrEmpty(headerColumn) ? new ArrayList<>(data) :
                MetrixListScreenManager.indexListData((List<HashMap<String, String>>) data, headerColumn);
        if (MetrixStringHelper.isNullOrEmpty(uniqueIDKey)) {
            this.data = newData;
            notifyDataSetChanged();
        } else {
            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new MetadataDiff<>(uniqueIDKey, this.data, newData));
            final Parcelable rvState = recyclerView != null ? recyclerView.getLayoutManager().onSaveInstanceState() : null;
            this.data = newData;
            result.dispatchUpdatesTo(this);
            if (rvState != null && recyclerView != null)
                recyclerView.getLayoutManager().onRestoreInstanceState(rvState);
        }
    }

    private void bindViews(@NonNull SparseArray<View> views, Map dataSet, int position) {
        if (dataSet == null)
            return;

        CheckBox cb = (CheckBox) views.get(R.id.checkboxState);
        if (cb != null) {
            cb.setChecked(false);
            cb.setTag(position);
        }

        final int count = to.length;
        for (int i = 0; i < count; i++) {
            final View v = views.get(to[i]);
            if (v != null) {
                final Object data = dataSet.get(from[i]);
                String text = data == null ? "" : data.toString();
                if (text == null) {
                    text = "";
                }

                if (v instanceof Checkable) {
                    if (data instanceof Boolean) {
                        ((Checkable) v).setChecked((Boolean) data);
                    } else if (v instanceof TextView) {
                        // Note: keep the instanceof TextView check at the bottom of these
                        // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                        ((TextView) v).setText(text);
                    } else {
                        throw new IllegalStateException(v.getClass().getName() +
                                " should be bound to a Boolean, not a " +
                                (data == null ? "<unknown type>" : data.getClass()));
                    }
                } else if (v instanceof TextView) {
                    // Note: keep the instanceof TextView check at the bottom of these
                    // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                    ((TextView) v).setText(text);
                } else if (v instanceof ImageView) {
                    if (data instanceof Integer) {
                        ((ImageView) v).setImageResource((Integer) data);
                    } else {
                        setViewImage((ImageView) v, text);
                    }
                } else {
                    throw new IllegalStateException(v.getClass().getName() + " is not a " +
                            " view that can be bound by this Adapter");
                }
            }
        }
    }

    private void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }

    public List<Object> getData() {
        return data;
    }

    public void setClickListener(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public String getSkinBasedSecondaryColor() {
        return skinBasedSecondaryColor;
    }

    public String getSkinBasedHyperlinkColor() {
        return skinBasedHyperlinkColor;
    }

    public class SimpleViewHolder extends RecyclerView.ViewHolder {
        public final SparseArray<View> views = new SparseArray<>();

        public SimpleViewHolder(View itemView) {
            super(itemView);
            itemView.setTag(MetadataRecyclerViewAdapter.SCRIPT_EXECUTABLE);
            mapViews(itemView);
            hideViewsIfAny();
            setupListeners();
            if (listItemThemeApplier != null && itemView instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) itemView;
                listItemThemeApplier.setColorToControls(vg, vg, true);
            }

            itemView.setOnClickListener((v) -> {
                if (clickListener != null) {
                    final int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION)
                        clickListener.onSimpleRvItemClick(position, data.get(position), this.itemView);
                }
            });

            itemView.setOnLongClickListener((v) -> {
                if (longClickListener != null) {
                    final int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION)
                        longClickListener.onSimpleRvItemLongClick(position, data.get(position), this.itemView);
                }
                return false;
            });
        }

        private void mapViews(@NonNull View view) {
            if (view.getId() != View.NO_ID)
                views.put(view.getId(), view);

            if (view instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    mapViews(vg.getChildAt(i));
                }
            }
        }

        private void hideViewsIfAny() {
            for (int i : hiddenViews) {
                final View view = views.get(i);
                if (view != null)
                    view.setVisibility(View.GONE);
            }
        }

        private void setupListeners() {
            for (int i = 0; i < to.length; i++) {
                final int resId = to[i];
                final String dataKey = from[i];
                final View view = views.get(resId);
                if (view instanceof Checkable) {
                    view.setOnClickListener(v -> {
                        final int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Map itemData = (HashMap) data.get(position);
                            itemData.put(dataKey, ((Checkable) v).isChecked());
                        }
                    });
                }
            }
        }
    }

    public interface OnItemClickListener {
        void onSimpleRvItemClick(int position, Object item, View view);
    }

    public interface OnItemLongClickListener {
        void onSimpleRvItemLongClick(int position, Object item, View view);
    }

    public interface ListItemThemeApplier {
        void setColorToControls(ViewGroup group, ViewGroup initialGroup, boolean doParent);
    }

    private class SimpleThemeApplier implements ListItemThemeApplier {
        @Override
        public void setColorToControls(ViewGroup group, ViewGroup initialGroup, boolean doParent) {
            if (group != null && group.getChildCount() > 0) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    View v = group.getChildAt(i);
                    if (v != null && v instanceof TextView) {
                        TextView tv = (TextView) v;
                        String tag = (tv.getTag() != null) ? tv.getTag().toString() : "";
                        if (!MetrixStringHelper.isNullOrEmpty(skinBasedSecondaryColor)
                                && MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Heading")) {
                            tv.setTextColor(Color.parseColor(skinBasedSecondaryColor));
                        } else if (!MetrixStringHelper.isNullOrEmpty(skinBasedSecondaryColor)
                                && MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Region")) {
                            tv.setBackgroundColor(Color.parseColor(skinBasedSecondaryColor));
                        } else if (!MetrixStringHelper.isNullOrEmpty(skinBasedHyperlinkColor) && tv.getAutoLinkMask() > 0) {
                            tv.setLinkTextColor(Color.parseColor(skinBasedHyperlinkColor));
                        }
                    } else if (v != null && v instanceof ViewGroup && v != initialGroup) {
                        setColorToControls((ViewGroup) v, initialGroup, false);
                    }
                }
            }

            if (doParent && group != null) {
                ViewParent parent = group.getParent();
                if (parent != null && parent instanceof ViewGroup && parent != initialGroup) {
                    setColorToControls((ViewGroup) parent, initialGroup, false);
                }
            }
        }
    }
}
