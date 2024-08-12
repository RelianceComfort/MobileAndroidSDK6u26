package com.metrix.metrixmobile;

import com.metrix.metrixmobile.R; // com.metrix.architecture.R;
import com.metrix.metrixmobile.system.MetrixActivity;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.User;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public class Profile extends MetrixActivity implements View.OnClickListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		try {
			String firstName = MetrixControlAssistant.getValue(mFormDef, mLayout, "person", "first_name");
			String lastName = MetrixControlAssistant.getValue(mFormDef, mLayout, "person", "last_name");
			
			try {
				MetrixControlAssistant.setValue(mFormDef,  mLayout, "custom", "full_name", firstName + " " + lastName);
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef personDef = new MetrixTableDef("person", MetrixTransactionTypes.UPDATE);
		personDef.constraints.add(new MetrixConstraintDef("person_id", MetrixConstraintOperands.EQUALS, User.getUser().personId, String.class));

		this.mFormDef = new MetrixFormDef(personDef);
	}
}
