package com.metrix.architecture.designer;

import androidx.annotation.NonNull;
import android.view.View;

import java.util.HashMap;

public interface MetrixRecyclerViewListener {
    void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view);

    void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view);
}
