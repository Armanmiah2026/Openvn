package com.alinaj.dev.psiphon;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.net.VpnService.Builder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.psiphon3.TunnelState;
import com.alinaj.dev.service.OpenVPNService;
import com.alinaj.dev.service.SocksDNSService;
import com.alinaj.dev.service.vpn.logger.SkStatus;
import com.alinaj.dev.utils.ConfigUtil;
import com.alinaj.dev.utils.StatisticsGraphData;
import com.alinaj.dev.utils.TrafficUtils;
import com.alinaj.dev.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.psiphon.PsiphonTunnel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/06/16
 */

public class TunnelManager implements PsiphonTunnel.HostService {

    enum ClientToServiceMessage {
        REGISTER,
        UNREGISTER,
        STOP_SERVICE,
        RESTART_TUNNEL,
        CHANGED_LOCALE,
    }

    // Service -> Client
    enum ServiceToClientMessage {
        TUNNEL_CONNECTION_STATE,
        DATA_TRANSFER_STATS,
        PING,
    }

    public static final String INTENT_ACTION_VIEW = "ACTION_VIEW";
    public static final String INTENT_ACTION_HANDSHAKE = "com.psiphon3.psiphonlibrary.TunnelManager.HANDSHAKE";
    public static final String INTENT_ACTION_VPN_REVOKED = "com.psiphon3.psiphonlibrary.TunnelManager.INTENT_ACTION_VPN_REVOKED";
    public static final String INTENT_ACTION_STOP_TUNNEL = "com.psiphon3.psiphonlibrary.TunnelManager.ACTION_STOP_TUNNEL";
    public static final String IS_CLIENT_AN_ACTIVITY = "com.psiphon3.psiphonlibrary.TunnelManager.IS_CLIENT_AN_ACTIVITY";

    // Client -> Service bundle parameter names
    static final String RESET_RECONNECT_FLAG = "resetReconnectFlag";

    // Service -> Client bundle parameter names
    static final String DATA_TUNNEL_STATE_IS_RUNNING = "isRunning";
    static final String DATA_TUNNEL_STATE_NETWORK_CONNECTION_STATE = "networkConnectionState";
    static final String DATA_TUNNEL_STATE_LISTENING_LOCAL_SOCKS_PROXY_PORT = "listeningLocalSocksProxyPort";
    static final String DATA_TUNNEL_STATE_LISTENING_LOCAL_HTTP_PROXY_PORT = "listeningLocalHttpProxyPort";
    static final String DATA_TUNNEL_STATE_CLIENT_REGION = "clientRegion";
    static final String DATA_TUNNEL_STATE_SPONSOR_ID = "sponsorId";
    public static final String DATA_TUNNEL_STATE_HOME_PAGES = "homePages";
    static final String DATA_TRANSFER_STATS_CONNECTED_TIME = "dataTransferStatsConnectedTime";
    static final String DATA_TRANSFER_STATS_TOTAL_BYTES_SENT = "dataTransferStatsTotalBytesSent";
    static final String DATA_TRANSFER_STATS_TOTAL_BYTES_RECEIVED = "dataTransferStatsTotalBytesReceived";
    static final String DATA_TRANSFER_STATS_SLOW_BUCKETS = "dataTransferStatsSlowBuckets";
    static final String DATA_TRANSFER_STATS_SLOW_BUCKETS_LAST_START_TIME = "dataTransferStatsSlowBucketsLastStartTime";
    static final String DATA_TRANSFER_STATS_FAST_BUCKETS = "dataTransferStatsFastBuckets";
    static final String DATA_TRANSFER_STATS_FAST_BUCKETS_LAST_START_TIME = "dataTransferStatsFastBucketsLastStartTime";

    // Tunnel config, received from the client.
    static class Config {
        String egressRegion = "";
        boolean disableTimeouts = false;
        String sponsorId = "67DDC3614847FCEA";
    }

    private Config m_tunnelConfig;

    private void setTunnelConfig(Config config) {
        m_tunnelConfig = config;
    }

    // Shared tunnel state, sent to the client in the HANDSHAKE
    // intent and in the MSG_TUNNEL_CONNECTION_STATE service message.
    public static class State {
        boolean isRunning = false;
        TunnelState.ConnectionData.NetworkConnectionState networkConnectionState = TunnelState.ConnectionData.NetworkConnectionState.CONNECTING;
        int listeningLocalSocksProxyPort = 0;
        int listeningLocalHttpProxyPort = 0;
        String clientRegion = "";
        String sponsorId = "";
        ArrayList<String> homePages = new ArrayList<>();

        boolean isConnected() {
            return networkConnectionState == TunnelState.ConnectionData.NetworkConnectionState.CONNECTED;
        }
    }

    private final State m_tunnelState = new State();

