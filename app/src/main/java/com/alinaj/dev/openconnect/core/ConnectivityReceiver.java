package com.alinaj.dev.openconnect.core;

import android.content.Context;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class ConnectivityReceiver extends ConnectivityReceiverBase {

    private final String TAG = "ConnectivityReceiver";
    private ConnectionState currentState = getConnectionState();
    private final OpenVPNManagement management;

    public ConnectivityReceiver(Context context, OpenVPNManagement management2) {
        super(context);
        this.management = management2;
    }

    @Override // com.alinaj.dev.connectivity.ConnectivityReceiverBase
    public void onAvailable(Object obj) {
        checkNewState();
    }

    @Override // com.alinaj.dev.connectivity.ConnectivityReceiverBase
    public void onLost(Object obj) {
        checkNewState();
    }

    private void checkNewState() {
        ConnectionState connectionState = getConnectionState();
        if (this.currentState.hasChanged(connectionState)) {
            onStateChange(connectionState);
        }
        this.currentState = connectionState;
    }

    private void onStateChange(ConnectionState connectionState) {
        getPVBS();
        if (!this.currentState.isConnected() || !connectionState.isDisconnected()) {
            if (!this.currentState.isDisconnected() || !connectionState.isConnected()) {
                if (this.management.isActive() && this.management.isPaused()) {
                    this.management.reconnect();
                }
            } else if (this.management.isPaused() && this.management.isActive()) {
                this.management.reconnect();
            }
        } else if (!this.management.isPaused() && this.management.isActive()) {
            this.management.pause();
        }
    }

    private ConnectionState getConnectionState() {
        return ConnectionState.getInstance(getManager());
    }

    private boolean getPVBS() {
        return false;
    }
}