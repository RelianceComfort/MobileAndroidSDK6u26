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
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.StockCountHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by hesplk on 4/8/2016.
 */
public class StockCountPartList extends MetrixActivity implements MetrixRecyclerViewListener {
    String runId, filter = "";
    private MetadataRecyclerViewAdapter mAdapter;
    private RecyclerView recyclerView;
    List<HashMap<String, String>> data;
    private FloatingActionButton btnPost;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_count_part_list);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    @Override
    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        btnPost = (FloatingActionButton) findViewById(R.id.btn_stock_post);
        mFABList.add(btnPost);

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String whereClause = String.format("run_id = '%s' and confirmed is null",runId);
                int unconfirmedCount = MetrixDatabaseManager.getCount("stock_count",whereClause );
                if(unconfirmedCount == 0) {
                    if(SettingsHelper.getSyncPause(mCurrentActivity))
                    {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if(syncPauseAlertDialog != null)
                        {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                    postStockCount();
                                }
                            });
                        }
                    }
                    else
                        postStockCount();
                }
                else{
                    MetrixUIHelper.showSnackbar(StockCountPartList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("UnconfirmedWarning"));
                }
            }
        });

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = this::showFABs;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

    private void postStockCount()
    {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(StockCountPartList.this);
                MobileApplication.startSync(StockCountPartList.this, 5);

                if (performStockCount() == false) {
                    MobileApplication.stopSync(StockCountPartList.this);
                    MobileApplication.startSync(StockCountPartList.this);
                    StockCountPartList.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MetrixUIHelper.showSnackbar(StockCountPartList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobileServiceUnavailable"));
                        }
                    });
                    return;
                }
                else{
                    StockCountPartList.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MetrixUIHelper.showSnackbar(StockCountPartList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("StockCountPostMessage"));
                        }
                    });
                }
            }
        });
        thread.start();
        StockCountHelper.UpdateFlag(data);
        Intent intent = MetrixActivityHelper.createActivityIntent(this, StockCount.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        MetrixActivityHelper.startNewActivity(this, intent);
        return;
    }

    private boolean performStockCount()
    {
        MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
        String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

        if (((MetrixActivity)this).ping(baseUrl, remote) == false)
            return false;
        return StockCountHelper.performStkCount();
    }

    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.btn_stock_post, "PostStock"));
        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        runId = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("stock_count.run_id"));
        filter = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("stock_count.filter"));

        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            if (this.mHandlingErrors) {
                actionBarTitle.setText(AndroidResourceHelper.getMessage("ErrorActionBarTitle1Arg", MobileGlobal.mErrorInfo.transactionDescription));
            } else {
                if (!MetrixStringHelper.isNullOrEmpty(runId)) {
                    actionBarTitle.setText(AndroidResourceHelper.getMessage("Run1Arg", runId));
                }
            }
        }

        populateList();
        setListeners();
    }

    private void populateList() {
        data = new ArrayList<HashMap<String, String>>();

        MetrixCursor cursor = null;
        try {
            String filterClause = " order by stock_count.page_number , stock_count.page_sequence";
            if (!MetrixStringHelper.isNullOrEmpty(filter)) {
                filterClause = String.format(" and stock_count.confirmed %s %s", filter, filterClause);
            }
            cursor = MetrixDatabaseManager.rawQueryMC(StockCountHelper.getPartListQuery(this, runId, filterClause), null);
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    {
                        HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
                        data.add(row);
                        cursor.moveToNext();
                    }
                }
                data = MetrixListScreenManager.performScriptListPopulation(this, data);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, data, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "stock_count.metrix_row_id", this);
            recyclerView.addItemDecoration(mBottomOffset);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(data);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(scriptEventConsumesListTap(StockCountPartList.this, view, MetrixScreenManager.getScreenId(StockCountPartList.this))) return;

        StockCountHelper.reset();

        MetrixPublicCache.instance.addItem("ToBarcode", "N");
        Intent intent = new Intent(StockCountPartList.this, StockCountDetail.class);
        MetrixPublicCache.instance.addItem("stock_count.run_id", runId);
        MetrixPublicCache.instance.addItem("stock_count.filter", filter);
        MetrixPublicCache.instance.addItem("stock_count.position", position);
        MetrixPublicCache.instance.addItem("stock_count.metrix_row_id", listItemData.get("stock_count.metrix_row_id"));
        MetrixActivityHelper.startNewActivity(StockCountPartList.this,intent);

    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}

