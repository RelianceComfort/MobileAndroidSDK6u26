package com.metrix.metrixmobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class ReceivingDetailList extends MetrixActivity implements View.OnClickListener, MetrixRecyclerViewListener {
    private MetadataRecyclerViewAdapter mAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton mProcessButton;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiving_detail_list);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    @Override
    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.process, "Process"));
        super.onStart();
        setReceivingActionBarTitle();
        populateList();
    }

    protected void setListeners() {
        mProcessButton = (FloatingActionButton) findViewById(R.id.process);
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        if (mProcessButton != null) {
            mProcessButton.setOnClickListener(this);
            mFABList.add(mProcessButton);
        }

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

    private boolean populateList() {
        String rcvId = MetrixCurrentKeysHelper.getKeyValue("receiving", "receiving_id");

        StringBuilder query = new StringBuilder();
        query.append(MetrixListScreenManager.generateListQuery(this, "receiving_detail", String.format("receiving_detail.inventory_adjusted = 'N' and receiving_detail.receiving_id = '%1$s'", rcvId)));

        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query.append(" limit " + maxRows);
        }

        MetrixCursor cursor = null;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

                    // CUSTOM.QTY_EXPECTED ... Calculated from purchase_detail record: qty_ordered – qty_received – qty_rejected.
                    int qtyExpected = 0;
                    String poId = row.get("receiving.purchase_order_id");
                    String partId = row.get("receiving_detail.item_id");
                    ArrayList<Hashtable<String, String>> pdValues = MetrixDatabaseManager.getFieldStringValuesList(String.format("select qty_ordered, qty_received, qty_rejected from purchase_detail where purchase_order_id = '%1$s' and part_id = '%2$s'", poId, partId));
                    if (pdValues != null && pdValues.size() > 0) {
                        // we expect one row, so take the first row in the non-empty result
                        Hashtable<String, String> pdRow = pdValues.get(0);
                        String qtyOrderedString = pdRow.get("qty_ordered") == null ? "0" : pdRow.get("qty_ordered");
                        String qtyReceivedString = pdRow.get("qty_received") == null ? "0" : pdRow.get("qty_received");
                        String qtyRejectedString = pdRow.get("qty_rejected") == null ? "0" : pdRow.get("qty_rejected");
                        int qtyOrdered = Integer.valueOf(qtyOrderedString);
                        int qtyReceived = Integer.valueOf(qtyReceivedString);
                        int qtyRejected = Integer.valueOf(qtyRejectedString);
                        qtyExpected = qtyOrdered - qtyReceived - qtyRejected;
                    }

                    // CUSTOM.QTY_RECEIVED ... Sum up the quantity of the receiving_unit records that are linked to the receiving_detail record.
                    int qtyReceived = 0;
                    String rcvSeq = row.get("receiving_detail.sequence");
                    ArrayList<Hashtable<String, String>> ruValues = MetrixDatabaseManager.getFieldStringValuesList(String.format("select quantity from receiving_unit where receiving_id = '%1$s' and receiving_sequence = %2$s", rcvId, rcvSeq));
                    if (ruValues != null && ruValues.size() > 0) {
                        for (Hashtable<String, String> ruRow : ruValues) {
                            String qtyString = ruRow.get("quantity") == null ? "0" : ruRow.get("quantity");
                            qtyReceived += Integer.valueOf(qtyString);
                        }
                    }

                    row.put("custom.qty_expected", String.valueOf(qtyExpected));
                    row.put("custom.qty_received", String.valueOf(qtyReceived));

                    table.add(row);
                    cursor.moveToNext();
                }

                table = MetrixListScreenManager.performScriptListPopulation(this, table);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "receiving_detail.metrix_row_id", this);
            recyclerView.addItemDecoration(mBottomOffset);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.process:
                RcvDetailCounter rcvDetailCtr = analyzeStateOfRcvDetails();
                if (rcvDetailCtr.mAllRDsAreZero) {
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PORcvNoUnits"));
                } else if (rcvDetailCtr.mAllRDsHaveExactQty) {
                    performPostReceipt();
                } else {
                    StringBuilder confirmBuilder = new StringBuilder();
                    if (!MetrixStringHelper.isNullOrEmpty(rcvDetailCtr.mListOfOverReceiptParts)) {
                        confirmBuilder.append(AndroidResourceHelper.getMessage("PORcvOverReceipt1Arg", rcvDetailCtr.mListOfOverReceiptParts));
                        confirmBuilder.append("\n");
                    }
                    if (!MetrixStringHelper.isNullOrEmpty(rcvDetailCtr.mListOfUnderReceiptParts)) {
                        confirmBuilder.append(AndroidResourceHelper.getMessage("PORcvUnderReceipt1Arg", rcvDetailCtr.mListOfUnderReceiptParts));
                        confirmBuilder.append("\n");
                    }
                    confirmBuilder.append(AndroidResourceHelper.getMessage("PORcvAreYouSure"));

                    DialogInterface.OnClickListener postListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            performPostReceipt();
                        }
                    };
                    MetrixDialogAssistant.showAlertDialog(AndroidResourceHelper.getMessage("PostReceipt"), confirmBuilder.toString(), AndroidResourceHelper.getMessage("Yes"), postListener, AndroidResourceHelper.getMessage("No"), null, this);
                }
                break;
            default:
                super.onClick(v);
        }
    }

    private void performPostReceipt () {
        try {
            // Since we are not doing any simultaneous checkbox/un-received part processing (as at ReceivingUnitList),
            // just send a simple perform message to post the receipt
            Hashtable<String, String> params = new Hashtable<String, String>();
            String rcvId = MetrixCurrentKeysHelper.getKeyValue("receiving", "receiving_id");
            params.put("receiving_id", rcvId);
            MetrixPerformMessage performPR = new MetrixPerformMessage("perform_post_receipt", params);
            boolean success = performPR.save();
            if (success) {
                MetrixDatabaseManager.executeSql("update receiving set inventory_adjusted = 'Y' where receiving_id ="+rcvId);
                finish();
            } else {
                MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FailedPostReceiving"));
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private RcvDetailCounter analyzeStateOfRcvDetails() {
        RcvDetailCounter rcvDC = new RcvDetailCounter();
        try {
            if (mAdapter != null && mAdapter.getListData() != null) {
                if (mAdapter.getListData().size() > 0) {
                    rcvDC.mAllRDsAreZero = true;
                    StringBuilder overReceivedBuilder = new StringBuilder();
                    StringBuilder underReceivedBuilder = new StringBuilder();
                    for (HashMap<String, String> dataRow : mAdapter.getListData()) {
                        String qtyExpectedString = dataRow.get("custom.qty_expected") == null ? "0" : dataRow.get("custom.qty_expected");
                        String qtyReceivedString = dataRow.get("custom.qty_received") == null ? "0" : dataRow.get("custom.qty_received");
                        String partId = dataRow.get("receiving_detail.item_id");
                        int qtyExpected = Integer.valueOf(qtyExpectedString);
                        int qtyReceived = Integer.valueOf(qtyReceivedString);

                        if (rcvDC.mAllRDsAreZero && qtyReceived > 0)
                            rcvDC.mAllRDsAreZero = false;

                        if (qtyReceived > qtyExpected)
                            overReceivedBuilder.append(String.format("%1$s, ", partId));
                        else if (qtyReceived < qtyExpected)
                            underReceivedBuilder.append(String.format("%1$s, ", partId));
                    }

                    if (overReceivedBuilder.length() > 0) {
                        String overRcvCandidate = overReceivedBuilder.toString();
                        rcvDC.mListOfOverReceiptParts = overRcvCandidate.substring(0, overRcvCandidate.lastIndexOf(","));
                    }
                    if (underReceivedBuilder.length() > 0) {
                        String underRcvCandidate = underReceivedBuilder.toString();
                        rcvDC.mListOfUnderReceiptParts = underRcvCandidate.substring(0, underRcvCandidate.lastIndexOf(","));
                    }
                }

                if (!rcvDC.mAllRDsAreZero && MetrixStringHelper.isNullOrEmpty(rcvDC.mListOfOverReceiptParts) && MetrixStringHelper.isNullOrEmpty(rcvDC.mListOfUnderReceiptParts))
                    rcvDC.mAllRDsHaveExactQty = true;
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
        return rcvDC;
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(scriptEventConsumesListTap(ReceivingDetailList.this, view, MetrixScreenManager.getScreenId(ReceivingDetailList.this))) return;

        String rcvId = listItemData.get("receiving_detail.receiving_id");
        String rcvSeq = listItemData.get("receiving_detail.sequence");
        String partId = listItemData.get("receiving_detail.item_id");
        MetrixCurrentKeysHelper.setKeyValue("receiving_detail", "receiving_id", rcvId);
        MetrixCurrentKeysHelper.setKeyValue("receiving_detail", "sequence", rcvSeq);
        MetrixCurrentKeysHelper.setKeyValue("receiving_detail", "item_id", partId);

        Intent intent = MetrixActivityHelper.createActivityIntent(ReceivingDetailList.this, ReceivingUnitPO.class);
        MetrixActivityHelper.startNewActivity(ReceivingDetailList.this, intent);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }

    public class RcvDetailCounter {
        public boolean mAllRDsAreZero;
        public boolean mAllRDsHaveExactQty;
        public String mListOfOverReceiptParts;
        public String mListOfUnderReceiptParts;

        public RcvDetailCounter() {
            mAllRDsAreZero = false;
            mAllRDsHaveExactQty = false;
            mListOfOverReceiptParts = "";
            mListOfUnderReceiptParts = "";
        }
    }
}
