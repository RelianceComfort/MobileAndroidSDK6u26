package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.services.JavaDotNetFormatHelper;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixPasswordHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant.LoginResult;
import com.metrix.metrixmobile.global.MobileGlobal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class Login extends Activity implements View.OnClickListener {
	Button mLogin, mChangePassword;
	CheckBox mShowPassword;
	EditText mPassword, mPersonId;
	CheckBox activateDeviceCheckBox;
	protected MetrixUIHelper metrixUIHelper = new MetrixUIHelper(this);
	private static final Map<String, String> dayOfWeekStrings = new HashMap<String, String>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO Handle the close app settings since the line below doesn't do anything
		boolean cameFromCloseApp = this.getIntent().getBooleanExtra("CLOSE_APP", false);//SettingsHelper.getBooleanSetting(this, SettingsHelper.FROM_CLOSED_APP);//this.getIntent().getBooleanExtra("CLOSE_APP", false);
		boolean cameFromSplash = this.getIntent().getBooleanExtra("SPLASH", false);

		if (!isTaskRoot() && !cameFromCloseApp && !cameFromSplash) {
			finish();
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.login);
		// INIT_FINISHED is to Load database adapters and global variables, it is different from INIT_STARTED for synchronization 
		SettingsHelper.saveBooleanSetting(this, SettingsHelper.INIT_FINISHED, false);

		if (MobileGlobal.isDemoBuild(this)) {
			try {
				MetrixDatabaseManager.createDatabaseAdapters(MobileApplication.getAppContext(), MetrixApplicationAssistant.getMetaIntValue(MobileApplication.getAppContext(), "DatabaseVersion"), com.metrix.metrixmobile.R.array.system_tables,
						com.metrix.metrixmobile.R.array.business_tables);
				MetrixDatabaseManager.performDatabaseImport(R.raw.metrix_demo, "metrix_demo_import.db", this.getApplicationContext(), false, com.metrix.metrixmobile.R.array.system_tables,
						com.metrix.metrixmobile.R.array.business_tables);
			}
			catch(Exception ex) {
				LogManager.getInstance().error(ex);
			}
			finally {
				setupDefaultLocale();
				SettingsHelper.saveStringSetting(this, "DemoBuildFirstRun", "true", true);
			}
		} else {
			if (MetrixStringHelper.isNullOrEmpty(SettingsHelper.getServiceAddress(this))
					|| (MetrixStringHelper.isNullOrEmpty(SettingsHelper.getActivatedUser(this)))) {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceEntry.class);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		}

		String personId = SettingsHelper.getRememberMe(this);

		if (MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild"))
			personId = "TECH01";

		mPersonId = (EditText) findViewById(R.id.personId);
		mPersonId.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

		mPassword = (EditText) findViewById(R.id.password);

		mPassword.setTypeface(Typeface.DEFAULT);
		mPersonId.setTypeface(Typeface.DEFAULT);
		mPassword.setTextSize(18);
		mPersonId.setTextSize(18);

		if (!MetrixStringHelper.isNullOrEmpty(personId)) {
            String oidcSuccess = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ENABLED);
			if (!MobileGlobal.isDemoBuild(this) && personHasPendingPasswordChange(personId)) {
				if (oidcSuccess.equalsIgnoreCase("OFF")) {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDevice.class);
					intent.putExtra("person_id", personId);
					MetrixActivityHelper.startNewActivityAndFinish(this, intent);
					return;
				} else if (oidcSuccess.equalsIgnoreCase("ON")) {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceOIDC.class);
					intent.putExtra("person_id", personId);
					MetrixActivityHelper.startNewActivityAndFinish(this, intent);
					return;
				}
			}

