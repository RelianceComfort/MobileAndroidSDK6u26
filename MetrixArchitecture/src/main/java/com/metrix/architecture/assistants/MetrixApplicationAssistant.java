package com.metrix.architecture.assistants;

import java.util.ArrayList;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;

/**
 * Contains helper methods to get meta data from the application's manifest file
 * about the client application. This includes things like the application's
 * version and default service URL.
 * 
 * @since 5.5
 */
public class MetrixApplicationAssistant {

	/***
	 * Gets the application version from the app's manifest.
	 * 
	 * @param context
	 *            The current activity being executed.
	 * @return The application version name.
	 */
	public static String getAppVersion(Context context) {
		String versionName = "";
		PackageInfo packageInfo;
		try {
			PackageManager manager = context.getPackageManager();
			packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
			versionName = packageInfo.versionName+"."+packageInfo.versionCode;
			//versionName = versionName.replace(" Build ",".");
		} catch (NameNotFoundException e) {
			LogManager.getInstance().error(e);
		}
		return versionName;
	}

	/**
	 * Gets the architecture version from the architecture's manifest.
	 * 
	 * @param context
	 *            The current activity being executed.
	 * @return The architecture version name.
	 * 
	 * @since 5.6
	 */
	public static String getArchitectureVersion(Context context) {
		return "5.6.0 Build 180";
	}

