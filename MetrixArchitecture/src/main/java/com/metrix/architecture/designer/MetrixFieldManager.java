package com.metrix.architecture.designer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.metrix.architecture.BuildConfig;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.attachment.AttachmentField;
import com.metrix.architecture.constants.MetrixControlCase;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixDropDownDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixOrderByDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.signature.SignatureField;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDate;
import com.metrix.architecture.utilities.MetrixDateTime;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTime;
import com.metrix.architecture.utilities.StopWatch;
import com.metrix.architecture.utilities.User;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class MetrixFieldManager extends MetrixDesignerManager {
	// These will be reused when setting up the AttachmentField control itself, so these should be public
	public static final String ALLOW_FILE = "ALLOW_FILE";
	public static final String ALLOW_PHOTO = "ALLOW_PHOTO";
	public static final String ALLOW_VIDEO = "ALLOW_VIDEO";
	public static final String CARD_SCREEN_ID = "CARD_SCREEN_ID";
	public static final String ALLOW_CLEAR = "ALLOW_CLEAR";
	public static final String SIGNER_COLUMN = "SIGNER_COLUMN";
	public static final String SIGNER_MESSAGE_ID = "SIGNER_MESSAGE_ID";
	public static final String TRANSACTION_TABLE = "TRANSACTION_TABLE";
	public static final String TRANSACTION_COLUMN = "TRANSACTION_COLUMN";

	private static final String CHECKBOX = "CHECKBOX";
	private static final String LABEL = "LABEL";
	private static final String TEXT = "TEXT";
	private static final String LIST = "LIST";
	private static final String LONG_TEXT = "LONGTEXT";
	private static final String BUTTON = "BUTTON";
	private static final String HYPERLINK = "HYPERLINK";
	private static final String LONGHYPERLINK = "LONGHYPERLINK";
	private static final String ATTACHMENT = "ATTACHMENT";
	private static final String SIGNATURE = "SIGNATURE";

	private static final String READ_ONLY = "READ_ONLY";
	private static final String VISIBLE = "VISIBLE";
	private static final String VALIDATION = "VALIDATION";
	private static final String INPUT_TYPE = "INPUT_TYPE";
	private static final String DATA_TYPE = "DATA_TYPE";
	private static final String REQUIRED = "REQUIRED";
	private static final String FORCE_CASE = "FORCE_CASE";
	private static final String CONTROL_TYPE = "CONTROL_TYPE";
	private static final String VALUE_CHANGED_EVENT = "VALUE_CHANGED_EVENT";
	private static final String MAX_CHARS = "MAX_CHARS";
	private static final String CONTROL_EVENT = "CONTROL_EVENT";
	private static final String LIST_DISPLAY_COLUMN = "LIST_DISPLAY_COLUMN";
	private static final String LIST_VALUE_COLUMN = "LIST_VALUE_COLUMN";
	private static final String LIST_TABLE_NAME = "LIST_TABLE_NAME";
	private static final String LIST_ORDER_BY = "LIST_ORDER_BY";
	private static final String LIST_FILTER_COLUMN = "LIST_FILTER_COLUMN";
	private static final String LIST_FILTER_VALUE = "LIST_FILTER_VALUE";

	private static final String DATA_TYPE_STRING = "STRING";
	private static final String DATA_TYPE_NUMBER = "NUMBER";
	private static final String DATA_TYPE_DATE = "DATE";
	private static final String DATA_TYPE_DATE_TIME = "DATE_TIME";
	private static final String DATA_TYPE_TIME = "TIME";

	private static final String INPUT_TYPE_EMAIL_ADDRESS = "EMAIL";
	private static final String INPUT_TYPE_PHONE = "PHONE";
	private static final String INPUT_TYPE_NUMBER = "NUMBER";
	private static final String INPUT_TYPE_PASSWORD = "PASSWORD";
	
	private static int portraitCheckBox;
	private static int portraitReadOnly;
	private static int portraitText;
	private static int portraitList;
	private static int portraitListvalue; //5.6.2
	private static int portraitButton;
	private static int portraitMetrixHyperlink;
	private static int portraitAttachment;
	private static int portraitSignature;

	private static int landscapeCheckBoxCheckBox;
	private static int landscapeCheckBoxReadOnly;
	private static int landscapeCheckBoxText;
	private static int landscapeCheckBoxList;
	private static int landscapeCheckBoxListvalue; //5.6.2
	private static int landscapeCheckBoxButton;
	private static int landscapeCheckBoxMetrixHyperlink;
	private static int landscapeCheckBoxAttachment;
	private static int landscapeCheckBoxSignature;

	private static int landscapeReadOnlyCheckBox;
	private static int landscapeReadOnlyReadOnly;
	private static int landscapeReadOnlyText;
	private static int landscapeReadOnlyList;
	private static int landscapeReadOnlyListvalue;//5.6.2
	private static int landscapeReadOnlyButton;
	private static int landscapeReadOnlyMetrixHyperlink;
	private static int landscapeReadOnlyAttachment;
	private static int landscapeReadOnlySignature;

	private static int landscapeTextCheckBox;
	private static int landscapeTextReadOnly;
	private static int landscapeTextText;
	private static int landscapeTextList;
	private static int landscapeTextListvalue;//5.6.2
	private static int landscapeTextButton;
	private static int landscapeTextMetrixHyperlink;
	private static int landscapeTextAttachment;
	private static int landscapeTextSignature;

	private static int landscapeListCheckBox;
	private static int landscapeListReadOnly;
	private static int landscapeListText;
	private static int landscapeListList;
	private static int landscapeListListvalue; //5.6.2
	private static int landscapeListButton;
	private static int landscapeListMetrixHyperlink;
	private static int landscapeListAttachment;
	private static int landscapeListSignature;

	private static int landscapeListvalueCheckBox; //5.6.2
	private static int landscapeListvalueReadOnly; //5.6.2
	private static int landscapeListvalueText; //5.6.2
	private static int landscapeListvalueList; //5.6.2	 
	private static int landscapeListvalueListvalue; //5.6.2
	private static int landscapeListvalueButton;
	private static int landscapeListvalueMetrixHyperlink;
	private static int landscapeListvalueAttachment;
	private static int landscapeListvalueSignature;

	private static int backgroundImage; //5.6.2
	public static int spinnerReadOnlyStyle; //5.6.2

	private static int landscapeButtonCheckBox;
	private static int landscapeButtonReadOnly;
	private static int landscapeButtonText;
	private static int landscapeButtonList;
	private static int landscapeButtonListvalue;
	private static int landscapeButtonButton;
	private static int landscapeButtonMetrixHyperlink;
	private static int landscapeButtonAttachment;
	private static int landscapeButtonSignature;

	private static int landscapeMetrixHyperlinkCheckBox;
	private static int landscapeMetrixHyperlinkReadOnly;
	private static int landscapeMetrixHyperlinkText;
	private static int landscapeMetrixHyperlinkList;
	private static int landscapeMetrixHyperlinkListvalue;
	private static int landscapeMetrixHyperlinkButton;
	private static int landscapeMetrixHyperlinkMetrixHyperlink;
	private static int landscapeMetrixHyperlinkAttachment;
	private static int landscapeMetrixHyperlinkSignature;

	private static int landscapeAttachmentCheckBox;
	private static int landscapeAttachmentReadOnly;
	private static int landscapeAttachmentText;
	private static int landscapeAttachmentList;
	private static int landscapeAttachmentListvalue;
	private static int landscapeAttachmentButton;
	private static int landscapeAttachmentMetrixHyperlink;
	private static int landscapeAttachmentAttachment;
	private static int landscapeAttachmentSignature;
	private static int landscapeSignatureCheckBox;
	private static int landscapeSignatureReadOnly;
	private static int landscapeSignatureText;
	private static int landscapeSignatureList;
	private static int landscapeSignatureListvalue;
	private static int landscapeSignatureButton;
	private static int landscapeSignatureMetrixHyperlink;
	private static int landscapeSignatureSignature;
	private static int landscapeSignatureAttachment;

	private static int standardScreenMapWidget;

	// outer key - screen id/field id
	// inner key - property name
	// inner value - property value
	private static Map<String, Map<String, String>> fieldProperties = new LinkedHashMap<String, Map<String, String>>();
	
	// outer key - screen id
	// inner key - field id
	// inner value - default value
	private static Map<String, Map<String, String>> defaultValues = new LinkedHashMap<String, Map<String, String>>();
	
	// dynamic default value codes
	private static final String DEFAULT_CURRENTDATETIME_MESSAGE = "~*CURRENTDATETIME*~";
	private static final String DEFAULT_CURRENTUSER_MESSAGE = "~*CURRENTUSER*~";
	private static final String DEFAULT_CURRENTUSERCURRENCY_MESSAGE = "~*CURRENTUSERCURRENCY*~";
	private static final String DEFAULT_CURRENTUSERSTOCKPLACE_MESSAGE = "~*CURRENTUSERSTOCKPLACE*~";
	private static final String DEFAULT_CURRENTUSERSTOCKLOCATION_MESSAGE = "~*CURRENTUSERSTOCKLOCATION*~";
	private static final String DEFAULT_CORPORATECURRENCY_MESSAGE = "~*CORPORATECURRENCY*~";
	private static final String DEFAULT_CURRENTTASKID_MESSAGE = "~*CURRENTTASKID*~";
	private static final String DEFAULT_CURRENTREQUESTID_MESSAGE = "~*CURRENTREQUESTID*~";
	private static final String DEFAULT_CURRENTPRODUCT_MESSAGE = "~*CURRENTPRODUCT*~";
	private static final String DEFAULT_CURRENTCONTACT_MESSAGE = "~*CURRENTCONTACT*~";
	private static final String DEFAULT_CURRENTLATITUDE_MESSAGE = "~*CURRENTLATITUDE*~";
	private static final String DEFAULT_CURRENTLONGITUDE_MESSAGE = "~*CURRENTLONGITUDE*~";
	private static final String DEFAULT_CURRENTQUOTEID_MESSAGE = "~*CURRENTQUOTEID*~";
	private static final String DEFAULT_CURRENTQUOTEVERSION_MESSAGE = "~*CURRENTQUOTEVERSION*~";
	// end - dynamic default value codes
	
	public static void clearFieldPropertiesCache() {
		MetrixFieldManager.fieldProperties.clear();
	}
	
	public static void clearDefaultValuesCache() {
		MetrixFieldManager.defaultValues.clear();
	}
	
	/**
	 * Determines whether this screen's region has any visible fields.
	 * 
	 * @param screenId The current screen id.
	 * @param region The region to test.
	 * 
	 * @since 5.6.3
	 */
	public static boolean regionContainsVisibleFields(int screenId, String region) {
		String whereClause = String.format("screen_id = %1$s and region = '%2$s' and visible = 'Y'", String.valueOf(screenId), region);
		int visibleFieldCount = MetrixDatabaseManager.getCount("use_mm_field", whereClause);
		return visibleFieldCount > 0;
	}

	public static void setInflatableLayouts(
			int portraitCheckBox, int portraitReadOnly, int portraitText, int portraitList, int portraitListvalue, int portraitButton, int portraitMetrixHyperlink,
			int landscapeCheckBoxCheckBox, int landscapeCheckBoxReadOnly, int landscapeCheckBoxText, int landscapeCheckBoxList, int landscapeCheckBoxListvalue, int landscapeCheckBoxButton, int landscapeCheckBoxMetrixHyperlink,
			int landscapeReadOnlyCheckBox, int landscapeReadOnlyReadOnly, int landscapeReadOnlyText, int landscapeReadOnlyList, int landscapeReadOnlyListvalue, int landscapeReadOnlyButton, int landscapeReadOnlyMetrixHyperlink,
			int landscapeTextCheckBox, int landscapeTextReadOnly, int landscapeTextText, int landscapeTextList, int landscapeTextListvalue, int landscapeTextButton, int landscapeTextMetrixHyperlink,
			int landscapeListCheckBox, int landscapeListReadOnly, int landscapeListText, int landscapeListList, int landscapeListListvalue, int landscapeListButton, int landscapeListMetrixHyperlink,
			int landscapeListvalueCheckBox, int landscapeListvalueReadOnly, int landscapeListvalueText, int landscapeListvalueList, int landscapeListvalueListvalue, int landscapeListvalueButton, int landscapeListvalueMetrixHyperlink,
			int backgroundDrawing, int spinnerReadOnlyItemView,
			int landscapeButtonCheckBox, int landscapeButtonReadOnly, int landscapeButtonText, int landscapeButtonList, int landscapeButtonListvalue, int landscapeButtonButton, int landscapeButtonMetrixHyperlink,
			int landscapeMetrixHyperlinkCheckBox, int landscapeMetrixHyperlinkReadOnly, int landscapeMetrixHyperlinkText, int landscapeMetrixHyperlinkList, int landscapeMetrixHyperlinkListvalue, int landscapeMetrixHyperlinkButton, int landscapeMetrixHyperlinkMetrixHyperlink,
			int standardScreenMapWidget,
			int portraitAttachment, int landscapeCheckBoxAttachment, int landscapeReadOnlyAttachment, int landscapeTextAttachment, int landscapeListAttachment, int landscapeListvalueAttachment, int landscapeButtonAttachment, int landscapeMetrixHyperlinkAttachment,
			int landscapeAttachmentCheckBox, int landscapeAttachmentReadOnly, int landscapeAttachmentText, int landscapeAttachmentList, int landscapeAttachmentListvalue, int landscapeAttachmentButton, int landscapeAttachmentMetrixHyperlink, int landscapeAttachmentAttachment,
			int landscapeAttachmentSignature,
			int portraitSignature, int landscapeCheckBoxSignature, int landscapeReadOnlySignature, int landscapeTextSignature, int landscapeListSignature, int landscapeListvalueSignature, int landscapeButtonSignature, int landscapeMetrixHyperlinkSignature,
			int landscapeSignatureCheckBox, int landscapeSignatureReadOnly, int landscapeSignatureText, int landscapeSignatureList, int landscapeSignatureListvalue, int landscapeSignatureButton, int landscapeSignatureMetrixHyperlink, int landscapeSignatureSignature, int landscapeSignatureAttachment) {
	
		MetrixFieldManager.portraitCheckBox = portraitCheckBox;
		MetrixFieldManager.portraitReadOnly = portraitReadOnly;
		MetrixFieldManager.portraitText = portraitText;
		MetrixFieldManager.portraitList = portraitList;
		MetrixFieldManager.portraitListvalue = portraitListvalue;
		MetrixFieldManager.portraitButton = portraitButton;
		MetrixFieldManager.portraitMetrixHyperlink = portraitMetrixHyperlink;

		MetrixFieldManager.landscapeCheckBoxCheckBox = landscapeCheckBoxCheckBox;
		MetrixFieldManager.landscapeCheckBoxReadOnly = landscapeCheckBoxReadOnly;
		MetrixFieldManager.landscapeCheckBoxText = landscapeCheckBoxText;
		MetrixFieldManager.landscapeCheckBoxList = landscapeCheckBoxList;
		MetrixFieldManager.landscapeCheckBoxListvalue = landscapeCheckBoxListvalue;
		MetrixFieldManager.landscapeCheckBoxButton = landscapeCheckBoxButton;
		MetrixFieldManager.landscapeCheckBoxMetrixHyperlink = landscapeCheckBoxMetrixHyperlink;

		MetrixFieldManager.landscapeReadOnlyCheckBox = landscapeReadOnlyCheckBox;
		MetrixFieldManager.landscapeReadOnlyReadOnly = landscapeReadOnlyReadOnly;
		MetrixFieldManager.landscapeReadOnlyText = landscapeReadOnlyText;
		MetrixFieldManager.landscapeReadOnlyList = landscapeReadOnlyList;
		MetrixFieldManager.landscapeReadOnlyListvalue = landscapeReadOnlyListvalue;
		MetrixFieldManager.landscapeReadOnlyButton = landscapeReadOnlyButton;
		MetrixFieldManager.landscapeReadOnlyMetrixHyperlink = landscapeReadOnlyMetrixHyperlink;

		MetrixFieldManager.landscapeTextCheckBox = landscapeTextCheckBox;
		MetrixFieldManager.landscapeTextReadOnly = landscapeTextReadOnly;
		MetrixFieldManager.landscapeTextText = landscapeTextText;
		MetrixFieldManager.landscapeTextList = landscapeTextList;
		MetrixFieldManager.landscapeTextListvalue = landscapeTextListvalue;
		MetrixFieldManager.landscapeTextButton = landscapeTextButton;
		MetrixFieldManager.landscapeTextMetrixHyperlink = landscapeTextMetrixHyperlink;

		MetrixFieldManager.landscapeListCheckBox = landscapeListCheckBox;
		MetrixFieldManager.landscapeListReadOnly = landscapeListReadOnly;
		MetrixFieldManager.landscapeListText = landscapeListText;
		MetrixFieldManager.landscapeListList = landscapeListList;
		MetrixFieldManager.landscapeListButton = landscapeListButton;
		MetrixFieldManager.landscapeListMetrixHyperlink = landscapeListMetrixHyperlink;
		
		MetrixFieldManager.landscapeListListvalue = landscapeListListvalue;
		MetrixFieldManager.landscapeListvalueCheckBox = landscapeListvalueCheckBox;
		MetrixFieldManager.landscapeListvalueReadOnly = landscapeListvalueReadOnly;
		MetrixFieldManager.landscapeListvalueText = landscapeListvalueText;
		MetrixFieldManager.landscapeListvalueList = landscapeListvalueList;		
		MetrixFieldManager.landscapeListvalueListvalue = landscapeListvalueListvalue;
		MetrixFieldManager.landscapeListvalueButton = landscapeListvalueButton;
		MetrixFieldManager.landscapeListvalueMetrixHyperlink = landscapeListvalueMetrixHyperlink;

		MetrixFieldManager.backgroundImage = backgroundDrawing;
		MetrixFieldManager.spinnerReadOnlyStyle = spinnerReadOnlyItemView;

		MetrixFieldManager.landscapeButtonCheckBox = landscapeButtonCheckBox;
		MetrixFieldManager.landscapeButtonReadOnly = landscapeButtonReadOnly;
		MetrixFieldManager.landscapeButtonText = landscapeButtonText;
		MetrixFieldManager.landscapeButtonList = landscapeButtonList;
		MetrixFieldManager.landscapeButtonListvalue = landscapeButtonListvalue;
		MetrixFieldManager.landscapeButtonButton = landscapeButtonButton;
		MetrixFieldManager.landscapeButtonMetrixHyperlink = landscapeButtonMetrixHyperlink;

		MetrixFieldManager.landscapeMetrixHyperlinkCheckBox = landscapeMetrixHyperlinkCheckBox;
		MetrixFieldManager.landscapeMetrixHyperlinkReadOnly = landscapeMetrixHyperlinkReadOnly;
		MetrixFieldManager.landscapeMetrixHyperlinkText = landscapeMetrixHyperlinkText;
		MetrixFieldManager.landscapeMetrixHyperlinkList = landscapeMetrixHyperlinkList;
		MetrixFieldManager.landscapeMetrixHyperlinkListvalue = landscapeMetrixHyperlinkListvalue;
		MetrixFieldManager.landscapeMetrixHyperlinkButton = landscapeMetrixHyperlinkButton;
		MetrixFieldManager.landscapeMetrixHyperlinkMetrixHyperlink = landscapeMetrixHyperlinkMetrixHyperlink;

		MetrixFieldManager.standardScreenMapWidget = standardScreenMapWidget;

		MetrixFieldManager.portraitAttachment = portraitAttachment;
		MetrixFieldManager.landscapeCheckBoxAttachment = landscapeCheckBoxAttachment;
		MetrixFieldManager.landscapeReadOnlyAttachment = landscapeReadOnlyAttachment;
		MetrixFieldManager.landscapeTextAttachment = landscapeTextAttachment;
		MetrixFieldManager.landscapeListAttachment = landscapeListAttachment;
		MetrixFieldManager.landscapeListvalueAttachment = landscapeListvalueAttachment;
		MetrixFieldManager.landscapeButtonAttachment = landscapeButtonAttachment;
		MetrixFieldManager.landscapeMetrixHyperlinkAttachment = landscapeMetrixHyperlinkAttachment;

		MetrixFieldManager.landscapeAttachmentCheckBox = landscapeAttachmentCheckBox;
		MetrixFieldManager.landscapeAttachmentReadOnly = landscapeAttachmentReadOnly;
		MetrixFieldManager.landscapeAttachmentText = landscapeAttachmentText;
		MetrixFieldManager.landscapeAttachmentList = landscapeAttachmentList;
		MetrixFieldManager.landscapeAttachmentListvalue = landscapeAttachmentListvalue;
		MetrixFieldManager.landscapeAttachmentButton = landscapeAttachmentButton;
		MetrixFieldManager.landscapeAttachmentMetrixHyperlink = landscapeAttachmentMetrixHyperlink;
		MetrixFieldManager.landscapeAttachmentAttachment = landscapeAttachmentAttachment;
		MetrixFieldManager.landscapeAttachmentSignature = landscapeAttachmentSignature;

		MetrixFieldManager.portraitSignature = portraitSignature;
		MetrixFieldManager.landscapeCheckBoxSignature = landscapeCheckBoxSignature;
		MetrixFieldManager.landscapeReadOnlySignature = landscapeReadOnlySignature;
		MetrixFieldManager.landscapeTextSignature = landscapeTextSignature;
		MetrixFieldManager.landscapeListSignature = landscapeListSignature;
		MetrixFieldManager.landscapeListvalueSignature = landscapeListvalueSignature;
		MetrixFieldManager.landscapeButtonSignature = landscapeButtonSignature;
		MetrixFieldManager.landscapeMetrixHyperlinkSignature = landscapeMetrixHyperlinkSignature;

		MetrixFieldManager.landscapeSignatureCheckBox = landscapeSignatureCheckBox;
		MetrixFieldManager.landscapeSignatureReadOnly = landscapeSignatureReadOnly;
		MetrixFieldManager.landscapeSignatureText = landscapeSignatureText;
		MetrixFieldManager.landscapeSignatureList = landscapeSignatureList;
		MetrixFieldManager.landscapeSignatureListvalue = landscapeSignatureListvalue;
		MetrixFieldManager.landscapeSignatureButton = landscapeSignatureButton;
		MetrixFieldManager.landscapeSignatureMetrixHyperlink = landscapeSignatureMetrixHyperlink;
		MetrixFieldManager.landscapeSignatureSignature = landscapeSignatureSignature;
		MetrixFieldManager.landscapeSignatureAttachment = landscapeSignatureAttachment;
	}
	
	/**
	 * Added fields dynamically to screens based on the Designer meta-data (currently assumes one workflow per field).
	 * 
	 * @param activity The current activity.
	 * @param layout The current activity's layout.
	 * @param formDef The current activity's meta-data form definition.
	 * 
	 * @since 5.6.1
	 */
	public static void addFieldsToScreen(Activity activity, ViewGroup layout, MetrixFormDef formDef) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		addFieldsToScreen(activity, layout, formDef, screenId);
	}
	
	/**
	 * Added fields dynamically to screens based on the Designer meta-data (currently assumes one workflow per field).
	 * 
	 * @param activity The current activity.
	 * @param layout The current activity's layout.
	 * @param formDef The current activity's meta-data form definition.
	 * @param screenId The current screen id.
	 * 
	 * @since 5.6.3
	 */
	public static void addFieldsToScreen(Activity activity, ViewGroup layout, MetrixFormDef formDef, int screenId) {
		StopWatch swatch = null;
		StopWatch querySwatch = null;
		StopWatch controlSwatch = null;
		
		long elapsedQuerySwatch = 0;
		long elapsedControlSwatch = 0;
		long initialElapsedQuerySwatch = 0;

		if (BuildConfig.DEBUG) {
			swatch = new StopWatch();
			swatch.start();
		
			querySwatch = new StopWatch();
			controlSwatch = new StopWatch();
		}
		
		if (activity == null)
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		if (layout == null)
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		if (formDef == null)
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
		
		Boolean portraitMode = true;
		ArrayList<MetrixColumnDef> landscapeColumnDefs = null;
		HashMap<String, Object> landscapeProperties = null;
		
		if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (MetrixScreenManager.shouldRunTabletSpecificLandUI(activity)) {
				//Forcefully changing the orientation mode to PORTRAIT - in-order to utilize the limited space
				portraitMode = true;
			} else {
				portraitMode = false;
				landscapeColumnDefs = new ArrayList<MetrixColumnDef>();
				landscapeProperties = new HashMap<String, Object>();
			}
		}
		
		MetrixCursor fieldCursor = null;
		MetrixCursor propertyCursor = null;
		HashMap<String, MetrixTableStructure> tableStructures = MobileApplication.getTableDefinitionsFromCache();
		
		try {
			String query = "SELECT use_mm_field.field_id, lower(use_mm_field.table_name), lower(use_mm_field.column_name), use_mm_field.region, use_mm_field.display_order " 
						+ " FROM use_mm_field WHERE use_mm_field.screen_id = " + screenId 
						+ " ORDER BY region, display_order ASC";
			
			if (BuildConfig.DEBUG)
				querySwatch.start();
			
			fieldCursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (fieldCursor == null || !fieldCursor.moveToFirst())
				return;

			if (BuildConfig.DEBUG) {
				elapsedQuerySwatch = elapsedQuerySwatch + querySwatch.getElapsedTimeMilli();
				initialElapsedQuerySwatch = initialElapsedQuerySwatch + querySwatch.getElapsedTimeMilli();
				querySwatch.reset();
			}

			String tableName = null;
			String columnName = null;
			String region = null;

			try {
				final boolean hasMap = MetrixScreenManager.screenHasMap(screenId);
				if (hasMap) {
					// Making sure to insert the map widget after screen tip label
					final int childCount = layout.getChildCount();
					boolean mapInserted = false;
					final View mapWidget = LayoutInflater.from(activity).inflate(standardScreenMapWidget, layout, false);

					for (int i = 0; i < childCount; i++) {
						final Object viewTag = layout.getChildAt(i).getTag();
						if (viewTag instanceof String && MetrixStringHelper.valueIsEqual("SCREEN_TIP", (String) viewTag)) {
							layout.addView(mapWidget, i + 1);
							mapInserted = true;
							break;
						}
					}

					if (!mapInserted)
						layout.addView(mapWidget);
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}

			while (fieldCursor.isAfterLast() == false) {
				int fieldId = fieldCursor.getInt(0);
				
				tableName = fieldCursor.getString(1);
				columnName = fieldCursor.getString(2);
				region = fieldCursor.getString(3);
				if (region == null)
					region = "";
				
				// only process this field if the tableName corresponds to a table in the client DB
				if (!clientDBContainsTable(tableName)) {
					LogManager.getInstance().info(String.format("Table not found in client DB for the %1$s.%2$s field ... will not render.", tableName, columnName));
					fieldCursor.moveToNext();
					continue;
				}
				
				// only add a column def for metrix row id when the screen is binding existing data
				if (!MetrixStringHelper.isNullOrEmpty(columnName) && columnName.compareToIgnoreCase("metrix_row_id") == 0) {
					Boolean skip = false;
					for (MetrixTableDef tableDef : formDef.tables) {
						if (tableDef.tableName.compareToIgnoreCase(tableName) == 0)
						{
							if (tableDef.transactionType == MetrixTransactionTypes.INSERT) {
								skip = true;
								break;
							} else
								break;
						}
					}
					if (skip) {
						fieldCursor.moveToNext();
						continue;
					}
				}

				Boolean visible = false;
				String inputType = "";
				String controlType = "";
				String label = "";
				String listDisplayColumn = "";
				String listValueColumn = "";
				String listTableName = "";
				String listOrderBy = "";
				String listFilterColumn = "";
				String listFilterValue = "";
				String maxChars = "";
				String readOnly = "";
				String valueChangedEvent = "";
				String controlEvent = "";
				MetrixColumnDef columnDef = new MetrixColumnDef();

				columnDef.columnName = columnName;
				
				if (tableName.compareToIgnoreCase("CUSTOM") != 0 && tableStructures.get(tableName).isPrimaryKey(columnName))
					columnDef.primaryKey = true;
				else
					columnDef.primaryKey = false;

				String uniqueName = String.format("%s-%s", screenId, fieldId);
				if (MetrixFieldManager.fieldProperties != null && MetrixFieldManager.fieldProperties.containsKey(uniqueName)) {
					Map<String, String> fieldProperties = MetrixFieldManager.fieldProperties.get(uniqueName);
					
					if (fieldProperties.get(VISIBLE).compareToIgnoreCase("N") == 0)
						visible = false;
					else
						visible = true;

					if (fieldProperties.get(REQUIRED).compareToIgnoreCase("N") == 0)
						columnDef.required = false;
					else
						columnDef.required = true;

					columnDef.validation = fieldProperties.get(VALIDATION);
					inputType = fieldProperties.get(INPUT_TYPE);
					setDataType(fieldProperties.get(DATA_TYPE), columnDef);
					setForceCase(fieldProperties.get(FORCE_CASE), columnDef);
					controlType = fieldProperties.get(CONTROL_TYPE);
					label = fieldProperties.get(LABEL);
					columnDef.friendlyName = fieldProperties.get(LABEL);
					listDisplayColumn = fieldProperties.get(LIST_DISPLAY_COLUMN);
					listValueColumn = fieldProperties.get(LIST_VALUE_COLUMN);
					listTableName = fieldProperties.get(LIST_TABLE_NAME);
					listOrderBy = fieldProperties.get(LIST_ORDER_BY);
					listFilterColumn = fieldProperties.get(LIST_FILTER_COLUMN);
					listFilterValue = fieldProperties.get(LIST_FILTER_VALUE);
					maxChars = fieldProperties.get(MAX_CHARS);
					readOnly = fieldProperties.get(READ_ONLY);
					valueChangedEvent = fieldProperties.get(VALUE_CHANGED_EVENT);
					controlEvent = fieldProperties.get(CONTROL_EVENT);

					if (MetrixStringHelper.valueIsEqual(controlType.toUpperCase(), "ATTACHMENT")) {
						columnDef.controlType = controlType;
						columnDef.readOnlyInMetadata = (readOnly.compareToIgnoreCase("Y") == 0);
						columnDef.allowPhoto = (fieldProperties.get(ALLOW_PHOTO).compareToIgnoreCase("Y") == 0);
						columnDef.allowVideo = (fieldProperties.get(ALLOW_VIDEO).compareToIgnoreCase("Y") == 0);
						columnDef.allowFile = (fieldProperties.get(ALLOW_FILE).compareToIgnoreCase("Y") == 0);
						String cardScreenIDString = fieldProperties.get(CARD_SCREEN_ID);
						if (!MetrixStringHelper.isNullOrEmpty(cardScreenIDString))
							columnDef.cardScreenID = Integer.valueOf(cardScreenIDString);
						columnDef.transactionIdTableName = fieldProperties.get(TRANSACTION_TABLE);
						columnDef.transactionIdColumnName = fieldProperties.get(TRANSACTION_COLUMN);
					} else if(MetrixStringHelper.valueIsEqual(controlType.toUpperCase(), "SIGNATURE")) {
						columnDef.controlType = controlType;
						columnDef.readOnlyInMetadata = (readOnly.compareToIgnoreCase("Y") == 0);
						columnDef.allowClear = (fieldProperties.get(ALLOW_CLEAR).compareToIgnoreCase("Y") == 0);
						columnDef.signerColumn = (fieldProperties.get(SIGNER_COLUMN));
						columnDef.messageId = (fieldProperties.get(SIGNER_MESSAGE_ID));
						columnDef.transactionIdTableName = fieldProperties.get(TRANSACTION_TABLE);
						columnDef.transactionIdColumnName = fieldProperties.get(TRANSACTION_COLUMN);
					}
				} else {										
					query = "SELECT use_mm_field.control_type, use_mm_field.data_type, use_mm_field.force_case, use_mm_field.input_type,"
							+ " use_mm_field.label, use_mm_field.list_display_column, use_mm_field.list_filter_column, use_mm_field.list_filter_value,"
							+ " use_mm_field.list_order_by, use_mm_field.list_table_name, use_mm_field.list_value_column,"
							+ " use_mm_field.required, use_mm_field.validation, use_mm_field.visible, use_mm_field.read_only,"
							+ " use_mm_field.value_changed_event, use_mm_field.max_chars, use_mm_field.control_event,"
							+ " use_mm_field.allow_file, use_mm_field.allow_photo, use_mm_field.allow_video,"
							+ " use_mm_field.card_screen_id, use_mm_field.transaction_table, use_mm_field.transaction_column,"
							+ " use_mm_field.allow_clear, use_mm_field.signer_column, use_mm_field.sign_message_id"
							+ " FROM use_mm_field WHERE use_mm_field.field_id = " + fieldId;
					
					if (BuildConfig.DEBUG)
						querySwatch.start();	
					
					try {
						propertyCursor = MetrixDatabaseManager.rawQueryMC(query, null);
						
						if (propertyCursor == null || !propertyCursor.moveToFirst()) {
							fieldCursor.moveToNext();
							continue;
						}

						Map<String, String> fieldProperties = new LinkedHashMap<String, String>();

						if (BuildConfig.DEBUG) {
							elapsedQuerySwatch = elapsedQuerySwatch + querySwatch.getElapsedTimeMilli();
							querySwatch.reset();
						}
						
						while (propertyCursor.isAfterLast() == false) {
							String testString = propertyCursor.getString(0);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								controlType = testString;
								fieldProperties.put(CONTROL_TYPE, testString);
							}

							testString = propertyCursor.getString(1);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(DATA_TYPE, testString);
								setDataType(testString, columnDef);
							}

							testString = propertyCursor.getString(2);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(FORCE_CASE, testString);
								setForceCase(testString, columnDef);
							}

							testString = propertyCursor.getString(3);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(INPUT_TYPE, testString);
								inputType = testString;
							}

							testString = propertyCursor.getString(4);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LABEL, testString);
								label = testString;
								columnDef.friendlyName = label;
							}

							testString = propertyCursor.getString(5);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LIST_DISPLAY_COLUMN, testString);
								listDisplayColumn = testString;
							}

							testString = propertyCursor.getString(6);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LIST_FILTER_COLUMN, testString);
								listFilterColumn = testString;
							}

							testString = propertyCursor.getString(7);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LIST_FILTER_VALUE, testString);
								listFilterValue = testString;
							}

							testString = propertyCursor.getString(8);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LIST_ORDER_BY, testString);
								listOrderBy = testString;
							}

							testString = propertyCursor.getString(9);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LIST_TABLE_NAME, testString);
								listTableName = testString;
							}

							testString = propertyCursor.getString(10);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(LIST_VALUE_COLUMN, testString);
								listValueColumn = testString;
							}

							testString = propertyCursor.getString(11);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(REQUIRED, testString);
								if (testString.compareToIgnoreCase("N") == 0) {
									columnDef.required = false;
								} else {
									columnDef.required = true;
								}
							} else {
								fieldProperties.put(REQUIRED, "N");
								columnDef.required = false;
							}

							testString = propertyCursor.getString(12);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(VALIDATION, testString);
								columnDef.validation = testString;
							}

							testString = propertyCursor.getString(13);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(VISIBLE, testString);
								if (testString.compareToIgnoreCase("N") == 0)
									visible = false;
								else
									visible = true;
							} else {
								fieldProperties.put(VISIBLE, "N");
								visible = false;
							}

							testString = propertyCursor.getString(14);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(READ_ONLY, testString);
								readOnly = testString;
							} else {
								fieldProperties.put(READ_ONLY, "N");
								readOnly = "N";
							}

							testString = propertyCursor.getString(15);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(VALUE_CHANGED_EVENT, testString);
								valueChangedEvent = testString;
							}

							testString = propertyCursor.getString(16);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(MAX_CHARS, testString);
								maxChars = testString;
							}

							testString = propertyCursor.getString(17);
							if (!MetrixStringHelper.isNullOrEmpty(testString)) {
								fieldProperties.put(CONTROL_EVENT, testString);
								controlEvent = testString;
							}

							//region #Handle Attachment Field properties, ensuring they will exist in cache for future use and setting up ColumnDef
							if (MetrixStringHelper.valueIsEqual(controlType.toUpperCase(), "ATTACHMENT")) {
								columnDef.controlType = controlType;
								columnDef.readOnlyInMetadata = (readOnly.compareToIgnoreCase("Y") == 0);

								testString = propertyCursor.getString(18);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(ALLOW_FILE, testString);
									columnDef.allowFile = (testString.compareToIgnoreCase("Y") == 0);
								}

								testString = propertyCursor.getString(19);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(ALLOW_PHOTO, testString);
									columnDef.allowPhoto = (testString.compareToIgnoreCase("Y") == 0);
								}

								testString = propertyCursor.getString(20);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(ALLOW_VIDEO, testString);
									columnDef.allowVideo = (testString.compareToIgnoreCase("Y") == 0);
								}

								testString = propertyCursor.getString(21);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(CARD_SCREEN_ID, testString);
									if (!MetrixStringHelper.isNullOrEmpty(testString))
										columnDef.cardScreenID = Integer.valueOf(testString);
								}

								testString = propertyCursor.getString(22);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(TRANSACTION_TABLE, testString);
									columnDef.transactionIdTableName = testString;
								}

								testString = propertyCursor.getString(23);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(TRANSACTION_COLUMN, testString);
									columnDef.transactionIdColumnName = testString;
								}
							}
							//endregion

							//region #Handle Signature Field properties, ensuring they will exist in cache for future use and setting up ColumnDef
							if (MetrixStringHelper.valueIsEqual(controlType.toUpperCase(), "SIGNATURE")) {
								columnDef.controlType = controlType;
								columnDef.readOnlyInMetadata = (readOnly.compareToIgnoreCase("Y") == 0);

								testString = propertyCursor.getString(22);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(TRANSACTION_TABLE, testString);
									columnDef.transactionIdTableName = testString;
								}

								testString = propertyCursor.getString(23);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(TRANSACTION_COLUMN, testString);
									columnDef.transactionIdColumnName = testString;
								}

								testString = propertyCursor.getString(24);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(ALLOW_CLEAR, testString);
									columnDef.allowClear = (testString.compareToIgnoreCase("Y") == 0);
								}

								testString = propertyCursor.getString(25);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(SIGNER_COLUMN, testString);
									columnDef.signerColumn = testString;
								}

								testString = propertyCursor.getString(26);
								if (!MetrixStringHelper.isNullOrEmpty(testString)) {
									fieldProperties.put(SIGNER_MESSAGE_ID, testString);
									columnDef.messageId = testString;
								}
							}
							//endregion

							// don't use LIST properties or render as LIST, if listTableName not in client DB
							if (!MetrixStringHelper.isNullOrEmpty(listTableName) && !clientDBContainsTable(listTableName)) {
								LogManager.getInstance().info(String.format("List table name (%1$s) not found in client DB for the %2$s.%3$s field ... will render without using list properties.", listTableName, tableName, columnName));
								listDisplayColumn = listValueColumn = listTableName = listOrderBy = listFilterColumn = listFilterValue = "";
								fieldProperties.put(LIST_DISPLAY_COLUMN, listDisplayColumn);
								fieldProperties.put(LIST_FILTER_COLUMN, listFilterColumn);
								fieldProperties.put(LIST_FILTER_VALUE, listFilterValue);
								fieldProperties.put(LIST_ORDER_BY, listOrderBy);
								fieldProperties.put(LIST_TABLE_NAME, listTableName);
								fieldProperties.put(LIST_VALUE_COLUMN, listValueColumn);
								if (MetrixStringHelper.valueIsEqual(controlType, "LIST")) {
									controlType = "TEXT";
									fieldProperties.put(CONTROL_TYPE, controlType);
								}
							}
							
							MetrixFieldManager.fieldProperties.put(uniqueName, fieldProperties);
							
							if (BuildConfig.DEBUG)
								querySwatch.reset();

							propertyCursor.moveToNext();
							if (BuildConfig.DEBUG) {
								elapsedQuerySwatch = elapsedQuerySwatch + querySwatch.getElapsedTimeMilli();
								querySwatch.reset();
							}
						}
					} finally {
						if (propertyCursor != null && (!propertyCursor.isClosed()))
							propertyCursor.close();
					}
				}
				
				populateLookupMetaData(listDisplayColumn, listValueColumn, listTableName, listOrderBy, listFilterColumn, listFilterValue, columnDef);
				
				if (portraitMode || (visible == false)) {
					if (BuildConfig.DEBUG)
						controlSwatch.start();
					
					addPortraitLayout(activity, layout, visible, inputType, controlType, label, columnDef, region, readOnly, valueChangedEvent, fieldId, tableName, maxChars, controlEvent);
					if (BuildConfig.DEBUG) {
						elapsedControlSwatch = elapsedControlSwatch + controlSwatch.getElapsedTimeMilli();
						controlSwatch.reset();
					}
					addColumnDefToTableDef(formDef, tableName, columnDef);
				} else {
					if (landscapeColumnDefs.size() == 1) {
						if (region.compareToIgnoreCase((String)landscapeProperties.get("region")) == 0) {
							if (BuildConfig.DEBUG)
								controlSwatch.start();
							
							addLandscapeLayout(activity, layout, landscapeColumnDefs.get(0), (Boolean)landscapeProperties.get("visible"), (String)landscapeProperties.get("inputType"), (String)landscapeProperties.get("controlType"), (String)landscapeProperties.get("label"), columnDef, visible, inputType, controlType, label, region, (String)landscapeProperties.get("readOnly"), readOnly, (String)landscapeProperties.get("valueChangedEvent"), valueChangedEvent, (Integer)landscapeProperties.get("fieldId"), fieldId, (String)landscapeProperties.get("tableName"), tableName, (String)landscapeProperties.get("maxChars"), maxChars, (String)landscapeProperties.get("controlEvent"), controlEvent);
							if (BuildConfig.DEBUG) {
								elapsedControlSwatch = elapsedControlSwatch + controlSwatch.getElapsedTimeMilli();
								controlSwatch.reset();
							}
							addColumnDefToTableDef(formDef, (String)landscapeProperties.get("tableName"), landscapeColumnDefs.get(0));
							addColumnDefToTableDef(formDef, tableName, columnDef);
							landscapeColumnDefs.clear();
							landscapeProperties.clear();							
						} else {
							if (BuildConfig.DEBUG)
								controlSwatch.start();

							addLandscapeLayout(activity, layout, landscapeColumnDefs.get(0), (Boolean)landscapeProperties.get("visible"), (String)landscapeProperties.get("inputType"), (String)landscapeProperties.get("controlType"), (String)landscapeProperties.get("label"), (String)landscapeProperties.get("region"), (String)landscapeProperties.get("readOnly"), (String)landscapeProperties.get("valueChangedEvent"), (Integer)landscapeProperties.get("fieldId"), (String)landscapeProperties.get("tableName"), (String)landscapeProperties.get("maxChars"), (String)landscapeProperties.get("controlEvent"));
							if (BuildConfig.DEBUG) {
								elapsedControlSwatch = elapsedControlSwatch + controlSwatch.getElapsedTimeMilli();
								controlSwatch.reset();
							}
							addColumnDefToTableDef(formDef, (String)landscapeProperties.get("tableName"), landscapeColumnDefs.get(0));
							landscapeColumnDefs.clear();
							landscapeProperties.clear();							
							landscapeColumnDefs.add(columnDef);
							landscapeProperties.put("fieldId", fieldId);
							landscapeProperties.put("visible", visible);
							landscapeProperties.put("inputType", inputType);
							landscapeProperties.put("controlType", controlType);
							landscapeProperties.put("label", label);
							landscapeProperties.put("tableName", tableName);
							landscapeProperties.put("region", region);
							landscapeProperties.put("readOnly", readOnly);
							landscapeProperties.put("valueChangedEvent", valueChangedEvent);
							landscapeProperties.put("maxChars", maxChars);
							landscapeProperties.put("controlEvent", controlEvent);
						}
					} else {
						landscapeColumnDefs.add(columnDef);
						landscapeProperties.put("fieldId", fieldId);
						landscapeProperties.put("visible", visible);
						landscapeProperties.put("inputType", inputType);
						landscapeProperties.put("controlType", controlType);
						landscapeProperties.put("label", label);
						landscapeProperties.put("tableName", tableName);
						landscapeProperties.put("region", region);
						landscapeProperties.put("readOnly", readOnly);
						landscapeProperties.put("valueChangedEvent", valueChangedEvent);
						landscapeProperties.put("maxChars", maxChars);
						landscapeProperties.put("controlEvent", controlEvent);
					}
				}
				
				if (BuildConfig.DEBUG)
					querySwatch.start();

				fieldCursor.moveToNext();
				if (BuildConfig.DEBUG) {
					elapsedQuerySwatch = elapsedQuerySwatch + querySwatch.getElapsedTimeMilli();
					initialElapsedQuerySwatch = initialElapsedQuerySwatch + querySwatch.getElapsedTimeMilli();
					querySwatch.reset();
				}
			}

			if (landscapeColumnDefs != null && landscapeColumnDefs.size() == 1) {
				if (BuildConfig.DEBUG)
					controlSwatch.start();

				addLandscapeLayout(activity, layout, landscapeColumnDefs.get(0), (Boolean)landscapeProperties.get("visible"), (String)landscapeProperties.get("inputType"), (String)landscapeProperties.get("controlType"), (String)landscapeProperties.get("label"), (String)landscapeProperties.get("region"), (String)landscapeProperties.get("readOnly"), (String)landscapeProperties.get("valueChangedEvent"), (Integer)landscapeProperties.get("fieldId"), (String)landscapeProperties.get("tableName"), (String)landscapeProperties.get("maxChars"), (String)landscapeProperties.get("controlEvent"));
				if (BuildConfig.DEBUG) {
					elapsedControlSwatch = elapsedControlSwatch + controlSwatch.getElapsedTimeMilli();
					controlSwatch.reset();
				}
				addColumnDefToTableDef(formDef, (String)landscapeProperties.get("tableName"), landscapeColumnDefs.get(0));
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (fieldCursor != null && (!fieldCursor.isClosed()))
				fieldCursor.close(); 
			if (propertyCursor != null && (!propertyCursor.isClosed()))
				propertyCursor.close();
		}
		
		if (BuildConfig.DEBUG) {
			Log.d("TIMING", "Total: " + swatch.getElapsedTimeMilli());
			Log.d("TIMING", "Query: " + elapsedQuerySwatch);
			Log.d("TIMING", "First Query: " + initialElapsedQuerySwatch);
			Log.d("TIMING", "Controls: " + elapsedControlSwatch);
		}
	}
	
	private static void setForceCase(String forceCase, MetrixColumnDef columnDef) {
		if (!MetrixStringHelper.isNullOrEmpty(forceCase)) {
			if (forceCase.compareToIgnoreCase("upper") == 0)
				columnDef.forceCase = MetrixControlCase.UPPER;
			else if (forceCase.compareToIgnoreCase("lower") == 0)
				columnDef.forceCase = MetrixControlCase.LOWER;
		}
	}

	private static void setDataType(String dataType, MetrixColumnDef columnDef) {
		if (!MetrixStringHelper.isNullOrEmpty(dataType)) {
			if (dataType.compareToIgnoreCase(DATA_TYPE_STRING) == 0)
				columnDef.dataType = String.class;
			else if (dataType.compareToIgnoreCase(DATA_TYPE_NUMBER) == 0)
				columnDef.dataType = double.class;
			else if (dataType.compareToIgnoreCase(DATA_TYPE_DATE) == 0)
				columnDef.dataType = MetrixDate.class;
			else if (dataType.compareToIgnoreCase(DATA_TYPE_TIME) == 0)
				columnDef.dataType = MetrixTime.class;
			else if (dataType.compareToIgnoreCase(DATA_TYPE_DATE_TIME) == 0)
				columnDef.dataType = MetrixDateTime.class;
		}
	}

	private static void populateLookupMetaData(String listDisplayColumn, String listValueColumn, String listTableName, String listOrderBy, String listFilterColumn, String listFilterValue, MetrixColumnDef columnDef) {
		if ((!MetrixStringHelper.isNullOrEmpty(listDisplayColumn)) && (!MetrixStringHelper.isNullOrEmpty(listValueColumn)) && (!MetrixStringHelper.isNullOrEmpty(listTableName))) {
			MetrixDropDownDef lookupDef = new MetrixDropDownDef();
			// working with a bug in the Architecture which flips the display and value columns around
			lookupDef.valueColumn = listDisplayColumn;
			lookupDef.displayColumn = listValueColumn;
			lookupDef.tableName = listTableName;
		
			if (!MetrixStringHelper.isNullOrEmpty(listOrderBy)) {
				MetrixOrderByDef orderBy = new MetrixOrderByDef();
				orderBy.columnName = listOrderBy;
				orderBy.sortOrder = "ASC";
				lookupDef.orderBy = orderBy;
			}
		
			if ((!MetrixStringHelper.isNullOrEmpty(listFilterColumn)) &&
				(!MetrixStringHelper.isNullOrEmpty(listFilterValue))) {
				lookupDef.filterValue = listFilterValue;
				lookupDef.filterColumn = listFilterColumn;
			}
		
			columnDef.lookupDef = lookupDef;
		}
	}

	private static void setInputTypeOnText(ViewGroup layout, String inputType, EditText editText, Type dataType) {
		if (dataType == MetrixDate.class || dataType == MetrixTime.class || dataType == MetrixDateTime.class) {
			editText.setInputType(InputType.TYPE_NULL);
			return;
		}
		if (!MetrixStringHelper.isNullOrEmpty(inputType)) {
			if (inputType.compareToIgnoreCase(INPUT_TYPE_PASSWORD) == 0)
				MetrixControlAssistant.addInputType(editText.getId(), layout, InputType.TYPE_TEXT_VARIATION_PASSWORD);
			else if (inputType.compareToIgnoreCase(INPUT_TYPE_PHONE) == 0)
				MetrixControlAssistant.addInputType(editText.getId(), layout, InputType.TYPE_CLASS_PHONE);
			else if (inputType.compareToIgnoreCase(INPUT_TYPE_EMAIL_ADDRESS) == 0)
				MetrixControlAssistant.addInputType(editText.getId(), layout, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			else if (inputType.compareToIgnoreCase(INPUT_TYPE_NUMBER) == 0) {
				MetrixControlAssistant.addInputType(editText.getId(), layout, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				MetrixFieldManager fieldManager = new MetrixFieldManager();
				DecimalKeyListener mDecimalKeyListener = fieldManager.new DecimalKeyListener();
				editText.setKeyListener(mDecimalKeyListener);
			} 
		}
	}
	
	private static void setMaxCharsOnText(String maxChars, EditText editText) {
		if (!MetrixStringHelper.isNullOrEmpty(maxChars)) {
			int maxLength = Integer.valueOf(maxChars);
			InputFilter lengthFilter = new InputFilter.LengthFilter(maxLength);
			InputFilter[] currFilters = editText.getFilters();
			if (currFilters != null && currFilters.length > 0) {
				InputFilter[] newFilters = new InputFilter[(currFilters.length + 1)];
				for (int i = 0; i < currFilters.length; i++) {
					newFilters[i] = currFilters[i];
				}
				newFilters[currFilters.length] = lengthFilter;
				editText.setFilters(newFilters);
			} else {				
				currFilters = new InputFilter[1];
				currFilters[0] = lengthFilter;
				editText.setFilters(currFilters);
			}
		}
	}

	class DecimalKeyListener extends DigitsKeyListener {
		private final char[] acceptedCharacters = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', new DecimalFormatSymbols().getDecimalSeparator()};

		@Override
		protected char[] getAcceptedChars() { return acceptedCharacters; }

		public int getInputType() { return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL; }
	}
	
	class WholeNumberKeyListener extends DigitsKeyListener {
		private final char[] acceptedCharacters = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

		@Override
		protected char[] getAcceptedChars() { return acceptedCharacters; }

		public int getInputType() { return InputType.TYPE_CLASS_NUMBER; }
	}

    public static void setEnableStatus(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setFocusable(enabled);
		v.setLongClickable(false);

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++)
                setEnableStatus(vg.getChildAt(i), enabled);
        }
    }

	private static void addPortraitLayout(Activity activity, ViewGroup layout, Boolean visible, String inputType, String controlType, String label, MetrixColumnDef columnDef, String region, String readOnly, String valueChangedEvent, Integer fieldId, String tableName, String maxChars, String controlEvent) throws Exception {
		LinearLayout layoutAdded = null;
		
		if (!MetrixStringHelper.isNullOrEmpty(controlType)) {
			if (controlType.compareToIgnoreCase(CHECKBOX) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitCheckBox, layout, region);
			else if (controlType.compareToIgnoreCase(TEXT) == 0 || controlType.compareToIgnoreCase(LONG_TEXT) == 0) {
				if (readOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitReadOnly, layout, region);					
				else {				
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitText, layout, region);
					setInputTypeOnText(layout, inputType, (EditText)layoutAdded.getChildAt(1), columnDef.dataType);
					setMaxCharsOnText(maxChars, (EditText)layoutAdded.getChildAt(1));
				}			
			} else if (controlType.compareToIgnoreCase(LIST) == 0) {
				if (readOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitListvalue, layout, region);
				else
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitList, layout, region);
			} else if (controlType.compareToIgnoreCase(BUTTON) == 0) {
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitButton, layout, region);
				MetrixControlAssistant.setValue((Button) layoutAdded.getChildAt(1), label);
			} else if (controlType.compareToIgnoreCase(HYPERLINK) == 0 || controlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitMetrixHyperlink, layout, region);
			else if (controlType.compareToIgnoreCase(ATTACHMENT) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitAttachment, layout, region);
			else if(controlType.compareToIgnoreCase(SIGNATURE) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.portraitSignature, layout, region);
		}

		if (!visible)
			layoutAdded.setVisibility(View.GONE);
		
		columnDef.fieldId = fieldId;
		columnDef.labelId = layoutAdded.getChildAt(0).getId();
		columnDef.id = layoutAdded.getChildAt(1).getId();
		columnDef.controlType = controlType;
		View controlAdd = MetrixControlAssistant.getControl(columnDef, layoutAdded);
		if(controlAdd != null) {
			controlAdd.setContentDescription(tableName + "__" + columnDef.columnName);
		}

		// Handle read-only
		if (readOnly.compareToIgnoreCase("Y") == 0) {
			columnDef.id = layoutAdded.getChildAt(1).getId();
			View control = MetrixControlAssistant.getControl(columnDef, layoutAdded);

			if (control != null) {
				//even-though READ_ONLY = Y, we skip disabling Hyperlinks and Attachment Fields since they need to be serviceable(Ex: click events)
				if (!(control instanceof MetrixHyperlink) && !(control instanceof AttachmentField) && !(control instanceof SignatureField) && !(control instanceof TextView))
					setEnableStatus(control, false);

				if (control instanceof EditText){
					control.setClickable(false);
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
				}
				else if (control instanceof Spinner)
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
			}
		}

		if (controlType.compareToIgnoreCase(BUTTON) != 0)
			MetrixControlAssistant.setValue((TextView)layoutAdded.getChildAt(0), label);
		
		if (!MetrixStringHelper.isNullOrEmpty(valueChangedEvent))
			setUpValueChangedEvent(activity, layoutAdded.getChildAt(1), valueChangedEvent, tableName, columnDef.columnName);
		
		if (controlType.compareToIgnoreCase(TEXT) == 0 && readOnly.compareToIgnoreCase("Y") != 0 && MetrixFieldLookupManager.fieldHasLookup(fieldId)) {
			if(controlAdd != null) {
				controlAdd.setContentDescription("Lookup_"+tableName + "__" + columnDef.columnName);
			}

			setUpFieldLookup(activity, layoutAdded.getChildAt(1), fieldId);
		}

		if (!MetrixStringHelper.isNullOrEmpty(controlEvent))
			MetrixControlAssistant.setUpControlEvent(activity, layoutAdded.getChildAt(1), controlEvent);
	}

	private static void addLandscapeLayout(Activity activity, ViewGroup layout, MetrixColumnDef firstColumnDef, Boolean firstVisible, String firstInputType, String firstControlType,
										   String firstLabel, String region, String readOnly, String valueChangedEvent, Integer fieldId, String tableName, String maxChars, String controlEvent) throws Exception {
		// This method exists to put a field at the bottom of a region or screen in landscape mode when there is only one field to show.
		// We achieve this by using a landscape layout of the first control type with (usually) a read-only field and then hide the right-side UI elements.
		LinearLayout layoutAdded = null;
		LinearLayout childLinearLayout = null;
		
		if (firstControlType.compareToIgnoreCase(CHECKBOX) == 0)
			layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxReadOnly, layout, region);		
		else if (firstControlType.compareToIgnoreCase(TEXT) == 0 || firstControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
			if (readOnly.compareToIgnoreCase("Y") == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyReadOnly, layout, region);					
			else {
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextText, layout, region);
				if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
					childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
					setInputTypeOnText(layout, firstInputType, (EditText)childLinearLayout.getChildAt(1), firstColumnDef.dataType);
					setMaxCharsOnText(maxChars, (EditText)childLinearLayout.getChildAt(1));
				} else {
					setInputTypeOnText(layout, firstInputType, (EditText)layoutAdded.getChildAt(1), firstColumnDef.dataType);
					setMaxCharsOnText(maxChars, (EditText)layoutAdded.getChildAt(1));
				}
			}
		} else if (firstControlType.compareToIgnoreCase(LIST) == 0) {
			if (readOnly.compareToIgnoreCase("Y") == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueReadOnly, layout, region);
			else
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListReadOnly, layout, region);
		} else if (firstControlType.compareToIgnoreCase(BUTTON) == 0) {
			layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonReadOnly, layout, region);
		}
		else if (firstControlType.compareToIgnoreCase(HYPERLINK) == 0 || firstControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
			layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkReadOnly, layout, region);
		else if (firstControlType.compareToIgnoreCase(ATTACHMENT) == 0)
			layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentReadOnly, layout, region);
		else if(firstControlType.compareToIgnoreCase(SIGNATURE) == 0)
			layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureReadOnly, layout, region);

		firstColumnDef.fieldId = fieldId;
		if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
			childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
			firstColumnDef.labelId = childLinearLayout.getChildAt(0).getId();
			firstColumnDef.id = childLinearLayout.getChildAt(1).getId();
		} else {
			firstColumnDef.labelId = layoutAdded.getChildAt(0).getId();
			firstColumnDef.id = layoutAdded.getChildAt(1).getId();
		}

		// Handle read-only
		if (readOnly.compareToIgnoreCase("Y") == 0) {			
			View control = MetrixControlAssistant.getControl(firstColumnDef, layoutAdded);
			if (control != null) {
				control.setContentDescription(tableName+"__"+firstColumnDef.columnName);
				//even-though READ_ONLY = Y, we skip disabling Hyperlinks since they need to be serviceable(Ex: click events)
				if (!(control instanceof MetrixHyperlink) && !(control instanceof AttachmentField) && !(control instanceof SignatureField) && !(control instanceof TextView))
					setEnableStatus(control,false);
				
				if (control instanceof EditText) {
					control.setClickable(false);
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
				} else if (control instanceof Spinner)
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
			}
		}

		View controlAdd = MetrixControlAssistant.getControl(firstColumnDef, layoutAdded);
		if(controlAdd != null)
			controlAdd.setContentDescription(tableName+"__"+firstColumnDef.columnName);

		//region #Handle label text
		if (firstControlType.compareToIgnoreCase(BUTTON) != 0) {
			if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
				childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
				MetrixControlAssistant.setValue((TextView) childLinearLayout.getChildAt(0), firstLabel);
			} else {
				MetrixControlAssistant.setValue((TextView) layoutAdded.getChildAt(0), firstLabel);
			}
		} else {
			if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
				childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
				MetrixControlAssistant.setValue((Button) childLinearLayout.getChildAt(1), firstLabel);
			} else {
				MetrixControlAssistant.setValue((Button) layoutAdded.getChildAt(1), firstLabel);
			}
		}
		//endregion

		// Handle value changed event
		if (!MetrixStringHelper.isNullOrEmpty(valueChangedEvent)) {
			if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
				childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
				setUpValueChangedEvent(activity, childLinearLayout.getChildAt(1), valueChangedEvent, tableName, firstColumnDef.columnName);
			} else {
				setUpValueChangedEvent(activity, layoutAdded.getChildAt(1), valueChangedEvent, tableName, firstColumnDef.columnName);
			}
		}

		// Handle field lookup
		if (firstControlType.compareToIgnoreCase(TEXT) == 0 && readOnly.compareToIgnoreCase("Y") != 0 && MetrixFieldLookupManager.fieldHasLookup(fieldId)) {
			if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
				childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
				setUpFieldLookup(activity, childLinearLayout.getChildAt(1), fieldId);
			} else {
				setUpFieldLookup(activity, layoutAdded.getChildAt(1), fieldId);
			}

			if(controlAdd != null) {
				controlAdd.setContentDescription("Lookup_"+tableName + "__" + firstColumnDef.columnName);
			}
		}

		// Handle control event
		if (!MetrixStringHelper.isNullOrEmpty(controlEvent)) {
			if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
				childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
				MetrixControlAssistant.setUpControlEvent(activity, childLinearLayout.getChildAt(1), controlEvent);
			} else {
				MetrixControlAssistant.setUpControlEvent(activity, layoutAdded.getChildAt(1), controlEvent);
			}
		}

		// Force right-side UI elements from inflated layout to disappear
		if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
			layoutAdded.getChildAt(1).setVisibility(View.GONE);
		} else {
			layoutAdded.getChildAt(2).setVisibility(View.GONE);
			layoutAdded.getChildAt(3).setVisibility(View.GONE);
		}
	}
	
	private static void addLandscapeLayout(Activity activity, ViewGroup layout, MetrixColumnDef firstColumnDef, Boolean firstVisible, String firstInputType, String firstControlType, String firstLabel,
			MetrixColumnDef secondColumnDef, Boolean secondVisible, String secondInputType, String secondControlType, String secondLabel, String region, String firstReadOnly, String secondReadOnly, 
			String firstValueChangedEvent, String secondValueChangedEvent, Integer firstFieldId, Integer secondFieldId, String firstTableName, String secondTableName, String firstMaxChars, String secondMaxChars,
										   String firstControlEvent, String secondControlEvent) throws Exception {
		LinearLayout layoutAdded = null;
		if (firstControlType.compareToIgnoreCase(CHECKBOX) == 0) {
			//region #CHECKBOX
			if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxCheckBox, layout, region);
			else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxReadOnly, layout, region);
				else {
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxText, layout, region);
					if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
						LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
						setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
					} else {
						setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
					}
				}
			} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxListvalue, layout, region);
				else
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxList, layout, region);
			} else if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxButton, layout, region);
			else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxMetrixHyperlink, layout, region);
			else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxAttachment, layout, region);
			else if(secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeCheckBoxSignature, layout, region);
			//endregion
		} else if (firstControlType.compareToIgnoreCase(TEXT) == 0 || firstControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
			//region #TEXT, #LONG_TEXT
			if (firstReadOnly.compareToIgnoreCase("Y") == 0) {
				//region #READ_ONLY TEXT
				if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyCheckBox, layout, region);
				else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y" )== 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyReadOnly, layout, region);
					else {
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyText, layout, region);
						if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
							LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
							setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
						} else {
							setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
						}
					}
				} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyListvalue, layout, region);
					else
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyList, layout, region);
				} else if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyButton, layout, region);
				else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyMetrixHyperlink, layout, region);
				else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlyAttachment, layout, region);
				else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeReadOnlySignature, layout, region);
				//endregion
			} else {
				//region #NON-READ_ONLY TEXT
				if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextCheckBox, layout, region);							
				else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextReadOnly, layout, region);					
					else {
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextText, layout, region);
						if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
							LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
							setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
						} else {
							setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
						}
					}				
				} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextListvalue, layout, region);
					else
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextList, layout, region);
				} else if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextButton, layout, region);
				else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextMetrixHyperlink, layout, region);
				else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextAttachment, layout, region);
				else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeTextSignature, layout, region);

				if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
					LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
					setInputTypeOnText(layout, firstInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
					setMaxCharsOnText(firstMaxChars, (EditText) childLinearLayout.getChildAt(1));
				} else {
					setInputTypeOnText(layout, firstInputType, (EditText)layoutAdded.getChildAt(1), secondColumnDef.dataType);
					setMaxCharsOnText(firstMaxChars, (EditText)layoutAdded.getChildAt(1));
				}
				//endregion
			}
			//endregion
		} else if (firstControlType.compareToIgnoreCase(LIST) == 0) {
			//region #LIST
			if (firstReadOnly.compareToIgnoreCase("Y") == 0) {
				//region #READ_ONLY LIST
				if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueCheckBox, layout, region); 			
				else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueReadOnly, layout, region);					
					else {
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueText, layout, region);
						if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
							LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
							setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
						} else {
							setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
						}
					}
				} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueListvalue, layout, region);
					else 
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueList, layout, region);
				} else if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueButton, layout, region);
				else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueMetrixHyperlink, layout, region);
				else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueAttachment, layout, region);
				else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListvalueSignature, layout, region);
				//endregion
			} else {
				//region #NON-READ_ONLY LIST
				if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListCheckBox, layout, region);			
				else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListReadOnly, layout, region);					
					else {
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListText, layout, region);
						if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
							LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
							setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
						} else {
							setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
							setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
						}
					}
				} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
					if (secondReadOnly.compareToIgnoreCase("Y") == 0)
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListListvalue, layout, region);
					else 
						layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListList, layout, region);
				} else if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListButton, layout, region);
				else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListMetrixHyperlink, layout, region);
				else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListAttachment, layout, region);
				else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeListSignature, layout, region);
				//endregion
			}
			//endregion
		} else if (firstControlType.compareToIgnoreCase(BUTTON) == 0) {
			//region #BUTTON
			if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonButton, layout, region);
			else if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonCheckBox, layout, region);
			else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonReadOnly, layout, region);
				else {
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonText, layout, region);
					if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
						LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
						setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
					} else {
						setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
					}
				}
			} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonListvalue, layout, region);
				else
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonList, layout, region);
			}
			else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonMetrixHyperlink, layout, region);
			else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonAttachment, layout, region);
			else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeButtonSignature, layout, region);
			//endregion
		} else if (firstControlType.compareToIgnoreCase(HYPERLINK) == 0 || firstControlType.compareToIgnoreCase(LONGHYPERLINK) == 0) {
			//region #HYPERLINK, #LONG_HYPERLINK
			if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkButton, layout, region);
			else if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkCheckBox, layout, region);
			else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkReadOnly, layout, region);
				else {
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkText, layout, region);
					if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
						LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
						setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
					} else {
						setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
					}
				}
			} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkListvalue, layout, region);
				else
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkList, layout, region);
			}
			else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkMetrixHyperlink, layout, region);
			else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkAttachment, layout, region);
			else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeMetrixHyperlinkSignature, layout, region);
			//endregion
		} else if (firstControlType.compareToIgnoreCase(ATTACHMENT) == 0) {
			//region #ATTACHMENT
			if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentButton, layout, region);
			else if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentCheckBox, layout, region);
			else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentReadOnly, layout, region);
				else {
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentText, layout, region);
					if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
						LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
						setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
					} else {
						setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
					}
				}
			} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentListvalue, layout, region);
				else
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentList, layout, region);
			}
			else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentMetrixHyperlink, layout, region);
			else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentAttachment, layout, region);
			else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeAttachmentSignature, layout, region);
			//endregion
		} else if (firstControlType.compareToIgnoreCase(SIGNATURE) == 0) {
			//region #SIGNATURE
			if (secondControlType.compareToIgnoreCase(BUTTON) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureButton, layout, region);
			else if (secondControlType.compareToIgnoreCase(CHECKBOX) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureCheckBox, layout, region);
			else if (secondControlType.compareToIgnoreCase(TEXT) == 0 || secondControlType.compareToIgnoreCase(LONG_TEXT) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureReadOnly, layout, region);
				else {
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureText, layout, region);
					if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
						LinearLayout childLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
						setInputTypeOnText(layout, secondInputType, (EditText) childLinearLayout.getChildAt(1), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) childLinearLayout.getChildAt(1));
					} else {
						setInputTypeOnText(layout, secondInputType, (EditText) layoutAdded.getChildAt(3), secondColumnDef.dataType);
						setMaxCharsOnText(secondMaxChars, (EditText) layoutAdded.getChildAt(3));
					}
				}
			} else if (secondControlType.compareToIgnoreCase(LIST) == 0) {
				if (secondReadOnly.compareToIgnoreCase("Y") == 0)
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureListvalue, layout, region);
				else
					layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureList, layout, region);
			}
			else if (secondControlType.compareToIgnoreCase(HYPERLINK) == 0 || secondControlType.compareToIgnoreCase(LONGHYPERLINK) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureMetrixHyperlink, layout, region);
			else if (secondControlType.compareToIgnoreCase(SIGNATURE) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureSignature, layout, region);
			else if (secondControlType.compareToIgnoreCase(ATTACHMENT) == 0)
				layoutAdded = MetrixControlAssistant.addLinearLayout(activity, MetrixFieldManager.landscapeSignatureAttachment, layout, region);
			//endregion
		}

		LinearLayout firstLinearLayout = null;
		LinearLayout secondLinearLayout = null;
		firstColumnDef.fieldId = firstFieldId;
		secondColumnDef.fieldId = secondFieldId;

		if (layoutAdded.getChildAt(0) instanceof LinearLayout) {
			firstLinearLayout = (LinearLayout) layoutAdded.getChildAt(0);
			firstColumnDef.labelId = firstLinearLayout.getChildAt(0).getId();
			firstColumnDef.id = firstLinearLayout.getChildAt(1).getId();
		} else {
			firstColumnDef.labelId = layoutAdded.getChildAt(0).getId();
			firstColumnDef.id = layoutAdded.getChildAt(1).getId();
		}

		if (layoutAdded.getChildAt(1) instanceof LinearLayout) {
			secondLinearLayout = (LinearLayout) layoutAdded.getChildAt(1);
			secondColumnDef.labelId = secondLinearLayout.getChildAt(0).getId();
			secondColumnDef.id = secondLinearLayout.getChildAt(1).getId();
		} else {
			secondColumnDef.labelId = layoutAdded.getChildAt(2).getId();
			secondColumnDef.id = layoutAdded.getChildAt(3).getId();
		}

		//region #Handle read-only
		if (firstReadOnly.compareToIgnoreCase("Y") == 0) {
			View control = MetrixControlAssistant.getControl(firstColumnDef, layoutAdded);
			if (control != null) {
				control.setContentDescription(firstTableName+"__"+firstColumnDef.columnName);
				//even-though READ_ONLY = Y, we skip disabling Hyperlinks since they need to be serviceable(Ex: click events)
				if (!(control instanceof MetrixHyperlink) && !(control instanceof AttachmentField) && !(control instanceof SignatureField) && !(control instanceof TextView))
					setEnableStatus(control,false);
				
				if (control instanceof EditText) {
					control.setClickable(false);
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
				} else if (control instanceof Spinner)
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
			}
		}
		
		if (secondReadOnly.compareToIgnoreCase("Y") == 0) {
			View control = MetrixControlAssistant.getControl(secondColumnDef, layoutAdded);
			control.setContentDescription(secondTableName+"__"+secondColumnDef.columnName);
			if (control != null) {
				//even-though READ_ONLY = Y, we skip disabling Hyperlinks since they need to be serviceable(Ex: click events)
				if (!(control instanceof MetrixHyperlink) && !(control instanceof AttachmentField) && !(control instanceof SignatureField) && !(control instanceof TextView))
					setEnableStatus(control,false);

				if (control instanceof EditText) {
					control.setClickable(false);
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
				} else if (control instanceof Spinner)
					control.setBackgroundDrawable(layoutAdded.getResources().getDrawable(MetrixFieldManager.backgroundImage));
			}
		}
		//endregion

		View firstControlAdd = MetrixControlAssistant.getControl(firstColumnDef, layoutAdded);
		if(firstControlAdd != null)
			firstControlAdd.setContentDescription(firstTableName+"__"+firstColumnDef.columnName);
		View secondControlAdd = MetrixControlAssistant.getControl(secondColumnDef, layoutAdded);
		if(secondControlAdd != null)
			secondControlAdd.setContentDescription(secondTableName+"__"+secondColumnDef.columnName);

		//region #Handle label text
		if (firstControlType.compareToIgnoreCase(BUTTON) != 0)
			if (firstLinearLayout != null)
				MetrixControlAssistant.setValue((TextView)firstLinearLayout.getChildAt(0), firstLabel);
			else
				MetrixControlAssistant.setValue((TextView)layoutAdded.getChildAt(0), firstLabel);
		else
			if (firstLinearLayout != null)
				MetrixControlAssistant.setValue((Button)firstLinearLayout.getChildAt(1), firstLabel);
			else
				MetrixControlAssistant.setValue((Button)layoutAdded.getChildAt(1), firstLabel);
		if (secondControlType.compareToIgnoreCase(BUTTON) != 0)
			if (secondLinearLayout != null)
				MetrixControlAssistant.setValue((TextView)secondLinearLayout.getChildAt(0), secondLabel);
			else
				MetrixControlAssistant.setValue((TextView)layoutAdded.getChildAt(2), secondLabel);
		else
			if (secondLinearLayout != null)
				MetrixControlAssistant.setValue((Button)secondLinearLayout.getChildAt(1), secondLabel);
			else
				MetrixControlAssistant.setValue((Button)layoutAdded.getChildAt(3), secondLabel);
		//endregion

		//region #Handle value changed event
		if (!MetrixStringHelper.isNullOrEmpty(firstValueChangedEvent))
			if (firstLinearLayout != null)
				setUpValueChangedEvent(activity, firstLinearLayout.getChildAt(1), firstValueChangedEvent, firstTableName, firstColumnDef.columnName);
			else
				setUpValueChangedEvent(activity, layoutAdded.getChildAt(1), firstValueChangedEvent, firstTableName, firstColumnDef.columnName);
		if (!MetrixStringHelper.isNullOrEmpty(secondValueChangedEvent))
			if (secondLinearLayout != null)
				setUpValueChangedEvent(activity, secondLinearLayout.getChildAt(1), secondValueChangedEvent, secondTableName, secondColumnDef.columnName);
			else
				setUpValueChangedEvent(activity, layoutAdded.getChildAt(3), secondValueChangedEvent, secondTableName, secondColumnDef.columnName);
		//endregion

		//region #Handle field lookup
		if (firstControlType.compareToIgnoreCase(TEXT) == 0 && firstReadOnly.compareToIgnoreCase("Y") != 0 && MetrixFieldLookupManager.fieldHasLookup(firstFieldId)) {
			if (firstLinearLayout != null)
				setUpFieldLookup(activity, firstLinearLayout.getChildAt(1), firstFieldId);
			else
				setUpFieldLookup(activity, layoutAdded.getChildAt(1), firstFieldId);

			if(firstControlAdd != null) {
				firstControlAdd.setContentDescription("Lookup_"+firstTableName + "__" + firstColumnDef.columnName);
			}
		}
		if (secondControlType.compareToIgnoreCase(TEXT) == 0 && secondReadOnly.compareToIgnoreCase("Y") != 0 && MetrixFieldLookupManager.fieldHasLookup(secondFieldId)) {
			if (secondLinearLayout != null)
				setUpFieldLookup(activity, secondLinearLayout.getChildAt(1), secondFieldId);
			else
				setUpFieldLookup(activity, layoutAdded.getChildAt(3), secondFieldId);

			if(secondControlAdd != null) {
				secondControlAdd.setContentDescription("Lookup_"+secondTableName + "__" + secondColumnDef.columnName);
			}
		}
		//endregion

		//region #Handle control event
		if (!MetrixStringHelper.isNullOrEmpty(firstControlEvent))
			if (firstLinearLayout != null)
				MetrixControlAssistant.setUpControlEvent(activity, firstLinearLayout.getChildAt(1), firstControlEvent);
			else
				MetrixControlAssistant.setUpControlEvent(activity, layoutAdded.getChildAt(1), firstControlEvent);
		if (!MetrixStringHelper.isNullOrEmpty(secondControlEvent))
			if (secondLinearLayout != null)
				MetrixControlAssistant.setUpControlEvent(activity, secondLinearLayout.getChildAt(1), secondControlEvent);
			else
				MetrixControlAssistant.setUpControlEvent(activity, layoutAdded.getChildAt(3), secondControlEvent);
		//endregion
	}
	
	private static void setUpValueChangedEvent(final Activity activity, View view, final String valueChangedEvent, final String tableName, final String columnName) {
		if (view != null && !MetrixStringHelper.isNullOrEmpty(valueChangedEvent)) {
			if (view instanceof AttachmentField) {
				TextView tv = ((AttachmentField)view).mHiddenAttachmentIdTextView;
				tv.addTextChangedListener(new TextWatcher() {
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						if (valueChangedEventScriptShouldExecute(tableName, columnName))
							MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(valueChangedEvent));
					}
					@Override
					public void afterTextChanged(Editable s) {}
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				});
			} else if (view instanceof SignatureField) {
				TextView tv = ((SignatureField)view).mHiddenAttachmentIdTextView;
				tv.addTextChangedListener(new TextWatcher() {
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						if (valueChangedEventScriptShouldExecute(tableName, columnName))
							MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(valueChangedEvent));
					}
					@Override
					public void afterTextChanged(Editable s) {}
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				});
			} else if (view instanceof Spinner) {
				final Spinner spn = (Spinner)view;
				spn.post(new Runnable() {
					public void run() {
						spn.setOnItemSelectedListener(new OnItemSelectedListener() {
							@Override
							public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
								if (valueChangedEventScriptShouldExecute(tableName, columnName))
									MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(valueChangedEvent));
							}
							@Override
							public void onNothingSelected(AdapterView<?> arg0) { }
						});	
					}
				});
			} else if (view instanceof CheckBox) {
				CheckBox chk = (CheckBox)view;
				chk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						if (valueChangedEventScriptShouldExecute(tableName, columnName))
						   MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(valueChangedEvent));
					}
				});
			} else if (view instanceof TextView) {
				// this should work for both TextView and EditText
				TextView tv = (TextView)view;
				tv.addTextChangedListener(new TextWatcher() {
				    public void onTextChanged(CharSequence s, int start, int before, int count) {
				    	if (valueChangedEventScriptShouldExecute(tableName, columnName))
				    		MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(valueChangedEvent));
				    }
					@Override
					public void afterTextChanged(Editable s) {}
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				});
			}
		}
	}
	
	private static void setUpFieldLookup(final Activity activity, View view, final Integer fieldId) {
		if (view != null && fieldId != null && fieldId > 0 && view instanceof EditText) {
			EditText et = (EditText)view;
			
			String magnifyingGlassID = String.valueOf(MetrixPublicCache.instance.getItem("magnifying_glass_id"));
			if (MetrixStringHelper.isNullOrEmpty(magnifyingGlassID)) { magnifyingGlassID = "0"; }
			
			et.setCompoundDrawablesWithIntrinsicBounds(0, 0, Integer.valueOf(magnifyingGlassID), 0);
			et.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {

					final int DRAWABLE_LEFT = 0;
					final int DRAWABLE_TOP = 1;
					final int DRAWABLE_RIGHT = 2;
					final int DRAWABLE_BOTTOM = 3;

					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (event.getRawX() >= (et.getRight() - et.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
							// Do something
							try {
								MetrixFormDef formDef = (MetrixFormDef) MetrixPublicCache.instance.getItem("theCurrentFormDef");
								ViewGroup layout = (ViewGroup) MetrixPublicCache.instance.getItem("theCurrentLayout");
								if (formDef == null)
									throw new Exception("MetrixFieldManager.setUpFieldLookup: cannot find cached form definition.  Will not launch field lookup.");
								if (layout == null)
									throw new Exception("MetrixFieldManager.setUpFieldLookup: cannot find cached layout.  Will not launch field lookup.");

								MetrixLookupDef lookupDef = MetrixFieldLookupManager.getFieldLookup(fieldId);

								// use current value in this control as the initial search criteria
								lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v);

								// resolve all MetrixLookupColumnDef.controlId, using corresponding linkedFieldId
								for (MetrixLookupColumnDef columnDef : lookupDef.columnNames) {
									if (columnDef.linkedFieldId > 0) {
										int controlID = formDef.getId(columnDef.linkedFieldId);
										if (controlID > 0)
											columnDef.controlId = controlID;
									}
								}

								// resolve all script-based MetrixLookupFilterDef.rightOperand
								for (MetrixLookupFilterDef filterDef : lookupDef.filters) {
									if (!MetrixStringHelper.isNullOrEmpty(filterDef.scriptForRightOperand)) {
										ClientScriptDef scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(filterDef.scriptForRightOperand);
										if (scriptDef != null) {
											String actualRightOperand = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(activity), scriptDef);
											filterDef.rightOperand = actualRightOperand;
										}
									}
								}

								Intent intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "Lookup");
								MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
								MetrixPublicCache.instance.addItem("lookupParentLayout", layout);
								activity.startActivityForResult(intent, MobileApplication.ARCH_GET_LOOKUP_RESULT);
								return true;
							} catch (Exception ex) {
								LogManager.getInstance().error(ex);
								return false;
							}
						} else {
							// Do something when touching inside the field like the onFocus
							// Returning false to allow the user to type in the field
							return false;
						}
					}
					// Returning false to allow the user to type in the field
					return false;
				}
			});
		}
	}

	private static boolean valueChangedEventScriptShouldExecute(String tableName, String columnName) {
		/*
		First, check if initialScreenValuesSet TRUE or NULL.
		Second, make sure that {TABLE}_{COLUMN}_FVC_SCRIPTDISABLE does NOT exist in cache
		Only if all of the above are correct should we return TRUE, FALSE otherwise.
		*/
		boolean initialScreenValuesSet = MetrixPublicCache.instance.containsKey("initialScreenValuesSet") ? (Boolean)MetrixPublicCache.instance.getItem("initialScreenValuesSet") : true;
		boolean fvcDisabledByScript = MetrixPublicCache.instance.containsKey(String.format("%1$s_%2$s_FVC_SCRIPTDISABLE", tableName.toUpperCase(), columnName.toUpperCase()));
		return (initialScreenValuesSet && !fvcDisabledByScript);
	}
	
	private static void addColumnDefToTableDef(MetrixFormDef formDef, String tableName, MetrixColumnDef columnDef) {
		if (tableName.compareToIgnoreCase("CUSTOM") == 0) {
			boolean tableDefExists = false;
			for (MetrixTableDef tableDef : formDef.tables) {
				if (tableDef.tableName.compareToIgnoreCase("CUSTOM") == 0) {
					tableDefExists = true;
					tableDef.columns.add(columnDef);
					break;
				}
			}
			if (!tableDefExists) {
				MetrixTableDef tableDef = new MetrixTableDef("custom", MetrixTransactionTypes.OTHER);
				tableDef.columns.add(columnDef);
				formDef.tables.add(tableDef);
			}
		}
		else {
			for (MetrixTableDef tableDef : formDef.tables) {
				if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
					tableDef.columns.add(columnDef);
					break;
				}
			}
		}
	}
	
	/**
	 * Populating default values for a particular screen
	 * 
	 * @param activity The current activity.
	 * @param layout The current activity's layout.
	 * @param formDef The current activity's meta-data form definition.
	 * 
	 * since 5.6.1
	 */
	public static void defaultValues(Activity activity, MetrixFormDef formDef, ViewGroup layout) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		defaultValues(activity, formDef, layout, screenId);
	}
	
	/**
	 * Populating default values for a particular screen
	 * 
	 * @param activity The current activity.
	 * @param layout The current activity's layout.
	 * @param formDef The current activity's meta-data form definition.
	 * @param screenId The current screen id.
	 * 
	 * updated 5.6.3
	 */
	public static void defaultValues(Activity activity, MetrixFormDef formDef, ViewGroup layout, int screenId) {
		if (formDef != null && formDef.tables != null && formDef.tables.size() > 0 && formDef.tables.get(0).transactionType == MetrixTransactionTypes.INSERT) {
			
			if ((MetrixFieldManager.defaultValues != null) && (MetrixFieldManager.defaultValues.size() > 0) && (MetrixFieldManager.defaultValues.containsKey(String.valueOf(screenId)))) {
				Map<String, String> defaultValues = MetrixFieldManager.defaultValues.get(String.valueOf(screenId));
				for (String uniqueName : defaultValues.keySet()) {
					String tableName = uniqueName.substring(0, uniqueName.indexOf("__"));
					//Reason : increasing the index by 2 -> Number of characters in the delimiter "__" 
					String columnName = uniqueName.substring((uniqueName.indexOf("__") + 2));
					String defaultValue = defaultValues.get(uniqueName);
					
					if (!MetrixStringHelper.isNullOrEmpty(defaultValue)) {
						String resolvedDynamicDefaultValueCode = getDynamicDefaultValueCode(defaultValue);
						if (!MetrixStringHelper.isNullOrEmpty(resolvedDynamicDefaultValueCode)){
							String resolvedDynamicDefaultValue = getResolvedDynamicDefaultValue(activity, formDef, tableName, columnName, resolvedDynamicDefaultValueCode);
							MetrixControlAssistant.setValue(formDef, layout, tableName, columnName, resolvedDynamicDefaultValue);
						}
						else
							MetrixControlAssistant.setValue(formDef, layout, tableName, columnName, defaultValue);
					}
				}
			} else {
				String query = "SELECT use_mm_field.table_name, use_mm_field.column_name, use_mm_field.default_value " + 
						"FROM use_mm_field WHERE use_mm_field.screen_id = " + screenId;
				
				MetrixCursor cursor = null;
				
				try
				{
					cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
					if (cursor == null || !cursor.moveToFirst())
						return;
			
					if (MetrixFieldManager.defaultValues == null)
						MetrixFieldManager.defaultValues = new LinkedHashMap<String, Map<String, String>>();
					
					LinkedHashMap<String, String> defaultValues = new LinkedHashMap<String, String>();
					
					while (cursor.isAfterLast() == false) {
						String tableName = cursor.getString(0);
						String columnName = cursor.getString(1);
						String defaultValue = cursor.getString(2);
						
						if (!MetrixStringHelper.isNullOrEmpty(defaultValue)) {
							String resolvedDynamicDefaultValueCode = getDynamicDefaultValueCode(defaultValue);
							
							if (!MetrixStringHelper.isNullOrEmpty(resolvedDynamicDefaultValueCode)) {
								String resolvedDynamicDefaultValue = getResolvedDynamicDefaultValue(activity, formDef, tableName, columnName, resolvedDynamicDefaultValueCode);
								MetrixControlAssistant.setValue(formDef, layout, tableName, columnName, resolvedDynamicDefaultValue);
								//We are not caching the resolvedDynamicDefaultValue since some of the 
								//dynamically generated default values like TASK_ID, REQUEST_ID, shouldn't be cached
								//because these values get change depending on the user action, 
								//and therefore these values should be re-populated always.
								//we are caching only the DynamicDefaultValueCode
								defaultValues.put(tableName + "__" + columnName, defaultValue);
							} else {
								MetrixControlAssistant.setValue(formDef, layout, tableName, columnName, defaultValue);
								defaultValues.put(tableName + "__" + columnName, defaultValue);
							}
						}
						cursor.moveToNext();
					}
					
					MetrixFieldManager.defaultValues.put(String.valueOf(screenId), defaultValues);
				} finally {
					if (cursor != null)
						cursor.close();
				}
			}	
		}
	}
	
	/**
	 * This method is used to resolve dynamically specified default value messages and return the actual value.
	 * Ex: ~*CURRENTDATE*~ -> This will returns the current date as the default value
	 * Ex: ~*CURRENTUSERSTOCKPLACE*~ -> This will returns the current user's stock place as the default value
	 * Added 5.6.3
	 * @return resolvedDefaultValue
	 */
	private static String getResolvedDynamicDefaultValue(Activity activity, MetrixFormDef formDef, String tableName, String columnName, String dynamicDefaultValueCode) {
		String resolvedDynamicDefaultValue = null;		
		User user = User.getUser();

		if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTDATETIME_MESSAGE)) {
			Date currDate = new Date();
			MetrixColumnDef colDef = formDef.getColumnDef(tableName, columnName);
			if (colDef.dataType == MetrixDate.class)
				resolvedDynamicDefaultValue = MetrixDateTimeHelper.convertDateTimeFromDateToUIDateOnly(currDate);
			else if (colDef.dataType == MetrixTime.class)
				resolvedDynamicDefaultValue = MetrixDateTimeHelper.convertDateTimeFromDateToUITimeOnly(currDate);
			else
				resolvedDynamicDefaultValue = MetrixDateTimeHelper.convertDateTimeFromDateToUI(currDate);
		} else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTUSER_MESSAGE))
			resolvedDynamicDefaultValue = user.personId;
		else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTUSERCURRENCY_MESSAGE))
			resolvedDynamicDefaultValue = user.currency;
		else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTUSERSTOCKPLACE_MESSAGE))
			resolvedDynamicDefaultValue = user.stockFromPlace;
		else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTUSERSTOCKLOCATION_MESSAGE))
			resolvedDynamicDefaultValue = user.stockFromLocation;
		else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CORPORATECURRENCY_MESSAGE))
			resolvedDynamicDefaultValue = user.corporateCurrency;
		else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTTASKID_MESSAGE))
			resolvedDynamicDefaultValue = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTREQUESTID_MESSAGE)) {
			String currentWorkflowName = MetrixWorkflowManager.getCurrentWorkflowName(activity);
			if (!MetrixStringHelper.isNullOrEmpty(currentWorkflowName) &&
					(MetrixStringHelper.valueIsEqual(currentWorkflowName.toLowerCase(), "schedule") ||
							currentWorkflowName.toLowerCase().startsWith("schedule"))){
				resolvedDynamicDefaultValue = MetrixCurrentKeysHelper.getKeyValue("request", "request_id");
			}
			else {
				String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
				if (!MetrixStringHelper.isNullOrEmpty(taskId)) {
					String requestId = MetrixDatabaseManager.getFieldStringValue("task", "request_id", String.format("task_id = %s", taskId));
					resolvedDynamicDefaultValue = requestId;
				} else
					resolvedDynamicDefaultValue = MetrixCurrentKeysHelper.getKeyValue("request", "request_id");
			}
		} else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTPRODUCT_MESSAGE)) {
			String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
			if (!MetrixStringHelper.isNullOrEmpty(taskId)) {
				String productId = MetrixDatabaseManager.getFieldStringValue("task", "product_id", String.format("task_id = %s", taskId));
				resolvedDynamicDefaultValue = productId;
			}
		} else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTCONTACT_MESSAGE)) {
			String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
			if (!MetrixStringHelper.isNullOrEmpty(taskId)){
				String contactId = MetrixDatabaseManager.getFieldStringValue("task", "contact_sequence", String.format("task_id = %s", taskId));
				resolvedDynamicDefaultValue = contactId;
			}
		} else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTLATITUDE_MESSAGE)) {
			Location currentLocation  = MetrixLocationAssistant.getCurrentLocation(activity);
			if (currentLocation != null)
				resolvedDynamicDefaultValue = MetrixFloatHelper.convertNumericFromForcedLocaleToUI(Double.toString(currentLocation.getLatitude()), Locale.US);
		} else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTLONGITUDE_MESSAGE)) {
			Location currentLocation  = MetrixLocationAssistant.getCurrentLocation(activity);
			if (currentLocation != null)
				resolvedDynamicDefaultValue = MetrixFloatHelper.convertNumericFromForcedLocaleToUI(Double.toString(currentLocation.getLongitude()), Locale.US);
		} else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTQUOTEID_MESSAGE)) {
			resolvedDynamicDefaultValue = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
		}else if (MetrixStringHelper.valueIsEqual(dynamicDefaultValueCode, DEFAULT_CURRENTQUOTEVERSION_MESSAGE)) {
			resolvedDynamicDefaultValue = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");
			if(MetrixStringHelper.isNullOrEmpty(resolvedDynamicDefaultValue))
			{
				String currentQuoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
				if(!MetrixStringHelper.isNullOrEmpty(currentQuoteId))
					resolvedDynamicDefaultValue = MetrixDatabaseManager.getFieldStringValue("quote", "quote_version", String.format("quote_id = %s", currentQuoteId));
			}
		}
		
		return resolvedDynamicDefaultValue;
	}

	/***
	 * Gets the dynamic default value code from metrix_code_table
	 * @param dynamicDefaultValueMessageId
	 * @return the correct dynamic default value code from metrix_code_table
	 */
	private static String getDynamicDefaultValueCode(String dynamicDefaultValueMessageId) {
		String resolvedDynamicDefaultValueCode = null;
		String dynamicDefaultValueCodeQuery = String.format("select mct.code_value from metrix_code_table mct inner join mm_message_def_view mmdv on mmdv.message_id = mct.message_id where mmdv.message_id = '%s' and mmdv.message_type = 'MM_DEFAULT_VALUE'", dynamicDefaultValueMessageId);
		ArrayList<Hashtable<String, String>> defaultValueCodes = MetrixDatabaseManager.getFieldStringValuesList(dynamicDefaultValueCodeQuery);
		if(defaultValueCodes != null && defaultValueCodes.size() > 0){
			for (Hashtable<String, String> defaultValueCode : defaultValueCodes) {
				resolvedDynamicDefaultValueCode = defaultValueCode.get("code_value");
				break;
			}
		}
		
		return resolvedDynamicDefaultValueCode;
	}
}
