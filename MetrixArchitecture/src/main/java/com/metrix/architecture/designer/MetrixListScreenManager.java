package com.metrix.architecture.designer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressLint({ "UseSparseArrays", "DefaultLocale" }) 
public class MetrixListScreenManager extends MetrixDesignerManager {
	private static final String COLUMN_NAME = "COLUMN_NAME";
	private static final String CONTROL_EVENT = "CONTROL_EVENT";
	private static final String CONTROL_TYPE = "CONTROL_TYPE";
	private static final String CURRENCY_COLUMN_NAME = "CURRENCY_COLUMN_NAME";
	private static final String CURSOR_ORDER = "CURSOR_ORDER";
	private static final String DATA_TYPE = "DATA_TYPE";
	private static final String DISPLAY_ORDER = "DISPLAY_ORDER";
	private static final String DISPLAY_STYLE = "DISPLAY_STYLE";
	private static final String FIELD_ID = "FIELD_ID";
	private static final String IS_CURRENCY_COLUMN = "IS_CURRENCY_COLUMN";
	private static final String LIST_DISPLAY_COLUMN = "LIST_DISPLAY_COLUMN";
	private static final String LIST_FILTER_COLUMN = "LIST_FILTER_COLUMN";
	private static final String LIST_FILTER_VALUE = "LIST_FILTER_VALUE";
	private static final String LIST_JOIN_TYPE = "LIST_JOIN_TYPE";
	private static final String LIST_VALUE_COLUMN = "LIST_VALUE_COLUMN";
	private static final String LIST_TABLE_NAME = "LIST_TABLE_NAME";
	private static final String RELATIVE_WIDTH = "RELATIVE_WIDTH";
	private static final String SEARCHABLE = "SEARCHABLE";
	private static final String TABLE_ALIAS = "TABLE_ALIAS";
	private static final String TABLE_NAME = "TABLE_NAME";
	private static final String VIEW_ID = "VIEW_ID";

	private static final String DATA_TYPE_STRING = "STRING";
	private static final String DATA_TYPE_NUMBER = "NUMBER";
	private static final String DATA_TYPE_DATE = "DATE";
	private static final String DATA_TYPE_DATE_TIME = "DATE_TIME";
	private static final String DATA_TYPE_TIME = "TIME";

	private static final String HYPERLINK = "HYPERLINK";
	private static final String TEXT = "TEXT";

	private static final String SLIVER_COLUMN = "custom.sliver_color";

	public static class AttachmentMetadataViewHolder {
		WeakReference<ImageView> mThumbnail;
		ArrayList<WeakReference<View>> mVisibleViews;
		HashMap<String, WeakReference<View>> mInvisibleViews;
	}

	public static class MetadataViewHolder {
		WeakReference<CheckBox> mBox;
		WeakReference<ImageView> mImage;
		WeakReference<View> mSliverView;
		ArrayList<WeakReference<View>> mVisibleViews;
		HashMap<String, WeakReference<View>> mInvisibleViews;
	}
	
	// outer key - screen_id
	// middle key - table_name.column_name
	// inner key - property name
	// inner value - property value
	private static HashMap<Integer, HashMap<String, HashMap<String, String>>> itemFieldProperties = new HashMap<Integer, HashMap<String, HashMap<String, String>>>();
	
	// outer key = screen_id
	// inner key = positive display_order
	// inner value = absolute positive order
	private static HashMap<Integer, HashMap<Integer, Integer>> displayOrderToActualOrderMap = new HashMap<Integer, HashMap<Integer, Integer>>();

	// key = screen_id
	// value = Boolean
	private static HashMap<Integer, Boolean> sliverScreenCache = new HashMap<>();

	public static void clearItemFieldPropertiesCache() {
		itemFieldProperties.clear();
		displayOrderToActualOrderMap.clear();
		sliverScreenCache.clear();
	}

