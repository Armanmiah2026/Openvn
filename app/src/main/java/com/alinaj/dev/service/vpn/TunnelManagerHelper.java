package com.alinaj.dev.service.vpn;

import android.content.Context;
import android.content.Intent;
import android.os.Build;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alinaj.dev.service.SocksDNSService;

public class TunnelManagerHelper
{
	public static void startSocksHttp(Context context) {
        Intent startVPN = new Intent(context, SocksDNSService.class);
		
		if (startVPN != null) {
			TunnelUtils.restartRotate();
			
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			//noinspection NewApi
                context.startForegroundService(startVPN);
            else
                context.startService(startVPN);
        }
    }
	
	public static void stopSocksHttp(Context context) {
		Intent stopTunnel = new Intent(SocksDNSService.TUNNEL_SSH_STOP_SERVICE);
		LocalBroadcastManager.getInstance(context)
			.sendBroadcast(stopTunnel);
	}
}
