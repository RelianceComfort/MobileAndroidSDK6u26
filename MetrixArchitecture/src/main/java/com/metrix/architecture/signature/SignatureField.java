package com.metrix.architecture.signature;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_TAKE_SIGNATURE;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.launchingSignatureField;

public class SignatureField extends LinearLayout implements View.OnClickListener, View.OnLongClickListener{
    public TextView mHiddenAttachmentIdTextView;

    private static HashMap<String, Integer> resourceCache;

    private WeakReference<Activity> activityInfo;

    private boolean fieldIsEnabled;
    private boolean fieldIsReadOnlyInMetadata;

    private boolean allowClear = false;
    private MetrixColumnDef signerColumnDef;
    private String fieldTableName = "";
    private String transactionIdTableName = "";
    private String transactionIdColumnName = "";
    public ViewGroup parentLayout;
    public MetrixFormDef metrixFormDef;
    public MetrixColumnDef metrixColumnDef;
    public String signerFieldValue;
    public String signerMessageId;
    public boolean required;

    private ImageView signatureView;
    private TextView signatureLabel;
    private Context context;
    private Drawable signerBackground;
    private boolean isSurvey = false;
    private String surveyQuestionId;

    public SignatureField(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        if (resourceCache == null)
            resourceCache = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("SignatureFieldResources");

        int layoutId = resourceCache.get("R.layout.signature_field");
        inflate(context, layoutId, this);

        signatureView = findViewById(resourceCache.get("R.id.signature_view"));
        signatureLabel = findViewById(resourceCache.get("R.id.signature_label"));

        // Set up a reference to a hidden TextView that tracks Attachment ID.
        // Using a textview like this allows for value changed event handling according to existing pattern.
        mHiddenAttachmentIdTextView = new TextView(context);
        mHiddenAttachmentIdTextView.setVisibility(View.GONE);
    }

    @Override
    public boolean isEnabled() {
        return fieldIsEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // Only allow enabled state to be toggled if field is not read-only in metadata
        if (!fieldIsReadOnlyInMetadata) {
            fieldIsEnabled = enabled;
            updateFieldUI();
        }
    }

    public String getFieldTableName() { return fieldTableName; }
    public String getTransactionIdTableName() { return transactionIdTableName; }
    public String getTransactionIdColumnName() { return transactionIdColumnName; }

    public void setupFromConfiguration(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, MetrixColumnDef columnDef, String tableName) {
        // Useful when current screen is in INSERT mode and the field value won't be set.
        parentLayout = layout;
        this.metrixFormDef = metrixFormDef;
        this.metrixColumnDef = columnDef;
        setupFromConfiguration(activity, columnDef, tableName);
    }

    public void setupFromConfiguration(Activity activity, MetrixColumnDef columnDef, String tableName) {
        fieldIsReadOnlyInMetadata = columnDef.readOnlyInMetadata;
        fieldIsEnabled = !fieldIsReadOnlyInMetadata;

        activityInfo = new WeakReference<>(activity);

        allowClear = columnDef.allowClear;
        required = columnDef.required;
        fieldTableName = tableName;
        transactionIdTableName = columnDef.transactionIdTableName;
        transactionIdColumnName = columnDef.transactionIdColumnName;
        signerMessageId = columnDef.messageId;
        SignatureWidgetManager.setParentTableFromField(fieldTableName);
        String labelValue = required ? AndroidResourceHelper.getMessage("TapToSignRequired") : AndroidResourceHelper.getMessage("TapToSign");
        signatureLabel.setText(labelValue);
        String controlId = "SignatureField_"+tableName+"__"+columnDef.columnName;

        if(fieldIsEnabled) {
            signatureView.setOnClickListener(this::onClick);
            setSignerFieldState(signerColumnDef, false);
        } else {
            signerColumnDef = GetSignerColumnDef(metrixFormDef, metrixColumnDef.signerColumn);
            setSignerFieldState(signerColumnDef, true);
            signatureView.setOnClickListener(null);
        }

        this.setContentDescription(controlId);
        signatureView.setContentDescription(controlId+"_imageView");
        updateFieldUI();
    }

