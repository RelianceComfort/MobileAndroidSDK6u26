package com.metrix.architecture.designer;

import android.app.Activity;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class MetrixFilterSortManager extends MetrixDesignerManager {
    // key = screen_id
    // value = Ordered List of FilterSortItems
    private static HashMap<Integer, ArrayList<FilterSortItem>> screenFilterCache = new HashMap<Integer, ArrayList<FilterSortItem>>();
    private static HashMap<Integer, ArrayList<FilterSortItem>> screenSortCache = new HashMap<Integer, ArrayList<FilterSortItem>>();

    public static void clearFilterSortCaches() {
        clearLastSelectedFilterSorts();
        screenFilterCache.clear();
        screenSortCache.clear();
    }

    public static ArrayList<FilterSortItem> getFilterItems(int screenId) {
        ArrayList<FilterSortItem> filterItems = new ArrayList<FilterSortItem>();
        if (screenFilterCache.containsKey(screenId))
            filterItems = screenFilterCache.get(screenId);
        else {
            filterItems = getFilterSortList(screenId, "FILTER");
            screenFilterCache.put(screenId, filterItems);
        }
        return filterItems;
    }

    public static ArrayList<FilterSortItem> getSortItems(int screenId) {
        ArrayList<FilterSortItem> sortItems = new ArrayList<FilterSortItem>();
        if (screenSortCache.containsKey(screenId))
            sortItems = screenSortCache.get(screenId);
        else {
            sortItems = getFilterSortList(screenId, "SORT");
            screenSortCache.put(screenId, sortItems);
        }
        return sortItems;
    }

    public static String getDefaultFilterItemLabel(int screenId) {
        ArrayList<FilterSortItem> fsiList = getFilterItems(screenId);
        if (fsiList != null && !fsiList.isEmpty()) {
            for (FilterSortItem fsi : fsiList) {
                if (fsi.Default)
                    return fsi.Label;
            }
        }
        return "";
    }

    public static String getDefaultSortItemLabel(int screenId) {
        ArrayList<FilterSortItem> fsiList = getSortItems(screenId);
        if (fsiList != null && !fsiList.isEmpty()) {
            for (FilterSortItem fsi : fsiList) {
                if (fsi.Default)
                    return fsi.Label;
            }
        }
        return "";
    }

    public static FilterSortItem getFilterSortItemByLabel(String labelValue, ArrayList<FilterSortItem> fsiList) {
        if (fsiList != null && !fsiList.isEmpty()) {
            for (FilterSortItem fsi : fsiList) {
                if (MetrixStringHelper.valueIsEqual(fsi.Label, labelValue))
                    return fsi;
            }
        }
        return null;
    }

    public static String getFilterLabelByItemName(String itemNameValue, int screenId) {
        ArrayList<FilterSortItem> fsiList = getFilterItems(screenId);
        if (fsiList != null && !fsiList.isEmpty()) {
            for (FilterSortItem fsi : fsiList) {
                if (MetrixStringHelper.valueIsEqual(fsi.ItemName, itemNameValue))
                    return fsi.Label;
            }
        }
        return "";
    }

    public static String getSelectedFilterItemSettingName(String screenName) {
        return String.format("%1$s_LAST_FILTER_NAME", screenName);
    }

    public static String getSelectedSortItemSettingName(String screenName) {
        return String.format("%1$s_LAST_SORT_NAME", screenName);
    }

    public static String getSortOrderSettingName(String screenName) {
        return String.format("%1$s_LAST_SORT_ORDER", screenName);
    }

    public static boolean hasFilterItems(int screenId) {
        ArrayList<FilterSortItem> filterList = getFilterItems(screenId);
        if (filterList != null && !filterList.isEmpty())
            return true;
        else
            return false;
    }

    public static boolean hasSortItems(int screenId) {
        ArrayList<FilterSortItem> sortList = getSortItems(screenId);
        if (sortList != null && !sortList.isEmpty())
            return true;
        else
            return false;
    }

    public static String resolveFilterSortContent(Activity activity, String content) {
        ClientScriptDef scriptDef = null;
        if (content.contains("__"))     // a script identifier will always have a double-underscore
            scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(content);

        if (scriptDef != null) {
            return MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(activity), scriptDef);
        }
        return content;
    }

    private static void clearLastSelectedFilterSorts() {
        try {
            ArrayList<Hashtable<String, String>> valuesList = MetrixDatabaseManager.getFieldStringValuesList("select use_mm_screen.screen_name from use_mm_screen where screen_id in (select screen_id from use_mm_filter_sort_item where display_order > 0)");
            if (valuesList != null && !valuesList.isEmpty()) {
                for (Hashtable<String, String> screen : valuesList) {
                    String screenName = screen.get("screen_name");
                    SettingsHelper.removeSetting(getSelectedFilterItemSettingName(screenName));
                    SettingsHelper.removeSetting(getSelectedSortItemSettingName(screenName));
                    SettingsHelper.removeSetting(getSortOrderSettingName(screenName));
                }
            }
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
        }
    }

    private static ArrayList<FilterSortItem> getFilterSortList(int screenId, String itemType) {
        ArrayList<FilterSortItem> fsiList = new ArrayList<FilterSortItem>();
        MetrixCursor filterSortCursor = null;

        try {
            String query = "SELECT use_mm_filter_sort_item.item_id, use_mm_filter_sort_item.item_name, use_mm_filter_sort_item.label,"
                    + " use_mm_filter_sort_item.content, use_mm_filter_sort_item.is_default, use_mm_filter_sort_item.full_filter"
                    + " FROM use_mm_filter_sort_item WHERE use_mm_filter_sort_item.screen_id = " + screenId + " and use_mm_filter_sort_item.item_type = '" + itemType + "'"
                    + " and use_mm_filter_sort_item.display_order > 0 and use_mm_filter_sort_item.label is not null and use_mm_filter_sort_item.label != ''"
                    + " ORDER BY use_mm_filter_sort_item.display_order ASC";

            filterSortCursor = MetrixDatabaseManager.rawQueryMC(query, null);
            if (filterSortCursor != null && filterSortCursor.moveToFirst()) {
                while (filterSortCursor.isAfterLast() == false) {
                    FilterSortItem thisItem = new FilterSortItem();
                    String itemID = filterSortCursor.getString(0);
                    String itemName = filterSortCursor.getString(1);
                    String label = filterSortCursor.getString(2);
                    String content = filterSortCursor.getString(3);
                    String isDefault = filterSortCursor.getString(4);
                    String fullFilter = filterSortCursor.getString(5);

                    thisItem.ItemID = Integer.valueOf(itemID);
                    thisItem.ItemName = itemName;
                    thisItem.Label = label;
                    thisItem.Content = content;
                    thisItem.Default = MetrixStringHelper.valueIsEqual(isDefault, "Y");
                    thisItem.FullFilter = MetrixStringHelper.valueIsEqual(fullFilter, "Y");

                    fsiList.add(thisItem);

                    filterSortCursor.moveToNext();
                }
            }
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
        } finally {
            if (filterSortCursor != null && (!filterSortCursor.isClosed())) {
                filterSortCursor.close();
            }
        }

        return fsiList;
    }
}