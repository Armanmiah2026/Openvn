package com.alinaj.dev.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import com.alinaj.dev.activities.OpenVPNClient;
import com.alinaj.dev.service.vpn.logger.SkStatus;
import com.alinaj.dev.tunnel.CIDRIP;
import com.alinaj.dev.tunnel.NetworkSpace;
import com.alinaj.dev.service.vpn.Pdnsd;
import com.alinaj.dev.service.vpn.Tun2Socks;
import com.alinaj.dev.tunnel.VPNUtils;
import com.alinaj.dev.utils.ConfigUtil;
import com.alinaj.dev.utils.StatisticsGraphData;
import com.alinaj.dev.utils.TrafficUtils;
import com.alinaj.dev.utils.VPNUtil;
import com.alinaj.dev.R;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionMonitor;
import com.trilead.ssh2.DynamicPortForwarder;
import com.trilead.ssh2.HTTPProxyData;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.LocalPortForwarder;
import com.trilead.ssh2.ServerHostKeyVerifier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.List;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/02/08
 */

public class SSHService extends VpnService implements ServerHostKeyVerifier, InteractiveCallback, ConnectionMonitor {

    public static final String START_SSH = "START_SSH_SERVICE";
    public static final String STOP_SSH = "STOP_SSH_SERVICE";
    public static final String NOTIFICATION = "notification";
    public static final String LOCAL_SERVER_ADDRESS = "127.0.0.1";
    public static final String LOCAL_SERVER_PORT = "1080";

    private Notification.Builder notification;
    private NotificationManager notificationManager;

    public static volatile boolean connected = false;
    public volatile static boolean isConnecting = false;

    private DynamicPortForwarder dynamicPortForwarder;
    private Connection connection;
    private String mHost;
    private String mUsername;
    private String mPassword;
    private int mLocalPort;
    private boolean isAuthFailed = false;
    private Thread sshThread;
    private int mPort;
    private LocalPortForwarder localPortForwarder;
    private Thread thread;
    private String privateIpAddress;
    private Process pdnsdProcess;
    private String mRouter;
    private ParcelFileDescriptor tunFd;
    private Tun2Socks tun2Socks;
    private Pdnsd mPdnsd;
    private NetworkSpace routes;
    private ConfigUtil config;
    private static long thread_started = 0;
    private Thread dataThread;
    private static SocksDNSService.TunListener tunListener;

    public class SSHTunnelBinder extends Binder {
        public SSHService getService() {
            return SSHService.this;
        }
    }

    public static void setTunListener(SocksDNSService.TunListener socksListener2) {
        tunListener = socksListener2;
    }

