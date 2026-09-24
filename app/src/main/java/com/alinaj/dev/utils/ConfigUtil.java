package com.alinaj.dev.utils;
import android.content.*;
import android.preference.*;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class ConfigUtil
{


    public static final int MODE_SSH_DIRECT = 0, MODE_SSH_DIRECT_WITH_PAYLOAD = 1,
    MODE_SSH_HTTP_PROXY = 2, MODE_SSL_DIRECT = 3, MODE_SSL_DIRECT_WITH_PAYLOAD = 4, 
    MODE_SSL_HTTP_PROXY = 5, MODE_UDP = 6, MODE_V2RAY = 7, MODE_PSIPHON = 8;
    public static final int MODE_OVPN_DIRECT_UDP = 7;
    private static SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private static ConfigUtil instance;

    public final String AD_UNIT_BANNER = "ca-app-pub-39544/63111";
    public final String AD_UNIT_INT = "ca-app-pub-39942544/112";
    public final String AD_UNIT_REWARDED = "ca-app-pub-39544/5917";
    public final String AD_UNIT_APPOPEN = "ca-app-pub-392544/9221";

    public ConfigUtil(Context context)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }
    public static ConfigUtil getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigUtil(context);
        }
        return instance;
    }
    public void setCustomSSLPortEnable(boolean enable)
    {
        editor.putBoolean("CustomSSLPortEnable", enable).apply();
        // TODO: Implement this method
    }
    public boolean isQueryMode()
    {
        return prefs.getBoolean("isQueryMode", false);
    }
    public void setIsQueryMode(boolean enable)
    {
        editor.putBoolean("isQueryMode", enable).apply();
    }
    public String getFrontQueryString()
    {
        return prefs.getString("FrontQuery", "");
    }
    public String getBackQueryString()
    {
        return  prefs.getString("BackQuery", "");
    }
    public void setFrontQuery(String query)
    {
        editor.putString("FrontQuery", query).apply();
    }
    public void setBackQuery(String query)
    {
        editor.putString("BackQuery", query).apply();
    }
    public void setNetworkSelectedName(String name)
    {
        editor.putString("NETWORK_SELECTED_NAME", name).apply();
    }
    public String getNetworkSelectedName()
    {
        // TODO: Implement this method
        return prefs.getString("NETWORK_SELECTED_NAME", "");
    }

    public void setServerSelectedName(String name)
    {
        editor.putString("SERVER_SELECTED_NAME", name).apply();
    }
    public String getServerSelectedName()
    {
        // TODO: Implement this method
        return prefs.getString("SERVER_SELECTED_NAME", "");
    }

    public boolean getCustomSSLPortEnabled()
    {
        return prefs.getBoolean("CustomSSLPortEnable", false);
    }
    public void setSSLPort(String string)
    {
        editor.putString("SSL_PORT", string).apply();
        // TODO: Implement this method
    }
    public void setServerSelectedPosition(int position)
    {
        editor.putInt("SERVER_SELECTED_POSITION", position).apply();
    }
    public int getServerSelectedPosition()
    {
        return prefs.getInt("SERVER_SELECTED_POSITION", 0);
    }
    public void setNetworkSelectedPosition(int position)
    {
        editor.putInt("NETWORK_SELECTED_POSITION", position).apply();
    }
    public int getNetworkSelectedPosition()
    {
        return prefs.getInt("NETWORK_SELECTED_POSITION", 0);
    }
    public void clear()
    {
        editor.clear().apply();
    }
    public String getSSHHost()
    {
        return prefs.getString("SSH_HOST","");
    }
    public int getSSHPort()
    {
        return Integer.parseInt(prefs.getString("SSH_PORT","443"));
    }
    public int getSSLPort()
    {
        return Integer.parseInt(prefs.getString("SSL_PORT","443"));
    }
    public String getSSHPortString()
    {
        return prefs.getString("SSH_PORT", "443");
    }


    public String getUsername() {
        return prefs.getString("X_USERNAME","");
    }
    public String getPassword() {
        return prefs.getString("X_PASSWORD","");
    }

    public String getPayload()
    {
        return prefs.getString("HTTP_PAYLOAD","");
    }
    public String getProxy()
    {
        return prefs.getString("PROXY_HOST", "");
    }
    public String getProxyPort()
    {
        return prefs.getString("PROXY_PORT", "");

    }
    public int getLocalPort() {
        return 8989;
    }

    public boolean getProxyAuthEnabled()
    {
        return prefs.getBoolean("ProxyAuth", false);
    }
    public String getProxyUsername()
    {
        return prefs.getString("ProxyUser", "");
    }
    public String getProxyPassword()
    {
        return prefs.getString("ProxyPass", "");
    }
    public String getBlockApps()
    {
        return prefs.getString("BlockApps", "");
    }
    public boolean getTorrentEnabled()
    {
        return prefs.getBoolean("AntiTorrent", false);
    }
    public void setAntiTorrentEnabled(boolean enabled)
    {
        editor.putBoolean("AntiTorrent", enabled).apply();
    }

    public String getInfo()
    {
        return prefs.getString("ConfigInfo","");
    }
    public String getSni()
    {
        return prefs.getString("SNI", "");
    }
    public void setSni(String sni)
    {
        editor.putString("SNI", sni).apply();
    }
    public int getTunnelType()
    {
        return prefs.getInt("TUNNEL_TYPE", 0);
    }
    public void setTunnelType(int type)
    {
        editor.putInt("TUNNEL_TYPE", type).apply();
    }
    public void setInfo(String info)
    {
        editor.putString("ConfigInfo", info).apply();
    }
    public void setProxyAuthEnabled(boolean enabled)
    {
        editor.putBoolean("ProxyAuth", enabled).apply();
    }
    public void setProxyPassword(String password)
    {
        editor.putString("ProxyPass", password).apply();
    }
    public void setProxyUsername(String username)
    {
        editor.putString("ProxyUser", username).apply();
    }
    public void setHTTPayload(String payload)
    {
        editor.putString("HTTP_PAYLOAD", payload).apply();
    }
    public void setProxy(String proxy)
    {
        editor.putString("PROXY_HOST", proxy).apply();
    }
    public void setProxyPort(String port)
    {
        editor.putString("PROXY_PORT", port).apply();
    }
    public void setLocalPort(String localport)
    {
        editor.putString("LOCAL_PORT", localport).apply();
    }
    public void setSSHHost(String host)
    {
        editor.putString("SSH_HOST", host).apply();
    }
    public void setSSHPort(String port)
    {
        editor.putString("SSH_PORT", port).apply();
    }

    public void setUsername(String username)
    {
        editor.putString("X_USERNAME", username).apply();
    }
    public void setPassword(String password)
    {
        editor.putString("X_PASSWORD", password).apply();
    }

    public static String hide(String str)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {

        // TODO: Implement this method
        return super.toString();
    }
    public String getUDPConfig()
    {
        return prefs.getString("UDPConf", "");
    }


    public void setUDPConfig(String config)
    {
        //editor.putString("MyConfigVersion", version).apply();
        editor.putString("UDPConf", config).apply();
    }
    public String getServerEntry() {
        return prefs.getString("ServerEntry", "");
    }
    public void setServerEntry(String serverEntry) {
        editor.putString("ServerEntry", serverEntry).apply();
    }
    public String getPublicKey() {
        return prefs.getString("PublicKey", "");
    }
    public void setPublicKey(String publicKey) {
        editor.putString("PublicKey", publicKey).apply();
    }
    public String getNameserver() {
        return prefs.getString("Nameserver", "");
    }
    public void setNameserver(String nameserver) {
        editor.putString("Nameserver", nameserver).apply();
    }
    public void setUdp(boolean hmm)
    {
        editor.putBoolean("isUDPSet", hmm).apply();
    }
    public Boolean isUDP()
    {
        return prefs.getBoolean("isUDPSet", false);
    }

    public void setV2RAY(boolean hmm)
    {
        editor.putBoolean("isV2RAYSet", hmm).apply();
    }
    public static Boolean isV2RAY()
    {
        return prefs.getBoolean("isV2RAYSet", false);
    }

    public void setBytesIn (Long config) { editor.putLong("bytesin", config).apply();}

    public long getBytesIn () {return prefs.getLong("bytesin", 0); }

    public void setBytesOut (Long config) { editor.putLong("bytesout", config).apply();}

    public long getBytesOut () {return prefs.getLong("bytesout", 0); }

    public int getProtocol() {
        return prefs.getInt("Protocol", 0);
    }

    public void setProtocol(int protocol) {
        editor.putInt("Protocol", protocol).apply();
    }

    public String getOVPNCert() {
        return prefs.getString("OVPNCert", "");
    }
    public void setOVPNCert(String cert) {
        editor.putString("OVPNCert", cert).apply();
    }
}
