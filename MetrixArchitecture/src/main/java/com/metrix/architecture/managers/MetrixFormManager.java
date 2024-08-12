package com.metrix.architecture.managers;

import android.app.Activity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.attachment.AttachmentField;
import com.metrix.architecture.constants.MetrixControlCase;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixDropDownDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.signature.SignatureField;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixSwipeHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains methods which manage the setup and processing on layouts.
 * 
 * @since 5.4
 */
@SuppressWarnings("deprecation")
public class MetrixFormManager {

	/**
	 * Prepares an activity to be displayed to the user based upon the meta data
	 * defined.
	 * 
	 * @param activity
	 *            the activity to be setup.
	 * @param layout
	 *            the activities related layout.
	 * @param metrixFormDef
	 *            the meta data defined for the activity.
	 * @return true if the activity was setup properly, false otherwise.
	 * 
	 *         <pre>
	 * defineForm();
	 * MetrixFormManager.setupForm(this, mLayout, this.mFormDef);
	 * </pre>
	 */
	public static boolean setupForm(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {

		if (metrixFormDef == null) {
			return false;
		}

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		clearControls(layout, metrixFormDef);
		populateSpinners(activity, layout, metrixFormDef);
		identifyRequiredFields(layout, metrixFormDef);
		setControlsCase(layout, metrixFormDef);
		setUpAttachmentFields(activity, layout, metrixFormDef);
		setUpSignatureFields(activity, layout, metrixFormDef);

		if (metrixFormDef.tables != null && metrixFormDef.tables.size() > 0) {
			if (metrixFormDef.tables.get(0).transactionType == MetrixTransactionTypes.INSERT) {
				MetrixFormManager.setPrimaryKeys(layout, metrixFormDef);
			} else if ((metrixFormDef.tables.get(0).transactionType == MetrixTransactionTypes.UPDATE) || (metrixFormDef.tables.get(0).transactionType == MetrixTransactionTypes.CORRECT_ERROR)
					|| (metrixFormDef.tables.get(0).transactionType == MetrixTransactionTypes.SELECT)) {
				String query = MetrixQueryManager.buildQuery(layout, metrixFormDef);

				MetrixCursor cursor = null;

				try {
					cursor = MetrixDatabaseManager.rawQueryMC(query, null);

					populateLayoutFromQuery(activity, layout, metrixFormDef, cursor);
				} catch (Exception e) {
					MetrixUIHelper.showSnackbar(activity, e.getMessage());
					LogManager.getInstance().error(e);
					return false;
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
		}

		return true;
	}

	/**
	 * @param activity
	 * @param layout
	 * 
	 * @deprecated in 5.6.0
	 */
	@Deprecated
	public static void setSwipeHandler(Activity activity, View layout) {
		MetrixSwipeHelper swipeHelper = new MetrixSwipeHelper(activity);
		layout.setOnTouchListener(swipeHelper);
	}

	private static void clearControls(ViewGroup layout, MetrixFormDef metrixFormDef) {
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				try {
					//Skipping clear controls logic for buttons
					View view = MetrixControlAssistant.getControl(columnDef, layout);
					if(view != null && (view instanceof Button)) continue;

					MetrixControlAssistant.setValue(columnDef, layout, "");
				} catch (Exception e) {
					LogManager.getInstance().error(e);
				}
			}
		}
	}

	/**
	 * Generate and set the primary key values for the tables on the current
	 * screen.
	 */
	private static void setPrimaryKeys(ViewGroup layout, MetrixFormDef metrixFormDef) {
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (tableDef.transactionType == MetrixTransactionTypes.INSERT) {
				int primaryKey = MetrixDatabaseManager.generatePrimaryKey(tableDef.tableName);

				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.primaryKey) {
						try {
							MetrixControlAssistant.setValue(metrixFormDef, layout, tableDef.tableName, columnDef.columnName, String.valueOf(primaryKey));
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Fill any spinners on the current layout with the data defined in the meta
	 * data.
	 * 
	 * @param layout
	 *            the activities related layout.
	 * @param metrixFormDef
	 *            the meta data defined for the activity.
	 */
	private static void populateSpinners(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (metrixFormDef.tables == null || metrixFormDef.tables.size() == 0) {
			return;
		}

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (columnDef.lookupDef != null && (!MetrixStringHelper.isNullOrEmpty(columnDef.lookupDef.tableName))) {
					View view = MetrixControlAssistant.getControl(columnDef, layout);
					if (view instanceof Spinner) {
						MetrixFormManager.populateSpinner(activity, layout, view, columnDef.lookupDef);
					}
				} else if (columnDef.lookupValues != null && columnDef.lookupValues.size() > 0) {
					View view = MetrixControlAssistant.getControl(columnDef, layout);
					if (view instanceof Spinner) {
						MetrixControlAssistant.populateSpinnerFromPair(activity, view, columnDef.lookupValues);
					}
				}
			}
		}

		for (MetrixDropDownDef dropDownDef : metrixFormDef.lookups) {
			View view = MetrixControlAssistant.getControl(dropDownDef.controlId, layout);
			MetrixFormManager.populateSpinner(activity, layout, view, dropDownDef);
		}
	}

	/**
	 * Fill a spinner on the current layout with the data defined in the meta
	 * data.
	 * 
	 * @param layout
	 *            the activities related layout.
	 * @param view
	 *            the spinner to populate
	 * @param lookupDef
	 *            the spinner's meta data
	 * 
	 *            <pre>
	 * MetrixDropDownDef fromLocationDef = new MetrixDropDownDef(R.id.stock_adjustment__location_from, &quot;location&quot;, &quot;location&quot;, &quot;location&quot;, &quot;place_id&quot;, ((SpinnerKeyValuePair) mPlaceFromSpinner.getSelectedItem()).mValue.toString());
	 * MetrixFormManager.populateSpinner(this, mLayout, mLocationFromSpinner, fromLocationDef);
	 * </pre>
	 */
	public static void populateSpinner(Activity activity, ViewGroup layout, View view, MetrixDropDownDef lookupDef) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		if (lookupDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLookupdefParamIsReq"));
		}

		StringBuilder query = new StringBuilder();
		query.append("select ");
		query.append(lookupDef.valueColumn);
		query.append(", ");
		query.append(lookupDef.displayColumn);
		query.append(" from ");
		query.append(lookupDef.tableName);

		if (!MetrixStringHelper.isNullOrEmpty(lookupDef.filterColumn)) {
			query.append(" where ");
			query.append(lookupDef.filterColumn);
			query.append(" = ");
			query.append("'");
			query.append(lookupDef.filterValue);
			query.append("'");
		}

		if (lookupDef.orderBy != null && (!MetrixStringHelper.isNullOrEmpty(lookupDef.orderBy.columnName))) {
			query.append(" order by ");
			query.append(lookupDef.orderBy.columnName);
			query.append(" ");
			query.append(lookupDef.orderBy.sortOrder);
		}
		
		MetrixControlAssistant.populateSpinnerFromQuery(activity, view, query.toString());
	}

	/**
	 * Enhances the controls applying input filters to enforce upper or lower
	 * cased values.
	 * 
	 * @param layout
	 *            the activities related layout.
	 * @param metrixFormDef
	 *            the meta data defined for the activity.
	 */
	private static void setControlsCase(ViewGroup layout, MetrixFormDef metrixFormDef) {
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (columnDef.forceCase != MetrixControlCase.NONE) {
					if (columnDef.forceCase == MetrixControlCase.UPPER) {
						View view = MetrixControlAssistant.getControl(columnDef, layout);
						if (view != null && view instanceof EditText) {
							final EditText editText = (EditText) view;
							InputFilter capsFilter = new InputFilter.AllCaps();
							InputFilter[] currFilters = editText.getFilters();
							if (currFilters != null && currFilters.length > 0) {
								InputFilter[] newFilters = new InputFilter[(currFilters.length + 1)];
								for (int i = 0; i < currFilters.length; i++) {
									newFilters[i] = currFilters[i];
								}
								newFilters[currFilters.length] = capsFilter;
								editText.setFilters(newFilters);
							} else {
								currFilters = new InputFilter[1];
								currFilters[0] = capsFilter;
								editText.setFilters(currFilters);
							}
							//new region force -> uppercase implementation
							final TextWatcher textWatcher = new TextWatcher() {
								boolean shouldContinue = true;

								@Override
								public void beforeTextChanged(CharSequence s, int start, int count, int after) {
									//To avoid executing of text change listener -> onTextChanged when the time of data binding..
									shouldContinue = editText.hasFocus() ? true : false;
								}

								@Override
								public void onTextChanged(CharSequence s, int start, int before, int count) {
									if(!shouldContinue)return;

									int curCursorLoc = editText.getSelectionEnd();
									if(curCursorLoc < 1) return;
								}

								@Override
								public void afterTextChanged(Editable s) {
								}
							};

							editText.addTextChangedListener(textWatcher);
							//endregion
						}
					}
				}
			}
		}
	}

	private static void setUpAttachmentFields(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (metrixFormDef.tables == null || metrixFormDef.tables.size() == 0) {
			return;
		}

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (!MetrixStringHelper.isNullOrEmpty(columnDef.controlType) && MetrixStringHelper.valueIsEqual(columnDef.controlType.toUpperCase(), "ATTACHMENT")) {
					View view = MetrixControlAssistant.getControl(columnDef, layout);
					if (view != null && view instanceof AttachmentField) {
						AttachmentField attachmentField = (AttachmentField)view;
						attachmentField.setupFromConfiguration(activity, columnDef, tableDef.tableName);
					}
				}
			}
		}
	}

	private static void setUpSignatureFields(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (metrixFormDef.tables == null || metrixFormDef.tables.size() == 0) {
			return;
		}

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (!MetrixStringHelper.isNullOrEmpty(columnDef.controlType) && MetrixStringHelper.valueIsEqual(columnDef.controlType.toUpperCase(), "SIGNATURE")) {
					View view = MetrixControlAssistant.getControl(columnDef, layout);
					if (view != null && view instanceof SignatureField) {
						SignatureField signatureField = (SignatureField) view;
						signatureField.setupFromConfiguration(activity, layout, metrixFormDef, columnDef, tableDef.tableName);
					}
				}
			}
		}
	}

	/**
	 * Enhances the controls changing their background color as a visual
	 * indication to the user that a value is required for them. This is called
	 * by the Architecture during the setupForm, but if you need to change which
	 * columns are required based on the data the user is entering, this can be
	 * called to refresh the layout display.
	 * 
	 * @param layout
	 *            the activities related layout.
	 * @param metrixFormDef
	 *            the meta data defined for the activity.
	 * 
	 *            <pre>
	 * MetrixFormManager.identifyRequiredFields(mLayout, this.mFormDef);
	 * </pre>
	 */
	public static void identifyRequiredFields(ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (columnDef.required) {
					View view = MetrixControlAssistant.getControl(columnDef, layout);

					if (view instanceof EditText) {
						EditText editText = (EditText) view;
						editText.setHint(AndroidResourceHelper.getMessage("Required"));
					}

					if (view instanceof Spinner) {
						/*
						 * Spinner spinner = (Spinner) view;
						 * spinner.setHint("Required");
						 */}
				}
			}
		}
	}

	/**
	 * Populates the current layout's controls with the values from the database
	 * that match the defined meta data for the controls.
	 * 
	 * @param layout
	 *            the activities related layout.
	 * @param metrixFormDef
	 *            the meta data defined for the activity.
	 * @param cursor
	 *            the database cursor containing the values selected from the
	 *            database.
	 * @throws Exception
	 */
	private static void populateLayoutFromQuery(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, MetrixCursor cursor) throws Exception {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		Map<String, String> originalValues = new HashMap<String, String>();

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}
			for (MetrixColumnDef columnDef : tableDef.columns) {

				int index = cursor.getColumnIndex(tableDef.tableName + "__" + columnDef.columnName);
				String value = cursor.getString(index);
				String dbValue = value;

				try {
					if (columnDef.maximumCharacters > 0) {
						MetrixControlAssistant.setTag(columnDef, layout, value);
						View control = MetrixControlAssistant.getControl(columnDef.id, layout);

						if (control instanceof EditText) {
							if (activity instanceof OnFocusChangeListener) {
								((EditText) control).setOnFocusChangeListener((OnFocusChangeListener) activity);
							}
							if (activity instanceof OnClickListener) {
								((EditText) control).setOnClickListener((OnClickListener) activity);
							}
						} else if (control instanceof TextView) {
							if (activity instanceof OnFocusChangeListener) {
								((TextView) control).setOnFocusChangeListener((OnFocusChangeListener) activity);
							}
							if (activity instanceof OnClickListener) {
								((TextView) control).setOnClickListener((OnClickListener) activity);
							}
						}

						if (value != null && value.length() > columnDef.maximumCharacters)
							value = getDisplayValue(value, columnDef.maximumCharacters) + "...";

						MetrixControlAssistant.setValue(columnDef, layout, value);
					} else {
						View view = MetrixControlAssistant.getControl(columnDef, layout);
						if(view instanceof SignatureField) {
							SignatureField signatureField = ((SignatureField) view);
							TextView textView = signatureField.mHiddenAttachmentIdTextView;
							textView.setText(value);
							signatureField.setupFromConfiguration(activity, layout, metrixFormDef, columnDef, tableDef.tableName);
							signatureField.updateFieldUI();
						} else {
						MetrixControlAssistant.setValue(columnDef, layout, value);
						}
					}

				} catch (Exception ex) {
					MetrixUIHelper.showSnackbar(activity, ex.getMessage());
				} finally {
					originalValues.put(tableDef.tableName + "." + columnDef.columnName, dbValue);
				}
			}
		}

		MetrixPublicCache.instance.addItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES, originalValues);
	}

	private static String getDisplayValue(String value, int maxLen) {
		StringBuilder displayValue = new StringBuilder();

		if (value.contains(" ")) {
			String[] words = value.split(" ");

			for (int i = 0; i < words.length; i++) {
				displayValue.append(words[i]);
				displayValue.append(" ");

				if (i < words.length - 1 && (displayValue.length() + words[i + 1].length()) > maxLen)
					break;
			}
		}

		return displayValue.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static void resetOriginalValue(String tableColumnName, String newValue){
		HashMap<String, String> originalSavedCache = (HashMap<String, String>) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);
		
		if(originalSavedCache.containsKey(tableColumnName)){
			originalSavedCache.put(tableColumnName, newValue);
		}
		
		MetrixPublicCache.instance.addItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES, originalSavedCache);
	}

	/**
	 * Returns the original value of a control's bound column when the screen
	 * was first bound. This allows you to quickly check if the value was
	 * changed by the user.
	 * 
	 * @param columnName
	 *            the name of the column whos original value you wish to get.
	 */
	public static String getOriginalValue(String columnName) {
		HashMap<String, String> originalValues = MetrixFormManager.getOriginalValues();
		if (originalValues!=null && originalValues.containsKey(columnName)) {
			return originalValues.get(columnName);
		} else {
			return "";
		}
	}

	/**
	 * Returns the origina value of all control's bound to the current screen.
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getOriginalValues() {
		return (HashMap<String, String>) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);
	}
}
