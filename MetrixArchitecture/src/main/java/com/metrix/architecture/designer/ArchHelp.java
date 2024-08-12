package com.metrix.architecture.designer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class ArchHelp extends Activity implements OnClickListener {
	MetrixDesignerResourceHelpData mHelpResourceData;
	Button mContinueButton;
	TextView mText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHelpResourceData = (MetrixDesignerResourceHelpData) MetrixPublicCache.instance.getItem("MetrixDesignerHelpResourceData");

		setContentView(mHelpResourceData.LayoutResourceID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		setListeners();
		
		String helpText = getIntent().getStringExtra("help_text");
		if (!MetrixStringHelper.isNullOrEmpty(helpText)){
			mText = (TextView) findViewById(mHelpResourceData.TextViewResourceID);
			mText.setText(helpText);
		}
		setTitle(AndroidResourceHelper.getMessage("MetrixHelp"));
	}
	
	private void setListeners() {
		mContinueButton = (Button) findViewById(mHelpResourceData.ButtonResourceID);
		AndroidResourceHelper.setResourceValues(mContinueButton, "Close");
		mContinueButton.setOnClickListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {		
		int viewId = v.getId();
		if (viewId == mHelpResourceData.ButtonResourceID) {
			finish();
		}
	}
}
