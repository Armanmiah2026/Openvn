package com.alinaj.dev.psiphon;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import com.psiphon3.TunnelState;
import com.alinaj.dev.service.vpn.logger.SkStatus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/06/16
 */

public class TunnelServiceInteractor {

    public enum RestartMode {
        TUNNEL,
        VPN,
    }
    private static final String SERVICE_STARTING_BROADCAST_INTENT = "SERVICE_STARTING_BROADCAST_INTENT";
    private final BroadcastReceiver broadcastReceiver;
    private final Relay<TunnelState> tunnelStateRelay = BehaviorRelay.<TunnelState>create().toSerialized();
    private final Relay<Boolean> dataStatsRelay = PublishRelay.<Boolean>create().toSerialized();

    private final Messenger incomingMessenger = new Messenger(new IncomingMessageHandler(this));

    private Rx2ServiceBindingFactory serviceBindingFactory;
    private final boolean isStopped = true;
    private boolean shouldRegisterAsActivity = false;
    private Disposable serviceMessengerDisposable;
    private Disposable restartServiceDisposable;

    private final Context context;

    public TunnelServiceInteractor(Context context, boolean registerAsActivity) {
        this.context = context;
        this.shouldRegisterAsActivity = registerAsActivity;
        // Listen to SERVICE_STARTING_BROADCAST_INTENT broadcast that may be sent by another instance
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SERVICE_STARTING_BROADCAST_INTENT);
        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(SERVICE_STARTING_BROADCAST_INTENT) && !isStopped) {
                        bindTunnelService(context, intent);
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void startTunnelService() {
        tunnelStateRelay.accept(TunnelState.unknown());
        Intent intent = new Intent(context, PsiphonVPNService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent.setAction(SERVICE_STARTING_BROADCAST_INTENT));
        } catch (SecurityException | IllegalStateException e) {
            SkStatus.logError("startTunnelService failed with error: " + e);
            tunnelStateRelay.accept(TunnelState.stopped());
        }
    }

    public void stopTunnelService() {
        SkStatus.logInfo("<b>Disconnected</b>");
        context.startService(new Intent(context, PsiphonVPNService.class).setAction(TunnelManager.INTENT_ACTION_STOP_TUNNEL));
        tunnelStateRelay.accept(TunnelState.unknown());
        sendServiceMessage(TunnelManager.ClientToServiceMessage.STOP_SERVICE.ordinal(), null);
    }

    public boolean isServiceRunning() {
        return getRunningService() != null;
    }

    private String getRunningService() {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return null;
        }
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.uid == android.os.Process.myUid() &&
                    PsiphonVPNService.class.getName().equals(service.service.getClassName())) {
                return service.service.getClassName();
            }
        }
        return null;
    }

    private void bindTunnelService(Context context, Intent intent) {
        serviceBindingFactory = new Rx2ServiceBindingFactory(context, intent);
        serviceMessengerDisposable = serviceBindingFactory.getMessengerObservable()
                .doOnComplete(() -> tunnelStateRelay.accept(TunnelState.stopped()))
                .doOnComplete(() -> dataStatsRelay.accept(Boolean.FALSE))
                .subscribe();
        Bundle data = new Bundle();
        data.putBoolean(TunnelManager.IS_CLIENT_AN_ACTIVITY, shouldRegisterAsActivity);
        sendServiceMessage(TunnelManager.ClientToServiceMessage.REGISTER.ordinal(), data);
    }

    private void sendServiceMessage(int what, Bundle data) {
        if (serviceMessengerDisposable == null || serviceMessengerDisposable.isDisposed()) {
            return;
        }
        serviceBindingFactory.getMessengerObservable()
                .firstOrError()
                .doOnSuccess(messenger -> {
                    try {
                        Message msg = Message.obtain(null, what);
                        msg.replyTo = incomingMessenger;
                        if (data != null) {
                            msg.setData(data);
                        }
                        messenger.send(msg);
                    } catch (RemoteException e) {
                        SkStatus.logError(String.format("sendServiceMessage failed: " + e));
                    }
                })
                .subscribe();
    }

    private static TunnelManager.State getTunnelStateFromBundle(Bundle data) {
        TunnelManager.State tunnelState = new TunnelManager.State();
        if (data == null) {
            return tunnelState;
        }
        tunnelState.isRunning = data.getBoolean(TunnelManager.DATA_TUNNEL_STATE_IS_RUNNING);
        tunnelState.networkConnectionState =
                (TunnelState.ConnectionData.NetworkConnectionState) data.getSerializable(TunnelManager.DATA_TUNNEL_STATE_NETWORK_CONNECTION_STATE);
        tunnelState.listeningLocalSocksProxyPort = data.getInt(TunnelManager.DATA_TUNNEL_STATE_LISTENING_LOCAL_SOCKS_PROXY_PORT);
        tunnelState.listeningLocalHttpProxyPort = data.getInt(TunnelManager.DATA_TUNNEL_STATE_LISTENING_LOCAL_HTTP_PROXY_PORT);
        tunnelState.clientRegion = data.getString(TunnelManager.DATA_TUNNEL_STATE_CLIENT_REGION);
        tunnelState.sponsorId = data.getString(TunnelManager.DATA_TUNNEL_STATE_SPONSOR_ID);
        ArrayList<String> homePages = data.getStringArrayList(TunnelManager.DATA_TUNNEL_STATE_HOME_PAGES);
        if (homePages != null && tunnelState.isConnected()) {
            tunnelState.homePages = homePages;
        }
        return tunnelState;
    }

    private static class IncomingMessageHandler extends Handler {
        private final WeakReference<TunnelServiceInteractor> weakServiceInteractor;
        private final TunnelManager.ServiceToClientMessage[] scm = TunnelManager.ServiceToClientMessage.values();
        private TunnelManager.State state;


        IncomingMessageHandler(TunnelServiceInteractor serviceInteractor) {
            super(Looper.getMainLooper());
            this.weakServiceInteractor = new WeakReference<>(serviceInteractor);
        }

        @Override
        public void handleMessage(Message msg) {
            TunnelServiceInteractor tunnelServiceInteractor = weakServiceInteractor.get();
            if (tunnelServiceInteractor == null) {
                return;
            }
            if (msg.what > scm.length) {
                super.handleMessage(msg);
                return;
            }
            Bundle data = msg.getData();
            switch (scm[msg.what]) {
                case TUNNEL_CONNECTION_STATE:
                    state = getTunnelStateFromBundle(data);
                    TunnelState tunnelState;
                    if (state.isRunning) {
                        TunnelState.ConnectionData connectionData = TunnelState.ConnectionData.builder()
                                .setNetworkConnectionState(state.networkConnectionState)
                                .setClientRegion(state.clientRegion)
                                .setClientVersion("169")
                                .setPropagationChannelId("EE0B7486ACAE75AA")
                                .setSponsorId(state.sponsorId)
                                .setHttpPort(state.listeningLocalHttpProxyPort)
                                .setHomePages(state.homePages)
                                .build();
                        tunnelState = TunnelState.running(connectionData);
                    } else {
                        tunnelState = TunnelState.stopped();
                    }
                    tunnelServiceInteractor.tunnelStateRelay.accept(tunnelState);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private static class Rx2ServiceBindingFactory {
        private final Observable<Messenger> messengerObservable;
        private ServiceConnection serviceConnection;

        Rx2ServiceBindingFactory(Context context, Intent intent) {
            this.messengerObservable = Observable.using(Connection::new,
                    (final Connection<Messenger> con) -> {
                        serviceConnection = con;
                        context.bindService(intent, con, 0);
                        return Observable.create(con);
                    },
                    __ -> unbind(context))
                    .replay(1)
                    .refCount();
        }

        Observable<Messenger> getMessengerObservable() {
            return messengerObservable;
        }

        void unbind(Context context) {
            if (serviceConnection != null) {
                try {
                    context.unbindService(serviceConnection);
                    serviceConnection = null;
                } catch (IllegalArgumentException e) {
                    // Ignore
                    // "java.lang.IllegalArgumentException: Service not registered"
                }
            }
        }

        private static class Connection<B extends Messenger> implements ServiceConnection, ObservableOnSubscribe<B> {
            private ObservableEmitter<? super B> subscriber;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (subscriber != null && !subscriber.isDisposed() && service != null) {
                    //noinspection unchecked - we trust this one
                    subscriber.onNext((B) new Messenger(service));
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (subscriber != null && !subscriber.isDisposed()) {
                    subscriber.onComplete();
                }
            }

            @Override
            public void subscribe(ObservableEmitter<B> observableEmitter) throws Exception {
                this.subscriber = observableEmitter;
            }
        }
    }
}
