package com.metrix.architecture.superclasses;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;
import com.metrix.architecture.signature.SignatureField;

/**
 * MetrixBaseActivity serves as the superclass for all of the activity
 * classes in the application. It contains a number of attributes and
 * abstract methods that all super classing classes must implement in
 * order to get consistent behavior throughout the application.
 *
 * @since 5.4
 */
public abstract class MetrixBaseActivity extends AppCompatActivity implements ProviderInstaller.ProviderInstallListener {
	public Class<?> nextActivity;
	public String helpText;

	public String codeLessScreenName;
	public int codeLessScreenId;
	public boolean isCodelessScreen;

	//Tablet UI Optimization
	//Keeping the linked screen information - when run in Tablet Landscape mode
	public int linkedScreenIdInTabletUIMode;
	public String linkedScreenTypeInTabletUIMode;
	//Mark when LIST screen is in workflow
	public boolean isCodelessListScreenInTabletUIMode;
	public boolean linkedScreenAvailableInTabletUIMode;
	//Keeping the LIST screen full name for coded screens(Ex: DebriefExpense, listActivityFullName would be com.metrix.metrixmobile.DebriefExpenseList)
	//- when run in Tablet Landscape mode 
	public String listActivityFullNameInTabletUIMode;
	//This is to inform - we must run Tablet specific UI
	public boolean shouldRunTabletSpecificUIMode;
	//This is only when a LIST Screen in workflow
	public boolean showListScreenLinkedScreenInTabletUIMode;
	//(Ex: DebriefTaskSteps screen : Where LIST screen comes/stays in Workflow)
	public String standardActivityFullNameInTabletUIMode;
	//End Tablet UI Optimization

	public static Uri baseMediaUri;
	public static SignatureField launchingSignatureField;
	public static final int BASE_SHOW_FILEDIALOG = 1230;
	public static final int SELECT_FILES = 789;
	public static final int BASE_TAKE_PICTURE = 4560;
	public static final int BASE_TAKE_VIDEO = 9990;
	public static final int BASE_PICTURE_CAMERA_PERMISSION = 13370;
	public static final int BASE_VIDEO_CAMERA_PERMISSION = 13380;
	public static final int BASE_TAKE_SIGNATURE =  2288;
	public static final int BASE_ATTACHMENT_ADD = 4980;
	public static final int BASE_ATTACHMENT_EDIT = 4981;

	private static final int ERROR_DIALOG_REQUEST_CODE = 1;

	private boolean mRetryProviderInstall;

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	//Update the security provider when the activity is created.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ProviderInstaller.installIfNeededAsync(this, this);
	}

	/**
	 * This method is only called if the provider is successfully updated
	 * (or is already up-to-date).
	 */
	@Override
	public void onProviderInstalled() {
		// Provider is up-to-date, app can make secure network calls.
	}

	/**
	 * This method is called if updating fails; the error code indicates
	 * whether the error is recoverable.
	 */

	public  void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
		if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
			// Recoverable error. Show a dialog prompting the user to
			// install/update/enable Google Play services.
			GooglePlayServicesUtil.showErrorDialogFragment(
					errorCode,
					this,
					ERROR_DIALOG_REQUEST_CODE,
					new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							// The user chose not to take the recovery action
							onProviderInstallerNotAvailable();
						}
					});
		} else {
			// Google Play services is not available.
			onProviderInstallerNotAvailable();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
									Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
			// Adding a fragment via GooglePlayServicesUtil.showErrorDialogFragment
			// before the instance state is restored throws an error. So instead,
			// set a flag here, which will cause the fragment to delay until
			// onPostResume.
			mRetryProviderInstall = true;
		}
	}

	/**
	 * On resume, check to see if we flagged that we need to reinstall the
	 * provider.
	 */
	@Override
	protected void onPostResume() {
		super.onPostResume();

		if (mRetryProviderInstall) {
			// We can now safely retry installation.
			ProviderInstaller.installIfNeededAsync(this, this);
		}
		mRetryProviderInstall = false;
	}


	private void onProviderInstallerNotAvailable() {
		// This is reached if the provider cannot be updated for some reason.
		// App should consider all HTTP communication to be vulnerable, and take
		// appropriate action.
	}

	protected abstract void defineForm();
	protected abstract void defaultValues();
	protected abstract void setListeners();
	protected abstract void setHyperlinkBehavior();
	protected abstract void displayPreviousCount();
	protected abstract void beforeStartForError();
	protected abstract void beforeUpdateForError();
	public abstract void resetFABOffset();
	public abstract void showIgnoreErrorDialog(String message, Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow);
}