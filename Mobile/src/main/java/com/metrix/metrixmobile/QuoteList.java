package com.metrix.metrixmobile;

import android.annotation.SuppressLint;
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
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by hesplk on 8/19/2016.
 */
public class QuoteList extends MetrixActivity implements TextWatcher, MetrixRecyclerViewListener {
    private MetadataRecyclerViewAdapter mAdapter;
    private AutoCompleteTextView mSearchCriteria;
    private RecyclerView recyclerView;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quote_list);
        mHandler = new Handler();
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    @Override
    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        populateList();

        registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());

        MetrixAutoCompleteHelper.populateAutoCompleteTextView(QuoteList.class.getName(), mSearchCriteria, this);
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        mSearchCriteria = (AutoCompleteTextView) findViewById(R.id.search_criteria);
        mSearchCriteria.addTextChangedListener(this);
    }

    @Override
    public boolean OnCreateMetrixActionView(View view, Integer... position) {
        MetrixActionView metrixActionView = getMetrixActionView();
        Menu menu = metrixActionView.getMenu();

        if (menu.findItem(QuoteListActionView.ADDQUOTEMENUITEM) == null)
            menu.add(1, QuoteListActionView.ADDQUOTEMENUITEM, 0, QuoteListActionView.ADDQUOTE);

        return super.OnCreateMetrixActionView(view);
    }

    @Override
    public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case QuoteListActionView.ADDQUOTEMENUITEM:
                MetrixCurrentKeysHelper.setKeyValue("quote", "quote_id", "");
                MetrixCurrentKeysHelper.setKeyValue("quote", "quote_version", "");
                MetrixWorkflowManager.advanceWorkflow(MetrixWorkflowManager.QUOTE_WORKFLOW, QuoteList.this);
                break;
        }

        return super.onMetrixActionViewItemClick(menuItem);
    }

    @Override
    public void afterTextChanged(Editable s) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> {
            if (!QuoteList.this.isDestroyed())
                this.populateList();
        }, 500);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}


    /**
     * Populate the job list with the tasks assigned to the user.
     */
    @SuppressLint("SimpleDateFormat")
    private boolean populateList() {
        StringBuilder whereClause = new StringBuilder();

        String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
        
        whereClause.append(String.format("where quote.quote_owner = '%1$s' and (quote.quote_internal_status = 'OP' or quote.quote_internal_status = 'RDY' or quote.quote_internal_status = 'HO' or quote.quote_internal_status = 'HR')",User.getUser().personId));
        StringBuilder query = new StringBuilder();
        query.append(MetrixListScreenManager.generateListQuery(this, "quote", whereClause.toString(),searchCriteria));

        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query.append(" limit " + maxRows);
        }

        MetrixCursor cursor = null;
        int rowCount = 0;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        int count = 0;

        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor != null && cursor.moveToFirst()) {
                rowCount = cursor.getCount();
                while (cursor.isAfterLast() == false) {
                    count = count + 1;
                    HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

                    String quoteId = row.get("quote.quote_id");
                    String quoteVersion = row.get("quote.quote_version");
                    String formattedIdValue = String.format("%1$s/%2$s",quoteId,quoteVersion);

                    row.put("custom.formatted_quote_id", String.format("%1$s", formattedIdValue));

                    table.add(row);
                    cursor.moveToNext();
                }

                table = MetrixListScreenManager.performScriptListPopulation(this, table);
            }
            else
            {
                MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoDataForSelectedFilter"));
            }
        }
        catch (Exception e) {
            LogManager.getInstance(this).error(e);
        } finally {
            MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
            if (cursor != null) {
                cursor.close();
            }
        }

        if (count > 0) {
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.GONE);
            registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "quote.metrix_row_id", this);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }

        return true;
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(mSearchCriteria != null)
            MetrixAutoCompleteHelper.saveAutoCompleteFilter(MetrixScreenManager.getScreenName(QuoteList.this), mSearchCriteria);

        if(scriptEventConsumesListTap(QuoteList.this, view, MetrixScreenManager.getScreenId(QuoteList.this))) return;

        String quoteId = listItemData.get("quote.quote_id");
        MetrixCurrentKeysHelper.setKeyValue("quote", "quote_id", quoteId);
        String quoteVersion = listItemData.get("quote.quote_version");
        MetrixCurrentKeysHelper.setKeyValue("quote", "quote_version", quoteVersion);

        String quoteType = MetrixDatabaseManager.getFieldStringValue("quote", "quote_type", String.format("quote_id = %s", quoteId));
        String workflowName = MetrixWorkflowManager.getQuoteWorkflowNameForQuoteType(quoteType);
        MetrixWorkflowManager.advanceWorkflow(workflowName, QuoteList.this);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(onCreateMetrixActionViewListner != null)
            onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
    }
}
