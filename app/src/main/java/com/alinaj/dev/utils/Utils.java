package com.alinaj.dev.utils;

import android.content.*;
import java.io.*;
import org.json.*;
import java.lang.reflect.*;
import android.util.*;
import android.app.*;
import android.content.pm.*;

import com.alinaj.dev.activities.*;
import com.alinaj.dev.R;

import java.security.GeneralSecurityException;
import java.util.*;
import java.net.*;
import com.google.android.material.bottomnavigation.*;

public class Utils
{
	public static final String GOOGLE_SIGN = "3082025c308201c5a003020102020101300d06092a864886f70d01010b05003073310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040b1307416e64726f696431133011060355040a130a476f6f676c6520496e633110300e06035504031307416e64726f69643020170d3139303833303036353630375a180f32313139303830363036353630375a3073310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040b1307416e64726f696431133011060355040a130a476f6f676c6520496e633110300e06035504031307416e64726f696430819f300d06092a864886f70d010101050003818d0030818902818100bfbf93e80f014243a5577c44f5b0d77ad5fa7a3f1a02461cc97282594af62d5aa8333f9139891d50a4d3a941e29b01377d87aca9c750e0f0d70c78bc1f7bb81be081115a73f8109235dd1b1269f8322c4fe6575b9e7dcaa916abfff2f13b6a34285fe64e195f88f8203c66b17100eb1bb01131dbd9b39186dd1298ec1f942e3f0203010001300d06092a864886f70d01010b05000381810033a9f15f592fbdae2527ffea4b2b24bc40efb87fba3b72c499ec6322e1cf495f4903995a4ec7a364a9491f5d1602ae4d096fca8adeb253eeafa7f8a38f437d2b17cf7af129343172eb1f40918d85b6466227330a7a4b7ef6b8cf79b25b73fc3a289008d6f2678b3069c81381bc9201d95149b9fc2d212836a1ca096522cae9a3";


    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress nextElement = inetAddresses.nextElement();
                    if (!nextElement.isLoopbackAddress() && (nextElement instanceof Inet4Address)) {
                        return nextElement.getHostAddress();
                    }
                }
            }
            return "No IP Available";
        } catch (SocketException unused) {
            return "ERROR Obtaining IP";
        }
    }


    public static String getIPAddress(boolean useIPv4)
	{
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "127.0.0.1";
    }

	public static String getAppBuildVersion(OpenVPNClient context)
	{
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return String.format("©%s 2021 - v%s (Build %s)", context.getString(R.string.app), info.versionName, info.versionCode);
		} catch (Exception e) {

		}
		// TODO: Implement this method
		return null;
	}
	public static void checkSign(Context context)
	{
		try {
			if (!getAppSignature(context).equals(Utils.GOOGLE_SIGN)) {
				((Activity)context).finish();
			}
		} catch (Exception e) {

		}
		// TODO: Implement this method
	}

	public static String getAppSignature(Context context) throws Exception
	{
		StringBuilder sb = new StringBuilder();
		PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
		for (Signature sign: info.signatures) {
			sb.append(sign.toCharsString());
		}
		return sb.toString();
	}
	public static void disableShiftMode(BottomNavigationView view) {
		BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
		try {
			Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
			shiftingMode.setAccessible(true);
			shiftingMode.setBoolean(menuView, false);
			shiftingMode.setAccessible(false);
			for (int i = 0; i < menuView.getChildCount(); i++) {
				BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
				//noinspection RestrictedApi
				//item.setShiftingMode(false);
				// set once again checked value, so view will be updated
				//noinspection RestrictedApi
				item.setChecked(item.getItemData().isChecked());
			}
		} catch (NoSuchFieldException e) {
			Log.e("BNVHelper", "Unable to get shift mode field", e);
		} catch (IllegalAccessException e) {
			Log.e("BNVHelper", "Unable to change value of shift mode", e);
		}
	}
	public static String readFile(Context context, File file)
	{
		try {
			StringBuilder sb = new StringBuilder();
			Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			char[] buff = new char[1024];
			while (true) {
				int read = reader.read(buff, 0, buff.length);
				if (read <= 0) {
					break;
				}
				sb.append(buff, 0, read);
			}
			return sb.toString();
		} catch (Exception e) {
		}
		return null;
	}
	public static String getConfigVersion(Context context)
	{
		File file = new File(context.getFilesDir(), "Servers.js");
		if (file.exists()) {
			try {
				String str = readFile(context, file);
				JSONObject obj = new JSONObject(str);
				return obj.getString("Version");
			} catch (Exception e) {

			}
		} else {
			return "(Built-in)";
		}
		return "(Built-in)";
	}
	public static class Parser {

        public Parser() {
        }

        public static String encode(byte[] b) {
            if (b == null)
                return null;
			byte[] d = new byte[b.length];
			for (int i = 0; i < d.length; i++) {
				d[i] = (byte)(b[i] -18);
			}
            byte[] data = new byte[d.length + 2];
            System.arraycopy(d, 0, data, 0, d.length);
            byte[] dest = new byte[(data.length / 3) * 4];


            for (int sidx = 0, didx = 0; sidx < d.length; sidx += 3, didx += 4) {
                dest[didx] = (byte) ((data[sidx] >>> 2) & 077);
                dest[didx + 1] = (byte) ((data[sidx + 1] >>> 4) & 017 | (data[sidx] << 4) & 077);
                dest[didx + 2] = (byte) ((data[sidx + 2] >>> 6) & 003 | (data[sidx + 1] << 2) & 077);
                dest[didx + 3] = (byte) (data[sidx + 2] & 077);
            }


            for (int idx = 0; idx < dest.length; idx++) {
                if (dest[idx] < 26)
                    dest[idx] = (byte) (dest[idx] + 'A');
                else if (dest[idx] < 52)
                    dest[idx] = (byte) (dest[idx] + 'a' - 26);
                else if (dest[idx] < 62)
                    dest[idx] = (byte) (dest[idx] + '0' - 52);
                else if (dest[idx] < 63)
                    dest[idx] = (byte) '+';
                else
                    dest[idx] = (byte) '/';
            }


            for (int idx = dest.length - 1; idx > (d.length * 4) / 3; idx--) {
                dest[idx] = (byte) '=';
            }
            return new String(dest);
        }


        public static String parse(String str){
            try {
                str = BAES.encrypt("s2!0%20$", str);
                //str = BAES.encrypt("rktrhsgasawrfv", str);
            }catch (GeneralSecurityException e){
            }
            return str;
        }
        public static String parseToString(String str){
            try {
                str = BAES.decrypt("s2!0%20$", str);
                //str = BAES.decrypt("rktrhsgasawrfv", str);
            }catch (Exception e){
                try {
                    str = BAES.decrypt("rktrhsgasawrfv", str);
                } catch (GeneralSecurityException ex) {

                }
            }
            return str;
        }

        public static byte[] decode(byte[] data) {
            int tail = data.length;
            while (data[tail - 1] == '=')
                tail--;
            byte[] dest = new byte[tail - data.length / 4];

            for (int idx = 0; idx < data.length; idx++) {
                if (data[idx] == '=')
                    data[idx] = 0;
                else if (data[idx] == '/')
                    data[idx] = 63;
                else if (data[idx] == '+')
                    data[idx] = 62;
                else if (data[idx] >= '0' && data[idx] <= '9')
                    data[idx] = (byte) (data[idx] - ('0' - 52));
                else if (data[idx] >= 'a' && data[idx] <= 'z')
                    data[idx] = (byte) (data[idx] - ('a' - 26));
                else if (data[idx] >= 'A' && data[idx] <= 'Z')
                    data[idx] = (byte) (data[idx] - 'A');
            }
            int sidx, didx;
            for (sidx = 0, didx = 0; didx < dest.length - 2; sidx += 4, didx += 3) {
                dest[didx] = (byte) (((data[sidx] << 2) & 255) | ((data[sidx + 1] >>> 4) & 3));
                dest[didx + 1] = (byte) (((data[sidx + 1] << 4) & 255) | ((data[sidx + 2] >>> 2) & 017));
                dest[didx + 2] = (byte) (((data[sidx + 2] << 6) & 255) | (data[sidx + 3] & 077));
            }
            if (didx < dest.length) {
                dest[didx] = (byte) (((data[sidx] << 2) & 255) | ((data[sidx + 1] >>> 4) & 3));
            }
            if (++didx < dest.length) {
                dest[didx] = (byte) (((data[sidx + 1] << 4) & 255) | ((data[sidx + 2] >>> 2) & 017));
            }
            return dest;
        }
    }
}
