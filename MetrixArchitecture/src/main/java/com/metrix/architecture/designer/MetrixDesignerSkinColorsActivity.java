package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.HexKeyboard;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

@SuppressWarnings("deprecation")
public class MetrixDesignerSkinColorsActivity extends MetrixDesignerActivity {
	private HexKeyboard mCustomKeyboard;
	private HashMap<String, String> mOriginalData;
	private Button mPrimaryColorPick, mSecondaryColorPick, mHyperlinkColorPick,
			mFirstGradient1Pick, mFirstGradient2Pick, mFirstGradientTextPick,
			mSecondGradient1Pick, mSecondGradient2Pick, mSecondGradientTextPick, mSave, mViewImages;
	private EditText mPrimaryColor, mSecondaryColor, mHyperlinkColor,
			mFirstGradient1, mFirstGradient2, mFirstGradientText,
			mSecondGradient1, mSecondGradient2, mSecondGradientText;
	private View mPrimaryColorPreview, mSecondaryColorPreview, mHyperlinkColorPreview,
			mFirstGradientPreview, mSecondGradientPreview;
	private MetrixDesignerResourceData mSkinColorsResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSkinColorsResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerSkinColorsActivityResourceData");

		setContentView(mSkinColorsResourceData.LayoutResourceID);

		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mSkinColorsResourceData.HelpTextString;

