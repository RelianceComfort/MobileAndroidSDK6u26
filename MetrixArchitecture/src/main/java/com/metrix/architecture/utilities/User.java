package com.metrix.architecture.utilities;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;

import android.content.Context;

public class User {
	public String personId = "";
	public String password = "";
	public String name = "";
	public String firstName = "";
	public String lastName = "";
	public String currency = "";
	public String worksFromPlace = "";
	public String stockFromPlace = "";
	public String stockFromLocation = "";
	public String emailAddress = "";
	public String phoneNumber = "";
	public String mobileNumber = "";
	public String passwordExpireDate = "";
	public Integer sequence = -1;
	public String corporateCurrency = "";
	public String laborRate = "";
	public String localeCode = "";
	public String serverDateTimeFormat = "";
	public String serverDateFormat = "";
	public String serverTimeFormat = "";
	public String serverNumericFormat = "";
	public String serverLocaleCode = "";
	public String serverTimeZoneId = "";
	public String serverTimeZoneOffset = "";

	private static final String mCacheKey = "MetrixUser";

	public User() {
	}

	public static boolean setUser(String personId, Context context) {
		User user = new User();
		context = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);
		LogManager.getInstance().info("MetrixPublicCache getInstance() in User hash "+ String.valueOf(MetrixPublicCache.instance.hashCode()));
		
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select person_id, first_name, last_name, currency, email_address, phone, mobile_phone, password_expire_dt, labor_rate_code, locale_code from person where person_id = '" + personId + "'", null);

			if (cursor != null && cursor.moveToFirst()) {
				user.personId = cursor.getString(0);
				user.firstName = cursor.getString(1);
				user.lastName = cursor.getString(2);
				user.name = String.format("%1$s %2$s", user.firstName, user.lastName);
				user.currency = cursor.getString(3);
				user.emailAddress = cursor.getString(4);
				user.phoneNumber = cursor.getString(5);
				user.mobileNumber = cursor.getString(6);
				user.passwordExpireDate = cursor.getString(7);
				user.laborRate = cursor.getString(8);
				user.localeCode = cursor.getString(9);
			}

			cursor.close();

			if (MetrixStringHelper.isNullOrEmpty(user.personId)) {
				user.personId = personId;
			}

			user.password = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", "person_id='" + personId + "'");
			
			user.sequence = SettingsHelper.getDeviceSequence(context);
			user.worksFromPlace = MetrixDatabaseManager.getFieldStringValue("person_place", "place_id", "place_relationship='WORKS_FROM' and person_id='" + personId + "'");
			user.corporateCurrency = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='CORPORATE_CURRENCY'");

			user.stockFromPlace = MetrixDatabaseManager.getFieldStringValue("person_place", "place_id", "place_relationship='FOR_STOCK' and person_id='" + personId + "'");
			user.stockFromLocation = MetrixDatabaseManager.getFieldStringValue("person_place", "location", "place_relationship='FOR_STOCK' and person_id='" + personId + "'");

			user.serverDateTimeFormat = SettingsHelper.getServerDateTimeFormat(context);
			user.serverDateFormat = SettingsHelper.getServerDateFormat(context);
			user.serverTimeFormat = SettingsHelper.getServerTimeFormat(context);
			user.serverNumericFormat = SettingsHelper.getServerNumericFormat(context);
			user.serverTimeZoneId = SettingsHelper.getServerTimeZoneId(context);
			user.serverTimeZoneOffset = SettingsHelper.getServerTimeZoneOffset(context);
			user.serverLocaleCode = SettingsHelper.getServerLocaleCode(context);			

			return true;

		} catch (Exception e) {
			LogManager.getInstance().error(e);
			return false;
		} finally {
			MetrixPublicCache.instance.addItem(mCacheKey, user);
			
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static User getUser() {
		User user = new User();
		
		if(MetrixPublicCache.instance.containsKey(mCacheKey))
			user = (User) MetrixPublicCache.instance.getItem(mCacheKey);
		
		Context context = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);
		
		if(user==null && context!=null){
			if(setUser(SettingsHelper.getRememberMe(context), context))			
				user = (User)MetrixPublicCache.instance.getItem(mCacheKey);
			else {
				user = new User();
				user.personId = SettingsHelper.getActivatedUser(context);				
			}
		}
		
		return user;
	}

	public String getCurrencyToUse() {
		if (MetrixStringHelper.isNullOrEmpty(this.currency)) {
			return this.corporateCurrency;
		} else {
			return this.currency;
		}
	}
}
