package com.metrix.metrixmobile.system;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixSecurityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant.LoginResult;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.policies.PasswordPolicy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.IntentCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePassword extends Activity implements View.OnClickListener {
	public final static String PERSON_ID = "PERSON_ID";
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);

	Button saveButton, cancelButton;
	EditText personIdEditText, oldPasswordEditText, newPasswordEditText, confirmPasswordEditText;

	private String personId;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.change_password);

		saveButton = (Button) findViewById(R.id.save);
		cancelButton = (Button) findViewById(R.id.cancel);

		saveButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		personId = getIntent().getStringExtra(PERSON_ID);
	}

	public void onStart() {
		oldPasswordEditText = (EditText) findViewById(R.id.oldPassword);
		newPasswordEditText = (EditText) findViewById(R.id.newPassword);
		confirmPasswordEditText = (EditText) findViewById(R.id.confirmPassword);

		AndroidResourceHelper.setResourceValues(this, saveButton, "Save");
		AndroidResourceHelper.setResourceValues(this, cancelButton, "Cancel");
		AndroidResourceHelper.setResourceValues(this, oldPasswordEditText, "OldPassword", true);
		AndroidResourceHelper.setResourceValues(this, newPasswordEditText, "NewPassword", true);
		AndroidResourceHelper.setResourceValues(this, confirmPasswordEditText, "ConfirmPassword", true);
		super.onStart();
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
	}

	@Override
	public void onClick(View v) {


		switch (v.getId()) {
			case R.id.save:
				String oldPassword = oldPasswordEditText.getText().toString();
				if (MetrixStringHelper.isNullOrEmpty(oldPassword)) {
					Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("EnterOldPassword"), Toast.LENGTH_LONG).show();
					return;
				} else if (MetrixStringHelper.isNullOrEmpty(newPasswordEditText.getText().toString())) {
					Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("EnterNewPassword"), Toast.LENGTH_LONG).show();
					return;
				} else if (MetrixStringHelper.isNullOrEmpty(confirmPasswordEditText.getText().toString())) {
					Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("ConfirmNewPassword"), Toast.LENGTH_LONG).show();
					return;
				} else if (!newPasswordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {
					Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("PasswordsDoNotMatch"), Toast.LENGTH_LONG).show();
					return;
				}

				// enable DB access pre-login
				String oldEnteredHashPassword = MetrixSecurityHelper.HashPassword(oldPassword);
				String oldUCHashPassword = "";

				if (!MobileApplication.DatabaseLoaded) {
					try {
						// temporarily try to open/use DB to get old hash password from user_credentials
						MetrixDatabaseManager.createDatabaseAdapters(this.getApplicationContext(), MobileGlobal.getDatabaseVersion(this.getApplicationContext()), com.metrix.metrixmobile.R.array.system_tables,
								com.metrix.metrixmobile.R.array.business_tables);

						oldUCHashPassword = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", String.format("person_id = '%s'", personId));

						MetrixDatabaseManager.closeDatabase();
					} catch (Exception ex) {
						LogManager.getInstance(this).error(ex);
					}
				} else {
					// use existing DB connections without affecting them (e.g., if app is still alive)
					oldUCHashPassword = MetrixDatabaseManager.getFieldStringValue("user_credentials", "password", String.format("person_id = '%s'", personId));
				}

				if (!MetrixStringHelper.valueIsEqual(oldEnteredHashPassword, oldUCHashPassword)) {
					Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("OldPasswordInvalid"), Toast.LENGTH_LONG).show();
					return;
				}

				try {
					String result = PasswordPolicy.passwordIsValid(MetrixControlAssistant.getValue(newPasswordEditText));

					if (!MetrixStringHelper.isNullOrEmpty(result)) {
						Toast.makeText(this.getBaseContext(), result, Toast.LENGTH_LONG).show();
						return;
					}
				} catch (Exception e) {
					LogManager.getInstance(this).error(e);
				}

				User.setUser(personId, this);

				try {
					new Thread(() -> {
						LoginResult result = null;
						mUIHelper = new MetrixUIHelper(ChangePassword.this);
						try {
							mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("PasswordChangeInProgress"));
							result = com.metrix.metrixmobile.global.MetrixAuthenticationAssistant.doLoginAndChangePassword(ChangePassword.this, personId, oldPasswordEditText
									.getText().toString(), newPasswordEditText.getText().toString());
							handleLoginResult(result);
						} catch (Exception e) {
							LogManager.getInstance().error(e);
							final String exMsg = e.getMessage();
							ChangePassword.this.runOnUiThread(() -> Toast.makeText(ChangePassword.this, exMsg, Toast.LENGTH_LONG).show());
						} finally {
							mUIHelper.dismissLoadingDialog();
						}
					}).start();
				} catch (Exception ex) {
					LogManager.getInstance(this).error(ex);
					Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.cancel:
				finish();
				break;
		}
	}

	private void handleLoginResult(LoginResult result) {
		switch (result) {
			case REQUIRES_ACTIVATION:
				Toast.makeText(this.getBaseContext(),
						AndroidResourceHelper.getMessage("UserNotAuthForDev"),
						Toast.LENGTH_LONG).show();
				return;

			case INVALID_PERSON_OR_PASSWORD:
				Toast.makeText(this.getBaseContext(), AndroidResourceHelper.getMessage("InvalidLogin"), Toast.LENGTH_LONG).show();
				return;

			case SUCCESS:
				configureAppParams();
				MetrixActivityHelper.hideKeyboard(this);
				Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				break;

			default:
				break;
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
}