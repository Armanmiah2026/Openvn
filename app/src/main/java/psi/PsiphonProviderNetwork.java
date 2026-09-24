package psi;

public interface PsiphonProviderNetwork {
    String getNetworkID();

    long hasNetworkConnectivity();

    String iPv6Synthesize(String str);
}