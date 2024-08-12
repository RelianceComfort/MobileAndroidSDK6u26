package com.metrix.metrixmobile;

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
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.collection.LruCache;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MessageHandler;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.ui.widget.FileDialog;
import com.metrix.architecture.ui.widget.MetrixGalleryView;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCameraHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixParcelable;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.PermissionHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.metrixmobile.system.MetrixPooledTaskAssignmentManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

@SuppressLint("DefaultLocale")
public class DebriefTaskAttachment extends DebriefTaskAttachmentActivity implements AdapterView.OnItemClickListener, OnClickListener, OnItemLongClickListener {

	private MetrixGalleryView mMetrixGalleryView;
	private static final int SHOW_FILEDIALOG = 123;
	private static final int TAKE_PICTURE = 456;
	private static final int TAKE_VIDEO = 999;
    private static final int PICTURE_CAMERA_PERMISSION = 1337;
    private static final int VIDEO_CAMERA_PERMISSION = 1338;
	private ImageButton mImageButtonCamera, mImageButtonFile, mImageButtonVideoCamera;
	private FloatingActionButton mSaveButton, mNextButton;
	@SuppressWarnings("unused")
	private static String currentMediaName = "";
	private static Uri mMediaUri;
	private static boolean comingViaWorkflow;
	private LruCache<String, Bitmap> mMemoryCache;
	private AlertDialog mInitAlert;
	private int incompletePermissionRequest = 0;

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_task_attachment_list);
		else
			setContentView(R.layout.debrief_task_attachment_list);

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mParentActivity = DebriefTaskAttachmentAdd.class;

		mMetrixGalleryView = (MetrixGalleryView) findViewById(R.id.attachment_gallery);

		mImageButtonCamera = (ImageButton) findViewById(R.id.attachment_imagebtn_camera);
		mImageButtonFile = (ImageButton) findViewById(R.id.attachment_imagebtn_file);
		mImageButtonVideoCamera = (ImageButton) findViewById(R.id.attachment_imagebtn_video_camera);

		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mSaveButton = (FloatingActionButton) findViewById(R.id.save);

	}

	public void onStart() {
		super.onStart();

		((FloatingActionButton)findViewById(R.id.save)).hide(); //mSaveButton.hide();

		// if it's from the QuickLinksBar, then treat it as not via workflow
		comingViaWorkflow = true;
		MetrixParcelable<Boolean> metrixParcelable = getIntent().getParcelableExtra("fromQuickLinksBar");
		if (metrixParcelable != null)
			comingViaWorkflow = !(metrixParcelable.getValue());

		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

		final int cacheSize = maxMemory / 8;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
			}
		};

		setupActionBar();
		populateGallery();

		showHideViews(comingViaWorkflow);
		setNavigation();

		ifPooledTaskDisableAttachmentCapturing();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (!taskIsComplete()) {
			mImageButtonCamera.setOnClickListener(this);
			mImageButtonFile.setOnClickListener(this);
			mImageButtonVideoCamera.setOnClickListener(this);

			if(!MetrixPooledTaskAssignmentManager.instance().isPooledTask(MetrixCurrentKeysHelper.getKeyValue("task", "task_id"))) {
				mMetrixGalleryView.setLongClickable(true);
				mMetrixGalleryView.setOnItemLongClickListener(this);
			}
		}else{
			//hide attachemnts add options when the task status is "Completed". Changes made to make t looks like in FSMi.
			mImageButtonCamera.setVisibility(View.GONE);
			mImageButtonFile.setVisibility(View.GONE);
			mImageButtonVideoCamera.setVisibility(View.GONE);
		}
		mMetrixGalleryView.setOnItemClickListener(this);
		mNextButton.setOnClickListener(this);
	}

	/**
	 * Populate the attachment list in Gallery Mode.
	 */
	private void populateGallery() {

		List<HashMap<String, String>> data = getAttachmentData();
		if(isTabletSpecificLandscapeUIRequired())
			mMetrixGalleryView.setDatasource(getApplicationContext(), "task_attachment.metrix_row_id", "attachment.attachment_name", data, mMemoryCache, true);
		else
			mMetrixGalleryView.setDatasource(getApplicationContext(), "task_attachment.metrix_row_id", "attachment.attachment_name", data, mMemoryCache);
	}

	/**
	 * Reset navigation based on allowed task_status
	 */
	protected void setNavigation() {
		boolean statusFound = false;
		String allowedStatusParam = MetrixDatabaseManager.getAppParam("DEBRIEF_ALLOWED_STATUSES");
		if (!MetrixStringHelper.isNullOrEmpty(allowedStatusParam)) {
			String[] allowedStatuses = allowedStatusParam.split(",");
			try {
				String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
				String currentStatus = MetrixDatabaseManager.getFieldStringValue("task", "task_status", "task_id = " + taskId);
				if (allowedStatuses != null) {
					for (String allowedStatus : allowedStatuses) {
						if (allowedStatus.trim().compareToIgnoreCase(currentStatus) == 0) {
							statusFound = true;
							break;
						}
					}
				}
			} catch (Exception ex) {}
		} else
			statusFound = true;

		if (statusFound == false) {
			if (comingViaWorkflow)
				MetrixControlAssistant.setButtonVisibility(mSaveButton, View.GONE);
			else {
				View addNextBar = findViewById(R.id.add_next_bar);
				addNextBar.setVisibility(View.GONE);
			}
			mDisableContextMenu = true;
			MetrixActionBarManager.getInstance().disableMenuButton(this);
			setTabletUILeftMenuVisible(false);
		} else
			setTabletUILeftMenuVisible(true);
	}

	@Override
	protected void bindService() {
		bindService(new Intent(DebriefTaskAttachment.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
							MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PTaskAssignmentAlreadyAssigned"));


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

							Intent intent = MetrixActivityHelper.createActivityIntent(DebriefTaskAttachment.this, DebriefTaskAttachment.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
							MetrixActivityHelper.startNewActivity(DebriefTaskAttachment.this, intent);
						}

					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};


	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
							int position, long id) {
		final HashMap<String, String> selectedItem = (HashMap<String, String>) mMetrixGalleryView.getItem(position);

		if(selectedItem != null) {
			int downloadCount = 0;
			Hashtable<String, String> downloadFiles = new Hashtable<String, String>();

			String onDemand = selectedItem.get("attachment.on_demand");
			final String attachmentId = selectedItem.get("attachment.attachment_id");
			final String attachmentName = selectedItem.get("attachment.attachment_name");

			if(!MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y")){
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
										startDownloadAttachment(selectedItem);
									}
								});
							}
						}
						else
							startDownloadAttachment(selectedItem);
					}
				});
				mInitAlert.setButton2(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				mInitAlert.show();

			}
			else {
				Intent intent = MetrixActivityHelper.createActivityIntent(this,
						DebriefTaskAttachmentFullScreen.class);
				intent.putExtra("position", position);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		}
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
								MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MobileServiceNotAccessible"));
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
				MetrixDatabaseManager.executeSql("update attachment set on_demand = 'N' where attachment_id =" + attachmentId);

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
							MessageHandler.downloadAttachment(downloadPath, attachmentName, attachmentId, false);
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

    private void openCameraForPicture() {
        String time_stamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
        String fileName = "task_" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id") + "_" + time_stamp + ".jpg";

        try {
            mMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void openCameraForVideo() {
		String time_stamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
		String fileName = "task_" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id") + "_" + time_stamp + ".mp4";

		try {
			mMediaUri = MetrixFileHelper.getOutputMediaFileUri(fileName);
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
			return;
		}  // create a file to save the video

		String maximumSize = MobileApplication.getAppParam("ATTACHMENT_MAX_SIZE");
		if (MetrixStringHelper.isNullOrEmpty(maximumSize)) {
			maximumSize = "10242880";
		}

		long sizeLimit = Long.parseLong(maximumSize);

		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);  // set the image file name
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
		intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);
		intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3600);
		intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		// start the Video Capture Intent
		startActivityForResult(intent, TAKE_VIDEO);
	}

	@SuppressLint("SdCardPath")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.attachment_imagebtn_file:
				Intent intent = new Intent(this, FileDialog.class);
				String startPath = MetrixFileHelper.getPublicMediaFolderPath();

				intent.putExtra("START_PATH", startPath);
				startActivityForResult(intent, SHOW_FILEDIALOG);
				break;

			case R.id.attachment_imagebtn_camera:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                	if (PermissionHelper.checkPublicFilePermission(this)) {
						openCameraForPicture();
					} else {
                		PermissionHelper.requestPublicFilePermission(this);
					}

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            PICTURE_CAMERA_PERMISSION);
                }
				break;

			case R.id.attachment_imagebtn_video_camera:
				if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
						== PackageManager.PERMISSION_GRANTED) {
					if (PermissionHelper.checkPublicFilePermission(this)) {
						openCameraForVideo();
					} else {
						PermissionHelper.requestPublicFilePermission(this);
					}
				} else {
					ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
							VIDEO_CAMERA_PERMISSION);
				}
				break;

			case R.id.next:
				next();
				break;

			default:
				super.onClick(v);
		}
	}

	private String getMimeType(String filePath) {
		File file = new File(filePath);
		MimeTypeMap map = MimeTypeMap.getSingleton();
		String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
		String type = map.getMimeTypeFromExtension(ext);

		if (type == null)
			type = "*/*";

		return type;
	}

	private void next() {
		if(super.scriptEventConsumesClick(this, mNextButton)) return;
		MetrixWorkflowManager.advanceWorkflow(this);
	}

	@SuppressWarnings("unused")
	private File createImageFile(int mediaType) throws IOException {
		// Create an image file name
		String timeStamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
		String imageFileName = "task_" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id") + "_" + timeStamp;
		File storageDir = new File(MetrixAttachmentManager.getInstance().getAttachmentPath());
		File mediaFile = null;

		if(mediaType == MetrixCameraHelper.getMediaTypeImage())
			mediaFile = File.createTempFile(
					imageFileName,  /* prefix */
					".jpg",         /* suffix */
					storageDir      /* directory */
			);
		else
			mediaFile = File.createTempFile(
					imageFileName,  /* prefix */
					".mp4",         /* suffix */
					storageDir      /* directory */
			);

		// Save a file: path for use with ACTION_VIEW intents
		currentMediaName = mediaFile.getAbsolutePath();
		return mediaFile;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case TAKE_PICTURE:
				if (resultCode == RESULT_OK) {
					try {
						Intent intent = MetrixActivityHelper.createActivityIntent(this,
								DebriefTaskAttachmentAdd.class);
						intent.putExtra("ImageUri", mMediaUri);
						intent.putExtra("FromCamera", true);

						MetrixActivityHelper.startNewActivity(this, intent);
					}
					catch(Exception ex){
						LogManager.getInstance().error(ex);
					}
					// Handle successful scan
				} else if (resultCode == RESULT_CANCELED) {
					// Handle cancel
				}
				break;

			case TAKE_VIDEO:
				if (resultCode == RESULT_OK) {
					try {
						Intent intent = MetrixActivityHelper.createActivityIntent(this,
								DebriefTaskAttachmentAdd.class);
						intent.putExtra("VideoUri", mMediaUri);
						intent.putExtra("FromVideoCamera", true);
						MetrixActivityHelper.startNewActivity(this, intent);
					}
					catch(Exception ex) {
						LogManager.getInstance().error(ex);
					}
					// Handle successful scan
				} else if (resultCode == RESULT_CANCELED) {
					// Handle cancel
				}
				break;

			case SHOW_FILEDIALOG:
				if (resultCode == RESULT_OK) {
					String fileName = data.getStringExtra("RESULT_PATH");
					Intent intent = MetrixActivityHelper.createActivityIntent(this,
							DebriefTaskAttachmentAdd.class);
					intent.putExtra("FileName", fileName);
					intent.putExtra("FromFileDialog", true);
					MetrixActivityHelper.startNewActivity(this, intent);

					// Handle successful scan
				} else if (resultCode == RESULT_CANCELED) {
					// Handle cancel
				}
				break;
		}
	}

	public boolean hasImageCaptureBug() {
		// list of known devices that have the bug
		ArrayList<String> devices = new ArrayList<String>();
		devices.add("android-devphone1/dream_devphone/dream");
		devices.add("generic/sdk/generic");
		devices.add("vodafone/vfpioneer/sapphire");
		devices.add("tmobile/kila/dream");
		devices.add("verizon/voles/sholes");
		devices.add("google_ion/google_ion/sapphire");

		return devices.contains(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/"
				+ android.os.Build.DEVICE);

	}

	/***
	 * If we visit this screen via workflow it won't show the two buttons with icons(camera, file).
	 * @param viaWorkFlow
	 */
	private void showHideViews(boolean viaWorkFlow) {

		if(viaWorkFlow){
			MetrixControlAssistant.setButtonVisibility(mSaveButton, View.GONE);
		}
		else{
			View addNextBar = findViewById(R.id.add_next_bar);
			addNextBar.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

		final HashMap<String, String> selectedItem = (HashMap<String, String>) mMetrixGalleryView.getItem(position);

		if(selectedItem != null){

			String attachmentPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + selectedItem.get("attachment.attachment_name");

			if(!MetrixStringHelper.isNullOrEmpty(attachmentPath)){

				File attachmentFile = new File(attachmentPath);
				if (attachmentFile.exists()) {

					boolean isImageFile = MetrixAttachmentHelper.checkImageFile(attachmentPath);

					android.content.DialogInterface.OnClickListener modifyListener = new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							try {

								modifyAction(selectedItem);

							} catch (Exception e) {
								LogManager.getInstance().error(e);
							}
						}
					};

					android.content.DialogInterface.OnClickListener deleteListener = new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try {

								deleteAction(selectedItem, mMetrixGalleryView);

								refreshDebriefNavigationList();

							} catch (Exception e) {
								LogManager.getInstance().error(e);
							}
						}

					};

					if(isImageFile)
						MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("AttachmentLCase"), modifyListener, deleteListener, this);
					else
						MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("AttachmentLCase"), deleteListener, null, this);

					//consumed the event and it should not be carried further. 
					return true;
				}
			}
		}
		return true;
	}

	private void ifPooledTaskDisableAttachmentCapturing() {
		if(MetrixPooledTaskAssignmentManager.instance().isPooledTask(MetrixCurrentKeysHelper.getKeyValue("task", "task_id")))
		{
			if(mImageButtonCamera == null)
				mImageButtonCamera = (ImageButton) findViewById(R.id.attachment_imagebtn_camera);
			mImageButtonCamera.setEnabled(false);

			if(mImageButtonFile == null)
				mImageButtonFile = (ImageButton) findViewById(R.id.attachment_imagebtn_file);
			mImageButtonFile.setEnabled(false);

			if(mImageButtonVideoCamera == null)
				mImageButtonVideoCamera = (ImageButton) findViewById(R.id.attachment_imagebtn_video_camera);
			mImageButtonVideoCamera.setEnabled(false);
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
    protected void onResume() {
		super.onResume();
        if (incompletePermissionRequest == PICTURE_CAMERA_PERMISSION || incompletePermissionRequest == VIDEO_CAMERA_PERMISSION) {
            final int requestCode = incompletePermissionRequest;
            incompletePermissionRequest = 0;
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(DebriefTaskAttachment.this, Manifest.permission.CAMERA)) {
                        // can request permission again
                        ActivityCompat.requestPermissions(DebriefTaskAttachment.this, new String[]{Manifest.permission.CAMERA}, requestCode);
                    } else {
                        // user need to go to app settings to enable it
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            // This is extremely rare
							MetrixUIHelper.showSnackbar(DebriefTaskAttachment.this, R.id.coordinator_layout, AndroidResourceHelper
									.getMessage("EnablePermManuallyAndRetry"));
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICTURE_CAMERA_PERMISSION || requestCode == VIDEO_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            	if (requestCode == PICTURE_CAMERA_PERMISSION)
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
}