    public void updateFieldUI() {
        /*
        This control has four states for the permutations of [Enabled]/[Disabled] and [Has Value]/[Null or Empty Value],
        each of which will lead to a different visual and/or functional state.

        [allowClear] = when this is true allows clear the signature
        [readOnly] = when this is true it will disable the signature field
        */

        String attachmentId = mHiddenAttachmentIdTextView.getText().toString();
        boolean hasValue = (!MetrixStringHelper.isNullOrEmpty(attachmentId) && !MetrixStringHelper.valueIsEqual(attachmentId, "0"));

        signatureLabel.setVisibility(View.VISIBLE);
        signatureView.setImageDrawable(null);
        if(hasValue) {
            signatureLabel.setVisibility(View.GONE);

            String filePath = "";
                if (MetrixStringHelper.isNegativeValue(attachmentId)) {
                    // Use mm_attachment_id_map to try to get a positive value.  If it exists, replace accordingly.
                    // This will handle things like screen rotation if Sync has occurred while waiting on this field's screen.
                    String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", attachmentId));
                    if (!MetrixStringHelper.isNullOrEmpty(candidateValue)) {
                        attachmentId = candidateValue;
                        mHiddenAttachmentIdTextView.setText(attachmentId);
                    }
                }

                // If we can find the attachment_name, then construct a file path to get a preview.  Otherwise, we'll get an empty placeholder.
                String attachmentName = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_name", String.format("attachment_id = %s", attachmentId));
                if (!MetrixStringHelper.isNullOrEmpty(attachmentName))
                    filePath = MetrixAttachmentHelper.getFilePathFromAttachment(attachmentName);
            if(!MetrixStringHelper.isNullOrEmpty(filePath)) {
                //MetrixAttachmentHelper.showAttachmentFieldPreview(MobileApplication.getAppContext(), filePath, signatureView);
                MetrixAttachmentHelper.showAttachmentFieldPreview(MobileApplication.getAppContext(), filePath, signatureView);
                signatureView.setOnLongClickListener(this::onLongClick);
                if (!isSurvey()) {
                signerColumnDef = GetSignerColumnDef(metrixFormDef, metrixColumnDef.signerColumn);
                    setSignerFieldState(signerColumnDef, true);
                }

            } else {
                signatureLabel.setVisibility(View.VISIBLE);
                signatureLabel.setText(AndroidResourceHelper.getMessage("SignatureNotFound"));
            }
        } else if(!fieldIsEnabled){
            signatureLabel.setVisibility(View.GONE);
        }
    }

    public boolean isSurvey() {
        return isSurvey;
    }

    public void setSurvey(boolean survey) {
        isSurvey = survey;
    }

    public String getSurveyQuestionId() { return surveyQuestionId; }

    public void setSurveyQuestionId(String mSurveyQuestionId) { this.surveyQuestionId = mSurveyQuestionId; }

    public void setIsSurvey() {
        setSurvey(true);
    }

