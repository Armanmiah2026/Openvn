package com.alinaj.dev.service;

import android.app.*;
import android.os.*;
import android.content.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.alinaj.dev.utils.*;
import javax.net.ssl.*;

import android.annotation.*;
import android.util.Log;

import com.alinaj.dev.thread.*;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class InjectorService extends Service implements Runnable, Handler.Callback
{

    public static final String NOTIFICATION_CHANNEL_ID = "injector";
    private Thread mThread;
    private Handler mHandler;
    private ConfigUtil config;
    private ServerSocket ss;
    private Socket client;
    public Socket server;
    public static boolean isRunning = false;
    private int repeatCount = 0;
    private InjectorService.InjectorListener InjectorListener;
    private HttpsURLConnection huc;
    private BackServer mBackServerThread;
    public SSLSocket mSSLSocket;

    private int mTunnelType;

    public interface InjectorListener
    {
        void startOpenVPN();
    }
    public void setInjectorListener(InjectorListener InjectorListener)
    {
        this.InjectorListener = InjectorListener;
    }
    @Override
    public IBinder onBind(Intent p1)
    {
        // TODO: Implement this method
        return new MyBinder();
    }
    public class MyBinder extends Binder
    {
        public InjectorService getService()
        {
            return InjectorService.this;
        }
    }

    @Override
    public void onCreate()
    {
        mHandler = new Handler(this);
        // TODO: Implement this method
        super.onCreate();
    }
    @Override
    public void onStart(Intent intent, int startId)
    {
		if (intent == null) {
            return;
        }
		
        String action = intent.getAction();
        if (action.equals("START")) {

            mThread = new Thread(this, "InjectorThread");
            config = ConfigUtil.getInstance(this);
            mTunnelType = config.getTunnelType();

            log("<b>Injector Service Start</b>");

            isRunning = true;
            showNotification();
            startInjector();
        }
        super.onStart(intent, startId);
        // TODO: Implement this method

    }

    public void stopInjector()
    {
        isRunning = false;
        log("<b>InjectorService Stopped</b>");
        repeatCount = 0;
        new Thread(new Runnable() {

                @Override
                public void run()
                {
                    closeAll();
                    // TODO: Implement this method
                }


            }).start();
        stopForeground(true);
        stopSelf();

        // TODO: Implement this method
    }
    private void closeAll()
    {
        try {
            if (ss != null) {
                ss.close();
                ss = null;
            }
            if (client != null) {
                client.close();
            }
            if (server != null) {
                server.close();
                server = null;
            }
            if (mSSLSocket != null) {
                mSSLSocket.close();
                mSSLSocket = null;
            }
            if (mThread != null) {
                mThread.interrupt();
            }
            if (huc != null) {
                huc.disconnect();
            }
            if (mBackServerThread != null) {
                mBackServerThread.Stop();
                mBackServerThread = null;
            }
        } catch (Exception e) {

        }
    }

    private void showNotification()
    {
        /*NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
         Notification.Builder builder =    null;
         builder = new Notification.Builder(this);
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         builder.setChannelId(NOTIFICATION_CHANNEL_ID);
         createNotificationChannel(nm);
         }
         builder.setContentTitle(String.format("%s %s", getString(R.string.app), "Injector"));
         builder.setContentText("Running");
         builder.setSmallIcon(R.drawable.ic_inject);

         Notification notif = builder.getNotification();
         nm.notify(2, notif);
         startForeground(2, notif);*/
        // TODO: Implement this method
    }

    private void startInjector()
    {
        if (mThread != null) {
            mThread.interrupt();
        }
        mThread.start();
    }
    private boolean connectSocket() throws Exception
    {
        try {
            String readLine;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            StringBuffer stringBuffer = new StringBuffer();
            while (true) {
                readLine = bufferedReader.readLine();
                if (readLine != null && readLine.length() > 0) {
                    stringBuffer.append(readLine);
                    stringBuffer.append("\r\n");
                } else {
                    break;
                }
                if (stringBuffer.toString().equals("")) {
                    log("Get Request", "Get request data failed, empty requestline");
                    return false;
                }

                //String[] tunnel_types = {"OVPN Direct", "OVPN Direct + Payload", "OVPN HTTP Proxy + Payload", "SSL Direct", "SSL Direct + Payload", "SSL HTTP Proxy + Payload"};
                //log(String.format("Tunnel Type <b>-></b> %s", tunnel_types[mTunnelType]));
                if (mTunnelType == ConfigUtil.MODE_SSH_DIRECT) {

                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    send200Status(client.getOutputStream());

                } else if (mTunnelType == ConfigUtil.MODE_SSH_DIRECT_WITH_PAYLOAD) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);

                    if (!c.equals("")) {
                        a(c, server);
                    }
                    send200Status(client.getOutputStream());
                } else if (mTunnelType == ConfigUtil.MODE_SSH_HTTP_PROXY) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String proxy = config.getProxy();
                    int proxyPort = Integer.parseInt(config.getProxyPort());

                    String remote = proxy + ":" + proxyPort;

                    //log("[Proxy Server]", "Connecting to " + remote);
                    connectSocket(proxy, proxyPort);
                    if (!c.equals("")) {
                        a(c, server);
                    }
                    //send200Status(client.getOutputStream());
                } else if (mTunnelType == ConfigUtil.MODE_SSL_DIRECT) {

                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    connectSSL();
                    send200Status(client.getOutputStream());

                } else if (mTunnelType == ConfigUtil.MODE_SSL_DIRECT_WITH_PAYLOAD) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    connectSSL();

                    if (!c.equals("")) {
                        a(c, mSSLSocket);
                    }
                    send200Status(client.getOutputStream());
                } else if (mTunnelType == ConfigUtil.MODE_SSL_HTTP_PROXY) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String proxy = config.getProxy();
                    int proxyPort = Integer.parseInt(config.getProxyPort());

                    String remote = proxy + ":" + proxyPort;

                    //log("[Proxy Server]", "Connecting to " + remote);
                    connectSocket(proxy, proxyPort);
                    connectSSL();
                    if (!c.equals("")) {
                        a(c, mSSLSocket);
                    }
                    //send200Status(client.getOutputStream());
                } else if (mTunnelType == ConfigUtil.MODE_OVPN_DIRECT_UDP) {

                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    send200Status(client.getOutputStream());

                }
                //send200Status(client.getOutputStream());
                if (mSSLSocket != null) {
                    return !client.isClosed() && server.isConnected() && mSSLSocket.isConnected();
                } else {
                    return !client.isClosed() && server.isConnected();
                }
            }

        } catch (Exception e) {
            log("Socket Server", e.toString());
        }
        return false;
    }
    private void connectSocket(String host, int port) throws Exception
    {
        server = new Socket();
        if (mTunnelType == ConfigUtil.MODE_SSL_DIRECT || mTunnelType == ConfigUtil.MODE_SSL_DIRECT_WITH_PAYLOAD || mTunnelType == ConfigUtil.MODE_SSL_HTTP_PROXY) {
            server.bind(new InetSocketAddress(0));
        }
        server.connect(new InetSocketAddress(host, port));
        doVpnProtect(server);

    }
    private void connectSSL() throws Exception
    {
        SSLSocketFactory factory = new SSLUtil(this);
        String mSni = config.getSni();
        URL url = new URL("https://" + mSni);
        mSni = url.getHost();
        if (url.getPort() > 0) {
            mSni = mSni + ":" + url.getPort();
        }
        if (!url.getPath().equals("/")) {
            mSni = mSni + url.getPath();
        }
        //log("(SNI) Host: " + ( ConfigUtil.hide(mSni)));

        huc = (HttpsURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, mBackServerThread.getLocalSocketAddr()));
        this.huc.setHostnameVerifier(new HostnameVerifier() {

                @SuppressLint({"BadHostnameVerifier"})
                public boolean verify(String str, SSLSession sSLSession) {
                    return true;
                }
            });
        huc.setSSLSocketFactory(factory);
        huc.connect();

    }
    private void a(String str, Socket socket) throws Exception
    {
        int i = 0;
        Random g;

        OutputStream outputStream = socket.getOutputStream();
        if (str.contains("[random]")) {
            g = new Random();
            String[] split = str.split(Pattern.quote("[random]"));
            str = split[g.nextInt(split.length)];
        }
        if (str.contains("[repeat]")) {
            String[] split = str.split(Pattern.quote("[repeat]"));
            str = split[repeatCount];
            repeatCount++;
            if (repeatCount > split.length - 1) {
                repeatCount= 0;
            }
        }
        String payload = str.replace("\r\n", "\\r\\n");
        //log(String.format("Payload: %s",  ConfigUtil.hide(payload)));
        log("Injecting");

        if (str.contains("[split_delay]")) {
            String[] split = str.split(Pattern.quote("[split_delay]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, socket, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(1500);
                }
                i++;
            }
        } else if (str.contains("[split_instant]")) {
            String[] split = str.split(Pattern.quote("[split_instant]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, socket, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(0);
                }
                i++;
            }
        } else if (str.contains("[instant_split]")) {
            String[] split = str.split(Pattern.quote("[instant_split]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, socket, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(0);
                }
                i++;
            }
        } else  if (str.contains("[delay_split]")) {
            String[] split = str.split(Pattern.quote("[delay_split]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, socket, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(1500);
                }
                i++;
            }
        } else if (a(str, socket, outputStream)) {
            outputStream.write(str.getBytes());
            outputStream.flush();
        }
    }
    private boolean a(String str, Socket socket, OutputStream outputStream) throws Exception
    {
        if (!str.contains("[split]")) {
            return true;
        }
        for (String str2 : str.split(Pattern.quote("[split]"))) {
            outputStream.write(str2.getBytes());
            outputStream.flush();
        }
        return false;
    }
    private String c(String str) {
        String str2 = null;
        if (str != null) {
            try {
                if (!str.equals("")) {
                    String charSequence = str.split("\r\n")[0];
                    String[] split = charSequence.split(" ");
                    String[] split2 = split[1].split(":");
                    String host = split2[0];
                    String port = split2[1];
                    str2 = d(config.getPayload().replace("[rlb]", config.getSSHHost()).replace("[real_raw]", str).replace("[raw]", charSequence).replace("[method]", split[0]).replace("[host_port]", split[1]).replace("[host]", host).replace("[port]", port).replace("[protocol]", split[2]).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr]", "\n\r").replace("\\r", "\r").replace("\\n", "\n"));

                    return str2;
                }
            } catch (Exception e) {
                log("Payload Error", e.toString());
            }
        }
        log("Payload Error", "Payload is null or empty");
        return str2;
    }
    private String d(String str) {
        if (str.contains("[cr*")) {
            str = a(str, "[cr*", "\r");
        }
        if (str.contains("[lf*")) {
            str = a(str, "[lf*", "\n");
        }
        if (str.contains("[crlf*")) {
            str = a(str, "[crlf*", "\r\n");
        }
        return str.contains("[lfcr*") ? a(str, "[lfcr*", "\n\r") : str;
    }
    private String a(String str, String str2, String str3) {
        while (str.contains(str2)) {
            Matcher matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])\\]").matcher(str);
            if (matcher.find()) {
                int intValue = Integer.valueOf(matcher.group(1)).intValue();
                CharSequence charSequence = "";
                for (int i = 0; i < intValue; i++) {
                    charSequence = charSequence + str3;
                }
                str = str.replace(str2 + intValue + "]", charSequence);
            }
        }
        return str;
    }
    @Override
    public void run()
    {


        try {
            log(String.format("Listening to localport %s", 8989));
            log("Listening for incoming connection");

            ss = new ServerSocket(8989);

            if (mTunnelType == ConfigUtil.MODE_SSL_DIRECT || mTunnelType == ConfigUtil.MODE_SSL_DIRECT_WITH_PAYLOAD || mTunnelType == ConfigUtil.MODE_SSL_HTTP_PROXY) {

                if (mBackServerThread != null) {
                    mBackServerThread.Stop();
                }
                mBackServerThread = new BackServer();
                mBackServerThread.start();
            }
            mHandler.sendEmptyMessage(2);

            while (isRunning) {
                client = ss.accept();
                if (client != null  && !client.isClosed() && connectSocket()) {
                    client.setKeepAlive(true);
                    if (mSSLSocket != null && mSSLSocket.isConnected()) {
                        mSSLSocket.setKeepAlive(true);
                        server.setKeepAlive(true);
                        doVpnProtect(mSSLSocket);
                        ProxyThread.connect(client, mSSLSocket, "16384", "32768", false);
                    }  else if (server != null && server.isConnected()) {
                        server.setKeepAlive(true);
                        doVpnProtect(server);
                        ProxyThread.connect(client, server, "16384", "32768",false);
                    }
                }  
            }
        } catch (Exception e) {
            log("InjectorException", e);
        }
        // TODO: Implement this method
    }
    private void send200Status(OutputStream output) throws Exception
    {
        output.write("HTTP/1.0 200 Connection Established\r\n\r\n".getBytes());
        output.flush();
    }

    private void doVpnProtect(Socket socket)
    {
        if (VPNUtil.isProtected(socket)) {
            log("Socket Protected!");
        }
        // TODO: Implement this method
    }
    private void log(String tag, String msg)
    {
        log(String.format("%s: %s", tag, msg));
    }
    private void log(String tag, Exception e)
    {
        log(String.format("%s: %s", tag, e.getMessage()));
    }
    public void log(String msg)
    {
        if (mHandler == null) {
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(1, msg));
    }
    @Override
    public boolean handleMessage(Message p1)
    {
        switch (p1.what) {
            case 1:
                String msg = (String)p1.obj;
                OpenVPNService service = VPNUtil.getService();
                if (service != null) {
                    service.log_message(msg);
                }
                break;
            case 2:
                startVPN();
                break;

        }
        // TODO: Implement this method
        return true;
    }
    private void startVPN()
    {
        InjectorListener.startOpenVPN();
    }

}
