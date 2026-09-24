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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class APIVpnProfile implements Parcelable {
	public static final Creator<APIVpnProfile> CREATOR = new Creator<APIVpnProfile>() {
		@Override
		public APIVpnProfile createFromParcel(Parcel in) {
			return new APIVpnProfile(in);
		}

		@Override
		public APIVpnProfile[] newArray(int size) {
			return new APIVpnProfile[size];
		}
	};
	public final String mName;
	public final String mUUID;
	public final boolean mUserEditable;

	public APIVpnProfile(Parcel in) {
		this.mUUID = in.readString();
		this.mName = in.readString();
		this.mUserEditable = in.readInt() != 0;
	}

	public APIVpnProfile(String uuidString, String name, boolean userEditable) {
		this.mUUID = uuidString;
		this.mName = name;
		this.mUserEditable = userEditable;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.mUUID);
		dest.writeString(this.mName);
		if (this.mUserEditable) {
			dest.writeInt(0);
		} else {
			dest.writeInt(1);
		}
	}
}