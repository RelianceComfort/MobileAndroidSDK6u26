/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metrix.metrixmobile.oidc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import com.metrix.metrixmobile.R;

import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.connectivity.DefaultConnectionBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Hashtable;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

/**
 * Reads and validates the demo app configuration from `res/raw/auth_config.json`. Configuration
 * changes are detected by comparing the hash of the last known configuration to the read
 * configuration. When a configuration change is detected, the app state is reset.
 */
public final class Configuration {

    private static final String TAG = "Configuration";

    private static final String PREFS_NAME = "config";
    private static final String KEY_LAST_HASH = "lastHash";

    private static WeakReference<Configuration> sInstance = new WeakReference<Configuration>(null);

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final Resources mResources;

    private JSONObject mConfigJson;
    private String mConfigHash;
    private String mConfigError;

    private String mClientId;
    private String mScope;
    private Uri mRedirectUri;
    private Uri mEndSessionRedirectUri;
    private Uri mDiscoveryUri;
    private Uri mAuthEndpointUri;
    private Uri mTokenEndpointUri;
    private Uri mEndSessionEndpoint;
    private Uri mRegistrationEndpointUri;
    private Uri mUserInfoEndpointUri;
    private boolean mHttpsRequired;

    public static Configuration getInstance(Context context) {
        Configuration config = sInstance.get();
        if (config == null) {
            config = new Configuration(context);
            sInstance = new WeakReference<Configuration>(config);
        }

        return config;
    }

    public static Configuration getInstance(Context context, Hashtable<String, String> latestConfig) {
        Configuration config = sInstance.get();
        if (config == null) {
            config = new Configuration(context, latestConfig);
            sInstance = new WeakReference<Configuration>(config);
        }

        return config;
    }

    public Configuration(Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mResources = context.getResources();

        try {
            readConfiguration();
        } catch (InvalidConfigurationException ex) {
            mConfigError = ex.getMessage();
        }
    }

    public Configuration(Context context, Hashtable<String, String> latestConfig) {
        mContext = context;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mResources = context.getResources();

        try {
            setConfiguration(latestConfig);
        } catch (InvalidConfigurationException ex) {
            mConfigError = ex.getMessage();
        }
    }

    /**
     * Indicates whether the configuration has changed from the last known valid state.
     */
    public boolean hasConfigurationChanged() {
        String lastHash = getLastKnownConfigHash();
        return !mConfigHash.equals(lastHash);
    }

    /**
     * Indicates whether the current configuration is valid.
     */
    public boolean isValid() {
        return mConfigError == null;
    }

    /**
     * Returns a description of the configuration error, if the configuration is invalid.
     */
    @Nullable
    public String getConfigurationError() {
        return mConfigError;
    }

    /**
     * Indicates that the current configuration should be accepted as the "last known valid"
     * configuration.
     */
    public void acceptConfiguration() {
        mPrefs.edit().putString(KEY_LAST_HASH, mConfigHash).apply();
    }

    @Nullable
    public String getClientId() {
        return mClientId;
    }

    @NonNull
    public String getScope() {
        return "openid";
    }

    @NonNull
    public Uri getRedirectUri() {
        return mRedirectUri;
    }

    @Nullable
    public Uri getDiscoveryUri() {
        return mDiscoveryUri;
    }

    @Nullable
    public Uri getEndSessionRedirectUri() { return mEndSessionRedirectUri; }

    @Nullable
    public Uri getAuthEndpointUri() {
        return mAuthEndpointUri;
    }

    @Nullable
    public Uri getTokenEndpointUri() {
        return mTokenEndpointUri;
    }

    @Nullable
    public Uri getEndSessionEndpoint() {
        return mEndSessionEndpoint;
    }

    @Nullable
    public Uri getRegistrationEndpointUri() {
        return mRegistrationEndpointUri;
    }

    @Nullable
    public Uri getUserInfoEndpointUri() {
        return mUserInfoEndpointUri;
    }

    public boolean isHttpsRequired() {
        return mHttpsRequired;
    }

    public ConnectionBuilder getConnectionBuilder() {
//        if (isHttpsRequired()) {
//            return DefaultConnectionBuilder.INSTANCE;
//        }
        return DefaultConnectionBuilder.INSTANCE; //ConnectionBuilderForTesting.INSTANCE;
    }

