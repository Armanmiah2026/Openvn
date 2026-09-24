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

package com.alinaj.dev.openconnect.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

import com.alinaj.dev.R;

import java.util.ArrayList;

public class GetRestrictionReceiver extends BroadcastReceiver {
    public void onReceive(final Context context, Intent intent) {
        final PendingResult result = goAsync();
        new Thread() {
            public void run() {
                Bundle extras = new Bundle();
                extras.putParcelableArrayList("android.intent.extra.restrictions_list", GetRestrictionReceiver.this.initRestrictions(context));
                result.setResult(-1, null, extras);
                result.finish();
            }
        }.run();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private ArrayList<RestrictionEntry> initRestrictions(Context context) {
        ArrayList<RestrictionEntry> restrictions = new ArrayList<>();
        RestrictionEntry allowChanges = new RestrictionEntry("allow_changes", false);
        allowChanges.setTitle(context.getString(R.string.allow_vpn_changes));
        restrictions.add(allowChanges);
        return restrictions;
    }
}