package com.metrix.architecture.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.SQLException;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.metrix.architecture.BuildConfig;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.constants.MetrixTransactionTypesConverter;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.notification.FSMNotificationAssistant;
import com.metrix.architecture.services.MetrixIntentService.Caller;
import com.metrix.architecture.services.RemoteMessagesHandler.HandlerException;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.Global.MessageInStatus;
import com.metrix.architecture.utilities.Global.MessageStatus;
import com.metrix.architecture.utilities.Global.MessageType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPasswordHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static android.content.Context.ACTIVITY_SERVICE;

public class MetrixSyncManager {
	private final MetrixRemoteExecutor mRemoteExecutor;
	private final int mDeviceId;
	private final boolean isRunningFromBackgroundWorker;
	private final String mUserId;
	private final String mServiceBaseUrl;

	private boolean mPasswordChangeInProgress = false;
	public boolean mAllowSync = true;
	private Hashtable<String, String> initializationAttachments;

	// Properties added to implement Fast Mode
	public static boolean mClientHadMessagesToSend;
	public static boolean mMostRecentPingSuccessful = false;
	public static boolean mUsingFastMode = false;

	private static boolean _activationVerified;
	private static boolean _reactivationPopupShowing = false;

	private static boolean mIsSyncing = false;
	private static final Object lock = new Object();

	public static Map<IPostListener, Caller> mCallers;

	public MetrixSyncManager(@NonNull String serviceBaseUrl, @NonNull String userId, int deviceId, @NonNull MetrixRemoteExecutor remoteExecutor, boolean isRunningFromBackgroundWorker) {
		this.mServiceBaseUrl = serviceBaseUrl;
		this.mRemoteExecutor = remoteExecutor;
		this.mDeviceId = deviceId;
		this.mUserId = userId;
		this.isRunningFromBackgroundWorker = isRunningFromBackgroundWorker;
	}

