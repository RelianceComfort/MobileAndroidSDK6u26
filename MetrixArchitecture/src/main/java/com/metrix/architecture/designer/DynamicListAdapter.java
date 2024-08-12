package com.metrix.architecture.designer;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class DynamicListAdapter extends BaseAdapter implements ListAdapter {
	protected final int INVALID_ID = -1;
	protected Context thisContext;
	protected List<HashMap<String, String>> mListData;
	protected HashMap<HashMap<String, String>, Integer> mIdMap = new HashMap<HashMap<String, String>, Integer>();
	protected LayoutInflater mInflater;
	protected int mListViewItemResourceID;
	protected HashMap<String, Integer> mListViewItemElementResourceIDs;
	protected boolean mAllowChanges;
	
	public DynamicListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
		DynamicListAdapterSetup(context, table, listViewItemResourceID, lviElemResIDs, true);
	}

	public DynamicListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
		DynamicListAdapterSetup(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
	}
	
	private void DynamicListAdapterSetup(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
		mInflater = LayoutInflater.from(context);
		mListViewItemResourceID = listViewItemResourceID;
		mListViewItemElementResourceIDs = lviElemResIDs;
		mAllowChanges = allowChanges;
		thisContext = context; 
		mListData = table;
        for (int i = 0; i < table.size(); ++i) {
            mIdMap.put(table.get(i), i);
        }
	}
	
	@Override
	public int getCount() {
		return mListData.size();
	}

	@Override
	public HashMap<String, String> getItem(int position) {
		return mListData.get(position);
	}

	@Override
    public long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        HashMap<String, String> item = getItem(position);
        return mIdMap.get(item);
    }
	
    @Override
    public boolean hasStableIds() {
        return true;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return null;
	}
}
