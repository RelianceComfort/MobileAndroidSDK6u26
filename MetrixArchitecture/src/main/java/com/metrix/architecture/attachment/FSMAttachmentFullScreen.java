package com.metrix.architecture.attachment;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MessageHandler;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixAttachmentUtil;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.PermissionHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class FSMAttachmentFullScreen extends AttachmentAPIBaseActivity implements View.OnClickListener {
    public int mSelectedPosition = -1;

    private HashMap<String, Integer> resourceData;
    private FloatingActionButton editButton, viewCardButton;
    private FSMCarouselView carouselView;

    private String attachmentIdFromField = "";
    private boolean isOnDemandFromField = false;
    private boolean allowModifyFromList = false;
    private boolean attachmentFieldIsEnabled = false;
    private boolean isFromList = true;
    private boolean showCard = false;

    private static String selectedFilePath;
    private static String attachmentId;
    private static String metrixRowId;
    private static String attachmentName;
    private static final int PHOTO_EDITOR_REQUEST_CODE = 2027;
    private static final int MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 321;

    private AlertDialog mInitAlert;
    protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resourceData = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("FSMAttachmentFullScreenResources");

        setContentView(resourceData.get("R.layout.aapi_attachment_fullscreen"));
        carouselView = (FSMCarouselView) findViewById(resourceData.get("R.id.attachment_pager"));
        carouselView.setParentActivity(this);

        // Only show the View Card option if we have Card metadata
        showCard = false;
        if (!MetrixStringHelper.isNullOrEmpty(AttachmentWidgetManager.getAttachmentCardScreenName()))
            showCard = true;

        allowModifyFromList = AttachmentWidgetManager.getListAllowModify();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSelectedPosition = extras.getInt("position");
            isFromList = extras.getBoolean("isFromList");
            attachmentFieldIsEnabled = extras.getBoolean("attachmentFieldIsEnabled");
            attachmentIdFromField = extras.getString("attachmentIdFromField");
            isOnDemandFromField = extras.getBoolean("isOnDemandFromField");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        populateCarousel();
    }

    @Override
    protected void onResume() {
        if (!PermissionHelper.checkPublicFilePermission(this)) {
            PermissionHelper.requestPublicFilePermission(this);
        } else {
            MetrixFileHelper.deletePublicFile(attachmentName);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (PermissionHelper.checkPublicFilePermission(this)) {
            MetrixFileHelper.deletePublicFile(attachmentName);
        }

        unbindService();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case PHOTO_EDITOR_REQUEST_CODE:
                        Uri outputUri = data.getData();
                        if (outputUri != null) {
                            saveFile(outputUri);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == resourceData.get("R.id.edit_attachment")) {
            launchPhotoEditor();
        } else if (viewId == resourceData.get("R.id.view_card")) {
            showCardScreen();
        } else {
            super.onClick(v);
        }
    }

    @Override
    protected void setListeners() {
        editButton = (FloatingActionButton) findViewById(resourceData.get("R.id.edit_attachment"));
        viewCardButton = (FloatingActionButton) findViewById(resourceData.get("R.id.view_card"));

        editButton.setOnClickListener(this);
        viewCardButton.setOnClickListener(this);

        if (showCard)
            viewCardButton.show();
        else
            viewCardButton.hide();
    }

    public void updateEditButtonAppearance() {
        try {
            if ((isFromList && allowModifyFromList) || (!isFromList && attachmentFieldIsEnabled)) {
                HashMap<String, String> dataRow = carouselView.getItem(mSelectedPosition);
                String attachmentName = dataRow.get("attachment.attachment_name");
                if (MetrixAttachmentUtil.isImageFile(attachmentName))
                    editButton.show();
                else
                    editButton.hide();
            } else
                editButton.hide();
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void populateCarousel() {
        List<HashMap<String, String>> data = getAttachmentData();
        if (data != null) {
            carouselView.setDataSource(getApplicationContext(), data);
            carouselView.setCurrentItem(mSelectedPosition);
            updateEditButtonAppearance();
            attachmentName = data.get(mSelectedPosition).get("attachment.attachment_name");
        } else {
            // If we have no data, hide the buttons
            editButton.hide();
            viewCardButton.hide();
        }
    }

    private List<HashMap<String, String>> getAttachmentData() {
        MetrixCursor cursor = null;
        try {
            if (!MetrixStringHelper.isNullOrEmpty(attachmentIdFromField) && MetrixStringHelper.isNegativeValue(attachmentIdFromField)) {
                // Try to get the updated value from mm_attachment_id_map
                String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", attachmentIdFromField));
                if (!MetrixStringHelper.isNullOrEmpty(candidateValue))
                    attachmentIdFromField = candidateValue;
            }

            StringBuilder query = new StringBuilder();
            query.append("select attachment.metrix_row_id, attachment.attachment_id, attachment.attachment_name,");
            query.append(" attachment.attachment_description, attachment.created_dttm, attachment.on_demand");
            query.append(" from attachment where ");
            query.append(AttachmentWidgetManager.generateWhereClause(attachmentIdFromField));

            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            List<HashMap<String, String>> table = new ArrayList<>();
            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = new HashMap<String, String>();
                row.put("attachment.metrix_row_id", cursor.getString(0));
                row.put("attachment.attachment_id", cursor.getString(1));
                row.put("attachment.attachment_name", cursor.getString(2));
                row.put("attachment.attachment_description", cursor.getString(3));
                row.put("attachment.created_dttm", cursor.getString(4));
                row.put("attachment.on_demand", cursor.getString(5));
                table.add(row);

                cursor.moveToNext();
            }

            return table;
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private void launchPhotoEditor() {
        try {
            HashMap<String, String> dataRow = carouselView.getItem(mSelectedPosition);
            metrixRowId = dataRow.get("attachment.metrix_row_id");
            attachmentId = dataRow.get("attachment.attachment_id");
            attachmentName = dataRow.get("attachment.attachment_name");
            selectedFilePath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + attachmentName;

            if (MetrixAttachmentHelper.checkImageFile(selectedFilePath)) {
                Intent dsPhotoEditorIntent = new Intent(this, DsPhotoEditorActivity.class);
                dsPhotoEditorIntent.setData(Uri.parse("file://" + selectedFilePath));

                // By providing an (optional) output directory, the edited photo will be saved in the specified folder on your device's external storage.
                // If this is omitted, the edited photo will be saved to a folder named "DS_Photo_Editor" by default.
                // [Using default approach in Attachment Widget ... sample code to do otherwise is below.]
                // dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "YOUR_OUTPUT_IMAGE_FOLDER");

                // Optional customization: hide some tools you don't need as below
                int[] toolsToHide = {DsPhotoEditorActivity.TOOL_FILTER, DsPhotoEditorActivity.TOOL_FRAME,
                        DsPhotoEditorActivity.TOOL_ROUND, DsPhotoEditorActivity.TOOL_EXPOSURE,
                        DsPhotoEditorActivity.TOOL_CONTRAST, DsPhotoEditorActivity.TOOL_VIGNETTE,
                        DsPhotoEditorActivity.TOOL_CROP, DsPhotoEditorActivity.TOOL_ORIENTATION,
                        DsPhotoEditorActivity.TOOL_SATURATION, DsPhotoEditorActivity.TOOL_SHARPNESS,
                        DsPhotoEditorActivity.TOOL_WARMTH, DsPhotoEditorActivity.TOOL_PIXELATE,
                        DsPhotoEditorActivity.TOOL_STICKER};
                dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, toolsToHide);

                startActivityForResult(dsPhotoEditorIntent, PHOTO_EDITOR_REQUEST_CODE);
            } else {
                MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("UnsupportedFileType"));
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void showCardScreen() {
        try {
            HashMap<String, String> dataRow = carouselView.getItem(mSelectedPosition);
            String metrixRowId = dataRow.get("attachment.metrix_row_id");
            Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentCard.class);
            intent.putExtra("MetrixRowID", metrixRowId);
            intent.putExtra("isFromList", isFromList);
            intent.putExtra("attachmentFieldIsEnabled", attachmentFieldIsEnabled);
            MetrixActivityHelper.startNewActivity(this, intent);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void saveFile(Uri sourceURI) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        String sourceFileName = "";

        try {
            final boolean isAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
            if (isAndroidQ)
                sourceFileName = MetrixAttachmentUtil.getFilePathFromUri(getApplicationContext(),sourceURI);
            else
                sourceFileName = sourceURI.getPath();
            
            String destinationFilename = selectedFilePath;
            bis = new BufferedInputStream(new FileInputStream(sourceFileName));
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while (bis.read(buf) != -1);

            if (metrixRowId != null) {
                // Refresh attachment_id if it is negative, in case Sync has made a pass and updated it after a successful insert.
                if (MetrixStringHelper.isNegativeValue(attachmentId)) {
                    attachmentId = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_id", "metrix_row_id=" + metrixRowId);
                    if (!MetrixStringHelper.isNullOrEmpty(attachmentIdFromField))
                        attachmentIdFromField = attachmentId;
                }

                MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ metrixRowId);
                attachmentData.dataFields.add(new DataField("metrix_row_id", metrixRowId));
                attachmentData.dataFields.add(new DataField("attachment_id", attachmentId));
                attachmentData.dataFields.add(new DataField("attachment_name", attachmentName));
                attachmentData.dataFields.add(new DataField("file_extension", MetrixFileHelper.getFileType(attachmentName)));
                attachmentData.dataFields.add(new DataField("mobile_path", selectedFilePath));

                ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<>();
                attachmentTrans.add(attachmentData);

                MetrixTransaction transactionInfo = AttachmentWidgetManager.getTransactionInfo();
                boolean successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);
                if (!successful) {
                    MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("DataErrorOnUpload"));
                    return;
                } else if (!isFromList) {
                    AttachmentField.LaunchingAttachmentFieldData fieldData = AttachmentWidgetManager.getAttachmentFieldData();
                    if (fieldData != null) {
                        Intent data = new Intent();
                        fieldData.saveToIntent(data);
                        setResult(RESULT_OK, data);
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        } finally {
            try {
                if (bis != null)
                    bis.close();
                if (bos != null)
                    bos.close();
            } catch (IOException e) {
                LogManager.getInstance().error(e);
            }
        }
    }

    // On demand download
    public void downloadOnDemandImage(final HashMap<String, String> dataRow) {
        //if (isFromList)            return;

        String attachmentId = dataRow.get("attachment.attachment_id");
        String onDemand = dataRow.get("attachment.on_demand");
        boolean isOnDemand = !MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y");

        if (isOnDemand && !MetrixAttachmentHelper.isAttachmentExists(attachmentId)) {
            mInitAlert = new AlertDialog.Builder(this).create();
            mInitAlert.setTitle(AndroidResourceHelper.getMessage("DownloadConfirmation"));
            mInitAlert.setMessage(AndroidResourceHelper.getMessage("DownloadOnDemandAtt"));
            mInitAlert.setButton(AndroidResourceHelper.getMessage("YesButton"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (SettingsHelper.getSyncPause(mCurrentActivity)) {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if (syncPauseAlertDialog != null) {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                    startDownloadAttachment(attachmentId);
                                }
                            });
                        }
                    } else
                        startDownloadAttachment(attachmentId);
                }
            });
            mInitAlert.setButton2(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            mInitAlert.show();
        }
    }

    private void startDownloadAttachment(final String selectedAttachmentId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
                String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));
                String cloudDownload = SettingsHelper.getStringSetting(mCurrentActivity, SettingsHelper.IS_AZURE);

                if (!MetrixStringHelper.isNullOrEmpty(cloudDownload) && cloudDownload.toUpperCase().equals("Y")) {
                    String token = getToken(baseUrl, remote);

                    //token should include "sv=" as signed version, otherwise invalid
                    if (MetrixStringHelper.isNullOrEmpty(token) || token.contains("sv=") == false) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mCurrentActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                MetrixUIHelper.showSnackbar(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceNotAccessible"));
                            }
                        });
                        return;
                    }
                    processOnDemandDownloads(selectedAttachmentId, token);
                } else {
                    processOnDemandDownloads(selectedAttachmentId, "");
                }
                if (mInitAlert != null) {
                    mInitAlert.dismiss();
                }
            }
        });

        thread.start();
    }

    private void processOnDemandDownloads(String selectedAttachmentId, String token) {
        if (!MetrixStringHelper.isNullOrEmpty(selectedAttachmentId)) {
            int downloadCount = 0;
            Hashtable<String, String> downloadFiles = new Hashtable<String, String>();

            final String azureToken = token;
            final String attachmentId = selectedAttachmentId;

            HashMap<String, String> dataRow = carouselView.getItem(mSelectedPosition);
            String attachmentName = dataRow.get("attachment.attachment_name");
            String onDemand = dataRow.get("attachment.on_demand");
            boolean isOnDemand = !MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y");

            if(isOnDemand){

                final String attachmentDownloadName = attachmentName;

                try {
                    downloadFiles.put(attachmentDownloadName, attachmentId);
                    downloadCount++;

                    new Thread(new Runnable() {
                        public void run() {
                            String downloadPath = MessageHandler.getDownloadUrl(attachmentId, attachmentName);
                            if(!MetrixStringHelper.isNullOrEmpty(azureToken)){
                                downloadPath += azureToken;
                                downloadPath = downloadPath.replace("u+0026amp;", "&").replace("u+0026", "&").replace("\\", "/");
                            }
                            MessageHandler.downloadAttachmentWithListener(downloadPath, attachmentName, attachmentId);
                        }
                    }).start();
                }
                catch(Exception ex) {
                    LogManager.getInstance().error(ex);
                }

                if (downloadCount > 0)
                {
                    MetrixPublicCache.instance.addItem("DOWNLOAD_FILES", downloadFiles);
                    mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AttachmentDownloading"));
                }
            }
        }
    }

    @Override
    protected void bindService() {
        bindService(new Intent(FSMAttachmentFullScreen.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    @Override
    protected void unbindService() {
        if (mIsBound) {
            try {
                if (service != null) {
                    service.removeListener(listener);
                    unbindService(mConnection);
                }
            } catch (Exception ex) {
                LogManager.getInstance().error(ex);
            } finally {
                mIsBound = false;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            // mBoundService = ((MetrixIntentService.LocalBinder) binder)
            // .getService();

            try {
                service = (IPostMonitor) binder;
                service.registerListener(listener);
            } catch (Throwable t) {
                LogManager.getInstance().error(t);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            // mBoundService = null;
            service = null;
        }
    };

    protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
        public void newSyncStatus(final Global.ActivityType activityType, final String message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (activityType == Global.ActivityType.Download) {
                        if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTAS\":null}")) {
                            MobileApplication.stopSync(mCurrentActivity);
                            MobileApplication.startSync(mCurrentActivity);

                        } else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTATAA\":null}")) {
                            MobileApplication.stopSync(mCurrentActivity);
                            MobileApplication.startSync(mCurrentActivity);
                            mUIHelper.dismissLoadingDialog();
                            MetrixUIHelper.showSnackbar(mCurrentActivity, AndroidResourceHelper.getMessage("PTaskAssignmentAlreadyAssigned"));

                        } else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTDNE\":null}")) {
                            MobileApplication.stopSync(mCurrentActivity);
                            MobileApplication.startSync(mCurrentActivity);
                            mUIHelper.dismissLoadingDialog();

                        } else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTA\":null}")) {
                            MobileApplication.stopSync(mCurrentActivity);
                            MobileApplication.startSync(mCurrentActivity);
                            mUIHelper.dismissLoadingDialog();
                        } else if (MetrixStringHelper.valueIsEqual(message, "On_Demand Completed")) {
                            MobileApplication.stopSync(mCurrentActivity);
                            MobileApplication.startSync(mCurrentActivity);
                            mUIHelper.dismissLoadingDialog();

                            // Refresh just the data
                            List<HashMap<String, String>> data = getAttachmentData();
                            if (data != null) {
                                carouselView.setDataSource(getApplicationContext(), data);
                                carouselView.setCurrentItem(mSelectedPosition);
                            }
                        }
                        removeAnyFailedDownloadFiles();

                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    @Override
    protected boolean removeAnyFailedDownloadFiles() {
        boolean refreshUI = super.removeAnyFailedDownloadFiles();
        if (refreshUI) {
            mUIHelper.dismissLoadingDialog();

            Intent intent = MetrixActivityHelper.createActivityIntent(FSMAttachmentFullScreen.this, FSMAttachmentFullScreen.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivity(FSMAttachmentFullScreen.this, intent);
        }
        return refreshUI;
    }

}
