package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.utilities.MetrixStringHelper;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class MetadataHeadingsAdapter extends MetadataAdapter implements SectionIndexer {
	private String mHeaderFieldName;
	private HashMap<String, Integer> mAlphaIndexer;
	private String[] mSections;
	private int mHeadingResourceLayoutID;
	private int mHeadingSubviewResourceID;
	private String mSkinBasedSecondaryColor;

	protected static final int ITEM = 0;
	protected static final int SEPARATOR = 1;
	private static final int TYPES_COUNT = SEPARATOR + 1;
	protected LayoutInflater mInflater;
	
	/**
	 * The constructor for the MetadataHeadingsAdapter.
	 * 
	 * @param activity The current activity.
	 * @param table The hash map containing the values returned from the query.
	 * @param listItemResourceID The basic top-level container layout for a list item.
	 * @param tableLayoutResourceID The id of the table_layout element of the list item 
	 * inside of which rows of data will be added.
	 * @param tableRowResourceID The layout for a single empty row of data within a list item.
	 * @param defaultColorResourceID The color to use on colored list item fields if the app 
	 * has no secondary color applied from skin metadata.
	 * @param headerFieldName The (table_name.column_name) used to sort the data.
	 * @param headingResourceLayoutID The layout for a single heading item.
	 * @param headingSubviewResourceID The resource ID for the subview inside the heading layout.
	 * 
	 */
	public MetadataHeadingsAdapter(Activity activity, List<HashMap<String, String>> table, int listItemResourceID,
			int tableLayoutResourceID, int tableRowResourceID, int defaultColorResourceID,
			String headerFieldName, int headingResourceLayoutID, int headingSubviewResourceID) {
		super(activity, null, listItemResourceID, tableLayoutResourceID, tableRowResourceID, defaultColorResourceID, 0,
				null, 0, null);
		
		mListData = MetrixListScreenManager.orderTableByHeadingField(table, headerFieldName);
		
		mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mHeaderFieldName = headerFieldName;
		mHeadingResourceLayoutID = headingResourceLayoutID;
		mHeadingSubviewResourceID = headingSubviewResourceID;
		mSkinBasedSecondaryColor = MetrixSkinManager.getSecondaryColor();

		mAlphaIndexer = new HashMap<String, Integer>();
		for (int i = 0; i < mListData.size(); i++) {
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
	
	protected String getSectionNameForPosition(int position) {
		String columnValue = mListData.get(position).get(mHeaderFieldName);

		if (MetrixStringHelper.isNullOrEmpty(columnValue)) {
			return "";
		}

		return columnValue.substring(0, 1).toUpperCase();
	}

	@Override
	public int getPositionForSection(int sectionIndex) {
		return mAlphaIndexer.get(mSections[sectionIndex]);
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
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {	
		int type = getItemViewType(position);

		switch (type) {
			case ITEM:
				return super.getView(position, convertView, parent);
			case SEPARATOR:
				TextView tvSectionHeader = null;
	
				if (convertView == null) {
					LinearLayout layout = new LinearLayout(parent.getContext());
					layout.setOrientation(LinearLayout.VERTICAL);
	
					View header = mInflater.inflate(mHeadingResourceLayoutID, null);
					tvSectionHeader = (TextView) header.findViewById(mHeadingSubviewResourceID);
					layout.addView(header);
	
					View item = super.getView(position, convertView, parent);
					layout.addView(item);
					convertView = layout;
				} else {
					LinearLayout layout = (LinearLayout) convertView;
	
					View header = layout.getChildAt(0);
					tvSectionHeader = (TextView) header.findViewById(mHeadingSubviewResourceID);
					
					View item = layout.getChildAt(1);
					layout.removeViewAt(1);
					item = super.getView(position, item, parent);
					layout.addView(item);
					convertView = layout;
				}
	
				if (tvSectionHeader != null) {
					String sn = getSectionNameForPosition(position);
					tvSectionHeader.setText(sn);
					if (!MetrixStringHelper.isNullOrEmpty(mSkinBasedSecondaryColor)) {
						tvSectionHeader.setTextColor(Color.parseColor(mSkinBasedSecondaryColor));
					}
				}
	
				break;
		}

		return convertView;
	}
}

