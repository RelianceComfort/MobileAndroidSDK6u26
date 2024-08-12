package com.metrix.architecture.attachment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;

import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_PICTURE_CAMERA_PERMISSION;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.SELECT_FILES;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_TAKE_PICTURE;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_TAKE_VIDEO;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_VIDEO_CAMERA_PERMISSION;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.baseMediaUri;

public class AttachmentField extends LinearLayout {
    public static final String LAUNCHING_ATTACHMENT_FIELD_DATA = "LAUNCHING_ATTACHMENT_FIELD_DATA";
    public TextView mHiddenAttachmentIdTextView;

    private static HashMap<String, Integer> resourceCache;

    private WeakReference<Activity> activityInfo;

    private boolean onDemand = false;
    private boolean fieldIsEnabled;
    private boolean fieldIsReadOnlyInMetadata;

    private boolean allowPhoto = false;
    private boolean allowVideo = false;
    private boolean allowFile = false;
    private int cardScreenId = 0;
    private String fieldTableName = "";
    private String transactionIdTableName = "";
    private String transactionIdColumnName = "";

    private AttachmentAdditionControl additionControl;
    private ImageView filePreview;
    private LaunchingAttachmentFieldData launchingAttachmentFieldData;

    public AttachmentField(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (resourceCache == null)
            resourceCache = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("AttachmentFieldResources");

        int layoutId = resourceCache.get("R.layout.aapi_attachment_field");
        inflate(context, layoutId, this);

        additionControl = findViewById(resourceCache.get("R.id.attachment_add_ctrl"));
        filePreview = findViewById(resourceCache.get("R.id.file_preview"));

        // Set up a reference to a hidden TextView that tracks Attachment ID.
        // Using a textview like this allows for value changed event handling according to existing pattern.
        mHiddenAttachmentIdTextView = new TextView(context);
        mHiddenAttachmentIdTextView.setVisibility(View.GONE);

        mHiddenAttachmentIdTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (launchingAttachmentFieldData != null) {
                    launchingAttachmentFieldData.attachmentId = mHiddenAttachmentIdTextView.getText().toString();
                }
            }
        });
    }

    @Override
    public boolean isEnabled() { return fieldIsEnabled; }
    @Override
    public void setEnabled(boolean enabled) {
        // Only allow enabled state to be toggled if field is not read-only in metadata
        if (!fieldIsReadOnlyInMetadata) {
            fieldIsEnabled = enabled;
            updateFieldUI();
        }
    }

    public int getCardScreenId() { return cardScreenId; }
    public String getFieldTableName() { return fieldTableName; }
    public String getTransactionIdTableName() { return transactionIdTableName; }
    public String getTransactionIdColumnName() { return transactionIdColumnName; }
    public boolean getAttachmentIsOnDemand() { return onDemand; }

    LaunchingAttachmentFieldData getLaunchingAttachmentFieldData() { return launchingAttachmentFieldData; }

    public void setupFromConfiguration(Activity activity, MetrixColumnDef columnDef, String tableName) {
        fieldIsReadOnlyInMetadata = columnDef.readOnlyInMetadata;
        fieldIsEnabled = !fieldIsReadOnlyInMetadata;

        activityInfo = new WeakReference<>(activity);

        allowPhoto = columnDef.allowPhoto;
        allowVideo = columnDef.allowVideo;
        allowFile = columnDef.allowFile;
        cardScreenId = columnDef.cardScreenID;
        fieldTableName = tableName;
        transactionIdTableName = columnDef.transactionIdTableName;
        transactionIdColumnName = columnDef.transactionIdColumnName;

        additionControl.setCameraBtnVisibility(allowPhoto);
        additionControl.setVideoBtnVisibility(allowVideo);
        additionControl.setFileBtnVisibility(allowFile);

        launchingAttachmentFieldData = new LaunchingAttachmentFieldData(tableName, columnDef.columnName);

        String controlId = "AttachmentField_"+tableName+"__"+columnDef.columnName;
        if (allowPhoto) {
            additionControl.setCameraBtnListener(v -> {
                // Handle Camera attachment addition
                if (ActivityCompat.checkSelfPermission(activityInfo.get(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCameraForPicture();
                } else {
                    ActivityCompat.requestPermissions(activityInfo.get(), new String[]{Manifest.permission.CAMERA}, BASE_PICTURE_CAMERA_PERMISSION);
                }
            });
            additionControl.setCameraControlId(controlId+"_camera");
        }

        if (allowVideo) {
            additionControl.setVideoBtnListener(v -> {
                // Handle Video attachment addition
                if (ActivityCompat.checkSelfPermission(activityInfo.get(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCameraForVideo();
                } else {
                    ActivityCompat.requestPermissions(activityInfo.get(), new String[]{Manifest.permission.CAMERA}, BASE_VIDEO_CAMERA_PERMISSION);
                }
            });
            additionControl.setVideoControlId(controlId+"_video");
        }

        if (allowFile) {
            additionControl.setFileBtnListener(v -> {
                // Handle File attachment addition
                try {

                    Activity thisActivity = activityInfo.get();
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    //intent.setType("image/*"); //allows any image file type. Change * to specific extension to limit it
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    MetrixPublicCache.instance.addItem(LAUNCHING_ATTACHMENT_FIELD_DATA, launchingAttachmentFieldData);
                    thisActivity.startActivityForResult(Intent.createChooser(intent, "Select Attachment"), SELECT_FILES); //SELECT_PICTURES is simply a global int used to check the calling intent in onActivityResult
                } catch (Exception e) {
                    LogManager.getInstance().error(e);
                }
            });
            additionControl.setFileControlId(controlId + "_file");
        }

        this.setContentDescription(controlId);
        if (this.filePreview != null) {
            this.filePreview.setContentDescription(controlId + "_preview");
        }
        updateFieldUI();    // Useful when current screen is in INSERT mode and the field value won't be set.
    }

    public void updateFieldUI() {
        /*
        This control has four states for the permutations of [Enabled]/[Disabled] and [Has Value]/[Null or Empty Value],
        each of which will lead to a different visual and/or functional state.

        [Enabled] / [Has Value] = Display attachment preview.  Can tap to advance into widget and make changes.  Can long-press to clear column data.
        [Enabled] / [Null/Empty] = Display Attachment Addition Control, with buttons shown per configuration.
        [Disabled] / [Has Value] = Display attachment preview.  Can tap to advance into widget but cannot make changes.  Cannot clear.
        [Disabled] / [Null/Empty] = Display empty placeholder.  No tap or long-press ability.

        If this field is defined as read-only in metadata, always treat as disabled.
        */

        String attachmentId = mHiddenAttachmentIdTextView.getText().toString();
        boolean treatAsEnabled = (!fieldIsReadOnlyInMetadata && fieldIsEnabled);
        boolean hasValue = (!MetrixStringHelper.isNullOrEmpty(attachmentId) && !MetrixStringHelper.valueIsEqual(attachmentId, "0"));

        if (treatAsEnabled && !hasValue) {
            additionControl.setVisibility(View.VISIBLE);
            filePreview.setVisibility(View.GONE);
        } else {
            // In any other state, we will not show the Attachment Addition Control and display something in the preview instead.
            additionControl.setVisibility(View.GONE);
            filePreview.setVisibility(View.VISIBLE);

            String filePath = "";
            if (hasValue) {
                if (MetrixStringHelper.isNegativeValue(attachmentId)) {
                    // Use mm_attachment_id_map to try to get a positive value.  If it exists, replace accordingly.
                    // This will handle things like screen rotation if Sync has occurred while waiting on this field's screen.
                    String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", attachmentId));
                    if (!MetrixStringHelper.isNullOrEmpty(candidateValue)) {
                        attachmentId = candidateValue;
                        mHiddenAttachmentIdTextView.setText(attachmentId);
                    }
                }

                Hashtable<String, String> attachmentInfo = MetrixDatabaseManager.getFieldStringValues("attachment", new String[]{"attachment_name", "on_demand"}, String.format("attachment_id = %s", attachmentId));

                if (attachmentInfo != null && attachmentInfo.size() > 0){
                    // If we can find the attachment_name, then construct a file path to get a preview.  Otherwise, we'll get an empty placeholder.
                    String attachmentName = attachmentInfo.get("attachment_name");

                    if (!MetrixStringHelper.isNullOrEmpty(attachmentName)) {
                        filePath = MetrixAttachmentHelper.getFilePathFromAttachment(attachmentName);
                    }
                    if (MetrixStringHelper.valueIsEqual(attachmentInfo.get("on_demand"), "Y")){
                        onDemand = true;
                    }
                }
            }
            MetrixAttachmentHelper.showAttachmentFieldPreview(MobileApplication.getAppContext(), filePath, filePreview, onDemand);

            // Manage tap - only allow if we have a value.
            if (!hasValue) {
                filePreview.setOnClickListener(null);
            } else {
                filePreview.setOnClickListener(v -> {
                    FetchAttachmentId();
                    AttachmentWidgetManager.openFromFieldForUpdate(activityInfo, this);
                    return;
                });
            }

            // Manage long-press - only allow if enabled.
            if (!treatAsEnabled) {
                filePreview.setOnLongClickListener(null);
            } else {
                filePreview.setOnLongClickListener(v -> {
                    android.content.DialogInterface.OnClickListener deleteListener = (dialog, which) -> {
                        try {
                            // Normal save behavior on the screen will store this change in data as a transaction to sync.
                            mHiddenAttachmentIdTextView.setText("");
                            updateFieldUI();
                        } catch (Exception e) {
                            LogManager.getInstance().error(e);
                        }
                    };

                    MetrixDialogAssistant.showConfirmDeleteDialog(
                            AndroidResourceHelper.getMessage("AttachmentLCase"),
                            deleteListener, null, activityInfo.get());
                    return true;
                });
            }
        }
    }

    private void FetchAttachmentId()
    {
        String attachmentIdValueInner = mHiddenAttachmentIdTextView.getText().toString();

        if (MetrixStringHelper.isNegativeValue(attachmentIdValueInner)) {
            // Use mm_attachment_id_map to try to get a positive value.  If it exists, replace accordingly.
            // This will handle tapping on the attachment if Sync has occurred while waiting on this field's screen.
            String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", attachmentIdValueInner));
            if (!MetrixStringHelper.isNullOrEmpty(candidateValue)) {
                attachmentIdValueInner = candidateValue;
                mHiddenAttachmentIdTextView.setText(attachmentIdValueInner);
            }
        }
    }

    public void setSurveyQuestionId(String surveyQuestionId) {
        launchingAttachmentFieldData = new LaunchingAttachmentFieldData(surveyQuestionId);
    }

    public void setBackgroundForSurvey(String colorCode) {
        try {
            int colorCodeInt = (int)Long.parseLong(colorCode, 16);
            int translatedColorCode = Color.rgb((colorCodeInt >> 16) & 0xFF, (colorCodeInt >> 8) & 0xFF, (colorCodeInt >> 0) & 0xFF);

            // Set background for overall Attachment Field, by walking down one level
            ViewGroup thisLayout = (ViewGroup)this;
            View mainChildView = thisLayout.getChildAt(0);
            mainChildView.setBackgroundColor(translatedColorCode);

            // Set background for Attachment Addition Control, by walking down one level
            ViewGroup additionLayout = (ViewGroup)additionControl;
            View aacChildView = additionLayout.getChildAt(0);
            aacChildView.setBackgroundColor(translatedColorCode);

            // Set background for File Preview control
            filePreview.setBackgroundColor(translatedColorCode);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void openCameraForPicture() {
        try {
            AttachmentWidgetManager.setParentTableFromField(fieldTableName);
            String fileName = AttachmentWidgetManager.generateFileName("jpg");
            baseMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
            launchingAttachmentFieldData.mediaUri = baseMediaUri;
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, baseMediaUri);
        MetrixPublicCache.instance.addItem(LAUNCHING_ATTACHMENT_FIELD_DATA, launchingAttachmentFieldData);
        activityInfo.get().startActivityForResult(intent, BASE_TAKE_PICTURE);
    }

    private void openCameraForVideo() {
        try {
            AttachmentWidgetManager.setParentTableFromField(fieldTableName);
            String fileName = AttachmentWidgetManager.generateFileName("mp4");
            baseMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
            launchingAttachmentFieldData.mediaUri = baseMediaUri;
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            return;
        }  // create a file to save the video

        String maximumSize = MobileApplication.getAppParam("ATTACHMENT_MAX_SIZE");
        if (MetrixStringHelper.isNullOrEmpty(maximumSize)) {
            maximumSize = "10242880";
        }

        long sizeLimit = Long.parseLong(maximumSize);

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, baseMediaUri);  // set the image file name
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3600);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        MetrixPublicCache.instance.addItem(LAUNCHING_ATTACHMENT_FIELD_DATA, launchingAttachmentFieldData);
        // start the Video Capture Intent
        activityInfo.get().startActivityForResult(intent, BASE_TAKE_VIDEO);
    }

    public static class LaunchingAttachmentFieldData {
        public final boolean fromSurvey;
        public final String tableName;
        public final String columnName;
        public final String surveyQuestionId;
        public Uri mediaUri;
        public String attachmentId;

        LaunchingAttachmentFieldData(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
            fromSurvey = false;
            surveyQuestionId = null;
        }

        LaunchingAttachmentFieldData(String surveyQuestionId) {
            tableName = null;
            columnName = null;
            fromSurvey = true;
            this.surveyQuestionId = surveyQuestionId;
        }

        public LaunchingAttachmentFieldData(Intent intent) {
            fromSurvey = intent.getBooleanExtra("fromSurvey", false);
            surveyQuestionId = intent.getStringExtra("surveyQuestionId");
            tableName = intent.getStringExtra("tableName");
            columnName = intent.getStringExtra("columnName");
            if (intent.hasExtra("mediaUri")) {
                this.mediaUri = Uri.parse(intent.getStringExtra("mediaUri"));
            }
            this.attachmentId = intent.getStringExtra("attachmentId");
        }

        public boolean isValid() {
            if (fromSurvey) {
                return !TextUtils.isEmpty(surveyQuestionId);
            }
            return !TextUtils.isEmpty(tableName) && !TextUtils.isEmpty(columnName);
        }

        public void saveToIntent(Intent intent) {
            intent.putExtra("fromSurvey", fromSurvey);
            if (fromSurvey) {
                intent.putExtra("surveyQuestionId", surveyQuestionId);
            } else {
                intent.putExtra("tableName", tableName);
                intent.putExtra("columnName", columnName);
            }
            intent.putExtra("attachmentId", attachmentId);
            if (mediaUri != null) {
                intent.putExtra("mediaUri", mediaUri.toString());
            }
        }
    }
}
