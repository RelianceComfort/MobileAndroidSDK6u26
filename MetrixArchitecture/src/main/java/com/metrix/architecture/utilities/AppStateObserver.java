package com.metrix.architecture.utilities;

import android.app.Activity;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.metrix.architecture.database.MobileApplication;

public class AppStateObserver implements LifecycleObserver {
    private boolean appWasInBackground = false;

    public AppStateObserver() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void appGoingToBackground() {
        LogManager.getInstance().debug("FSM Mobile going to background...", null);
        appWasInBackground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void appComingToForeground() {
        LogManager.getInstance().debug("FSM Mobile coming to foreground...", null);

        // If coming back to foreground from background, determine whether we should kick off a Sync cycle within about 5 seconds
        // using the existing startSync API (which has built-in stop/start behavior)
        if (appWasInBackground) {
            try {
                // If current activity is not an authentication page (*Activate* or *Login*)
                // (we include *Login* because it is still a pass-through activity)
                // AND EnableSyncProcess is set to TRUE AND the DB is loaded, then proceed.
                boolean appIsOnNonAuthenticationScreen = false;
                Activity currActivity = MobileApplication.mCurrentActivity.get();
                if (currActivity != null) {
                    String currActivityName = currActivity.getLocalClassName();
                    if (!currActivityName.contains("Activate") && !currActivityName.contains("Login"))
                        appIsOnNonAuthenticationScreen = true;
                }

                boolean syncIsEnabled = Boolean.parseBoolean(MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("EnableSyncProcess")));
                if (appIsOnNonAuthenticationScreen && syncIsEnabled && MobileApplication.DatabaseLoaded && !SettingsHelper.getSyncPause(MobileApplication.getAppContext()))
                    MobileApplication.startSync(MobileApplication.getAppContext());
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        }

        appWasInBackground = false;
    }
}
