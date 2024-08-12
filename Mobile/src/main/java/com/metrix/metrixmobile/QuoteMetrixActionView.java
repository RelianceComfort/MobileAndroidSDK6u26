package com.metrix.metrixmobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import androidx.core.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;

import java.util.HashMap;
import java.util.Map;

public class QuoteMetrixActionView {

    /***
     * Populate action menu with the use of Android ActionBar
     * @param menu
     */
    public static void onCreateMetrixActionView(Activity activity, Menu menu) {
        String workflowName = MetrixWorkflowManager.getCurrentWorkflowName(MobileApplication.getAppContext());
        if (MetrixStringHelper.isNullOrEmpty(workflowName))
            workflowName = MetrixWorkflowManager.QUOTE_WORKFLOW;
        int i = 0;
        Map<String, String> jumpToItems = MetrixWorkflowManager.getJumpToMenuForWorkflow(activity, workflowName);
        if (jumpToItems != null && jumpToItems.size() > 0) {
            for (String key : jumpToItems.keySet()) {
                if (!QuoteMetrixActionView.itemShouldBeSkipped(key)) {
                    MenuItem menuItem = menu.add(0,i, 1, jumpToItems.get(key));
                    MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER);
                }
                i++;
            }
        }
    }

    /***
     * Handling each action menu item click events
     * @param activity
     * @param item
     */
    @SuppressLint("DefaultLocale")
    public static void onMetrixActionMenuItemSelected(Activity activity, MenuItem item) {
        String workflowName = MetrixWorkflowManager.getCurrentWorkflowName(MobileApplication.getAppContext());
        if (MetrixStringHelper.isNullOrEmpty(workflowName))
            workflowName = MetrixWorkflowManager.QUOTE_WORKFLOW;
        int workFlowItemIndex = 0;
        Map<String, String> jumpToItems = MetrixWorkflowManager.getJumpToMenuForWorkflow(activity, workflowName);
        if (jumpToItems == null)
            return;
        for (String key : jumpToItems.keySet()) {
            if (item!= null && item.getTitle()!=null && jumpToItems.get(key).compareToIgnoreCase(item.getTitle().toString()) == 0 && item.getItemId() == workFlowItemIndex) {
                if (QuoteMetrixActionView.itemNeedsComplexHandling(key)) {
                    QuoteMetrixActionView.handleComplexActionMenuItem(activity, key);
                } else {
                    if (MetrixApplicationAssistant.screenNameHasClassInCode(key)) {
                        Intent intent = MetrixActivityHelper.createActivityIntent(activity, key);
                        MetrixActivityHelper.startNewActivity(activity, intent);
                    } else {
                        int screenId = MetrixScreenManager.getScreenId(key);
                        HashMap<String, String> screenPropertyMap = MetrixScreenManager.getScreenProperties(screenId);
                        if (screenPropertyMap != null) {
                            String screenType = screenPropertyMap.get("screen_type");
                            if(!MetrixStringHelper.isNullOrEmpty(screenType)) {
                                if (screenType.toLowerCase().contains("standard")) {
                                    Intent	intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataQuoteActivity");
                                    intent.putExtra("ScreenID", screenId);
                                    MetrixActivityHelper.startNewActivity(activity, intent);
                                } else if (screenType.toLowerCase().contains("list")) {
                                    Intent	intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataListQuoteActivity");
                                    intent.putExtra("ScreenID", screenId);
                                    MetrixActivityHelper.startNewActivity(activity, intent);
                                } else
                                    MetrixUIHelper.showSnackbar(activity, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
                            }
                        }
                    }
                }
            }
            workFlowItemIndex ++;
        }
    }

    private static boolean itemShouldBeSkipped(String item) {
        String quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");
        if (MetrixStringHelper.isNullOrEmpty(quoteId))
            return true;

        return false;
    }

    public static boolean itemNeedsComplexHandling(String item) {
        if (item.compareToIgnoreCase("QuoteAttachmentList") == 0){
            return true;
        } else {
            return false;
        }
    }

    @SuppressLint("DefaultLocale")
    public static void handleComplexActionMenuItem(final Activity activity, String item) {
        final String quoteId = MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id");

        if (item.compareToIgnoreCase("QuoteAttachmentList") == 0) {
            AttachmentWidgetManager.openFromWorkFlow(activity, "Quote");
        }
    }
}
