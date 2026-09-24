package com.alinaj.dev.openconnect.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.alinaj.dev.openconnect.AuthFormHandler;
import com.alinaj.dev.openconnect.VpnProfile;
import com.alinaj.dev.service.OpenVPNService;
import com.alinaj.dev.service.vpn.logger.SkStatus;
import com.alinaj.dev.utils.ConfigUtil;
import com.alinaj.dev.utils.VPNUtil;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;
import com.alinaj.dev.BuildConfig;
import com.alinaj.dev.R;

import org.infradead.libopenconnect.LibOpenConnect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class OpenConnectManagementThread implements Runnable, OpenVPNManagement {
    public static final String[] DNS = {"8.8.8.8", "8.8.4.4"};
    public static final int STATE_AUTHENTICATED = 2;
    public static final int STATE_AUTHENTICATING = 1;
    public static final int STATE_CONNECTED = 4;
    public static final int STATE_CONNECTING = 3;
    public static final int STATE_DISCONNECTED = 5;
    public static final int STATE_NO_NETWORK = 6;
    public static final String TAG = "OpenConnect";
    private boolean active = false;
    private final HashMap<String, Boolean> mAcceptedCerts = new HashMap<>();
    private final SharedPreferences mAppPrefs;
    private boolean mAuthDone = false;
    private boolean mAuthgroupSet = false;
    private String mCacheDir;
    private final Context mContext;
    private String mFilesDir;
    private String mLastFormDigest;
    private final Object mMainloopLock = new Object();
    private LibOpenConnect mOC;
    private final OpenVpnService mOpenVPNService;
    private final SharedPreferences mPrefs;
    private final VpnProfile mProfile;
    private boolean mReconnecting = false;
    private final HashMap<String, Boolean> mRejectedCerts = new HashMap<>();
    private boolean mRequestDisconnect;
    private boolean mRequestPause;
    private String mServerAddr;
    private boolean mStopping = false;

    public OpenConnectManagementThread(Context context, VpnProfile profile, OpenVpnService openVpnService) {
        this.mContext = context;
        this.mProfile = profile;
        this.mOpenVPNService = openVpnService;
        this.mPrefs = profile.mPrefs;
        this.mAppPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    private String getStringPref(String key) {
        return this.mPrefs.getString(key, "");
    }

    private boolean getBoolPref(String key) {
        return this.mPrefs.getBoolean(key, false);
    }

    private void putStringPref(String key, String value) {
        this.mPrefs.edit().putString(key, value).commit();
    }

    private String formatTime(long in) {
        if (in <= 0) {
            return "NEVER";
        }
        return DateFormat.getDateTimeInstance(3, 3, Locale.US).format(Long.valueOf(in));
    }

    private void updateStatPref(String key) {
        long now = System.currentTimeMillis();
        SharedPreferences sharedPreferences = this.mPrefs;
        long first = sharedPreferences.getLong(key + "_first", now);
        SharedPreferences.Editor ed = this.mPrefs.edit();
        ed.putLong(key, this.mPrefs.getLong(key, 0) + 1);
        ed.putLong(key + "_first", first);
        ed.putLong(key + "_prev", now);
        ed.apply();
    }

    private void logOneStat(String key) {
        long count = this.mPrefs.getLong(key, 0);
        SharedPreferences sharedPreferences = this.mPrefs;
        long first = sharedPreferences.getLong(key + "_first", 0);
        SharedPreferences sharedPreferences2 = this.mPrefs;
        long prev = sharedPreferences2.getLong(key + "_prev", 0);
        log("STAT: " + key + "=" + count + "; first=" + formatTime(first) + "; prev=" + formatTime(prev));
    }

    private void logStats() {
        logOneStat("attempt");
        logOneStat("connect");
        logOneStat("cancel");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void log(String msg) {
        this.mOpenVPNService.log(1, msg);

        OpenVPNService service = VPNUtil.getService();
        if (service != null) {
            service.log_message(msg);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isCertAccepted(String hash) {
        if (this.mAcceptedCerts.containsKey(hash)) {
            return true;
        }
        return getStringPref("ACCEPTED-CERT-" + hash).equals("true");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void acceptCert(String hash, boolean save) {
        this.mAcceptedCerts.put(hash, true);
        if (save) {
            putStringPref("ACCEPTED-CERT-" + hash, "true");
        }
    }

    /* access modifiers changed from: private */
    public class AndroidOC extends LibOpenConnect {
        private AndroidOC() {
        }

        private String getPeerCertSHA1() {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                md.reset();
                md.update(getPeerCertDER());
                Formatter f = new Formatter();
                byte[] digest = md.digest();
                int length = digest.length;
                for (int i = 0; i < length; i++) {
                    f.format("%02X", Byte.valueOf(digest[i]));
                }
                String ret = f.toString();
                f.close();
                return ret;
            } catch (Exception e) {
                OpenConnectManagementThread.this.log("getPeerCertSHA1: could not initialize MessageDigest");
                return null;
            }
        }

        @Override // org.infradead.libopenconnect.LibOpenConnect
        public int onValidatePeerCert(String reason) {
            OpenConnectManagementThread.this.log("CALLBACK: onValidatePeerCert");
            String hash = getPeerCertSHA1().toLowerCase(Locale.US);
            if (OpenConnectManagementThread.this.isCertAccepted(hash)) {
                return 0;
            }
            if (OpenConnectManagementThread.this.mRejectedCerts.containsKey(hash)) {
                return -1;
            }
            if (OpenConnectManagementThread.this.mAuthDone) {
                OpenConnectManagementThread.this.log("AUTH: certificate mismatch on existing connection");
                return -1;
            }
            Integer response = (Integer) OpenConnectManagementThread.this.mOpenVPNService.promptUser(new CertWarningDialog(OpenConnectManagementThread.this.mPrefs, getHostname(), hash, reason));
            boolean z = true;
            if (response.intValue() != 0) {
                OpenConnectManagementThread openConnectManagementThread = OpenConnectManagementThread.this;
                if (response.intValue() != 2) {
                    z = false;
                }
                openConnectManagementThread.acceptCert(hash, z);
                return 0;
            }
            OpenConnectManagementThread.this.log("AUTH: user rejected bad certificate");
            OpenConnectManagementThread.this.mRejectedCerts.put(hash, true);
            return -1;
        }

        @Override // org.infradead.libopenconnect.LibOpenConnect
        public int onWriteNewConfig(byte[] buf) {
            OpenConnectManagementThread.this.log("CALLBACK: onWriteNewConfig");
            return 0;
        }

        @Override // org.infradead.libopenconnect.LibOpenConnect
        public int onProcessAuthForm(AuthForm authForm) {
            OpenConnectManagementThread.this.log("CALLBACK: onProcessAuthForm");
            if (authForm.error != null) {
                OpenConnectManagementThread openConnectManagementThread = OpenConnectManagementThread.this;
                //openConnectManagementThread.log("AUTH: error '" + authForm.error + "'");
            }
            if (authForm.message != null) {
                OpenConnectManagementThread openConnectManagementThread2 = OpenConnectManagementThread.this;
                //openConnectManagementThread2.log("AUTH: message '" + authForm.message + "'");
            }
            OpenConnectManagementThread.this.setState(1);
            AuthFormHandler h = new AuthFormHandler(OpenConnectManagementThread.this.mPrefs, authForm, OpenConnectManagementThread.this.mAuthgroupSet, OpenConnectManagementThread.this.mLastFormDigest);
            Integer response = (Integer) OpenConnectManagementThread.this.mOpenVPNService.promptUser(h);
            if (response.intValue() == 0) {
                OpenConnectManagementThread.this.setState(1);
                OpenConnectManagementThread.this.mLastFormDigest = h.getFormDigest();
            } else if (response.intValue() == 2) {
                OpenConnectManagementThread openConnectManagementThread3 = OpenConnectManagementThread.this;
                String sb = "AUTH: requesting authgroup change " +
                        (OpenConnectManagementThread.this.mAuthgroupSet ? "(interactive)" : "(non-interactive)");
                openConnectManagementThread3.log(sb);
                OpenConnectManagementThread.this.mAuthgroupSet = true;
            } else {
                OpenConnectManagementThread openConnectManagementThread4 = OpenConnectManagementThread.this;
                openConnectManagementThread4.log("AUTH: form result is " + response);
            }
            return response.intValue();
        }

        @Override // org.infradead.libopenconnect.LibOpenConnect
        public void onProgress(int level, String msg) {
            OpenVpnService openVpnService = OpenConnectManagementThread.this.mOpenVPNService;
            openVpnService.log(level, "LIB: " + msg.trim());
        }

        @Override // org.infradead.libopenconnect.LibOpenConnect
        public void onProtectSocket(int fd) {
        }

        @Override // org.infradead.libopenconnect.LibOpenConnect
        public void onStatsUpdate(VPNStats stats) {
            OpenConnectManagementThread.this.mOpenVPNService.setStats(stats);
        }
    }

    public void run() {
        runOCS();
    }

    private void runOCS() {
        this.active = true;
        logStats();
        try {
            if (this.mAppPrefs.getBoolean("loadTunModule", false)) {
                Shell.runRootCommand(new CommandCapture(0, "insmod /system/lib/modules/tun.ko"));
            }
            if (this.mAppPrefs.getBoolean("useCM9Fix", false)) {
                Shell.runRootCommand(new CommandCapture(0, "chown 1000 /dev/tun"));
            }
        } catch (Exception e) {
            log("error running root commands: " + e.getLocalizedMessage());
        }
        this.mStopping = false;
        if (!runVPN()) {
            reconnectVpn();
        }
        setState(5);
        synchronized (this.mMainloopLock) {
            this.mOC.destroy();
            this.mOC = null;
        }
        UserDialog.clearDeferredPrefs();
        this.mOpenVPNService.threadDone();
    }

    public void reconnectVpn() {
        for (int reconNum = 1; reconNum <= 100; reconNum++) {
            if (!this.mStopping) {
                closeVpn();
                this.mReconnecting = true;
                OpenVpnService openVpnService = this.mOpenVPNService;
                openVpnService.setReconnectingState("" + reconNum);
                log("Reconnecting..." + reconNum);
                if (runVPN()) {
                    this.mReconnecting = false;
                    return;
                }
                setState(1);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            } else {
                return;
            }
        }
        this.mReconnecting = false;
    }

    private void closeVpn() {
        this.mOC.cancel();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private synchronized void setState(int state) {
        this.mOpenVPNService.setConnectionState(state);
    }

    private boolean rewriteShell(String s) {
        Matcher m = Pattern.compile("^#![ \\t]*(/\\S+)[ \\t\\n]").matcher(s);
        return m.find() && !new File(m.group(1)).exists();
    }

    private byte[] decodeBase64(String in) throws IllegalArgumentException {
        if (in.matches("^[A-Za-z0-9+/=\\n]+$")) {
            return Base64.decode(in, 0);
        }
        throw new IllegalArgumentException("invalid chars");
    }

    private boolean setExecutable(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            log("PREF: file does not exist");
            return false;
        } else if (f.setExecutable(true)) {
            return true;
        } else {
            throw new IOException();
        }
    }

    private int writeCertOrScript(String path, String prefData, boolean isExecutable) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
        if (isExecutable && rewriteShell(prefData)) {
            writer.write("#!/system/bin/sh\n");
        }
        writer.write(prefData);
        writer.close();
        if (isExecutable) {
            setExecutable(path);
        }
        return prefData.length();
    }

    private int inlineToTempFile(String path, String prefData, boolean isExecutable) throws IOException {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            byte[] data = decodeBase64(prefData);
            int bytes = data.length;
            if (isExecutable) {
                try {
                    if (rewriteShell(new String(data))) {
                        fos.write("#!/system/bin/sh\n".getBytes());
                    }
                } catch (Exception e) {
                }
            }
            fos.write(data);
            fos.close();
            if (!isExecutable) {
                return bytes;
            }
            setExecutable(path);
            return bytes;
        } catch (IllegalArgumentException e2) {
            return writeCertOrScript(path, prefData, isExecutable);
        } catch (IOException e3) {
            return -1;
        }
    }

    private String prefToTempFile(String prefName, boolean isExecutable) throws IOException {
        String srcPath;
        String prefData = getStringPref(prefName);
        String path = this.mCacheDir + File.separator + prefName + ".tmp";
        if (prefData.equals("")) {
            return null;
        }
        if (prefData.startsWith(VpnProfile.INLINE_TAG)) {
            int bytes = inlineToTempFile(path, prefData.substring(10), isExecutable);
            if (bytes < 0) {
                log("PREF: I/O exception writing " + prefName);
                return null;
            }
            log("PREF: wrote out " + path + " (" + bytes + ")");
            return path;
        }
        log("PREF: using existing file " + prefData);
        if (prefData.startsWith("/")) {
            srcPath = prefData;
        } else {
            srcPath = ProfileManager.getCertPath() + prefData;
        }
        if (!isExecutable) {
            return srcPath;
        }
        String contents = AssetExtractor.readStringFromFile(srcPath);
        if (contents == null) {
            return null;
        }
        int bytes2 = writeCertOrScript(path, contents, true);
        if (bytes2 < 0) {
            log("PREF: I/O exception writing " + prefName);
            return null;
        }
        log("PREF: wrote out " + path + " (" + bytes2 + ")");
        return path;
    }

    private boolean setPreferences() {
        String str;
        try {
            String PATH = System.getenv("PATH");
            if (!PATH.startsWith(this.mFilesDir)) {
                PATH = this.mFilesDir + ":" + PATH;
            }
            this.mOC.setProtocol("anyconnect");
            String s = prefToTempFile("custom_csd_wrapper", true);
            LibOpenConnect libOpenConnect = this.mOC;
            if (s != null) {
                str = s;
            } else {
                str = this.mFilesDir + File.separator + "android_csd_anyconnect.sh";
            }
            libOpenConnect.setCSDWrapper(str, this.mCacheDir, PATH);
            String s2 = prefToTempFile("ca_certificate", false);
            if (s2 != null) {
                this.mOC.setCAFile(s2);
            }
            String s3 = prefToTempFile("user_certificate", false);
            String key = prefToTempFile("private_key", false);
            if (s3 != null) {
                if (key == null) {
                    this.mOC.setClientCert(s3, s3);
                } else {
                    this.mOC.setClientCert(s3, key);
                }
            }
            ConfigUtil config = ConfigUtil.getInstance(this.mOpenVPNService);
            int tunnel_type = config.getTunnelType();
            this.mServerAddr = config.getSSHHost();
            if (tunnel_type == 0 || tunnel_type == 1 || tunnel_type == 2) {
                this.mServerAddr += ":" + config.getSSHPortString();
            } else {
                this.mServerAddr += ":" + config.getSSLPort();
            }
            String query = config.getFrontQueryString();
            if (config.isQueryMode()) {
                if (query.contains(":")) {
                    this.mServerAddr = query;
                }
                this.mServerAddr = query + ":80";
            }
            this.mOC.setHTTPProxy("127.0.0.1:" + config.getLocalPort());
            this.mOC.setXMLPost(true);
            this.mOC.setPFS(getBoolPref("require_pfs"));
            String os = getStringPref("reported_os");
            this.mOC.setReportedOS(os);
            if (os.equals("android") || os.equals("apple-ios")) {
                this.mOC.setMobileInfo(BuildConfig.VERSION_NAME, os, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            }
            if (getBoolPref("dpd_override")) {
                try {
                    int dpd = Integer.parseInt(getStringPref("dpd_value"));
                    if (dpd > 0) {
                        this.mOC.setDPD(dpd);
                    }
                } catch (Exception e) {
                    log("DPD: bad dpd_value, ignoring");
                }
            }
            String s4 = getStringPref("software_token");
            String token = getStringPref("token_string");
            int ret = 0;
            if (s4.equals("securid")) {
                ret = this.mOC.setTokenMode(1, token);
            } else if (s4.equals("totp")) {
                ret = this.mOC.setTokenMode(2, token);
            }
            if (ret < 0) {
                log("Error " + ret + " setting token string");
                return false;
            }
            prefChanged();
            return true;
        } catch (IOException e2) {
            log("Error writing temporary file");
            return false;
        }
    }

    private void updateLogLevel() {
        if (this.mAppPrefs.getBoolean("trace_log", false)) {
            this.mOC.setLogLevel(3);
        } else {
            this.mOC.setLogLevel(2);
        }
    }

    private boolean getSubnetPref(ArrayList<String> subnets) {
        String[] split = getStringPref("split_tunnel_networks").split("[,\\s]+");
        for (String s : split) {
            if (!s.equals("")) {
                subnets.add(s);
            }
        }
        if (!subnets.isEmpty()) {
            return true;
        }
        log("ROUTE: split tunnel list is empty; check your VPN settings");
        return false;
    }

    private void addDefaultRoutes(VpnService.Builder b, LibOpenConnect.IPInfo ip, ArrayList<String> subnets) {
        boolean ip4def = true;
        boolean ip6def = true;
        Iterator<String> it = subnets.iterator();
        while (it.hasNext()) {
            if (it.next().contains(":")) {
                ip6def = false;
            } else {
                ip4def = false;
            }
        }
        if (ip4def && ip.addr != null) {
            b.addRoute("0.0.0.0", 0);
            log("ROUTE: 0.0.0.0/0");
        }
        if (ip6def && ip.netmask6 != null) {
            b.addRoute("::", 0);
            log("ROUTE: ::/0");
        }
    }

    private void addSubnetRoutes(VpnService.Builder b, LibOpenConnect.IPInfo ip, ArrayList<String> subnets) {
        CIDRIP cdr;
        Iterator<String> it = subnets.iterator();
        while (it.hasNext()) {
            String s = it.next().trim();
            try {
                if (s.contains(":")) {
                    String[] ss = s.split("/");
                    if (ss.length == 1) {
                        b.addRoute(ss[0], 128);
                    } else {
                        b.addRoute(ss[0], Integer.parseInt(ss[1]));
                    }
                    log("ROUTE: " + s);
                } else {
                    if (!s.contains("/")) {
                        cdr = new CIDRIP(s + "/32");
                    } else {
                        cdr = new CIDRIP(s);
                    }
                    b.addRoute(cdr.mIp, cdr.len);
                    log("ROUTE: " + cdr.mIp + "/" + cdr.len);
                }
            } catch (Exception e) {
                log("ROUTE: skipping invalid route '" + s + "'");
            }
        }
    }

    private void setIPInfo(VpnService.Builder b) {
        LibOpenConnect.IPInfo ip = this.mOC.getIPInfo();
        int minMtu = 576;
        if (!(ip.addr == null || ip.netmask == null)) {
            CIDRIP cdr = new CIDRIP(ip.addr, ip.netmask);
            b.addAddress(cdr.mIp, cdr.len);
            log("IPv4: " + cdr.mIp + "/" + cdr.len);
        }
        if (ip.netmask6 != null) {
            String[] ss = ip.netmask6.split("/");
            if (ss.length == 2) {
                b.addAddress(ss[0], Integer.parseInt(ss[1]));
                log("IPv6: " + ip.netmask6);
                minMtu = 1280;
            }
        }
        if (Build.VERSION.SDK_INT >= 19) {
            minMtu = 1280;
        }
        if (ip.MTU < minMtu) {
            b.setMtu(minMtu);
            log("MTU: " + minMtu + " (forced)");
        } else {
            b.setMtu(ip.MTU);
            log("MTU: " + ip.MTU);
        }
        ArrayList<String> subnets = new ArrayList<>();
        ArrayList<String> dns = ip.DNS;
        String domain = ip.domain;
        if (getStringPref("split_tunnel_mode").equals("on_vpn_dns")) {
            getSubnetPref(subnets);
        } else if (getStringPref("split_tunnel_mode").equals("on_uplink_dns")) {
            getSubnetPref(subnets);
            dns = new ArrayList<>();
            domain = null;
        } else {
            subnets = ip.splitIncludes;
            addDefaultRoutes(b, ip, subnets);
        }
        addSubnetRoutes(b, ip, subnets);
        Iterator<String> it = dns.iterator();
        while (it.hasNext()) {
            String s = it.next().trim();
            try {
                b.addDnsServer(s);
                b.addRoute(s, s.contains(":") ? 128 : 32);
                log("DNS: " + s);
            } catch (Exception e) {
                log("DNS: skipping invalid server '" + s + "'");
            }
        }
        if (domain != null) {
            b.addSearchDomain(domain);
            log("DOMAIN: " + domain);
        }
        this.mOpenVPNService.setIPInfo(ip, this.mOC.getHostname(), this.mOC.getIdleTimeout());
    }

    private void errorAlert(String message) {
        this.mOpenVPNService.promptUser(new ErrorDialog(this.mPrefs, this.mContext.getString(R.string.error_connection_failed), message));
    }

    private void errorAlert() {
        errorAlert(this.mContext.getString(R.string.error_cant_connect, this.mOC.getHostname()));
    }

    private void extractBinaries() {
        if (!AssetExtractor.extractAll(this.mContext)) {
            log("Error extracting assets");
        }
        try {
            String curl_bin = this.mFilesDir + "/curl-bin";
            String run_pie = this.mFilesDir + "/run_pie ";
            if (Build.VERSION.SDK_INT >= 16) {
                run_pie = "";
            }
            writeCertOrScript(this.mFilesDir + "/curl", "#!/system/bin/sh\nexec " + run_pie + curl_bin + " \"$@\"\n", true);
        } catch (IOException e) {
            log("Error writing curl wrapper scripts");
        }
    }

    private boolean runVPN() {
        updateStatPref("attempt");
        this.mFilesDir = this.mContext.getFilesDir().getPath();
        this.mCacheDir = this.mContext.getCacheDir().getPath();
        extractBinaries();
        setState(3);
        synchronized (this.mMainloopLock) {
            this.mOC = new AndroidOC();
        }
        if (!setPreferences()) {
            return false;
        }
        if (this.mOC.parseURL(this.mServerAddr) != 0) {
            log("Error parsing server address");
            errorAlert(this.mContext.getString(R.string.error_invalid_hostname, this.mServerAddr));
            return false;
        }
        int ret = this.mOC.obtainCookie();
        if (ret < 0) {
            if (!this.mRejectedCerts.isEmpty() || this.mRequestDisconnect) {
                updateStatPref("cancel");
            } else {
                log("Error obtaining cookie");
                errorAlert();
            }
            return false;
        } else if (ret > 0) {
            log("User canceled auth dialog");
            updateStatPref("cancel");
            return false;
        } else {
            this.mAuthDone = true;
            UserDialog.writeDeferredPrefs();
            setState(2);
            if (this.mOC.makeCSTPConnection() != 0) {
                if (!this.mRequestDisconnect) {
                    log("Error establishing CSTP connection");
                    errorAlert();
                }
                return false;
            }
            VpnService.Builder b = this.mOpenVPNService.getVpnServiceBuilder();
            setIPInfo(b);
            try {
                ParcelFileDescriptor pfd = b.establish();
                if (pfd == null || this.mOC.setupTunFD(pfd.getFd()) != 0) {
                    log("Error setting up tunnel fd");
                    errorAlert();
                    return false;
                }
                SkStatus.updateStateString(SkStatus.SSH_CONNECTED, mContext.getString(R.string.connected));
                setState(4);
                updateStatPref("connect");
                log("<b>Connected</b>");
                this.mOC.setupDTLS(60);
                while (this.mOC.mainloop(5, 10) >= 0) {
                    synchronized (this.mMainloopLock) {
                        if (!this.mRequestDisconnect) {
                            while (this.mRequestPause) {
                                try {
                                    this.mMainloopLock.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                }
                try {
                    pfd.close();
                } catch (IOException e2) {
                }
                return true;
            } catch (Exception e3) {
                log("Exception during establish(): " + e3.getLocalizedMessage());
                return false;
            }
        }
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public void reconnect() {
        log("RECONNECT");
        this.mOpenVPNService.restart();
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public void pause() {
        LibOpenConnect libOpenConnect;
        log("PAUSE");
        setState(6);
        synchronized (this.mMainloopLock) {
            if (!this.mRequestPause && !this.mRequestDisconnect && (libOpenConnect = this.mOC) != null) {
                this.mRequestPause = true;
                libOpenConnect.pause();
            }
        }
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public boolean isPaused() {
        return this.mRequestPause;
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public boolean isActive() {
        return !this.mRequestDisconnect;
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public void resume() {
        log("RESUME");
        synchronized (this.mMainloopLock) {
            if (this.mRequestPause) {
                this.mRequestPause = false;
                this.mMainloopLock.notify();
            }
        }
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public boolean stopVPN() {
        log("STOP");
        log("<b>Disconnected</b>");
        this.mStopping = true;
        this.active = false;
        synchronized (this.mMainloopLock) {
            if (!this.mRequestDisconnect) {
                LibOpenConnect libOpenConnect = this.mOC;
                if (libOpenConnect != null) {
                    this.mRequestDisconnect = true;
                    this.mRequestPause = false;
                    libOpenConnect.cancel();
                    this.mMainloopLock.notify();
                    return true;
                }
            }
            return true;
        }
    }

    public void requestStats() {
        boolean noStats = false;
        synchronized (this.mMainloopLock) {
            if (!this.mRequestPause && !this.mRequestDisconnect) {
                LibOpenConnect libOpenConnect = this.mOC;
                if (libOpenConnect != null) {
                    libOpenConnect.requestStats();
                }
            }
            noStats = true;
        }
        if (noStats) {
            this.mOpenVPNService.setStats(null);
        }
    }

    @Override // com.alinaj.dev.openconnect.core.OpenVPNManagement
    public void prefChanged() {
        updateLogLevel();
    }
}