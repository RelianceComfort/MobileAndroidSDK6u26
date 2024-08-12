package com.metrix.architecture.ui.widget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import java.util.List;
import java.util.Map;

public class DebriefNavigationRecyclerViewAdapter extends SimpleRecyclerViewAdapter {
    private final Activity activity;

    public DebriefNavigationRecyclerViewAdapter(Activity activity, @NonNull List<? extends Map<String, ?>> data, int resource, String[] from, int[] to, String uniqueIDKey) {
        super(data, resource, from, to, new int[]{}, uniqueIDKey);
        this.activity = activity;
        this.listItemThemeApplier = getThemeApplier();
    }

    private ListItemThemeApplier getThemeApplier() {
        return new ListItemThemeApplier() {
            @Override
            public void setColorToControls(ViewGroup group, ViewGroup initialGroup, boolean doParent) {
                if (group != null && group.getChildCount() > 0) {
                    for (int i = 0; i < group.getChildCount(); i++) {
                        View v = group.getChildAt(i);
                        if (v != null && v instanceof TextView) {
                            TextView tv = (TextView) v;
                            String tag = (tv.getTag() != null) ? tv.getTag().toString() : "";
                            int tvID = tv.getId();
                            if (tvID == R.id.sidelist_item_count) {
                                float scale = activity.getResources().getDisplayMetrics().density;
                                float radius = 10f * scale + 0.5f;
                                MetrixSkinManager.setFirstGradientColorsForTextView(tv, radius);
                            } else if (tvID != R.id.sidelist_screen_name) {
                                if (!MetrixStringHelper.isNullOrEmpty(skinBasedSecondaryColor)
                                        && MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Heading")) {
                                    tv.setTextColor(Color.parseColor(skinBasedSecondaryColor));
                                } else if (!MetrixStringHelper.isNullOrEmpty(skinBasedSecondaryColor)
                                        && MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Region")) {
                                    tv.setBackgroundColor(Color.parseColor(skinBasedSecondaryColor));
                                } else if (!MetrixStringHelper.isNullOrEmpty(skinBasedHyperlinkColor) && tv.getAutoLinkMask() > 0) {
                                    tv.setLinkTextColor(Color.parseColor(skinBasedHyperlinkColor));
                                }
                            }
                        } else if (v != null && v instanceof ViewGroup && v != initialGroup) {
                            setColorToControls((ViewGroup)v, initialGroup, false);
                        }
                    }
                }

                if (doParent && group != null) {
                    ViewParent parent = group.getParent();
                    if (parent != null && parent instanceof ViewGroup && parent != initialGroup) {
                        setColorToControls((ViewGroup)parent, initialGroup, false);
                    }
                }
            }
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (holder instanceof SimpleViewHolder) {
            final SimpleViewHolder viewHolder = (SimpleViewHolder) holder;
            final TextView tvItemCount = (TextView) viewHolder.views.get(R.id.sidelist_item_count);
            if (tvItemCount != null) {
                final String tvTextCount = tvItemCount.getText().toString();
                if(MetrixStringHelper.valueIsEqual(tvTextCount, "-1") || MetrixStringHelper.valueIsEqual(tvTextCount, "0"))
                    tvItemCount.setVisibility(View.GONE);
                else
                    tvItemCount.setVisibility(View.VISIBLE);
            }

            final TextView tvScreenName = (TextView) viewHolder.views.get(R.id.sidelist_screen_name);
            if (tvScreenName != null) {
                final String tvText = tvScreenName.getText().toString();
                String currentActivityName = activity.getClass().getSimpleName();
                //Codeless screens
                if(activity instanceof MetrixBaseActivity) {
                    MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
                    if(metrixBaseActivity.isCodelessScreen)
                        currentActivityName = metrixBaseActivity.codeLessScreenName;
                }

                final TextView visibleItem = (TextView) viewHolder.views.get(R.id.sidelist_item_name);
                if (visibleItem != null) {
                    if (tvText.compareToIgnoreCase(currentActivityName) == 0
                            || (currentActivityName.compareToIgnoreCase("MetrixSurveyActivity") == 0) && tvText.contains("Survey")) {
                        visibleItem.setTypeface(null, Typeface.BOLD);
                    } else {
                        visibleItem.setTypeface(null, Typeface.NORMAL);
                    }
                }
            }
        }
    }
}
