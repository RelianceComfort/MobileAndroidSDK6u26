package com.metrix.architecture.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by RaWiLK on 8/23/2016.
 */
public class SyncPauseAlertDialog  extends AlertDialog {

    private OnSyncPauseAlertButtonClickListner mOnSyncPauseAlertButtonClickListner;

    public SyncPauseAlertDialog(final Context context) {
        super(context);
        this.setButton(AndroidResourceHelper.getMessage("YesButton"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SettingsHelper.saveSyncPause(context, false);

                if(mOnSyncPauseAlertButtonClickListner != null) {
                    try {
                        mOnSyncPauseAlertButtonClickListner.OnSyncPauseAlertButtonClick(dialog, which);
                    } catch (Exception e) {
                        LogManager.getInstance(context).error(e);
                    }
                }
            }
        });
        this.setButton2(AndroidResourceHelper.getMessage("NoButton"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    public void setOnSyncPauseAlertButtonClickListner(OnSyncPauseAlertButtonClickListner onSyncPauseAlertButtonClickListner){
        this.mOnSyncPauseAlertButtonClickListner = onSyncPauseAlertButtonClickListner;
    }

    public interface OnSyncPauseAlertButtonClickListner{
        void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) throws Exception;
    }
}