package com.metrix.metrixmobile.system;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FullTextEdit extends Activity implements View.OnClickListener {
	private String mTableName = "";
	private String mColumnName = "";
	private String mColumnValue = "";
	private String mFriendlyName = "";
	
	Button saveButton, cancelButton;
	TextView textLabel;
	EditText detailEditText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Remove title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.full_text);
		
		mTableName = getIntent().getStringExtra("table_name");
		mColumnName = getIntent().getStringExtra("column_name");
		mColumnValue = getIntent().getStringExtra("column_value");
		mFriendlyName = getIntent().getStringExtra("friendly_name");		
		
		textLabel = (TextView) findViewById(R.id.contentLabel);
		textLabel.setText(mFriendlyName);
		
		detailEditText = (EditText) findViewById(R.id.fullText);
		detailEditText.setText(mColumnValue);
		
		saveButton = (Button) findViewById(R.id.ok);
		cancelButton = (Button) findViewById(R.id.cancel);
		
		saveButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
	}


	public void onStart() {
		List<ResourceValueObject> resourceStrings = new ArrayList<ResourceValueObject>();
		resourceStrings.add(new ResourceValueObject(R.id.ok, "OK"));
		resourceStrings.add(new ResourceValueObject(R.id.cancel, "Cancel"));
		try {
			AndroidResourceHelper.setResourceValues(this, resourceStrings);
		} catch (Exception e) {
		}
		super.onStart();
	}

	@Override
	public void onClick(View v) {		

		switch (v.getId()) {
		case R.id.ok:
			String resultText = detailEditText.getText().toString();
			getIntent().putExtra("table_name", mTableName);
			getIntent().putExtra("column_name", mColumnName);
			getIntent().putExtra("column_value", resultText);
			
			setResult(RESULT_OK, getIntent());
			finish();
			
			break;
		case R.id.cancel:
			finish();
			break;
		}
	}
}
