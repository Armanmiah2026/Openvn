package psi;

public interface PsiphonProvider extends PsiphonProviderNetwork, PsiphonProviderNoticeHandler {

    String bindToDevice(long j9);

    @Override
    String getNetworkID();

    String getPrimaryDnsServer();

    String getSecondaryDnsServer();

    @Override
    long hasNetworkConnectivity();

    @Override
    String iPv6Synthesize(String str);

    void notice(String str);
}