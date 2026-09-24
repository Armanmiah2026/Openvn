package com.alinaj.dev.v2ray.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.alinaj.dev.v2ray.core.V2rayCoreManager;
import com.alinaj.dev.v2ray.dto.EConfigType;
import com.alinaj.dev.v2ray.interfaces.V2rayServicesListener;
import com.alinaj.dev.v2ray.utils.AppConfigs;
import com.alinaj.dev.v2ray.utils.V2rayConfig;

import org.json.JSONObject;

public class V2rayVPNService extends VpnService implements V2rayServicesListener {

    private V2rayCoreManager v2rayCoreManager;
    private ParcelFileDescriptor mInterface;
    private Process process;
    private V2rayConfig v2rayConfig;
    private String user;
    private String pass;
    private String auth_api;
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        v2rayCoreManager = V2rayCoreManager.getInstance();
        v2rayCoreManager.setUpListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppConfigs.V2RAY_SERVICE_COMMANDS startCommand = (AppConfigs.V2RAY_SERVICE_COMMANDS) intent.getSerializableExtra("COMMAND");
        if (startCommand.equals(AppConfigs.V2RAY_SERVICE_COMMANDS.START_SERVICE)) {
            v2rayConfig = (V2rayConfig) intent.getSerializableExtra("V2RAY_CONFIG");
            user = intent.getStringExtra("USER");
            pass = intent.getStringExtra("PASS");
            v2rayCoreManager.name = intent.getStringExtra("NAME");
            auth_api = intent.getStringExtra("AUTH_API");
            if (v2rayConfig == null) {
                this.onDestroy();
            }
            if (V2rayCoreManager.getInstance().isV2rayCoreRunning()) {
                V2rayCoreManager.getInstance().stopCore();
            }
            if (V2rayCoreManager.getInstance().startCore(v2rayConfig)) {
                Log.e(V2rayVPNService.class.getSimpleName(), "onStartCommand success => v2ray core started.");
            } else {
                this.onDestroy();
            }
        } else if (startCommand.equals(AppConfigs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE)) {
            V2rayCoreManager.getInstance().stopCore();
        } else if (startCommand.equals(AppConfigs.V2RAY_SERVICE_COMMANDS.MEASURE_DELAY)) {
            new Thread(() -> {
                Intent sendB = new Intent("CONNECTED_V2RAY_SERVER_DELAY");
                sendB.putExtra("DELAY", String.valueOf(V2rayCoreManager.getInstance().getConnectedV2rayServerDelay()));
                sendBroadcast(sendB);
            }, "MEASURE_CONNECTED_V2RAY_SERVER_DELAY").start();
        } else {
            this.onDestroy();
        }
        return START_STICKY;
    }

    private void stopAllProcess() {
        stopForeground(true);
        isRunning = false;
        if (process != null) {
            process.destroy();
        }
        V2rayCoreManager.getInstance().stopCore();
        try {
            stopSelf();
        } catch (Exception e) {
            //ignore
            Log.e("CANT_STOP", "SELF");
        }
        try {
            mInterface.close();
        } catch (Exception e) {
            // ignored
        }

    }

    private void setup() {
        Intent prepare_intent = prepare(this);
        if (prepare_intent != null) {
            return;
        }
        Builder builder = new Builder();
        builder.setSession(v2rayConfig.REMARK);
        builder.setMtu(1500);
        builder.addAddress("26.26.26.1", 30);

        try {
            if (v2rayCoreManager.profileItem.getConfigType() == EConfigType.HYSTERIA2) {
                builder.addDisallowedApplication(getPackageName());
            }
        } catch (Exception e) {}

        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer("1.1.1.1");
        builder.addDnsServer("8.8.4.4");
        if (v2rayConfig.BLOCKED_APPS != null) {
            for (int i = 0; i < v2rayConfig.BLOCKED_APPS.size(); i++) {
                try {
                    builder.addDisallowedApplication(v2rayConfig.BLOCKED_APPS.get(i));
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        try {
            mInterface.close();
        } catch (Exception e) {
            //ignore
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }

        try {
            mInterface = builder.establish();
            isRunning = true;
            runTun2socks();
        } catch (Exception e) {
            stopAllProcess();
        }

    }

    private void runTun2socks() {
        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(new File(getApplicationInfo().nativeLibraryDir, "libv2ray.so").getAbsolutePath(),
                "--netif-ipaddr", "26.26.26.2",
                "--netif-netmask", "255.255.255.252",
                "--socks-server-addr", "127.0.0.1:" + v2rayConfig.LOCAL_SOCKS5_PORT,
                "--tunmtu", "1500",
                "--sock-path", "sock_path",
                "--enable-udprelay",
                "--loglevel", "error"));
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.directory(getApplicationContext().getFilesDir()).start();
            new Thread(() -> {
                try {
                    process.waitFor();
                    if (isRunning) {
                        runTun2socks();
                    }
                } catch (InterruptedException e) {
                    //ignore
                }
            }, "Tun2socks_Thread").start();
            sendFileDescriptor();
        } catch (Exception e) {
            Log.e("VPN_SERVICE", "FAILED=>", e);
            this.onDestroy();
        }
    }

    private void sendFileDescriptor() {
        String localSocksFile = new File(getApplicationContext().getFilesDir(), "sock_path").getAbsolutePath();
        FileDescriptor tunFd = mInterface.getFileDescriptor();
        new Thread(() -> {
            int tries = 0;
            while (true) {
                try {
                    Thread.sleep(50L * tries);
                    LocalSocket clientLocalSocket = new LocalSocket();
                    clientLocalSocket.connect(new LocalSocketAddress(localSocksFile, LocalSocketAddress.Namespace.FILESYSTEM));
                    if (!clientLocalSocket.isConnected()) {
                        Log.e("SOCK_FILE", "Unable to connect to localSocksFile [" + localSocksFile + "]");
                    } else {
                        Log.e("SOCK_FILE", "connected to sock file [" + localSocksFile + "]");
                    }
                    OutputStream clientOutStream = clientLocalSocket.getOutputStream();
                    clientLocalSocket.setFileDescriptorsForSend(new FileDescriptor[]{tunFd});
                    clientOutStream.write(32);
                    clientLocalSocket.setFileDescriptorsForSend(null);
                    clientLocalSocket.shutdownOutput();
                    clientLocalSocket.close();
                    break;
                } catch (Exception e) {
                    Log.e(V2rayVPNService.class.getSimpleName(), "sendFd failed =>", e);
                    if (tries > 5) break;
                    tries += 1;
                }
            }
        }, "sendFd_Thread").start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        stopAllProcess();
    }

    @Override
    public boolean onProtect(int socket) {
        return protect(socket);
    }

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void startService() {

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
            if (!user.matches("\\p{Alnum}*") || !pass.matches("\\p{Alnum}*")  || user.equals("0") || pass.equals("0")) {
                stopService();
                toast("Wrong username or password! Please recheck your account");
                return;
            }
            long delay = v2rayCoreManager.getConnectedV2rayServerDelay();
            if (delay != -1) {
                v2rayCoreManager.connected(delay);
                setup();
                check();
            } else {
                stopService();
                toast("Network error. Please try again!");
            }
        }, "check_v2ray").start();
    }

    private void check() {

        String model = Build.MODEL;
        String id = getHWID();

        String jsonUrl = String.format(auth_api, user, pass, id, model);
        //String jsonUrl = String.format(format, user, id, model);

        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            StringRequest req = new StringRequest(jsonUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    try {
                        JSONObject js = new JSONObject(response);
                        if (js.getString("device_match").equals("none")) {
                            stopService();
                            toast("Wrong username or password! Please recheck your account");
                            return;
                        }
                        if (js.getString("device_match").equals("false")) {
                            stopService();
                            toast("Account is used in another device! Please recheck your account");
                            return;
                        }
                    } catch (Exception e) {
                        stopService();
                        toast("Network error. Please try again");
                    }

                    requestQueue.getCache().clear();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    requestQueue.getCache().clear();
                    check();
                }
            });
            //req.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(req);
        } catch (Exception e) {}
    }

    private class ProxiedHurlStack extends HurlStack {
        @Override
        protected HttpURLConnection createConnection(URL url) throws IOException {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 10809));
            HttpURLConnection returnThis = (HttpURLConnection) url.openConnection(proxy);
            return returnThis;
        }
    }

    private void toast(String str) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(V2rayVPNService.this, str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getHWID() {
        return md5(Build.SERIAL + Build.BOARD.length() % 5 + Build.BRAND.length() % 5 + Build.DEVICE.length() % 5 + Build.MANUFACTURER.length() % 5 + Build.MODEL.length() % 5 + Build.PRODUCT.length() % 5 + Build.HARDWARE).toUpperCase(Locale.getDefault());
    }

    private String md5(String str) {
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(str.getBytes());
            byte[] digest = instance.digest();
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : digest) {
                String toHexString = Integer.toHexString(b & 255);
                while (toHexString.length() < 2) {
                    toHexString = "0" + toHexString;
                }
                stringBuilder.append(toHexString);
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @Override
    public void stopService() {
        stopAllProcess();
    }
}
