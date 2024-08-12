package com.metrix.architecture.ui.widget;

import java.util.List;
import java.util.Map;

import com.metrix.architecture.utilities.MetrixStringHelper;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LookupItemCustomSimpleAdapter extends CustomSimpleAdapter {

	LayoutInflater mInflater;
	Context context;
	int resource;
	String[] from;
	int[] to;
	List<? extends Map<String, ?>> dataMapList;

	public LookupItemCustomSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);

		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.context = context;
		this.dataMapList = data;
		this.resource = resource;
		this.from = from;
		this.to = to;
	}

	@SuppressWarnings("unused")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			row = mInflater.inflate(this.resource, parent, false);
		}
		bindData(position, row);
		return row;
	}

	private void bindData(int position, View view) {

		Map<String, ?> dataMap = dataMapList.get(position);
		if (dataMap == null)
			return;
		int size = to.length;

		for (int i = 0; i < size; i++) {
			View v = view.findViewById(to[i]);
			if (v == null)
				return;
			Object data = dataMap.get(from[i]);
			String dataText = data == null ? "" : data.toString();
			if (v instanceof TextView) {
				TextView textView = ((TextView) v);
				textView.setText(dataText);
				textView.setVisibility(View.VISIBLE);
				
				String tag = (v.getTag() != null) ? v.getTag().toString() : "";
				if (!MetrixStringHelper.isNullOrEmpty(skinBasedSecondaryColor)
						&& MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Heading")) {
					textView.setTextColor(Color.parseColor(skinBasedSecondaryColor));
				} else if (!MetrixStringHelper.isNullOrEmpty(skinBasedSecondaryColor)
						&& MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Region")) {
					textView.setBackgroundColor(Color.parseColor(skinBasedSecondaryColor));
				}  else if (!MetrixStringHelper.isNullOrEmpty(skinBasedHyperlinkColor) && textView.getAutoLinkMask() > 0) {
					textView.setLinkTextColor(Color.parseColor(skinBasedHyperlinkColor));
				}
			}
			
		}
	}

}
