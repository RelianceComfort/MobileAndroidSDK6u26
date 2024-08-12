package com.metrix.architecture.designer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;

import com.metrix.architecture.designer.MetrixListScreenManager.MetadataViewHolder;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;

/**
 * Utilizes field list item metadata to populate list items.
 * All data must be fully formatted for display within the mListData object.
 * 
 * @since 5.6.2
 */
public class MetadataAdapter extends BaseAdapter implements ListAdapter  {
	static MetadataViewHolder holder;
	private LayoutInflater mInflater;
	public List<HashMap<String, String>> mListData;
	private String mScreenType;
	private Activity mActivity;
	private int mListItemResourceID;
	private int mTableLayoutResourceID;
	private int mTableRowResourceID;
	private int mDefaultColorResourceID;
	private int mCheckboxOrImageResourceID;
	private String mFieldNameForCheckboxOrImage;
	private int mDefaultImageResourceID;
	private HashMap<String, Integer> mFieldValueMapForImageLookup;
	private int mScreenID;

	/**
	 * The constructor for the MetadataAdapter.
	 * 
	 * @param activity The current activity.
	 * @param table The hash map containing the values returned from the query.
	 * @param listItemResourceID The basic top-level container layout for a list item.
	 * @param tableLayoutResourceID The id of the table_layout element of the list item 
	 * inside of which rows of data will be added.
	 * @param tableRowResourceID The layout for a single empty row of data within a list item.
	 * @param defaultColorResourceID The color to use on colored list item fields if the app 
	 * has no secondary color applied from skin metadata.
	 * @param imageOrCheckboxResourceID The resource ID for the image view or checkbox
	 * contained within the layout defined by listItemResourceID.
	 * @param fieldNameForImageOrCheckbox The (table_name.column_name) mapped to the 
	 * image/checkbox on the list item.
	 * @param defaultImageResourceID The resource ID for the image to use if no data-based image can be found.
	 * @param fieldValueMapForImageLookup For a screenType that uses images, if this parameter is defined, 
	 * the app will use the current value on fieldNameForCheckboxOrImage in combination with this map 
	 * to determine what bitmap to apply. If this screenType uses images and this parameter is not defined,
	 * the app will operate on the assumption that fieldNameForCheckboxOrImage itself specifies an image_id.
	 * 
	 */
	public MetadataAdapter(Activity activity, List<HashMap<String, String>> table, int listItemResourceID, int tableLayoutResourceID, 
		int tableRowResourceID, int defaultColorResourceID, int imageOrCheckboxResourceID,
		String fieldNameForImageOrCheckbox, int defaultImageResourceID, HashMap<String, Integer> fieldValueMapForImageLookup) {

		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);

