package com.metrix.architecture.utilities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * a helper class to display UI controls, which includes Progress dialog,
 * datetimepicker etc.
 *
 * @author elin
 *
 */
public class MetrixUIHelper {
	ProgressDialog loadingDialog;
	Activity activity;

	public MetrixUIHelper(Activity activity) {
		this.activity = activity;
	}

	public void showLoadingDialog(final String message) {
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					try {
						if (!isDialogActive()) {
							loadingDialog = ProgressDialog.show(activity, "", message, true, false);
						}
					}
					catch(Exception ex) {
						if(activity!=null)
							LogManager.getInstance(activity).debug(ex.getMessage());
					}
				}
			});
		}
	}

	/**
	 * display message on progress dialog on the UI thread
	 *
	 * @param message
	 */
	public void updateLoadingDialog(final String message) {
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					loadingDialog.setMessage(message);
				}
			});
		}
	}

	public boolean isDialogActive() {
		if (loadingDialog != null) {
			return loadingDialog.isShowing();
		} else {
			return false;
		}
	}

	/**
	 * Release loading dialog resource
	 */
	public void dismissLoadingDialog() {
		if (activity != null && loadingDialog != null) {
			try {
				loadingDialog.dismiss();
			}
			catch(Exception ex){
				if(activity != null)
					LogManager.getInstance(activity).debug(ex.getMessage());
			}
		}
	}

	public void showErrorDialog(String errorMessage) {
		new AlertDialog.Builder(activity).setMessage(errorMessage)
				.setTitle(AndroidResourceHelper.getMessage("Error"))
				.setPositiveButton(AndroidResourceHelper.getMessage("OK"), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				}).create().show();
	}

	/**
	 * This method can be invoked to display an AlertDialog for an
	 * error message which will included the error message received
	 * and an OK button.
	 *
	 * @param errorMessage The error message to include.
	 * @since 5.6
	 */
	public void showErrorDialogOnGuiThread(final String errorMessage) {
		try {
			if (activity != null) {
				activity.runOnUiThread(new Runnable() {
					public void run() {
						new AlertDialog.Builder(activity)
								.setMessage(errorMessage)
								.setTitle(AndroidResourceHelper.getMessage("Error"))
								.setPositiveButton(AndroidResourceHelper.getMessage("OK"),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog, int id) {
												dialog.dismiss();
												dismissLoadingDialog();
											}
										}).create().show();
					}
				});
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	public static void showErrorDialogOnGuiThread(final Activity activity,
												  final String errorMessage) {
		try {
			if (activity != null) {
				activity.runOnUiThread(new Runnable() {
					public void run() {
						new AlertDialog.Builder(activity)
								.setMessage(errorMessage)
								.setTitle(AndroidResourceHelper.getMessage("Error"))
								.setPositiveButton(AndroidResourceHelper.getMessage("OK"),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog, int id) {
												dialog.dismiss();
											}
										}).create().show();
					}
				});
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}


	/**
	 * Show error dialog and navigate to the next intent when hit the OK button
	 * @param activity
	 * @param nextInent
	 * @param errorMessage
	 */
	public static void showErrorDialogOnGuiThread(final Activity activity, final Intent nextInent,
												  final String errorMessage) {
		try {
			if (activity != null) {
				activity.runOnUiThread(new Runnable() {
					public void run() {
						new AlertDialog.Builder(activity)
								.setMessage(errorMessage)
								.setTitle(AndroidResourceHelper.getMessage("Error"))
								.setPositiveButton(AndroidResourceHelper.getMessage("OK"),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog, int id) {
												dialog.dismiss();
												MetrixActivityHelper.startNewActivityAndFinish(activity, nextInent);
											}
										}).create().show();
					}
				});
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	public static void showSnackbar(final Activity activity, final String message) {
		String coordinatorLayoutId = String.valueOf(MetrixPublicCache.instance.getItem("R.id.coordinator_layout"));
		if (MetrixStringHelper.isNullOrEmpty(coordinatorLayoutId)) { coordinatorLayoutId = "0"; }

		CoordinatorLayout coordinatorLayout = (CoordinatorLayout)activity.findViewById(Integer.valueOf(coordinatorLayoutId));

		if(coordinatorLayout == null)
			showSnackbar(activity, 0, message, "", null);
		else {
			for(int i = 0; i < coordinatorLayout.getChildCount(); i++) {
				if(coordinatorLayout.getChildAt(i) instanceof CoordinatorLayout) {
					coordinatorLayout = (CoordinatorLayout)coordinatorLayout.getChildAt(i);
				}
			}
			showSnackbar(activity, coordinatorLayout, message, "", null);
		}
	}

	public static void showSnackbar(final Activity activity, final String message, final String actionText, final View.OnClickListener clickAction) {
		String coordinatorLayoutId = String.valueOf(MetrixPublicCache.instance.getItem("R.id.coordinator_layout"));
		if (MetrixStringHelper.isNullOrEmpty(coordinatorLayoutId)) { coordinatorLayoutId = "0"; }

		CoordinatorLayout coordinatorLayout = (CoordinatorLayout)activity.findViewById(Integer.valueOf(coordinatorLayoutId));

		if(coordinatorLayout == null)
			showSnackbar(activity, 0, message, actionText, clickAction);
		else {
			for(int i = 0; i < coordinatorLayout.getChildCount(); i++) {
				if(coordinatorLayout.getChildAt(i) instanceof CoordinatorLayout) {
					coordinatorLayout = (CoordinatorLayout)coordinatorLayout.getChildAt(i);
				}
			}
			showSnackbar(activity, coordinatorLayout, message, actionText, clickAction);
		}
	}

	public static void showSnackbar(final Activity activity, final int parentViewId, final String message) {
		showSnackbar(activity, parentViewId, message, "", null);
	}

	public static void showSnackbar(final Activity activity, final int parentViewId, final String message, final String actionText, final View.OnClickListener clickAction) {
		try {
			if (activity != null) {
				View v = null;
				if (parentViewId > 0)
					v = activity.findViewById(parentViewId);			// resolve view by ID passed in
				if (v == null)
					v = activity.findViewById(android.R.id.content);	// if not found, fallback to root view

				showSnackbar(activity, v, message, actionText, clickAction);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private static void showSnackbar(final Activity activity, final View v, final String message, final String actionText, final View.OnClickListener clickAction) {
		if (activity != null && v != null) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					try {
						Snackbar sb = Snackbar.make(v, message, Snackbar.LENGTH_LONG);
						if (!MetrixStringHelper.isNullOrEmpty(actionText))
							sb.setAction(actionText, clickAction);
						sb.show();
					} catch (Exception e) {
						LogManager.getInstance().error(e);
					}
				}
			});
		}
	}

	public static Bitmap RotateBitmap(Bitmap source, float angle)
	{
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	//Show row count, specially when there's filtering enabled
	public static void displayListRowCount(final Activity activity, TextView rowCountView, int rowCount){
		try {
			if (rowCountView != null) {
				rowCountView.setText(AndroidResourceHelper.getMessage("RecordsFound1Arg", rowCount));
			}
		} catch (Exception e) {
			LogManager.getInstance(activity).error(e);
		}
	}

	@SuppressLint("DefaultLocale")
	/***
	 * Get the previous items count for a set of Debrief screens.
	 * @param screenName
	 * @param currentActivity
	 * @return items count
	 */
	public static int getPreviousDebriefItemCount(String screenName, Activity currentActivity) {
		int count = -1;
		//standard screen
		if (MetrixApplicationAssistant.screenNameHasClassInCode(screenName)) {
			if (MetrixStringHelper.valueIsEqual(screenName, "DebriefPartUsageType"))
				count = MetrixDatabaseManager.getCount("part_usage", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
			else if (MetrixStringHelper.valueIsEqual(screenName, "DebriefPayment"))
				count = MetrixDatabaseManager.getCount("payment", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
			else if (MetrixStringHelper.valueIsEqual(screenName, "DebriefProductRemove")) {
				String requestId = MetrixDatabaseManager.getFieldStringValue("task", "request_id", String.format("task_id = %s", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
				StringBuilder whereClause = new StringBuilder();
				whereClause.append(String.format("request_unit.request_id = '%s'", requestId));
				whereClause.append(" and request_unit.part_id is not null");
				whereClause.append(" and not exists (select disposition_id from part_disp where request_unit_id = request_unit.request_unit_id)");
				whereClause.append(" order by request_unit.request_unit_id");
				count = MetrixDatabaseManager.getCount("request_unit", whereClause.toString());
			}
			else if (MetrixStringHelper.valueIsEqual(screenName, "DebriefTaskAttachment"))
				count = MetrixDatabaseManager.getCount("task_attachment join attachment on task_attachment.attachment_id = attachment.attachment_id", "task_attachment.task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id") + " and (attachment.attachment_type is null or attachment.attachment_type != 'SIGNATURE')");
			else if (MetrixStringHelper.valueIsEqual(screenName, "DebriefTaskSteps"))
				count = MetrixDatabaseManager.getCount("task_steps", "task_steps.task_unit_id is null and task_steps.task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
		} else {
			//codeless screen
			int csScreenId = MetrixScreenManager.getScreenId(screenName);
			HashMap<String, String> screenPropertyMap = MetrixScreenManager.getScreenProperties(csScreenId);
			if (screenPropertyMap != null) {
				String linkedScreenID = screenPropertyMap.get("linked_screen_id");
				String screenType = screenPropertyMap.get("screen_type");
				if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
					if (screenType.toLowerCase().contains("standard") && !MetrixStringHelper.isNullOrEmpty(linkedScreenID)) {
						String whereClauseScript = null;
						String primaryTable = null;
						if (screenPropertyMap.containsKey("where_clause_script"))
							whereClauseScript = screenPropertyMap.get("where_clause_script");
						if (screenPropertyMap.containsKey("primary_table"))
							primaryTable = screenPropertyMap.get("primary_table");
						ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
						String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(currentActivity), clientScriptDef);
						count = MetrixDatabaseManager.getCount(primaryTable, result);
					} else if (screenType.toLowerCase().contains("list")) {
						String whereClauseScript = null;
						String primaryTable = null;
						if (screenPropertyMap.containsKey("where_clause_script"))
							whereClauseScript = screenPropertyMap.get("where_clause_script");
						if (screenPropertyMap.containsKey("primary_table"))
							primaryTable = screenPropertyMap.get("primary_table");
						ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
						String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(currentActivity), clientScriptDef);
						if (MetrixStringHelper.isNullOrEmpty(result))
							result = "";
						String query = MetrixListScreenManager.generateListQuery(primaryTable, result, csScreenId);
						MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
						if (cursor != null) {
							count = cursor.getCount();
							cursor.close();
						}
					}
				}
			}
		}
		return count;
	}

	public static void setFirstGradientColorsForButton(Button btn, String firstGradient1String,
													   String firstGradient2String, String firstGradientTextString, float cornerRadius) {
		if (!MetrixStringHelper.isNullOrEmpty(firstGradient1String) || !MetrixStringHelper.isNullOrEmpty(firstGradient2String)) {
			firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
			firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
			int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
			int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);

			int[] colors = new int[2];
			colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
			colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
			GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
			firstGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
			firstGradient.setStroke(1, Color.BLACK);

			int[] lightColors = new int[2];
			String lightGradient1String = MetrixSkinManager.generateLighterVersionOfColor(firstGradient1String, 0.4f, false);
			String lightGradient2String = MetrixSkinManager.generateLighterVersionOfColor(firstGradient2String, 0.4f, false);
			int lightGradient1 = (int)Long.parseLong(lightGradient1String, 16);
			int lightGradient2 = (int)Long.parseLong(lightGradient2String, 16);
			lightColors[0] = Color.rgb((lightGradient1 >> 16) & 0xFF, (lightGradient1 >> 8) & 0xFF, (lightGradient1 >> 0) & 0xFF);
			lightColors[1] = Color.rgb((lightGradient2 >> 16) & 0xFF, (lightGradient2 >> 8) & 0xFF, (lightGradient2 >> 0) & 0xFF);
			GradientDrawable lightGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, lightColors);
			lightGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
			lightGradient.setStroke(1, Color.BLACK);

			StateListDrawable states = new StateListDrawable();
			states.addState(new int[] {-android.R.attr.state_enabled}, lightGradient);
			states.addState(new int[] {android.R.attr.state_pressed}, lightGradient);
			states.addState(new int[] {}, firstGradient);

			btn.setBackgroundDrawable(states);
		}

		if (!MetrixStringHelper.isNullOrEmpty(firstGradientTextString)) {
			btn.setTextColor(Color.parseColor(firstGradientTextString));
		}
	}

	public static boolean hasImage(@NonNull ImageView view) {
		final Drawable drawable = view.getDrawable();
		boolean hasImage = (drawable != null);

		if (hasImage && drawable instanceof BitmapDrawable) {
			hasImage = ((BitmapDrawable)drawable).getBitmap() != null;
		}

		return hasImage;
	}

	public static Bitmap getBitmapFromImageView(@NonNull ImageView view) {
		final Drawable drawable = view.getDrawable();
		if (drawable != null && drawable instanceof BitmapDrawable)
			return ((BitmapDrawable)drawable).getBitmap();

		return null;
	}

	public static Bitmap applyBackgroundColorToBitmap(Bitmap bitmap, @ColorInt int color) {
		if (bitmap == null)
			return null;

		Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		Canvas canvas = new Canvas(newBitmap);
		canvas.drawColor(color);
		canvas.drawBitmap(bitmap, 0, 0, null);
		return newBitmap;
	}
}


