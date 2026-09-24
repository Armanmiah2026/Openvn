package psi;

import go.Seq1;

public abstract class Psi {

    public static final class proxyPsiphonProvider implements Seq1.Proxy, PsiphonProvider {
        private final int refnum;

        public proxyPsiphonProvider(int i9) {
            this.refnum = i9;
            Seq1.trackGoRef(i9, this);
        }

        @Override
        public native String bindToDevice(long j9);

        @Override
        public native String getNetworkID();

        @Override
        public native String getPrimaryDnsServer();

        @Override
        public native String getSecondaryDnsServer();

        @Override
        public native long hasNetworkConnectivity();

        @Override
        public native String iPv6Synthesize(String str);

        @Override
        public int incRefnum() {
            Seq1.incGoRef(this.refnum, this);
            return this.refnum;
        }

        @Override
        public native void notice(String str);
    }

    public static final class proxyPsiphonProviderFeedbackHandler implements Seq1.Proxy, PsiphonProviderFeedbackHandler {
        private final int refnum;

        public proxyPsiphonProviderFeedbackHandler(int i9) {
            this.refnum = i9;
            Seq1.trackGoRef(i9, this);
        }

        @Override
        public int incRefnum() {
            Seq1.incGoRef(this.refnum, this);
            return this.refnum;
        }

        @Override
        public native void sendFeedbackCompleted(Exception exc);
    }

    public static final class proxyPsiphonProviderNetwork implements Seq1.Proxy, PsiphonProviderNetwork {
        private final int refnum;

        public proxyPsiphonProviderNetwork(int i9) {
            this.refnum = i9;
            Seq1.trackGoRef(i9, this);
        }

        @Override
        public native String getNetworkID();

        @Override
        public native long hasNetworkConnectivity();

        @Override
        public native String iPv6Synthesize(String str);

        @Override
        public int incRefnum() {
            Seq1.incGoRef(this.refnum, this);
            return this.refnum;
        }
    }

    public static final class proxyPsiphonProviderNoticeHandler implements Seq1.Proxy, PsiphonProviderNoticeHandler {
        private final int refnum;

        public proxyPsiphonProviderNoticeHandler(int i9) {
            this.refnum = i9;
            Seq1.trackGoRef(i9, this);
        }

        @Override
        public int incRefnum() {
            Seq1.incGoRef(this.refnum, this);
            return this.refnum;
        }

        @Override
        public native void notice(String str);
    }

    static {
        Seq1.touch();
        _init();
    }

    private Psi() {
    }

    private static native void _init();

    public static native String exportExchangePayload();

    public static native String getBuildInfo();

    public static native String getPacketTunnelDNSResolverIPv4Address();

    public static native String getPacketTunnelDNSResolverIPv6Address();

    public static native long getPacketTunnelMTU();

    public static native String homepageFilePath(String str);

    public static native boolean importExchangePayload(String str);

    public static native void noticeUserLog(String str);

    public static native String noticesFilePath(String str);

    public static native String oldNoticesFilePath(String str);

    public static native void reconnectTunnel();

    public static native void setDynamicConfig(String str, String str2);

    public static native void start(String str, String str2, String str3, PsiphonProvider psiphonProvider, boolean z9, boolean z10);

    public static native void startSendFeedback(String str, String str2, String str3, PsiphonProviderFeedbackHandler psiphonProviderFeedbackHandler, PsiphonProviderNetwork psiphonProviderNetwork, PsiphonProviderNoticeHandler psiphonProviderNoticeHandler, boolean z9);

    public static native void stop();

    public static native void stopSendFeedback();

    public static void touch() {
    }

    public static native String upgradeDownloadFilePath(String str);

    public static native void writeRuntimeProfiles(String str, long j9, long j10);
}