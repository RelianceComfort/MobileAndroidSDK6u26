package com.metrix.metrixmobile.system;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.CommitmentList;
import com.metrix.metrixmobile.CustomerList;
import com.metrix.metrixmobile.EscalationList;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.Profile;
import com.metrix.metrixmobile.R; //com.metrix.architecture.R;
import com.metrix.metrixmobile.ReceivingList;
import com.metrix.metrixmobile.StockCount;
import com.metrix.metrixmobile.StockList;
import com.metrix.metrixmobile.TeamList;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.metrixmobile.global.MetrixImportantInformation;
import com.metrix.metrixmobile.global.MetrixWorkStatusAssistant;
import com.metrix.metrixmobile.global.MobileGlobal;

public class DemoHome extends MetrixActivity implements View.OnClickListener {
	Button mJobsButton, mCustomersButton, mInventoryButton, mTeamButton, mProfileButton, mWorkStatusButton, mAdminButton, mReceivingButton, mAboutButton;
	TextView mInformation;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_home);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.important_information_heading, "ImportantInformation"));
		resourceStrings.add(new ResourceValueObject(R.id.tasks, "Jobs"));
		resourceStrings.add(new ResourceValueObject(R.id.customers, "Customers"));
		resourceStrings.add(new ResourceValueObject(R.id.equipment, "Stock"));
		resourceStrings.add(new ResourceValueObject(R.id.team, "Team"));
		resourceStrings.add(new ResourceValueObject(R.id.receiving, "Receiving"));
		resourceStrings.add(new ResourceValueObject(R.id.work_status, "WorkStatus"));
		resourceStrings.add(new ResourceValueObject(R.id.admin, "Admin"));
		resourceStrings.add(new ResourceValueObject(R.id.profile, "MyProfile"));
		resourceStrings.add(new ResourceValueObject(R.id.about, "About"));
		super.onStart();

		mJobsButton = (Button) findViewById(R.id.tasks);
		mCustomersButton = (Button) findViewById(R.id.customers);
		mInventoryButton = (Button) findViewById(R.id.equipment);
		mTeamButton = (Button) findViewById(R.id.team);
		mProfileButton = (Button) findViewById(R.id.profile);
		mWorkStatusButton = (Button) findViewById(R.id.work_status);
		mAdminButton = (Button) findViewById(R.id.admin);
		mReceivingButton = (Button) findViewById(R.id.receiving);
		mAboutButton = (Button) findViewById(R.id.about);

		mJobsButton.setOnClickListener(this);
		mCustomersButton.setOnClickListener(this);
		mInventoryButton.setOnClickListener(this);
		mTeamButton.setOnClickListener(this);
		mProfileButton.setOnClickListener(this);
		mWorkStatusButton.setOnClickListener(this);
		mAdminButton.setOnClickListener(this);
		mReceivingButton.setOnClickListener(this);
		mAboutButton.setOnClickListener(this);

		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		enhanceButtonText();
		addImportantInformation();
		
		if (!MetrixStringHelper.isNullOrEmpty(SettingsHelper.getStringSetting(this, "DemoBuildFirstRun"))) {
			SettingsHelper.saveStringSetting(this, "DemoBuildFirstRun", "", true);
			Intent intent = MetrixActivityHelper.createActivityIntent(this, DemoWelcome.class);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
		
		this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionDemoHome");
	}

	private void enhanceButtonText() {
		this.removePreviousEnhancements(mJobsButton);
		this.removePreviousEnhancements(mTeamButton);
		this.removePreviousEnhancements(mCustomersButton);
		this.removePreviousEnhancements(mWorkStatusButton);
		this.removePreviousEnhancements(mInventoryButton);
		this.removePreviousEnhancements(mReceivingButton);

		String personId = User.getUser().personId;
		int jobsCount = MetrixDatabaseManager.getCount("task", "person_id='" + personId + "' and task_status in (select task_status from task_status where status = 'OP' and task_status <> '" + MobileApplication.getAppParam("REJECTED_TASK_STATUS") + "') and request_id is not null");
		mJobsButton.setText(mJobsButton.getText() + " (" + jobsCount + ")");

		int teamMemberCount = MetrixDatabaseManager.getCount("team_member", "team_id in (select distinct(team_id) from team_member where person_id = '" + personId + "') and person_id != '" + personId + "'");
		mTeamButton.setText(mTeamButton.getText() + " (" + teamMemberCount + ")");

		int customersCount = MetrixDatabaseManager.getCount("place", "place.whos_place = 'CUST'");
		mCustomersButton.setText(mCustomersButton.getText() + " (" + customersCount + ")");

		int stockCount = MetrixDatabaseManager.getCount("stock_bin", "qty_on_hand > 0");
		mInventoryButton.setText(mInventoryButton.getText() + " (" + stockCount + ")");

		String workStatus = MetrixDatabaseManager.getFieldStringValue("person", "work_status", "person_id='" + personId + "'");
		mWorkStatusButton.setText(mWorkStatusButton.getText() + " (" + workStatus + ")");

		int receivingCount = MetrixDatabaseManager.getCount("receiving", "inventory_adjusted = 'N'");
		mReceivingButton.setText(mReceivingButton.getText() + " (" + receivingCount + ")");
	}

	private void addImportantInformation() {
		mLayout = (ViewGroup) findViewById(R.id.home_background);
		MetrixImportantInformation.reset(mLayout, this);
		String personId = User.getUser().personId;

        // New Jobs
        String extraFilter = MetrixDatabaseManager.getAppParam("ADDITIONAL_JOB_LIST_CONSTRAINTS");
        String filter = "task_status = 'OPEN' and request_id is not null and person_id='" + personId + "'";
        if(!MetrixStringHelper.isNullOrEmpty(extraFilter)){
            filter += " and ("+extraFilter+")";
        }
        int jobsCount = MetrixDatabaseManager.getCount("task", filter);
        if (jobsCount > 0) {
            String message = "";
            if (jobsCount == 1)
                message = AndroidResourceHelper.getMessage("AssignedANewJob");
            else
                message = AndroidResourceHelper.getMessage("AssignedNewJobs1Arg", jobsCount);

            android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(DemoHome.this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("filter", AndroidResourceHelper.getMessage("New"));
                    MetrixActivityHelper.startNewActivity(DemoHome.this, intent);
                }
            };
            MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("NewLCase"), onClickListener);
        }

        // Commitments
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select count(*) from time_commit ");
        queryBuilder.append("join task on time_commit.request_id = task.request_id ");
        queryBuilder.append("left outer join response_code response_code1 on time_commit.response_code = response_code1.response_code ");
        queryBuilder.append("left outer join global_code_table global_code_table1 on time_commit.response_type = global_code_table1.code_value and global_code_table1.code_name = 'RESPONSE_TYPE' ");
        queryBuilder.append("where time_commit.actual_dttm is null and response_code1.response_type = time_commit.response_type and commit_dttm < '"
                + MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true) + "'");
        String commitmentsCountStr = MetrixDatabaseManager.getFieldStringValue(queryBuilder.toString());
        int commitmentsCount = Integer.parseInt(commitmentsCountStr);
        if (commitmentsCount > 0) {
            String message = "";
            String hyperlink = "";
            if (commitmentsCount == 1) {
                message = AndroidResourceHelper.getMessage("AnOverdueCommitment");
                hyperlink = AndroidResourceHelper.getMessage("CommitmentLCase");
            } else {
                message = AndroidResourceHelper.getMessage("OverdueCommitments1Arg", commitmentsCount);
                hyperlink = AndroidResourceHelper.getMessage("CommitmentsLCase");
            }
            android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(DemoHome.this, CommitmentList.class);
                    intent.putExtra("filter", AndroidResourceHelper.getMessage("Overdue"));
                    MetrixActivityHelper.startNewActivity(DemoHome.this, intent);
                }
            };
            MetrixImportantInformation.add(mLayout, message, hyperlink, onClickListener);
        }

        // Escalations
        int escalationsCount = MetrixDatabaseManager.getCount("escalation", "(status is null or status != 'CLOSED') and escalation.table_name = 'TASK' and person_id = '" + personId + "'");
        if (escalationsCount > 0) {
            String message = "";
            if (escalationsCount == 1)
                message = AndroidResourceHelper.getMessage("AnEscalation");
            else
                message = AndroidResourceHelper.getMessage("Escalations1Arg", escalationsCount);

            android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(DemoHome.this, EscalationList.class);
                    MetrixActivityHelper.startNewActivity(DemoHome.this, intent);
                }
            };
            MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("AssignedLCase"), onClickListener);
        }

        // Overdue Jobs
        filter = "task_status in (select task_status from task_status where status = 'OP' and task_status <> '" + MobileApplication.getAppParam("REJECTED_TASK_STATUS") + "') and plan_start_dttm < '"
                + MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS,true)
                + "' and request_id is not null and person_id='"
                + personId + "'";
        if (!MetrixStringHelper.isNullOrEmpty(extraFilter)) {
            filter += " and ("+extraFilter+")";
        }
        int overdueCount = MetrixDatabaseManager.getCount("task", filter);
        if (overdueCount > 0) {
            String message = "";
            if (overdueCount == 1)
                message = AndroidResourceHelper.getMessage("AnOverdueJob");
            else
                message = AndroidResourceHelper.getMessage("JobsOverdue1Arg", overdueCount);

            android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(DemoHome.this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("filter", AndroidResourceHelper.getMessage("Overdue"));
                    MetrixActivityHelper.startNewActivity(DemoHome.this, intent);
                }
            };
            MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("OverdueLCase"), onClickListener);
        }

        // Expiring Password
        String value = MetrixDatabaseManager.getFieldStringValue("person", "password_expire_dt", "person_id = '" + personId + "'");
        Calendar expireDate = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, value, ISO8601.Yes);
        Calendar fifteenDays = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, 15));
        if (expireDate.before(fifteenDays)) {
            String message = AndroidResourceHelper.getMessage("PasswordExpiringSoon1Arg", MetrixDateTimeHelper.convertDateTimeFromDBToUI(value, MetrixDateTimeHelper.DATE_FORMAT));
            MetrixImportantInformation.add(mLayout, message);
        }

        // Stock Counts
        ArrayList<Hashtable<String, String>> stockCountList = MetrixDatabaseManager.getFieldStringValuesList("select distinct run_id from stock_count where posted is null");
        if (stockCountList != null) {
            if (stockCountList.size() > 0) {
                String message = "";
                if (stockCountList.size() == 1)
                    message = AndroidResourceHelper.getMessage("ANewStockCount");
                else
                    message = AndroidResourceHelper.getMessage("NewStockCounts1Arg", stockCountList.size());

                android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MetrixPublicCache.instance.addItem("ToBarcode", "N");
                        Intent intent = MetrixActivityHelper.createActivityIntent(DemoHome.this, StockCount.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        MetrixActivityHelper.startNewActivity(DemoHome.this, intent);
                    }
                };
                MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("NewLCase"), onClickListener);
            }
        }
	}

	private void removePreviousEnhancements(Button button) {
		if (button.getText().toString().indexOf(" (") > 0) {
			button.setText(button.getText().toString().substring(0, button.getText().toString().indexOf(" (")));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tasks:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.customers:
			intent = MetrixActivityHelper.createActivityIntent(this, CustomerList.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.equipment:
			intent = MetrixActivityHelper.createActivityIntent(this, StockList.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.team:
			intent = MetrixActivityHelper.createActivityIntent(this, TeamList.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.profile:
			intent = MetrixActivityHelper.createActivityIntent(this, Profile.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.work_status:
			MetrixWorkStatusAssistant statusAssistant = new MetrixWorkStatusAssistant();
			statusAssistant.displayStatusDialog(this, this, true);
			break;
		case R.id.admin:
			intent = MetrixActivityHelper.createActivityIntent(this, DemoApplicationSettings.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.receiving:
			intent = MetrixActivityHelper.createActivityIntent(this, ReceivingList.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.about:
			intent = MetrixActivityHelper.createActivityIntent(this, DemoWelcome.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		default:
			super.onClick(v);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case MobileGlobal.UPDATE_WORK_STATUS_RESULT:
			if (resultCode == RESULT_OK) {
				this.enhanceButtonText();
			}
			break;
		}
	}
}