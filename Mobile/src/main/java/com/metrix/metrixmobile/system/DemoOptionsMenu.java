package com.metrix.metrixmobile.system;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuItem;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.CustomerList;
import com.metrix.metrixmobile.EscalationList;
import com.metrix.metrixmobile.Home;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.Profile;
import com.metrix.metrixmobile.PurchaseOrderList;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.ReceivingList;
import com.metrix.metrixmobile.StockList;
import com.metrix.metrixmobile.TeamList;
import com.metrix.metrixmobile.global.MetrixWorkStatusAssistant;
import com.metrix.metrixmobile.global.MobileGlobal;

public class DemoOptionsMenu {

	private static final String JOBS = "Jobs";
	private static final String CUSTOMERS = "Customers";
	private static final String EQUIPMENT = "Stock";
	private static final String TEAM = "Team";
	private static final String ADMIN = "Admin";
	private static final String ABOUT = "About";
	private static final String HOME = "Home";
	private static final String WORKSTATUS = "Work Status";
	private static final String PURCHASEORDER = "Purchase Order";
	private static final String RECEIVING = "Receiving";
	private static final String QUERY = "Restore Demonstration Data";
	private static final String PROFILE = "My Profile";
	private static final String ESCALATIONS = "Escalations";
	private static final String HELP = "Help";
	private static final String TIMEREPORTING = "Time Reporting";

	public static void onPrepareOptionsMenu(ArrayList<MetrixSlidingMenuItem> slidingMenuItems) {

		String personId = User.getUser().personId;

		slidingMenuItems.clear();

		int jobsCount = MetrixDatabaseManager.getCount("task", "person_id='" + personId + "' and task_status in (select task_status from task_status where status = 'OP' and task_status <> '" + MobileApplication.getAppParam("REJECTED_TASK_STATUS") + "') and request_id is not null");

		int teamMemberCount = MetrixDatabaseManager.getCount("team_member", "team_id in (select distinct(team_id) from team_member where person_id = '"
				+ personId + "') and person_id != '" + personId + "'");

		int customersCount = MetrixDatabaseManager.getCount("place", "place.whos_place = 'CUST'");
		int stockCount = MetrixDatabaseManager.getCount("stock_bin", "qty_on_hand > 0");
		int receivingCount = MetrixDatabaseManager.getCount("receiving", "inventory_adjusted='N'");
		int escalationsCount = MetrixDatabaseManager.getCount("escalation", "");

		MetrixSlidingMenuItem slidingMenuItemJobs = new MetrixSlidingMenuItem(DemoOptionsMenu.JOBS, String.valueOf(jobsCount), R.drawable.sliding_menu_jobs);
		slidingMenuItems.add(slidingMenuItemJobs);

		MetrixSlidingMenuItem slidingMenuItemCustomers = new MetrixSlidingMenuItem(DemoOptionsMenu.CUSTOMERS, String.valueOf(customersCount), R.drawable.sliding_menu_jobs);
		slidingMenuItems.add(slidingMenuItemCustomers);

		MetrixSlidingMenuItem slidingMenuItemEquipments = new MetrixSlidingMenuItem(DemoOptionsMenu.EQUIPMENT, String.valueOf(stockCount), R.drawable.sliding_menu_stock);
		slidingMenuItems.add(slidingMenuItemEquipments);

		MetrixSlidingMenuItem slidingMenuItemWorkStatus = new MetrixSlidingMenuItem(DemoOptionsMenu.WORKSTATUS, R.drawable.sliding_menu_shift);
		slidingMenuItems.add(slidingMenuItemWorkStatus);

		MetrixSlidingMenuItem slidingMenuItemPurchaseOrder = new MetrixSlidingMenuItem(DemoOptionsMenu.PURCHASEORDER, R.drawable.sliding_menu_stock);
		slidingMenuItems.add(slidingMenuItemPurchaseOrder);

		MetrixSlidingMenuItem slidingMenuItemHome = new MetrixSlidingMenuItem(DemoOptionsMenu.HOME, R.drawable.sliding_menu_home);
		slidingMenuItems.add(slidingMenuItemHome);

		MetrixSlidingMenuItem slidingMenuItemTeam = new MetrixSlidingMenuItem(DemoOptionsMenu.TEAM, String.valueOf(teamMemberCount), R.drawable.sliding_menu_team);
		slidingMenuItems.add(slidingMenuItemTeam);

		MetrixSlidingMenuItem slidingMenuItemReceiving = new MetrixSlidingMenuItem(DemoOptionsMenu.RECEIVING, String.valueOf(receivingCount), R.drawable.sliding_menu_receiving);
		slidingMenuItems.add(slidingMenuItemReceiving);

		if (escalationsCount > 0) {
			MetrixSlidingMenuItem slidingMenuItemEscalations = new MetrixSlidingMenuItem(DemoOptionsMenu.ESCALATIONS, String.valueOf(escalationsCount), R.drawable.sliding_menu_receiving);
			slidingMenuItems.add(slidingMenuItemEscalations);
		}

		MetrixSlidingMenuItem slidingMenuItemTimeReporting = new MetrixSlidingMenuItem(DemoOptionsMenu.TIMEREPORTING, R.drawable.sliding_menu_about);
		slidingMenuItems.add(slidingMenuItemTimeReporting);

		MetrixSlidingMenuItem slidingMenuItemAdmin = new MetrixSlidingMenuItem(DemoOptionsMenu.ADMIN, R.drawable.sliding_menu_settings);
		slidingMenuItems.add(slidingMenuItemAdmin);

		MetrixSlidingMenuItem slidingMenuItemProfile = new MetrixSlidingMenuItem(DemoOptionsMenu.PROFILE, R.drawable.sliding_menu_profile);
		slidingMenuItems.add(slidingMenuItemProfile);

		MetrixSlidingMenuItem slidingMenuItemQuery = new MetrixSlidingMenuItem(DemoOptionsMenu.QUERY, R.drawable.sliding_menu_query);
		slidingMenuItems.add(slidingMenuItemQuery);

		MetrixSlidingMenuItem slidingMenuItemHelp = new MetrixSlidingMenuItem(DemoOptionsMenu.HELP, R.drawable.sliding_menu_about);
		slidingMenuItems.add(slidingMenuItemHelp);

		MetrixSlidingMenuItem slidingMenuItemAbout = new MetrixSlidingMenuItem(DemoOptionsMenu.ABOUT, R.drawable.sliding_menu_about);
		slidingMenuItems.add(slidingMenuItemAbout);
	}

