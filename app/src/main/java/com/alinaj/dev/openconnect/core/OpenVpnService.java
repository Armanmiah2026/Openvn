package com.alinaj.dev.openconnect.core;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.alinaj.dev.activities.OpenVPNClient;
import com.alinaj.dev.openconnect.VpnProfile;
import com.alinaj.dev.openconnect.api.GrantPermissionsActivity;
import com.alinaj.dev.service.SocksDNSService;
import com.alinaj.dev.service.vpn.logger.SkStatus;
import com.alinaj.dev.utils.ConfigUtil;
import com.alinaj.dev.utils.StatisticsGraphData;
import com.alinaj.dev.utils.TrafficUtils;
import com.alinaj.dev.R;

import org.infradead.libopenconnect.LibOpenConnect;
import org.infradead.libopenconnect.LibOpenConnect.VPNStats;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class OpenVpnService extends VpnService implements Handler.Callback {

    public static final String ACTION_VPN_STATUS = "app.openconnect.VPN_STATUS";
    public static final String ALWAYS_SHOW_NOTIFICATION = "app.openconnect.NOTIFICATION_ALWAYS_VISIBLE";
    public static final String EXTRA_CONNECTION_STATE = "app.openconnect.connectionState";
    public static final String EXTRA_UUID = "app.openconnect.UUID";
    public static final int SHOW_EXPIRE_DATE = 10;
    public static final String START_SERVICE = "app.openconnect.START_SERVICE";
    public static final String START_SERVICE_STICKY = "app.openconnect.START_SERVICE_STICKY";
    public static final String TAG = "OpenConnect";
    private static long thread_started = 0;
    private final int NOTIFICATION_ID = 1;
    VPNStats deltaStats = new VPNStats();
    public LibOpenConnect.IPInfo ipInfo;
    private int mActivityConnections;
    private final IBinder mBinder = new LocalBinder();
    private long mConnectTime = 0;
    private int mConnectionState = 5;
    private String[] mConnectionStateNames;
    private ConnectivityReceiver mConnectivityReceiver;
    private DeviceStateReceiver mDeviceStateReceiver;
    private UserDialog mDialog;
    private Context mDialogContext;
    private final Handler mHandler = new Handler();
    private int mIdleTimeout;
    private KeepAlive mKeepAlive;
    private ReconnectListener mListener;
    private NotificationManager mNotifManager;
    private boolean mNotificationActive;
    private Notification.Builder mNotifyBuilder;
    private SharedPreferences mPrefs;
    private int mStartId;
    private VPNStats mStats = new VPNStats();
    private int mStatsCount = 0;
    private Handler mStatsHandler;
    private Runnable mStatsRunnable;
    private String mUUID;
    private OpenConnectManagementThread mVPN;
    private final VPNLog mVPNLog = new VPNLog();
    private Thread mVPNThread;
    private Handler myHandler;
    VPNStats newStats = new VPNStats();
    VPNStats oldStats = new VPNStats();
    public VpnProfile profile;
    public String serverName;
    public Date startTime;
    private boolean statsValid = false;
    private static SocksDNSService.TunListener tunListener;
    private StatisticsGraphData.DataTransferStats dataTransferStats;

    public interface ReconnectListener {
        void showReconnectStaus(String str);
    }

    static int access$004(OpenVpnService x0) {
        int i = x0.mStatsCount + 1;
        x0.mStatsCount = i;
        return i;
    }

    public void restart() {
        stopVPN();
        startVPN();
    }

    public static void setTunListener(SocksDNSService.TunListener socksListener2) {
        tunListener = socksListener2;
    }

    private void startVPN() {

        SkStatus.updateStateString(SkStatus.SSH_CONNECTING, getString(R.string.starting_service_ssh));
        killVPNThread(true);
        log(1, "Wakelock Aquired 1");
        mStatsHandler = new Handler();
        mStatsRunnable = new Runnable() {
            @Override
            public void run() {
                OpenVpnService openVpnService = OpenVpnService.this;
                openVpnService.oldStats = openVpnService.newStats;
                OpenVpnService openVpnService2 = OpenVpnService.this;
                openVpnService2.newStats = openVpnService2.getStats();
                OpenVpnService.this.deltaStats.rxBytes = OpenVpnService.this.newStats.rxBytes - OpenVpnService.this.oldStats.rxBytes;
                OpenVpnService.this.deltaStats.rxPkts = OpenVpnService.this.newStats.rxPkts - OpenVpnService.this.oldStats.rxPkts;
                OpenVpnService.this.deltaStats.txBytes = OpenVpnService.this.newStats.txBytes - OpenVpnService.this.oldStats.txBytes;
                OpenVpnService.this.deltaStats.txPkts = OpenVpnService.this.newStats.txPkts - OpenVpnService.this.oldStats.txPkts;
                OpenVpnService.this.requestStats();
                if (OpenVpnService.access$004(OpenVpnService.this) >= 2) {
                    OpenVpnService.this.statsValid = true;
                }
                if (tunListener != null) {
                    tunListener.updateBytesTime();
                }
                dataTransferStats.addBytesSent(OpenVpnService.this.newStats.txBytes);
                dataTransferStats.addBytesReceived(OpenVpnService.this.newStats.rxBytes);
                sendGraphBroadcast();
                OpenVpnService.this.mStatsHandler.postDelayed(OpenVpnService.this.mStatsRunnable, 1000);
            }
        };
        mStatsRunnable.run();
        this.mVPN = new OpenConnectManagementThread(getApplicationContext(), this.profile, this);
        Thread thread = new Thread(this.mVPN, "OpenVPNManagementThread");
        this.mVPNThread = thread;
        thread.start();
        register_connectivity_receiver();
        unregisterReceivers();
        ProfileManager.setConnectedVpnProfile(this.profile);
    }

    public void setReconnectListener(ReconnectListener ReconnectListener2) {
        this.mListener = ReconnectListener2;
    }

    public boolean handleMessage(Message p1) {
        switch (p1.what) {
            case 98:
                String str = (String) p1.obj;
                ReconnectListener reconnectListener = this.mListener;
                if (reconnectListener == null) {
                    return false;
                }
                reconnectListener.showReconnectStaus(str);
                return false;
            case 99:
                int state = ((Integer) p1.obj).intValue();
                if (state == 5) {
                    stopForeground(true);
                } else {
                    updateNotification(state);
                }
                return true;
          /*  case 100:
                sendGraphBroadcast();
                return true;*/
            default:
                return false;
        }
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public OpenVpnService getService() {
            return OpenVpnService.this;
        }
    }

    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(START_SERVICE)) {
            return super.onBind(intent);
        }
        return this.mBinder;
    }

    public void onRevoke() {
        Log.i("OpenConnect", "VPN access has been revoked");
        stopVPN();
    }

    @SuppressLint("WrongConstant")
    public void onCreate() {
        dataTransferStats = StatisticsGraphData.getStatisticData().getDataTransferStats();
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.mPrefs = defaultSharedPreferences;
        this.mUUID = defaultSharedPreferences.getString("service_mUUID", "");
        VPNLog vPNLog = this.mVPNLog;
        vPNLog.restoreFromFile(getCacheDir().getAbsolutePath() + "/logdata.ser");
        this.mConnectionStateNames = getResources().getStringArray(R.array.connection_states);
        this.mNotifManager = (NotificationManager) getSystemService("notification");
        this.myHandler = new Handler(this);
    }

    public void onDestroy() {
        unregister_connectivity_receiver();
        killVPNThread(true);
        DeviceStateReceiver deviceStateReceiver = this.mDeviceStateReceiver;
        if (deviceStateReceiver != null) {
            unregisterReceiver(deviceStateReceiver);
        }
        VPNLog vPNLog = this.mVPNLog;
        vPNLog.saveToFile(getCacheDir().getAbsolutePath() + "/logdata.ser");
    }

    private synchronized boolean doStopVPN() {
        OpenConnectManagementThread openConnectManagementThread = this.mVPN;
        if (openConnectManagementThread == null) {
            return false;
        }
        openConnectManagementThread.stopVPN();
        return true;
    }

    private void killVPNThread(boolean joinThread) {
        if (doStopVPN() && joinThread) {
            try {
                this.mVPNThread.join(1000);
            } catch (InterruptedException e) {
                Log.e("OpenConnect", "OpenConnect thread did not exit");
            }
        }
    }

    private PendingIntent getMainActivityIntent() {
        Intent intent = new Intent(getBaseContext(), OpenVPNClient.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        return PendingIntent.getActivity(this, 0, intent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private synchronized void registerKeepAlive() {
        String DNSServer = "8.8.8.8";
        try {
            String dns = this.ipInfo.DNS.get(0);
            if (InetAddress.getByName(dns) != null) {
                DNSServer = dns;
            }
        } catch (IndexOutOfBoundsException e) {
        } catch (Exception e2) {
            Log.i("OpenConnect", "server DNS IP is bogus, falling back to " + DNSServer + " for KeepAlive", e2);
        }
        int idle = this.mIdleTimeout;
        if (idle < 60 || idle > 7200) {
            idle = 1800;
        }
        int idle2 = (idle * 4) / 10;
        Log.d("OpenConnect", "calculated KeepAlive interval: " + idle2 + " seconds");
        IntentFilter filter = new IntentFilter(KeepAlive.ACTION_KEEPALIVE_ALARM);
        KeepAlive keepAlive = new KeepAlive(idle2, DNSServer);
        this.mKeepAlive = keepAlive;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(keepAlive, filter);
        } else {
            registerReceiver(keepAlive, filter, RECEIVER_EXPORTED);
        }
        this.mKeepAlive.start(this);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void unregisterReceivers() {
        try {
            KeepAlive keepAlive = this.mKeepAlive;
            if (keepAlive != null) {
                keepAlive.stop(this);
                unregisterReceiver(this.mKeepAlive);
            }
            this.mKeepAlive = null;
        } catch (IllegalArgumentException iae) {
            Log.w("OpenConnect", "can't unregister KeepAlive", iae);
        }
    }

    @SuppressLint("WrongConstant")
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e("OpenConnect", "OpenVpnService started with null intent");
            stopSelf();
            return 2;
        }
        String action = intent.getAction();
        if (START_SERVICE.equals(action)) {
            return 2;
        }
        if (START_SERVICE_STICKY.equals(action)) {
            return 3;
        }
        String stringExtra = intent.getStringExtra(EXTRA_UUID);
        this.mUUID = stringExtra;
        if (stringExtra == null) {
            return 2;
        }
        this.mPrefs.edit().putString("service_mUUID", this.mUUID).apply();
        VpnProfile vpnProfile = ProfileManager.get(this.mUUID);
        this.profile = vpnProfile;
        if (vpnProfile == null) {
            return 2;
        }
        thread_started = SystemClock.elapsedRealtime();
        startVPN();
        return 2;
    }

    public Builder getVpnServiceBuilder() {
        Builder b = new Builder();
        b.setSession(getString(R.string.app));
        b.setConfigureIntent(getMainActivityIntent());
        return b;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void allowAllFamily(Builder builder) {
        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);
    }

    public synchronized void startActiveDialog(Context context) {
        UserDialog userDialog = this.mDialog;
        if (userDialog != null && this.mDialogContext == null) {
            this.mDialogContext = context;
            userDialog.onStart(context);
        }
    }

    private synchronized void setDialog(Context context, UserDialog dialog) {
        this.mDialogContext = context;
        this.mDialog = dialog;
    }

    public static int getDuration() {
        return ((int) (SystemClock.elapsedRealtime() - thread_started)) / 1000;
    }

    @SuppressLint("WrongConstant")
    private void updateNotification(int state) {
        String mStatus = this.mConnectionStateNames[state];
        if (mStatus != null) {
            Notification.Builder builder = new Notification.Builder(this);
            this.mNotifyBuilder = builder;
            builder.setSmallIcon(R.drawable.ic_launcher);
            this.mNotifyBuilder.setContentTitle("Connected to " + new ConfigUtil(this).getServerSelectedName());
            this.mNotifyBuilder.setContentText(mStatus);
            this.mNotifyBuilder.setPriority(2);
            this.mNotifyBuilder.setOngoing(true);
            this.mNotifyBuilder.setOnlyAlertOnce(true);
            if (state == 4) {
                this.mConnectTime = System.currentTimeMillis();
            }
            this.mNotifyBuilder.setWhen(this.mConnectTime);
            if (Build.VERSION.SDK_INT >= 26) {
                this.mNotifyBuilder.setChannelId(getPackageName());
                createNotificationChannel(this.mNotifManager, getPackageName());
            }
            if (Build.VERSION.SDK_INT >= 16) {
                jbNotification();
            }
            this.mNotifManager.notify(1, this.mNotifyBuilder.getNotification());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startForeground(1, this.mNotifyBuilder.getNotification());
            } else {
                startForeground(1, this.mNotifyBuilder.getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(NotificationManager mNotificationManager, String id) {
        @SuppressLint("WrongConstant") NotificationChannel mChannel = new NotificationChannel(id, "openconnect_channel", 2);
        mChannel.setShowBadge(true);
        mChannel.setDescription("Openconnect Notification");
        mNotificationManager.createNotificationChannel(mChannel);
    }

    private void jbNotification() {
        try {
            this.mNotifyBuilder.getClass().getMethod("setUsesChronometer", Boolean.TYPE).invoke(this.mNotifyBuilder, true);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException e) {
        }
    }

    private void wakeUpActivity() {
        this.mHandler.post(new Runnable() {
            public void run() {
                Intent vpnstatus = new Intent(OpenVpnService.ACTION_VPN_STATUS);
                vpnstatus.putExtra(OpenVpnService.EXTRA_CONNECTION_STATE, OpenVpnService.this.mConnectionState);
                vpnstatus.putExtra(OpenVpnService.EXTRA_UUID, OpenVpnService.this.mUUID);
                OpenVpnService.this.sendBroadcast(vpnstatus, "android.permission.ACCESS_NETWORK_STATE");
                if (OpenVpnService.this.mConnectionState == 4 && OpenVpnService.this.mKeepAlive == null) {
                    OpenVpnService.this.registerKeepAlive();
                }
            }
        });
    }

    public void updateActivityRefcount(int num) {
        this.mActivityConnections += num;
    }

    public Object promptUser(UserDialog dialog) {
        Object ret = dialog.earlyReturn();
        if (ret != null) {
            return ret;
        }
        setDialog(null, dialog);
        wakeUpActivity();
        Object ret2 = this.mDialog.waitForResponse();
        setDialog(null, null);
        return ret2;
    }

    public synchronized void threadDone() {
        final int startId = this.mStartId;
        Log.i("OpenConnect", "VPN thread has terminated");
        this.mVPN = null;
        this.mHandler.post(new Runnable() {
            public void run() {
                if (!OpenVpnService.this.stopSelfResult(startId)) {
                    Log.w("OpenConnect", "not stopping service due to startId mismatch");
                } else {
                    OpenVpnService.this.unregisterReceivers();
                }
            }
        });
    }

    public void setReconnectingState(String state) {
        Message msg = new Message();
        msg.what = 98;
        msg.obj = String.format("Retries : %s", state + "/100");
        this.myHandler.sendMessage(msg);
    }

    public synchronized void setConnectionState(int state) {
        if (state == 4) {
            if (this.mConnectionState != 4) {
                this.startTime = new Date();
            }
        }
        this.mConnectionState = state;
        Message msg = new Message();
        msg.what = 99;
        msg.obj = Integer.valueOf(this.mConnectionState);
        this.myHandler.sendMessage(msg);
        wakeUpActivity();
    }

    public synchronized int getConnectionState() {
        return this.mConnectionState;
    }

    public String getConnectionStateName() {
        return this.mConnectionStateNames[getConnectionState()];
    }

    public void requestStats() {
        OpenConnectManagementThread openConnectManagementThread = this.mVPN;
        if (openConnectManagementThread != null) {
            openConnectManagementThread.requestStats();
        }
    }

    public synchronized void setStats(VPNStats stats) {
        if (stats != null) {
            this.mStats = stats;
        }
        wakeUpActivity();
    }

    public synchronized VPNStats getStats() {
        return this.mStats;
    }

    public synchronized void setIPInfo(LibOpenConnect.IPInfo ipInfo2, String serverName2, int idleTimeout) {
        this.ipInfo = ipInfo2;
        this.serverName = serverName2;
        this.mIdleTimeout = idleTimeout;
    }

    public VPNLog.LogArrayAdapter getArrayAdapter(Context context) {
        return this.mVPNLog.getArrayAdapter(context);
    }

    public void putArrayAdapter(VPNLog.LogArrayAdapter adapter) {
        if (adapter != null) {
            this.mVPNLog.putArrayAdapter(adapter);
        }
    }

    public void log(final int level, final String msg) {
        this.mHandler.post(new Runnable() {
            public void run() {
                OpenVpnService.this.mVPNLog.add(level, msg);
            }
        });
    }

    public void clearLog() {
        this.mVPNLog.clear();
    }

    public String dumpLog() {
        return this.mVPNLog.dump();
    }

    public String getReconnectName() {
        VpnProfile p = ProfileManager.get(this.mUUID);
        if (p == null) {
            return null;
        }
        return p.getName();
    }

    public void startReconnectActivity(Context context) {
        Intent intent = new Intent(context, GrantPermissionsActivity.class);
        intent.putExtra(getPackageName() + GrantPermissionsActivity.EXTRA_UUID, this.mUUID);
        context.startActivity(intent);
    }

    private void register_connectivity_receiver() {
        this.mConnectivityReceiver = new ConnectivityReceiver(this, this.mVPN);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mConnectivityReceiver.register();
    }

    private void unregister_connectivity_receiver() {
        if (this.mConnectivityReceiver != null) {
            this.mConnectivityReceiver.unregister();
        }
    }

    public void stopVPN() {
        SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, getString(R.string.disconnected));
        this.mConnectTime = 0;
        Handler handler = this.mStatsHandler;
        if (handler != null) {
            handler.removeCallbacks(this.mStatsRunnable);
            this.mStatsHandler = null;
        }
        stopForeground(true);
        killVPNThread(true);
        ProfileManager.setConnectedVpnProfileDisconnected();
    }
}