package com.metrix.metrixmobile.system;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant.LoginResult;

import android.app.Activity;
import android.content.Intent;
import android.database.SQLException;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginAfterPassChange extends Activity implements View.OnClickListener {
	private EditText mOldPassword, mNewPassword, mConfirmNewPassword;
	private Button mLogin;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.login_after_pass_change);	
		
		mOldPassword = (EditText) findViewById(R.id.oldPassword);
		mNewPassword = (EditText) findViewById(R.id.newPassword);
		mConfirmNewPassword = (EditText) findViewById(R.id.confirmNewPassword);
		
		mLogin = (Button) findViewById(R.id.logIn);
		mLogin.setOnClickListener(this);

//		TextView mLgnAfterPwChng = (TextView) findViewById(R.id.login_after_pass_change);

//		AndroidResourceHelper.setResourceValues(this, mLgnAfterPwChng, "LoginAfterPassChange");
		AndroidResourceHelper.setResourceValues(mLogin, "SignIn");
		AndroidResourceHelper.setResourceValues(mOldPassword, "OldPassword", true);
		AndroidResourceHelper.setResourceValues(mNewPassword, "NewPassword", true);
		AndroidResourceHelper.setResourceValues(mConfirmNewPassword, "ConfirmPassword", true);
	}
	
	public void onStart() {
		super.onStart();
		
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
		
		// if not passwordChangeInProgress, go back to Login screen
		String passwordChangeInProgress = SettingsHelper.getStringSetting(this, "SETTING_PASSWORD_UPDATED");
		if (!(!MetrixStringHelper.isNullOrEmpty(passwordChangeInProgress) && MetrixStringHelper.valueIsEqual(passwordChangeInProgress, "Y"))) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, Login.class);
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
		}
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
		}
	}

	private void attemptLogin() {
		final String strPerson = getIntent().getStringExtra("person_id");
		final String strOldPass = mOldPassword.getText().toString().trim();
		final String strNewPass = mNewPassword.getText().toString().trim();
		final String strConfirmPass = mConfirmNewPassword.getText().toString().trim();
		
		if (MetrixStringHelper.isNullOrEmpty(strOldPass)) {
			Toast.makeText(this, AndroidResourceHelper.getMessage("EnterOldPassword"), Toast.LENGTH_LONG).show();
			return;
		}
		
		if (MetrixStringHelper.isNullOrEmpty(strNewPass)) {
			Toast.makeText(this, AndroidResourceHelper.getMessage("EnterNewPassword"), Toast.LENGTH_LONG).show();
			return;
		}

		if (MetrixStringHelper.valueIsEqual(strNewPass, strConfirmPass)) {
			Toast.makeText(this, AndroidResourceHelper.getMessage("NewPasswordDoesNotMatch"), Toast.LENGTH_LONG).show();
			return;
		}

		try {
			new Thread(() -> {
				mUIHelper = new MetrixUIHelper(LoginAfterPassChange.this);
				try {
					mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("PasswordVerificationInProgress"));
					LoginResult loginResult = MetrixAuthenticationAssistant.performLogin(LoginAfterPassChange.this, strPerson, strNewPass, true, true, strOldPass,true);
					handleLoginResult(loginResult, strPerson);

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

					MobileApplication.startSync(MobileApplication.getAppContext());
				} catch (Exception e) {
					LogManager.getInstance().error(e);
					final String exMsg = e.getMessage();
					LoginAfterPassChange.this.runOnUiThread(() -> Toast.makeText(LoginAfterPassChange.this, exMsg, Toast.LENGTH_LONG).show());
				} finally {
					mUIHelper.dismissLoadingDialog();
				}
			}).start();
		} catch (SQLException ex) {
			LogManager.getInstance(this).error(ex);
			Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void handleLoginResult(LoginResult result, String personId) {
		switch (result) {
		case REQUIRES_ACTIVATION:
			this.runOnUiThread(() -> Toast.makeText(LoginAfterPassChange.this.getBaseContext(),
					AndroidResourceHelper.getMessage("UserNotAuthForDev"),
					Toast.LENGTH_LONG).show());
			return;

		case PASSWORD_EXPIRED:
			this.runOnUiThread(() -> Toast.makeText(LoginAfterPassChange.this.getBaseContext(), AndroidResourceHelper.getMessage("PasswordExpired"), Toast.LENGTH_LONG).show());
			return;

		case INVALID_PERSON_OR_PASSWORD:
			this.runOnUiThread(() -> Toast.makeText(LoginAfterPassChange.this.getBaseContext(), AndroidResourceHelper.getMessage("PasswordNotVerified"), Toast.LENGTH_LONG).show());
			return;

		case SUCCESS:		
			LogManager.getInstance().info("Logged in the Application.");
			SettingsHelper.saveRememberMe(this, personId);
			MetrixActivityHelper.hideKeyboard(this);
			Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			break;

		default:
			break;
		}
	}
}
