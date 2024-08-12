package com.metrix.metrixmobile.global;

import java.util.ArrayList;
import java.util.Hashtable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;

import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.Home;
import com.metrix.metrixmobile.LocationBroadcastReceiver;

public class MetrixWorkStatusAssistant {

	private Activity mActivity;
	private ArrayList<String> mStatuses = new ArrayList<String>();
	private boolean mGoToHome = false;
	
	public void displayStatusDialog(Context context, Activity activity, boolean goToHome) {
		String workStatus = MetrixDatabaseManager.getFieldStringValue("person", "work_status", "person_id='" + User.getUser().personId + "'");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		if (MetrixStringHelper.isNullOrEmpty(workStatus)) {
			builder.setTitle(AndroidResourceHelper.getMessage("Status"));
		} else {
			builder.setTitle(AndroidResourceHelper.getMessage("Status1Arg", workStatus));
		}

		mActivity = activity;
		mGoToHome = goToHome;
		
		ArrayList<Hashtable<String, String>> statuses = null;
		
		if (MetrixStringHelper.isNullOrEmpty(workStatus)) {
			statuses = MetrixDatabaseManager.getFieldStringValuesList("select description from global_code_table where code_name = 'WORK_STATUS'");
		} else {
			statuses = MetrixDatabaseManager.getFieldStringValuesList("select description from global_code_table where code_name = 'WORK_STATUS' and code_value != (select work_status from person where person_id = '" + User.getUser().personId + "')");
		}

		if (statuses != null) {
			for (Hashtable<String, String> status : statuses) {
				mStatuses.add(status.get("description"));
			}
		}

		CharSequence[] items = mStatuses.toArray(new CharSequence[mStatuses.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int pos) {
				MetrixSqlData personData = new MetrixSqlData("person", MetrixTransactionTypes.UPDATE);
				personData.dataFields.add(new DataField("person_id", User.getUser().personId));
				personData.dataFields.add(new DataField("metrix_row_id", MetrixDatabaseManager.getFieldStringValue("person", "metrix_row_id", "person_id = '" + User.getUser().personId + "'")));
				personData.dataFields.add(new DataField("work_status", MetrixDatabaseManager.getFieldStringValue("global_code_table", "code_value", "code_name = 'WORK_STATUS' and description = '" + mStatuses.get(pos) + "'")));
				personData.dataFields.add(new DataField("work_status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, ISO8601.Yes, true)));
				personData.dataFields.add(new DataField("modified_by", User.getUser().personId));
				personData.dataFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, ISO8601.Yes, true)));

				String recordGPS = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_PERSON_STATUS_UPDATE'");
				if (!MetrixStringHelper.isNullOrEmpty(recordGPS) && recordGPS.compareToIgnoreCase("Y") == 0) {
					boolean updatePerson = MetrixRoleHelper.isGPSFunctionEnabled("GPS_PERSON");
					
					if(updatePerson) {
						Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mActivity);
						
						if (currentLocation != null) {
							String currentLatitude = MetrixFloatHelper.convertNumericFromUIToDB(String.valueOf(currentLocation.getLatitude()));
							String currentLongitude = MetrixFloatHelper.convertNumericFromUIToDB(String.valueOf(currentLocation.getLongitude()));
							
							personData.dataFields.add(new DataField("geocode_lat", currentLatitude));
							personData.dataFields.add(new DataField("geocode_long", currentLongitude));
							personData.dataFields.add(new DataField("location_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, ISO8601.Yes, true)));
						}
					}
				}

				personData.filter = "person_id='" + User.getUser().personId + "'";
				ArrayList<MetrixSqlData> personTransaction = new ArrayList<MetrixSqlData>();
				personTransaction.add(personData);

				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("", "");
				MetrixUpdateManager.update(personTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("Person"), mActivity);

				LocationBroadcastReceiver.setLocationListenerStatus();
				
				if (mGoToHome) {
					Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, Home.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
					MetrixActivityHelper.startNewActivity(mActivity, intent);
				}
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	public static boolean workStatusRequiresGPSTracking(String workStatus) {
		String workStatusValuesRequiringTracking = MetrixDatabaseManager.getAppParam("GPS_LOCATION_INTERVAL_WORK_STATUS");
		
		if (MetrixStringHelper.isNullOrEmpty(workStatusValuesRequiringTracking)) {
			return false;
		} else {
			String[] workStatusValues = workStatusValuesRequiringTracking.split(",");
			for (String workStatusValue : workStatusValues) {
				if (workStatus.compareToIgnoreCase(workStatusValue) == 0) {
					return true;
				}
			}
			return false;
		}
	}
}
