package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.R;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
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
public class MetrixDesignerScreenPropActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mTitle;
	private Button mSave, mViewItems, mViewFields;
	private CheckBox mAllowModifyChkbox, mAllowDeleteChkbox;
	private EditText mTapEventEdtText;
	private String mScreenName;
	private static boolean mLinkedScreenCtrlHasOptions;
	private boolean mIsTabChildScreen;
	private boolean mIsFirstTabChild;
	private String mCurrentScreenType;
	private Spinner mLinkedScreen;
	private AlertDialog mSaveScreenDialog;
	private MetrixDesignerResourceData mScreenPropResourceData;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mScreenPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenPropActivityResourceData");

		setContentView(mScreenPropResourceData.LayoutResourceID);

		mTable = (LinearLayout) findViewById(mScreenPropResourceData.getExtraResourceID("R.id.table_layout"));
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mScreenPropResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mTitle = (TextView) findViewById(mScreenPropResourceData.getExtraResourceID("R.id.zzmd_screen_prop_title"));
		String fullTitle =  AndroidResourceHelper.getMessage("Properties1Args", mScreenName);
		mTitle.setText(fullTitle);

		mSave = (Button) findViewById(mScreenPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mViewItems = (Button) findViewById(mScreenPropResourceData.getExtraResourceID("R.id.view_items"));
		mViewItems.setOnClickListener(this);
		// Disallow View Items for Tab Parent/Child screens and Attachment API List
		if (mIsTabChildScreen || MetrixStringHelper.valueIsEqual(mCurrentScreenType, "TAB_PARENT")|| MetrixStringHelper.valueIsEqual(mCurrentScreenType, "ATTACHMENT_API_LIST"))
			mViewItems.setEnabled(false);

		mViewFields = (Button) findViewById(mScreenPropResourceData.getExtraResourceID("R.id.view_fields"));
		mViewFields.setOnClickListener(this);
		mViewFields.setEnabled(thisScreenHasFields());

        TextView mScrInfo = (TextView) findViewById(mScreenPropResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_screen_prop"));

        AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesScnProp");
        AndroidResourceHelper.setResourceValues(mSave, "Save");
        AndroidResourceHelper.setResourceValues(mViewItems, "ViewItems");
        AndroidResourceHelper.setResourceValues(mViewFields, "ViewFields");

		if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
			// if we get here, we have to go to MetrixDesignerFieldActivity		
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldActivity.class);
			intent.putExtra("headingText", mHeadingText);

			String targetDesignerActivity = (String) this.getIntent().getExtras().get("targetDesignerActivity");
			if (!MetrixStringHelper.valueIsEqual(targetDesignerActivity, "MetrixDesignerFieldActivity")) {
				intent.putExtra("targetDesignerActivity", targetDesignerActivity);
			}

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void checkTapEventValue() {
		try {
			String selectedTapEvent = MetrixControlAssistant.getValue(mTapEventEdtText);
			if (!MetrixStringHelper.isNullOrEmpty(selectedTapEvent)) {
				if (mAllowModifyChkbox != null) {
					mAllowModifyChkbox.setChecked(false);
					mAllowModifyChkbox.setEnabled(false);
				}
				if (mAllowDeleteChkbox != null) {
					mAllowDeleteChkbox.setChecked(false);
					mAllowDeleteChkbox.setEnabled(false);
				}
			} else {
				if (mAllowModifyChkbox != null)
					mAllowModifyChkbox.setEnabled(true);
				if (mAllowDeleteChkbox != null)
					mAllowDeleteChkbox.setEnabled(true);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void populateScreen() {
		//checking whether this screen has a mapping class in code
		String screenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		boolean screenExistsInCodebase = MetrixApplicationAssistant.screenNameHasClassInCode(screenName);
		String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");

		boolean screenEnabledInWorkflow = screenEnabledInWorkflow(screenID);
		boolean screenEnabledInContextMenu = screenEnabledInContextMenu(screenID);

		String metrixRowID = "";
		String help = "";
		String label = "";
		String readOnly = "";
		String tip = "";
		String screenType = "";
		String forceOrder = "";
		String refreshEvent = "";
		String primaryTable = "";
		String whereClauseScript = "";
		String linkedScreenId = "";
		String allowModify = "";
		String allowDelete = "";
		String contextMenuScript = "";
		String workflowScript = "";
		String populateScript = "";
		String actionBarScript = "";
		String tabParentId = "";
		String tabHidden = "";
		String tabTitle = "";
		String searchable = "";
		String tapEvent = "";
		String mapEnabled = "";
		String mapScript = "";

		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_screen.metrix_row_id, mm_screen.help, mm_screen.label, mm_screen.read_only, mm_screen.tip, mm_screen.screen_type, mm_screen.force_order, mm_screen.refresh_event, mm_screen.primary_table,");
		query.append(" mm_screen.where_clause_script, mm_screen.linked_screen_id, mm_screen.allow_modify, mm_screen.allow_delete, mm_screen.context_menu_script, mm_screen.workflow_script, mm_screen.populate_script,");
		query.append(" mm_screen.action_bar_script, mm_screen.tab_parent_id, mm_screen.tab_hidden, mm_screen.tab_title, mm_screen.searchable, mm_screen.tap_event, mm_screen.map_enabled, mm_screen.map_script");
		query.append(" FROM mm_screen WHERE mm_screen.screen_id = " + screenID);

		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				help = cursor.getString(1);
				label = cursor.getString(2);
				readOnly = cursor.getString(3);
				tip = cursor.getString(4);
				screenType = cursor.getString(5);
				forceOrder = cursor.getString(6);
				refreshEvent = cursor.getString(7);
				primaryTable = cursor.getString(8);
				whereClauseScript = cursor.getString(9);
				linkedScreenId = cursor.getString(10);
				allowModify = cursor.getString(11);
				allowDelete = cursor.getString(12);
				contextMenuScript = cursor.getString(13);
				workflowScript = cursor.getString(14);
				populateScript = cursor.getString(15);
				actionBarScript = cursor.getString(16);
				tabParentId = cursor.getString(17);
				tabHidden = cursor.getString(18);
				tabTitle = cursor.getString(19);
				searchable = cursor.getString(20);
				tapEvent = cursor.getString(21);
				mapEnabled = cursor.getString(22);
				mapScript = cursor.getString(23);

				if (MetrixStringHelper.isNullOrEmpty(help)) help = "";
				if (MetrixStringHelper.isNullOrEmpty(label)) label = "";
				if (MetrixStringHelper.isNullOrEmpty(readOnly)) readOnly = "";
				if (MetrixStringHelper.isNullOrEmpty(tabTitle)) tabTitle = "";
				if (MetrixStringHelper.isNullOrEmpty(tip)) tip = "";

				int thisScreenIDNum = Integer.valueOf(screenID);

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_screen.metrix_row_id", metrixRowID);
				origRow.put("mm_screen.help", help);
				origRow.put("mm_screen.label", label);
				origRow.put("mm_screen.read_only", readOnly);
				origRow.put("mm_screen.tip", tip);
				origRow.put("mm_screen.screen_type", screenType);
				origRow.put("mm_screen.force_order", forceOrder);
				origRow.put("mm_screen.refresh_event", refreshEvent);
				origRow.put("mm_screen.primary_table", primaryTable);
				origRow.put("mm_screen.where_clause_script", whereClauseScript);
				origRow.put("mm_screen.linked_screen_id", linkedScreenId);
				origRow.put("mm_screen.allow_modify", allowModify);
				origRow.put("mm_screen.allow_delete", allowDelete);
				origRow.put("mm_screen.context_menu_script", contextMenuScript);
				origRow.put("mm_screen.workflow_script", workflowScript);
				origRow.put("mm_screen.populate_script", populateScript);
				origRow.put("mm_screen.action_bar_script", actionBarScript);
				origRow.put("mm_screen.tab_parent_id", tabParentId);
				origRow.put("mm_screen.tab_hidden", tabHidden);
				origRow.put("mm_screen.tab_title", tabTitle);
				origRow.put("mm_screen.searchable", searchable);
				origRow.put("mm_screen.tap_event", tapEvent);
				origRow.put("mm_screen.map_enabled", mapEnabled);
				origRow.put("mm_screen.map_script", mapScript);
				mOriginalData.put(thisScreenIDNum, origRow);

				break;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		mCurrentScreenType = screenType;
		mIsTabChildScreen = (!MetrixStringHelper.isNullOrEmpty(tabParentId));
		mIsFirstTabChild = false;
		if (mIsTabChildScreen) {
			String tabOrder = MetrixDatabaseManager.getFieldStringValue("mm_screen", "tab_order", String.format("screen_id = %s", screenID));
			mIsFirstTabChild = (MetrixStringHelper.valueIsEqual(tabOrder, "1"));
		}

		// generate an ordered map, so that properties appear in translated alphabetical order
		// iterate through map and dynamically render layouts accordingly
		StringBuilder msgQuery = new StringBuilder();
		msgQuery.append("SELECT c.code_value, v.message_text FROM metrix_code_table c");
		msgQuery.append(" JOIN mm_message_def_view v ON v.message_id = c.message_id AND v.message_type = 'CODE'");
		if (MetrixStringHelper.valueIsEqual(mCurrentScreenType, "TAB_PARENT"))
			msgQuery.append(" WHERE c.code_name = 'MM_TABP_SCREEN_PROP_NAME'");
		else if (mIsTabChildScreen)
			msgQuery.append(" WHERE c.code_name = 'MM_TABC_SCREEN_PROP_NAME'");
		else if (MetrixStringHelper.valueIsEqual(mCurrentScreenType, "ATTACHMENT_API_LIST"))
			msgQuery.append(" WHERE c.code_name = 'MM_ATTACH_SCREEN_PROP_NAME'");
		else if (MetrixStringHelper.valueIsEqual(mCurrentScreenType, "ATTACHMENT_API_CARD"))
			msgQuery.append(" WHERE c.code_name = 'MM_ATTACH_SCREEN_PROP_NAME' AND c.code_value NOT IN ('ALLOW_DELETE','ALLOW_MODIFY','POPULATE_SCRIPT')");
		else
			msgQuery.append(" WHERE c.code_name = 'MM_SCREEN_PROP_NAME' AND c.code_value NOT IN ('JUMP_ORDER','STEP_ORDER','READ_ONLY')");
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

				if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY") && !screenExistsInCodebase && MetrixStringHelper.valueIsEqual(screenType, "STANDARD")) {
					propNameString = String.format("%1$s %2$s", propNameString, AndroidResourceHelper.getMessage("Only"));
				}

				// using property_name, use hard-coded map to determine what layout to inflate and populate with data
				// BUTTON					FILTER_SORT, TAB_ORDER
				// CHECKBOX					ALLOW_MODIFY(only), ALLOW_DELETE, MAP_ENABLED, READ_ONLY, SEARCHABLE, TAB_HIDDEN
				// CLIENT SCRIPT LOOKUP		*_SCRIPT, REFRESH_EVENT
				// COMBOBOX					FORCE_ORDER, LINKED_SCREEN_ID
				// LABEL (translated)		SCREEN_TYPE
				// LABEL (unadjusted)		PRIMARY_TABLE, TAB_PARENT
				// MESSAGE LOOKUP			LABEL, TIP, HELP, TAB_TITLE
				// TEXTBOX					(default, all others)
				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY") || MetrixStringHelper.valueIsEqual(propName, "TAB_HIDDEN")) {
					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					String propValue = MetrixStringHelper.valueIsEqual(propName, "READ_ONLY") ? readOnly : tabHidden;
					if (propValue.compareToIgnoreCase("Y") == 0)
						chkPropValue.setChecked(true);
					else
						chkPropValue.setChecked(false);

					// Disable TAB_HIDDEN if this is the first tab child
					if (mIsFirstTabChild && MetrixStringHelper.valueIsEqual(propName, "TAB_HIDDEN"))
						chkPropValue.setEnabled(false);
				} else if (MetrixStringHelper.valueIsEqual(propName, "SCREEN_TYPE")) {
					// LABEL (with translated text, if possible)
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");

					StringBuilder translationQuery = new StringBuilder();
					translationQuery.append("select mm_message_def_view.message_text from metrix_code_table ");
					translationQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
					translationQuery.append(String.format("where metrix_code_table.code_name = 'MM_SCREEN_TYPE' and metrix_code_table.code_value = '%s'", screenType));
					ArrayList<Hashtable<String, String>> result = MetrixDatabaseManager.getFieldStringValuesList(translationQuery.toString());
					if (result != null && result.size() > 0 && result.get(0).values().size() > 0) {
						String translatedValue = (String) result.get(0).values().toArray()[0];
						tvPropValue.setText(translatedValue);
					} else {
						tvPropValue.setText(screenType);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "PRIMARY_TABLE")) {
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
					tvPropValue.setText(primaryTable);
				} else if (MetrixStringHelper.valueIsEqual(propName, "TAB_PARENT")) {
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
					String tabParentName = (MetrixStringHelper.isNullOrEmpty(tabParentId)) ? "" : MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_name", String.format("screen_id = %s", tabParentId));
					tvPropValue.setText(tabParentName);
				} else if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");
					String workflowId = MetrixDatabaseManager.getFieldStringValue("mm_workflow_screen", "workflow_id","step_order is Not null and screen_id="+screenID);

					if (!MetrixStringHelper.isNullOrEmpty(workflowId)) {
						StringBuilder spinnerQuery = new StringBuilder();
						spinnerQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
						spinnerQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
						spinnerQuery.append(String.format("where metrix_code_table.code_name = 'MM_SCREEN_%s' ", propName));
						spinnerQuery.append("order by mm_message_def_view.message_text asc");
						MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, spinnerQuery.toString(), true);

						try {
							if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER"))
								MetrixControlAssistant.setValue(spnPropValue, forceOrder);
						} catch (Exception e) {
							LogManager.getInstance(this).error(e);
						}
					}
					else {
						layout.setVisibility(View.GONE);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "LINKED_SCREEN_ID")) {
					if(screenExistsInCodebase){ msgCursor.moveToNext(); continue; }

					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");

					StringBuilder linkedScreenQuery = new StringBuilder();
					linkedScreenQuery.append("select distinct mm_screen.screen_name, mm_screen.screen_id");
					linkedScreenQuery.append(" from mm_screen");
					linkedScreenQuery.append(" where mm_screen.screen_name not in ('" + screenName + "') and mm_screen.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
					linkedScreenQuery.append(" and mm_screen.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
					linkedScreenQuery.append(" and mm_screen.tab_parent_id is null");

					//based on the current codeless screen-screen_type we filter out the linked screens.
					//Ex : if the codeless screen type = 'STANDARD', the linked_screen type should be 'LIST'.
					if(MetrixStringHelper.valueIsEqual(screenType, "LIST"))
						linkedScreenQuery.append(" and mm_screen.screen_type = 'STANDARD'");
					else if(MetrixStringHelper.valueIsEqual(screenType, "STANDARD"))
						linkedScreenQuery.append(" and mm_screen.screen_type = 'LIST'");
					else{
						Toast.makeText(this, AndroidResourceHelper.getMessage("YYCSCurrScnTypeNotSupp", screenType), Toast.LENGTH_SHORT).show();
						return;
					}
					linkedScreenQuery.append(" order by mm_screen.screen_name asc");

					// populate all the other screen names except the current screen.
					getOppositeTypeOfLinkedScreens(this, spnPropValue, linkedScreenQuery.toString(), true);
					spnPropValue.setEnabled(mLinkedScreenCtrlHasOptions);

					if (MetrixStringHelper.valueIsEqual(allowModify, "Y") && MetrixStringHelper.valueIsEqual(mCurrentScreenType, "STANDARD"))
						spnPropValue.setEnabled(false);

					try {
						MetrixControlAssistant.setValue(spnPropValue, linkedScreenId);
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}

					mLinkedScreen = spnPropValue;
				} else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY") || MetrixStringHelper.valueIsEqual(propName, "ALLOW_DELETE") || MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE")) {
					String valueString = "";
					if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_DELETE")) {
						// coded screens, non-list screens, or codeless non-workflow list screens should not show allow_delete
						if (screenExistsInCodebase || (!MetrixStringHelper.valueIsEqual(screenType, "LIST") && !MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_LIST"))
							|| (MetrixStringHelper.valueIsEqual(screenType, "LIST") && !screenIsInWorkflowOrIsLinkedScreen(screenID))) {
							msgCursor.moveToNext();
							continue;
						}
						valueString = allowDelete;
					} else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY")) {
						// coded screens or codeless non-workflow list screens should not show allow_modify
						if (screenExistsInCodebase || (MetrixStringHelper.valueIsEqual(screenType, "LIST") && !screenIsInWorkflowOrIsLinkedScreen(screenID))) {
							msgCursor.moveToNext();
							continue;
						}
						valueString = allowModify;
					} else if (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE")) {
						// Show the Searchable property only if screen is a codeless list screen that is not in a workflow, is not a linked screen, and is not a tab child.
						if (screenExistsInCodebase || mIsTabChildScreen || !MetrixStringHelper.valueIsEqual(screenType, "LIST") || screenIsInWorkflowOrIsLinkedScreen(screenID)) {
							msgCursor.moveToNext();
							continue;
						}
						valueString = searchable;
					}

					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					if (!MetrixStringHelper.isNullOrEmpty(valueString)) {
						if (valueString.compareToIgnoreCase("Y") == 0)
							chkPropValue.setChecked(true);
						else
							chkPropValue.setChecked(false);
					}

					if(!MetrixStringHelper.isNullOrEmpty(tapEvent) && screenType.contains("LIST")
						&& (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY") || MetrixStringHelper.valueIsEqual(propName, "ALLOW_DELETE"))){
						chkPropValue.setChecked(false);
						chkPropValue.setEnabled(false);
					}

					if(MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY"))
						mAllowModifyChkbox = chkPropValue;
					if(MetrixStringHelper.valueIsEqual(propName, "ALLOW_DELETE"))
						mAllowDeleteChkbox = chkPropValue;

					if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY") && MetrixStringHelper.valueIsEqual(mCurrentScreenType, "STANDARD")) {
						chkPropValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								if (mLinkedScreen != null) {
									if (isChecked) {
										mLinkedScreen.setEnabled(false);
									} else {
										if (mLinkedScreenCtrlHasOptions)
											mLinkedScreen.setEnabled(true);
									}
								}
							}
						});
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "FILTER_SORT")) {
					// CRUD BUTTON - for JobList and ReceivingList only
					if (!MetrixStringHelper.valueIsEqual(screenName, "JobList") && !MetrixStringHelper.valueIsEqual(screenName, "ReceivingList")) {
						msgCursor.moveToNext();
						continue;
					}

					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_crud_button_line"), mTable);
					layout.setTag("FILTER_SORT_CRUD_BUTTON");
					Button btnAdd = (Button) layout.findViewWithTag("add_button");
					Button btnModify = (Button) layout.findViewWithTag("modify_button");
					Button btnDelete = (Button) layout.findViewWithTag("delete_button");

					AndroidResourceHelper.setResourceValues(btnAdd, "Add");
					AndroidResourceHelper.setResourceValues(btnModify, "Modify");
					AndroidResourceHelper.setResourceValues(btnDelete, "Delete");

					btnAdd.setVisibility(View.GONE);
					btnModify.setVisibility(View.VISIBLE);
					btnDelete.setVisibility(View.GONE);

					btnModify.setOnClickListener(new View.OnClickListener() {
						public void onClick (View v) {
							Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerScreenPropActivity.this, MetrixDesignerFilterSortEnablingActivity.class);
							intent.putExtra("headingText", mHeadingText);
							MetrixActivityHelper.startNewActivity(MetrixDesignerScreenPropActivity.this, intent);
						}
					});
				} else if (MetrixStringHelper.valueIsEqual(propName, "TAB_ORDER")) {
					// CRUD BUTTON
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_crud_button_line"), mTable);
					layout.setTag("TAB_ORDER_CRUD_BUTTON");
					Button btnAdd = (Button) layout.findViewWithTag("add_button");
					Button btnModify = (Button) layout.findViewWithTag("modify_button");
					Button btnDelete = (Button) layout.findViewWithTag("delete_button");

					AndroidResourceHelper.setResourceValues(btnAdd, "Add");
					AndroidResourceHelper.setResourceValues(btnModify, "Modify");
					AndroidResourceHelper.setResourceValues(btnDelete, "Delete");

					btnAdd.setVisibility(View.GONE);
					btnModify.setVisibility(View.VISIBLE);
					btnDelete.setVisibility(View.GONE);

					int tabChildCount = MetrixDatabaseManager.getCount("mm_screen", String.format("tab_parent_id = %s", screenID));
					btnModify.setEnabled((tabChildCount > 0));

					btnModify.setOnClickListener(new View.OnClickListener() {
						public void onClick (View v) {
							Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerScreenPropActivity.this, MetrixDesignerScreenTabOrderActivity.class);
							intent.putExtra("headingText", mHeadingText);
							MetrixActivityHelper.startNewActivity(MetrixDesignerScreenPropActivity.this, intent);
						}
					});
				} else if (MetrixStringHelper.valueIsEqual(propName, "REFRESH_EVENT")) {
					// CLIENT SCRIPT LOOKUP
					layout = addScriptLookupField(refreshEvent);
				} else if (MetrixStringHelper.valueIsEqual(propName, "ACTION_BAR_SCRIPT")) {
					// CLIENT SCRIPT LOOKUP - only if TAB PARENT or custom codeless non-workflow screen
					if (MetrixStringHelper.valueIsEqual(mCurrentScreenType, "TAB_PARENT")
							|| (!screenExistsInCodebase && !screenIsInWorkflowOrIsLinkedScreen(screenID) && !screenNameIsBaselineWorkflowScreenWithoutMetadata(screenName))) {
						layout = addScriptLookupField(actionBarScript);
					} else { msgCursor.moveToNext(); continue; }
				} else if (MetrixStringHelper.valueIsEqual(propName, "WHERE_CLAUSE_SCRIPT")) {
					// Display this property - only if this screen is a Start From Scratch one(= No mapping class in code level)
					if(screenExistsInCodebase){ msgCursor.moveToNext(); continue; }

					// WHERE_CLAUSE_SCRIPT LOOKUP
					String propValue = whereClauseScript;
					layout = addScriptLookupField(propValue);
				} else if (MetrixStringHelper.valueIsEqual(propName, "CONTEXT_MENU_SCRIPT")) {
					// Display this property - only if this screen is in a context menu
					if(!screenEnabledInContextMenu){ msgCursor.moveToNext(); continue; }

					// CONTEXT_MENU_SCRIPT LOOKUP
					String propValue = contextMenuScript;
					layout = addScriptLookupField(propValue);
				} else if (MetrixStringHelper.valueIsEqual(propName, "WORKFLOW_SCRIPT")) {
					// Display this property - only if this screen is in a workflow
					if(!screenEnabledInWorkflow){ msgCursor.moveToNext(); continue; }

					// WORKFLOW_SCRIPT LOOKUP
					String propValue = workflowScript;
					layout = addScriptLookupField(propValue);
				} else if (MetrixStringHelper.valueIsEqual(propName, "POPULATE_SCRIPT")) {
					// Display this property only if this is some kind of list screen
					if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")){ msgCursor.moveToNext(); continue; }

					// POPULATE_SCRIPT LOOKUP
					String propValue = populateScript;
					layout = addScriptLookupField(propValue);
				} else if (MetrixStringHelper.valueIsEqual(propName, "MAP_SCRIPT")) {
					// Display this property only if this is a Standard screen and contain at least one field
					if (!MetrixStringHelper.valueIsEqual(screenType, "STANDARD") || !thisScreenHasFields()){ msgCursor.moveToNext(); continue; }

					// MAP_SCRIPT LOOKUP
					String propValue = mapScript;
					layout = addScriptLookupField(propValue);
				} else if (MetrixStringHelper.valueIsEqual(propName, "MAP_ENABLED")) {
					// Display this property only if this is a Standard screen and contain at least one field
					if (!MetrixStringHelper.valueIsEqual(screenType, "STANDARD") || !thisScreenHasFields()){ msgCursor.moveToNext(); continue; }

					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					String propValue = mapEnabled;
                    chkPropValue.setChecked(MetrixStringHelper.valueIsEqual(propValue, "Y"));
				} else if (MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT")) {
					// Display this property only if this is some kind of list screen
					if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")){ msgCursor.moveToNext(); continue; }
					// TAP_EVENT LOOKUP
					String propValue = tapEvent;
					layout = addScriptLookupField(propValue);
					mTapEventEdtText = ((EditText)layout.findViewWithTag("property_value"));
					mTapEventEdtText.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) { }
						@Override
						public void afterTextChanged(Editable s) { checkTapEventValue(); }
					});

				} else if (MetrixStringHelper.valueIsEqual(propName, "LABEL") || MetrixStringHelper.valueIsEqual(propName, "TIP")
						|| MetrixStringHelper.valueIsEqual(propName, "HELP") || MetrixStringHelper.valueIsEqual(propName, "TAB_TITLE")) {
					// MESSAGE LOOKUP
					String propValue = "";
					if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
						propValue = label;
					else if (MetrixStringHelper.valueIsEqual(propName, "TIP"))
						propValue = tip;
					else if (MetrixStringHelper.valueIsEqual(propName, "HELP"))
						propValue = help;
					else if (MetrixStringHelper.valueIsEqual(propName, "TAB_TITLE"))
						propValue = tabTitle;

					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
					String messageType = "MM_SCREEN_" + propName;
					if (MetrixStringHelper.valueIsEqual(propName, "TAB_TITLE"))
						messageType = "MM_TAB_TITLE";
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
					TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
					etPropValue.setId(MetrixControlAssistant.generateViewId());
					etPropValue.setText(propValue);
					etPropValue.setOnFocusChangeListener(this);
					etPropValue.addTextChangedListener(new MessageTextWatcher(messageType, tvDescLabel, tvDescText));

					tvDescLabel.setVisibility(View.GONE);
					tvDescText.setVisibility(View.GONE);
					if (!MetrixStringHelper.isNullOrEmpty(propValue)) {
						String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", messageType, propValue));
						if (!MetrixStringHelper.isNullOrEmpty(description)) {
							tvDescText.setText(description);
							tvDescLabel.setVisibility(View.INVISIBLE);
							tvDescText.setVisibility(View.VISIBLE);
						}
					}
				} else {
					// TEXTBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_line"), mTable);
				}

				displayCommonInformation(screenID, metrixRowID, propName, propNameString, layout);

				msgCursor.moveToNext();
			}
		} finally {
			if (msgCursor != null) {
				msgCursor.close();
			}
		}
	}

	private boolean screenEnabledInWorkflow(String screenID) {
		int count = MetrixDatabaseManager.getCount("mm_workflow_screen", String.format("screen_id = %s and step_order > 0", screenID));
		return count > 0;
	}

	private boolean screenEnabledInContextMenu(String screenID) {
		int count = MetrixDatabaseManager.getCount("mm_workflow_screen", String.format("screen_id = %s and jump_order > 0", screenID));
		return count > 0;
	}

	private boolean screenIsInWorkflowOrIsLinkedScreen(String screenID) {
		// determine whether this screen is in mm_workflow_screen at all or is a linked screen for any workflow screen
		int directCount = MetrixDatabaseManager.getCount("mm_workflow_screen", String.format("screen_id = %s", screenID));
		int indirectCount = MetrixDatabaseManager.getCount("mm_screen", String.format("linked_screen_id = %s and screen_id in (select screen_id from mm_workflow_screen)", screenID));
		return (directCount > 0 || indirectCount > 0);
	}

	private boolean screenNameIsBaselineWorkflowScreenWithoutMetadata(String screenName) {
		List<String> screenList = Arrays.asList("DebriefPartDisposition", "DebriefPartUsage", "DebriefPartUsageList", "DebriefPaymentContact",
				"DebriefPaymentList", "DebriefRequest", "DebriefRequestUnit", "DebriefRequestUnitList", "DebriefTaskAttachmentAdd", "DebriefTaskStep",
				"DebriefTaskText", "DebriefTaskTextList", "DebriefTaskUnitSteps", "QuoteList", "QuoteQuoteAttachmentAdd", "FSMAttachmentList");
		return screenList.contains(screenName);
	}

	// all layouts have these, so set generically
	private void displayCommonInformation(String screenID, String metrixRowID,
										  String propName, String propNameString, LinearLayout layout) {
		TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
		TextView tvScreenID = (TextView) layout.findViewWithTag("pv_id");
		TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
		TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

		tvMetrixRowID.setText(metrixRowID);
		tvScreenID.setText(screenID);
		tvPropName.setText(propName);
		tvPropNameString.setText(propNameString);
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

				if (MetrixStringHelper.valueIsEqual(propName, "REFRESH_EVENT") || MetrixStringHelper.valueIsEqual(propName, "WHERE_CLAUSE_SCRIPT")
						|| MetrixStringHelper.valueIsEqual(propName, "CONTEXT_MENU_SCRIPT") || MetrixStringHelper.valueIsEqual(propName, "WORKFLOW_SCRIPT")
						|| MetrixStringHelper.valueIsEqual(propName, "POPULATE_SCRIPT") || MetrixStringHelper.valueIsEqual(propName, "ACTION_BAR_SCRIPT")
						|| MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT") || MetrixStringHelper.valueIsEqual(propName, "MAP_SCRIPT"))
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
		if (viewId == mScreenPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mScreenPropResourceData.getExtraResourceID("R.id.view_items")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenItemActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mScreenPropResourceData.getExtraResourceID("R.id.view_fields")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private boolean thisScreenHasFields() {
		int fieldCount = MetrixDatabaseManager.getCount("mm_field", "screen_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
		return (fieldCount > 0);
	}

	private void doMessageSelection(String propName, int viewToPopulateId, LinearLayout parentLayout) {
		MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
		lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
		lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
		if (MetrixStringHelper.valueIsEqual(propName, "TAB_TITLE"))
			lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_TAB_TITLE"));
		else
			lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_SCREEN_" + propName));

		Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
		intent.putExtra("NoOptionsMenu", true);
		MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
		MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
		startActivityForResult(intent, 2727);
	}

	private boolean processAndSaveChanges() {
		try {
			String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");

			boolean forceOrderChanged = false;
			String linkedScreenName = null;
			boolean allowModify = false;
			boolean mapIsEnabled = false;
			String mapScriptValue = "";

			for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

				String controlType = tvControlType.getText().toString();
				String propName = tvPropName.getText().toString();
				String currentPropValue = "";
				String origPropValue = "";

				if (MetrixStringHelper.valueIsEqual(controlType, "COMBOBOX")) {
					Spinner spnPropValue = (Spinner) currLayout.findViewWithTag("property_value");
					if (spnPropValue != null){
						if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER")){
							if (spnPropValue.isShown())
								currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
						} else if (MetrixStringHelper.valueIsEqual(propName, "LINKED_SCREEN_ID")) {
							if (spnPropValue.isShown()) {
								SpinnerKeyValuePair pair = (SpinnerKeyValuePair) spnPropValue.getSelectedItem();
								if (pair != null)
									linkedScreenName = pair.spinnerKey;
							}
						}
					}
				} else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
					CheckBox chkBoxPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
					if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY")) {
						if (chkBoxPropValue != null)
							allowModify = chkBoxPropValue.isChecked();
					} else if (MetrixStringHelper.valueIsEqual(propName, "MAP_ENABLED")) {
						if (chkBoxPropValue != null)
							mapIsEnabled = chkBoxPropValue.isChecked();
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "MAP_SCRIPT")) {
					EditText txtPropValue = (EditText) currLayout.findViewWithTag("property_value");
					if (txtPropValue != null)
						mapScriptValue = txtPropValue.getText().toString();
				}

				int currScreenIDNum = Integer.valueOf(screenID);
				HashMap<String, String> origRow = mOriginalData.get(currScreenIDNum);

				if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER"))
					origPropValue = origRow.get("mm_screen.force_order");

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER")) {
						forceOrderChanged = true;
						break;
					}
				}
			}

			// warn user if FORCE_ORDER property changed before save
			if (forceOrderChanged) {
				mSaveScreenDialog = new AlertDialog.Builder(this).create();
				mSaveScreenDialog.setMessage(AndroidResourceHelper.getMessage("SaveForceOrderConfirm"));
				mSaveScreenDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), saveScreenListener);
				mSaveScreenDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), saveScreenListener);
				mSaveScreenDialog.show();
				return false;
				// warn user if ALLOW_MODIFY property checked, and LINKED_SCREEN is empty
			} else if (allowModify && MetrixStringHelper.isNullOrEmpty(linkedScreenName)
					&& !MetrixStringHelper.valueIsEqual(mCurrentScreenType, "STANDARD")&& !MetrixStringHelper.valueIsEqual(mCurrentScreenType, "ATTACHMENT_API_LIST")) {
				Toast.makeText(this, AndroidResourceHelper.getMessage("YYCSSelLinkScnBeforeSaving"), Toast.LENGTH_LONG).show();
				return false;
			} else if (allowModify && !MetrixStringHelper.isNullOrEmpty(linkedScreenName) && MetrixStringHelper.valueIsEqual(mCurrentScreenType, "STANDARD")) {
				if (mLinkedScreen != null) {
					mLinkedScreen.setSelection(0);    // blank item
				}
			} else if (MetrixStringHelper.valueIsEqual(mCurrentScreenType, "STANDARD") && mapIsEnabled && MetrixStringHelper.isNullOrEmpty(mapScriptValue)) {
				Toast.makeText(this, AndroidResourceHelper.getMessage("ScreenPropMapValuesError"), Toast.LENGTH_LONG).show();
				return false;
			}

			saveScreenData();
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	DialogInterface.OnClickListener saveScreenListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							saveScreenData();

							if (mSaveScreenDialog != null) {
								mSaveScreenDialog.dismiss();
							}

							Intent intent = MetrixActivityHelper.createActivityIntent((Activity) mContext, MetrixDesignerScreenPropActivity.class);
							intent.putExtra("headingText", mHeadingText);
							MetrixActivityHelper.startNewActivityAndFinish((Activity) mContext, intent);
						}
					});

					thread.start();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void saveScreenData() {
		try {
			int screenPropChangeCount = 0;

			ArrayList<MetrixSqlData> screenToUpdate = new ArrayList<MetrixSqlData>();

			String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_screen", "metrix_row_id", "screen_id = " + screenID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_screen", "created_revision_id", "screen_id = " + screenID);
			MetrixSqlData data = new MetrixSqlData("mm_screen", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("screen_id", screenID));

			boolean forceOrderChanged = false;
			String forceOrderValue = "";

			for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

				String controlType = tvControlType.getText().toString();
				String propName = tvPropName.getText().toString();
				String currentPropValue = "";

				if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX") || MetrixStringHelper.valueIsEqual(controlType, "LOOKUP")) {
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
					if(spnPropValue.isShown())
						currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
				}
				else if (MetrixStringHelper.valueIsEqual(controlType, "LABEL") || MetrixStringHelper.valueIsEqual(controlType, "CRUD_BUTTON")) {
					// don't detect changes, and move onto next control
					continue;
				} else {
					throw new Exception("MDScreenProp: Unhandled control type.");
				}

				int currScreenIDNum = Integer.valueOf(screenID);
				HashMap<String, String> origRow = mOriginalData.get(currScreenIDNum);

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "HELP"))
					origPropValue = origRow.get("mm_screen.help");
				else if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
					origPropValue = origRow.get("mm_screen.label");
				else if (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY"))
					origPropValue = origRow.get("mm_screen.read_only");
				else if (MetrixStringHelper.valueIsEqual(propName, "TIP"))
					origPropValue = origRow.get("mm_screen.tip");
				else if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER"))
					origPropValue = origRow.get("mm_screen.force_order");
				else if (MetrixStringHelper.valueIsEqual(propName, "REFRESH_EVENT"))
					origPropValue = origRow.get("mm_screen.refresh_event");
				else if (MetrixStringHelper.valueIsEqual(propName, "WHERE_CLAUSE_SCRIPT"))
					origPropValue = origRow.get("mm_screen.where_clause_script");
				else if (MetrixStringHelper.valueIsEqual(propName, "CONTEXT_MENU_SCRIPT"))
					origPropValue = origRow.get("mm_screen.context_menu_script");
				else if (MetrixStringHelper.valueIsEqual(propName, "WORKFLOW_SCRIPT"))
					origPropValue = origRow.get("mm_screen.workflow_script");
				else if (MetrixStringHelper.valueIsEqual(propName, "POPULATE_SCRIPT"))
					origPropValue = origRow.get("mm_screen.populate_script");
				else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY"))
					origPropValue = origRow.get("mm_screen.allow_modify");
				else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_DELETE"))
					origPropValue = origRow.get("mm_screen.allow_delete");
				else if (MetrixStringHelper.valueIsEqual(propName, "LINKED_SCREEN_ID"))
					origPropValue = origRow.get("mm_screen.linked_screen_id");
				else if (MetrixStringHelper.valueIsEqual(propName, "ACTION_BAR_SCRIPT"))
					origPropValue = origRow.get("mm_screen.action_bar_script");
				else if (MetrixStringHelper.valueIsEqual(propName, "TAB_HIDDEN"))
					origPropValue = origRow.get("mm_screen.tab_hidden");
				else if (MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT"))
					origPropValue = origRow.get("mm_screen.tap_event");
				else if (MetrixStringHelper.valueIsEqual(propName, "TAB_TITLE"))
					origPropValue = origRow.get("mm_screen.tab_title");
				else if (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE"))
					origPropValue = origRow.get("mm_screen.searchable");
				else if (MetrixStringHelper.valueIsEqual(propName, "MAP_ENABLED"))
					origPropValue = origRow.get("mm_screen.map_enabled");
				else if (MetrixStringHelper.valueIsEqual(propName, "MAP_SCRIPT"))
					origPropValue = origRow.get("mm_screen.map_script");

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "HELP"))
						data.dataFields.add(new DataField("help", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
						data.dataFields.add(new DataField("label", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY"))
						data.dataFields.add(new DataField("read_only", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "TIP"))
						data.dataFields.add(new DataField("tip", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "FORCE_ORDER")) {
						data.dataFields.add(new DataField("force_order", currentPropValue));
						forceOrderChanged = true;
						forceOrderValue = currentPropValue;
					} else if (MetrixStringHelper.valueIsEqual(propName, "REFRESH_EVENT"))
						data.dataFields.add(new DataField("refresh_event", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "WHERE_CLAUSE_SCRIPT"))
						data.dataFields.add(new DataField("where_clause_script", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "CONTEXT_MENU_SCRIPT"))
						data.dataFields.add(new DataField("context_menu_script", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "WORKFLOW_SCRIPT"))
						data.dataFields.add(new DataField("workflow_script", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "POPULATE_SCRIPT"))
						data.dataFields.add(new DataField("populate_script", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_MODIFY"))
						data.dataFields.add(new DataField("allow_modify", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_DELETE"))
						data.dataFields.add(new DataField("allow_delete", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "LINKED_SCREEN_ID"))
						data.dataFields.add(new DataField("linked_screen_id", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "ACTION_BAR_SCRIPT"))
						data.dataFields.add(new DataField("action_bar_script", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "TAP_EVENT"))
						data.dataFields.add(new DataField("tap_event", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "TAB_HIDDEN")) {
						data.dataFields.add(new DataField("tab_hidden", currentPropValue));
						DataField tabOrderData = changeTabOrderBasedOnNewTabHidden(currentPropValue);
						data.dataFields.add(tabOrderData);
					} else if (MetrixStringHelper.valueIsEqual(propName, "TAB_TITLE"))
						data.dataFields.add(new DataField("tab_title", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE"))
						data.dataFields.add(new DataField("searchable", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "MAP_ENABLED"))
						data.dataFields.add(new DataField("map_enabled", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "MAP_SCRIPT"))
						data.dataFields.add(new DataField("map_script", currentPropValue));

					screenPropChangeCount++;
				}
			}

			if (screenPropChangeCount > 0) {
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

				screenToUpdate.add(data);

				// reset other screens with FORCE_ORDER property and get screen_id for other screen with the same FORCE_ORDER value
				if (forceOrderChanged) {
					ArrayList<Hashtable<String, String>> samePropertyScreenIds = MetrixDatabaseManager.getFieldStringValuesList(String.format("select s.screen_id from mm_screen s join mm_workflow_screen ws on s.screen_id = ws.screen_id where s.force_order = '%1$s' and s.screen_id <> %2$s and ws.workflow_id in (select workflow_id from mm_workflow_screen where screen_id = %2$s)", forceOrderValue, screenID));

					if (samePropertyScreenIds != null && samePropertyScreenIds.size() > 0) {
						for (Hashtable<String, String> row : samePropertyScreenIds) {
							String sScreenId = row.get("screen_id");
							metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_screen", "metrix_row_id", "screen_id = " + sScreenId);
							MetrixSqlData sData = new MetrixSqlData("mm_screen", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
							sData.dataFields.add(new DataField("metrix_row_id", metrixRowID));
							sData.dataFields.add(new DataField("screen_id", sScreenId));
							sData.dataFields.add(new DataField("force_order", ""));

							screenToUpdate.add(sData);
						}
					}
				}

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(screenToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("ScreenChange"), this);

			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
		}
	}

	//We pick the codeless screens against the current codeless screen type.
	public static void getOppositeTypeOfLinkedScreens(Activity activity, Spinner spinner, String query, boolean displayNull) {
		if (spinner == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheSpinnerParamIsReq"));
		}

		MetrixCursor cursor = null;
		List<SpinnerKeyValuePair> items = new ArrayList<SpinnerKeyValuePair>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(activity, R.layout.spinner_item,
						new SpinnerKeyValuePair[0]);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinner.setAdapter(adapter);
				return;
			}

			if (displayNull)
				items.add(new SpinnerKeyValuePair("", ""));

			while (cursor.isAfterLast() == false) {
				//if the screen is not a codeless screen we just omit it.
				String codelessOppositeTypeScreenName = cursor.getString(0);
				if (!MetrixApplicationAssistant.screenNameHasClassInCode(codelessOppositeTypeScreenName)) {
					if (cursor.getColumnCount() == 1) {
						items.add(new SpinnerKeyValuePair(cursor.getString(0), cursor.getString(0)));
					} else {
						items.add(new SpinnerKeyValuePair(cursor.getString(0), cursor.getString(1)));
					}
				}
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		mLinkedScreenCtrlHasOptions = (displayNull ? items.size() > 1 : items.size() > 0);

		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(activity, R.layout.spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	private DataField changeTabOrderBasedOnNewTabHidden(String tabHiddenValue) {
		// if value is N ... set TAB_ORDER to MAX(Order) + 1 (if this is the first visible tab child, TAB_ORDER is 1)
		// if value is Y ... set TAB_ORDER to -1
		String newOrder = "-1";
		if (MetrixStringHelper.valueIsEqual(tabHiddenValue, "N")) {
			String tabParentID = MetrixDatabaseManager.getFieldStringValue("mm_screen", "tab_parent_id", String.format("screen_id = %s", MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id")));
			String maxOrder = MetrixDatabaseManager.getFieldStringValue(true, "mm_screen", "tab_order", String.format("tab_parent_id = %s", tabParentID), null, null, null, "tab_order DESC", null);
			int currentMax = Integer.valueOf(maxOrder);
			if (currentMax < 0) currentMax = 0;
			newOrder = String.valueOf(currentMax + 1);
		}

		return new DataField("tab_order", newOrder);
	}

	private LinearLayout addScriptLookupField(String propValue){
		LinearLayout layout = MetrixControlAssistant.addLinearLayout(this, mScreenPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
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
			String description;
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
		return layout;
	}
}

