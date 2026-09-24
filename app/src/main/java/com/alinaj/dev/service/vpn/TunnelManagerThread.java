package com.alinaj.dev.service.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import android.util.Log;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.alinaj.dev.R;
import com.alinaj.dev.service.vpn.logger.SkStatus;

public class TunnelManagerThread
implements Runnable {



	private static final String TAG = TunnelManagerThread.class.getSimpleName();
	
	/*ConnectivityManager.NetworkCallback networkVpnCallback = new ConnectivityManager.NetworkCallback() {
		@Override
		public void onAvailable(Network network) {
			boolean connected = false;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				connected = mCmgr.bindProcessToNetwork(network);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				//noinspection deprecation
				connected = ConnectivityManager.setProcessDefaultNetwork(network);
			}
			
			SkStatus.logInfo("vpn disponibel");

			if (!mConnected && connected) {
				SkStatus.logInfo("<strong>Outro aplicativo VPN em execução foi detectado, pare ele antes</strong>");
				stopAll();
			}
		}

		@Override
		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities)
		{
			if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
				SkStatus.logInfo(String.format("Vpn detected ou não. Velocidade: %d Kb/s", networkCapabilities.getLinkDownstreamBandwidthKbps()));
			}
		}
		
	};*/
	
	private OnStopCliente mListener;
	private final Context mContext;
	private final Handler mHandler;

	private boolean mRunning = false, mStopping = false, mStarting = false;
	
	private CountDownLatch mTunnelThreadStopSignal;
	//private ConnectivityManager mCmgr;
	
	public interface OnStopCliente {
		void onStop();
	}
	
	public TunnelManagerThread(Handler handler, Context context) {
		mContext = context;
		mHandler = handler;
		

	}
	
	public void setOnStopClienteListener(OnStopCliente listener) {
		mListener = listener;
	}

	@Override
	public void run()
	{
		mStarting = true;
		mTunnelThreadStopSignal = new CountDownLatch(1);
		
		SkStatus.logInfo("<strong>" + mContext.getString(R.string.starting_service_ssh) + "</strong>");
		
		// anti vpn sniffer
		/*if (Build.VERSION.SDK_INT >= 21) {
			mCmgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			
			mCmgr.registerNetworkCallback(new NetworkRequest.Builder()
				.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
					.build(), networkVpnCallback);
		}*/
		
		int tries = 0;
		while (!mStopping) {
			try {
				if (!TunnelUtils.isNetworkOnline(mContext)) {
					SkStatus.updateStateString(SkStatus.SSH_WAITING, mContext.getString(R.string.state_nonetwork));

					SkStatus.logInfo(R.string.state_nonetwork);
					
					try {
						Thread.sleep(5000);
					} catch(InterruptedException e2) {
						stopAll();
						break;
					}
				}
				else {
					if (tries > 0)
						SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

					try {
						Thread.sleep(1000);
					} catch(InterruptedException e2) {
						stopAll();
						break;
					}

					startClienteSSH();
					break;
				}
			} catch(Exception e) {

				SkStatus.logError("<strong>" + mContext.getString(R.string.state_disconnected) + "</strong>");
				closeSSH();
				
				try {
					Thread.sleep(3000);
				} catch(InterruptedException e2) {
					stopAll();
					break;
				}
			}
			
			tries++;
		}
		
		mStarting = false;
		
		if (!mStopping) {
			try {
				mTunnelThreadStopSignal.await();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		if (mListener != null) {
			mListener.onStop();
		}
	}
	
	public void stopAll() {
		if (mStopping) return;
		
		SkStatus.updateStateString(SkStatus.SSH_STOPPING, mContext.getString(R.string.stopping_service_ssh));
		SkStatus.logInfo("<strong>" + mContext.getString(R.string.stopping_service_ssh) + "</strong>");
		
		/*if (mCmgr != null && Build.VERSION.SDK_INT >= 21) {
			mCmgr.unregisterNetworkCallback(networkVpnCallback);
			mCmgr = null;
		}*/
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				mStopping = true;

				if (mTunnelThreadStopSignal != null)
					mTunnelThreadStopSignal.countDown();

				closeSSH();
				
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e){}

				SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, mContext.getString(R.string.state_disconnected));

				mRunning = false;
				mStarting = false;
				mReconnecting = false;
			}
		}).start();
	}
	
	
	/**
	 * Forwarder
	*/

	protected void startForwarder(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}
		
	//	startForwarderSocks(portaLocal);
		
		startTunnelVpnService();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (!mConnected) break;
					
					try {
						Thread.sleep(2000);
					} catch(InterruptedException e) {
						break;
					}
				}
			}
		}).start();
	}

	protected void stopForwarder() {
		stopTunnelVpnService();
		

	}
	
	
	/**
	* Cliente SSH
	*/
	
	private final static int AUTH_TRIES = 1;
	private final static int RECONNECT_TRIES = 5;
	

	private boolean mConnected = false;
	
	protected void startClienteSSH() throws Exception {
		mStopping = false;
		mRunning = true;
		try {
			SkStatus.updateStateString(SkStatus.SSH_CONNECTED, "SSH connection established");
			SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_connected) + "</strong>");
			mConnected = true;
			startTunnelVpnService();
		} catch(Exception e) {
			mConnected = false;

			throw e;
		}
	}
	
	public synchronized void closeSSH() {
		stopForwarder();
	}

	public boolean mReconnecting = false;
	
	public void reconnectSSH() {
		if (mStarting || mStopping || mReconnecting) {
			return;
		}
		
		mReconnecting = true;
		
		closeSSH();
		
		SkStatus.updateStateString(SkStatus.SSH_RECONECTANDO, "Reconnecting..");

		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
			mReconnecting = false;
			return;
		}

		for (int i = 0; i < RECONNECT_TRIES; i++) {
			if (mStopping) {
				mReconnecting = false;
				return;
			}

			int sleepTime = 5;
			if (!TunnelUtils.isNetworkOnline(mContext)) {
				SkStatus.updateStateString(SkStatus.SSH_WAITING, "Waiting for network...");

				SkStatus.logInfo(R.string.state_nonetwork);
			}
			else {
				sleepTime = 3;
				mStarting = true;
				SkStatus.updateStateString(SkStatus.SSH_RECONECTANDO, "Reconnecting..");

				SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

				try {
					startClienteSSH();
					
					mStarting = false;
					mReconnecting = false;
					//mConnected = true;

					return;
				} catch(Exception e) {
					SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_disconnected) + "</strong>");
				}
				
				mStarting = false;
			}

			try {
				Thread.sleep(sleepTime*1000);
				i--;
			} catch(InterruptedException e2){
				mReconnecting = false;
				return;
			}
		}
		
		mReconnecting = false;

		stopAll();
	}

	/**
	 * Debug Logger
	 */

	

	/**
	 * Vpn Tunnel
	 */
	 
	//String serverAddr;

	protected void startTunnelVpnService() throws IOException {
		if (!mConnected) {
			throw new IOException();
		}
		
		SkStatus.logInfo(R.string.service_tunnel_start);


		// Broadcast
		IntentFilter broadcastFilter =
			new IntentFilter(TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST);
		broadcastFilter.addAction(TunnelVpnService.TUNNEL_VPN_START_BROADCAST);
		// Inicia Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.registerReceiver(m_vpnTunnelBroadcastReceiver, broadcastFilter);

		String m_socksServerAddress = String.format("127.0.0.1:%s",1080);
		boolean m_dnsForward = false;
		String m_udpResolver = null;

	/*	try {
		//	InetAddress servidorAddr = TransportManager.createInetAddress(servidorIP);
		//	serverAddr = servidorIP = servidorAddr.getHostAddress();
		} catch(Exception e) {
			throw new IOException(mContext.getString(R.string.error_server_ip_invalid));
		}

	 */
		
		String[] m_excludeIps = {};

		String[] m_dnsResolvers = null;
		if (m_dnsForward) {
			m_dnsResolvers = new String[]{"8.8.8.8"};
		}
		else {
			List<String> lista = VpnUtils.getNetworkDnsServer(mContext);
			m_dnsResolvers = new String[]{lista.get(0)};
		}

		if (isServiceVpnRunning()) {
			Log.d(TAG, "already running service");

			TunnelVpnManager tunnelManager = TunnelState.getTunnelState()
				.getTunnelManager();
			
			if (tunnelManager != null) {
				tunnelManager.restartTunnel(m_socksServerAddress);
			}

			return;
		}

		Intent startTunnelVpn = new Intent(mContext, TunnelVpnService.class);
		startTunnelVpn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		TunnelVpnSettings settings = new TunnelVpnSettings(m_socksServerAddress, m_dnsForward, m_dnsResolvers,
			(m_dnsForward && m_udpResolver == null || !m_dnsForward && m_udpResolver != null), m_udpResolver, m_excludeIps,
				false,false, new String[]{},true,false);
		startTunnelVpn.putExtra(TunnelVpnManager.VPN_SETTINGS, settings);

		if (mContext.startService(startTunnelVpn) == null) {
			SkStatus.logInfo(R.string.service_tunnel_failed);

			throw new IOException(mContext.getString(R.string.vpn_service_failed));
		}

		TunnelState.getTunnelState().setStartingTunnelManager();
	}

	public static boolean isServiceVpnRunning() {
		TunnelState tunnelState = TunnelState.getTunnelState();
		return tunnelState.getStartingTunnelManager() || tunnelState.getTunnelManager() != null;
	}

	protected synchronized void stopTunnelVpnService() {
		if (!isServiceVpnRunning()) {
			return;
		}
		
		// Use signalStopService to asynchronously stop the service.
		// 1. VpnService doesn't respond to stopService calls
		// 2. The UI will not block while waiting for stopService to return
		// This scheme assumes that the UI will monitor that the service is
		// running while the Activity is not bound to it. This is the state
		// while the tunnel is shutting down.
		SkStatus.logInfo(R.string.service_tunnel_stopping);
		
		TunnelVpnManager currentTunnelManager = TunnelState.getTunnelState()
			.getTunnelManager();
		
		if (currentTunnelManager != null) {
			currentTunnelManager.signalStopService();
		}
		
		/*if (mThreadLocation != null && mThreadLocation.isAlive()) {
			mThreadLocation.interrupt();
		}
		mThreadLocation = null;*/

		// Parando Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.unregisterReceiver(m_vpnTunnelBroadcastReceiver);
	}
	
	//private Thread mThreadLocation;

	// Local BroadcastReceiver
	private final BroadcastReceiver m_vpnTunnelBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public synchronized void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (TunnelVpnService.TUNNEL_VPN_START_BROADCAST.equals(action)) {
				boolean startSuccess = intent.getBooleanExtra(TunnelVpnService.TUNNEL_VPN_START_SUCCESS_EXTRA, true);

				if (!startSuccess) {
					stopAll();
				}
				else {
					/*mThreadLocation = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(3000);
							} catch(InterruptedException e) {
								return;
							}
							try {
								if (serverAddr != null) {
									SkStatus.logInfo(String.format(
										"Server Localização: %s", TunnelUtils.getLocationIp(serverAddr)));
								}
							} catch (IOException e) {
								SkStatus.logDebug("Server Location Error: " + e.getMessage());
							}
						}
					});
					mThreadLocation.start();*/
				}
				
			} else if (TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST.equals(action)) {
				stopAll();
			}
		}
	};
	
}
