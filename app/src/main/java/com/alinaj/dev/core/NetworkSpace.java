/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.alinaj.dev.core;

import android.os.Build;
import androidx.annotation.NonNull;

import net.openvpn.openvpn.BuildConfig;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.util.*;

/**
 * Updated version — removed JUnit dependency and replaced Assert.* calls
 * with safe runtime checks for Android build (no test dependency).
 */
public class NetworkSpace {

    public static class ipAddress implements Comparable<ipAddress> {
        private BigInteger netAddress;
        public int networkMask;
        private final boolean included;
        private boolean isV4;
        private BigInteger firstAddress;
        private BigInteger lastAddress;

        @Override
        public int compareTo(@NonNull ipAddress another) {
            int comp = getFirstAddress().compareTo(another.getFirstAddress());
            if (comp != 0) return comp;

            if (networkMask > another.networkMask)
                return -1;
            else if (another.networkMask == networkMask)
                return 0;
            else
                return 1;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ipAddress))
                return super.equals(o);

            ipAddress on = (ipAddress) o;
            return (networkMask == on.networkMask) && on.getFirstAddress().equals(getFirstAddress());
        }

        public ipAddress(CIDRIP ip, boolean include) {
            included = include;
            netAddress = BigInteger.valueOf(ip.getInt());
            networkMask = ip.len;
            isV4 = true;
        }

        public ipAddress(Inet6Address address, int mask, boolean include) {
            networkMask = mask;
            included = include;

            int s = 128;
            netAddress = BigInteger.ZERO;
            for (byte b : address.getAddress()) {
                s -= 8;
                netAddress = netAddress.add(BigInteger.valueOf((b & 0xFF)).shiftLeft(s));
            }
        }

        public BigInteger getLastAddress() {
            if (lastAddress == null)
                lastAddress = getMaskedAddress(true);
            return lastAddress;
        }

        public BigInteger getFirstAddress() {
            if (firstAddress == null)
                firstAddress = getMaskedAddress(false);
            return firstAddress;
        }

        private BigInteger getMaskedAddress(boolean one) {
            BigInteger numAddress = netAddress;
            int numBits = isV4 ? 32 - networkMask : 128 - networkMask;

            for (int i = 0; i < numBits; i++) {
                numAddress = one ? numAddress.setBit(i) : numAddress.clearBit(i);
            }
            return numAddress;
        }

        @Override
        public String toString() {
            return isV4
                    ? String.format(Locale.US, "%s/%d", getIPv4Address(), networkMask)
                    : String.format(Locale.US, "%s/%d", getIPv6Address(), networkMask);
        }

        ipAddress(BigInteger baseAddress, int mask, boolean included, boolean isV4) {
            this.netAddress = baseAddress;
            this.networkMask = mask;
            this.included = included;
            this.isV4 = isV4;
        }

        public ipAddress[] split() {
            ipAddress firstHalf = new ipAddress(getFirstAddress(), networkMask + 1, included, isV4);
            ipAddress secondHalf = new ipAddress(firstHalf.getLastAddress().add(BigInteger.ONE),
                    networkMask + 1, included, isV4);

            if (BuildConfig.DEBUG) {
                if (!secondHalf.getLastAddress().equals(getLastAddress())) {
                    throw new IllegalStateException("Split mismatch: last address check failed");
                }
            }
            return new ipAddress[]{firstHalf, secondHalf};
        }

        public String getIPv4Address() {
            if (BuildConfig.DEBUG) {
                if (!isV4)
                    throw new IllegalStateException("Expected IPv4 address, got IPv6.");
                long value = netAddress.longValue();
                if (value > 0xffffffffL || value < 0)
                    throw new IllegalStateException("Invalid IPv4 address: " + value);
            }

            long ip = netAddress.longValue();
            return String.format(Locale.US, "%d.%d.%d.%d",
                    (ip >> 24) & 0xFF, (ip >> 16) & 0xFF, (ip >> 8) & 0xFF, ip & 0xFF);
        }

        String getIPv6Address() {
            if (BuildConfig.DEBUG && isV4)
                throw new IllegalStateException("Expected IPv6 address, got IPv4.");

            BigInteger r = netAddress;
            StringBuilder sb = new StringBuilder();
            boolean hasStarted = false;

            while (r.compareTo(BigInteger.ZERO) > 0) {
                long part = r.mod(BigInteger.valueOf(0x10000)).longValue();
                if (hasStarted)
                    sb.insert(0, ":");
                sb.insert(0, String.format(Locale.US, "%x", part));
                r = r.shiftRight(16);
                hasStarted = true;
            }

            return sb.length() == 0 ? "::" : sb.toString();
        }

        public boolean containsNet(ipAddress network) {
            BigInteger ourFirst = getFirstAddress();
            BigInteger ourLast = getLastAddress();
            BigInteger netFirst = network.getFirstAddress();
            BigInteger netLast = network.getLastAddress();

            boolean a = ourFirst.compareTo(netFirst) <= 0;
            boolean b = ourLast.compareTo(netLast) >= 0;
            return a && b;
        }
    }

    private final TreeSet<ipAddress> mIpAddresses = new TreeSet<>();

    public Collection<ipAddress> getNetworks(boolean included) {
        Vector<ipAddress> ips = new Vector<>();
        for (ipAddress ip : mIpAddresses) {
            if (ip.included == included)
                ips.add(ip);
        }
        return ips;
    }

    public void clear() {
        mIpAddresses.clear();
    }

    public void addIP(CIDRIP cidrIp, boolean include) {
        mIpAddresses.add(new ipAddress(cidrIp, include));
    }

    public void addIPSplit(CIDRIP cidrIp, boolean include) {
        ipAddress newIP = new ipAddress(cidrIp, include);
        ipAddress[] splitIps = newIP.split();
        Collections.addAll(mIpAddresses, splitIps);
    }

    void addIPv6(Inet6Address address, int mask, boolean included) {
        mIpAddresses.add(new ipAddress(address, mask, included));
    }

    private TreeSet<ipAddress> generateIPList() {
        PriorityQueue<ipAddress> networks = new PriorityQueue<>(mIpAddresses);
        TreeSet<ipAddress> ipsDone = new TreeSet<>();

        ipAddress currentNet = networks.poll();
        if (currentNet == null)
            return ipsDone;

        while (currentNet != null) {
            ipAddress nextNet = networks.poll();

            if (nextNet == null || currentNet.getLastAddress().compareTo(nextNet.getFirstAddress()) < 0) {
                ipsDone.add(currentNet);
                currentNet = nextNet;
            } else {
                if (currentNet.getFirstAddress().equals(nextNet.getFirstAddress())
                        && currentNet.networkMask >= nextNet.networkMask) {

                    if (currentNet.included != nextNet.included) {
                        ipAddress[] newNets = nextNet.split();
                        if (!networks.contains(newNets[1]))
                            networks.add(newNets[1]);

                        if (!newNets[0].getLastAddress().equals(currentNet.getLastAddress())
                                && !networks.contains(newNets[0])) {
                            networks.add(newNets[0]);
                        }
                    }
                    currentNet = nextNet;
                } else {
                    if (currentNet.included != nextNet.included) {
                        ipAddress[] newNets = currentNet.split();
                        networks.add(newNets[1]);
                        networks.add(nextNet);
                        currentNet = newNets[0];
                    }
                }
            }
        }
        return ipsDone;
    }

    public Collection<ipAddress> getPositiveIPList() {
        TreeSet<ipAddress> ipsSorted = generateIPList();
        Vector<ipAddress> ips = new Vector<>();

        for (ipAddress ia : ipsSorted) {
            if (ia.included)
                ips.add(ia);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            for (ipAddress origIp : mIpAddresses) {
                if (!origIp.included || ipsSorted.contains(origIp))
                    continue;

                boolean skipIp = false;
                for (ipAddress calculatedIp : ipsSorted) {
                    if (!calculatedIp.included && origIp.containsNet(calculatedIp)) {
                        skipIp = true;
                        break;
                    }
                }
                if (!skipIp)
                    ips.add(origIp);
            }
        }

        return ips;
    }
}
