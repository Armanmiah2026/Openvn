package com.alinaj.dev.openconnect.core;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class DeviceStateReceiver extends BroadcastReceiver {
    public static final String PREF_CHANGED = "app.openconnect.PREF_CHANGED";
    public static final String TAG = "OpenConnect";
    private boolean mKeepaliveActive;
    private final OpenVPNManagement mManagement;
    private boolean mNetchangeReconnect;
    private boolean mNetworkOff;
    private int mNetworkType = -1;
    private boolean mPauseOnScreenOff;
    private boolean mPaused;
    private final SharedPreferences mPrefs;
    private boolean mScreenOff;

    public DeviceStateReceiver(OpenVPNManagement management, SharedPreferences prefs) {
        this.mManagement = management;
        this.mPrefs = prefs;
        readPrefs();
    }

    private void readPrefs() {
        this.mPauseOnScreenOff = this.mPrefs.getBoolean("screenoff", false);
        this.mNetchangeReconnect = this.mPrefs.getBoolean("netchangereconnect", true);
    }

    private void updatePauseState() {
        boolean pause = this.mPauseOnScreenOff && this.mScreenOff && !this.mKeepaliveActive;
        if (this.mNetworkOff) {
            pause = true;
        }
        if (pause && !this.mPaused) {
            Log.i("OpenConnect", "pausing: mScreenOff=" + this.mScreenOff + " mNetworkOff=" + this.mNetworkOff);
        } else if (!pause && this.mPaused) {
            Log.i("OpenConnect", "resuming: mScreenOff=" + this.mScreenOff + " mNetworkOff=" + this.mNetworkOff);
        }
        this.mPaused = pause;
    }

    public void onReceive(Context context, Intent intent) {
        String s = intent.getAction();
        if (PREF_CHANGED.equals(s)) {
            this.mManagement.prefChanged();
            readPrefs();
            networkStateChange(context);
        } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(s)) {
            networkStateChange(context);
        } else if ("android.intent.action.SCREEN_OFF".equals(s)) {
            this.mScreenOff = true;
        } else if ("android.intent.action.SCREEN_ON".equals(s)) {
            this.mScreenOff = false;
        }
    }

    private void networkStateChange(Context context) {
        @SuppressLint("WrongConstant") NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED) {
            this.mNetworkOff = true;
            return;
        }
        int networkType = networkInfo.getType();
        int i = this.mNetworkType;
        if (i != -1 && i != networkType && !this.mPaused && this.mNetchangeReconnect) {
            Log.i("OpenConnect", "reconnecting due to network type change");
            this.mManagement.reconnect();
        }
        this.mNetworkType = networkType;
        this.mNetworkOff = false;
    }

    public void setKeepalive(boolean active) {
        this.mKeepaliveActive = active;
    }
}