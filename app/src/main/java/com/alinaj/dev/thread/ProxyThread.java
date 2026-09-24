package com.alinaj.dev.thread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.alinaj.dev.utils.*;
import com.alinaj.dev.service.*;


public class ProxyThread extends Thread {
    boolean LoopingThread = true;
    boolean autoReplace = false;
    String buffReq = "1024";
    String buffRes = "4096";
    boolean clientToServer;
    Socket incoming;
    Socket outgoing;
	
    ProxyThread(Socket in, Socket out, boolean clientToServer, String buffReq, String buffRes, boolean autoReplace) {
        this.incoming = in;
        this.outgoing = out;
        this.clientToServer = clientToServer;
        this.buffReq = buffReq;
        this.buffRes = buffRes;
        this.autoReplace = autoReplace;
    }

    public final void run()
	{
        byte[] buffer;
        if (this.clientToServer) {
            buffer = new byte[Integer.parseInt(this.buffReq)];
        } else {
            buffer = new byte[Integer.parseInt(this.buffRes)];
        }
		try {
			InputStream FromClient = this.incoming.getInputStream();
			OutputStream ToClient = this.outgoing.getOutputStream();
			while (true) {
				int numberRead = FromClient.read(buffer);
				if (numberRead == -1) {
					break;
				}
				try {
					String result = new String(buffer, 0, numberRead);
					if (this.clientToServer) {
						ToClient.write(buffer, 0, numberRead);
						ToClient.flush();
					} else {
						String[] split = result.split("\r\n");
						if (split[0].startsWith("HTTP/")) {
							String line = split[0];
							String msg_success = "Successful - The action requested by the client was successful.";
							int code = Integer.parseInt(line.substring(9, 12));
							String msg = line.substring(13);
							String log = String.format("<b>Status: %s (%s)</b>", code, msg);
							Log(log);
							if (code == 200) {
								Log(msg_success);
								ToClient.write(buffer, 0, numberRead);
								ToClient.flush();
							} else {
								ToClient.write("HTTP/1.0 200 Connection Established\r\n\r\n".getBytes());
								ToClient.flush();
							}
						} else {
							ToClient.write(buffer, 0, numberRead);
							ToClient.flush();
						}
					}
				} catch (Exception e) {
					//Log.i("", String.format("ProxyThread: %s" , e.getMessage()));
					try {
						if (this.incoming != null) {
							this.incoming.close();
						}
						if (this.outgoing != null) {
							this.outgoing.close();
							return;
						}
						return;
					} catch (IOException e2) {
						return;
					}
				} catch (Throwable th) {
					try {
						if (this.incoming != null) {
							this.incoming.close();
						}
						if (this.outgoing != null) {
							this.outgoing.close();
						}
					} catch (IOException e3) {
					}
				}
			}
			
		} catch (Exception e) {

		}
    }

    public static void connect(Socket first, Socket second, String buffReq, String buffRes, boolean autoReplace) 
	{
        new ProxyThread(first, second, true, buffReq, buffRes, autoReplace).start();
        new ProxyThread(second, first, false, buffReq, buffRes, autoReplace).start();
    }
	private void Log(String msg)
	{
		OpenVPNService serv = VPNUtil.getService();
		if (serv != null) {
			serv.log_message(msg);
		}
	}
}
