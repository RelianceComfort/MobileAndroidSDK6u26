package com.metrix.metrixmobile.global;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.UUID;

import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import com.google.gson.JsonObject;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.RemoteMessagesHandler.HandlerException;
import com.metrix.architecture.utilities.Global.UploadType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixSecurityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.LocationBroadcastReceiver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Xml;

public class MetrixAuthenticationAssistant {
	@SuppressLint("MissingPermission")
	public static LoginResult activateDevice(Activity activity, String serviceUrl, String personId, String password, boolean hashed) throws HandlerException {
		try {
			MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(activity, 5);

			if (ping(remoteExecutor, serviceUrl) == false)
				return LoginResult.REQUIRES_ACTIVATION;

			remoteExecutor = new MetrixRemoteExecutor(activity);	// back to standard timeout
			Global.encodeUrl = MetrixAuthenticationAssistant.encodeUrlParam(serviceUrl, remoteExecutor);

			String servicePersonId = Global.encodeUrl ? MetrixStringHelper.encodeBase64ToUrlSafe(personId.toUpperCase()) : personId.toUpperCase();
			String hashPassword = password;
			if (hashed == false)
				hashPassword =	MetrixSecurityHelper.HashPassword(password);

			TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);

			// This try/catch is needs to be nested in this other try/catch because if an exception
			// is thrown here that is specific to the TelephonyManager, we want to continue with the
			// code since we have a way of handling the deviceId being null.
			String deviceId = null;
			try {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
					deviceId = telephonyManager.getDeviceId();
				} else {
					deviceId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
				}
			} catch (SecurityException ex) {
				LogManager.getInstance().error(ex);
			}

			deviceId = (deviceId != null) ? deviceId : "null";
			String carrier = telephonyManager.getNetworkOperatorName();
			carrier = (!MetrixStringHelper.isNullOrEmpty(carrier)) ? carrier : "(none)";

			String brand = Build.MANUFACTURER;
			String model = Build.MODEL;
			String osType = "Android";
			String osVersion = Build.VERSION.RELEASE;
			String clientVersion = MetrixApplicationAssistant.getAppVersion(activity);

			JsonObject properties = new JsonObject();
			properties.addProperty("person_id", servicePersonId);
			properties.addProperty("password", hashPassword);
			properties.addProperty("device_id", deviceId);
			properties.addProperty("carrier", carrier);
			properties.addProperty("brand", brand);
			properties.addProperty("model", model);
			properties.addProperty("os_type", osType);
			properties.addProperty("os_version", osVersion);
			properties.addProperty("client_version", clientVersion);

			String postBody = MetrixSyncManager.preparePostBodyForTransmission(properties);
			postBody = postBody.replace("&", "");

			String fullUrl = serviceUrl + "/Activation/";
			String response = remoteExecutor.executePost(fullUrl, null, postBody);
			String sequence = response.substring(response.indexOf(">") + 1, response.indexOf("</"));

			if (sequence.contains("-3")) {
				return LoginResult.LICENSING_VIOLATION;
			} else if (sequence.contains("-")) {
				return LoginResult.INVALID_PERSON_OR_PASSWORD;
			}

			SettingsHelper.saveServiceAddress(activity, serviceUrl);
			MetrixPublicCache.instance.addItem("MetrixServiceAddress", serviceUrl);
			SettingsHelper.saveDeviceSequence(activity, Integer.valueOf(sequence));