		mHeadingText = MetrixCurrentKeysHelper.getKeyValue("mm_skin", "name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mPrimaryColorPick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.primary_color_pick"));
		mSecondaryColorPick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.secondary_color_pick"));
		mHyperlinkColorPick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.hyperlink_color_pick"));
		mFirstGradient1Pick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient1_pick"));
		mFirstGradient2Pick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient2_pick"));
		mFirstGradientTextPick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient_text_pick"));
		mSecondGradient1Pick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient1_pick"));
		mSecondGradient2Pick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient2_pick"));
		mSecondGradientTextPick = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient_text_pick"));
		mSave = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.save"));
		mViewImages = (Button) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.view_images"));

		mPrimaryColorPick.setOnClickListener(this);
		mSecondaryColorPick.setOnClickListener(this);
		mHyperlinkColorPick.setOnClickListener(this);
		mFirstGradient1Pick.setOnClickListener(this);
		mFirstGradient2Pick.setOnClickListener(this);
		mFirstGradientTextPick.setOnClickListener(this);
		mSecondGradient1Pick.setOnClickListener(this);
		mSecondGradient2Pick.setOnClickListener(this);
		mSecondGradientTextPick.setOnClickListener(this);
		mSave.setOnClickListener(this);
		mViewImages.setOnClickListener(this);

		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mViewImages, "ViewImages");
	}

	@Override
	public void onBackPressed() {
		if (mCustomKeyboard.isHexKeyboardVisible())
			mCustomKeyboard.hideHexKeyboard();
		else
			super.onBackPressed();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mCustomKeyboard.refreshKeyboardLayout();
	}

	private void populateScreen() {
		mPrimaryColor = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__primary_color"));
		mSecondaryColor = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__secondary_color"));
		mHyperlinkColor = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__hyperlink_color"));
		mFirstGradient1 = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__first_gradient1"));
		mFirstGradient2 = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__first_gradient2"));
		mFirstGradientText = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__first_gradient_text"));
		mSecondGradient1 = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__second_gradient1"));
		mSecondGradient2 = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__second_gradient2"));
		mSecondGradientText = (EditText) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__second_gradient_text"));

		mPrimaryColorPreview = findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__primary_color_preview"));
		mSecondaryColorPreview = findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__secondary_color_preview"));
		mHyperlinkColorPreview = findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__hyperlink_color_preview"));
		mFirstGradientPreview = findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__first_gradient_preview"));
		mSecondGradientPreview = findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__second_gradient_preview"));

		mPrimaryColor.addTextChangedListener(new ColorTextWatcher(mPrimaryColorPreview));
		mSecondaryColor.addTextChangedListener(new ColorTextWatcher(mSecondaryColorPreview));
		mHyperlinkColor.addTextChangedListener(new ColorTextWatcher(mHyperlinkColorPreview));
		mFirstGradient1.addTextChangedListener(new ColorTextWatcher(mFirstGradientPreview, mFirstGradient2, true));
		mFirstGradient2.addTextChangedListener(new ColorTextWatcher(mFirstGradientPreview, mFirstGradient1, false));
		mFirstGradientText.addTextChangedListener(new ColorTextWatcher(mFirstGradientPreview, true));
		mSecondGradient1.addTextChangedListener(new ColorTextWatcher(mSecondGradientPreview, mSecondGradient2, true));
		mSecondGradient2.addTextChangedListener(new ColorTextWatcher(mSecondGradientPreview, mSecondGradient1, false));
		mSecondGradientText.addTextChangedListener(new ColorTextWatcher(mSecondGradientPreview, true));

		TextView mClrs = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.colors"));
		TextView mScrInfo = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_skin_colors"));
		TextView mPrmry = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.primary"));
		TextView mHyperLnk = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.hyperlink"));
		TextView mSecndry = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.secondary"));
		TextView mFrstGrad = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient"));
		TextView mSecGrad = (TextView) findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient"));

		AndroidResourceHelper.setResourceValues(mClrs, "Colors");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesSkinColors");
		AndroidResourceHelper.setResourceValues(mPrmry, "Primary");
		AndroidResourceHelper.setResourceValues(mSecndry, "Secondary");
		AndroidResourceHelper.setResourceValues(mHyperLnk, "Hyperlink");
		AndroidResourceHelper.setResourceValues(mFrstGrad, "FirstGradient");
		AndroidResourceHelper.setResourceValues(mSecGrad, "SecondGradient");
		AndroidResourceHelper.setResourceValues(mFirstGradientPreview, "A");
		AndroidResourceHelper.setResourceValues(mSecondGradientPreview, "A");


		registerHexKeyboard();

		String currentSkinID = MetrixCurrentKeysHelper.getKeyValue("mm_skin", "skin_id");
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_skin.metrix_row_id, mm_skin.primary_color, mm_skin.secondary_color, mm_skin.hyperlink_color,");
		query.append(" mm_skin.first_gradient1, mm_skin.first_gradient2, mm_skin.first_gradient_text,");
		query.append(" mm_skin.second_gradient1, mm_skin.second_gradient2, mm_skin.second_gradient_text");
		query.append(String.format(" from mm_skin where skin_id = %s", currentSkinID));

		MetrixCursor cursor = null;
		mOriginalData = new HashMap<String, String>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				String primaryColorString = cursor.getString(1);
				String secondaryColorString = cursor.getString(2);
				String hyperlinkColorString = cursor.getString(3);
				String firstGradient1String = cursor.getString(4);
				String firstGradient2String = cursor.getString(5);
				String firstGradientTextString = cursor.getString(6);
				String secondGradient1String = cursor.getString(7);
				String secondGradient2String = cursor.getString(8);
				String secondGradientTextString = cursor.getString(9);

				mOriginalData.put("mm_skin.metrix_row_id", cursor.getString(0));
				mOriginalData.put("mm_skin.primary_color", primaryColorString);
				mOriginalData.put("mm_skin.secondary_color", secondaryColorString);
				mOriginalData.put("mm_skin.hyperlink_color", hyperlinkColorString);
				mOriginalData.put("mm_skin.first_gradient1", firstGradient1String);
				mOriginalData.put("mm_skin.first_gradient2", firstGradient2String);
				mOriginalData.put("mm_skin.first_gradient_text", firstGradientTextString);
				mOriginalData.put("mm_skin.second_gradient1", secondGradient1String);
				mOriginalData.put("mm_skin.second_gradient2", secondGradient2String);
				mOriginalData.put("mm_skin.second_gradient_text", secondGradientTextString);

				mPrimaryColor.setText(primaryColorString);
				mSecondaryColor.setText(secondaryColorString);
				mHyperlinkColor.setText(hyperlinkColorString);
				mFirstGradient1.setText(firstGradient1String);
				mFirstGradient2.setText(firstGradient2String);
				mFirstGradientText.setText(firstGradientTextString);
				mSecondGradient1.setText(secondGradient1String);
				mSecondGradient2.setText(secondGradient2String);
				mSecondGradientText.setText(secondGradientTextString);
				break;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void registerHexKeyboard() {
		mCustomKeyboard = new HexKeyboard(this, mSkinColorsResourceData.getExtraResourceID("R.id.hex_keyboard_view"), mSkinColorsResourceData.getExtraResourceID("R.xml.hexkey"));
		mCustomKeyboard.registerEditText(mPrimaryColor);
		mCustomKeyboard.registerEditText(mSecondaryColor);
		mCustomKeyboard.registerEditText(mHyperlinkColor);
		mCustomKeyboard.registerEditText(mFirstGradient1);
		mCustomKeyboard.registerEditText(mFirstGradient2);
		mCustomKeyboard.registerEditText(mFirstGradientText);
		mCustomKeyboard.registerEditText(mSecondGradient1);
		mCustomKeyboard.registerEditText(mSecondGradient2);
		mCustomKeyboard.registerEditText(mSecondGradientText);
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
		if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.primary_color_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.secondary_color_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.hyperlink_color_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient1_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient2_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient_text_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient1_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient2_pick")
				|| viewId == mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient_text_pick")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.architecture.ui.widget", "ColorPicker");
			LinearLayout parentLayout = (LinearLayout) v.getParent();

			String columnName = "";
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.primary_color_pick")) {columnName = "primary_color";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.secondary_color_pick")) {columnName = "secondary_color";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.hyperlink_color_pick")) {columnName = "hyperlink_color";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient1_pick")) {columnName = "first_gradient1";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient2_pick")) {columnName = "first_gradient2";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.first_gradient_text_pick")) {columnName = "first_gradient_text";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient1_pick")) {columnName = "second_gradient1";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient2_pick")) {columnName = "second_gradient2";}
			if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.second_gradient_text_pick")) {columnName = "second_gradient_text";}

			if (!MetrixStringHelper.isNullOrEmpty(columnName)) {
				EditText etValue = (EditText) parentLayout.findViewById(mSkinColorsResourceData.getExtraResourceID("R.id.mm_skin__" + columnName));
				MetrixPublicCache.instance.addItem("colorPickerEditText", etValue);
				MetrixPublicCache.instance.addItem("colorPickerParentLayout", parentLayout);
				startActivityForResult(intent, 8181);
			}
		} else if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.save")) {
			if (processAndSaveChanges()) {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerSkinColorsActivity.class);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mSkinColorsResourceData.getExtraResourceID("R.id.view_images")) {
			if (processAndSaveChanges()) {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerSkinImagesActivity.class);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		}
	}

	private boolean processAndSaveChanges() {
		try {
			String primaryColorString = mPrimaryColor.getText().toString();
			String secondaryColorString = mSecondaryColor.getText().toString();
			String hyperlinkColorString = mHyperlinkColor.getText().toString();
			String firstGradient1String = mFirstGradient1.getText().toString();
			String firstGradient2String = mFirstGradient2.getText().toString();
			String firstGradientTextString = mFirstGradientText.getText().toString();
			String secondGradient1String = mSecondGradient1.getText().toString();
			String secondGradient2String = mSecondGradient2.getText().toString();
			String secondGradientTextString = mSecondGradientText.getText().toString();

			// all color strings must be NULL or six-digit hex strings
			if ((!MetrixStringHelper.isNullOrEmpty(primaryColorString) && primaryColorString.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(secondaryColorString) && secondaryColorString.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(hyperlinkColorString) && hyperlinkColorString.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(firstGradient1String) && firstGradient1String.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(firstGradient2String) && firstGradient2String.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(firstGradientTextString) && firstGradientTextString.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(secondGradient1String) && secondGradient1String.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(secondGradient2String) && secondGradient2String.length() != 6) ||
					(!MetrixStringHelper.isNullOrEmpty(secondGradientTextString) && secondGradientTextString.length() != 6)) {
				Toast.makeText(this, AndroidResourceHelper.getMessage("SkinColorsEditError"), Toast.LENGTH_LONG).show();
				return false;
			}

			boolean generateUpdateMsg = false;
			ArrayList<MetrixSqlData> skinToUpdate = new ArrayList<MetrixSqlData>();
			String metrixRowID = mOriginalData.get("mm_skin.metrix_row_id");
			MetrixSqlData data = new MetrixSqlData("mm_skin", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("skin_id", MetrixCurrentKeysHelper.getKeyValue("mm_skin", "skin_id")));

			if (!MetrixStringHelper.valueIsEqual(primaryColorString, mOriginalData.get("mm_skin.primary_color"))) {
				data.dataFields.add(new DataField("primary_color", primaryColorString));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(secondaryColorString, mOriginalData.get("mm_skin.secondary_color"))) {
				data.dataFields.add(new DataField("secondary_color", secondaryColorString));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(hyperlinkColorString, mOriginalData.get("mm_skin.hyperlink_color"))) {
				data.dataFields.add(new DataField("hyperlink_color", hyperlinkColorString));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(firstGradient1String, mOriginalData.get("mm_skin.first_gradient1"))) {
				data.dataFields.add(new DataField("first_gradient1", firstGradient1String));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(firstGradient2String, mOriginalData.get("mm_skin.first_gradient2"))) {
				data.dataFields.add(new DataField("first_gradient2", firstGradient2String));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(firstGradientTextString, mOriginalData.get("mm_skin.first_gradient_text"))) {
				data.dataFields.add(new DataField("first_gradient_text", firstGradientTextString));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(secondGradient1String, mOriginalData.get("mm_skin.second_gradient1"))) {
				data.dataFields.add(new DataField("second_gradient1", secondGradient1String));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(secondGradient2String, mOriginalData.get("mm_skin.second_gradient2"))) {
				data.dataFields.add(new DataField("second_gradient2", secondGradient2String));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(secondGradientTextString, mOriginalData.get("mm_skin.second_gradient_text"))) {
				data.dataFields.add(new DataField("second_gradient_text", secondGradientTextString));
				generateUpdateMsg = true;
			}

			if (generateUpdateMsg) {
				skinToUpdate.add(data);

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(skinToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("SkinColorsChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}

		return true;
	}

	/**
	 *
	 * Used in the context of an edittext that permits color hex entry.
	 * Provides the opportunity to preview the color string entered.
	 *
	 */
	protected class ColorTextWatcher implements TextWatcher {
		private TextView mGradientCounterpart;
		private View mDisplay;
		boolean isGradientText;
		boolean isGradient;
		boolean isTopGradient;

		public ColorTextWatcher(View vDisplay) {
			mDisplay = vDisplay;
			mGradientCounterpart = null;
			isGradientText = false;
			isGradient = false;
			isTopGradient = false;
		}

		public ColorTextWatcher(View vDisplay, boolean isForGradientText) {
			mDisplay = vDisplay;
			mGradientCounterpart = null;
			isGradientText = isForGradientText;
			isGradient = false;
			isTopGradient = false;
		}

		public ColorTextWatcher(View vDisplay, TextView tvCounterpart, boolean isTop) {
			mDisplay = vDisplay;
			mGradientCounterpart = tvCounterpart;
			isGradient = true;
			isTopGradient = isTop;
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			String colorString = s.toString();
			colorString = MetrixStringHelper.isNullOrEmpty(colorString) ? "FFFFFF" : colorString;
			int colorInt = (int)Long.parseLong(colorString, 16);
			if (!isGradient && !isGradientText) {
				mDisplay.setBackgroundColor(Color.rgb((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, (colorInt >> 0) & 0xFF));
			} else if (isGradientText) {
				((TextView) mDisplay).setTextColor(Color.rgb((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, (colorInt >> 0) & 0xFF));
			} else {
				String otherColor = mGradientCounterpart.getText().toString();
				otherColor = MetrixStringHelper.isNullOrEmpty(otherColor) ? "FFFFFF" : otherColor;
				int otherColorInt = (int)Long.parseLong(otherColor, 16);
				int[] colors = new int[2];
				if (isTopGradient) {
					colors[0] = Color.rgb((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, (colorInt >> 0) & 0xFF);
					colors[1] = Color.rgb((otherColorInt >> 16) & 0xFF, (otherColorInt >> 8) & 0xFF, (otherColorInt >> 0) & 0xFF);
				} else {
					colors[0] = Color.rgb((otherColorInt >> 16) & 0xFF, (otherColorInt >> 8) & 0xFF, (otherColorInt >> 0) & 0xFF);
					colors[1] = Color.rgb((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, (colorInt >> 0) & 0xFF);
				}
				GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				gradient.setCornerRadius(0f);
				mDisplay.setBackgroundDrawable(gradient);
			}
		}

		public void afterTextChanged(Editable s) {
		}
	}
}

