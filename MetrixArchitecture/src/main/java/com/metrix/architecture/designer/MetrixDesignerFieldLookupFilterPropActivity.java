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
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerFieldLookupFilterPropActivity extends MetrixDesignerActivity {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mFieldName;
	private MetrixDesignerResourceData mLookupFilterPropResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupFilterPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupFilterPropActivityResourceData");

		setContentView(mLookupFilterPropResourceData.LayoutResourceID);

		mTable = (LinearLayout) findViewById(mLookupFilterPropResourceData.getExtraResourceID("R.id.table_layout"));
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupFilterPropResourceData.HelpTextString;

		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupFilterPropResourceData.getExtraResourceID("R.id.zzmd_field_lookup_filter_prop_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupFltrProp", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSave = (Button) findViewById(mLookupFilterPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mLookupFilterPropResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mFilterProp = (TextView) findViewById(mLookupFilterPropResourceData.getExtraResourceID("R.id.lookup_filter_properties"));

		AndroidResourceHelper.setResourceValues(mFilterProp, "LookupFilterProperties");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");
	}

	private void populateScreen() {
		String lkupFilterID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_filter", "lkup_filter_id");

		String metrixRowID = "";
		String tableName = "";
		String columnName = "";
		String operator = "";
		String rightOperand = "";
		String logicalOperator = "";
		String leftParens = "";
		String rightParens = "";
		String noQuotes = "";

		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_filter.metrix_row_id, mm_field_lkup_filter.table_name, mm_field_lkup_filter.column_name,");
		query.append(" mm_field_lkup_filter.operator, mm_field_lkup_filter.right_operand, mm_field_lkup_filter.logical_operator,");
		query.append(" mm_field_lkup_filter.left_parens, mm_field_lkup_filter.right_parens, mm_field_lkup_filter.no_quotes");
		query.append(" FROM mm_field_lkup_filter WHERE mm_field_lkup_filter.lkup_filter_id = " + lkupFilterID);

		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				tableName = cursor.getString(1);
				columnName = cursor.getString(2);
				operator = cursor.getString(3);
				rightOperand = cursor.getString(4);
				logicalOperator = cursor.getString(5);
				leftParens = cursor.getString(6);
				rightParens = cursor.getString(7);
				noQuotes = cursor.getString(8);

				if (MetrixStringHelper.isNullOrEmpty(logicalOperator)) logicalOperator = "";

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_field_lkup_filter.metrix_row_id", metrixRowID);
				origRow.put("mm_field_lkup_filter.table_name", tableName);
				origRow.put("mm_field_lkup_filter.column_name", columnName);
				origRow.put("mm_field_lkup_filter.operator", operator);
				origRow.put("mm_field_lkup_filter.right_operand", rightOperand);
				origRow.put("mm_field_lkup_filter.logical_operator", logicalOperator);
				origRow.put("mm_field_lkup_filter.left_parens", leftParens);
				origRow.put("mm_field_lkup_filter.right_parens", rightParens);
				origRow.put("mm_field_lkup_filter.no_quotes", noQuotes);
				mOriginalData.put(Integer.valueOf(lkupFilterID), origRow);

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
		msgQuery.append(" WHERE c.code_name = 'MM_FIELD_LKUP_FILTER_PROP'");
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
				// COMBOBOX			LOGICAL_OPERATOR, OPERATOR
				// LABEL			TABLE_NAME, COLUMN_NAME
				// CHECKBOX			NO_QUOTES
				// NUMBER TEXT		LEFT_PARENS, RIGHT_PARENS
				// TEXTBOX/LOOKUP	RIGHT_OPERAND
				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "OPERATOR") || MetrixStringHelper.valueIsEqual(propName, "LOGICAL_OPERATOR")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupFilterPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");

					StringBuilder spinnerQuery = new StringBuilder();
					spinnerQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
					spinnerQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
					spinnerQuery.append(String.format("where metrix_code_table.code_name = 'MM_FIELD_LKUP_FILTER_%s' ", propName));
					spinnerQuery.append("order by mm_message_def_view.message_text asc");
					MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, spinnerQuery.toString(), true);

					try {
						if (MetrixStringHelper.valueIsEqual(propName, "OPERATOR")) {
							MetrixControlAssistant.setValue(spnPropValue, operator);

							layout.setTag("OPERATOR");
							spnPropValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
								public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
									setRightOperandVisibilityBasedOnSelectedOperator();
								}

								public void onNothingSelected(AdapterView<?> parent) {
									setRightOperandVisibilityBasedOnSelectedOperator();
								}
							});
						} else if (MetrixStringHelper.valueIsEqual(propName, "LOGICAL_OPERATOR"))
							MetrixControlAssistant.setValue(spnPropValue, logicalOperator);
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "TABLE_NAME") || MetrixStringHelper.valueIsEqual(propName, "COLUMN_NAME")) {
					// LABEL
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupFilterPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");

					if (MetrixStringHelper.valueIsEqual(propName, "TABLE_NAME"))
						tvPropValue.setText(tableName);
					else if (MetrixStringHelper.valueIsEqual(propName, "COLUMN_NAME"))
						tvPropValue.setText(columnName);
				} else if (MetrixStringHelper.valueIsEqual(propName, "NO_QUOTES")) {
					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupFilterPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					layout.setTag("NO_QUOTES");
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					if (!MetrixStringHelper.isNullOrEmpty(noQuotes)) {
						if (noQuotes.compareToIgnoreCase("Y") == 0)
							chkPropValue.setChecked(true);
						else
							chkPropValue.setChecked(false);
					}

					if (!MetrixStringHelper.isNullOrEmpty(operator) && (MetrixStringHelper.valueIsEqual(operator, "IS_NULL") || MetrixStringHelper.valueIsEqual(operator, "IS_NOT_NULL")))
						layout.setVisibility(View.GONE);
					else if (MetrixStringHelper.valueIsEqual(operator, "LIKE") || MetrixStringHelper.valueIsEqual(operator, "NOT_LIKE")) {
						chkPropValue.setChecked(false);
						chkPropValue.setEnabled(false);
					} else if (MetrixStringHelper.valueIsEqual(operator, "IN") || MetrixStringHelper.valueIsEqual(operator, "NOT_IN")) {
						chkPropValue.setChecked(true);
						chkPropValue.setEnabled(false);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "LEFT_PARENS") || MetrixStringHelper.valueIsEqual(propName, "RIGHT_PARENS")) {
					// NUMBER TEXT
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupFilterPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_line"), mTable);
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					etPropValue.setInputType(InputType.TYPE_CLASS_NUMBER);
					if (MetrixStringHelper.valueIsEqual(propName, "LEFT_PARENS")) {
						etPropValue.setText(leftParens);
						layout.setTag("LEFT_PARENS");
					} else if (MetrixStringHelper.valueIsEqual(propName, "RIGHT_PARENS")) {
						etPropValue.setText(rightParens);
						layout.setTag("RIGHT_PARENS");
					}
				} else {
					// TEXTBOX/LOOKUP (can type in a value OR do a client script lookup)
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupFilterPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_imageview_line"), mTable);
					layout.setTag("RIGHT_OPERAND");
					ImageView imgLookupButton = (ImageView)layout.findViewWithTag("lookup_button");

					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					etPropValue.setId(MetrixControlAssistant.generateViewId());
					etPropValue.setText(rightOperand);
					TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
					TextView tvDescText = (TextView) layout.findViewWithTag("description_text");

					TextView tvPropDataType = (TextView) layout.findViewWithTag("default_value_data_type");
					tvPropDataType.setId(MetrixControlAssistant.generateViewId());

					//Search button click event
					imgLookupButton.setOnClickListener(this);
					//Text change event
					etPropValue.addTextChangedListener(new ClientScriptOrLiteralTextWatcher(tvDescLabel, tvDescText));

					tvDescLabel.setVisibility(View.GONE);
					tvDescText.setVisibility(View.GONE);
					if (!MetrixStringHelper.isNullOrEmpty(rightOperand)) {
						String description = "";
						String scriptName = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "name", String.format("unique_vs = '%s'", rightOperand));
						String scriptVersion = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "version_number", String.format("unique_vs = '%s'", rightOperand));
						if (!MetrixStringHelper.isNullOrEmpty(scriptName) && !MetrixStringHelper.isNullOrEmpty(scriptVersion)) {
							if (MetrixStringHelper.valueIsEqual(scriptVersion, "0"))
								description = String.format("%1$s (%2$s)", scriptName, AndroidResourceHelper.getMessage("Baseline"));
							else
								description = String.format("%1$s (%2$s %3$s)", scriptName, AndroidResourceHelper.getMessage("Version"), scriptVersion);
						} else
							description = AndroidResourceHelper.getMessage("LiteralValue");

						tvDescText.setText(description);
						tvDescLabel.setVisibility(View.INVISIBLE);
						tvDescText.setVisibility(View.VISIBLE);
					}

					if (!MetrixStringHelper.isNullOrEmpty(operator) && (MetrixStringHelper.valueIsEqual(operator, "IS_NULL") || MetrixStringHelper.valueIsEqual(operator, "IS_NOT_NULL")))
						layout.setVisibility(View.GONE);
				}

				// all layouts have these, so set generically
				if (layout != null) {
					TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
					TextView tvLkupFilterID = (TextView) layout.findViewWithTag("pv_id");
					TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
					TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

					tvMetrixRowID.setText(metrixRowID);
					tvLkupFilterID.setText(lkupFilterID);
					tvPropName.setText(propName);
					tvPropNameString.setText(propNameString);
				}

				msgCursor.moveToNext();
			}
		} finally {
			if (msgCursor != null) {
				msgCursor.close();
			}
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupFilterPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupFilterPropResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else {
			Object viewTag = v.getTag();
			if (viewTag != null) {
				String tag = String.valueOf(viewTag);
				if (MetrixStringHelper.valueIsEqual(tag, "lookup_button")) {
					LinearLayout rowLayout = (LinearLayout) v.getParent();
					EditText etPropValue = (EditText) rowLayout.findViewWithTag("property_value");
					doClientScriptSelection(etPropValue.getId(), rowLayout);
				}
			}
		}
	}

	private boolean processAndSaveChanges() {
		try {
			int lookupFilterPropChangeCount = 0;
			ArrayList<MetrixSqlData> lookupFilterToUpdate = new ArrayList<MetrixSqlData>();

			String lkupFilterID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_filter", "lkup_filter_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_filter", "metrix_row_id", "lkup_filter_id = " + lkupFilterID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_filter", "created_revision_id", "lkup_filter_id = " + lkupFilterID);
			MetrixSqlData data = new MetrixSqlData("mm_field_lkup_filter", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("lkup_filter_id", lkupFilterID));

			// first validate that operator is not empty, then check if we are changing to IS NULL or IS NOT NULL
			// if we are, then force-blank right_operand
			// if we are not, then validate that right_operand is not empty
			// (halt save process if any of the above fails validation)
			boolean validationFailed = false;
			LinearLayout operatorLayout = (LinearLayout) mTable.findViewWithTag("OPERATOR");
			Spinner spnOperator = (Spinner) operatorLayout.findViewWithTag("property_value");
			String currentOperator = MetrixControlAssistant.getValue(spnOperator);
			if (MetrixStringHelper.isNullOrEmpty(currentOperator))
				validationFailed = true;
			else {
				LinearLayout rightOperandLayout = (LinearLayout)mTable.findViewWithTag("RIGHT_OPERAND");
				EditText etRightOperand = (EditText) rightOperandLayout.findViewWithTag("property_value");
				LinearLayout noQuotesLayout = (LinearLayout)mTable.findViewWithTag("NO_QUOTES");
				CheckBox chkNoQuotes = (CheckBox) noQuotesLayout.findViewWithTag("property_value");
				if (MetrixStringHelper.valueIsEqual(currentOperator, "IS_NULL") || MetrixStringHelper.valueIsEqual(currentOperator, "IS_NOT_NULL")) {
					etRightOperand.setText("");
					chkNoQuotes.setChecked(false);
				} else if (MetrixStringHelper.isNullOrEmpty(etRightOperand.getText().toString()))
					validationFailed = true;
			}

			if (validationFailed) {
				Toast.makeText(this, AndroidResourceHelper.getMessage("LookupFilterPropError"), Toast.LENGTH_LONG).show();
				return false;
			}

			LinearLayout leftParensLayout = (LinearLayout)mTable.findViewWithTag("LEFT_PARENS");
			EditText txtLeftParens = (EditText) leftParensLayout.findViewWithTag("property_value");
			String leftParens = MetrixControlAssistant.getValue(txtLeftParens);
			boolean leftParensIsValid = true;
			if (!MetrixStringHelper.isNullOrEmpty(leftParens)) {
				int leftParensCount = MetrixFloatHelper.convertNumericFromUIToNumber(leftParens).intValue();
				leftParensIsValid = leftParensCount >= 0 && !leftParens.contains(MetrixFloatHelper.getDecimalSeparator());
			}

			LinearLayout rightParensLayout = (LinearLayout)mTable.findViewWithTag("RIGHT_PARENS");
			EditText txtRightParens = (EditText) rightParensLayout.findViewWithTag("property_value");
			String rightParens = MetrixControlAssistant.getValue(txtRightParens);
			boolean rightParensIsValid = true;
			if (!MetrixStringHelper.isNullOrEmpty(rightParens)) {
				int rightParensCount = MetrixFloatHelper.convertNumericFromUIToNumber(rightParens).intValue();
				rightParensIsValid = rightParensCount >= 0 && !rightParens.contains(MetrixFloatHelper.getDecimalSeparator());
			}

			if (!leftParensIsValid || !rightParensIsValid) {
				Toast.makeText(this, AndroidResourceHelper.getMessage("LookupFilterParensError"), Toast.LENGTH_LONG).show();
				return false;
			}

			for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

				String controlType = tvControlType.getText().toString();
				String propName = tvPropName.getText().toString();
				String currentPropValue = "";

				if (MetrixStringHelper.valueIsEqual(controlType, "COMBOBOX")) {
					Spinner spnPropValue = (Spinner) currLayout.findViewWithTag("property_value");
					currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
				} else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
					CheckBox chkPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
					if (chkPropValue.isChecked())
						currentPropValue = "Y";
					else
						currentPropValue = "N";
				} else if (MetrixStringHelper.valueIsEqual(controlType, "LABEL")) {
					continue;	// don't consider labels for saving changes
				} else if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX")) {
					EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
					currentPropValue = etPropValue.getText().toString();
				} else {
					throw new Exception("MDFieldLookupFilterProp: Unhandled control type.");
				}

				int currLookupTableIDNum = Integer.valueOf(lkupFilterID);
				HashMap<String, String> origRow = mOriginalData.get(currLookupTableIDNum);

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "LOGICAL_OPERATOR"))
					origPropValue = origRow.get("mm_field_lkup_filter.logical_operator");
				else if (MetrixStringHelper.valueIsEqual(propName, "OPERATOR"))
					origPropValue = origRow.get("mm_field_lkup_filter.operator");
				else if (MetrixStringHelper.valueIsEqual(propName, "RIGHT_OPERAND"))
					origPropValue = origRow.get("mm_field_lkup_filter.right_operand");
				else if (MetrixStringHelper.valueIsEqual(propName, "LEFT_PARENS"))
					origPropValue = origRow.get("mm_field_lkup_filter.left_parens");
				else if (MetrixStringHelper.valueIsEqual(propName, "RIGHT_PARENS"))
					origPropValue = origRow.get("mm_field_lkup_filter.right_parens");
				else if (MetrixStringHelper.valueIsEqual(propName, "NO_QUOTES"))
					origPropValue = origRow.get("mm_field_lkup_filter.no_quotes");

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "LOGICAL_OPERATOR"))
						data.dataFields.add(new DataField("logical_operator", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "OPERATOR"))
						data.dataFields.add(new DataField("operator", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "RIGHT_OPERAND"))
						data.dataFields.add(new DataField("right_operand", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "LEFT_PARENS"))
						data.dataFields.add(new DataField("left_parens", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "RIGHT_PARENS"))
						data.dataFields.add(new DataField("right_parens", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "NO_QUOTES"))
						data.dataFields.add(new DataField("no_quotes", currentPropValue));

					lookupFilterPropChangeCount++;
				}
			}

			if (lookupFilterPropChangeCount > 0) {
				// now, update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this field lookup filter has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
				String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
				// if previousRevisionID exists, then this is not the first revision
				if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
						&& (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
					// filter meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}

				lookupFilterToUpdate.add(data);

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupFilterToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookupFilterChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	private void setRightOperandVisibilityBasedOnSelectedOperator() {
		try {
			LinearLayout operatorLayout = (LinearLayout)mTable.findViewWithTag("OPERATOR");
			LinearLayout rightOperandLayout = (LinearLayout)mTable.findViewWithTag("RIGHT_OPERAND");
			LinearLayout noQuotesLayout = (LinearLayout)mTable.findViewWithTag("NO_QUOTES");
			Spinner spnOperator = (Spinner) operatorLayout.findViewWithTag("property_value");
			String currentOperator = MetrixControlAssistant.getValue(spnOperator);
			if (!MetrixStringHelper.isNullOrEmpty(currentOperator) && (MetrixStringHelper.valueIsEqual(currentOperator, "IS_NULL") || MetrixStringHelper.valueIsEqual(currentOperator, "IS_NOT_NULL"))) {
				rightOperandLayout.setVisibility(View.GONE);
				noQuotesLayout.setVisibility(View.GONE);
			} else {
				rightOperandLayout.setVisibility(View.VISIBLE);
				noQuotesLayout.setVisibility(View.VISIBLE);
				CheckBox chkNoQuotes = (CheckBox) noQuotesLayout.findViewWithTag("property_value");
				chkNoQuotes.setEnabled(true);
				if (MetrixStringHelper.valueIsEqual(currentOperator, "LIKE") || MetrixStringHelper.valueIsEqual(currentOperator, "NOT_LIKE")) {
					chkNoQuotes.setChecked(false);
					chkNoQuotes.setEnabled(false);
				} else if (MetrixStringHelper.valueIsEqual(currentOperator, "IN") || MetrixStringHelper.valueIsEqual(currentOperator, "NOT_IN")) {
					chkNoQuotes.setChecked(true);
					chkNoQuotes.setEnabled(false);
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}
}
