/*
 * Adapted from OpenVPN for Android
 * Copyright (c) 2012-2013, Arne Schwabe
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

package com.alinaj.dev.openconnect;

import android.content.SharedPreferences;

import java.util.Locale;
import java.util.UUID;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class VpnProfile implements Comparable<VpnProfile> {

    public static final String INLINE_TAG = "[[INLINE]]";
    public String mName;
    public SharedPreferences mPrefs;
    private UUID mUuid;

    private void loadPrefs(SharedPreferences prefs) {
        this.mPrefs = prefs;
        String uuid = prefs.getString("profile_uuid", null);
        if (uuid != null) {
            this.mUuid = UUID.fromString(uuid);
        }
        this.mName = this.mPrefs.getString("profile_name", null);
    }

    public VpnProfile(SharedPreferences prefs, String uuid, String name) {
        prefs.edit().putString("profile_uuid", uuid).putString("profile_name", name).commit();
        loadPrefs(prefs);
    }

    public VpnProfile(SharedPreferences prefs) {
        loadPrefs(prefs);
    }

    public VpnProfile(String name, String uuid) {
        this.mUuid = UUID.fromString(uuid);
        this.mName = name;
    }

    public boolean isValid() {
        return this.mName != null && this.mUuid != null;
    }

    public UUID getUUID() {
        return this.mUuid;
    }

    public String getName() {
        return this.mName;
    }

    public String toString() {
        return this.mName;
    }

    public String getUUIDString() {
        return this.mUuid.toString();
    }

    public int compareTo(VpnProfile arg0) {
        Locale def = Locale.getDefault();
        return getName().toUpperCase(def).compareTo(arg0.getName().toUpperCase(def));
    }
}