//			boolean ssoEnabled = false;
			boolean oidcEnabled = false;

			if(!MetrixStringHelper.isNullOrEmpty(oidcSuccess)&& oidcSuccess.equalsIgnoreCase("ON")) {
//				ssoEnabled = true; //TODO possible that this line needs to move to a different check
				oidcEnabled = true;
			}

			if ((!SettingsHelper.getManualLogin(this) && !SettingsHelper.getBooleanSetting(this, SettingsHelper.FROM_LOGOUT)) || MobileGlobal.isDemoBuild(this)) { //(!SettingsHelper.getManualLogin(this) || MobileGlobal.isDemoBuild(this) || ssoEnabled) {
				if (MobileGlobal.isDemoBuild(this)) {
					SettingsHelper.setActivatedUser(this, personId);
				}

				String password = MetrixPasswordHelper.getUserPassword();

				// If oidcEnabled is false, then login has to execute performLogin locally
				if(oidcEnabled == false) {
					LoginResult loginResult = MetrixAuthenticationAssistant.performLogin(this, personId, password, false, true);
					boolean success = handleLoginResult(loginResult, personId);
					if (!success) {
						finish();
						return;
					}
				} else {
					String accessToken = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ACCESS_TOKEN);

					if(!MetrixStringHelper.isNullOrEmpty(accessToken)) {
						LoginResult loginResult = MetrixAuthenticationAssistant.performLogin(this, personId, password, false, true);
						boolean success = handleLoginResult(loginResult, personId);
						if (!success) {
							finish();
							return;
						}

						SettingsHelper.saveManualLogin(this,false);
					}
				}

				if (MobileGlobal.isDemoBuild(this)) {
					updateDemoTasks(personId);
					updateDemoTimeReporting();
					SettingsHelper.saveLogLevel(this, "");
					SettingsHelper.saveSyncInterval(this, 60);
					SettingsHelper.saveServiceAddress(this, "http://yourwebserver");
				}

				String enable_time_zone = MobileApplication.getAppParam("MOBILE_ENABLE_TIME_ZONE");
				if (!MetrixStringHelper.isNullOrEmpty(enable_time_zone) && enable_time_zone.toLowerCase().contains("y")) {
					Global.enableTimeZone = true;
				} else {
					Global.enableTimeZone = false;
				}

				String encode_url = MobileApplication.getAppParam("MOBILE_ENCODE_URL_PARAM");
				if (!MetrixStringHelper.isNullOrEmpty(encode_url) && encode_url.toLowerCase().contains("y")) {
					Global.encodeUrl = true;
				} else {
					Global.encodeUrl = false;
				}

				if(!SettingsHelper.getSyncPause(getApplicationContext()))
					MobileApplication.startSync(this);
				Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);

				if(oidcEnabled) {
					String accessToken = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ACCESS_TOKEN);

					if(MetrixStringHelper.isNullOrEmpty(accessToken))
						intent = MetrixActivityHelper.createActivityIntent(this, "ActivateDeviceOIDC");
				}

				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				return;
			} else {
				if (!MobileGlobal.isDemoBuild(this)) {
					if (oidcSuccess.equalsIgnoreCase("OFF")) {
						Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDevice.class);
						intent.putExtra("person_id", personId);
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
						return;
					} else if (oidcSuccess.equalsIgnoreCase("ON")) {
						Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceOIDC.class);
						intent.putExtra("person_id", personId);
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
						return;
					}
				}
			}
		}
	}

	public void onStart() {
		CheckBox mRememberMe = (CheckBox) findViewById(R.id.rememberMe);

		AndroidResourceHelper.setResourceValues(this, mPersonId, "PersonId", true);
		AndroidResourceHelper.setResourceValues(mPassword, "Password", true);
		AndroidResourceHelper.setResourceValues(mShowPassword, "ShowPassword");
		AndroidResourceHelper.setResourceValues(mRememberMe, "RememberMe");
		AndroidResourceHelper.setResourceValues(mLogin, "SignIn");
		AndroidResourceHelper.setResourceValues(mChangePassword, "ChangePassword");

		super.onStart();

		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
	}

	@SuppressWarnings("deprecation")
	public void onDestroy() {
		View v = findViewById(R.id.login_block);
		if (v != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				v.setBackgroundDrawable(null);
			} else {
				v.setBackground(null);
			}
		}

		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public static void updateDemoTasks(String personId) {
		ArrayList<Hashtable<String, String>> tasks = MetrixDatabaseManager.getFieldStringValuesList("select task_id, plan_start_dttm, plan_end_dttm from task where person_id = '" + personId + "' and task_status in (select task_status from task_status where status = 'OP' and task_status <> '" + MobileApplication.getAppParam("REJECTED_TASK_STATUS") + "')");
		int i = -1;
		for (Hashtable<String, String> task : tasks) {
			ArrayList<DataField> fields = new ArrayList<DataField>();

			Calendar plannedStart = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, task.get("plan_start_dttm"), ISO8601.Yes);
			Calendar plannedEnd = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, task.get("plan_end_dttm"), ISO8601.Yes);
			Calendar currentDate = Calendar.getInstance();

			plannedStart.set(Calendar.YEAR, currentDate.get(Calendar.YEAR));
			plannedStart.set(Calendar.MONTH, currentDate.get(Calendar.MONTH));
			plannedStart.set(Calendar.DAY_OF_MONTH, currentDate.get(Calendar.DAY_OF_MONTH));
			plannedStart.add(Calendar.DAY_OF_MONTH, i);

			plannedEnd.set(Calendar.YEAR, currentDate.get(Calendar.YEAR));
			plannedEnd.set(Calendar.MONTH, currentDate.get(Calendar.MONTH));
			plannedEnd.set(Calendar.DAY_OF_MONTH, currentDate.get(Calendar.DAY_OF_MONTH));
			plannedEnd.add(Calendar.DAY_OF_MONTH, i);

			fields.add(new DataField("plan_start_dttm", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, plannedStart.get(Calendar.YEAR), plannedStart.get(Calendar.MONTH), plannedStart.get(Calendar.DAY_OF_MONTH), plannedStart.get(Calendar.HOUR_OF_DAY), plannedStart.get(Calendar.MINUTE), plannedStart.get(Calendar.SECOND)))));
			fields.add(new DataField("plan_end_dttm", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, plannedEnd.get(Calendar.YEAR), plannedEnd.get(Calendar.MONTH), plannedEnd.get(Calendar.DAY_OF_MONTH), plannedEnd.get(Calendar.HOUR_OF_DAY), plannedEnd.get(Calendar.MINUTE), plannedEnd.get(Calendar.SECOND)))));
			if (i < 1) {
				i = i + 1;
			}
			MetrixDatabaseManager.updateRow("task", fields, "task_id = " + task.get("task_id"));
		}
	}

	public static void updateDemoTimeReporting() {
		MetrixDatabaseManager.executeSql("Delete from work_cal_time_person_view");

		Calendar theDay = Calendar.getInstance(); // initiate as today
		int currentMonth = theDay.get(Calendar.MONTH);
		int currentMonthYear = theDay.get(Calendar.YEAR);
		int currentMonthDays = theDay.getActualMaximum(Calendar.DAY_OF_MONTH);
		theDay.add(Calendar.MONTH, -1);
		int lastMonth = theDay.get(Calendar.MONTH);
		int lastMonthYear = theDay.get(Calendar.YEAR);
		int lastMonthDays = theDay.getActualMaximum(Calendar.DAY_OF_MONTH);
		theDay.add(Calendar.MONTH, 2);
		int nextMonth = theDay.get(Calendar.MONTH);
		int nextMonthYear = theDay.get(Calendar.YEAR);
		int nextMonthDays = theDay.getActualMaximum(Calendar.DAY_OF_MONTH);

		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, lastMonth);
		cal.set(Calendar.YEAR, lastMonthYear);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		for(int k=0; k<lastMonthDays; k++) {
			ArrayList<DataField> fields = new ArrayList<DataField>();
			if(k != 0)
				cal.add(Calendar.DATE, 1);
			DataField work_dt = new DataField("work_dt", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.formatDate(lastMonthYear, lastMonth, cal.get(Calendar.DATE)), MetrixDateTimeHelper.DATE_FORMAT));
			DataField calendar_id= new DataField("calendar_id", "STD");
			DataField day_of_week = new DataField("day_of_week", cal.get(Calendar.DAY_OF_WEEK));
			DataField first_name = new DataField("first_name", "Fred");
			DataField last_name = new DataField("last_name", "Taylor");

			String work_day_string = getDayString(cal);
			if (MetrixStringHelper.valueIsEqual(work_day_string, getDayOfWeekTranslation("SaturdayCaps")) || MetrixStringHelper.valueIsEqual(work_day_string, getDayOfWeekTranslation("SundayCaps"))) {
				fields.add(new DataField("max_work_hours", 0));
			} else {
				fields.add(new DataField("max_work_hours", 8));
			}

			fields.add(new DataField("work_day", getDayString(cal)));
			fields.add(new DataField("total_time", 0));
			fields.add(new DataField("person_id", "TECH01"));
			fields.add(work_dt);
			fields.add(calendar_id);
			fields.add(day_of_week);
			fields.add(first_name);
			fields.add(last_name);

			MetrixDatabaseManager.insertRow("work_cal_time_person_view", fields);
		}

		cal = new GregorianCalendar();
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, currentMonth);
		cal.set(Calendar.YEAR, currentMonthYear);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		for(int k=0; k<currentMonthDays; k++) {
			ArrayList<DataField> fields = new ArrayList<DataField>();
			if(k != 0)
				cal.add(Calendar.DATE, 1);
			DataField work_dt = new DataField("work_dt", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.formatDate(currentMonthYear, currentMonth, cal.get(Calendar.DATE)), MetrixDateTimeHelper.DATE_FORMAT));
			DataField calendar_id= new DataField("calendar_id", "STD");
			DataField day_of_week = new DataField("day_of_week", cal.get(Calendar.DAY_OF_WEEK));
			DataField first_name = new DataField("first_name", "Fred");
			DataField last_name = new DataField("last_name", "Taylor");

			String work_day_string = getDayString(cal);
			if (MetrixStringHelper.valueIsEqual(work_day_string, getDayOfWeekTranslation("SaturdayCaps")) || MetrixStringHelper.valueIsEqual(work_day_string, getDayOfWeekTranslation("SundayCaps"))) {
				fields.add(new DataField("max_work_hours", 0));
			} else {
				fields.add(new DataField("max_work_hours", 8));
			}

			fields.add(new DataField("work_day", getDayString(cal)));
			fields.add(new DataField("total_time", 0));
			fields.add(new DataField("person_id", "TECH01"));
			fields.add(work_dt);
			fields.add(calendar_id);
			fields.add(day_of_week);
			fields.add(first_name);
			fields.add(last_name);

			MetrixDatabaseManager.insertRow("work_cal_time_person_view", fields);
		}

		cal = new GregorianCalendar();
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, nextMonth);
		cal.set(Calendar.YEAR, nextMonthYear);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		for(int k=0; k<nextMonthDays; k++) {
			ArrayList<DataField> fields = new ArrayList<DataField>();
			if(k != 0)
				cal.add(Calendar.DATE, 1);
			DataField work_dt = new DataField("work_dt", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.formatDate(nextMonthYear, nextMonth, cal.get(Calendar.DATE)), MetrixDateTimeHelper.DATE_FORMAT));
			DataField calendar_id= new DataField("calendar_id", "STD");
			DataField day_of_week = new DataField("day_of_week", cal.get(Calendar.DAY_OF_WEEK));
			DataField first_name = new DataField("first_name", "Fred");
			DataField last_name = new DataField("last_name", "Taylor");

			String work_day_string = getDayString(cal);
			if (MetrixStringHelper.valueIsEqual(work_day_string, getDayOfWeekTranslation("SaturdayCaps")) || MetrixStringHelper.valueIsEqual(work_day_string, getDayOfWeekTranslation("SundayCaps"))) {
				fields.add(new DataField("max_work_hours", 0));
			} else {
				fields.add(new DataField("max_work_hours", 8));
			}

			fields.add(new DataField("work_day", getDayString(cal)));
			fields.add(new DataField("total_time", 0));
			fields.add(new DataField("person_id", "TECH01"));
			fields.add(work_dt);
			fields.add(calendar_id);
			fields.add(day_of_week);
			fields.add(first_name);
			fields.add(last_name);

			MetrixDatabaseManager.insertRow("work_cal_time_person_view", fields);
		}
	}

	public static String getDayString(Calendar cal) {
		switch (cal.get(Calendar.DAY_OF_WEEK)) {
			case Calendar.SUNDAY:
				return getDayOfWeekTranslation("SundayCaps");
			case Calendar.MONDAY:
				return getDayOfWeekTranslation("MondayCaps");
			case Calendar.TUESDAY:
				return getDayOfWeekTranslation("TuesdayCaps");
			case Calendar.WEDNESDAY:
				return getDayOfWeekTranslation("WednesdayCaps");
			case Calendar.THURSDAY:
				return getDayOfWeekTranslation("ThursdayCaps");
			case Calendar.FRIDAY:
				return getDayOfWeekTranslation("FridayCaps");
			case Calendar.SATURDAY:
				return getDayOfWeekTranslation("SaturdayCaps");
			default:
				return "";
		}
	}

	private static String getDayOfWeekTranslation(String key) {
		if(!dayOfWeekStrings.containsKey(key))
			dayOfWeekStrings.put(key, AndroidResourceHelper.getMessage(key));

		return dayOfWeekStrings.get(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.logIn:
				attemptLogin();
				break;
			case R.id.cancel:
				finish();
				break;
			case R.id.changePassword:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, ChangePassword.class);
				EditText txtPerson = (EditText) findViewById(R.id.personId);
				String strPerson = txtPerson.getText().toString();
				intent.putExtra(ChangePassword.PERSON_ID, strPerson);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			case R.id.showPassword:
				if (mShowPassword.isChecked()) {
					mPassword.setTransformationMethod(null);
				} else {
					mPassword.setTransformationMethod(new PasswordTransformationMethod());
				}
				break;
		}
	}

	private void attemptLogin() {
		EditText txtPerson = (EditText) findViewById(R.id.personId);
		EditText txtPass = (EditText) findViewById(R.id.password);

		String strPerson = txtPerson.getText().toString().trim();
		String strPass = txtPass.getText().toString().trim();

		try {
			LoginResult loginResult = MetrixAuthenticationAssistant.performLogin(Login.this, strPerson, strPass, true, true);
			boolean success = handleLoginResult(loginResult, strPerson);
			if (!success)
				return;

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

			if(!SettingsHelper.getSyncPause(getApplicationContext()))
				MobileApplication.startSync(this);
		} catch (SQLException ex) {
			LogManager.getInstance(this).error(ex);
			Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	public void displayMessageDialog(Context context, Activity activity, String errorMessage) {
		final Activity mActivity;
		ArrayList<String> mStatuses = new ArrayList<String>();
		mStatuses.add(errorMessage);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(AndroidResourceHelper.getMessage("PasswordErrorTitle"));

		mActivity = activity;

		builder.setMessage(errorMessage);
		builder.setPositiveButton(AndroidResourceHelper.getMessage("OKButton"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int pos) {
				Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, ChangePassword.class);
				MetrixActivityHelper.startNewActivity(mActivity, intent);
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

	private boolean handleLoginResult(LoginResult result, String personId) {
		switch (result) {
			case REQUIRES_ACTIVATION:
				Toast.makeText(this.getBaseContext(),
						AndroidResourceHelper.getMessage("UserNotAuthForDev"),
						Toast.LENGTH_LONG).show();
				return false;

			case PASSWORD_EXPIRED:
				Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("PasswordExpired"), Toast.LENGTH_LONG).show();
				return false;

			case INVALID_PERSON_OR_PASSWORD:
				Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("InvalidLogin"), Toast.LENGTH_LONG).show();
				return false;

			case SUCCESS:
				LogManager.getInstance().info("Logged in the Application.");
				SettingsHelper.saveRememberMe(this, personId);
				Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				MetrixApplicationAssistant.updateOSVersion(this, personId);
				break;

			default:
				break;
		}

		return true;
	}

	/**
	 * Save the default locale settings for demo user
	 */
	private void setupDefaultLocale() {
		String serverDateTimeFormat = "MM/dd/yyyy hh:mm:ss tt";
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_DATE_TIME_FORMAT,
				JavaDotNetFormatHelper.convertDateTimeFormatFromDotNet(serverDateTimeFormat), false);

		String serverDateFormat = "MM/dd/yyyy";
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_DATE_FORMAT, JavaDotNetFormatHelper.convertDateTimeFormatFromDotNet(serverDateFormat),
				false);

		String serverTimeFormat = "hh:mm:ss tt";
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_TIME_FORMAT, JavaDotNetFormatHelper.convertDateTimeFormatFromDotNet(serverTimeFormat),
				false);

		String serverLocaleCode = "en-US";
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_LOCALE_CODE, serverLocaleCode, false);

		String serverNumericFormat = "#,###.##";
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_NUMERIC_FORMAT, serverNumericFormat, false);

		String serverTimeZoneOffset = "-06:00:00";
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_TIME_ZONE_OFFSET, serverTimeZoneOffset, false);

		String serverTimeZoneId = "Central Standard Time";
		serverTimeZoneId = JavaDotNetFormatHelper.convertTimeZoneIdFromDotNet(serverTimeZoneId);
		SettingsHelper.saveStringSetting(this, SettingsHelper.SERVER_TIME_ZONE_ID, serverTimeZoneId, false);
	}

	private boolean personHasPendingPasswordChange(String personId) {
		String passwordChangeInProgress = SettingsHelper.getStringSetting(this, "SETTING_PASSWORD_UPDATED");
		if (!MetrixStringHelper.isNullOrEmpty(passwordChangeInProgress) && MetrixStringHelper.valueIsEqual(passwordChangeInProgress, "Y"))
			return true;

		return false;
	}

	/***
	 * Set custom/default login image with related skin properties
	 */
	private void setLoginImage() {
		try {
			ImageView loginImageImageView = (ImageView) findViewById(R.id.login_image_id);

			boolean isLandscape = false;
			String loginImageId = null;
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				isLandscape = true;
				loginImageId = SettingsHelper.getStringSetting(this, SettingsHelper.LOGIN_IMAGE_ID_LANDSCAPE);
			} else
				loginImageId = SettingsHelper.getStringSetting(this, SettingsHelper.LOGIN_IMAGE_ID_PORTRAIT);

			int requiredWidth;
			int requiredHeight;
			WindowManager w = getWindowManager();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				Point size = new Point();
				w.getDefaultDisplay().getSize(size);
				requiredWidth = size.x;
				requiredHeight = size.y;
			} else {
				Display d = w.getDefaultDisplay();
				requiredWidth = d.getWidth();
				requiredHeight = d.getHeight();
			}

			if (MetrixStringHelper.isNullOrEmpty(loginImageId)) {
				Bitmap defaultLoginBitmap = isLandscape ? BitmapFactory.decodeResource(getResources(), R.drawable.sign_in_landscape) : BitmapFactory.decodeResource(getResources(), R.drawable.sign_in_portrait);
				defaultLoginBitmap = MetrixAttachmentHelper.resizeBitmap(defaultLoginBitmap, requiredHeight, requiredWidth);
				loginImageImageView.setImageBitmap(defaultLoginBitmap);
			} else
				MetrixAttachmentHelper.applyImageWithPixelScale(loginImageId, loginImageImageView, requiredHeight, requiredWidth);

			String tfgColor = SettingsHelper.getStringSetting(this, SettingsHelper.TOP_FIRST_GRADIENT_COLOR);
			String bfgColor = SettingsHelper.getStringSetting(this, SettingsHelper.BOTTOM_FIRST_GRADIENT_COLOR);
			String fgtColor = SettingsHelper.getStringSetting(this, SettingsHelper.FIRST_GRADIENT_TEXT_COLOR);

			float scale = getResources().getDisplayMetrics().density;
			float btnCornerRadius = 4f * scale + 0.5f;

			metrixUIHelper.setFirstGradientColorsForButton(mLogin, tfgColor, bfgColor, fgtColor, btnCornerRadius);
			metrixUIHelper.setFirstGradientColorsForButton(mChangePassword, tfgColor, bfgColor, fgtColor, btnCornerRadius);

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}
}
