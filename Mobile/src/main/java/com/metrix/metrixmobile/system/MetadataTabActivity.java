package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataTabActivity extends MetrixTabActivity {
    private String _maxRows;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _maxRows = MetrixDatabaseManager.getAppParam("MAX_ROWS");
        performInitialSetup(codeLessScreenId);
    }

    @Override
    protected void addTabs() {
        try {
            FrameLayout tabChildFrame = (FrameLayout)findViewById(android.R.id.tabcontent);
            for (Map.Entry<String, MetrixTabScreenManager.TabChildInfo> childTab : mTabChildren.entrySet()) {
                String screenName = childTab.getKey();
                MetrixTabScreenManager.TabChildInfo tci = childTab.getValue();

                int layoutResourceID = (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD")) ? R.layout.yycsmd_tab_child_standard : R.layout.yycsmd_tab_child_list;
                LinearLayout thisChildLayout = MetrixControlAssistant.addLinearLayoutWithoutChildIDs(this, layoutResourceID, tabChildFrame);
                tci.mLayoutId = thisChildLayout.getId();

                TextView tvLabel = (TextView)thisChildLayout.findViewById(R.id.tab_child_label);
                TextView tvTip = (TextView)thisChildLayout.findViewById(R.id.tab_child_tip);
                tvLabel.setText(tci.mLabel);
                tvTip.setText(tci.mTip);

                if (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD")) {
                    // set up a form def-based layout
                    setupCodelessStandardTabChild(thisChildLayout, tci);
                } else {

                    // Set up search field
                    AutoCompleteTextView autoCompleteTextView = thisChildLayout.findViewById(R.id.tab_child_search_criteria);
                    autoCompleteTextView.setId(tci.mSearchFieldId);
                    resourceStrings.add(new ResourceValueObject(tci.mSearchFieldId, "Search", true));
                    MetrixAutoCompleteHelper.populateAutoCompleteTextView(tci.mScreenId, autoCompleteTextView, this);
                    autoCompleteTextView.addTextChangedListener(new TabSearchTextWatcher(searchText -> searchMetadataCodelessListTabChild(tci, screenName, _maxRows, searchText)));

                    // set up a recyclerview-based layout
                    final RecyclerView recyclerView = thisChildLayout.findViewById(R.id.tab_child_recyclerview);
                    MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

                    setupMetadataCodelessListTabChild(recyclerView, thisChildLayout, tci, screenName, _maxRows, null);

                    if (tci.mCount < 2)
                        autoCompleteTextView.setVisibility(View.GONE);
                    else
                        autoCompleteTextView.setVisibility(View.VISIBLE);
                }
                tci.mLayout = thisChildLayout;

                setSkinBasedColorsOnRelevantControls(thisChildLayout, MetrixSkinManager.getPrimaryColor(), MetrixSkinManager.getSecondaryColor(), MetrixSkinManager.getHyperlinkColor(), thisChildLayout, true);

                // now, add tabs to the tab host
                String indicatorString = "";
                if (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD") || tci.mCount < 1)
                    indicatorString = tci.mTabTitle;
                else
                    indicatorString = String.format("%1$s (%2$s)", tci.mTabTitle, String.valueOf(tci.mCount));

                TabHost.TabSpec specThisChild = mTabHost.newTabSpec(screenName);
                specThisChild.setContent(tci.mLayoutId);
                specThisChild.setIndicator(indicatorString);

                mTabHost.addTab(specThisChild);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void searchMetadataCodelessListTabChild(MetrixTabScreenManager.TabChildInfo tci, String screenName, String maxRows, String searchCriteria) {
        final int thisChildScreenID = Integer.valueOf(tci.mScreenId);
        if (tci.mListAdapter == null) {
            return;
        }
        ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tci.mWhereClauseScript);

        String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(this), clientScriptDef);
        String query = MetrixListScreenManager.generateListQuery(tci.mPrimaryTable, result, searchCriteria, thisChildScreenID);

        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query = query + " limit " + maxRows;
        }

        LinearLayout thisChildLayout = (LinearLayout) tci.mLayout;
        final RecyclerView recyclerView = thisChildLayout.findViewById(R.id.tab_child_recyclerview);

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

            tci.mListAdapter.updateData(table);
        } else {
            tci.mListAdapter.updateData(new ArrayList<HashMap<String, String>>());
        }
        if (recyclerView.getAdapter() == null) {
            recyclerView.setAdapter(tci.mListAdapter);
        }
    }

}
