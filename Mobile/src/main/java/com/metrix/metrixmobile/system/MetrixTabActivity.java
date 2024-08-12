package com.metrix.metrixmobile.system;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.ui.widget.MobileUIHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MetrixTabActivity extends MetrixActivity implements TabHost.OnTabChangeListener, MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener{
    protected TabHost mTabHost;
    protected LinkedHashMap<String, MetrixTabScreenManager.TabChildInfo> mTabChildren;
    protected HashMap<String, String> mParentScreenProperties;
    protected int mCurrentTabChildScreenId;
    protected String mCurrentScreenName = null;

    public void onStart() {
        super.onStart();

        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        String actionBarScript = mParentScreenProperties.get("action_bar_script");
        if (actionBarTitle != null && !MetrixStringHelper.isNullOrEmpty(actionBarScript)) {
            ClientScriptDef actionBarScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(actionBarScript);
            Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), actionBarScriptDef);

            String title;
            if (result == null)
                title = "";
            else if (result instanceof String)
                title = (String) result;
            else if (result instanceof Double && (((Double) result) % 1 == 0))
                title = new DecimalFormat("#").format(result);
            else
                title = result.toString();

            actionBarTitle.setText(title);
        }

        this.helpText = mParentScreenProperties.get("help");

        // force a call to the tab change event, so that the default tab's refresh event
        // has a chance to fire on navigation to the tab screen
        onTabChanged("");
    }

    protected void performInitialSetup(int tabParentScreenID) {
        setContentView(R.layout.yycsmd_tab_parent);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        // Use the tab parent screen ID to obtain all relevant screen metadata for visible tab children.
        mParentScreenProperties = MetrixScreenManager.getScreenProperties(tabParentScreenID);
        mTabChildren = MetrixTabScreenManager.initTabChildren(tabParentScreenID);

        ensureFieldResourcesAreSet();
        addTabs();
        MobileUIHelper.alteringTabWidget(mTabHost);
        mTabHost.setOnTabChangedListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putInt("tabState", mTabHost.getCurrentTab());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            mTabHost.setCurrentTab(savedInstanceState.getInt("tabState"));
        }
    }

    protected void addTabs() { }

    protected void ensureFieldResourcesAreSet() {
        MetrixFieldManager.setInflatableLayouts(R.layout.template_layout_textview_checkbox, R.layout.template_layout_textview_textview, R.layout.template_layout_textview_editview, R.layout.template_layout_textview_spinner, R.layout.template_layout_textview_spinnertext, R.layout.template_layout_view_button, R.layout.template_layout_textview_metrixhyperlink,
                R.layout.template_layout_textview_checkbox_textview_checkbox, R.layout.template_layout_textview_checkbox_textview_textview, R.layout.template_layout_textview_checkbox_textview_editview, R.layout.template_layout_textview_checkbox_textview_spinner,R.layout.template_layout_textview_checkbox_textview_spinnertext, R.layout.template_layout_textview_checkbox_view_button, R.layout.template_layout_textview_checkbox_textview_metrixhyperlink,
                R.layout.template_layout_textview_textview_textview_checkbox, R.layout.template_layout_textview_textview_textview_textview, R.layout.template_layout_textview_textview_textview_editview, R.layout.template_layout_textview_textview_textview_spinner, R.layout.template_layout_textview_textview_textview_spinnertext, R.layout.template_layout_textview_textview_view_button, R.layout.template_layout_textview_textview_textview_metrixhyperlink,
                R.layout.template_layout_textview_editview_textview_checkbox, R.layout.template_layout_textview_editview_textview_textview, R.layout.template_layout_textview_editview_textview_editview, R.layout.template_layout_textview_editview_textview_spinner, R.layout.template_layout_textview_editview_textview_spinnertext, R.layout.template_layout_textview_editview_view_button, R.layout.template_layout_textview_editview_textview_metrixhyperlink,
                R.layout.template_layout_textview_spinner_textview_checkbox, R.layout.template_layout_textview_spinner_textview_textview, R.layout.template_layout_textview_spinner_textview_editview, R.layout.template_layout_textview_spinner_textview_spinner, R.layout.template_layout_textview_spinner_textview_spinnertext, R.layout.template_layout_textview_spinner_view_button, R.layout.template_layout_textview_spinner_textview_metrixhyperlink,
                R.layout.template_layout_textview_spinnertext_textview_checkbox, R.layout.template_layout_textview_spinnertext_textview_textview, R.layout.template_layout_textview_spinnertext_textview_editview, R.layout.template_layout_textview_spinnertext_textview_spinner, R.layout.template_layout_textview_spinnertext_textview_spinnertext, R.layout.template_layout_textview_spinnertext_view_button, R.layout.template_layout_textview_spinnertext_textview_metrixhyperlink,
                R.drawable.textview_background, R.layout.template_spinner_readonly_item,
                R.layout.template_layout_view_button_textview_checkbox, R.layout.template_layout_view_button_textview_textview, R.layout.template_layout_view_button_textview_editview, R.layout.template_layout_view_button_textview_spinner, R.layout.template_layout_view_button_textview_spinnertext, R.layout.template_layout_view_button_view_button, R.layout.template_layout_view_button_textview_metrixhyperlink,
                R.layout.template_layout_textview_metrixhyperlink_textview_checkbox, R.layout.template_layout_textview_metrixhyperlink_textview_textview, R.layout.template_layout_textview_metrixhyperlink_textview_editview, R.layout.template_layout_textview_metrixhyperlink_textview_spinner, R.layout.template_layout_textview_metrixhyperlink_textview_spinnertext, R.layout.template_layout_textview_metrixhyperlink_view_button, R.layout.template_layout_textview_metrixhyperlink_textview_metrixhyperlink,
                R.layout.template_layout_standard_map,
                R.layout.template_layout_textview_attachment, R.layout.template_layout_textview_checkbox_textview_attachment, R.layout.template_layout_textview_textview_textview_attachment, R.layout.template_layout_textview_editview_textview_attachment, R.layout.template_layout_textview_spinner_textview_attachment, R.layout.template_layout_textview_spinnertext_textview_attachment, R.layout.template_layout_view_button_textview_attachment, R.layout.template_layout_textview_metrixhyperlink_textview_attachment,
                R.layout.template_layout_textview_attachment_textview_checkbox, R.layout.template_layout_textview_attachment_textview_textview, R.layout.template_layout_textview_attachment_textview_editview, R.layout.template_layout_textview_attachment_textview_spinner, R.layout.template_layout_textview_attachment_textview_spinnertext, R.layout.template_layout_textview_attachment_view_button, R.layout.template_layout_textview_attachment_textview_metrixhyperlink, R.layout.template_layout_textview_attachment_textview_attachment,
                R.layout.template_layout_textview_attachment_textview_signature,
                R.layout.template_layout_textview_signature, R.layout.template_layout_textview_checkbox_textview_signature, R.layout.template_layout_textview_textview_textview_signature, R.layout.template_layout_textview_editview_textview_signature, R.layout.template_layout_textview_spinner_textview_signature, R.layout.template_layout_textview_spinnertext_textview_signature, R.layout.template_layout_view_button_textview_signature, R.layout.template_layout_textview_metrixhyperlink_textview_signature,
                R.layout.template_layout_textview_signature_textview_checkbox, R.layout.template_layout_textview_signature_textview_textview, R.layout.template_layout_textview_signature_textview_editview, R.layout.template_layout_textview_signature_textview_spinner, R.layout.template_layout_textview_signature_textview_spinnertext, R.layout.template_layout_textview_signature_view_button, R.layout.template_layout_textview_signature_textview_metrixhyperlink, R.layout.template_layout_textview_signature_textview_signature,
                R.layout.template_layout_textview_signature_textview_attachment);
    }

    public void onTabChanged(String tabId) {
        boolean stopExecution = false;
        String screenName = mTabHost.getCurrentTabTag();
        MetrixTabScreenManager.TabChildInfo tci = mTabChildren.get(screenName);
        mCurrentScreenName = screenName;
        mCurrentTabChildScreenId = Integer.valueOf(tci.mScreenId);

        ClientScriptDef refreshScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tci.mRefreshScript);
        if (refreshScriptDef != null) {
            MetrixPublicCache.instance.addItem("theCurrentFormDef", tci.mFormDef);
            MetrixPublicCache.instance.addItem("theCurrentLayout", tci.mLayout);

            Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), refreshScriptDef);
            if (result instanceof String) {
                String strResult = (String)result;
                stopExecution = (strResult == "STOP_EXECUTION");
            }
        }
        if (stopExecution)
            return;

        // potentially, some other tab change action could go here that stopExecution would skip
    }

    protected void setupCodelessStandardTabChild(LinearLayout thisChildLayout, MetrixTabScreenManager.TabChildInfo tci) {
        LinearLayout innerLayout = (LinearLayout)thisChildLayout.findViewById(R.id.tab_child_layout);

        MetrixTableDef metrixTableDef = new MetrixTableDef(tci.mPrimaryTable, MetrixTransactionTypes.SELECT);

        ClientScriptDef whereScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tci.mWhereClauseScript);
        String whereClause = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(this), whereScriptDef);
        String metrixRowID = MetrixDatabaseManager.getFieldStringValue(tci.mPrimaryTable, "metrix_row_id", whereClause);

        metrixTableDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, metrixRowID, double.class));
        MetrixFormDef thisChildFormDef = new MetrixFormDef(metrixTableDef);
        tci.mFormDef = thisChildFormDef;

        final int screenId = Integer.valueOf(tci.mScreenId);
        MetrixFieldManager.addFieldsToScreen(this, innerLayout, thisChildFormDef, screenId);
        MetrixFormManager.setupForm(this, innerLayout, thisChildFormDef);
        loadMapIfNeeded(screenId, innerLayout, true);
    }

    protected void setupCodelessListTabChild(LinearLayout thisChildLayout, MetrixTabScreenManager.TabChildInfo tci, String screenName, String maxRows, String searchCriteria) {
        final int thisChildScreenID = Integer.valueOf(tci.mScreenId);
        ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tci.mWhereClauseScript);

        RecyclerView recyclerView = thisChildLayout.findViewById(R.id.tab_child_recyclerview);
        if (recyclerView == null) {
            return;
        }

        MetrixPublicCache.instance.addItem("theCurrentFormDef", null);
        MetrixPublicCache.instance.addItem("theCurrentLayout", thisChildLayout);

        String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(this), clientScriptDef);
        String query = MetrixListScreenManager.generateListQuery(tci.mPrimaryTable, result, searchCriteria, thisChildScreenID);

        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query = query + " limit " + maxRows;
        }

        int listCount = 0;
        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, thisChildScreenID);
                table.add(row);
                listCount++;
                cursor.moveToNext();
            }
            cursor.close();

            table = MetrixListScreenManager.performScriptListPopulation(this, thisChildScreenID, screenName, table);

            if (tci.mListAdapter == null) {
                //overloaded constructor to cater to cater code-less screens
                tci.mListAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null,
                        0, R.id.sliver, null, thisChildScreenID,
                        tci.mPrimaryTable.toLowerCase() + ".metrix_row_id", null);

                tci.mListAdapter.setClickListener(this);
                recyclerView.setAdapter(tci.mListAdapter);
            }
            else{
                tci.mListAdapter.updateData(table);
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setAdapter(tci.mListAdapter);
                }
            }
        }
        tci.mCount = listCount;
    }

    protected void setupMetadataCodelessListTabChild(RecyclerView recyclerView, LinearLayout thisChildLayout, MetrixTabScreenManager.TabChildInfo tci, String screenName, String maxRows, String searchCriteria) {
        final int thisChildScreenID = Integer.valueOf(tci.mScreenId);
        ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tci.mWhereClauseScript);

        MetrixPublicCache.instance.addItem("theCurrentFormDef", null);
        MetrixPublicCache.instance.addItem("theCurrentLayout", thisChildLayout);

        String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(this), clientScriptDef);
        String query = MetrixListScreenManager.generateListQuery(tci.mPrimaryTable, result, searchCriteria, thisChildScreenID);

        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query = query + " limit " + maxRows;
        }

        int listCount = 0;
        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, thisChildScreenID);
                table.add(row);
                listCount++;
                cursor.moveToNext();
            }
            cursor.close();

            table = MetrixListScreenManager.performScriptListPopulation(this, thisChildScreenID, screenName, table);

            if (tci.mListAdapter == null) {
                //overloaded constructor to cater for code-less screens
                tci.mListAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null,
                        0, R.id.sliver, null, thisChildScreenID,
                        tci.mPrimaryTable.toLowerCase() + ".metrix_row_id", null);

                tci.mListAdapter.setClickListener(this);
                recyclerView.setAdapter(tci.mListAdapter);
            }
            else{
                tci.mListAdapter.updateData(table);
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setAdapter(tci.mListAdapter);
                }
            }
        }
        tci.mCount = listCount;
    }

    @Override
    public void onItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        MetrixTabScreenManager.TabChildInfo tci = mTabChildren.get(mCurrentScreenName);
        if (tci != null && tci.mLayout.findViewById(tci.mSearchFieldId) != null) {
            MetrixAutoCompleteHelper.saveAutoCompleteFilter(tci.mScreenId, tci.mLayout.findViewById(tci.mSearchFieldId));
        }
        scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this));
    }

    protected String GetTabLabel(MetrixTabScreenManager.TabChildInfo tci) {
        // now, add tabs to the tab host
        String indicatorString = "";
        if (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD") || tci.mCount < 1)
            indicatorString = tci.mTabTitle;
        else
            indicatorString = String.format("%1$s (%2$s)", tci.mTabTitle, String.valueOf(tci.mCount));

        return indicatorString;
    }

    public static class TabSearchTextWatcher implements TextWatcher {
        private final Handler handler = new Handler();
        private final SearchTextChangedCallback onSearchTextChangedCallback;
        public TabSearchTextWatcher(SearchTextChangedCallback onSearchTextChangedCallback) {
            this.onSearchTextChangedCallback = onSearchTextChangedCallback;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(() -> {
                onSearchTextChangedCallback.onSearchTextChanged(s.toString());
            }, 500);
        }

        public interface SearchTextChangedCallback {
            void onSearchTextChanged(String searchText);
        }
    }
}
