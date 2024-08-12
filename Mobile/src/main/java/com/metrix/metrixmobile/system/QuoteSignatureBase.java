package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.PaymentHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.SignaturePad;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Hashtable;
import java.util.Map;

public class QuoteSignatureBase extends QuoteActivity implements View.OnClickListener {
	Button mSaveButton, mNextButton;
	protected Bitmap mSignatureImage;
	protected ImageView mClearSignature, signaturePreview;
	protected static boolean mSignatureSigned = false;
	protected static boolean mSignatureSaved = false;
	protected boolean mSignatureRestore = false;
	protected boolean mScreenLefted = false;
	protected static String NOT_APPLICABLE = "NA";
	protected NestedScrollView mScrollView = null;
	protected RelativeLayout mSignatureInformationArea, mSplitActionBar,mCusSignatureAlreadySaved = null;
	protected String alreadySignedArea;
	protected String quoteId, quoteVersion, signatureFileName;

	private static final int SIGNATURE_REQUEST_CODE = 221;

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		quoteVersion = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");
		signaturePreview = findViewById(R.id.signaturePreview);

		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
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
				final String fileName = filePath + "/" + signatureFileName;
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

		if (mSignatureRestore == true) {
			try {
				final String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
				final String fileName = filePath + "/" + signatureFileName;
				final Bitmap signImage = MetrixAttachmentManager.getInstance().loadPicture(fileName);
				Glide.with(this)
						.load(signImage)
						.into(signaturePreview);
			}
			catch(Exception ex){
				LogManager.getInstance().error(ex);
			}

			if(mSignatureSaved) {
				mSaveButton.setEnabled(false);
			}
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

		MetrixTableDef quoteDef = new MetrixTableDef("quote", MetrixTransactionTypes.UPDATE);
		quoteDef.constraints.add(new MetrixConstraintDef("quote_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"), String.class));

		MetrixRelationDef placeRelationDef = new MetrixRelationDef("quote", "place_id", "place_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef placeDef = new MetrixTableDef("place", MetrixTransactionTypes.SELECT, placeRelationDef);

		tableDefs.add(quoteDef);
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

			Map<String, Double> totalExpenses = PaymentHelper.CalculateTotalQuoteExpenses();
			Map<String, Double> totalLabor = PaymentHelper.CalculateTotalQuoteLabor();
			Map<String, Double> totalParts = PaymentHelper.CalculateTotalQuoteParts();

			String requestCurrency = MetrixDatabaseManager.getFieldStringValue("select currency from quote where quote_id = '"+MetrixControlAssistant.getValue(mFormDef, mLayout, "quote", "quote_id")+"'");

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

			ArrayList<Hashtable<String, String>> laborItems = this.getLaborList(quoteId, quoteVersion);
			String laborItemsString = this.formatItems(laborItems);
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "labor_used", laborItemsString);

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

