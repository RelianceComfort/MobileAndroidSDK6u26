package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MetadataListActivity extends MetrixActivity implements View.OnClickListener, MetrixRecyclerViewListener, TextWatcher {
    private RecyclerView recyclerView;
    private MetadataRecyclerViewAdapter mAdapter;
    private AutoCompleteTextView mSearchCriteria;
    private HashMap<String, String> currentScreenProperties;
    private String primaryTable, whereClauseScript, linkedScreenId;
    private boolean gotHereFromLinkedScreen, searchable, shouldRenderContextMenu;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yycsmd_list_activity);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

        gotHereFromLinkedScreen = (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"));
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        mHandler = new Handler();

        try {
            currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);
            if (currentScreenProperties != null) {
                if (currentScreenProperties.containsKey("primary_table")) {
                    primaryTable = currentScreenProperties.get("primary_table");
                    if (MetrixStringHelper.isNullOrEmpty(primaryTable)) {
                        throw new Exception("Primary table is required for code-less screen");
                    }
                    //This is needed (Ex: NON_PART_USAGE)
                    primaryTable = primaryTable.toLowerCase();
                }

                if (currentScreenProperties.containsKey("linked_screen_id"))
                    linkedScreenId = currentScreenProperties.get("linked_screen_id");
                if (currentScreenProperties.containsKey("where_clause_script"))
                    whereClauseScript = currentScreenProperties.get("where_clause_script");

                searchable = false;
                if (currentScreenProperties.containsKey("searchable")) {
                    String searchableString = currentScreenProperties.get("searchable");
                    if (MetrixStringHelper.valueIsEqual(searchableString, "Y"))
                        searchable = true;
                }
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }

        shouldRenderContextMenu = (!gotHereFromLinkedScreen && !MetrixStringHelper.isNullOrEmpty(linkedScreenId));
    }

    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
        super.onStart();

        setupActionBar();
        populateList();
        setListeners();

        if (shouldRenderContextMenu)
            registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());

        if (searchable)
            MetrixAutoCompleteHelper.populateAutoCompleteTextView(codeLessScreenName, mSearchCriteria, this);
    }

    protected void setupActionBar() {
        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        String actionBarScript = currentScreenProperties.get("action_bar_script");
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
    }

    protected void setListeners() {
        mSearchCriteria = (AutoCompleteTextView) findViewById(R.id.search_criteria);
        if (searchable)
            mSearchCriteria.addTextChangedListener(this);
        else
            mSearchCriteria.setVisibility(View.GONE);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                MetrixActivityHelper.hideKeyboard(MetadataListActivity.this);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    private void populateList() {
        try {
            MetrixCursor cursor = null;
            int rowCount = 0;
            List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
            try {
                ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
                String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(mCurrentActivity), clientScriptDef);
                String searchCriteria = "";
                if (searchable)
                    searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
                String query = MetrixListScreenManager.generateListQuery(primaryTable, result, searchCriteria, codeLessScreenId);

                String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
                if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
                    query = query + " limit " + maxRows;
                }

                cursor = MetrixDatabaseManager.rawQueryMC(query, null);
                if (cursor != null && cursor.moveToFirst()) {
                    rowCount = cursor.getCount();

                    while (cursor.isAfterLast() == false) {
                        HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, codeLessScreenId);
                        table.add(row);
                        cursor.moveToNext();
                    }
                    cursor.close();

                    table = MetrixListScreenManager.performScriptListPopulation(this, codeLessScreenId, codeLessScreenName, table);
                }
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            } finally {
                MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (rowCount > 0) {
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
                if (shouldRenderContextMenu)
                    registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());
            }

            if (mAdapter == null) {
                mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, codeLessScreenId, "time_commit.metrix_row_id", this);
                recyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.updateData(table);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    @Override
    public boolean OnCreateMetrixActionView(View view, Integer... position) {
        MetrixActionView metrixActionView = getMetrixActionView();
        Menu menu = metrixActionView.getMenu();

        if (menu.findItem(MetadataListMetrixActionView.ADD_NEW_ITEM_ID) == null)
            menu.add(1, MetadataListMetrixActionView.ADD_NEW_ITEM_ID, 0, MetadataListMetrixActionView.ADD_NEW_ITEM);

        return super.OnCreateMetrixActionView(view);
    }

    @Override
    public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case MetadataListMetrixActionView.ADD_NEW_ITEM_ID:
                if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                    int intLinkedScreenId = Integer.valueOf(linkedScreenId);
                    String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
                    if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
                        if (screenType.toLowerCase().contains("standard")) {
                            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataStandardActivity.class);
                            intent.putExtra("ScreenID", intLinkedScreenId);
                            intent.putExtra("NavigatedFromLinkedScreen", true);
                            MetrixActivityHelper.startNewActivity(this, intent);
                        } else
                            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
                    }
                }
                break;
        }

        return super.onMetrixActionViewItemClick(menuItem);
    }

    @Override
    public void afterTextChanged(Editable s) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> {
            if (!MetadataListActivity.this.isDestroyed())
                this.populateList();
        }, 500);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if (searchable)
            MetrixAutoCompleteHelper.saveAutoCompleteFilter(codeLessScreenName, mSearchCriteria);

        if (scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this)))
            return;

        try {
            HashMap<String, String> selectedItem = mAdapter.getListData().get(position);
            if (selectedItem == null) return;

            if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                int intLinkedScreenId = Integer.valueOf(linkedScreenId);
                String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
                if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
                    if (screenType.toLowerCase().contains("standard")) {
                        String metrixRowIdKeyName = String.format("%s.%s", primaryTable, "metrix_row_id");
                        Intent intent = MetrixActivityHelper.createActivityIntent(MetadataListActivity.this, MetadataStandardActivity.class, MetrixTransactionTypes.UPDATE,
                                "metrix_row_id", selectedItem.get(metrixRowIdKeyName));
                        intent.putExtra("ScreenID", intLinkedScreenId);
                        intent.putExtra("NavigatedFromLinkedScreen", true);
                        MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
                    } else
                        MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if (shouldRenderContextMenu && onCreateMetrixActionViewListner != null)
            onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
    }
}
