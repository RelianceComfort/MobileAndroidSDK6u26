package com.metrix.metrixmobile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.metrixmobile.system.DebriefActivity;
import com.metrix.metrixmobile.system.MetrixPooledTaskAssignmentManager;

public class DebriefCustomerSignature extends DebriefActivity implements View.OnClickListener {
	private FloatingActionButton mSaveButton, mNextButton, mSignatureButton;
	private Bitmap mSignatureImage;
	private ImageView mClearSignature, signaturePreview;
	private static boolean mSignatureSaved = false;
	private boolean mSignatureRestore = false;
	private static boolean mSignatureSigned = false; // This flag will track Signed status for orientation change
	private boolean mScreenLefted = false;
	private static String NOT_APPLICABLE = AndroidResourceHelper.getMessage("NaCaps");
	private NestedScrollView mScrollView = null;
	private RelativeLayout mSignatureInformationArea, mSplitActionBar,mCusSignatureAlreadySaved = null;
	private String taskId;

	private static final int SIGNATURE_REQUEST_CODE = 221;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			mSignatureRestore = true;
		}
		else {
			mSignatureSigned = false;
			mSignatureSaved = false;
		}

		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_customer_signature);
		else
			setContentView(R.layout.debrief_customer_signature);

		mCusSignatureAlreadySaved = findViewById(R.id.cus_signature_already_saved);
		signaturePreview = findViewById(R.id.signaturePreview);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.SignBelowc2202ef6, "TapBelowToSign"));
		resourceStrings.add(new ResourceValueObject(R.id.alreadySavedMessage, "AlreadySavedMessage"));
		taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		String[] tableNames = new String[]{"attachment", "task_attachment"};
		String whereClause = "attachment.attachment_id = task_attachment.attachment_id and task_id = '" + taskId + "' and attachment_name like '%customer_signature%'";
		mSignatureSaved = MetrixDatabaseManager.getCount(tableNames, whereClause) > 0;
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		LinearLayout outerLayout = findViewById(R.id.outer_layout);
		outerLayout.setPadding(outerLayout.getPaddingLeft(), outerLayout.getPaddingRight(), outerLayout.getPaddingTop(), mSplitActionBarHeight);
		AndroidResourceHelper.getMessage("NotAvailable");

		// Due to the limited height -> signature area will be hidden when the
		// soft keyboard is shown.
		// This modification is applicable in android phones(regardless of the orientation) & tablets/tabs in landscape mode only.
		// MX-4035
		if (shouldHideSignatureArea()) {
			final View contentView = findViewById(android.R.id.content);
			contentView.getViewTreeObserver().addOnGlobalLayoutListener(
					new OnGlobalLayoutListener() {
						int mPreviousHeight;

						@Override
						public void onGlobalLayout() {
							contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
							int newHeight = contentView.getHeight();
							if (mPreviousHeight != 0) {
								if (mPreviousHeight > newHeight) {
									// Height decreased: keyboard was shown
									if (signaturePreview != null)signaturePreview.setVisibility(View.GONE);
									if (mSignatureInformationArea != null)mSignatureInformationArea.setVisibility(View.GONE);
									if (mSplitActionBar != null)mSplitActionBar.setVisibility(View.GONE);
								} else if (mPreviousHeight < newHeight) {
									// Height increased: keyboard was hidden
									if (signaturePreview != null)signaturePreview.setVisibility(View.VISIBLE);
									if (mSignatureInformationArea != null)mSignatureInformationArea.setVisibility(View.VISIBLE);
									if (mSplitActionBar != null) {
										// This is a quick fix to avoiding the red bar below when there were no sync errors
										// The change is targeted to only DebriefCustomerSignature and QuoteSignature
										TextView splitActionBarText = findViewById(R.id.action_bar_error);
										if (!splitActionBarText.getText().toString().equals(AndroidResourceHelper.getMessage("ViewSyncErrors")))
											mSplitActionBar.setVisibility(View.VISIBLE);
									}
									// Reset the scroll-bar
									if (mScrollView != null)mScrollView.fullScroll(View.FOCUS_UP);
								} else {
									// No change
								}
							}
							mPreviousHeight = newHeight;
						}
					});
		}
		updateControlState();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mSignatureSigned = MetrixUIHelper.hasImage(signaturePreview);

		if(this.mScreenLefted) {
			try {
				signaturePreview.setImageBitmap(null);
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
			finally {
				this.mScreenLefted = false;
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		try {
			super.onSaveInstanceState(outState);
			if(MetrixUIHelper.hasImage(signaturePreview)) {
				mSignatureSigned = true;
				final String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
				final String fileName = filePath + "/" + "customerSignature.png";
				MetrixAttachmentManager.getInstance().savePicture(fileName, MetrixUIHelper.getBitmapFromImageView(signaturePreview), this);
			}
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle outState) {
		super.onRestoreInstanceState(outState);

		if (mSignatureSigned) {
			try {
				final String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
				final String fileName = filePath+"/"+"customerSignature.png";
				final Bitmap signImage = MetrixAttachmentManager.getInstance().loadPicture(fileName);
				Glide.with(this)
						.load(signImage)
						.into(signaturePreview);
			}
			catch(Exception ex){
				LogManager.getInstance().error(ex);
			}

			if(mSignatureSaved) {
				mSaveButton.setEnabled(false); //TODO may need to add the signature button here
			}
		}
	}


	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

		MetrixTableDef taskDef = new MetrixTableDef("task", MetrixTransactionTypes.UPDATE);
		taskDef.constraints.add(new MetrixConstraintDef("task_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("task", "task_id"), String.class));

		MetrixRelationDef placeRelationDef = new MetrixRelationDef("task", "place_id_cust", "place_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef placeDef = new MetrixTableDef("place", MetrixTransactionTypes.SELECT, placeRelationDef);

		tableDefs.add(taskDef);
		tableDefs.add(placeDef);

		this.mFormDef = new MetrixFormDef(tableDefs);
	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		double tExpenses =0;
		double tPart = 0;
		double tLabor = 0;
		double tPayment = 0;

		try {
			DecimalFormat currencyFormatter = MetrixFloatHelper.getCurrencyFormatter();
			DecimalFormat formatter = MetrixFloatHelper.getCurrencyFormatter();

			Map<String, Double> totalExpenses = CalculateTotalExpenses();
			Map<String, Double> totalLabor = CalculateTotalLabor();
			Map<String, Double> totalParts = CalculateTotalParts();
			Map<String, Double> totalPayments = CalculatePayments();

			String requestIdQuery = "select request_id from task where task_id = "+MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");
			String requestId = MetrixDatabaseManager.getFieldStringValue(requestIdQuery);
			String requestCurrency = MetrixDatabaseManager.getFieldStringValue("select currency from request where request_id = '"+requestId+"'");

			try {
				currencyFormatter.setCurrency(Currency.getInstance(requestCurrency));
			} catch (Exception ex) {
				currencyFormatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
			}

			if (totalExpenses != null && totalExpenses.size()>0) {
				StringBuilder expensesMessage = new StringBuilder();
				int i = 0;
				for (String currency : totalExpenses.keySet()) {
					if (i != 0) {
						expensesMessage.append("\n");
					}
					try {
						formatter.setCurrency(Currency.getInstance(currency));
					} catch (Exception ex) {
						formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
					}
					expensesMessage.append(formatter.format(totalExpenses.get(currency)));
					i = 1;
				}
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_expenses", expensesMessage.toString());
			} else {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_expenses", currencyFormatter.format(0));
			}

			if (totalLabor != null && totalLabor.size()>0) {
				StringBuilder laborMessage = new StringBuilder();
				int i = 0;
				for (String currency : totalLabor.keySet()) {
					if (i != 0) {
						laborMessage.append("\n");
					}
					try {
						formatter.setCurrency(Currency.getInstance(currency));
					} catch (Exception ex) {
						formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
					}
					laborMessage.append(formatter.format(totalLabor.get(currency)));
					i = 1;
				}
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_labor", laborMessage.toString());
			} else {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_labor", currencyFormatter.format(0));
			}


			if (totalParts != null && totalParts.size()>0) {
				StringBuilder partsMessage = new StringBuilder();
				int i = 0;
				for (String currency : totalParts.keySet()) {
					if (i != 0) {
						partsMessage.append("\n");
					}
					try {
						formatter.setCurrency(Currency.getInstance(currency));
					} catch (Exception ex) {
						formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
					}
					partsMessage.append(formatter.format(totalParts.get(currency)));
					i = 1;
				}
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_parts", partsMessage.toString());
			} else {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_parts", currencyFormatter.format(0));
			}

			if (totalPayments != null && totalPayments.size()>0) {
				StringBuilder paymentsMessage = new StringBuilder();
				int i = 0;
				for (String currency : totalPayments.keySet()) {
					if (i != 0) {
						paymentsMessage.append("\n");
					}
					try {
						formatter.setCurrency(Currency.getInstance(currency));
					} catch (Exception ex) {
						formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
					}
					paymentsMessage.append(formatter.format(totalPayments.get(currency)));
					i = 1;
				}
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "paid", paymentsMessage.toString());
			} else {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "paid", currencyFormatter.format(0));
			}

			if (totalExpenses!=null && totalLabor!=null && totalParts!=null && (totalExpenses.size()>1 || totalLabor.size()>1 || totalParts.size()>1)) {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total", DebriefCustomerSignature.NOT_APPLICABLE);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", DebriefCustomerSignature.NOT_APPLICABLE);
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
						&& (totalParts == null ||(totalParts!=null && totalParts.size()<=1)))
				{
					boolean sameCurrency = false;
					String eCurrency = getCurrency(totalExpenses);
					String lCurrency = getCurrency(totalLabor);
					String pCurrency = getCurrency(totalParts);

					sameCurrency = isSameCurrency(eCurrency, lCurrency, pCurrency);

					if(sameCurrency){
						String currency = MetrixStringHelper.isNullOrEmpty(requestCurrency) ? "":requestCurrency;

						if(!MetrixStringHelper.isNullOrEmpty(eCurrency))
							currency = eCurrency;
						else if(!MetrixStringHelper.isNullOrEmpty(lCurrency))
							currency = lCurrency;
						else if(!MetrixStringHelper.isNullOrEmpty(pCurrency))
							currency = pCurrency;

						try {
							if(!MetrixStringHelper.isNullOrEmpty(currency))
								formatter.setCurrency(Currency.getInstance(currency));
							else
								formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
						} catch (Exception ex) {
							formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
						}
						MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total", formatter.format(tExpenses + tLabor + tPart));
					}
					else
						MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total", NOT_APPLICABLE);
				}
				else {
					MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total", NOT_APPLICABLE);
				}

				if((totalExpenses == null ||(totalExpenses!=null && totalExpenses.size()<=1))
						&& (totalLabor == null ||(totalLabor!=null && totalLabor.size()<=1))
						&& (totalPayments == null ||(totalPayments!=null && totalPayments.size()<=1))
						&& (totalParts == null ||(totalParts!=null && totalParts.size()<=1))) {
					boolean sameCurrency = false;
					String eCurrency = getCurrency(totalExpenses);
					String lCurrency = getCurrency(totalLabor);
					String pCurrency = getCurrency(totalParts);
					String mCurrency = getCurrency(totalPayments);

					sameCurrency = isSameCurrency(eCurrency, lCurrency, pCurrency, mCurrency);

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

			taskId = MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");
			if (mSignatureSaved) {
				String query = String.format("select attachment.signer from attachment left outer join task_attachment on task_attachment.attachment_id = attachment.attachment_id where task_attachment.task_id = '%1$s' and attachment_name like '%%customer_signature%%'", taskId);
				ArrayList<Hashtable<String, String>> results = MetrixDatabaseManager.getFieldStringValuesList(query);
				if (results.size() > 0 && !MetrixStringHelper.isNullOrEmpty(results.get(results.size() - 1).get("signer")))
					MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "customer_full_name", results.get(results.size() - 1).get("signer"));
			} else {
				String contactSequence = MetrixDatabaseManager.getFieldStringValue("task", "contact_sequence", "task_id=" + taskId);
				String[] columnsToGet = new String[]{"first_name", "last_name"};
				Hashtable<String, String> results = null;

				if (MetrixStringHelper.isNullOrEmpty(contactSequence)) {
					results = MetrixDatabaseManager.getFieldStringValues("task_contact", columnsToGet, "task_id=" + taskId);
				} else {
					results = MetrixDatabaseManager.getFieldStringValues("contact", columnsToGet, "contact_sequence=" + contactSequence);
				}

				if (!MetrixStringHelper.isNullOrEmpty(results.get("first_name")) && !MetrixStringHelper.isNullOrEmpty(results.get("last_name")))
					MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "customer_full_name", results.get("first_name") + " " + results.get("last_name"));
			}

//			String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
//
//			if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("Y") == 0) {
//
//				Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);
//
//				if (currentLocation != null) {
//					try {
//						MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "geocode_lat", Double.toString(currentLocation.getLatitude()));
//						MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "geocode_long", Double.toString(currentLocation.getLongitude()));
//					} catch (Exception e) {
//						LogManager.getInstance(this).error(e);
//					}
//				}
//			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private boolean isSameCurrency(String a, String b, String c, String d) {
		boolean sameCurrency = false;

		if(isSameCurrency(a, b, c) && isSameCurrency(a, b, d) && isSameCurrency(b, c, d) && isSameCurrency(a, c, d))
			sameCurrency = true;

		return sameCurrency;
	}

	private boolean isSameCurrency(String a, String b, String c) {
		boolean sameCurrency = false;

		if(isSameCurrency(a, b)&&isSameCurrency(a,c)&& isSameCurrency(b,c))
			sameCurrency = true;

		return sameCurrency;
	}

	private boolean isSameCurrency(String a, String b) {
		boolean sameCurrency = false;

		if(MetrixStringHelper.isNullOrEmpty(a)|| MetrixStringHelper.isNullOrEmpty(b))
			return true;

		if(a.compareToIgnoreCase(b)==0)
			sameCurrency = true;

		return sameCurrency;
	}

	private String getCurrency(Map<String, Double> money) {
		String currency = "";

		if(money == null || money.size()<1)
			return "";
		else {
			for (Map.Entry<String, Double> entry : money.entrySet()) {
				currency = entry.getKey();
				break;
			}
		}

		return currency;
	}

	private Map<String, Double> CalculateTotalLabor() {
		Map<String, Double> totalLabor = new HashMap<String, Double>();
		String query = "select bill_price, transaction_currency, quantity from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code where line_code.npu_code = 'TIME' and non_part_usage.task_id = "
				+ MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 1;
				while (cursor.isAfterLast() == false) {
					double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
					String transactionCurrency = cursor.getString(1);
					double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(2), Locale.US));

					if (totalLabor.containsKey(transactionCurrency)) {
						totalLabor.put(transactionCurrency, totalLabor.get(transactionCurrency) + (billPrice * quantity));
					} else {
						totalLabor.put(transactionCurrency, (billPrice * quantity));
					}
					cursor.moveToNext();
					i = i + 1;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return totalLabor;
	}

	private Map<String, Double> CalculateTotalExpenses() {
		Map<String, Double> totalExpenses = new HashMap<String, Double>();

		// add to totalExpenses any expenses with a quantity_format NOT 1, using (transaction_amount * bill_price)
		String query = "select transaction_amount, transaction_currency, bill_price from non_part_usage "
				+ "left join line_code on non_part_usage.line_code = line_code.line_code "
				+ "where line_code.npu_code = 'EXP' and quantity_format != '1' and non_part_usage.task_id = "
				+ MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					double transactionAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
					String transactionCurrency = cursor.getString(1);
					double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(2), Locale.US));
					double extendedPrice = (transactionAmount * billPrice);

					if (totalExpenses.containsKey(transactionCurrency)) {
						totalExpenses.put(transactionCurrency, totalExpenses.get(transactionCurrency) + extendedPrice);
					} else {
						totalExpenses.put(transactionCurrency, extendedPrice);
					}

					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// add to totalExpenses any expenses with a quantity_format of 1, using transaction_amount
		query = "select transaction_amount, transaction_currency from non_part_usage "
				+ "left join line_code on non_part_usage.line_code = line_code.line_code "
				+ "where line_code.npu_code = 'EXP' and quantity_format = '1' and non_part_usage.task_id = "
				+ MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");

		cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					double transactionAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
					String transactionCurrency = cursor.getString(1);

					if (totalExpenses.containsKey(transactionCurrency)) {
						totalExpenses.put(transactionCurrency, totalExpenses.get(transactionCurrency) + transactionAmount);
					} else {
						totalExpenses.put(transactionCurrency, transactionAmount);
					}

					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return totalExpenses;
	}


	private Map<String, Double> CalculateTotalParts() {
		Map<String, Double> totalPart = new HashMap<String, Double>();
		String query = "select bill_price, quantity, billing_currency from part_usage where part_usage.task_id = " + MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					String transactionCurrency = cursor.getString(2);
					double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
					int quantity = cursor.getInt(1);

					if(totalPart.containsKey(transactionCurrency)) {
						totalPart.put(transactionCurrency, totalPart.get(transactionCurrency) + billPrice*quantity);
					}
					else {
						totalPart.put(transactionCurrency, billPrice*quantity);
					}

					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return totalPart;
	}

	private Map<String, Double> CalculatePayments() {
		Map<String, Double> totalPayment = new HashMap<String, Double>();
		String query = "select payment_amount, payment_currency from payment where payment_status = 'AUTH' and request_id = '" + MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")) + "'";

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					String transactionCurrency = cursor.getString(1);
					double paymentAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));

					if(totalPayment.containsKey(transactionCurrency)) {
						totalPayment.put(transactionCurrency, totalPayment.get(transactionCurrency) + paymentAmount);
					}
					else {
						totalPayment.put(transactionCurrency, paymentAmount);
					}
					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return totalPayment;
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mClearSignature = (ImageView) findViewById(R.id.clear_signature);
		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);