	/**
	 * Performs a Sync with the Remote Restful Web Service.
	 */
	public SyncStatus sync() {
		mPasswordChangeInProgress = SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.PCHANGE);
		mAllowSync = Boolean.parseBoolean(MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("EnableSyncProcess")));

		if (!isRunningFromBackgroundWorker && MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("DatabaseDownloaded")).compareToIgnoreCase("Y")==0) {
			final Context appContext = MobileApplication.getAppContext();
			try {
				DatabaseLoadTask loadTask = new DatabaseLoadTask();
				boolean result = loadTask.execute(this).get();

				if (result) {
					MobileApplication.stopSync(appContext);
					MobileApplication.startSync(appContext); // this method will use the sync_interval from the setting
					
					this.onSynced(ActivityType.InitializationEnded, MetrixStringHelper.getArchitectureString("InitializationEnded"));
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() { 
						MetrixLocationAssistant.startLocationManager(appContext);
					}
				});
			}
		}

		if (!isRunningFromBackgroundWorker && MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("OnDemandCompleted")).compareToIgnoreCase("Y")==0) {
			MetrixSyncManager.onSyncedNotification(ActivityType.Download, "On_Demand Completed");
			MetrixPublicCache.instance.removeItem("OnDemandCompleted");
		}

		if ((!mAllowSync) || mIsSyncing || mPasswordChangeInProgress)
			return SyncStatus.STOP;

		try {
			synchronized (lock) {
				mClientHadMessagesToSend = false;
				_activationVerified = true;
				setIsSyncRunning(true);
				
				this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("StartingSync"));
				this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("PingingService"));

				if (this.mobilePing()) {
					if (!_activationVerified) {
						if (!isRunningFromBackgroundWorker)
							this.forceReactivation();	// only force reactivation in the foreground

						return SyncStatus.STOP;
					}

					this.signForStoredReceipts();

					if (!isRunningFromBackgroundWorker && MetrixPublicCache.instance.getItem("INIT_STARTED") != null && (Boolean)MetrixPublicCache.instance.getItem("INIT_STARTED")) {
						this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("FetchingWaitingTransactions"));
						this.fetchTransactions();	// Ignore SyncStatus here, because this is always run in the Foreground
					} else {
						this.onSynced(ActivityType.Information,	MetrixStringHelper.getArchitectureString("FetchingMailReceipts"));
						this.fetchMailReceipts();

						this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("FetchingWaitingTransactions"));
						SyncStatus firstFetchOutput = this.fetchTransactions();
						if (isRunningFromBackgroundWorker && firstFetchOutput == SyncStatus.STOP)
							return SyncStatus.STOP;

						mPasswordChangeInProgress =SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.PCHANGE);
						if(mPasswordChangeInProgress)
							return SyncStatus.STOP;

						this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("SendingWaitingTransactions"));
						this.sendTransactions();

						this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("FetchingWaitingTransactions"));
						SyncStatus secondFetchOutput = this.fetchTransactions();
						if (isRunningFromBackgroundWorker && secondFetchOutput == SyncStatus.STOP)
							return SyncStatus.STOP;
					}
				}
			}
		} catch (Exception ex) {
			this.onSynced(ActivityType.Information, ex.getMessage());
			LogManager.getInstance().error(ex);

			// If any exception is encountered during Background Sync, stop immediately after recording the exception in the log
			if (isRunningFromBackgroundWorker)
				return SyncStatus.STOP;
		} finally {
			this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("FinishedSync"));
			setIsSyncRunning(false);
			System.gc();
		}

		// If we are doing Background Sync, send the signal to do another Sync process if ping is good and we have messages ready to send
		if (isRunningFromBackgroundWorker && mMostRecentPingSuccessful && MessageHandler.messagesAreQueuedWithoutErrors())
			return SyncStatus.KEEP_RUNNING;

		if (!isRunningFromBackgroundWorker) {
			// Handle Fast Mode, based on current state of Mobile Client and messages
			if (mUsingFastMode && (!mClientHadMessagesToSend || !mMostRecentPingSuccessful))
				toggleFastMode(false);
			else if (!mUsingFastMode && mMostRecentPingSuccessful && MessageHandler.messagesAreQueuedWithoutErrors())
				toggleFastMode(true);
		}

		return SyncStatus.STOP;
	}
	
	public static void onSyncedNotification(ActivityType activityType, String message) {
		String messageToDisplay;
		
		// a special rule that all perform result will send the completed message to UI 
		if (message.length() > 100 && !message.contains("perform_")) {
			messageToDisplay = message.substring(0, 100);
		} else if (message.contains(MessageHandler.getIfsGetPrefix())) {
			int startPos = message.indexOf(MessageHandler.getIfsGetPrefix()) + MessageHandler.getIfsGetPrefix().length();
			int endPos = message.indexOf("_result\"", startPos);
			String table_name = message.substring(startPos, endPos);
			messageToDisplay = "<ifs_entity>"+table_name+"</ifs_entity>";
		}
		else {
			messageToDisplay = message;
		}

		if(mCallers!=null && mCallers.values()!=null)
			for (Caller c : mCallers.values()) {
				if(c.callback!=null)
				c.callback.newSyncStatus(activityType, messageToDisplay);
			}

		if(BuildConfig.DEBUG)
		    LogManager.getInstance().debug(messageToDisplay);
	}

	private void onSynced(ActivityType activityType, String message) {
		String messageToDisplay;
		
		// a special rule that all perform result will send the completed message to UI 
		if (message.length() > 100 && !message.contains("perform_") && !message.contains("~~Message~~")) {
			messageToDisplay = message.substring(0, 100);
		} else if (message.contains(MessageHandler.getIfsGetPrefix())) {
			int startPos = message.indexOf(MessageHandler.getIfsGetPrefix()) + MessageHandler.getIfsGetPrefix().length();
			int endPos = message.indexOf("_result\"", startPos);
			String table_name = message.substring(startPos, endPos);
			messageToDisplay = "<ifs_entity>"+table_name+"</ifs_entity>";
		}
		else {
			messageToDisplay = message;
		}

		// Only bubble this up to the UI if we are doing Foreground Sync
		if (mCallers != null && mCallers.values() != null && !isRunningFromBackgroundWorker) {
			for (Caller c : mCallers.values()) {
				c.callback.newSyncStatus(activityType, messageToDisplay);
			}
		}

		// Log this message regardless of Foreground or Background Sync, though
        if(BuildConfig.DEBUG)
		    LogManager.getInstance().debug(messageToDisplay);
	}

	private void onSynced(ActivityType activityType, MmMessageIn messageIn) {
		String messageToDisplay;
		
		// a special rule that all perform result will send the completed message to UI 
		if (messageIn.message.length() > 100 && !messageIn.message.contains("perform_")) {
			messageToDisplay = messageIn.message.substring(0, 100);
		} else if (messageIn.message.contains(MessageHandler.getIfsGetPrefix())) {
			int startPos = messageIn.message.indexOf(MessageHandler.getIfsGetPrefix()) + MessageHandler.getIfsGetPrefix().length();
			int endPos = messageIn.message.indexOf("_result\"", startPos);
			String table_name = messageIn.message.substring(startPos, endPos);
			messageToDisplay = "<ifs_entity>"+table_name+"</ifs_entity>";
		}
		else {
			if(messageIn.message.contains("perform_mobile_authorize_payment_result")){
				messageIn.message = messageIn.message.replace(":{\"payment\":{", ":{\"payment\":{\"message_id\":\""+messageIn.related_message_id+"\",");
			}
			
			messageToDisplay = messageIn.message;
		}

		// Only bubble this up to the UI if we are doing Foreground Sync
		if (mCallers != null && mCallers.values() != null && !isRunningFromBackgroundWorker) {
			for (Caller c : mCallers.values()) {
				c.callback.newSyncStatus(activityType, messageToDisplay);
			}
		}

		// Log this message regardless of Foreground or Background Sync, though
        if(BuildConfig.DEBUG)
		    LogManager.getInstance().debug(messageToDisplay);
	}
	
	@SuppressLint("DefaultLocale") 
	private SyncStatus fetchTransactions() {
		this.onSynced(ActivityType.Download, MetrixStringHelper.getArchitectureString("SyncTransMessages"));

		try {
			Context appContext = MobileApplication.getAppContext();
			ArrayList<MmMessageIn> transactionalMessages = this.getMail();
			
			getMailFromMessageIn(transactionalMessages);

			if (transactionalMessages != null && transactionalMessages.size() > 0) {
				this.onSynced(ActivityType.Download, AndroidResourceHelper.getMessage("FoundTransMessages", transactionalMessages.size()));

				ArrayList<String> mailIDs = new ArrayList<String>();

				boolean isResponse = false;

				while (transactionalMessages.size() > 0) {
					// If INITSTARTED encountered, make sure it is the last message processed
					boolean initStartedEncountered = false;
					for (MmMessageIn message_in : transactionalMessages) {
						this.onSynced(ActivityType.Download, message_in);					

						if (message_in.related_message_id == "0"
								|| MetrixStringHelper.isNullOrEmpty(message_in.related_message_id)
								|| message_in.related_message_id.compareToIgnoreCase("null") == 0) {
							isResponse = false;
						} else {
							isResponse = true;
						}

						if (message_in.transaction_type == MetrixTransactionTypes.INITSTARTED) {
							if (isRunningFromBackgroundWorker) {
								FSMNotificationAssistant.generateBGSyncPendingInitNotification();
								return SyncStatus.STOP;
							}

							SettingsHelper.saveInitStatus(appContext, true);
							initializationAttachments = new Hashtable<String, String>();
							MetrixPublicCache.instance.addItem("INITIALIZATION_ATTACHMENTS", initializationAttachments);
							
							MobileApplication.stopSync(appContext);
							MobileApplication.startSync(appContext, 5);

							MetrixPublicCache.instance.removeItem("INIT_STARTED");
							MetrixPublicCache.instance.addItem("INIT_STARTED", true);
							this.onSynced(ActivityType.InitializationStarted, MetrixStringHelper.getArchitectureString("InitializationStarted"));
							
							HashMap<String, MetrixTableStructure> tableCache = MobileApplication.getTableDefinitionsFromCache();
							ArrayList<String> statements = new ArrayList<String>();

							ArrayList<Hashtable<String, String>> triggerNames = MetrixDatabaseManager.getFieldStringValuesList("sqlite_master", new String[] {"name"}, "type = 'trigger'");
							if (triggerNames != null && triggerNames.size()>0) {
								for (Hashtable<String, String> triggerRow : triggerNames) {
									// Drop all triggers, so that subsequent deletes can be run as truncates.
									String triggerName = triggerRow.get("name");
									statements.add("drop trigger " + triggerName);
								}
							}

							StringBuilder statement = new StringBuilder();
							if (tableCache!= null && tableCache.keySet()!=null) {
								for (String keyName : tableCache.keySet()) {
									statement.append("delete from ");
									statement.append(keyName);
									statements.add(statement.toString());
									statement.append("_log");
									statements.add(statement.toString());
									statement.setLength(0);
								}
							}

							MetrixDatabaseManager.executeSqlArray(statements, false);
							initStartedEncountered = true;
						} else if (message_in.transaction_type == MetrixTransactionTypes.INITREQUESTED) {
							if (isRunningFromBackgroundWorker) {
								FSMNotificationAssistant.generateBGSyncPendingInitNotification();
								return SyncStatus.STOP;
							}

							MetrixRemoteExecutor remote = new MetrixRemoteExecutor(appContext, 5);
							int device_id = SettingsHelper.getDeviceSequence(appContext);
							String user_id = SettingsHelper.getActivatedUser(appContext);
							String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

							String dir = MetrixAttachmentManager.getInstance().getAttachmentPath();
							MetrixAttachmentManager.getInstance().deleteAttachmentFiles(dir);
							MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());
							String url = MetrixSyncManager.generateRestfulServiceUrl(baseUrl, MessageType.Initialization, user_id, device_id, null, null);

							String personId = User.getUser().personId;
							String password = MetrixPasswordHelper.getUserPassword();
								
							MetrixPublicCache.instance.addItem("person_id", personId);
								
							MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());
								
							String loginMessage = getPerformLogin(personId, password);
							JsonObject properties = generatePostBodyJSONWithAuthentication();
							properties.addProperty("login_message", loginMessage);
							String postBody = preparePostBodyForTransmission(properties);

							@SuppressWarnings("unused")
							String response = remote.executePost(url, null, postBody);
						} else if (message_in.transaction_type == MetrixTransactionTypes.INITENDED) {
							if (isRunningFromBackgroundWorker) {
								return SyncStatus.STOP;
							}

							SettingsHelper.saveInitStatus(appContext, false);
							MetrixPublicCache.instance.removeItem("INITIALIZATION_ATTACHMENTS");
							
							MetrixPublicCache.instance.removeItem("INIT_STARTED");
							MetrixPublicCache.instance.addItem("INIT_STARTED", false);	
							
							JSONObject jMessage = new JSONObject(message_in.message);
							String serverAttachmentPath = jMessage.optString("initialization_ended").replace("u+0026amp;", "&").replace("u+0026", "&");
							String fileName;
							if (serverAttachmentPath.contains("?"))
								fileName = serverAttachmentPath.substring( serverAttachmentPath.lastIndexOf('/')+1, serverAttachmentPath.indexOf('?'));
							else
								fileName = serverAttachmentPath.substring( serverAttachmentPath.lastIndexOf('/')+1, serverAttachmentPath.length() );
									
							long enqueue = MessageHandler.downloadAttachment(serverAttachmentPath, fileName, "", true);
							MetrixPublicCache.instance.addItem("DownloadDatabaseId", ""+enqueue);
							LogManager.getInstance().debug("Metrix Mobile Database download ID: " + enqueue);

							mAllowSync = false;
							MetrixPublicCache.instance.addItem("EnableSyncProcess", false);
							//this.onSynced(ActivityType.InitializationEnded, "Initialization Ended!");
						} else if (message_in.transaction_type == MetrixTransactionTypes.NOTIFY) {
							parseAndGenerateNotification(message_in);
						} else if (message_in.transaction_type == MetrixTransactionTypes.PCHANGE) {
							if (isRunningFromBackgroundWorker) {
								FSMNotificationAssistant.generateBGSyncPendingPChangeNotification();
								return SyncStatus.STOP;
							}

							if (MetrixStringHelper.isNullOrEmpty(message_in.message))
								return SyncStatus.KEEP_RUNNING;

							processIncomingPChangeMessage(message_in);
						}

						boolean messageInSaved = saveMessageInAndReceipt(message_in, mailIDs);
						if (messageInSaved) {
							// mailIDs.add(message_in.server_message_id);	
							boolean enableForeignKeyConstraint = isResponse;

							boolean processDownload = Boolean.parseBoolean(MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("EnableSyncProcess")));
							if (processDownload) {
								MetrixProcessStatus downloadProcessStatus = MessageHandler.processDownloadMessages(appContext, message_in, enableForeignKeyConstraint);
								
								if (downloadProcessStatus.mTableName.compareToIgnoreCase("task") == 0||
									downloadProcessStatus.mTableName.compareToIgnoreCase("receiving") == 0 ||
									downloadProcessStatus.mTableName.compareToIgnoreCase("shipment") == 0 ||
									downloadProcessStatus.mTableName.compareToIgnoreCase("task_text") == 0) {
									this.onSynced(ActivityType.Download, downloadProcessStatus.mTableName.toUpperCase());
								}
	
								if (downloadProcessStatus.mSuccessful) {
									if (isResponse) {
										boolean exceptionThrowed = false;

										try {	
											Hashtable<String, String> messageOutFields = MetrixDatabaseManager.getFieldStringValues("mm_message_out",
													new String[] { "table_name", "metrix_log_id", "transaction_desc"}, "message_id="+ message_in.related_message_id);
											
											MetrixDatabaseManager.begintransaction();
											MessageHandler.deleteTransLog(messageOutFields);
											MessageHandler.deleteMessage("mm_message_out", Integer.parseInt(message_in.related_message_id));
											MessageHandler.deleteMessage("mm_message_in", message_in.message_id);					
										} catch (SQLException ex) {
											exceptionThrowed = true;
											LogManager.getInstance().error(ex);
											throw ex;
										} finally {
											if (messageInSaved && exceptionThrowed == false) {
												MetrixDatabaseManager.setTransactionSuccessful();
											}
											MetrixDatabaseManager.endTransaction();
										}										
				
									} else {
										MessageHandler.deleteMessage("mm_message_in", message_in.message_id);
									}
								}
							}
						}

						// We actually did encounter and process INITSTARTED,
						// so break out of the loop processing these messages
						if (initStartedEncountered || mPasswordChangeInProgress)
							break;
					}

					transactionalMessages.clear();

					boolean signSucceeded = true;
					if (mailIDs.size() > 0) {
						signSucceeded = signForMail(mailIDs);
						mailIDs.clear();
					}

					if (signSucceeded && mAllowSync && mPasswordChangeInProgress == false)
						transactionalMessages = this.getMail();
				}
			}
		} catch (Exception ex) {
			this.onSynced(ActivityType.Download, AndroidResourceHelper.getMessage("OnSyncedError", ex.getMessage()));
			LogManager.getInstance().error(ex);
		} finally {
			MessageHandler.updateSentQueueSyncTime();
		}

		return SyncStatus.KEEP_RUNNING;
	}
	
	public static String getPerformLogin(String person_id, String password) {
		JsonObject perform = new JsonObject();
		JsonObject authentication = new JsonObject();
		JsonObject logonInfo = new JsonObject();
		JsonObject logonProperties = new JsonObject();

		logonProperties.addProperty("person_id", person_id);
		logonProperties.addProperty("password", password);
		if(SettingsHelper.getDeviceSequence(MobileApplication.getAppContext())>0)
			logonProperties.addProperty("device_sequence", SettingsHelper.getDeviceSequence(MobileApplication.getAppContext()));
		logonProperties.addProperty("session_id", "");
		logonProperties.addProperty("return_custom_assemblies", false);

		logonInfo.add("logon_info", logonProperties);
		authentication.add("authentication", logonInfo);
		perform.add("perform_login", authentication);
		return perform.toString();
	}

	private void parseAndGenerateNotification(MmMessageIn messageIn) {
		try {
			JSONObject jMessage = new JSONObject(MetrixStringHelper.filterJsonMessage(messageIn.message));
			JSONObject mobileMsg = jMessage.optJSONObject("mobile_notification_msg");
			String title = mobileMsg.optString("title", "");
			String message = mobileMsg.optString("message", "");
			String clientScript = mobileMsg.optString("client_script", "");
			String[] dataPoints = new String[9];
			dataPoints[0] = mobileMsg.optString("data_point1", "");
			dataPoints[1] = mobileMsg.optString("data_point2", "");
			dataPoints[2] = mobileMsg.optString("data_point3", "");
			dataPoints[3] = mobileMsg.optString("data_point4", "");
			dataPoints[4] = mobileMsg.optString("data_point5", "");
			dataPoints[5] = mobileMsg.optString("data_point6", "");
			dataPoints[6] = mobileMsg.optString("data_point7", "");
			dataPoints[7] = mobileMsg.optString("data_point8", "");
			dataPoints[8] = mobileMsg.optString("data_point9", "");

			FSMNotificationAssistant.generateNotification(title, message, clientScript, dataPoints);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private boolean saveMessageInAndReceipt(MmMessageIn message_in, ArrayList<String> mailIDs) {
		boolean exceptionThrowed = false;
		boolean messageInSaved = false;
		try {
			MetrixDatabaseManager.begintransaction();
			if(message_in.retry_num==0){											
				messageInSaved = MessageHandler.saveDownloadMessage(message_in, MessageStatus.READY, 1);
								
				if(messageInSaved){
					MessageHandler.createReceipt(message_in);
					mailIDs.add(message_in.server_message_id);
				}					
			}
			else {
				// it is old mm_message_in just update the retry_num
				messageInSaved = MessageHandler.saveDownloadMessage(message_in, MessageStatus.READY, 1);				
			}
		} catch (SQLException ex) {
			exceptionThrowed = true;
			LogManager.getInstance().error(ex);
			throw ex;
		} finally {
			if (messageInSaved && exceptionThrowed == false) {
				MetrixDatabaseManager.setTransactionSuccessful();
			}
			MetrixDatabaseManager.endTransaction();
		}

		return messageInSaved;
	}

	private void fetchMailReceipts() throws Exception {
		this.onSynced(ActivityType.Download, MetrixStringHelper.getArchitectureString("ResolvingMailReceipts"));

		try {
			int[] mailReceipts = this.getMailReceipts();

			if (mailReceipts != null && mailReceipts.length > 0) {
				for (int index = 0; index <= mailReceipts.length - 1; index++) {
					MessageHandler.updateMessageStatus("mm_message_out", mailReceipts[index], MessageStatus.SENT);
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			throw ex;
		}
	}

	private int[] getMailReceipts() throws Exception {
		if (this.mDeviceId<=0)
			return null;
		
		String serviceUrl = generateRestfulServiceUrl(this.mServiceBaseUrl, MessageType.AllReceipts, this.mUserId, this.mDeviceId, null, null);
		String postBody = preparePostBodyForTransmission(generatePostBodyJSONWithAuthentication());

		try {
			String response = this.mRemoteExecutor.executePost(serviceUrl, "application/xml; charset=utf-8", postBody).replace("\\", "");

			if (response != null) {
				response = response.substring(response.indexOf(">") + 1, response.indexOf("</"));
				JSONObject jResult = new JSONObject(response);

				JSONArray jReceipts = new JSONArray();

				if (jResult != null && jResult.has("mm_message_receipt_hierarchy_select_result")) {
					JSONObject jSelectResult = jResult.getJSONObject("mm_message_receipt_hierarchy_select_result");
					if (jResult.getJSONObject("mm_message_receipt_hierarchy_select_result").has("mm_message_receipt")) {

						JSONObject jServerReceiptObject = null;
						if(jSelectResult != null)
							jServerReceiptObject = jSelectResult.optJSONObject("mm_message_receipt");

						// it is not a JSONObject so it may be an array
						if (jServerReceiptObject == null) {
							jReceipts = jResult.getJSONObject("mm_message_receipt_hierarchy_select_result").getJSONArray("mm_message_receipt");
						} else {
							jReceipts.put(jServerReceiptObject);
						}
					}

					if (jReceipts != null) {
						int[] ids = new int[jReceipts.length()];
						for (int i = 0; i < jReceipts.length(); i++) {
							JSONObject id = jReceipts.getJSONObject(i);
							if (id != null)
								ids[i] = id.getInt("message_id");
						}

						return ids;
					}
				}
			}
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			throw ex;
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			throw ex;
		}
		return null;
	}

	private ArrayList<MmMessageIn> getMail() {
		ArrayList<MmMessageIn> serverMessages = new ArrayList<MmMessageIn>();

		try {
			// Default Messages GET API Call
			MessageType msgTypeToUse = MessageType.AllMessages;
			Object initStartCacheItem = MetrixPublicCache.instance.getItem("INIT_STARTED");
			if (initStartCacheItem != null && (Boolean)initStartCacheItem)
			{
				// INIT Case: Switch to an init-only Messages GET API Call
				msgTypeToUse = MessageType.InitRelatedMessages;
			}

			String serviceUrl = generateRestfulServiceUrl(this.mServiceBaseUrl, msgTypeToUse, this.mUserId, this.mDeviceId, null, null);
			String postBody = preparePostBodyForTransmission(generatePostBodyJSONWithAuthentication());
			String response = this.mRemoteExecutor.executePost(serviceUrl, null, postBody);

			response = stripWrapperFromResponse(response);
			response = response.replace("\\", "").replace("message\":\"{", "message\":{").replace("}\",\"message_id", "},\"message_id");

			if (response != null) {
				JSONObject jResult = new JSONObject(response);
				
				JSONArray jServerMessageOut = new JSONArray();
				if (jResult != null && jResult.length() > 0) {
					if (jResult.has("mm_message_out_hierarchy_select_result")) {
						JSONObject jSelectResult = jResult.optJSONObject("mm_message_out_hierarchy_select_result");
						if (jSelectResult != null && jSelectResult.length() > 0) {
							if (jSelectResult.has("mm_message_out")) {
								JSONObject jServerMessageObject = jSelectResult.optJSONObject("mm_message_out");
								
								// it is not a JSONObject so it may be an array
								if (jServerMessageObject == null) {
									jServerMessageOut = jSelectResult.getJSONArray("mm_message_out");
								} else {
									jServerMessageOut.put(jServerMessageObject);
								}
							}
						}
					}
				}

				if (jServerMessageOut != null) {
					// ToDo: Parse Json messages from web service
					for (int i = 0; i < jServerMessageOut.length(); i++) {
						JSONObject serverMessage = jServerMessageOut.getJSONObject(i);
						if (serverMessage != null) {
							MmMessageIn clientMessageIn = new MmMessageIn();
							clientMessageIn.server_message_id = serverMessage.getString("message_id");
							clientMessageIn.related_message_id = serverMessage.getString("related_message_id");
							clientMessageIn.person_id = serverMessage.getString("person_id");
							clientMessageIn.message = serverMessage.getString("message");
							clientMessageIn.transaction_type = MetrixTransactionTypesConverter.toEnum(serverMessage.getString("transaction_type"));
							clientMessageIn.retry_num = 0;
							clientMessageIn.status = MessageInStatus.LOADED.toString();
							clientMessageIn.created_dttm = serverMessage.getString("created_dttm"); 
							// Should verify if we use server datetime, or created dttm when receive the message

							serverMessages.add(clientMessageIn);
						}
					}

					jServerMessageOut = null;
					return serverMessages;
				}
			}
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			return null;
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return null;
		}
		return serverMessages;
	}

	private void getMailFromMessageIn(ArrayList<MmMessageIn> transactionalMessages){
		if(transactionalMessages == null)
			transactionalMessages = new ArrayList<MmMessageIn>();
			
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.getRowsMC("mm_message_in",
					new String[] { "message_id", "person_id", "transaction_type", "related_message_id", "message", "retry_num", /*"attachment",*/ "created_dttm", "status" },
					"retry_num>=0 and retry_num<3 and status='"+MessageInStatus.LOADED.toString()+"'");

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			MmMessageIn message_in = new MmMessageIn();

			while (cursor.isAfterLast() == false) {
				message_in.message_id = cursor.getInt(0);
				message_in.person_id = cursor.getString(1);
				message_in.transaction_type = MetrixTransactionTypesConverter.toEnum(cursor.getString(2));
				message_in.related_message_id = cursor.getString(3);
				message_in.message = cursor.getString(4);
				message_in.retry_num = cursor.getInt(5);
				// message_in.attachment = cursor.getString(6);
				message_in.created_dttm = cursor.getString(6);
				message_in.status = cursor.getString(7);

				cursor.moveToNext();
				transactionalMessages.add(message_in);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}		
	}
	
	private void sendTransactions() {
		ArrayList<MmMessageOut> messagesToSend = MessageHandler.getMessagesToSend();
		if (messagesToSend != null && messagesToSend.size() > 0) {
			mClientHadMessagesToSend = true;
			this.onSynced(ActivityType.Upload, AndroidResourceHelper.getMessage("UploadingMessages", messagesToSend.size()));

			// To do: currently, send the message one by one. Need a batch load method on server
			for (MmMessageOut message : messagesToSend) {
				if (message.message != null) {
					this.onSynced(ActivityType.Upload, message.message);

					if ((message.attachment == null) || (message.attachment != null && message.attachment.compareToIgnoreCase("binary") != 0)) {
						try {
							boolean resend = message.status.compareToIgnoreCase("SENT") == 0;
							String serviceUrl = generateRestfulServiceUrl(this.mServiceBaseUrl, MessageType.CreateMessage, this.mUserId, this.mDeviceId,
									Integer.toString(message.message_id), message.transaction_type, resend);

							JsonObject properties = generatePostBodyJSONWithAuthentication();
							properties.addProperty("message_id", String.valueOf(message.message_id));

							// If this is a resend, bolt "resend" on the front of the trans_type value
							String transType = message.transaction_type;
							if (resend)
								transType = "resend" + transType;

							properties.addProperty("trans_type", transType);
							properties.addProperty("message_content", message.message);
							String postBody = preparePostBodyForTransmission(properties);

							String response = this.mRemoteExecutor.executePost(serviceUrl, null, postBody);
							if (response.contains("true")) {
								//MessageHandler.UpdateMessageStatus(message, MessageStatus.UploadSent);
							}					
						} catch (Exception ex) {
							LogManager.getInstance().error(ex);
						} 
					} else {
						if (MetrixStringHelper.isNullOrEmpty(message.table_name) == false && message.table_name.compareToIgnoreCase("attachment") == 0) {
							Hashtable<String, String> attachmentInfo = MetrixDatabaseManager.getFieldStringValues("attachment_log", new String[] {"attachment_name", "file_extension", "mobile_path"}, "metrix_log_id="+message.metrix_log_id);
							if (attachmentInfo.get("attachment_name") != null) {
								String encodeFileName = MetrixStringHelper.encodeBase64String(attachmentInfo.get("attachment_name"));
								String localFilePath = attachmentInfo.get("mobile_path");
								String localFileName = attachmentInfo.get("attachment_name");

								if (localFilePath.contains("/") || localFilePath.contains("\\")) {
									if (localFilePath.contains("/"))
										localFilePath = localFilePath.substring(localFilePath.lastIndexOf("/")+1);
									if (localFilePath.contains("\\"))
										localFilePath = localFilePath.substring(localFilePath.lastIndexOf("\\")+1);

									// by this point, localFilePath should be trimmed down to just the file name, regardless of original path slash order
									localFileName = localFilePath;
								}

								String localFile = MetrixAttachmentManager.getInstance().getAttachmentPath()+"/"+localFileName;
								boolean resend = message.status.compareToIgnoreCase("SENT") == 0;

								String serverUrl = generateAttachmentUrl(mServiceBaseUrl);
								
								@SuppressWarnings("unused")
								int responseCode = -1;
								try {
									JsonObject properties = generatePostBodyJSONWithAuthentication();
									properties.addProperty("message_id", String.valueOf(message.message_id));

									// If this is a resend, bolt "resend" on the front of the trans_type value
									String transType = message.transaction_type;
									if (resend)
										transType = "resend" + transType;

									properties.addProperty("trans_type", transType);
									properties.addProperty("attachment_type", attachmentInfo.get("file_extension"));
									properties.addProperty("file_name", encodeFileName);
									String postBody = preparePostBodyForTransmission(properties);

									responseCode = mRemoteExecutor.executePostBinary(serverUrl, message.message, localFile, postBody);

									// The attachment file is not physically existing in the local file system
									if(responseCode == -2) {
										MessageHandler.removeAttachmentMessages(message);
									}
									else if(responseCode == 401) {
										MessageHandler.updateMessageStatus("mm_message_out", message.message_id, MessageStatus.READY);
									}
								} catch (Exception ex) {
									LogManager.getInstance().error(ex);
								}
							}
						}
					}
				}
			}
		} else
			this.onSynced(ActivityType.Upload, MetrixStringHelper.getArchitectureString("NoMessagesToUpload"));
	}

	private void signForStoredReceipts() throws Exception {
		ArrayList<MmMessageReceipt> storedReceipts = MessageHandler.getReceiptsToSend();
		ArrayList<String> mailIDs = new ArrayList<String>();

		if (storedReceipts != null) {
			for (MmMessageReceipt message_receipt : storedReceipts) {
				mailIDs.add(Integer.toString(message_receipt.message_id));
			}

			if (mailIDs.size() > 0) {
				if (!signForMail(mailIDs)) {
					throw new Exception(AndroidResourceHelper.getMessage("Failed1Args","signForStoredReceipts"));
				}
			}
		}
	}

	private boolean signForMail(ArrayList<String> ids) {
		StringBuilder messageIDs = new StringBuilder();
		boolean success = false;

		for (int i = 0; i < ids.size(); i++) {
			if (i == 0) {
				messageIDs.append(ids.get(i));
			} else {
				messageIDs.append("_" + ids.get(i));
			}
		}

		String serviceUrl = generateRestfulServiceUrl(this.mServiceBaseUrl, MessageType.CreateReceipt, this.mUserId, this.mDeviceId, "1", null);

		try {
			JsonObject properties = generatePostBodyJSONWithAuthentication();
			properties.addProperty("message_ids", messageIDs.toString());
			String postBody = preparePostBodyForTransmission(properties);

			String response = this.mRemoteExecutor.executePost(serviceUrl, "application/xml; charset=utf-8", postBody);
			
			// Post the message successfully
			if (response.contains("true")) {
				for (String message_id : ids) {
					if (MetrixStringHelper.isInteger(message_id)) {
						MessageHandler.deleteMessage("mm_message_receipt", Integer.parseInt(message_id));
					}
				}

				this.onSynced(ActivityType.Upload, AndroidResourceHelper.getMessage("SignForMessages", messageIDs.toString().replace("_", ", ")));
				success = true;
			} else
				this.onSynced(ActivityType.Upload, AndroidResourceHelper.getMessage("SignForMailFailed", messageIDs.toString().replace("_", ", ")));

		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return success;
	}

	private synchronized boolean ping() {
		String serviceUrl = generateRestfulServiceUrl(this.mServiceBaseUrl, MessageType.Messages, null, 0, null, null);

		try {
			String response = this.mRemoteExecutor.executeGet(serviceUrl).replace("\\", "");

			if (response != null) {
				if (response.contains("true")){
					this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("Connected"));
					return setRecentPingSuccessAndReturn(true);
				}
			}
		} catch (HandlerException ex) {
			this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("Disconnected"));
			LogManager.getInstance().error(ex);
			return setRecentPingSuccessAndReturn(false);
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return setRecentPingSuccessAndReturn(false);
		}

		return setRecentPingSuccessAndReturn(false);
	}

	private synchronized boolean mobilePing() {
		try {
			if (SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.PCHANGE_JUST_PROCESSED)) {
				boolean validTokenRetrieved = false;
				if (ping()) {
					String personId = SettingsHelper.getActivatedUser(MobileApplication.getAppContext());
					String hashPassword = MetrixPasswordHelper.getUserPassword();
					validTokenRetrieved = reacquireValidToken(personId, hashPassword);
				}

				if (validTokenRetrieved)
					SettingsHelper.removeSetting(SettingsHelper.PCHANGE_JUST_PROCESSED);

				return generateMobilePingOutput(validTokenRetrieved);
			} else {
				String pingResult = this.executeMobilePing();
				if (!MetrixStringHelper.isNullOrEmpty(pingResult)) {
					if (pingResult.equalsIgnoreCase("true")) {
						return generateMobilePingOutput(true);
					} else if (pingResult.equalsIgnoreCase("false")) {
						String personId = SettingsHelper.getActivatedUser(MobileApplication.getAppContext());
						String hashPassword = MetrixPasswordHelper.getUserPassword();
						if (reacquireValidToken(personId, hashPassword)) {
							return generateMobilePingOutput(true);
						} else {
							return generateMobilePingOutput(false);
						}
					} else if (pingResult.equalsIgnoreCase("NOT_ACTIVE_DNE")) {
						_activationVerified = false;
						// Allow sync to proceed, so that inner handling of forced re-activation can occur
						return generateMobilePingOutput(true);
					} else if (pingResult.equalsIgnoreCase("FSM_OFFLINE")) {
						return generateMobilePingOutput(false);
					} else if (pingResult.contains("PCHANGE")) {
						if (isRunningFromBackgroundWorker) {
							FSMNotificationAssistant.generateBGSyncPendingPChangeNotification();
							return generateMobilePingOutput(false);
						} else {
							pingResult = pingResult.replace("\\", "").replace("message\":\"{", "message\":{").replace("\"}}\",", "\"}},");
							JSONObject jResult = new JSONObject(pingResult);
							JSONObject serverMessage = jResult.optJSONObject("mm_message_out_hierarchy_select_result").optJSONObject("mm_message_out");

							MmMessageIn pChangeMessage = new MmMessageIn();
							pChangeMessage.server_message_id = serverMessage.optString("message_id");
							pChangeMessage.related_message_id = serverMessage.optString("related_message_id");
							pChangeMessage.person_id = serverMessage.optString("person_id");
							pChangeMessage.message = serverMessage.optString("message");
							pChangeMessage.transaction_type = MetrixTransactionTypesConverter.toEnum(serverMessage.optString("transaction_type"));
							pChangeMessage.retry_num = 0;
							pChangeMessage.status = MessageInStatus.LOADED.toString();
							pChangeMessage.created_dttm = serverMessage.optString("created_dttm");

							processIncomingPChangeMessage(pChangeMessage);

							// Store this in mm_message_receipt, so that we can sign for it later (after we have a valid token)
							MessageHandler.createReceipt(pChangeMessage);
							return generateMobilePingOutput(false);
						}
					}
				}
			}
			return generateMobilePingOutput(false);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return generateMobilePingOutput(false);
		}
	}

	private String executeMobilePing() throws Exception {
		String pingResult = "";
		try {
			String serviceUrl = mServiceBaseUrl+"/messages/mobileping";
			String postBody = preparePostBodyForTransmission(generatePostBodyJSONWithAuthentication());
			pingResult = this.mRemoteExecutor.executePost(serviceUrl, "application/json", postBody);

		} catch (HandlerException ex) {
			this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("Disconnected"));
			LogManager.getInstance().error(ex);
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
		}

		return pingResult;
	}

	public static JsonObject generatePostBodyJSONWithAuthentication() throws JSONException {
		String personId = SettingsHelper.getActivatedUser(MobileApplication.getAppContext());
		personId = Global.encodeUrl ? MetrixStringHelper.encodeBase64ToUrlSafe(personId.toUpperCase()) : personId.toUpperCase();

		int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		String tokenId = SettingsHelper.getTokenId(MobileApplication.getAppContext());
		String sessionId = SettingsHelper.getSessionId(MobileApplication.getAppContext());

		JsonObject properties = new JsonObject();
		properties.addProperty("device_sequence", String.valueOf(deviceSequence));
		properties.addProperty("token_id", tokenId);
		properties.addProperty("person_id", personId);
		properties.addProperty("session_id", sessionId);

		return properties;
	}

	public static String preparePostBodyForTransmission(JsonObject properties) {
		JsonObject postBody = new JsonObject();
		postBody.add("post_body", properties);
		return postBody.toString();
	}

	private boolean generateMobilePingOutput(boolean success) {
		if (success) {
			this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("Connected"));
			return setRecentPingSuccessAndReturn(true);
		} else {
			this.onSynced(ActivityType.Information, MetrixStringHelper.getArchitectureString("Disconnected"));
			return setRecentPingSuccessAndReturn(false);
		}
	}

	public boolean pingMobileService(){
		return ping();
	}

	private boolean setRecentPingSuccessAndReturn(boolean success) {
		mMostRecentPingSuccessful = success;
		return success;
	}

	public static boolean reacquireValidToken(String personId, String password) throws Exception {
		MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String serviceUrl = SettingsHelper.getServiceAddress(MobileApplication.getAppContext());

		String message = getPerformLogin(personId, password);
		String fullUrl = serviceUrl + "/DirectPost/";
		String response = remoteExecutor.executePost(fullUrl, null, message);
		response = response.substring(response.indexOf(">") + 1, response.indexOf("</"));
		JSONObject jResult = new JSONObject(response);

		if (jResult.has("perform_login_result")){
			JSONObject jPerformLogin = null;
			if(jResult != null)
				jPerformLogin = jResult.getJSONObject("perform_login_result");

			JSONObject jInfo = null;
			if(jPerformLogin != null)
				jInfo = jPerformLogin.optJSONObject("session_info");

			if (jInfo != null) // We only care about token_id and session_id in this scenario
			{
				String tokenId = jInfo.optString(SettingsHelper.TOKEN_ID.toLowerCase());
				SettingsHelper.saveTokenId(MobileApplication.getAppContext(), tokenId);

				String sessionId = jInfo.optString(SettingsHelper.SESSION_ID.toLowerCase());
				SettingsHelper.saveSessionId(MobileApplication.getAppContext(), sessionId);
				return true;
			}
		}
		else {
			if(response.contains("metrix_response")) {
				JSONObject jMetrixResponse = jResult.getJSONObject("metrix_response");
				JSONObject jErrorResult = jMetrixResponse.getJSONObject("result");
				JSONObject jError = jErrorResult.getJSONObject("error");
				JSONObject jApplicationError = jError.getJSONObject("application_error");
				String errorMessage = jApplicationError.getString("message");
				SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), SettingsHelper.SERVER_AUTHENTICATE_ERROR_MESSAGE, errorMessage, false);
			}
			else {
				SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), SettingsHelper.SERVER_AUTHENTICATE_ERROR_MESSAGE, "Network error", false);
			}
			return false;
		}

		return false;
	}

	public synchronized boolean getIsSyncRunning(){
		return mIsSyncing;
	}
	
	public synchronized void setIsSyncRunning(boolean value){
		mIsSyncing = value;
	}

	/**
	 * Generate the rest service url based on the parameters
	 *
	 * @param baseUrl
	 * @param messageType
	 * @param user_id
	 * @param device_id
	 * @param message_id
	 * @param transType
	 * @return
	 */
	public static String generateRestfulServiceUrl(String baseUrl, MessageType messageType, String user_id, int device_id, String message_id, String transType) {
		return generateRestfulServiceUrl(baseUrl, messageType, user_id, device_id, message_id, transType, false);
	}

	/**
	 * Generate the rest service url based on the parameters
	 * 
	 * @param baseUrl
	 * @param messageType
	 * @param user_id
	 * @param device_id
	 * @param message_id
	 * @param transType
	 * @return
	 */
	public static String generateRestfulServiceUrl(String baseUrl, MessageType messageType, String user_id, int device_id, String message_id, String transType, boolean resend) {
		StringBuilder urlBuilder = new StringBuilder();

		urlBuilder.append(baseUrl);

		if (user_id == null && message_id == null) {
			if(!MetrixStringHelper.isNullOrEmpty(transType) && transType.equalsIgnoreCase("token")) {
				urlBuilder.append("/Messages/GetToken");
				return urlBuilder.toString();
			}
			else
				urlBuilder.append("/Messages/ping");
		} else {
			user_id = Global.encodeUrl? MetrixStringHelper.encodeBase64ToUrlSafe(user_id):user_id;
			
			switch (messageType) {
				case Messages:
					urlBuilder.append("/Messages/user=" + user_id + "/device=" + device_id);
					break;
				case AllReceipts:
					urlBuilder.append("/Receipts/getall");
					return urlBuilder.toString();
				case CreateReceipt:
					urlBuilder.append("/Receipts/");
					return urlBuilder.toString();
				case Initialization:
					urlBuilder.append("/Initialization/");
					return urlBuilder.toString();
				case InitRelatedMessages:
					urlBuilder.append("/Messages/initrelated");
					return urlBuilder.toString();
				case AllMessages:
					urlBuilder.append("/Messages/getall");
					return urlBuilder.toString();
				case CreateMessage:
					urlBuilder.append("/Messages/");
					return urlBuilder.toString();
				default:
					return "";
			}
		}

		if (message_id != null)
			urlBuilder.append("/messageId=" + message_id);

		if (transType != null) {
			if(resend)
				urlBuilder.append("/transType=resend" + transType);
			else
				urlBuilder.append("/transType=" + transType);
		}

		return urlBuilder.toString();
	}

	public static String generateAttachmentUrl(String baseUrl) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(baseUrl);
		urlBuilder.append("/Messages/attach");
		return urlBuilder.toString();
	}

	public void toggleFastMode(boolean enabled) {
		if (mUsingFastMode == enabled)
			return;  // this is not actually toggling anything, so just return

		String fastModeMessage = enabled ? MetrixStringHelper.getArchitectureString("FastModeEnabled") : MetrixStringHelper.getArchitectureString("FastModeDisabled");
		this.onSynced(ActivityType.Information, fastModeMessage);

		Context appContext = MobileApplication.getAppContext();
		mUsingFastMode = enabled;
		MobileApplication.stopSync(appContext);
		if (enabled)
			MobileApplication.startSync(appContext, 5);  // force using a 5-second interval
		else
			MobileApplication.startSync(appContext);  // resume using a normal interval from settings/default
	}

	private void forceReactivation() {
		try {
			// Establish reactivation texts and contexts for running on UI thread
			final String reactivationMsg = AndroidResourceHelper.getMessage("DeviceMustBeReactivated");
			final String okMsg = AndroidResourceHelper.getMessage("OK");
			final Context appContext = MobileApplication.getAppContext();
			final Activity currActivity = MobileApplication.mCurrentActivity.get();

			// Force-delete some DB and file content before showing the dialog,
			// so that the app does not retain useful information even if the user somehow dances around the reactivation dialog
			ArrayList<String> statements = new ArrayList<String>();

			ArrayList<Hashtable<String, String>> triggerNames = MetrixDatabaseManager.getFieldStringValuesList("sqlite_master", new String[] {"name"}, "type = 'trigger'");
			if (triggerNames != null && !triggerNames.isEmpty()) {
				for (Hashtable<String, String> triggerRow : triggerNames) {
					// Drop all triggers, so that subsequent deletes can be run as truncates.
					String triggerName = triggerRow.get("name");
					statements.add("drop trigger " + triggerName);
				}
			}

			HashMap<String, MetrixTableStructure> tableCache = MobileApplication.getTableDefinitionsFromCache();
			if (tableCache != null && !tableCache.isEmpty()) {
				for (String keyName : tableCache.keySet()) {
					statements.add("delete from " + keyName);
					statements.add("delete from " + keyName + "_log");
				}
			}

			ArrayList<Hashtable<String, String>> systemTableNames = MetrixDatabaseManager.getFieldStringValuesList("sqlite_master", new String[] {"name"}, "type ='table' AND name LIKE 'mm_message%'");
			if (systemTableNames != null && !systemTableNames.isEmpty()) {
				for (Hashtable<String, String> systemRow : systemTableNames) {
					String keyName = systemRow.get("name");
					// Leave localizations in place for now
					if (MetrixStringHelper.valueIsEqual(keyName, "mm_message_def_view"))
						continue;

					statements.add("delete from " + keyName);
				}
			}

			MetrixDatabaseManager.executeSqlArray(statements, false);
			MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());

			// Now, on the UI thread, display a modal dialog that indicates a re-activation is required.
			// The OK button click will do the equivalent of Clear Data and force-stop the app.
			// When the app is next opened, it should be in a pristine pre-activation state - no re-install required.
			if (currActivity != null) {
				currActivity.runOnUiThread(new Runnable() {
					public void run() {
						try {
							if (!_reactivationPopupShowing) {
								_reactivationPopupShowing = true;
								new AlertDialog.Builder(currActivity)
										.setMessage(reactivationMsg)
										.setCancelable(false)
										.setPositiveButton(okMsg, new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id) {
												dialog.dismiss();

												// This one line should wipe out all (remaining) DB data, files, and app settings and force-close the app
												((ActivityManager)appContext.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();

												// This line should not be hit, but it's here just in case the above doesn't force-close the app
												Process.killProcess(Process.myPid());
											}
										}).create().show();
							}
						} catch (Exception e) {
						   LogManager.getInstance().error(e);
						   LogManager.getInstance().error("The forceReactivation() dialog failed, due to the above exception.", null);
						}
					}
			   });
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			LogManager.getInstance().error("The forceReactivation() method failed to work, due to the above exception.", null);
		}
	}

	private void processIncomingPChangeMessage(MmMessageIn message_in) throws Exception {
		Context appContext = MobileApplication.getAppContext();

		JSONObject jResult = new JSONObject(message_in.message);
		if (jResult != null && jResult.has("pchange")) {
			JSONObject jHash = jResult.optJSONObject("pchange");
			String hashPassword = jHash.getString("simpleHash");
			String credential = SettingsHelper.getStringSetting(appContext, SettingsHelper.USER_LOGIN_PASSWORD);

			if (!MetrixStringHelper.isNullOrEmpty(hashPassword) && !hashPassword.equals(credential)) {
				mPasswordChangeInProgress = true;
				SettingsHelper.saveBooleanSetting(appContext, SettingsHelper.PCHANGE, true);
				SettingsHelper.saveStringSetting(appContext, SettingsHelper.PCHANGE_SIMPLEHASH, hashPassword, true);
				SettingsHelper.saveStringSetting(appContext, SettingsHelper.USER_LOGIN_PASSWORD, hashPassword, true);

				if (User.getUser() != null) {
					this.onSynced(ActivityType.PasswordChangedFromServer, "SERVER PASSWORD UPDATE");
				}

				if (MobileApplication.appIsInBackground(appContext))
					MetrixDatabaseManager.executeSql("update user_credentials set hidden_chg_occurred = 'Y'");
			}
		}
	}

	private String stripWrapperFromResponse(String response) {
		if (response.contains("/\">")) {
			int headerEndIndex = response.indexOf("/\">");
			String modifiedResponse = response.substring(headerEndIndex + 3);
			modifiedResponse.replace("</string>", "");
			modifiedResponse.replace("</boolean>", "");
			return modifiedResponse;
		}
		return response;
	}

	// An enumeration strictly for signaling Background Sync to spin up another Sync process (KEEP_RUNNING) or not (STOP).
	public enum SyncStatus {
		KEEP_RUNNING,
		STOP
	}
}