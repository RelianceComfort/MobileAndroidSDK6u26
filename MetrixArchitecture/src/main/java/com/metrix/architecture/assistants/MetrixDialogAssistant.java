package com.metrix.architecture.assistants;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * Contains helper methods to make it easy to raise interactive dialogs to
 * the user and respond based on their feedback. showAlertDialog allows you
 * to open the dialog, present the user with two choices, and then react to
 * either by defining an <code>OnClickListener</code> for each. 
 * showConfirmDeleteDialog allows you to ask the user to confirm that they
 * want to delete some data and then again define a <code>OnClickListener</code>
 * to respond to their choice. showConfirmClearDialog is typically used for
 * signature views to ask the user to confirm that they want to clear out
 * the signature they previously entered. Finally showEditOrDeleteDialog is
 * typically used from a ListView to ask the user if they want to edit or
 * delete a row of data that they clicked on.
 *
 * @since 5.4
 */
@SuppressWarnings("deprecation")
public class MetrixDialogAssistant {
	/**
	 * Shows an alert dialog to the user with a message and the option to chose
	 * one of two options.
	 *
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message that the dialog should display.
	 * @param button1Text
	 *            the text of the first button choice.
	 * @param button1Listener
	 *            a listener to process the selection of the first button.
	 * @param button2Text
	 *            the text of the second button choice.
	 * @param button2Listener
	 *            a listener to process the selection of the second button.
	 * @param context
	 *            the Android context that the dialog should exist in.
	 */
	public static void showAlertDialog(String title, String message, String button1Text, OnClickListener button1Listener, String button2Text, OnClickListener button2Listener, Context context) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		dialog.setTitle(title);
		dialog.setMessage(message);

