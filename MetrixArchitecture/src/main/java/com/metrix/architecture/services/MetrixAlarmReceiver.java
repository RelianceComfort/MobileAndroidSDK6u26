package com.metrix.architecture.services;

import java.util.List;

import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MetrixAlarmReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		boolean appIsRunning = false;

		LogManager.getInstance().debug("Sync alarm received");

		ActivityManager am = (ActivityManager)context.getSystemService(Activity.ACTIVITY_SERVICE);
		if (am != null) {
			List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);
	    	for (ActivityManager.RunningTaskInfo info : tasks) {
	    		String pkgName = info.baseActivity.getPackageName();
	    		if (MetrixStringHelper.valueIsEqual(pkgName, context.getPackageName())) {
	    			appIsRunning = true;
	    			break;
	    		}
	    	}
		}
		
		if (appIsRunning) {
			// Calling sync service when the alarm notified the receiver
			MetrixServiceManager.setup(context);

			User user = User.getUser();
			if (user != null) {
				Intent serviceIntent = new Intent(context, getLongRunningServiceClass());
				context.startService(serviceIntent);
				LogManager.getInstance().debug("Sync service started");
			}	
		} else {
			MetrixLocationAssistant.stopLocationManager();
			MobileApplication.stopSync(context);
			Intent serviceIntent = new Intent(context, getLongRunningServiceClass());
			context.stopService(serviceIntent);
		}
	}

	public void onReceive(Context context) {
		onReceive(context, null);
	}

	protected Class<?> getLongRunningServiceClass() {
		return MetrixIntentService.class;
	}
}
