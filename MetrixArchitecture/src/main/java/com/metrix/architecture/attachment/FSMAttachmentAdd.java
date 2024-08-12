package com.metrix.architecture.attachment;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class FSMAttachmentAdd extends AttachmentAPIBaseActivity implements View.OnClickListener {
    private HashMap<String, Integer> resourceData;
    private ViewGroup parentTableLayout;
    private FloatingActionButton cancelButton, customSaveButton;
    private ImageView filePreview;
    private String attachmentFileName;
    private ArrayList<Uri> attachmentMultipleFiles;
    private boolean isFromList = true;
    private boolean isExistingFile;

    private static Uri mediaUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resourceData = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("FSMAttachmentAddResources");

        setContentView(resourceData.get("R.layout.aapi_attachment_add"));
        parentTableLayout = (ViewGroup) findViewById(resourceData.get("R.id.parent_table_layout"));
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            initialScreenSetup(extras);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == resourceData.get("R.id.custom_save")) {
            if (scriptEventConsumesClick(this, "BUTTON_SAVE"))
                return;

            if(attachmentMultipleFiles!=null && attachmentMultipleFiles.size()>0) {
//                for (String selectFileName : attachmentMultipleFiles) {
//                    String filePath = MetrixAttachmentHelper.getFilePathFromAttachment(selectFileName);
//                    super.save(filePath, isExistingFile, isFromList);
//                }

                for (Uri selectFilePath : attachmentMultipleFiles) {
                    Boolean isSameFileNameExistInAttachmentFolder = false;
                    String attachmentFileName = MetrixAttachmentHelper.transferFileToAttachmentFolder(selectFilePath);
                    if (MetrixStringHelper.isNullOrEmpty(attachmentFileName))
                        isSameFileNameExistInAttachmentFolder = true;
                    String attachmentFilePath = MetrixAttachmentHelper.getFilePathFromAttachment(attachmentFileName);

                    // Save file to attachment and get it to upload
                    super.save(attachmentFilePath, isSameFileNameExistInAttachmentFolder, true);
                }
                finish();
            }
            else {
                if (attachmentFileName.compareTo("") != 0) {
                    String filePath = MetrixAttachmentHelper.getFilePathFromAttachment(attachmentFileName);
                    if (super.save(filePath, isExistingFile, isFromList))
                        finish();
                } else {
                    MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("NoUploadFileSelected"));
                }
            }
        } else if (viewId == resourceData.get("R.id.cancel")) {
            finish();
        } else
            super.onClick(v);
    }

    @Override
    public void resetFABOffset() {
        try {
            // Poke the FAB rendering first
            hideFABs(mFABList);
            showFABs();

            // Now that FABs are showing, set the offset on parent layout
            parentTableLayout.setPadding(parentTableLayout.getPaddingLeft(), parentTableLayout.getPaddingTop(), parentTableLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        customSaveButton = (FloatingActionButton) findViewById(resourceData.get("R.id.custom_save"));
        cancelButton = (FloatingActionButton) findViewById(resourceData.get("R.id.cancel"));

        customSaveButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        filePreview = (ImageView) findViewById(resourceData.get("R.id.file_preview"));

        // Add all buttons to mFABList, so that client scripting can show/hide them and the scrolling framework will still operate well
        mFABList.add(customSaveButton);
        mFABList.add(cancelButton);

        completeFABSetup();
    }

    @Override
    protected void defineForm() {
        MetrixTableDef attachmentDef = new MetrixTableDef("attachment", MetrixTransactionTypes.INSERT);
        this.mFormDef = new MetrixFormDef(attachmentDef);
    }

    private void initialScreenSetup(Bundle extras) {
        try {
            isExistingFile = false;

            isFromList = extras.getBoolean("isFromList");
            Object extrasCameraValue = extras.get("FromCamera");
            Object extrasVideoCameraValue = extras.get("FromVideoCamera");
            Object extrasFileDialogValue = extras.get("FromFileDialog");
            Object extrasFileUriValue = extras.get("FromFileUri");
            Object extrasFileUriListValue = extras.get("FileUriList");
            String fileName = "";
            String filePath = "";

            if (extrasCameraValue != null) {
                mediaUri = (Uri) extras.get("ImageUri");
                filePath = mediaUri.getPath();
                fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
                if (!new File(filePath).exists()) { // Don't copy again if already copied, which would happen if you rotate the screen
                    MetrixFileHelper.copyFileUriToPrivate(this, mediaUri);
                }
            } else if (extrasVideoCameraValue != null) {
                mediaUri = (Uri) extras.get("VideoUri");
                filePath = mediaUri.getPath();
                fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
                if (!new File(filePath).exists()) { // Don't copy again if already copied, which would happen if you rotate the screen
                    MetrixFileHelper.copyFileUriToPrivate(this, mediaUri);
                }
            } else if (extrasFileDialogValue != null) {
                String fileOriginalPath = extras.getString("FileName");
                String fileOriginalName = fileOriginalPath.substring(fileOriginalPath.lastIndexOf("/") + 1);
                String attachmentTestPath = MetrixAttachmentHelper.getFilePathFromAttachment(fileOriginalName);
                String fileExtension = MetrixFileHelper.getFileType(fileOriginalName);

                // OSP-3443 We might be able to remove this because we are saving files to the private
                //  directory and our File Picker currently doesn't get access to other files beside
                //  our own
                // Since we copy the original attachment to FSM managed attachment directory, verify we can do that first
                if (MetrixStringHelper.valueIsEqual(fileOriginalPath, attachmentTestPath)) {
                    isExistingFile = true; // User has selected a file from the FSM managed attachment directory. No need to copy the file again.
                    fileName = fileOriginalName;
                    filePath = attachmentTestPath;
                } else {
                    // Use standard file name pattern for destination
                    fileName = AttachmentWidgetManager.generateFileName(fileExtension);
                    filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);

                    final File attachmentNewFile = new File(filePath);
                    final File attachmentOldFile = new File(fileOriginalPath);
                    if (!MetrixAttachmentManager.getInstance().copyFileToNewLocation(attachmentOldFile, attachmentNewFile, this))
                        return;
                }
            }
            else  if (extrasFileUriValue != null) {
                mediaUri = (Uri) extras.get("FileUri");
                fileName = MetrixAttachmentHelper.transferFileToAttachmentFolder(mediaUri);
                filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
            }
            else  if (extrasFileUriListValue != null) {
                ArrayList<Uri> fileList = (ArrayList<Uri>) extrasFileUriListValue;
                attachmentMultipleFiles = fileList;

                for (int i=0; i<fileList.size(); i++) {
                    // Save file to attachment and get it to upload
                    mediaUri = (Uri)(fileList.get(i));
                    if(i == 0) {
                        fileName = MetrixAttachmentHelper.transferFileToAttachmentFolder(mediaUri);
                        filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
                    }
                    else {
                        MetrixAttachmentHelper.transferFileToAttachmentFolder(mediaUri);
                        MetrixAttachmentHelper.getFilePathFromAttachment(fileName);
                    }
                }
            }
            if (MetrixStringHelper.isNullOrEmpty(fileName))
                return;

            attachmentFileName = fileName;

            String validationMessage = AttachmentWidgetManager.attachmentIsValid(filePath);
            if (!MetrixStringHelper.isNullOrEmpty(validationMessage)) {
                MetrixUIHelper.showSnackbar(mCurrentActivity, getCoordinatorLayoutID(), validationMessage);
                attachmentFileName = "";
                mediaUri = null;
                customSaveButton.setEnabled(false);
                return;
            }

            MetrixAttachmentHelper.showAttachmentFullScreenPreview(getApplicationContext(), filePath, filePreview);

            if (isExistingFile) {
                // If we are selecting an existing attachment when adding, don't show the Card.
                // Any changes wouldn't be saved in this case anyway.
                mLayout.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("ErrorSavingAttachmentUnknown"));
            LogManager.getInstance(this).error(e);
        }
    }
}
