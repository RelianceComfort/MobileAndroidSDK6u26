package com.metrix.metrixmobile;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder extends MetrixActivity implements View.OnClickListener, View.OnFocusChangeListener {
    private FloatingActionButton mNextButton;
    private String mPurchaseOrderId;
    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.purchase_order);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        mPurchaseOrderId = getIntent().getExtras().getString("purchase_order_id");
        mLayout = (ViewGroup) findViewById(R.id.table_layout);

        if(MetrixStringHelper.isNullOrEmpty(mPurchaseOrderId)) {
            String pageMode = getIntent().getExtras().getString("page_mode");

            if(MetrixStringHelper.isNullOrEmpty(pageMode)||(!MetrixStringHelper.isNullOrEmpty(pageMode)&& pageMode.equalsIgnoreCase("UPDATE"))){
                mPurchaseOrderId =  MetrixCurrentKeysHelper.getKeyValue("purchase_order", "purchase_order_id");
                this.defineForm();
            }
        }

        super.onStart();

        String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
        String actionBarTitle = "";
        if (!MetrixStringHelper.isNullOrEmpty(mPurchaseOrderId))
            actionBarTitle = AndroidResourceHelper.getMessage("PurchaseOrder1Arg", mPurchaseOrderId);
        else
            actionBarTitle = String.format("%s", AndroidResourceHelper.getMessage("PurchaseOrder"));

        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);
 //       displayPreviousCount();
    }

    /**
     * Define the listeners for this activity.
     */

    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mNextButton = (FloatingActionButton) findViewById(R.id.next);
//        mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);
        mNextButton.setOnClickListener(this);
 //       mViewPreviousEntriesButton.setOnClickListener(this);

        mFABList.add(mNextButton);

        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
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
        if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
            try {
                MetrixControlAssistant.setValue(mFormDef, mLayout, "purchase_order", "person_id_buyer", User.getUser().personId);
                MetrixControlAssistant.setValue(mFormDef, mLayout, "purchase_order", "place_id_ship_to", MetrixDatabaseManager.getFieldStringValue("select person_place.place_id from person_place where person_place.place_relationship = 'FOR_STOCK' and person_place.person_id = '"+User.getUser().personId+"'"));
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        }
    }

    /**
     * This method is responsible for setting up the meta data which the
     * architecture uses for data binding and validation.
     */
    protected void defineForm() {
        MetrixTableDef purchaseOrderDef = null;
        String metrixRowId = "";

        if(!MetrixStringHelper.isNullOrEmpty(mPurchaseOrderId))
            metrixRowId = MetrixDatabaseManager.getFieldStringValue("select metrix_row_id from purchase_order where purchase_order_id ="+mPurchaseOrderId);

        if (this.mActivityDef != null) {
            purchaseOrderDef = new MetrixTableDef("purchase_order", this.mActivityDef.TransactionType);

            if (this.mActivityDef.Keys != null) {
                purchaseOrderDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, metrixRowId, double.class));
            }
        } else {
            if(MetrixStringHelper.isNullOrEmpty(mPurchaseOrderId))
                purchaseOrderDef = new MetrixTableDef("purchase_order", MetrixTransactionTypes.INSERT);
            else {
                purchaseOrderDef = this.mFormDef.tables.get(0);
                purchaseOrderDef.transactionType =  MetrixTransactionTypes.UPDATE;
                purchaseOrderDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, metrixRowId, double.class));
                this.mFormDef.tables.remove(0);
            }
        }
        this.mFormDef = new MetrixFormDef(purchaseOrderDef);
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
            case R.id.next:
                if (anyOnStartValuesChanged()) {
                    String purchaseOrderId = MetrixControlAssistant.getValue(mFormDef, mLayout, "purchase_order", "purchase_order_id");
                    MetrixTransaction transactionInfo = new MetrixTransaction(purchaseOrderId, "purchase_order_id");

                    MetrixCurrentKeysHelper.setKeyValue("purchase_order", "purchase_order_id", purchaseOrderId);
                    MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("PurchaseOrder"));

                    if (result == MetrixSaveResult.SUCCESSFUL) {
                        getIntent().putExtra("page_mode", "UPDATE"); // update the current page mode to update
                        Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetail.class);
                        intent.putExtra("purchase_order_id", purchaseOrderId);

                        MetrixActivityHelper.startNewActivity(this, intent);
                    }
                }
                else {
                    if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
                        if(this.mActivityDef == null) {
                            String pageMode = getIntent().getExtras().getString("page_mode");

                            if(!MetrixStringHelper.isNullOrEmpty(pageMode) && pageMode.equalsIgnoreCase("UPDATE")) {
                                String purchaseOrderId = MetrixCurrentKeysHelper.getKeyValue("purchase_order", "purchase_order_id");
                                Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetail.class);
                                intent.putExtra("purchase_order_id", purchaseOrderId);

                                MetrixActivityHelper.startNewActivity(this, intent);
                                return;
                            }
                        }

                        String purchaseOrderId = MetrixControlAssistant.getValue(mFormDef, mLayout, "purchase_order", "purchase_order_id");
                        MetrixTransaction transactionInfo = new MetrixTransaction(purchaseOrderId, "purchase_order_id");

                        MetrixCurrentKeysHelper.setKeyValue("purchase_order", "purchase_order_id", purchaseOrderId);
                        MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("PurchaseOrder"));

                        if (result == MetrixSaveResult.SUCCESSFUL) {
                            getIntent().putExtra("page_mode", "UPDATE"); // update the current page mode to update
                            Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetail.class);
                            intent.putExtra("purchase_order_id", purchaseOrderId);

                            MetrixActivityHelper.startNewActivity(this, intent);
                        }
                    }
                    else {
                        String purchaseOrderId = MetrixControlAssistant.getValue(mFormDef, mLayout, "purchase_order", "purchase_order_id");
                        getIntent().putExtra("page_mode", "UPDATE"); // update the current page mode to update
                        Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetail.class);
                        intent.putExtra("purchase_order_id", purchaseOrderId);

                        MetrixActivityHelper.startNewActivity(this, intent);
                    }
                }

                break;
            default:
                super.onClick(v);
        }
    }
}