package com.metrix.architecture.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;

/*
 * Uses IntentService as the base class
 * to make this work on a separate thread.
 */
public class MetrixIntentService extends MetrixIntentServiceBase {
	public static String tag = "MetrixIntentService";
	public String mBaseUrl;
	private MetrixRemoteExecutor mRemoteExecutor;
	private MetrixSyncManager mSyncManager;

	private Map<IPostListener, Caller> mCallers = new ConcurrentHashMap<IPostListener, Caller>();

	// Required by IntentService
	public MetrixIntentService() {
		super("com.metrix.metrixmobile.service.MetrixIntentService");
		mBaseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder implements IPostMonitor {
		public MetrixIntentService getService() {
			return MetrixIntentService.this;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.metrix.architecture.services.IPostMonitor#registerListener(com
		 * .metrix.architecture.services.IPostListener)
		 */
		public void registerListener(IPostListener callback) {
			Caller l = new Caller(callback);

			// poll(l);
			mCallers.put(callback, l);
		}

		public void removeListener(IPostListener callback) {
			mCallers.remove(callback);
		}
	}

	class Caller {
		IPostListener callback = null;

		Caller(IPostListener callback) {
			this.callback = callback;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mRemoteExecutor = new MetrixRemoteExecutor(this);
		final Context context = MobileApplication.getAppContext();
		final String user = SettingsHelper.getActivatedUser(context);
		final int deviceSequence = SettingsHelper.getDeviceSequence(context);

		mSyncManager = new MetrixSyncManager(this.mBaseUrl, user, deviceSequence, mRemoteExecutor, false);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	/*
	 * Perform long running operations in this method. This is executed in a
	 * separate thread.
	 */
	@Override
	protected void handleBroadcastIntent(Intent broadcastIntent) {
		serviceWork(broadcastIntent);
	}

	/*
	 * Perform long running operations in this method. This is executed in a
	 * separate thread.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		serviceWork(intent);
	}

	private void serviceWork(Intent intent) {
		ThreadHelper.logThreadSignature(tag);

		if (isNetworkAvailable() == false)
			return;
		try {
			MetrixSyncManager.mCallers = this.mCallers;
			mSyncManager.sync();

			LogManager.getInstance(this).debug("Job completed");
		} catch (Exception ex) {
			if(ex.getMessage() != null) {
				LogManager.getInstance().error(ex.getCause());
				LogManager.getInstance(this).debug(ex.getMessage());
			}
		}
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	public MetrixSyncManager getSyncManager() {
		return mSyncManager;
	}
}
