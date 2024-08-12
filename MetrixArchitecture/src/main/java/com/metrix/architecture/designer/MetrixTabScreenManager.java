package com.metrix.architecture.designer;

import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.utilities.LogManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetrixTabScreenManager extends MetrixDesignerManager {
    private static Map<Integer, LinkedHashMap<String, TabChildInfo>> tabScreensCache = new HashMap<Integer, LinkedHashMap<String, TabChildInfo>>();

    /**
     * Clear all cached tab screen properties.
     */
    public static void clearTabScreensCache() {
        MetrixTabScreenManager.tabScreensCache.clear();
    }

    /**
     * Returns tab screen properties (for all visible tab children) by screen id.
     * @param screenId
     * @return tab screen properties.
     */
    public static LinkedHashMap<String, TabChildInfo> initTabChildren(int screenId) {
        MetrixCursor cursor = null;
        LinkedHashMap<String, TabChildInfo> currentTabScreen = tabScreensCache.get(screenId);

        if (currentTabScreen == null) {
            currentTabScreen = new LinkedHashMap<String, TabChildInfo>();
            try {
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("SELECT use_mm_screen.screen_name, use_mm_screen.screen_id, use_mm_screen.screen_type, use_mm_screen.primary_table,");
                queryBuilder.append(" use_mm_screen.label, use_mm_screen.refresh_event, use_mm_screen.tab_title, use_mm_screen.tip, use_mm_screen.where_clause_script");
                queryBuilder.append(" FROM use_mm_screen WHERE use_mm_screen.tab_order > 0");
                queryBuilder.append(String.format(" AND use_mm_screen.tab_parent_id = %d", screenId));
                queryBuilder.append(" ORDER BY use_mm_screen.tab_order ASC");

                cursor = MetrixDatabaseManager.rawQueryMC(queryBuilder.toString(), null);
                if (cursor == null || !cursor.moveToFirst()) {
                    throw new Exception(String.format("initTabChildren: No tab child metadata found for (Parent ID:%d)!", screenId));
                }

                int tabIndex = 0;
                while (cursor.isAfterLast() == false) {
                    String screenName = cursor.getString(0);

                    TabChildInfo tci = new TabChildInfo();
                    tci.mScreenId = cursor.getString(1);
                    tci.mScreenType = cursor.getString(2);
                    tci.mPrimaryTable = cursor.getString(3);
                    tci.mLabel = cursor.getString(4);
                    tci.mRefreshScript = cursor.getString(5);
                    tci.mTabTitle = cursor.getString(6);
                    tci.mTip = cursor.getString(7);
                    tci.mWhereClauseScript = cursor.getString(8);
                    tci.mTabIndex = tabIndex;
                    tci.mSearchFieldId = MetrixControlAssistant.generateViewId();

                    tabIndex++;
                    currentTabScreen.put(screenName, tci);
                    cursor.moveToNext();
                }
                tabScreensCache.put(screenId, currentTabScreen);
            } catch (Exception ex) {
                LogManager.getInstance().error(ex);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return currentTabScreen;
    }

    public static class TabChildInfo {
        public int mLayoutId;
        public int mSearchFieldId;
        public int mCount;
        public int mTabIndex;
        public String mPrimaryTable;
        public String mLabel;
        public String mRefreshScript;
        public String mScreenId;
        public String mScreenType;
        public String mTabTitle;
        public String mTip;
        public String mWhereClauseScript;
        public MetadataRecyclerViewAdapter mListAdapter;
        public MetrixFormDef mFormDef;
        public ViewGroup mLayout;
    }
}