    private void authenticate() {
        try {
            if (connection.authenticateWithNone(mUsername)) {
                log("Authenticate with none");
                return;
            }
        } catch (Exception e) {
            log("Host does not support 'none' authentication.");
        }
        try {
            if (this.connection.isAuthMethodAvailable(this.mUsername, "password")) {
                log(R.string.connecting);
                if (this.connection.authenticateWithPassword(this.mUsername, this.mPassword)) {
                    log("Authenticate with password");
                    return;
                }
                log("Authentication Failed");
                isAuthFailed = true;
                SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, getString(R.string.auth_failed));
                //onStatusChanged(1);
            }
        } catch (Exception e) {
        }
    }

    public boolean connect() {
        try {
            log("Waiting for server reply");
            connection = new Connection(mHost, mPort);
            if (config.getTunnelType() != ConfigUtil.MODE_SSH_DIRECT) {
                HTTPProxyData data = new HTTPProxyData("127.0.0.1", mLocalPort);
                connection.setProxyData(data);
            }
            connection.setCompression(true);
            connection.addConnectionMonitor(this);
            //onStatusChanged(0);
            connection.connect(this, 6 * 1000, 60 * 1000);
            connected = true;
            try {
                int i = 0;
                while (connected && !this.connection.isAuthenticationComplete()) {
                    try {
                        int i2 = i + 1;
                        if (i >= 1) {
                            break;
                        }
                        authenticate();
                        Thread.sleep(1000);
                        i = i2;
                    } catch (Exception e) {
                        return false;
                    }
                }
            } catch (Exception e) {
                log("Problem in SSH connection thread during authentication");
                return false;
            }
            try {
                if (connection.isAuthenticationComplete()) {
                    return enablePortForward();
                }
            } catch (Exception e) {
                log("Problem in SSH connection thread during enabling port");
                return false;
            }
        } catch (Exception e) {
            // server host display in error
            log(e.getMessage());
        }

        return false;
    }

    @Override
    public void connectionLost(Throwable th) {

        if (tun2Socks != null && tun2Socks.isAlive()) {
            tun2Socks.interrupt();
        }
        tun2Socks = null;

        if (th == null) {
            stopReconnect();
        } else if (th.getMessage().contains("There was a problem during connect")) {

        } else if (th.getMessage().contains("Closed due to user request")) {

        } else if (th.getMessage().contains("The connect timeout expired")) {
            reconnect();
        } else if (!th.getMessage().contains("socket closed")) {
            log("connection lost: " + th.getMessage());
            reconnect();
        }
    }

    private void reconnect() {
        if (connected && InjectorService.isRunning) {
            while (InjectorService.isRunning) {
                //startService(new Intent(this, InjectorService.class).setAction(InjectorService.ACTION_RESTART));
                onDisconnect();
                log(R.string.reconnecting);
                if (connect()) {
                    log(R.string.connected);
                    VPNHandler(true);
                    return;
                }

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }
            return;
        }
        stopReconnect();
    }

    public void stopReconnect() {
        connected = false;
        stopForeground(true);
        log("Reconnecting Stopped");
        stopSelf();
    }

    public boolean enablePortForward() {
        try {
            localPortForwarder = connection.createLocalPortForwarder(8053, "www.google.com", 80);
            dynamicPortForwarder = connection.createDynamicPortForwarder(new InetSocketAddress("127.0.0.1", 1080));
            log("Forward Successful");
            return true;
        } catch (Exception e) {
            log(e.getMessage());
            log("Could not create local port forward");
            return false;
        }
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        onDisconnect();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new SSHTunnelBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        routes = new NetworkSpace();
        config = new ConfigUtil(this);

        VPNUtil.setVPNProtectListener(new VPNUtil.VPNProtectListener() {
            @Override
            public boolean protectSocket(Socket socket) {
                return protect(socket);
            }
        });
    }

    public void onDisconnect() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                connected = false;
                try {
                    if (localPortForwarder != null) {
                        localPortForwarder.close();
                        localPortForwarder = null;
                    }
                } catch (Exception ignore) {
                }
                try {
                    if (dynamicPortForwarder != null) {
                        dynamicPortForwarder.close();
                        dynamicPortForwarder = null;
                    }
                } catch (Exception ignore) {
                }
                if (connection != null) {
                    connection.close();
                    connection = null;
                }
                try {
                    if (sshThread != null) {
                        sshThread.stop();
                    }
                } catch (Exception e) {
                }
            }
        }).start();

        log(R.string.disconnected);
        SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, getString(R.string.disconnected));

        if (!isAuthFailed) {
            //onStatusChanged(2);
        }

        VPNHandler(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return Service.START_STICKY;
        }

        if (intent.getAction().equals(START_SSH)) {
            log("Building configuration...");
            showNotification();
            thread_started = SystemClock.elapsedRealtime();
            this.dataThread = new Thread(new DataThread(startId));
            this.dataThread.start();

            mHost = config.getSSHHost();
            mPort = config.getTunnelType() > ConfigUtil.MODE_SSH_HTTP_PROXY ? config.getSSLPort() : (config.getSSHPort());
            mUsername = config.getUsername();
            mPassword = config.getPassword();
            mLocalPort = config.getLocalPort();
            isAuthFailed = false;
            log("Start tunnel service");
            sshThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    log(R.string.connecting);
                    updateNotification(R.string.connecting);
                    SkStatus.updateStateString(SkStatus.SSH_CONNECTING, getString(R.string.connecting));
                    if (connect()) {
                        log(R.string.connected);
                        updateNotification(R.string.connected);
                        SkStatus.updateStateString(SkStatus.SSH_CONNECTED, getString(R.string.connected));
                        VPNHandler(true);
                    } else {
                        log(R.string.disconnected);
                        SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, getString(R.string.disconnected));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {
                        }
                        connected = false;
                        stopSelf();
                    }

                    isConnecting = false;
                }
            });
            sshThread.start();
        }

        return Service.START_STICKY;
    }

    private void showNotification() {
        notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Connected to " + config.getServerSelectedName())
                .setContentText(getString(R.string.connecting))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setLocalOnly(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setUsesChronometer(true);
        Intent notificationIntent = new Intent(this, OpenVPNClient.class);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultIntent = PendingIntent.getActivity(this, 0, notificationIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setContentIntent(resultIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION, getClass().getName(), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
            notification.setChannelId(NOTIFICATION);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION.hashCode(), notification.build());
        } else {
            startForeground(NOTIFICATION.hashCode(), notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        }
    }

    private void updateNotification(int state) {
        if (notification != null && SkStatus.isTunnelActive()) {
            notification.setContentText(getString(state));
            notificationManager.notify(NOTIFICATION.hashCode(), notification.build());
        }
    }

    public static int getDuration() {
        return ((int) (SystemClock.elapsedRealtime() - thread_started)) / 1000;
    }

    @Override
    public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) {
        String[] responses = new String[numPrompts];
        for (int i = 0; i < numPrompts; i++) {
            if (prompt[i].toLowerCase().contains("password"))
                responses[i] = mPassword;
        }
        return responses;
    }

    @Override
    public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) {
        try {
            String fingerPrint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public void log(String msg) {
        Log.d("technore_logs", msg);

        OpenVPNService service = VPNUtil.getService();
        if (service != null) {
            service.log_message(msg);
        }
    }

    public void log(int resId) {
        Log.d("technore_logs", getString(resId));
    }

    private void VPNHandler(boolean isConnect) {

        if (isConnect) {
            if (thread != null) {
                thread.interrupt();
            }
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!establishVpn()) {
                            log("Failed to establish the VPN");
                            return;
                        }
                        connectTunnel(LOCAL_SERVER_ADDRESS + ":" + LOCAL_SERVER_PORT, LOCAL_SERVER_ADDRESS + ":7300", true);
                    } catch (Exception e) {
                    }
                }
            }, "VPNThread");
            thread.start();
        } else {
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            disconnectTunnel();
        }
    }

    public synchronized boolean establishVpn() {

        try {

            VPNUtils.PrivateAddress privateAddress = VPNUtils.selectPrivateAddress();
            privateIpAddress = privateAddress.mIpAddress;
            mRouter = privateAddress.mRouter;

            Builder builder = new Builder();
            builder.setSession(getString(R.string.app));
            builder.setMtu(1500);
            builder.addAddress(privateIpAddress, privateAddress.mPrefixLength);

            if (config.getTunnelType() == 9) {
                builder.addDisallowedApplication(getPackageName());
            }

            routes.addIP(new CIDRIP("0.0.0.0", 0), true);
            routes.addIP(new CIDRIP("10.0.0.0", 8), false);
            routes.addIP(new CIDRIP("192.168.42.0", 23), false);
            routes.addIP(new CIDRIP("192.168.44.0", 24), false);
            routes.addIP(new CIDRIP("192.168.49.0", 24), false);

            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");
            mPdnsd = new Pdnsd(this, new String[]{"8.8.8.8", "8.8.4.4"}, 53, privateAddress.mIpAddress, 9395);
            mPdnsd.setOnPdnsdListener(new Pdnsd.OnPdnsdListener() {
                @Override
                public void onStart() {
                    log("pdnsd started");
                }

                @Override
                public void onStop() {
                    log("pdnsd stopped");
                }
            });
            mPdnsd.start();
            routes.addIP(new CIDRIP("8.8.8.8", 32), true);
            routes.addIP(new CIDRIP("8.8.4.4", 32), true);

            Collection<NetworkSpace.IpAddress> positiveIPv4Routes = routes.getPositiveIPList();
            NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);
            for (NetworkSpace.IpAddress route : positiveIPv4Routes) {
                try {
                    if (!multicastRange.containsNet(route)) {
                        builder.addRoute(route.getIPv4Address(), route.networkMask);
                    }
                } catch (IllegalArgumentException ia) {
                    log("Route rejected by Android" + route + " " + ia.getLocalizedMessage());
                }
            }
            /*String excluded = TextUtils.join(", ", routes.getNetworks(false)).replace(config.getServerHost(), "**********");
            log("Routes: " + TextUtils.join(", ", routes.getNetworks(true)));
            log("Routes excluded: " + excluded);*/
            routes.clear();

            tunFd = builder.establish();

            return tunFd != null;

        } catch (Exception e) {
            log("Failed to establish the VPN " + e);
            return false;
        }
    }

    public synchronized void connectTunnel(final String socksServerAddress, final String udpServerAddress, final boolean remoteUdpForwardingEnabled) {

        if (socksServerAddress == null) {
            throw new IllegalArgumentException("Must provide an IP address to a SOCKS server.");
        }
        if (tunFd == null) {
            throw new IllegalStateException("Must establish the VPN before connecting the tunnel.");
        }
        if (tun2Socks != null) {
            throw new IllegalStateException("Tunnel already connected");
        }

        tun2Socks = new Tun2Socks(this, tunFd, 1500, mRouter, "255.255.255.0", socksServerAddress, udpServerAddress, privateIpAddress + ":9395", remoteUdpForwardingEnabled);
        tun2Socks.setOnTun2SocksListener(new Tun2Socks.OnTun2SocksListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStop() {
            }

        });
        tun2Socks.start();

        log("<<b>Connected</b>");
        //onStatusChanged(3);
    }

    public synchronized void disconnectTunnel() {

        if (pdnsdProcess != null) {
            pdnsdProcess.destroy();
            pdnsdProcess = null;
        }
        try {
            if (tunFd != null) {
                tunFd.close();
                tunFd = null;
            }

        } catch (IOException e) {
            log("Failed to close the VPN interface file descriptor.");
        }

        if (mPdnsd != null && mPdnsd.isAlive()) {
            mPdnsd.interrupt();
        }
        mPdnsd = null;

        if (tun2Socks != null && tun2Socks.isAlive()) {
            tun2Socks.interrupt();
        }
        tun2Socks = null;

        notificationManager.cancelAll();
        notification = null;
        if (dataThread != null) {
            dataThread.interrupt();
        }
        stopForeground(true);
        stopSelf();

        log("<b>Disconnected</b>");
    }

    private class DataThread implements Runnable {

        int service_id;

        public DataThread(int i) {
            this.service_id = i;
        }

        @Override
        public void run() {
            synchronized (this) {
                while (SSHService.this.dataThread.getName() != "showNotification") {
                    if (tunListener != null) {
                        tunListener.updateBytesTime();
                    }
                    sendGraphBroadcast();
                    try {
                        wait(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void sendGraphBroadcast() {
        try {
            List<Long> findData = TrafficUtils.getData();
            Intent intent = new Intent(getPackageName() + ".GRAPH");
            intent.putExtra("DOWNLOAD", findData.get(0));
            intent.putExtra("UPLOAD", findData.get(1));
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.d("technore_graph", "Exception sendGraphBroadcast() : " + e);
        }
    }
}
