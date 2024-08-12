package com.metrix.architecture.utilities;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;

public class MetrixPasswordHelper {
	public static String getUserPassword() {
		String currPassword = "";
		
		try {
			currPassword = User.getUser().password;
			if (MetrixStringHelper.isNullOrEmpty(currPassword)) {
				// user_credentials should only ever have one row
				currPassword = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", "1=1");
				
				// if it's still empty, log that this occurred
				if (MetrixStringHelper.isNullOrEmpty(currPassword)) {
					LogManager.getInstance().error("MetrixPasswordHelper.getUserPassword failed to get a non-empty password from DB.");
					
					// if the DB attempt failed, try grabbing user password from app settings
					currPassword = SettingsHelper.getStringSetting(MobileApplication.getAppContext(), SettingsHelper.USER_LOGIN_PASSWORD);
					// if it's still empty, log that this occurred
					if (MetrixStringHelper.isNullOrEmpty(currPassword)) {
						LogManager.getInstance().error("MetrixPasswordHelper.getUserPassword also failed to get a non-empty password from app settings.");
					}
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return currPassword;
	}
}
