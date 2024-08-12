package com.metrix.architecture.designer;

import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.SettingsHelper;

public class MetrixDesignerHelper {

    public static void refreshMetadataCaches() {
        // regenerate any/all caches of design metadata, so that we don't retain stuff from previous design/revision
        MetrixClientScriptManager.clearClientScriptCache();
        MetrixFieldManager.clearDefaultValuesCache();
        MetrixFieldManager.clearFieldPropertiesCache();
        MetrixFieldLookupManager.clearFieldLookupCache();
        MetrixGlobalMenuManager.cacheGlobalMenuItems();
        MetrixHomeMenuManager.cacheHomeMenuItems();
        MetrixFilterSortManager.clearFilterSortCaches();
        MetrixListScreenManager.clearItemFieldPropertiesCache();
        MetrixScreenManager.clearScreenIdsCache();
        MetrixSkinManager.cacheSkinItems();
        MetrixWorkflowManager.clearAllWorkflowCaches();
        MetrixScreenManager.clearScreenPropertiesCache();
        MetrixTabScreenManager.clearTabScreensCache();
        /**Store login image id and necessary skin information*/
        SettingsHelper.storeLoginImageInformation();
    }
}
