package com.metrix.metrixmobile.system;

import java.util.ArrayList;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.R;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.LogManager.Level;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

public class DemoApplicationSettings extends MetrixActivity implements View.OnClickListener {
	Button mSaveButton, /*mBluetoothScanButton,*/ mSendLogButton, mInitButton;
	CheckBox mManualLoginCheckBox, mPauseSyncCheckBox, mPlaySoundCheckBox;
	EditText mServiceAddress;
	Spinner mLogLevel, mSyncInterval;
	AlertDialog mInitAlert;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.application_settings);

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mManualLoginCheckBox = (CheckBox) findViewById(R.id.requireLogin);
		mPauseSyncCheckBox = (CheckBox) findViewById(R.id.pauseSync);
		mPlaySoundCheckBox = (CheckBox) findViewById(R.id.playSound);
		mSyncInterval = (Spinner) findViewById(R.id.appsetting__sync_interval);
		mServiceAddress = (EditText) findViewById(R.id.appsetting__service_address);
		mSaveButton = (Button) findViewById(R.id.save);
		//mBluetoothScanButton = (Button) findViewById(R.id.bluetoothScan);
		mSendLogButton = (Button) findViewById(R.id.sendLog);
		mInitButton = (Button) findViewById(R.id.Init);
		mLogLevel = (Spinner) findViewById(R.id.appsetting__logLevel);

		if (SettingsHelper.getManualLogin(this)) {
			mManualLoginCheckBox = (CheckBox) findViewById(R.id.requireLogin);
			mManualLoginCheckBox.setChecked(true);
		}

		if (SettingsHelper.getSyncPause(this)) {
			mPauseSyncCheckBox = (CheckBox) findViewById(R.id.pauseSync);
			mPauseSyncCheckBox.setChecked(true);
		}

		String serviceAddress = SettingsHelper.getServiceAddress(this);
		try {
			MetrixControlAssistant.setValue(R.id.appsetting__service_address, mLayout, serviceAddress);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		setControls();
		mManualLoginCheckBox.setEnabled(false);
		mPauseSyncCheckBox.setEnabled(false);
		mServiceAddress.setEnabled(false);
		mSendLogButton.setEnabled(false);
		mInitButton.setEnabled(false);
		mLogLevel.setEnabled(false);
		
		String allowSound = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_PLAY_SOUND'");

		if (allowSound.compareToIgnoreCase("Y") == 0) {
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

		mPlaySoundCheckBox.setOnClickListener(this);
		mSaveButton.setOnClickListener(this);
		//mBluetoothScanButton.setOnClickListener(this);

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

		this.helpText = AndroidResourceHelper.getMessage("ScrnDescAppSettings");
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.save:
			saveAndFinish();
			break;
		case R.id.playSound:
			if (mPlaySoundCheckBox.isChecked()) {
				SettingsHelper.savePlaySound(this, true);
			} else {
				SettingsHelper.savePlaySound(this, false);
			}
			break;
			/*
		case R.id.bluetoothScan:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, BluetoothDiscovery.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
			*/
		}
	}

	public void saveAndFinish() {
		String syncInterval = MetrixControlAssistant.getValue(R.id.appsetting__sync_interval, mLayout);
		String serviceAddress = MetrixControlAssistant.getValue(R.id.appsetting__service_address, mLayout);
		String logLevel = MetrixControlAssistant.getValue(R.id.appsetting__logLevel, mLayout);

		if (MetrixStringHelper.isNullOrEmpty(syncInterval)) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnterSyncInt"));
			return;
		}

		if (MetrixStringHelper.isNullOrEmpty(serviceAddress)) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnterServiceAddr"));
			return;
		}

		try {
			SettingsHelper.saveSyncInterval(this, Integer.parseInt(syncInterval));
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
		SpinnerKeyValuePair items[] = null;

		items = new SpinnerKeyValuePair[5];
		items[0] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("None"), "");
		items[1] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Debugging"), "DEBUG");
		items[2] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Informational"), "INFO");
		items[3] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Warnings"), "WARN");
		items[4] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Errors"), "ERROR");

		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(this, R.layout.spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sLog.setAdapter(adapter);

		String logLevel = SettingsHelper.getLogLevel(this);
		if (!MetrixStringHelper.isNullOrEmpty(logLevel)) {
			try {
				MetrixControlAssistant.setValue(sLog, logLevel);
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}

		items = new SpinnerKeyValuePair[5];
		items[0] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("5Seconds"), "5");
		items[1] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("15Seconds"), "15");
		items[2] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("1Minute"), "60");
		items[3] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("5Minutes"), "300");
		items[4] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("1Hour"), "3600");
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
		LinearLayout importantInformationLayout = (LinearLayout) findViewById(R.id.important_information_bar);
		if (syncInterval == 0 || syncInterval > 15) {
			importantInformationLayout.setVisibility(View.GONE);
		}
		else {
			try {
				TextView information = (TextView) findViewById(R.id.important_information);
				MetrixControlAssistant.setValue(information, AndroidResourceHelper.getMessage("SyncIntervalWarning"));
				importantInformationLayout.setVisibility(View.VISIBLE);
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	public static ArrayList<String> getSystemTableNames() {
		ArrayList<String> systemTableNames = new ArrayList<String>();

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select name from sqlite_master where type ='table' AND name LIKE 'mm_message%'", null);
			// "select name from sqlite_master where type ='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE '%_log' AND name NOT LIKE 'mm_%'",

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			int i = 1;
			while (cursor.isAfterLast() == false) {

				String table_name = cursor.getString(0);
				systemTableNames.add(table_name);
				cursor.moveToNext();
				i = i + 1;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return systemTableNames;
	}
}