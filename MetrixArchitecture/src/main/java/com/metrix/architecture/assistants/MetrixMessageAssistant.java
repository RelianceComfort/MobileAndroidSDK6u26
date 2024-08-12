package com.metrix.architecture.assistants;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.services.MessageHandler;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.MessageStatus;
import com.metrix.architecture.utilities.LogManager;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Contains helper methods to make it easy to query and interest with data
 * exchange messages for transactions being synchronized with the Mobile
 * Service.
 * 
 * @since 5.4
 */
public class MetrixMessageAssistant {
	/**
	 * Determines whether or not any error messages from M5 exist on the device
	 * waiting for the user to address.
	 * 
	 * @return TRUE if M5 error messages exist, FALSE otherwise.
	 */
	public static boolean errorMessagesExist() {
		boolean exist = false;
		try {
			int count = MetrixDatabaseManager.getCount(new String[] { "mm_message_in", "mm_message_out" }, "mm_message_in.related_message_id = mm_message_out.message_id and mm_message_in.status = '" + MessageStatus.ERROR + "'");
	
			if (count > 0) {
				exist = true;
			} else {
				exist = false;				
			}
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}
		return exist;		
	}


	public static boolean errorMessagesInExist() {
		boolean exist = false;
		try {
			int count = MetrixDatabaseManager.getCount(new String[] { "mm_message_in"}, "mm_message_in.status = '" + MessageStatus.ERROR + "'");

			if (count > 0) {
				exist = true;
			} else {
				exist = false;
			}
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}
		return exist;
	}

	public static ArrayList<Hashtable<String, String>> getErrorMessages() {
		ArrayList<Hashtable<String, String>> errorList = new ArrayList<Hashtable<String, String>> ();
		try {
			errorList = MetrixDatabaseManager.getFieldStringValuesList("select message from mm_message_in where status = '" + MessageStatus.ERROR + "'");
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}
		return errorList;
	}

	public static String getFirstErrorMessageType() {
		ArrayList<Hashtable<String, String>> errorList = getErrorMessages();
		String errorMessage = "";

		if(errorList != null && errorList.size() > 0)
			for(Hashtable<String, String> errorItem : errorList) {
				String jsonError = errorItem.get("message");
				errorMessage = MessageHandler.getErrorMessage(jsonError);

				if(errorMessage.contains("~BatchSyncError"))
					return AndroidResourceHelper.getMessage("BatchSyncErrorOccurred");
				else if(errorMessage.contains("~InitializationError"))
					return AndroidResourceHelper.getMessage("InitializationErrorOccurred");
				else
					break;
			}

		return errorMessage;
	}
}