			if (MetrixSyncManager.reacquireValidToken(personId, hashPassword)) {
				SettingsHelper.setActivatedUser(activity, personId);
				SettingsHelper.setEncodedPersonId(activity, servicePersonId);

				JsonObject initProperties = MetrixSyncManager.generatePostBodyJSONWithAuthentication();
				String loginMessage = MetrixSyncManager.getPerformLogin(personId, hashPassword);
				initProperties.addProperty("login_message", loginMessage);
				String initPostBody = MetrixSyncManager.preparePostBodyForTransmission(initProperties);

				fullUrl = serviceUrl + "/Initialization/";
				response = remoteExecutor.executePost(fullUrl, null, initPostBody);

				MetrixDatabaseManager.deleteDatabase(activity);
				return performLogin(activity, personId, password, false, false);
			}
			else {
				String errorMessage = SettingsHelper.getStringSetting(MobileApplication.getAppContext(), "SERVER_AUTHENTICATE_ERROR_MESSAGE");
				return setLoginResultFromErrorMessage(errorMessage);
			}
		} catch (Exception ex) {
			return LoginResult.REQUIRES_ACTIVATION;
		}
	}

	public static LoginResult setLoginResultFromErrorMessage(String errorMessage){

		String[] result = errorMessage.trim().split(":");
		String errorCode = result[0];

		if (MetrixStringHelper.valueIsEqual(errorCode,"A117"))
			return LoginResult.PASSWORD_EXPIRED;
		else if (MetrixStringHelper.valueIsEqual(errorCode,"A116"))
			return LoginResult.INVALID_PERSON_OR_PASSWORD;
		else
			return LoginResult.REQUIRES_ACTIVATION;
	}

	public static LoginResult performLogin(Activity activity, String personId, String password, boolean checkPassword, boolean startLocation) {
		return performLogin(activity, personId, password, checkPassword, false, null, startLocation);
	}

	public static LoginResult performLogin(Activity activity, String personId, String password, boolean checkPassword, boolean processPendingPasswordChg, String oldPassword, boolean startLocation) {
		// if processPendingPasswordChg, database should still be open at this point
		// get password from user_credentials
		String oldHashPassword = "";
		if (processPendingPasswordChg) {
			if (MetrixStringHelper.isNullOrEmpty(password) || MetrixStringHelper.isNullOrEmpty(oldPassword))
				return LoginResult.INVALID_PERSON_OR_PASSWORD;

			MobileApplication.stopSync(activity);

			if (startLocation) // It is used by login screen now
				activity.runOnUiThread(new Runnable() { public void run() { LocationBroadcastReceiver.pop(); }});

			if (oldPassword.endsWith("=="))
				oldHashPassword = oldPassword;
			else
				oldHashPassword = MetrixSecurityHelper.HashPassword(oldPassword);
		}

		try {
			// Make sure database is closed first
			MetrixDatabaseManager.closeDatabase();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		String activatedUser = SettingsHelper.getActivatedUser(activity);
		if (MetrixStringHelper.isNullOrEmpty(activatedUser) || activatedUser.compareTo(personId) != 0)
			return LoginResult.REQUIRES_ACTIVATION;

		String hashPassword = "";
		if (password.endsWith("=="))
			hashPassword = password;
		else
			hashPassword = MetrixSecurityHelper.HashPassword(password);

		if (processPendingPasswordChg) {
			// first, validate old hashed password

			MetrixDatabaseManager.createDatabaseAdapters(activity.getApplicationContext(), MobileGlobal.getDatabaseVersion(activity.getApplicationContext()), com.metrix.metrixmobile.R.array.system_tables,
					com.metrix.metrixmobile.R.array.business_tables);

			String oldUCHashedPassword = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", String.format("person_id = '%s'", personId));

			if (!MetrixStringHelper.valueIsEqual(oldHashPassword, oldUCHashedPassword))
				return LoginResult.INVALID_PERSON_OR_PASSWORD;


			// submit perform_login, to make sure that entered password is good
			try {
				if(!MetrixSyncManager.reacquireValidToken(personId, hashPassword))
					return LoginResult.INVALID_PERSON_OR_PASSWORD;
			} catch (Exception e) {
				LogManager.getInstance().error(e);
				return LoginResult.INVALID_PERSON_OR_PASSWORD;
			}

			// if perform_login succeeds (i.e., if we get here)...		
			// if dbRequiresKey, Rekey with hashPassword, close, and reopen

			// update user_credentials with hashPassword and set SETTING_PASSWORD_UPDATED = N
			MetrixDatabaseManager.executeSql(String.format("update user_credentials set password = '%1$s' where person_id = '%2$s'", hashPassword, personId));
			SettingsHelper.saveStringSetting(activity, "SETTING_PASSWORD_UPDATED", "N", false);

			MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());

			MetrixPublicCache.instance.addItem("person_id", personId);
			// and we're done ... we don't do standard local password validation, as the perform_login covers that
		} else {
			// do standard DB loading and local password validation
			MetrixDatabaseManager.createDatabaseAdapters(activity.getApplicationContext(), MobileGlobal.getDatabaseVersion(activity.getApplicationContext()), com.metrix.metrixmobile.R.array.system_tables,
					com.metrix.metrixmobile.R.array.business_tables);

			if (checkPassword) {
				String hashActualPass = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", "person_id=" + DatabaseUtils.sqlEscapeString(personId));
				if (hashActualPass == null || hashActualPass.compareTo(hashPassword) != 0) {
					MetrixDatabaseManager.closeDatabase();
					return LoginResult.INVALID_PERSON_OR_PASSWORD;
				}
			}

			if (passwordIsExpired(activity, personId)) {
				MetrixDatabaseManager.closeDatabase();
				return LoginResult.PASSWORD_EXPIRED;
			}
		}

		// if we get here, we have successfully opened the DB and the password is good		
		SettingsHelper.saveIntegerSetting(activity, SettingsHelper.SystemDatabaseId, com.metrix.metrixmobile.R.array.system_tables);
		SettingsHelper.saveIntegerSetting(activity, SettingsHelper.BusinessDatabaseId, com.metrix.metrixmobile.R.array.business_tables);
		SettingsHelper.saveIntegerSetting(activity, SettingsHelper.DatabaseVersion, MobileGlobal.getDatabaseVersion(activity));
		SettingsHelper.saveStringSetting(activity, SettingsHelper.USER_LOGIN_PASSWORD, hashPassword, true);
		MobileApplication.DatabaseLoaded = true;

		databaseLoaded(activity);
		User.setUser(personId, activity);
		if (startLocation) // It is used by login screen now
			activity.runOnUiThread(new Runnable() {
				public void run() {
					LocationBroadcastReceiver.setLocationListenerStatus();
				}
			});

		SettingsHelper.saveBooleanSetting(activity, SettingsHelper.INIT_FINISHED, true);
		MobileApplication.ApplicationNullIfCrashed = "NOT NULL";
		return LoginResult.SUCCESS;
	}

	public static void setDatabaseLoaded(Context context){
		MobileApplication.DatabaseLoaded = true;
		databaseLoaded(context);
	}

	public static void handleLoginSuccess(String personId) {
		LogManager.getInstance().info("Logged in the Application.");
		SettingsHelper.saveRememberMe(MobileApplication.getAppContext(), personId);

		configureAppParams();
		User.setUser(personId, MobileApplication.getAppContext());

		MobileApplication.ApplicationNullIfCrashed = "NOT NULL";
		if(!SettingsHelper.getSyncPause(MobileApplication.getAppContext()))
			MobileApplication.startSync(MobileApplication.getAppContext());
	}

	private static void databaseLoaded(Context context) {
		String sync_type = MobileApplication.getAppParam("SYNC_TYPE_MOBILE_ANDROID");
		if (MetrixStringHelper.isNullOrEmpty(sync_type)) {
			SettingsHelper.saveSyncType(context, UploadType.TRANSACTION_INDEPENDENT.toString());
		} else {
			SettingsHelper.saveSyncType(context, sync_type);
		}
		MetrixPublicCache.instance.addItem("SYNC_TYPE", SettingsHelper.getSyncType(context));
		MobileApplication.saveTableDefinitionToCache();
	}

	/**
	 * Identifies whether the user's password is expired.
	 *
	 * @param strPerson
	 *            the user whose password to check.
	 * @return TRUE if the password is expired, FALSE otherwise.
	 */
	private static boolean passwordIsExpired(Activity activity, String strPerson) {
		String expireDate = MetrixDatabaseManager.getFieldStringValue("person", "password_expire_dt", "person_id='" + strPerson + "'");
		if (!MetrixStringHelper.isNullOrEmpty(expireDate)) {
			try {
				Calendar expireDt = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, expireDate, ISO8601.Yes);
				Calendar today = Calendar.getInstance();
				if (today.after(expireDt)) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
			}
		}
		return false;
	}

	public static LoginResult doLoginAndChangePassword(Activity activity, String personId, String oldPassword, String newPassword) throws Exception {
		try {
			// Make sure database is closed first
			MetrixDatabaseManager.closeDatabase();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		Context context = (Context) MetrixPublicCache.instance.getItem(Global.MobileApplication);

		String activatedUser = SettingsHelper.getActivatedUser(context);
		if (MetrixStringHelper.isNullOrEmpty(activatedUser) || activatedUser.compareTo(personId) != 0)
			return LoginResult.REQUIRES_ACTIVATION;

		MetrixDatabaseManager.createDatabaseAdapters(activity.getApplicationContext(), MobileGlobal.getDatabaseVersion(context), com.metrix.metrixmobile.R.array.system_tables,
				com.metrix.metrixmobile.R.array.business_tables);

		String hashActualPass = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", "person_id=" + DatabaseUtils.sqlEscapeString(personId));
		String hashOldPass = MetrixSecurityHelper.HashPassword(oldPassword);
		if (hashActualPass == null || hashActualPass.compareTo(hashOldPass) != 0) {
			MetrixDatabaseManager.closeDatabase();
			return LoginResult.INVALID_PERSON_OR_PASSWORD;
		}

		String hashNewPass = MetrixSecurityHelper.HashPassword(newPassword);
		if (!submitPasswordChange(activity, personId, hashOldPass, hashNewPass)) {
			MetrixDatabaseManager.closeDatabase();
			throw new Exception(AndroidResourceHelper.getMessage("FailedToSubmitPasswordChange"));
		}

		try {
			MetrixDatabaseManager.executeSql(String.format("update user_credentials set password = '%1$s' where person_id = '%2$s'", hashNewPass, personId));
			User.setUser(personId, context);

			MetrixPublicCache.instance.addItem("person_id", personId);

			SettingsHelper.saveIntegerSetting(activity, SettingsHelper.SystemDatabaseId, com.metrix.metrixmobile.R.array.system_tables);
			SettingsHelper.saveIntegerSetting(activity, SettingsHelper.BusinessDatabaseId, com.metrix.metrixmobile.R.array.business_tables);
			SettingsHelper.saveIntegerSetting(activity, SettingsHelper.DatabaseVersion, MobileGlobal.getDatabaseVersion(activity));
			SettingsHelper.saveStringSetting(activity, SettingsHelper.USER_LOGIN_PASSWORD, hashNewPass, true);
		} catch(Exception ex) {
			LogManager.getInstance().error("Change Password error during local saving - '%1$s'", ex.getMessage());
			return LoginResult.REQUIRES_ACTIVATION;
		}

		databaseLoaded(context);

		MobileApplication.startSync(context);
		activity.runOnUiThread(new Runnable() { public void run() { LocationBroadcastReceiver.setLocationListenerStatus(); }});
		SettingsHelper.saveBooleanSetting(activity, SettingsHelper.INIT_FINISHED, true);

		MobileApplication.ApplicationNullIfCrashed = "NOT NULL";
		return LoginResult.SUCCESS;
	}

	private static boolean submitPasswordChange(Activity activity, String strPersonId, String strOldPassword, String strNewPassword) throws Exception {
		try {
			// both passwords are already locally hashed at this point	
			// ping the web service first ... only proceed if successful	
			String serviceUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));
			MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(activity, 5);
			if (ping(remoteExecutor, serviceUrl)) {
				// post an update_person message straight to the web service
				remoteExecutor = new MetrixRemoteExecutor(activity);	// back to standard timeout
				Global.encodeUrl = MetrixAuthenticationAssistant.encodeUrlParam(serviceUrl, remoteExecutor);

				StringBuilder updatePersonMsg = new StringBuilder();
				updatePersonMsg.append(String.format("<update_person><person><person_id>%s</person_id>", strPersonId));
				updatePersonMsg.append(String.format("<password>%s</password><update /></person>", strNewPassword));
				updatePersonMsg.append(String.format("<authentication><logon_info><person_id>%s</person_id>", strPersonId));
				updatePersonMsg.append(String.format("<password>%s</password><ignore_password_expiry_check>true</ignore_password_expiry_check></logon_info></authentication></update_person>", strOldPassword));

				String fullUrl = serviceUrl + "/DirectPost/";
				String response = remoteExecutor.executePost(fullUrl, null, updatePersonMsg.toString());

				String xmlResponse = response.substring(response.indexOf(">") + 1, response.indexOf("</"));
				xmlResponse = xmlResponse.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;","&");

				// make sure response is not in error ... only proceed if it's not
				if (xmlResponse.contains("<error>")) {
					String errorMsg = xmlResponse.substring(xmlResponse.indexOf("<message>") + 9, xmlResponse.indexOf("</message>"));
					throw new Exception(errorMsg);
				}

				// manually process the update_person_result and update the corresponding person record with result data
				try {
					ArrayList<MetrixSqlData> personDataList = new ArrayList<MetrixSqlData>();
					MetrixSqlData personData = new MetrixSqlData("person", MetrixTransactionTypes.UPDATE);
					String colName = "";
					String colValue = "";

					InputStream responseStream = new ByteArrayInputStream(xmlResponse.getBytes("UTF-8"));
					XmlPullParser parser = Xml.newPullParser();
					parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
					parser.setInput(responseStream, null);
					parser.nextTag();
					parser.nextTag();
					while (parser.next() != XmlPullParser.END_DOCUMENT) {
						if (parser.getEventType() == XmlPullParser.START_TAG) {
							colName = parser.getName();
						} else if (parser.getEventType() == XmlPullParser.TEXT) {
							colValue = parser.getText();
						} else if (parser.getEventType() == XmlPullParser.END_TAG
								&& !MetrixStringHelper.valueIsEqual(parser.getName(), "person")
								&& !MetrixStringHelper.valueIsEqual(parser.getName(), "update_person_result")) {
							personData.dataFields.add(new DataField(colName, colValue));

							if (MetrixStringHelper.valueIsEqual(colName, "person_id")) {
								personData.filter = String.format("person_id = '%s'", colValue);
							}
						}
					}

					personDataList.add(personData);
					MetrixDatabaseManager.executeDataFieldArray(personDataList, false);
				} catch (Exception e) {
					// if there was a problem with processing the result, we should still allow overall success
					// because the password HAS changed!
					LogManager.getInstance().error(e);
				}

				return true;
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			throw e;
		}

		return false;
	}

	public enum LoginResult {
		SUCCESS, INVALID_PERSON_OR_PASSWORD, PASSWORD_EXPIRED, REQUIRES_ACTIVATION, LICENSING_VIOLATION
	}

	private static boolean encodeUrlParam(String serviceBaseUrl, MetrixRemoteExecutor remoteExecutor) {
		String serviceUrl = serviceBaseUrl + "/Messages/encode";

		try {
			String response = remoteExecutor.executeGet(serviceUrl).replace("\\", "");
			if (response != null) {
				if (response.contains("true")) {
					return true;
				}
			}
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			return false;
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return false;
		}
		return false;
	}

	public static boolean ping(MetrixRemoteExecutor remoteExecutor, String serviceBaseUrl) {
		String serviceUrl = serviceBaseUrl + "/Messages/ping";

		try {
			String response = remoteExecutor.executeGet(serviceUrl).replace("\\", "");

			if (response != null) {
				if (response.contains("true")) {
					return true;
				}
			}
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			return false;
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return false;
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return false;
	}

	public static Hashtable<String, String> getOidcPersonSetting(Activity activity, String serviceUrl, String refreshToken, String idToken, String accessToken) {
		Hashtable<String, String> oidcUserInfo = new Hashtable<String, String>();

		MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(activity, 5);

		try {
			if (ping(remoteExecutor, serviceUrl) == false)
				return oidcUserInfo;

			remoteExecutor = new MetrixRemoteExecutor(activity);    // back to standard timeout

			String message = "<oidcinfo>" + "<id_token>" + idToken + "</id_token>"+ "<access_token>" + accessToken + "</access_token>" + "<refresh_token>" + refreshToken + "</refresh_token>" + "</oidcinfo>";

			message = message.replace("&", "");
			String fullUrl = serviceUrl + "/activation/oidcinfo";

			String response = remoteExecutor.executePost(fullUrl, null, message);
			//response = response.substring(1, response.length()-1).replace("\\", "");

			if (response != null) {
				JSONObject jResult = new JSONObject(response);
				JSONObject jSelect = jResult.getJSONObject("perform_login_result");

				JSONArray jTables = jSelect.names();

				for (int m = 0; m < jTables.length(); m++) {
					String tableName = jTables.getString(m);

					if(tableName.equalsIgnoreCase("session_info")) {
						JSONObject openIdSetting = jSelect.getJSONObject(tableName);
						String personId  = openIdSetting.getString("person_id");
						if(!MetrixStringHelper.isNullOrEmpty(personId)) {
							oidcUserInfo.put("person_id", personId);
							SettingsHelper.saveRememberMe(activity, personId);
						}
						String password = openIdSetting.getString("password");
						if(!MetrixStringHelper.isNullOrEmpty(password)) {
							oidcUserInfo.put("password", password);
							SettingsHelper.saveStringSetting(activity,SettingsHelper.USER_LOGIN_PASSWORD, password, false);
						}
					}
				}
			}
		}
		catch(Exception ex) {
			LogManager.getInstance().error("Acquire Oidc Person Info Error", ex.getMessage());
		}

		return oidcUserInfo;
	}

	public static Hashtable<String, String> getOidcConfiguration(Activity activity) {
		Hashtable<String, String> oidcSetting = new Hashtable<String, String>();
		String serviceUrl = SettingsHelper.getStringSetting(activity, SettingsHelper.OIDC_AUTHORIZATION_URI);
		MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(activity, 5);

		try {
			String fullUrl = serviceUrl + ".well-known/openid-configuration";
			String response = remoteExecutor.executeGet(fullUrl);

			if (response != null) {
				JSONObject jResult = new JSONObject(response);

				if(jResult != null) {
					String frontChannelSupported = jResult.optString("frontchannel_logout_supported");
					String logoutEndPoint  = jResult.getString("end_session_endpoint");
					JSONArray jScopes = jResult.getJSONArray("scopes_supported");
					String authEndPoint  = jResult.getString("authorization_endpoint");
					String tokenEndPoint  = jResult.getString("token_endpoint");
					String userInfoEndPoint  = jResult.getString("userinfo_endpoint");

					if(!MetrixStringHelper.isNullOrEmpty(frontChannelSupported) && frontChannelSupported.equalsIgnoreCase("true")) {
						oidcSetting.put("frontchannel_logout_supported", frontChannelSupported);
						SettingsHelper.saveBooleanSetting(activity, SettingsHelper.OIDC_FRONTCHANNEL_LOGOUT_SUPPORTED, true);
					}

					if(!MetrixStringHelper.isNullOrEmpty(logoutEndPoint)) {
						oidcSetting.put("OIDC_ENDSESSION_ENDPOINT", logoutEndPoint);
						SettingsHelper.saveStringSetting(activity, SettingsHelper.OIDC_ENDSESSION_ENDPOINT_URI, logoutEndPoint, true);
					}

					if(jScopes != null){
						String scopes = "";
						for (int i = 0; i < jScopes.length(); i++) {
							String scope = jScopes.getString(i);

							if(i==0)
								scopes += scope;
							else
								scopes += " "+scope;
						}
						oidcSetting.put("OIDC_SCOPE", scopes);
						SettingsHelper.saveStringSetting(activity, SettingsHelper.OIDC_SCOPE, scopes, true);
					}

					if(!MetrixStringHelper.isNullOrEmpty(authEndPoint)) {
						oidcSetting.put("OIDC_AUTHORIZATION_ENDPOINT_URI", authEndPoint);
						SettingsHelper.saveStringSetting(activity,SettingsHelper.OIDC_AUTHORIZATION_ENDPOINT_URI, authEndPoint, true);
					}

					if(!MetrixStringHelper.isNullOrEmpty(tokenEndPoint)) {
						oidcSetting.put("OIDC_TOKEN_ENDPOINT_URI", tokenEndPoint);
						SettingsHelper.saveStringSetting(activity,SettingsHelper.OIDC_TOKEN_ENDPOINT_URI, tokenEndPoint, true);
					}

					if(!MetrixStringHelper.isNullOrEmpty(userInfoEndPoint)) {
						oidcSetting.put("OIDC_USER_INFO_ENDPOINT_URI", userInfoEndPoint);
						SettingsHelper.saveStringSetting(activity,SettingsHelper.OIDC_USER_INFO_ENDPOINT_URI, userInfoEndPoint, true);
					}
				}
			}
		}
		catch(Exception ex) {
			LogManager.getInstance().error("Acquire Oidc Configuration Info Error", ex.getMessage());
		}

		return oidcSetting;
	}

	public static Hashtable<String, String> getAuthenticationMethods(Activity activity, String serviceUrl) {
		Hashtable<String, String> authMethods = new Hashtable<String, String>();

		MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(activity, 5);

		try {
			if (ping(remoteExecutor, serviceUrl) == false)
				return authMethods;

			remoteExecutor = new MetrixRemoteExecutor(activity);    // back to standard timeout
			String fullUrl = serviceUrl + "/activation/authinfo";

			String response = remoteExecutor.executeGet(fullUrl);

			if (response != null) {
				JSONObject jResult = new JSONObject(response);
				JSONObject jSelect = jResult.getJSONObject("perform_get_server_authentication_result");

				JSONArray jTables = jSelect.names();

				for (int m = 0; m < jTables.length(); m++) {
					String tableName = jTables.getString(m);
					if (tableName.equalsIgnoreCase("azure_setting"))
					{
						JSONObject azureEnvironment = jSelect.getJSONObject(tableName);
						String isAzureValue = azureEnvironment.getString("is_azure");
						if (!MetrixStringHelper.isNullOrEmpty(isAzureValue) && isAzureValue.equalsIgnoreCase("Y"))
						{
							SettingsHelper.saveStringSetting(activity, SettingsHelper.IS_AZURE, "Y", true);
							String attachmentBaseValue = azureEnvironment.getString("azure_attachment_storage");
							if(!MetrixStringHelper.isNullOrEmpty(attachmentBaseValue))
							{
								SettingsHelper.saveStringSetting(activity, SettingsHelper.AZURE_ATTACHMENT_STORAGE_BASE, attachmentBaseValue, true);
							}
							else
							{
								throw new InvalidParameterException("the download base URL is missing in Azure environment");
							}
						}
						else
							SettingsHelper.saveStringSetting(activity, SettingsHelper.IS_AZURE, "N", true);
					}

					if(tableName.equalsIgnoreCase("authenticate_open_id_connect")) {
						JSONObject openIdSetting = jSelect.getJSONObject(tableName);

						String fsmAuthMethods = openIdSetting.getString("fsm_authentication_methods");
						if (!MetrixStringHelper.isNullOrEmpty(fsmAuthMethods)) {
							authMethods.put("FSM_AUTHENTICATION_METHODS", fsmAuthMethods);
							SettingsHelper.saveStringSetting(activity, SettingsHelper.AUTHENTICATION_METHODS, fsmAuthMethods, true);
						}

						String openIdAuth = openIdSetting.getString("openid_ad_authority");
						if (!MetrixStringHelper.isNullOrEmpty(openIdAuth)) {
							authMethods.put("openid_ad_authority", openIdAuth);
							SettingsHelper.saveStringSetting(activity, SettingsHelper.OIDC_AUTHORIZATION_URI, openIdAuth, true);
						}
						String clientIdCompress = openIdSetting.getString("openid_native_application_id");
						if (!MetrixStringHelper.isNullOrEmpty(openIdAuth)) {
							String clientId = MetrixStringHelper.decodeBase64ToString(clientIdCompress);
							authMethods.put("openid_native_application_id", clientId);
							SettingsHelper.saveStringSetting(activity, SettingsHelper.OIDC_CLIENT_ID, clientId, true);
						}
					}
				}
			}
		}
		catch(Exception ex) {
			LogManager.getInstance().error("Acquire Azure Setting Error", ex.getMessage());
		}

		return authMethods;
	}

	public static String GetAuthenticationUrl(String clientId, String dirTenant, String redirectUri, String adPersonId) {
		//OAuth 2.0 authorization Request
		String requestFormat = "https://login.microsoftonline.com/%1s/oauth2/authorize?response_type=code&client_id=%2s&redirect_uri=%3s&state=%4s&prompt=login&login_hint=%5s";
		String requestUri = String.format(requestFormat, dirTenant, clientId, redirectUri, UUID.randomUUID().toString(), adPersonId);
		return requestUri;
	}

	public static void configureAppParams() {
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
	}

	public static void resetServerPasswordChanged(String personId, String hashedServerPass) {
		// update user_credentials with hashPassword and set SETTING_PASSWORD_UPDATED = N
		MetrixDatabaseManager.executeSql(String.format("update user_credentials set password = '%1$s' where person_id = '%2$s'", hashedServerPass, personId));
		SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), "SETTING_PASSWORD_UPDATED", "N",true);
		SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), SettingsHelper.USER_LOGIN_PASSWORD, hashedServerPass, true);
		SettingsHelper.saveBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.PCHANGE, false);
		SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), SettingsHelper.PCHANGE_SIMPLEHASH, "",true);
		SettingsHelper.saveBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.PCHANGE_JUST_PROCESSED, true);
	}
}
