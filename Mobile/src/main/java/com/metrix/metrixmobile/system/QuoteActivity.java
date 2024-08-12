package com.metrix.metrixmobile.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.QuoteMetrixActionView;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MobileGlobal;

public class QuoteActivity extends MetrixActivity {
    protected boolean mDisableContextMenu = false;

    public void onStart() {
        super.onStart();
        this.setupActionBar();

        displayPreviousCount();
    }

    public void setupActionBar() {
        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            if (this.mHandlingErrors) {
                actionBarTitle.setText(AndroidResourceHelper.getMessage("ErrorActionBarTitle1Arg", MobileGlobal.mErrorInfo.transactionDescription));
            } else {
                String quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
                if (!MetrixStringHelper.isNullOrEmpty(quoteId))
                {
                    String placeId = MetrixDatabaseManager.getFieldStringValue("quote", "place_id", "quote_id = " + quoteId);
                    String name = MetrixDatabaseManager.getFieldStringValue("place", "name", "place_id = '" + placeId + "'");
                    actionBarTitle.setText(AndroidResourceHelper.getMessage("QuoteActionBarTitleFormat", quoteId, name));
                }
                else
                    actionBarTitle.setText(AndroidResourceHelper.getMessage("GenerateQuote"));
            }
        }
    }

    @Override
    public void onClick(View v) {
        MetrixControlAssistant.setValue(this.mFormDef, this.mLayout, this.mFormDef.getTableNames().get(0), "quote_id", MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"));

        switch (v.getId()) {
            case R.id.correct_error:
                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, "");
                break;
            default:
                super.onClick(v);
        }
    }

    protected boolean quoteIsAccepted() {
        try {
            if (MetrixStringHelper.isNullOrEmpty(MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"))) {
                return false;
            }

            if (MetrixStringHelper.isNullOrEmpty(MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version"))) {
                return false;
            }

            String quoteStatus = MetrixDatabaseManager.getFieldStringValue("quote", "quote_status",
                    "quote_id = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id")
                            + " and quote_version = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version"));

            String status = MetrixDatabaseManager.getFieldStringValue("quote_status", "quote_internal_status",
                    "quote_status = '" + quoteStatus + "'");

            if ((status.compareToIgnoreCase("CA") == 0) || (status.compareToIgnoreCase("CO") == 0)
                    || (status.compareToIgnoreCase("CL") == 0) || (status.compareToIgnoreCase("RDY") == 0)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
        return super.onMetrixActionViewItemClick(menuItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        QuoteMetrixActionView.onCreateMetrixActionView(this, menu);
        boolean createOption = super.onCreateOptionsMenu(menu);
        return createOption;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        QuoteMetrixActionView.onMetrixActionMenuItemSelected(this, item);
        boolean itemSelected = super.onOptionsItemSelected(item);
        return itemSelected;
    }

    protected boolean quoteIsComplete() {
        try {
            if(MetrixStringHelper.isNullOrEmpty(MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"))){
                return false;
            }

            String quoteStatus = MetrixDatabaseManager.getFieldStringValue("quote", "quote_status",
                    "quote_id = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"));

            String internalStatus = MetrixDatabaseManager.getFieldStringValue("quote_status", "quote_internal_status",
                    "quote_status = '" + quoteStatus + "'");

            if ((internalStatus.compareToIgnoreCase("CA") == 0) || (internalStatus.compareToIgnoreCase("CO") == 0)
                    || (internalStatus.compareToIgnoreCase("CL") == 0)) {
                return true;
            } else {
                return false;
            }
        }
        catch(Exception ex){
            return false;
        }
    }

    @Override
    protected void bindService() {
        bindService(new Intent(this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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

    protected ServiceConnection mConnection = new ServiceConnection() {
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
                    processPostListener(activityType, message);
                }
            });
        }
    };
}

