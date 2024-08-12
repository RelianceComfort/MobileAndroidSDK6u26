package com.metrix.metrixmobile.system;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.metrixmobile.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DemoWelcome extends Activity implements View.OnClickListener {
	Button mContinueButton;
	TextView mWelcomeMessage, mSalesPhone, mEmail, mWeb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_welcome);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		setListeners();

		AndroidResourceHelper.setResourceValues(mWelcomeMessage, "DemoWelcome");
		AndroidResourceHelper.setResourceValues(mContinueButton, "Continue");
		AndroidResourceHelper.setResourceValues(mSalesPhone, "phone_800_543_2130");
		AndroidResourceHelper.setResourceValues(mEmail, "email_sales_metrix_com");
		AndroidResourceHelper.setResourceValues(mWeb, "web_www_metrix_com");
	}
	
	private void setListeners() {
		mContinueButton = (Button) findViewById(R.id.continueButton);
		mWelcomeMessage = (TextView) findViewById(R.id.welcome_message);
		mSalesPhone = (TextView) findViewById(R.id.metrix_sales_phone);
		mEmail = (TextView) findViewById(R.id.metrix_sales_email);
		mWeb = (TextView) findViewById(R.id.metrix_web_address);

		mContinueButton.setOnClickListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.continueButton:
			finish();
			break;
		}
	}
}
