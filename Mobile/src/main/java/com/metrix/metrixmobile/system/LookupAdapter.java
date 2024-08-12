package com.metrix.metrixmobile.system;

import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.ui.widget.CustomSimpleAdapter;
import com.metrix.metrixmobile.R.color;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class LookupAdapter extends CustomSimpleAdapter {
	private int[] colors = new int[] { color.GhostWhite, color.CornflowerBlue };

	public LookupAdapter(Context context, List<HashMap<String, String>> items,
			int resource, String[] from, int[] to) {
		super(context, items, resource, from, to);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		int colorPos = position % colors.length;
		view.setBackgroundColor(colors[colorPos]);
		return view;
	}
}
