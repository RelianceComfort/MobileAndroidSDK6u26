package com.metrix.architecture.database;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.app.DownloadManager;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.metadata.FSMNotificationContent;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.metadata.TableColumnDef;
import com.metrix.architecture.notification.FSMNotificationAssistant;
import com.metrix.architecture.services.MetrixAlarmReceiver;
import com.metrix.architecture.services.MetrixAttachmentReceiver;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.AppStateObserver;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.LogManager.Level;
import com.metrix.architecture.utilities.MetrixDatabases;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MobileApplication extends Application {
	public IntentService mMobileSyncService;
	public static WeakReference<Activity> mCurrentActivity;
	public static ScheduledExecutorService mSyncService;
	public static ScheduledFuture<?> mResult;
	public static final int ARCH_GET_LOOKUP_RESULT = 2468;
	public static final int METRIXREQUESTCODE = 12345;
	public static final int SYNC_INTERVAL = 60; // default (starting in 5.5) is 60 seconds
	public static MetrixDatabaseAdapter UpdateAdapter;
	public static MetrixDatabaseAdapter QueryAdapter;
	public static MetrixDatabaseAdapter InitAdapter;
	public static boolean DatabaseLoaded = false;
	public static boolean ApplicationLaunched = false;
	public static String ApplicationNullIfCrashed = null;
	private static String DatabaseVersion = "DatabaseVersion";

	public static boolean PerformingActivation = false;
	public static boolean NeedsAppStartupPushProcessing = true;
	public static boolean SplashComplete = false;
	public static FSMNotificationContent NotificationContent = null;

	public static long mLastMemoryCount = 0;
	
	private static Context context;
	private static boolean isSyncStarted = false;
	
	public static Context getAppContext(){
        return context;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
	}

	@Override
	public void onCreate() {		
		// Setup handler for uncaught exceptions.
	    Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException (Thread thread, Throwable e) {
				handleUncaughtException (thread, e);
			}
	    });
		
		super.onCreate();
		MobileApplication.context = getApplicationContext();
		ApplicationLaunched = true;		
		
		MetrixPublicCache.instance.addItem(Global.MobileApplication, this);
		
		String logLevel = SettingsHelper.getLogLevel(this);
		for (Level level : Level.values()) {
			if (logLevel.compareToIgnoreCase(level.toString()) == 0) {
				LogManager.getInstance(this).setLevel(level);
			}
		}
		
		LogManager.getInstance().info("Metrix Application launched.");

		boolean isDemoBuild = MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild");
		MetrixPublicCache.instance.addItem("EnableSyncProcess", !isDemoBuild);
		MetrixPublicCache.instance.addItem("MetrixServiceAddress", SettingsHelper.getServiceAddress(this));
		MetrixPublicCache.instance.addItem("SYNCREQUESTCODE", METRIXREQUESTCODE);
		MetrixPublicCache.instance.addItem("INIT_STARTED", SettingsHelper.getInitStatus(this));		
		
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
		MetrixAttachmentReceiver receiver = new MetrixAttachmentReceiver();
		if (Build.VERSION.SDK_INT >= 33) {
			mCtxt.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
		} else {
			//noinspection UnspecifiedRegisterReceiverFlag
			mCtxt.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		}


		createNotificationChannels();
		registerForActivityCallbacks();
		registerForAppStateCallbacks();
	}
	
	public void handleUncaughtException (Thread thread, Throwable e) {
	    try {
	    	LogManager.getInstance(this).error(e);
			LogManager.getInstance(this).sendEmail(this, true);
		} catch (Exception ex) {
			LogManager.getInstance(this).error(ex);
		}

	    System.exit(1);
	}
	
	public static boolean appIsInBackground(Context context) { 
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		// Lollipop or higher
		List<RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
		for (RunningAppProcessInfo processInfo : runningProcesses) {
			if (processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				for (String activeProcess : processInfo.pkgList) {
					if (activeProcess.equals(context.getPackageName())) {
						isInBackground = false;
					}
				}
			}
		}

		return isInBackground;
	}
	
	public static boolean serverSidePasswordChangeOccurredInBackground() {
		int count = MetrixDatabaseManager.getCount("user_credentials", "hidden_chg_occurred = 'Y'");

		if(SettingsHelper.getBooleanSetting(getAppContext(), "REDISPLAY_PASSWORD_DIALOG"))
			count = 1;
		return count > 0;
	}
	
	public static void startSync(Context context) {
		int syncInterval = SettingsHelper.getSyncInterval(context);

		startSync(context, syncInterval);
	}
	
	/**
	 * Added overloaded method to start sync with the interval defined in parameter
	 * @param context
	 * @param interval
	 */
	public static void startSync(Context context, int interval) {
		if (MetrixApplicationAssistant.getMetaBooleanValue(context, "DemoBuild"))
			return;

		//if there's an existing sync service - stop it first.
		if(isSyncStarted)
			stopSync(context);

		final MetrixAlarmReceiver receiver = new MetrixAlarmReceiver();
		final Context cxt = context;

		Runnable delayTask = new Runnable(){
			@Override
			public void run() {
				try{
					receiver.onReceive(cxt);
				}catch(Exception e){

				}
			}
		};

		if(mSyncService == null)
			mSyncService = Executors.newSingleThreadScheduledExecutor();
		if(mResult == null)
			mResult = mSyncService.scheduleWithFixedDelay(delayTask, 5, interval, TimeUnit.SECONDS);

		if(mSyncService != null && (mSyncService.isTerminated() || mSyncService.isShutdown()))
			mSyncService = Executors.newSingleThreadScheduledExecutor();
		if(mResult != null && (mResult.isCancelled() || mResult.isDone()))
			mResult = mSyncService.scheduleWithFixedDelay(delayTask, 5, interval, TimeUnit.SECONDS);

		isSyncStarted = true;
		Log.e("SYNC", "sync service has started.");
	}	

	public static void stopSync(Context context) {
		Log.e("SYNC", "Service stopping - started.");
		LogManager.getInstance().info("sync service stopping - started.");

		if (mResult != null) {
			if (!mResult.isCancelled())
				mResult.cancel(true);
		}

		if (mSyncService != null) {
			try {
				Log.e("SYNC", "Service stopping - in progress.");
				LogManager.getInstance().info("Service stopping - in progress.");
				mSyncService.shutdown();
				mSyncService.awaitTermination(2, TimeUnit.SECONDS);
			} catch (InterruptedException ex) {
				Log.e("SYNC", "Tasks interrupted exception.");
				LogManager.getInstance().error("Tasks interrupted exception.");
				LogManager.getInstance().error(ex);
			} catch (Exception ex) {
				Log.e("SYNC", "General exception occurred.");
				LogManager.getInstance().error("General exception occurred.");
				LogManager.getInstance().error(ex);
			} finally {
				if (!mSyncService.isTerminated())
					Log.e("SYNC", "Cancel non-finished tasks.");

				mSyncService.shutdownNow();
				mResult = null;
				mSyncService = null;

				isSyncStarted = false;
				Log.e("SYNC", "Service has stopped completely.");
			}
		}
	}

	public static void resumeSync(Context context) {
		if (MetrixApplicationAssistant.getMetaBooleanValue(context, "DemoBuild"))
			return;

		startSync(context);
	}

	public static ArrayList<String> getMobileDesignTableNames() {
		ArrayList<String> mobileDesignTableNames = new ArrayList<String>();
		
		mobileDesignTableNames.add("mm_assignment");
		mobileDesignTableNames.add("mm_assignment_val");
		mobileDesignTableNames.add("mm_design");
		mobileDesignTableNames.add("mm_design_set");
		mobileDesignTableNames.add("mm_field");
		mobileDesignTableNames.add("mm_field_lkup");
		mobileDesignTableNames.add("mm_field_lkup_column");
		mobileDesignTableNames.add("mm_field_lkup_filter");
		mobileDesignTableNames.add("mm_field_lkup_orderby");
		mobileDesignTableNames.add("mm_field_lkup_table");
		mobileDesignTableNames.add("mm_filter_sort_item");
		mobileDesignTableNames.add("mm_home_item");
		mobileDesignTableNames.add("mm_menu_item");
		mobileDesignTableNames.add("mm_revision");
		mobileDesignTableNames.add("mm_screen");
		mobileDesignTableNames.add("mm_screen_item");
		mobileDesignTableNames.add("mm_skin");
		mobileDesignTableNames.add("mm_workflow");
		mobileDesignTableNames.add("mm_workflow_screen");
		
		return mobileDesignTableNames;
	}
	
	public static ArrayList<String> getMobileNoLogDesignTableNames() {
		ArrayList<String> mobileDesignTableNames = new ArrayList<String>();
		
		mobileDesignTableNames.add("metrix_client_script_view");
		mobileDesignTableNames.add("mm_design_revision_view");
		mobileDesignTableNames.add("mm_message_def_view");
		mobileDesignTableNames.add("mm_user_assignment_view");
		mobileDesignTableNames.add("use_mm_design");
		mobileDesignTableNames.add("use_mm_design_set");
		mobileDesignTableNames.add("use_mm_field");
		mobileDesignTableNames.add("use_mm_field_lkup");
		mobileDesignTableNames.add("use_mm_field_lkup_column");
		mobileDesignTableNames.add("use_mm_field_lkup_filter");
		mobileDesignTableNames.add("use_mm_field_lkup_orderby");
		mobileDesignTableNames.add("use_mm_field_lkup_table");
		mobileDesignTableNames.add("use_mm_filter_sort_item");
		mobileDesignTableNames.add("use_mm_home_item");
		mobileDesignTableNames.add("use_mm_menu_item");
		mobileDesignTableNames.add("use_mm_revision");
		mobileDesignTableNames.add("use_mm_screen");
		mobileDesignTableNames.add("use_mm_screen_item");
		mobileDesignTableNames.add("use_mm_skin");
		mobileDesignTableNames.add("use_mm_workflow");
		mobileDesignTableNames.add("use_mm_workflow_screen");
		
		return mobileDesignTableNames;
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, MetrixTableStructure> getTableDefinitionsFromCache() {
		HashMap<String, MetrixTableStructure> tablesDefinition = (HashMap<String, MetrixTableStructure>) MetrixPublicCache.instance.getItem(MetrixDatabases.METRIXTABLEDEFINITION);
		if (tablesDefinition == null) {
			MobileApplication.saveTableDefinitionToCache();
			tablesDefinition = (HashMap<String, MetrixTableStructure>) MetrixPublicCache.instance.getItem(MetrixDatabases.METRIXTABLEDEFINITION);
		}
		return tablesDefinition;
	}
	
	public static void saveTableDefinitionToCache() {
		HashMap<String, MetrixTableStructure> columnCache = new HashMap<String, MetrixTableStructure>();

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select name from sqlite_master where type ='table' AND name LIKE '%_log'", null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			int i = 1;
			while (cursor.isAfterLast() == false) {
				String value = cursor.getString(0);
				String tableName = value.substring(0, value.length() - 4);
				if ((MetrixDatabaseManager.getCount("sqlite_master", "type = 'table' and name = '" + tableName + "'") == 1) && (columnCache.get(tableName) == null)) {
					MetrixKeys metrixKeys = getMetrixKeys(tableName);
					HashMap<String, MetrixKeys> foreignKeys = getForeignKeys(tableName);
					HashMap<String, TableColumnDef> tableDef = getTableSchema(tableName);
					MetrixTableStructure tableStructure = new MetrixTableStructure(tableName, metrixKeys, tableDef, foreignKeys);
					columnCache.put(tableName, tableStructure);
				} 
				cursor.moveToNext();
				i = i + 1;
			}

			if (MetrixPublicCache.instance.containsKey(MetrixDatabases.METRIXTABLEDEFINITION)) {
				MetrixPublicCache.instance.removeItem(MetrixDatabases.METRIXTABLEDEFINITION);
				MetrixPublicCache.instance.addItem(MetrixDatabases.METRIXTABLEDEFINITION, columnCache);
			} else {
				MetrixPublicCache.instance.addItem(MetrixDatabases.METRIXTABLEDEFINITION, columnCache);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static MetrixKeys getMetrixKeys(String table_name) {
		MetrixKeys metrix_keys = new MetrixKeys();

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select * from mm_keys where table_name = '" + table_name + "'", null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			int i = 1;
			metrix_keys.tableName = table_name;

			while (cursor.isAfterLast() == false) {
				String column = cursor.getString(1);
				String column_type = cursor.getString(2);

				metrix_keys.keyInfo.put(column, column_type);

				cursor.moveToNext();
				i = i + 1;
			}

			return metrix_keys;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static HashMap<String, TableColumnDef> getTableSchema(String table_name) {
		HashMap<String, TableColumnDef> tableDef = new HashMap<String, TableColumnDef>();

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("pragma table_info (" + table_name + ")", null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			int i = 1;
			while (cursor.isAfterLast() == false) {
				String column = cursor.getString(1);
				String column_type = cursor.getString(2);
				String isNotNull = cursor.getString(3);
				String isPrimaryKey = cursor.getString(5);

				TableColumnDef columnDef = new TableColumnDef(column, column_type, isPrimaryKey, isNotNull);

				tableDef.put(column, columnDef);

				cursor.moveToNext();
				i = i + 1;
			}

			return tableDef;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 
	 * @param tableName
	 * 
	 * @return foreign key information based on the parent table name
	 *         HashMap<String parent_table, mm_keys
	 *         foreignKeysDefinedInThisTable>
	 */
	public static HashMap<String, MetrixKeys> getForeignKeys(String tableName) {
		HashMap<String, MetrixKeys> foreignKeys = new HashMap<String, MetrixKeys>();

		if(MetrixApplicationAssistant.getMetaStringValue(context, "ClientOwner").compareToIgnoreCase("IFS")==0){
			MetrixCursor cursor = null;
			try {
				cursor = MetrixDatabaseManager.rawQueryMC("pragma foreign_key_list (" + tableName + ")", null);

				if (cursor == null || !cursor.moveToFirst()) {
					return null;
				}

				int i = 1;
				while (cursor.isAfterLast() == false) {
					String parent_table = cursor.getString(2);
					String parent_columnName = cursor.getString(3);
					String child_columnName = cursor.getString(4);

					if (foreignKeys.containsKey(parent_table)) {
						foreignKeys.get(parent_table).keyInfo.put(child_columnName, parent_columnName);
					} else {
						MetrixKeys tableForeignKeys = new MetrixKeys();
						tableForeignKeys.tableName = parent_table;
						tableForeignKeys.keyInfo.put(child_columnName, parent_columnName);
						foreignKeys.put(parent_table, tableForeignKeys);
					}
					cursor.moveToNext();
					i = i + 1;
				}

				return foreignKeys;
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		else {
			ArrayList<Hashtable<String, String>> mmForeignKeys = MetrixDatabaseManager.getFieldStringValuesList("mm_foreign_keys", new String[] { "table_name", "column_name", "foreign_table_name", "foreign_column_name" }, "foreign_table_name = '" + tableName
					+ "'");
	
			if (mmForeignKeys != null) {
				for (Hashtable<String, String> mmForeignKey : mmForeignKeys) {
					String parentTableName = mmForeignKey.get("table_name");
					String parentColumnName = mmForeignKey.get("column_name");
					String childColumnName = mmForeignKey.get("foreign_column_name");
	
					if (foreignKeys.containsKey(parentTableName)) {
						foreignKeys.get(parentTableName).keyInfo.put(childColumnName, parentColumnName);
					} else {
						MetrixKeys tableForeignKeys = new MetrixKeys();
						tableForeignKeys.tableName = parentTableName;
						tableForeignKeys.keyInfo.put(childColumnName, parentColumnName);
						foreignKeys.put(parentTableName, tableForeignKeys);
					}
				}
			}
		}

		return foreignKeys;
	}

	/**
	 * Gets the value of an application parameter by name.
	 * 
	 * @param name The name of the parameter whose value to return.
	 * @return The value of the parameter.
	 */
	public static String getAppParam(String name) {
		return MetrixDatabaseManager.getAppParam(name);
	}

	public static int getDatabaseVersion(Context context){
		return MetrixApplicationAssistant.getMetaIntValue(context, DatabaseVersion);
	}

	private void createNotificationChannels() {
		// Create the NotificationChannel, but only on API 26+,
		// because the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = getSystemService(NotificationManager.class);

			// This code block establishes a channel for Local Notifications
			if (notificationManager.getNotificationChannel(NotificationChannel.DEFAULT_CHANNEL_ID) == null) {
				CharSequence name = AndroidResourceHelper.getMessage("GeneralNotificationName");
				String description = AndroidResourceHelper.getMessage("GeneralNotificationDesc");
				int importance = NotificationManager.IMPORTANCE_DEFAULT;
				NotificationChannel channel = new NotificationChannel(FSMNotificationAssistant.CHANNEL_NAME, name, importance);
				channel.setDescription(description);

				// Register the channel with the system
				// You can't change the importance or other notification behaviors after this
				notificationManager.createNotificationChannel(channel);
			}

			// This code block establishes a channel for Push Notifications from a Notification Hub
			if (notificationManager.getNotificationChannel(FSMNotificationAssistant.HUB_NOTIFICATION_CHANNEL_ID) == null) {
				CharSequence hubName = AndroidResourceHelper.getMessage("HubNotificationName");
				String hubDescription = AndroidResourceHelper.getMessage("HubNotificationDesc");
				NotificationChannel channel = new NotificationChannel(FSMNotificationAssistant.HUB_NOTIFICATION_CHANNEL_ID, hubName, NotificationManager.IMPORTANCE_HIGH);
				channel.setDescription(hubDescription);
				channel.setShowBadge(true);

				notificationManager.createNotificationChannel(channel);
			}
		}
	}

	private void registerForActivityCallbacks() {
		try {
			ActivityLifecycleCallbacks alc = new ActivityLifecycleCallbacks() {
				@Override
				public void onActivityCreated(Activity activity, Bundle savedInstanceState) { mCurrentActivity = new WeakReference<>(activity); }

				@Override
				public void onActivityStarted(Activity activity) { mCurrentActivity = new WeakReference<>(activity); }

				@Override
				public void onActivityResumed(Activity activity) { mCurrentActivity = new WeakReference<>(activity); }

				@Override
				public void onActivityPaused(Activity activity) { }

				@Override
				public void onActivityStopped(Activity activity) { }

				@Override
				public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

				@Override
				public void onActivityDestroyed(Activity activity) { }
			};

			this.registerActivityLifecycleCallbacks(alc);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void registerForAppStateCallbacks() {
		try {
			LifecycleOwner appCycleOwner = ProcessLifecycleOwner.get();
			Lifecycle lc = appCycleOwner.getLifecycle();
			lc.addObserver(new AppStateObserver());
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}
}
