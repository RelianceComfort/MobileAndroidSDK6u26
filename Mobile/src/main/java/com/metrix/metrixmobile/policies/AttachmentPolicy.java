package com.metrix.metrixmobile.policies;

import java.io.File;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.database.MobileApplication;

public class AttachmentPolicy {
	public static String attachmentIsValid(String fileName) {		
		String maximumSize = MobileApplication.getAppParam("ATTACHMENT_MAX_SIZE"); 			

		if (MetrixStringHelper.isNullOrEmpty(maximumSize)) {
			maximumSize = "10242880";
		}	
		
		try {
			File file = new File(fileName);
			
			if(file.length()> Long.parseLong(maximumSize)) {
				return AndroidResourceHelper.getMessage("ExceedMaxFileSize");
			}			
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}		
		
		return "";
	}
}