    private String getLastKnownConfigHash() {
        return mPrefs.getString(KEY_LAST_HASH, null);
    }

    public void setConfiguration(Hashtable<String, String> latestJsonConfig) throws InvalidConfigurationException {
        readConfigurationFile();
        updateConfiguration(latestJsonConfig);
        setProperties();
    }

    private void readConfigurationFile() throws InvalidConfigurationException {
        BufferedSource configSource =
                Okio.buffer(Okio.source(mResources.openRawResource(R.raw.auth_config)));
        Buffer configData = new Buffer();
        try {
            configSource.readAll(configData);
            mConfigJson = new JSONObject(configData.readString(Charset.forName("UTF-8")));
        } catch (IOException ex) {
            throw new InvalidConfigurationException(
                    "Failed to read configuration: " + ex.getMessage());
        } catch (JSONException ex) {
            throw new InvalidConfigurationException(
                    "Unable to parse configuration: " + ex.getMessage());
        }
    }

    private void updateConfiguration(Hashtable<String, String> latestJsonConfig)  {
        if(latestJsonConfig == null)
            return;

        if(mConfigJson != null) {
            try {
                if(latestJsonConfig.containsKey("client_id"))
                    mConfigJson.put("client_id", latestJsonConfig.get("client_id"));
                if(latestJsonConfig.containsKey("authorization_scope"))
                    mConfigJson.put("authorization_scope", latestJsonConfig.get("authorization_scope"));
                if(latestJsonConfig.containsKey("discovery_uri"))
                    mConfigJson.put("discovery_uri", latestJsonConfig.get("discovery_uri"));
                if(latestJsonConfig.containsKey("authorization_endpoint_uri"))
                    mConfigJson.put("authorization_endpoint_uri", latestJsonConfig.get("authorization_endpoint_uri"));
                if(latestJsonConfig.containsKey("token_endpoint_uri"))
                    mConfigJson.put("token_endpoint_uri", latestJsonConfig.get("token_endpoint_uri"));
                if(latestJsonConfig.containsKey("registration_endpoint_uri"))
                    mConfigJson.put("registration_endpoint_uri", latestJsonConfig.get("registration_endpoint_uri"));
                if(latestJsonConfig.containsKey("user_info_endpoint_uri"))
                    mConfigJson.put("user_info_endpoint_uri", latestJsonConfig.get("user_info_endpoint_uri"));
                if(latestJsonConfig.containsKey("end_session_endpoint"))
                    mConfigJson.put("end_session_endpoint", latestJsonConfig.get("end_session_endpoint"));
            } catch (JSONException ex) {

            }
        }
    }

    private void setProperties() throws InvalidConfigurationException  {
        mConfigHash = Base64.encodeToString(mConfigJson.toString().getBytes(), Base64.NO_WRAP);
        mClientId = getConfigString("client_id");
        mScope = getRequiredConfigString("authorization_scope");
        mRedirectUri = getRequiredConfigUri("redirect_uri");

        if (!isRedirectUriRegistered()) {
            throw new InvalidConfigurationException(
                    "redirect_uri is not handled by any activity in this app! "
                            + "Ensure that the appAuthRedirectScheme in your build.gradle file "
                            + "is correctly configured, or that an appropriate intent filter "
                            + "exists in your app manifest.");
        }

        if (getConfigString("discovery_uri") == null) {
            mAuthEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri");

            mTokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri");
            mUserInfoEndpointUri = getRequiredConfigWebUri("user_info_endpoint_uri");
            mEndSessionEndpoint = getRequiredConfigUri("end_session_endpoint");
            mEndSessionRedirectUri = getRequiredConfigUri("end_session_redirect_uri");

            if (mClientId == null) {
                mRegistrationEndpointUri = getRequiredConfigWebUri("registration_endpoint_uri");
            }
        } else {
            mDiscoveryUri = getRequiredConfigWebUri("discovery_uri");
        }

        mHttpsRequired = mConfigJson.optBoolean("https_required", true);
    }

