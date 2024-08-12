package com.metrix.metrixmobile.system;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPasswordHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixSecurityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant.LoginResult;
import com.metrix.metrixmobile.global.MobileGlobal;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivateDevice extends Activity implements View.OnClickListener {
	private ViewGroup mLayout;
	private Button activateButton;
	private Context mCurrentContext;
	private Boolean welcomeDisplayed = null;
	private static ProgressDialog progressDialog;
	CheckBox mShowPassword;
	EditText mPersonId, mPassword;
	ImageView mLoginIcon;
	private TextView oidcButton, changePasswordButton, reenterButton;
	EditText mServiceUrl;
	private Drawable originalBackground;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activate_device);

		mCurrentContext = this;
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mLoginIcon = (ImageView) findViewById(R.id.login_icon);
		mPersonId = (EditText) findViewById(R.id.personId);
		mPersonId.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

		originalBackground = mPersonId.getBackground();

		activateButton = (Button) findViewById(R.id.activate);
		activateButton.setOnClickListener(this);
		oidcButton = (TextView) findViewById(R.id.linkOidc);
		oidcButton.setMovementMethod(LinkMovementMethod.getInstance());
		oidcButton.setOnClickListener(this);

		changePasswordButton = (TextView) findViewById(R.id.linkChangePassword);
		changePasswordButton.setMovementMethod(LinkMovementMethod.getInstance());
		changePasswordButton.setOnClickListener(this);
		changePasswordButton.setVisibility(View.GONE);

		reenterButton = (TextView) findViewById(R.id.linkEntry);
		reenterButton.setMovementMethod(LinkMovementMethod.getInstance());
		reenterButton.setOnClickListener(this);
		String serviceUrl = SettingsHelper.getServiceAddress(this);
		if (MetrixStringHelper.isNullOrEmpty(serviceUrl)) {
			serviceUrl = "http://";
		}

		try {
			if(MetrixStringHelper.isNullOrEmpty(serviceUrl))
				MetrixControlAssistant.setValue(R.id.serviceUrl, mLayout, MetrixApplicationAssistant.getMetaStringValue(this, "DefaultServiceUrl"));
			else
				MetrixControlAssistant.setValue(R.id.serviceUrl, mLayout, serviceUrl);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
		configureAppParams();
		mServiceUrl = (EditText) findViewById(R.id.serviceUrl);

		mPassword = (EditText) findViewById(R.id.password);
		mShowPassword = (CheckBox) findViewById(R.id.showPassword);
		mShowPassword.setOnClickListener(this);
		mShowPassword.setChecked(false);

		mPassword.setTypeface(Typeface.DEFAULT);
		mPersonId.setTypeface(Typeface.DEFAULT);
		mPassword.setTextSize(18);
		mPersonId.setTextSize(18);
	}

	public void onStart() {
		super.onStart();
		applyTheme();

		AndroidResourceHelper.setResourceValues(mPersonId, "PersonId", true);
		AndroidResourceHelper.setResourceValues(mPassword, "Password", true);
		AndroidResourceHelper.setResourceValues(mShowPassword, "ShowPassword");
		AndroidResourceHelper.setResourceValues(oidcButton, "UseSSO");
		AndroidResourceHelper.setResourceValues(changePasswordButton, "ChangePassword");
		AndroidResourceHelper.setResourceValues(reenterButton, "ReEnterUrl");
		AndroidResourceHelper.setResourceValues(activateButton, "Activate");

		verifyConfig();

		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
	}

	@SuppressWarnings("deprecation")
	protected void onDestroy() {
		View v = findViewById(R.id.table_layout);
		if (v != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				v.setBackgroundDrawable(null);
			} else {
				v.setBackground(null);
			}
		}

		super.onDestroy();

		if (progressDialog != null)
			this.progressDialog.dismiss();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return welcomeDisplayed;
	}

	private void verifyConfig() {
		String authMethods = SettingsHelper.getStringSetting(this, SettingsHelper.AUTHENTICATION_METHODS);
		String userId = SettingsHelper.getActivatedUser(this);
		String oidcEnabled = SettingsHelper.getStringSetting(this,SettingsHelper.OIDC_ENABLED);
		String oidcInvalid = SettingsHelper.getStringSetting(this,"OIDC_INVALID");

		if (!MetrixStringHelper.isNullOrEmpty(userId)) {
			mPersonId.setText(userId);
			mPersonId.setKeyListener(null);
			mPassword.requestFocus();
			oidcButton.setVisibility(View.GONE);
			changePasswordButton.setVisibility(View.VISIBLE);
			reenterButton.setVisibility(View.GONE);
			activateButton.setText(AndroidResourceHelper.getMessage("Login"));
		} else if (!authMethods.toUpperCase().contains("OIDC") || oidcInvalid.compareToIgnoreCase("True") == 0) {
			oidcButton.setVisibility(View.GONE);

			// Display error dialog only if authentication method contains OIDC option and OIDC is invalid
			if(oidcInvalid.compareToIgnoreCase("True") == 0) {
				MetrixUIHelper.showErrorDialogOnGuiThread(this, AndroidResourceHelper.getMessage("FetchOIDCSettingsFailure"));
			}
		} else if (!MetrixStringHelper.isNullOrEmpty(userId) && !MetrixStringHelper.valueIsEqual(oidcEnabled,"ON")){
			oidcButton.setVisibility(View.GONE);
		}
	}

	private void applyTheme() {
		try {
			String userId = SettingsHelper.getActivatedUser(this);
			if (!MetrixStringHelper.isNullOrEmpty(userId)) {
				// We need active adapters cached for metadata retrieval to work.
				// If we cannot get a handle on adapters successfully, then fail with a log entry and carry on.
				MetrixDatabaseManager.createDatabaseAdapters(MobileApplication.getAppContext(), MetrixApplicationAssistant.getMetaIntValue(MobileApplication.getAppContext(), "DatabaseVersion"), com.metrix.metrixmobile.R.array.system_tables,
						com.metrix.metrixmobile.R.array.business_tables);

				// Only apply theming to this screen if we already have an activated user
				String largeIconImageID = MetrixSkinManager.getLargeIconImageID();
				if (!MetrixStringHelper.isNullOrEmpty(largeIconImageID))
					MetrixAttachmentHelper.applyImageWithDPScale(largeIconImageID, mLoginIcon, 96, 96);

				String primaryColorString = MetrixSkinManager.getPrimaryColor();
				String hyperlinkColorString = MetrixSkinManager.getHyperlinkColor();
				int hyperlinkColor = Color.parseColor(hyperlinkColorString);
				MetrixActivity.setMaterialDesignForButtons(activateButton, primaryColorString, this);
				oidcButton.setTextColor(hyperlinkColor);
				changePasswordButton.setTextColor(hyperlinkColor);
				reenterButton.setTextColor(hyperlinkColor);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void configureAppParams() {

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
	}

	private void displayWelcome() {
		@SuppressWarnings("deprecation") final Object displayed = getLastNonConfigurationInstance();

		if (displayed == null) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, Welcome.class);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if ((Boolean) displayed == false) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, Welcome.class);
			MetrixActivityHelper.startNewActivity(this, intent);
		}

		welcomeDisplayed = true;
	}

	private void setAuthenticationFields(boolean enabled) {
		mPersonId.setEnabled(enabled);
		mPassword.setEnabled(enabled);
		activateButton.setEnabled(enabled);
		if (!enabled) {
			mPersonId.setBackgroundColor(Color.LTGRAY);
			mPassword.setBackgroundColor(Color.LTGRAY);
		} else {
			mPersonId.setBackgroundDrawable(originalBackground);
			mPassword.setBackgroundDrawable(originalBackground);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.activate:
				final String personId = MetrixControlAssistant.getValue(R.id.personId, mLayout).trim();
				final String password = MetrixControlAssistant.getValue(R.id.password, mLayout).trim();
				final String serviceUrl = SettingsHelper.getServiceAddress(this);

				String currentUser = SettingsHelper.getActivatedUser(this);
				if(MetrixStringHelper.isNullOrEmpty(currentUser)){
					progressDialog = ProgressDialog.show(this, AndroidResourceHelper.getMessage("Activating"),
							AndroidResourceHelper.getMessage("ActivationWait"), true, false);
					new Thread(() -> activateDevice(progressDialog, personId, password, serviceUrl, false)).start();
				} else {
					new Thread(() -> {
						try {
							LoginResult loginResult;
							String passwordChangeInProgress = SettingsHelper.getStringSetting(MobileApplication.getAppContext(), "SETTING_PASSWORD_UPDATED");
							if (!MetrixStringHelper.isNullOrEmpty(passwordChangeInProgress) && MetrixStringHelper.valueIsEqual(passwordChangeInProgress, "Y")) {
								// Change password and login
								// Creating the Database Adapter in cases where the change password happens server side and the use closes/kills the app before next login
								MetrixDatabaseManager.createDatabaseAdapters(ActivateDevice.this.getApplicationContext(), MobileGlobal.getDatabaseVersion(ActivateDevice.this.getApplicationContext()), R.array.system_tables,
										R.array.business_tables);
								String hashedServerPass = SettingsHelper.getStringSetting(this, SettingsHelper.PCHANGE_SIMPLEHASH);

								loginResult = verifyPasswordChange(hashedServerPass, password);
								if(loginResult == LoginResult.SUCCESS) {
									MetrixAuthenticationAssistant.resetServerPasswordChanged(personId, hashedServerPass);
								}
							} else {
								// regular login
								loginResult = MetrixAuthenticationAssistant.performLogin(ActivateDevice.this, personId, password, true, false);
							}

							if (loginResult == LoginResult.LICENSING_VIOLATION)
								throw new Exception((AndroidResourceHelper.getMessage("LicensingViolation")));
							else if (loginResult == LoginResult.INVALID_PERSON_OR_PASSWORD)
								throw new Exception(AndroidResourceHelper.getMessage("InvalidUserPass"));
							else if (loginResult == LoginResult.REQUIRES_ACTIVATION)
								throw new Exception(AndroidResourceHelper.getMessage("NetworkProblem"));
							else if (loginResult != LoginResult.SUCCESS)
								throw new Exception(AndroidResourceHelper.getMessage("UnknownActivationError"));

							SettingsHelper.saveBooleanSetting(this, SettingsHelper.FROM_LOGOUT, false);
							MetrixAuthenticationAssistant.handleLoginSuccess(personId);
							MetrixActivityHelper.hideKeyboard(this);

							Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);
							MetrixActivityHelper.startNewActivityAndFinish(this, intent);

						} catch (Exception ex) {
							LogManager.getInstance().error(ex);
							MetrixUIHelper.showErrorDialogOnGuiThread(this, ex.getMessage());
						}
					}).start();
				}

				break;
			case R.id.linkEntry:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceEntry.class);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				break;
			case R.id.linkOidc:
				intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceOIDC.class);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			case R.id.showPassword:
				if (mShowPassword.isChecked()) {
					mPassword.setTransformationMethod(null);
				} else {
					mPassword.setTransformationMethod(new PasswordTransformationMethod());
				}
				break;
			case R.id.linkChangePassword:
                intent = MetrixActivityHelper.createActivityIntent(this, ChangePassword.class);
                String strPerson = SettingsHelper.getRememberMe(this);
                intent.putExtra(ChangePassword.PERSON_ID, strPerson);
                MetrixActivityHelper.startNewActivity(this, intent);
				break;
		}
	}

	private LoginResult verifyPasswordChange(String serverHashedPass, String newPass) {
		String hashedNewPass = MetrixSecurityHelper.HashPassword(newPass);

		if(hashedNewPass.equals(serverHashedPass))
			return LoginResult.SUCCESS;
		else
			return LoginResult.INVALID_PERSON_OR_PASSWORD;
	}

	private boolean personHasPendingPasswordChange(String personId) {
		String passwordChangeInProgress = SettingsHelper.getStringSetting(this, "SETTING_PASSWORD_UPDATED");
		if (!MetrixStringHelper.isNullOrEmpty(passwordChangeInProgress) && MetrixStringHelper.valueIsEqual(passwordChangeInProgress, "Y"))
			return true;

		return false;
	}

	/**
	 * @param progressDialog
	 * @param personId
	 * @param password
	 * @param serviceUrl
	 */
	private void activateDevice(ProgressDialog progressDialog, String personId, String password, String serviceUrl, boolean hashed) {
		try {
			LoginResult loginResult = MetrixAuthenticationAssistant.activateDevice(this, serviceUrl, personId, password, false);

			if (loginResult == LoginResult.LICENSING_VIOLATION)
				throw new Exception((AndroidResourceHelper.getMessage("LicensingViolation")));
			else if (loginResult == LoginResult.INVALID_PERSON_OR_PASSWORD)
				throw new Exception(AndroidResourceHelper.getMessage("InvalidUserPass"));
			else if (loginResult == MetrixAuthenticationAssistant.LoginResult.PASSWORD_EXPIRED)
				throw new Exception(AndroidResourceHelper.getMessage("PasswordExpired"));
			else if (loginResult == LoginResult.REQUIRES_ACTIVATION)
				throw new Exception(AndroidResourceHelper.getMessage("NetworkProblem"));
			else if (loginResult != LoginResult.SUCCESS)
				throw new Exception(AndroidResourceHelper.getMessage("UnknownActivationError"));

			if (progressDialog != null)
				progressDialog.dismiss();

			SettingsHelper.saveRememberMe(this, personId);
			LogManager.getInstance().delete();
			LogManager.getInstance().setMaxLogs();

			String hashPassword = "";
			if (password.endsWith("=="))
				hashPassword = password;
			else
				hashPassword = MetrixSecurityHelper.HashPassword(password);
			MetrixDatabaseManager.executeSql(String.format("insert into user_credentials (person_id, password) values ('%1$s', '%2$s')", personId, hashPassword));

			MetrixPublicCache.instance.addItem("person_id", personId);
			SettingsHelper.saveStringSetting(this, SettingsHelper.USER_LOGIN_PASSWORD, hashPassword, true);
			SettingsHelper.saveStringSetting(this, SettingsHelper.OIDC_ENABLED, "OFF", true);

			MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());

			SettingsHelper.saveBooleanSetting(this, SettingsHelper.FROM_LOGOUT, false);

			Intent intent = MetrixActivityHelper.createActivityIntent(this, SyncServiceMonitor.class);
			intent.putExtra("StartService", true);
			intent.putExtra("StartSync", true);
			intent.putExtra("StartLocation", true);
			intent.putExtra("ShowInitDialog", true);
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
		} catch (Exception e) {
			if (progressDialog != null)
				progressDialog.dismiss();

			SettingsHelper.removeSetting("SERVER_AUTHENTICATE_ERROR_MESSAGE");
			MetrixUIHelper.showErrorDialogOnGuiThread(this, e.getMessage());
		}
	}
}

