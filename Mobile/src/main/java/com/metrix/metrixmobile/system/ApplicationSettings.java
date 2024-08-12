package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.gson.JsonObject;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.MessageType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.LogManager.Level;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPasswordHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.LocationBroadcastReceiver;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixImportantInformation;
import com.metrix.metrixmobile.oidc.AuthStateManager;
import com.metrix.metrixmobile.oidc.Configuration;
import com.metrix.metrixmobile.oidc.ConfigurationManager;
import com.metrix.metrixmobile.oidc.LogoutHandler;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.EndSessionRequest;
import net.openid.appauth.browser.AnyBrowserMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class ApplicationSettings extends MetrixActivity implements View.OnClickListener {
	Button mSendLogButton, mSendDatabaseButton, mRefreshDesignButton, mLogoutAndCloseButton, mChangePasswordButton;
	FloatingActionButton  mSaveButton, mInitButton;
	CheckBox mManualLoginCheckBox, mPauseSyncCheckBox, mPlaySoundCheckBox;
	EditText mServiceAddress;
	Spinner mLogLevel, mSyncInterval;
	AlertDialog mInitAlert, mRefreshDesignAlert;
	Context mCtx;
	private static final int READ_PERMISSION_REQUEST_CODE = 6789;

	protected static Activity mActivity = null;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	private AuthorizationService mAuthService;
	private AuthStateManager mStateManager;
	private Configuration mConfiguration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.application_settings);
		mActivity = this;
		mCtx = this;
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mManualLoginCheckBox = (CheckBox) findViewById(R.id.requireLogin);
		mPauseSyncCheckBox = (CheckBox) findViewById(R.id.pauseSync);
		mPlaySoundCheckBox = (CheckBox) findViewById(R.id.playSound);
		mSyncInterval = (Spinner) findViewById(R.id.appsetting__sync_interval);
		mServiceAddress = (EditText) findViewById(R.id.appsetting__service_address);
		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mSendLogButton = (Button) findViewById(R.id.sendLog);
		mSendDatabaseButton = (Button) findViewById(R.id.sendDatabase);
		mInitButton = (FloatingActionButton) findViewById(R.id.Init);
		mRefreshDesignButton = (Button) findViewById(R.id.refreshCustomDesign);
		mLogoutAndCloseButton = (Button) findViewById(R.id.logoutAndClose);
		mChangePasswordButton = (Button) findViewById(R.id.changePassword);
		mLogLevel = (Spinner) findViewById(R.id.appsetting__logLevel);

		boolean isManualLogin = SettingsHelper.getManualLogin(this);
		mManualLoginCheckBox.setChecked(isManualLogin);
		MetrixControlAssistant.setParentVisibility(mManualLoginCheckBox.getId(), mLayout, View.VISIBLE);

		if (SettingsHelper.getSyncPause(this)) {
			mPauseSyncCheckBox.setChecked(true);
		}

		String serviceAddress = SettingsHelper.getServiceAddress(this);
		try {
			MetrixControlAssistant.setValue(R.id.appsetting__service_address, mLayout, serviceAddress);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		setControls();

		String allowEdits = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ALLOW_ADMIN_EDITS_IN_MOBILE'");
		if (allowEdits.compareToIgnoreCase("N") == 0) {
			//Search from makeText
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NotAllowedChngScreen"));
			mSaveButton.setEnabled(false);
			mManualLoginCheckBox.setEnabled(false);
			mPauseSyncCheckBox.setEnabled(false);
			mPlaySoundCheckBox.setEnabled(false);
			//mBluetoothScanButton.setEnabled(false);
			mSyncInterval.setEnabled(false);
			mServiceAddress.setEnabled(false);
		} else {
			String allowSound = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_PLAY_SOUND'");

			if (allowSound.compareToIgnoreCase("Y") == 0) {
				// User settings override the system setting
				if (SettingsHelper.getPlaySound(this)) {
					mPlaySoundCheckBox = (CheckBox) findViewById(R.id.playSound);
					mPlaySoundCheckBox.setChecked(true);
					SettingsHelper.savePlaySound(this, true);
				} else {
					SettingsHelper.savePlaySound(this, false);
				}
			} else {
				SettingsHelper.savePlaySound(this, false);
				LinearLayout playSoundLayout = (LinearLayout) findViewById(R.id.enableSoundLayout);
				playSoundLayout.setVisibility(View.GONE);
			}
		}

		mManualLoginCheckBox.setOnClickListener(this);
		mPauseSyncCheckBox.setOnClickListener(this);
		mPlaySoundCheckBox.setOnClickListener(this);
		mSaveButton.setOnClickListener(this);
		//mBluetoothScanButton.setOnClickListener(this);
		mSendLogButton.setOnClickListener(this);
		mSendDatabaseButton.setOnClickListener(this);
		mInitButton.setOnClickListener(this);
		mRefreshDesignButton.setOnClickListener(this);
		mLogoutAndCloseButton.setOnClickListener(this);
		mChangePasswordButton.setOnClickListener(this);

		mSyncInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				try {
					warnIfIntervalIsLessThanMinute(Integer.valueOf(MetrixControlAssistant.getValue(mSyncInterval)));
				} catch (NumberFormatException e) {
					LogManager.getInstance().error(e);
				} catch (Exception e) {
					LogManager.getInstance().error(e);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		String aadSuccess = SettingsHelper.getStringSetting(this, SettingsHelper.AZURE_AD_SUCCESS);

		if(!MetrixStringHelper.isNullOrEmpty(aadSuccess)&& aadSuccess.equalsIgnoreCase("Y")) {
			mManualLoginCheckBox.setChecked(false);
			mManualLoginCheckBox.setEnabled(false);
		}

		if(SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ENABLED).equalsIgnoreCase("ON"))
			mChangePasswordButton.setEnabled(false);
	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.ServiceAddress55dd55a0, "ServiceAddress"));
		resourceStrings.add(new ResourceValueObject(R.id.SyncInterval7086a07f, "SyncInterval"));
		resourceStrings.add(new ResourceValueObject(R.id.logText, "SelectLogType"));
		resourceStrings.add(new ResourceValueObject(R.id.empty_space7a21d8b8, "EmptySpace"));
		resourceStrings.add(new ResourceValueObject(R.id.pauseSync, "PauseSync"));
		resourceStrings.add(new ResourceValueObject(R.id.empty_space1ebcfaf8, "EmptySpace"));
		resourceStrings.add(new ResourceValueObject(R.id.requireLogin, "RequireLogin"));
		resourceStrings.add(new ResourceValueObject(R.id.empty_spaced46f86ed, "EmptySpace"));
		resourceStrings.add(new ResourceValueObject(R.id.playSound, "PlaySound"));
		//resourceStrings.add(new ResourceValueObject(R.id.bluetoothScan, "DiscoverBluetoothDevice"));
		resourceStrings.add(new ResourceValueObject(R.id.sendLog, "SendLog"));
		resourceStrings.add(new ResourceValueObject(R.id.sendDatabase, "SendDatabase"));
		resourceStrings.add(new ResourceValueObject(R.id.refreshCustomDesign, "RefreshCustomDesign"));
		resourceStrings.add(new ResourceValueObject(R.id.important_information_heading, "ImportantInformation"));
		resourceStrings.add(new ResourceValueObject(R.id.logoutAndClose, "LogoutAndClose"));
		resourceStrings.add(new ResourceValueObject(R.id.changePassword, "ChangePassword"));
		AndroidResourceHelper.setResourceValues(mLogoutAndCloseButton, "LogoutAndClose");
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		CoordinatorLayout.LayoutParams saveParams = (CoordinatorLayout.LayoutParams) mSaveButton.getLayoutParams();
		saveParams.setMargins(0, 0, (int) getResources().getDimension(R.dimen.fab_margin), (int) getResources().getDimension(R.dimen.fab_2_margin));
		saveParams.setAnchorId(R.id.Init);
		mSaveButton.setLayoutParams(saveParams);

		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mFABList.add(mInitButton);
		mFABList.add(mSaveButton);

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

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.save:
				saveAndFinish();
				break;
			case R.id.pauseSync:
				if (mPauseSyncCheckBox.isChecked()) {
					SettingsHelper.saveSyncPause(this, true);
					MobileApplication.stopSync(this);
				} else {
					SettingsHelper.saveSyncPause(this, false);
					MobileApplication.resumeSync(this);
				}
				break;
			case R.id.requireLogin:
				if (mManualLoginCheckBox.isChecked()) {
					SettingsHelper.saveManualLogin(this, true);
				} else {
					SettingsHelper.saveManualLogin(this, false);
				}
				break;
			case R.id.playSound:
				if (mPlaySoundCheckBox.isChecked()) {
					SettingsHelper.savePlaySound(this, true);
				} else {
					SettingsHelper.savePlaySound(this, false);
				}
				break;
			case R.id.sendLog:
				try {
					LogManager.getInstance(this).sendEmail(this);
				} catch (Exception ex) {
					LogManager.getInstance(this).error(ex);
				}
				break;
			case R.id.sendDatabase:

				mInitAlert = new AlertDialog.Builder(this).create();
				mInitAlert.setTitle(AndroidResourceHelper.getMessage("SendDatabase"));
				mInitAlert.setMessage(AndroidResourceHelper.getMessage("ExptedDBFromQueryScnConfirm"));
				mInitAlert.setButton(AndroidResourceHelper.getMessage("Yes"), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							LogManager.getInstance(MobileApplication.getAppContext()).sendDatabase(mCtx);
						} catch (Exception ex) {
							LogManager.getInstance(MobileApplication.getAppContext()).error(ex);
						}
					}
				});
				mInitAlert.setButton2(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				mInitAlert.show();
				break;
			case R.id.refreshCustomDesign:
				if (mobileDesignChangesInProgress()) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobDesignCannotProcYetErr"));
					return;
				}

				mRefreshDesignAlert = new AlertDialog.Builder(this).create();
				mRefreshDesignAlert.setTitle(AndroidResourceHelper.getMessage("RefreshCustomDesign"));
				mRefreshDesignAlert.setMessage(AndroidResourceHelper.getMessage("RefreshCustomDesignConfirm"));
				mRefreshDesignAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), designRefreshListener);
				mRefreshDesignAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), designRefreshListener);
				mRefreshDesignAlert.show();
				break;
			case R.id.Init:
				mInitAlert = new AlertDialog.Builder(this).create();
				mInitAlert.setTitle(AndroidResourceHelper.getMessage("Initialization"));
				mInitAlert.setMessage(AndroidResourceHelper.getMessage("AllTranWillDelConfirmInit"));
				mInitAlert.setButton(AndroidResourceHelper.getMessage("Yes"), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (SettingsHelper.getSyncPause(mCurrentActivity)) {
							SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
							if (syncPauseAlertDialog != null) {
								syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
									@Override
									public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
										startInitialize();
									}
								});
							}
						} else
							startInitialize();
					}
				});
				mInitAlert.setButton2(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				mInitAlert.show();
				break;
			case R.id.logoutAndClose:
				LogoutHandler logoutHandler = new LogoutHandler(this);
				logoutHandler.logout();
				break;
			case R.id.changePassword:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, ChangePassword.class);
				String strPerson = SettingsHelper.getRememberMe(this);
				intent.putExtra(ChangePassword.PERSON_ID, strPerson);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			default:
				super.onClick(v);
		}
	}

	private AuthorizationService createAuthorizationService() {
		try {
			AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();
			ConfigurationManager configurationManager = ConfigurationManager.getInstance();
			configurationManager.refreshConfiguration();
			mConfiguration = configurationManager.getCurrent();
			builder.setBrowserMatcher(AnyBrowserMatcher.INSTANCE); // it should match the same BrowserMatcher in ActiveDeviceOIDC class
			builder.setConnectionBuilder(mConfiguration.getConnectionBuilder());

			return new AuthorizationService(this, builder.build());
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return null;
	}

	private void startInitialize() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				if (mPauseSyncCheckBox.isChecked()) {
					SettingsHelper.saveSyncPause(mCurrentActivity, false);
				}

				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doInit(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					ApplicationSettings.this.runOnUiThread(new Runnable() {
						public void run() {
							MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobServNotAccessPleaseTryLater"));

							if (mInitAlert != null) {
								mInitAlert.dismiss();
							}
						}
					});
					return;
				}

				ApplicationSettings.this.runOnUiThread(new Runnable() {
					public void run() {
						LocationBroadcastReceiver.pop();
					}
				});

				if (mInitAlert != null) {
					mInitAlert.dismiss();
				}

				Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, SyncServiceMonitor.class);
				intent.putExtra("ShowInitDialog", true);
				intent.putExtra("StartLocation", true);
				MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
			}
		});

		thread.start();
	}

	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					saveAndFinish();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
			}
		}
	};

	DialogInterface.OnClickListener designRefreshListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startDesignRefresh();
								}
							});
						}
					} else
						startDesignRefresh();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mInitAlert != null) {
			mInitAlert.dismiss();
		}
	}

	public void saveAndFinish() {
		String syncInterval = MetrixControlAssistant.getValue(R.id.appsetting__sync_interval, mLayout);
		String serviceAddress = MetrixControlAssistant.getValue(R.id.appsetting__service_address, mLayout);
		String logLevel = MetrixControlAssistant.getValue(R.id.appsetting__logLevel, mLayout);

		if (MetrixStringHelper.isNullOrEmpty(syncInterval)) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnterTheSyncInterval"));
			return;
		}

		if (MetrixStringHelper.isNullOrEmpty(serviceAddress)) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnterTheServiceAddress"));
			return;
		}

		try {

			int storedInterval = SettingsHelper.getSyncInterval(this);
			int savedInterval = Integer.parseInt(syncInterval);

			if (storedInterval != savedInterval) {
				SettingsHelper.saveSyncInterval(this, Integer.parseInt(syncInterval));

				if(!mPauseSyncCheckBox.isChecked() && !SettingsHelper.getSyncPause(this)) {
					MobileApplication.stopSync(this);
					MobileApplication.startSync(this);
				}
			}
		} catch (Exception ex) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("InvalidIntervalSet"));
			return;
		}

		try {
			SettingsHelper.saveServiceAddress(this, serviceAddress);
		} catch (Exception ex) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("InvalidServiceAddr"));
			return;
		}

		try {
			SettingsHelper.saveLogLevel(this, logLevel);
			for (Level level : Level.values()) {
				if (logLevel.compareToIgnoreCase(level.toString()) == 0) {
					LogManager.getInstance(this).setLevel(level);
				}
			}
		} catch (Exception ex) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("InvalidLogLvl"));
			return;
		}

		finish();
	}

	private void setControls() {
		Spinner sLog = (Spinner) findViewById(R.id.appsetting__logLevel);
		SpinnerKeyValuePair items[] = new SpinnerKeyValuePair[4];
		items[0] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Debug"), "DEBUG");
		items[1] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Info"), "INFO");
		items[2] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Warning"), "WARN");
		items[3] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Error"), "ERROR");

		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(this, R.layout.spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sLog.setAdapter(adapter);

		String logLevel = SettingsHelper.getLogLevel(this);
		if (MetrixStringHelper.isNullOrEmpty(logLevel)) {
			SettingsHelper.saveLogLevel(this, logLevel);
			logLevel = "ERROR";
		}

		try {
			MetrixControlAssistant.setValue(sLog, logLevel);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		items = new SpinnerKeyValuePair[6];
		items[0] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("5Seconds"), "5");
		items[1] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("15Seconds"), "15");
		items[2] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("30Seconds"), "30");
		items[3] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("1Minute"), "60");
		items[4] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("5Minutes"), "300");
		items[5] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("1Hour"), "3600");
		adapter = new ArrayAdapter<SpinnerKeyValuePair>(this, R.layout.spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSyncInterval.setAdapter(adapter);

		int syncInterval = SettingsHelper.getSyncInterval(this);
		if (syncInterval != 0) {
			try {
				MetrixControlAssistant.setValue(mSyncInterval, String.valueOf(syncInterval));
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}

		warnIfIntervalIsLessThanMinute(syncInterval);

		return;
	}

	private void warnIfIntervalIsLessThanMinute(int syncInterval) {
		ViewGroup outerLayout = (ViewGroup) findViewById(R.id.outer_layout);
		if (syncInterval != 0 && syncInterval <= 59) {
			MetrixImportantInformation.reset(outerLayout, this);
			MetrixImportantInformation.add(outerLayout,
					AndroidResourceHelper.getMessage("SyncIntvlLessMinDecrOfBatLife"));
		} else {
			MetrixImportantInformation.reset(outerLayout, this);
		}
	}

	public static boolean doInit(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		String user_id = SettingsHelper.getActivatedUser(MobileApplication.getAppContext());
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		String dir = MetrixAttachmentManager.getInstance().getAttachmentPath();
		MetrixAttachmentManager.getInstance().deleteAttachmentFiles(dir);

		HashMap<String, MetrixTableStructure> tableCache = MobileApplication.getTableDefinitionsFromCache();
		ArrayList<String> systemTableNames = getSystemTableNames();
		ArrayList<String> mobileNoLogDesignTableNames = MobileApplication.getMobileNoLogDesignTableNames();
		ArrayList<String> statements = new ArrayList<String>();

		ArrayList<Hashtable<String, String>> triggerNames = MetrixDatabaseManager.getFieldStringValuesList("sqlite_master", new String[] {"name"}, "type = 'trigger'");
		for (Hashtable<String, String> triggerRow : triggerNames) {
			// Drop all triggers, so that subsequent deletes can be run as truncates.
			String triggerName = triggerRow.get("name");
			statements.add("drop trigger " + triggerName);
		}

		for (String keyName : tableCache.keySet()) {
			statements.add("delete from " + keyName);
			statements.add("delete from " + keyName + "_log");
		}

		for (String keyName : systemTableNames) {
			statements.add("delete from " + keyName);
		}

		for (String keyName : mobileNoLogDesignTableNames) {
			statements.add("delete from " + keyName);
		}

		MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());

		if (MetrixDatabaseManager.executeSqlArray(statements, false)) {
			clearDesignerCaches();
			String url = MetrixSyncManager.generateRestfulServiceUrl(baseUrl, MessageType.Initialization, user_id, device_id, null, null);

			try {
				String personId = User.getUser().personId;
				String password = MetrixPasswordHelper.getUserPassword();

				MetrixPublicCache.instance.addItem("person_id", personId);

				MetrixFileHelper.deleteFiles(mActivity, MetrixAttachmentManager.getInstance().getAttachmentPath(), READ_PERMISSION_REQUEST_CODE);

				String loginMessage = MetrixSyncManager.getPerformLogin(personId, password);
				JsonObject properties = MetrixSyncManager.generatePostBodyJSONWithAuthentication();
				properties.addProperty("login_message", loginMessage);
				String postBody = MetrixSyncManager.preparePostBodyForTransmission(properties);

				@SuppressWarnings("unused")
				String response = remote.executePost(url, null, postBody);
			} catch (Exception ex) {
				LogManager.getInstance(activity).error(ex);
				MetrixUIHelper.showErrorDialogOnGuiThread(activity, ex.getMessage());
				return false;
			}
		} else {
			MetrixUIHelper.showErrorDialogOnGuiThread(activity, AndroidResourceHelper.getMessage("LocDbNotAbleToCleanInitFail"));
			return false;
		}

		return true;
	}

	private void startDesignRefresh() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				if (mPauseSyncCheckBox.isChecked()) {
					SettingsHelper.saveSyncPause(mCurrentActivity, false);
				}

				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doDesignRefresh(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					ApplicationSettings.this.runOnUiThread(new Runnable() {
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

	@SuppressLint("MissingSuperCall")
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case READ_PERMISSION_REQUEST_CODE:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());
				}
				break;
		}
	}
}

