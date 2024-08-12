package com.metrix.metrixmobile;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.QuoteSignatureBase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QuoteSignature extends QuoteSignatureBase implements View.OnClickListener, OnFocusChangeListener {
	FloatingActionButton mSaveButton, mNextButton, mSaveImageButton;
	//	private Bitmap mSignatureImage;
//	//private EditText mStartDate, mEndDate;
//	private ImageView mClearSignature;
//	private SignatureArea mSignatureArea = null;
//	private boolean mSignatureRestore = false;
//	private static boolean mSignatureSigned = false; // This flag will track Signed status for orientation change
	private MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private RelativeLayout mSignBelowBar;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			mSignatureRestore = true;
		}
		else {
			mSignatureSigned = false;
		}

		signatureFileName = "Signature.png";
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quote_signature);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		this.quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		this.quoteVersion = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

		resourceStrings.add(new ResourceValueObject(R.id.SignBelow41d5a802, "TapBelowToSign"));
		resourceStrings.add(new ResourceValueObject(R.id.alreadySavedMessage, "AlreadySavedMessage"));

		super.onStart();

		AndroidResourceHelper.setResourceValues(mCurrentActivity, resourceStrings);

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		LinearLayout outerLayout = findViewById(R.id.outer_layout);
		outerLayout.setPadding(outerLayout.getPaddingLeft(), outerLayout.getPaddingRight(), outerLayout.getPaddingTop(), mSplitActionBarHeight);

		String[] tableNames = new String[]{"attachment", "quote_attachment"};
		String whereClause = "attachment.attachment_id = quote_attachment.attachment_id and quote_id = '" + quoteId + "' and attachment_name like '%technician_signature%'";
		mSignatureSaved = MetrixDatabaseManager.getCount(tableNames, whereClause) > 0;
		updateControlState();

	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		super.setListeners();
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mClearSignature = (ImageView) findViewById(R.id.clear_signature);
		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mClearSignature.setOnClickListener(this);
		mSaveButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		mScrollView = (NestedScrollView) findViewById(R.id.scroll_view);
		mSignatureInformationArea = (RelativeLayout) findViewById(R.id.cus_signature_options_area);
		mSplitActionBar = (RelativeLayout) findViewById(R.id.split_action_bar);
		mCusSignatureAlreadySaved = (RelativeLayout) findViewById(R.id.cus_signature_already_saved);

		mFABList.add(mSaveButton);
		mFABList.add(mNextButton);
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

		MetrixTableDef quoteDef = new MetrixTableDef("quote", MetrixTransactionTypes.UPDATE);
		quoteDef.constraints.add(new MetrixConstraintDef("quote_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"),
				double.class));

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
		try {
			super.defaultValues();
			MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "technician_full_name", User.getUser().firstName + " " + User.getUser().lastName);

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private String IdentifyPartsUsed() {
		StringBuilder partsUsed = new StringBuilder();
		String query = "select part.internal_descriptn, quote_part_usage.quantity from quote_part_usage, part where quote_part_usage.part_id = part.part_id and quote_part_usage.quote_id = "
				+ MetrixControlAssistant.getValue(mFormDef, mLayout, "quote", "quote_id");

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

	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;
		this.quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		this.quoteVersion = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

		switch (v.getId()) {
			case R.id.save:
				if(mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) {
					if(mSignatureSaved == false) {
						this.save();
						if (mSignatureSaved)
							updateControlState();
					}
					else {
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("AlreadySavedMessage"));
						return;
					}
				} else {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PleaseSign"));
					return;
				}

				break;
			case R.id.next:
				if ((mSignatureSigned || MetrixUIHelper.hasImage(signaturePreview)) && mSignatureSaved == false) {
					//if the task is completed, the signature will not be saved even if the user signs in the signature area/edit an existing signature
					if (!quoteIsAccepted() && mSaveButton.isEnabled()) {
						this.save();
					}
				}

				this.mScreenLefted = true;
				this.signaturePreview.setImageBitmap(null);
				mSignatureSigned = false;
				mSignatureSaved = false;
				MetrixWorkflowManager.advanceWorkflow(this);

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
			default:
				super.onClick(v);
		}
	}

	OnClickListener dialogClickListener = new OnClickListener() {
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

					save();
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
			}
		}
	};

	public void save() {
		String path = MetrixAttachmentManager.getInstance().getAttachmentPath();

		String filename;
		String timeStamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
		String quoteTempId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");;
		if(MetrixStringHelper.isNullOrEmpty(quoteTempId) || MetrixStringHelper.isNegativeValue(quoteTempId))
			quoteTempId = "";

		filename = "quote_" + quoteTempId + "_technician_signature_" + timeStamp;

		try {
			File file = new File(path, "/" + filename + ".jpg");
			final Bitmap signature = MetrixUIHelper.getBitmapFromImageView(signaturePreview);
			this.mSignatureImage = MetrixUIHelper.applyBackgroundColorToBitmap(signature, Color.WHITE);
			if(!MetrixAttachmentManager.getInstance().saveBitmapImageAttachment(this.mSignatureImage, file, this))
				return;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			this.mSignatureImage.compress(Bitmap.CompressFormat.JPEG, 85, baos);
			String attachment_string = MetrixStringHelper.encodeBase64(baos.toByteArray());
			String signerName = MetrixControlAssistant.getValue(mFormDef, mLayout, "custom", "customer_full_name");

			MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
			DataField attachment_name = new DataField("attachment_name", file.getName());
			DataField attachment_description = new DataField("attachment_description", AndroidResourceHelper.getMessage("QuoteTechnicianSignature"));
			DataField file_extension = new DataField("file_extension", "jpg");
			DataField signer = new DataField("signer", signerName);
			DataField selectable = new DataField("selectable", "N");						// M5.7 field
			DataField internalType = new DataField("internal_type", "SIGNATURE");			// M5.7 field
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

			MetrixSqlData quoteAttachmentData = new MetrixSqlData("quote_attachment", MetrixTransactionTypes.INSERT);
			DataField quote_id = new DataField("quote_id", this.quoteId);
			quoteAttachmentData.dataFields.add(quote_id);
			DataField quote_version = new DataField("quote_version", this.quoteVersion);
			quoteAttachmentData.dataFields.add(quote_version);
			quoteAttachmentData.dataFields.add(attachment_id);

			ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<MetrixSqlData>();
			attachmentTrans.add(attachmentData);
			attachmentTrans.add(quoteAttachmentData);

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
			mSignatureSaved = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("QuoteTechnicianSignature"), this);
		} catch (Exception e) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ErrorSavingAttachmentUnknown"));
			LogManager.getInstance(this).error(e);
		}
	}

	private Bitmap GetSignatureBitmap(String attachment_description) {
		Bitmap bmp = null;

		// get the task id so we get correct signature
		String quote_id = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");

		// get list of attachments for this quote
		StringBuilder query = new StringBuilder();
		query.append("select attachment_id from quote_attachment where quote_id='" + quote_id + "'");

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			// loop through each attachment until we find the customer signature
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					String attachment_id = cursor.getString(0);

					// not ideal using the attachment description, but currently no other way
					String file_name = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_name", "attachment_id='" + attachment_id
							+ "' AND attachment_description='" + attachment_description + "'");

					// did we get a good filename?
					if (!MetrixStringHelper.isNullOrEmpty(file_name) && file_name.length() > 0) {
						// add /sdcard/ as default folder
						String path = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + file_name;

						// make sure we get to the file and path
						File attachmentFile = new File(path);
						if (attachmentFile.exists()) {
							// create the bitmap from the file (note: it returns an immutable copy)
							Bitmap tempBmp = BitmapFactory.decodeFile(path);

							// make it so we can write to it so we can get rid of background color
							bmp = tempBmp.copy(Bitmap.Config.ARGB_8888, true);

							// get rid of the light gray background (looks bad when it prints, otherwise)
							ChangeColorsInBitmap(bmp, Color.LTGRAY, Color.WHITE);
							break;
						}
					}

					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return bmp;
	}

	private void ChangeColorsInBitmap(Bitmap myBitmap, int oldColor, int newColor) {
		int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];

		myBitmap.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());

		for (int i = 0; i < myBitmap.getHeight() * myBitmap.getWidth(); i++) {
			if (allpixels[i] == oldColor)
				allpixels[i] = newColor;
		}

		myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
	}

	private void updateControlState() {

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
}

