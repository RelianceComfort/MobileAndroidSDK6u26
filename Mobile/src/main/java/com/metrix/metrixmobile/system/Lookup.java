package com.metrix.metrixmobile.system;

import android.os.Bundle;

import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.R;

public class Lookup extends LookupBase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lookup);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.row_count, "RowCount"));
		super.onStart();
	}
}
