package com.metrix.metrixmobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import static com.metrix.architecture.utilities.LogManager.*;

public class PurchaseDetailList extends MetrixActivity implements View.OnClickListener, SimpleRecyclerViewAdapter.OnItemClickListener, SimpleRecyclerViewAdapter.OnItemLongClickListener, TextWatcher {
    private RecyclerView recyclerView;
    private TextView mProcessButton;
    private SimpleRecyclerViewAdapter mAdapter;
    private static int mSelectedPosition = -1;
    private String mPurchaseOrderId = "";
    private ArrayList<String> mProcessOptions;
    private String mLastProcess, mLastProcessName;
    private String[] mFrom;
    private int[] mTo;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase_detail_list);
        mHandler = new Handler();
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
        recyclerView.setNestedScrollingEnabled(false);
    }

    /*
 * (non-Javadoc)
 *
 * @see android.app.Activity#onStart()
 */
    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.process, "Process"));

        mPurchaseOrderId = MetrixCurrentKeysHelper.getKeyValue("purchase_order", "purchase_order_id");

        if(MetrixStringHelper.isNullOrEmpty(mPurchaseOrderId))
            mPurchaseOrderId = getIntent().getExtras().getString("purchase_order_id");

        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        MetrixSkinManager.setFirstGradientBackground(findViewById(R.id.filter_bar), 0);
        mProcessButton = (TextView) findViewById(R.id.process);

        String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
        String actionBarTitle = AndroidResourceHelper.getMessage("PurchaseOrder1Arg", mPurchaseOrderId);
        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);

        populateList();
        setListeners();
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        mProcessButton = (TextView) findViewById(R.id.process);
        mProcessButton.setOnClickListener(this);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                MetrixActivityHelper.hideKeyboard(PurchaseDetailList.this);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    /**
     * This method is responsible for setting up the meta data which the
     * architecture uses for data binding and validation.
     */
    protected void defineForm() {
        MetrixTableDef purchaseOrderDef = null;
        if (this.mActivityDef != null) {
            purchaseOrderDef = new MetrixTableDef("purchase_order", this.mActivityDef.TransactionType);
            if (this.mActivityDef.Keys != null) {
                purchaseOrderDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
            }
        } else {
            purchaseOrderDef = new MetrixTableDef("purchase_order", MetrixTransactionTypes.SELECT);
            //Query metrixRowId based on purchase_order_id
            String metrixRowId = MetrixDatabaseManager.getFieldStringValue("select metrix_row_id from purchase_order where purchase_order_id="+mPurchaseOrderId);
            purchaseOrderDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, metrixRowId, double.class));
        }
        this.mFormDef = new MetrixFormDef(purchaseOrderDef);

        mFrom = new String[] { "purchase_detail.metrix_row_id", "purchase_detail.purchase_order_id", "purchase_detail.sequence", "purchase_detail.part_id", "purchase_detail.qty_ordered", "purchase_detail.unit_cost"};
        mTo = new int[] { R.id.purchase_detail__metrix_row_id, R.id.purchase_detail__purchase_order_id, R.id.purchase_detail__sequence,  R.id.purchase_detail__part_id, R.id.purchase_detail__qty_ordered, R.id.purchase_detail__unit_cost };
    }

    /**
     * Set the default values for views for this activity.
     */
    protected void defaultValues() {
        try {
            MetrixControlAssistant.setValue(mFormDef, mLayout, "purchase_order", "purchase_order_id", mPurchaseOrderId);
            calculateChange();
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    /**
     * Populate the purchase_order list with the person_id_buyer assigned to the user.
     */
    private void populateList() {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" where purchase_detail.purchase_order_id = '"+ mPurchaseOrderId+"'");


        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");

        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            whereClause.append(" limit " + maxRows);
        }

        String query = "select purchase_detail.metrix_row_id, purchase_detail.purchase_order_id, purchase_detail.sequence, purchase_detail.part_id, purchase_detail.unit_cost, purchase_detail.qty_ordered from purchase_detail "+whereClause.toString();
        MetrixCursor cursor = null;
        int rowCount = 0;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                rowCount = cursor.getCount();
                while (cursor.isAfterLast() == false) {
                    String metrixRowId = cursor.getString(0);
                    String poId = cursor.getString(1);
                    String sequence = cursor.getString(2);
                    String partId = cursor.getString(3);
                    String cost = cursor.getString(4);
                    String quantity = cursor.getString(5);

                    HashMap<String, String> row = new HashMap<String, String>();
                    row.put(mFrom[0], metrixRowId);
                    row.put(mFrom[1], poId);
                    row.put(mFrom[2], sequence);
                    row.put(mFrom[3], partId);
                    row.put(mFrom[4], cost);
                    row.put(mFrom[5], quantity);
                    //MetrixListScreenManager.generateRowFromCursor(this, cursor);
                    table.add(row);
                    cursor.moveToNext();
                }

                //table = MetrixListScreenManager.performScriptListPopulation(this, table);
            }
        } finally {
            MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new SimpleRecyclerViewAdapter(table, R.layout.purchase_detail_list_part_list_item, mFrom, mTo, new int[]{}, "purchase_detail.metrix_row_id");
            mAdapter.setClickListener(this);
            mAdapter.setLongClickListener(this);
            recyclerView.setAdapter(mAdapter);
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
                displayProcessOptions();
                break;
            default:
                super.onClick(v);
        }
    }

    private void displayProcessOptions() {
        if(unprocessedPO()) {
            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,AndroidResourceHelper.getMessage("DataPreviousUnsynced"));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(AndroidResourceHelper.getMessage("PurchaseOrderFunctionsTitle"));

        mProcessOptions = new ArrayList<String>();
        String purchaseStatus = MetrixDatabaseManager.getFieldStringValue("purchase_order", "approval_status", "purchase_order_id ="+mPurchaseOrderId);
        final String submit = AndroidResourceHelper.getMessage("SubmitPurchaseOrderOpt");
        final String approve = AndroidResourceHelper.getMessage("ApprovePurchaseOrderOpt");
        final String post = AndroidResourceHelper.getMessage("PostPOOpt");
        mProcessOptions.add(submit);
        if(purchaseStatus.toLowerCase().equals("submitted"))
            mProcessOptions.add(approve);

        if(purchaseStatus.toLowerCase().equals("approved"))
            mProcessOptions.add(post);

        String poStatus = MetrixDatabaseManager.getFieldStringValue("purchase_order", "po_status", "purchase_order_id =" + mPurchaseOrderId);
        final String cancel = AndroidResourceHelper.getMessage("CancelPOOpt");
        final String uncancel = AndroidResourceHelper.getMessage("UncancelPOOpt");
        if(poStatus.equals("CA"))
            mProcessOptions.add(uncancel);
        else
            mProcessOptions.add(cancel);

        CharSequence[] items = mProcessOptions.toArray(new CharSequence[mProcessOptions.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                mLastProcessName = mProcessOptions.get(pos);

                if (mLastProcessName.compareToIgnoreCase(submit) == 0) {
                    mLastProcess = submit;
                    submitPurchaseOrder();
                } else if (mLastProcessName.compareToIgnoreCase(approve) == 0) {
                    mLastProcess = approve;
                    approvePurchaseOrder();
                }
                else if (mLastProcessName.compareToIgnoreCase(post) == 0) {
                    mLastProcess = post;
                    postPurchaseOrder();
                }
                else if (mLastProcessName.compareToIgnoreCase(cancel) == 0) {
                    mLastProcess = cancel;
                    cancelPurchaseOrder();
                }
                else if (mLastProcessName.compareToIgnoreCase(uncancel) == 0) {
                    mLastProcess = uncancel;
                    uncancelPurchaseOrder();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean unprocessedPO () {
        boolean unprocessed = false;
        mPurchaseOrderId = MetrixCurrentKeysHelper.getKeyValue("purchase_order", "purchase_order_id");

        if(MetrixStringHelper.isNegativeValue(mPurchaseOrderId)) {
            return true;
        }

        ArrayList<Hashtable<String, String>> sequences = MetrixDatabaseManager.getFieldStringValuesList("select sequence from purchase_detail where purchase_order_id ="+mPurchaseOrderId);

        if(sequences == null)
            return false;

        for(Hashtable<String, String> row : sequences) {
            if(row.containsKey("sequence")) {
                String seq = row.get("sequence");
                if(MetrixStringHelper.isNegativeValue(seq))
                    return true;
            }
        }

        return unprocessed;
    }

    private void submitPurchaseOrder() {
        changePurchaseOrder("SUBMITTED");
    }

    private void approvePurchaseOrder() {
        changePurchaseOrder("APPROVED");
    }

    private void changePurchaseOrder(String status) {
        MetrixSqlData purchaseOrderData;

        String metrixRowId = MetrixDatabaseManager.getFieldStringValue("purchase_order", "metrix_row_id", "purchase_order_id="+mPurchaseOrderId);
        DataField purchaseOrderIdField, approvalStatusField;
        purchaseOrderData = new MetrixSqlData("purchase_order", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ metrixRowId);
        purchaseOrderIdField = new DataField("purchase_order_id", mPurchaseOrderId);
        DataField rowIdField = new DataField("metrix_row_id", metrixRowId);
        approvalStatusField = new DataField("approval_status", status);

        purchaseOrderData.dataFields.add(rowIdField);
        purchaseOrderData.dataFields.add(purchaseOrderIdField);
        purchaseOrderData.dataFields.add(approvalStatusField);

        ArrayList<MetrixSqlData> purchaseTrans = new ArrayList<MetrixSqlData>();
        purchaseTrans.add(purchaseOrderData);

        MetrixTransaction transactionInfo = new MetrixTransaction();

        boolean successful = MetrixUpdateManager.update(purchaseTrans, true, transactionInfo, AndroidResourceHelper.getMessage("PurchaseOrder"), this);

        if (!successful) {
            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,AndroidResourceHelper.getMessage("DataErrorOnUpload"));
            return;
        }
    }

    private void postPurchaseOrder() {
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("purchase_order_id", mPurchaseOrderId);
        MetrixPerformMessage performMessage = new MetrixPerformMessage("perform_review_and_post_purchase_order");
        performMessage.parameters = params;
        boolean successful = performMessage.save();

        if (!successful) {
            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("DataErrorOnUpload"));
            return;
        }
        else {
            Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseOrderList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivityAndFinish(this, intent);
        }
    }

    private void cancelPurchaseOrder() {
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("purchase_order_id", mPurchaseOrderId);
        MetrixPerformMessage performMessage = new MetrixPerformMessage("perform_cancel_purchase_order");
        performMessage.save();
    }

    private void uncancelPurchaseOrder() {
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("purchase_order_id", mPurchaseOrderId);
        MetrixPerformMessage performMessage = new MetrixPerformMessage("perform_uncancel_purchase_order");
        performMessage.save();
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> {
            if (!PurchaseDetailList.this.isDestroyed())
                this.populateList();
        }, 500);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    private void calculateChange() {
        double totalCost = 0;
        double unitCost = 0;
        double qty = 0;

        String currentSequence = MetrixControlAssistant.getValue(this.mFormDef,this.mLayout,  "purchase_detail", "sequence");
        String currentUnitCost = MetrixControlAssistant.getValue(this.mFormDef,this.mLayout,  "purchase_detail", "unit_cost");
        String currentQuantity = MetrixControlAssistant.getValue(this.mFormDef,this.mLayout,  "purchase_detail", "qty_ordered");

        if (MetrixStringHelper.isNullOrEmpty(currentUnitCost) || MetrixStringHelper.isNullOrEmpty(currentQuantity)
                || !MetrixStringHelper.isDouble(currentUnitCost) || !MetrixStringHelper.isDouble(currentQuantity))
        {
            unitCost = 0;
            qty = 0;
        }
        else
        {
            unitCost = Double.valueOf(currentUnitCost);
            qty = Double.valueOf(currentQuantity);
        }

        if (MetrixStringHelper.isNullOrEmpty(currentSequence))
        {
            totalCost = this.calculateTotalCost() + unitCost * qty;
        }
        else
        {
            totalCost = this.calculateTotalCostWithoutCurrentRecord(currentSequence) + unitCost * qty;
        }
        MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_cost_po", ""+totalCost);
    }

    private double calculateTotalCost() {
        double total = 0.0;
        String query = "select unit_cost, qty_ordered from purchase_detail where purchase_order_id = "
                + MetrixControlAssistant.getValue(mFormDef, mLayout, "purchase_order", "purchase_order_id");

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int i = 1;
                while (cursor.isAfterLast() == false) {
                    double cost = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(1), Locale.US));
                    total = total + cost * quantity;

                    cursor.moveToNext();
                    i = i + 1;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return MetrixFloatHelper.round(total, 2);
    }

    private double calculateTotalCostWithoutCurrentRecord(String sequence) {
        double total = 0.0;
        String query = "select unit_cost, qty_ordered from purchase_detail where purchase_order_id = "
                + MetrixControlAssistant.getValue(mFormDef, mLayout, "purchase_detail", "purchase_order_id")+" and sequence <> "+sequence;

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int i = 1;
                while (cursor.isAfterLast() == false) {
                    double cost = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(1), Locale.US));
                    total = total + cost * quantity;

                    cursor.moveToNext();
                    i = i + 1;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return total;
    }


    @Override
    public void onSimpleRvItemClick(int position, Object item, View view) {
        mSelectedPosition = position;

        final DialogInterface.OnClickListener modifyListener = (dialog, pos) -> {
            try {
                Object listItem = mAdapter.getData().get(mSelectedPosition);
                @SuppressWarnings("unchecked")
                HashMap<String, String> selectedItem = (HashMap<String, String>) listItem;

                Intent intent = MetrixActivityHelper.createActivityIntent(PurchaseDetailList.this, PurchaseDetail.class, MetrixTransactionTypes.UPDATE,
                        "metrix_row_id", selectedItem.get("purchase_detail.metrix_row_id"));
                String poId = MetrixDatabaseManager.getFieldStringValue("purchase_detail", "purchase_order_id",
                        "metrix_row_id=" + selectedItem.get("purchase_detail.metrix_row_id"));
                String sequence = MetrixDatabaseManager.getFieldStringValue("purchase_detail", "sequence",
                        "metrix_row_id=" +selectedItem.get("purchase_detail.metrix_row_id"));

                intent.putExtra("purchase_order_id", poId);
                intent.putExtra("sequence", sequence);
                MetrixActivityHelper.startNewActivity(PurchaseDetailList.this, intent);
            } catch (Exception e) {
                getInstance().error(e);
            }
        };

        final DialogInterface.OnClickListener deleteListener = (dialog, pos) -> {
            try {
                @SuppressWarnings("unchecked")
                HashMap<String, String> selectedItem = (HashMap<String, String>) mAdapter.getData().get(mSelectedPosition);

                String metrixRowId = selectedItem.get("purchase_detail.metrix_row_id");
                String poId = MetrixDatabaseManager.getFieldStringValue("purchase_detail", "purchase_order_id",
                        "metrix_row_id=" + metrixRowId);
                String sequence = MetrixDatabaseManager.getFieldStringValue("purchase_detail", "sequence",
                        "metrix_row_id=" + metrixRowId);

                Hashtable<String, String> primaryKeys = new Hashtable<String, String>();
                primaryKeys.put("purchase_order_id",  poId);
                primaryKeys.put("sequence",  sequence);

                MetrixUpdateManager.delete(PurchaseDetailList.this, "purchase_detail", metrixRowId, primaryKeys, AndroidResourceHelper.getMessage("PurchaseDetail"), new MetrixTransaction());
                mAdapter.getData().remove(mSelectedPosition);
                mAdapter.notifyItemRemoved(mSelectedPosition);
            } catch (Exception e) {
                getInstance().error(e);
            }
        };

        MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("PurchaseDetail"), modifyListener, deleteListener, this);
    }

    @Override
    public void onSimpleRvItemLongClick(int position, Object item, View view) {
        mSelectedPosition = position;
    }
}

