package com.metrix.architecture.ui.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixParcelable;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.metrixmobile.DebriefOverview;
import com.metrix.metrixmobile.DebriefSignature;
import com.metrix.metrixmobile.DebriefTaskAttachment;
import com.metrix.metrixmobile.DebriefTaskTextList;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixTimeClockAssistant;
import com.metrix.metrixmobile.system.MetrixActivity;
import com.metrix.metrixmobile.system.MetrixPooledTaskAssignmentManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

public class MetrixQuickLinksBar extends LinearLayout {
	@SuppressWarnings("unused")
	private LinearLayout mLayout;
	private ImageButton mTaskStatusButton;
	private ImageButton mOverviewButton;
	private ImageButton mAttachmentsButton;
	private ImageButton mNotesButton;
	private TextView mAttachmentsBadge;
	private TextView mNotesBadge;

	private Activity mActivity;

	private ArrayList<String> mStatuses = new ArrayList<String>();
	private LinkedHashMap<String, HashMap<String, String>> mStatusInfo = new LinkedHashMap<String, HashMap<String, String>>();
	private String mTaskId;
	private String mNextTaskStatus;
	private TextView mCommentEntry;
	private boolean isPooledTask;

	public MetrixQuickLinksBar(Context context) {
		super(context);
	}

	public MetrixQuickLinksBar(Context context, AttributeSet attrs) {
		super(context, attrs);

		mActivity = (Activity) context;

		LayoutInflater inflater = LayoutInflater.from(context);
		mLayout = (LinearLayout) inflater.inflate(R.layout.attachment_button_selector, this);

		mTaskStatusButton = (ImageButton) findViewById(R.id.taskStatusButton);
		mOverviewButton = (ImageButton) findViewById(R.id.homeButton);
		mAttachmentsButton = (ImageButton) findViewById(R.id.attachmentsButton);
		mNotesButton = (ImageButton) findViewById(R.id.notesButton);
		mAttachmentsBadge = (TextView) findViewById(R.id.attachmentsBadge);
		mNotesBadge = (TextView) findViewById(R.id.notesBadge);

		if (mActivity.getClass().getName().compareToIgnoreCase("com.metrix.metrixmobile.DebriefOverview") == 0) {
			hideView(mOverviewButton);
			setupClickListener(mTaskStatusButton);
			setupClickListener(mNotesButton);
			setupClickListener(mAttachmentsButton);
		} else if (mActivity.getClass().getName().compareToIgnoreCase("com.metrix.metrixmobile.DebriefTaskTextList") == 0){
			hideView(mNotesButton);
			hideView(mNotesBadge);
			setupClickListener(mTaskStatusButton);
			setupClickListener(mOverviewButton);
			setupClickListener(mAttachmentsButton);
		} else if (mActivity.getClass().getName().compareToIgnoreCase("com.metrix.metrixmobile.DebriefTaskAttachment") == 0) {
			hideView(mAttachmentsButton);
			hideView(mAttachmentsBadge);
			setupClickListener(mTaskStatusButton);
			setupClickListener(mOverviewButton);
			setupClickListener(mNotesButton);
		} else {
			setupClickListener(mTaskStatusButton);
			if (MetrixPublicCache.instance.containsKey("currentScreen")) {
				String currentScreen = (String)MetrixPublicCache.instance.getItem("currentScreen");
				if (MetrixStringHelper.valueIsEqual(currentScreen,"DebriefFollowUpTasks")) {
					hideView(mTaskStatusButton);
					MetrixPublicCache.instance.removeItem("currentScreen");
				}
			}

			hideView(mOverviewButton);
			hideView(mAttachmentsButton);
			hideView(mAttachmentsBadge);
			hideView(mNotesButton);
			hideView(mNotesBadge);
			//setupClickListener(mOverviewButton);
			//setupClickListener(mNotesButton);
			//setupClickListener(mAttachmentsButton);
		}

		refreshBadges();
	}

