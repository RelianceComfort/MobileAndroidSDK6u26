package com.metrix.metrixmobile.system;

import java.util.ArrayList;

import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

public class RejectTaskBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		String taskId = extras.getString("TASK_ID");

		String taskStatus = MetrixDatabaseManager.getAppParam("REJECTED_TASK_STATUS");

		String rowId = MetrixDatabaseManager.getFieldStringValue("task", "metrix_row_id", "task_id = '" + taskId + "'");

		MetrixSqlData data = new MetrixSqlData("task", MetrixTransactionTypes.UPDATE, "task_id = " + taskId);
		data.dataFields.add(new DataField("metrix_row_id", rowId));
		data.dataFields.add(new DataField("task_id", taskId));
		data.dataFields.add(new DataField("task_status", taskStatus));
		DataField tStatusAsOf = new DataField("status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));		
		data.dataFields.add(tStatusAsOf);	
		
		String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
		boolean updateTask = MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK");
		
		if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("Y") == 0) {
			if(updateTask) {
				Location currentLocation = MetrixLocationAssistant.getCurrentLocation(context);

				if (currentLocation != null) {
					try {
						DataField gLat = new DataField("geocode_lat", MetrixFloatHelper.convertNumericFromUIToDB(Double.toString(currentLocation.getLatitude())));
						DataField gLong = new DataField("geocode_long", MetrixFloatHelper.convertNumericFromUIToDB(Double.toString(currentLocation.getLongitude())));
						
						data.dataFields.add(gLat);
						data.dataFields.add(gLong);									
					} catch (Exception e) {
						LogManager.getInstance(context).error(e);
					}
				}
			}
		}
		
		ArrayList<MetrixSqlData> dataList = new ArrayList<MetrixSqlData>();
		dataList.add(data);

		MetrixTransaction transactionInfo = new MetrixTransaction();
		MetrixUpdateManager.update(dataList, true, transactionInfo, AndroidResourceHelper.getMessage("Task"), null);
		
		NotificationManager notificationManager = (NotificationManager) MobileApplication.getAppContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		
		notificationManager.cancel(1);
	}

}
