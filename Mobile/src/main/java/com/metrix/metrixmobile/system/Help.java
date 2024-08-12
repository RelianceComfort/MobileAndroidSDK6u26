package com.metrix.metrixmobile.system;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Help extends Activity implements View.OnClickListener {
	Button mContinueButton;
	TextView mText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		List<ResourceValueObject> resourceStrings = new ArrayList<ResourceValueObject>();
		resourceStrings.add(new ResourceValueObject(R.id.textView1, "ScreenInfoHelp"));
		resourceStrings.add(new ResourceValueObject(R.id.continueButton, "Close"));
		try {
			AndroidResourceHelper.setResourceValues(this, resourceStrings);
			setTitle(AndroidResourceHelper.getMessage("MetrixHelp"));
		} catch (Exception e) {
		}


		super.onStart();
		setListeners();
		
		String helpText = getIntent().getStringExtra("help_text");
		if (!MetrixStringHelper.isNullOrEmpty(helpText)){
			mText = (TextView) findViewById(R.id.textView1);
			mText.setText(helpText);
		}
	}
	
	private void setListeners() {
		mContinueButton = (Button) findViewById(R.id.continueButton);
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
