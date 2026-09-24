/*
 * Copyright (c) 2013, Kevin Cernekee
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

package com.alinaj.dev.openconnect.core;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class KeepAlive extends BroadcastReceiver {

	public static final String ACTION_KEEPALIVE_ALARM = "app.openconnect.KEEPALIVE_ALARM";
	public static final String TAG = "OpenConnect";
	private final int mBaseDelayMs;
	private boolean mConnectionActive;
	private final String mDNSHost = "www.google.com";
	private final String mDNSServer;
	private Handler mMainHandler;
	private PendingIntent mPendingIntent;
	private PowerManager.WakeLock mWakeLock;
	private Handler mWorkerHandler;

	public KeepAlive(int seconds, String DNSServer) {
		this.mBaseDelayMs = seconds * 1000;
		this.mDNSServer = DNSServer;
	}

	private byte[] buildDNSQuery(byte[] transID, String hostname) {
		byte[] prefix = {1, 0, 0, 1, 0, 0, 0, 0, 0, 0};
		byte[] suffix = {0, 1, 0, 1};
		byte[] name = new byte[(hostname.length() + 2)];
		int i = 0;
		String[] split = hostname.split("\\.");
		int length = split.length;
		int i2 = 0;
		while (i2 < length) {
			String s = split[i2];
			int i3 = i + 1;
			name[i] = (byte) s.length();
			int j = 0;
			while (j < s.length()) {
				name[i3] = (byte) s.charAt(j);
				j++;
				i3++;
			}
			i2++;
			i = i3;
		}
		byte[] q = new byte[(transID.length + prefix.length + name.length + suffix.length)];
		System.arraycopy(transID, 0, q, 0, transID.length);
		System.arraycopy(prefix, 0, q, transID.length, prefix.length);
		System.arraycopy(name, 0, q, transID.length + prefix.length, name.length);
		System.arraycopy(suffix, 0, q, transID.length + prefix.length + name.length, suffix.length);
		return q;
	}

	/* access modifiers changed from: private */
	/* access modifiers changed from: public */
	private DatagramSocket openSocket() {
		try {
			DatagramSocket sock = new DatagramSocket();
			sock.connect(InetAddress.getByName(this.mDNSServer), 53);
			return sock;
		} catch (Exception e) {
			Log.e("OpenConnect", "KeepAlive: unexpected socket exception", e);
			return null;
		}
	}

	private byte[] receiveDNSResponse(DatagramSocket sock, int timeoutMs) {
		byte[] data = new byte[1024];
		DatagramPacket p = new DatagramPacket(data, 1024);
		try {
			sock.setSoTimeout(timeoutMs);
			sock.receive(p);
			return data;
		} catch (IOException e) {
			return null;
		}
	}

	/* access modifiers changed from: private */
	/* access modifiers changed from: public */
	private boolean sendDNSQuery(DatagramSocket sock) {
		Random r = new Random();
		byte[] transID = {(byte) r.nextInt(256), (byte) r.nextInt(256)};
		byte[] q = buildDNSQuery(transID, this.mDNSHost);
		try {
			sock.send(new DatagramPacket(q, q.length));
			int timeoutMs = 10000;
			boolean gotResponse = false;
			while (true) {
				byte[] data = receiveDNSResponse(sock, timeoutMs);
				if (data == null) {
					if (gotResponse) {
						Log.w("OpenConnect", "KeepAlive: got reply with bad transaction ID");
					} else {
						Log.i("OpenConnect", "KeepAlive: no reply was received");
					}
					return false;
				}
				gotResponse = true;
				if (data[0] == transID[0] && data[1] == transID[1]) {
					Log.d("OpenConnect", "KeepAlive: good reply from server");
					return true;
				}
				timeoutMs = 100;
			}
		} catch (IOException e) {
			Log.w("OpenConnect", "KeepAlive: error sending DNS request", e);
			return false;
		}
	}

	private void handleKeepAlive(final Context context) {
		this.mPendingIntent = null;
		if (this.mConnectionActive) {
			this.mWakeLock.acquire();
			this.mWorkerHandler.post(new Runnable() {
				/* class com.alinaj.dev.openconnect.core.KeepAlive.RunnableC10751 */

				public void run() {
					final boolean result;
					DatagramSocket sock = KeepAlive.this.openSocket();
					if (sock != null) {
						result = KeepAlive.this.sendDNSQuery(sock);
						sock.close();
					} else {
						result = false;
					}
					KeepAlive.this.mMainHandler.post(new Runnable() {
						/* class com.alinaj.dev.openconnect.core.KeepAlive.RunnableC10751.RunnableC10761 */

						public void run() {
							KeepAlive.this.scheduleNext(context, result ? KeepAlive.this.mBaseDelayMs : KeepAlive.this.mBaseDelayMs / 2);
							KeepAlive.this.mWakeLock.release();
						}
					});
				}
			});
		}
	}

	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_KEEPALIVE_ALARM)) {
			handleKeepAlive(context);
		}
	}

	/* access modifiers changed from: private */
	/* access modifiers changed from: public */
	@SuppressLint("WrongConstant")
	private void scheduleNext(Context context, int delayMs) {
		this.mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_KEEPALIVE_ALARM), Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
		((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).set(0, System.currentTimeMillis() + ((long) delayMs), this.mPendingIntent);
	}

	@SuppressLint({"InvalidWakeLockTag", "WrongConstant"})
	public void start(Context context) {
		if (this.mBaseDelayMs != 0) {
			HandlerThread t = new HandlerThread("KeepAlive");
			t.start();
			this.mWorkerHandler = new Handler(t.getLooper());
			this.mMainHandler = new Handler();
			this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "KeepAlive");
			this.mConnectionActive = true;
			scheduleNext(context, this.mBaseDelayMs);
		}
	}

	@SuppressLint("WrongConstant")
	public void stop(Context context) {
		this.mConnectionActive = false;
		if (this.mPendingIntent != null) {
			((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).cancel(this.mPendingIntent);
			this.mPendingIntent = null;
		}
		Handler handler = this.mWorkerHandler;
		if (handler != null) {
			handler.post(new Runnable() {
				/* class com.alinaj.dev.openconnect.core.KeepAlive.RunnableC10772 */

				public void run() {
					Looper.myLooper().quit();
				}
			});
		}
	}
}