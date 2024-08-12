package com.metrix.architecture.utilities;

import com.google.gson.Gson;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.Global.UploadType;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

/**
 * @author elin
 * Save the app settings for the current application
 */
public class SettingsHelper {

    private static final String PREFERENCES_NAME = "AppSettings";
    private static final String BLUETOOTH_ADDRESS = "Zebra Bluetooth Address";
    private static final String TCP_ADDRESS = "Zebra TCP Address";
    private static final String TCP_PORT = "Zebra TCP Port";
    private static final String SYNC_INTERVAL = "Sync Interval";
    private static final String SERVICE_ADDRESS = "Service Address";
    private static final String REMEMBER_ME = "Remember Me";
    private static final String DEVICE_SEQUENCE = "Device Sequence";
    private static final String INIT_STARTED = "INIT_STARTED";    
    private static final String ACTIVATED_USER = "Activated User";    
    private static final String ENCODED_PERSON_ID = "Encoded Person ID";        
    private static final String LAST_FOLDER = "";
    
    public static final String syncPause = "Sync Pause";    
    public static final String ManualLogin = "Manual Login";
    public static final String SYNC_TYPE = UploadType.TRANSACTION_INDEPENDENT.toString();
    public static final String MAX_LOGS = "MAX_LOGS";
    public static final String LOG_LEVEL = "LOG LEVEL";
    public static final String PLAY_SOUND = "Play Sound";
    public static final String USER_LOGIN_PASSWORD = "LOGIN PASSWORD";
    public static final String ASKED_INIT_LOCATION_PERMISSION = "ASKED INIT LOCATION";
    public static final String FROM_CLOSED_APP = "From Closed App";
    public static final String FROM_LOGOUT = "FROM LOGOUT";
    public static final String PUSH_REGISTRATION_ID = "PUSH_REGISTRATION_ID";

    // Below is the Metrix Server information
    public static final String SERVER_DATE_TIME_FORMAT="SERVER_DATE_TIME_FORMAT";
    public static final String SERVER_DATE_FORMAT = "SERVER_DATE_FORMAT";
    public static final String SERVER_TIME_FORMAT = "SERVER_TIME_FORMAT";
    public static final String SERVER_LOCALE_CODE = "SERVER_LOCALE_CODE";
    public static final String SERVER_NUMERIC_FORMAT = "SERVER_NUMERIC_FORMAT";
    public static final String SERVER_TIME_ZONE_ID = "SERVER_TIME_ZONE_ID";
    public static final String SERVER_TIME_ZONE_OFFSET = "SERVER_TIME_ZONE_OFFSET";
    public static final String TOKEN_ID = "TOKEN_ID";
    public static final String SESSION_ID = "SESSION_ID";
    public static final String SERVER_AUTHENTICATE_ERROR_MESSAGE = "SERVER_AUTHENTICATE_ERROR_MESSAGE";
    
    public static final String INIT_FINISHED = "INIT_FINISHED";
    public static final String PCHANGE = "PCHANGE";
    public static final String PCHANGE_SIMPLEHASH = "PCHANGE_SIMPLEHASH";
    public static final String PCHANGE_JUST_PROCESSED = "PCHANGE_JUST_PROCESSED";

    // Global variables
    public static final String SystemDatabaseId = "SystemDatabaseId";
    public static final String BusinessDatabaseId = "BusinessDatabaseId";
    public static final String DatabaseVersion = "DatabaseVersion";

	// randomly generated keys meant to obfuscate purpose of these fields
    private static final String WOS1L1ALC4A7IO8XBYL1 = "cojNDm25Oy0a3TiJpYxj";
    private static final String PWYXZDGESLCKYUM3HLZ0 = "Im8ICgTFcNQ8W71lUWir";
    private static final String HLRQNCM9QBQK236SI179 = "eYdhAhE9QDf3vY3e9eKX";
    private static final String DKIPHIHGSZ7TA2EFNPV0 = "pXNC2VrRN5mHHCo83ky7";

    //Login screen iamge, skin info
    public static final String LOGIN_IMAGE_ID_PORTRAIT = "LOGIN_IMAGE_ID_PORTRAIT";
    public static final String LOGIN_IMAGE_ID_LANDSCAPE = "LOGIN_IMAGE_ID_LANDSCAPE";
    public static final String TOP_FIRST_GRADIENT_COLOR = "TOP_FIRST_GRADIENT_COLOR";
    public static final String BOTTOM_FIRST_GRADIENT_COLOR = "BOTTOM_FIRST_GRADIENT_COLOR";
    public static final String FIRST_GRADIENT_TEXT_COLOR = "FIRST_GRADIENT_TEXT_COLOR";