	/***
	 * Gets a meta data value from the app's manifest and returns it as a
	 * string.
	 * 
	 * @param context
	 *            The current activity being executed.
	 * @param metaName
	 *            The name of the meta data item.
	 * @return The meta data's value as a string.
	 */
	public static String getMetaStringValue(Context context, String metaName) {
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			String KeyValue = bundle.getString(metaName);
			return KeyValue;
		} catch (NameNotFoundException e) {
			LogManager.getInstance().error(e);
			return "";
		} catch (NullPointerException e) {
			LogManager.getInstance().error(e);
			return "";
		}
	}

	/***
	 * Gets a meta data value from the app's manifest and returns it as an
	 * integer.
	 * 
	 * @param context
	 *            The current activity being executed.
	 * @param metaName
	 *            The name of the meta data item.
	 * @return The meta data's value as an integer.
	 */
	public static int getMetaIntValue(Context context, String metaName) {
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			int KeyValue = bundle.getInt(metaName, -1);
			return KeyValue;
		} catch (NameNotFoundException e) {
			LogManager.getInstance().error(e);
			return -1;
		} catch (NullPointerException e) {
			LogManager.getInstance().error(e);
			return -1;
		}
	}

	/***
	 * Gets a meta data value from the app's manifest and returns it as an
	 * boolean.
	 * 
	 * @param context
	 *            The current activity being executed.
	 * @param metaName
	 *            The name of the meta data item.
	 * @return The meta data's value as an boolean.
	 */
	public static boolean getMetaBooleanValue(Context context, String metaName) {
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			return bundle.getBoolean(metaName, false);
		} catch (NameNotFoundException e) {
			LogManager.getInstance().error(e);
			return false;
		} catch (NullPointerException e) {
			LogManager.getInstance().error(e);
			return false;
		}
	}

	/**
	 * Gets the name of the directory where the application should store it's
	 * log files. This value is read from the manifest file (DirectoryName)
	 * meta-data. The default is com.metrix.metrixmobile.
	 * 
	 * @return The name of the directory.
	 * @since 5.6
	 */
	public static String getApplicationLogsDirectory() {
		String directory = MetrixApplicationAssistant.getApplicationDirectory();
		return directory;
	}

	/**
	 * Gets the name of the directory where the application should store file
	 * attachments. This value is read from the manifest file (DirectoryName)
	 * meta-data. The default is com.metrix.metrixmobile.
	 * 
	 * @return The name of the directory.
	 * @since 5.6
	 */
	public static String getApplicationAttachmentsDirectory() {
		String directory = MetrixApplicationAssistant.getApplicationDirectory();
		return directory + "/files";
	}
	
	/**
	 * Updates the OS Version on the person_mobile record 
	 */
	public static void updateOSVersion(Activity activity, String personId)
	{		
		if (!MetrixStringHelper.isNullOrEmpty(personId))
		{
			Integer seq = SettingsHelper.getDeviceSequence(activity);
			String clientVersion = MetrixApplicationAssistant.getAppVersion(activity);
			String rowId = MetrixDatabaseManager.getFieldStringValue("person_mobile", "metrix_row_id", "person_id = '" + personId + "' and sequence = " + seq);
			MetrixSqlData personData = new MetrixSqlData("person_mobile", MetrixTransactionTypes.UPDATE);
			personData.dataFields.add(new DataField("metrix_row_id", rowId));
			personData.dataFields.add(new DataField("person_id", personId));
			personData.dataFields.add(new DataField("sequence", seq));
			personData.dataFields.add(new DataField("os_version", Build.VERSION.RELEASE));
			personData.dataFields.add(new DataField("client_version", clientVersion));
			personData.filter = "person_id = '" + personId + "' and sequence = " + seq;
			ArrayList<MetrixSqlData> personTransaction = new ArrayList<MetrixSqlData>();
			personTransaction.add(personData);
			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(personTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("PersonMobile"), activity);
		}
	}

	/**
	 * Updates the OS Version on the person_mobile record
	 */
	public static void updateSSOId(String personId, String ssoId, String domain)
	{
		if (!MetrixStringHelper.isNullOrEmpty(personId))
		{
			String rowId = MetrixDatabaseManager.getFieldStringValue("person", "metrix_row_id", "person_id = '" + personId + "'");
			MetrixSqlData personData = new MetrixSqlData("person", MetrixTransactionTypes.UPDATE);
			personData.dataFields.add(new DataField("metrix_row_id", rowId));
			personData.dataFields.add(new DataField("person_id", personId));
			personData.dataFields.add(new DataField("sso_enabled", "Y"));
			personData.dataFields.add(new DataField("sso_user_id", ssoId));
			personData.dataFields.add(new DataField("sso_domain", domain));

			personData.filter = "person_id = '" + personId + "'";
			ArrayList<MetrixSqlData> personTransaction = new ArrayList<MetrixSqlData>();
			personTransaction.add(personData);
			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(personTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("Person"), null);
		}
	}

	/***
	 * Detects if a given screen_name corresponds with an activity in the Mobile client code base.
	 * Added 5.6.3
	 * @param screenName
	 * @return return true, if a given screen_name maps with an activity in the mobile client code base, else returns false.
	 */
	public static boolean screenNameHasClassInCode(String screenName) {
		if (!MetrixStringHelper.isNullOrEmpty(screenName)) {
			if (screenName.startsWith("DebriefSurvey")) {
				screenName = "DebriefSurvey";
			} else if (MetrixStringHelper.valueIsEqual(screenName, "DebriefTaskTextFeed")) {
				screenName = "DebriefTaskTextList";
			}
		}
		return screenNameHasClassInCode(screenName, new String[] {
				"com.metrix.metrixmobile", "com.metrix.metrixmobile.survey",
				"com.metrix.metrixmobile.system" });
	}

	/***
	 * Determines a safe theme ID to use from Android style resources for DatePickerDialog / TimePickerDialog.
	 * Added 5.7.0
	 * @return An android.R.style resource ID.
	 */
	public static int getSafeDialogThemeStyleID() {
		int currentBuild = Build.VERSION.SDK_INT;
		if (currentBuild < 14) {
			// our minimum API version is 11, so use a safe theme added in API 11
			// for whatever reason, Theme_Holo_Light_Dialog is unsafe on some devices,
			// so we default to this darker theme on older API versions
			return android.R.style.Theme_Holo_Dialog;
		} else {
			// use a safe theme added in API 14 that is light
			return android.R.style.Theme_DeviceDefault_Light_Dialog;
		}
	}

	/***
	 * Detects if a given screen_name corresponds with an activity in the Mobile client code base.
	 * Added 5.6.3
	 * @param screenName
	 * @param packageNames
	 * @return return true, if a given screen_name maps with an activity in the mobile client code base, else returns false.
	 */
	private static boolean screenNameHasClassInCode(String screenName, String[] packageNames) {
		boolean status = false;

		if (!MetrixStringHelper.isNullOrEmpty(screenName)) {
			for (String packageName : packageNames) {
				try {
					if (Class.forName(String.format("%s.%s", packageName, screenName)) != null) {
						status = true;
						break;
					}
				} catch (Exception ex) {
					status = false;
				}
			}
		}
		return status;
	}

	private static String getApplicationDirectory() {
		Application app = (Application) MetrixPublicCache.instance.getItem(Global.MobileApplication);
		String setting = MetrixApplicationAssistant.getMetaStringValue(app, "DirectoryName");

		if (!MetrixStringHelper.isNullOrEmpty(setting)) {
			return setting;
		} else {
			return "com.metrix.metrixmobile";
		}
	}
}