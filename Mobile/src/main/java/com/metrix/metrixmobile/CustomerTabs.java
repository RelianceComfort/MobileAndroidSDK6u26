package com.metrix.metrixmobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.MetadataStandardActivity;
import com.metrix.metrixmobile.system.MetrixTabActivity;

import java.util.HashMap;
import java.util.Map;

public class CustomerTabs extends MetrixTabActivity implements MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener {
	private static int mAttrSelectedPosition = -1;
	private String mStartingTabName = "";
	private String _maxRows = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int custTabsScreenID = MetrixScreenManager.getScreenId("CustomerTabs");
		_maxRows = MetrixDatabaseManager.getAppParam("MAX_ROWS");
		performInitialSetup(custTabsScreenID);
	}

	public void onStart() {
		super.onStart();
		processStartingTab();

		MetrixTabScreenManager.TabChildInfo tciAttr = mTabChildren.get("CustTabAttributes");
		if (tciAttr != null) {
			LinearLayout thisAttributeChildLayout = (LinearLayout)tciAttr.mLayout;
			registerForMetrixActionView(thisAttributeChildLayout, getMetrixActionBar().getCustomView());
		}
	}

	protected void setListeners() {
		for(MetrixTabScreenManager.TabChildInfo tci : mTabChildren.values()){
			final MetadataRecyclerViewAdapter adapter = tci.mListAdapter;
			if(adapter != null)
				adapter.setClickListener(this);
		}

		MetrixTabScreenManager.TabChildInfo tciAttr = mTabChildren.get("CustTabAttributes");
		if (tciAttr != null) {
			final MetadataRecyclerViewAdapter adapter = tciAttr.mListAdapter;
			if (adapter != null)
				adapter.setLongClickListener((pos, data, view) -> {
					mAttrSelectedPosition = pos;
					if (onCreateMetrixActionViewListner != null)
						onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, pos);
				});
		}
	}

	@Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {
		if (position.length > 0)
			mAttrSelectedPosition = position[0];

		MetrixActionView metrixActionView = getMetrixActionView();
		Menu menu = metrixActionView.getMenu();

		MetrixTabScreenManager.TabChildInfo tciAttr = mTabChildren.get("CustTabAttributes");
		if (tciAttr != null) {
			if (view.getParent() instanceof RecyclerView)
				AttributesListMetrixActionView.onCreateMetrixActionView(menu, tciAttr.mCount);
			else
				AttributesListMetrixActionView.onCreateMetrixActionView(menu, 0);
		}

		return super.OnCreateMetrixActionView(view);
	}

	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		int targetScreenId = MetrixScreenManager.getScreenId("PlaceAttribute");
		MetrixTabScreenManager.TabChildInfo tciAttr = mTabChildren.get("CustTabAttributes");
		if (tciAttr != null) {
			final MetadataRecyclerViewAdapter attrAdapter = tciAttr.mListAdapter;

			if ((menuItem.getTitle().toString().compareToIgnoreCase(AttributesListMetrixActionView.MODIFY) == 0)) {
				if (targetScreenId > 0) {
					mStartingTabName = "CustTabAttributes";
					HashMap<String, String> selectedItem = attrAdapter.getListData().get(mAttrSelectedPosition);

					Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataStandardActivity.class, MetrixTransactionTypes.UPDATE,
							"metrix_row_id", selectedItem.get("attribute.metrix_row_id"));
					intent.putExtra("ScreenID", targetScreenId);
					intent.putExtra("NavigatedFromLinkedScreen", true);
					MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
				}
			} else if ((menuItem.getTitle().toString().compareToIgnoreCase(AttributesListMetrixActionView.ADD) == 0)) {
				if (targetScreenId > 0) {
					mStartingTabName = "CustTabAttributes";
					Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataStandardActivity.class);
					intent.putExtra("ScreenID", targetScreenId);
					intent.putExtra("NavigatedFromLinkedScreen", true);
					MetrixActivityHelper.startNewActivity(this, intent);
				}
			} else if ((menuItem.getTitle().toString().compareToIgnoreCase(AttributesListMetrixActionView.DELETE) == 0)) {
				DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							MetrixTabScreenManager.TabChildInfo tciAttr2 = mTabChildren.get("CustTabAttributes");
							LinearLayout thisChildLayout = (LinearLayout) tciAttr2.mLayout;
							final MetadataRecyclerViewAdapter attrAdapter2 = tciAttr2.mListAdapter;

							HashMap<String, String> selectedItem = attrAdapter2.getListData().get(mAttrSelectedPosition);

							String metrixRowId = selectedItem.get("attribute.metrix_row_id");
							String attrId = selectedItem.get("attribute.attr_id");
							MetrixTransaction transactionInfo = new MetrixTransaction();
							MetrixUpdateManager.delete(CustomerTabs.this, "attribute", metrixRowId, "attr_id", attrId,
									AndroidResourceHelper.getMessage("Attribute"), transactionInfo);

							attrAdapter2.getListData().remove(mAttrSelectedPosition);
							attrAdapter2.notifyItemRemoved(mAttrSelectedPosition);
							tciAttr2.mCount = attrAdapter2.getListData().size();

							String tabLabel = "";
							if (tciAttr2.mCount > 0){
								tabLabel = String.format("%1$s (%2$s)", tciAttr2.mTabTitle, String.valueOf(tciAttr2.mCount));
							}
							else{
								tabLabel = String.format("%1$s", tciAttr2.mTabTitle);
							}
							((TextView) mTabHost.getTabWidget().getChildAt(tciAttr2.mTabIndex).findViewById(android.R.id.title)).setText(tabLabel);

							if (tciAttr2.mCount <= 0) {
								final RecyclerView attributeRecyclerView = thisChildLayout.findViewById(R.id.tab_child_recyclerview);
								attributeRecyclerView.setVisibility(View.GONE);
								registerForMetrixActionView(thisChildLayout, getMetrixActionBar().getCustomView());
							}
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					}
				};
				MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("AttributeLCase"), deleteListener, null, this);
			}
		}
		return true;
	}

	@Override
	protected void addTabs() {
		try {
			FrameLayout tabChildFrame = (FrameLayout)findViewById(android.R.id.tabcontent);
			for (Map.Entry<String, MetrixTabScreenManager.TabChildInfo> childTab : mTabChildren.entrySet()) {
				String screenName = childTab.getKey();
				MetrixTabScreenManager.TabChildInfo tci = childTab.getValue();

				int layoutResourceID = (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD")) ? R.layout.yycsmd_tab_child_standard : R.layout.yycsmd_tab_child_list;
				LinearLayout thisChildLayout = MetrixControlAssistant.addLinearLayoutWithoutChildIDs(this, layoutResourceID, tabChildFrame);
				tci.mLayoutId = thisChildLayout.getId();

				TextView tvLabel = (TextView)thisChildLayout.findViewById(R.id.tab_child_label);
				TextView tvTip = (TextView)thisChildLayout.findViewById(R.id.tab_child_tip);
				tvLabel.setText(tci.mLabel);
				tvTip.setText(tci.mTip);

				if (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD")) {
					// set up a form def-based layout
					setupCodelessStandardTabChild(thisChildLayout, tci);
				} else {
					// Set up search field
					AutoCompleteTextView searchField = thisChildLayout.findViewById(R.id.tab_child_search_criteria);
					searchField.setId(tci.mSearchFieldId);
					resourceStrings.add(new ResourceValueObject(tci.mSearchFieldId, "Search", true));
					MetrixAutoCompleteHelper.populateAutoCompleteTextView(tci.mScreenId, searchField, this);

					// set up a recyclerView-based layout
					int thisChildScreenID = Integer.valueOf(tci.mScreenId);
					final RecyclerView recyclerView = thisChildLayout.findViewById(R.id.tab_child_recyclerview);
					MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

					if (MetrixStringHelper.valueIsEqual(screenName, "CustTabProducts")){
						CustTabProducts.populateProductList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, null);
						searchField.addTextChangedListener(new TabSearchTextWatcher(searchText -> CustTabProducts.updateProductList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, searchText)));
					}
					else if (MetrixStringHelper.valueIsEqual(screenName, "CustTabContacts")){
						CustTabContacts.populateContactList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, null);
						searchField.addTextChangedListener(new TabSearchTextWatcher(searchText -> CustTabContacts.updateContactList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, searchText))); //));
					}
					else if (MetrixStringHelper.valueIsEqual(screenName, "CustTabHistory")){
						CustTabHistory.populateHistoryList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, null);
						searchField.addTextChangedListener(new TabSearchTextWatcher(searchText -> CustTabHistory.updateHistoryList(this, screenName, thisChildScreenID, tci, _maxRows, searchText)));
					}
					else if (MetrixStringHelper.valueIsEqual(screenName, "CustTabAttributes")){
						CustTabAttributes.populateAttributeList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, null);
						searchField.addTextChangedListener(new TabSearchTextWatcher(searchText -> CustTabAttributes.updateAttributeList(this, screenName, thisChildScreenID, tci, _maxRows, searchText)));
					}
					else {
						// this is a codeless list tab child
						setupCodelessListTabChild(thisChildLayout, tci, screenName, _maxRows, null);
						searchField.addTextChangedListener(new TabSearchTextWatcher(searchText -> setupCodelessListTabChild(thisChildLayout, tci, screenName, _maxRows, searchText)));
					}

					if (tci.mCount < 2)
						searchField.setVisibility(View.GONE);
					else
						searchField.setVisibility(View.VISIBLE);
				}
				tci.mLayout = thisChildLayout;

				setSkinBasedColorsOnRelevantControls(thisChildLayout, MetrixSkinManager.getPrimaryColor(), MetrixSkinManager.getSecondaryColor(), MetrixSkinManager.getHyperlinkColor(), thisChildLayout, true);

				// now, add tabs to the tab host
				TabHost.TabSpec specThisChild = mTabHost.newTabSpec(screenName);
				specThisChild.setContent(tci.mLayoutId);
				specThisChild.setIndicator(GetTabLabel(tci));

				mTabHost.addTab(specThisChild);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	@Override
	public void onItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		MetrixTabScreenManager.TabChildInfo tci = mTabChildren.get(mCurrentScreenName);
		if (tci != null && tci.mLayout.findViewById(tci.mSearchFieldId) != null) {
			MetrixAutoCompleteHelper.saveAutoCompleteFilter(tci.mScreenId, tci.mLayout.findViewById(tci.mSearchFieldId));
		}
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this)))
			return;

		if (tci != null && "CustTabProducts".equals(mCurrentScreenName)) {
			//The Customer Products tab is being viewed
			String productId = listItemData.get("product.product_id");
			MetrixCurrentKeysHelper.setKeyValue("product", "product_id", productId);
			Intent intent = new Intent(this, ProductHistory.class);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void processStartingTab() {
		// Currently, this really only supports navigating from an attribute screen, so we want to ensure CustTabAttributes refreshes
		if (!MetrixStringHelper.isNullOrEmpty(mStartingTabName)) {
			int defaultTab = 0;
			MetrixTabScreenManager.TabChildInfo tci = mTabChildren.get(mStartingTabName);
			if (tci != null) {
				defaultTab = tci.mTabIndex;
				if (MetrixStringHelper.valueIsEqual(mStartingTabName, "CustTabAttributes")) {
					// refresh data in the recyclerView and tab text
					int thisChildScreenID = Integer.valueOf(tci.mScreenId);
					final RecyclerView recyclerView = tci.mLayout.findViewById(R.id.tab_child_recyclerview);
					CustTabAttributes.populateAttributeList(this, mStartingTabName, thisChildScreenID, recyclerView, tci, _maxRows, null);

					String tabLabel = null;
					if (tci.mCount > 0){
						tabLabel = String.format("%1$s (%2$s)", tci.mTabTitle, String.valueOf(tci.mCount));
					}
					else{
						tabLabel = String.format("%1$s", tci.mTabTitle);
					}
					((TextView) mTabHost.getTabWidget().getChildAt(tci.mTabIndex).findViewById(android.R.id.title)).setText(tabLabel);

					final MetadataRecyclerViewAdapter adapter = tci.mListAdapter;
					if (adapter != null)
						adapter.setLongClickListener((pos, data, view) -> {
							mAttrSelectedPosition = pos;
							if (onCreateMetrixActionViewListner != null)
								onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, pos);
						});
				}
			}

			mTabHost.setCurrentTab(defaultTab);
			mStartingTabName = "";
		}
	}
}