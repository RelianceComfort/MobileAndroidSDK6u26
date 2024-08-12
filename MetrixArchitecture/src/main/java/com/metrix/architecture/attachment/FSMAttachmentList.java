package com.metrix.architecture.attachment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetadataAttachmentRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MessageHandler;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.services.RemoteMessagesHandler;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixParcelable;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class FSMAttachmentList extends AttachmentAPIBaseActivity implements MetrixRecyclerViewListener, MetadataAttachmentRecyclerViewAdapter.MetrixRecyclerViewImageClickListener {
    private HashMap<String, Integer> resourceData;
    private RecyclerView recyclerView;
    private MetadataAttachmentRecyclerViewAdapter metadataAdapter;
    private HashMap<String, String> currentScreenProperties;
    private LruCache<String, Bitmap> memoryCache;
    private boolean comingViaWorkflow;
    private String workflowName;
    public String workflowScreenName;
    private FloatingActionButton mSaveButton, mNextButton;
    private int incompletePermissionRequest = 0;
    private int selectedPosition;
    private boolean allowDelete = false;
    private AlertDialog mInitAlert;
    protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);

    // Adding a public reference to this screen's attachment addition control for scripting access
    public AttachmentAdditionControl mAttachmentAdditionControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resourceData = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("FSMAttachmentListResources");

        setContentView(resourceData.get("R.layout.aapi_attachment_list"));

        mAttachmentAdditionControl = findViewById(resourceData.get("R.id.attachment_add_ctrl"));
        recyclerView = findViewById(resourceData.get("R.id.recyclerView"));
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, resourceData.get("R.drawable.rv_item_divider"));

        currentScreenProperties = MetrixScreenManager.getScreenProperties(AttachmentWidgetManager.getAttachmentListScreenId());
        if (currentScreenProperties != null) {
            allowDelete = false;
            if (currentScreenProperties.containsKey("allow_delete"))
                allowDelete = MetrixStringHelper.valueIsEqual(currentScreenProperties.get("allow_delete"), "Y");
        }

        mNextButton = (FloatingActionButton) findViewById(resourceData.get("R.id.next"));
    }

    private boolean isComingViaWorkflow() {
        // if there is fromworkflow, then treat it as via workflow
        MetrixParcelable<String> metrixParcelableString = getIntent().getParcelableExtra("fromWorkflow");
        if (metrixParcelableString != null) {
            workflowName = metrixParcelableString.getValue();
            comingViaWorkflow = !MetrixStringHelper.isNullOrEmpty(workflowName);
        }

        // if it's from the QuickLinksBar, then treat it as not via workflow
        MetrixParcelable<Boolean> metrixParcelableBool = getIntent().getParcelableExtra("fromQuickLinksBar");
        if (metrixParcelableBool != null)
            comingViaWorkflow = !(metrixParcelableBool.getValue());

        return comingViaWorkflow;
    }

    private void getWorkflowScreenName() {
        // if there is fromworkflow, then get workflow screen name
        MetrixParcelable<String> metrixParcelableString = getIntent().getParcelableExtra("workflowScreenName");
        if (metrixParcelableString != null) {
            workflowScreenName = metrixParcelableString.getValue();
        } else {
            workflowScreenName = "";
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        bindService();
        ((FloatingActionButton) findViewById(resourceData.get("R.id.save"))).hide(); //mSaveButton.hide();
        if (isComingViaWorkflow() == false) {
            ((FloatingActionButton) findViewById(resourceData.get("R.id.next"))).hide(); //mNextButton.hide()
        } else {
            getWorkflowScreenName();
        }

        // Reserve 1/8 of available memory in KB for an image cache
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // represent the size of each bitmap in KB, so that we accurately track used cache
                return bitmap.getAllocationByteCount() / 1024;
            }
        };

        populateList();
    }

    @Override
    public void onStop() {
        // Free up memory being used by thumbnails when activity is not being displayed (regenerated at onStart)
        metadataAdapter.clearAllImages();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (incompletePermissionRequest == BASE_PICTURE_CAMERA_PERMISSION || incompletePermissionRequest == BASE_VIDEO_CAMERA_PERMISSION) {
            final int requestCode = incompletePermissionRequest;
            incompletePermissionRequest = 0;
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(FSMAttachmentList.this, Manifest.permission.CAMERA)) {
                        // can request permission again
                        ActivityCompat.requestPermissions(FSMAttachmentList.this, new String[]{Manifest.permission.CAMERA}, requestCode);
                    } else {
                        // user needs to go to app settings to enable it
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            // This is extremely rare
                            MetrixUIHelper.showSnackbar(FSMAttachmentList.this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("EnablePermManuallyAndRetry"));
                        }
                    }
                }
            };

            MetrixDialogAssistant.showAlertDialog(
                    AndroidResourceHelper.getMessage("PermissionRequired"),
                    AndroidResourceHelper.getMessage("CameraPermGenericExpl"),
                    AndroidResourceHelper.getMessage("Yes"),
                    listener,
                    AndroidResourceHelper.getMessage("No"),
                    null,
                    this
            );
        }

        // Ensures that any photo editing is picked up in thumbnails when coming back to the list
        metadataAdapter.notifyDataSetChanged();
    }

    @Override
    protected void setListeners() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                MetrixActivityHelper.hideKeyboard(FSMAttachmentList.this);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mAttachmentAdditionControl.setCameraBtnListener(v -> {
            // Handle Camera attachment addition
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCameraForPicture();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, BASE_PICTURE_CAMERA_PERMISSION);
            }
        });

        mAttachmentAdditionControl.setVideoBtnListener(v -> {
            // Handle Video attachment addition
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCameraForVideo();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, BASE_VIDEO_CAMERA_PERMISSION);
            }
        });

        mAttachmentAdditionControl.setFileBtnListener(v -> {
            // Handle File attachment addition
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("image/*"); //allows any image file type. Change * to specific extension to limit it
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_FILES); //SELECT_PICTURES is simply a global int used to check the calling intent in onActivityResult

            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        });

        if (mNextButton != null)
            mNextButton.setOnClickListener(this);
    }

    private void populateList() {
        try {
            int screenId = AttachmentWidgetManager.getAttachmentListScreenId();
            String screenName = AttachmentWidgetManager.getAttachmentListScreenName();

            MetrixCursor cursor = null;
            int rowCount = 0;
            List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
            try {
                String whereClause = AttachmentWidgetManager.generateWhereClause(null);
                String query = MetrixListScreenManager.generateListQuery("attachment", whereClause, null, screenId);

                cursor = MetrixDatabaseManager.rawQueryMC(query, null);
                if (cursor != null && cursor.moveToFirst()) {
                    rowCount = cursor.getCount();

                    while (cursor.isAfterLast() == false) {
                        HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, screenId);
                        table.add(row);
                        cursor.moveToNext();
                    }
                    cursor.close();

                    table = MetrixListScreenManager.performScriptListPopulation(this, screenId, screenName, table);
                }
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (rowCount > 0)
                recyclerView.setVisibility(View.VISIBLE);
            else
                recyclerView.setVisibility(View.GONE);

            if (metadataAdapter == null) {
                metadataAdapter = new MetadataAttachmentRecyclerViewAdapter(this, memoryCache, table, resourceData.get("R.layout.aapi_list_item"),
                        getTableLayoutID(), resourceData.get("R.layout.list_item_table_row"), resourceData.get("R.id.attachment_thumbnail"),
                        resourceData.get("R.color.IFSGold"), screenId, "attachment.metrix_row_id", this, this);
                recyclerView.setAdapter(metadataAdapter);
            } else {
                // We regenerated memoryCache earlier, so reset it on the adapter
                metadataAdapter.setMemoryCache(memoryCache);
                metadataAdapter.updateData(table);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        try {
            Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentFullScreen.class);
            intent.putExtra("position", position);
            intent.putExtra("isFromList", true);
            MetrixActivityHelper.startNewActivity(this, intent);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if (allowDelete) {
            try {
                selectedPosition = position;
                android.content.DialogInterface.OnClickListener deleteListener = (dialog, which) -> {
                    try {
                        HashMap<String, String> selectedItem = metadataAdapter.getListData().get(selectedPosition);
                        String linkTable = AttachmentWidgetManager.getLinkTable();
                        String attachmentId = selectedItem.get("attachment.attachment_id");
                        if (MetrixStringHelper.isNegativeValue(attachmentId)) {
                            // Try to get the updated value from mm_attachment_id_map
                            String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", attachmentId));
                            if (!MetrixStringHelper.isNullOrEmpty(candidateValue))
                                attachmentId = candidateValue;
                        }
                        String metrixRowId = AttachmentWidgetManager.getLinkTableMetrixRowId(attachmentId);
                        Hashtable<String, String> primaryKeys = AttachmentWidgetManager.generateLinkTablePrimaryKeyList(attachmentId);
                        MetrixTransaction transactionInfo = AttachmentWidgetManager.getTransactionInfo();

                        if (MetrixUpdateManager.delete(this, linkTable, metrixRowId, primaryKeys, AndroidResourceHelper.getMessage("Attachment"), transactionInfo)) {
                            metadataAdapter.getListData().remove(selectedPosition);
                            metadataAdapter.notifyItemRemoved(selectedPosition);
                        }
                    } catch (Exception e) {
                        LogManager.getInstance().error(e);
                    }
                };
                MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("AttachmentLCase"), deleteListener, null, this);
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        }
    }

    public void onListImageClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        try {
            String onDemand = listItemData.get(("attachment.on_demand"));
            String path = MetrixAttachmentHelper.getFilePathFromAttachment(listItemData.get("attachment.attachment_name"));
            if(!MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y") && !MetrixAttachmentHelper.isAttachmentExists(path)) {
                mInitAlert = new AlertDialog.Builder(this).create();
                mInitAlert.setTitle(AndroidResourceHelper.getMessage("DownloadConfirmation"));
                mInitAlert.setMessage(AndroidResourceHelper.getMessage("DownloadOnDemandAtt"));
                mInitAlert.setButton(AndroidResourceHelper.getMessage("YesButton"), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(SettingsHelper.getSyncPause(mCurrentActivity))
                        {
                            SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                            if(syncPauseAlertDialog != null)
                            {
                                syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                    @Override
                                    public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                        startDownloadAttachment(listItemData);
                                    }
                                });
                            }
                        }
                        else
                            startDownloadAttachment(listItemData);
                    }
                });
                mInitAlert.setButton2(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                mInitAlert.show();
            } else {
                Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentFullScreen.class);
                intent.putExtra("position", position);
                intent.putExtra("isFromList", true);
                MetrixActivityHelper.startNewActivity(this, intent);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }


    @SuppressLint("SdCardPath")
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == resourceData.get("R.id.next")) {
            next();
        } else
            super.onClick(v);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BASE_TAKE_PICTURE:
                if (resultCode == RESULT_OK) {
                    try {
                        Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentAdd.class);
                        intent.putExtra("ImageUri", baseMediaUri);
                        intent.putExtra("FromCamera", true);
                        intent.putExtra("isFromList", true);
                        MetrixActivityHelper.startNewActivity(this, intent);
                    } catch (Exception e) {
                        LogManager.getInstance().error(e);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    // Handle cancel ... do nothing
                }
                break;

            case BASE_TAKE_VIDEO:
                if (resultCode == RESULT_OK) {
                    try {
                        Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentAdd.class);
                        intent.putExtra("VideoUri", baseMediaUri);
                        intent.putExtra("FromVideoCamera", true);
                        intent.putExtra("isFromList", true);
                        MetrixActivityHelper.startNewActivity(this, intent);
                    } catch (Exception e) {
                        LogManager.getInstance().error(e);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    // Handle cancel ... do nothing
                }
                break;

            case BASE_SHOW_FILEDIALOG:
                if (resultCode == RESULT_OK) {
                    String fileName = data.getStringExtra("RESULT_PATH");
                    Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentAdd.class);
                    intent.putExtra("FileName", fileName);
                    intent.putExtra("FromFileDialog", true);
                    intent.putExtra("isFromList", true);
                    MetrixActivityHelper.startNewActivity(this, intent);
                } else if (resultCode == RESULT_CANCELED) {
                    // Handle cancel ... do nothing
                }
                break;
            case SELECT_FILES:
                if (requestCode == SELECT_FILES) {
                    if (resultCode == RESULT_OK) {
                        ArrayList<Uri> fileList = new ArrayList<Uri>();

                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            int currentItem = 0;
                            while (currentItem < count) {
                                Uri fileUri = data.getClipData().getItemAt(currentItem).getUri();
                                //String itemFilePath = AttachmentPathResolver.getPath(MobileApplication.getAppContext(), fileUri);
                                currentItem = currentItem + 1;
                                fileList.add(fileUri);
                            }
                        } else if (data.getData() != null) {
                            Uri fileUri = data.getData();
                            //String itemFilePath = AttachmentPathResolver.getPath(MobileApplication.getAppContext(), fileUri);
                            fileList.add(fileUri);
                        }

                        if (fileList == null)
                            return;

                        if (fileList.size() > 1) {
                            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // user need to go to app settings to enable it
                                    try {
                                        Intent intent = MetrixActivityHelper.createActivityIntent(FSMAttachmentList.this, FSMAttachmentAdd.class);
                                        intent.putExtra("FileUriList", fileList);
                                        intent.putExtra("multipleFiles", true);
                                        intent.putExtra("isFromList", true);
                                        MetrixActivityHelper.startNewActivity(FSMAttachmentList.this, intent);
                                    } catch (Exception ex) {
                                        // This is extremely rare
                                        LogManager.getInstance().error(ex);
                                    }
                                }
                            };

                            MetrixDialogAssistant.showAlertDialog(
                                    AndroidResourceHelper.getMessage("SelectFiles"),
                                    AndroidResourceHelper.getMessage("ConfirmMultiFileSelection", fileList.size()),
                                    AndroidResourceHelper.getMessage("Yes"),
                                    listener,
                                    AndroidResourceHelper.getMessage("No"),
                                    null,
                                    this
                            );
                        } else {
                            try {
                                if (fileList.size() == 1)
                                    for (Uri selectFilePath : fileList) {
                                        Intent intent = MetrixActivityHelper.createActivityIntent(this, FSMAttachmentAdd.class);
                                        intent.putExtra("FileUri", selectFilePath);
                                        intent.putExtra("FromFileUri", true);
                                        intent.putExtra("isFromList", true);
                                        MetrixActivityHelper.startNewActivity(this, intent);
                                    }
                            } catch (Exception ex) {
                                // This is extremely rare
                                LogManager.getInstance().error(ex);
                            }
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BASE_PICTURE_CAMERA_PERMISSION || requestCode == BASE_VIDEO_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == BASE_PICTURE_CAMERA_PERMISSION)
                    openCameraForPicture();
                else
                    openCameraForVideo();
            } else {
                // Showing dialog at this Activity lifecycle can lead to app crash as the view is not guaranteed to
                // be visible to the user. So we set the incompletePermissionRequest value and handle it inside
                // onResume activity life cycle
                incompletePermissionRequest = requestCode;
            }
        }
    }

    private void openCameraForPicture() {
        try {
            String fileName = AttachmentWidgetManager.generateFileName("jpg") ;
            baseMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, baseMediaUri);
        startActivityForResult(intent, BASE_TAKE_PICTURE);
    }

    private void openCameraForVideo() {
        try {
            String fileName = AttachmentWidgetManager.generateFileName("mp4");
            baseMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
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
        // start the Video Capture Intent
        startActivityForResult(intent, BASE_TAKE_VIDEO);
    }

    private void next() {
        if (super.scriptEventConsumesClick(this, "BUTTON_NEXT"))
            return;
        MetrixWorkflowManager.advanceWorkflow(this);
    }

    private void startDownloadAttachment(final HashMap<String, String> selectedItem) {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
                String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));
                String cloudDownload = SettingsHelper.getStringSetting(mCurrentActivity, SettingsHelper.IS_AZURE);

                if (!MetrixStringHelper.isNullOrEmpty(cloudDownload) && cloudDownload.toUpperCase().equals("Y"))
                {
                    String token = getToken(baseUrl, remote);

                    //token should include "sv=" as signed version, otherwise invalid
                    if (MetrixStringHelper.isNullOrEmpty(token) || token.contains("sv=")==false) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mCurrentActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                MetrixUIHelper.showSnackbar(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceNotAccessible"));
                            }
                        });

                        return;
                    }
                    processOnDemandDownloads(selectedItem, token);
                }
                else {
                    processOnDemandDownloads(selectedItem, "");
                }
                if (mInitAlert != null) {
                    mInitAlert.dismiss();
                }
            }
        });

        thread.start();
    }

    private void processOnDemandDownloads(HashMap<String, String> selectedItem, String token) {
        int downloadCount = 0;
        final String azureToken = token;

        Hashtable<String, String> downloadFiles = new Hashtable<String, String>();

        if (selectedItem != null) {

            String onDemand = selectedItem.get("attachment.on_demand");
            final String attachmentId = selectedItem.get("attachment.attachment_id");
            final String attachmentName = selectedItem.get("attachment.attachment_name");

            if(!MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y")){

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
        bindService(new Intent(FSMAttachmentList.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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

                            Intent intent = MetrixActivityHelper.createActivityIntent(FSMAttachmentList.this, FSMAttachmentList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            MetrixActivityHelper.startNewActivity(FSMAttachmentList.this, intent);
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

            Intent intent = MetrixActivityHelper.createActivityIntent(FSMAttachmentList.this, FSMAttachmentList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivity(FSMAttachmentList.this, intent);
        }
        return refreshUI;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }
}
