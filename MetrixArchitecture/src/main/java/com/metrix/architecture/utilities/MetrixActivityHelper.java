package com.metrix.architecture.utilities;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixActivityDef;

public class MetrixActivityHelper {
	static private final String EXTRA_WRAPPED_INTENT = "ActivityHelper_wrappedIntent";

	/**
	 * Create a precondition activity intent.
	 * 
	 * @param activity
	 *            the original activity
	 * @param preconditionActivityClass
	 *            the precondition activity's class
	 * @return an intent which will launch the precondition activity.
	 */
	public static Intent createActivityIntent(Activity activity, Class<?> preconditionActivityClass) {
		Intent newIntent = new Intent();
		newIntent.setClass(activity, preconditionActivityClass);
		newIntent.putExtra(EXTRA_WRAPPED_INTENT, activity.getIntent());
		return newIntent;
	}

	public static Intent createActivityIntent(Activity activity, Class<?> preconditionActivityClass, int flag) {
		Intent newIntent = new Intent();
		newIntent.setClass(activity, preconditionActivityClass);
		newIntent.putExtra(EXTRA_WRAPPED_INTENT, activity.getIntent());
		newIntent.addFlags(flag);
		return newIntent;
	}	
	
	public static Intent createActivityIntent(Activity activity, Class<?> preconditionActivityClass, MetrixTransactionTypes transactionType, String keyName, String keyValue) {
		Intent newIntent = createActivityIntent(activity, preconditionActivityClass);
		newIntent.putExtra(MetrixActivityDef.METRIX_ACTIVITY_DEF, new MetrixActivityDef(transactionType, keyName, keyValue));
		return newIntent;
	}

	public static Intent createActivityIntent(Activity activity, Class<?> preconditionActivityClass, MetrixTransactionTypes transactionType, HashMap<String, String> keys) {
		Intent newIntent = createActivityIntent(activity, preconditionActivityClass);
		newIntent.putExtra(MetrixActivityDef.METRIX_ACTIVITY_DEF, new MetrixActivityDef(transactionType, keys));
		return newIntent;
	}

	/**
	 * Start the original activity, and finish the precondition activity.
	 * 
	 * @param preconditionActivity
	 * @deprecated As of release 5.6.0. Method was not used baseline.
	 */
	@Deprecated
	public static void startOriginalActivityAndFinish(
			Activity preconditionActivity) {
		preconditionActivity.startActivity((Intent) preconditionActivity
				.getIntent().getParcelableExtra(EXTRA_WRAPPED_INTENT));
		preconditionActivity.finish();

	}

	/**
	 * Starts a new activity and leaves any existing activity running.
	 * 
	 * @param activity
	 *            the currently running activity.
	 * @param intent
	 *            the intent to start the new activity.
	 */
	public static void startNewActivity(Activity activity, Intent intent) {
		activity.startActivity(intent);
	}

	/**
	 * Starts a new activity and leaves any existing activity running.
	 * 
	 * @param activity
	 *            the currently running activity.
	 * @param preconditionActivityClass
	 *            the intent to start the new activity.
	 */
	public static void startNewActivity(Activity activity, Class<?> preconditionActivityClass) {
		Intent intent = createActivityIntent(activity,
				preconditionActivityClass);
		startNewActivity(activity, intent);
	}

	/**
	 * Starts a new activity and leaves any existing activity running.
	 * 
	 * @param activity
	 *            the currently running activity.
	 * @param preconditionActivityClass
	 *            the intent to start the new activity.
	 */
	public static void startNewActivity(Activity activity, Class<?> preconditionActivityClass, String extraName, String extraValue) {
		Intent intent = createActivityIntent(activity,
				preconditionActivityClass);
		intent.putExtra(extraName, extraValue);
		startNewActivity(activity, intent);
	}

	/**
	 * Start the precondition activity using a given intent, which should have
	 * been created by calling createPreconditionIntent.
	 * 
	 * @param activity
	 * @param intent
	 */
	public static void startNewActivityAndFinish(Activity activity, Intent intent) {
		activity.startActivity(intent);
		activity.finish();
	}
	
	/**
	 * Start the precondition activity using a given intent with transition animation, which should have
	 * been created by calling createPreconditionIntent.
	 * 
	 * @param activity
	 * @param intent
	 * @param enterAnimatedResource animation resource
	 * @param exitAnimatedResource animation resource
	 */
	public static void startNewActivityAndFinishWithTransition(Activity activity, Intent intent, int enterAnimatedResource, int exitAnimatedResource) {
		activity.startActivity(intent);
		activity.overridePendingTransition(enterAnimatedResource, exitAnimatedResource);
		activity.finish();
	}

	/**
	 * Start the precondition activity using a given intent, which should have
	 * been created by calling createPreconditionIntent.
	 * 
	 * @param activity
	 * @param preconditionActivityClass
	 */
	public static void startNewActivityAndFinish(Activity activity, Class<?> preconditionActivityClass) {
		Intent intent = createActivityIntent(activity,
				preconditionActivityClass);
		startNewActivityAndFinish(activity, intent);
	}

	/**
	 * Start the precondition activity using a given intent, which should have
	 * been created by calling createPreconditionIntent.
	 * 
	 * @param activity
	 * @param preconditionActivityClass
	 */
	public static void startNewActivityAndFinish(Activity activity, Class<?> preconditionActivityClass, String extraName, String extraValue) {
		Intent intent = createActivityIntent(activity,
				preconditionActivityClass);
		intent.putExtra(extraName, extraValue);

		startNewActivityAndFinish(activity, intent);
	}

