package com.alinaj.dev.tunnel;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.alinaj.dev.utils.FileUtils;
import com.alinaj.dev.utils.StreamGobbler;
import com.alinaj.dev.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Pdnsd extends Thread {

    private final static String TAG = "PdnsdThread";
    private final static String PDNSD_SERVER = "server {\n label= \"%1$s\";\n ip = %2$s;\n port = %3$d;\n uptest = none;\n }\n";
    private final static String PDNSD_SERVER_TEST = "server {\n label= \"%1$s\";\n ip = %2$s;\n port = %3$d;\n reject = ::/0;\n reject_policy = negate;\n reject_recursively = on;\n timeout = 5;\n }\n";
    private final static String PDNSD_BIN = "libpdnsd.so";

    private OnPdnsdListener mListener;

    public interface OnPdnsdListener {
        void onStart();

        void onStop();
    }

    private Process mProcess;
    private File filePdnsd;

    private final Context mContext;
    private final String[] mDnsHosts;
    private final int mDnsPort;
    private final String mPdnsdHost;
    private final int mPdnsdPort;

    public Pdnsd(Context context, String[] dnsHosts, int dnsPort, String pdnsdHost, int pdnsdPort) {
        mContext = context;

        mDnsHosts = dnsHosts;
        mDnsPort = dnsPort;
        mPdnsdHost = pdnsdHost;
        mPdnsdPort = pdnsdPort;
    }

    @Override
    public void run() {

        if (mListener != null) {
            mListener.onStart();
        }

        try {
            ApplicationInfo appInfo = mContext.getApplicationInfo();
            String filePdnsd = appInfo.nativeLibraryDir;

            //File filePdnsd = CustomNativeLoader.loadExecutableBinary(mContext, "libpdnsd.so");
//			filePdnsd = CustomNativeLoader.loadNativeBinary(mContext, PDNSD_BIN, new File(mContext.getFilesDir(), PDNSD_BIN));
//			
//			if (filePdnsd == null) {
//				throw new IOException("Bin Pdnsd não encontrada");
//			}

            File fileConf = makePdnsdConf(mContext.getFilesDir(), mDnsHosts, mDnsPort, mPdnsdHost, mPdnsdPort);

            String cmdString = filePdnsd + "/" + PDNSD_BIN + " -v9 -c " + fileConf;

            mProcess = Runtime.getRuntime().exec(cmdString);

            StreamGobbler.OnLineListener onLineListener = new StreamGobbler.OnLineListener() {
                @Override
                public void onLine(String log) {
                    log("Pdnsd: " + log);
                }
            };

            StreamGobbler stdoutGobbler = new StreamGobbler(mProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(mProcess.getErrorStream(), onLineListener);

            stdoutGobbler.start();
            stderrGobbler.start();

            mProcess.waitFor();

        } catch (IOException e) {
            log("Pdnsd IOError: " + e);
        } catch (Exception e) {
            log("Pdnsd Error: " + e);
        }

        mProcess = null;
        if (mListener != null) {
            mListener.onStop();
        }

    }

    @Override
    public synchronized void interrupt() {
        // TODO: Implement this method
        super.interrupt();

        if (mProcess != null)
            mProcess.destroy();

        try {
            if (filePdnsd != null)
                VPNUtils.killProcess(filePdnsd);
        } catch (Exception e) {
        }

        mProcess = null;
        filePdnsd = null;
    }

    private File makePdnsdConf(File fileDir, String[] dnsHosts, int dnsPort, String pdnsdHost, int pdnsdPort) throws IOException {
        String content = FileUtils.readFromRaw(mContext, R.raw.pdnsd_local);

        // dns servers
        StringBuilder server_dns = new StringBuilder();
        for (int i = 0; i < dnsHosts.length; i++) {
            String dnsHost = dnsHosts[i];
            server_dns.append(String.format(PDNSD_SERVER, "server" + (i + 1), dnsHost, dnsPort));
            //server_dns.append(String.format(PDNSD_SERVER_TEST, "server" + Integer.toString(i+1), "127.0.0.1", 8865));
        }

        String conf = String.format(content, server_dns, fileDir.getCanonicalPath(), pdnsdHost, pdnsdPort);

        Log.d(TAG, "pdnsd conf:" + conf);

        File f = new File(fileDir, "pdnsd.conf");
        if (f.exists()) {
            f.delete();
        }
        FileUtils.saveTextFile(f, conf);

        File cache = new File(fileDir, "pdnsd.cache");
        if (!cache.exists()) {
            try {
                cache.createNewFile();
            } catch (Exception e) {
            }
        }

        return f;
    }

    public void setOnPdnsdListener(OnPdnsdListener listener) {
        this.mListener = listener;
    }

    private void log(String msg) {
        Log.d("technore_logs", msg);
    }
}