	public void refreshBadges() {
		int notesCount = 0;
		int attachmentsCount = 0;
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

		if(!MetrixStringHelper.isNullOrEmpty(taskId)) {
			notesCount = MetrixDatabaseManager.getCount("task_text", "task_id = " + taskId);
			attachmentsCount = MetrixDatabaseManager.getCount("task_attachment join attachment on task_attachment.attachment_id = attachment.attachment_id", "task_attachment.task_id = " + taskId + " and (attachment.attachment_type is null or attachment.attachment_type != 'SIGNATURE')");

			if(mNotesBadge == null)
				mNotesBadge = (TextView) findViewById(R.id.notesBadge);
			mNotesBadge.setText(Integer.toString(notesCount));

			if(mAttachmentsBadge == null)
				mAttachmentsBadge = (TextView) findViewById(R.id.attachmentsBadge);
			mAttachmentsBadge.setText(Integer.toString(attachmentsCount));

			float scale = getResources().getDisplayMetrics().density;
			float radius = 10f * scale + 0.5f;
			MetrixSkinManager.setFirstGradientColorsForTextView(mNotesBadge, radius);
			MetrixSkinManager.setFirstGradientColorsForTextView(mAttachmentsBadge, radius);

			setTaskStatusButtonImage();
		}
	}

	private void setTaskStatusButtonImage() {
		boolean didUseImageID = false;
		int requiredHeight = 25;
		int requiredWidth = 25;

		mTaskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		if (!MetrixStringHelper.isNullOrEmpty(mTaskId)) {
			String taskStatus = MetrixDatabaseManager.getFieldStringValue("task", "task_status", "task_id = " + mTaskId);
			String imageID = MetrixDatabaseManager.getFieldStringValue("task_status", "image_id", String.format("task_status = '%s'", taskStatus));
			if (!MetrixStringHelper.isNullOrEmpty(imageID)) {

				String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + imageID;
				File iconFile = new File(fullPath);
				if (iconFile.exists()) {
					FileInputStream fileInputStream = null;
					try {
						BitmapFactory.Options options = new BitmapFactory.Options();
						DisplayMetrics metrics = mActivity.getApplicationContext().getResources().getDisplayMetrics();
						options.inScreenDensity = metrics.densityDpi;
						options.inTargetDensity =  metrics.densityDpi;
						options.inDensity = DisplayMetrics.DENSITY_DEFAULT;

						fileInputStream = new FileInputStream(iconFile);
						Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null, options);
						BitmapDrawable bitmapDrawable = new BitmapDrawable(mActivity.getResources(), bitmap);
						mTaskStatusButton.setImageDrawable(bitmapDrawable);

						didUseImageID = true;

					} catch (Exception e) {
						LogManager.getInstance().error(e);
					}
					finally{
						try {
							if(fileInputStream != null)
								fileInputStream.close();
						} catch (IOException e) {
							LogManager.getInstance().error(e);
						}
					}
				}
			}
		}

		if (!didUseImageID) {
			try {
				float scale = getResources().getDisplayMetrics().density;
				if (scale != 1.0f) {
					// convert these int's from DP to PX
					requiredHeight = (int) (requiredHeight * scale + 0.5f);
					requiredWidth = (int) (requiredWidth * scale + 0.5f);
				}

				Bitmap tempBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.calendar_25);
				Bitmap resourceImage = Bitmap.createScaledBitmap(tempBitmap, requiredWidth, requiredHeight, false);
				if (resourceImage != null) {
					mTaskStatusButton.setImageBitmap(resourceImage);
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		}
	}

