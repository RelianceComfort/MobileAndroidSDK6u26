package com.metrix.metrixmobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixTimeClockAssistant;
import com.metrix.metrixmobile.policies.MeterReadingsPolicy;
import com.metrix.metrixmobile.system.DebriefActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DebriefSignature extends DebriefActivity implements View.OnClickListener, OnFocusChangeListener {
	FloatingActionButton mSaveImageButton, /*mPrintReportButton,*/ mCompleteButton;
	private Bitmap mSignatureImage;
	//private EditText mStartDate, mEndDate;
	private ImageView mClearSignature, signaturePreview;
	private boolean mSignatureRestore = false;
	private static boolean mSignatureSigned = false; // This flag will track Signed status for orientation change
	private MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private RelativeLayout mSignBelowBar;

	private static final int SIGNATURE_REQUEST_CODE = 221;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			mSignatureRestore = true;
		}
		else {
			mSignatureSigned = false;
		}

		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_signature);
		else
			setContentView(R.layout.debrief_signature);

		signaturePreview = findViewById(R.id.signaturePreview);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.SignBelowe8dcfc74, "TapBelowToSign"));
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		LinearLayout outerLayout = findViewById(R.id.outer_layout);
		outerLayout.setPadding(outerLayout.getPaddingLeft(), outerLayout.getPaddingRight(), outerLayout.getPaddingTop(), mSplitActionBarHeight);
		enableOrDisableTechnicianSignature();

		if (taskIsComplete() || !enableTaskCompleteButton()) {
			View view = findViewById(R.id.next);
			if (view != null) {
				FloatingActionButton complete = (FloatingActionButton) view;
				AndroidResourceHelper.setResourceValues(complete, "Complete");
				complete.setEnabled(false);
			}
		}
	}

	public void setCompleteEnabled()
	{
		View view = findViewById(R.id.next);
		if (taskIsComplete() || !enableTaskCompleteButton()) {
			if (view != null) {
				FloatingActionButton complete = (FloatingActionButton) view;
				complete.setEnabled(false);
			}
		}
		else
		{
			if (view != null) {
				FloatingActionButton complete = (FloatingActionButton) view;
				complete.setEnabled(true);
			}
		}
	}

	private boolean enableTaskCompleteButton() {
		String taskType = "";
		String taskStatus = "";
		MetrixCursor resultsCursor = null;
		MetrixCursor taskOptionsCursor = null;
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		String query = "select task_status, task_type from task where task_id = " + taskId;

		try {
			int completedCount = MetrixDatabaseManager.getCount("task_status_flow", "next_task_status = 'COMPLETED' and active = 'Y'");
			if (completedCount == 0)
				return true;

			resultsCursor = MetrixDatabaseManager.rawQueryMC(query, null);
			if (resultsCursor != null && resultsCursor.moveToFirst()) {
				while (resultsCursor.isAfterLast() == false) {
					taskStatus = resultsCursor.getString(0);
					taskType = resultsCursor.getString(1);
					resultsCursor.moveToNext();
				}
			}

			if (!MetrixStringHelper.isNullOrEmpty(taskType)) {
				// try to get next task status value(s) by the current task_type and task_status
				query = "select ts.task_status, ts.description, ts.desc_message_id, tsf.comment_required, tsf.comment_message, tsf.comment_message_id, tsf.confirm_required, tsf.confirm_message, tsf.confirm_message_id from task_status_flow tsf join task_status ts on ts.task_status = tsf.next_task_status where tsf.task_status = '" + taskStatus + "' and tsf.task_type = '" + taskType + "' and tsf.active = 'Y' order by ts.sequence, ts.description";
				taskOptionsCursor = MetrixDatabaseManager.rawQueryMC(query, null);
			}

			if (taskOptionsCursor == null || taskOptionsCursor.getCount() == 0) {
				// try to get next task status value(s) by the current task_status only (i.e. with null task_type)
				query = "select ts.task_status, ts.description, ts.desc_message_id, tsf.comment_required, tsf.comment_message, tsf.comment_message_id, tsf.confirm_required, tsf.confirm_message, tsf.confirm_message_id from task_status_flow tsf join task_status ts on ts.task_status = tsf.next_task_status where tsf.task_status = '" + taskStatus + "' and tsf.task_type is null and tsf.active = 'Y' order by ts.sequence, ts.description";
				taskOptionsCursor = MetrixDatabaseManager.rawQueryMC(query, null);
			}

			if (taskOptionsCursor == null || taskOptionsCursor.getCount() == 0) {
				// get all active task_status values (other than the current value)...
				//
				query = "select ts.task_status, ts.description, ts.desc_message_id, 'N' as comment_required, null as comment_message, null as comment_message_id, 'N' as confirm_required, null as confirm_message, null as confirm_message_id from task_status ts where ts.task_status <> '" + taskStatus + "' and ts.active = 'Y' order by ts.sequence, ts.description";
				taskOptionsCursor = MetrixDatabaseManager.rawQueryMC(query, null);
			}

			if (resultsCursor != null && resultsCursor.getCount() > 0) {
				if (taskOptionsCursor != null && taskOptionsCursor.moveToFirst()) {
					while (taskOptionsCursor.isAfterLast() == false) {
						String theTaskStatus = taskOptionsCursor.getString(0);

						if (!MetrixStringHelper.isNullOrEmpty(theTaskStatus) && theTaskStatus.equalsIgnoreCase("completed"))
						{
							return true;
						}
						taskOptionsCursor.moveToNext();
					}
				}
			}

			return false;

		} finally {
			if (resultsCursor != null) {
				resultsCursor.close();
			}

			if (taskOptionsCursor != null) {
				taskOptionsCursor.close();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mSignatureSigned = MetrixUIHelper.hasImage(signaturePreview);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		try {
			super.onSaveInstanceState(outState);
			if(MetrixUIHelper.hasImage(signaturePreview)) {
				mSignatureSigned = true;
				final String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
				final String fileName = filePath + "/" + "Signature.png";
				MetrixAttachmentManager.getInstance().savePicture(fileName, MetrixUIHelper.getBitmapFromImageView(signaturePreview), this);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle outState) {
		super.onRestoreInstanceState(outState);

		if (mSignatureRestore == true) {
			try {
				final String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
				final String fileName = filePath+"/"+"Signature.png";
				final Bitmap signImage = MetrixAttachmentManager.getInstance().loadPicture(fileName);
				Glide.with(this)
						.load(signImage)
						.into(signaturePreview);
			}
			catch(Exception ex){
				LogManager.getInstance().error(ex);
			}
		}
	}

	private void enableOrDisableTechnicianSignature() {
		String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_TECHNICIAN_SIGNATURE'");
		if (value.compareToIgnoreCase("N") == 0) {
			signaturePreview.setVisibility(View.GONE);
			mSignBelowBar.setVisibility(View.GONE);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

		MetrixTableDef taskDef = new MetrixTableDef("task", MetrixTransactionTypes.UPDATE);
		taskDef.constraints.add(new MetrixConstraintDef("task_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("task", "task_id"),
				double.class));

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
		try {
			Map<String, Double> totalExpenses = CalculateTotalExpenses();
			double totalLabor = CalculateTotalLabor();
			String partsUsed = IdentifyPartsUsed();

			//NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
			DecimalFormat currencyFormatter = MetrixFloatHelper.getCurrencyFormatter();
			DecimalFormat formatter = MetrixFloatHelper.getCurrencyFormatter();

			if (totalExpenses.size() > 0) {
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

			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "total_labor", MetrixFloatHelper.convertNumericFromForcedLocaleToUI(String.valueOf(totalLabor), Locale.US));
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "parts_used", partsUsed);
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "technician_full_name", User.getUser().firstName + " " + User.getUser().lastName);

			String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
			boolean updateTask = MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK");

			if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("Y") == 0) {
				if(updateTask) {
					Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);

					if (currentLocation != null) {
						try {
							MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "geocode_lat", Double.toString(currentLocation.getLatitude()));
							MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "geocode_long", Double.toString(currentLocation.getLongitude()));
						} catch (Exception e) {
							LogManager.getInstance(this).error(e);
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private Double CalculateTotalLabor() {
		double total = 0.0;
		String query = "select quantity from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code where line_code.npu_code = 'TIME' and non_part_usage.task_id = "
				+ MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 1;
				while (cursor.isAfterLast() == false) {
					double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
					total = total + quantity;

					cursor.moveToNext();
					i = i + 1;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return total;
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

	private String IdentifyPartsUsed() {
		StringBuilder partsUsed = new StringBuilder();
		String query = "select part.internal_descriptn, part_usage.quantity from part_usage, part where part_usage.part_id = part.part_id and part_usage.task_id = "
				+ MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id");

		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					String part = cursor.getString(0);
					int quantity = cursor.getInt(1);
					partsUsed.append(part);
					partsUsed.append(" (");
					partsUsed.append(quantity);
					partsUsed.append(")\n");

					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		String returnValue = partsUsed.toString();
		if (!MetrixStringHelper.isNullOrEmpty(returnValue) && returnValue.contains("\n"))
			returnValue = returnValue.substring(0, returnValue.lastIndexOf("\n"));

		return returnValue;
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mClearSignature = (ImageView) findViewById(R.id.clear_signature);
		//mPrintReportButton = (Button) findViewById(R.id.print_report);
		mCompleteButton = (FloatingActionButton) findViewById(R.id.next);
		mSignBelowBar = (RelativeLayout) findViewById(R.id.sign_below_bar);

		//resourceStrings.add(new ResourceValueObject(R.id.print_report, ""));
		AndroidResourceHelper.setResourceValues(mCompleteButton, "Complete");
		//AndroidResourceHelper.setResourceValues(mPrintReportButton, "PrintReport");

		//mPrintReportButton.setOnClickListener(this);
		mCompleteButton.setOnClickListener(this);
		mClearSignature.setOnClickListener(this);
		signaturePreview.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;

		switch (v.getId()) {
			/*
			case R.id.print_report:
				new Thread(new Runnable() {
					public void run() {
						Looper.prepare();
						printReport();
						Looper.loop();
						Looper.myLooper().quit();
					}
				}).start();
				break;
				*/
			case R.id.next:
				if (!taskIsComplete()) {
					if (taskCanBeCompleted()) {
						String confirm = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='CONFIRM_TASK_COMPLETION_IN_MOBILE'");
						if (confirm.compareToIgnoreCase("Y") == 0 && taskCanBeCompleted()) {
							AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setMessage(AndroidResourceHelper.getMessage("JobCompletionConfirm")).setPositiveButton((AndroidResourceHelper.getMessage("Yes")), dialogClickListener).setNegativeButton((AndroidResourceHelper.getMessage("No")), dialogClickListener).show();
						} else {
							saveAndFinish();
						}
					}
				}
				break;
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
				SignaturePad.openForResult(this, SIGNATURE_REQUEST_CODE);
				break;
			default:
				super.onClick(v);
		}
	}

	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
					String cfileName = filePath + "/" + "customerSignature.png"; // temporary customer signature
					String tfileName = filePath + "/" + "signature.png"; // temporary technician signature

					try {
						MetrixAttachmentManager.getInstance().deleteAttachmentFile(cfileName);
						MetrixAttachmentManager.getInstance().deleteAttachmentFile(tfileName);
					} catch (Exception ex) {
						LogManager.getInstance().error(ex);
					}

					saveAndFinish();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
			}
		}
	};

	private boolean taskCanBeCompleted() {
		Date startDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "plan_start_dttm"));
		Date endDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "plan_end_dttm"));
		if (startDate.after(endDate)) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("JobCompletionBadDates"));
			return false;
		}

		int count = MetrixDatabaseManager.getCount("task_steps", "task_id=" + MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id")
				+ " and required='Y' and completed='N'");
		if (count > 0) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("JobCompletionMissingSteps"));
			return false;
		}

		if (MeterReadingsPolicy.displayMessageForOutstandingReadings(MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"), this)) {
			return false;
		}

		return true;
	}

	public void saveAndFinish() {
		ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<MetrixSqlData>();
		String path = MetrixAttachmentManager.getInstance().getAttachmentPath();
		String filename;
		String timeStamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
		filename = "task_" + MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id") + "_" + "_technician_signature" + timeStamp;

		try {
			if (mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) {
			    
				File file = new File(path, "/" + filename + ".jpg");
				final Bitmap signature = MetrixUIHelper.getBitmapFromImageView(signaturePreview);
				this.mSignatureImage = MetrixUIHelper.applyBackgroundColorToBitmap(signature, Color.WHITE);
				if(!MetrixAttachmentManager.getInstance().saveBitmapImageAttachment(this.mSignatureImage, file, this))
					return;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				this.mSignatureImage.compress(Bitmap.CompressFormat.JPEG, 60, baos);
				String attachment_string = MetrixStringHelper.encodeBase64(baos.toByteArray());
				String signerName = MetrixControlAssistant.getValue(mFormDef, mLayout, "custom", "technician_full_name");

				MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
				DataField attachment_name = new DataField("attachment_name", file.getName());
				DataField attachment_description = new DataField("attachment_description", AndroidResourceHelper.getMessage("TechnicianSignature"));
				DataField signer = new DataField("signer", signerName);                        // M5.7 field
				DataField selectable = new DataField("selectable", "N");						// M5.7 field
				DataField internalType = new DataField("internal_type", "SIGNATURE");			// M5.7 field
				DataField file_extension = new DataField("file_extension", "jpg");
				DataField attachmentField = new DataField("attachment", attachment_string);
				DataField local_path = new DataField("mobile_path", file.getPath());
				int primaryKey = MetrixDatabaseManager.generatePrimaryKey("attachment");

				// only set the attachment type if 'SIGNATURE' exists as a value
				// option
				String type = MetrixDatabaseManager.getFieldStringValue("global_code_table", "code_value",
						"code_name = 'ATTACHMENT_TYPE' and code_value = 'SIGNATURE'");
				if (!MetrixStringHelper.isNullOrEmpty(type)) {
					DataField attachment_type = new DataField("attachment_type", "SIGNATURE");
					attachmentData.dataFields.add(attachment_type);
				}

				DataField attachment_id = new DataField("attachment_id", primaryKey);

				attachmentData.dataFields.add(attachment_name);
				attachmentData.dataFields.add(attachment_description);
				attachmentData.dataFields.add(file_extension);
				attachmentData.dataFields.add(signer);
				attachmentData.dataFields.add(selectable);
				attachmentData.dataFields.add(internalType);
				attachmentData.dataFields.add(attachmentField);
				attachmentData.dataFields.add(attachment_id);
				attachmentData.dataFields.add(local_path);

				MetrixSqlData taskAttachmentData = new MetrixSqlData("task_attachment", MetrixTransactionTypes.INSERT);
				DataField task_id = new DataField("task_id", MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"));
				taskAttachmentData.dataFields.add(task_id);
				taskAttachmentData.dataFields.add(attachment_id);

				attachmentTrans.add(attachmentData);
				attachmentTrans.add(taskAttachmentData);
			}

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");

			if (mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) {
				MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("TechnicianSignature"), this);
			}

			try {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "task_status", "COMPLETED");
				MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS));
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			String currentStatus = MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_status");
			MetrixTimeClockAssistant.updateAutomatedTimeClock(this, currentStatus);

			if (MetrixUpdateManager.update(this, mLayout, mFormDef, true, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("TaskCompletion"), true) == MetrixSaveResult.SUCCESSFUL) {

				String playSound = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_PLAY_SOUND'");

				if (playSound.compareToIgnoreCase("Y") == 0) {
					// User settings override the system setting
					if (SettingsHelper.getPlaySound(this)) {
						MediaPlayer player = MediaPlayer.create(this, R.raw.swoosh07);
						player.start();
					}
				}

				Intent intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} catch (Exception e) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, e.getMessage());
			LogManager.getInstance(this).error(e);
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

