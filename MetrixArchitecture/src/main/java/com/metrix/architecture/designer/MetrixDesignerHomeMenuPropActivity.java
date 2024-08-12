package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerHomeMenuPropActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mTitle;
	private Button mSave, mFinish;
	private String mItemName;
	private MetrixDesignerResourceData mHomeMenuPropResourceData;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mHomeMenuPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerHomeMenuPropActivityResourceData");

            setContentView(mHomeMenuPropResourceData.LayoutResourceID);

            mTable = (LinearLayout) findViewById(mHomeMenuPropResourceData.getExtraResourceID("R.id.table_layout"));
			mItemName = MetrixCurrentKeysHelper.getKeyValue("mm_home_item", "item_name");
            populateScreen();
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

	@Override
	public void onStart() {
		super.onStart();

		helpText = mHomeMenuPropResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null)
			mActionBarTitle.setText(mHeadingText);

		mTitle = (TextView) findViewById(mHomeMenuPropResourceData.getExtraResourceID("R.id.zzmd_home_menu_prop_title"));
        String fullTitle = AndroidResourceHelper.getMessage("Properties1Args", mItemName);
        mTitle.setText(fullTitle);

		mSave = (Button) findViewById(mHomeMenuPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setText(AndroidResourceHelper.getMessage("Save"));
		mSave.setEnabled(mAllowChanges);
        mSave.setOnClickListener(this);

        mFinish = (Button) findViewById(mHomeMenuPropResourceData.getExtraResourceID("R.id.finish"));
        mFinish.setOnClickListener(this);

        TextView mTitle = (TextView) findViewById(mHomeMenuPropResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_home_menu_prop"));

        AndroidResourceHelper.setResourceValues(mTitle, "ScnInfoMxDesHomeMenuProp");
        AndroidResourceHelper.setResourceValues(mFinish, "Finish");

	}

	private void populateScreen() {
		String itemID = MetrixCurrentKeysHelper.getKeyValue("mm_home_item", "item_id");

		String metrixRowID = "";
		String label = "";
		String imageID = "";
		String countScript = "";
		String screenId = "";
		String tapEvent = "";

		String query = "select mm_home_item.metrix_row_id, mm_home_item.label, mm_home_item.image_id,"
                + " mm_home_item.count_script, mm_home_item.screen_id, mm_home_item.tap_event"
                + " from mm_home_item where item_id = " + itemID;

		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				label = cursor.getString(1);
				imageID = cursor.getString(2);
				countScript = cursor.getString(3);
				screenId = cursor.getString(4);
				tapEvent = cursor.getString(5);

				if (MetrixStringHelper.isNullOrEmpty(label)) label = "";
				if (MetrixStringHelper.isNullOrEmpty(imageID)) imageID = "";
				if (MetrixStringHelper.isNullOrEmpty(countScript)) countScript = "";
				if (MetrixStringHelper.isNullOrEmpty(screenId)) screenId = "";
				if (MetrixStringHelper.isNullOrEmpty(tapEvent)) tapEvent = "";

				int thisItemIDNum = Integer.valueOf(itemID);

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_home_item.metrix_row_id", metrixRowID);
				origRow.put("mm_home_item.label", label);
				origRow.put("mm_home_item.image_id", imageID);
				origRow.put("mm_home_item.count_script", countScript);
				origRow.put("mm_home_item.screen_id", screenId);
				origRow.put("mm_home_item.tap_event", tapEvent);
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
		msgQuery.append(" WHERE c.code_name = 'MM_HOME_ITEM_PROP_NAME' AND c.code_value NOT IN ('ORDER')");
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
                // MESSAGE LOOKUP	LABEL
				// SCRIPT LOOKUP	COUNT_SCRIPT, TAP_EVENT
				// IMAGE_LOOKUP		IMAGE_ID
				// COMBOBOX			SCREEN_ID
				// TEXTBOX			(default, all others)
				LinearLayout layout;
				if (MetrixStringHelper.valueIsEqual(propName, "LABEL")) {
					// MESSAGE LOOKUP
					layout = MetrixControlAssistant.addLinearLayout(this, mHomeMenuPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
					String messageType = "MM_HOME_" + propName;
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
					TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
					etPropValue.setId(MetrixControlAssistant.generateViewId());
					etPropValue.setText(label);
					etPropValue.setOnFocusChangeListener(this);
					etPropValue.addTextChangedListener(new MessageTextWatcher(messageType, tvDescLabel, tvDescText));

					tvDescLabel.setVisibility(View.GONE);
                    tvDescText.setVisibility(View.GONE);
                    if (!MetrixStringHelper.isNullOrEmpty(label)) {
                        String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", messageType, label));
                        if (!MetrixStringHelper.isNullOrEmpty(description)) {
							tvDescText.setText(description);
							tvDescLabel.setVisibility(View.INVISIBLE);
							tvDescText.setVisibility(View.VISIBLE);
						}
                    }
                } else if (MetrixStringHelper.valueIsEqual(propName, "COUNT_SCRIPT") || MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT")) {
					// SCRIPT LOOKUP
					String propValue = MetrixStringHelper.valueIsEqual(propName, "COUNT_SCRIPT") ? countScript : tapEvent;
					layout = MetrixControlAssistant.addLinearLayout(this, mHomeMenuPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
					TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
					etPropValue.setId(MetrixControlAssistant.generateViewId());
					etPropValue.setText(propValue);
					etPropValue.setOnFocusChangeListener(this);
					etPropValue.addTextChangedListener(new ClientScriptTextWatcher(tvDescLabel, tvDescText));

					if (MetrixStringHelper.valueIsEqual(mItemName, "Close App") || (MetrixStringHelper.valueIsEqual(mItemName, "Work Status") && MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT")))
						etPropValue.setEnabled(false);

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

					if (MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT")) {
						layout.setTag("TAP_EVENT");
						if (!MetrixStringHelper.isNullOrEmpty(screenId))
							etPropValue.setEnabled(false);
						etPropValue.addTextChangedListener(new TextWatcher() {
							@Override
							public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
							@Override
							public void onTextChanged(CharSequence s, int start, int before, int count) {}
							@Override
							public void afterTextChanged(Editable s) { reactToSelectedTapEvent(); }
						});
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "IMAGE_ID")) {
					// IMAGE_LOOKUP
					layout = MetrixControlAssistant.addLinearLayout(this, mHomeMenuPropResourceData.getExtraResourceID("R.layout.zzmd_prop_image_lookup_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
					ImageView ivPreview = (ImageView) layout.findViewWithTag("image_preview");
					tvPropValue.setId(MetrixControlAssistant.generateViewId());
					tvPropValue.addTextChangedListener(new ImageTextWatcher(ivPreview, mHomeMenuPropResourceData.getExtraResourceID("R.drawable.no_image80x80")));
					tvPropValue.setText(imageID);

                    Button btnSelect = (Button) layout.findViewWithTag("image_select");
					Button btnClear = (Button) layout.findViewWithTag("image_clear");

					AndroidResourceHelper.setResourceValues(btnSelect, "Select");
					AndroidResourceHelper.setResourceValues(btnClear, "Clear");
					btnSelect.setOnClickListener(new OnClickListener() {
						public void onClick (View v) {
							LinearLayout layout = (LinearLayout) v.getParent().getParent();
                            TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
                            MetrixPublicCache.instance.addItem("imagePickerTextView", tvPropValue);
							MetrixPublicCache.instance.addItem("imagePickerParentLayout", layout);
							Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerHomeMenuPropActivity.this, "com.metrix.architecture.ui.widget", "ImagePicker");
							startActivityForResult(intent, 8181);
                        }
                    });
					btnClear.setOnClickListener(new OnClickListener() {
						public void onClick (View v) {
							LinearLayout layout = (LinearLayout) v.getParent().getParent();
							TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
							tvPropValue.setText("");
                        }
                    });
				} else if (MetrixStringHelper.valueIsEqual(propName, "SCREEN_ID")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mHomeMenuPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");
					StringBuilder screenIdQuery = new StringBuilder();
					screenIdQuery.append("select mm_screen.screen_name, mm_screen.screen_id from mm_screen");
					screenIdQuery.append(" where (tab_parent_id is null and screen_type not in ('ATTACHMENT_API_CARD', 'ATTACHMENT_API_LIST')");
					screenIdQuery.append(" and revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
					screenIdQuery.append(" and screen_id not in (select linked_screen_id from mm_screen where linked_screen_id is not null and screen_id in (select screen_id from mm_workflow_screen))");
					screenIdQuery.append(" and screen_id not in (select screen_id from mm_workflow_screen where screen_id is not null)");
					screenIdQuery.append(" and screen_id not in (select screen_id from mm_home_item where screen_id is not null))");
					if (!MetrixStringHelper.isNullOrEmpty(screenId))
						screenIdQuery.append(" or screen_id = " + screenId);
					screenIdQuery.append(" order by mm_screen.screen_name asc");
					MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, screenIdQuery.toString(), true);

					if (MetrixStringHelper.valueIsEqual(mItemName, "Close App") || MetrixStringHelper.valueIsEqual(mItemName, "Work Status"))
						spnPropValue.setEnabled(false);

					try {
						if (MetrixStringHelper.valueIsEqual(propName, "SCREEN_ID")) {
							MetrixControlAssistant.setValue(spnPropValue, screenId);
							layout.setTag("SCREEN_ID");
							spnPropValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
								public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedScreen(); }
								public void onNothingSelected(AdapterView<?> parent) { reactToSelectedScreen(); }
							});

							if (!MetrixStringHelper.isNullOrEmpty(tapEvent))
								spnPropValue.setEnabled(false);
						}
					} catch (Exception e) {
						LogManager.getInstance().error(e);
					}
				} else {
					// TEXTBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mHomeMenuPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_line"), mTable);
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

				if (MetrixStringHelper.valueIsEqual(propName, "COUNT_SCRIPT") || MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT"))
					doClientScriptSelection(v.getId(), rowLayout);
				else
					doMessageSelection(propName, v.getId(), rowLayout);
			}
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

        int viewId = v.getId();
		if (viewId == mHomeMenuPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

                Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mHomeMenuPropResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				processAndSaveChanges();
            }
            // allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuEnablingActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void reactToSelectedScreen() {
		try {
			LinearLayout screenLayout = (LinearLayout)mTable.findViewWithTag("SCREEN_ID");
			LinearLayout tapEventLayout = (LinearLayout)mTable.findViewWithTag("TAP_EVENT");
			Spinner spnScreenId = (Spinner) screenLayout.findViewWithTag("property_value");
			EditText etTapEvent = (EditText) tapEventLayout.findViewWithTag("property_value");
			String selectedScreen = MetrixControlAssistant.getValue(spnScreenId);
			if (!MetrixStringHelper.isNullOrEmpty(selectedScreen)) {
				MetrixControlAssistant.setValue(etTapEvent, "");
				etTapEvent.setEnabled(false);
			} else {
				etTapEvent.setEnabled(true);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void reactToSelectedTapEvent() {
		try {
			LinearLayout screenLayout = (LinearLayout)mTable.findViewWithTag("SCREEN_ID");
			LinearLayout tapEventLayout = (LinearLayout)mTable.findViewWithTag("TAP_EVENT");
			Spinner spnScreenId = (Spinner) screenLayout.findViewWithTag("property_value");
			EditText etTapEvent = (EditText) tapEventLayout.findViewWithTag("property_value");
			String selectedTapEvent = MetrixControlAssistant.getValue(etTapEvent);
			if (!MetrixStringHelper.isNullOrEmpty(selectedTapEvent)) {
				MetrixControlAssistant.setValue(spnScreenId, "");
				spnScreenId.setEnabled(false);
			} else {
				spnScreenId.setEnabled(true);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void doMessageSelection(String propName, int viewToPopulateId, LinearLayout parentLayout) {
		MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
		lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_HOME_" + propName));

		Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
		intent.putExtra("NoOptionsMenu", true);
		MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
		MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
		startActivityForResult(intent, 2727);
	}

	private void processAndSaveChanges() {
		try {
			int itemPropChangeCount = 0;

            ArrayList<MetrixSqlData> itemToUpdate = new ArrayList<MetrixSqlData>();

            String itemID = MetrixCurrentKeysHelper.getKeyValue("mm_home_item", "item_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_home_item", "metrix_row_id", "item_id = " + itemID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_home_item", "created_revision_id", "item_id = " + itemID);
			MetrixSqlData data = new MetrixSqlData("mm_home_item", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("item_id", itemID));

            for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

                String controlType = tvControlType.getText().toString();
                String propName = tvPropName.getText().toString();
				String currentPropValue = "";

                if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX")	|| MetrixStringHelper.valueIsEqual(controlType, "LOOKUP")) {
					EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
					currentPropValue = etPropValue.getText().toString();
				} else if (MetrixStringHelper.valueIsEqual(controlType, "COMBOBOX")) {
					Spinner spnPropValue = (Spinner) currLayout.findViewWithTag("property_value");
					if (spnPropValue != null) {
						currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
					}
				} else if (MetrixStringHelper.valueIsEqual(controlType, "IMAGE_LOOKUP")) {
					TextView tvPropValue = (TextView) currLayout.findViewWithTag("property_value");
					currentPropValue = tvPropValue.getText().toString();
				} else {
					throw new Exception("MDHomeMenuProp: Unhandled control type.");
				}

                int currItemIDNum = Integer.valueOf(itemID);
				HashMap<String, String> origRow = mOriginalData.get(currItemIDNum);

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
					origPropValue = origRow.get("mm_home_item.label");
				else if (MetrixStringHelper.valueIsEqual(propName, "IMAGE_ID"))
					origPropValue = origRow.get("mm_home_item.image_id");
				else if (MetrixStringHelper.valueIsEqual(propName, "COUNT_SCRIPT"))
					origPropValue = origRow.get("mm_home_item.count_script");
				else if (MetrixStringHelper.valueIsEqual(propName, "SCREEN_ID"))
					origPropValue = origRow.get("mm_home_item.screen_id");
				else if (MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT"))
					origPropValue = origRow.get("mm_home_item.tap_event");

                if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
                    if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
						data.dataFields.add(new DataField("label", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "IMAGE_ID"))
						data.dataFields.add(new DataField("image_id", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "COUNT_SCRIPT"))
						data.dataFields.add(new DataField("count_script", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "SCREEN_ID"))
						data.dataFields.add(new DataField("screen_id", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT"))
						data.dataFields.add(new DataField("tap_event", currentPropValue));

					itemPropChangeCount++;
                }
            }

            if (itemPropChangeCount > 0) {
				// upon detecting changes...
				// update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this item has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
                String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
                // if previousRevisionID exists, then this is not the first revision
                if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
                        && (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
                    // field meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}

                itemToUpdate.add(data);

                MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(itemToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("HomeItemChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
		}
	}
}

