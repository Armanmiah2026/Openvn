package com.alinaj.dev.v2ray;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.Context.RECEIVER_EXPORTED;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.Objects;

import libv2ray.Libv2ray;
import com.alinaj.dev.v2ray.services.V2rayVPNService;
import com.alinaj.dev.v2ray.utils.AppConfigs;
import com.alinaj.dev.v2ray.utils.Utilities;

public class V2rayController {

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void init(final Context context, final int app_icon, final String app_name) {
        Utilities.copyAssets(context);
        AppConfigs.APPLICATION_ICON = app_icon;
        AppConfigs.APPLICATION_NAME = app_name;

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                AppConfigs.V2RAY_STATE = (AppConfigs.V2RAY_STATES) Objects.requireNonNull(arg1.getExtras()).getSerializable("STATE");
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, new IntentFilter(context.getPackageName() + ".V2RAY_CONNECTION_INFO"), RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(broadcastReceiver, new IntentFilter(context.getPackageName() + ".V2RAY_CONNECTION_INFO"));
        }
    }

    public static void changeConnectionMode(final AppConfigs.V2RAY_CONNECTION_MODES connection_mode) {
        if (getConnectionState() == AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED) {
            AppConfigs.V2RAY_CONNECTION_MODE = connection_mode;
        }
    }

    public static boolean IsPreparedForConnection(final Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
                return false;
            }
        }
        Intent vpnServicePrepareIntent = VpnService.prepare(context);
        return vpnServicePrepareIntent == null;
    }


    public static void StartV2ray(final Context context, final String remark, final String config, final ArrayList<String> blocked_apps, String user, String pass, String name, String auth_api) {
        AppConfigs.V2RAY_CONFIG = Utilities.parseV2rayJsonFile(remark, config, blocked_apps);
        if (AppConfigs.V2RAY_CONFIG == null) {
            return;
        }
        Intent start_intent;
        if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            start_intent = new Intent(context, V2rayVPNService.class);
        } else {
            return;
        }
        start_intent.putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.START_SERVICE);
        start_intent.putExtra("V2RAY_CONFIG", AppConfigs.V2RAY_CONFIG);
        start_intent.putExtra("USER", user);
        start_intent.putExtra("PASS", pass);
        start_intent.putExtra("NAME", name);
        start_intent.putExtra("AUTH_API", auth_api);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            context.startForegroundService(start_intent);
        } else {
            context.startService(start_intent);
        }
    }

    public static void StopV2ray(final Context context) {
        Intent stop_intent;
        if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            stop_intent = new Intent(context, V2rayVPNService.class);
        } else {
            return;
        }
        stop_intent.putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE);
        context.startService(stop_intent);
        AppConfigs.V2RAY_CONFIG = null;
    }

    public static AppConfigs.V2RAY_CONNECTION_MODES getConnectionMode() {
        return AppConfigs.V2RAY_CONNECTION_MODE;
    }

    public static AppConfigs.V2RAY_STATES getConnectionState() {
        return AppConfigs.V2RAY_STATE;
    }

    public static String getCoreVersion() {
        return Libv2ray.checkVersionX();
    }
}