    private void readConfiguration() throws InvalidConfigurationException {
        BufferedSource configSource =
                Okio.buffer(Okio.source(mResources.openRawResource(R.raw.auth_config)));
        Buffer configData = new Buffer();
        try {
            configSource.readAll(configData);
            mConfigJson = new JSONObject(configData.readString(Charset.forName("UTF-8")));
        } catch (IOException ex) {
            throw new InvalidConfigurationException(
                    "Failed to read configuration: " + ex.getMessage());
        } catch (JSONException ex) {
            throw new InvalidConfigurationException(
                    "Unable to parse configuration: " + ex.getMessage());
        }

        mConfigHash = configData.sha256().base64();
        mClientId = getConfigString("client_id");
//        mEndSessionEndpoint = getRequiredConfigUri("end_session_endpoint");
        mScope = getRequiredConfigString("authorization_scope");
        mRedirectUri = getRequiredConfigUri("redirect_uri");
        mEndSessionRedirectUri = getRequiredConfigUri("end_session_redirect_uri");
        if(getEndSessionRedirectUri() == null) {
            mEndSessionRedirectUri = getRequiredConfigUri("redirect_uri");
        }

        if (!isRedirectUriRegistered()) {
            throw new InvalidConfigurationException(
                    "redirect_uri is not handled by any activity in this app! "
                            + "Ensure that the appAuthRedirectScheme in your build.gradle file "
                            + "is correctly configured, or that an appropriate intent filter "
                            + "exists in your app manifest.");
        }

        if (getConfigString("discovery_uri") == null) {
            mAuthEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri");

            mTokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri");
            mUserInfoEndpointUri = getRequiredConfigWebUri("user_info_endpoint_uri");

            if (mClientId == null) {
                mRegistrationEndpointUri = getRequiredConfigWebUri("registration_endpoint_uri");
            }
        } else {
            mDiscoveryUri = getRequiredConfigWebUri("discovery_uri");
        }

        mHttpsRequired = mConfigJson.optBoolean("https_required", true);
    }

    @Nullable
    String getConfigString(String propName) {
        String value = mConfigJson.optString(propName);
        if (value == null) {
            return null;
        }

        value = value.trim();
        if (TextUtils.isEmpty(value)) {
            return null;
        }

        return value;
    }

    @NonNull
    private String getRequiredConfigString(String propName)
            throws InvalidConfigurationException {
        String value = getConfigString(propName);
        if (value == null) {
            throw new InvalidConfigurationException(
                    propName + " is required but not specified in the configuration");
        }

        return value;
    }

    @NonNull
    Uri getRequiredConfigUri(String propName)
            throws InvalidConfigurationException {
        String uriStr = getRequiredConfigString(propName);
        Uri uri;
        try {
            uri = Uri.parse(uriStr);
        } catch (Throwable ex) {
            throw new InvalidConfigurationException(propName + " could not be parsed", ex);
        }

        if (!uri.isHierarchical() || !uri.isAbsolute()) {
            throw new InvalidConfigurationException(
                    propName + " must be hierarchical and absolute");
        }

        if (!TextUtils.isEmpty(uri.getEncodedUserInfo())) {
            throw new InvalidConfigurationException(propName + " must not have user info");
        }

        if (!TextUtils.isEmpty(uri.getEncodedQuery())) {
            throw new InvalidConfigurationException(propName + " must not have query parameters");
        }

        if (!TextUtils.isEmpty(uri.getEncodedFragment())) {
            throw new InvalidConfigurationException(propName + " must not have a fragment");
        }

        return uri;
    }

    Uri getRequiredConfigWebUri(String propName)
            throws InvalidConfigurationException {
        Uri uri = getRequiredConfigUri(propName);
        String scheme = uri.getScheme();
        if (TextUtils.isEmpty(scheme) || !("http".equals(scheme) || "https".equals(scheme))) {
            throw new InvalidConfigurationException(
                    propName + " must have an http or https scheme");
        }

        return uri;
    }

    private boolean isRedirectUriRegistered() {
        // ensure that the redirect URI declared in the configuration is handled by some activity
        // in the app, by querying the package manager speculatively
        Intent redirectIntent = new Intent();
        redirectIntent.setPackage(mContext.getPackageName());
        redirectIntent.setAction(Intent.ACTION_VIEW);
        redirectIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        redirectIntent.setData(mRedirectUri);

        return !mContext.getPackageManager().queryIntentActivities(redirectIntent, 0).isEmpty();
    }

    public static final class InvalidConfigurationException extends Exception {
        InvalidConfigurationException(String reason) {
            super(reason);
        }

        InvalidConfigurationException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }
}
