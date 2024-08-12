package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;

import android.content.Context;
import android.location.Location;
import android.os.Handler;

import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixWorkStatusAssistant;

public class LocationBroadcastReceiver {
	private static Handler handler = new Handler();
	private static Runnable runnable = new Runnable() {
		@Override
		public void run() {
				LocationBroadcastReceiver.updateLocation();
			
				handler.removeCallbacks(this);
				
				final String listenerInterval = MetrixDatabaseManager.getAppParam("GPS_LOCATION_INTERVAL_MIN");
				if ((!MetrixStringHelper.isNullOrEmpty(listenerInterval)) && MetrixStringHelper.isInteger(listenerInterval) && Integer.valueOf(listenerInterval) > 0) {
					handler.postDelayed(this, Integer.valueOf(listenerInterval) * 1000 * 60);
				}
			}
	};

	public static void setLocationListenerStatus() {
		final String listenerInterval = MetrixDatabaseManager.getAppParam("GPS_LOCATION_INTERVAL_MIN");
		if ((!MetrixStringHelper.isNullOrEmpty(listenerInterval)) && MetrixStringHelper.isInteger(listenerInterval) && Integer.valueOf(listenerInterval) > 0) {
			if(MetrixRoleHelper.isGPSFunctionEnabled("GPS_INTERVAL")) {
				String workStatus = MetrixDatabaseManager.getFieldStringValue("person", "work_status", "person_id = '" + User.getUser().personId + "'");
	
				if (!MetrixStringHelper.isNullOrEmpty(workStatus) && MetrixWorkStatusAssistant.workStatusRequiresGPSTracking(workStatus)) {
					handler.postDelayed(runnable, Integer.valueOf(listenerInterval) * 1000 * 60);
				} else {
					handler.removeCallbacks(runnable);
				}
			}
		}
	}
	
	public static void pop() {
		handler.removeCallbacks(runnable);
	}
	
	private static void updateLocation() {
		Location currentLocation = MetrixLocationAssistant.getCurrentLocation((Context) MetrixPublicCache.instance.getItem(Global.MobileApplication));

		try {
			if (currentLocation != null) {
				String rowId = MetrixDatabaseManager.getFieldStringValue("person", "metrix_row_id", "person_id='" + User.getUser().personId + "'");

				if (MetrixStringHelper.isNullOrEmpty(rowId)) {
					LogManager.getInstance().error("Database query problem caused LocationBroadcastReceiver error");
					return;
				}

				MetrixSqlData data = new MetrixSqlData("person", MetrixTransactionTypes.UPDATE, "metrix_row_id=" + rowId);

				// these two values are currently in US format
				String currentLatitude = String.valueOf(currentLocation.getLatitude());
				String currentLongitude = String.valueOf(currentLocation.getLongitude());

				Hashtable<String, String> lastCoordinate = MetrixDatabaseManager.getFieldStringValues("person", new String[] { "geocode_lat", "geocode_long" },
						"person_id='" + User.getUser().personId + "'");
				String previousLatitude = lastCoordinate.get("geocode_lat");
				String previousLongitude = lastCoordinate.get("geocode_long");

				previousLatitude = MetrixFloatHelper.convertNumericFromDBToForcedLocale(previousLatitude, Locale.US);
				previousLongitude = MetrixFloatHelper.convertNumericFromDBToForcedLocale(previousLongitude, Locale.US);
				
				// only create the update if the user's position has changed
				// since the last time we recorded it
				if (MetrixStringHelper.isNullOrEmpty(previousLatitude)
						|| MetrixStringHelper.isNullOrEmpty(previousLongitude)
						|| MetrixLocationAssistant
								.distanceIsGreaterThanAllowedTolerance(currentLatitude, currentLongitude, previousLatitude, previousLongitude)) {
					currentLatitude = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(currentLatitude, Locale.US);
					currentLongitude = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(currentLongitude, Locale.US);
					
					data.dataFields.add(new DataField("metrix_row_id", rowId));
					data.dataFields.add(new DataField("person_id", User.getUser().personId));
					data.dataFields.add(new DataField("geocode_lat", currentLatitude));
					data.dataFields.add(new DataField("geocode_long", currentLongitude));
					data.dataFields.add(new DataField("location_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, ISO8601.Yes, true)));				
					data.dataFields.add(new DataField("modified_by", User.getUser().personId));
					data.dataFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, ISO8601.Yes, true)));

					ArrayList<MetrixSqlData> dataList = new ArrayList<MetrixSqlData>();
					dataList.add(data);

					MetrixTransaction transactionInfo = new MetrixTransaction();
					MetrixUpdateManager.update(dataList, true, transactionInfo, AndroidResourceHelper.getMessage("Location"), null);
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}		
	}
}