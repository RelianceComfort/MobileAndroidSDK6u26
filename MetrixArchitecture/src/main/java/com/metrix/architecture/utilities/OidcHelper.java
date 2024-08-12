package com.metrix.architecture.utilities;

import android.content.Context;
import android.content.Intent;

import java.util.Hashtable;

public class OidcHelper {
    public static Hashtable<String, String> retrieveOidcSettings(Context context) {
        Hashtable<String, String> oidcSettings = new Hashtable<String, String>();

        oidcSettings.put("client_id", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_CLIENT_ID));
        oidcSettings.put("authorization_scope", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_SCOPE));
        oidcSettings.put("authorization_endpoint_uri", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_AUTHORIZATION_ENDPOINT_URI));
        oidcSettings.put("token_endpoint_uri", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_TOKEN_ENDPOINT_URI));
        oidcSettings.put("user_info_endpoint_uri", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_USER_INFO_ENDPOINT_URI));
        oidcSettings.put("end_session_endpoint", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_ENDSESSION_ENDPOINT_URI));
        oidcSettings.put("end_session_redirect_uri", SettingsHelper.getStringSetting(context, SettingsHelper.OIDC_REDIRECT_URI));
        return oidcSettings;
    }
}
