package com.alinaj.dev.activities;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;

import android.util.Log;
import android.view.KeyEvent;
import java.io.IOException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import net.openvpn.openvpn.HttpsClient.AuthContext;
import net.openvpn.openvpn.HttpsClient.CancelDetect;
import net.openvpn.openvpn.HttpsClient.CancelDetect.I;
import net.openvpn.openvpn.HttpsClient.Task;
import com.alinaj.dev.service.OpenVPNService.*;

import org.json.*;
import java.io.*;
import android.widget.*;
import com.alinaj.dev.utils.Utils.*;
import com.alinaj.dev.service.*;
import net.openvpn.openvpn.*;

import com.alinaj.dev.utils.VPNUtil;
import com.alinaj.dev.R;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog.Builder;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public abstract class OpenVPNClientBase extends AppCompatActivity implements InjectorService.InjectorListener, EventReceiver {
    private static final String TAG = "OpenVPNClientBase";
    public static int themeResId;
    public static boolean themeSet = false;
    protected SSHService mSSHService;
    protected OpenVPNService mBoundService = null;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            OpenVPNClientBase.this.mBoundService = ((LocalBinder) service).getService();
            VPNUtil.setVPNService(mBoundService);
            Log.d(OpenVPNClientBase.TAG, "CLIBASE: onServiceConnected: " + OpenVPNClientBase.this.mBoundService.toString());
            OpenVPNClientBase.this.mBoundService.client_attach(OpenVPNClientBase.this);
            OpenVPNClientBase.this.post_bind();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(OpenVPNClientBase.TAG, "CLIBASE: onServiceDisconnected");
            OpenVPNClientBase.this.mBoundService = null;
        }
    };
    protected InjectorService mInjector;
    private final ServiceConnection mInjectorConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName p1, IBinder p2)
        {
            mInjector = ((InjectorService.MyBinder)p2).getService();
            mInjector.setInjectorListener(OpenVPNClientBase.this);
            // TODO: Implement this method
        }

        @Override
        public void onServiceDisconnected(ComponentName p1)
        {
            mInjector = null;
            // TODO: Implement this method
        }
        
        
    };

    private final ServiceConnection mSSHTunnelConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSSHService = ((SSHService.SSHTunnelBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mSSHService = null;
        }
    };

    protected interface EpkiPost {
        void post_dispatch(String str);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }
    protected JSONObject getJSONObject()
    {
        File file = new File(getFilesDir(), "Servers.js");
        try {
            if (file.exists()) {
                return new JSONObject(readStream(new FileInputStream(file)));
            } else {
                String s = readStream(getResources().openRawResource(R.raw.servers));
                String s2 = Parser.parseToString(s);
                return new JSONObject(s2);
            }
        } catch (Exception e) {

        }
        return null;
    }

    protected JSONArray getServersArray()
    {
        try {
            JSONArray obj = getJSONObject().getJSONArray("Servers");
            return obj;
        } catch (Exception e) {
            //showToast(e.getMessage());
        }
        return null;
    }
    protected JSONArray getNetworksArray()
    {
        try {
            JSONArray js = getJSONObject().getJSONArray("Networks");
            return js;
        } catch (Exception e) {

        }
        return null;
    }
    protected JSONArray getSSLNetworks()
    {
        try {
            return getJSONObject().getJSONArray("SSLNetworks");
        } catch (Exception e) {
            return null;
        }
    }
    protected JSONArray getCustomTweaks()
    {
        File file = new File(getFilesDir(), "custom_data.technore");
        try {
            if (file.exists()) {
                return new JSONArray(readStream(new FileInputStream(file)));
            } else {
                return new JSONArray("[{\n" +
                        "      \"Name\": \"Custom Payload\",\n" +
                        "      \"Info\": \"Custom 1\",\n" +
                        "      \"Payload\": \"\",\n" +
                        "      \"SNIHost\": \"\",\n" +
                        "      \"TunnelType\": 0,\n" +
                        "      \"ProxySettings\": {\n" +
                        "        \"Squid\": \"\",\n" +
                        "        \"Port\": \"80\"\n" +
                        "      }\n" +
                        "    }]");
            }
        } catch (Exception e) {

        }
        return null;
    }
    protected void setCustomTweaks(JSONArray jsonArray) {
        try {
            OutputStream output = new FileOutputStream(new File(getFilesDir(), "custom_data.technore"));
            output.write(jsonArray.toString().getBytes());
            output.flush();
            output.close();
        } catch (Exception e) {
            showToast(e.toString());
        }
    }
    protected String readStream(InputStream in)
    {
        StringBuilder sb = new StringBuilder();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in));
            char[] buff = new char[1024];
            while (true) {
                int read = reader.read(buff, 0, buff.length);
                if (read <= 0) {
                    break;
                }
                sb.append(buff, 0, read);
            }
        } catch (Exception e) {

        }
        return sb.toString();
    }
    protected void showToast(String str)
    {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }
    protected void setCurrentTheme(int resId) {
        themeResId = resId;
        themeSet = true;
        setTheme(themeResId);
    }
    protected OpenVPNService getOpenVPNService()
    {
        return this.mBoundService;
    }
    protected void doBindService() {
        bindService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_BIND), this.mConnection, 65);
        bindService(new Intent(this, SSHService.class), mSSHTunnelConnection, Context.BIND_AUTO_CREATE);
    }
    protected void dobindInjector()
    {
        bindService(new Intent(this, InjectorService.class), mInjectorConnection, 1);
    }

    protected void doUnbindService() {
        Log.d(TAG, "CLIBASE: doUnbindService");
        if (this.mBoundService != null) {
            this.mBoundService.client_detach(this);
            unbindService(this.mConnection);
            this.mBoundService = null;
        }
        if (mSSHService != null) {
            unbindService(mSSHTunnelConnection);
            mSSHService = null;
        }
        
    }
    protected void unbindInjector()
    {
        if (mInjector != null) {
             unbindService(mInjectorConnection);
             mInjector = null;
         }
    }
    @Override
    public void startOpenVPN()
    {
        // TODO: Implement this method
    }
    /*protected void startImport(OpenVPNClient main, String file)
    {
        
            //showToast("Update Success!");
        } catch (Exception e) {
            showToast(String.format("Update Error: %s - %s", e.getClass().getName(), e.getMessage()));
        }
    }*/
    protected void post_bind() {
    }

    public void event(EventMsg ev) {
    }

    public void log(LogMsg lm) {
    }

    public PendingIntent get_configure_intent(int requestCode) {
        return null;
    }

    protected boolean is_active() {
        return this.mBoundService != null && this.mBoundService.is_active();
    }

    protected Profile current_profile() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_current_profile();
        }
        return null;
    }

    protected ProfileList profile_list() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_profile_list();
        }
        return null;
    }

    protected long max_profile_size() {
        return OpenVPNService.max_profile_size();
    }

    protected MergedProfile merge_parse_profile(String basename, String profile_content) {
        if (this.mBoundService != null) {
            return this.mBoundService.merge_parse_profile(basename, profile_content);
        }
        return null;
    }

    protected ClientAPI_LLVector get_stat_values_full() {
        if (this.mBoundService != null) {
            return this.mBoundService.stat_values_full();
        }
        return null;
    }

    protected ConnectionStats get_connection_stats() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_connection_stats();
        }
        return null;
    }

    protected long get_tunnel_bytes_per_cpu_second() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_tunnel_bytes_per_cpu_second();
        }
        return 0;
    }

    protected EventMsg get_last_event() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_last_event();
        }
        return null;
    }

    protected EventMsg get_last_event_prof_manage() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_last_event_prof_manage();
        }
        return null;
    }

    public ArrayDeque<LogMsg> log_history() {
        if (this.mBoundService != null) {
            return this.mBoundService.log_history();
        }
        return null;
    }

    protected void jellyBeanHackPurge() {
        if (this.mBoundService != null) {
            this.mBoundService.jellyBeanHackPurge();
        }
    }

    protected ProxyList get_proxy_list() {
        if (this.mBoundService != null) {
            return this.mBoundService.proxy_list;
        }
        return null;
    }

    public void gen_proxy_context_expired_event() {
        if (this.mBoundService != null) {
            this.mBoundService.gen_proxy_context_expired_event();
        }
    }

    protected void gen_ui_reset_event(boolean exclude_self) {
        if (this.mBoundService != null) {
            this.mBoundService.gen_ui_reset_event(exclude_self, this);
        }
    }

    protected String get_gui_version(String name) {
        String versionName = "0.0";
        int versionCode = 0;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pi.versionName;
            versionCode = pi.versionCode;
        } catch (Exception e) {
            Log.e(TAG, "cannot obtain version info", e);
        }
        return String.format("%s %s-%d", name, versionName, Integer.valueOf(versionCode));
    }

    protected void submitConnectIntent(String profile_name, String server, String vpn_proto, String ipv6, String conn_timeout, String username, String password, boolean cache_password, String pk_password, String response, String epki_alias, String compression_mode, String proxy_name, String proxy_username, String proxy_password, boolean proxy_allow_creds_dialog, String gui_version) {
        String prefix = OpenVPNService.INTENT_PREFIX;
        Intent intent = new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_CONNECT).putExtra(prefix + ".PROFILE", profile_name).putExtra(prefix + ".GUI_VERSION", gui_version).putExtra(prefix + ".PROXY_NAME", proxy_name).putExtra(prefix + ".PROXY_USERNAME", proxy_username).putExtra(prefix + ".PROXY_PASSWORD", proxy_password).putExtra(prefix + ".PROXY_ALLOW_CREDS_DIALOG", proxy_allow_creds_dialog).putExtra(prefix + ".SERVER", server).putExtra(prefix + ".PROTO", vpn_proto).putExtra(prefix + ".IPv6", ipv6).putExtra(prefix + ".CONN_TIMEOUT", conn_timeout).putExtra(prefix + ".USERNAME", username).putExtra(prefix + ".PASSWORD", password).putExtra(prefix + ".CACHE_PASSWORD", cache_password).putExtra(prefix + ".PK_PASSWORD", pk_password).putExtra(prefix + ".RESPONSE", response).putExtra(prefix + ".EPKI_ALIAS", epki_alias).putExtra(prefix + ".COMPRESSION_MODE", compression_mode);
        if (this.mBoundService != null) {
            this.mBoundService.client_attach(this);
        }
        ComponentName serv = startService(intent);
        Log.d(TAG, "CLI: submitConnectIntent: " + profile_name);
    }

    public static void autostart(Context context) {
        PrefUtil prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(context));
        String profile_name = prefs.get_string("autostart_profile_name");
        if (profile_name != null) {
            prefs.delete_key("autostart_profile_name");
            if (prefs.get_boolean("autostart", false)) {
                Intent intent = new Intent(context, OpenVPNClient.class).addFlags(276824064).putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", profile_name);
                context.startActivity(intent);
                Log.d(TAG, "CLIBASE: autostart profile='" + profile_name + "' intent=" + intent);
            }
        }
    }

    protected Intent buildDisconnectIntent(boolean stop_service) {
        return new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_DISCONNECT).putExtra(OpenVPNService.INTENT_PREFIX + ".STOP", stop_service);
    }

    protected void submitDisconnectIntent(boolean stop_service) {
        Log.d(TAG, "CLIBASE: submitDisconnectIntent");
        startService(buildDisconnectIntent(stop_service));
    }

    protected void submitImportProfileIntent(String profile_content, String save_as_filename, boolean merge) {
        String prefix = OpenVPNService.INTENT_PREFIX;
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_IMPORT_PROFILE).putExtra(prefix + ".CONTENT", profile_content).putExtra(prefix + ".FILENAME", save_as_filename).putExtra(prefix + ".MERGE", merge));
    }

    protected void submitImportProfileViaPathIntent(String profile_path) {
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_IMPORT_PROFILE_VIA_PATH).putExtra(OpenVPNService.INTENT_PREFIX + ".PATH", profile_path));
    }

    protected void submitDeleteProfileIntent(String profile_name) {
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_DELETE_PROFILE).putExtra(OpenVPNService.INTENT_PREFIX + ".PROFILE", profile_name));
    }

    protected void submitDeleteProfileIntentWithConfirm(final String profile_name) {
        OnClickListener dialogClickListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case -1:
                        OpenVPNClientBase.this.submitDeleteProfileIntent(profile_name);
                        return;
                    default:
                }
            }
        };
        new Builder(this).setTitle(R.string.delete_profile_confirm_title).setMessage(profile_name).setPositiveButton(R.string.delete_profile_confirm_yes, dialogClickListener).setNegativeButton(R.string.delete_profile_confirm_cancel, dialogClickListener).show();
    }

    protected void submitRenameProfileIntent(String profile_name, String new_profile_name) {
        String prefix = OpenVPNService.INTENT_PREFIX;
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_RENAME_PROFILE).putExtra(prefix + ".PROFILE", profile_name).putExtra(prefix + ".NEW_PROFILE", new_profile_name));
    }

    private void createShortcut(String shortcut_name, Intent shortcutIntent, int iconId) {
        /*if (ShortcutManagerCompat.isRequestPinShortcutSupported(getApplicationContext())) {
            ShortcutManagerCompat.requestPinShortcut(getApplicationContext(), new ShortcutInfoCompat.Builder(this, shortcut_name).setShortLabel(shortcut_name).setLongLabel(shortcut_name).setIcon(IconCompat.createWithResource(getApplicationContext(), iconId)).setIntent(shortcutIntent).build(), null);
        }*/
    }

    protected void createConnectShortcut(String profile_name, String shortcut_name) {
        createShortcut(shortcut_name, new Intent(this, OpenVPNClient.class).addFlags(8388608).setAction(OpenVPNService.ACTION_CONNECT).putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", profile_name), R.drawable.openvpn_connected);
    }

    protected void createDisconnectShortcut(String shortcut_name) {
        createShortcut(shortcut_name, new Intent(this, OpenVPNDisconnect.class).addFlags(8388608).setAction(OpenVPNService.ACTION_DISCONNECT), R.drawable.openvpn_disconnected);
    }

    protected void createAppShortcut(String shortcut_name) {
        createShortcut(shortcut_name, new Intent(this, OpenVPNClient.class).setAction("android.intent.action.RUN"), R.drawable.ic_launcher);
    }

    protected void resolveExternalPkiAlias(Profile prof, final EpkiPost next_action) {
        final Handler handler = new Handler();
        final OpenVPNClientBase topthis = this;
        final KeyChainAliasCallback response = new KeyChainAliasCallback() {
            public void alias(final String alias) {
                if (alias != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            next_action.post_dispatch(alias);
                        }
                    });
                }
            }
        };
        OnClickListener dialogClickListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case -2:
                        next_action.post_dispatch("DISABLE_CLIENT_CERT");
                        return;
                    case -1:
                        KeyChain.choosePrivateKeyAlias(topthis, response, new String[]{"RSA"}, null, null, -1, null);
                        return;
                    default:
                }
            }
        };
        if (prof == null || !prof.need_external_pki_alias()) {
            next_action.post_dispatch(null);
        } else {
            next_action.post_dispatch("DISABLE_CLIENT_CERT");
            //new Builder(topthis).setTitle(R.string.select_certificate_title).setMessage(R.string.select_certificate_message).setPositiveButton(R.string.select_certificate_yes, dialogClickListener).setNegativeButton(R.string.select_certificate_no, dialogClickListener).show();
        }
    }

    protected void import_pkcs12(String path) {
        jellyBeanHackPurge();
        try {
            Intent intent = KeyChain.createInstallIntent();
            intent.putExtra("PKCS12", FileUtil.readFileByteArray(path, 262144));
            startActivity(intent);
        } catch (IOException e) {
            ok_dialog(String.format("[ %s ] %s", path, getText(R.string.file_read_error)));
        }
    }

    protected static String render_duration(int duration) {
        int seconds = duration % 60;
        int minutes = (duration / 60) % 60;
        return String.format("%d:%02d:%02d", Integer.valueOf(duration / 3600), Integer.valueOf(minutes), Integer.valueOf(seconds));
    }

    protected void ok_dialog(String text) {
        ok_dialog(null, text);
    }

    protected void ok_dialog(String title, String message) {
        ok_dialog(title, message, null);
    }

    protected void ok_dialog(String title, String message, Runnable event) {
        final Runnable okEvent = event;
        Builder alert = new Builder(this).setMessage(message).setPositiveButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (okEvent != null) {
                    okEvent.run();
                }
            }
        });
        if (title != null) {
            alert.setTitle(title);
        }
        alert.show();
    }

    protected boolean action_enter(int actionId, KeyEvent event) {
        return actionId == 5 || actionId == 6 || (actionId == 0 && event != null && event.getAction() == 0 && event.getRepeatCount() == 0);
    }

    protected void raise_file_selection_dialog(int requestCode, int prompt_res_id) {
        startActivityForResult(new Intent(this, FileDialog.class).putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath()).putExtra(FileDialog.CAN_SELECT_DIR, false).putExtra(FileDialog.SELECTION_MODE, 1).putExtra(FileDialog.OPTION_ONE_CLICK_SELECT, false).putExtra(FileDialog.OPTION_PROMPT, resString(prompt_res_id)), requestCode);
    }

    protected static String get_app_expire_string() {
        Date expire = OpenVPNService.get_app_expire();
        if (expire != null) {
            return DateFormat.getDateTimeInstance().format(expire);
        }
        return null;
    }

    protected void warn_app_expiration(PrefUtil prefs) {
        String expire_string = get_app_expire_string();
        if (expire_string != null && !expire_string.equals(prefs.get_string("app_expire_string"))) {
            ok_dialog(String.format(resString(R.string.beta_expire_warn), expire_string));
            prefs.set_string("app_expire_string", expire_string);
        }
    }

    protected static String get_openvpn_core_platform() {
        return OpenVPNService.get_openvpn_core_platform();
    }

    protected void init_default_preferences(PrefUtil prefs) {
        if (!prefs.contains_key("vpn_proto")) {
            prefs.set_string("vpn_proto", "adaptive");
        }
        if (!prefs.contains_key("ipv6")) {
            prefs.set_string("ipv6", "default");
        }
        if (!prefs.contains_key("conn_timeout")) {
            prefs.set_string("conn_timeout", "60");
        }
        if (!prefs.contains_key("compression_mode")) {
            prefs.set_string("compression_mode", "yes");
        }
        if (!prefs.contains_key("tls_version_min_override")) {
            prefs.set_string("tls_version_min_override", "default");
        }
        if (!prefs.contains_key("auto_keyboard")) {
            prefs.set_boolean("auto_keyboard", true);
        }
        if (!prefs.contains_key("google_dns_fallback")) {
            prefs.set_boolean("google_dns_fallback", true);
        }
        if (!prefs.contains_key("autostart_finish_on_connect")) {
            prefs.set_boolean("autostart_finish_on_connect", true);
        }
        if (!prefs.contains_key("cert_profile_insecure")) {
            prefs.set_boolean("cert_profile_insecure", true);
        }
        if (!prefs.contains_key("ui_dark_theme")) {
            prefs.set_boolean("ui_dark_theme", false);
        }
    }

    protected String resString(int res_id) {
        return getResources().getString(res_id);
    }

    protected void importProfileRemote(AuthContext ac, boolean prefer_autologin, I cancel_source, Runnable on_success, Runnable on_fail, String save_as_filename, boolean enable_cert_dialog, boolean enable_trust_error_dialog) {
        final Handler handler = new Handler();
        final CancelDetect cancel = new CancelDetect(cancel_source);
        final AuthContext authContext = ac;
        final boolean z = prefer_autologin;
        final String str = save_as_filename;
        final Runnable runnable = on_success;
        HttpsClient.run_task(this, new Task() {
            public void run() {
                try {
                    get_session_id(authContext);
                    int pavail = profile_types_available(authContext);
                    if (z && (pavail & 1) != 0) {
                        dispatch_prof(get_profile(authContext, "GetAutologin"));
                    } else if ((pavail & 2) != 0) {
                        dispatch_prof(get_profile(authContext, "GetUserlogin"));
                    } else {
                        this.interact.error_dialog(R.string.profile_import_error, R.string.no_profile_types, null);
                    }
                    close_session(authContext);
                } catch (ErrorDialogException e) {
                    e.dispatch(this.interact);
                } catch (Exception e2) {
                    Log.e(OpenVPNClientBase.TAG, "import_profile", e2);
                    this.interact.error_dialog(R.string.profile_import_error, 0, e2);
                }
            }

            private void dispatch_prof(final String profile_content) {
                handler.post(new Runnable() {
                    public void run() {
                        if (!cancel.is_canceled()) {
                            if (str != null) {
                                OpenVPNClientBase.this.submitImportProfileIntent(profile_content, str, true);
                            } else {
                                OpenVPNClientBase.this.submitImportProfileIntent(profile_content, "client.ovpn", true);
                            }
                            handler.post(runnable);
                        }
                    }
                });
            }
        }, cancel_source, on_fail, enable_cert_dialog, enable_trust_error_dialog, max_profile_size());
    }

    protected void dumpCryptoAlgs() {
        try {
            for (Provider provider : Security.getProviders()) {
                Log.d(TAG, "CRYPTO provider: " + provider.getName() + " ****************");
                for (Service service : provider.getServices()) {
                    Log.d(TAG, "CRYPTO algorithm: " + service.getAlgorithm());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "dumpCryptoAlgs", e);
        }
    }
}