			ArrayList<Hashtable<String, String>> partItems = this.getPartUsedList(quoteId, quoteVersion);
			String partItemsString = this.formatItems(partItems);
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "parts_used", partItemsString);

			if (totalExpenses!=null && totalLabor!=null && totalParts!=null && (totalExpenses.size()>1 || totalLabor.size()>1 || totalParts.size()>1)) {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total", QuoteSignatureBase.NOT_APPLICABLE);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", QuoteSignatureBase.NOT_APPLICABLE);
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

				// process the combined value such as total, outstanding etc
				if((totalExpenses == null ||(totalExpenses!=null && totalExpenses.size()<=1))
						&& (totalLabor == null ||(totalLabor!=null && totalLabor.size()<=1))
						&& (totalParts == null ||(totalParts!=null && totalParts.size()<=1)))
				{
					boolean sameCurrency = false;
					String eCurrency = PaymentHelper.getCurrency(totalExpenses);
					String lCurrency = PaymentHelper.getCurrency(totalLabor);
					String pCurrency = PaymentHelper.getCurrency(totalParts);

					sameCurrency = PaymentHelper.isSameCurrency(eCurrency, lCurrency, pCurrency);

					if(sameCurrency){
						String currency = "";

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
						&& (totalParts == null ||(totalParts!=null && totalParts.size()<=1))) {
					boolean sameCurrency = false;
					String eCurrency = PaymentHelper.getCurrency(totalExpenses);
					String lCurrency = PaymentHelper.getCurrency(totalLabor);
					String pCurrency = PaymentHelper.getCurrency(totalParts);

					sameCurrency = PaymentHelper.isSameCurrency(eCurrency, lCurrency, pCurrency);

					if(sameCurrency) {
						String currency = "";

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
						MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", formatter.format((tExpenses + tLabor + tPart) - tPayment));
					}
					else
						MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", NOT_APPLICABLE);
				}
				else {
					MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "outstanding", NOT_APPLICABLE);
				}
			}

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	protected boolean shouldHideSignatureArea(){
		boolean status = true;
		if(isTablet() && !isTabletRunningInLandscapeMode())
			status = false;
		return status;
	}

	private ArrayList<Hashtable<String, String>> getLaborList(String quoteId, String quoteVerison) {
		ArrayList<Hashtable<String, String>> laborList = new ArrayList<Hashtable<String, String>>();

		String query = "select line_code.line_code, quote_non_part_usage.description, bill_price, quantity from quote_non_part_usage left join line_code on quote_non_part_usage.line_code = line_code.line_code where quote_non_part_usage.quote_id = "
				+ quoteId + " and quote_version = "+quoteVerison;

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 1;
				while (cursor.isAfterLast() == false) {
					Hashtable<String, String> row = new Hashtable<String, String>();
					String line_code = cursor.getString(0)==null? "": cursor.getString(0);
					String description = cursor.getString(1)==null? "": cursor.getString(1);;
					String billPrice = cursor.getString(2)==null? "": cursor.getString(2);
					String quantity = cursor.getString(3)==null? "": cursor.getString(3);

					row.put("line_code", line_code);
					row.put("description", description);
					row.put("bill_price", billPrice);
					row.put("quantity", quantity);

					laborList.add(row);

					cursor.moveToNext();
					i = i + 1;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return laborList;
	}

	private ArrayList<Hashtable<String, String>> getPartUsedList(String quoteId, String quoteVerison) {
		ArrayList<Hashtable<String, String>> partList = new ArrayList<Hashtable<String, String>>();

		String query = "select quote_part_usage.part_line_code, part.internal_descriptn, bill_price, quantity from quote_part_usage left join part on quote_part_usage.part_id = part.part_id where quote_part_usage.quote_id = "
				+ quoteId + " and quote_version = "+quoteVerison;

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 1;
				while (cursor.isAfterLast() == false) {
					Hashtable<String, String> row = new Hashtable<String, String>();
					String line_code = cursor.getString(0)==null? "": cursor.getString(0);
					String description = cursor.getString(1)==null? "": cursor.getString(1);;
					String billPrice = cursor.getString(2)==null? "": cursor.getString(2);
					String quantity = cursor.getString(3)==null? "": cursor.getString(3);

					row.put("line_code", line_code);
					row.put("description", description);
					row.put("bill_price", billPrice);
					row.put("quantity", quantity);

					partList.add(row);

					cursor.moveToNext();
					i = i + 1;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return partList;
	}

	protected String formatItems(ArrayList<Hashtable<String, String>> listItems) {
		StringBuilder itemBuilder = new StringBuilder();

		for(Hashtable<String, String> item : listItems) {
			itemBuilder.append(String.format("(%s)     %s      %s",
					item.get("quantity"),
					item.get("description"), item.get("bill_price")));
			itemBuilder.append("\r\n");
		}

		return itemBuilder.toString();
	}

	protected boolean quoteCanBeCompleted()
	{
		String currentQuoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		return currentTransactionMessagesCompleted(currentQuoteId, "quote_id");
	}

	public static boolean currentTransactionMessagesCompleted(String transactionKeyId, String transactionKeyName)
	{
		boolean messageCompleted = true;

		try
		{
			long currentTransactionId = MetrixUpdateManager.getTransactionId(transactionKeyId, transactionKeyName);

			ArrayList<Hashtable<String, String>> messageList = MetrixDatabaseManager.getFieldStringValuesList("mm_message_out", new String[] { "message_id", "status", "table_name" }, "transaction_id = " + currentTransactionId);

			if (messageList != null && messageList.size() > 0)
				messageCompleted = false;
		}
		catch (Exception ex)
		{
			messageCompleted = false;
		}

		return messageCompleted;
	}

	@Override
	protected void setListeners() {
		super.setListeners();
		signaturePreview.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		if (v.getId() == R.id.signaturePreview) {
			if (!mSignatureSaved)
				SignaturePad.openForResult(this, SIGNATURE_REQUEST_CODE);
		}
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