//		mSignatureButton = (FloatingActionButton) findViewById(R.id.signature_btn);
		mClearSignature.setOnClickListener(this);
		mSaveButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
//		mSignatureButton.setOnClickListener(this);
		mScrollView = (NestedScrollView) findViewById(R.id.scroll_view);
		mSignatureInformationArea = (RelativeLayout) findViewById(R.id.cus_signature_options_area);
		mSplitActionBar = (RelativeLayout) findViewById(R.id.split_action_bar);
		mCusSignatureAlreadySaved = (RelativeLayout) findViewById(R.id.cus_signature_already_saved);
		signaturePreview.setOnClickListener(this);


	}

	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;

		switch (v.getId()) {
			case R.id.save:
				//if (mSignatureArea.mSigned) {
				if(mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) {
					if(mSignatureSaved == false) {
						mSignatureSaved = this.save(false);
						if (mSignatureSaved)
							updateControlState();
					}
					else {
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, (AndroidResourceHelper.getMessage("AlreadySavedMessage")));
						return;
					}
				} else {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, (AndroidResourceHelper.getMessage("PleaseSign")));
					return;
				}

				break;
			case R.id.next:
				boolean canAdvance = true;
				if ((mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) && mSignatureSaved == false) {
					//if the task is completed, the signature will not be saved even if the user signs in the signature area/edit an existing signature
					if (!taskIsComplete()) {
						canAdvance = this.save(true);
					}
				}

				if (canAdvance) {
					this.mScreenLefted = true;
					this.signaturePreview.setImageBitmap(null);
					mSignatureSigned = false;
					mSignatureSaved = false;
					MetrixWorkflowManager.advanceWorkflow(this);
				}

				break;
