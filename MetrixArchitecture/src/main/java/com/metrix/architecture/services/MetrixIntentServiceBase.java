package com.metrix.architecture.services;

import android.content.Intent;
import android.app.IntentService;
import android.app.Service;

abstract class MetrixIntentServiceBase extends IntentService {
	public static String tag = "MetrixIntentServiceBase";

	protected abstract void handleBroadcastIntent(Intent broadcastIntent);

	public MetrixIntentServiceBase(String name) {
		super(name);
	}

	/*
	 * This method can be invoked under two circumstances 1. When a broadcast
	 * receiver issues a "startService" 2. when android restarts it due to
	 * pending "startService" intents.
	 * 
	 * In case 1, the broadcast receiver has already setup the
	 * "MetrixServiceManager". In case 2, do the same.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// Set up the MetrixServiceManager
		// The setup is capable of getting called multiple times.
		MetrixServiceManager.setup(this.getApplicationContext());

		// It is possible that there are more than one service of this type is
		// running.
		// Knowing the number will allow us to clean up the locks in onDestroy.
		MetrixServiceManager.registerServiceClient();
	}

	@Override
	public int onStartCommand(Intent intent, int flag, int startId) {
		// Call the IntetnService "onStart"
		super.onStart(intent, startId);

		// Tell the MetrixServiceManager there is a new caller
		MetrixServiceManager.enterService();

		// mark this as non sticky
		// Means: Don't restart the service if there are no pending intents.
		return Service.START_NOT_STICKY;
	}

	/*
	 * Note that this method call runs in a secondary thread setup by the
	 * IntentService.
	 * 
	 * Override this method from IntentService. Retrieve the original broadcast
	 * intent. Call the derived class to handle the broadcast intent. finally
	 * tell the MetrixServiceManager that you are done. if this is the last
	 * caller then the lock will be released.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Intent broadcastIntent = intent
					.getParcelableExtra("original_intent");
			handleBroadcastIntent(broadcastIntent);
		} finally {
			MetrixServiceManager.leaveService();
		}
	}

	/*
	 * If Android reclaims this process, this method will release the lock
	 * irrespective of how many callers there are.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		MetrixServiceManager.unregisterServiceClient();
	}
}
