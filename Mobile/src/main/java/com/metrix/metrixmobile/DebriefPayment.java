package com.metrix.metrixmobile;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.PaymentHelper;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixIntentService.LocalBinder;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.DebriefActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DebriefPayment extends DebriefActivity implements View.OnClickListener, View.OnFocusChangeListener, MetrixRecyclerViewListener {
	private FloatingActionButton mSaveButton, mNextButton;
	private Button mViewPreviousEntriesButton;
	private Spinner mPaymentCardType, mPaymentMethod, mExpireMonth, mExpireYear;
	private EditText mPaymentCardHolder, mPaymentCardNoMask, mPaymentCardExpired, mPaymentCardCvvMask, mPaymentRef;
	Drawable mPaymentStatusImg;
	private Button mPaymentImg;
	private static boolean mResponseProcessed = false;


	private String mCurrentMessageId;

	// Service Binding related objects
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	protected boolean mIsBound = false;
	protected IPostMonitor service = null;
	protected boolean mInitializationStarted = false;
	protected boolean mHandlingErrors = false;
	protected boolean mGoNext = false;

	private LocalBinder mLocalBinder;
	private MetrixIntentService mSyncService;
	private Context mCurrentContext;
	private static SyncTask mSyncTask;
	private static String NOT_APPLICABLE = AndroidResourceHelper.getMessage("NaCaps");
	private Button mContactAddButton;

	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	private String key;
	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_payments);
		else
			setContentView(R.layout.debrief_payments);

		recyclerView = findViewById(R.id.recyclerView);
		if (recyclerView != null)
			MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

		mCurrentContext = this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.view_previous_entries, "ViewPrevious"));
		//after adding new contact record from DebriefPaymentContact, receives the new row id when navigating back. If not, key is null.
		key = MetrixCurrentKeysHelper.keyExists("request_contact", "metrix_row_id") ? MetrixCurrentKeysHelper.getKeyValue("request_contact", "metrix_row_id") : null;

		//reset the value
		MetrixCurrentKeysHelper.setKeyValue("request_contact", "metrix_row_id", "");

		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		mContactAddButton = (Button) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "add");
		if(mContactAddButton != null)
			mContactAddButton.setOnClickListener(addContact);

		setCreditCardLayoutsState();
		this.mGoNext = false;

		restoreCurrentPaymentControlValues();

		setNewContact();
	}

	View.OnClickListener addContact = new View.OnClickListener(){

		@Override
		public void onClick(View v) {
			cacheCurrentPaymentControlValues();
			Intent intent = MetrixActivityHelper.createActivityIntent(DebriefPayment.this, DebriefPaymentContact.class);
			MetrixActivityHelper.startNewActivity(DebriefPayment.this, intent);
		}
	};

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void populateContacts() {
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

		MetrixCursor requestContactCursor = null;
		MetrixCursor contactCursor = null;
		SpinnerKeyValuePair items[] = null;

		ArrayList<SpinnerKeyValuePair> arrayListItems = new ArrayList<SpinnerKeyValuePair>();
		arrayListItems.add(new SpinnerKeyValuePair("", ""));
		String noName = AndroidResourceHelper.getMessage("NoName");
		try {

			String requestContactQuery = String.format("%s'%s'", "select rc.metrix_row_id, rc.first_name, rc.last_name from request_contact rc " +
					"left outer join task t on rc.request_id = t.request_id where t.task_id = ", taskId);

			requestContactCursor = MetrixDatabaseManager.rawQueryMC(requestContactQuery, null);
			if (requestContactCursor != null && requestContactCursor.moveToFirst()) {
				while (requestContactCursor.isAfterLast() == false) {
					String metrixRowId = "RC|" + requestContactCursor.getString(0);
					String firstName = requestContactCursor.getString(1);
					String lastName = requestContactCursor.getString(2);

					if (MetrixStringHelper.isNullOrEmpty(firstName))
						firstName = noName;
					if (MetrixStringHelper.isNullOrEmpty(lastName))
						lastName = noName;

					String name = firstName + " " + lastName;

					arrayListItems.add(new SpinnerKeyValuePair(name, metrixRowId));
					requestContactCursor.moveToNext();
				}
			}

			String contactQuery = String.format("%s'%s'", "select c.metrix_row_id, c.first_name, c.last_name from contact c left outer join place_contact pc on c.contact_sequence = pc.contact_sequence " +
					"left outer join task t on pc.place_id = t.place_id_cust where t.task_id = ", taskId);

			contactCursor = MetrixDatabaseManager.rawQueryMC(contactQuery, null);
			if (contactCursor != null && contactCursor.moveToFirst()) {
				while (contactCursor.isAfterLast() == false) {
					String metrixRowId = "CO|" + contactCursor.getString(0);
					String firstName = contactCursor.getString(1);
					String lastName = contactCursor.getString(2);

					if (MetrixStringHelper.isNullOrEmpty(firstName))
						firstName = noName;
					if (MetrixStringHelper.isNullOrEmpty(lastName))
						lastName = noName;

					String name = firstName + " " + lastName;

					if(!contactNameExists(arrayListItems, name)){
						arrayListItems.add(new SpinnerKeyValuePair(name, metrixRowId));
					}

					contactCursor.moveToNext();
				}
			}

			items = new SpinnerKeyValuePair[arrayListItems.size()];
			items = arrayListItems.toArray(items);

		}
		catch(Exception e){
			e.printStackTrace();
		} finally {
			if (requestContactCursor != null) {
				requestContactCursor.close();
			}
			if (contactCursor != null) {
				contactCursor.close();
			}
		}

		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(this, R.layout.spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "contact_name");
		if(spinner != null)
			spinner.setAdapter(adapter);
	}

	private void setCreditCardLayoutsState() {
		int visible = 0;
		String paymentMethod = "";

		try {
			paymentMethod = MetrixControlAssistant.getValue(mPaymentMethod);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		if ((paymentMethod.compareToIgnoreCase("CRDC") == 0) || (paymentMethod.compareToIgnoreCase("DBTC") == 0)) {
			visible = View.VISIBLE;

			for (MetrixColumnDef columnDef : this.mFormDef.tables.get(0).columns) {
				if ((columnDef.columnName.compareToIgnoreCase("payment_card_type") == 0) || (columnDef.columnName.compareToIgnoreCase("payment_card_holder") == 0) || (columnDef.columnName.compareToIgnoreCase("payment_card_no_mask") == 0)
						|| (columnDef.columnName.compareToIgnoreCase("payment_card_cvv_mask") == 0)) {
					columnDef.required = true;
				}
			}

			try {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "payment_status", "");
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		} else {
			visible = View.GONE;

			for (MetrixColumnDef columnDef : this.mFormDef.tables.get(0).columns) {
				if ((columnDef.columnName.compareToIgnoreCase("payment_card_type") == 0) || (columnDef.columnName.compareToIgnoreCase("payment_card_holder") == 0) || (columnDef.columnName.compareToIgnoreCase("payment_card_no_mask") == 0)
						|| (columnDef.columnName.compareToIgnoreCase("payment_card_cvv_mask") == 0)) {
					columnDef.required = false;
				}
			}

			try {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "payment_status", "AUTH");
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}

		MetrixFormManager.identifyRequiredFields(mLayout, this.mFormDef);

		MetrixControlAssistant.setParentVisibility(mLayout, visible,this.mFormDef, "payment", "payment_card_type");
		MetrixControlAssistant.setParentVisibility(mPaymentCardHolder.getId(), mLayout, visible);
		MetrixControlAssistant.setParentVisibility(mPaymentCardNoMask.getId(), mLayout, visible);
		MetrixControlAssistant.setParentVisibility(mPaymentCardCvvMask.getId(), mLayout, visible);
		MetrixControlAssistant.setParentVisibility(mExpireMonth.getId(), mLayout, visible);
		MetrixControlAssistant.setParentVisibility(mExpireYear.getId(), mLayout, visible);

		if(paymentMethod.compareToIgnoreCase("CHK") == 0) {
			MetrixControlAssistant.setParentVisibility(mPaymentRef.getId(), mLayout, View.VISIBLE);
		}
		else {
			MetrixControlAssistant.setParentVisibility(mPaymentRef.getId(), mLayout, View.GONE);
		}

		ViewGroup viewPreviousBar = (ViewGroup) findViewById(R.id.view_previous_entries_bar);
		mPaymentImg = (Button)viewPreviousBar.findViewById(R.id.view_payment_status);
		mPaymentImg.setVisibility(View.GONE);
	}

	protected void displayPreviousCount() {
		int count = MetrixDatabaseManager.getCount("payment", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

		try {
			MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);

		mFABList.add(mSaveButton);
		mFABList.add(mNextButton);

		mPaymentCardType = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_type");
		mPaymentCardHolder = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_holder");
		mPaymentCardNoMask = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_no_mask");
		mPaymentCardCvvMask = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_cvv_mask");
		mPaymentCardExpired = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_expired");
		mPaymentRef = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_ref");
		mPaymentMethod = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_method");
		mExpireMonth = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "expire_month");
		mExpireYear = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "expire_year");

		mSaveButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		mViewPreviousEntriesButton.setOnClickListener(this);
		mPaymentCardExpired.setOnClickListener(this);
		mPaymentCardExpired.setOnFocusChangeListener(this);

		MetrixControlAssistant.setOnItemSelectedListener(mPaymentMethod, new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (MetrixControlAssistant.spinnerIsReady(mPaymentMethod)) {
					setCreditCardLayoutsState();
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		if (this.mActivityDef != null && this.mActivityDef.TransactionType == MetrixTransactionTypes.UPDATE) {
			this.displaySaveButtonOnAddNextBar();
		}

		populateContacts();

		fabRunnable = this::showFABs;

		float mOffset = generateOffsetForFABs(mFABList);
		// The amount subtracted it equal to the button height plus the top and bottom margins of the button
		mOffset -= getResources().getDimension((R.dimen.button_height)) + (2*getResources().getDimension((R.dimen.md_margin)));

		NestedScrollView scrollView = findViewById(R.id.scroll_view);
		mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)mOffset);
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
		if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
			try {
				String contactSequence = MetrixDatabaseManager.getFieldStringValue("task", "contact_sequence", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "contact_sequence", contactSequence);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "payment_currency", User.getUser().getCurrencyToUse());
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "request_id", MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));

				setOutstandingValue();

				ArrayList<String> months = new ArrayList<String>();
				months.add("");

				for (int i = 1; i < 13; i++) {
					String month = "";
					if (i < 10) {
						month = "0" + String.valueOf(i);
					} else {
						month = String.valueOf(i);
					}
					months.add(month);
				}

				ArrayList<String> years = new ArrayList<String>();
				years.add("");

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				for (int i = 0; i < 25; i++) {
					years.add(String.valueOf(year + i));
				}

				mExpireMonth = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "expire_month");
				mExpireYear = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "expire_year");
				MetrixControlAssistant.populateSpinnerFromList(this, mExpireMonth, months);
				MetrixControlAssistant.populateSpinnerFromList(this, mExpireYear, years);

			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	private void setOutstandingValue() {
		DecimalFormat formatter = MetrixFloatHelper.getCurrencyFormatter();
		Map<String, Double> totalExpenses = PaymentHelper.CalculateTotalExpenses();
		Map<String, Double> totalLabor = PaymentHelper.CalculateTotalLabor();
		Map<String, Double> totalParts = PaymentHelper.CalculateTotalParts();
		Map<String, Double> totalPayments = PaymentHelper.CalculatePayments();

		double tExpenses =0;
		double tPart = 0;
		double tLabor = 0;
		double tPayment = 0;

		if (totalExpenses!=null && totalLabor!=null && totalParts!=null && (totalExpenses.size()>1 || totalLabor.size()>1 || totalParts.size()>1)) {
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", NOT_APPLICABLE);
		} else {
			if(totalExpenses!=null && totalExpenses.size()==1) {
				for (Map.Entry<String, Double> entry : totalExpenses.entrySet()) {
					tExpenses = Double.valueOf(entry.getValue());
				}
			}

			if(totalParts!=null && totalParts.size()==1) {
				for (Map.Entry<String, Double> entry : totalParts.entrySet()) {
					tPart = Double.valueOf(entry.getValue());
				}
			}

			if(totalLabor!=null && totalLabor.size()==1) {
				for (Map.Entry<String, Double> entry : totalLabor.entrySet()) {
					tLabor = Double.valueOf(entry.getValue());
				}
			}

			if(totalPayments!=null && totalPayments.size()==1) {
				for (Map.Entry<String, Double> entry : totalPayments.entrySet()) {
					tPayment = Double.valueOf(entry.getValue());
				}
			}

			// process the combined value such as total, outstanding etc
			if((totalExpenses == null ||(totalExpenses!=null && totalExpenses.size()<=1))
					&& (totalLabor == null ||(totalLabor!=null && totalLabor.size()<=1))
					&& (totalPayments == null ||(totalPayments!=null && totalPayments.size()<=1))
					&& (totalParts == null ||(totalParts!=null && totalParts.size()<=1))) {
				boolean sameCurrency = false;
				String eCurrency = PaymentHelper.getCurrency(totalExpenses);
				String lCurrency = PaymentHelper.getCurrency(totalLabor);
				String pCurrency = PaymentHelper.getCurrency(totalParts);
				String mCurrency = PaymentHelper.getCurrency(totalPayments);

				sameCurrency = PaymentHelper.isSameCurrency(eCurrency, lCurrency, pCurrency, mCurrency);

				if(sameCurrency) {
					String currency = "";

					if(!MetrixStringHelper.isNullOrEmpty(eCurrency))
						currency = eCurrency;
					else if(!MetrixStringHelper.isNullOrEmpty(lCurrency))
						currency = lCurrency;
					else if(!MetrixStringHelper.isNullOrEmpty(pCurrency))
						currency = pCurrency;
					else if(!MetrixStringHelper.isNullOrEmpty(mCurrency))
						currency = mCurrency;

					try {
						if(!MetrixStringHelper.isNullOrEmpty(currency))
							formatter.setCurrency(Currency.getInstance(currency));
						else
							formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
					} catch (Exception ex) {
						formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
					}
					MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", formatter.format((tExpenses + tLabor + tPart) - tPayment));
				}
				else
					MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", NOT_APPLICABLE);
			}
			else {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", NOT_APPLICABLE);
			}
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef paymentDef = null;
		if (this.mActivityDef != null) {
			paymentDef = new MetrixTableDef("payment", this.mActivityDef.TransactionType);
			if (this.mActivityDef.Keys != null) {
				paymentDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
			}
		} else {
			paymentDef = new MetrixTableDef("payment", MetrixTransactionTypes.INSERT);
		}

		this.mFormDef = new MetrixFormDef(paymentDef);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;

		try {
			if (v.getId() == mPaymentCardExpired.getId()) {
				showDialog(0);
				return;
			}

			switch (v.getId()) {
				case R.id.save:
					if (MobileGlobal.isDemoBuild(this)) {
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PaymentAccepted"));
						break;
					}
					processContact();
					processPayment(false);
					break;
				case R.id.next:
					this.mGoNext = true;
					if (anyOnStartValuesChanged() && (!taskIsComplete())) {
						processContact();
						processPayment(true);
					} else {
						MetrixWorkflowManager.advanceWorkflow(this);
					}
					break;
				case R.id.update:
					if (anyOnStartValuesChanged()) {
						processContact();
						MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
						MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("Payment"));
					}
					finish();
					break;
				case R.id.view_previous_entries:
					Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPaymentList.class);
					MetrixActivityHelper.startNewActivity(this, intent);
					break;
				default:
					super.onClick(v);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void processPayment(Boolean goNext) throws Exception {
		mResponseProcessed = false;

		try {
			String checkMessage = setPaymentCardExpired();
			if (!MetrixStringHelper.isNullOrEmpty(checkMessage)) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, checkMessage);
				return;
			}

			String paymentMethod = MetrixControlAssistant.getValue(mPaymentMethod);
			boolean isCreditOrDebit = ((paymentMethod.compareToIgnoreCase("CRDC") == 0) || (paymentMethod.compareToIgnoreCase("DBTC") == 0));

			if(isCreditOrDebit == false) {
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, true, AndroidResourceHelper.getMessage("Payment"));
				if (result != MetrixSaveResult.SUCCESSFUL) {
					if (isCreditOrDebit)
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("CreditCardFail"));

					return;
				} else {
					this.mCurrentMessageId = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "message_id", "table_name='payment' order by message_id desc");
				}

				if (goNext) {
					MetrixWorkflowManager.advanceWorkflow(this);
				} else {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPayment.class);
					MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				}
			}
			else {
				if(SettingsHelper.getSyncPause(mCurrentActivity))
				{
					SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
					if(syncPauseAlertDialog != null)
					{
						syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
							@Override
							public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) throws Exception {
								processCCPayment();
							}
						});
					}
				}
				else
					processCCPayment();
			}

		}catch(Exception ex){
			throw ex;
		}
	}

	private void processCCPayment() throws Exception {
		Hashtable<String, String> params = new Hashtable<String, String>();
		String firstName = "";
		String lastName = "";

		String[] nameParts = MetrixControlAssistant.getValue(mPaymentCardHolder).split(" ");
		if(nameParts!=null && nameParts.length>1) {
			firstName = nameParts[0];
			lastName = nameParts[1];
		}

		params.put("request_id", MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		params.put("place_id", "");
		params.put("contract_id", "");
		params.put("person_id", User.getUser().personId);
		params.put("amount", MetrixControlAssistant.getValue(this.mFormDef, this.mLayout, "payment", "payment_amount"));
		params.put("currency", User.getUser().getCurrencyToUse());
		params.put("card_holder_first_name", firstName);
		params.put("card_holder_last_name", lastName);
		params.put("card_no", MetrixControlAssistant.getValue(mPaymentCardNoMask));
		params.put("card_issue_no", "");
		params.put("card_type", MetrixControlAssistant.getValue(mPaymentCardType));
		params.put("card_cvv", MetrixControlAssistant.getValue(mPaymentCardCvvMask));
		params.put("card_start_date", "");
		params.put("card_expire_date", MetrixControlAssistant.getValue(mFormDef, mLayout, "payment", "payment_card_expired"));

		params.put("street1", "");
		params.put("street2", "");
		params.put("city", "");
		params.put("state_province", "");
		params.put("postal_code", "");
		params.put("country_name", "");
		params.put("task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
		params.put("contact_sequence", MetrixControlAssistant.getValue(mFormDef, mLayout, "payment", "contact_sequence"));
		params.put("payment_method", MetrixControlAssistant.getValue(mPaymentMethod));

		MetrixPerformMessage performPayment = new MetrixPerformMessage("perform_mobile_authorize_payment", params);
		performPayment.save();

		this.mCurrentMessageId = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "message_id", "transaction_desc='perform_mobile_authorize_payment' order by message_id desc");

		startCreditCardProcess();

		try {
			if (mSyncService != null && mSyncService.getSyncManager() != null) {
				// runOnUiThread would not work well, because it will block the UI.
				mSyncTask = new SyncTask(2);
				mSyncTask.execute(mSyncService.getSyncManager());
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private String setPaymentCardExpired() {
		try {
			String paymentMethod = MetrixControlAssistant.getValue(mPaymentMethod);

			if ((paymentMethod.compareToIgnoreCase("CRDC") == 0) || (paymentMethod.compareToIgnoreCase("DBTC") == 0)) {
				String month = MetrixControlAssistant.getValue(mExpireMonth);
				String year = MetrixControlAssistant.getValue(mExpireYear);

				if(MetrixStringHelper.isNullOrEmpty(month)|| MetrixStringHelper.isNullOrEmpty(year))
				{
					return (AndroidResourceHelper.getMessage("ExpirationRequired"));
				}

				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "payment_card_expired", month + "/" + year);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "person_id", User.getUser().personId);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		return "";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * android.view.View.OnFocusChangeListener#onFocusChange(android.view.View,
	 * boolean)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (hasFocus) {
			if (v.getId() == mPaymentCardExpired.getId()) {
				showDialog(0);
			} else {
				super.onFocusChange(v, hasFocus);
			}
		} else {
			super.onFocusChange(v, hasFocus);
		}
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			try {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "payment_card_expired", MetrixDateTimeHelper.formatDate(year, monthOfYear, dayOfMonth));
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		}
	};


	@Override
	protected Dialog onCreateDialog(int id) {
		try {
			switch (id) {
				case 0:
					String workDate = MetrixControlAssistant.getValue(mPaymentCardExpired);
					Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, workDate);

					return new DatePickerDialog(this, MetrixApplicationAssistant.getSafeDialogThemeStyleID(), mDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		try {
			switch (id) {
				case 0:
					String workDate = MetrixControlAssistant.getValue(mPaymentCardExpired);
					Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, workDate);

					((DatePickerDialog) dialog).updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private boolean showPaymentResult(String message) {
		ArrayList<HashMap<String, String>> resultList = processMessage(message);

		try {
			if (resultList == null||resultList.size()<=0) {
				return false;
			}

			for (HashMap<String, String> payment : resultList) {
				if (payment.get("payment_status") != null) {
					String status = payment.get("payment_status");
					String payment_id = payment.get("payment_id");
					String message_id = payment.get("message_id");

					if(this.mCurrentMessageId.compareTo(message_id)!=0)
						continue;

					if(!MetrixStringHelper.isNullOrEmpty(payment_id)){
						MetrixDatabaseManager.executeSql("update payment set payment_status='"+status+"' where payment_id = "+payment_id);
					}

					if (status.compareToIgnoreCase("AUTH") == 0) {
						displayResultImage("authorized");

						if (this.mGoNext == true) {
							AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setMessage(AndroidResourceHelper.getMessage("PaymentAcceptedContinue")).setPositiveButton(AndroidResourceHelper.getMessage("Yes"), dialogClickListener).setNegativeButton(AndroidResourceHelper.getMessage("No"), dialogClickListener).show();
						}
						else {
							AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setMessage(AndroidResourceHelper.getMessage("PaymentAccepted")).setPositiveButton(AndroidResourceHelper.getMessage("OK"), dialogClickListener).show();
						}
					}
					else {
						displayResultImage("declined");
						// reset primary key for the new submission
						this.setPrimaryKeys(mLayout, mFormDef);
						this.mGoNext = false;
					}

					return true;
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance(this).error(ex);
		}

		return false;
	}

	private void setPrimaryKeys(ViewGroup layout, MetrixFormDef metrixFormDef) {
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (tableDef.transactionType == MetrixTransactionTypes.INSERT) {
				int primaryKey = MetrixDatabaseManager.generatePrimaryKey(tableDef.tableName);

				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.primaryKey) {
						try {
							MetrixControlAssistant.setValue(metrixFormDef, layout, tableDef.tableName, columnDef.columnName, String.valueOf(primaryKey));
						} catch (Exception e) {
							LogManager.getInstance(this).error(e);
						}
					}
				}
			}
		}
	}


	private ArrayList<HashMap<String, String>> processMessage(String message) {
		String metrixPerformResponse = "perform_mobile_authorize_payment_result";
		String matchResult = "";
		ArrayList<HashMap<String, String>> tableValues = new ArrayList<HashMap<String, String>>();

		try {
			if (MetrixStringHelper.doRegularExpressionMatch(metrixPerformResponse, message)) {

				String metrixResponse = MetrixStringHelper.getRegularExpressionMatch(metrixPerformResponse, message);

				if (MetrixStringHelper.isNullOrEmpty(metrixResponse) == false) {
					matchResult = metrixResponse;
				}

				JSONObject jResult = new JSONObject(message);
				JSONObject jSelect = jResult.getJSONObject(matchResult);

				JSONArray jTables = jSelect.names();

				for (int m = 0; m < jTables.length(); m++) {
					String tableName = jTables.getString(m);
					JSONArray jArrays = jSelect.optJSONArray(tableName);

					if (jArrays != null) {
						String jName = jArrays.getString(0);
						if (jName.compareToIgnoreCase("error") == 0) {
							return null;
						} else {
							for (int n = 0; n < jArrays.length(); n++) {
								JSONObject jArrayField = jArrays.optJSONObject(n);
								HashMap<String, String> columns = new HashMap<String, String>();

								if (jArrayField != null) {
									@SuppressWarnings("unchecked")
									Iterator<String> it = jArrayField.keys();
									while (it.hasNext()) {
										String keyName = (String) it.next();
										String keyValue = (String) jArrayField.getString(keyName);
										columns.put(keyName, keyValue);
									}
								}

								tableValues.add(columns);
							}
						}
					}
					else {
						JSONObject jTable = jSelect.optJSONObject(tableName);

						HashMap<String, String> columns = new HashMap<String, String>();

						columns = new HashMap<String, String>();

						if (jTable != null) {
							@SuppressWarnings("unchecked")
							Iterator<String> it = jTable.keys();
							while (it.hasNext()) {
								String keyName = (String) it.next();
								String keyValue = (String) jTable.getString(keyName);
								columns.put(keyName, keyValue);
							}
						}

						tableValues.add(columns);
					}
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return tableValues;
	}

	@Override
	protected void bindService() {
		bindService(new Intent(this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	protected void unbindService() {
		if (mIsBound) {
			try {
				if (service != null) {
					service.removeListener(listener);
					unbindService(mConnection);
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				mIsBound = false;
			}
		}
	}

	protected ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);

				mLocalBinder = (MetrixIntentService.LocalBinder) binder;
				mSyncService = mLocalBinder.getService();

			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					String sync_message = MetrixStringHelper.filterJsonMessage(message);
					if (activityType == ActivityType.Download && mResponseProcessed == false) {
						if (sync_message.contains("perform_mobile_authorize_payment")) {
							mResponseProcessed = showPaymentResult(sync_message);

							if(mResponseProcessed)
								mUIHelper.dismissLoadingDialog();
						}
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					if (((DebriefPayment) mCurrentContext).mGoNext)
						MetrixWorkflowManager.advanceWorkflow((DebriefPayment) mCurrentContext);
					else
						MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, DebriefPayment.class);
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, DebriefPayment.class);
					break;
			}
		}
	};

	private void searchEnded() {
		mUIHelper.dismissLoadingDialog();
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			if (!mResponseProcessed) {
				// This method runs in the same thread as the UI.
				searchEnded();
				String appParamMessage = getAppParamValue("CREDIT_CHECK_TIMED_OUT");
				MetrixUIHelper.showErrorDialogOnGuiThread(mCurrentActivity, appParamMessage);
			}
			else {
				searchEnded();
			}
		}
	};

	private void startCreditCardProcess(){
		mUIHelper = new MetrixUIHelper(mCurrentActivity);
		mUIHelper.showLoadingDialog((AndroidResourceHelper.getMessage("AuthorizingCreditCard")));

		mLayout.postDelayed(Timer_Tick, 30000);
	}

	private String getAppParamValue(String param_name)
	{
		String appMessage = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='"+param_name+"'");
		return appMessage;
	}

	private void displayResultImage(String status) {
		ViewGroup viewPreviousBar = (ViewGroup) findViewById(R.id.view_previous_entries_bar);
		mPaymentImg = (Button)viewPreviousBar.findViewById(R.id.view_payment_status);
		mPaymentImg.setVisibility(View.VISIBLE);

		if (status.compareToIgnoreCase("authorized") == 0) {
			mPaymentStatusImg = getResources().getDrawable(R.drawable.payment_auth);
			mPaymentImg.setCompoundDrawablesWithIntrinsicBounds(null, mPaymentStatusImg, null, null);
		}
		else if (status.compareToIgnoreCase("declined") == 0) {
			mPaymentStatusImg = getResources().getDrawable(R.drawable.payment_reject);
			mPaymentImg.setCompoundDrawablesWithIntrinsicBounds(null, mPaymentStatusImg, null, null);
		}
		else if (status.compareToIgnoreCase("timeout") == 0) {
			mPaymentStatusImg = getResources().getDrawable(R.drawable.block);
			mPaymentImg.setCompoundDrawablesWithIntrinsicBounds(null, mPaymentStatusImg, null, null);
		}

		try {
			mPaymentCardCvvMask = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_cvv_mask");
			if (status.compareToIgnoreCase("authorized") == 0) {
				mPaymentCardNoMask = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_no_mask");
				mPaymentCardExpired = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "payment", "payment_card_expired");
				mExpireMonth = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "expire_month");
				mExpireYear = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "expire_year");

				MetrixControlAssistant.setParentVisibility(mPaymentCardNoMask.getId(), mLayout, View.GONE);
				MetrixControlAssistant.setParentVisibility(mPaymentCardCvvMask.getId(), mLayout, View.GONE);
				MetrixControlAssistant.setParentVisibility(mExpireMonth.getId(), mLayout, View.GONE);
				MetrixControlAssistant.setParentVisibility(mExpireYear.getId(), mLayout, View.GONE);
				mPaymentCardExpired.setVisibility(View.GONE);
			} else {
				mPaymentCardCvvMask.setText("");
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * Set Contact details
	 */
	private void processContact() {
		String selectedValue = MetrixControlAssistant.getValue(mFormDef, mLayout, "custom", "contact_name");
		if(!MetrixStringHelper.isNullOrEmpty(selectedValue)){
			if(selectedValue.contains("RC|")){
				String metrixRowId = selectedValue.substring(3);
				String sequence = MetrixDatabaseManager.getFieldStringValue("request_contact", "sequence", "metrix_row_id=" + metrixRowId);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "contact_sequence", null);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "req_cont_seq", sequence);
			}
			else{
				String metrixRowId = selectedValue.substring(3);
				String contactSequence = MetrixDatabaseManager.getFieldStringValue("contact", "contact_sequence", "metrix_row_id=" + metrixRowId);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "contact_sequence", contactSequence);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "payment", "req_cont_seq", null);
			}
		}
	}

	/**
	 * @param items
	 * @param name
	 * @return true if the contact name found
	 */
	private boolean contactNameExists(ArrayList<SpinnerKeyValuePair> items, String name) {
		boolean found = false;
		for (SpinnerKeyValuePair spinnerKeyValuePair : items) {
			if(!MetrixStringHelper.isNullOrEmpty(name)){
				if(name.compareToIgnoreCase(spinnerKeyValuePair.spinnerKey) == 0){
					found = true;
					break;
				}
			}
		}
		return found;
	}

	private void cacheCurrentPaymentControlValues()
	{
		if (mFormDef != null && mFormDef.tables.size() > 0 && mLayout != null) {
			HashMap<String, String> map = new HashMap<String, String>();
			for (MetrixTableDef table : mFormDef.tables) {
				for (MetrixColumnDef col : table.columns) {
					String cacheKey = String.format("%1$s__%2$s__%3$s", this.getClass().getName(), table.tableName, col.columnName);
					String value = MetrixControlAssistant.getValue(mFormDef, mLayout, table.tableName, col.columnName);
					map.put(cacheKey, value);
				}
			}
			MetrixPublicCache.instance.addItem("CurrentPaymentControlValues", map);
		}
	}

	private void restoreCurrentPaymentControlValues()
	{
		if(MetrixPublicCache.instance.containsKey("CurrentPaymentControlValues")) {
			HashMap<String, String> map = (HashMap<String, String>) MetrixPublicCache.instance.getItem("CurrentPaymentControlValues");
			if (map != null) {
				if (mFormDef != null && mFormDef.tables.size() > 0 && mLayout != null) {
					for (MetrixTableDef table : mFormDef.tables) {
						for (MetrixColumnDef col : table.columns) {
							if (col.primaryKey) continue;
							String cacheKey = String.format("%1$s__%2$s__%3$s", this.getClass().getName(), table.tableName, col.columnName);
							if (map.containsKey(cacheKey)) {
								String value = map.get(cacheKey);
								MetrixControlAssistant.setValue(mFormDef, mLayout, table.tableName, col.columnName, value);
							}
						}
					}
				}
			}

			MetrixPublicCache.instance.removeItem("CurrentPaymentControlValues");
		}
	}

	private void setNewContact()
	{
		if (!MetrixStringHelper.isNullOrEmpty(key))
		{
			//display newly added contact name
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "contact_name", key);
		}
	}

	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}

	protected void renderTabletSpecificLayoutControls(){
		super.renderTabletSpecificLayoutControls();
		populateList();
		this.listActivityFullNameInTabletUIMode = String.format("%s.%s", "com.metrix.metrixmobile", "DebriefPaymentList");
	}

	private void populateList() {
		int listScreenId = MetrixScreenManager.getScreenId("DebriefPaymentList");
		String query = MetrixListScreenManager.generateListQuery("payment", String.format("payment.task_id = %s", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")), listScreenId);
		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, listScreenId);

				String firstName = "";
				String lastName = "";
				String contactSequence = row.get("payment.contact_sequence");
				if (!MetrixStringHelper.isNullOrEmpty(contactSequence)) {
					firstName = row.get("contact.first_name");
					lastName = row.get("contact.last_name");
				}

				String requestContactSequence = row.get("payment.req_cont_seq");
				if (!MetrixStringHelper.isNullOrEmpty(requestContactSequence)) {
					firstName = row.get("request_contact.first_name");
					lastName = row.get("request_contact.last_name");
				}

				String fullName = "";
				if (!MetrixStringHelper.isNullOrEmpty(firstName) || !MetrixStringHelper.isNullOrEmpty(lastName)) {
					if (MetrixStringHelper.isNullOrEmpty(firstName)) {
						fullName = String.format("%s", lastName);
					} else {
						fullName = String.format("%1$s %2$s", firstName, lastName);
					}
				}
				row.put("custom.full_name", fullName);

				table.add(row);
				cursor.moveToNext();
			}

			table = MetrixListScreenManager.performScriptListPopulation(this, listScreenId, "DebriefPaymentList", table);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, listScreenId, "payment.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		mSelectedPosition = position;
		if(taskIsComplete() || scriptEventConsumesListTap(this, view, linkedScreenIdInTabletUIMode)) return;

		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);

				String metrixRowId = selectedItem.get("payment.metrix_row_id");
				String paymentId = MetrixDatabaseManager.getFieldStringValue("payment", "payment_id",
						"metrix_row_id=" + metrixRowId);

				MetrixUpdateManager.delete(DebriefPayment.this, "payment", metrixRowId, "payment_id", paymentId, AndroidResourceHelper.getMessage("Payment"), MetrixTransaction.getTransaction("task", "task_id"));
				mAdapter.getListData().remove(mSelectedPosition);
				mAdapter.notifyItemRemoved(mSelectedPosition);

				refreshDebriefNavigationList();
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("PaymentLCase"), deleteListener, null, this);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
	//End Tablet UI Optimization
}

