package com.metrix.metrixmobile.system;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.R; //com.metrix.architecture.R;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.metrixmobile.global.MobileGlobal;

public class ScheduleActivity extends MetrixActivity {

	/**
	 * @param button
	 */
	public void registerContextMenu(Button button) {
		registerForContextMenu(button);
	}

	public void onStart() {
		super.onStart();
		this.setupActionBar();
		displayPreviousCount();
	}

	protected void displayPreviousCount() {

	}

	public void setupActionBar() {
		TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
		if (actionBarTitle != null) {
			if (this.mHandlingErrors) {
				actionBarTitle.setText(AndroidResourceHelper.getMessage("ErrorActionBarTitle1Arg", MobileGlobal.mErrorInfo.transactionDescription));
			} else {
				actionBarTitle.setText(AndroidResourceHelper.getMessage("ScheduleJob"));
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.correct_error:
			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("request", "request_id");

			MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, "");
			break;
		case R.id.up:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		default:
			super.onClick(v);
		}
	}
}
