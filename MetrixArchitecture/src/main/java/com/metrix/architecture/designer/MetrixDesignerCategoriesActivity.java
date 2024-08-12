package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MetrixDesignerCategoriesActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private String mOriginalSkinID;
	private String mSelectedSkinID;
	private ListView mListView;
	private Spinner mSkins;
	private LinearLayout mColorPreview;
	private View mPrimaryColor, mSecondaryColor, mHyperlinkColor;
	private TextView mFirstGradient, mSecondGradient;
	private AlertDialog mSaveSkinAlert, mPublishRevisionAlert;
	private Button mSaveSkinButton, mPublishRevisionButton;
	private CategoriesListAdapter mCategoriesAdapter;
	private MetrixDesignerResourceData mCategoriesResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCategoriesResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerCategoriesActivityResourceData");

		setContentView(mCategoriesResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mCategoriesResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mCategoriesResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mColorPreview = (LinearLayout) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.color_preview"));
		mPrimaryColor = findViewById(mCategoriesResourceData.getExtraResourceID("R.id.mm_skin__primary_color"));
		mSecondaryColor = findViewById(mCategoriesResourceData.getExtraResourceID("R.id.mm_skin__secondary_color"));
		mHyperlinkColor = findViewById(mCategoriesResourceData.getExtraResourceID("R.id.mm_skin__hyperlink_color"));
		mFirstGradient = (TextView) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.mm_skin__first_gradient"));
		mSecondGradient = (TextView) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.mm_skin__second_gradient"));

		mSkins = (Spinner) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.mm_revision__skin_id"));
		mSaveSkinButton = (Button) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.save_skin"));
		mPublishRevisionButton = (Button) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.publish_revision"));

		TextView mCategories = (TextView) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.categories"));
		TextView mScreenInfo = (TextView) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_categories"));
		TextView mSkin = (TextView) findViewById(mCategoriesResourceData.getExtraResourceID("R.id.skin"));

		AndroidResourceHelper.setResourceValues(mCategories, "Categories");
		AndroidResourceHelper.setResourceValues(mScreenInfo, "ScnDescMxDesCat");
		AndroidResourceHelper.setResourceValues(mSkin, "Skin");
		AndroidResourceHelper.setResourceValues(mSaveSkinButton, "SaveSkin");
		AndroidResourceHelper.setResourceValues(mPublishRevisionButton, "PublishRevision");

		final String letterA = AndroidResourceHelper.getMessage("A");
		mFirstGradient.setText(letterA);
		mSecondGradient.setText(letterA);

		if (mAllowChanges) {
			mSkins.setEnabled(true);
			mSaveSkinButton.setVisibility(View.VISIBLE);
			mSaveSkinButton.setOnClickListener(this);
			mPublishRevisionButton.setVisibility(View.VISIBLE);
			mPublishRevisionButton.setOnClickListener(this);

		} else {
			mSkins.setEnabled(false);
			mSaveSkinButton.setVisibility(View.GONE);
			mPublishRevisionButton.setVisibility(View.GONE);

		}

		populateScreen();

		if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
			String targetDesignerActivity = (String) this.getIntent().getExtras().get("targetDesignerActivity");
			String immediateDestination = "";

			if (MetrixStringHelper.valueIsEqual(targetDesignerActivity, "MetrixDesignerHomeMenuEnablingActivity")) {
				immediateDestination = "MetrixDesignerMenusActivity";
			} else {
				immediateDestination = "MetrixDesignerScreenActivity";
			}

			Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.architecture.designer", immediateDestination);
			intent.putExtra("headingText", mHeadingText);

			intent.putExtra("targetDesignerActivity", targetDesignerActivity);
			if (this.getIntent().getExtras().containsKey("targetDesignerScreenID")) {
				intent.putExtra("targetDesignerScreenID", (String) this.getIntent().getExtras().get("targetDesignerScreenID"));
				intent.putExtra("targetDesignerScreenName", (String) this.getIntent().getExtras().get("targetDesignerScreenName"));
			}

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void populateScreen() {
		try {
			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

			HashMap<String, String> row = new HashMap<String, String>();
			row.put("category", AndroidResourceHelper.getMessage("Menus"));
			row.put("icon", String.valueOf(mCategoriesResourceData.getExtraResourceID("R.drawable.categories_menus")));
			table.add(row);

			row = new HashMap<String, String>();
			row.put("category", AndroidResourceHelper.getMessage("Screens"));
			row.put("icon", String.valueOf(mCategoriesResourceData.getExtraResourceID("R.drawable.categories_screens")));
			table.add(row);

			row = new HashMap<String, String>();
			row.put("category", AndroidResourceHelper.getMessage("Workflows"));
			row.put("icon", String.valueOf(mCategoriesResourceData.getExtraResourceID("R.drawable.categories_workflows")));
			table.add(row);

			mCategoriesAdapter = new CategoriesListAdapter(this, table, mCategoriesResourceData.ListViewItemResourceID, mCategoriesResourceData.ExtraResourceIDs);
			mListView.setAdapter(mCategoriesAdapter);

			MetrixControlAssistant.populateSpinnerFromQuery(this, mSkins, "select distinct name, skin_id from mm_skin order by name asc", true);
			mSkins.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					setColorPreviewBasedOnSkinSelection();
				}

				public void onNothingSelected(AdapterView<?> parent) {
					setColorPreviewBasedOnSkinSelection();
				}
			});
			if (mSkins.getAdapter().getCount() <= 0) {
				setColorPreviewBasedOnSkinSelection();
				mSaveSkinButton.setEnabled(false);
			}

			mOriginalSkinID = MetrixDatabaseManager.getFieldStringValue("mm_revision", "skin_id", String.format("revision_id = %s", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id")));
			MetrixControlAssistant.setValue(mSkins, mOriginalSkinID);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mCategoriesResourceData.getExtraResourceID("R.id.save_skin")) {
			SpinnerKeyValuePair pair = (SpinnerKeyValuePair) mSkins.getSelectedItem();
			if (pair != null) {
				mSelectedSkinID = pair.spinnerValue;
				if (!MetrixStringHelper.valueIsEqual(mSelectedSkinID, mOriginalSkinID)) {
					mSaveSkinAlert = new AlertDialog.Builder(this).create();
					mSaveSkinAlert.setMessage(AndroidResourceHelper.getMessage("SkinRevisionConfirmation", mActionBarTitle.getText().toString()));
					mSaveSkinAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), saveSkinListener);
					mSaveSkinAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), saveSkinListener);
					mSaveSkinAlert.show();
				}
			}
		} else if (viewId == mCategoriesResourceData.getExtraResourceID("R.id.publish_revision")) {
			mPublishRevisionAlert = new AlertDialog.Builder(this).create();
			mPublishRevisionAlert.setMessage(AndroidResourceHelper.getMessage("PublishRevisionConfirmation", mActionBarTitle.getText().toString()));
			mPublishRevisionAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), publishRevisionListener);
			mPublishRevisionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), publishRevisionListener);
			mPublishRevisionAlert.show();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mCategoriesAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String categoryString = selectedItem.get("category");
		if (MetrixStringHelper.valueIsEqual(categoryString, AndroidResourceHelper.getMessage("Menus"))) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerMenusActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (MetrixStringHelper.valueIsEqual(categoryString, AndroidResourceHelper.getMessage("Screens"))) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (MetrixStringHelper.valueIsEqual(categoryString, AndroidResourceHelper.getMessage("Workflows"))) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void setColorPreviewBasedOnSkinSelection() {
		try {
			String skinID = "";
			SpinnerKeyValuePair pair = (SpinnerKeyValuePair) mSkins.getSelectedItem();
			if (pair != null) {
				skinID = pair.spinnerValue;
			}

			if (!MetrixStringHelper.isNullOrEmpty(skinID)) {
				setColorViewsForSkinID(skinID);
				mColorPreview.setVisibility(View.VISIBLE);
			} else {
				mColorPreview.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void setColorViewsForSkinID(String skinID) {
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_skin.primary_color, mm_skin.secondary_color, mm_skin.hyperlink_color,");
		query.append(" mm_skin.first_gradient1, mm_skin.first_gradient2, mm_skin.first_gradient_text,");
		query.append(" mm_skin.second_gradient1, mm_skin.second_gradient2, mm_skin.second_gradient_text");
		query.append(String.format(" from mm_skin where skin_id = %s", skinID));

		MetrixCursor cursor = null;
		String primaryColorString = "";
		String secondaryColorString = "";
		String hyperlinkColorString = "";
		String firstGradient1String = "";
		String firstGradient2String = "";
		String firstGradientTextString = "";
		String secondGradient1String = "";
		String secondGradient2String = "";
		String secondGradientTextString = "";

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				primaryColorString = cursor.getString(0);
				secondaryColorString = cursor.getString(1);
				hyperlinkColorString = cursor.getString(2);
				firstGradient1String = cursor.getString(3);
				firstGradient2String = cursor.getString(4);
				firstGradientTextString = cursor.getString(5);
				secondGradient1String = cursor.getString(6);
				secondGradient2String = cursor.getString(7);
				secondGradientTextString = cursor.getString(8);
				break;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		primaryColorString = MetrixStringHelper.isNullOrEmpty(primaryColorString) ? "FFFFFF" : primaryColorString;
		secondaryColorString = MetrixStringHelper.isNullOrEmpty(secondaryColorString) ? "FFFFFF" : secondaryColorString;
		hyperlinkColorString = MetrixStringHelper.isNullOrEmpty(hyperlinkColorString) ? "FFFFFF" : hyperlinkColorString;
		firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
		firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
		firstGradientTextString = MetrixStringHelper.isNullOrEmpty(firstGradientTextString) ? "FFFFFF" : firstGradientTextString;
		secondGradient1String = MetrixStringHelper.isNullOrEmpty(secondGradient1String) ? "FFFFFF" : secondGradient1String;
		secondGradient2String = MetrixStringHelper.isNullOrEmpty(secondGradient2String) ? "FFFFFF" : secondGradient2String;
		secondGradientTextString = MetrixStringHelper.isNullOrEmpty(secondGradientTextString) ? "FFFFFF" : secondGradientTextString;

		int primaryColor = (int)Long.parseLong(primaryColorString, 16);
		int secondaryColor = (int)Long.parseLong(secondaryColorString, 16);
		int hyperlinkColor = (int)Long.parseLong(hyperlinkColorString, 16);
		int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
		int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);
		int firstGradientText = (int)Long.parseLong(firstGradientTextString, 16);
		int secondGradient1 = (int)Long.parseLong(secondGradient1String, 16);
		int secondGradient2 = (int)Long.parseLong(secondGradient2String, 16);
		int secondGradientText = (int)Long.parseLong(secondGradientTextString, 16);

		mPrimaryColor.setBackgroundColor(Color.rgb((primaryColor >> 16) & 0xFF, (primaryColor >> 8) & 0xFF, (primaryColor >> 0) & 0xFF));
		mSecondaryColor.setBackgroundColor(Color.rgb((secondaryColor >> 16) & 0xFF, (secondaryColor >> 8) & 0xFF, (secondaryColor >> 0) & 0xFF));
		mHyperlinkColor.setBackgroundColor(Color.rgb((hyperlinkColor >> 16) & 0xFF, (hyperlinkColor >> 8) & 0xFF, (hyperlinkColor >> 0) & 0xFF));

		int[] colors = new int[2];
		colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
		colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
		GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
		firstGradient.setCornerRadius(0f);
		mFirstGradient.setBackgroundDrawable(firstGradient);
		mFirstGradient.setTextColor(Color.rgb((firstGradientText >> 16) & 0xFF, (firstGradientText >> 8) & 0xFF, (firstGradientText >> 0) & 0xFF));

		int[] colors2 = new int[2];
		colors2[0] = Color.rgb((secondGradient1 >> 16) & 0xFF, (secondGradient1 >> 8) & 0xFF, (secondGradient1 >> 0) & 0xFF);
		colors2[1] = Color.rgb((secondGradient2 >> 16) & 0xFF, (secondGradient2 >> 8) & 0xFF, (secondGradient2 >> 0) & 0xFF);
		GradientDrawable secondGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors2);
		secondGradient.setCornerRadius(0f);
		mSecondGradient.setBackgroundDrawable(secondGradient);
		mSecondGradient.setTextColor(Color.rgb((secondGradientText >> 16) & 0xFF, (secondGradientText >> 8) & 0xFF, (secondGradientText >> 0) & 0xFF));
		}

	DialogInterface.OnClickListener saveSkinListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					String revisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
					String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_revision", "metrix_row_id", "revision_id = " + revisionID);
					ArrayList<MetrixSqlData> revisionToUpdate = new ArrayList<MetrixSqlData>();
					MetrixSqlData data = new MetrixSqlData("mm_revision", MetrixTransactionTypes.UPDATE, "metrix_row_id=" + metrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
					data.dataFields.add(new DataField("revision_id", revisionID));
					data.dataFields.add(new DataField("skin_id", mSelectedSkinID));
					revisionToUpdate.add(data);
					MetrixTransaction transactionInfo = new MetrixTransaction();
					MetrixUpdateManager.update(revisionToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("RevisionSkinEdit"), mCurrentActivity);

					Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, MetrixDesignerCategoriesActivity.class);
					intent.putExtra("headingText", mHeadingText);
					MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
					break;
				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
		}
		}
	};

	DialogInterface.OnClickListener publishRevisionListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					String revisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
					String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_revision", "metrix_row_id", "revision_id = " + revisionID);
					ArrayList<MetrixSqlData> revisionToUpdate = new ArrayList<MetrixSqlData>();
					MetrixSqlData data = new MetrixSqlData("mm_revision", MetrixTransactionTypes.UPDATE, "metrix_row_id=" + metrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
					data.dataFields.add(new DataField("revision_id", revisionID));
					data.dataFields.add(new DataField("status", "PUBLISHED"));
					revisionToUpdate.add(data);
					MetrixTransaction transactionInfo = new MetrixTransaction();
					MetrixUpdateManager.update(revisionToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("RevisionPublish"), mCurrentActivity);

					mCurrentActivity.finish();
					break;
				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
		}
		}
	};

	public static class CategoriesListAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public CategoriesListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.category_priority_icon"));
				holder.mCategory = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.zzmd_category"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
		}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mIcon.setImageDrawable(thisContext.getResources().getDrawable(Integer.valueOf(dataRow.get("icon"))));
			holder.mCategory.setText(dataRow.get("category"));

			return vi;
	}

		static class ViewHolder {
			ImageView mIcon;
			TextView mCategory;
		}
}
}

