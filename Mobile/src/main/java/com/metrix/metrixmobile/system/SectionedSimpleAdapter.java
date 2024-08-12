package com.metrix.metrixmobile.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.ui.widget.CustomSimpleAdapter;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class SectionedSimpleAdapter extends CustomSimpleAdapter implements SectionIndexer {

	private List<HashMap<String, String>> mData;
	private String mHeaderColumn;
	private HashMap<String, Integer> mAlphaIndexer;
	private String[] mSections;

	protected static final int ITEM = 0;
	protected static final int SEPARATOR = 1;
	private static final int TYPES_COUNT = SEPARATOR + 1;
	protected LayoutInflater mInflater;

	public SectionedSimpleAdapter(Context context, List<HashMap<String, String>> data, int resource, String[] from, int[] to, String headerColumn) {
		super(context, data, resource, from, to);

		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mData = data;
		mHeaderColumn = headerColumn;

		mAlphaIndexer = new HashMap<String, Integer>();
		for (int i = 0; i < mData.size(); i++) {
			String sn = getSectionNameForPosition(i);
			if (!mAlphaIndexer.containsKey(sn)) {
				mAlphaIndexer.put(sn, i);
			}
		}

		ArrayList<String> sectionList = new ArrayList<String>(mAlphaIndexer.keySet());
		Collections.sort(sectionList);
		mSections = new String[sectionList.size()];
		sectionList.toArray(mSections);
	}

	@SuppressLint("DefaultLocale") 
	protected String getSectionNameForPosition(int position) {
		if(mData == null || mData.size()<= position || mData.get(position) == null)
			return "";
		
		try {
			String columnValue = mData.get(position).get(mHeaderColumn);
	
			if (MetrixStringHelper.isNullOrEmpty(columnValue)) {
				return "";
			}
	
			return columnValue.substring(0, 1).toUpperCase();
		}
		catch(Exception ex){
			return "";
		}
	}

	@Override
	public int getPositionForSection(int section) {
	//The check condition added due to get rid of ArrayIndexOutOfBoundsException(which was happened/experienced in 
	//a particular Lookup screen while working in a Galaxy Tab 2(GT-P3110), Android 4.1.2)
	//http://developer.android.com/reference/android/widget/SectionIndexer.html#getPositionForSection%28int%29
		int maxSize = mSections.length - 1;
	    if (section > maxSize)
	        return 0;
	    else
	    	return mAlphaIndexer.get(mSections[section]);
	}

	@Override
	public int getSectionForPosition(int position) {
		String sn = getSectionNameForPosition(position);
		for (int i = 0; i < mSections.length; i++) {
			if (mSections[i].equals(sn)) {
				return i;
			}
		}
		return 0;
	}

	@Override
	public Object[] getSections() {
		return mSections;
	}

	@Override
	public int getItemViewType(int position) {

		if (position == 0) {
			return SEPARATOR;
		}

		int prevPosition = position - 1;

		String snPrevious = getSectionNameForPosition(prevPosition);
		String sn = getSectionNameForPosition(position);

		return snPrevious.equals(sn) ? ITEM : SEPARATOR;
	}

	@Override
	public int getViewTypeCount() {
		return TYPES_COUNT;
	}

	@SuppressLint("InflateParams") @SuppressWarnings("unused")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View unusedView = super.getView(position, convertView, parent);
		
		int type = getItemViewType(position);

		switch (type) {
		case ITEM:
			return super.getView(position, convertView, parent);
		case SEPARATOR:

			TextView tvSectionHeader = null;

			if (convertView == null) {
				LinearLayout layout = new LinearLayout(parent.getContext());
				layout.setOrientation(LinearLayout.VERTICAL);

				View header = mInflater.inflate(R.layout.list_item_seperator, null);
				tvSectionHeader = (TextView) header.findViewById(R.id.list_item_seperator__header);
				layout.addView(header);

				View item = super.getView(position, convertView, parent);
				layout.addView(item);
				convertView = layout;
			} else {
				LinearLayout layout = (LinearLayout) convertView;

				View header = layout.getChildAt(0);
				tvSectionHeader = (TextView) header.findViewById(R.id.list_item_seperator__header);
				
				View item = layout.getChildAt(1);
				layout.removeViewAt(1);
				item = super.getView(position, item, parent);
				layout.addView(item);
				convertView = layout;
			}

			if (tvSectionHeader != null) {
				String sn = getSectionNameForPosition(position);
				tvSectionHeader.setText(sn);
				if (!MetrixStringHelper.isNullOrEmpty(this.skinBasedSecondaryColor)) {
					tvSectionHeader.setTextColor(Color.parseColor(skinBasedSecondaryColor));
				}
			}

			break;
		}

		return convertView;
	}
}
