package com.metrix.architecture.utilities;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;

public class MetrixAutoCompleteHelper {

	/***
	 * Save an filter that was used to select a row from a list of values so
	 * that the filter can be presented to the user last as an option.
	 * 
	 * @param filterName
	 *            The name of the filter that this value should be saved for.
	 * @param filterValue
	 *            The value that was used to select a value.
	 */
	public static void saveAutoCompleteFilter(String filterName, AutoCompleteTextView autoCompleteTextView) {
		if (MetrixStringHelper.isNullOrEmpty(filterName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFilternameParamIsReq"));
		}

		String filterValue = null;
		try {
			filterValue = MetrixControlAssistant.getValue(autoCompleteTextView);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		
		if (MetrixStringHelper.isNullOrEmpty(filterValue)) {
			return;
		}

		String usageCount = MetrixDatabaseManager.getFieldStringValue("mm_filter_history", "usage_count", "filter_name='" + filterName + "' and filter_value='" + filterValue + "'");

		if (MetrixStringHelper.isNullOrEmpty(usageCount)) {
			MetrixDatabaseManager.executeSql("insert into mm_filter_history(filter_name, filter_value, usage_count) values ('" + filterName + "', '" + filterValue + "', 1)");
		} else {
			MetrixDatabaseManager.executeSql("update mm_filter_history set usage_count = " + (Integer.parseInt(usageCount) + 1) + " where filter_name = '" + filterName + "' and filter_value ='" + filterValue + "'");
		}
	}

	/***
	 * Populate an auto complete text view with options that the user previously
	 * used to find rows that they selected from the specified list.
	 * 
	 * @param filterName
	 *            The name of the filter to populate with previously used
	 *            values.
	 * @param autoCompleteTextView
	 *            The auto complete text view which should be populated.
	 * @param activity
	 *            The activity hosting the auto complete text view.
	 */
	public static void populateAutoCompleteTextView(String filterName, AutoCompleteTextView autoCompleteTextView, Context activity) {
		ArrayList<Hashtable<String, String>> filterValues = MetrixDatabaseManager.getFieldStringValuesList("select filter_value from mm_filter_history where filter_name = '" + filterName + "' order by usage_count desc");
		if (filterValues != null) {
			String[] suggestions = new String[filterValues.size()];
			int i = 0;

			for (Hashtable<String, String> filterValue : filterValues) {
				suggestions[i] = filterValue.get("filter_value");
				i = i + 1;
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_dropdown_item_1line, suggestions);
			autoCompleteTextView.setAdapter(adapter);
		}
	}
}