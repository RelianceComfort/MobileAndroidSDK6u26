package com.metrix.metrixmobile.policies;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class PasswordPolicy {
	public static String passwordIsValid(String password) {
		String maximumSize = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name = 'PASSWORD_VALUE_MAX_SIZE'");
		String minimumSize = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name = 'PASSWORD_VALUE_MIN_SIZE'");
		String numberOfNumericCharactersRequired = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name = 'PASSWORD_NUMERIC_REQUIRED'");
		String numberOfSpecialCharactersRequired = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name = 'PASSWORD_SPECIAL_REQUIRED'");

		if (MetrixStringHelper.isNullOrEmpty(maximumSize)) {
			maximumSize = "10";
		}

		if (MetrixStringHelper.isNullOrEmpty(minimumSize)) {
			minimumSize = "6";
		}

		if (MetrixStringHelper.isNullOrEmpty(numberOfNumericCharactersRequired)) {
			numberOfNumericCharactersRequired = "0";
		}

		if (MetrixStringHelper.isNullOrEmpty(numberOfSpecialCharactersRequired)) {
			numberOfSpecialCharactersRequired = "0";
		}

		if ((password.length() > Integer.parseInt(maximumSize)) || (password.length() < Integer.parseInt(minimumSize))) {
			return AndroidResourceHelper.getMessage("NewPwdInvalidBySize2Args", minimumSize, maximumSize);
		}

		if (Integer.parseInt(numberOfNumericCharactersRequired) > 0) {
			int numberOfNumericsFound = 0;
			
			for (int i = 0; i < password.length(); i++) {
				if (Character.isDigit(password.charAt(i))) {
					numberOfNumericsFound = numberOfNumericsFound + 1;
				}
			}
			
			if (Integer.parseInt(numberOfNumericCharactersRequired) > numberOfNumericsFound) {
				return AndroidResourceHelper.getMessage("NewPwdInvalidByType2Args", numberOfNumericCharactersRequired, numberOfSpecialCharactersRequired);
			}
		}
			
		if (Integer.parseInt(numberOfSpecialCharactersRequired) > 0) {
			int numberOfSpecialsFound = 0;
			
			for (int i = 0; i < password.length(); i++) {
				if ((!Character.isDigit(password.charAt(i))) && (!Character.isLetter(password.charAt(i)))) {
					numberOfSpecialsFound = numberOfSpecialsFound + 1;
				}
			}
			
			if (Integer.parseInt(numberOfSpecialCharactersRequired) > numberOfSpecialsFound) {
				return AndroidResourceHelper.getMessage("NewPwdInvalidByType2Args", numberOfNumericCharactersRequired, numberOfSpecialCharactersRequired);
			}
		}

		return "";
	}
}