package com.alinaj.dev.openconnect.core;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import androidx.core.net.ConnectivityManagerCompat;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class ConnectivityReceiverBase {

    private static final String TAG = "service.ConnectivityReceiver";
    private Object _refHandle;
    protected Context context;
    private ConnectivityManager manager = null;

    public void onAvailable(Object obj) {
    }

    public void onLosing(Object obj) {
    }

    public void onLost(Object obj) {
    }

    public ConnectivityReceiverBase(Context context2) {
        this.context = context2;
    }

    public void register() {
        registerFor21AndUp();
        registerFor20AndDown();
    }

    private void registerFor21AndUp() {
        if (Build.VERSION.SDK_INT >= 21) {
            ConnectivityManager manager2 = getManager();
            @SuppressLint("WrongConstant") NetworkRequest build = new NetworkRequest.Builder().addCapability(15).build();
            ConnectivityManager.NetworkCallback anonymousClass1 = new ConnectivityManager.NetworkCallback() {
                /* class com.alinaj.dev.connectivity.ConnectivityReceiverBase.C10611 */

                public void onAvailable(Network network) {
                    Log.i(ConnectivityReceiverBase.TAG, "onAvailable");
                    ConnectivityReceiverBase.this.onAvailable(network);
                }

                public void onLosing(Network network, int i) {
                    Log.i(ConnectivityReceiverBase.TAG, "onLosing");
                    ConnectivityReceiverBase.this.onLosing(network);
                }

                public void onLost(Network network) {
                    Log.i(ConnectivityReceiverBase.TAG, "onLost");
                    ConnectivityReceiverBase.this.onLost(network);
                }
            };
            this._refHandle = anonymousClass1;
            manager2.registerNetworkCallback(build, anonymousClass1);
        }
    }

    private void registerFor20AndDown() {
        if (Build.VERSION.SDK_INT < 21) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            BroadcastReceiver anonymousClass2 = new BroadcastReceiver() {
                /* class com.alinaj.dev.connectivity.ConnectivityReceiverBase.C10622 */

                private boolean isOnline() {
                    NetworkInfo activeNetworkInfo = ConnectivityReceiverBase.this.getManager().getActiveNetworkInfo();
                    return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
                }

                public void onReceive(Context context, Intent intent) {
                    Object stringBuilder;
                    String action = intent.getAction();
                    if (((action.hashCode() != -1172645946 || !action.equals("android.net.conn.CONNECTIVITY_CHANGE")) ? -1 : null) == null) {
                        boolean booleanExtra = intent.getBooleanExtra("noConnectivity", false);
                        boolean booleanExtra2 = intent.getBooleanExtra("isFailover", false);
                        boolean isOnline = isOnline();
                        NetworkInfo networkInfoFromBroadcast = ConnectivityManagerCompat.getNetworkInfoFromBroadcast(ConnectivityReceiverBase.this.getManager(), intent);
                        try {
                            stringBuilder = networkInfoFromBroadcast.getTypeName() + networkInfoFromBroadcast.getSubtypeName();
                        } catch (NullPointerException e) {
                            stringBuilder = "---" + networkInfoFromBroadcast.getSubtypeName();
                        }
                        Log.i(ConnectivityReceiverBase.TAG, String.format("ConnectivityReceiver: CONNECTIVITY_ACTION conn=%b fo=%b", Boolean.valueOf(isOnline), Boolean.valueOf(booleanExtra2)));
                        if (booleanExtra2) {
                            ConnectivityReceiverBase.this.onLosing(stringBuilder);
                        }
                        if (booleanExtra && !isOnline) {
                            ConnectivityReceiverBase.this.onLost(stringBuilder);
                        }
                        if (!booleanExtra && isOnline) {
                            ConnectivityReceiverBase.this.onAvailable(stringBuilder);
                        }
                    }
                }
            };
            this._refHandle = anonymousClass2;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                this.context.registerReceiver(anonymousClass2, intentFilter);
            } else {
                this.context.registerReceiver(anonymousClass2, intentFilter, Context.RECEIVER_EXPORTED);
            }
        }
    }

    public void unregister() {
        unregisterFor21AndUp();
        unregisterFor20AndDown();
    }

    private void unregisterFor21AndUp() {
        if (Build.VERSION.SDK_INT >= 21) {
            getManager().unregisterNetworkCallback((ConnectivityManager.NetworkCallback) this._refHandle);
        }
    }

    private void unregisterFor20AndDown() {
        if (Build.VERSION.SDK_INT < 21) {
            this.context.unregisterReceiver((BroadcastReceiver) this._refHandle);
        }
    }

    /* access modifiers changed from: protected */
    @SuppressLint("WrongConstant")
    public ConnectivityManager getManager() {
        if (this.manager == null) {
            this.manager = (ConnectivityManager) this.context.getSystemService("connectivity");
        }
        return this.manager;
    }
}