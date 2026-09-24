package com.alinaj.dev.psiphon;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/06/16
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PsiphonVPNService extends VpnService {
    private final TunnelManager m_Manager = new TunnelManager(this);

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Note that this will be called no matter what the current LocaleManager locale is, be it system or not.
        // So we want to always have the TunnelManager update their context to have the new configuration.
        // We don't need to update the notifications though if the language is not set to default.
        // Also note that if this service is stopped when the system language is changed, notifications like the
        // upgrade one will not be updated until something else triggers them to be updated. This could be fixed by
        // adding a broadcast receiver for locale changes but ATM it feels not worth the effort.
        m_Manager.updateContext(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Need to use super class behavior in specified cases:
        // http://developer.android.com/reference/android/net/VpnService.html#onBind%28android.content.Intent%29
        String action = intent.getAction();
        if (action != null && action.equals(SERVICE_INTERFACE)) {
            return super.onBind(intent);
        }
        return m_Manager.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return m_Manager.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        m_Manager.onCreate();
    }

    @Override
    public void onDestroy() {
        m_Manager.onDestroy();
    }

    @Override
    public void onRevoke() {
        m_Manager.onRevoke();
    }

    public Builder newBuilder() {
        return new Builder();
    }
}
