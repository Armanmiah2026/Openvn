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

import java.util.Locale;

public class CIDRIP {
	int len;
	String mIp;

	public CIDRIP(String combo) {
		String[] ss = combo.split("/");
		this.mIp = ss[0];
		if (ss[1].matches("^[0-9]+$")) {
			this.len = Integer.parseInt(ss[1]);
		} else {
			this.len = maskToLen(ss[1]);
		}
		int i = this.len;
		if (i < 0 || i > 32) {
			this.len = 32;
		}
		normalise();
	}

	public CIDRIP(String ip, String mask) {
		this.mIp = ip;
		this.len = maskToLen(mask);
	}

	public CIDRIP(String ip, int prefixLen) {
		this.mIp = ip;
		this.len = prefixLen;
	}

	private static int maskToLen(String mask) {
		long netmask = getInt(mask) + 4294967296L;
		int lenZeros = 0;
		while ((1 & netmask) == 0) {
			lenZeros++;
			netmask >>= 1;
		}
		if (netmask != (0x1ffffffffL >> lenZeros)) {
			return 32;
		}
		return 32 - lenZeros;
	}

	public String toString() {
		return String.format(Locale.ENGLISH, "%s/%d", this.mIp, Integer.valueOf(this.len));
	}

	public boolean normalise() {
		long ip = getInt(this.mIp);
		long newip = (0xffffffffL << (32 - this.len)) & ip;
		if (newip == ip) {
			return false;
		}
		this.mIp = String.format("%d.%d.%d.%d", Long.valueOf((-16777216 & newip) >> 24), Long.valueOf((16711680 & newip) >> 16), Long.valueOf((65280 & newip) >> 8), Long.valueOf(255 & newip));
		return true;
	}

	static long getInt(String ipaddr) {
		String[] ipt = ipaddr.split("\\.");
		return (Long.parseLong(ipt[0]) << 24) + ((long) ((long) Integer.parseInt(ipt[1]) << 16)) + ((long) ((long) Integer.parseInt(ipt[2]) << 8)) + ((long) Integer.parseInt(ipt[3]));
	}

	public long getInt() {
		return getInt(this.mIp);
	}
}