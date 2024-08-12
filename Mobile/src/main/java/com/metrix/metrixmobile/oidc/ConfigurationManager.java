package com.metrix.metrixmobile.oidc;

import android.content.Context;
import android.util.Log;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.OidcHelper;

import net.openid.appauth.AuthState;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;

public class ConfigurationManager {
    private static final AtomicReference<WeakReference<ConfigurationManager>> INSTANCE_REF =
            new AtomicReference(new WeakReference(null));
    private final AtomicReference<Configuration> mCurrentConfiguration;
    private Configuration mConfiguration;

    public static ConfigurationManager getInstance() {
        ConfigurationManager manager = INSTANCE_REF.get().get();
        if (manager == null) {
            manager = new ConfigurationManager();
        }

        return manager;
    }

    private ConfigurationManager() {
        mCurrentConfiguration = new AtomicReference();
    }

    public Configuration getCurrent() {
        if (mCurrentConfiguration.get() != null) {
            return mCurrentConfiguration.get();
        }

        Configuration state = mConfiguration;
        if (mCurrentConfiguration.compareAndSet(null, state)) {
            return state;
        } else {
            return mCurrentConfiguration.get();
        }
    }

    public void refreshConfiguration() {
        Hashtable<String, String> mLatestOidcSettings = OidcHelper.retrieveOidcSettings(MobileApplication.getAppContext());
        mConfiguration = Configuration.getInstance(MobileApplication.getAppContext(), mLatestOidcSettings);

        if (!mConfiguration.isValid()) {
            return;
        }

        if (mConfiguration.hasConfigurationChanged()) {
            mConfiguration.acceptConfiguration();
        }
    }
}