    // Azure Active Directory info
    public static final String SSO_ID = "SSO_ID";
    public static final String SSO_DOMAIN = "SSO_DOMAIN";
    public static final String OIDC_CLIENT_ID = "OIDC_CLIENT_ID";
    public static final String OIDC_REDIRECT_URI = "OIDC_REDIRECT_URI";
    public static final String OIDC_APPLICATION_ID = "OIDC_APPLICATION_ID";
    public static final String OIDC_AUTHORIZATION_URI = "OIDC_AUTHORIZATION_URI";
    public static final String OIDC_AUTHORIZATION_ENDPOINT_URI = "OIDC_AUTHORIZATION_ENDPOINT_URI";
    public static final String OIDC_TOKEN_ENDPOINT_URI = "OIDC_TOKEN_ENDPOINT_URI";
    public static final String OIDC_USER_INFO_ENDPOINT_URI = "OIDC_USER_INFO_ENDPOINT_URI";
    public static final String OIDC_ENDSESSION_ENDPOINT_URI = "OIDC_ENDSESSION_ENDPOINT_URI";
    public static final String OIDC_ACCESS_TOKEN = "OIDC_ACCESS_TOKEN";
    public static final String OIDC_REFRESH_TOKEN = "OIDC_REFRESH_TOKEN";
    public static final String OIDC_ID_TOKEN = "OIDC_ID_TOKEN";
    public static final String OIDC_SCOPE = "OIDC_SCOPE";
    public static final String OIDC_ENABLED = "OIDC_ENABLED";
    public static final String OIDC_FRONTCHANNEL_LOGOUT_SUPPORTED = "OIDC_FRONTCHANNEL_LOGOUT_SUPPORTED";
    public static final String AUTHENTICATION_METHODS = "AUTHENTICATION_METHODS";

    public static final String AZURE_CLIENT_ID = "AZURE_CLIENT_ID";
    public static final String AZURE_TENANT_NAME = "AZURE_TENANT_NAME";
    public static final String AZURE_END_POINT = "AZURE_END_POINT";
    public static final String AZURE_AUTH_CODE = "AZURE_AUTH_CODE";
    public static final String AZURE_REDIRECT_URL = "AZURE_REDIRECT_URL";
    public static final String AZURE_ACCESS_TOKEN = "AZURE_ACCESS_TOKEN";
    public static final String AZURE_AD_SUCCESS = "AZURE_AD_SUCCESS";
    public static final String IS_AZURE = "IS_AZURE";
    public static final String AZURE_ATTACHMENT_STORAGE_BASE = "AZURE_ATTACHMENT_STORAGE_BASE";

    public static String getIp(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getString(TCP_ADDRESS, "");
    }

    public static String getPort(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getString(TCP_PORT, "");
    }

    public static String getBluetoothAddress(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getString(BLUETOOTH_ADDRESS, "");
    }
    
    public static int getSyncInterval(Context context) {
        int defaultSyncSeconds = 60;
        try {
            // Figure out what our default interval should be (using DEFAULT_SYNC_INTERVAL app param)
            // If it has a value, and it is one of the user-selectable intervals, use that
            // Use 60 only if this logic falls through
            String defaultSyncInterval = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='DEFAULT_SYNC_INTERVAL'");
            if (!MetrixStringHelper.isNullOrEmpty(defaultSyncInterval) && MetrixStringHelper.isInteger(defaultSyncInterval)) {
                int testValue = Integer.parseInt(defaultSyncInterval);
                if (testValue == 5 || testValue == 15 || testValue == 30 || testValue == 60 || testValue == 300 || testValue == 3600)
                    defaultSyncSeconds = testValue;
            }

            // If ALLOW_ADMIN_EDITS_IN_MOBILE is Y, then grab from User Settings, using the above default as a backstop
            SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
            int currentSettingsValue = settings.getInt(SYNC_INTERVAL, defaultSyncSeconds);
            String adminEditsAllowed = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ALLOW_ADMIN_EDITS_IN_MOBILE'");
            if (!MetrixStringHelper.isNullOrEmpty(adminEditsAllowed) && adminEditsAllowed.compareToIgnoreCase("Y") == 0)
                return currentSettingsValue;
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            LogManager.getInstance().error(String.format("Due to the above exception in determining sync interval, using %s seconds as the sync interval.", String.valueOf(defaultSyncSeconds)));
        }

        // At this point, we either have 60 or a valid DEFAULT_SYNC_INTERVAL app param value to use
        // and [we are ignoring User Settings intentionally OR an exception prematurely stopped us].
        return defaultSyncSeconds;
    }

