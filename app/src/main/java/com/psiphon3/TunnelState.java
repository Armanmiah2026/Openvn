package com.psiphon3;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/06/16
 */

@AutoValue
public abstract class TunnelState {
    public enum Status {
        RUNNING,
        STOPPED,
        UNKNOWN,
    }

    @AutoValue
    public static abstract class ConnectionData {
        public enum NetworkConnectionState {
            CONNECTED,
            CONNECTING,
            WAITING_FOR_NETWORK,
        }

        public abstract NetworkConnectionState networkConnectionState();

        public abstract String clientRegion();

        public abstract String clientVersion();

        public abstract String propagationChannelId();

        public abstract String sponsorId();

        public abstract int httpPort();

        @Nullable
        public abstract ArrayList<String> homePages();

        public static Builder builder() {
            return new AutoValue_TunnelState_ConnectionData.Builder()
                    .setNetworkConnectionState(NetworkConnectionState.CONNECTING)
                    .setClientRegion("")
                    .setClientVersion("")
                    .setPropagationChannelId("")
                    .setSponsorId("")
                    .setHttpPort(0)
                    .setHomePages(null);
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder setNetworkConnectionState(NetworkConnectionState networkConnectionState);

            public abstract Builder setClientRegion(String value);

            public abstract Builder setClientVersion(String value);

            public abstract Builder setPropagationChannelId(String value);

            public abstract Builder setSponsorId(String value);

            public abstract Builder setHttpPort(int port);

            public abstract Builder setHomePages(@Nullable ArrayList<String> homePages);

            public abstract ConnectionData build();
        }

        public boolean isConnected() {
            return networkConnectionState() == NetworkConnectionState.CONNECTED;
        }
    }

    public abstract Status status();

    @Nullable
    public abstract ConnectionData connectionData();

    public static TunnelState unknown() {
        return new AutoValue_TunnelState(Status.UNKNOWN, null);
    }

    public static TunnelState stopped() {
        return new AutoValue_TunnelState(Status.STOPPED, null);
    }

    public static TunnelState running(ConnectionData connectionData) {
        return new AutoValue_TunnelState(Status.RUNNING, connectionData);
    }

    public boolean isRunning() {
        return status() == Status.RUNNING;
    }

    public boolean isUnknown() {
        return status() == Status.UNKNOWN;
    }

    public boolean isStopped() {
        return status() == Status.STOPPED;
    }
}
