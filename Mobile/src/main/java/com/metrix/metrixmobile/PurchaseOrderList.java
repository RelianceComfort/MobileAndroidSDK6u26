package com.metrix.metrixmobile;

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

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
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

public class PurchaseOrderList extends MetrixActivity implements View.OnClickListener, TextWatcher, MetrixRecyclerViewListener {
    private RecyclerView recyclerView;
    private MetadataRecyclerViewAdapter mAdapter;
    private AutoCompleteTextView mSearchCriteria;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase_order_list);
        mHandler = new Handler();
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    /*
 * (non-Javadoc)
 *
 * @see android.app.Activity#onStart()
 */
    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);

        String barcodingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_BARCODE_SCANNING'");
        if (!MetrixStringHelper.isNullOrEmpty(barcodingEnabled) && barcodingEnabled.compareToIgnoreCase("Y") == 0) {
            registerForMetrixActionView(findViewById(R.id.search_criteria), getMetrixActionBar().getCustomView());
        }

        populateList();

        registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());

        MetrixAutoCompleteHelper.populateAutoCompleteTextView(PurchaseOrderList.class.getName(), mSearchCriteria, this);

        String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
        String actionBarTitle = "";
        actionBarTitle = String.format("%s", AndroidResourceHelper.getMessage("PurchaseOrderList"));

        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);
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
        PurchaseOrderListMetrixActionView.onCreateMetrixActionView(menu);
        return super.OnCreateMetrixActionView(view);
    }

    @Override
    public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
        if (menuItem.getTitle().toString().compareToIgnoreCase(StockListMetrixActionView.ADDPURCHASE) == 0) {
            Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseOrder.class);
            intent.putExtra("page_mode", "INSERT");
            MetrixActivityHelper.startNewActivity(this, intent);
        }
        return super.onMetrixActionViewItemClick(menuItem);
    }

    /**
     * Populate the purchase_order list with the person_id_buyer assigned to the user.
     */
    private void populateList() {
        StringBuilder whereClause = new StringBuilder();
        String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
        if (!MetrixStringHelper.isNullOrEmpty(searchCriteria)) {
            whereClause.append(" where (purchase_order.supplier_id like '%" + searchCriteria + "%' or purchase_order.approval_status like '%" + searchCriteria + "%' or purchase_order.place_id_ship_to like '%" + searchCriteria
                    + "%') and purchase_order.person_id_buyer = '"+ User.getUser().personId+"'");
        }
        else {
            whereClause.append(" where (purchase_order.person_id_buyer = '"+ User.getUser().personId+"')");
        }

        whereClause.append(" and (purchase_order.po_status is null or purchase_order.po_status in ('CO','PR'))");

        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");

        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            whereClause.append(" limit " + maxRows);
        }

        String query = MetrixListScreenManager.generateListQuery(this, "purchase_order", whereClause.toString());
        MetrixCursor cursor = null;
        int rowCount = 0;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor != null && cursor.moveToFirst()) {

                rowCount = cursor.getCount();

                while (cursor.isAfterLast() == false) {
                    HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

                    table.add(row);
                    cursor.moveToNext();
                }

                table = MetrixListScreenManager.performScriptListPopulation(this, table);
            }

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
            registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "purchase_order.metrix_row_id", this);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> {
            if (!PurchaseOrderList.this.isDestroyed())
                this.populateList();
        }, 500);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        MetrixAutoCompleteHelper.saveAutoCompleteFilter(PurchaseOrderList.class.getName(), mSearchCriteria);
        if(scriptEventConsumesListTap(PurchaseOrderList.this, view, MetrixScreenManager.getScreenId(PurchaseOrderList.this))) return;

        final String purchaseOrderId = listItemData.get("purchase_order.purchase_order_id");

        Intent intent = MetrixActivityHelper.createActivityIntent(PurchaseOrderList.this, PurchaseOrder.class, MetrixTransactionTypes.UPDATE,
                "metrix_row_id", listItemData.get("purchase_order.metrix_row_id"));

        MetrixCurrentKeysHelper.setKeyValue("purchase_order", "purchase_order_id", purchaseOrderId);
        intent.putExtra("purchase_order_id", purchaseOrderId);

        MetrixActivityHelper.startNewActivity(PurchaseOrderList.this, intent);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(onCreateMetrixActionViewListner != null)
            onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
    }
}