    private static final String NOTIFICATION = "psiphon_notification";

    private final Service m_parentService;
    private static SocksDNSService.TunListener tunListener;
    private Context m_context;
    private boolean m_firstStart = true;
    private CountDownLatch m_tunnelThreadStopSignal;
    private Thread m_tunnelThread;
    private final AtomicBoolean m_startedTunneling;
    private final AtomicBoolean m_isReconnect;
    private final AtomicBoolean m_isStopping;
    private final PsiphonTunnel m_tunnel;
    private String m_lastUpstreamProxyErrorMessage;
    private final Handler m_Handler = new Handler();

    private Notification.Builder notification;
    private NotificationManager notificationManager;
    private StatisticsGraphData.DataTransferStats dataTransferStats;

    private static long thread_started = 0;
    private Timer timer;

    private final BehaviorRelay<TunnelState.ConnectionData.NetworkConnectionState> m_networkConnectionStateBehaviorRelay = BehaviorRelay.create();
    private final PublishRelay<Object> m_newClientPublishRelay = PublishRelay.create();
    private final CompositeDisposable m_compositeDisposable = new CompositeDisposable();

    TunnelManager(Service parentService) {
        m_parentService = parentService;
        m_context = parentService;
        m_startedTunneling = new AtomicBoolean(false);
        m_isReconnect = new AtomicBoolean(false);
        m_isStopping = new AtomicBoolean(false);
        // Note that we are requesting manual control over PsiphonTunnel.routeThroughTunnel() functionality.
        m_tunnel = PsiphonTunnel.newPsiphonTunnel(this, false);
    }

    void onCreate() {

        notificationManager = (NotificationManager) m_parentService.getSystemService(NOTIFICATION_SERVICE);
        dataTransferStats = StatisticsGraphData.getStatisticData().getDataTransferStats();

        m_tunnelState.isRunning = true;

        m_compositeDisposable.clear();
        m_compositeDisposable.add(connectionStatusUpdaterDisposable());

        thread_started = SystemClock.elapsedRealtime();
        showNotification();
    }

