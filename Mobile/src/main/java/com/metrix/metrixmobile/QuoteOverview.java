package com.metrix.metrixmobile;

import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.QuoteActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by RaWiLK on 8/24/2016.
 */
public class QuoteOverview extends QuoteActivity implements View.OnClickListener{

    private FloatingActionButton mSaveButton, mNextButton, mAddButton;
    private Boolean backPressed = false;
    private boolean mIsUpdate;
    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    public void onCreate(Bundle savedInstanceState) {
        String currentQuoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
        if (!MetrixStringHelper.isNullOrEmpty(currentQuoteId))
            this.mIsUpdate = true;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.quote_overview);
    }

    public void onStart() {
        backPressed = false;
        String backCached = (String) MetrixPublicCache.instance.getItem("BackbuttonPressed");
        if (!MetrixStringHelper.isNullOrEmpty(backCached) && backCached.equalsIgnoreCase("Y"))
            backPressed = true;

        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        try {
            if (mFABList == null)
                mFABList = new ArrayList<FloatingActionButton>();
            else
                mFABList.clear();

            if(!mHandlingErrors) {
                mAddButton = (FloatingActionButton) findViewById(R.id.save);
                mSaveButton = (FloatingActionButton) findViewById(R.id.update);
                mNextButton = (FloatingActionButton) findViewById(R.id.next);

                if (mAddButton != null) {
                    mAddButton.setOnClickListener(this);
                    MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
                }

                if (mSaveButton != null) {
                    mSaveButton.setOnClickListener(this);
                    if (mIsUpdate) {
                        MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
                        mFABList.add(mSaveButton);
                    }
                    else
                        MetrixControlAssistant.setButtonVisibility(mSaveButton, View.GONE);
                }

                if (mNextButton != null) {
                    mNextButton.setOnClickListener(this);
                    mFABList.add(mNextButton);
                }
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

        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    /**
     * This method is responsible for setting up the meta data which the
     * architecture uses for data binding and validation.
     */
    protected void defineForm() {
        ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

        MetrixTableDef quoteDef = null;
        if(mIsUpdate) {
            quoteDef = new MetrixTableDef("quote", MetrixTransactionTypes.UPDATE);
            quoteDef.constraints.add(new MetrixConstraintDef("quote_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"),
                    double.class));
        }else
            quoteDef = new MetrixTableDef("quote", MetrixTransactionTypes.INSERT);

        MetrixRelationDef quoteStatusRelationDef = new MetrixRelationDef("quote", "quote_status", "quote_status", MetrixRelationOperands.LEFT_OUTER);
        MetrixTableDef quoteStatusDef = new MetrixTableDef("quote_status", MetrixTransactionTypes.SELECT, quoteStatusRelationDef);

        MetrixRelationDef quotePlaceRelationDef = new MetrixRelationDef("quote", "place_id", "place_id", MetrixRelationOperands.LEFT_OUTER);
        MetrixTableDef quotePlaceDef = new MetrixTableDef("place", MetrixTransactionTypes.SELECT, quotePlaceRelationDef);

        MetrixRelationDef quoteAddressRelationDef = new MetrixRelationDef("quote", "address_id", "address_id", MetrixRelationOperands.LEFT_OUTER);
        MetrixTableDef quoteAddressDef = new MetrixTableDef("address", MetrixTransactionTypes.SELECT, quoteAddressRelationDef);

        tableDefs.add(quoteDef);
        tableDefs.add(quoteStatusDef);
        tableDefs.add(quotePlaceDef);
        tableDefs.add(quoteAddressDef);

        this.mFormDef = new MetrixFormDef(tableDefs);
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
            case R.id.update:
                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("Quote"));
                if (saveResult == MetrixSaveResult.SUCCESSFUL) {
                    reloadActivity();
                }
                break;
            case R.id.next:
                String currentTransactionType = getCurrentTransactionType();
                String currentRequestTemplateId = MetrixControlAssistant.getValue(mFormDef, mLayout, "quote", "request_template_id");

                if(MetrixStringHelper.valueIsEqual(currentTransactionType, "INSERT"))
                {
                    if (anyOnStartValuesChanged())
                    {
                        String quoteId = MetrixControlAssistant.getValue(mFormDef, mLayout,"quote", "quote_id");
                        String quoteVersion = MetrixControlAssistant.getValue(mFormDef, mLayout,"quote", "quote_version");
                        String quoteType = MetrixControlAssistant.getValue(mFormDef, mLayout, "quote", "quote_type");

                        MetrixCurrentKeysHelper.setKeyValue("quote", "quote_id", quoteId);
                        MetrixCurrentKeysHelper.setKeyValue("quote", "quote_version", quoteVersion);
                        MetrixWorkflowManager.setCurrentWorkflowName(this, String.format("Quote~%s", quoteType));

                        MetrixControlAssistant.setValue(mFormDef, mLayout, "quote", "quote_owner", User.getUser().personId);

                        transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                        if (!MetrixStringHelper.isNullOrEmpty(currentRequestTemplateId))
                        {
                            MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("QuoteOverview"));
                            if(result == MetrixSaveResult.SUCCESSFUL)
                            {
                                if(SettingsHelper.getSyncPause(mCurrentActivity))
                                {
                                    SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                                    if(syncPauseAlertDialog != null)
                                    {
                                        syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                            @Override
                                            public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                                startHandleRequestTemplateProcess();
                                            }
                                        });
                                    }
                                }
                                else
                                    startHandleRequestTemplateProcess();
                            }
                        }
                        else
                            MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, true, AndroidResourceHelper.getMessage("QuoteOverview"), true);
                    }
                    else
                        MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("QuoteOverviewAllFieldsRequired"));
                }
                else if (MetrixStringHelper.valueIsEqual(currentTransactionType, "UPDATE"))
                {
                    if (anyOnStartValuesChanged() && !quoteIsComplete())
                    {
                        transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                        MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("QuoteOverview"), true);
                    }
                    else
                        MetrixWorkflowManager.advanceWorkflow(this);
                }
                break;
            default:
                super.onClick(v);
        }
    }

    private void startHandleRequestTemplateProcess() {
        MetrixPublicCache.instance.addItem("RequestTemplateIdSelected", "Y");

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
                String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

                if (ping(baseUrl, remote) == false) {
                    navigateToNextScreen();
                    return;
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("QuoteOverviewPleaseWait"));
            }
        });

        thread.start();
    }

    private void navigateToNextScreen() {
        MobileApplication.stopSync(mCurrentActivity);
        MobileApplication.startSync(mCurrentActivity);

        MetrixPublicCache.instance.removeItem("RequestTemplateIdSelected");
        MetrixWorkflowManager.advanceWorkflow(mCurrentActivity);
        finish();
    }

    private String getCurrentTransactionType(){
        String currentTransactionType = null;
        MetrixFormDef currentFormDef = this.mFormDef;
        if(currentFormDef != null && currentFormDef.tables != null){
            MetrixTableDef tableDef = currentFormDef.tables.get(0);
            currentTransactionType = tableDef.transactionType.toString();
        }
        return currentTransactionType;
    }

    @Override
    protected void processPostListener(Global.ActivityType activityType, String message) {
        if (activityType == Global.ActivityType.Download) {
            if (!MetrixStringHelper.isNullOrEmpty(message) && message.contains("{\"update_quote_result\":")) {
                if (MetrixPublicCache.instance.containsKey("RequestTemplateIdSelected")) {
                    String value = (String) MetrixPublicCache.instance.getItem("RequestTemplateIdSelected");
                    if (!MetrixStringHelper.isNullOrEmpty(value) && MetrixStringHelper.valueIsEqual(value, "Y")) {
                        mUIHelper.dismissLoadingDialog();
                        navigateToNextScreen();
                    }
                }
            }
        }

        super.processPostListener(activityType, message);
    }
}
