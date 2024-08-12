package com.metrix.metrixmobile.global;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixErrorInfo;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.JobList;

public class MobileGlobal {
	public static final int GET_LOOKUP_RESULT = 1234;
	public static final String RETURN_LOOKUP_RESULT = "Return Lookup Result";
	public static MetrixErrorInfo mErrorInfo;
	public static final int UPDATE_WORK_STATUS_RESULT = 123457;
	public static final int END_SESSION_REQUEST_CODE = 911;
	public static final int END_SESSION_RELOGIN_CODE = 800;
	public static boolean jobListFilterEngaged = false; 

	// provides a list of activities for which error correction is not possible
	public static final String[] nonCorrectableActivities = new String[] {
		"com.metrix.metrixmobile.CalendarExceptionList",
		"com.metrix.metrixmobile.DebriefNonPartUsageList",
		"com.metrix.metrixmobile.DebriefPartUsageList",
		"com.metrix.metrixmobile.DebriefPartUsageType",
		"com.metrix.metrixmobile.DebriefPaymentList",
		"com.metrix.metrixmobile.DebriefProductRemove",
		"com.metrix.metrixmobile.DebriefTaskAttachmentList",
		"com.metrix.metrixmobile.DebriefTaskSteps",
		"com.metrix.metrixmobile.DebriefTaskTextList",
		"com.metrix.metrixmobile.ProductHistory",
		"com.metrix.metrixmobile.system.MetadataListDebriefActivity",
		"com.metrix.metrixmobile.system.MetadataListQuoteActivity",
		"com.metrix.metrixmobile.system.MetadataListScheduleActivity"
	};
	
	private static String DemoBuild = "DemoBuild";
	private static String DatabaseVersion = "DatabaseVersion";
	
	public static int getDatabaseVersion(Context context){
		return MetrixApplicationAssistant.getMetaIntValue(context, DatabaseVersion);
	}

	public static boolean isDemoBuild(Context context){
		return MetrixApplicationAssistant.getMetaBooleanValue(context, DemoBuild);
	}
}