    // Implementation of android.app.Service.onStartCommand
    int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && INTENT_ACTION_STOP_TUNNEL.equals(intent.getAction())) {

            notification = null;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }

            //SkStatus.updateStateString("NOPROCESS", "Psiphon disconnected", R.string.app_name, ConnectionStatus.LEVEL_NOPROCESS);
            if (m_tunnelThreadStopSignal == null || m_tunnelThreadStopSignal.getCount() == 0) {
                m_parentService.stopForeground(true);
                m_parentService.stopSelf();
            } else {
                signalStopService();
            }
            return Service.START_NOT_STICKY;
        }

        if (m_firstStart) {
            m_firstStart = false;
            m_tunnelThreadStopSignal = new CountDownLatch(1);
            m_compositeDisposable.add(
                    getTunnelConfigSingle()
                            .doOnSuccess(config -> {
                                setTunnelConfig(config);
                                m_tunnelThread = new Thread(this::runTunnel);
                                m_tunnelThread.start();
                            })
                            .subscribe());
        }
        return Service.START_REDELIVER_INTENT;
    }

    IBinder onBind(Intent intent) {
        return m_incomingMessenger.getBinder();
    }

    // Sends handshake intent and tunnel state updates to the client Activity,
    // also updates service notification.
    private Disposable connectionStatusUpdaterDisposable() {
        return connectionObservable().switchMapSingle(networkConnectionState
                        -> {
                    // If tunnel is not connected return immediately
                    if (networkConnectionState != TunnelState.ConnectionData.NetworkConnectionState.CONNECTED) {
                        return Single.just(networkConnectionState);
                    }
                    // If this is a reconnect return immediately
                    if (m_isReconnect.get()) {
                        return Single.just(networkConnectionState);
                    }
                    // If there are no home pages to show return immediately
                    if (m_tunnelState.homePages == null || m_tunnelState.homePages.size() == 0) {
                        return Single.just(networkConnectionState);
                    }
                    // If OS is less than Android 10 return immediately
                    if (Build.VERSION.SDK_INT < 29) {
                        return Single.just(networkConnectionState);
                    }

                    return Single.just(networkConnectionState);
                })
                .doOnNext(networkConnectionState -> {
                    m_tunnelState.networkConnectionState = networkConnectionState;
                    // Any subsequent onConnected after this first one will be a reconnect.
                    if (networkConnectionState == TunnelState.ConnectionData.NetworkConnectionState.CONNECTED
                            && m_isReconnect.compareAndSet(false, true)) {
                        m_tunnel.routeThroughTunnel();
                        if (m_tunnelState.homePages != null && m_tunnelState.homePages.size() > 0) {
                            sendHandshakeIntent();
                        }
                    }
                    sendClientMessage(ServiceToClientMessage.TUNNEL_CONNECTION_STATE.ordinal(), getTunnelStateBundle());
                    // Don't update notification to CONNECTING, etc., when a stop was commanded.
                    if (!m_isStopping.get()) {
                        // We expect only distinct connection status from connectionObservable
                        // which means we always add a sound / vibration alert to the notification
                        postServiceNotification(networkConnectionState);
                    }
                })
                .subscribe();
    }

    // Implementation of android.app.Service.onDestroy
    void onDestroy() {

        notificationManager.cancelAll();
        notification = null;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        stopAndWaitForTunnel();
        m_compositeDisposable.dispose();
    }

    void onRevoke() {

        SkStatus.logError(R.string.vpn_service_revoked);

        stopAndWaitForTunnel();
        PendingIntent vpnRevokedPendingIntent = getPendingIntent(m_parentService, INTENT_ACTION_VPN_REVOKED);
        // Try and foreground client activity with the vpnRevokedPendingIntent in order to notify user.
        // If Android < 10 or there is a live activity client then send the intent right away,
        // otherwise show a notification.
        if (Build.VERSION.SDK_INT < 29) {
            try {
                vpnRevokedPendingIntent.send(m_parentService, 0, null);
            } catch (PendingIntent.CanceledException e) {
                //SkStatus.logError("vpnRevokedPendingIntent send failed: " + e);
            }
        }
    }

    private void stopAndWaitForTunnel() {
        if (m_tunnelThread == null) {
            return;
        }

        // signalStopService could have been called, but in case is was not, call here.
        // If signalStopService was not already called, the join may block the calling
        // thread for some time.
        signalStopService();

        try {
            m_tunnelThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        m_tunnelThreadStopSignal = null;
        m_tunnelThread = null;
    }

    // signalStopService signals the runTunnel thread to stop. The thread will
    // self-stop the service. This is the preferred method for stopping the
    // Psiphon tunnel service:
    // 1. VpnService doesn't respond to stopService calls
    // 2. The UI will not block while waiting for stopService to return
    public void signalStopService() {
        if (m_tunnelThreadStopSignal != null) {
            m_tunnelThreadStopSignal.countDown();
        }
    }

    private PendingIntent getPendingIntent(Context ctx, final String actionString) {
        return getPendingIntent(ctx, actionString, null);
    }

    private PendingIntent getPendingIntent(Context ctx, final String actionString, final Bundle extras) {
        // This comment is copied from MainActivity::HandleCurrentIntent
        //
        // MainActivity is exposed to other apps because it is declared as an entry point activity of the app in the manifest.
        // For the purpose of handling internal intents, such as handshake, etc., from the tunnel service we have declared a not
        // exported activity alias 'com.psiphon3.psiphonlibrary.TunnelIntentsHandler' that should act as a proxy for MainActivity.
        // We expect our own intents have a component set to 'com.psiphon3.psiphonlibrary.TunnelIntentsHandler', all other intents
        // should be ignored.
        Intent intent = new Intent();
        ComponentName intentComponentName = new ComponentName(m_parentService, "com.psiphon3.psiphonlibrary.TunnelIntentsHandler");
        intent.setComponent(intentComponentName);
        intent.setAction(actionString);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (extras != null) {
            intent.putExtras(extras);
        }

        return PendingIntent.getActivity(
                ctx,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Single<Config> getTunnelConfigSingle() {
        Single<Config> configSingle = Single.fromCallable(() -> {
            Config tunnelConfig = new Config();
            tunnelConfig.egressRegion = "";
            tunnelConfig.disableTimeouts = false;
            return tunnelConfig;
        });
        return configSingle;
    }

    void updateContext(Context context) {
        m_context = context;
    }

    public static int getDuration() {
        return ((int) (SystemClock.elapsedRealtime() - thread_started)) / 1000;
    }

    private void showNotification() {
        notification = new Notification.Builder(m_context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Connected to " + new ConfigUtil(m_context).getServerSelectedName())
                .setContentText(m_parentService.getString(R.string.connecting))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setLocalOnly(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setUsesChronometer(true);
        Intent notificationIntent = new Intent(m_context, OpenVPNService.class);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultIntent = PendingIntent.getActivity(m_context, 0, notificationIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setContentIntent(resultIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION, getClass().getName(), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
            notification.setChannelId(NOTIFICATION);
        }
        m_parentService.startForeground(NOTIFICATION.hashCode(), notification.build());
    }

    private void startStats() {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (tunListener != null) {
                    tunListener.updateBytesTime();
                }
                sendGraphBroadcast();
            }
        }, 0, 1000);
    }

    private void sendGraphBroadcast() {
        try {
            List<Long> findData = TrafficUtils.getData();
            Intent intent = new Intent(m_context.getPackageName() + ".GRAPH");
            intent.putExtra("DOWNLOAD", findData.get(0));
            intent.putExtra("UPLOAD", findData.get(1));
            m_context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.d("technore_graph", "Exception sendGraphBroadcast() : " + e);
        }
    }

    private synchronized void postServiceNotification(TunnelState.ConnectionData.NetworkConnectionState networkConnectionState) {
        if (notificationManager != null) {
            m_Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (networkConnectionState == TunnelState.ConnectionData.NetworkConnectionState.CONNECTED) {
                        startStats();
                    }
                }
            });
        }
    }

    private static class MessengerWrapper {
        @NonNull
        Messenger messenger;
        boolean isActivity;

        MessengerWrapper(@NonNull Messenger messenger, Bundle data) {
            this.messenger = messenger;
            if (data != null) {
                isActivity = data.getBoolean(IS_CLIENT_AN_ACTIVITY, false);
            }
        }

        void send(Message message) throws RemoteException {
            messenger.send(message);
        }
    }

    private final Messenger m_incomingMessenger = new Messenger(
            new IncomingMessageHandler(this));
    private final HashMap<Integer, MessengerWrapper> mClients = new HashMap<>();


    private static class IncomingMessageHandler extends Handler {
        private final WeakReference<TunnelManager> mTunnelManager;
        private final ClientToServiceMessage[] csm = ClientToServiceMessage.values();

        IncomingMessageHandler(TunnelManager manager) {
            mTunnelManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            TunnelManager manager = mTunnelManager.get();
            switch (csm[msg.what]) {
                case REGISTER:
                    if (manager != null) {
                        if (msg.replyTo == null) {
                            SkStatus.logError("Error registering a client: client's messenger is null.");
                            return;
                        }
                        MessengerWrapper client = new MessengerWrapper(msg.replyTo, msg.getData());
                        // Respond immediately to the new client with current connection state and
                        // data stats. All following distinct tunnel connection updates will be provided
                        // by an Rx connectionStatusUpdaterDisposable() subscription to all clients.
                        List<Message> messageList = new ArrayList<>();
                        messageList.add(manager.composeClientMessage(ServiceToClientMessage.TUNNEL_CONNECTION_STATE.ordinal(), manager.getTunnelStateBundle()));
                        //messageList.add(manager.composeClientMessage(ServiceToClientMessage.DATA_TRANSFER_STATS.ordinal(), manager.getDataTransferStatsBundle()));
                        for (Message message : messageList) {
                            try {
                                client.send(message);
                            } catch (RemoteException e) {
                                // Client is dead, do not add it to the clients list
                                return;
                            }
                        }
                        manager.mClients.put(msg.replyTo.hashCode(), client);
                        manager.m_newClientPublishRelay.accept(new Object());
                    }
                    break;

                case UNREGISTER:
                    if (manager != null) {
                        manager.mClients.remove(msg.replyTo.hashCode());
                    }
                    break;

                case STOP_SERVICE:
                    if (manager != null) {
                        // Do not send any more messages after a stop was commanded.
                        // Client side will receive a ServiceConnection.onServiceDisconnected callback
                        // when the service finally stops.
                        manager.mClients.clear();
                        manager.signalStopService();
                    }
                    break;

                case RESTART_TUNNEL:
                    if (manager != null) {
                        final boolean resetReconnectFlag;
                        Bundle data = msg.getData();
                        if (data != null) {
                            resetReconnectFlag = data.getBoolean(RESET_RECONNECT_FLAG, true);
                        } else {
                            resetReconnectFlag = true;
                        }
                        manager.m_compositeDisposable.add(
                                manager.getTunnelConfigSingle()
                                        .doOnSuccess(config -> {
                                            if (resetReconnectFlag) {
                                                manager.m_isReconnect.set(false);
                                            }
                                            manager.setTunnelConfig(config);
                                            manager.onRestartTunnel();
                                        })
                                        .subscribe());
                    }
                    break;

                case CHANGED_LOCALE:
                    if (manager != null) {

                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    private Message composeClientMessage(int what, Bundle data) {
        Message msg = Message.obtain(null, what);
        if (data != null) {
            msg.setData(data);
        }
        return msg;
    }

    private void sendClientMessage(int what, Bundle data) {
        Message msg = composeClientMessage(what, data);
        for (Iterator i = mClients.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry pair = (Map.Entry) i.next();
            MessengerWrapper messenger = (MessengerWrapper) pair.getValue();
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                i.remove();
            }
        }
    }

    private void sendHandshakeIntent() {
        Intent fillInExtras = new Intent();
        fillInExtras.putExtras(getTunnelStateBundle());
        PendingIntent handshakePendingIntent = getPendingIntent(m_parentService, INTENT_ACTION_HANDSHAKE);
        try {
            handshakePendingIntent.send(m_parentService, 0, fillInExtras);
        } catch (PendingIntent.CanceledException e) {
            //SkStatus.logError("handshakePendingIntent send failed: " + e);
        }
    }

    private Bundle getTunnelStateBundle() {
        // Update with the latest sponsorId from the tunnel config
        m_tunnelState.sponsorId = m_tunnelConfig != null ? m_tunnelConfig.sponsorId : "";

        Bundle data = new Bundle();
        data.putBoolean(DATA_TUNNEL_STATE_IS_RUNNING, m_tunnelState.isRunning);
        data.putInt(DATA_TUNNEL_STATE_LISTENING_LOCAL_SOCKS_PROXY_PORT, m_tunnelState.listeningLocalSocksProxyPort);
        data.putInt(DATA_TUNNEL_STATE_LISTENING_LOCAL_HTTP_PROXY_PORT, m_tunnelState.listeningLocalHttpProxyPort);
        data.putSerializable(DATA_TUNNEL_STATE_NETWORK_CONNECTION_STATE, m_tunnelState.networkConnectionState);
        data.putString(DATA_TUNNEL_STATE_CLIENT_REGION, m_tunnelState.clientRegion);
        data.putString(DATA_TUNNEL_STATE_SPONSOR_ID, m_tunnelState.sponsorId);
        data.putStringArrayList(DATA_TUNNEL_STATE_HOME_PAGES, m_tunnelState.homePages);
        return data;
    }

    private final Handler sendDataTransferStatsHandler = new Handler();
    private final long sendDataTransferStatsIntervalMs = 1000;
    private final Runnable sendDataTransferStats = new Runnable() {
        @Override
        public void run() {
            //sendClientMessage(ServiceToClientMessage.DATA_TRANSFER_STATS.ordinal(), getDataTransferStatsBundle());
            //sendDataTransferStatsHandler.postDelayed(this, sendDataTransferStatsIntervalMs);
        }
    };

    private final static String LEGACY_SERVER_ENTRY_FILENAME = "psiphon_server_entries.json";

    static String getServerEntries(Context context) {
        StringBuilder list = new StringBuilder();

        for (String encodedServerEntry : EmbeddedValues.EMBEDDED_SERVER_LIST) {
            list.append(encodedServerEntry);
            list.append("\n");
        }

        // Delete legacy server entries if they exist
        context.deleteFile(LEGACY_SERVER_ENTRY_FILENAME);

        return list.toString();
    }

    private void runTunnel() {

        Utils.initializeSecureRandom();

        m_isReconnect.set(false);
        m_isStopping.set(false);
        m_startedTunneling.set(false);
        m_networkConnectionStateBehaviorRelay.accept(TunnelState.ConnectionData.NetworkConnectionState.CONNECTING);

        SkStatus.updateStateString(SkStatus.SSH_STARTING, m_context.getString(R.string.connecting));
        SkStatus.logInfo(R.string.starting_tunnel);

        m_tunnelState.homePages.clear();

        sendDataTransferStatsHandler.postDelayed(sendDataTransferStats, sendDataTransferStatsIntervalMs);

        try {
            if (!m_tunnel.startRouting()) {
                throw new PsiphonTunnel.Exception("application is not prepared or revoked");
            }
            m_tunnel.startTunneling(getServerEntries(m_parentService));
            m_startedTunneling.set(true);
            try {
                m_tunnelThreadStopSignal.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (PsiphonTunnel.Exception e) {
            String errorMessage = e.getMessage();
            SkStatus.logError(R.string.start_tunnel_failed, errorMessage);
            if ((errorMessage.startsWith("get package uid:") || errorMessage.startsWith("getPackageUid:"))
                    && errorMessage.endsWith("android.permission.INTERACT_ACROSS_USERS.")) {
                SkStatus.logInfo(R.string.vpn_exclusions_conflict);
            }
        } finally {

            m_isStopping.set(true);
            m_networkConnectionStateBehaviorRelay.accept(TunnelState.ConnectionData.NetworkConnectionState.CONNECTING);
            m_tunnel.stop();

            sendDataTransferStatsHandler.removeCallbacks(sendDataTransferStats);

            SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, m_context.getString(R.string.state_disconnected));

            m_parentService.stopForeground(true);
            m_parentService.stopSelf();
        }
    }

    private void onRestartTunnel() {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    m_tunnel.restartPsiphon();
                } catch (PsiphonTunnel.Exception e) {
                    SkStatus.logError(R.string.start_tunnel_failed, e.getMessage());
                }
            }
        });
    }

    @Override
    public String getAppName() {
        return m_parentService.getString(R.string.app);
    }

    @Override
    public Context getContext() {
        return m_context;
    }

    @Override
    public VpnService getVpnService() {
        return ((PsiphonVPNService) m_parentService);
    }

    @Override
    public Builder newVpnServiceBuilder() {
        Builder vpnBuilder = ((PsiphonVPNService) m_parentService).newBuilder();
        // only can control tunneling post lollipop
        if (Build.VERSION.SDK_INT < LOLLIPOP) {
            return vpnBuilder;
        }

//        Added on API 29:
//        Marks the VPN network as metered. A VPN network is classified as metered when the user is
//        sensitive to heavy data usage due to monetary costs and/or data limitations. In such cases,
//        you should set this to true so that apps on the system can avoid doing large data transfers.
//        Otherwise, set this to false. Doing so would cause VPN network to inherit its meteredness
//        from its underlying networks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vpnBuilder.setMetered(false);
        }

        return vpnBuilder;
    }

    private Observable<TunnelState.ConnectionData.NetworkConnectionState> connectionObservable() {
        return m_networkConnectionStateBehaviorRelay
                .hide()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged();
    }

    public String buildTunnelCoreConfig(
            Context context,
            Config tunnelConfig,
            boolean useUpstreamProxy,
            String tempTunnelName) {
        boolean temporaryTunnel = tempTunnelName != null && !tempTunnelName.isEmpty();

        JSONObject json = new JSONObject();

        try {

            json.put("ClientVersion", EmbeddedValues.CLIENT_VERSION);

            /*if (UpgradeChecker.upgradeCheckNeeded(context)) {

                json.put("UpgradeDownloadURLs", new JSONArray(EmbeddedValues.UPGRADE_URLS_JSON));

                json.put("UpgradeDownloadClientVersionHeader", "x-amz-meta-psiphon-client-version");
            }

            json.put("MigrateUpgradeDownloadFilename", new UpgradeManager.OldDownloadedUpgradeFile(context).getFullPath());
*/
            json.put("PropagationChannelId", EmbeddedValues.PROPAGATION_CHANNEL_ID);

            json.put("SponsorId", tunnelConfig.sponsorId);

            json.put("RemoteServerListURLs", new JSONArray(EmbeddedValues.REMOTE_SERVER_LIST_URLS_JSON));

            json.put("ObfuscatedServerListRootURLs", new JSONArray(EmbeddedValues.OBFUSCATED_SERVER_LIST_ROOT_URLS_JSON));

            json.put("RemoteServerListSignaturePublicKey", EmbeddedValues.REMOTE_SERVER_LIST_SIGNATURE_PUBLIC_KEY);

            //json.put("ServerEntrySignaturePublicKey", EmbeddedValues.SERVER_ENTRY_SIGNATURE_PUBLIC_KEY);

            //json.put("ExchangeObfuscationKey", EmbeddedValues.SERVER_ENTRY_EXCHANGE_OBFUSCATION_KEY);

            /*if (useUpstreamProxy) {
                if (UpstreamProxySettings.getUseHTTPProxy(context)) {
                    if (UpstreamProxySettings.getProxySettings(context) != null) {
                        json.put("UpstreamProxyUrl", UpstreamProxySettings.getUpstreamProxyUrl(context));
                    }
                }
            }*/

            json.put("EmitDiagnosticNotices", true);

            json.put("EmitDiagnosticNetworkParameters", true);

            //json.put("FeedbackUploadURLs", new JSONArray(EmbeddedValues.FEEDBACK_DIAGNOSTIC_INFO_UPLOAD_URLS_JSON));
            json.put("FeedbackEncryptionPublicKey", EmbeddedValues.FEEDBACK_ENCRYPTION_PUBLIC_KEY);

            // If this is a temporary tunnel (like for UpgradeChecker) we need to override some of
            // the implicit config values.
            if (temporaryTunnel) {
                File tempTunnelDir = new File(context.getFilesDir(), tempTunnelName);
                if (!tempTunnelDir.exists()
                        && !tempTunnelDir.mkdirs()) {
                    // Failed to create DB directory
                    return null;
                }

                // On Android, these directories must be set to the app private storage area.
                // The Psiphon library won't be able to use its current working directory
                // and the standard temporary directories do not exist.
                json.put("DataRootDirectory", tempTunnelDir.getAbsolutePath());

                json.put("MigrateDataStoreDirectory", tempTunnelDir.getAbsolutePath());

                File remoteServerListDownload = new File(tempTunnelDir, "remote_server_list");
                json.put("MigrateRemoteServerListDownloadFilename", remoteServerListDownload.getAbsolutePath());

                File oslDownloadDir = new File(tempTunnelDir, "osl");
                if (oslDownloadDir.exists()) {
                    json.put("MigrateObfuscatedServerListDownloadDirectory", oslDownloadDir.getAbsolutePath());
                }

                // This number is an arbitrary guess at what might be the "best" balance between
                // wake-lock-battery-burning and successful upgrade downloading.
                // Note that the fall-back untunneled upgrade download doesn't start for 30 secs,
                // so we should be waiting longer than that.
                json.put("EstablishTunnelTimeoutSeconds", 300);

                json.put("TunnelWholeDevice", 0);
                json.put("EgressRegion", "");
            } else {
                String egressRegion = tunnelConfig.egressRegion;
                //VPNLog.i("EgressRegion", "regionCode", egressRegion);
                json.put("EgressRegion", egressRegion);
            }

            if (tunnelConfig.disableTimeouts) {
                //disable timeouts
                //VPNLog.i("DisableTimeouts", "disableTimeouts", true);
                json.put("NetworkLatencyMultiplierLambda", 0.1);
            }

            ConfigUtil config = ConfigUtil.getInstance(context);
            if (!config.getServerEntry().isEmpty()) {
                json.put("TargetServerEntry", config.getServerEntry());
            }

            json.put("EmitServerAlerts", true);

            /*if (Utils.getUnsafeTrafficAlertsOptInState(context)) {
                json.put("ClientFeatures", new JSONArray("[\"unsafe-traffic-alerts\"]"));
            }*/

            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    static public void setPlatformAffixes(PsiphonTunnel tunnel, String clientPlatformPrefix) {
        String prefix = "";
        if (clientPlatformPrefix != null && !clientPlatformPrefix.isEmpty()) {
            prefix = clientPlatformPrefix;
        }

        String suffix = Utils.getClientPlatformSuffix();

        tunnel.setClientPlatformAffixes(prefix, suffix);
    }

    @Override
    public String getPsiphonConfig() {
        setPlatformAffixes(m_tunnel, null);
        String config = buildTunnelCoreConfig(getContext(), m_tunnelConfig, true, null);
        return config == null ? "" : config;
    }

    /*@Override
    public String getPsiphonConfig() {
        m_tunnel.setClientPlatformAffixes("", Utils.getClientPlatformSuffix());
        return "{\n" +
                "  \"ClientVersion\": \"169\",\n" +
                "  \"PropagationChannelId\": \"EE0B7486ACAE75AA\",\n" +
                "  \"SponsorId\": \"67DDC3614847FCEA\",\n" +
                "  \"RemoteServerListURLs\": [\n" +
                "    {\n" +
                "      \"URL\": \"aHR0cHM6Ly9zMy5hbWF6b25hd3MuY29tL3BzaXBob24vd2ViL213NHotYTJreC0wd2J6L3NlcnZlcl9saXN0X2NvbXByZXNzZWQ=\",\n" +
                "      \"OnlyAfterAttempts\": 0,\n" +
                "      \"SkipVerify\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"ObfuscatedServerListRootURLs\": [\n" +
                "    {\n" +
                "      \"URL\": \"aHR0cHM6Ly9zMy5hbWF6b25hd3MuY29tL3BzaXBob24vd2ViL213NHotYTJreC0wd2J6L29zbA==\",\n" +
                "      \"OnlyAfterAttempts\": 0,\n" +
                "      \"SkipVerify\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"RemoteServerListSignaturePublicKey\": \"MIICIDANBgkqhkiG9w0BAQEFAAOCAg0AMIICCAKCAgEAt7Ls+/39r+T6zNW7GiVpJfzq/xvL9SBH5rIFnk0RXYEYavax3WS6HOD35eTAqn8AniOwiH+DOkvgSKF2caqk/y1dfq47Pdymtwzp9ikpB1C5OfAysXzBiwVJlCdajBKvBZDerV1cMvRzCKvKwRmvDmHgphQQ7WfXIGbRbmmk6opMBh3roE42KcotLFtqp0RRwLtcBRNtCdsrVsjiI1Lqz/lH+T61sGjSjQ3CHMuZYSQJZo/KrvzgQXpkaCTdbObxHqb6/+i1qaVOfEsvjoiyzTxJADvSytVtcTjijhPEV6XskJVHE1Zgl+7rATr/pDQkw6DPCNBS1+Y6fy7GstZALQXwEDN/qhQI9kWkHijT8ns+i1vGg00Mk/6J75arLhqcodWsdeG/M/moWgqQAnlZAGVtJI1OgeF5fsPpXu4kctOfuZlGjVZXQNW34aOzm8r8S0eVZitPlbhcPiR4gT/aSMz/wd8lZlzZYsje/Jr8u/YtlwjjreZrGRmG8KMOzukV3lLmMppXFMvl4bxv6YFEmIuTsOhbLTwFgh7KYNjodLj/LsqRVfwz31PgWQFTEPICV7GCvgVlPRxnofqKSjgTWI4mxDhBpVcATvaoBl1L/6WLbFvBsoAUBItWwctO2xalKxF5szhGm8lccoc5MZr8kfE0uxMgsxz4er68iCID+rsCAQM=\",\n" +
                "  \"EmitDiagnosticNotices\": true,\n" +
                "  \"EmitDiagnosticNetworkParameters\": true,\n" +
                "  \"TargetServerEntry\": \"" + new ConfigUtil(m_context).getServerEntry() + "\",\n" +
                "  \"EmitServerAlerts\": false\n" +
                "}";
    }*/

    @Override
    public void onDiagnosticMessage(final String message) {
        /*m_Handler.post(new Runnable() {
            @Override
            public void run() {
                SkStatus.logInfo(message);
            }
        });*/
    }

    @Override
    public void onAvailableEgressRegions(final List<String> regions) {
    }

    @Override
    public void onSocksProxyPortInUse(final int port) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                SkStatus.logError(R.string.socks_port_in_use, port);
                signalStopService();
            }
        });
    }

    @Override
    public void onHttpProxyPortInUse(final int port) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                SkStatus.logError(R.string.http_proxy_port_in_use, port);
                signalStopService();
            }
        });
    }

    @Override
    public void onListeningSocksProxyPort(final int port) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                SkStatus.logInfo(R.string.socks_running, port);
                m_tunnelState.listeningLocalSocksProxyPort = port;
            }
        });
    }

    @Override
    public void onListeningHttpProxyPort(final int port) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                SkStatus.logInfo(R.string.http_proxy_running, port);
                m_tunnelState.listeningLocalHttpProxyPort = port;
            }
        });
    }

    @Override
    public void onUpstreamProxyError(final String message) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                // Display the error message only once, and continue trying to connect in
                // case the issue is temporary.
                if (m_lastUpstreamProxyErrorMessage == null || !m_lastUpstreamProxyErrorMessage.equals(message)) {
                    SkStatus.logInfo(R.string.upstream_proxy_error, message);
                    m_lastUpstreamProxyErrorMessage = message;
                }
            }
        });
    }

    @Override
    public void onConnecting() {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                m_networkConnectionStateBehaviorRelay.accept(TunnelState.ConnectionData.NetworkConnectionState.CONNECTING);
                m_tunnelState.homePages.clear();

                // Do not log "Connecting" if tunnel is stopping
                if (!m_isStopping.get()) {
                    SkStatus.logInfo(R.string.tunnel_connecting);
                }
            }
        });
    }

    public static void setTunListener(SocksDNSService.TunListener socksListener2) {
        tunListener = socksListener2;
    }

    @Override
    public void onConnected() {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                notification.setContentText(m_context.getString(R.string.connected));
                notificationManager.notify(NOTIFICATION.hashCode(), notification.getNotification());
                SkStatus.updateStateString(SkStatus.SSH_CONNECTED, m_context.getString(R.string.connected));
                SkStatus.logInfo("<b>Connected</b>");
                m_networkConnectionStateBehaviorRelay.accept(TunnelState.ConnectionData.NetworkConnectionState.CONNECTED);
            }
        });
    }

    @Override
    public void onHomepage(final String url) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (String homePage : m_tunnelState.homePages) {
                    if (homePage.equals(url)) {
                        return;
                    }
                }
                m_tunnelState.homePages.add(url);
            }
        });
    }

    @Override
    public void onClientRegion(final String region) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                m_tunnelState.clientRegion = region;
            }
        });
    }

    @Override
    public void onClientUpgradeDownloaded(String filename) {
    }

    @Override
    public void onUntunneledAddress(final String address) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                SkStatus.logInfo(R.string.untunneled_address, address);
            }
        });
    }

    @Override
    public void onBytesTransferred(final long sent, final long received) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                dataTransferStats.addBytesSent(sent);
                dataTransferStats.addBytesReceived(received);
            }
        });
    }

    @Override
    public void onStartedWaitingForNetworkConnectivity() {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                m_networkConnectionStateBehaviorRelay.accept(TunnelState.ConnectionData.NetworkConnectionState.WAITING_FOR_NETWORK);
                SkStatus.logInfo(R.string.waiting_for_network_connectivity);
            }
        });
    }

    @Override
    public void onStoppedWaitingForNetworkConnectivity() {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                m_networkConnectionStateBehaviorRelay.accept(TunnelState.ConnectionData.NetworkConnectionState.CONNECTING);
                // Do not log "Connecting" if tunnel is stopping
                if (!m_isStopping.get()) {
                    SkStatus.logInfo(R.string.tunnel_connecting);
                }
            }
        });
    }

    @Override
    public void onServerAlert(String reason, String subject, List<String> actionURLs) {
    }
}