		if (button1Listener == null) {
			button1Listener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			};
		}
		dialog.setButton(button1Text, button1Listener);

		if (button2Listener == null) {
			button2Listener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			};
		}

		dialog.setButton2(button2Text, button2Listener);
		dialog.show();
	}

	/**
	 * Shows a dialog to the user asking them to confirm that they want to
	 * delete a row of data. Allow them to select yes or no.
	 *
	 * @param tableName
	 *            a friendly name of the table that the row will be deleted
	 *            from.
	 * @param yesButtonListener
	 *            a listener to process the selection of the yes button.
	 * @param noButtonListener
	 *            a listener to process the selection of the no button.
	 * @param context
	 *            the Android context that the dialog should exist in.
	 *
	 * <pre>
	 * MetrixDialogAssistant.showConfirmDeleteDialog("contact", yesListener, null, this);
	 * </pre>
	 */
	public static void showConfirmDeleteDialog(String tableName, OnClickListener yesButtonListener, OnClickListener noButtonListener, Context context) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		dialog.setTitle(AndroidResourceHelper.getMessage("ConfirmDeleteTitle"));
		dialog.setMessage(AndroidResourceHelper.getMessage("ConfirmDeleteMessage", tableName));
		dialog.setButton(AndroidResourceHelper.getMessage("Yes"), yesButtonListener);

		if (noButtonListener == null) {
			noButtonListener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			};
		}

		dialog.setButton2(AndroidResourceHelper.getMessage("No"), noButtonListener);
		dialog.show();
	}

	/**
	 * Shows a dialog to the user asking them to confirm that they want to clear
	 * a row of data or signature. Allow them to select yes or no.
	 *
	 * @param tableName
	 *            a friendly name of the table that the row or signature will be
	 *            cleared from.
	 * @param yesButtonListener
	 *            a listener to process the selection of the yes button.
	 * @param noButtonListener
	 *            a listener to process the selection of the no button.
	 * @param context
	 *            the Android context that the dialog should exist in.
	 *
	 * <pre>
	 * MetrixDialogAssistant.showConfirmClearDialog("signature", yesListener,
	 * 	null, this);
	 * </pre>
	 */
	public static void showConfirmClearDialog(String tableName, OnClickListener yesButtonListener, OnClickListener noButtonListener, Context context) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		dialog.setTitle(AndroidResourceHelper.getMessage("ConfirmClearTitle"));
		dialog.setMessage(AndroidResourceHelper.getMessage("ConfirmClearMessage", tableName));
		dialog.setButton(AndroidResourceHelper.getMessage("Yes"), yesButtonListener);

		if (noButtonListener == null) {
			noButtonListener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			};
		}

		dialog.setButton2(AndroidResourceHelper.getMessage("No"), noButtonListener);
		dialog.show();
	}

	/**
	 * Shows a dialog to the user asking them to choose if they want to modify
	 * or delete a row of data.
	 *
	 * @param tableName
	 *            a friendly name of the table that the row or signature will be
	 *            cleared from.
	 * @param editButtonListener
	 *            a listener to process the selection of the edit button.
	 * @param deleteButtonListener
	 *            a listener to process the selection of the delete button.
	 * @param context
	 *            the Android context that the dialog should exist in.
	 *
	 * <pre>
	MetrixDialogAssistant.showEditOrDeleteDialog("escalation", modifyListener, deleteListener, this);
	 * </pre>
	 */
	public static void showEditOrDeleteDialog(String tableName, OnClickListener editButtonListener, OnClickListener deleteButtonListener, Context context) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		dialog.setTitle(AndroidResourceHelper.getMessage("ModifyOrDeleteTitle"));
		dialog.setMessage(AndroidResourceHelper.getMessage("ModifyOrDeleteMessage", tableName));
		dialog.setButton(AndroidResourceHelper.getMessage("Modify"), editButtonListener);
		dialog.setButton2(AndroidResourceHelper.getMessage("Delete"), deleteButtonListener);

		dialog.show();
	}

	/**
	 * Shows a dialog to the user asking them to choose if they want to modify
	 * or delete a row of data.
	 *
	 * @param mediaType
	 *            media type.
	 * @param saveButtonListener
	 *            a listener to process the selection of the save button.
	 * @param retakeButtonListener
	 *            a listener to process the selection of the re-take button.
	 * @param context
	 *            the Android context that the dialog should exist in.
	 *
	 * <pre>
	MetrixDialogAssistant.showEditOrDeleteDialog("escalation", modifyListener, deleteListener, this);
	 * </pre>
	 */
	public static void showRetakeDialog(String mediaType, OnClickListener saveButtonListener, OnClickListener retakeButtonListener, Context context) {

		try{
			AlertDialog dialog = new AlertDialog.Builder(context).create();
			dialog.setTitle(AndroidResourceHelper.getMessage("RetakeTitle"));
			dialog.setMessage(AndroidResourceHelper.getMessage("RetakeMessage", mediaType));
			dialog.setButton(AndroidResourceHelper.getMessage("Yes"), retakeButtonListener);
			dialog.setButton2(AndroidResourceHelper.getMessage("No"), saveButtonListener);

			dialog.show();
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
	}

	/**
	 * Shows a dialog to the user asking whether he/she needs to modify the attachment.
	 *
	 * @param mediaType
	 *            media type.
	 * @param saveButtonListener
	 *            a listener to process the selection of the save button.
	 * @param retakeButtonListener
	 *            a listener to process the selection of the re-take button.
	 * @param context
	 *            the Android context that the dialog should exist in.
	 *
	 * <pre>
	MetrixDialogAssistant.showEditOrDeleteDialog("escalation", modifyListener, deleteListener, this);
	 * </pre>
	 */
	public static void showModifyDialog(String tableName, OnClickListener yesButtonListener, OnClickListener noButtonListener, Context context) {

		try{
			AlertDialog dialog = new AlertDialog.Builder(context).create();
			dialog.setTitle(AndroidResourceHelper.getMessage("Modify"));
			dialog.setMessage(AndroidResourceHelper.getMessage("ModifyMessage", tableName));
			dialog.setButton(AndroidResourceHelper.getMessage("Yes"), yesButtonListener);

			if (noButtonListener == null) {
				noButtonListener = new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				};
			}
			dialog.setButton2(AndroidResourceHelper.getMessage("No"), noButtonListener);

			dialog.show();
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
	}

	public static SyncPauseAlertDialog showSyncPauseAlertDialog(Context context)
	{
		SyncPauseAlertDialog syncPauseAlertDialog = null;
		try{
			syncPauseAlertDialog = new SyncPauseAlertDialog(context);
			syncPauseAlertDialog.setTitle(AndroidResourceHelper.getMessage("SyncPauseDialogTitle"));
			syncPauseAlertDialog.setMessage(AndroidResourceHelper.getMessage("SyncPauseDialogMessage"));
			syncPauseAlertDialog.show();
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}

		return syncPauseAlertDialog;
	}

}

