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

package com.alinaj.dev.openconnect;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alinaj.dev.openconnect.api.GrantPermissionsActivity;
import com.alinaj.dev.openconnect.core.ProfileManager;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class OnBootReceiver extends BroadcastReceiver {

	public static final String TAG = "OpenConnect";

	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			VpnProfile bootProfile = ProfileManager.getOnBootProfile();
			if (bootProfile != null) {
				Log.i("OpenConnect", "starting profile '" + bootProfile.getName() + "' on boot");
				launchVPN(bootProfile, context);
				return;
			}
			Log.d("OpenConnect", "no boot profile configured");
		}
	}

	/* access modifiers changed from: package-private */
	@SuppressLint("WrongConstant")
	public void launchVPN(VpnProfile profile, Context context) {
		Intent startVpnIntent = new Intent(context, GrantPermissionsActivity.class);
		startVpnIntent.putExtra(context.getPackageName() + GrantPermissionsActivity.EXTRA_UUID, profile.getUUIDString());
		startVpnIntent.setFlags(268435456);
		context.startActivity(startVpnIntent);
	}
}