    private String GetLinkedSignerFieldValue(ViewGroup parentLayout, int signerColumnId)
    {
        if (parentLayout == null || signerColumnId == 0)
            return null;

        View signerField = MetrixControlAssistant.getControl(signerColumnId, parentLayout);
        try {
            return MetrixControlAssistant.getValue(signerField);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onClick(View view) {
        try {
            launchingSignatureField = this;
            if(!isSurvey()) {
                signerColumnDef = GetSignerColumnDef(metrixFormDef, metrixColumnDef.signerColumn);
                    if (signerColumnDef != null && signatureView.getDrawable() == null) {
                        signerFieldValue = GetLinkedSignerFieldValue(parentLayout, signerColumnDef.id);

                        if (MetrixStringHelper.isNullOrEmpty(signerFieldValue)) {
                            String signerLableText = removeLabelColon(getSignerFieldLabel(parentLayout, signerColumnDef));

                            if (signerColumnDef.required) {
                                // check that singer field is a required field and has value before adding this signature.
                                // Otherwise we end up in a unusable screen, by locking a required field.
                                String sMessage = AndroidResourceHelper.getMessage("XisRequiredBeforeAddingSignature", signerLableText);
                                MetrixDialogAssistant.showAlertDialog("Alert", sMessage, "Ok", null, null, null,  context);
                                return;
                            }
                            else {
                                String sMessageTitle = AndroidResourceHelper.getMessage("XfieldIsEmpty", signerLableText);
                                String sMessageBody = AndroidResourceHelper.getMessage("ContinueSignWithoutValueX", signerLableText);

                                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        SignatureWidgetManager.openFromFieldForInsert(new WeakReference<>(activityInfo.get()), launchingSignatureField, BASE_TAKE_SIGNATURE, "");
                                    }
                                };
                                MetrixDialogAssistant.showAlertDialog(sMessageTitle, sMessageBody, AndroidResourceHelper.getMessage("Continue"), listener,
                                        AndroidResourceHelper.getMessage("AddSigner"), null, context);
                                return;
                            }
                        }
                        else if(signerFieldValue != null && signatureView.getDrawable() == null){
                            SignatureWidgetManager.openFromFieldForInsert(new WeakReference<>(activityInfo.get()), launchingSignatureField, BASE_TAKE_SIGNATURE, "");
                    }
                } else if (signatureView.getDrawable() == null && signerColumnDef == null) {
                    SignatureWidgetManager.openFromFieldForInsert(new WeakReference<>(activityInfo.get()), launchingSignatureField, BASE_TAKE_SIGNATURE, "");
                }
            } else if (isSurvey() && signatureView.getDrawable() == null) {
                SignatureWidgetManager.openFromFieldForInsert(new WeakReference<>(activityInfo.get()), launchingSignatureField, BASE_TAKE_SIGNATURE, "");
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private String getSignerFieldLabel(ViewGroup parentLayout, MetrixColumnDef signerColumDef)
    {
        // Get the label text
        View signerLabelField = MetrixControlAssistant.getControl(signerColumDef.labelId, parentLayout);

        if (signerLabelField instanceof TextView)
            return ((TextView) signerLabelField).getText().toString();

        return "";
    }

    private String removeLabelColon(String label)
    {
        if (!MetrixStringHelper.isNullOrEmpty(label) && label.contains(":"))
        {
            return label.replace(':', ' ');
        }
        return label;
    }

    private MetrixColumnDef GetSignerColumnDef(MetrixFormDef metrixFormDef, String signerColumnName)
    {
        if (metrixFormDef == null || MetrixStringHelper.isNullOrEmpty(signerColumnName))
            return null;

        for (MetrixTableDef tableDef : metrixFormDef.tables)
        {
            for (MetrixColumnDef columnDef : tableDef.columns)
            {
                if (MetrixStringHelper.valueIsEqual(columnDef.columnName, signerColumnName.toLowerCase()))
                {
                    return columnDef;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onLongClick(View view) {
        if(signatureView.getDrawable() != null && allowClear) {
            android.content.DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mHiddenAttachmentIdTextView.setText("");
                    signatureView.setImageDrawable(null);
                    signatureLabel.setVisibility(VISIBLE);
                    updateFieldUI();
                    if(!isSurvey()) {
                        setSignerFieldState(signerColumnDef, false);
                }
                }
            };
            MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("SignatureLCase"), deleteListener, null, activityInfo.get());
        }
            return true;
    }

    private void setSignerFieldState(MetrixColumnDef metrixColumnDef, boolean isReadOnly) {
        if(metrixColumnDef == null)
            return;
        EditText signerField = (EditText) MetrixControlAssistant.getControl(metrixColumnDef.id, parentLayout);
        if (signerField instanceof EditText) {
            if(isReadOnly) {
                signerField.setInputType(InputType.TYPE_NULL);
                signerField.setEnabled(false);
            } else {
                signerField.setInputType(InputType.TYPE_CLASS_TEXT);
                signerField.setEnabled(true);
            }
        }
    }
}
