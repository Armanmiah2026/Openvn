package com.alinaj.dev.utils;

import android.content.*;
import java.io.*;

public class Util
{
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


        public static String parse(String s) {

            return encode(s.getBytes());
        }

        public static String parseToString(String str) {
            if (str == null)
                return null;
			byte[] bytes = decode(str.getBytes());

			byte[] data = new byte[bytes.length];
			for (int i = 0; i < bytes.length; i++) {
				data[i] = (byte)(bytes[i] + 18);
			}

            return new String(data);
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
