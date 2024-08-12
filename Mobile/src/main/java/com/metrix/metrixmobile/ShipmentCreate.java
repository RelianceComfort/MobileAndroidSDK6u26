package com.metrix.metrixmobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
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
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by royaus on 4/4/2016.
 */
public class ShipmentCreate extends MetrixActivity implements View.OnFocusChangeListener, SimpleRecyclerViewAdapter.OnItemClickListener {
    private FloatingActionButton mAddButton, mProcessButton;
    private EditText mPartId, mPlaceIdTo, mLocationTo, mPlaceIdFrom, mLocationFrom, mQuantity;
    private CheckBox mInTransit, mUsable;
    private RecyclerView recyclerView;
    private SimpleRecyclerViewAdapter simpleAdapter;
    private List<HashMap<String, String>> mPartsList;
    private String[] mFrom;
    private int[] mTo;
    private int mSelectedPosition;
    protected MetrixUIHelper coreUIHelper = new MetrixUIHelper(this);
    private static boolean mResponseProcessed = false;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipment_create);
        mPartsList = new ArrayList<HashMap<String, String>>();
        recyclerView = findViewById(R.id.partsList);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
        recyclerView.setNestedScrollingEnabled(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        mAddButton = (FloatingActionButton) findViewById(R.id.add);
        mProcessButton = (FloatingActionButton) findViewById(R.id.process);

        mAddButton.setOnClickListener(this);
        mProcessButton.setOnClickListener(this);

        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mFABList.add(mAddButton);
        mFABList.add(mProcessButton);

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        if(mPartsList == null || mPartsList.isEmpty())
            mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
        else {
            recyclerView.removeItemDecoration(mBottomOffset);
            mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));
            recyclerView.addItemDecoration(mBottomOffset);
        }

        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
                fabHandler.removeCallbacks(fabRunnable);
                if(mFABsToShow != null)
                    mFABsToShow.clear();
                else
                    mFABsToShow = new ArrayList<>();

                hideFABs(mFABList);
                fabHandler.postDelayed(fabRunnable, fabDelay);
            }
        });
    }

    /**
     * Set the default values for views for this activity.
     */

    protected void defaultValues() {
        try {
            String inTransit = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='INTRANSIT_SHIPMENTS'");

            mPartId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "part_id");
            mPlaceIdFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "place_id_from");
            mLocationFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location_from");
            mPlaceIdTo = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "place_id_to");
            mLocationTo = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location_to");
            mInTransit = (CheckBox) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "in_transit");
            mQuantity = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "quantity");
            mUsable = (CheckBox) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "usable");

            String theDefaultPartId = MetrixCurrentKeysHelper.getKeyValue("stock_bin", "part_id");
            String usable = MetrixCurrentKeysHelper.getKeyValue("stock_bin", "usable");
            MetrixControlAssistant.setValue(mPartId, theDefaultPartId);
            //MetrixControlAssistant.setValue(mPlaceIdFrom, "");
            //MetrixControlAssistant.setValue(mLocationFrom, "");
            MetrixControlAssistant.setValue(mPlaceIdTo, "");
            MetrixControlAssistant.setValue(mLocationTo, "");
            MetrixControlAssistant.setValue(mQuantity, "1");
            MetrixControlAssistant.setValue(mInTransit, inTransit);
            MetrixControlAssistant.setValue(mUsable, usable);
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }



    /**
     * This method is responsible for setting up the meta data which the
     * architecture uses for data binding and validation.
     */
    protected void defineForm() {
        MetrixTableDef shipmentUnitDef = new MetrixTableDef("shipment", MetrixTransactionTypes.INSERT);
        this.mFormDef = new MetrixFormDef(shipmentUnitDef);

        mFrom = new String[] { "part_id", "quantity", "usable"};
        mTo = new int[] { R.id.part_id, R.id.quantity, R.id.usable };
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        switch (v.getId()) {
            case R.id.add:
                addPart();
                break;
            case R.id.process:
                processClick();
                break;
            default:
                super.onClick(v);
        }
    }

    private void addPart()
    {
        try {
            if (MetrixControlAssistant.getValue(mPartId).length() > 0 && MetrixControlAssistant.getValue(mQuantity).length() > 0)
            {
                //Add the part to the list.
                String partId = MetrixControlAssistant.getValue(mPartId);
                String quantity = MetrixControlAssistant.getValue(mQuantity);
                String usable = MetrixControlAssistant.getValue(mUsable);

                HashMap<String, String> hash = new HashMap<String, String>();
                hash.put(mFrom[0], partId);
                hash.put(mFrom[1], quantity);
                hash.put(mFrom[2], usable);
                mPartsList.add(hash);
                MetrixControlAssistant.setValue(mPartId, "");
                MetrixControlAssistant.setValue(mQuantity, "");
                MetrixControlAssistant.setValue(mUsable, "N");
                bindListView();
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    /**
     * Bind the listview for the stock list
     */
    private void bindListView() {
        // Bind the data with the list
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        for (HashMap<String, String> item : mPartsList) {
            HashMap<String, String> row = new HashMap<String, String>();
            row.put(mFrom[0], item.get("part_id"));
            row.put(mFrom[1], item.get("quantity"));
            row.put(mFrom[2], item.get("usable"));
            table.add(row);
        }

        if (simpleAdapter == null) {
            mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)getResources().getDimension((R.dimen.md_margin)));
            simpleAdapter = new SimpleRecyclerViewAdapter(table, R.layout.shipment_create_part_list_item, mFrom, mTo, new int[]{}, null);
            simpleAdapter.setClickListener(this);
            recyclerView.addItemDecoration(mBottomOffset);
            recyclerView.setAdapter(simpleAdapter);
        } else {
            simpleAdapter.updateData(table);
        }
    }

    private boolean doCreateShipment()
    {
        mResponseProcessed = false;
        MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
        String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

        if (ping(baseUrl, remote) == false)
            return false;

        try {
            String inTransit = MetrixControlAssistant.getValue(mInTransit);
            String partId = MetrixControlAssistant.getValue(mPartId);
            String quantity = MetrixControlAssistant.getValue(mQuantity);
            String usable = MetrixControlAssistant.getValue(mUsable);
            String placeIdFrom = MetrixControlAssistant.getValue(mPlaceIdFrom);
            String locationFrom = MetrixControlAssistant.getValue(mLocationFrom);
            String placeIdTo = MetrixControlAssistant.getValue(mPlaceIdTo);
            String locationTo = MetrixControlAssistant.getValue(mLocationTo);

            Hashtable<String, String> theMpmParameters = new Hashtable<String, String>();
            theMpmParameters.put("in_transit", inTransit);
            theMpmParameters.put("place_id_from", placeIdFrom);
            theMpmParameters.put("location_from", locationFrom);
            theMpmParameters.put("place_id_to", placeIdTo);
            theMpmParameters.put("location_to", locationTo);

            StringBuilder parts = new StringBuilder();
            if (this.simpleAdapter.getItemCount() > 0)
            {
                int partCount = this.simpleAdapter.getItemCount();
                int currentIndex = 0;
                for (HashMap<String, String> item : mPartsList)
                {
                    parts.append(item.get("part_id"));
                    parts.append("~" + item.get("quantity"));
                    parts.append("~" + item.get("usable"));

                    if (currentIndex != (partCount - 1))
                        parts.append("|");
                    currentIndex++;
                }
            }
            else
            {
                parts.append(partId);
                parts.append("~" + quantity);
                parts.append("~" + usable);
            }
            theMpmParameters.put("parts", parts.toString());
            theMpmParameters.put("person_id", User.getUser().personId);

            MetrixPerformMessage theMPM = new MetrixPerformMessage("perform_create_mobile_shipment", theMpmParameters);
            theMPM.save();
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
            return false;
        }

        return true;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        super.onFocusChange(v, hasFocus);
    }

    private void processClick() {

        try {
            String partId = MetrixControlAssistant.getValue(mPartId);
            String quantity = MetrixControlAssistant.getValue(mQuantity);
            String placeIdFrom = MetrixControlAssistant.getValue(mPlaceIdFrom);
            String locationFrom = MetrixControlAssistant.getValue(mLocationFrom);
            String placeIdTo = MetrixControlAssistant.getValue(mPlaceIdTo);
            String locationTo = MetrixControlAssistant.getValue(mLocationTo);

            if (MetrixStringHelper.isNullOrEmpty(placeIdFrom)||MetrixStringHelper.isNullOrEmpty(locationFrom)) {
                MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PlaceIdFromValuesRequired"));
                return;
            }

            if (MetrixStringHelper.isNullOrEmpty(placeIdTo)||MetrixStringHelper.isNullOrEmpty(locationTo)) {
                MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PlaceIdToValuesRequired"));
                return;
            }

            //Make sure we have at least one part listed
            if (simpleAdapter.getItemCount() == 0 && (MetrixStringHelper.isNullOrEmpty(partId) || MetrixStringHelper.isNullOrEmpty(quantity)))
            {
                MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobileShipmentCreateNoPart"));
                return;
            }
        }
        catch (Exception e) {
            LogManager.getInstance(this).error(e);
            return;
        }

        if(SettingsHelper.getSyncPause(mCurrentActivity))
        {
            SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
            if(syncPauseAlertDialog != null)
            {
                syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                    @Override
                    public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                        startShipmentCreate();
                    }
                });
            }
        }
        else
            startShipmentCreate();
    }

    private void startShipmentCreate() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doCreateShipment() == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    ShipmentCreate.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobileServiceUnavailable")+" "+AndroidResourceHelper.getMessage("ConfirmShipmentWithBackOffice"));
                        }
                    });
                    return;
                }

                // start waiting dialog on-screen
                mLayout.postDelayed(Timer_Tick, 25000);
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddShipmentProgress"));
            }
        });

        thread.start();
    }

    @Override
    protected void bindService() {
        bindService(new Intent(ShipmentCreate.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
                    if (activityType == Global.ActivityType.Download && message.contains("SHIPMENT")) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mResponseProcessed = true;
                        if(mResponseProcessed)
                            mUIHelper.dismissLoadingDialog();
                        Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, ShipmentList.class);
                        MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    private void processEnded() {
        MobileApplication.stopSync(mCurrentActivity);
        MobileApplication.startSync(mCurrentActivity);
        mUIHelper.dismissLoadingDialog();
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            if (!mResponseProcessed) {
                // This method runs in the same thread as the UI.
                processEnded();
                MetrixUIHelper.showErrorDialogOnGuiThread(mCurrentActivity, AndroidResourceHelper.getMessage("ConfirmShipmentWithBackOffice"));
            }
            else {
                processEnded();
            }
        }
    };

    @Override
    public void onSimpleRvItemClick(int position, Object item, View view) {
        mSelectedPosition = position;
        final DialogInterface.OnClickListener deleteListener = (dialog, which) -> {
            try {
                @SuppressWarnings("unchecked")
                HashMap<String, String> selectedItem = (HashMap<String, String>) simpleAdapter.getData().get(mSelectedPosition);
                mPartsList.remove(selectedItem);
                bindListView();
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("PartLCase"), deleteListener, null, this);
    }
}

