package com.metrix.metrixmobile;

import java.util.Map;

import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.MetrixTabActivity;

public class DebriefRequest extends MetrixTabActivity {

	private String _maxRows = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int custTabsScreenID = MetrixScreenManager.getScreenId("DebriefRequest");
		_maxRows = MetrixDatabaseManager.getAppParam("MAX_ROWS");
		performInitialSetup(custTabsScreenID);
	}

	protected void setListeners() {
		for(MetrixTabScreenManager.TabChildInfo tci : mTabChildren.values()){
			final MetadataRecyclerViewAdapter adapter = tci.mListAdapter;
			if(adapter != null) {
				adapter.setClickListener(this);

			}
		}
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
				tvLabel.setText(tci.mLabel);
				TextView tvTip = (TextView)thisChildLayout.findViewById(R.id.tab_child_tip);
				tvTip.setText(tci.mTip);

				if (MetrixStringHelper.valueIsEqual(tci.mScreenType, "STANDARD")) {
					// set up a form def-based layout
					setupCodelessStandardTabChild(thisChildLayout, tci);
				} else {
					// set up a recyclerview-based layout
					int thisChildScreenID = Integer.valueOf(tci.mScreenId);

					// Set up search field
					AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) thisChildLayout.findViewById(R.id.tab_child_search_criteria);
					autoCompleteTextView.setId(tci.mSearchFieldId);
					MetrixAutoCompleteHelper.populateAutoCompleteTextView(tci.mScreenId, autoCompleteTextView, this);
					resourceStrings.add(new ResourceValueObject(tci.mSearchFieldId, "Search", true));

					final RecyclerView recyclerView = thisChildLayout.findViewById(R.id.tab_child_recyclerview);
					MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

					if (MetrixStringHelper.valueIsEqual(screenName, "DebReqTasks")) {
						DebReqTasks.populateTaskList(this, screenName, thisChildScreenID, recyclerView, tci, _maxRows, null);
						autoCompleteTextView.addTextChangedListener(new TabSearchTextWatcher(searchText -> DebReqTasks.updateTaskList(this, screenName, thisChildScreenID, tci, _maxRows, searchText)));
					}
					else if (MetrixStringHelper.valueIsEqual(screenName, "DebReqNotes")){
						DebReqNotes.populateNotesList(recyclerView, tci, _maxRows, null);
						autoCompleteTextView.addTextChangedListener(new TabSearchTextWatcher(DebReqNotes::updateNotesList));
					}
					else {
						// this is a codeless list tab child
						setupCodelessListTabChild(thisChildLayout, tci, screenName, _maxRows, null);
					}

					if (tci.mCount < 2)
						autoCompleteTextView.setVisibility(View.GONE);
					else
						autoCompleteTextView.setVisibility(View.VISIBLE);
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
}
