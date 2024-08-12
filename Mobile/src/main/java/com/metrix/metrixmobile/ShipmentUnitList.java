package com.metrix.metrixmobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by royaus on 4/4/2016.
 */
public class ShipmentUnitList extends MetrixActivity implements
        MetrixRecyclerViewListener, View.OnClickListener {
    private RecyclerView shipmentUnitRecyclerView = null;
    private MetadataRecyclerViewAdapter mAdapter;
    private FloatingActionButton mAddButton;
    private String mShipmentId = "";
    private int mSelectedPosition;
    private String mShipmentSequence = "";
    private String mPlaceIdShipdTo = "";
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipment_unit_list);
        shipmentUnitRecyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(shipmentUnitRecyclerView, R.drawable.rv_item_divider);
//        try {
//            mLayout = (ViewGroup) findViewById(R.id.table_layout);
//            View parent = getLayoutInflater().inflate(R.layout.shipment_unit_list_menu_bar, mLayout);
//            AndroidResourceHelper.setResourceValues(parent.findViewById(R.id.addUnit), "Add", false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        AndroidResourceHelper.setResourceValues(this.findViewById(R.id.addUnit), "Add", false);
        mShipmentId = MetrixCurrentKeysHelper.getKeyValue("shipment","shipment_id");
        mShipmentSequence = MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "sequence");
        mPlaceIdShipdTo = MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "place_id_shipd_to");
        super.onStart();

        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            actionBarTitle.setText(AndroidResourceHelper.getMessage("ShipActionBarTitle2Args", mShipmentId, mPlaceIdShipdTo));
        }

        populateList();
        setListeners();
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mAddButton = (FloatingActionButton) findViewById(R.id.addUnit);
        mAddButton.setOnClickListener(this);
        mFABList.add(mAddButton);

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = this::showFABs;

        shipmentUnitRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 || dy < 0) {
                    fabHandler.removeCallbacks(fabRunnable);
                    hideFABs(mFABList);
                    fabHandler.postDelayed(fabRunnable, fabDelay);
                }
            }
        });
    }

    /**
     * Populate the receiving list with the tasks assigned to the user.
     */
    private void populateList() {
        String query = MetrixListScreenManager.generateListQuery(this, "shipment_unit", "shipment_unit.shipment_id = " + mShipmentId + " and shipment_unit.shipment_sequence = " + mShipmentSequence);

        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query = query + " limit " + maxRows;
        }

        MetrixCursor cursor = null;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
                table.add(row);
                cursor.moveToNext();
            }

            table = MetrixListScreenManager.performScriptListPopulation(this, table);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "shipment_unit.metrix_row_id", this);
            shipmentUnitRecyclerView.addItemDecoration(mBottomOffset);
            shipmentUnitRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addUnit:
                Intent intent = MetrixActivityHelper.createActivityIntent(this, ShipmentUnit.class);
                MetrixActivityHelper.startNewActivity(this, intent);
                break;
            default:
                super.onClick(v);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        mSelectedPosition = position;
        if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

        final DialogInterface.OnClickListener modifyListener = (dialog, which) -> {
            try {
                HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
                String shipmentId = selectedItem.get("shipment_unit.shipment_id");
                String shipmentSequence = selectedItem.get("shipment_unit.shipment_sequence");
                String sequence = selectedItem.get("shipment_unit.sequence");
                Intent intent = MetrixActivityHelper.createActivityIntent(ShipmentUnitList.this, ShipmentUnit.class,
                        MetrixTransactionTypes.UPDATE, "metrix_row_id", selectedItem.get("shipment_unit.metrix_row_id"));
                intent.putExtra("shipment_unit__shipment_id", shipmentId);
                intent.putExtra("shipment_unit__shipment_sequence", shipmentSequence);
                intent.putExtra("shipment_unit__sequence", sequence);
                MetrixActivityHelper.startNewActivity(ShipmentUnitList.this, intent);
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        final DialogInterface.OnClickListener deleteListener = (dialog, which) -> {
            try {
                HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);

                String metrixRowId = selectedItem.get("shipment_unit.metrix_row_id");
                String shipmentId = MetrixDatabaseManager.getFieldStringValue("shipment_unit", "shipment_id",
                        "metrix_row_id=" + metrixRowId);
                String shipmentSequence = MetrixDatabaseManager.getFieldStringValue("shipment_unit", "shipment_sequence",
                        "metrix_row_id=" + metrixRowId);
                String sequence = MetrixDatabaseManager.getFieldStringValue("shipment_unit", "sequence",
                        "metrix_row_id=" + metrixRowId);
                Hashtable<String,String> keys = new Hashtable<String, String>();
                keys.put("shipment_id", shipmentId);
                keys.put("shipment_sequence", shipmentSequence);
                keys.put("sequence", sequence);

                MetrixUpdateManager.delete(ShipmentUnitList.this, "shipment_unit", metrixRowId, keys, AndroidResourceHelper.getMessage("ShipmentUnit"), new MetrixTransaction());
                mAdapter.getListData().remove(mSelectedPosition);
                mAdapter.notifyItemRemoved(mSelectedPosition);
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("UnitLCase"), modifyListener, deleteListener, this);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        mSelectedPosition = position;
    }
}
