package com.metrix.metrixmobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixTimeClockAssistant;
import com.metrix.metrixmobile.system.QuoteSignatureBase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

public class QuoteCustomerSignature extends QuoteSignatureBase implements View.OnClickListener {
	FloatingActionButton mSaveButton, mNextButton, mCompleteButton;
	private RelativeLayout mSignBelowBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			mSignatureRestore = true;
		}
		else {
			mSignatureSigned = false;
			mSignatureSaved = false;
		}

		signatureFileName = "customerSignature.png";
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quote_customer_signature);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.SignBelowe3dc4fa1, "TapBelowToSign"));

		this.quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		this.quoteVersion = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

		super.onStart();

		LinearLayout outerLayout = findViewById(R.id.outer_layout);
		outerLayout.setPadding(outerLayout.getPaddingLeft(), outerLayout.getPaddingRight(), outerLayout.getPaddingTop(), mSplitActionBarHeight);

		resourceStrings.add(new ResourceValueObject(R.id.next, "Accept"));
		AndroidResourceHelper.setResourceValues(mCurrentActivity, resourceStrings);

		if (quoteIsAccepted()) {
			this.setCompleteEnabled();
		}
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		super.setListeners();
		mClearSignature = (ImageView) findViewById(R.id.clear_signature);
		mCompleteButton = (FloatingActionButton) findViewById(R.id.next);
		mSignBelowBar = (RelativeLayout) findViewById(R.id.sign_below_bar);

		mCompleteButton.setOnClickListener(this);
		mClearSignature.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;
		this.quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		this.quoteVersion = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

		switch (v.getId()) {
            case R.id.next:
                if (!quoteIsAccepted()) {
                    if (this.quoteCanBeCompleted()) {
                        String confirm = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='CONFIRM_QUOTE_ACCEPTANCE_IN_MOBILE'");
                        if (!MetrixStringHelper.isNullOrEmpty(confirm) && confirm.compareToIgnoreCase("Y") == 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(AndroidResourceHelper.getMessage("QuoteAcceptConfirm")).setPositiveButton(AndroidResourceHelper.getMessage("Yes"),
                                    dialogClickListener).setNegativeButton((AndroidResourceHelper.getMessage("No")), dialogClickListener).show();
                        } else {
                            saveAndFinish();
                        }
                    } else {
                        MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("DataPreviousUnsyncedQuote"));
                        return;
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
					String cfileName = filePath+"/"+"customerSignature.png"; // temporary customer signature
					String tfileName = filePath+"/"+"signature.png"; // temporary technician signature

					try {
						MetrixAttachmentManager.getInstance().deleteAttachmentFile(cfileName);
						MetrixAttachmentManager.getInstance().deleteAttachmentFile(tfileName);
					} catch(Exception ex){
						LogManager.getInstance().error(ex);
					}

					saveAndFinish();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
			}
		}
	};


	public void setCompleteEnabled()
	{
		View view = findViewById(R.id.next);
		if (quoteIsAccepted()) {
			if (view != null) {
				FloatingActionButton complete = (FloatingActionButton) view;
				complete.setEnabled(false);
			}
		}
		else
		{
			if (view != null) {
				Button complete = (Button) view;
				complete.setEnabled(true);
			}
		}
	}

	public void saveAndFinish() {
		ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<MetrixSqlData>();
		String path = MetrixAttachmentManager.getInstance().getAttachmentPath();
		String filename;
		String timeStamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
		String quoteTempId = "";
		if(MetrixStringHelper.isNullOrEmpty(this.quoteId) || MetrixStringHelper.isNegativeValue(this.quoteId))
			quoteTempId = "";
		else
			quoteTempId = this.quoteId;

		filename = "quote_" + quoteTempId + "_" + "_customer_signature_" + timeStamp;

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
				//String signerName = MetrixControlAssistant.getValue(mFormDef, mLayout, "custom", "technician_full_name");

				MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
				DataField attachment_name = new DataField("attachment_name", file.getName());
				DataField attachment_description = new DataField("attachment_description", AndroidResourceHelper.getMessage("QuoteCustomerSignature"));
				//DataField signer = new DataField("signer", signerName);						// M5.7 field
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
				//attachmentData.dataFields.add(signer);
				attachmentData.dataFields.add(selectable);
				attachmentData.dataFields.add(internalType);
				attachmentData.dataFields.add(attachmentField);
				attachmentData.dataFields.add(attachment_id);
				attachmentData.dataFields.add(local_path);

				MetrixSqlData quoteAttachmentData = new MetrixSqlData("quote_attachment", MetrixTransactionTypes.INSERT);
				DataField quote_id = new DataField("quote_id", this.quoteId);
				quoteAttachmentData.dataFields.add(quote_id);
				DataField quote_version = new DataField("quote_version", this.quoteVersion);
				quoteAttachmentData.dataFields.add(quote_version);
				quoteAttachmentData.dataFields.add(attachment_id);

				attachmentTrans.add(attachmentData);
				attachmentTrans.add(quoteAttachmentData);
			}

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");

			if (mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) {
				MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("QuoteCustomerSignature"), this);
			}

			try {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "quote", "quote_status", "ACCEPTED");
				MetrixControlAssistant.setValue(mFormDef, mLayout, "quote", "quote_status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS));
				MetrixControlAssistant.setValue(mFormDef, mLayout, "quote", "quote_owner", User.getUser().personId);
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			String currentStatus = MetrixControlAssistant.getValue(mFormDef, mLayout, "quote", "quote_status");
			MetrixTimeClockAssistant.updateAutomatedTimeClock(this, currentStatus);

			if (MetrixUpdateManager.update(this, mLayout, mFormDef, true, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("QuoteCompletion"), true) == MetrixSaveResult.SUCCESSFUL) {

				String playSound = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_PLAY_SOUND'");

				if (playSound.compareToIgnoreCase("Y") == 0) {
					// User settings override the system setting
					if (SettingsHelper.getPlaySound(this)) {
						MediaPlayer player = MediaPlayer.create(this, R.raw.swoosh07);
						player.start();
					}
				}

				Intent intent = MetrixActivityHelper.createActivityIntent(this, QuoteList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} catch (Exception e) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, e.getMessage());
			LogManager.getInstance(this).error(e);
		}
	}
	//End Tablet UI Optimization
}

