package com.alinaj.dev.openconnect.core;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

public abstract class ConnectionState {
    public abstract boolean hasChanged(ConnectionState connectionState);

    public abstract boolean isConnected();

    public abstract boolean isDisconnected();

    public static class ConnectionStateV20AndLower extends ConnectionState {
        private boolean hasLte = false;
        private boolean hasWifi = false;

        public ConnectionStateV20AndLower(ConnectivityManager connectivityManager) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(1);
            NetworkInfo networkInfo2 = connectivityManager.getNetworkInfo(0);
            if (networkInfo != null) {
                this.hasWifi = networkInfo.isConnectedOrConnecting();
            }
            if (networkInfo2 != null) {
                this.hasLte = networkInfo2.isConnectedOrConnecting();
            }
        }

        @Override // com.alinaj.dev.connectivity.ConnectionState
        public boolean hasChanged(ConnectionState connectionState) {
            return hasChanged((ConnectionStateV20AndLower) connectionState);
        }

        public boolean hasChanged(ConnectionStateV20AndLower connectionStateV20AndLower) {
            boolean z = this.hasWifi;
            if (z && connectionStateV20AndLower.hasWifi) {
                return false;
            }
            return z != connectionStateV20AndLower.hasWifi || this.hasLte != connectionStateV20AndLower.hasLte;
        }

        @Override // com.alinaj.dev.connectivity.ConnectionState
        public boolean isConnected() {
            return this.hasWifi || this.hasLte;
        }

        @Override // com.alinaj.dev.connectivity.ConnectionState
        public boolean isDisconnected() {
            return !isConnected();
        }
    }

    public static class ConnectionStateV21AndHigher extends ConnectionState {
        private int mLteActiveNetworks = 0;
        private int mWifiActiveNetworks = 0;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public ConnectionStateV21AndHigher(ConnectivityManager connectivityManager) {
            for (Network networkInfo : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo2 = connectivityManager.getNetworkInfo(networkInfo);
                if (networkInfo2 != null && networkInfo2.isConnectedOrConnecting()) {
                    int i = 1;
                    this.mWifiActiveNetworks += networkInfo2.getType() != 1 ? 0 : 1;
                    this.mLteActiveNetworks += networkInfo2.getType() != 0 ? 0 : i;
                }
            }
        }

        @Override // com.alinaj.dev.connectivity.ConnectionState
        public boolean hasChanged(ConnectionState connectionState) {
            return hasChanged((ConnectionStateV21AndHigher) connectionState);
        }

        public boolean hasChanged(ConnectionStateV21AndHigher connectionStateV21AndHigher) {
            int i = this.mWifiActiveNetworks;
            if (i > 0 && connectionStateV21AndHigher.mWifiActiveNetworks > 0) {
                return false;
            }
            return i != connectionStateV21AndHigher.mWifiActiveNetworks || this.mLteActiveNetworks != connectionStateV21AndHigher.mLteActiveNetworks;
        }

        @Override // com.alinaj.dev.connectivity.ConnectionState
        public boolean isConnected() {
            return this.mWifiActiveNetworks > 0 || this.mLteActiveNetworks > 0;
        }

        @Override // com.alinaj.dev.connectivity.ConnectionState
        public boolean isDisconnected() {
            return !isConnected();
        }
    }

    public static ConnectionState getInstance(ConnectivityManager connectivityManager) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ConnectionStateV21AndHigher(connectivityManager);
        }
        return new ConnectionStateV20AndLower(connectivityManager);
    }
}