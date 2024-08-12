package com.metrix.metrixmobile;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by royaus on 4/4/2016.
 */
public class ShipmentDetailList extends MetrixActivity implements
        View.OnClickListener, MetrixRecyclerViewListener {
    private RecyclerView shipmentDetailRecyclerView = null;
    private MetadataRecyclerViewAdapter mAdapter;
    private FloatingActionButton mSaveButton;
    private String mShipmentId = "";
    protected MetrixUIHelper coreUIHelper = new MetrixUIHelper(this);
    private AlertDialog mPostShipmentDialog;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //mBaseResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixArchitectureResourceStrings");
        setContentView(R.layout.shipment_detail_list);
        shipmentDetailRecyclerView = findViewById(R.id.shipmentDetailList);
        MetrixListScreenManager.setupVerticalRecyclerView(shipmentDetailRecyclerView, R.drawable.rv_item_divider);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        try {
            AndroidResourceHelper.setResourceValues(findViewById(R.id.process), "Process");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mShipmentId = getIntent().getExtras().getString("shipment_id");
        super.onStart();

        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            actionBarTitle.setText(AndroidResourceHelper.getMessage("ShipActionBarTitle1Arg", mShipmentId));
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

        mSaveButton = (FloatingActionButton) findViewById(R.id.process);
        mSaveButton.setOnClickListener(this);

        mFABList.add(mSaveButton);

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = this::showFABs;

        shipmentDetailRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        String query = MetrixListScreenManager.generateListQuery(this, "shipment_detail", "shipment_detail.shipment_id = " + mShipmentId);

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
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "shipment_detail.metrix_row_id", this);
            shipmentDetailRecyclerView.addItemDecoration(mBottomOffset);
            shipmentDetailRecyclerView.setAdapter(mAdapter);
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
            case R.id.process:
                if(SettingsHelper.getSyncPause(mCurrentActivity))
                {
                    SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                    if(syncPauseAlertDialog != null)
                    {
                        syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                            @Override
                            public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                processClick();
                            }
                        });
                    }
                }
                else
                    processClick();
                break;
            default:
                super.onClick(v);
        }
    }

    public static boolean doPostShipment() {
        MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
        String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

        if (ping(baseUrl, remote) == false)
            return false;

        Hashtable<String, String> params = new Hashtable<String, String>();
        try {

            String shipmentId = MetrixCurrentKeysHelper.getKeyValue("shipment","shipment_id");
            params.put("shipment_id", shipmentId);

            MetrixPerformMessage performPayment = new MetrixPerformMessage("perform_post_mobile_shipment", params);
            performPayment.save();
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return false;
        }

        return true;
    }

    private void processClick() {

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doPostShipment() == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    ShipmentDetailList.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobileServiceUnavailable"));

                            if (mPostShipmentDialog != null) {
                                mPostShipmentDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mPostShipmentDialog != null) {
                    mPostShipmentDialog.dismiss();
                }

// start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("PostShipmentProgress"));

            }
        });

        thread.start();
    }

    @Override
    protected void bindService() {
        bindService(new Intent(ShipmentDetailList.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    @Override
    protected void unbindService() {
        if (mIsBound) {
            try {
                if (service != null) {
                    service.removeListener(listener);
                    unbindService(mConnection);
                }
            } catch (Exception ex) {
                LogManager.getInstance().error(ex);
            } finally {
                mIsBound = false;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            try {
                service = (IPostMonitor) binder;
                service.registerListener(listener);
            } catch (Throwable t) {
                LogManager.getInstance().error(t);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
        public void newSyncStatus(final Global.ActivityType activityType, final String message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (activityType == Global.ActivityType.Download && message.contains("perform_post_mobile_shipment_result")) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        coreUIHelper.dismissLoadingDialog();
                        mCurrentActivity.finish();
                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

        String sequence = listItemData.get("shipment_detail.sequence");
        String itemId = listItemData.get("shipment_detail.item_id");
        String usable = listItemData.get("shipment_detail.usable");
        String placeIdShipdTo = listItemData.get("shipment_detail.place_id_shipd_to");
        MetrixCurrentKeysHelper.setKeyValue("shipment_detail", "sequence", sequence);
        MetrixCurrentKeysHelper.setKeyValue("shipment_detail", "item_id", itemId);
        MetrixCurrentKeysHelper.setKeyValue("shipment_detail", "usable", usable);
        if(!MetrixStringHelper.isNullOrEmpty(placeIdShipdTo))
            MetrixCurrentKeysHelper.setKeyValue("shipment_detail", "place_id_shipd_to", placeIdShipdTo);

        Intent intent = MetrixActivityHelper.createActivityIntent(this, ShipmentUnitList.class);
        MetrixActivityHelper.startNewActivity(this, intent);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}
