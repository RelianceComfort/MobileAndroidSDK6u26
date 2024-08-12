package com.metrix.metrixmobile;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.policies.AttachmentPolicy;

import java.io.File;
import java.util.ArrayList;

@SuppressLint("DefaultLocale")
public class QuoteQuoteAttachmentAdd extends QuoteQuoteAttachmentActivity implements View.OnClickListener, View.OnLongClickListener {

	private FloatingActionButton mCancelButton, mCustomSaveButton;
	private EditText mAttachmentFile;
	private static Uri mMediaUri;
	//final static private String APP_KEY = "cm80v0gwnkjl7ba";
	//final static private String APP_SECRET = "2ht2lc5l5deye6l";
	private boolean isExistingFile;
	private ImageView imageDisplay;

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.quote_quote_attachment);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();

		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		Bundle extras = getIntent().getExtras();
		if (extras != null)
			initialScreenSetup(extras);

	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {

		mCustomSaveButton = (FloatingActionButton) findViewById(R.id.custom_save);
		mCancelButton = (FloatingActionButton) findViewById(R.id.cancel);

		mAttachmentFile = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "attachment_file");

		mCustomSaveButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);

		imageDisplay = (ImageView) findViewById(R.id.showImage);
		imageDisplay.setOnLongClickListener(this);

	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		try {
			MetrixControlAssistant.setValue(mFormDef, mLayout, "quote_attachment", "quote_id", MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"));
			MetrixControlAssistant.setValue(mFormDef, mLayout, "quote_attachment", "quote_version", MetrixCurrentKeysHelper.getKeyValue("quote", "task_version"));
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef taskAttachmentDef = new MetrixTableDef("quote_attachment", MetrixTransactionTypes.INSERT);
		this.mFormDef = new MetrixFormDef(taskAttachmentDef);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@SuppressLint({ "SdCardPath", "DefaultLocale" })
	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			//region : execute the actions are related to save/cancel button
			case R.id.custom_save:
                String fileName = mAttachmentFile.getText().toString();
                String filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);

				if (filePath.compareTo("") != 0) {
					this.save(filePath, isExistingFile);
					finish();

				} else {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoUploadFileSelected"));
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

	public void save(String fileName, boolean isExistingFile) {
		// Environment.getExternalStorageDirectory().toString();
		String attachment_string = "";
		if (fileName == null || fileName.length() <= 0) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("SelectUploadFile"));
			return;
		}

		try {
			File file = new File(fileName);

			if (file.length() <= 0) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FileContentEmpty"));
				return;
			}

			String[] filePieces = fileName.split("\\.");
			String fileExtension = "";

			if (filePieces.length > 1) {
				fileExtension = filePieces[filePieces.length - 1];
			}

			if (!isExistingFile) {
				if (file.exists()) {
					MetrixAttachmentManager metrixAttachmentManager = MetrixAttachmentManager.getInstance();
					String copyPath = metrixAttachmentManager.getAttachmentPath() + "/" + file.getName();
					try {
						boolean success;
						if (MetrixAttachmentHelper.checkImageFile(file.getPath())) {
							success = MetrixAttachmentHelper.resizeImageFile(file.getPath(), copyPath, this);
							if (!success)
								return;
						} else {
							if (!copyPath.equals(fileName)) {
								success = MetrixAttachmentManager.getInstance().copyFileToNewLocation(file, new File(copyPath), this);
								if (!success)
									return;
							}
						}
						fileName = copyPath;
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
						throw new Exception("Error occurred while attaching an existing file ");
					}
				}
			}

			DataField store = new DataField("stored", "N");
			MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
			DataField attachment_name = new DataField("attachment_name", file.getName());
			// mobile_path is the local path of the attachment file
			DataField local_path = new DataField("mobile_path", fileName);
			DataField file_extension = new DataField("file_extension", fileExtension);
			MetrixAttachmentManager.getInstance().saveAttachmentStringToFile(attachment_string, fileName+".tmp");

			DataField attachment_id = null;
			boolean shouldUploadAttachment = true;
			if (isExistingFile) {
				final String oldAttachmentId = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_id", "attachment_name = '" + file.getName() + "'");
				if (!MetrixStringHelper.isNullOrEmpty(oldAttachmentId)) {
					attachment_id = new DataField("attachment_id", oldAttachmentId);
					shouldUploadAttachment = false;
				}
			}

			if (attachment_id == null) {
				int primaryKey = MetrixDatabaseManager.generatePrimaryKey("attachment");
				attachment_id = new DataField("attachment_id", primaryKey);
			}

			attachmentData.dataFields.add(attachment_name);
			View descCtrl = MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "attachment_description");
			if (descCtrl != null) {
				String description = MetrixControlAssistant.getValue(descCtrl);
				attachmentData.dataFields.add(new DataField("attachment_description", description));
			}
			View typeCtrl = MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "attachment_type");
			if (typeCtrl != null) {
				String attachType = MetrixControlAssistant.getValue(typeCtrl);
				attachmentData.dataFields.add(new DataField("attachment_type", attachType));
			}
			attachmentData.dataFields.add(file_extension);
			attachmentData.dataFields.add(store);
			attachmentData.dataFields.add(attachment_id);
			attachmentData.dataFields.add(local_path);

			boolean successful = false;

			MetrixSqlData quoteAttachmentData = new MetrixSqlData("quote_attachment", MetrixTransactionTypes.INSERT);
			DataField quote_id = new DataField("quote_id", MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"));
			DataField quote_version = new DataField("quote_version", MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version"));
			quoteAttachmentData.dataFields.add(quote_id);
			quoteAttachmentData.dataFields.add(quote_version);
			quoteAttachmentData.dataFields.add(attachment_id);

			ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<MetrixSqlData>();
			if (shouldUploadAttachment)
				attachmentTrans.add(attachmentData);
			attachmentTrans.add(quoteAttachmentData);

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");

			successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);

			if (!successful) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("DataErrorOnUpload"));
				return;
			}

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

	}

	public void next() {
		MetrixWorkflowManager.advanceWorkflow(this);
	}


	/***
	 * This method was introduced, since the same activity is rendered in two different ways to cater two different scenarios.
	 * 1. You can visit this screen by the default way. Ex: coming via workFlow[That time, it won't have any value in the activity bundle]
	 * 2. You can visit here from Attachments Gallery screen[using quick links] either by pressing camera or file buttons[This time if will have a boolean value to represent whether it comes via camera or file browser].
	 * Depending on these situations this method will show/hide necessary views.
	 * @param extras
	 */
	private void initialScreenSetup(Bundle extras) {
		try {

			isExistingFile = false;

			Object extrasCameraValue = extras.get("FromCamera");
			Object extrasVideoCameraValue = extras.get("FromVideoCamera");
			Object extrasFileDialogValue = extras.get("FromFileDialog");

			String fileName = "";
			String filePath = "";

			if (extrasCameraValue != null){
				mMediaUri = (Uri) extras.get("ImageUri");
				filePath = mMediaUri.getPath();
				fileName = filePath.substring(filePath.lastIndexOf("/")+1);
				MetrixFileHelper.copyFileUriToPrivate(this, mMediaUri);
				filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
			}
			else if (extrasVideoCameraValue != null){
				mMediaUri = (Uri) extras.get("VideoUri");
				filePath = mMediaUri.getPath();
				fileName = filePath.substring(filePath.lastIndexOf("/")+1);
				MetrixFileHelper.copyFileUriToPrivate(this, mMediaUri);
				filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
			}
			else if(extrasFileDialogValue != null){
				String fileOriginalPath = extras.getString("FileName");
				fileName = fileOriginalPath.substring(fileOriginalPath.lastIndexOf("/")+1);
				filePath  = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);

				// OSP-3443: We might be able to remove this because we are saving files to the private
				//  directory and our File Picker currently doesn't get access to other files beside
				//  our own
				// Since we copy the original attachment to FSM managed attachment directory,
                // verify we can do that first
				if (MetrixStringHelper.valueIsEqual(fileOriginalPath, filePath))
					isExistingFile = true; // User has selected a file from the FSM managed attachment directory. No need to copy the file again.
				else {
					final File attachmentNewFile = new File(filePath);
					final File attachmentOldFile = new File(fileOriginalPath);
					if (!MetrixAttachmentManager.getInstance().copyFileToNewLocation(attachmentOldFile, attachmentNewFile, this))
						return;
				}
			}
			if (MetrixStringHelper.isNullOrEmpty(fileName))return;

			mAttachmentFile.setText(fileName);

			String validationMessage = AttachmentPolicy.attachmentIsValid(filePath);
			if (!MetrixStringHelper.isNullOrEmpty(validationMessage)) {
				MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, validationMessage);
				mAttachmentFile.setText("");
				mMediaUri = null;
				return;
			}

			ImageView imageViewVideoIcon = (ImageView) findViewById(R.id.imageview_video_icon);

			MetrixAttachmentHelper.showAttachmentFullScreenPreview(getApplicationContext(), filePath, imageDisplay);
			MetrixAttachmentHelper.setVideoIcon(imageViewVideoIcon, filePath, false);

		}catch (Exception e) {
            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ErrorSavingAttachmentUnknown"));
			LogManager.getInstance(this).error(e);
		}
	}

	@Override
	public boolean onLongClick(View v) {

		final String filePath = mAttachmentFile.getText().toString();
		if (!MetrixStringHelper.isNullOrEmpty(filePath)) {

			if (MetrixAttachmentHelper.checkImageFile(filePath)) {
				DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						try {
							modifyAction(filePath);
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					}
				};

				MetrixDialogAssistant.showModifyDialog(AndroidResourceHelper.getMessage("AttachmentLCase"), yesListener, null, this);
			}
		}
		return false;
	}
}

