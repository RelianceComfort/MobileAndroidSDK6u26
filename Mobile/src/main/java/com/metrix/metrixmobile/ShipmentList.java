package com.metrix.metrixmobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by royaus on 4/4/2016.
 */
public class ShipmentList extends MetrixActivity implements MetrixRecyclerViewListener {
    private RecyclerView recyclerView;
    private MetadataRecyclerViewAdapter mAdapter;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipment_list);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);

        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            //String partId = MetrixCurrentKeysHelper.getKeyValue("stock_bin", "part_id");
            //String name = MetrixDatabaseManager.getFieldStringValue("part", "internal_descriptn", "part_id = '" + partId + "'");
            actionBarTitle.setText(AndroidResourceHelper.getMessage("ShipmentList"));
        }

        populateList();
    }

    @Override
    protected void bindService() {
        bindService(new Intent(ShipmentList.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            // mBoundService = ((MetrixIntentService.LocalBinder) binder)
            // .getService();

            try {
                service = (IPostMonitor) binder;
                service.registerListener(listener);
            } catch (Throwable t) {
                LogManager.getInstance().error(t);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            // mBoundService = null;
            service = null;
        }
    };

    protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
        public void newSyncStatus(final Global.ActivityType activityType, final String message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (activityType == Global.ActivityType.Download) {
                        if (message.compareTo("SHIPMENT") == 0) {
                            populateList();
                        }
                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    /**
     * Populate the receiving list with the tasks assigned to the user.
     */
    private void populateList() {
        String personId = User.getUser().personId;
        String placeId = MetrixDatabaseManager.getFieldStringValue("person_place", "place_id", "place_relationship = 'FOR_STOCK' and person_id = '" + personId + "'");
        String query = MetrixListScreenManager.generateListQuery(this, "shipment", "shipment.inventory_adjusted != 'Y' and shipment.place_id_shipd_frm = '" + placeId + "'");

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
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "shipment.metrix_row_id", this);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

        String shipmentId = listItemData.get("shipment.shipment_id");
        MetrixCurrentKeysHelper.setKeyValue("shipment", "shipment_id", shipmentId);

        Intent intent = MetrixActivityHelper.createActivityIntent(this, ShipmentDetailList.class);
        intent.putExtra("shipment_id", shipmentId);

        MetrixActivityHelper.startNewActivity(this, intent);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}
