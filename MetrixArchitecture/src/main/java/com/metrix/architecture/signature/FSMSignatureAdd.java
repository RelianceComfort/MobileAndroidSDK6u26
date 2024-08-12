package com.metrix.architecture.signature;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.R;
import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixDesignerResourceBaseData;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FSMSignatureAdd extends MetrixBaseActivity implements View.OnClickListener {
    private HashMap<String, Integer> resourceData;
    private FloatingActionButton[] fabs;
    private FloatingActionButton acceptBtn, clearBtn, cancelBtn;
    private com.github.gcacace.signaturepad.views.SignaturePad signaturePad;
    private String attachmentFileName;
    private boolean isFromList = false;
    private boolean isExistingFile;
    protected MetrixDesignerResourceBaseData mBaseResourceData;
    protected TextView mActionBarTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resourceData = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("FSMSignatureAddResources");

        setContentView(resourceData.get("R.layout.fsm_signature_add"));
        acceptBtn = (FloatingActionButton) findViewById(resourceData.get("R.id.acceptBtn"));
        clearBtn = (FloatingActionButton) findViewById(resourceData.get("R.id.clearBtn"));
        cancelBtn = (FloatingActionButton) findViewById(resourceData.get("R.id.cancelBtn"));
        signaturePad = findViewById(resourceData.get("R.id.signaturePad"));
        initialScreenSetup();
    }

    @Override
    protected void defineForm() {

    }

    @Override
    protected void defaultValues() {

    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == resourceData.get("R.id.acceptBtn")) {
            final Bitmap signature = signaturePad.getTransparentSignatureBitmap(true);
            if (signature != null){
                String fileName = SignatureWidgetManager.generateFileName() + ".jpg";
                baseMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
                String filePath = baseMediaUri.getPath();
                fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                attachmentFileName = fileName;
                if (attachmentFileName.compareTo("") != 0) {
                    filePath = MetrixAttachmentHelper.getFilePathFromAttachment(attachmentFileName);
                    if (MetrixAttachmentManager.getInstance().canFileBeSuccessfullySaved(signature.getByteCount())) {
                        MetrixAttachmentManager.getInstance().savePicture(filePath, signature, this);
                        isExistingFile = true;
                    }
                    if (this.save(filePath, isExistingFile))
                        finish();
                } else {
                    MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("NoUploadFileSelected"));
                }
            }
        } else if(viewId == resourceData.get("R.id.cancelBtn")) {
            if (signaturePad.isEmpty()) {
                finish();
            } else {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(AndroidResourceHelper.getMessage("LeaveSignatureScreenTitle"))
                        .setMessage(AndroidResourceHelper.getMessage("LeaveSignatureScreenMessage"))
                        .setPositiveButton(AndroidResourceHelper.getMessage("Leave"), (d, p) -> finish())
                        .setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null)
                        .show();
            }
        } else if(viewId == resourceData.get("R.id.clearBtn")) {
            signaturePad.clear();
        }
    }

    protected void setListeners() {

        acceptBtn.setOnClickListener(this);
        clearBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    @Override
    protected void setHyperlinkBehavior() {

    }

    @Override
    protected void displayPreviousCount() {

    }

    @Override
    protected void beforeStartForError() {

    }

    @Override
    protected void beforeUpdateForError() {

    }

    @Override
    public void resetFABOffset() {

    }

    @Override
    public void showIgnoreErrorDialog(String message, Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow) {

    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void initialScreenSetup() {
        setListeners();
        isExistingFile = false;
        acceptBtn.setEnabled(false);
        mBaseResourceData = (MetrixDesignerResourceBaseData) MetrixPublicCache.instance.getItem("FSMSignatureBaseResources");
        MetrixActionBarManager.getInstance().setupActionBar(this, mBaseResourceData.getActionBarLayoutID(), true);
        mActionBarTitle = MetrixActionBarManager.getInstance().setupActionBarTitle(this, mBaseResourceData.ActionBarTitleResourceID, mBaseResourceData.ActionBarTitleString, null);

        fabs = new FloatingActionButton[]{acceptBtn, clearBtn, cancelBtn};
        applyThemeToFABs();

        if (this.getResources().getConfiguration().orientation != 2) {
        if(getResources().getBoolean(R.bool.landscape_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        }
        signaturePad.setOnSignedListener(new com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                hideFABs();
            }

            @Override
            public void onSigned() {
                showFABs();
                acceptBtn.setEnabled(true);
                applyThemeToFABs();
            }

            @Override
            public void onClear() {
                acceptBtn.setEnabled(false);
                acceptBtn.setAlpha((float) 0.5);
            }
        });

        AndroidResourceHelper.setResourceValues(findViewById(resourceData.get("R.id.signatureLabel")), "PlaceSignatureAboveLine");
    }

    private void hideFABs() {
        for (FloatingActionButton fab : fabs) {
            fab.hide();
        }
    }

    protected void showFABs() {
        for (FloatingActionButton fab : fabs) {
            fab.show();
        }
    }

    private void applyThemeToFABs() {
        String buttonColorStr = MetrixSkinManager.getPrimaryColor();
        if (!MetrixStringHelper.isNullOrEmpty(buttonColorStr) && buttonColorStr.charAt(0) != '#')
            buttonColorStr = "#" + buttonColorStr;

        final int buttonColor = Color.parseColor(buttonColorStr);
        final int iconColor = ColorUtils.calculateLuminance(buttonColor) < 0.5 ? Color.WHITE : Color.BLACK;

        for (FloatingActionButton fab : fabs) {
            fab.setBackgroundTintList(ColorStateList.valueOf(buttonColor));
            fab.setColorFilter(iconColor);
            fab.setRippleColor(iconColor);
            if (!acceptBtn.isEnabled())
                acceptBtn.setAlpha((float) 0.5);
        }
    }

    private boolean save(String fileName, boolean isExistingFile) {
        try {
            File file = new File(fileName);
            if (file.length() <= 0) {
                MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("FileContentEmpty"));
                return false;
            }

            String[] filePieces = fileName.split("\\.");
            String fileExtension = "";
            if (filePieces.length > 1) {
                fileExtension = filePieces[filePieces.length - 1];
            }

            if (!isExistingFile) {
                if (file.exists()) {
                    //region #Attempt a file copy to the attachment folder
                    MetrixAttachmentManager metrixAttachmentManager = MetrixAttachmentManager.getInstance();
                    String copyPath = metrixAttachmentManager.getAttachmentPath() + "/" + file.getName();
                    try {
                        boolean success;
                        if (MetrixAttachmentHelper.checkImageFile(file.getPath())) {
                            success = MetrixAttachmentHelper.resizeImageFile(file.getPath(), copyPath, this);
                            if (!success)
                                return false;
                        } else {
                            if (!copyPath.equals(fileName)) {
                                success = MetrixAttachmentManager.getInstance().copyFileToNewLocation(file, new File(copyPath), this);
                                if (!success)
                                    return false;
                            }
                        }
                        fileName = copyPath;
                    } catch (Exception e) {
                        LogManager.getInstance(this).error(e);
                        throw new Exception("Error occurred while attaching an existing file ");
                    }
                    //endregion
                }
            }

            DataField attachmentIdDataField = null;
            boolean shouldUploadAttachment = true;
            String attachmentIdToUse = "";
            if (isExistingFile) {
                // If we are working with an existing attachment, then don't try to insert an attachment row again.
                attachmentIdToUse = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_id", "attachment_name = '" + file.getName() + "'");
                if (!MetrixStringHelper.isNullOrEmpty(attachmentIdToUse)) {
                    attachmentIdDataField = new DataField("attachment_id", attachmentIdToUse);
                    shouldUploadAttachment = false;
                }
            }

            SignatureField signatureField = SignatureWidgetManager.getAttachmentField();
            ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<>();
            if (shouldUploadAttachment && signatureField != null) {

                //region #Set up Attachment INSERT MetrixSqlData
                MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
                attachmentIdToUse = String.valueOf(MetrixDatabaseManager.generatePrimaryKey("attachment"));
                attachmentIdDataField = new DataField("attachment_id", attachmentIdToUse);
                attachmentData.dataFields.add(attachmentIdDataField);
                attachmentData.dataFields.add(new DataField("attachment_name", file.getName()));
                attachmentData.dataFields.add(new DataField("created_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
                attachmentData.dataFields.add(new DataField("file_extension", fileExtension));
                attachmentData.dataFields.add(new DataField("mobile_path", fileName));  // mobile_path is the local path of the attachment file
                attachmentData.dataFields.add(new DataField("stored", "N"));
                attachmentData.dataFields.add(new DataField("internal_type", "SIGNATURE"));
                attachmentData.dataFields.add(new DataField("signer", signatureField.signerFieldValue));
                String description = "";
                if(!MetrixStringHelper.isNullOrEmpty(signatureField.signerMessageId))
                    description = AndroidResourceHelper.getMessage(signatureField.signerMessageId);
                else
                    description = SignatureWidgetManager.getParentTable() + " " + AndroidResourceHelper.getMessage("Signature");
                attachmentData.dataFields.add(new DataField("attachment_description", description));
                //endregion

                attachmentTrans.add(attachmentData);
            }

            boolean successful = false;
            if (isFromList) {
                //region #Generate link table INSERT MetrixSqlData and SAVE the transaction
                LinkedHashMap<String, Object> keyMap = SignatureWidgetManager.getKeyNameValueMap();
                if (keyMap == null || keyMap.isEmpty())
                    successful = false;
                else {
                    MetrixSqlData linkTableData = new MetrixSqlData(AttachmentWidgetManager.getLinkTable(), MetrixTransactionTypes.INSERT);
                    linkTableData.dataFields.add(attachmentIdDataField);
                    for (Map.Entry<String, Object> row : keyMap.entrySet()) {
                        String columnName = row.getKey();
                        String columnValue = AttachmentWidgetManager.convertKeyValueToDBString(columnName, row.getValue());
                        linkTableData.dataFields.add(new DataField(columnName, columnValue));
                    }
                    attachmentTrans.add(linkTableData);

                    MetrixTransaction transactionInfo = AttachmentWidgetManager.getTransactionInfo();
                    successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);
                }
                //endregion
            } else {
                //region #Save the attachment transaction (if there is one), pass the Attachment ID into the Signature Field, and navigate back
                successful = true;
                if (shouldUploadAttachment) {
                    MetrixTransaction transactionInfo = SignatureWidgetManager.getTransactionInfo();
                    successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);
                }

                if (successful) {
                    // Update Signature Field UI if we successfully added the attachment OR we grabbed an existing attachment
                    if (signatureField != null) {
                        signatureField.mHiddenAttachmentIdTextView.setText(attachmentIdToUse);
                        signatureField.updateFieldUI();
                        if(launchingSignatureField != null) {
                            if(signatureField.isSurvey()) {
                                launchingSignatureField.setSurveyQuestionId(signatureField.getSurveyQuestionId());
                            }
                        }
                    }
                }
                //endregion
            }

            if (successful && shouldUploadAttachment) {
                // We successfully saved a new signature which presently has a negative Attachment ID.
                // Upsert a row in mm_attachment_id_map accordingly, so that Sync will provide the positive key when the time comes.
                MetrixDatabaseManager.executeSql(String.format("insert or replace into mm_attachment_id_map (negative_key, positive_key) values (%s, NULL)", attachmentIdToUse));
            }

            if (!successful) {
                MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("DataErrorOnUpload"));
                return false;
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
            return false;
        }
        return true;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected void toggleOrientationLock(boolean doLock) {
        // if !doLock AND cache indicates that we've lock orientation previously, unlock it
        // otherwise if doLock, then lock orientation AND cache that we have done so
        // the caching should prevent useless setRequestedOrientation calls
        if (!doLock && MetrixPublicCache.instance.containsKey("METRIX_ORIENTATION_LOCK_ACTIVE")) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            MetrixPublicCache.instance.removeItem("METRIX_ORIENTATION_LOCK_ACTIVE");
        } else if (doLock) {
            Display display = this.getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();
            int height;
            int width;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
                width = display.getWidth();
                height = display.getHeight();
            } else {
                Point size = new Point();
                display.getSize(size);
                width = size.x;
                height = size.y;
            }

            switch (rotation) {
                case Surface.ROTATION_90:
                    if (width > height)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_180:
                    if (height > width)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
                case Surface.ROTATION_270:
                    if (width > height)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                default :
                    if (height > width)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            MetrixPublicCache.instance.addItem("METRIX_ORIENTATION_LOCK_ACTIVE", true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected int getCoordinatorLayoutID() { return mBaseResourceData.ExtraResourceIDs.get("R.id.coordinator_layout"); }
}
