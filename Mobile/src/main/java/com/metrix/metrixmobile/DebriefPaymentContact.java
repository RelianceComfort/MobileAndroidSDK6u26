package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.metrixmobile.system.DebriefActivity;

public class DebriefPaymentContact extends DebriefActivity implements View.OnClickListener{

	private FloatingActionButton mCancelButton, mCustomSaveButton;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.debrief_payments_contact);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
    public void onStart() {
        super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
//        try {
//            AndroidResourceHelper.setResourceValues(findViewById(R.id.custom_save), "Save", false);
//            AndroidResourceHelper.setResourceValues(findViewById(R.id.update), "Save", false);
//            AndroidResourceHelper.setResourceValues(findViewById(R.id.cancel), "Cancel", false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
	
	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef paymentContactDef = null;
		if (this.mActivityDef != null) {
			paymentContactDef = new MetrixTableDef("request_contact", this.mActivityDef.TransactionType);
			if (this.mActivityDef.Keys != null) {
				paymentContactDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
			}
		} else {
			paymentContactDef = new MetrixTableDef("request_contact", MetrixTransactionTypes.INSERT);
		}

		this.mFormDef = new MetrixFormDef(paymentContactDef);
	}
	
	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mCustomSaveButton = (FloatingActionButton) findViewById(R.id.custom_save);
		mCancelButton = (FloatingActionButton) findViewById(R.id.cancel);

		mFABList.add(mCustomSaveButton);
		mFABList.add(mCancelButton);

		mCustomSaveButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);

		fabRunnable = this::showFABs;

		NestedScrollView scrollView = findViewById(R.id.scroll_view);
		mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
		scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
			if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
				fabHandler.removeCallbacks(fabRunnable);
				if(mFABsToShow != null)
					mFABsToShow.clear();
				else
					mFABsToShow = new ArrayList<>();

				hideFABs(mFABList);
				fabHandler.postDelayed(fabRunnable, fabDelay);
			}
		});
	}
	
	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		super.defaultValues();
		
		MetrixControlAssistant.setValue(mFormDef, mLayout, "request_contact", "request_id", MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		MetrixControlAssistant.setValue(mFormDef, mLayout, "request_contact", "place_id", MetrixDatabaseManager.getFieldStringValue("task", "place_id_cust", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		//region : execute the actions are related to save/cancel button
		case R.id.custom_save:
			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");		
			MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, true, "PaymentContact");		
			if (result == MetrixSaveResult.SUCCESSFUL) {
				
				String metrixRowId = null;
				String sequence = null;

				MetrixCursor requestContactCursor = MetrixDatabaseManager.rawQueryMC("select metrix_row_id, sequence, first_name, last_name from request_contact where request_id = " + MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")) + 
						" order by created_dttm desc limit 1", null);
				
				if (requestContactCursor != null && requestContactCursor.moveToFirst()) {
					HashMap<String,String> newContactMap = null;
					
					while (requestContactCursor.isAfterLast() == false) {
						metrixRowId = "RC|" + requestContactCursor.getString(0);
						sequence = requestContactCursor.getString(1);
						break;
					}
					//send the new row id to DebriefPayment for displaying the newly added contact name.
					MetrixCurrentKeysHelper.setKeyValue("request_contact", "metrix_row_id", metrixRowId);
					//for further processing
					MetrixCurrentKeysHelper.setKeyValue("request_contact", "sequence", sequence);

					Intent intent = new Intent(this, DebriefPayment.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				}
				
			}
			break;
			
		case R.id.cancel:
			finish();
			break;
		//end-region : execute the actions are related to save/cancel button	
			
		default:
			super.onClick(v);
		}
	}

}
