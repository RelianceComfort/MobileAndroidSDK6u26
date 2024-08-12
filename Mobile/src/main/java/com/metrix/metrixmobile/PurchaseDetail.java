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
import com.metrix.metrixmobile.system.MetrixActivity;
import com.metrix.metrixmobile.system.SyncServiceMonitor;

import java.util.ArrayList;
import java.util.List;

public class PurchaseDetail extends MetrixActivity implements View.OnClickListener, View.OnFocusChangeListener {
//    private Button mViewPreviousEntriesButton;
    private FloatingActionButton mSaveButton, mNextButton;
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

        mPurchaseOrderId = getIntent().getExtras().getString("purchase_order_id");

        setContentView(R.layout.purchase_detail);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        super.onStart();

        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
        String actionBarTitle = AndroidResourceHelper.getMessage("PurchaseOrder1Arg", mPurchaseOrderId);
        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);
 //       calculateChange();
//        displayPreviousCount();
    }

    protected void displayPreviousCount() {
        int count = MetrixDatabaseManager.getCount("purchase_detail", "purchase_detail.purchase_order_id = '" + mPurchaseOrderId + "'");

        try {
            MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mSaveButton = (FloatingActionButton) findViewById(R.id.save);
        mNextButton = (FloatingActionButton) findViewById(R.id.next);
//        mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);

        mFABList.add(mSaveButton);
        mFABList.add(mNextButton);

        mSaveButton.setImageResource(R.drawable.ic_save_white_24dp);

        mSaveButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
 //       mViewPreviousEntriesButton.setOnClickListener(this);

        if (this.mActivityDef != null && this.mActivityDef.TransactionType == MetrixTransactionTypes.UPDATE) {
            this.displaySaveButtonOnAddNextBar();
        }

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

    protected void displaySaveButtonOnAddNextBar() {
        FloatingActionButton correctErrorsButton = (FloatingActionButton) findViewById(R.id.correct_error);
        FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
        FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
//        TextView updateButton = (TextView) findViewById(R.id.update);
        ViewGroup viewPreviousBar = (ViewGroup) findViewById(R.id.view_previous_entries_bar);

        if (addButton != null) {
            MetrixControlAssistant.setButtonVisibility(addButton, View.GONE);
        }

        if (correctErrorsButton != null) {
            MetrixControlAssistant.setButtonVisibility(correctErrorsButton, View.GONE);
        }

//        if (viewPreviousBar != null) {
//            viewPreviousBar.setVisibility(View.GONE);
//        }

        if(nextButton != null)
            MetrixControlAssistant.setButtonVisibility(nextButton, View.VISIBLE);
    }


    /**
     * Set the default values for views for this activity.
     */
    protected void defaultValues() {
        if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
            try {
                MetrixControlAssistant.setValue(mFormDef, mLayout, "purchase_detail", "purchase_order_id", mPurchaseOrderId);
                MetrixControlAssistant.setValue(mFormDef, mLayout, "purchase_detail", "qty_ordered", "1");
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
        MetrixTableDef purchaseDetailDef = null;
        if (this.mActivityDef != null) {
            purchaseDetailDef = new MetrixTableDef("purchase_detail", this.mActivityDef.TransactionType);
            if (this.mActivityDef.Keys != null) {
                purchaseDetailDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
            }
        } else {
            purchaseDetailDef = new MetrixTableDef("purchase_detail", MetrixTransactionTypes.INSERT);
        }
        this.mFormDef = new MetrixFormDef(purchaseDetailDef);
    }

 /*
 * (non-Javadoc)
 *
 * @see
 * android.view.View.OnFocusChangeListener#onFocusChange(android.view.View,
 * boolean)
 */


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
            case R.id.save:
                MetrixTransaction transactionInfo = new MetrixTransaction();
                mPurchaseOrderId = MetrixCurrentKeysHelper.getKeyValue("purchase_order", "purchase_order_id");
                MetrixControlAssistant.setValue(this.mFormDef, this.mLayout, "purchase_detail", "purchase_order_id", mPurchaseOrderId);

                MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, true, AndroidResourceHelper.getMessage("PurchaseDetail"));
                if (result == MetrixSaveResult.SUCCESSFUL) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetail.class);
                    intent.putExtra("purchase_order_id", mPurchaseOrderId);
                    MetrixActivityHelper.startNewActivity(this, intent);
                }

                break;

            case R.id.next:
                if (anyOnStartValuesChanged()) {
                    mPurchaseOrderId = MetrixCurrentKeysHelper.getKeyValue("purchase_order", "purchase_order_id");
                    MetrixControlAssistant.setValue(this.mFormDef, this.mLayout, "purchase_detail", "purchase_order_id", mPurchaseOrderId);

                    transactionInfo = new MetrixTransaction();
                    MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("PurchaseDetail"));

                    Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetailList.class);
                    intent.putExtra("purchase_order_id", mPurchaseOrderId);
                    MetrixActivityHelper.startNewActivity(this, intent);
                }
                else {
                    Intent intent = MetrixActivityHelper.createActivityIntent(this, PurchaseDetailList.class);
                    intent.putExtra("purchase_order_id", mPurchaseOrderId);
                    MetrixActivityHelper.startNewActivity(this, intent);
                }
                break;
            case R.id.correct_error:
                transactionInfo = new MetrixTransaction();
                MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, AndroidResourceHelper.getMessage("PurchaseDetail"));
                break;
            default:
                super.onClick(v);
        }
    }
}