package com.metrix.architecture.ui.widget;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class CustomSimpleAdapterSpannable extends SimpleAdapterSpannable {
	Context context;    
    int resource;
    String[] from;
    int[] to;
    protected String skinBasedSecondaryColor;
    protected String skinBasedHyperlinkColor;
    public List<? extends Map<String, ?>> list;

    public CustomSimpleAdapterSpannable(Context context, List<? extends Map<String, ?>> data,
            int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);

        this.context = context;
        this.list = data;
        this.resource = resource;
        this.from = from;
        this.to = to;
        this.skinBasedSecondaryColor = MetrixSkinManager.getSecondaryColor();
        this.skinBasedHyperlinkColor = MetrixSkinManager.getHyperlinkColor();
    }    
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        if (view != null && view instanceof ViewGroup) {
        	ViewGroup vg = (ViewGroup) view;
        	setSkinBasedColorsOnControls(vg, vg, true);
        }

        return view;
    }
    
	private void setSkinBasedColorsOnControls(ViewGroup group, ViewGroup initialGroup, boolean doParent) {		
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
					setSkinBasedColorsOnControls((ViewGroup)v, initialGroup, false);
				}
			}
		}
		
		if (doParent && group != null) {
			ViewParent parent = group.getParent();	
			if (parent != null && parent instanceof ViewGroup && parent != initialGroup) {
				setSkinBasedColorsOnControls((ViewGroup)parent, initialGroup, false);
			}
		}	
	}
}
