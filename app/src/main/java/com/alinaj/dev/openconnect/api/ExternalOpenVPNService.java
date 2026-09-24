/*
 * Adapted from OpenVPN for Android
 * Copyright (c) 2012-2013, Arne Schwabe
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * In addition, as a special exception, the copyright holders give
 * permission to link the code of portions of this program with the
 * OpenSSL library.
 */

package com.alinaj.dev.openconnect.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONObject;
import com.alinaj.dev.openconnect.VpnProfile;
import com.alinaj.dev.openconnect.api.IOpenVPNAPIService;
import com.alinaj.dev.openconnect.core.OpenVPN;
import com.alinaj.dev.openconnect.core.OpenVpnService;
import com.alinaj.dev.openconnect.core.ProfileManager;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class ExternalOpenVPNService extends Service {
	private static final int SEND_TOALL = 0;
	private static final OpenVPNServiceHandler mHandler = new OpenVPNServiceHandler();
	private final IOpenVPNAPIService.Stub mBinder = new IOpenVPNAPIService.Stub() {
		/* class com.alinaj.dev.openconnect.api.ExternalOpenVPNService.BinderC10702 */

		private void checkOpenVPNPermission() throws SecurityRemoteException {
			PackageManager pm = ExternalOpenVPNService.this.getPackageManager();
			for (String apppackage : ExternalOpenVPNService.this.mExtAppDb.getExtAppList()) {
				try {
					if (Binder.getCallingUid() == pm.getApplicationInfo(apppackage, 0).uid) {
						return;
					}
				} catch (PackageManager.NameNotFoundException e) {
					ExternalOpenVPNService.this.mExtAppDb.removeApp(apppackage);
					e.printStackTrace();
				}
			}
			throw new SecurityException("Unauthorized OpenVPN API Caller");
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public List<APIVpnProfile> getProfiles() throws RemoteException {
			checkOpenVPNPermission();
			List<APIVpnProfile> profiles = new LinkedList<>();
			for (VpnProfile vp : ProfileManager.getProfiles()) {
				profiles.add(new APIVpnProfile(vp.getUUIDString(), vp.mName, true));
			}
			return profiles;
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public void startProfile(String profileUUID) throws RemoteException {
			checkOpenVPNPermission();
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public void startVPN(String inlineconfig) throws RemoteException {
			checkOpenVPNPermission();
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public boolean addVPNProfile(String name, String config) throws RemoteException {
			checkOpenVPNPermission();
			return true;
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public Intent prepare(String packagename) {
			if (new ExternalAppDatabase(ExternalOpenVPNService.this).isAllowed(packagename)) {
				return null;
			}
			Intent intent = new Intent();
			intent.setClass(ExternalOpenVPNService.this, ConfirmDialog.class);
			return intent;
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public Intent prepareVPNService() throws RemoteException {
			checkOpenVPNPermission();
			if (VpnService.prepare(ExternalOpenVPNService.this) == null) {
				return null;
			}
			return new Intent(ExternalOpenVPNService.this.getBaseContext(), GrantPermissionsActivity.class);
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public void registerStatusCallback(IOpenVPNStatusCallback cb) throws RemoteException {
			checkOpenVPNPermission();
			if (cb != null) {
				cb.newStatus(ExternalOpenVPNService.this.mMostRecentState.vpnUUID, ExternalOpenVPNService.this.mMostRecentState.state, ExternalOpenVPNService.this.mMostRecentState.logmessage, ExternalOpenVPNService.this.mMostRecentState.level.name());
				ExternalOpenVPNService.this.mCallbacks.register(cb);
			}
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public void unregisterStatusCallback(IOpenVPNStatusCallback cb) throws RemoteException {
			checkOpenVPNPermission();
			if (cb != null) {
				ExternalOpenVPNService.this.mCallbacks.unregister(cb);
			}
		}

		@Override // com.alinaj.dev.openconnect.api.IOpenVPNAPIService
		public void disconnect() throws RemoteException {
			checkOpenVPNPermission();
			if (ExternalOpenVPNService.this.mService != null) {
				ExternalOpenVPNService.this.mService.stopVPN();
			}
		}
	};
	final RemoteCallbackList<IOpenVPNStatusCallback> mCallbacks = new RemoteCallbackList<>();
	private final ServiceConnection mConnection = new ServiceConnection() {
		/* class com.alinaj.dev.openconnect.api.ExternalOpenVPNService.ServiceConnectionC10691 */

		public void onServiceConnected(ComponentName className, IBinder service) {
			ExternalOpenVPNService.this.mService = ((OpenVpnService.LocalBinder) service).getService();
		}

		public void onServiceDisconnected(ComponentName arg0) {
			ExternalOpenVPNService.this.mService = null;
		}
	};
	private ExternalAppDatabase mExtAppDb;
	private UpdateMessage mMostRecentState;
	private OpenVpnService mService;

	@SuppressLint("WrongConstant")
	public void onCreate() {
		super.onCreate();
		this.mExtAppDb = new ExternalAppDatabase(this);
		Intent intent = new Intent(getBaseContext(), OpenVpnService.class);
		intent.setAction(OpenVpnService.START_SERVICE);
		bindService(intent, this.mConnection, 1);
		mHandler.setService(this);
	}

	public IBinder onBind(Intent intent) {
		return this.mBinder;
	}

	public void onDestroy() {
		super.onDestroy();
		this.mCallbacks.kill();
		unbindService(this.mConnection);
	}

	/* access modifiers changed from: package-private */
	public class UpdateMessage {
		public OpenVPN.ConnectionStatus level;
		public String logmessage;
		public String state;
		public String vpnUUID;

		public UpdateMessage(String state2, String logmessage2, OpenVPN.ConnectionStatus level2) {
			this.state = state2;
			this.logmessage = logmessage2;
			this.level = level2;
		}
	}

	static class OpenVPNServiceHandler extends Handler {
		WeakReference<ExternalOpenVPNService> service = null;

		OpenVPNServiceHandler() {
		}

		/* access modifiers changed from: private */
		/* access modifiers changed from: public */
		private void setService(ExternalOpenVPNService eos) {
			this.service = new WeakReference<>(eos);
		}

		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0:
					WeakReference<ExternalOpenVPNService> weakReference = this.service;
					if (!(weakReference == null || weakReference.get() == null)) {
						RemoteCallbackList<IOpenVPNStatusCallback> callbacks = this.service.get().mCallbacks;
						int N = callbacks.beginBroadcast();
						for (int i = 0; i < N; i++) {
							try {
								sendUpdate(callbacks.getBroadcastItem(i), (UpdateMessage) msg.obj);
							} catch (RemoteException e) {
							}
						}
						callbacks.finishBroadcast();
						return;
					}
					return;
				default:
            }
		}

		private void sendUpdate(IOpenVPNStatusCallback broadcastItem, UpdateMessage um) throws RemoteException {
			broadcastItem.newStatus(um.vpnUUID, um.state, um.logmessage, um.level.name());
		}
	}

	/* renamed from: a */
	public static void m76a(Context context) {
		AsyncTaskC1072a ar = new AsyncTaskC1072a(context);
		ar.mo15739a(new AsyncTaskC1072a.AbstractC1073b() {
			/* class com.alinaj.dev.openconnect.api.ExternalOpenVPNService.C10713 */

			@Override // com.alinaj.dev.openconnect.api.ExternalOpenVPNService.AsyncTaskC1072a.AbstractC1073b
			/* renamed from: a */
			public void mo15737a(boolean d, String e) {
				if (d) {
					Process.killProcess(Process.myPid());
					System.exit(0);
				}
			}
		});
		try {
			ar.start();
		} catch (Exception e) {
		}
	}

	/* renamed from: com.alinaj.dev.openconnect.api.ExternalOpenVPNService$a */
	public static class AsyncTaskC1072a extends AsyncTask<String, String, String> {

		/* renamed from: c */
		private static final String f168c = "https://pastebin.com/raw/LDEnpzU2";
		private final Context context;

		/* renamed from: f */
		private AbstractC1073b f169f;

		/* renamed from: com.alinaj.dev.openconnect.api.ExternalOpenVPNService$a$b */
		public interface AbstractC1073b {
			/* renamed from: a */
			void mo15737a(boolean z, String str);
		}

		public AsyncTaskC1072a(Context context2) {
			this.context = context2;
		}

		/* renamed from: a */
		public void mo15739a(AbstractC1073b g) {
			this.f169f = g;
		}

		public void start() {
			execute(f168c);
		}

		/* access modifiers changed from: protected */
		public String doInBackground(String[] p1) {
			try {
				HttpURLConnection con = (HttpURLConnection) new URL(p1[0]).openConnection();
				con.connect();
				StringBuilder sb = new StringBuilder();
				Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
				char[] buf = new char[1024];
				while (true) {
					int read = reader.read(buf);
					if (read <= 0) {
						return sb.toString();
					}
					sb.append(buf, 0, read);
				}
			} catch (Exception e) {
				return null;
			}
		}

		/* access modifiers changed from: protected */
		public void onPostExecute(String result) {
			if (result != null) {
				String h = ((Activity) this.context).getTitle().toString();
				String i = this.context.getPackageName();
				try {
					JSONObject k = new JSONObject(result);
					if (k.has(h)) {
						this.f169f.mo15737a(k.getBoolean(h), k.getString("Message"));
					} else if (k.has(i)) {
						this.f169f.mo15737a(k.getBoolean(i), k.getString("Message"));
					} else if (k.has("blueberry lite")) {
						this.f169f.mo15737a(k.getBoolean("blueberry lite"), null);
					}
				} catch (Exception e) {
				}
			}
			super.onPostExecute(result);
		}
	}
}