	/**
	 * @param pkg
	 * @param componentName
	 * @return
	 */
	public static Intent activityIntent(String pkg, String componentName) {
		Intent result = new Intent();
		result.setClassName(pkg, componentName);
		return result;
	}
	
	/**
	 * Create an intent associated to an activity identified by the activity's
	 * name. 
	 * 
	 * @param activity the current activity.
	 * @param activityName the name of the activity to start.
	 * @return the intent to start the named activity.
	 * @since 5.6.1
	 */
	public static Intent createActivityIntent(Activity activity, String activityName) {
		Intent intent;
		try {
			intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile", activityName);
			if (intent == null)
				intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", activityName);
			return intent;
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return null;
	}

	/**
	 * Create an intent associated to an activity identified by the activity's
	 * name.
	 *
	 * @param activity the current activity.
	 * @param activityName the name of the activity to start.
	 * @param flag the intent flag to apply
	 * @return the intent to start the named activity.
	 * @since 5.7.0
	 */
	public static Intent createActivityIntent(Activity activity, String activityName, int flag) {
		Intent newIntent = MetrixActivityHelper.createActivityIntent(activity, activityName);
		newIntent.addFlags(flag);
		return newIntent;
	}

	/**
	 * Create an intent associated to an activity identified by the activity's
	 * name. 
	 * 
	 * @param activity the current activity.
	 * @param namespace the namespace containing the activity.
	 * @param activityName the name of the activity to start.
	 * @return the intent to start the named activity.
	 * @since 5.6.1
	 */
	public static Intent createActivityIntent(Activity activity, String namespace, String activityName) {
		try {
			Intent intent = new Intent();
			intent.setClass(activity, Class.forName(namespace + "." + activityName));
			intent.putExtra(EXTRA_WRAPPED_INTENT, activity.getIntent());
			return intent;
		} catch (ClassNotFoundException e) {
			LogManager.getInstance().error(e);
		}
		return null;
	}

	/**
	 * Create an intent associated to an activity identified by the activity's
	 * name.
	 *
	 * @param activity the current activity.
	 * @param namespace the namespace containing the activity.
	 * @param activityName the name of the activity to start.
	 * @param flag the intent flag to apply
	 * @return the intent to start the named activity.
	 * @since 5.7.0
	 */
	public static Intent createActivityIntent(Activity activity, String namespace, String activityName, int flag) {
		Intent newIntent = MetrixActivityHelper.createActivityIntent(activity, namespace, activityName);
		newIntent.addFlags(flag);
		return newIntent;
	}

	/****
	 * Added this method, since sometimes the soft keyboard doesn't hide even if we have added
	 * android:windowSoftInputMode="stateHidden" in AndroidManifest.xml 
	 * Explicitly hides keyboard
	 * @param currentWindow
	 */
	public static void explicitlyHideSoftKeyboard(Window currentWindow) {
		if(currentWindow != null)
			currentWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
	/****
	 * Added this method, so that we can pass any type of object when navigating to another activity
	 * @param activity
	 * @param preconditionActivityClass
	 * @param extraKey
	 * @param extraValue
	 * @param finishCurrentActivity
	 */
	public static void startNewActivityAndFinish(Activity activity, Class<?> preconditionActivityClass, String extraKey, Parcelable extraValue, boolean finishCurrentActivity) {
		Intent intent = createActivityIntent(activity,preconditionActivityClass);
		intent.putExtra(extraKey, extraValue);

		if(finishCurrentActivity)
			startNewActivityAndFinish(activity, intent);
		else
			startNewActivity(activity, intent);
	}

	/**
	 * Returns the initial screen intent as specified by MOBILE_INITIAL_SCREEN app param. Falls back
	 * to Home if any error occurs
	 * @param activity is the current running activity
	 * @return activity intent to start initial screen
	 */
	public static Intent getInitialActivityIntent(Activity activity) {
		String initialScreenName;

		// Redirect to the DemoHome screen if it is a DEMO_BUILD version
		if (MetrixApplicationAssistant.getMetaBooleanValue(activity, "DemoBuild")) {
			initialScreenName = "DemoHome";
		} else {
			initialScreenName = MetrixDatabaseManager.getFieldStringValue("metrix_app_params",
					"param_value", "param_name = 'MOBILE_INITIAL_SCREEN'");
		}

		Intent intent = null;
		if (MetrixStringHelper.isNullOrEmpty(initialScreenName)) {
			try {
				Class<?> clazz = Class.forName("com.metrix.metrixmobile.Home");
				intent = MetrixActivityHelper.createActivityIntent(activity, clazz);
			} catch (ClassNotFoundException e) {
				// This should never hit.
			}
		} else {
			try {
				Class<?> clazz = Class.forName("com.metrix.metrixmobile." + initialScreenName);
				intent = MetrixActivityHelper.createActivityIntent(activity, clazz);
			} catch (ClassNotFoundException e) {
				try {
					Class<?> clazz = Class.forName("com.metrix.metrixmobile.system." + initialScreenName);
					intent = MetrixActivityHelper.createActivityIntent(activity, clazz);
				} catch (ClassNotFoundException e1) {
					LogManager.getInstance().error(e1);

					try {
						Class<?> clazz = Class.forName("com.metrix.metrixmobile.Home");
						intent = MetrixActivityHelper.createActivityIntent(activity, clazz);
					} catch (ClassNotFoundException e2) {
						// This should never hit.
					}
				}
			}
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static void hideKeyboard(Activity activity) {
		if (activity == null) return;
		View view = activity.getCurrentFocus();
		if (view != null) {
			InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputManager != null)
				inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}
}