	private void handleTaskStatusClick() {
		try {
			mTaskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
			if (!MetrixStringHelper.isNullOrEmpty(mTaskId)) {
				// try to get a list of possible task_status values, using a status/type match on task_status_flow
				mStatusInfo = new LinkedHashMap<String, HashMap<String, String>>();

				isPooledTask = MetrixPooledTaskAssignmentManager.instance().isPooledTask(mTaskId);
				if(isPooledTask){
					if(mStatusInfo != null && mStatusInfo.size() > 0)
						mStatusInfo.clear();

					HashMap<String, String> row = new HashMap<String, String>();
					String nextTaskStatusDescription = MetrixDatabaseManager.getFieldStringValue("task_status", "description", "task_status = 'ACCEPTED'");
					row.put("next_task_status", "ACCEPTED");
					row.put("comment_required", "N");
					row.put("comment_message", null);
					row.put("confirm_required", null);
					row.put("confirm_message", null);
					mStatusInfo.put(nextTaskStatusDescription, row);
				}
				else {
					if (!MetrixPooledTaskAssignmentManager.instance().currentTaskExists(mTaskId)) {
						MetrixPooledTaskAssignmentManager.instance().dismissCurrentScreen(mActivity, AndroidResourceHelper.getMessage("PTskAsgnmntAlrdyAsgndOrNotExsts"));
						return;
					} else {
						if (!MetrixPooledTaskAssignmentManager.instance().currentUserIsTheOwnerOfTheTask(mTaskId)) {
							MetrixPooledTaskAssignmentManager.instance().dismissCurrentScreen(mActivity, AndroidResourceHelper.getMessage("PTskAsgnmntAlrdyAsgndOrNotExsts"));
							return;
						}
					}

					String tsfSelectChunk = "select task_status.description, task_status_flow.next_task_status,"
							+ " task_status_flow.comment_required, task_status_flow.comment_message,"
							+ " task_status_flow.confirm_required, task_status_flow.confirm_message"
							+ " from task_status_flow"
							+ " join task_status on task_status_flow.next_task_status = task_status.task_status";
					String taskStatus = MetrixDatabaseManager.getFieldStringValue("task", "task_status", "task_id = " + mTaskId);
					String taskType = MetrixDatabaseManager.getFieldStringValue("task", "task_type", "task_id = " + mTaskId);

					// try to get a list of possible task_status values, using a status/type match on task_status_flow
					mStatusInfo = new LinkedHashMap<String, HashMap<String, String>>();
					String tsfQuery = String.format("%1$s where task_status_flow.active = 'Y' and task_status_flow.task_status = '%2$s' and task_status_flow.task_type = '%3$s' order by task_status.sequence, task_status.description", tsfSelectChunk, taskStatus, taskType);
					boolean taskTypeWorked = false;
					if (!MetrixStringHelper.isNullOrEmpty(taskType)) {
						MetrixCursor taskTypeCursor = MetrixDatabaseManager.rawQueryMC(tsfQuery, null);
						if (taskTypeCursor != null && taskTypeCursor.moveToFirst()) {
							while (taskTypeCursor.isAfterLast() == false) {
								HashMap<String, String> row = new HashMap<String, String>();
								String nextTaskStatusDescription = taskTypeCursor.getString(0);
								row.put("next_task_status", taskTypeCursor.getString(1));
								row.put("comment_required", taskTypeCursor.getString(2));
								row.put("comment_message", taskTypeCursor.getString(3));
								row.put("confirm_required", taskTypeCursor.getString(4));
								row.put("confirm_message", taskTypeCursor.getString(5));
								mStatusInfo.put(nextTaskStatusDescription, row);
								taskTypeCursor.moveToNext();
							}
							taskTypeWorked = true;
						}
						taskTypeCursor.close();
					}

					// if status/type match failed, retry with just status
					boolean taskStatusAloneWorked = false;
					if (!taskTypeWorked) {
						tsfQuery = String.format("%1$s where task_status_flow.active = 'Y' and task_status_flow.task_status = '%2$s' and task_status_flow.task_type is null order by task_status.sequence, task_status.description", tsfSelectChunk, taskStatus);
						MetrixCursor taskStatusCursor = MetrixDatabaseManager.rawQueryMC(tsfQuery, null);
						if (taskStatusCursor != null && taskStatusCursor.moveToFirst()) {
							while (taskStatusCursor.isAfterLast() == false) {
								HashMap<String, String> row = new HashMap<String, String>();
								String nextTaskStatusDescription = taskStatusCursor.getString(0);
								row.put("next_task_status", taskStatusCursor.getString(1));
								row.put("comment_required", taskStatusCursor.getString(2));
								row.put("comment_message", taskStatusCursor.getString(3));
								row.put("confirm_required", taskStatusCursor.getString(4));
								row.put("confirm_message", taskStatusCursor.getString(5));
								mStatusInfo.put(nextTaskStatusDescription, row);
								taskStatusCursor.moveToNext();
							}
							taskStatusAloneWorked = true;
						}
						taskStatusCursor.close();
					}

					// if still none found, grab all other task_status values, straight up (excluding CL-status values)
					if (!taskTypeWorked && !taskStatusAloneWorked) {
						String otherQuery = String.format("select task_status.description, task_status.task_status from task_status"
								+ " where task_status.active = 'Y' and task_status.status <> 'CL' and task_status.task_status <> '%s' order by task_status.sequence, task_status.description", taskStatus);
						MetrixCursor otherCursor = MetrixDatabaseManager.rawQueryMC(otherQuery, null);
						if (otherCursor != null && otherCursor.moveToFirst()) {
							while (otherCursor.isAfterLast() == false) {
								HashMap<String, String> row = new HashMap<String, String>();
								String nextTaskStatusDescription = otherCursor.getString(0);
								row.put("next_task_status", otherCursor.getString(1));
								mStatusInfo.put(nextTaskStatusDescription, row);
								otherCursor.moveToNext();
							}
						}
					}
				}

				setupNextTaskStatusDialog();
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void setupNextTaskStatusDialog() {
		if (mStatusInfo != null && mStatusInfo.size() > 0) {

			mStatuses = new ArrayList<String>(mStatusInfo.keySet());
			Collections.sort(mStatuses);

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(AndroidResourceHelper.getMessage("ChooseNewStatus"));
			CharSequence[] items = mStatuses.toArray(new CharSequence[mStatuses.size()]);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@SuppressLint("InflateParams") public void onClick(DialogInterface dialog, int pos) {
					String selectedNextTaskStatusDesc = mStatuses.get(pos);
					HashMap<String, String> statusItem = mStatusInfo.get(selectedNextTaskStatusDesc);
					mNextTaskStatus = statusItem.get("next_task_status");
					String commentRequired = statusItem.get("comment_required");
					String confirmRequired = statusItem.get("confirm_required");
					String commentMessage = statusItem.get("comment_message");
					String confirmMessage = statusItem.get("confirm_message");
					boolean isCommentRequired = (!MetrixStringHelper.isNullOrEmpty(commentRequired) && MetrixStringHelper.valueIsEqual(commentRequired, "Y"));
					boolean isConfirmRequired = (!MetrixStringHelper.isNullOrEmpty(confirmRequired) && MetrixStringHelper.valueIsEqual(confirmRequired, "Y"));

					boolean passesCompletionValidation = false;
					if (!MetrixStringHelper.valueIsEqual(mNextTaskStatus, "COMPLETED")) {
						passesCompletionValidation = true;
					} else if (mActivity instanceof MetrixActivity && ((MetrixActivity)mActivity).taskMeetsCompletionPrerequisites(mTaskId)) {
						passesCompletionValidation = true;
					}
					if (!passesCompletionValidation)
						return;

					// If both COMMENT_REQUIRED and CONFIRM_REQUIRED are set, do not display the confirmation request.
					// Entering a comment implies confirmation.
					if (isCommentRequired) {
						AlertDialog.Builder commentBuilder = new AlertDialog.Builder(mActivity);
						LayoutInflater inflater = mActivity.getLayoutInflater();
						View inflatedView = inflater.inflate(R.layout.next_task_status_comment_dialog, null);
						commentBuilder.setView(inflatedView);
						TextView commentMsg = (TextView) inflatedView.findViewById(R.id.nts_comment_message);
						if (!MetrixStringHelper.isNullOrEmpty(commentMessage)) {
							commentMsg.setText(commentMessage);
							commentMsg.setVisibility(View.VISIBLE);
						} else {
							commentMsg.setVisibility(View.GONE);
						}
						mCommentEntry = (TextView) inflatedView.findViewById(R.id.nts_comment);
						AlertDialog commentAlert = commentBuilder.create();
						commentAlert.setTitle(AndroidResourceHelper.getMessage("EnterReason"));
						commentAlert.setButton(DialogInterface.BUTTON_NEUTRAL,AndroidResourceHelper.getMessage("CancelButton"), commentListener);
						commentAlert.setButton(DialogInterface.BUTTON_POSITIVE, (AndroidResourceHelper.getMessage("Save")), commentListener);
						//commentAlert.setButton(DialogInterface.BUTTON_NEGATIVE, (AndroidResourceHelper.getMessage("Cancel")), commentListener);

						commentAlert.show();

						final Button okButton = commentAlert.getButton(AlertDialog.BUTTON_POSITIVE);
						okButton.setEnabled(false);
						mCommentEntry.addTextChangedListener(new TextWatcher()
						{
							@Override
							public void beforeTextChanged(CharSequence s, int start, int count, int after) {
							}

							@Override
							public void onTextChanged(CharSequence s, int start, int before, int count) {
								if (mCommentEntry.getText().length() > 0) {
									okButton.setEnabled(true);
								} else {
									okButton.setEnabled(false);
								}
							}

							@Override
							public void afterTextChanged(Editable s) {
							}
						});
					} else if (isConfirmRequired) {
						AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(mActivity);
						confirmBuilder.setTitle(AndroidResourceHelper.getMessage("ConfirmChange"));
						if (!MetrixStringHelper.isNullOrEmpty(confirmMessage))
							confirmBuilder.setMessage(confirmMessage);
						confirmBuilder.setPositiveButton(AndroidResourceHelper.getMessage("Yes"), confirmListener).setNegativeButton((AndroidResourceHelper.getMessage("No")), confirmListener).show();
					} else {
						processTaskStatusUpdate("");
						if (mActivity instanceof DebriefSignature) {
							((DebriefSignature)mActivity).setCompleteEnabled();
						}

					}
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private void processTaskStatusUpdate(final String commentString) {

		try {

			if(MetrixStringHelper.valueIsEqual(mNextTaskStatus, "ACCEPTED"))
			{
				if(isPooledTask) {
					if(SettingsHelper.getSyncPause(mActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mActivity);
						if(syncPauseAlertDialog != null)
						{
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									MetrixPooledTaskAssignmentManager.instance().doTeamTaskAccept(mActivity, mTaskId, mNextTaskStatus, commentString, true);
								}
							});
						}
					}
					else
						MetrixPooledTaskAssignmentManager.instance().doTeamTaskAccept(mActivity, mTaskId, mNextTaskStatus, commentString, true);

					return;
				}
			}

			boolean isTaskCompletion = MetrixStringHelper.valueIsEqual(mNextTaskStatus, "COMPLETED");
			String transactionFriendlyName = AndroidResourceHelper.getMessage("TaskStatusUpdate");
			if (isTaskCompletion) {
				transactionFriendlyName = AndroidResourceHelper.getMessage("TaskCompletion");
			}

			boolean shouldJumpToJobList = false;
			String statusValue = MetrixDatabaseManager.getFieldStringValue("task_status", "status", String.format("task_status = '%s'", mNextTaskStatus));
			shouldJumpToJobList = (MetrixStringHelper.valueIsEqual(statusValue, "CA")
					|| MetrixStringHelper.valueIsEqual(statusValue, "CL")
					|| MetrixStringHelper.valueIsEqual(statusValue, "CO")
					|| MetrixStringHelper.valueIsEqual(mNextTaskStatus, MobileApplication.getAppParam("REJECTED_TASK_STATUS")));

			MetrixSqlData taskData = new MetrixSqlData("task", MetrixTransactionTypes.UPDATE);
			taskData.dataFields.add(new DataField("task_id", mTaskId));
			taskData.dataFields.add(new DataField("metrix_row_id", MetrixDatabaseManager.getFieldStringValue("task", "metrix_row_id", "task_id = " + mTaskId)));
			taskData.dataFields.add(new DataField("task_status", mNextTaskStatus));
			taskData.dataFields.add(new DataField("task_status_comment", commentString));
			taskData.dataFields.add(new DataField("status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, ISO8601.Yes, true)));

			String updateGPS = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
			if (!MetrixStringHelper.isNullOrEmpty(updateGPS) && updateGPS.compareToIgnoreCase("Y") == 0 && MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK")) {
				Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mActivity);
				if (currentLocation != null) {
					taskData.dataFields.add(new DataField("geocode_lat", MetrixFloatHelper.convertNumericFromForcedLocaleToDB(Double.toString(currentLocation.getLatitude()), Locale.US)));
					taskData.dataFields.add(new DataField("geocode_long", MetrixFloatHelper.convertNumericFromForcedLocaleToDB(Double.toString(currentLocation.getLongitude()), Locale.US)));
				}
			}

			MetrixTimeClockAssistant.updateAutomatedTimeClock(mActivity, mNextTaskStatus);

			taskData.filter = String.format("task_id = %s", mTaskId);
			ArrayList<MetrixSqlData> taskTransaction = new ArrayList<MetrixSqlData>();
			taskTransaction.add(taskData);

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
			boolean successful = MetrixUpdateManager.update(taskTransaction, true, transactionInfo, transactionFriendlyName, mActivity);
			if (successful) {
				MetrixFormManager.resetOriginalValue("task.task_status", mNextTaskStatus);

				if (shouldJumpToJobList) {
					if (isTaskCompletion) {
						String playSound = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_PLAY_SOUND'");
						if (playSound.compareToIgnoreCase("Y") == 0) {
							// User settings override the system setting
							if (SettingsHelper.getPlaySound(mActivity)) {
								MediaPlayer player = MediaPlayer.create(mActivity, R.raw.swoosh07);
								player.start();
							}
						}
					}

					Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
					MetrixActivityHelper.startNewActivityAndFinish(mActivity, intent);
				} else if (mActivity instanceof MetrixActivity) {
					// in general, refresh activity if task_status has been changed
					if(MetrixStringHelper.valueIsEqual(((MetrixActivity)mActivity).getClass().getSimpleName(),"DebriefTaskAttachmentAdd")){
						Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, DebriefTaskAttachment.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						MetrixActivityHelper.startNewActivity(mActivity, intent);
					}
					else{
						((MetrixActivity)mActivity).reloadActivity();
					}
				} else {
					// pass-through else block that we do not expect to get hit
					setTaskStatusButtonImage();
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	DialogInterface.OnClickListener commentListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // OK
					String commentString = mCommentEntry.getText().toString();
					processTaskStatusUpdate(commentString);
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // Cancel (do nothing)
					break;
			}
		}
	};

	DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					processTaskStatusUpdate("");
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					break;
			}
		}
	};

	private void hideView(View view) {
		view.setVisibility(View.GONE);
	}

	private void setupClickListener(final ImageButton button) {
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (button.getTag().toString().compareToIgnoreCase("taskStatus") == 0) {
					handleTaskStatusClick();
				} else {
					boolean isGoingToTaskAttachment = false;
					Class<?> activity = null;
					if (button.getTag().toString().compareToIgnoreCase("overview") == 0) {
						activity = DebriefOverview.class;
					} else if (button.getTag().toString().compareToIgnoreCase("attachments") == 0) {
						// The baseline attachment API screen DebriefAttachmentList exists in current Debrief workflow
						if(MetrixWorkflowManager.isScreenNameExistsInCurrentWorkflow(mActivity, "DebriefAttachmentList")) {
							AttachmentWidgetManager.openFromWorkFlow(mActivity, "Debrief");
							return;
						}
						else {
							isGoingToTaskAttachment = true;
							activity = DebriefTaskAttachment.class;
						}
					} else if (button.getTag().toString().compareToIgnoreCase("notes") == 0) {
						activity = DebriefTaskTextList.class;
					}

					Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, activity);
					if (isGoingToTaskAttachment) {
						intent.putExtra("fromQuickLinksBar", new MetrixParcelable<Boolean>(true));
					}
					MetrixActivityHelper.startNewActivity(mActivity, intent);
				}
			}
		});
	}
}
