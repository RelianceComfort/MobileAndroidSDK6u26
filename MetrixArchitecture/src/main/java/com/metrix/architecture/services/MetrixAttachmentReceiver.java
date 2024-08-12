package com.metrix.architecture.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixFieldLookupManager;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.designer.MetrixFilterSortManager;
import com.metrix.architecture.designer.MetrixGlobalMenuManager;
import com.metrix.architecture.designer.MetrixHomeMenuManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.notification.FSMNotificationAssistant;
import com.metrix.architecture.notification.PushRegistrationManager;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCompressionHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPrivateCache;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

public class MetrixAttachmentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent intent) {
		// TODO Auto-generated method stub
		//check if the broadcast message is for our Enqueued download
		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long downloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);

			Query query = new Query();
			query.setFilterById(downloadId);

			Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
			String appFolder = MetrixApplicationAssistant.getMetaStringValue(mCtxt, "DirectoryName");
			String destDirectory = MetrixAttachmentManager.getInstance().getAttachmentPath();

			DownloadManager downloadManager = (DownloadManager)mCtxt.getSystemService(Context.DOWNLOAD_SERVICE);

			long dbDownloadId = -1;

			if(MetrixPublicCache.instance.containsKey("DownloadDatabaseId"))
				dbDownloadId = Long.parseLong((String)MetrixPublicCache.instance.getItem("DownloadDatabaseId"));

			Cursor c = downloadManager.query(query);
			if (c != null && c.moveToFirst()) {
				int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int columnUriIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

				int status = c.getInt(columnIndex);
				String localUriString = c.getString(columnUriIndex);

				if (DownloadManager.STATUS_FAILED == status) {
					// Failed : file on the server not found.
					String attachmentIdX = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
					String fileNameX = c.getString(c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));

					if (!MetrixStringHelper.isNullOrEmpty(fileNameX)) {

						if (MetrixPublicCache.instance.containsKey("DOWNLOAD_FILES")) {
							// Note: key = attachmentName, Value = attachmentId
							Hashtable<String, String> downloadFiles = (Hashtable<String, String>) MetrixPublicCache.instance.getItem("DOWNLOAD_FILES");

							if (downloadFiles != null && downloadFiles.size() > 0) {
								// Also, update the DB with change to attachment status.
								String attachmentId = downloadFiles.get(fileNameX);
								MetrixDatabaseManager.executeSql("UPDATE attachment SET on_demand = 'N' WHERE attachment_id =" + attachmentId);

								if (downloadFiles.containsKey(fileNameX))
									downloadFiles.remove(fileNameX);

								addToFailedDownloadFiles(attachmentIdX, fileNameX);
							}
						}
					}
				}

				// the download files have to be related to IFS app, otherwise ignore.
				if(MetrixStringHelper.isNullOrEmpty(localUriString) || localUriString.contains(appFolder)==false)
					return;
				String person_id = SettingsHelper.getActivatedUser(mCtxt); //User.getUser().personId;
				int device_id = SettingsHelper.getDeviceSequence(mCtxt);
				String expectedDBFile = person_id+"__"+device_id+".db";
				String expectedDBZipPath = destDirectory+"/"+person_id+"__"+device_id+".zip";

				Uri filePathUri = Uri.parse(localUriString);
				String filePath = filePathUri.getPath();
				String fileName = filePathUri.getLastPathSegment();

				if(dbDownloadId > 0 && downloadId == dbDownloadId) {
					if (DownloadManager.STATUS_SUCCESSFUL == status) {
						synchronized (this) {
							try {
								if (fileName.contains(".zip")) {
									try {
										MetrixCompressionHelper.unzip(filePath, destDirectory);
									} catch (Exception ex) {
										// it might be unzipped by downloadManager of the device automatically
									}

									MetrixFileHelper.deleteFile(filePath);
								}
							} catch (Exception e) {
								LogManager.getInstance().error(e);
							}
							finally {
								if (fileName.contains(person_id + "__" + device_id)) {
									ArrayList<String> dbFiles = MetrixFileHelper.getFilesFromDirectory(destDirectory, ".db");
									if (dbFiles.contains(expectedDBFile)) {
										MetrixPublicCache.instance.addItem("DatabaseDownloaded", "Y");
										MetrixPublicCache.instance.removeItem("DownloadDatabaseId");
									}
								}
							}
						}
					} else if (status == DownloadManager.STATUS_FAILED) {
						LogManager.getInstance().error("Database Download error code: " +c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
						// download failed, it is likely downloading passed maximum retry thresholds
						MetrixSyncManager.onSyncedNotification(ActivityType.InitializationEnded, AndroidResourceHelper.getMessage("InitializationEnded"));
					} else {
						LogManager.getInstance().error("Database Download error code: " +c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
						if (fileName.contains(person_id + "__" + device_id)) {
							ArrayList<String> dbFiles = MetrixFileHelper.getFilesFromDirectory(destDirectory, ".db");
							if (dbFiles.contains(expectedDBFile)) {
								MetrixPublicCache.instance.addItem("DatabaseDownloaded", "Y");
								MetrixPublicCache.instance.removeItem("DownloadDatabaseId");
							}
						}
					}
				} else {
					if (DownloadManager.STATUS_SUCCESSFUL == status) {
						synchronized (this) {
							try {
								if (fileName.contains(".zip")) {
									try {
										MetrixCompressionHelper.unzip(filePath, destDirectory);
									} catch (Exception ex) {
										// it might be unzipped by downloadManager of the device automatically
									}

									MetrixFileHelper.deleteFile(filePath);
								}
							} catch (Exception e) {
								LogManager.getInstance().error(e);
							}
						}
					}
				}

				if(allFilesDownloaded(fileName)) {
					if(MetrixPublicCache.instance.containsKey("DOWNLOAD_FILES")) {
						//MetrixSyncManager.onSyncedNotification(ActivityType.Download, "On_Demand Completed");
						MetrixPublicCache.instance.removeItem("DOWNLOAD_FILES");
						MetrixPublicCache.instance.addItem("OnDemandCompleted", "Y");
					}
				}
			}
		}

	}

	private boolean allFilesDownloaded(String currentFile) {
		Hashtable<String, String> downloadFiles = new Hashtable<String, String>();

		if(MetrixPublicCache.instance.containsKey("DOWNLOAD_FILES"))
			downloadFiles = (Hashtable<String, String>)MetrixPublicCache.instance.getItem("DOWNLOAD_FILES");

		if(downloadFiles != null && downloadFiles.size()>0) {
			if(downloadFiles.containsKey(currentFile))
				downloadFiles.remove(currentFile);
		}

		if(downloadFiles == null || downloadFiles.isEmpty())
			return true;

		return false;
	}

	private void addToFailedDownloadFiles(String fileName, String attachmentId) {
		Hashtable<String, String> downloadFiles = new Hashtable<String, String>();

		if (MetrixPublicCache.instance.containsKey("FAILED_DOWNLOAD_FILES"))
			downloadFiles = (Hashtable<String, String>) MetrixPublicCache.instance.getItem("FAILED_DOWNLOAD_FILES");

		if (!downloadFiles.containsKey(fileName))
			downloadFiles.put(fileName, attachmentId);

		MetrixPublicCache.instance.addItem("FAILED_DOWNLOAD_FILES", downloadFiles);
	}

	public static void processDatabase() {
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
		String destDirectory = MetrixAttachmentManager.getInstance().getAttachmentPath();

		try {
			String person_id = SettingsHelper.getActivatedUser(mCtxt); //User.getUser().personId;
			int device_id = SettingsHelper.getDeviceSequence(mCtxt);
			String expectedDBFile = person_id+"__"+device_id+".db";
			String dbPath = destDirectory+"/"+expectedDBFile;

			MetrixDatabaseManager.closeDatabase();
			MetrixDatabaseManager.importDatabase(dbPath, mCtxt);

			//MetrixDatabaseManager.executeSql("");
			boolean recreateTable = false;
			MetrixPrivateCache.resetDatabase(recreateTable);

			String enable_time_zone = MobileApplication.getAppParam("MOBILE_ENABLE_TIME_ZONE");
			if (!MetrixStringHelper.isNullOrEmpty(enable_time_zone) && enable_time_zone.toLowerCase().contains("y"))
				Global.enableTimeZone = true;
			else
				Global.enableTimeZone = false;

			String encode_url = MobileApplication.getAppParam("MOBILE_ENCODE_URL_PARAM");
			if (!MetrixStringHelper.isNullOrEmpty(encode_url) && encode_url.toLowerCase().contains("y"))
				Global.encodeUrl = true;
			else
				Global.encodeUrl = false;

			String personId = (String)MetrixPublicCache.instance.getItem("person_id");
			String hashPassword = SettingsHelper.getStringSetting(mCtxt, SettingsHelper.USER_LOGIN_PASSWORD);

			String statement = "insert into user_credentials(person_id, password) values('"+personId+"','"+hashPassword+"')";
			MetrixDatabaseManager.executeSql(statement);
			MobileApplication.saveTableDefinitionToCache();

			refreshMobileDesignMetadataCaches();
			MetrixGlobalMenuManager.populateDesignerGlobalMenuResources(true);
			MetrixGlobalMenuManager.cacheGlobalMenuItems();

			String azureSuccess = SettingsHelper.getStringSetting(mCtxt, SettingsHelper.AZURE_AD_SUCCESS);
			if (!MetrixStringHelper.isNullOrEmpty(azureSuccess) && azureSuccess.equalsIgnoreCase("y")) {
				String ssoId = SettingsHelper.getStringSetting(mCtxt, SettingsHelper.SSO_ID);
				String ssoDomain = SettingsHelper.getStringSetting(mCtxt, SettingsHelper.SSO_DOMAIN);
				if (!MetrixStringHelper.isNullOrEmpty(ssoId))
					MetrixApplicationAssistant.updateSSOId(personId, ssoId, ssoDomain);
			}

			Hashtable<String, String> downloadCache = (Hashtable<String, String>)MetrixPublicCache.instance.getItem("DOWNLOAD_PATH_ATTACHMENTS");
			if (downloadCache!= null && downloadCache.size() > 0) {
				for (String aId : downloadCache.keySet()) {
					String aPath = downloadCache.get(aId);
					String sqlStatement = "update attachment set download_path='" + aPath + "' where attachment_id=" + aId;
					MetrixDatabaseManager.executeSql(sqlStatement);
				}

				MetrixPublicCache.instance.removeItem("DOWNLOAD_PATH_ATTACHMENTS");
			}

			if (MobileApplication.PerformingActivation) {
				MobileApplication.PerformingActivation = false;
				PushRegistrationManager pushMgr = new PushRegistrationManager();
				pushMgr.initNotifications();
				FSMNotificationAssistant.recordUserNotificationPermission();
			}
		} catch (FileNotFoundException e) {
			LogManager.getInstance().error(e);
		} catch (IOException e) {
			LogManager.getInstance().error(e);
		}
	}

	private static void refreshMobileDesignMetadataCaches() {
		// regenerate any/all caches of design metadata, so that the cache has the latest metadata (useful after a refresh)
		MetrixClientScriptManager.clearClientScriptCache();
		MetrixFieldManager.clearDefaultValuesCache();
		MetrixFieldManager.clearFieldPropertiesCache();
		MetrixFieldLookupManager.clearFieldLookupCache();
		MetrixGlobalMenuManager.cacheGlobalMenuItems();
		MetrixHomeMenuManager.cacheHomeMenuItems();
		MetrixFilterSortManager.clearFilterSortCaches();
		MetrixListScreenManager.clearItemFieldPropertiesCache();
		MetrixScreenManager.clearScreenIdsCache();
		MetrixSkinManager.cacheSkinItems();
		MetrixWorkflowManager.clearAllWorkflowCaches();
		MetrixScreenManager.clearScreenPropertiesCache();
		MetrixTabScreenManager.clearTabScreensCache();
		/**Store login image id and necessary skin information*/
		SettingsHelper.storeLoginImageInformation();

		String query = "SELECT use_mm_workflow.workflow_id FROM use_mm_workflow";

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (!(cursor == null || !cursor.moveToFirst())) {
				while (cursor.isAfterLast() == false) {
					String workflowId = cursor.getString(0);
					MetrixWorkflowManager.cacheScreensForWorkflow(workflowId);
					MetrixWorkflowManager.cacheScreensForWorkflowJumpToMenus(workflowId);

					cursor.moveToNext();
				}
			}
		}
		catch(Exception ex)
		{
			LogManager.getInstance().error(ex);
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