		mListData = table;
		mScreenType = MetrixScreenManager.getScreenType(screenId);
		mActivity = activity;
		mInflater = LayoutInflater.from(activity);
		mListItemResourceID = listItemResourceID;
		mTableLayoutResourceID = tableLayoutResourceID;
		mTableRowResourceID = tableRowResourceID;
		mDefaultColorResourceID = defaultColorResourceID;
		mCheckboxOrImageResourceID = imageOrCheckboxResourceID;
		mFieldNameForCheckboxOrImage = fieldNameForImageOrCheckbox;
		mDefaultImageResourceID = defaultImageResourceID;
		mFieldValueMapForImageLookup = fieldValueMapForImageLookup;
		mScreenID = screenId;
	}
	
	/**
	 * The constructor for the MetadataAdapter.
	 * 
	 * @param activity The current activity.
	 * @param table The hash map containing the values returned from the query.
	 * @param listItemResourceID The basic top-level container layout for a list item.
	 * @param tableLayoutResourceID The id of the table_layout element of the list item 
	 * inside of which rows of data will be added.
	 * @param tableRowResourceID The layout for a single empty row of data within a list item.
	 * @param defaultColorResourceID The color to use on colored list item fields if the app 
	 * has no secondary color applied from skin metadata.
	 * @param imageOrCheckboxResourceID The resource ID for the image view or checkbox
	 * contained within the layout defined by listItemResourceID.
	 * @param fieldNameForImageOrCheckbox The (table_name.column_name) mapped to the 
	 * image/checkbox on the list item.
	 * @param defaultImageResourceID The resource ID for the image to use if no data-based image can be found.
	 * @param fieldValueMapForImageLookup For a screenType that uses images, if this parameter is defined, 
	 * the app will use the current value on fieldNameForCheckboxOrImage in combination with this map 
	 * to determine what bitmap to apply. If this screenType uses images and this parameter is not defined,
	 * the app will operate on the assumption that fieldNameForCheckboxOrImage itself specifies an image_id.
	 * @param current screen id
	 * 
	 */
	public MetadataAdapter(Activity activity, List<HashMap<String, String>> table, int listItemResourceID, int tableLayoutResourceID, 
		int tableRowResourceID, int defaultColorResourceID, int imageOrCheckboxResourceID,
		String fieldNameForImageOrCheckbox, int defaultImageResourceID, HashMap<String, Integer> fieldValueMapForImageLookup, int screenId) {
		mListData = table;
		mScreenType = MetrixScreenManager.getScreenType(screenId);
		mActivity = activity;
		mInflater = LayoutInflater.from(activity);
		mListItemResourceID = listItemResourceID;
		mTableLayoutResourceID = tableLayoutResourceID;
		mTableRowResourceID = tableRowResourceID;
		mDefaultColorResourceID = defaultColorResourceID;
		mCheckboxOrImageResourceID = imageOrCheckboxResourceID;
		mFieldNameForCheckboxOrImage = fieldNameForImageOrCheckbox;
		mDefaultImageResourceID = defaultImageResourceID;
		mFieldValueMapForImageLookup = fieldValueMapForImageLookup;
		mScreenID = screenId;
	}
	
	/**
	 * The number of items in the list is determined by the number of items
	 * in our data list.
	 * 
	 * @see android.widget.ListAdapter#getCount()
	 */
	public int getCount() {
		return mListData.size();
	}

	/**
	 * Return whatever object represents one row in the list, using the position as an index.
	 * 
	 * @see android.widget.ListAdapter#getItem(int)
	 */
	public Object getItem(int position) {
		return mListData.get(position);
	}

	/**
	 * Use the list index as a unique id.
	 * 
	 * @see android.widget.ListAdapter#getItemId(int)
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Make a view to hold each row.
	 * 
	 * @see android.widget.ListAdapter#getView(int, android.view.View,
	 *      android.view.ViewGroup)
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		CheckBox chkBox = null;		
		
		if (convertView == null) {
			vi = mInflater.inflate(mListItemResourceID, parent, false);
			
			SimpleEntry<View, MetadataViewHolder> thisPair = MetrixListScreenManager.generateListItem(mScreenType, mActivity, mInflater,
					vi, mTableLayoutResourceID, mTableRowResourceID, mDefaultColorResourceID, mCheckboxOrImageResourceID, mScreenID);
			vi = thisPair.getKey();
			holder = thisPair.getValue();
			
			if (MetrixStringHelper.valueIsEqual(mScreenType, "LIST_CHECKBOX")) {
				chkBox = holder.mBox.get();
				chkBox.setOnClickListener(new View.OnClickListener() {
					public void onClick(View checkboxView) {
						int getPosition = (Integer) checkboxView.getTag();
						HashMap<String, String> dataRow = mListData.get(getPosition);

						String checkState = "";
						if (((CheckBox) checkboxView).isChecked()) {
							checkState = "Y";
						} else {
							checkState = "N";
						}

						dataRow.put("checkboxState", checkState);
					}
				});			
			}
			
			vi.setTag(holder);
		} else {
			holder = (MetadataViewHolder) vi.getTag();
		}
		
		if (MetrixStringHelper.valueIsEqual(mScreenType, "LIST_CHECKBOX")) {
			chkBox = holder.mBox.get();
			chkBox.setTag(position);
		}
		
		HashMap<String, String> dataRow = mListData.get(position);
		MetrixListScreenManager.populateListItemView(mScreenType, mActivity, holder, dataRow,
				mFieldNameForCheckboxOrImage, mDefaultImageResourceID, mFieldValueMapForImageLookup, mScreenID);
		
		if (MetrixStringHelper.valueIsEqual(mScreenType, "LIST_CHECKBOX")) {
			String chkState = dataRow.get("checkboxState");
			if (chkState.compareToIgnoreCase("Y") == 0)
				chkBox.setChecked(true);
			else
				chkBox.setChecked(false);
		}
		
		return vi;
	}
}