	/**
	 * Provide the results of a self-healing cache, a hash map of all list item fields and their properties.
	 * 
	 * @param activity The current activity.
	 * 
	 * @since 5.6.2
	 */
	public static HashMap<String, HashMap<String, String>> getFieldSetForListScreen(Activity activity) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return getFieldSetForListScreen(screenId);
	}
	
	/**
	 * Provide the results of a self-healing cache, a hash map of all list item fields and their properties.
	 * 
	 * @param screenId The current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static HashMap<String, HashMap<String, String>> getFieldSetForListScreen(int screenId) {
		HashMap<String, HashMap<String, String>> fieldSet = itemFieldProperties.get(screenId);
		if (fieldSet == null) {
			// regenerate cache from DB
			fieldSet = new HashMap<String, HashMap<String, String>>();
			MetrixCursor fieldCursor = null;
			
			try {
				// not retrieving control_type or read_only, since they are constant for list screens
				// don't need visible, since display_order contains that information
				String query = "SELECT use_mm_field.field_id, lower(use_mm_field.table_name), lower(use_mm_field.column_name)," 
						+ " use_mm_field.currency_column_name, use_mm_field.data_type,"
						+ " use_mm_field.display_order, use_mm_field.display_style, use_mm_field.list_display_column,"
						+ " use_mm_field.list_filter_column, use_mm_field.list_filter_value, use_mm_field.list_join_type,"
						+ " use_mm_field.list_table_name, use_mm_field.list_value_column, use_mm_field.relative_width,"
						+ " use_mm_field.control_type, use_mm_field.control_event, use_mm_field.searchable"
						+ " FROM use_mm_field WHERE use_mm_field.screen_id = " + screenId;
				
				fieldCursor = MetrixDatabaseManager.rawQueryMC(query, null);		
				if (fieldCursor == null || !fieldCursor.moveToFirst()) {
					throw new Exception("getFieldSetForListScreen: No field metadata found!");
				}
				
				int cursorOrderIterator = 0;
				HashMap<String, Integer> aliasCounter = new HashMap<String, Integer>();
				while (fieldCursor.isAfterLast() == false) {
					String fieldId = fieldCursor.getString(0);			
					String tableName = fieldCursor.getString(1);
					String columnName = fieldCursor.getString(2);
					String currencyColumnName = fieldCursor.getString(3);
					String dataType = fieldCursor.getString(4);
					String displayOrder = fieldCursor.getString(5);
					String displayStyle = fieldCursor.getString(6);
					String listDisplayColumn = fieldCursor.getString(7);
					String listFilterColumn = fieldCursor.getString(8);
					String listFilterValue = fieldCursor.getString(9);
					String listJoinType = fieldCursor.getString(10);
					String listTableName = fieldCursor.getString(11);
					String listValueColumn = fieldCursor.getString(12);
					String relativeWidth = fieldCursor.getString(13);
					String controlType = fieldCursor.getString(14);
					String controlEvent = fieldCursor.getString(15);
					String searchable = fieldCursor.getString(16);
					
					if (!MetrixStringHelper.isNullOrEmpty(currencyColumnName)) currencyColumnName = currencyColumnName.toLowerCase();
					if (!MetrixStringHelper.isNullOrEmpty(listDisplayColumn)) listDisplayColumn = listDisplayColumn.toLowerCase();
					if (!MetrixStringHelper.isNullOrEmpty(listFilterColumn)) listFilterColumn = listFilterColumn.toLowerCase();
					if (!MetrixStringHelper.isNullOrEmpty(listTableName)) listTableName = listTableName.toLowerCase();
					if (!MetrixStringHelper.isNullOrEmpty(listValueColumn)) listValueColumn = listValueColumn.toLowerCase();
						
					// only process this field if the tableName corresponds to a table in the client DB
					if (!clientDBContainsTable(tableName)) {
						LogManager.getInstance().info(String.format("Table not found in client DB for the %1$s.%2$s field ... will not use this field in list.", tableName, columnName));
						fieldCursor.moveToNext();
						continue;
					}
					
					// don't use LIST properties if listTableName not in client DB
					if (!MetrixStringHelper.isNullOrEmpty(listTableName) && !clientDBContainsTable(listTableName)) {
						LogManager.getInstance().info(String.format("List table name (%1$s) not found in client DB for the %2$s.%3$s field ... will not use LIST properties when rendering field in list.", listTableName, tableName, columnName));
						listDisplayColumn = listValueColumn = listTableName = listFilterColumn = listFilterValue = "";
					}
					
					String cursorOrder = "-1";
					if (!MetrixStringHelper.valueIsEqual(tableName, "custom")) {
						cursorOrder = String.valueOf(cursorOrderIterator);
						cursorOrderIterator++;
						
						// add provision for a currency column in the itemFieldProperties set, if defined 
						// ... need to add this to make sure it gets in the query
						// currency column MUST be on same table (so it only works on non-CUSTOM table fields) 
						if (!MetrixStringHelper.isNullOrEmpty(currencyColumnName)) {
							HashMap<String, String> currencyPropSet = new HashMap<String, String>();
							String currencyCursorOrder = String.valueOf(cursorOrderIterator);
							cursorOrderIterator++;
							currencyPropSet.put(CURSOR_ORDER, currencyCursorOrder);
							currencyPropSet.put(IS_CURRENCY_COLUMN, "Y");
							String currencyFieldName = String.format("%1$s.%2$s", tableName, currencyColumnName);
							fieldSet.put(currencyFieldName, currencyPropSet);
						}
					}
					
					String tableAlias = "";
					if (!MetrixStringHelper.isNullOrEmpty(listTableName) && !MetrixStringHelper.isNullOrEmpty(listValueColumn)) {
						tableAlias = listTableName.trim();
						
						// if we are using LIST_ properties to define *replacement* value (i.e., there exists a listDisplayColumn),
						// we need to set up a unique alias for listTableName and use "alias.listDisplayColumn" on fieldArray
						// we need to make the join use the alias, as well
						if (!MetrixStringHelper.isNullOrEmpty(listDisplayColumn)) {
							Integer counter = aliasCounter.get(listTableName);
							if (counter == null) {
								counter = 1;
							} else {
								counter++;
							}
							
							tableAlias = listTableName + String.valueOf(counter);
							aliasCounter.put(listTableName, counter);
						}
					}
					
					HashMap<String, String> propSet = new HashMap<String, String>();
					propSet.put(COLUMN_NAME, columnName);
					propSet.put(CONTROL_EVENT, controlEvent);
					propSet.put(CONTROL_TYPE, controlType);
					propSet.put(CURRENCY_COLUMN_NAME, currencyColumnName);
					propSet.put(CURSOR_ORDER, cursorOrder);
					propSet.put(DATA_TYPE, dataType);
					propSet.put(DISPLAY_ORDER, displayOrder);
					propSet.put(DISPLAY_STYLE, displayStyle);
					propSet.put(FIELD_ID, fieldId);
					propSet.put(LIST_DISPLAY_COLUMN, listDisplayColumn);
					propSet.put(LIST_FILTER_COLUMN, listFilterColumn);
					propSet.put(LIST_FILTER_VALUE, listFilterValue);
					propSet.put(LIST_JOIN_TYPE, listJoinType);
					propSet.put(LIST_TABLE_NAME, listTableName);
					propSet.put(LIST_VALUE_COLUMN, listValueColumn);
					propSet.put(RELATIVE_WIDTH, relativeWidth);
					propSet.put(SEARCHABLE, searchable);
					propSet.put(TABLE_ALIAS, tableAlias);
					propSet.put(TABLE_NAME, tableName);
					propSet.put(VIEW_ID, String.valueOf(MetrixControlAssistant.generateViewId()));

					String fieldName = String.format("%1$s.%2$s", tableName, columnName);
					fieldSet.put(fieldName, propSet);
					
					fieldCursor.moveToNext();
				}
				
				itemFieldProperties.put(screenId, fieldSet);
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				if (fieldCursor != null && (!fieldCursor.isClosed())) {
					fieldCursor.close();
				}
			}
		}
		String screenType = MetrixScreenManager.getScreenType(screenId);
		if(screenType.equals("ATTACHMENT_API_LIST") && fieldSet.get("attachment.on_demand") == null)
		{
			HashMap<String, String> propSet = new HashMap<String, String>();
			propSet.put(COLUMN_NAME, "on_demand");
			propSet.put(CONTROL_TYPE, "TEXT");
			propSet.put(DATA_TYPE, "STRING");
			propSet.put(DISPLAY_ORDER, "-100");
			propSet.put(LIST_VALUE_COLUMN, null);
			propSet.put(LIST_FILTER_VALUE, null);
			propSet.put(LIST_DISPLAY_COLUMN, null);
			propSet.put(CONTROL_EVENT, null);
			propSet.put(LIST_FILTER_COLUMN, null);
			propSet.put(LIST_TABLE_NAME, null);
			propSet.put(DISPLAY_STYLE, "BLACK_AND_WHITE");
			propSet.put(TABLE_ALIAS, "");
			propSet.put(VIEW_ID, "233");
			propSet.put(TABLE_NAME, "attachment");
			propSet.put(RELATIVE_WIDTH, "1");
			propSet.put("VISIBLE", "N");
			propSet.put("ALLOW_CLEAR", "Y");
			propSet.put("READ_ONLY", "Y");
			propSet.put("INCLUDE_IN_QUERY", "Y");
			propSet.put(CURSOR_ORDER, String.valueOf(fieldSet.size()));

			fieldSet.put("attachment.on_demand", propSet);
		}



		return fieldSet;
	}
	
	/**
	 * Generates a list item (both view and holding object), based on relevant list item field metadata.
	 * 
	 * @param screenType The screen_type defined in screen metadata.
	 * @param activity The current activity.
	 * @since 5.6.2
	 */
	public static SimpleEntry<View, MetadataViewHolder> generateListItem(String screenType, Activity activity, 
			LayoutInflater inflater, View vi, int tableLayoutResourceID, int tableRowResourceID, 
			int defaultColorResourceID, int checkboxOrImageResourceID, int sliverID) {
		
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return generateListItem(screenType, activity, inflater, vi, tableLayoutResourceID, tableRowResourceID, 
				defaultColorResourceID, checkboxOrImageResourceID, sliverID, screenId);
	}

	public static SimpleEntry<View, AttachmentMetadataViewHolder> generateAttachmentListItem(Activity activity,
		LayoutInflater inflater, View vi, int tableLayoutResourceID, int tableRowResourceID, int thumbnailResourceID, int defaultColorResourceID, int screenId) {
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
		HashMap<Integer, Integer> orderMap = getDisplayOrderToActualOrderMap(screenId);
		if (fieldSet.size() > 0) {
			int colorToUse = 0;
			String secondaryColor = MetrixSkinManager.getSecondaryColor();
			if (MetrixStringHelper.isNullOrEmpty(secondaryColor)) {
				colorToUse = activity.getResources().getColor(defaultColorResourceID);
			} else {
				colorToUse = Color.parseColor(secondaryColor);
			}

			AttachmentMetadataViewHolder holder = new AttachmentMetadataViewHolder();
			holder.mVisibleViews = new ArrayList<WeakReference<View>>();
			holder.mInvisibleViews = new HashMap<String, WeakReference<View>>();
			holder.mThumbnail = null;
			String[] orderedVisibleViewList = new String[fieldSet.size()];

			final float dpToPxScale = activity.getResources().getDisplayMetrics().density;
			final int pxScreenWidth = activity.getResources().getDisplayMetrics().widthPixels;
			int pxTablePadding = (int) (5 * dpToPxScale + 0.5f);
			int pxToReserveForPaddings = (int) (20 * dpToPxScale + 0.5f);

			ImageView imageView = (ImageView) vi.findViewById(thumbnailResourceID);
			holder.mThumbnail = new WeakReference<ImageView>(imageView);
			// add thumbnail's width to pixel amount that we reserve
			pxToReserveForPaddings = pxToReserveForPaddings + imageView.getLayoutParams().width;
			// pxUsableScreenWidth denotes "100% of usable screen width", for use with relative_width
			BigDecimal pxUsableScreenWidth = new BigDecimal(String.valueOf(pxScreenWidth - pxToReserveForPaddings));
			BigDecimal currentUsedWidth = BigDecimal.ZERO;
			BigDecimal relativeWidthLimit = BigDecimal.ONE;

			int currentOrientation = activity.getResources().getConfiguration().orientation;
			if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
				relativeWidthLimit = new BigDecimal("2");
				// pxUsableScreenWidth now has to be 50% of total usable screen width,
				// so that we can use it twice to represent a total relative width of 2.0 on a single row
				pxUsableScreenWidth = pxUsableScreenWidth.divide(new BigDecimal("2"));
			}

			LinearLayout tableLayout = (LinearLayout) vi.findViewById(tableLayoutResourceID);
			LinearLayout currentRow = (LinearLayout) inflater.inflate(tableRowResourceID, null);
			currentRow.setId(MetrixControlAssistant.generateViewId());
			for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet()) {
				String fieldName = entry.getKey();
				String identifier = getListRowControlIdentifier(fieldName);
				HashMap<String, String> propSet = entry.getValue();

				String isCurrencyColumn = propSet.get(IS_CURRENCY_COLUMN);
				if (!MetrixStringHelper.isNullOrEmpty(isCurrencyColumn) && MetrixStringHelper.valueIsEqual(isCurrencyColumn, "Y") ) {
					// don't attempt to render a column that we retrieved JUST for currency purposes
					continue;
				}

				int viewId = Integer.valueOf(propSet.get(VIEW_ID));
				int displayOrder = Integer.valueOf(propSet.get(DISPLAY_ORDER));
				String controlType = propSet.get(CONTROL_TYPE);
				if (displayOrder > 0) {
					int visibleArrayIndex = orderMap.get(displayOrder) - 1;
					orderedVisibleViewList[visibleArrayIndex] = fieldName;
				} else {
					if (MetrixStringHelper.valueIsEqual(controlType, "TEXT")) {
						TextView invisibleTextView = new TextView(activity);
						invisibleTextView.setId(viewId);
						invisibleTextView.setTag(identifier);
						invisibleTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						invisibleTextView.setVisibility(View.GONE);

						currentRow.addView(invisibleTextView);
						holder.mInvisibleViews.put(fieldName, new WeakReference<View>(invisibleTextView));
					}
				}
			}

			float currentFontScale =  activity.getResources().getConfiguration().fontScale;
			List<TextView> textFieldList = new ArrayList<>();
			int totalFieldWidth = 0;

			for (int i = 0; i < orderedVisibleViewList.length; i++) {
				String fieldName = orderedVisibleViewList[i];
				// we could have invisible fields, so break on the first blank fieldName
				if (MetrixStringHelper.isNullOrEmpty(fieldName)) break;
				String identifier = getListRowControlIdentifier(fieldName);

				HashMap<String, String> propSet = fieldSet.get(fieldName);
				int viewId = Integer.valueOf(propSet.get(VIEW_ID));
				String displayStyle = propSet.get(DISPLAY_STYLE);
				String relativeWidthString = propSet.get(RELATIVE_WIDTH);
				if (MetrixStringHelper.isNullOrEmpty(relativeWidthString)) relativeWidthString = "1";
				BigDecimal relativeWidth = new BigDecimal(MetrixFloatHelper.convertNumericFromDBToForcedLocale(relativeWidthString, Locale.US));

				// if the current font scale is Large or Huge calculate the relative width
				if (currentFontScale > 1.0){
					relativeWidth = relativeWidth.multiply(BigDecimal.valueOf(currentFontScale));
					if (relativeWidth.compareTo(relativeWidthLimit) > 0) {
						relativeWidth = relativeWidthLimit;
					}
				}

				// if this next field is too wide, stow this row away in the table
				// and create a new one, resetting currentUsedWidth
				if (relativeWidth.compareTo(relativeWidthLimit.subtract(currentUsedWidth)) > 0) {
					// calculate the field width for specific row
					// and available width is equally distributed to the fields
					CalculateListScreenFieldWidthForLargeFontScale(textFieldList,relativeWidthLimit,pxUsableScreenWidth,totalFieldWidth);
					totalFieldWidth = 0;
					tableLayout.addView(currentRow);
					currentRow = (LinearLayout) inflater.inflate(tableRowResourceID, null);
					currentRow.setId(MetrixControlAssistant.generateViewId());
					currentUsedWidth = BigDecimal.ZERO;
				}

				String controlType = propSet.get(CONTROL_TYPE);
				if (controlType.compareToIgnoreCase(TEXT) == 0) {
					TextView visibleTextView = new TextView(activity);
					visibleTextView.setId(viewId);
					visibleTextView.setTag(identifier);
					visibleTextView.setContentDescription(identifier);
					BigDecimal widthToUse = relativeWidth.multiply(pxUsableScreenWidth);
					visibleTextView.setLayoutParams(new LinearLayout.LayoutParams(widthToUse.intValue(), LinearLayout.LayoutParams.WRAP_CONTENT));
					visibleTextView.setPadding(pxTablePadding, 0, pxTablePadding, 0);
					visibleTextView.setTextAppearance(activity, android.R.style.TextAppearance_Medium);
					if (MetrixStringHelper.valueIsEqual(displayStyle, "COLOR")) {
						visibleTextView.setTypeface(null, Typeface.BOLD);
						visibleTextView.setTextColor(colorToUse);
					} else {
						visibleTextView.setTextColor(Color.BLACK);
					}
					textFieldList.add(visibleTextView);
					totalFieldWidth += widthToUse.intValue();
					currentRow.addView(visibleTextView);
					holder.mVisibleViews.add(i, new WeakReference<View>(visibleTextView));
				}

				currentUsedWidth = currentUsedWidth.add(relativeWidth);
			}

			if (currentRow != null && currentRow.getChildCount() > 0) {
				// if we exited the loop in the midst of adding views to a TableRow,
				// add this partially-filled TableRow here, to finish it off
				CalculateListScreenFieldWidthForLargeFontScale(textFieldList,relativeWidthLimit,pxUsableScreenWidth,totalFieldWidth);
				tableLayout.addView(currentRow);
			}

			SimpleEntry<View, AttachmentMetadataViewHolder> thisPair = new SimpleEntry<>(vi, holder);
			return thisPair;
		}

		return null;
	}

	/**
	 * Generates a list item (both view and holding object), based on relevant list item field metadata.
	 * 
	 * @param screenType The screen_type defined in screen metadata.
	 * @param activity The current activity.
	 * @param screenId The current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static SimpleEntry<View, MetadataViewHolder> generateListItem(String screenType, Activity activity, 
		LayoutInflater inflater, View vi, int tableLayoutResourceID, int tableRowResourceID, 
		int defaultColorResourceID, int checkboxOrImageResourceID, int sliverID, int screenId) {
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
		HashMap<Integer, Integer> orderMap = getDisplayOrderToActualOrderMap(screenId);
		if (fieldSet.size() > 0) {
		    int colorToUse = 0;
		    String secondaryColor = MetrixSkinManager.getSecondaryColor();
		    if (MetrixStringHelper.isNullOrEmpty(secondaryColor)) {
		    	colorToUse = activity.getResources().getColor(defaultColorResourceID);
		    } else {
		    	colorToUse = Color.parseColor(secondaryColor);
		    }

		    final View sliver = vi.findViewById(sliverID);
			MetadataViewHolder holder = new MetadataViewHolder();
			holder.mVisibleViews = new ArrayList<WeakReference<View>>();
			holder.mInvisibleViews = new HashMap<String, WeakReference<View>>();
			holder.mImage = null;
			holder.mBox = null;
			holder.mSliverView = sliver != null ? new WeakReference<>(sliver) : null;
			String[] orderedVisibleViewList = new String[fieldSet.size()];
            		
		    final float dpToPxScale = activity.getResources().getDisplayMetrics().density;
		    final int pxScreenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		    int pxTablePadding = (int) (5 * dpToPxScale + 0.5f);
		    int pxToReserveForPaddings = (int) (20 * dpToPxScale + 0.5f);
		    
		    if (MetrixStringHelper.valueIsEqual(screenType, "LIST_IMAGE")) {
		    	ImageView imageView = (ImageView) vi.findViewById(checkboxOrImageResourceID);
		    	if (imageView != null) {
		    		imageView.setContentDescription("ListImage");
					holder.mImage = new WeakReference<ImageView>(imageView);
					// add image's width to pixel amount that we reserve
					pxToReserveForPaddings = pxToReserveForPaddings + imageView.getLayoutParams().width;
					//TO-DO:Tablet UI mode, need an extra padding/margin
				}
		    } else if (MetrixStringHelper.valueIsEqual(screenType, "LIST_CHECKBOX")) {
		    	CheckBox chkBox = (CheckBox) vi.findViewById(checkboxOrImageResourceID);
		    	if (chkBox != null) {
					holder.mBox = new WeakReference<CheckBox>(chkBox);
					// add checkbox's width to pixel amount that we reserve
					pxToReserveForPaddings = pxToReserveForPaddings + chkBox.getLayoutParams().width;
					// for safer side/to properly display lengthier texts, we should have to add a magic number/extra margin
					if (MetrixScreenManager.shouldRunTabletSpecificLandUI(activity)) {
						int extraPaddingForChkBox = (int) (10 * dpToPxScale + 0.5f);
						pxToReserveForPaddings = pxToReserveForPaddings + extraPaddingForChkBox;
					}
				}
		    }

		    // If the screen has sliver, Add the size of the sliver to reserved padding space
		    if (sliver != null && screenHasSliver(screenId))
		    	pxToReserveForPaddings += sliver.getLayoutParams().width;
		    	    
		    // pxUsableScreenWidth denotes "100% of usable screen width", for use with relative_width
		    BigDecimal pxUsableScreenWidth = new BigDecimal(String.valueOf(pxScreenWidth - pxToReserveForPaddings));	    
			BigDecimal currentUsedWidth = BigDecimal.ZERO;
			BigDecimal relativeWidthLimit = BigDecimal.ONE;

			int currentOrientation = activity.getResources().getConfiguration().orientation;
			if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
				if(!MetrixScreenManager.shouldRunTabletSpecificLandUI(activity)){
					relativeWidthLimit = new BigDecimal("2");
					// pxUsableScreenWidth now has to be 50% of total usable screen width,
					// so that we can use it twice to represent a total relative width of 2.0 on a single row
					pxUsableScreenWidth = pxUsableScreenWidth.divide(new BigDecimal("2"));
				}
				//When tablet specific landscape UI required, if there's a link screen(if it's a list)
				else {
					ListWidthDetails listWidthDetails = getListPanelWidthForTabletUI(activity, relativeWidthLimit, pxScreenWidth, pxToReserveForPaddings);
					if(listWidthDetails != null){
						relativeWidthLimit = listWidthDetails.getCalRelativeWidthLimit();
						pxUsableScreenWidth = listWidthDetails.getCalPxScreenWidth();
					}
				}
			}
				    
		    LinearLayout tableLayout = (LinearLayout) vi.findViewById(tableLayoutResourceID);
			LinearLayout currentRow = (LinearLayout) inflater.inflate(tableRowResourceID, null);
			currentRow.setId(MetrixControlAssistant.generateViewId());
			for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet())
			{				
				String fieldName = entry.getKey();
				String identifier = getListRowControlIdentifier(fieldName);
				HashMap<String, String> propSet = entry.getValue();
				
				String isCurrencyColumn = propSet.get(IS_CURRENCY_COLUMN);
				if (!MetrixStringHelper.isNullOrEmpty(isCurrencyColumn) 
					&& MetrixStringHelper.valueIsEqual(isCurrencyColumn, "Y") ) {
					// don't attempt to render a column that we retrieved JUST for currency purposes
					continue;
				}
				
				int viewId = Integer.valueOf(propSet.get(VIEW_ID));
				int displayOrder = Integer.valueOf(propSet.get(DISPLAY_ORDER));
				String controlType = propSet.get(CONTROL_TYPE);
				if (displayOrder > 0) {
					int visibleArrayIndex = orderMap.get(displayOrder) - 1;
					orderedVisibleViewList[visibleArrayIndex] = fieldName;
				} else {
					if(MetrixStringHelper.valueIsEqual(controlType, "TEXT")) {
						TextView invisibleTextView = new TextView(activity);
						invisibleTextView.setId(viewId);
						invisibleTextView.setTag(identifier);
						invisibleTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						invisibleTextView.setVisibility(View.GONE);

						currentRow.addView(invisibleTextView);
						holder.mInvisibleViews.put(fieldName, new WeakReference<View>(invisibleTextView));
					}
					else if(MetrixStringHelper.valueIsEqual(controlType, "HYPERLINK")){
						MetrixHyperlink invisibleHyperlink = new MetrixHyperlink(activity);
						invisibleHyperlink.setId(viewId);
						invisibleHyperlink.setTag(identifier);
						invisibleHyperlink.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						invisibleHyperlink.setVisibility(View.GONE);

						currentRow.addView(invisibleHyperlink);
						holder.mInvisibleViews.put(fieldName, new WeakReference<View>(invisibleHyperlink));
					}
				}
			}
			float currentFontScale =  activity.getResources().getConfiguration().fontScale;
			List<TextView> textFieldList = new ArrayList<>();
			int totalFieldWidth = 0;

			for (int i = 0; i < orderedVisibleViewList.length; i++) {
				String fieldName = orderedVisibleViewList[i];
				// we could have invisible fields, so break on the first blank fieldName
				if (MetrixStringHelper.isNullOrEmpty(fieldName)) break;
				String identifier = getListRowControlIdentifier(fieldName);
				
				HashMap<String, String> propSet = fieldSet.get(fieldName);
				int viewId = Integer.valueOf(propSet.get(VIEW_ID));
				String displayStyle = propSet.get(DISPLAY_STYLE);
				String relativeWidthString = propSet.get(RELATIVE_WIDTH);
				if (MetrixStringHelper.isNullOrEmpty(relativeWidthString)) relativeWidthString = "1";
				BigDecimal relativeWidth = new BigDecimal(MetrixFloatHelper.convertNumericFromDBToForcedLocale(relativeWidthString, Locale.US));

				// if the current font scale is Large or Huge calculate the relative width
				if (currentFontScale > 1.0){
					relativeWidth = relativeWidth.multiply(BigDecimal.valueOf(currentFontScale));
					if (relativeWidth.compareTo(relativeWidthLimit)>0){
						relativeWidth = relativeWidthLimit;
					}
				}

				// if this next field is too wide, stow this row away in the table
				// and create a new one, resetting currentUsedWidth
				if (relativeWidth.compareTo(relativeWidthLimit.subtract(currentUsedWidth)) > 0) {
					// calculate the field width for specific row
					// and available width is equally distributed to the fields
					CalculateListScreenFieldWidthForLargeFontScale(textFieldList,relativeWidthLimit,pxUsableScreenWidth,totalFieldWidth);
					totalFieldWidth = 0;
					tableLayout.addView(currentRow);
					currentRow = (LinearLayout) inflater.inflate(tableRowResourceID, null);
					currentRow.setId(MetrixControlAssistant.generateViewId());
					currentUsedWidth = BigDecimal.ZERO;
				}

				String controlType = propSet.get(CONTROL_TYPE);
				if(controlType.compareToIgnoreCase(TEXT) == 0) {
					TextView visibleTextView = new TextView(activity);
					visibleTextView.setId(viewId);
					visibleTextView.setTag(identifier);
					visibleTextView.setContentDescription(identifier);
					BigDecimal widthToUse = relativeWidth.multiply(pxUsableScreenWidth);
					visibleTextView.setLayoutParams(new LinearLayout.LayoutParams(widthToUse.intValue(), LinearLayout.LayoutParams.WRAP_CONTENT));
					visibleTextView.setPadding(pxTablePadding, 0, pxTablePadding, 0);
					visibleTextView.setTextAppearance(activity, android.R.style.TextAppearance_Medium);
					if (MetrixStringHelper.valueIsEqual(displayStyle, "COLOR")) {
						visibleTextView.setTypeface(null, Typeface.BOLD);
						visibleTextView.setTextColor(colorToUse);
					} else {
						visibleTextView.setTextColor(Color.BLACK);
					}
					textFieldList.add(visibleTextView);
					totalFieldWidth += widthToUse.intValue();
					currentRow.addView(visibleTextView);
					holder.mVisibleViews.add(i, new WeakReference<View>(visibleTextView));
				}
				else if(controlType.compareToIgnoreCase(HYPERLINK) == 0){
					MetrixHyperlink visibleHyperlink = new MetrixHyperlink(activity);
					visibleHyperlink.setId(viewId);
					visibleHyperlink.setTag(identifier);
					visibleHyperlink.setContentDescription(identifier);
					BigDecimal widthToUse = relativeWidth.multiply(pxUsableScreenWidth);
					visibleHyperlink.setLayoutParams(new LinearLayout.LayoutParams(widthToUse.intValue(), LinearLayout.LayoutParams.WRAP_CONTENT));
					visibleHyperlink.setPadding(pxTablePadding, 0, pxTablePadding, 0);
					visibleHyperlink.setTextAppearance(activity, android.R.style.TextAppearance_Medium);
					textFieldList.add(visibleHyperlink);
					totalFieldWidth += widthToUse.intValue();
					currentRow.addView(visibleHyperlink);
					holder.mVisibleViews.add(i, new WeakReference<View>(visibleHyperlink));
				}
				
				currentUsedWidth = currentUsedWidth.add(relativeWidth);				
			}
			
			if (currentRow != null && currentRow.getChildCount() > 0) {
				// if we exited the loop in the midst of adding views to a TableRow,
				// add this partially-filled TableRow here, to finish it off
				CalculateListScreenFieldWidthForLargeFontScale(textFieldList,relativeWidthLimit,pxUsableScreenWidth,totalFieldWidth);
				tableLayout.addView(currentRow);
			}
            
			SimpleEntry<View, MetadataViewHolder> thisPair = new SimpleEntry<View, MetadataViewHolder>(vi, holder);
			return thisPair;
		}
		
		return null;
	}
	
	/**
	 * Generates a list population query for list screens, based on the Designer metadata.
	 * 
	 * @param activity The current activity.
	 * @param primaryTableName The name of the root table to use for this query.
	 * @param whereClause The where clause to use when constructing this query (including order by, etc.).
	 * 
	 * @since 5.6.2
	 */
	public static String generateListQuery(Activity activity, String primaryTableName, String whereClause) {
		return generateListQuery(activity, primaryTableName, whereClause, "");
	}

	/**
	 * Generates a list population query for list screens, based on the Designer metadata.
	 *
	 * @param activity The current activity.
	 * @param primaryTableName The name of the root table to use for this query.
	 * @param whereClause The where clause to use when constructing this query (including order by, etc.).
	 * @param listSearchConstraint The user-entered search constraint to further filter the result set.
	 *
	 * @since 5.7.0
	 */
	public static String generateListQuery(Activity activity, String primaryTableName, String whereClause, String listSearchConstraint) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		if (listSearchConstraint.contains("'"))
			listSearchConstraint = listSearchConstraint.replace("'", "''");
		return generateListQuery(primaryTableName, whereClause, listSearchConstraint, screenId);
	}

	/**
	 * Generates a list population query for list screens, based on the Designer metadata.
	 * 
	 * @param primaryTableName The name of the root table to use for this query.
	 * @param whereClause The where clause to use when constructing this query (including order by, etc.).
	 * @param screenId The current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static String generateListQuery(String primaryTableName, String whereClause, int screenId) {
		return generateListQuery(primaryTableName, whereClause, "", screenId);
	}

	/**
	 * Generates a list population query for list screens, based on the Designer metadata.
	 *
	 * @param primaryTableName The name of the root table to use for this query.
	 * @param whereClause The where clause to use when constructing this query (including order by, etc.).
	 * @param listSearchConstraint The user-entered search constraint to further filter the result set.
	 * @param screenId The current screen id.
	 *
	 * @since 5.7.0
	 */
	public static String generateListQuery(String primaryTableName, String whereClause, String listSearchConstraint, int screenId) {
		String finalQuery = "";
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);

		if (fieldSet.size() > 0) {
			int fieldArrayMaxSize = fieldSet.size();
			String[] fieldArray = new String[fieldArrayMaxSize];
			ArrayList<JoinInfo> innerJoinArray = new ArrayList<JoinInfo>();
			ArrayList<JoinInfo> outerJoinArray = new ArrayList<JoinInfo>();
			StringBuilder columnChunk = new StringBuilder();
			StringBuilder joinChunk = new StringBuilder();

			for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet()) {
				HashMap<String, String> propSet = entry.getValue();

				int thisCursorOrder = Integer.valueOf(propSet.get(CURSOR_ORDER));
				if (thisCursorOrder < 0)
					continue;

				String fieldName = entry.getKey();
				String fieldNameForCursor = fieldName;
				String listDisplayColumn = propSet.get(LIST_DISPLAY_COLUMN);
				String listFilterColumn = propSet.get(LIST_FILTER_COLUMN);
				String listFilterValue = propSet.get(LIST_FILTER_VALUE);
				String listJoinType = propSet.get(LIST_JOIN_TYPE);
				String listTableName = propSet.get(LIST_TABLE_NAME);
				String listValueColumn = propSet.get(LIST_VALUE_COLUMN);
				String tableAlias = propSet.get(TABLE_ALIAS);
				String tableName = propSet.get(TABLE_NAME);

				// if we are using LIST_ properties at all (minimum listTableName/listValueColumn), we need to construct a join statement
				if (!MetrixStringHelper.isNullOrEmpty(listTableName) && !MetrixStringHelper.isNullOrEmpty(listValueColumn)) {
					boolean deployTableAlias = false;

					// if we are using LIST_ properties to define *replacement* value (i.e., there exists a listDisplayColumn),
					// we need to set up a unique alias for listTableName and use "alias.listDisplayColumn" on fieldArray
					// we need to make the join use the alias, as well
					if (!MetrixStringHelper.isNullOrEmpty(listDisplayColumn)) {
						deployTableAlias = true;
						fieldNameForCursor = String.format("%1$s.%2$s", tableAlias, listDisplayColumn);
					}

					JoinInfo thisJoinInfo = new JoinInfo(fieldName, tableName, listTableName, deployTableAlias, tableAlias, listValueColumn, listFilterColumn, listFilterValue);
					if (MetrixStringHelper.valueIsEqual(listJoinType, "INNER")) {
						innerJoinArray.add(thisJoinInfo);
					} else {
						outerJoinArray.add(thisJoinInfo);
					}
				}

				fieldArray[thisCursorOrder] = fieldNameForCursor;
			}

			// only fields that need to be retrieved should be in this array, in sequential order, starting at index 0
			// it's entirely possible that we have some fields without cursor order (CUSTOM table fields)
			// so, we stop looking after we encounter the first null/empty string
			for (int i = 0; i < fieldArrayMaxSize; i++) {
				String thisFieldName = fieldArray[i];
				if (MetrixStringHelper.isNullOrEmpty(thisFieldName))
					break;

				columnChunk.append(String.format("%s, ", thisFieldName));
			}

			// must add inner joins before outer joins, also add joins linking to primary table first (allows for two-level joins)
			for (JoinInfo item : innerJoinArray) {
				if (MetrixStringHelper.valueIsEqual(item.mTableName, primaryTableName)) {
					joinChunk = generateThisJoin(joinChunk, item, false);
				}
			}
			for (JoinInfo item : innerJoinArray) {
				if (!MetrixStringHelper.valueIsEqual(item.mTableName, primaryTableName)) {
					joinChunk = generateThisJoin(joinChunk, item, false);
				}
			}

			for (JoinInfo item : outerJoinArray) {
				if (MetrixStringHelper.valueIsEqual(item.mTableName, primaryTableName)) {
					joinChunk = generateThisJoin(joinChunk, item, true);
				}
			}
			for (JoinInfo item : outerJoinArray) {
				if (!MetrixStringHelper.valueIsEqual(item.mTableName, primaryTableName)) {
					joinChunk = generateThisJoin(joinChunk, item, true);
				}
			}

			// make sure all elements are suitably formatted
			String columnString = columnChunk.toString();
			columnString = columnString.substring(0, columnString.lastIndexOf(","));
			primaryTableName = primaryTableName.trim();
			String joinString = joinChunk.toString().trim();
			whereClause = !MetrixStringHelper.isNullOrEmpty(whereClause) ? whereClause.trim() : "";
			if (!MetrixStringHelper.isNullOrEmpty(whereClause) && !whereClause.toLowerCase().startsWith("where")) {
				whereClause = "where " + whereClause;
			}

			String searchConstraintChunk = "";
			if (!MetrixStringHelper.isNullOrEmpty(listSearchConstraint)) {
				// augment the where clause to filter on any non-CUSTOM table field with display_order > 0 OR searchable = 'Y'
				// where content is LIKE the listSearchConstraint
				StringBuilder searchConstraintBuilder = new StringBuilder();
				if (MetrixStringHelper.isNullOrEmpty(whereClause))
					searchConstraintBuilder.append("where (");
				else
					searchConstraintBuilder.append("and (");

				boolean firstSearchTerm = true;
				for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet()) {
					HashMap<String, String> propSet = entry.getValue();

					// CUSTOM table fields have negative cursor order; we want to exclude these from consideration
					int thisCursorOrder = Integer.valueOf(propSet.get(CURSOR_ORDER));
					if (thisCursorOrder < 0)
						continue;

					int thisDisplayOrder = Integer.valueOf(propSet.get(DISPLAY_ORDER));
					String searchable = propSet.get(SEARCHABLE);
					if (thisDisplayOrder > 0 || MetrixStringHelper.valueIsEqual(searchable, "Y")) {
						// this is a field we want to search on
						if (!firstSearchTerm)
							searchConstraintBuilder.append(" or ");
						else
							firstSearchTerm = false;

						String fieldNameForQuery = fieldArray[thisCursorOrder];
						searchConstraintBuilder.append(String.format("%1$s LIKE '%%%2$s%%'", fieldNameForQuery, listSearchConstraint));
					}
				}

				if (!firstSearchTerm)	// we found at least one search term
					searchConstraintChunk = searchConstraintBuilder.toString() + ")";
			}

			if (!MetrixStringHelper.isNullOrEmpty(searchConstraintChunk)) {
				// If the original where clause chunk includes an order by clause,
				// insert the search clause directly before the order by chunk by means of substring parsing/insertion.
				// Otherwise, just put the searchConstraintChunk directly on the end of the existing whereClause
				if (MetrixStringHelper.isNullOrEmpty(whereClause))
					whereClause = searchConstraintChunk;
				else if (whereClause.toLowerCase().contains("order by")) {
					String testWhere = whereClause.toLowerCase();
					int orderByIndex = testWhere.indexOf("order by");				// case-insensitive detection of "order by" index
					String leftWhere = whereClause.substring(0, orderByIndex - 1);
					String orderByChunk = whereClause.substring(orderByIndex);
					whereClause = String.format("%1$s %2$s %3$s", leftWhere, searchConstraintChunk, orderByChunk);
				} else
					whereClause = String.format("%1$s %2$s", whereClause, searchConstraintChunk);
			}

			finalQuery = String.format("select distinct %1$s from %2$s", columnString, primaryTableName);
			if (!MetrixStringHelper.isNullOrEmpty(joinString))
				finalQuery = String.format("%1$s %2$s", finalQuery, joinString);
			if (!MetrixStringHelper.isNullOrEmpty(whereClause))
				finalQuery = String.format("%1$s %2$s", finalQuery, whereClause);
		}

		return finalQuery;
	}

	/**
	 * Provides a populated HashMap for a single row of data, based on cursor and list item metadata.
	 * 
	 * @param activity The current activity.
	 * @param cursor The current cursor in use to iterate over returned DB rows.
	 * 
	 * @since 5.6.2
	 */
	public static HashMap<String, String> generateRowFromCursor(Activity activity, MetrixCursor cursor) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return generateRowFromCursor(cursor, screenId);
	}
	
	/**
	 * Provides a populated HashMap for a single row of data, based on cursor and list item metadata.
	 * 
	 * @param cursor The current cursor in use to iterate over returned DB rows.
	 * @param screenId current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static HashMap<String, String> generateRowFromCursor(MetrixCursor cursor, int screenId) {
		// any filling of CUSTOM fields will be done in the activity-specific populateList method
		// here, we populate the row HashMap with data retrieved from DB, 
		// using itemFieldProperties.CURSOR_ORDER (> -1) to get the data from the correct cursor position

		HashMap<String, String> row = new HashMap<String, String>();
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
		
		if (fieldSet.size() > 0) {
			for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet())
			{
				HashMap<String, String> propSet = entry.getValue();
				
			    int thisCursorOrder = Integer.valueOf(propSet.get(CURSOR_ORDER));
			    if (thisCursorOrder < 0)
			    	continue;
			    		    	    
				String fieldName = entry.getKey();
				String dataType = propSet.get(DATA_TYPE);
				
				if (MetrixStringHelper.valueIsEqual(dataType, DATA_TYPE_NUMBER)) {
					String currencyColumnName = propSet.get(CURRENCY_COLUMN_NAME);
					if (MetrixStringHelper.isNullOrEmpty(currencyColumnName)) {
						row.put(fieldName, MetrixFloatHelper.convertNumericFromDBToUI(cursor.getString(thisCursorOrder)));
					} else {
						String tableName = propSet.get(TABLE_NAME);
						HashMap<String, String> currencyPropSet = fieldSet.get(String.format("%1$s.%2$s", tableName, currencyColumnName));
						int currencyCursorOrder = Integer.valueOf(currencyPropSet.get(CURSOR_ORDER));
						
						DecimalFormat formatter = MetrixFloatHelper.getCurrencyFormatter();
						
						try {
							formatter.setCurrency(Currency.getInstance(cursor.getString(currencyCursorOrder)));
						} catch (Exception ex) {
							formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
						}
						String numberToFormat = MetrixFloatHelper.convertNumericFromDBToUI(cursor.getString(thisCursorOrder));
						if(!MetrixStringHelper.isNullOrEmpty(numberToFormat))
							row.put(fieldName, formatter.format(MetrixFloatHelper.convertNumericFromUIToNumber(numberToFormat).doubleValue()));
					}
				} else if (MetrixStringHelper.valueIsEqual(dataType, DATA_TYPE_DATE)) {
					row.put(fieldName, MetrixDateTimeHelper.convertDateTimeFromDBToUIDateOnly(cursor.getString(thisCursorOrder)));
				} else if (MetrixStringHelper.valueIsEqual(dataType, DATA_TYPE_TIME)) {
					row.put(fieldName, MetrixDateTimeHelper.convertDateTimeFromDBToUITimeOnly(cursor.getString(thisCursorOrder)));
				} else if (MetrixStringHelper.valueIsEqual(dataType, DATA_TYPE_DATE_TIME)) {
					row.put(fieldName, MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(thisCursorOrder), MetrixDateTimeHelper.DATE_TIME_FORMAT));
				} else if (MetrixStringHelper.valueIsEqual(dataType, DATA_TYPE_STRING)) {
					row.put(fieldName, cursor.getString(thisCursorOrder));
				}			
			}
		}
		
		return row;
	}

	/**
	 * Where applicable, executes the population script associated with the current list screen.
	 *
	 * @param activity The current activity.
	 * @param table The current table of list data.
	 *
	 * @since 5.7.0
	 */
	public static List<HashMap<String, String>> performScriptListPopulation(Activity activity, List<HashMap<String, String>> table) {
		String screenName = MetrixScreenManager.getScreenName(activity);
		table = performScriptListPopulation(activity, MetrixScreenManager.getScreenId(screenName), screenName, table);
		return table;
	}

	/**
	 * Where applicable, executes the population script associated with the current list screen.
	 *
	 * @param activity The current activity.
	 * @param screenId The screen ID to use.
	 * @param screenName The screen name to use.
	 * @param table The current table of list data.
	 *
	 * @since 5.7.0
	 */
	public static List<HashMap<String, String>> performScriptListPopulation(Activity activity, int screenId, String screenName, List<HashMap<String, String>> table) {
		ClientScriptDef populateScript = MetrixScreenManager.getScriptDefForScreenPopulation(screenId);
		if (populateScript != null && table.size() > 0) {
			String dataCacheName = String.format("%s__CurrentListData", screenName);
			MetrixPublicCache.instance.addItem(dataCacheName, table);
			MetrixClientScriptManager.mListPopulationScreenName = screenName;
			Object resultTable = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(activity), populateScript);
			if (resultTable != null && resultTable instanceof ArrayList)
				table = (ArrayList<HashMap<String, String>>)resultTable;
			MetrixPublicCache.instance.removeItem(dataCacheName);
		}
		return table;
	}

	/**
	 * Provides the name ("[table_name].[column_name]") of the first field 
	 * displayed in this activity's list.
	 * 
	 * @param activity The current activity.
	 * 
	 * @since 5.6.2
	 */
	public static String getFirstDisplayedFieldName(Activity activity) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return getFirstDisplayedFieldName(screenId);
	}

	/**
	 * Provides the name ("[table_name].[column_name]") of the first field 
	 * displayed in this activity's list.
	 * 
	 * @param screenId The current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static String getFirstDisplayedFieldName(int screenId) {
		String fieldName = "";
		String whereClause = String.format("screen_id = %s and display_order > 0 order by display_order asc", screenId);
		String tableName = MetrixDatabaseManager.getFieldStringValue("use_mm_field", "lower(table_name)", whereClause);
		String columnName = MetrixDatabaseManager.getFieldStringValue("use_mm_field", "lower(column_name)", whereClause);
		if (!(MetrixStringHelper.isNullOrEmpty(tableName) && !(MetrixStringHelper.isNullOrEmpty(columnName)))) {
			fieldName = String.format("%1$s.%2$s", tableName, columnName);
		}
		
		return fieldName;
	}
	
	/**
	 * Provides the alias used by the join table within the generated query for the field specified.
	 * 
	 * @param activity The current activity.
	 * @param fieldName The unique identifier of the field: "[table_name].[column_name]".
	 * 
	 * @since 5.6.2
	 */
	public static String getJoinTableAliasForFieldName(Activity activity, String fieldName) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return getJoinTableAliasForFieldName(fieldName, screenId);
	}
	
	/**
	 * Provides the alias used by the join table within the generated query for the field specified.
	 * 
	 * @param fieldName The unique identifier of the field: "[table_name].[column_name]".
	 * @param screenId The current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static String getJoinTableAliasForFieldName(String fieldName, int screenId) {
		String tableAlias = "";
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
		if (fieldSet.size() > 0) {
			HashMap<String, String> propSet = fieldSet.get(fieldName);
			if (propSet != null && propSet.containsKey(TABLE_ALIAS)) {
				tableAlias = propSet.get(TABLE_ALIAS);
			}
		}
		
		return tableAlias;
	}
	
	/**
	 * Ensures that the table of data provided is ordered by the value found in the field specified.
	 * This method uses a non-case-sensitive string ordering.
	 * 
	 * @param table The original table of data.
	 * @param fieldName The field name ("[table_name].[column_name]") to use for value sorting.
	 * 
	 * @since 5.6.2
	 */
	public static List<HashMap<String, String>> orderTableByHeadingField(List<HashMap<String, String>> table, final String fieldName) {		
		Collections.sort(table, new Comparator<HashMap<String, String>>() {
			public int compare(HashMap<String, String> item1, HashMap<String, String> item2) {
				String item1Value = item1.get(fieldName);
				String item2Value = item2.get(fieldName);		
				if (item1Value == null) { item1Value = ""; }
				if (item2Value == null) { item2Value = ""; }
				return item1Value.compareToIgnoreCase(item2Value);
			}
		});
		
		return table;
	}

	public static void populateAttachmentListItemView(AttachmentMetadataViewHolder holder, HashMap<String, String> dataRow, int screenId) {
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
		HashMap<Integer, Integer> orderMap = getDisplayOrderToActualOrderMap(screenId);

		if (fieldSet.size() > 0) {
			for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet()) {
				String fieldName = entry.getKey();
				HashMap<String, String> propSet = entry.getValue();

				String controlType = propSet.get(CONTROL_TYPE);
				String isCurrencyColumn = propSet.get(IS_CURRENCY_COLUMN);
				if (!MetrixStringHelper.isNullOrEmpty(isCurrencyColumn) && MetrixStringHelper.valueIsEqual(isCurrencyColumn, "Y") ) {
					// don't attempt to populate a column that we retrieved JUST for currency purposes
					continue;
				}

				boolean useVisibleItemSet = true;
				int thisDisplayOrder = Integer.valueOf(propSet.get(DISPLAY_ORDER));
				if (thisDisplayOrder < 0)
					useVisibleItemSet = false;

				View itemToUpdate;
				TextView textView;
				if (useVisibleItemSet) {
					int visibleArrayOrder = orderMap.get(thisDisplayOrder) - 1;
					itemToUpdate = holder.mVisibleViews.get(visibleArrayOrder).get();
				} else {
					itemToUpdate = holder.mInvisibleViews.get(fieldName).get();
				}

				String retrievedData = dataRow.get(fieldName);
				if ((controlType.compareToIgnoreCase(TEXT) == 0) && itemToUpdate instanceof TextView) {
					textView = (TextView) itemToUpdate;
					textView.setText(retrievedData);
				}
			}
		}
	}

	/**
	 * Sets values on all controls of a MetadataViewHolder, based on relevant list item field metadata
	 * and the current dataRow in context.  Data should already be fully formatted for display.
	 * 
	 * @param screenType The screen_type defined in screen metadata.
	 * @param activity The current activity.
	 * @param holder The current MetadataViewHolder in context.
	 * @param dataRow The current row of data in context.
	 * @param fieldNameForCheckboxOrImage The (table_name.column_name) mapped to the 
	 * checkbox/image on the list item.
	 * @param defaultImageResourceID The resource ID for the image to use if no data-based image can be found.
	 * @param fieldValueMapForImageLookup For a screenType that uses images, if this parameter is defined, 
	 * the app will use the current value on fieldNameForCheckboxOrImage in combination with this map 
	 * to determine what bitmap to apply. If this screenType uses images and this parameter is not defined,
	 * the app will operate on the assumption that fieldNameForCheckboxOrImage itself specifies an image_id.
	 * 
	 * @since 5.6.2
	 */
 	public static void populateListItemView(String screenType, Activity activity, MetadataViewHolder holder, 
			HashMap<String, String> dataRow, String fieldNameForCheckboxOrImage, 
			int defaultImageResourceID, HashMap<String, Integer> fieldValueMapForImageLookup) {
 		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		populateListItemView(screenType, activity, holder, dataRow, fieldNameForCheckboxOrImage, defaultImageResourceID,
				fieldValueMapForImageLookup, screenId);
 	}
	
	/**
	 * Sets values on all controls of a MetadataViewHolder, based on relevant list item field metadata
	 * and the current dataRow in context.  Data should already be fully formatted for display.
	 * 
	 * @param screenType The screen_type defined in screen metadata.
	 * @param screenId The current screen id.
	 * @param holder The current MetadataViewHolder in context.
	 * @param dataRow The current row of data in context.
	 * @param fieldNameForCheckboxOrImage The (table_name.column_name) mapped to the 
	 * checkbox/image on the list item.
	 * @param defaultImageResourceID The resource ID for the image to use if no data-based image can be found.
	 * @param fieldValueMapForImageLookup For a screenType that uses images, if this parameter is defined, 
	 * the app will use the current value on fieldNameForCheckboxOrImage in combination with this map 
	 * to determine what bitmap to apply. If this screenType uses images and this parameter is not defined,
	 * the app will operate on the assumption that fieldNameForCheckboxOrImage itself specifies an image_id.
	 * 
	 * @since 5.6.3
	 */
 	public static void populateListItemView(String screenType, Activity activity, MetadataViewHolder holder,
			HashMap<String, String> dataRow, String fieldNameForCheckboxOrImage, 
			int defaultImageResourceID, HashMap<String, Integer> fieldValueMapForImageLookup, int screenId) {
		HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
		HashMap<Integer, Integer> orderMap = getDisplayOrderToActualOrderMap(screenId);

		if (screenHasSliver(screenId)) {
			if (holder.mSliverView != null) {
				final View sliver = holder.mSliverView.get();
				if (sliver != null) {
					sliver.setVisibility(View.VISIBLE);
					final int color = translateRGBString(dataRow.get(SLIVER_COLUMN));
					sliver.setBackgroundColor(color);
				}
			}
		}

		if (fieldSet.size() > 0) {
			for (Map.Entry<String, HashMap<String, String>> entry : fieldSet.entrySet())
			{
				String fieldName = entry.getKey();
				HashMap<String, String> propSet = entry.getValue();

				String controlType = propSet.get(CONTROL_TYPE);
				String controlEvent = propSet.get(CONTROL_EVENT);
				String isCurrencyColumn = propSet.get(IS_CURRENCY_COLUMN);
				if (!MetrixStringHelper.isNullOrEmpty(isCurrencyColumn) 
					&& MetrixStringHelper.valueIsEqual(isCurrencyColumn, "Y") ) {
					// don't attempt to populate a column that we retrieved JUST for currency purposes
					continue;
				}
				
				boolean useVisibleItemSet = true;
			    int thisDisplayOrder = Integer.valueOf(propSet.get(DISPLAY_ORDER));
			    if (thisDisplayOrder < 0)
			    	useVisibleItemSet = false;
			    
			    View itemToUpdate;
				TextView textView;
				MetrixHyperlink hyperlink;
			    if (useVisibleItemSet) {
			    	int visibleArrayOrder = orderMap.get(thisDisplayOrder) - 1;
			    	itemToUpdate = holder.mVisibleViews.get(visibleArrayOrder).get();
			    } else {
			    	itemToUpdate = holder.mInvisibleViews.get(fieldName).get();
			    }

			    String retrievedData = dataRow.get(fieldName);

				if((controlType.compareToIgnoreCase(TEXT) == 0) && itemToUpdate instanceof TextView) {
					textView = (TextView) itemToUpdate;
					textView.setText(retrievedData);
				}
				else if ((controlType.compareToIgnoreCase(HYPERLINK) == 0) && itemToUpdate instanceof MetrixHyperlink) {
					hyperlink = (MetrixHyperlink) itemToUpdate;
					hyperlink.setLinkText(retrievedData);
				}

				if (!MetrixStringHelper.isNullOrEmpty(controlEvent)) {
					ViewGroup currentListRowContainer = getParentView(itemToUpdate);
					MetrixControlAssistant.setUpControlEvent(activity, itemToUpdate, controlEvent, currentListRowContainer);
				}
		    	
		    	if (MetrixStringHelper.valueIsEqual(fieldName, fieldNameForCheckboxOrImage)) {
			    	if (MetrixStringHelper.valueIsEqual(screenType, "LIST_IMAGE")) {
			    		ImageView tempImage = holder.mImage.get();
			    		Bitmap defaultImage = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), defaultImageResourceID);
			    		
			    		if (fieldValueMapForImageLookup != null && fieldValueMapForImageLookup.size() > 0) {
			    			// attempt to use the map to get the proper image, based on retrievedData
			    			boolean matchFound = false;
			    			for (Map.Entry<String, Integer> imageItem : fieldValueMapForImageLookup.entrySet()) {
			    				if (MetrixStringHelper.valueIsEqual(retrievedData, imageItem.getKey())) {
			    					int imageResourceID = imageItem.getValue();
			    					Bitmap imageToUse = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), imageResourceID);
			    					if (imageToUse != null) {
			    						tempImage.setImageBitmap(imageToUse);
				    					matchFound = true;
			    					}
			    					break;
			    				}
			    			}
			    			
			    			if (!matchFound) {
			    				// use default Bitmap, if possible (otherwise blank it out)
			    				if (defaultImage != null) {
			    					tempImage.setImageBitmap(defaultImage);
			    				} else {
			    					tempImage.setImageResource(android.R.color.transparent);
			    				}
			    			}		    			
			    		} else {
			    			// treat retrievedData as an image_id
			    			if (!MetrixAttachmentHelper.applyImageWithPixelScale(retrievedData, tempImage, 
			    					tempImage.getHeight(), tempImage.getWidth())) {
			    				// if it fails, use default Bitmap, if possible (otherwise blank it out)
			    				if (defaultImage != null) {
			    					tempImage.setImageBitmap(defaultImage);
			    				} else {
			    					tempImage.setImageResource(android.R.color.transparent);
			    				}
			    			}
			    		}
			    	} else if (MetrixStringHelper.valueIsEqual(screenType, "LIST_CHECKBOX")
			    			&& !dataRow.containsKey("checkboxState")) {
			    		// use fieldNameForCheckboxOrImage only to set INITIAL value for checkboxState
			    		// need to allow for user input-based change to checkboxState
			    		if (MetrixStringHelper.valueIsEqual(retrievedData, "Y")) {
			    			dataRow.put("checkboxState", "Y");
			    		} else {
			    			dataRow.put("checkboxState", "N");
			    		}
			    	}
		    	}
			}		
		}
	}

 	/**
 	 * Gets the display order of the screen.
 	 * 
 	 * @param screenId The current screen id.
 	 * @return display order map.
 	 */
	private static HashMap<Integer, Integer> getDisplayOrderToActualOrderMap(int screenId) {

		HashMap<Integer, Integer> orderMap = displayOrderToActualOrderMap.get(screenId);
		if (orderMap == null) {
			orderMap = new HashMap<Integer, Integer>();
			MetrixCursor fieldCursor = null;
			try {
				String query = "SELECT use_mm_field.display_order FROM use_mm_field WHERE use_mm_field.screen_id = " 
						+ screenId + " and use_mm_field.display_order > 0 order by use_mm_field.display_order asc";
				
				fieldCursor = MetrixDatabaseManager.rawQueryMC(query, null);		
				if (fieldCursor == null || !fieldCursor.moveToFirst()) {
					throw new Exception("getDisplayOrderToActualOrderMap: No field metadata found!");
				}
				
				int orderIterator = 1;
				while (fieldCursor.isAfterLast() == false) {
					orderMap.put(fieldCursor.getInt(0), orderIterator);			
					orderIterator++;
					fieldCursor.moveToNext();
				}
				
				displayOrderToActualOrderMap.put(screenId, orderMap);
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				if (fieldCursor != null && (!fieldCursor.isClosed())) {
					fieldCursor.close();
				}
			}
		}
		
		return orderMap;
	}

	private static StringBuilder generateThisJoin(StringBuilder joinChunk, JoinInfo thisInfo, boolean isOuterJoin) {
		// now construct the join ... tableAlias is same as listTableName, if deployTableAlias FALSE
		if (isOuterJoin) {
			joinChunk.append(String.format("left outer join %s ", thisInfo.mListTableName));
		} else {
			joinChunk.append(String.format("join %s ", thisInfo.mListTableName));
		}
						
		if (thisInfo.mDeployTableAlias) {
			joinChunk.append(String.format("%s ", thisInfo.mTableAlias));
		}
		joinChunk.append(String.format("on %1$s = %2$s.%3$s ", thisInfo.mFieldName, thisInfo.mTableAlias, thisInfo.mListValueColumn));
		
		// tack on the filter if both listFilterColumn and listFilterValue are not null
		if (!MetrixStringHelper.isNullOrEmpty(thisInfo.mListFilterColumn) && !MetrixStringHelper.isNullOrEmpty(thisInfo.mListFilterValue)) {
			if (thisInfo.mListFilterColumn.contains("|")) {
				// we have multiple, pipe-delimited columns to filter on
				// there should be a similarly pipe-delimited value set in listFilterValue
				String[] filterColumnSet = thisInfo.mListFilterColumn.split("\\|");
				String[] filterValueSet = thisInfo.mListFilterValue.split("\\|");
				if (filterColumnSet.length > 1 && filterColumnSet.length == filterValueSet.length) {
					for (int i = 0; i < filterColumnSet.length; i++) {
						joinChunk = appendJoinFilter(joinChunk, thisInfo.mTableAlias, filterColumnSet[i], filterValueSet[i]);
					}
				}
			} else {
				joinChunk = appendJoinFilter(joinChunk, thisInfo.mTableAlias, thisInfo.mListFilterColumn, thisInfo.mListFilterValue);
			}
		}	
		
		return joinChunk;
	}
	
	private static StringBuilder appendJoinFilter(StringBuilder joinChunk, String tableAlias, String listFilterColumn, String listFilterValue) {
		if (listFilterValue.contains("!=")) {
			joinChunk.append(String.format("and %1$s.%2$s != ", tableAlias, listFilterColumn));
			listFilterValue = listFilterValue.substring(listFilterValue.indexOf("!=") + 2).trim();
		} else {
			joinChunk.append(String.format("and %1$s.%2$s = ", tableAlias, listFilterColumn));
		}
		
		// attempt to use single quotes intelligently
		if (MetrixStringHelper.isInteger(listFilterValue) || MetrixStringHelper.isDouble(listFilterValue)
				|| (listFilterValue.startsWith("'") && listFilterValue.endsWith("'"))) {
			joinChunk.append(String.format("%s ", listFilterValue));
		} else {
			joinChunk.append(String.format("'%s' ", listFilterValue));
		}
		
		return joinChunk;
	}
	
	private static class JoinInfo {
		String mFieldName;
		String mTableName;
		String mListTableName;
		boolean mDeployTableAlias;
		String mTableAlias;
		String mListValueColumn;
		String mListFilterColumn;
		String mListFilterValue;
		
		JoinInfo(String fieldName, String tableName, String listTableName, boolean deployTableAlias, String tableAlias, String listValueColumn, String listFilterColumn, String listFilterValue) {
			mFieldName = fieldName;
			mTableName = tableName;
			mListTableName = listTableName;
			mDeployTableAlias = deployTableAlias;
			mTableAlias = tableAlias;
			mListValueColumn = listValueColumn;
			mListFilterColumn = listFilterColumn;
			mListFilterValue = listFilterValue;			
		}
	}

	//Tablet UI Optimization

	/***
	 * Detects the different scenarios of lists are getting displayed.
	 * @param activity
	 * @return list panel width
	 */
	private static boolean isLinkedListScreen(Activity activity) {
		boolean status = false;
		if(activity instanceof MetrixBaseActivity){
			MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
			if(metrixBaseActivity.linkedScreenAvailableInTabletUIMode){
				//This will true in places like DebriefLabor (Navigation Panel (0.2) + Standard Panel/Second Panel (0.5) + List Panel/Third Panel (0.3))
				if((!MetrixStringHelper.isNullOrEmpty(metrixBaseActivity.linkedScreenTypeInTabletUIMode) && metrixBaseActivity.linkedScreenTypeInTabletUIMode.toLowerCase().contains("list"))
				//This will true for scenarios like Code-less List screens (Navigation Panel (0.2) + List Panel/Third Panel (0.8) -> Navigation Panel (0.2) + Standard Panel/Second Panel (0.5) + List Panel/Third Panel (0.3))
				|| metrixBaseActivity.showListScreenLinkedScreenInTabletUIMode)
					status = true;
			}
		}
		return status;
	}

	private static ListWidthDetails getListPanelWidthForTabletUI(Activity activity, BigDecimal relativeWidthLimit, int pxScreenWidth, int pxToReserveForPaddings) {
		BigDecimal calculatedRelativeWidthLimit = relativeWidthLimit;
		BigDecimal calculatedListPanelWidth = null;

		if (isLinkedListScreen(activity))
			calculatedListPanelWidth = getListPanelWidth(0.3, pxScreenWidth, pxToReserveForPaddings);
		else {
			calculatedRelativeWidthLimit = new BigDecimal("2");

			calculatedListPanelWidth = getListPanelWidth(0.8, pxScreenWidth, pxToReserveForPaddings);
			calculatedListPanelWidth = calculatedListPanelWidth.divide(new BigDecimal("2"));
		}
		return new ListWidthDetails(calculatedListPanelWidth, calculatedRelativeWidthLimit);
	}

	private static BigDecimal getListPanelWidth(double widthPortion, int pxScreenWidth, int pxToReserveForPaddings) {
		double calculatedPxScreenWidth = pxScreenWidth * widthPortion;
		calculatedPxScreenWidth = calculatedPxScreenWidth - pxToReserveForPaddings;
		return new BigDecimal(String.valueOf((int) calculatedPxScreenWidth));
	}

	//End Tablet UI Optimization

	private static class ListWidthDetails {
		private BigDecimal calPxScreenWidth;
		private BigDecimal calRelativeWidthLimit;

		public ListWidthDetails(BigDecimal calPxScreenWidth, BigDecimal calRelativeWidthLimit){
			this.calPxScreenWidth = calPxScreenWidth;
			this.calRelativeWidthLimit = calRelativeWidthLimit;
		}

		public BigDecimal getCalPxScreenWidth() {
			return calPxScreenWidth;
		}

		public BigDecimal getCalRelativeWidthLimit() {
			return calRelativeWidthLimit;
		}
	}

	private static String getListRowControlIdentifier(String fieldName) {
		String identifier = "";
		String[] tableColumnArr = fieldName.split("\\.");
		if (tableColumnArr != null && tableColumnArr.length > 1)
		{
			String tableName = tableColumnArr[0];
			String columnName = tableColumnArr[1];
			identifier = String.format("%1s__%2s", tableName, columnName);
		}
		else
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("EitherTabNameOrColName"));

		return identifier;
	}

	private static ViewGroup getParentView(View view){
		if(view == null) return null;
		ViewParent parent = view.getParent();
		if(parent == null) return null;

		ViewGroup vgParent = (ViewGroup) parent;
		Object vgpTag = vgParent.getTag();
		if(vgpTag != null && vgpTag instanceof MetadataViewHolder ||
				(vgpTag instanceof String && MetrixStringHelper.valueIsEqual(MetadataRecyclerViewAdapter.SCRIPT_EXECUTABLE, (String)vgpTag)))
			return vgParent;
		else
			return getParentView(vgParent);
	}

	public static void setupVerticalRecyclerView(@NonNull RecyclerView rv, int dividerRes) {
		rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
		if (dividerRes > 0) {
			final DividerItemDecoration id = new DividerItemDecoration(rv.getContext(), DividerItemDecoration.VERTICAL);
			id.setDrawable(ContextCompat.getDrawable(rv.getContext(), dividerRes));
			rv.addItemDecoration(id);
		}
	}

	public static List<Object> indexListData(List<HashMap<String, String>> data, @NonNull String headerFieldName) {
		final List<Object> sortedData = new ArrayList<Object>(MetrixListScreenManager.orderTableByHeadingField(data, headerFieldName));
		final Map<String, Integer> alphaIndex = new LinkedHashMap<>();
		for (int i = 0; i < sortedData.size(); i++) {
			String key = ((HashMap<String, String>) sortedData.get(i)).get(headerFieldName);
			if (!MetrixStringHelper.isNullOrEmpty(key))
				key = key.substring(0, 1).toUpperCase();
			else
				key = "";

			if (!alphaIndex.containsKey(key))
				alphaIndex.put(key, i);
		}

		int indexOffset = 0;
		for (String key : alphaIndex.keySet()) {
			final int position = alphaIndex.get(key);
			sortedData.add(position + indexOffset, key);
			indexOffset++;
		}
		return sortedData;
	}

	public static boolean screenHasSliver(int screenId) {
		if (!sliverScreenCache.containsKey(screenId)) {
			final HashMap<String, HashMap<String, String>> fieldSet = getFieldSetForListScreen(screenId);
			sliverScreenCache.put(screenId, fieldSet.containsKey(SLIVER_COLUMN));
		}
		return sliverScreenCache.get(screenId);
	}

	@ColorInt
	public static int translateRGBString(String rgbText) {
		if (!MetrixStringHelper.isNullOrEmpty(rgbText)) {
			try {
				long color = Long.parseLong(rgbText, 16);
				if (rgbText.length() == 6)
					color |= 0x00000000ff000000; // Set the alpha value
				else if (rgbText.length() != 8)
					return Color.TRANSPARENT; // Unknown colour. Return transparent

				return (int)color;
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			}
		}
		return Color.TRANSPARENT;
	}

	public static void CalculateListScreenFieldWidthForLargeFontScale(List<TextView> textFieldList,
						BigDecimal relativeWidthLimit, BigDecimal pxUsableScreenWidth, int totalFieldWidth){
		if (!textFieldList.isEmpty()){
			int remainingWidth = (relativeWidthLimit.multiply(pxUsableScreenWidth).intValue() - totalFieldWidth)/textFieldList.size();
			for (TextView tv:textFieldList) {
				tv.getLayoutParams().width += remainingWidth;
			}
			textFieldList.clear();
		}
	}

	public static List<HashMap<String, String>> applySearchCriteria(List<HashMap<String, String>> table, String searchCriteria) {
		if (!MetrixStringHelper.isNullOrEmpty(searchCriteria)) {

			List<HashMap<String, String>> searchedTable = new ArrayList<HashMap<String, String>>();

			for (HashMap<String, String> row : table) {
				for (Map.Entry<String, String> entry : row.entrySet()) {

					String value = entry.getValue();

					if (value != null && value.toLowerCase().contains(searchCriteria.toLowerCase())) {
						searchedTable.add( row );
						break;
					}
				}
			}
			table.clear();
			table = searchedTable;
		}
		return table;
	}
}