	public static void onOptionsItemSelected(Activity activity, MetrixSlidingMenuItem slidingMenuItem, String helpText, Boolean handlingErrors) {

		String title = slidingMenuItem.getTitle();

		if(!MetrixStringHelper.isNullOrEmpty(title)){

			if (title.contains(DemoOptionsMenu.JOBS)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.CUSTOMERS)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, CustomerList.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.EQUIPMENT)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, StockList.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.TEAM)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, TeamList.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			}
			else if (title.contains(DemoOptionsMenu.ADMIN)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, DemoApplicationSettings.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.ABOUT)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, About.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.HOME)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, Home.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.WORKSTATUS)) {
				MetrixWorkStatusAssistant statusAssistant = new MetrixWorkStatusAssistant();
				statusAssistant.displayStatusDialog(activity, activity, false);
			}  else if (title.contains(DemoOptionsMenu.PURCHASEORDER)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, PurchaseOrderList.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.RECEIVING)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, ReceivingList.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.QUERY)) {
				MetrixDatabaseManager.performDatabaseImport(R.raw.metrix_demo, "metrix_demo_import.db", activity.getApplicationContext(), false,
						com.metrix.metrixmobile.R.array.system_tables, com.metrix.metrixmobile.R.array.business_tables);
				Login.updateDemoTasks("TECH01");
				Login.updateDemoTimeReporting();
				MetrixUIHelper.showSnackbar(activity, AndroidResourceHelper.getMessage("DataRefreshed"));
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, Home.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.PROFILE)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, Profile.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.ESCALATIONS)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, EscalationList.class);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.HELP)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, Help.class);
				String message = null;
				if(!MetrixStringHelper.isNullOrEmpty(helpText))
					message = helpText;
				else
					message = AndroidResourceHelper.getMessage("HelpNotAvail");

				if(handlingErrors != null)
					if (handlingErrors.booleanValue())
						message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ErrorMess1Arg", MobileGlobal.mErrorInfo.errorMessage);

				message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ScreenColon1Arg", activity.getClass().getSimpleName());

				intent.putExtra("help_text", message);
				MetrixActivityHelper.startNewActivity(activity, intent);
			} else if (title.contains(DemoOptionsMenu.TIMEREPORTING)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile", "TimeReporting");
				MetrixActivityHelper.startNewActivity(activity, intent);
			}
		}
	}
}
