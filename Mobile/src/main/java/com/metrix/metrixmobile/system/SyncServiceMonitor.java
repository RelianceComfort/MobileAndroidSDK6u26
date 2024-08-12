package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixDesignerHelper;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.metadata.MetrixErrorInfo;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MessageHandler;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixIntentService.LocalBinder;
import com.metrix.architecture.services.MetrixServiceManager;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.ui.widget.MobileUIHelper;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.Global.ConnectionStatus;
import com.metrix.architecture.utilities.Global.MessageStatus;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.JobListMetrixActionView;
import com.metrix.metrixmobile.LocationBroadcastReceiver;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MobileGlobal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class SyncServiceMonitor extends MetrixActivity implements OnItemClickListener {
	private TabHost mTabHost;

	private SimpleRecyclerViewAdapter mWaitingSimpleAdapter;
	private String[] mWaitingFrom;
	private int[] mWaitingTo;

	private SimpleRecyclerViewAdapter mReadySimpleAdapter;
	private String[] mReadyFrom;
	private int[] mReadyTo;

	private SimpleRecyclerViewAdapter mSentSimpleAdapter;
	private String[] mSentFrom;
	private int[] mSentTo;

	private SimpleRecyclerViewAdapter mErrorSimpleAdapter;
	private String[] mErrorFrom;
	private int[] mErrorTo;

	private List<TimelineEntry> timeline = new ArrayList<>();
	private TimelineAdapter adapter = null;

	private int mWaitingCount = 0;
	private int mReadyCount = 0;
	private int mSentCount = 0;
	private int mErrorCount = 0;

	private TabHost.TabSpec mWaiting;
	private TabHost.TabSpec mReady;
	private TabHost.TabSpec mSent;
	private TabHost.TabSpec mError;

	private AlertDialog mDeleteAlert, mRefreshDesignAlert;
	private RecyclerView mRecyclerView, mReadyRecyclerView, mWaitingRecyclerView, mSentRecyclerView, mErrorRecyclerView;
	private static int mSelectedPosition = -1;
	private LocalBinder mLocalBinder;
	private MetrixIntentService mSyncService;
	private static ConnectionStatus mLastConnectionStatus = ConnectionStatus.Unknown;
	private boolean mStartLocation = false;
	private PingTask mPingTask;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// mLastConnectionStatus = ConnectionStatus.Unknown;;
		Resources res = getResources(); // Resource object to get Drawables
		setContentView(R.layout.sync_monitor);

		boolean startSync = getIntent().getBooleanExtra("StartSync", false);
		boolean startService = getIntent().getBooleanExtra("StartService", false);
		mStartLocation = getIntent().getBooleanExtra("StartLocation", false);
		mRecyclerView = findViewById(R.id.recyclerView);
		mReadyRecyclerView = findViewById(R.id.ready_messages);
		mWaitingRecyclerView = findViewById(R.id.waiting_messages);
		mSentRecyclerView = findViewById(R.id.sent_messages);
		mErrorRecyclerView = findViewById(R.id.error_messages);
		mRecyclerView.setItemAnimator(null);

		MetrixListScreenManager.setupVerticalRecyclerView(mRecyclerView, R.drawable.rv_item_divider);
		MetrixListScreenManager.setupVerticalRecyclerView(mReadyRecyclerView, R.drawable.rv_item_divider);
		MetrixListScreenManager.setupVerticalRecyclerView(mSentRecyclerView, R.drawable.rv_item_divider);
		MetrixListScreenManager.setupVerticalRecyclerView(mWaitingRecyclerView, R.drawable.rv_item_divider);
		MetrixListScreenManager.setupVerticalRecyclerView(mErrorRecyclerView, R.drawable.rv_item_divider);

		this.startSyncService(startSync, startService);

		populateWaitingList();
		populateReadyList();
		populateSentList();
		populateErrorList();

		addTabs(res);

		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onTabChanged(String arg0) {
				if (mTabHost.getCurrentTabTag().toLowerCase().contains(AndroidResourceHelper.getMessage("Waiting").toLowerCase())) {
					populateWaitingList();
				} else if (mTabHost.getCurrentTabTag().toLowerCase().contains(AndroidResourceHelper.getMessage("Ready").toLowerCase())) {
					populateReadyList();
				} else if (mTabHost.getCurrentTabTag().toLowerCase().contains(AndroidResourceHelper.getMessage("Sent").toLowerCase())) {
					populateSentList();
				} else if (mTabHost.getCurrentTabTag().toLowerCase().contains(AndroidResourceHelper.getMessage("Error").toLowerCase())) {
					populateErrorList();
				}

				Resources res = getResources();
				mWaiting.setIndicator(AndroidResourceHelper.getMessage("Waiting") + " (" + mWaitingCount + ")", res.getDrawable(R.drawable.tab_waiting));
				mReady.setIndicator(AndroidResourceHelper.getMessage("Ready") + " (" + mReadyCount + ")", res.getDrawable(R.drawable.tab_ready));
				mSent.setIndicator(AndroidResourceHelper.getMessage("Sent") + " (" + mSentCount + ")", res.getDrawable(R.drawable.tab_sent));
				mError.setIndicator(AndroidResourceHelper.getMessage("Error") + " (" + mErrorCount + ")", res.getDrawable(R.drawable.tab_error));
			}
		});

		adapter = new TimelineAdapter(timeline);
		mRecyclerView.setAdapter(adapter);

		int defaultTab = getIntent().getIntExtra("default_tab", 0);
		mTabHost.setCurrentTab(defaultTab);

		mCurrentActivity = this;
	}

	private void startSyncService(boolean startSync, boolean startService) {
		try {
			if (startService) {
				// Calling sync service immediately.
				MetrixServiceManager.setup(getApplicationContext());

				User user = User.getUser();
				if (user != null) {
					Intent serviceIntent = new Intent(getApplicationContext(), MetrixIntentService.class);
					getApplicationContext().startService(serviceIntent);
				}
			}
		} catch (Exception ex) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ServiceFailed"));
			return;
		} finally {
			if (SettingsHelper.getSyncPause(getApplicationContext()) == false && startSync) {
				MobileApplication.startSync(this);
			}
		}
	}

	private void addTabs(Resources res) {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		TabHost.TabSpec specSync = mTabHost.newTabSpec("sync");
		specSync.setContent(R.id.table_layout);
		specSync.setIndicator(AndroidResourceHelper.getMessage("Sync"), res.getDrawable(R.drawable.tab_sync));

		mWaiting = mTabHost.newTabSpec("waiting");
		mWaiting.setContent(R.id.waiting_messages_layout);
		mWaiting.setIndicator(AndroidResourceHelper.getMessage("Waiting") + " (" + mWaitingCount + ")", res.getDrawable(R.drawable.tab_waiting));

		mReady = mTabHost.newTabSpec("ready");
		mReady.setContent(R.id.ready_messages_layout);
		mReady.setIndicator(AndroidResourceHelper.getMessage("Ready") + " (" + mReadyCount + ")", res.getDrawable(R.drawable.tab_ready));

		mSent = mTabHost.newTabSpec("sent");
		mSent.setContent(R.id.sent_messages_layout);
		mSent.setIndicator(AndroidResourceHelper.getMessage("Sent") + " (" + mSentCount + ")", res.getDrawable(R.drawable.tab_sent));

		mError = mTabHost.newTabSpec("error");
		mError.setContent(R.id.error_messages_layout);
		mError.setIndicator(AndroidResourceHelper.getMessage("Error") + " (" + mErrorCount + ")", res.getDrawable(R.drawable.tab_error));

		mTabHost.addTab(specSync);
		mTabHost.addTab(mWaiting);
		mTabHost.addTab(mReady);
		mTabHost.addTab(mSent);
		mTabHost.addTab(mError);
		
		MobileUIHelper.alteringTabWidget(mTabHost);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.sync_monitor828dcecc, "SyncMonitor"));
		resourceStrings.add(new ResourceValueObject(R.id.ScnInfoSyncMonMsgs, "ScnInfoSyncMonMsgs"));
		resourceStrings.add(new ResourceValueObject(R.id.WaitingMessages87c230fd, "WaitingMessages"));
		resourceStrings.add(new ResourceValueObject(R.id.ScnInfoSyncMonWaitMsgs, "ScnInfoSyncMonWaitMsgs"));
		resourceStrings.add(new ResourceValueObject(R.id.ReadyMessages0f92c567, "ReadyMessages"));
		resourceStrings.add(new ResourceValueObject(R.id.ScnInfoSyncMonRdyMsgs, "ScnInfoSyncMonRdyMsgs"));
		resourceStrings.add(new ResourceValueObject(R.id.SentMessagescb397c46, "SentMessages"));
		resourceStrings.add(new ResourceValueObject(R.id.ScnInfoSyncMonSentMsgs, "ScnInfoSyncMonSentMsgs"));
		resourceStrings.add(new ResourceValueObject(R.id.ErrorMessages0445a934, "ErrorMessages"));
		resourceStrings.add(new ResourceValueObject(R.id.ScnInfoSyncMonErrMsgs, "ScnInfoSyncMonErrMsgs"));
		super.onStart();
		setListeners();
		
		boolean showInitDialog = getIntent().getBooleanExtra("ShowInitDialog", false);
		boolean showRefreshMobileDesignData = getIntent().getBooleanExtra("ShowRefreshDesignDialog", false);

		if (showInitDialog) {
			mUIHelper.showLoadingDialog((AndroidResourceHelper.getMessage("InitializingDevice")));
			LogManager.getInstance(mCurrentActivity).debug("Initialization dialog shown, based on intent.");
		} else if (showRefreshMobileDesignData) {
			mUIHelper.showLoadingDialog((AndroidResourceHelper.getMessage("RefreshCustomDesignInProgress")));
			LogManager.getInstance(mCurrentActivity).debug("Refresh data dialog shown, based on intent.");
		}
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		initMetrixActionView(getMetrixActionBar().getCustomView());
	}

	private void showMessageDetails(HashMap<String, String> selectedItem, String tabName)
	{
		if (selectedItem != null)
		{
			try
			{
				String[] columnsToGet = new String[] { "message", "message_id", "table_name", "metrix_log_id", "transaction_type" };
				Hashtable<String, String> results = MetrixDatabaseManager.getFieldStringValues("mm_message_out", columnsToGet, "message_id=" + selectedItem.get("mm_message_out.message_id"));

				String message = "";
				if (results != null)
				{
					String logId = results.get("metrix_log_id");
					int metrixLogId = 0;
					if (!MetrixStringHelper.isNullOrEmpty(logId))
						metrixLogId = Integer.parseInt(results.get("metrix_log_id"));
					String messageValue = "";

					if (metrixLogId != 0)
					{
						messageValue = MessageHandler.getMessageFromTransLogTable(results.get("transaction_type"), results.get("table_name"), metrixLogId);
					}

					String originalMessage = MetrixStringHelper.isNullOrEmpty(messageValue) ? results.get("message") : messageValue;
					message = originalMessage; //MessageHandler.getAuthenticatedMessage(originalMessage);
				}

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MessageDetails.class);
				intent.putExtra("SYNC_MESSAGE_DETAILS", message);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
			catch (Exception ex) {
				LogManager.getInstance().error(ex);
			}
		}
	}

	private String getMessageInfo(String input) {
		if (MetrixStringHelper.isNullOrEmpty(input)) {
			return "";
		} else
			return input;
	}



	/**
	 * Enables error correction for default/non-codeless screens.
	 * @param message_id
	 * @param activity_name
	 * @param transaction_desc
	 * @param error_message
	 * @param table_name
	 * @param log_id
	 */
	private void correctError(String message_id, String activity_name, String transaction_desc, String error_message, String table_name, int log_id, int codeLessScreenId) {
		Hashtable<String, String> error_keys = getErrorKeys(table_name, log_id);
		MobileGlobal.mErrorInfo = new MetrixErrorInfo(table_name, transaction_desc, message_id, error_message, error_keys);

		if (!MetrixStringHelper.isNullOrEmpty(activity_name) && activity_name.contains(".")) {
			try {
				Intent intent = null;
				intent = MetrixActivityHelper.createActivityIntent(this,activity_name.substring(activity_name.lastIndexOf(".")+1));
				intent.putExtra("HandleError", true);
				intent.putExtra("HandleErrorMessageId", message_id);

				String logId = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "metrix_log_id", "message_id=" + message_id);
				if (MetrixStringHelper.isNullOrEmpty(logId) 
					|| Arrays.asList(MobileGlobal.nonCorrectableActivities).contains(activity_name)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,AndroidResourceHelper.getMessage("SyncErrorCannotCorrect"));
					return;
				}

				intent.putExtra("HandleErrorLogId", logId);
				if(codeLessScreenId > 0)
					intent.putExtra("ScreenID", codeLessScreenId);
				// MetrixActivityHelper.startNewActivity(this, intent);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			} catch (Exception ex) {
				LogManager.getInstance(this).error(ex);
			}
		}
	}

	private synchronized void changeStatus(ConnectionStatus cStatus) {
		if (cStatus == ConnectionStatus.Connected) {
			ImageView image = (ImageView) findViewById(R.id.syncStatus);
			image.setImageResource(R.drawable.marker_green);
		} else if (cStatus == ConnectionStatus.Disconnected) {
			ImageView image = (ImageView) findViewById(R.id.syncStatus);
			image.setImageResource(R.drawable.marker_red);
		} else if (cStatus == ConnectionStatus.Pause) {
			ImageView image = (ImageView) findViewById(R.id.syncStatus);
			image.setImageResource(R.drawable.sync_pause);
		}
	}

	private void startLocationListnerIfNeeded() {
		LocationBroadcastReceiver.setLocationListenerStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void bindService() {
		bindService(new Intent(SyncServiceMonitor.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mLastConnectionStatus = ConnectionStatus.Unknown;
	}

	protected ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			// mBoundService = ((MetrixIntentService.LocalBinder) binder)
			// .getService();

			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);

				mLocalBinder = (MetrixIntentService.LocalBinder) binder;
				mSyncService = mLocalBinder.getService();

				if (mSyncService != null && mSyncService.getSyncManager() != null) {
					// runOnUiThread would not work well, because it will block the UI.
					mPingTask = new PingTask();
					mPingTask.execute(mSyncService.getSyncManager());
				}
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			// mBoundService = null;
			service.removeListener(listener);
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				@SuppressLint("DefaultLocale") public void run() {
					if (activityType == ActivityType.Information 
							&& (MetrixStringHelper.valueIsEqual(message, MetrixStringHelper.getArchitectureString("FinishedSync")) 
								|| MetrixStringHelper.valueIsEqual(message, MetrixStringHelper.getArchitectureString("StartingSync")))) {
						String whereClause = "";
						String statusName = "";

						for (int i = 1; i < 5; i++) {
							switch (i) {
							case 1:
								whereClause = "mm_message_out.status in ('" + MessageStatus.WAITING.toString() + "', '"
										+ MessageStatus.WAITING_PREVIOUS.toString() + "')";
								statusName = AndroidResourceHelper.getMessage("Waiting");
								break;
							case 2:
								whereClause = "mm_message_out.status = '" + MessageStatus.READY.toString() + "'";
								statusName = AndroidResourceHelper.getMessage("Ready");
								break;
							case 3:
								whereClause = "mm_message_out.status = '"
										+ MessageStatus.SENT.toString()
										+ "' and not exists (select message_id from mm_message_in where mm_message_out.message_id = mm_message_in.related_message_id and mm_message_in.status = '"
										+ MessageStatus.ERROR.toString() + "')";
								statusName = AndroidResourceHelper.getMessage("Sent");
								break;
							case 4:
								whereClause = "mm_message_out.status = '" + MessageStatus.ERROR.toString() + "'";
								statusName = AndroidResourceHelper.getMessage("Error");
								break;
							}

							TextView title = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
							if(i!=4)
								title.setText(statusName + " (" + MetrixDatabaseManager.getCount("mm_message_out", whereClause) + ")");
							else
							{
								int countMessageOut = MetrixDatabaseManager.getCount("mm_message_out", whereClause);
								whereClause = "mm_message_in.message like '%InitializationError%' or mm_message_in.message like '%BatchSyncError%'";
								int countMessageIn =  MetrixDatabaseManager.getCount("mm_message_in", whereClause);

								title.setText(statusName + " (" + (countMessageOut + countMessageIn)+ ")");
							}
						}
					}

					if (activityType == ActivityType.InitializationStarted) {
						mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("InitializingDevice"));
						LogManager.getInstance(mCurrentActivity).debug("Initialization, run on the child activity.");
					} else if (activityType == ActivityType.InitializationEnded) {
						mUIHelper.dismissLoadingDialog();
						User.setUser(User.getUser().personId, mCurrentActivity);
						
						if (mStartLocation)
							startLocationListnerIfNeeded();

						// Reset the sync interval based on the latest Metrix_App_Params
						MobileApplication.stopSync(mCurrentActivity);
						startSyncService(true, false);

						String enable_time_zone = MobileApplication.getAppParam("MOBILE_ENABLE_TIME_ZONE");		
						if (!MetrixStringHelper.isNullOrEmpty(enable_time_zone) && enable_time_zone.toLowerCase().contains("y")) {
							Global.enableTimeZone = true;
						}
						else {
							Global.enableTimeZone = false;
						}
						
						String encode_url = MobileApplication.getAppParam("MOBILE_ENCODE_URL_PARAM");
						if (!MetrixStringHelper.isNullOrEmpty(encode_url) && encode_url.toLowerCase().contains("y")) {
							Global.encodeUrl = true;
						}
						else {
							Global.encodeUrl = false;
						}

						//Clearing Attachment screen Action Bar cache to set new data
						if (MetrixPublicCache.instance.containsKey("FSMAttachmentAPIBaseResources")) {
							MetrixPublicCache.instance.removeItem("FSMAttachmentAPIBaseResources");
						}

						AndroidResourceHelper.clearMessageTranslationCache();
						JobListMetrixActionView.refreshJobListActionView();
						Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
						MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
					} else if (activityType == ActivityType.PasswordChangedFromServer) {
						handleServerPasswordChange();
					} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMDS\":null}")) {
						// this is the end of a mini-INIT for refreshing custom mobile design data
						mUIHelper.dismissLoadingDialog();
						if (mobileDesignerTablesHaveNoContent()) {
							mRefreshDesignAlert = new AlertDialog.Builder(SyncServiceMonitor.this).create();
							mRefreshDesignAlert.setMessage(AndroidResourceHelper.getMessage("RefreshCustDesFailConfirm"));
							mRefreshDesignAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), designRefreshListener);
							mRefreshDesignAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), designRefreshListener);
							mRefreshDesignAlert.show();							
						} else {

							MetrixDesignerHelper.refreshMetadataCaches();

							MobileApplication.stopSync(mCurrentActivity);
							MobileApplication.startSync(mCurrentActivity);

							Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
							MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
						}
					} else {
						final Parcelable rvState = mRecyclerView.getLayoutManager().onSaveInstanceState();
						int row_num = adapter.getItemCount();
						if (row_num >= 20) {
							adapter.getData().remove(row_num - 1);
							adapter.notifyItemRemoved(row_num - 1);
						}

						adapter.getData().add(0, new TimelineEntry(activityType.toString(), MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.TIME_FORMAT),
								message));
						adapter.notifyItemInserted(0);
						mRecyclerView.getLayoutManager().onRestoreInstanceState(rvState);

						if (activityType == ActivityType.Information) {
							ConnectionStatus cStatus = ConnectionStatus.Unknown;

							if (MetrixStringHelper.valueIsEqual(message, MetrixStringHelper.getArchitectureString("Disconnected"))) {
								cStatus = ConnectionStatus.Disconnected;

								if (cStatus != mLastConnectionStatus) {
									mLastConnectionStatus = cStatus;
									changeStatus(mLastConnectionStatus);
								}
							} else if (MetrixStringHelper.valueIsEqual(message, MetrixStringHelper.getArchitectureString("Connected"))) {
								cStatus = ConnectionStatus.Connected;

								if (cStatus != mLastConnectionStatus) {
									mLastConnectionStatus = cStatus;
									changeStatus(mLastConnectionStatus);
								}
							}
						}
					}
				}
			});
		}
	};

	private boolean mobileDesignerTablesHaveNoContent() {
		// get counts for design/revision and parallel use_ tables ... if sum == 0, return TRUE (otherwise FALSE)
		int designCount = MetrixDatabaseManager.getCount("mm_design", "");
		int revCount = MetrixDatabaseManager.getCount("mm_revision", "");
		int useDesignCount = MetrixDatabaseManager.getCount("use_mm_design", "");
		int useRevCount = MetrixDatabaseManager.getCount("use_mm_revision", "");
		
		int sum = designCount + revCount + useDesignCount + useRevCount;
		
		return (sum == 0);
	}
	
	DialogInterface.OnClickListener designRefreshListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:	// Yes
				if(SettingsHelper.getSyncPause(mCurrentActivity))
				{
					SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
					if(syncPauseAlertDialog != null)
					{
						syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
							@Override
							public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
								startDesignRefreshListener();
							}
						});
					}
				}
				else
					startDesignRefreshListener();
				break;

			case DialogInterface.BUTTON_NEGATIVE:	// No (exit Sync screen with Sync running at normal interval)
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity);
				Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
				MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
				break;
			}
		}
	};

	private void startDesignRefreshListener() {
		Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doDesignRefresh(mCurrentActivity) == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    SyncServiceMonitor.this.runOnUiThread(new Runnable() {
                          public void run() {
							  MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobileServiceUnavailable"));
							  if (mRefreshDesignAlert != null) {
                              mRefreshDesignAlert.dismiss();
                          }
                          }
                    });
                    return;
                }

                if (mRefreshDesignAlert != null) {
                    mRefreshDesignAlert.dismiss();
                }

                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, SyncServiceMonitor.class);
                intent.putExtra("ShowRefreshDesignDialog", true);
                MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
            }
        });

		thread.start();
	}

	/**
	 * Time line entry for sync message
	 * 
	 */
	public static class TimelineEntry {
		String mDatatype = "";
		String mCreatedAt = "";
		String mMessage = "";

		TimelineEntry(String datatype, String createdAt, String message) {
			this.mDatatype = datatype;
			this.mCreatedAt = createdAt;
			this.mMessage = message;
		}
	}

	/**
	 * Adapter for timelineentry
	 * 
	 */
	static class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineEntryWrapper> {
		private List<TimelineEntry> data;
		TimelineAdapter(List<TimelineEntry> data) {
			this.data = data;
		}

		@NonNull
		@Override
		public TimelineEntryWrapper onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sync_monitor_item, parent, false);
			return new TimelineEntryWrapper(view);
		}

		@Override
		public void onBindViewHolder(@NonNull TimelineEntryWrapper holder, int position) {
			holder.populateFrom(data.get(position));
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public List<TimelineEntry> getData() {
			return data;
		}

		static class TimelineEntryWrapper extends RecyclerView.ViewHolder {
			private final TextView mDataType;
			private final TextView mCreatedAt;
			private final TextView mMessage;

			TimelineEntryWrapper(View row) {
				super(row);
				mDataType = row.findViewById(R.id.sync__datatype);
				mCreatedAt = row.findViewById(R.id.sync__datetime);
				mMessage = row.findViewById(R.id.sync__message);
			}

			void populateFrom(TimelineEntry s) {
				mDataType.setText(s.mDatatype);
				mCreatedAt.setText(s.mCreatedAt);
				mMessage.setText(s.mMessage);
			}
		}
	}

	@SuppressLint("DefaultLocale")
	private void populateWaitingList() {
		mWaitingCount = 0;

		StringBuilder query = new StringBuilder();
		query.append("select mm_message_out.message_id, mm_message_out.transaction_desc, mm_message_out.created_dttm, mm_message_out.transaction_type, mm_message_out.transaction_key_name, mm_message_out.transaction_key_id");
		query.append(" from mm_message_out");
		query.append(" where mm_message_out.status in ('" + MessageStatus.WAITING.toString() + "', '" + MessageStatus.WAITING_PREVIOUS.toString() + "')");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

		try {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mWaitingSimpleAdapter != null)
					mWaitingSimpleAdapter.updateData(new ArrayList<>());
				return;
			}

			mWaitingTo = new int[] { R.id.mm_message_out__message_id, R.id.mm_message_out__table_name, R.id.mm_message_out__created_dttm,
					R.id.mm_message_out__transaction_type };

			mWaitingFrom = new String[] { "mm_message_out.message_id", "mm_message_out.table_name", "mm_message_out.created_dttm",
					"mm_message_out.transaction_type" };

			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();

				row.put(mWaitingFrom[0], cursor.getString(0));

				String value = cursor.getString(1);
				if (MetrixStringHelper.isNullOrEmpty(value)) {
					row.put(mWaitingFrom[1], "");
				} else {
					value = value.toUpperCase();
					value = value + " (" + cursor.getString(4).replace("_", " ") + " " + cursor.getString(5) + ")";
					row.put(mWaitingFrom[1], value);
				}

				row.put(mWaitingFrom[2], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(2)));
				row.put(mWaitingFrom[3], cursor.getString(3) == null ? "" : cursor.getString(3).toUpperCase());

				mWaitingCount = mWaitingCount + 1;

				table.add(row);
				cursor.moveToNext();
			}

			if (mWaitingSimpleAdapter == null) {
				final int layoutId = R.layout.message_list_item_waiting;
				mWaitingSimpleAdapter = new SimpleRecyclerViewAdapter(new ArrayList<>(table), layoutId, mWaitingFrom, mWaitingTo, new int[] {}, "mm_message_out.message_id");
				mWaitingRecyclerView.setAdapter(mWaitingSimpleAdapter);
				mWaitingSimpleAdapter.setClickListener((pos, data, view) -> {
					HashMap<String, String> selectedItem = (HashMap<String, String>) data;
					showMessageDetails(selectedItem, AndroidResourceHelper.getMessage("Waiting"));
				});
			} else {
				mWaitingSimpleAdapter.updateData(table);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	@SuppressLint("DefaultLocale")
	private void populateReadyList() {
		mReadyCount = 0;

		StringBuilder query = new StringBuilder();
		query.append("select mm_message_out.message_id, mm_message_out.transaction_desc, mm_message_out.created_dttm, mm_message_out.transaction_type, mm_message_out.transaction_key_name, mm_message_out.transaction_key_id");
		query.append(" from mm_message_out");
		query.append(" where mm_message_out.status = '" + MessageStatus.READY.toString() + "'");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

		try {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mReadySimpleAdapter != null)
					mReadySimpleAdapter.updateData(new ArrayList<>());
				return;
			}

			mReadyTo = new int[] { R.id.mm_message_out__message_id, R.id.mm_message_out__table_name, R.id.mm_message_out__created_dttm,
					R.id.mm_message_out__transaction_type };

			mReadyFrom = new String[] { "mm_message_out.message_id", "mm_message_out.table_name", "mm_message_out.created_dttm",
					"mm_message_out.transaction_type" };

			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();

				row.put(mReadyFrom[0], cursor.getString(0));

				String value = cursor.getString(1);
				if (MetrixStringHelper.isNullOrEmpty(value)) {
					row.put(mReadyFrom[1], "");
				} else {
					value = value.toUpperCase();
					value = value + " (" + cursor.getString(4).replace("_", " ") + " " + cursor.getString(5) + ")";
					row.put(mReadyFrom[1], value);
				}

				row.put(mReadyFrom[2], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(2)));
				row.put(mReadyFrom[3], cursor.getString(3) == null ? "" : cursor.getString(3).toUpperCase());

				mReadyCount = mReadyCount + 1;

				table.add(row);
				cursor.moveToNext();
			}

			if (mReadySimpleAdapter == null) {
				final int layoutId = R.layout.message_list_item_ready;
				mReadySimpleAdapter = new SimpleRecyclerViewAdapter(new ArrayList<>(table), layoutId, mReadyFrom, mReadyTo, new int[] {}, "mm_message_out.message_id");
				mReadyRecyclerView.setAdapter(mReadySimpleAdapter);
				mReadySimpleAdapter.setClickListener((pos, data, view) -> {
					HashMap<String, String> selectedItem = (HashMap<String, String>) data;
					showMessageDetails(selectedItem, AndroidResourceHelper.getMessage("Ready"));
				});
			} else {
				mReadySimpleAdapter.updateData(table);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	@SuppressLint("DefaultLocale")
	private void populateSentList() {
		mSentCount = 0;

		StringBuilder query = new StringBuilder();
		query.append("select mm_message_out.message_id, mm_message_out.transaction_desc, mm_message_out.created_dttm, mm_message_out.table_name, mm_message_out.transaction_type, mm_message_out.transaction_key_name, mm_message_out.transaction_key_id");
		query.append(" from mm_message_out");
		query.append(" where mm_message_out.status = '" + MessageStatus.SENT.toString()
				+ "' and not exists (select message_id from mm_message_in where mm_message_out.message_id = mm_message_in.related_message_id");
		query.append(" and mm_message_in.status = '" + MessageStatus.ERROR.toString() + "')");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

		try {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mSentSimpleAdapter != null)
					mSentSimpleAdapter.updateData(new ArrayList<>());
				return;
			}

            mSentTo = new int[] { R.id.mm_message_out__message_id, R.id.mm_message_out__table_name, R.id.mm_message_out__created_dttm, R.id.mm_message_out__transaction_type };

            mSentFrom = new String[] { "mm_message_out.message_id", "mm_message_out.table_name", "mm_message_out.created_dttm",	"mm_message_out.transaction_type" };

			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();

				row.put(mSentFrom[0], cursor.getString(0));

				String value = cursor.getString(1);
				if (MetrixStringHelper.isNullOrEmpty(value)) {
					row.put(mSentFrom[1], "");
				} else {
					value = value.toUpperCase();
					value = value + " (" + cursor.getString(4).replace("_", " ") + " " + cursor.getString(5) + ")";
					row.put(mSentFrom[1], value);
				}

                row.put(mSentFrom[2], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(2)));
                row.put(mSentFrom[3], cursor.getString(3) == null ? "" : cursor.getString(3).toUpperCase());

				mSentCount = mSentCount + 1;

				table.add(row);
				cursor.moveToNext();
			}

			if (mSentSimpleAdapter == null) {
				final int layoutId = R.layout.message_list_item_sent;
				mSentSimpleAdapter = new SimpleRecyclerViewAdapter(new ArrayList<>(table), layoutId, mSentFrom, mSentTo, new int[] {}, "mm_message_out.message_id");
				mSentRecyclerView.setAdapter(mSentSimpleAdapter);
				mSentSimpleAdapter.setClickListener((pos, data, view) -> {
					HashMap<String, String> selectedItem = (HashMap<String, String>) data;
					showMessageDetails(selectedItem, AndroidResourceHelper.getMessage("Sent"));
				});
			} else {
				mSentSimpleAdapter.updateData(table);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	private void populateErrorList() {
		mErrorCount = 0;

		StringBuilder query = new StringBuilder();
		query.append("select distinct msout.message_id, msout.metrix_log_id, msout.table_name, msin.message, msout.created_dttm, msout.transaction_type, msout.transaction_desc, msout.activity_name, msin.message_id, msout.transaction_key_name, msout.transaction_key_id, msout.screen_id");
		query.append(" from mm_message_out msout, mm_message_in msin");
		query.append(" where msin.status = '" + MessageStatus.ERROR.toString() + "' and msin.related_message_id=msout.message_id");

		String uniqueID = "msout.message_id";

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

		try {
			mErrorTo = new int[] { R.id.mm_message_out__message_id, R.id.mm_message_out__metrix_log_id, R.id.mm_message_out__table_name,
					R.id.mm_message_in__message, R.id.mm_message_out__created_dttm, R.id.mm_message_out__transaction_type,
					R.id.mm_message_out__transaction_desc, R.id.mm_message_out__activity_name, R.id.mm_message_in__message_id, R.id.mm_message_out__codeless_screen_id };

			mErrorFrom = new String[] { "mm_message_out.message_id", "mm_message_out.metrix_log_id", "mm_message_out.table_name", "mm_message_in.message",
					"mm_message_out.created_dttm", "mm_message_out.transaction_type", "mm_message_out.transaction_desc", "mm_message_out.activity_name",
					"mm_message_in.message_id", "mm_message_out.codeless_screen_id" };

			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

			if (cursor == null || !cursor.moveToFirst()) {
				if (mErrorSimpleAdapter != null)
					mErrorSimpleAdapter.updateData(new ArrayList<>());
				if(this.displayInitAndBatchSyncError()) {
					query = new StringBuilder();
					query.append("select distinct msin.message, msin.created_dttm, msin.transaction_type, 'Sync Error', 'Unknown', msin.message_id");
					query.append(" from mm_message_in msin");
					query.append(" where msin.status = '" + MessageStatus.ERROR.toString() + "' and (msin.message like '%~BatchSyncError%' or msin.message like '%~InitializationError%')");

					uniqueID = "msin.message_id";
					cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

					try {
						if (cursor == null || !cursor.moveToFirst()) {
							return;
						}

						while (cursor.isAfterLast() == false) {
							HashMap<String, String> row = new HashMap<String, String>();

							row.put(mErrorFrom[0], "");
							row.put(mErrorFrom[1], "");
							row.put(mErrorFrom[2], "");
							String errorMessage = MessageHandler.getErrorMessage(cursor.getString(0));
							row.put(mErrorFrom[3], errorMessage);
							row.put(mErrorFrom[4], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(1)));
							row.put(mErrorFrom[5], cursor.getString(2));
							row.put(mErrorFrom[6], "");


							row.put(mErrorFrom[7], "");
							row.put(mErrorFrom[8], cursor.getString(5));
							row.put(mErrorFrom[9], "");

							mErrorCount = mErrorCount + 1;

							table.add(row);
							cursor.moveToNext();
						}
					} catch (Exception ex) {

					} finally {
						if (cursor != null)
							cursor.close();
					}
				}
			}
			else {
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = new HashMap<String, String>();

					row.put(mErrorFrom[0], cursor.getString(0));
					row.put(mErrorFrom[1], cursor.getString(1));
					row.put(mErrorFrom[2], cursor.getString(2));
					String errorMessage = MessageHandler.getErrorMessage(cursor.getString(3));
					row.put(mErrorFrom[3], errorMessage);
					row.put(mErrorFrom[4], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(4)));
					row.put(mErrorFrom[5], cursor.getString(5));
					if (MetrixStringHelper.isNullOrEmpty(cursor.getString(6))) {
						if (!MetrixStringHelper.isNullOrEmpty(cursor.getString(9))) {
							row.put(mErrorFrom[6], cursor.getString(2) + " (" + cursor.getString(9).replace("_", " ") + " " + cursor.getString(10) + ")");
						} else {
							row.put(mErrorFrom[6], cursor.getString(2));
						}
					} else {
						if (!MetrixStringHelper.isNullOrEmpty(cursor.getString(9))) {
							row.put(mErrorFrom[6], cursor.getString(6) + " (" + cursor.getString(9).replace("_", " ") + " " + cursor.getString(10) + ")");
						} else {
							row.put(mErrorFrom[6], cursor.getString(6));
						}
					}
					row.put(mErrorFrom[7], cursor.getString(7));
					row.put(mErrorFrom[8], cursor.getString(8));
					row.put(mErrorFrom[9], cursor.getString(11));

					mErrorCount = mErrorCount + 1;

					table.add(row);
					cursor.moveToNext();
				}

				if(cursor != null)
					cursor.close();
				if(this.displayInitAndBatchSyncError()) {
					query = new StringBuilder();
					query.append("select distinct msin.message, msin.created_dttm, msin.transaction_type, 'Sync Error', 'Unknown', msin.message_id");
					query.append(" from mm_message_in msin");
					query.append(" where msin.status = '" + MessageStatus.ERROR.toString() + "' and (msin.message like '%~BatchSyncError%' or msin.message like '%~InitializationError%')");

					uniqueID = "msin.message_id";
					cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

					try {
						if (cursor == null || !cursor.moveToFirst()) {
							if (mErrorSimpleAdapter != null)
								mErrorSimpleAdapter.updateData(new ArrayList<>());
						} else {
							while (cursor.isAfterLast() == false) {
								HashMap<String, String> row = new HashMap<String, String>();

								row.put(mErrorFrom[0], "");
								row.put(mErrorFrom[1], "");
								row.put(mErrorFrom[2], "");
								String errorMessage = MessageHandler.getErrorMessage(cursor.getString(0));
								row.put(mErrorFrom[3], errorMessage);
								row.put(mErrorFrom[4], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(1)));
								row.put(mErrorFrom[5], cursor.getString(2));
								row.put(mErrorFrom[6], "");


								row.put(mErrorFrom[7], "");
								row.put(mErrorFrom[8], cursor.getString(5));
								row.put(mErrorFrom[9], "");

								mErrorCount = mErrorCount + 1;

								table.add(row);
								cursor.moveToNext();
							}
						}
					} catch (Exception ex) {

					} finally {
						if (cursor != null)
							cursor.close();
					}
				}
			}

			if (mErrorSimpleAdapter == null) {
				final int layoutId = R.layout.message_list_item_error;
				mErrorSimpleAdapter = new SimpleRecyclerViewAdapter(new ArrayList<>(table), layoutId, mErrorFrom, mErrorTo, new int[] {}, uniqueID);
				mErrorRecyclerView.setAdapter(mErrorSimpleAdapter);
				mErrorSimpleAdapter.setClickListener((pos, data, view) -> {
					HashMap<String, String> selectedItem = (HashMap<String, String>) data;
					showMessageDetails(selectedItem, AndroidResourceHelper.getMessage("Error"));
				});
				mErrorSimpleAdapter.setLongClickListener((pos, data, view) -> {
					mSelectedPosition = pos;
					if(onCreateMetrixActionViewListner != null)
						onCreateMetrixActionViewListner.OnCreateMetrixActionView(mErrorRecyclerView, pos);
				});
			} else {
				mErrorSimpleAdapter.updateData(table);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (cursor != null)
				cursor.close();

			try {
				TextView title = (TextView) mTabHost.getTabWidget().getChildAt(4).findViewById(android.R.id.title);
				String whereClause = "mm_message_out.status = '" + MessageStatus.ERROR.toString() + "'";

				int countMessageOut = MetrixDatabaseManager.getCount("mm_message_out", whereClause);
				whereClause = "mm_message_in.message like '%InitializationError%' or mm_message_in.message like '%BatchSyncError%'";
				int countMessageIn = MetrixDatabaseManager.getCount("mm_message_in", whereClause);

				title.setText(AndroidResourceHelper.getMessage("Error") + " (" + (countMessageOut + countMessageIn) + ")");
			}catch(Exception ex) {
				LogManager.getInstance().error(ex);
			}
		}
	}

	/**
	 * 
	 * @param tableName
	 * @param log_id
	 */
	private Hashtable<String, String> getErrorKeys(String tableName, int log_id) {
		Hashtable<String, String> errorKeys = new Hashtable<String, String>();

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select * from " + tableName + "_log where metrix_log_id=" + log_id, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			while (cursor.isAfterLast() == false) {

				String[] columnNames = cursor.getColumnNames();

				for (String column : columnNames) {
					if (column.compareToIgnoreCase("metrix_log_id") == 0 || column.compareToIgnoreCase("metrix_row_id") == 0)
						continue;

					int columnIndex = cursor.getColumnIndex(column);

					if (MessageHandler.isMetrixTablePrimaryKey(tableName, column)) {
						String value = cursor.getString(columnIndex);
						if (!MetrixStringHelper.isNullOrEmpty(value))
							errorKeys.put(column, value);
						else
							LogManager.getInstance().error("Missing value for Primary Key Column - " + column);

					}
				}

				cursor.moveToNext();
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return errorKeys;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) mErrorSimpleAdapter.getData().get(position);

		try {
			int codelessScreenId = -1;
			if(selectedItem.containsKey("mm_message_out.codeless_screen_id")){
				String strCodelessScreenId = selectedItem.get("mm_message_out.codeless_screen_id");
				if(!MetrixStringHelper.isNullOrEmpty(strCodelessScreenId))
					codelessScreenId = Integer.parseInt(strCodelessScreenId);
			}
						
			String messageId = selectedItem.get("mm_message_out.message_id");
			String activityName = selectedItem.get("mm_message_out.activity_name");
			String transactionDesc = selectedItem.get("mm_message_out.transaction_desc");
			if (!MetrixStringHelper.isNullOrEmpty(transactionDesc) && transactionDesc.contains("(")) {
				transactionDesc = transactionDesc.substring(0, selectedItem.get("mm_message_out.transaction_desc").indexOf("("));
			}
			String errorMessage = selectedItem.get("mm_message_in.message");
			String tableName = selectedItem.get("mm_message_out.table_name");
	
			if (!MetrixStringHelper.isNullOrEmpty(selectedItem.get("mm_message_out.metrix_log_id"))) {
				int logId = Integer.parseInt(selectedItem.get("mm_message_out.metrix_log_id"));
				correctError(messageId, activityName, transactionDesc, errorMessage, tableName, logId, codelessScreenId);
			} else {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,AndroidResourceHelper.getMessage("SyncErrorCannotCorrect"));
				return;
			}
		}
		catch(Exception ex) {
			if(ex.getMessage()!=null)
				LogManager.getInstance().error(ex);
		}
	}

	/**
	 * sub-class of AsyncTask for pinging the Mobile Service and show status on
	 * UI
	 */
	public class PingTask extends AsyncTask<MetrixSyncManager, Integer, ConnectionStatus> {
		// -- run intensive processes here
		// -- notice that the datatype of the first param in the class
		// definition matches the param passed to this method
		// -- and that the datatype of the last param in the class definition
		// matches the return type of this mehtod
		@Override
		protected ConnectionStatus doInBackground(MetrixSyncManager... params) {
			if (SettingsHelper.getSyncPause(mCurrentActivity) == true)
				return ConnectionStatus.Pause;

			if (params[0].getIsSyncRunning() == false) {
				params[0].setIsSyncRunning(true);
				try {
					if (params[0].pingMobileService()) {
						mLastConnectionStatus = ConnectionStatus.Connected;
						return ConnectionStatus.Connected;
					} else {
						mLastConnectionStatus = ConnectionStatus.Disconnected;
						return ConnectionStatus.Disconnected;
					}
				} catch (Exception ex) {
					LogManager.getInstance().error(ex);
				} finally {
					params[0].setIsSyncRunning(false);
				}
			}

			return ConnectionStatus.Unknown;
		}

		// gets called just before thread begins
		@Override
		protected void onPreExecute() {
			LogManager.getInstance().info("com.metrix.metrixmobile.onPreExecute()");
			super.onPreExecute();

		}

		// called from the publish progress
		// notice that the datatype of the second param gets passed to this
		// method
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			LogManager.getInstance().info("com.metrix.metrixmobile.onProgressUpdate(): " + String.valueOf(values[0]));
		}

		// called if the cancel button is pressed
		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		// called as soon as doInBackground method completes
		// notice that the third param gets passed to this method
		@Override
		protected void onPostExecute(ConnectionStatus result) {
			super.onPostExecute(result);
			LogManager.getInstance().info("com.metrix.metrixmobile.onPostExecute(): " + result);
			changeStatus(result);
		}
	}

	@Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {
		if(position.length > 0)
			mSelectedPosition = position[0];

		Object listItem = mErrorSimpleAdapter.getData().get(mSelectedPosition);
		HashMap<String, String> selectedItem = (HashMap<String, String>) listItem;

		MetrixActionView metrixActionView = getMetrixActionView();
		Menu menu = metrixActionView.getMenu();

		String messageIdOut = selectedItem.get("mm_message_out.message_id");
		if(!MetrixStringHelper.isNullOrEmpty(messageIdOut))		{
			String outputMessage = MetrixDatabaseManager.getFieldStringValue("select message from mm_message_out where message_id ="+messageIdOut);

			if(outputMessage.contains("perform_")){
				SyncMonitorMetrixActionView.onCreateMetrixActionViewDelete(menu);
			}
			else
				SyncMonitorMetrixActionView.onCreateMetrixActionView(menu);
		}
		else
			SyncMonitorMetrixActionView.onCreateMetrixActionViewDelete(menu);

		return super.OnCreateMetrixActionView(view);
	}
	
	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		Object listItem = mErrorSimpleAdapter.getData().get(mSelectedPosition);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) listItem;

		final String message_id = getMessageInfo(selectedItem.get("mm_message_out.message_id"));
		final String activity_name = getMessageInfo(selectedItem.get("mm_message_out.activity_name"));
		final String transaction_desc = getMessageInfo(selectedItem.get("mm_message_out.transaction_desc"));
		final String error_message = getMessageInfo(selectedItem.get("mm_message_in.message"));
		final String table_name = getMessageInfo(selectedItem.get("mm_message_out.table_name"));
		final String message_id_in = getMessageInfo(selectedItem.get("mm_message_in.message_id"));
		
		int codelessScreenId = -1;
		if(selectedItem.containsKey("mm_message_out.codeless_screen_id")){
			String strCodelessScreenId = selectedItem.get("mm_message_out.codeless_screen_id");
			if(!MetrixStringHelper.isNullOrEmpty(strCodelessScreenId))
				codelessScreenId = Integer.parseInt(strCodelessScreenId);
		}

		final int log_id;

		if (selectedItem.get("mm_message_out.metrix_log_id") != null
				&& !MetrixStringHelper.isNullOrEmpty(selectedItem.get("mm_message_out.metrix_log_id")))  {
			log_id = Integer.parseInt(selectedItem.get("mm_message_out.metrix_log_id"));
		} else {
			// in some scenarios, the upload message does not create row in
			// trans_log table
			// with the metrix_log_id. Use log_id = -1 to indicate no trans_log
			// row for this message.
			log_id = -1;
		}

		if (menuItem.getTitle().toString().compareToIgnoreCase(SyncMonitorMetrixActionView.CORRECT) == 0) {
			correctError(message_id, activity_name, transaction_desc, error_message, table_name, log_id, codelessScreenId);
		} else if (menuItem.getTitle().toString().compareToIgnoreCase(SyncMonitorMetrixActionView.DELETE) == 0) {
			try {
				if(MetrixStringHelper.isNullOrEmpty(message_id) && !MetrixStringHelper.isNullOrEmpty(message_id_in)) {
					MessageHandler.deleteMessage("mm_message_in", Long.parseLong(message_id_in));
				}
				else {
					Hashtable<String, String> dataRow = MetrixDatabaseManager.getFieldStringValues("mm_message_out", new String[]{"table_name", "transaction_type", "metrix_log_id"}, "message_id=" + message_id);

					if (dataRow != null && dataRow.size() > 0) {
						final String mTableName = dataRow.get("table_name");
						final String mTransType = dataRow.get("transaction_type");
						String mTranslationMessageId = "";
						final String mOutMessage = MessageHandler.getMessageFromTransLogTable(mTransType, mTableName, log_id);

						if(!MetrixStringHelper.isNullOrEmpty(mTableName)) {
							if (mTransType.equalsIgnoreCase("INSERT"))
								mTranslationMessageId = "ConfirmDelSyncErrInsert";
							else if (mTransType.equalsIgnoreCase("UPDATE"))
								mTranslationMessageId = "ConfirmDelSyncErrUpdate";
							else if (mTransType.equalsIgnoreCase("DELETE"))
								mTranslationMessageId = "ConfirmDelSyncErrDelete";

							mDeleteAlert = new AlertDialog.Builder(this).create();
							mDeleteAlert.setTitle(AndroidResourceHelper.getMessage("ConfirmDeleteTitle"));
							mDeleteAlert.setMessage(AndroidResourceHelper.getMessage(mTranslationMessageId));
							mDeleteAlert.setButton(AndroidResourceHelper.getMessage("Yes"), new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									try {
										MessageHandler.deleteMessageAndRelatedMessages(message_id, table_name, message_id_in, log_id);
										LogManager.getInstance(MobileApplication.getAppContext()).error(String.format("Deleted error message with message_id %1s for %2s table %3s", message_id, mTransType, mTableName), "");
										LogManager.getInstance(MobileApplication.getAppContext()).error(String.format("Deleted error message: %1s ", mOutMessage), "");
									} catch (Exception ex) {
										LogManager.getInstance(MobileApplication.getAppContext()).error(ex);
									}

									populateWaitingList();
									populateReadyList();
									populateSentList();
									populateErrorList();
								}
							});
							mDeleteAlert.setButton2(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							});
							mDeleteAlert.show();
						}
						else {
							try {
								final String mMessageOutMessage = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "message", "message_id="+message_id);
								MessageHandler.deleteMessageAndRelatedMessages(message_id, table_name, message_id_in, log_id);

								if (!MetrixStringHelper.isNullOrEmpty(mMessageOutMessage))
									LogManager.getInstance().error(String.format("Deleted error message with message_id %1s for record %2s", message_id, mMessageOutMessage));

								populateWaitingList();
								populateReadyList();
								populateSentList();
								populateErrorList();
							} catch (Exception ex) {
								LogManager.getInstance(MobileApplication.getAppContext()).error(ex);
							}
						}
					}
					else {
						// No table_name recorded, it could be a perform MPM message
						MessageHandler.deleteMessage("mm_message_in", Long.parseLong(message_id_in));

						populateWaitingList();
						populateReadyList();
						populateSentList();
						populateErrorList();
					}
				}
			} catch (Exception ex) {
				LogManager.getInstance(this).error(ex);
			}
		} else if (menuItem.getTitle().toString().compareToIgnoreCase(SyncMonitorMetrixActionView.RESEND) == 0) {
			try {
				ArrayList<DataField> updateFields = new ArrayList<DataField>();
				updateFields.add(new DataField("status", MessageStatus.READY.toString()));
				updateFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));

				MetrixDatabaseManager.updateRow("mm_message_out", updateFields, "message_id=" + message_id);
				MessageHandler.deleteMessage("mm_message_in", Long.parseLong(message_id_in));

				populateWaitingList();
				populateReadyList();
				populateSentList();
				populateErrorList();
			} catch (Exception ex) {
				LogManager.getInstance(this).error(ex);
			}
		}
		return super.onMetrixActionViewItemClick(menuItem);
	}
}