    public static String getServiceAddress(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVICE_ADDRESS, "");
    }
    
    public static boolean getSyncPause(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getBoolean(syncPause, false);
    }       
    
    public static boolean getManualLogin(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getBoolean(ManualLogin, true);
    }     
    
    public static boolean getPlaySound(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);        
        return settings.getBoolean(PLAY_SOUND, true);
    }         
    
    public static String getRememberMe(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(REMEMBER_ME, "");
    }
    
    public static Integer getDeviceSequence(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getInt(DEVICE_SEQUENCE, 0);
    }
    
    public static String getSyncType(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString("SYNC_TYPE", UploadType.TRANSACTION_INDEPENDENT.toString());
    }  
    
    public static boolean getInitStatus(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getBoolean(INIT_STARTED, false);
    }      
    
    public static String getActivatedUser(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(ACTIVATED_USER, "");
    }
    
    /**
     * This method will returned the person's encoded (Base64) person id.
     * @param context The application context.
     * @return The encoded person id.
     * @since 5.6
     */
    public static String getEncodedPersonId(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(ENCODED_PERSON_ID, "");
    }    
    
    public static String getServerDateTimeFormat(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_DATE_TIME_FORMAT, "");
    }  
    
    public static String getServerDateFormat(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_DATE_FORMAT, "");
    }  
    
    public static String getServerTimeFormat(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_TIME_FORMAT, "");
    }  
    
    public static String getServerNumericFormat(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_NUMERIC_FORMAT, "");
    }  
    
    public static String getServerLocaleCode(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_LOCALE_CODE, "");
    }  
    
    public static String getServerTimeZoneId(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_TIME_ZONE_ID, "");
    }  
    
    public static String getServerTimeZoneOffset(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(SERVER_TIME_ZONE_OFFSET, "");
    }

    public static String getTokenId(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getString(TOKEN_ID, "");
    }

    public static String getSessionId(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        return settings.getString(SESSION_ID, "");
    }

    public static String getLogLevel(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(LOG_LEVEL, "");
    }
    
    public static int getMaxLogs(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getInt(MAX_LOGS, 10);
    }
    
    public static String getLastFolder(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);

    	String setting = settings.getString(LAST_FOLDER, "");
    	if (MetrixStringHelper.isNullOrEmpty(setting)) {
            setting = context.getExternalFilesDir(null).getPath();
            SettingsHelper.setLastFolder(context, setting);
    	}
    	return setting;
    }
    
    public static String getStringSetting(Context context, String key) {
    	if (key.compareToIgnoreCase(SettingsHelper.USER_LOGIN_PASSWORD) == 0) {
    		SharedPreferences settings = context.getSharedPreferences(SettingsHelper.USER_LOGIN_PASSWORD, 0);
    		if (settings.contains(SettingsHelper.USER_LOGIN_PASSWORD)) {
    			SharedPreferences.Editor editor = settings.edit();
    			editor.remove(SettingsHelper.USER_LOGIN_PASSWORD);
    			editor.commit();
    			SettingsHelper.saveStringSetting(context, SettingsHelper.HLRQNCM9QBQK236SI179, "XBWO22f+fD+N1W5O60zrYkjL3OkYFjOFssYQik4uCtiL/PwGmy8IO3G27VKqK4oEEGdPdtS5DhkM7R/cbGmBvQ==", false);
    			SettingsHelper.saveStringSetting(context, SettingsHelper.PWYXZDGESLCKYUM3HLZ0, "Im8ICgTFcNQ8W71lUWir64wFU1qd8ZNlzLchOxGQik4uCtUhnPYXqXtNNhkM7R/cbGmB5iMLdDtPBDSV64wVxQ==", false);
    			SettingsHelper.saveStringSetting(context, SettingsHelper.DKIPHIHGSZ7TA2EFNPV0, "ANqvY2V6zPwA7NIPBDSV64wFU1qd8ZNlzLchOxGyTXaYYGUhnPYXqXtNNWmPiZzSZCjo3iMLdDtG0wHkES/WeQ==", false);
    		}
    		key = SettingsHelper.WOS1L1ALC4A7IO8XBYL1; 
    	}
    	
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getString(key, "");
    }
    
    public static boolean getBooleanSetting(Context context, String key) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getBoolean(key, false);
    }    
    
    /**
     * This method will return, as an integer value, the value of the
     * setting identified by the received key name.
     * @param context The application context.
     * @param key The key of the setting to get.
     * @return The setting value.
     * @since 5.6
     */
    public static int getIntegerSetting(Context context, String key) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	return settings.getInt(key, 0);
    }

    public static void saveSyncType(Context context, String syncType) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("SYNC_TYPE", syncType);
        editor.commit();
    }    
    
    public static void saveIp(Context context, String ip) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TCP_ADDRESS, ip);
        editor.commit();
    }

    public static void savePort(Context context, String port) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TCP_PORT, port);
        editor.commit();
    }
    
    public static void saveInitStatus(Context context, Boolean initStarted) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(INIT_STARTED, initStarted);
        editor.commit();
    }    

    public static void setActivatedUser(Context context, String personId) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACTIVATED_USER, personId);
        editor.commit();
    }
    
    /**
     * This method sets the encoded person id in the shared preferences to the received value.
     * @param context The application context.
     * @param encodedPersonId The encoded person id.
     * @since 5.6
     */
    public static void setEncodedPersonId(Context context, String encodedPersonId) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ENCODED_PERSON_ID, encodedPersonId);
        editor.commit();
    }    
    
    public static void setLastFolder(Context context, String folder) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(LAST_FOLDER, folder);
        editor.commit();
    }
    
    /**
     * Save bluetooth address to the app preferences
     * @param context
     * @param address
     */
    public static void saveBluetoothAddress(Context context, String address) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BLUETOOTH_ADDRESS, address);
        editor.commit();
    }
    
    /**
     * Save Sync Interval to the app preferences
     * @param context
     * @param interval
     */
    public static void saveSyncInterval(Context context, int interval) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(SYNC_INTERVAL, interval);
        editor.commit();
    }    
    
    public static void saveServiceAddress(Context context, String address) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVICE_ADDRESS, address); 
    	editor.commit();
    }
    /**
     * Save Sync pause or not to the app preferences
     * @param context
     * @param pause - true/false
     */
    public static void saveSyncPause(Context context, boolean pause) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(syncPause, pause);
        editor.commit();
    }            
    
    /**
     * Save manual login to the app preferences
     * @param context
     * @param value login - true/false
     */
    public static void saveManualLogin(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(ManualLogin, value);
        editor.commit();
    }
    
    /**
     * Save play sound to the app preferences
     * @param context
     * @param value sound - true/false
     */
    public static void savePlaySound(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PLAY_SOUND, value);
        editor.commit();
    }    
    
    public static void saveRememberMe(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(REMEMBER_ME, value);
    	editor.commit();
    }
    
    public static void saveDeviceSequence(Context context, Integer value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(DEVICE_SEQUENCE, value);
    	editor.commit();
    }
    
    public static void saveLastFolder(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(LAST_FOLDER, value);
    	editor.commit();
    }
    
    public static void saveLogLevel(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(LOG_LEVEL, value);
    	editor.commit();
    }
    
    public static void saveMaxLogs(Context context, int value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(MAX_LOGS, value);
    	editor.commit();
    }
    
    public static void saveServerDateTimeFormat(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_DATE_TIME_FORMAT, value);
    	editor.commit();
    }   
    
    public static void saveServerDateFormat(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_DATE_FORMAT, value);
    	editor.commit();
    }   
    
    public static void saveServerTimeFormat(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_TIME_FORMAT, value);
    	editor.commit();
    }   
    
    public static void saveServerNumericFormat(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_NUMERIC_FORMAT, value);
    	editor.commit();
    }   
    
    public static void saveServerLocaleCode(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_LOCALE_CODE, value);
    	editor.commit();
    }   
    
    public static void saveServerTimeZoneId(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_TIME_ZONE_ID, value);
    	editor.commit();
    }   
    
    public static void saveServerTimeZoneOffset(Context context, String value) {
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(SERVER_TIME_ZONE_OFFSET, value);
    	editor.commit();
    }

    public static void saveTokenId(Context context, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TOKEN_ID, value);
        editor.commit();
    }

    public static void saveSessionId(Context context, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SESSION_ID, value);
        editor.commit();
    }

    public static void saveStringSetting(Context context, String key, String value, boolean saveEmpty) {
    	if(saveEmpty == false && MetrixStringHelper.isNullOrEmpty(value))
    		return;

    	if (key.compareToIgnoreCase(SettingsHelper.USER_LOGIN_PASSWORD) == 0) {
    		SharedPreferences settings = context.getSharedPreferences(SettingsHelper.USER_LOGIN_PASSWORD, 0);
    		if (settings.contains(SettingsHelper.USER_LOGIN_PASSWORD)) {
    			SharedPreferences.Editor editor = settings.edit();
    			editor.remove(SettingsHelper.USER_LOGIN_PASSWORD);
    			editor.commit();
    			SettingsHelper.saveStringSetting(context, SettingsHelper.HLRQNCM9QBQK236SI179, "XBWO22f+fD+N1W5O60zrYkjL3OkYFjOFssYQik4uCtiL/PwGmy8IO3G27VKqK4oEEGdPdtS5DhkM7R/cbGmBvQ==", false);
    			SettingsHelper.saveStringSetting(context, SettingsHelper.WOS1L1ALC4A7IO8XBYL1, settings.getString(SettingsHelper.USER_LOGIN_PASSWORD, ""), false);
    			SettingsHelper.saveStringSetting(context, SettingsHelper.DKIPHIHGSZ7TA2EFNPV0, "ANqvY2V6zPwA7NIPBDSV64wFU1qd8ZNlzLchOxGyTXaYYGUhnPYXqXtNNWmPiZzSZCjo3iMLdDtG0wHkES/WeQ==", false);
    		}
    		key = SettingsHelper.WOS1L1ALC4A7IO8XBYL1;
    	}

    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(key, value);
    	editor.commit();
    }
    
    public static void saveBooleanSetting(Context context, String key, boolean value) {   	
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putBoolean(key, value);
    	editor.commit();
    }    
    
    /**
     * This method will save the received integer value to the setting identified
     * by the received key name.
     * @param context The application context.
     * @param key The key name.
     * @param value The value to save.
     * @since 5.6
     */
    public static void saveIntegerSetting(Context context, String key, int value) {   	
    	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(key, value);
    	editor.commit();
    }

    /***
     * Clear login screen skin information
     */
    public static void clearLoginScreenSkinInfo() {
        try {
            Context context = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);

            SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            if (settings.contains(SettingsHelper.LOGIN_IMAGE_ID_PORTRAIT))
                editor.remove(SettingsHelper.LOGIN_IMAGE_ID_PORTRAIT);
            if (settings.contains(SettingsHelper.LOGIN_IMAGE_ID_LANDSCAPE))
                editor.remove(SettingsHelper.LOGIN_IMAGE_ID_LANDSCAPE);
            if (settings.contains(SettingsHelper.TOP_FIRST_GRADIENT_COLOR))
                editor.remove(SettingsHelper.TOP_FIRST_GRADIENT_COLOR);
            if (settings.contains(SettingsHelper.BOTTOM_FIRST_GRADIENT_COLOR))
                editor.remove(SettingsHelper.BOTTOM_FIRST_GRADIENT_COLOR);
            if (settings.contains(SettingsHelper.FIRST_GRADIENT_TEXT_COLOR))
                editor.remove(SettingsHelper.FIRST_GRADIENT_TEXT_COLOR);
            editor.commit();
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    /***
     * Store login image id and necessary skin information
     */
    public static void storeLoginImageInformation() {
        try {
            Context context = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);

            String	curSkinLoginImageIdPortrait = MetrixSkinManager.getLoginImageImageIDPortrait();
            SettingsHelper.saveStringSetting(context, SettingsHelper.LOGIN_IMAGE_ID_PORTRAIT, curSkinLoginImageIdPortrait, true);

            String	curSkinLoginImageIdLandscape = MetrixSkinManager.getLoginImageImageIDLandscape();
            SettingsHelper.saveStringSetting(context, SettingsHelper.LOGIN_IMAGE_ID_LANDSCAPE, curSkinLoginImageIdLandscape, true);

            String tfgColor = MetrixSkinManager.getTopFirstGradientColor();
            SettingsHelper.saveStringSetting(context, SettingsHelper.TOP_FIRST_GRADIENT_COLOR, tfgColor, true);

            String bfgColor = MetrixSkinManager.getBottomFirstGradientColor();
            SettingsHelper.saveStringSetting(context, SettingsHelper.BOTTOM_FIRST_GRADIENT_COLOR, bfgColor, true);

            String fgtColor = MetrixSkinManager.getFirstGradientTextColor();
            SettingsHelper.saveStringSetting(context, SettingsHelper.FIRST_GRADIENT_TEXT_COLOR, fgtColor, true);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void removeSetting(String settingName) {
        try {
            Context context = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);

            SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            if (settings.contains(settingName))
                editor.remove(settingName);
            editor.commit();
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }
}
