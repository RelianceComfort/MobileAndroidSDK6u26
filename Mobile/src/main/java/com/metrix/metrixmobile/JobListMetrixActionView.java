package com.metrix.metrixmobile;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class JobListMetrixActionView {

	public static String ACTIONS = AndroidResourceHelper.getMessage("ActionsTitle");
	public static String SCHEDULEJOB = AndroidResourceHelper.getMessage("ScheduleJob");
	public static String SHOWMAP = AndroidResourceHelper.getMessage("ShowMap");
	public static String SHOWTEAMMAP = AndroidResourceHelper.getMessage("ShowTeamTaskMap");
	
	//private static final String ACCEPT_REJECT = "Accept/Reject Job"; 
	public static String JOBACCEPT = AndroidResourceHelper.getMessage("AcceptJob");
	public static String JOBREJECT = AndroidResourceHelper.getMessage("RejectJob");
	
	public static String FILTERTODAYSJOBS = AndroidResourceHelper.getMessage("TodaysJobsFilter");
	public static String FILTERTOMORROWSJOBS = AndroidResourceHelper.getMessage("TomorrowsJobsFilter");

	public static final int SHOWMAPJOBMENUITEM = 5;
	public static final int SCHEDULEJOBMENUITEM = 6;
	public static final int JOBSTATUSFILTERTODAYSJOBSMENUITEM = 14;
	public static final int JOBSTATUSFILTERTOMORROWSJOBSMENUITEM = 15;
	public static final int MAPGROUP = 17;
	public static final int JOBMAPFILTERALLMENUITEM = 18;
	public static final int JOBMAPFILTERNEWJOBSMENUITEM = 19;
	public static final int JOBMAPFILTERTODAYSJOBSMENUITEM = 20;
	public static final int JOBMAPFILTERTOMORROWSJOBSMENUITEM = 21;
	public static final int JOBACCEPTMENUITEM = 3;
	public static final int JOBREJECTMENUITEM = 4;

	public static final int SHOWTEAMMAPJOBMENUITEM = 22;

	public static void refreshJobListActionView(){
		ACTIONS = AndroidResourceHelper.getMessage("ActionsTitle");
		SCHEDULEJOB = AndroidResourceHelper.getMessage("ScheduleJob");
		SHOWMAP = AndroidResourceHelper.getMessage("ShowMap");
		SHOWTEAMMAP = AndroidResourceHelper.getMessage("ShowTeamTaskMap");

		//private static final String ACCEPT_REJECT = "Accept/Reject Job";
		JOBACCEPT = AndroidResourceHelper.getMessage("AcceptJob");
		JOBREJECT = AndroidResourceHelper.getMessage("RejectJob");

		FILTERTODAYSJOBS = AndroidResourceHelper.getMessage("TodaysJobsFilter");
		FILTERTOMORROWSJOBS = AndroidResourceHelper.getMessage("TomorrowsJobsFilter");
	}
}