//			case R.id.signature_btn:
//				//TODO go to signature modal and might add an alert dialog if signature already saved as a way to clear the signature
//				break;
			case R.id.clear_signature:
				OnClickListener yesListener = new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							signaturePreview.setImageBitmap(null);
							mSignatureSigned = false;
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					}
				};
				MetrixDialogAssistant.showConfirmClearDialog(AndroidResourceHelper.getMessage("SignatureLCase"), yesListener, null, this);
				break;
			case R.id.signaturePreview:
				if (!mSignatureSaved)
					SignaturePad.openForResult(this, SIGNATURE_REQUEST_CODE);
				break;
			default:
				super.onClick(v);
		}
	}

	public boolean save(boolean advanceWorkflowOnError) {
		String errors = MetrixUpdateManager.requiredFieldsAreSet(this, mLayout, mFormDef);
		if (!MetrixStringHelper.isNullOrEmpty(errors)) {
			showIgnoreErrorDialog(errors, null, false, advanceWorkflowOnError);
			return false;
		}
		String path = MetrixAttachmentManager.getInstance().getAttachmentPath();

		String filename;
		String timeStamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
		filename = "task_" + MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id") + "_" + "_customer_signature" + timeStamp;

		try {

			File file = new File(path, "/" + filename + ".jpg");
			final Bitmap signature = MetrixUIHelper.getBitmapFromImageView(signaturePreview);
			this.mSignatureImage = MetrixUIHelper.applyBackgroundColorToBitmap(signature, Color.WHITE);
			if(!MetrixAttachmentManager.getInstance().saveBitmapImageAttachment(this.mSignatureImage, file, this))
				return false;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			this.mSignatureImage.compress(Bitmap.CompressFormat.JPEG, 85, baos);
			String attachment_string = MetrixStringHelper.encodeBase64(baos.toByteArray());
			String signerName = MetrixControlAssistant.getValue(mFormDef, mLayout, "custom", "customer_full_name");

			MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
			DataField attachment_name = new DataField("attachment_name", file.getName());
			DataField attachment_description = new DataField("attachment_description", AndroidResourceHelper.getMessage("CustomerSignature"));
			DataField file_extension = new DataField("file_extension", "jpg");
			DataField signer = new DataField("signer", signerName);
			DataField selectable = new DataField("selectable", "N");                        // M5.7 field
			DataField internalType = new DataField("internal_type", "SIGNATURE");            // M5.7 field
			DataField attachmentField = new DataField("attachment", attachment_string);
			DataField local_path = new DataField("mobile_path", file.getPath());
			int primaryKey = MetrixDatabaseManager.generatePrimaryKey("attachment");

			// only set the attachment type if 'SIGNATURE' exists as a value
			// option
			String type = MetrixDatabaseManager.getFieldStringValue("global_code_table", "code_value", "code_name = 'ATTACHMENT_TYPE' and code_value = 'SIGNATURE'");
			if (!MetrixStringHelper.isNullOrEmpty(type)) {
				DataField attachment_type = new DataField("attachment_type", "SIGNATURE");
				attachmentData.dataFields.add(attachment_type);
			}

			DataField attachment_id = new DataField("attachment_id", primaryKey);

			attachmentData.dataFields.add(attachment_name);
			attachmentData.dataFields.add(selectable);
			attachmentData.dataFields.add(internalType);
			attachmentData.dataFields.add(attachment_description);
			attachmentData.dataFields.add(file_extension);
			attachmentData.dataFields.add(signer);
			attachmentData.dataFields.add(attachmentField);
			attachmentData.dataFields.add(attachment_id);
			attachmentData.dataFields.add(local_path);

			MetrixSqlData taskAttachmentData = new MetrixSqlData("task_attachment", MetrixTransactionTypes.INSERT);
			DataField task_id = new DataField("task_id", MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"));
			taskAttachmentData.dataFields.add(task_id);
			taskAttachmentData.dataFields.add(attachment_id);

			ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<MetrixSqlData>();
			attachmentTrans.add(attachmentData);
			attachmentTrans.add(taskAttachmentData);

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");

			MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("CustomerSignature"), this);
			return true;
		}catch (Exception e) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ErrorSavingAttachmentUnknown"));
			LogManager.getInstance(this).error(e);
			return false;
		}
	}

	private boolean shouldHideSignatureArea(){
		boolean status = true;
		if(isTablet() && !isTabletRunningInLandscapeMode())
			status = false;
		return status;
	}

	private void updateControlState() {
		if (MetrixPooledTaskAssignmentManager.instance().isPooledTask(taskId))
			return; // Controls are already disabled for pooled tasks. Do not override.

		if (mCusSignatureAlreadySaved != null)
			mCusSignatureAlreadySaved.setVisibility(mSignatureSaved ? View.VISIBLE : View.GONE);

		if (signaturePreview != null)
			signaturePreview.setEnabled(!mSignatureSaved);

		if (mFormDef != null) {
			for (MetrixTableDef tableDef : mFormDef.tables) {
				if(tableDef == null) continue;
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if(columnDef == null) continue;
					View view = MetrixControlAssistant.getControl(columnDef.id, mLayout);
					if(view == null) continue;
						view.setEnabled(!mSignatureSaved);
				}
			}
		}

		if (mSaveButton != null)
			mSaveButton.setEnabled(!mSignatureSaved);

		if (mClearSignature != null)
			mClearSignature.setEnabled(!mSignatureSaved);
	}

	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}
	//End Tablet UI Optimization

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK && requestCode == SIGNATURE_REQUEST_CODE && data != null) {
			final String fileName = data.getStringExtra(SignaturePad.SIGNATURE_LOCATION);
			final Bitmap signature = MetrixAttachmentManager.getInstance().loadPicture(fileName);
			if (signature != null) {
				Glide.with(this)
						.load(signature)
						.into(signaturePreview);
				new File(fileName).delete();
			}
		}
	}
}

