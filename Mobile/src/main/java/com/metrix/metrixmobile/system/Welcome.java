package com.metrix.metrixmobile.system;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Welcome extends Activity implements View.OnClickListener {
	Button mContinueButton;
	TextView mWelcomeMessage;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
		
		setListeners();

		if (SettingsHelper.getInitStatus(this)) {
			mWelcomeMessage.setText(AndroidResourceHelper.getMessage("WelcomeIncompleteActivation"));
		} else {
			mWelcomeMessage.setText(AndroidResourceHelper.getMessage("Welcome"));
		}
		AndroidResourceHelper.setResourceValues(mContinueButton, "Continue");
	}
	
	private void setListeners() {
		mContinueButton = (Button) findViewById(R.id.continueButton);
		mWelcomeMessage = (TextView) findViewById(R.id.welcome_message);

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
