package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays") public class MetrixDesignerScreenItemPropActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mScreenItemName;
	private MetrixDesignerResourceData mScreenItemPropResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mScreenItemPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenItemPropActivityResourceData");

		setContentView(mScreenItemPropResourceData.LayoutResourceID);

		mTable = (LinearLayout) findViewById(mScreenItemPropResourceData.getExtraResourceID("R.id.table_layout"));
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mScreenItemPropResourceData.HelpTextString;

		mScreenItemName = MetrixCurrentKeysHelper.getKeyValue("mm_screen_item", "item_name");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mScreenItemPropResourceData.getExtraResourceID("R.id.zzmd_screen_item_prop_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesScnItmProp", mScreenItemName, mScreenName);
		mEmphasis.setText(fullText);

		mSave = (Button) findViewById(mScreenItemPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mScreenItemPropResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mScrItems = (TextView) findViewById(mScreenItemPropResourceData.getExtraResourceID("R.id.screen_item_properties"));

		AndroidResourceHelper.setResourceValues(mScrItems, "ScreenItemProperties");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");
	}

	private void populateScreen() {
		String itemID = MetrixCurrentKeysHelper.getKeyValue("mm_screen_item", "item_id");
		String metrixRowID = "";
		String event = "";
		String eventType = "";
		String active = "";

		String query = "SELECT mm_screen_item.metrix_row_id, mm_screen_item.event, mm_screen_item.event_type, mm_screen_item.active FROM mm_screen_item WHERE mm_screen_item.item_id = " + itemID;

		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				event = cursor.getString(1);
				eventType = cursor.getString(2);
				active = cursor.getString(3);

				if (MetrixStringHelper.isNullOrEmpty(event)) event = "";
				if (MetrixStringHelper.isNullOrEmpty(eventType)) eventType = "";
				if (MetrixStringHelper.isNullOrEmpty(active)) active = "";

				int thisItemIDNum = Integer.valueOf(itemID);

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_screen_item.metrix_row_id", metrixRowID);
				origRow.put("mm_screen_item.item_id", itemID);
				origRow.put("mm_screen_item.event", event);
				origRow.put("mm_screen_item.event_type", eventType);
				origRow.put("mm_screen_item.active", active);
				mOriginalData.put(thisItemIDNum, origRow);

				break;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// generate an ordered map, so that properties appear in translated alphabetical order
		// iterate through map and dynamically render layouts accordingly
		StringBuilder msgQuery = new StringBuilder();
		msgQuery.append("SELECT c.code_value, v.message_text FROM metrix_code_table c");
		msgQuery.append(" JOIN mm_message_def_view v ON v.message_id = c.message_id AND v.message_type = 'CODE'");
		msgQuery.append(" WHERE c.code_name = 'MM_SCREEN_ITEM_PROP_NAME'");
		msgQuery.append(" ORDER BY v.message_text ASC");

		MetrixCursor msgCursor = null;
		try {
			msgCursor = MetrixDatabaseManager.rawQueryMC(msgQuery.toString(), null);

			if (msgCursor == null || !msgCursor.moveToFirst()) {
				return;
			}

			while (msgCursor.isAfterLast() == false) {
				String propName = msgCursor.getString(0);
				String propNameString = msgCursor.getString(1);

				// using property_name, use hard-coded map to determine what layout to inflate and populate with data
				// CHECKBOX					ACTIVE
				// CLIENT SCRIPT LOOKUP		EVENT
				// COMBOBOX					EVENT_TYPE
				// TEXTBOX					(default, all others)

				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "ACTIVE")) {
					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenItemPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					if (active.compareToIgnoreCase("Y") == 0)
						chkPropValue.setChecked(true);
					else
						chkPropValue.setChecked(false);
				} else if (MetrixStringHelper.valueIsEqual(propName, "EVENT_TYPE")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenItemPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");

					StringBuilder spinnerQuery = new StringBuilder();
					spinnerQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
					spinnerQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
					spinnerQuery.append(String.format("where metrix_code_table.code_name = 'MM_SCREEN_ITEM_%s' ", propName));
					spinnerQuery.append("order by mm_message_def_view.message_text asc");
					MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, spinnerQuery.toString(), true);

					try {
						if (MetrixStringHelper.valueIsEqual(propName, "EVENT_TYPE"))
							MetrixControlAssistant.setValue(spnPropValue, eventType);

						String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = "
								+ MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
						if (MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD")) {
							// Force event type to be VALIDATION and disable.
							MetrixControlAssistant.setValue(spnPropValue, "VALIDATION");
							spnPropValue.setEnabled(false);
						}
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "EVENT")) {
					// CLIENT SCRIPT LOOKUP
					String propValue = event;
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenItemPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
					TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
					etPropValue.setId(MetrixControlAssistant.generateViewId());
					etPropValue.setText(propValue);
					etPropValue.setOnFocusChangeListener(this);
					etPropValue.addTextChangedListener(new ClientScriptTextWatcher(tvDescLabel, tvDescText));

					tvDescLabel.setVisibility(View.GONE);
					tvDescText.setVisibility(View.GONE);
					if (!MetrixStringHelper.isNullOrEmpty(propValue)) {
						String description = "";
						String scriptName = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "name", String.format("unique_vs = '%s'", propValue));
						String scriptVersion = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "version_number", String.format("unique_vs = '%s'", propValue));
						if (!MetrixStringHelper.isNullOrEmpty(scriptName) && !MetrixStringHelper.isNullOrEmpty(scriptVersion)) {
							if (MetrixStringHelper.valueIsEqual(scriptVersion, "0"))
								description = String.format("%1$s (%2$s)", scriptName, AndroidResourceHelper.getMessage("Baseline"));
							else
								description = String.format("%1$s (%2$s %3$s)", scriptName, AndroidResourceHelper.getMessage("Version"), scriptVersion);

							tvDescText.setText(description);
							tvDescLabel.setVisibility(View.INVISIBLE);
							tvDescText.setVisibility(View.VISIBLE);
						}
					}
				}

				// all layouts have these, so set generically
				TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
				TextView tvItemID = (TextView) layout.findViewWithTag("pv_id");
				TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
				TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

				tvMetrixRowID.setText(metrixRowID);
				tvItemID.setText(itemID);
				tvPropName.setText(propName);
				tvPropNameString.setText(propNameString);

				msgCursor.moveToNext();
			}
		} finally {
			if (msgCursor != null) {
				msgCursor.close();
			}
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		Object tagObj = v.getTag();
		if (v instanceof EditText && tagObj != null && tagObj instanceof String) {
			String viewTag = tagObj.toString();
			if (MetrixStringHelper.valueIsEqual(viewTag, "property_value") && hasFocus) {
				LinearLayout rowLayout = (LinearLayout) v.getParent();
				TextView tvPropName = (TextView) rowLayout.findViewWithTag("property_name");
				String propName = tvPropName.getText().toString();

				if (MetrixStringHelper.valueIsEqual(propName, "EVENT"))
					doClientScriptSelection(v.getId(), rowLayout);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mScreenItemPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges && processAndSaveChanges()) {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenItemPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mScreenItemPropResourceData.getExtraResourceID("R.id.finish")) {
			boolean success = true;
			if (mAllowChanges) {
				success = processAndSaveChanges();
			}
			// allow pass through, even if changes aren't allowed (popping this activity off of stack)
			if (success) {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenItemActivity.class);
				intent.putExtra("headingText", mHeadingText);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		}
	}

	private boolean processAndSaveChanges() {
		try {
			int screenItemPropChangeCount = 0;
			String currEvent = "";
			String currEventType = "";

			ArrayList<MetrixSqlData> screenItemToUpdate = new ArrayList<MetrixSqlData>();

			String itemID = MetrixCurrentKeysHelper.getKeyValue("mm_screen_item", "item_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_screen_item", "metrix_row_id", "item_id = " + itemID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_screen_item", "created_revision_id", "item_id = " + itemID);
			MetrixSqlData data = new MetrixSqlData("mm_screen_item", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("item_id", itemID));

			for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

				String controlType = tvControlType.getText().toString();
				String propName = tvPropName.getText().toString();
				String currentPropValue = "";

				if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX") || MetrixStringHelper.valueIsEqual(controlType, "LONGTEXT")
						|| MetrixStringHelper.valueIsEqual(controlType, "LOOKUP")) {
					EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
					currentPropValue = etPropValue.getText().toString();
				} else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
					CheckBox chkPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
					if (chkPropValue.isChecked())
						currentPropValue = "Y";
					else
						currentPropValue = "N";
				} else if (MetrixStringHelper.valueIsEqual(controlType, "COMBOBOX")) {
					Spinner spnPropValue = (Spinner) currLayout.findViewWithTag("property_value");
					currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
				} else if (MetrixStringHelper.valueIsEqual(controlType, "LABEL")) {
					// don't detect changes, and move onto next control
					continue;
				} else {
					throw new Exception("MDScreenItemProp: Unhandled control type.");
				}

				int currItemIDNum = Integer.valueOf(itemID);
				HashMap<String, String> origRow = mOriginalData.get(currItemIDNum);

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "ACTIVE")) {
					origPropValue = origRow.get("mm_screen_item.active");
				} else if (MetrixStringHelper.valueIsEqual(propName, "EVENT")) {
					origPropValue = origRow.get("mm_screen_item.event");
					currEvent = origPropValue;
				} else if (MetrixStringHelper.valueIsEqual(propName, "EVENT_TYPE")) {
					origPropValue = origRow.get("mm_screen_item.event_type");
					currEventType = origPropValue;
				}

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "ACTIVE")) {
						data.dataFields.add(new DataField("active", currentPropValue));
					} else if (MetrixStringHelper.valueIsEqual(propName, "EVENT")) {
						data.dataFields.add(new DataField("event", currentPropValue));
						currEvent = currentPropValue;
					} else if (MetrixStringHelper.valueIsEqual(propName, "EVENT_TYPE")) {
						data.dataFields.add(new DataField("event_type", currentPropValue));
						currEventType = currentPropValue;
					}

					screenItemPropChangeCount++;
				}
			}

			if (screenItemPropChangeCount > 0) {
				// before trying to save changes, validate that event/event_type both have values or are both blank
				if ((!MetrixStringHelper.isNullOrEmpty(currEventType) && MetrixStringHelper.isNullOrEmpty(currEvent))
						|| (MetrixStringHelper.isNullOrEmpty(currEventType) && !MetrixStringHelper.isNullOrEmpty(currEvent))) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("ScnItmEvntErr"), Toast.LENGTH_LONG).show();
					return false;
				}

				// upon detecting changes...
				// update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this field has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
				String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
				// if previousRevisionID exists, then this is not the first revision
				if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
						&& (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
					// field meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}

				screenItemToUpdate.add(data);

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(screenItemToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("ScreenItemChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}

