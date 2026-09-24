package com.alinaj.dev.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.alinaj.dev.openconnect.core.OpenVpnService;
import com.alinaj.dev.openconnect.core.ProfileManager;
import com.alinaj.dev.openconnect.core.VPNConnector;
import com.alinaj.dev.psiphon.TunnelManager;
import com.alinaj.dev.psiphon.TunnelServiceInteractor;
import com.alinaj.dev.service.SSHService;
import com.alinaj.dev.service.TimerService;
import com.alinaj.dev.thread.RetryingThread;
import com.alinaj.dev.utils.CustomNativeLoader;
import com.alinaj.dev.utils.KillThis;
import com.alinaj.dev.utils.TeaBase64;
import com.alinaj.dev.v2ray.utils.AppConfigs;
import com.alinaj.dev.R;
import com.alinaj.dev.ServerSelectDialog;
import com.alinaj.dev.adapter.Adapter;
import com.alinaj.dev.adapter.Adapter.NetworkAdapter;
import com.alinaj.dev.adapter.Adapter.ServerAdapter;
import com.alinaj.dev.core.ConfigParser;
import com.alinaj.dev.core.Connection;
import com.alinaj.dev.core.PasswordCache;
import com.alinaj.dev.core.VpnProfile;
import com.alinaj.dev.helper.GeneratorHelper;
import com.alinaj.dev.json.JsonManager.*;
import com.alinaj.dev.service.InjectorService;
import com.alinaj.dev.service.OpenVPNService.Challenge;
import com.alinaj.dev.service.OpenVPNService.ConnectionStats;
import com.alinaj.dev.service.OpenVPNService.EventMsg;
import com.alinaj.dev.service.OpenVPNService.Profile;
import com.alinaj.dev.service.OpenVPNService.ProfileList;
import com.alinaj.dev.service.SocksDNSService;
import com.alinaj.dev.service.vpn.TunnelManagerHelper;
import com.alinaj.dev.service.vpn.logger.ConnectionStatus;
import com.alinaj.dev.service.vpn.logger.SkStatus;
import com.alinaj.dev.thread.DNSTunnelThread;
import com.alinaj.dev.utils.AppRemote;
import com.alinaj.dev.utils.ConfigUtil;
import com.alinaj.dev.utils.ExceptionHandlerUtils;
import com.alinaj.dev.utils.ExpireDate;
import com.alinaj.dev.utils.StatisticsGraphData;
import com.alinaj.dev.utils.Utils;
import com.alinaj.dev.v2ray.V2ray2Json;
import com.alinaj.dev.v2ray.V2rayController;
import com.alinaj.dev.view.CircleProgressBar;
import com.alinaj.dev.view.PayloadGenerator;

import net.openvpn.openvpn.BuildConfig;
import net.openvpn.openvpn.ClientAPI_ConnectionInfo;
import net.openvpn.openvpn.PasswordUtil;
import net.openvpn.openvpn.PrefUtil;
import net.openvpn.openvpn.ProxyList;
import net.openvpn.openvpn.SpinUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.lsposed.lsparanoid.Obfuscate;

import me.wangyuwei.flipshare.FlipShareView;
import me.wangyuwei.flipshare.ShareItem;


@Obfuscate
public class OpenVPNClient extends OpenVPNClientBase implements /*OnRequestPermissionsResultCallback, */OnClickListener, OnTouchListener, OnItemSelectedListener, OnEditorActionListener, ExpireDate.ExpireDateListener,
        /*NetworkAdapter.OnNetworkSelectedListener,*/ PayloadGenerator.GeneratorListener,
        RadioGroup.OnCheckedChangeListener, BottomNavigationView.OnNavigationItemSelectedListener,
        /*ServerAdapter.OnServerSelectedListener,*/ GeneratorHelper.GeneratorListener, DNSTunnelThread.SocksListener, SkStatus.StateListener, SocksDNSService.TunListener {


    public String bannerID = "ca-app-pub-39544/61";
    public String interstitialID = "ca-app-pub-392544/1712";
    public String rewardedAdsID = "ca-app-pub-3942544/517";
    private ImageView mSimpleDraweeView;
    private TextView status_01;
    private static final int REQUEST_IMPORT_PKCS12 = 3;
    private static final int REQUEST_IMPORT_PROFILE = 2;
    private static final int REQUEST_VPN_ACTOR_RIGHTS = 1;
    private static final boolean RETAIN_AUTH = false;
    private static final int S_BIND_CALLED = 1;
    private static final int S_ONSTART_CALLED = 2;
    private static final String TAG = "OpenVPNClient";
    private static final int UIF_PROFILE_SETTING_FROM_SPINNER = 262144;
    private static final int UIF_REFLECTED = 131072;
    private static final int UIF_RESET = 65536;
    private static final boolean UI_OVERLOADED = false;
    private String autostart_profile_name;
    private View button_group;
    private TextView bytes_in_view;
    private TextView bytes_out_view;
    private TextView challenge_view;
    private View conn_details_group;
    private Button connect_button;
    private Button disconnect_button;
    private View cr_group;
    private FinishOnConnect delayed_finish_on_connect = FinishOnConnect.DISABLED;
    private TextView details_more_less;

    private TextView duration_view;
    private FinishOnConnect finish_on_connect = FinishOnConnect.DISABLED;
    private View info_group;
    private boolean last_active = RETAIN_AUTH;
    private TextView last_pkt_recv_view;
    private ScrollView main_scroll_view;
    private EditText password_edit;
    private View password_group;
    private CheckBox password_save_checkbox;
    private EditText pk_password_edit;
    private View pk_password_group;
    private CheckBox pk_password_save_checkbox;
    private View post_import_help_blurb;
    private PrefUtil prefs;
    private ImageButton profile_edit;
    private View profile_group;
    private Spinner profile_spin;
    private ProgressBar progress_bar;
    private ImageButton proxy_edit;
    private View proxy_group;
    private Spinner proxy_spin;
    private PasswordUtil pwds;
    private EditText response_edit;
    private View server_group;
    private Spinner server_spin;
    private int startup_state = 0;
    private View stats_expansion_group;
    private View stats_group;
    private VPNConnector mConn;
    private int protocol = 0;
    private final Handler stats_timer_handler = new Handler();
    private final Runnable stats_timer_task = new Runnable() {
        public void run() {
            OpenVPNClient.this.show_stats();
            OpenVPNClient.this.schedule_stats();
        }
    };
    private boolean clicked = false;
    private ImageView status_icon_view;
    private TextView status_view;
    private final boolean stop_service_on_client_exit = RETAIN_AUTH;
    private View[] textgroups;
    private TextView[] textviews;
    private final Handler ui_reset_timer_handler = new Handler();
    private final Runnable ui_reset_timer_task = new Runnable() {
        public void run() {
            if (!OpenVPNClient.this.is_active()) {
                OpenVPNClient.this.ui_setup(OpenVPNClient.RETAIN_AUTH, OpenVPNClient.UIF_RESET, null);
            }
        }
    };
    private boolean isStart = false;
    private EditText username_edit;
    private View username_group;
    private ConfigUtil config;
    private boolean showNoUpdate;
    private TextView timeLeft;
    private TextView con_status;
    private TextView mServerVersion;
    private BottomSheetDialog mBottomSheetDialog;
    private TextView mExpirationDate;
    private View mNetworkLayout;
    private ArrayList<JSONObject> listNetwork;
    public NetworkAdapter networkAdapter;
    private ArrayList<JSONObject> arrayList;
    private JSONArray servers = new JSONArray();
    private TimerService timerService;
    private TunnelServiceInteractor psiphonHelper;
    private View options_group;
    //private View mServerLayout;

    private ImageView bannerImage;
    private final List<String> bannerUrls = new ArrayList<>();
    private int currentIndex = 0;
    private final Handler handler = new Handler();
    private final int BANNER_INTERVAL = 5000;
    private Runnable bannerRunnable;

    public Adapter.ServerAdapter mServerAdapter;

    private ServerSelectDialog mServerDialog;

    public ArrayList<JSONObject> listProfiles;

    private int mRandowmServer = 0;

    //private TextView mIpAddr;

    private Timer timer;

    //private TextView mConfigVersion;

    //private SwitchButton mCustomTweakSw;

    private EditText mCustomTweakEdit;

    //private Spinner mNetworkSpin;

    private boolean autoUpdate;

    private final ServiceConnection timerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            timerService = ((TimerService.ServiceBinder) binder).getService();
            timerService.setTimerListener(new TimerService.TimerListener() {
                @Override
                public void onTimeChanged(String time) {
                    timeLeft.setText(time);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName p1) {
            timerService = null;
        }
    };

    private Spinner network_spin;
    private CircleProgressBar progress;
    private BroadcastReceiver v2rayBroadCastReceiver;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;
    private AlertDialog checkingDialog = null;

    private final String auth_api = "https://penel-demo.ggff.net/api/auth.php?username=%s&password=%s&device_id=%s&device_model=%s";  // copy panel
    //  private String auth_api = "https://rktunnelvip.xyz/api/soodeif7ah/auth?username=%s&password=%s&device_id=%s&device_model=%s";  // kobz panel


    @Override
    public void startSocksOpenVPN() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //   startOpenVPN();

            }
        });
    }

    @Override
    public void addStatus(String log) {
        if (ConfigUtil.isV2RAY()) {
            return;
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (log.contains("Invalid Authentication!")) {
                    showToast(log);
                } else if (log.contains("Disconnected")) {
                    progress.setProgressWithAnimation((float) 0);
                    progress.setColor(getResources().getColor(R.color.jx_start_bg));
                    disconnect_button.setTextColor(getResources().getColor(R.color.jx_start_bg));
                    OpenVPNClient.this.status_01.setTextColor(OpenVPNClient.this.getResources().getColor(R.color.jx_start_bg));
                    OpenVPNClient.this.status_01.setText("Disconnected");
                    OpenVPNClient.this.isConnected = false;
                } else if (log.contains("Waiting for UDP response")) {
                    progress.setProgressWithAnimation((float) 60);
                } else if (log.contains("UDP is running")) {
                    if (clicked) {
                        progress.setProgressWithAnimation((float) 100);
                    } else {
                        progress.setProgress((float) 100);
                    }
                    progress.setColor(getResources().getColor(R.color.jx_stop_bg));
                    disconnect_button.setTextColor(getResources().getColor(R.color.jx_stop_bg));
                    status_view.setTextColor(getResources().getColor(R.color.connectedcolor));
                    OpenVPNClient.this.status_01.setTextColor(OpenVPNClient.this.getResources().getColor(R.color.jx_stop_bg));
                    OpenVPNClient.this.status_01.setText("Connected");
                    OpenVPNClient.this.isConnected = true;
                    status(true);
                    if (isStart) {
                        //startService(new Intent(OpenVPNClient.this, TimerService.class));
                        showInterstitialAds();
                        runBanner();
                        loadBanners();
                        isStart = false;
                    }
                }
            }
        });
    }

    @Override
    public void stopSocksOpenVPN() {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enabledWidgets(true);
                stopUdp();
                stopVPN();

            }
        });
    }

    @Override
    public void updateBytesTime() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!SkStatus.isTunnelActive()) {
                    return;
                }

                if (config.getTunnelType() == ConfigUtil.MODE_UDP) {
                    duration_view.setText(SocksDNSService.getTime());
                    long d = StatisticsGraphData.getStatisticData().getDataTransferStats().getBytesReceived();
                    config.setBytesIn(d + config.getBytesIn());
                    long u = StatisticsGraphData.getStatisticData().getDataTransferStats().getBytesSent();
                    config.setBytesOut(u + config.getBytesOut());
                    bytes_in_view.setText(render_bandwidth(config.getBytesIn()));
                    bytes_out_view.setText(render_bandwidth(config.getBytesOut()));
                } else {
                    if (protocol == 1) {
                        duration_view.setText(render_duration(OpenVpnService.getDuration()));
                    } else if (protocol == 2) {
                        duration_view.setText(render_duration(SSHService.getDuration()));
                    } else {
                        duration_view.setText(render_duration(TunnelManager.getDuration()));
                    }
                    bytes_in_view.setText(StatisticsGraphData.getStatisticData().getDataTransferStats().byteCountToDisplaySize(StatisticsGraphData.getStatisticData().getDataTransferStats().getTotalBytesReceived(), false));
                    bytes_out_view.setText(StatisticsGraphData.getStatisticData().getDataTransferStats().byteCountToDisplaySize(StatisticsGraphData.getStatisticData().getDataTransferStats().getTotalBytesSent(), false));
                }
            }
        });
    }

    private void startVerifyingAccount() {
        if (!RetryingThread.thVerifyIsRunning) {
            new RetryingThread(retryingCount -> {
                if (retryingCount == 1) {
                    checkUpdates();
                } else if (retryingCount == 2) {
                    if (isConnected) checkUpdates();
                }
            }).startVerifyingAccount();
        }
    }

    @Override
    public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //   startOpenVPN();
                setStarterButton();
                long e = 0;
                config.setBytesIn(e);
                config.setBytesOut(e);
            }
        });


    }

    public void setStarterButton() {

        if (ConfigUtil.isV2RAY()) {
            return;
        }

        String state = SkStatus.getLastState();
        boolean isRunning = SkStatus.isTunnelActive();
        TextView status = findViewById(R.id.status);


        if (SkStatus.SSH_STARTING.equals(state) || SkStatus.SSH_CONNECTING.equals(state)) {
            if (config.getTunnelType() == ConfigUtil.MODE_PSIPHON || isSSHorOC()) {
                progress.setProgressWithAnimation((float) 35);
                progress.setColor(getResources().getColor(R.color.jx_start_bg));
            }
            show_status("Connecting");
            status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
            //	starterButton.setEnabled(false);
        } else if (SkStatus.SSH_STOPPING.equals(state)) {

            show_status("Stopping");
            status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
            //	starterButton.setEnabled(false);
        } else if (SkStatus.SSH_DISCONNECTED.equals(state)) {
            if (config.getTunnelType() == ConfigUtil.MODE_PSIPHON || isSSHorOC()) {
                progress.setProgressWithAnimation((float) 0);
                progress.setColor(getResources().getColor(R.color.jx_start_bg));
            }
            show_status("Disconnected");
            status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
            //	starterButton.setEnabled(false);
        } else if (SkStatus.SSH_CONNECTED.equals(state)) {
            showExpireDate();
            show_status("Connected");
            status_view.setText("Connected");
            status(true);
            if (config.getTunnelType() == ConfigUtil.MODE_PSIPHON || isSSHorOC()) {
                if (clicked) {
                    progress.setProgressWithAnimation((float) 100);
                } else {
                    progress.setProgress((float) 100);
                }
                progress.setColor(getResources().getColor(R.color.jx_stop_bg));
                if (isStart) {
                    //startService(new Intent(OpenVPNClient.this, TimerService.class));
                    showInterstitialAds();
                    runBanner();
                    loadBanners();
                    isStart = false;
                }
            }
        } else {

        }
        if (config.getTunnelType() != ConfigUtil.MODE_PSIPHON || isSSHorOC()) {
            if (SkStatus.getLastState().equals("NOPROCESS")) {
                show_status(SkStatus.getLocalizedState("DISCONNECTED"));

            } else {
                status.setText(SkStatus.getLocalizedState(SkStatus.getLastState()));
            }
            duration_view.setText(SocksDNSService.getTime());
        }

        enabledWidgets(!isRunning);
        //    starterButton.setText(resId);

    }

    private enum FinishOnConnect {
        DISABLED,
        ENABLED,
        ENABLED_ACROSS_ONSTART,
        PENDING
    }

    private enum ProfileSource {
        UNDEF,
        SERVICE,
        PRIORITY,
        PREFERENCES,
        SPINNER,
        LIST0
    }

    private static final int REQUEST_OFFLINE_UPDATE = 99;
    public static String USERNAME = "VPN_USERNAME";
    public static String PASSWORD = "VPN_PASSWORD";
    public static String SELECTED_PROFILE = "SELECTED_PROFILE";
    public static String SELECTED_NETWORK = "SELECTED_NETWORK";
    public static String SELECTED_NETWORK_INFO = "SELECTED_NETWORK_INFO";
    private SharedPreferences myPrefs;
    private SharedPreferences.Editor editor;

    private ConsentInformation consentInformation;
    private AppUpdateManager mAppUpdateManager;

    private static final int RC_APP_UPDATE = 100;

    public boolean isConnected = false;

    private ImageView flipmenu;
    private EditText myUser, myPass;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandlerUtils(this));
        Intent intent = getIntent();
        String str = TAG;
        Object[] objArr = new Object[S_BIND_CALLED];
        objArr[0] = intent.toString();
        Log.d(str, String.format("CLI: onCreate intent=%s", objArr));
        setContentView(R.layout.form);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic("all");

        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = myPrefs.edit();
        psiphonHelper = new TunnelServiceInteractor(this, true);
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(this));
        this.pwds = new PasswordUtil(PreferenceManager.getDefaultSharedPreferences(this));
        init_default_preferences(this.prefs);

        /*ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("9EEFCF678FA467729DB01A8BDA379297")
                .build();*/

        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                //.setConsentDebugSettings(debugSettings)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(FormError formError) {

                    }
                });

        myPrefs.edit().putBoolean("cr", consentInformation.canRequestAds()).apply();

        autoUpdate = false;
        showNoUpdate = false;

        RelativeLayout ad = findViewById(R.id.adView);
        mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId(myPrefs.getString("banner_ad", bannerID));
        ad.addView(mAdView);

        MobileAds.initialize(this);
        runBanner();
        runInterstitialAds(false);
        load_ui_elements();
        load();
        myBottomNav();
       // tweeksnetwork_create();
        setBottomLayout();
        doBindService();
        dobindInjector();
        bindService(new Intent(this, TimerService.class), timerConnection, Context.BIND_AUTO_CREATE);

        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo result) {
                if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    try {
                        mAppUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, OpenVPNClient.this,
                                RC_APP_UPDATE);

                    } catch (IntentSender.SendIntentException e) {
                    }
                }
            }
        });

        mAppUpdateManager.registerListener(installUpdatelistener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
            }
        }

        startVerifyingAccount();

    }

    private void loadForm() {
        UserMessagingPlatform.loadConsentForm(this, new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                    @Override
                    public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                        if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                            consentForm.show(
                                    OpenVPNClient.this,
                                    new ConsentForm.OnConsentFormDismissedListener() {
                                        @Override
                                        public void onConsentFormDismissed(@Nullable FormError formError) {
                                            myPrefs.edit().putBoolean("cr", consentInformation.canRequestAds()).apply();
                                            if (consentInformation.canRequestAds()) {
                                                runBanner();
                                                runInterstitialAds(false);
                                            }
                                        }
                                    });
                        }
                    }
                },
                new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                    @Override
                    public void onConsentFormLoadFailure(FormError formError) {
                    }
                });
    }

    private final InstallStateUpdatedListener installUpdatelistener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(InstallState state) {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showCompleterUpdate();
            }
        }
    };

    private void showCompleterUpdate() {
        Snackbar snacks = Snackbar.make(findViewById(android.R.id.content), "New app is ready!",
                Snackbar.LENGTH_INDEFINITE);
        snacks.setAction("Install", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAppUpdateManager.completeUpdate();
            }
        });
        snacks.setActionTextColor(Color.parseColor("#ffffff"));
        snacks.show();
    }

//    private void tweeksnetwork_create() {
//        ImageView network_create = findViewById(R.id.network_create);
//        network_create.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                customTweak();
//            }
//        });
//
//    }

    private void myBottomNav() {
        LinearLayout cleardata = findViewById(R.id.b_update);
        LinearLayout update = findViewById(R.id.b_tweaks);
        LinearLayout c_exit = findViewById(R.id.c_exit);

        c_exit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog dialog = new AlertDialog.Builder(OpenVPNClient.this, R.style.technore_dialog).create();
                View v = getLayoutInflater().inflate(R.layout.dialog_exit, null);
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.window);
                dialog.setView(v);
                v.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                    }

                });
                v.findViewById(R.id.mini).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                    }
                });
                v.findViewById(R.id.exit).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (android.os.Build.VERSION.SDK_INT >= 21) {
                            finishAndRemoveTask();
                        } else {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                        System.exit(0);
                    }
                });
                dialog.show();
            }
        });

        findViewById(R.id.a_whatsapp).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.whatsapp))));
            }
        });

        findViewById(R.id.a_logs).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(OpenVPNClient.this, LogActivity.class));
            }
        });

        findViewById(R.id.b_logout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                View view = LayoutInflater.from(OpenVPNClient.this).inflate(R.layout.layout_dialog, null);

                AlertDialog alertDialog = new AlertDialog.Builder(OpenVPNClient.this)
                        .setView(view)
                        .show();

                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                ((TextView) view.findViewById(R.id.title_text)).setText("Logout!");
                ((TextView) view.findViewById(R.id.desc_text)).setText("Are you sure do you want to logout?");
                ((TextView) view.findViewById(R.id.positive_text)).setText("Okay");
                view.findViewById(R.id.negative_button).setVisibility(View.VISIBLE);
                view.findViewById(R.id.padding4).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.negative_text)).setText("Cancel");
                ((LottieAnimationView) view.findViewById(R.id.main_animation)).setAnimation(R.raw.anim5);

                view.findViewById(R.id.close_icon).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });

                view.findViewById(R.id.positive_button).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        logout();
                    }
                });

                view.findViewById(R.id.negative_button).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });
            }
        });

        update.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showUpdateDialog();
            }
        });

        cleardata.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showClearDataDialog();
            }
        });
    }

    private void logout() {
        if (myPrefs.getBoolean("isLogin", false)) {
            editor.putBoolean("isLogin", false).apply();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        }
    }

    private void load() {
        showNoUpdate = false;
        config = ConfigUtil.getInstance(this);

        protocol = config.getProtocol();
        bannerImage = findViewById(R.id.bannerImage);

        myUser = findViewById(R.id.myusername);
        myPass = findViewById(R.id.mypassword);

        //mServerLayout = findViewById(R.id.server_layout);
        options_group = findViewById(R.id.options_group);
        timeLeft = findViewById(R.id.time_left);
        flipmenu = findViewById(R.id.btn_right_top);
        flipmenu.setOnClickListener(this);

        this.mSimpleDraweeView = findViewById(R.id.my_image_view);
        progress = findViewById(R.id.custom_progressBar);
        progress.setColor(getResources().getColor(R.color.ring_circle));

        con_status = findViewById(R.id.con_status);
        mNetworkLayout = findViewById(R.id.network_layout);

        mConn = new VPNConnector(this, true) {
            @Override
            public void onUpdate(final OpenVpnService service) {
                service.startActiveDialog(OpenVPNClient.this);
            }

            @Override
            public void showReconnectCount(String state) {
            }
        };

        listNetwork = new ArrayList<JSONObject>();
        network_spin = findViewById(R.id.network_spin);
        networkAdapter = new NetworkAdapter(this, listNetwork, false);
        network_spin.setAdapter(networkAdapter);
        network_spin.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    JSONObject js = listNetwork.get(position);
                    editor.putString(SELECTED_NETWORK, js.getString("Name")).apply();
                    editor.putInt("network_position", position).apply();
                    TextView tunnel_type = findViewById(R.id.tunnel_type);
                    switch (js.getInt("TunnelType")) {
                        case 0:
                            tunnel_type.setText("DIRECT");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 1:
                            tunnel_type.setText("INJECT");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 2:
                            tunnel_type.setText("INJECT");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 3:
                            tunnel_type.setText("SSL/TLS");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 4:
                            tunnel_type.setText("SSL INJECT");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 5:
                            tunnel_type.setText("SSL INJECT");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 6:
                            tunnel_type.setText("UDP");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 7:
                            tunnel_type.setText("V2RAY");
                            v2ray();
                            break;
                        case 8:
                            tunnel_type.setText("PSIPHON");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                        case 9:
                            tunnel_type.setText("SLOW DNS");
                            if (v2rayBroadCastReceiver != null) {
                                unregisterReceiver(v2rayBroadCastReceiver);
                                v2rayBroadCastReceiver = null;
                            }
                            break;
                    }
                    setResult(RESULT_OK);
                    setSelectedNetworkInfo();
                    loadServers();
                    profile_spin.setSelection(getServerSelected());
                } catch (Exception e) {
                    //showToast(e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        loadBanners();

        loadNetworks();
        if (myPrefs.getInt("network_position", 0) <= listNetwork.size()) {
            network_spin.setSelection(myPrefs.getInt("network_position", 0));
        }

        listProfiles = new ArrayList<JSONObject>();
        mServerAdapter = new ServerAdapter(this, listProfiles);
        profile_spin.setAdapter(mServerAdapter);
        loadServers();
        profile_spin.setSelection(getServerSelected());

        findViewById(R.id.check_update).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View p1) {
                showNoUpdate = true;
                autoUpdate = false;
                checkUpdates();
                // TODO: Implement this method
            }


        });

        findViewById(R.id.account_dialog).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View p1) {
                showAccountLogin();
                // TODO: Implement this method
            }


        });

        findViewById(R.id.network_layout).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View p1) {
                Intent intent = new Intent(getApplicationContext(), SelectNetworkActivity.class);
                startActivityForResult(intent, SelectNetworkActivity.SELECT_NETWORK_CODE);
                // TODO: Implement this method
            }


        });

        network_spin.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {

                    View view = LayoutInflater.from(OpenVPNClient.this).inflate(R.layout.dialog_networks, null);

                    ListView listView = view.findViewById(R.id.listItems);
                    listView.setAdapter(new NetworkAdapter(OpenVPNClient.this, listNetwork, true));

                    SearchView searchView = view.findViewById(R.id.search);
                    searchView.setQueryHint("Search Here");
                    searchView.setIconifiedByDefault(false);

                    arrayList = new ArrayList<JSONObject>();

                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String str) {
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String str) {
                            arrayList = new ArrayList<JSONObject>();
                            for (JSONObject item : listNetwork) {
                                if (item.optString("Name", "").toLowerCase().contains(str.toLowerCase())) {
                                    arrayList.add(item);
                                }
                            }
                            listView.setAdapter(new NetworkAdapter(OpenVPNClient.this, arrayList, true));
                            return false;
                        }
                    });

                    AlertDialog alertDialog = new AlertDialog.Builder(OpenVPNClient.this, R.style.technore_dialog)
                            .setView(view)
                            .setPositiveButton("Close", null)
                            .show();

                    listView.setSelection(network_spin.getSelectedItemPosition());

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (arrayList.size() > 0) {
                                for (int s = 0; s < listNetwork.size(); s++) {
                                    if (listNetwork.get(s).equals(arrayList.get(i))) {
                                        network_spin.setSelection(s);
                                    }
                                }
                            } else {
                                network_spin.setSelection(i);
                            }
                            alertDialog.dismiss();
                        }
                    });
                }
                return true;
            }
        });

        Switch switchButton = findViewById(R.id.custom);
        switchButton.setChecked(myPrefs.getBoolean("is_custom", false));
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                myPrefs.edit().putBoolean("is_custom", isChecked).commit();
                loadNetworks();
                /*if (myPrefs.getInt("network_position",0) <= listNetwork.size()) {
                    network_spin.setSelection(myPrefs.getInt("network_position",0));
                } else if (!listNetwork.isEmpty()) {
                    network_spin.setSelection(0);
                }*/
                network_spin.setSelection(0);
                loadServers();
                profile_spin.setSelection(getServerSelected());
                JSONObject js = listNetwork.get(network_spin.getSelectedItemPosition());
                editor.putString(SELECTED_NETWORK, js.optString("Name", "")).apply();
            }
        });

        findViewById(R.id.custom_add_tweaks).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!switchButton.isChecked()) {
                    Toast.makeText(OpenVPNClient.this, "Please enable custom tweak first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                showCustomTweakDialog(false);
            }
        });

        findViewById(R.id.custom_edit_tweaks).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!switchButton.isChecked()) {
                    Toast.makeText(OpenVPNClient.this, "Please enable custom tweak first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (listNetwork.size() > 0) {
                    showCustomTweakDialog(true);
                }
            }
        });

        findViewById(R.id.custom_delete_tweaks).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!switchButton.isChecked()) {
                    Toast.makeText(OpenVPNClient.this, "Please enable custom tweak first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (listNetwork.size() == 1) {
                    Toast.makeText(OpenVPNClient.this, "Cannot be empty the custom tweaks list!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (listNetwork.size() > 0) {
                    new AlertDialog.Builder(OpenVPNClient.this).setMessage("Are you sure do you want to delete this tweak!")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        int pos = network_spin.getSelectedItemPosition();
                                        JSONArray array = getCustomTweaks();
                                        array.remove(pos);
                                        setCustomTweaks(array);
                                        loadNetworks();
                                        network_spin.setSelection(pos - 1);
                                    } catch (Exception e) {
                                    }
                                    showToast("Delete Tweak Successfully!");
                                }
                            }).setNegativeButton("No", null)
                            .show();
                }
            }
        });

        if (myPrefs.getBoolean("is_show_custom", false)) {
            options_group.setVisibility(View.VISIBLE);
        } else {
            options_group.setVisibility(View.GONE);
        }

        myUser.setText(myPrefs.getString(USERNAME, ""));
        myPass.setText(myPrefs.getString(PASSWORD, ""));

        showExpireDate();

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.a_update) {
                            Builder builder3 = new Builder(OpenVPNClient.this, R.style.technore_dialog);
                            builder3.setCancelable(true);
                            builder3.setIcon(R.drawable.ic_launcher);
                            builder3.setTitle("Config Updater");
                            builder3.setMessage("1.) Online Update - requires internet connection.\n\n2.) Clear Data - Clearing all settings.");
                            builder3.setPositiveButton("Online", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface p1, int p2) {
                                    showNoUpdate = true;
                                    checkUpdates();
                                }
                            });
                            builder3.setNeutralButton("Clear Data", new DialogInterface.OnClickListener() {
                                @SuppressLint("WrongConstant")
                                @Override
                                public void onClick(DialogInterface p1, int p2) {
                                    clearAppData();
                                }
                            });
                            builder3.show();

                            return true;
                        } else if (itemId == R.id.a_tweaks) {
                            customTweak();
                            intentTele();
                            return true;
                        } else if (itemId == R.id.a_exit) {
                            Builder builder = new Builder(OpenVPNClient.this, R.style.technore_dialog);
                            builder.setCancelable(true);
                            builder.setMessage("Do you want to minimize or exit?");
                            builder.setPositiveButton("Exit", new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (Build.VERSION.SDK_INT >= 21) {
                                                finishAndRemoveTask();
                                            } else {
                                                Process.killProcess(Process.myPid());
                                            }
                                            System.exit(0);
                                        }
                                    });
                            builder.setNeutralButton("Minimize", new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                                            startMain.addCategory(Intent.CATEGORY_HOME);
                                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(startMain);
                                        }
                                    });
                            builder.show();

                            return true;
                        }
                        return false;
                    }
                });


        if (SkStatus.isTunnelActive() && SkStatus.getLastState().equals(SkStatus.SSH_CONNECTED)) {
            addStatus("UDP is running");
        } else if (SkStatus.isTunnelActive()) {
            addStatus("Waiting for UDP response");
        }

        //mRememberMe.setOnCheckedChangeListener(OnCheckedChanged());

        findViewById(R.id.add_time).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                View view = getLayoutInflater().inflate(R.layout.layout_add_time_dialog, null);

                AlertDialog alertDialog = new AlertDialog.Builder(OpenVPNClient.this, R.style.technore_dialog)
                        .setView(view)
                        .show();

                view.findViewById(R.id.watch).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        alertDialog.dismiss();

                        if (!consentInformation.canRequestAds()) {
                            Toast.makeText(OpenVPNClient.this, "Failed to load ad.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final ProgressDialog pd = new ProgressDialog(OpenVPNClient.this);
                        pd.setMessage("Loading...");
                        pd.setCancelable(false);
                        pd.show();

                        AdRequest adRequest = new AdRequest.Builder().build();
                        RewardedAd.load(OpenVPNClient.this, myPrefs.getString("rewarded_ad", rewardedAdsID),
                                adRequest, new RewardedAdLoadCallback() {
                                    @Override
                                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                        pd.dismiss();
                                        Toast.makeText(OpenVPNClient.this, "Please try again later", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                                        pd.dismiss();
                                        rewardedAd.show(OpenVPNClient.this, new OnUserEarnedRewardListener() {
                                            @Override
                                            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                                timerService.add(TimeUnit.HOURS.toMillis(3));
                                                Toast.makeText(OpenVPNClient.this, "3 hours added", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                });
                    }
                });

            }
        });

        //Utils.checkSign(this);
        AppRemote ar = new AppRemote(this);
        ar.setListener(new AppRemote.OnFinishListener() {

            @Override
            public void onFinish(boolean isDestroy, String message) {
                if (isDestroy) {
                    showDialog(message);
                }
                // TODO: Implement this method
            }


        });
        try {
            ar.start();
        } catch (Exception e) {

        }
        setSelectedNetworkInfo();

        /*AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.technore_dialog);
        builder.setTitle("Join Us On Telegram!!");
        builder.setMessage("We have a Telegram support channel where we post and discuss about Settings, new Features, and also assist our Users.\n" + "\n" + "Would you like to join us there?");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("Join Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.telegram))));
            }
        });
        builder.setNeutralButton("Cancel", null);
        builder.show();*/

        if (ConfigUtil.isV2RAY() && V2rayController.getConnectionState() == AppConfigs.V2RAY_STATES.V2RAY_CONNECTED) {
            status_view.setText("Connected");
            status_view.setTextColor(getResources().getColor(R.color.connectedcolor));
            progress.setProgress((float) 100);
            progress.setColor(getResources().getColor(R.color.jx_stop_bg));
            disconnect_button.setTextColor(getResources().getColor(R.color.jx_stop_bg));
            OpenVPNClient.this.status_01.setTextColor(getResources().getColor(R.color.jx_appName));
            OpenVPNClient.this.status_01.setText("Close");
            OpenVPNClient.this.isConnected = true;
            enabledWidgets(false);
            status(true);
            showExpireDate();
        }
    }

    private void v2ray() {

        v2rayBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (Objects.requireNonNull(intent.getExtras().getSerializable("STATE")).toString()) {
                    case "V2RAY_CONNECTED":
                        status_view.setText("Connected " + intent.getExtras().getLong("DELAY") + "ms");
                        status_view.setTextColor(getResources().getColor(R.color.connectedcolor));
                        progress.setProgressWithAnimation((float) 100);
                        progress.setColor(getResources().getColor(R.color.jx_stop_bg));
                        duration_view.setText(Objects.requireNonNull(intent.getExtras()).getString("DURATION"));
                        bytes_in_view.setText(intent.getExtras().getString("DOWNLOAD_TRAFFIC"));
                        bytes_out_view.setText(intent.getExtras().getString("UPLOAD_TRAFFIC"));
                        disconnect_button.setTextColor(getResources().getColor(R.color.jx_stop_bg));
                        OpenVPNClient.this.status_01.setTextColor(getResources().getColor(R.color.jx_appName));
                        OpenVPNClient.this.status_01.setText("Close");
                        OpenVPNClient.this.isConnected = true;
                        enabledWidgets(false);
                        status(true);
                        if (isStart) {
                            //startService(new Intent(OpenVPNClient.this, TimerService.class));
                            showInterstitialAds();
                            runBanner();
                            showExpireDate();
                            SkStatus.logInfo("<b>Connected</b>");
                            isStart = false;
                        }
                        break;
                    case "V2RAY_DISCONNECTED":
                        status_view.setText("Disconnected");
                        status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
                        progress.setProgressWithAnimation((float) 0);
                        progress.setColor(getResources().getColor(R.color.ring_circle));
                        disconnect_button.setTextColor(getResources().getColor(R.color.jx_start_bg));
                        duration_view.setText("00:00:00");
                        bytes_in_view.setText("0KB");
                        bytes_out_view.setText("0KB");
                        OpenVPNClient.this.status_01.setTextColor(getResources().getColor(R.color.jx_stop_bg));
                        OpenVPNClient.this.status_01.setText("Connect");
                        OpenVPNClient.this.isConnected = false;
                        enabledWidgets(true);
                        break;
                    case "V2RAY_CONNECTING":
                        status_view.setText("Authenticating");
                        progress.setProgressWithAnimation((float) 50);
                        status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
                        OpenVPNClient.this.status_01.setTextColor(getResources().getColor(R.color.jx_start_bg));
                        OpenVPNClient.this.status_01.setText("Connecting");
                        OpenVPNClient.this.isConnected = false;
                        break;
                    default:
                        break;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(getPackageName() + ".V2RAY_CONNECTION_INFO"), RECEIVER_EXPORTED);
        } else {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(getPackageName() + ".V2RAY_CONNECTION_INFO"));
        }
    }

    private void intentTele() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://chat.whatsapp.com/+88088841"));
        startActivity(intent);
    }

    private void clearAppData() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.technore_dialog);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Clear Setting/data");
        builder.setMessage("Are you sure you want to reset all the data?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                try {
                    // clearing app data
                    if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData(); // note: it has a return value!
                    } else {
                        String packageName = getApplicationContext().getPackageName();
                        Runtime runtime = Runtime.getRuntime();
                        runtime.exec("pm clear " + packageName);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    public void loadNetworks() {
        try {
            if (listNetwork.size() > 0) {
                listNetwork.clear();
            }

            //   JSONObject js = new JSONObject();
            //   js.put("Name", "UDP Direct");
            //  js.put("Info", "Direct UDP Connection");
            //    js.put("TunnelType", ConfigUtil.MODE_OVPN_DIRECT_UDP);
            //   listNetwork.add(js);

            if (myPrefs.getBoolean("is_custom", false)) {
                JSONArray network = getCustomTweaks();
                for (int i = 0; i < network.length(); i++) {
                    listNetwork.add(network.getJSONObject(i));
                }
            } else {
                JSONArray network = getNetworksArray();
                for (int i = 0; i < network.length(); i++) {
                    listNetwork.add(network.getJSONObject(i));
                }
                JSONArray sslnetwork = getSSLNetworks();
                for (int i = 0; i < sslnetwork.length(); i++) {
                    listNetwork.add(sslnetwork.getJSONObject(i));
                }
            }
            Collections.sort(listNetwork, NetworkNameComparator());
            networkAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            showToast(e.getMessage());
        }
        // TODO: Implement this method
    }

    private void setSelectedNetworkInfo() {
        try {
            TextView network_name = findViewById(R.id.network_title);
            TextView network_info = findViewById(R.id.network_info);
            ImageView iv = findViewById(R.id.network_icon);

            JSONObject js = getNetworkSelectedJson();
            network_name.setText(js.getString("Name"));
            String info = js.getString("Info");
            if (info.isEmpty()) {
                network_info.setText(info);
                network_info.setVisibility(View.VISIBLE);
            } else {
                network_info.setVisibility(View.GONE);
            }
            String name = js.getString("Name").toLowerCase();
            //tunnel_title.setText(js.getInt("TunnelType") == 0 ? "SSH/INJECT" : "SSH/SSL");
            setIcon(iv, getIcon(name));
        } catch (Exception e) {

        }
        // TODO: Implement this method
    }

    private int getIcon(String name) {
        if (name.contains("gtm")) {
            return (R.drawable.ic_globe);
        } else if (name.contains("omantel")) {
            return (R.drawable.ic_omantel);
        } else if (name.contains("gp")) {
            return (R.drawable.gphone);
        } else if (name.contains("lebara")) {
            return (R.drawable.ic_lebara);
        } else if (name.contains("tnt")) {
            return (R.drawable.ic_tnt);
        } else if (name.contains("vargin")) {
            return (R.drawable.ic_vargin);
        } else if (name.contains("facebook")) {
            return (R.drawable.ic_facebook);
        } else if (name.contains("google")) {
            return (R.drawable.ic_google);
        } else if (name.contains("youtube")) {
            return (R.drawable.ic_youtube);
        } else if (name.contains("instagram")) {
            return (R.drawable.ic_instagram);
        } else if (name.contains("iflix")) {
            return (R.drawable.ic_iflix);
        } else if (name.contains("snapchat")) {
            return (R.drawable.ic_snapchat);
        } else if (name.contains("twitter")) {
            return (R.drawable.ic_twitter);
        } else if (name.contains("neflix")) {
            return (R.drawable.ic_netflix);
        } else if (name.contains("mobile legends")) {
            return (R.drawable.ic_ml);
        } else if (name.contains("du")) {
            return (R.drawable.ic_du);
        } else if (name.contains("etisalat")) {
            return (R.drawable.ic_eti);
        } else if (name.contains("wifi")) {
            return (R.drawable.ic_wifi);
        } else if (name.contains("whatsapp")) {
            return (R.drawable.ic_whatsapp);
        } else if (name.contains("tiktok")) {
            return (R.drawable.ic_tiktok);
        } else if (name.contains("viber")) {
            return (R.drawable.ic_viber);
        } else if (name.contains("airtel")) {
            return (R.drawable.ic_airtel);
        } else if (name.contains("jawwy")) {
            return (R.drawable.ic_jawwy);
        } else if (name.contains("digi")) {
            return (R.drawable.ic_digi);
        } else if (name.contains("airtel")) {
            return (R.drawable.ic_airtel);
        } else if (name.contains("pubg")) {
            return (R.drawable.ic_pubg);
        } else if (name.contains("playstore")) {
            return (R.drawable.ic_playstore);
        } else if (name.contains("skype")) {
            return (R.drawable.ic_skype);
        } else if (name.contains("telegram")) {
            return (R.drawable.ic_telegram);
        } else if (name.contains("vivobee")) {
            return (R.drawable.ic_vivobee);
        } else if (name.contains("ooredoo")) {
            if (!name.contains("free")) {
                return (R.drawable.ic_ooredoo);
            }
            return (R.drawable.ic_ooredoo_free);
        } else if (name.contains("viva")) {
            return (R.drawable.ic_viva);
        } else if (name.contains("progresif")) {
            return (R.drawable.ic_progresif);
        } else if (name.contains("jio")) {
            return (R.drawable.ic_jio);
        } else if (name.contains("flexi")) {
            return (R.drawable.ic_flexi);
        } else if (name.contains("vodaphone")) {
            return (R.drawable.ic_vodafone);
        } else if (name.contains("mobily")) {
            if (name.contains("free")) {
                return (R.drawable.ic_mobily_free);
            }
            return (R.drawable.ic_mobily);
        } else if (name.contains("zain")) {
            if (name.contains("free")) {
                return (R.drawable.ic_zain_free);
            }
            return (R.drawable.ic_zain);
        } else if (name.contains("banglalink")) {
            return (R.drawable.ic_banglalink);
        } else if (name.contains("dhiraagu")) {
            return (R.drawable.ic_dhiraagu);
        } else if (name.contains("dst")) {
            return (R.drawable.ic_dst);
        } else if (name.contains("friendi")) {
            return (R.drawable.ic_friendi);
        } else if (name.contains("grameen")) {
            return (R.drawable.ic_grameenphone);
        } else if (name.contains("imagine")) {
            return (R.drawable.ic_imagine);
        } else if (name.contains("kuwait zain")) {
            return (R.drawable.ic_kuwait_zain);
        } else if (name.contains("lebera")) {
            return (R.drawable.ic_lebara);
        } else if (name.contains("omantel")) {
            return (R.drawable.ic_omantel);
        } else if (name.contains("progresif")) {
            return (R.drawable.ic_progresif);
        } else if (name.contains("vodafone")) {
            return (R.drawable.ic_vodafone);
        } else if (name.contains("robi")) {
            return (R.drawable.ic_robi);
        } else if (name.contains("v2ray")) {
            return (R.drawable.ic_v2ray);
        } else if (name.contains("mci")) {
            return (R.drawable.ic_mci);
        } else if (name.contains("irancell")) {
            return (R.drawable.ic_irancell);
        } else if (name.contains("rightel")) {
            return (R.drawable.ic_rightel);
        } else if (name.contains("ssh")) {
            return (R.drawable.icon_ssh);
        } else if (name.contains("ovpn")) {
            return (R.drawable.icon_openvpn);
        } else if (name.contains("shatel")) {
            return (R.drawable.ic_shatel);
        } else if (name.contains("singtel")) {
            return (R.drawable.ic_singtel);
        } else if (name.contains("stc")) {
            if (name.contains("free")) {
                return (R.drawable.ic_stc_free);
            }
            return (R.drawable.ic_stc);
        } else if (name.contains("vargin")) {
            return (R.drawable.ic_vargin);
        } else if (name.contains("starhub")) {
            return (R.drawable.starhub);
        } else {
            return (R.drawable.ic_launcher);
        }
        // TODO: Implement this method
        //return 0;
    }

    public void setIcon(ImageView iv, int icon) {
        iv.setImageResource(icon);
    }

    private String getConfigVersion() {
        String version = "Config Version: %s";
        // TODO: Implement this method
        try {
            return getJSONObject().getString("Version");
        } catch (JSONException e) {
        }
        return "1.0";
    }

    private CompoundButton.OnCheckedChangeListener OnCheckedChanged() {
        // TODO: Implement this method
        return new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                editor.putBoolean("RememberMe", p2).apply();
                // TODO: Implement this method
            }


        };
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem p1) {
        /*switch (p1.getItemId()) {
            case R.id.menu_offline_update:
                showOfflineUpdate();
                break;
            case R.id.menu_clear_data:
                showClearDataDialog();
                break;
            case R.id.menu_add_tweak:
                showCustomTweakDialog();
                break;

        }*/
        // TODO: Implement this method
        return true;
    }


    void showUpdateDialog() {
        DialogInterface.OnClickListener DialogListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface p1, int p2) {
                switch (p2) {
                    case DialogInterface.BUTTON_NEUTRAL:
                        showClearDataDialog();
                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        showNoUpdate = true;
                        autoUpdate = false;
                        checkUpdates();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        showOfflineUpdate();
                        break;
                }
                // TODO: Implement this method
            }


        };

        AlertDialog dialog = new AlertDialog.Builder(OpenVPNClient.this, R.style.technore_dialog).create();
        dialog.setTitle("Config Updater");
        dialog.setMessage("1.) Online Update - Requires internet connection.\n\n2.) Offline Update - Needs to import .rj or .js file.\n\n3.) Clear Data - Clear all saved configs.");
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Clear Data", DialogListener);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Online", DialogListener);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Offline", DialogListener);
        dialog.show();

    }

    private void showAccountLogin() {
        View v = getLayoutInflater().inflate(R.layout.login_activity, null);
        final EditText mUsername = v.findViewById(R.id.login_username);
        final EditText mPassword = v.findViewById(R.id.login_password);
        Button mLoginBtn = v.findViewById(R.id.login_button);

        mUsername.setText(prefs.get_string(USERNAME));
        mPassword.setText(prefs.get_string(PASSWORD));

        final AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setView(v);

        mLoginBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View p1) {
                String user = mUsername.getText().toString();
                String pass = mPassword.getText().toString();
                if (user.isEmpty() || pass.isEmpty()) {
                    showToast("Username or Password is empty");
                    return;
                }
                prefs.set_string(USERNAME, user);
                prefs.set_string(PASSWORD, pass);
                builder.dismiss();
                // TODO: Implement this method
            }


        });
        builder.show();
        // TODO: Implement this method
    }

    private void showOfflineUpdate() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(OpenVPNClient.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_OFFLINE_UPDATE);
        } else {

        }
    }

    private void showCustomTweakDialog(boolean isEdit) {

        View v = getLayoutInflater().inflate(R.layout.custom_tweak_dialog, null);
        final Spinner mTweakMode = v.findViewById(R.id.custom_tweak_mode);
        final TextInputLayout mCustomTweakEdit = v.findViewById(R.id.payload);
        final TextInputLayout name = v.findViewById(R.id.name);
        final TextInputLayout info = v.findViewById(R.id.info);
        final TextInputLayout front_query = v.findViewById(R.id.front_query);
        final TextInputLayout back_query = v.findViewById(R.id.back_query);
        final TextInputLayout mCustomSNI = v.findViewById(R.id.custom_sni_edit);
        final TextInputLayout mCustomTweakProxy = v.findViewById(R.id.custom_tweak_proxy);
        final TextInputLayout mCustomTweakProxyPort = v.findViewById(R.id.custom_tweak_proxy_port);
        final Switch mUseDefProxy = v.findViewById(R.id.custom_tweak_default_proxy);
        final ImageView mCustomTweakGenerate = v.findViewById(R.id.generator);
        final LinearLayout mSSLLayout = v.findViewById(R.id.ssl_layout);
        final LinearLayout mInjectLayout = v.findViewById(R.id.inject_layout);
        final LinearLayout mProxyLayout = v.findViewById(R.id.custom_tweak_proxy_layout);
        final RadioGroup protoGroup = v.findViewById(R.id.proto_group);
        final RadioGroup serverGroup = v.findViewById(R.id.server_group);

        String[] tweaks = {"Direct Connection", "Direct SSL/TLS", "HTTP Proxy", "SSL/TLS Payload", "SSL/TLS Proxy + Payload"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, tweaks);
        mTweakMode.setAdapter(adapter);
        mTweakMode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4) {
                if (protoGroup.getCheckedRadioButtonId() != R.id.ovpn) {
                    return;
                }
                if (p3 == 0) {
                    mInjectLayout.setVisibility(View.VISIBLE);
                    mProxyLayout.setVisibility(View.VISIBLE);
                    mSSLLayout.setVisibility(View.GONE);
                } else if (p3 == 1) {
                    mInjectLayout.setVisibility(View.GONE);
                    mProxyLayout.setVisibility(View.GONE);
                    mSSLLayout.setVisibility(View.VISIBLE);
                } else if (p3 == 2) {
                    mInjectLayout.setVisibility(View.VISIBLE);
                    mProxyLayout.setVisibility(View.VISIBLE);
                    mSSLLayout.setVisibility(View.GONE);
                } else if (p3 == 3) {
                    mInjectLayout.setVisibility(View.VISIBLE);
                    mProxyLayout.setVisibility(View.VISIBLE);
                    mSSLLayout.setVisibility(View.VISIBLE);
                } else if (p3 == 4) {
                    mInjectLayout.setVisibility(View.VISIBLE);
                    mProxyLayout.setVisibility(View.VISIBLE);
                    mSSLLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> p1) {
            }
        });

        mTweakMode.setSelection(0);
        mUseDefProxy.setChecked(true);

        protoGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.ovpn) {
                    mCustomTweakEdit.setHint("Payload");
                    mTweakMode.setVisibility(View.VISIBLE);
                    serverGroup.setVisibility(View.VISIBLE);
                    front_query.setVisibility(View.VISIBLE);
                    back_query.setVisibility(View.VISIBLE);
                    mInjectLayout.setVisibility(View.VISIBLE);
                    mCustomTweakGenerate.setVisibility(View.VISIBLE);
                    int pos = mTweakMode.getSelectedItemPosition();
                    if (pos == 0) {
                        mInjectLayout.setVisibility(View.VISIBLE);
                        mProxyLayout.setVisibility(View.VISIBLE);
                        mSSLLayout.setVisibility(View.GONE);
                    } else if (pos == 1) {
                        mInjectLayout.setVisibility(View.GONE);
                        mProxyLayout.setVisibility(View.GONE);
                        mSSLLayout.setVisibility(View.VISIBLE);
                    } else if (pos == 2) {
                        mInjectLayout.setVisibility(View.VISIBLE);
                        mProxyLayout.setVisibility(View.VISIBLE);
                        mSSLLayout.setVisibility(View.GONE);
                    } else if (pos == 3) {
                        mInjectLayout.setVisibility(View.VISIBLE);
                        mProxyLayout.setVisibility(View.VISIBLE);
                        mSSLLayout.setVisibility(View.VISIBLE);
                    } else if (pos == 4) {
                        mInjectLayout.setVisibility(View.VISIBLE);
                        mProxyLayout.setVisibility(View.VISIBLE);
                        mSSLLayout.setVisibility(View.VISIBLE);
                    }
                } else if (i == R.id.udp) {
                    mTweakMode.setVisibility(View.GONE);
                    serverGroup.setVisibility(View.GONE);
                    front_query.setVisibility(View.GONE);
                    back_query.setVisibility(View.GONE);
                    mInjectLayout.setVisibility(View.GONE);
                    mProxyLayout.setVisibility(View.GONE);
                    mSSLLayout.setVisibility(View.GONE);
                } else if (i == R.id.v2ray) {
                    mCustomTweakEdit.setHint("V2Ray Config");
                    mTweakMode.setVisibility(View.GONE);
                    serverGroup.setVisibility(View.GONE);
                    front_query.setVisibility(View.GONE);
                    back_query.setVisibility(View.GONE);
                    mInjectLayout.setVisibility(View.GONE);
                    mCustomTweakGenerate.setVisibility(View.GONE);
                    mInjectLayout.setVisibility(View.VISIBLE);
                    mProxyLayout.setVisibility(View.GONE);
                    mSSLLayout.setVisibility(View.GONE);
                }
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.technore_dialog)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("Name", name.getEditText().getText().toString());
                            obj.put("Info", info.getEditText().getText().toString());
                            obj.put("Payload", encrypt(mCustomTweakEdit.getEditText().getText().toString()));
                            obj.put("SNIHost", encrypt(mCustomSNI.getEditText().getText().toString()));
                            obj.put("ServerHostDNS", serverGroup.getCheckedRadioButtonId() == R.id.cf ? "cf" : (serverGroup.getCheckedRadioButtonId() == R.id.ws ? "ws" : "http"));
                            obj.put("FrontQuery", front_query.getEditText().getText().toString());
                            obj.put("BackQuery", back_query.getEditText().getText().toString());
                            obj.put("isCustomConfig", protoGroup.getCheckedRadioButtonId() == R.id.v2ray);

                            if (protoGroup.getCheckedRadioButtonId() == R.id.udp) {
                                obj.put("TunnelType", ConfigUtil.MODE_UDP);
                            } else if (protoGroup.getCheckedRadioButtonId() == R.id.v2ray) {
                                obj.put("TunnelType", ConfigUtil.MODE_V2RAY);
                            } else if (mTweakMode.getSelectedItemPosition() == 0) {
                                obj.put("TunnelType", 0);
                            } else if (mTweakMode.getSelectedItemPosition() == 1) {
                                obj.put("TunnelType", 3);
                            } else if (mTweakMode.getSelectedItemPosition() == 2) {
                                obj.put("TunnelType", 2);
                            } else if (mTweakMode.getSelectedItemPosition() == 3) {
                                obj.put("TunnelType", 4);
                            } else if (mTweakMode.getSelectedItemPosition() == 4) {
                                obj.put("TunnelType", 5);
                            }

                            JSONObject proxySettings = new JSONObject();
                            if (mUseDefProxy.isChecked()) {
                                proxySettings.put("Squid", encrypt("[Default]"));
                                proxySettings.put("Port", "80");
                            } else {
                                proxySettings.put("Squid", encrypt(mCustomTweakProxy.getEditText().getText().toString()));
                                proxySettings.put("Port", mCustomTweakProxyPort.getEditText().getText().toString());
                            }
                            obj.put("ProxySettings", proxySettings);

                            if (isEdit) {
                                setCustomTweaks(getCustomTweaks().put(network_spin.getSelectedItemPosition(), obj));
                                showToast("Edited Tweak Successfully!");
                            } else {
                                setCustomTweaks(getCustomTweaks().put(obj));
                                showToast("Added Tweak Successfully!");
                            }

                            loadNetworks();
                            if (!isEdit) {
                                network_spin.setSelection(listNetwork.size() - 1);
                            }

                        } catch (Exception e) {
                            showToast(e.getMessage());
                        }
                    }
                }).setNegativeButton("Cancel", null).create();
        dialog.setTitle(isEdit ? "Edit Tweak" : "Custom Tweaks");
        dialog.setIcon(R.drawable.ic_launcher);
        dialog.setView(v);

        if (isEdit) {
            try {
                JSONObject obj = getCustomTweaks().getJSONObject(network_spin.getSelectedItemPosition());
                if (obj.getInt("TunnelType") == 0) {
                    mTweakMode.setSelection(0);
                } else if (obj.getInt("TunnelType") == 2) {
                    mTweakMode.setSelection(2);
                } else if (obj.getInt("TunnelType") == 3) {
                    mTweakMode.setSelection(1);
                } else if (obj.getInt("TunnelType") == 4) {
                    mTweakMode.setSelection(3);
                } else if (obj.getInt("TunnelType") == 5) {
                    mTweakMode.setSelection(4);
                }
                if (obj.has("ServerHostDNS")) {
                    String string = obj.getString("ServerHostDNS");
                    if (string.equals("ws")) {
                        serverGroup.check(R.id.ws);
                    }
                    if (string.equals("cf")) {
                        serverGroup.check(R.id.cf);
                    }
                    if (string.equals("http")) {
                        serverGroup.check(R.id.http);
                    }
                }
                protoGroup.check(obj.getInt("TunnelType") == ConfigUtil.MODE_UDP ? R.id.udp : (obj.getInt("TunnelType") == ConfigUtil.MODE_V2RAY ? R.id.v2ray : R.id.ovpn));
                name.getEditText().setText(obj.getString("Name"));
                info.getEditText().setText(obj.getString("Info"));
                front_query.getEditText().setText(obj.optString("FrontQuery", ""));
                back_query.getEditText().setText(obj.optString("BackQuery", ""));
                mCustomTweakEdit.getEditText().setText(decrypt(obj.getString("Payload")));
                mCustomSNI.getEditText().setText(decrypt(obj.getString("SNIHost")));
                String proxy = decrypt(obj.getJSONObject("ProxySettings").getString("Squid"));
                if (proxy.isEmpty()) {
                    proxy = "[Default]";
                }
                mCustomTweakProxy.getEditText().setText(proxy);
                mCustomTweakProxyPort.getEditText().setText(obj.getJSONObject("ProxySettings").getString("Port"));
                if (!proxy.equals("[Default]")) {
                    mUseDefProxy.setChecked(false);
                    mCustomTweakProxy.setEnabled(true);
                    mCustomTweakProxyPort.setEnabled(true);
                }
            } catch (Exception e) {
            }
        }

        mUseDefProxy.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton p1, boolean isChecked) {
                mCustomTweakProxy.getEditText().setText(isChecked ? "[Default]" : "");
                mCustomTweakProxy.setEnabled(!isChecked);
                mCustomTweakProxyPort.setEnabled(!isChecked);
            }
        });

        mCustomTweakGenerate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View p1) {
                GeneratorHelper gh = new GeneratorHelper(OpenVPNClient.this);
                gh.setCancelListener(new GeneratorHelper.GeneratorListener() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onGenerate(String payload) {
                        mCustomTweakEdit.getEditText().setText(payload);
                    }
                });
                gh.show();
            }
        });

        dialog.show();
    }


    @Override
    public void onCancel() {
        // mCustomTweakSw.setChecked(false);
        // TODO: Implement this method
    }

    @Override
    public void onGenerate(String payload) {
        if (mCustomTweakEdit == null) {
            return;
        }
        mCustomTweakEdit.setText(payload);
        // TODO: Implement this method
    }


    @Override
    public JSONArray getNetworksArray() {
        // TODO: Implement this method
        return super.getNetworksArray();
    }

    @Override
    public JSONArray getSSLNetworks() {
        // TODO: Implement this method
        return super.getSSLNetworks();
    }

    private int getServerSelected() {
        try {
            for (int i = 0; i < listProfiles.size(); i++) {
                if (myPrefs.getString(SELECTED_PROFILE, "").equals(listProfiles.get(i))) {
                    return i;
                }
            }
        } catch (Exception e) {

        }
        // TODO: Implement this method
        return 0;
    }

    @Override
    public void onCheckedChanged(RadioGroup p1, int p2) {

        // TODO: Implement this method
    }

    private void enableNetworks(boolean p0) {
        mNetworkLayout.setEnabled(p0);
        // TODO: Implement this method
    }

    protected void showDialog(String msg) {
        new AlertDialog.Builder(this).setTitle("Attention").
                setMessage(msg).
                setCancelable(false).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface p1, int p2) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                        finish();
                        // TODO: Implement this method
                    }


                }).show();
    }

    private void customTweak() {
        final Dialog dialog = new Dialog(OpenVPNClient.this, R.style.technore_dialog);
        dialog.setContentView(R.layout.dialog_tweaks);

        dialog.findViewById(R.id.unlock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPrefs.edit().putBoolean("is_show_custom", true).commit();
                options_group.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.lock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myPrefs.edit().putBoolean("is_show_custom", false).commit();
                options_group.setVisibility(View.GONE);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private Comparator<JSONObject> NetworkNameComparator() {
        // TODO: Implement this method
        return new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject p1, JSONObject p2) {
                // TODO: Implement this method
                try {
                    return String.valueOf(p2.getInt("Priority")).compareTo(String.valueOf(p1.getInt("Priority")));
                } catch (JSONException e) {
                }
                return 0;
            }
        };
    }

    public void loadServers() {
        try {
            for (File f : getFilesDir().listFiles()) {
                if (f.getAbsolutePath().toLowerCase().endsWith(".ovpn")) {
                    f.delete();
                }
            }
            if (listProfiles.size() > 0) {
                listProfiles.clear();
            }
            servers = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Name", "Auto Select Server");
            jsonObject.put("Flag", "AA.png");
            jsonObject.put("ServerIPHost", "");
            jsonObject.put("ServerCloudFrontHost", "");
            jsonObject.put("ServerHTTPHost", "");
            jsonObject.put("Category", "Random");
            jsonObject.put("OpenVPNTCPPort", "");
            jsonObject.put("OpenVPNSSLPort", "");
            PasswordCache.init(this);
            listProfiles.add(jsonObject);
            JSONArray serversArray = getServersArray();
            JSONObject obj = ((JSONObject) network_spin.getSelectedItem());
            int tunnelType = obj.getInt("TunnelType");
            int proto = obj.optInt("Protocol", 0);
            for (int i = 0; i < serversArray.length(); i++) {
                JSONObject server = serversArray.getJSONObject(i);
                String server_name = server.getString("Name");
                String server_ip = server.getString("ServerIPHost");
                String server_port = server.getString("OpenVPNTCPPort");

                ConfigParser parser = new ConfigParser();
                int cert = R.raw.cert1;
                if (server.optInt("Cert", 1) == 2) {
                    cert = R.raw.cert2;
                } else if (server.optInt("Cert", 1) == 3) {
                    cert = R.raw.cert3;
                }
                //parser.parseConfig(new InputStreamReader(getResources().openRawResource(cert)));
                parser.parseConfig(config.getOVPNCert().isEmpty() ? new InputStreamReader(getResources().openRawResource(cert)) : new StringReader(config.getOVPNCert()));
                VpnProfile vp = parser.convertProfile();

                Connection mConnection = vp.mConnections[0];
                mConnection.mServerName = server_ip;
                mConnection.mServerPort = server_port;
                mConnection.mUseCustomConfig = true;
                //mConnection.mCustomConfiguration = String.format("http-retry 1\nhttp-rertry-max 3\nhttp-proxy %s", "127.0.0.1 8989");

                String encoded_name = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    encoded_name = String.format("%s.ovpn", URLEncoder.encode(server_name, StandardCharsets.UTF_8));
                }
                String config = vp.getConfigFile(this, true);
                File dir = new File(getFilesDir(), encoded_name);
                OutputStream out = new FileOutputStream(dir);
                out.write(config.getBytes());
                out.flush();
                out.close();

                int serverProtocol = server.optInt("Protocol", -1);
                if (serverProtocol != -1 && ((tunnelType <= 5 && (proto == 0 ? serverProtocol != 0 : (proto == 1 ? serverProtocol != 4 : serverProtocol != 5))) || (tunnelType == 6 && serverProtocol != 1) || (tunnelType == 7 && serverProtocol != 2) || (tunnelType == 8 && serverProtocol != 3) || (tunnelType == 9 && serverProtocol != 6))) {
                    continue;
                }
                servers.put(server);

                listProfiles.add(serversArray.getJSONObject(i));
                mServerAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            showToast("Server Error: " + e.getMessage());
        }
    }

    public void checkUpdates() {

        if (showNoUpdate) {

            View view = LayoutInflater.from(OpenVPNClient.this).inflate(R.layout.layout_dialog, null);

            checkingDialog = new AlertDialog.Builder(OpenVPNClient.this)
                    .setView(view)
                    .show();

            checkingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            ((LottieAnimationView) view.findViewById(R.id.main_animation)).setAnimation(R.raw.anim3);
            ((TextView) view.findViewById(R.id.title_text)).setText("Config Update");
            ((TextView) view.findViewById(R.id.desc_text)).setText("Checking for new update...");
            ((TextView) view.findViewById(R.id.positive_text)).setText("Okay");

            view.findViewById(R.id.close_icon).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkingDialog.dismiss();
                }
            });

            view.findViewById(R.id.positive_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkingDialog.dismiss();
                }
            });
        }


        final ServerUpdate su = new ServerUpdate(this);
        su.setURL(myPrefs.getString("update_api", "https://penel-demo.ggff.net/api/files/app?json=smtunnel"));
        //su.setURL("https://json.rktunnelvip.xyz/uploads/json/f53a6d464a44837c375e.json");
        su.setUpdateListener(new ServerUpdate.OnUpdateListener() {
            @Override
            public void onShowUpdate(JSONObject obj) {
                if (checkingDialog != null && checkingDialog.isShowing()) {
                    checkingDialog.dismiss();
                }
                for (File f : getFilesDir().listFiles()) {
                    if (f.getAbsolutePath().endsWith(".ovpn")) {
                        f.delete();
                    }
                }
                myPrefs.edit().putString("banner_ad", obj.optString("banner_ad", "")).apply();
                myPrefs.edit().putString("interstitial_ad", obj.optString("interstitial_ad", "")).apply();
                myPrefs.edit().putString("rewarded_ad", obj.optString("rewarded_ad", "")).apply();
                myPrefs.edit().putString("app_open_ad", obj.optString("app_open_ad", "")).apply();
                myPrefs.edit().putString("auth_api", obj.optString("auth_api", "")).apply();
                if (!obj.optString("update_api", "").isEmpty()) {
                    myPrefs.edit().putString("update_api", obj.optString("update_api", "")).apply();
                }
                myPrefs.edit().putString("notice_api", obj.optString("notice_api", "")).apply();
                myPrefs.edit().putString("banner1_url", obj.optString("banner1_url", "")).apply();
                myPrefs.edit().putString("banner2_url", obj.optString("banner2_url", "")).apply();
                myPrefs.edit().putString("banner3_url", obj.optString("banner3_url", "")).apply();
                config.setOVPNCert(decrypt(obj.optString("OVPNCert", "")));
                refresh();
                loadBanners();
                Toast.makeText(OpenVPNClient.this, "Updated!", Toast.LENGTH_SHORT).show();
                showNotice();
            }

            @Override
            public void onNoUpdateAvailable(String oldVersion) {
                if (checkingDialog != null && checkingDialog.isShowing()) {
                    checkingDialog.dismiss();
                }
                AlertDialog.Builder ab = new AlertDialog.Builder(OpenVPNClient.this);
                ab.setTitle("No Update");
                ab.setMessage("Sorry there's no Update Available. Your current Server Version is " + oldVersion);
                ab.setPositiveButton("Ok", null);
                //  if (showNoUpdate) {
                //      ab.show();
                //  }
                if (showNoUpdate) {
                    Toast.makeText(OpenVPNClient.this, "No Update Available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUpdateError(String error) {
                if (checkingDialog != null && checkingDialog.isShowing()) {
                    checkingDialog.dismiss();
                }
            }
        });

        try {
            su.setCurrentVersion(getJSONObject().getString("Version"));
        } catch (JSONException e) {
        }
        su.start();
    }

    private void refresh() {
        loadNetworks();
        loadServers();
        mBoundService.refresh_profile_list();
        String version = getConfigVersion();
        profile_spin.setSelection(getServerSelected());
        if (myPrefs.getInt("network_position", 0) <= listNetwork.size()) {
            network_spin.setSelection(myPrefs.getInt("network_position", 0));
        }
        if (myPrefs.getInt("MyProfile", 0) <= listProfiles.size()) {
            profile_spin.setSelection(myPrefs.getInt("MyProfile", 0));
        }
        TextView configversion = findViewById(R.id.config_version);
        configversion.setText(version);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String str = TAG;
        Object[] objArr = new Object[S_BIND_CALLED];
        objArr[0] = intent.toString();
        Log.d(str, String.format("CLI: onNewIntent intent=%s", objArr));
        setIntent(intent);
    }

    protected void post_bind() {
        Log.d(TAG, "CLI: post bind");
        this.startup_state |= S_BIND_CALLED;
        process_autostart_intent(is_active());
        render_last_event();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_update) {
            showNoUpdate = true;
            showToast("Checking Updates");
            checkUpdates();
            return true;
        } else if (itemId == R.id.menu_clear_data) {
            showClearDataDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void event(EventMsg ev) {
        render_event(ev, RETAIN_AUTH, is_active(), RETAIN_AUTH);
    }

    private void render_last_event() {
        boolean active = is_active();
        EventMsg ev = get_last_event();
        if (ev != null) {
            render_event(ev, true, active, true);
        } else if (n_profiles_loaded() > 0) {
            render_event(EventMsg.disconnected(), true, active, true);
        } else {
            hide_status();
            ui_setup(active, UIF_RESET, null);
            show_progress(0, active);
        }
        EventMsg pev = get_last_event_prof_manage();
        if (pev != null) {
            render_event(pev, true, active, true);
        }
    }

    @SuppressLint("WrongConstant")
    private boolean show_conn_info_field(String text, int field_id, int row_id) {
        int i = 0;
        boolean vis = text.length() > 0 || RETAIN_AUTH;
        TextView tv = findViewById(field_id);
        View row = findViewById(row_id);
        tv.setText(text);
        if (!vis) {
            i = 8;
        }
        row.setVisibility(i);
        return vis;
    }

    private void reset_conn_info() {
        show_conn_info(new ClientAPI_ConnectionInfo());
    }

    private void show_conn_info(ClientAPI_ConnectionInfo ci) {
        this.info_group.setVisibility((((((((RETAIN_AUTH | show_conn_info_field(ci.getVpnIp4(), R.id.ipv4_addr, R.id.ipv4_addr_row)) | show_conn_info_field(ci.getVpnIp6(), R.id.ipv6_addr, R.id.ipv6_addr_row)) | show_conn_info_field(ci.getUser(), R.id.user, R.id.user_row)) | show_conn_info_field(ci.getClientIp(), R.id.client_ip, R.id.client_ip_row)) | show_conn_info_field(ci.getServerHost(), R.id.server_host, R.id.server_host_row)) | show_conn_info_field(ci.getServerIp(), R.id.server_ip, R.id.server_ip_row)) | show_conn_info_field(ci.getServerPort(), R.id.server_port, R.id.server_port_row)) | show_conn_info_field(ci.getServerProto(), R.id.server_proto, R.id.server_proto_row) ? View.VISIBLE : View.GONE);
        set_visibility_stats_expansion_group();
    }

    @SuppressLint("WrongConstant")
    private void set_visibility_stats_expansion_group() {
        int i = 0;
        boolean expand_stats = this.prefs.get_boolean("expand_stats", RETAIN_AUTH);
        View view = this.stats_expansion_group;
        if (!expand_stats) {
            i = 8;
        }
        view.setVisibility(android.view.View.VISIBLE);
        this.details_more_less.setText(expand_stats ? R.string.touch_less : R.string.touch_more);
    }

    private void render_event(EventMsg ev, boolean reset, boolean active, boolean cached) {
        int flags = ev.flags;
        if (ev.is_reflected(this)) {
            flags |= UIF_REFLECTED;
        }
        if (reset || (flags & 8) != 0 || ev.profile_override != null) {
            ui_setup(active, UIF_RESET | flags, ev.profile_override);

        } else if (ev.res_id == R.string.core_thread_active) {
            active = true;
            ui_setup(true, flags, null);
            enabledWidgets(false);
        } else if (ev.res_id == R.string.core_thread_inactive) {
            active = RETAIN_AUTH;
            ui_setup(RETAIN_AUTH, flags, null);
            if (!ConfigUtil.isV2RAY()) {
                if (!(config.getTunnelType() == ConfigUtil.MODE_OVPN_DIRECT_UDP)) {
                    enabledWidgets(!InjectorService.isRunning);
                } else {
                    enabledWidgets(true);
                }
            }
        }

        if (ev.res_id == R.string.connected) { /*2131034168*/
            progress.setColor(getResources().getColor(R.color.jx_stop_bg));
            disconnect_button.setTextColor(getResources().getColor(R.color.jx_stop_bg));
            this.main_scroll_view.fullScroll(33);
            enabledWidgets(false);
            this.status_01.setTextColor(getResources().getColor(R.color.jx_stop_bg));
            this.status_01.setText("Connected");
            this.isConnected = true;
            enabledWidgets(false);
            status(true);
            if (isStart) {
                //startService(new Intent(OpenVPNClient.this, TimerService.class));
                showInterstitialAds();
                runBanner();
                loadBanners();
                isStart = false;
            }
        } else if (ev.res_id == R.string.auth_failed) {
            stopVPN();
        } else if (ev.res_id == R.string.info_msg) { /*2131034237*/
            if (ev.info.startsWith("OPEN_URL:")) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(ev.info.substring(9)));
                intent.putExtra("com.android.browser.application_id", getPackageName());
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        } else if (ev.res_id == R.string.tap_not_supported) { /*2131034362*/
            if (!cached) {
                ok_dialog(resString(R.string.tap_unsupported_title), resString(R.string.tap_unsupported_error));
            }
        } else if (ev.res_id == R.string.tun_iface_create) { /*2131034371*/
            if (!cached) {
                ok_dialog(resString(R.string.tun_ko_title), resString(R.string.tun_ko_error));
            }
        } else if (ev.res_id == R.string.warn_msg) { /*2131034390*/
            this.delayed_finish_on_connect = FinishOnConnect.PENDING;
            final AppCompatActivity self = this;
            ok_dialog(resString(R.string.warning_title), ev.info, new Runnable() {
                public void run() {
                    if (!(OpenVPNClient.this.delayed_finish_on_connect == FinishOnConnect.PENDING || OpenVPNClient.this.delayed_finish_on_connect == FinishOnConnect.DISABLED)) {
                        self.finish();
                    }
                    OpenVPNClient.this.delayed_finish_on_connect = FinishOnConnect.DISABLED;
                }
            });
        }
        if (ev.priority >= S_BIND_CALLED) {
            if (ev.icon_res_id >= 0) {
                show_status_icon(ev.icon_res_id);
            }
            if (ev.res_id == R.string.connected) {
                if (!clicked) {
                    progress.setProgress(100f);
                }
                showExpireDate();
                showNoUpdate = false;
                autoUpdate = true;
                this.isConnected = true;
                showNotice();
                show_status(ev.res_id);
                if (ev.conn_info != null) {
                    show_conn_info(ev.conn_info);
                }

            } else if (ev.info.length() > 0) {
                Object[] objArr = new Object[S_ONSTART_CALLED];
                objArr[0] = resString(ev.res_id);
                objArr[S_BIND_CALLED] = ev.info;
                show_status(String.format("%s : %s", objArr));
            } else {
                if (!(config.getTunnelType() == ConfigUtil.MODE_UDP) && !(config.getTunnelType() == ConfigUtil.MODE_PSIPHON) && !ConfigUtil.isV2RAY() && !isSSHorOC()) {
                    show_status(ev.res_id);
                    if (ev.res_id == R.string.disconnected) {
                        progress.setProgressWithAnimation(0.0f);
                        progress.setColor(getResources().getColor(R.color.jx_start_bg));
                        disconnect_button.setTextColor(getResources().getColor(R.color.jx_start_bg));
                        this.status_01.setTextColor(getResources().getColor(R.color.jx_start_bg));
                        this.status_01.setText("Start");
                        this.isConnected = false;
                    }
                }
            }
        }

        show_progress(ev.progress, active);
        show_stats();
        if (ev.res_id == R.string.connected && this.finish_on_connect != FinishOnConnect.DISABLED) {
            if (this.prefs.get_boolean("autostart_finish_on_connect", RETAIN_AUTH)) {
                //Activity self = this;
                if (this.delayed_finish_on_connect == FinishOnConnect.PENDING) {
                    this.delayed_finish_on_connect = this.finish_on_connect;
                    return;
                }
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (OpenVPNClient.this.finish_on_connect != FinishOnConnect.DISABLED) {
                            finish();
                        }
                    }
                }, 1000);
                return;
            }
            this.finish_on_connect = FinishOnConnect.DISABLED;
        }
    }

    private void stop_service() {
        submitDisconnectIntent(true);
    }

    private void stop() {
        cancel_stats();
        doUnbindService();
        unbindInjector();
        if (timerService != null) {
            unbindService(timerConnection);
            timerService = null;
        }
        if (this.stop_service_on_client_exit) {
            Log.d(TAG, "CLI: stopping service");
            stop_service();
        }
    }

    protected void onStop() {
        Log.d(TAG, "CLI: onStop");
        cancel_stats();
        super.onStop();
    }

    @Override
    protected void onResume() {
        SkStatus.addStateListener(this);
        DNSTunnelThread.setSocksListener(this);
        SocksDNSService.setTunListener(this);
        OpenVpnService.setTunListener(this);
        SSHService.setTunListener(this);
        TunnelManager.setTunListener(this);
        /*timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run()
                {
                    runOnUiThread(new Runnable() {

                            @Override
                            public void run()
                            {
                                String ipAddr = String.format("%s IP: %s", getNetworkType(), Utils.getIPAddress(true));
                                mIpAddr.setText(ipAddr);
                                // TODO: Implement this method
                            }


                    });
                    // TODO: Implement this method
                }


        }, 0, 1000);*/
        // TODO: Implement this method
        // enabledWidgets(is_active());
        String version = getConfigVersion();
        profile_spin.setSelection(getServerSelected());
        if (myPrefs.getInt("network_position", 0) <= listNetwork.size()) {
            network_spin.setSelection(myPrefs.getInt("network_position", 0));
        }
        if (myPrefs.getInt("MyProfile", 0) <= listProfiles.size()) {
            profile_spin.setSelection(myPrefs.getInt("MyProfile", 0));
        }
        if (myUser.getText().toString().isEmpty()) {
            myUser.setText(myPrefs.getString(USERNAME, ""));
        }
        if (myPass.getText().toString().isEmpty()) {
            myPass.setText(myPrefs.getString(PASSWORD, ""));
        }
        //checkUpdates();
        TextView configversion = findViewById(R.id.config_version);
        configversion.setText(version);

        TextView iplocal = findViewById(R.id.iplocal);
        iplocal.setText("Local IP: " + Utils.getLocalIP());
        super.onResume();
    }

    private String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info != null) {
            return info.getTypeName();
        }
        return "";
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "CLI: onStart");
        this.startup_state |= S_ONSTART_CALLED;
        if (this.finish_on_connect == FinishOnConnect.ENABLED) {
            this.finish_on_connect = FinishOnConnect.ENABLED_ACROSS_ONSTART;
        }
        showNoUpdate = false;
        checkUpdates();
        showNotice();
        boolean active = is_active();
        if (active) {
            schedule_stats();
        }
        if (process_autostart_intent(active)) {
            ui_setup(active, UIF_RESET, null);
        }
    }

    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        stop();
        if (v2rayBroadCastReceiver != null) {
            unregisterReceiver(v2rayBroadCastReceiver);
            v2rayBroadCastReceiver = null;
        }
        SkStatus.removeStateListener(this);
        Log.d(TAG, "CLI: onDestroy called");
        super.onDestroy();
    }

    private boolean process_autostart_intent(boolean active) {
        if ((this.startup_state & REQUEST_IMPORT_PKCS12) == REQUEST_IMPORT_PKCS12) {
            Intent intent = getIntent();
            String apn_key = "net.openvpn.openvpn.AUTOSTART_PROFILE_NAME";
            String apn = intent.getStringExtra(apn_key);
            if (apn != null) {
                this.autostart_profile_name = null;
                String str = TAG;
                Object[] objArr = new Object[S_BIND_CALLED];
                objArr[0] = apn;
                Log.d(str, String.format("CLI: autostart: %s", objArr));
                intent.removeExtra(apn_key);
                if (!active) {
                    ProfileList proflist = profile_list();
                    if (proflist == null || proflist.get_profile_by_name(apn) == null) {
                        ok_dialog(resString(R.string.profile_not_found), apn);
                    } else {
                        this.autostart_profile_name = apn;
                        return true;
                    }
                } else if (!current_profile().get_name().equals(apn)) {
                    this.autostart_profile_name = apn;
                    submitDisconnectIntent(RETAIN_AUTH);
                }
            }
        }
        return RETAIN_AUTH;
    }

    private void cancel_ui_reset() {
        this.ui_reset_timer_handler.removeCallbacks(this.ui_reset_timer_task);
    }

    private void schedule_ui_reset(long delay) {
        cancel_ui_reset();
        this.ui_reset_timer_handler.postDelayed(this.ui_reset_timer_task, delay);
    }

    private void hide_status() {
        this.status_view.setVisibility(View.GONE);
    }

    private void show_status(String text) {
        this.status_view.setVisibility(View.VISIBLE);
        if (!text.contains("127.0.0.1")) {
            this.status_view.setText(text);
        }
        if (text.equals(getString(R.string.auth_failed))) {
            status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
            status_view.setText("Invalid Account!");
            return;
        }
        if (text.equals(getString(R.string.disconnected)) || text.equals(getString(R.string.auth_failed))) {
            status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
            status_view.setText("Disconnected");
            return;
        }
        if (text.equals(getString(R.string.connected))) {
            status_view.setTextColor(getResources().getColor(R.color.connectedcolor));

            return;
        }
        status_view.setTextColor(getResources().getColor(R.color.red_btn_bg_color));
    }

    private void show_status(int res_id) {
        this.status_view.setVisibility(View.VISIBLE);
        if (!getString(res_id).contains("127.0.0.1")) {
            this.status_view.setText(res_id);
        }
        //disconnect_button.setTextColor(Color.BLACK);
        // disconnect_button.setBackgroundResource(R.drawable.button_connect);
        //disconnect_button.setText(getString(res_id));

        /*if (res_id == R.string.auth_failed) {
            status_view.setTextColor(Color.RED);
            status_view.setText("Invalid Account!");
            return;
        }*/
        if (res_id == R.string.disconnected || res_id == R.string.auth_failed) {
            status_view.setTextColor(getResources().getColor(R.color.red_btn_bg_color));
            status_view.setText("Disconnected");
            // disconnect_button.setText("Disconnect");
            return;
        }
        if (res_id == R.string.connected) {
            //disconnect_button.setTextColor(Color.WHITE);
            // disconnect_button.setBackgroundResource(R.drawable.button_disconnect);
            status_view.setTextColor(getResources().getColor(R.color.connectedcolor));
            return;
        }
        status_view.setTextColor(getResources().getColor(R.color.red_btn_bg_color));
    }

    private void show_status_icon(int res_id) {
        this.status_icon_view.setImageResource(res_id);
    }

    private void show_progress(int progress, boolean active) {
        if (!(config.getTunnelType() == ConfigUtil.MODE_UDP) && !(config.getTunnelType() == ConfigUtil.MODE_PSIPHON) && !ConfigUtil.isV2RAY() && !isSSHorOC()) {
            this.progress.setProgressWithAnimation((float) progress);
        }
    }

    @SuppressLint("WrongConstant")
    private void setDraweeView1(boolean a) {
        @SuppressLint("ResourceType") Animation animation = AnimationUtils.loadAnimation(this, R.animator.rtt);
        @SuppressLint("ResourceType") Animation animation2 = AnimationUtils.loadAnimation(this, R.animator.blink);
        String str = getResources().getString(R.string.connecting);
        ImageView iv = findViewById(R.id.my_image_view_01);
        if (a && !this.isConnected) {
            for (int i = 0; i < str.length(); i++) {
                this.status_01.setText(str.substring(0, i + 1));
            }
            iv.setVisibility(8);
            iv.clearAnimation();
            animation2.reset();
            this.mSimpleDraweeView.setVisibility(0);
            this.mSimpleDraweeView.startAnimation(animation);
        } else if (a && this.isConnected) {
            iv.setVisibility(android.view.View.VISIBLE);
            iv.startAnimation(animation2);
            this.status_01.setText("");
            this.mSimpleDraweeView.setVisibility(8);
            this.mSimpleDraweeView.clearAnimation();
            animation.reset();
        } else {
            iv.setVisibility(8);
            iv.clearAnimation();
            animation2.reset();
            this.status_01.setText("START");
            this.mSimpleDraweeView.setVisibility(8);
            this.mSimpleDraweeView.clearAnimation();
            animation.reset();
        }
    }


    private void cancel_stats() {
        this.stats_timer_handler.removeCallbacks(this.stats_timer_task);
    }

    private void schedule_stats() {
        cancel_stats();
        this.stats_timer_handler.postDelayed(this.stats_timer_task, 1000);
    }

    private static String render_bandwidth(long bw) {
        String postfix;
        float div;
        Object[] objArr;
        float bwf = (float) bw;
        if (bwf >= 1.0E12f) {
            postfix = "TB";
            div = 1.09951163E12f;
        } else if (bwf >= 1.0E9f) {
            postfix = "GB";
            div = 1.07374182E9f;
        } else if (bwf >= 1000000.0f) {
            postfix = "MB";
            div = 1048576.0f;
        } else if (bwf >= 1000.0f) {
            postfix = "KB";
            div = 1024.0f;
        } else {
            objArr = new Object[S_BIND_CALLED];
            objArr[0] = Float.valueOf(bwf);
            return String.format("%.0f KB", objArr);
        }
        objArr = new Object[S_ONSTART_CALLED];
        objArr[0] = Float.valueOf(bwf / div);
        objArr[S_BIND_CALLED] = postfix;
        return String.format("%.2f %s", objArr);
    }

    private String render_last_pkt_recv(int sec) {
        if (sec >= 3600) {
            return resString(R.string.lpr_gt_1_hour_ago);
        }
        String resString;
        Object[] objArr;
        if (sec >= 120) {
            resString = resString(R.string.lpr_gt_n_min_ago);
            objArr = new Object[S_BIND_CALLED];
            objArr[0] = Integer.valueOf(sec / 60);
            return String.format(resString, objArr);
        } else if (sec >= S_ONSTART_CALLED) {
            resString = resString(R.string.lpr_n_sec_ago);
            objArr = new Object[S_BIND_CALLED];
            objArr[0] = Integer.valueOf(sec);
            return String.format(resString, objArr);
        } else if (sec == S_BIND_CALLED) {
            return resString(R.string.lpr_1_sec_ago);
        } else {
            if (sec == 0) {
                return resString(R.string.lpr_lt_1_sec_ago);
            }
            return BuildConfig.FLAVOR;
        }
    }

    private void show_stats() {
        if (is_active()) {
            ConnectionStats stats = get_connection_stats();
            this.last_pkt_recv_view.setText(render_last_pkt_recv(stats.last_packet_received));
            this.duration_view.setText(OpenVPNClientBase.render_duration(stats.duration));
            this.bytes_in_view.setText(render_bandwidth(stats.bytes_in));
            this.bytes_out_view.setText(render_bandwidth(stats.bytes_out));
        }
    }

    private void clear_stats() {
        this.last_pkt_recv_view.setText(BuildConfig.FLAVOR);
        this.duration_view.setText(BuildConfig.FLAVOR);
        this.bytes_in_view.setText(BuildConfig.FLAVOR);
        this.bytes_out_view.setText(BuildConfig.FLAVOR);
        reset_conn_info();
    }

    private int n_profiles_loaded() {
        ProfileList proflist = profile_list();
        if (proflist != null) {
            return proflist.size();
        }
        return 0;
    }

    private String selected_profile_name() {
        String ret = null;
        ProfileList proflist = profile_list();
        if (SpinUtil.get_spinner_selected_item(profile_spin).contains("Auto")) {
            try {
                return servers.getJSONObject(mRandowmServer).getString("Name");
            } catch (JSONException e) {
            }
        }
        if (proflist != null && proflist.size() > 0) {
            ret = proflist.size() == S_BIND_CALLED ? proflist.get(0).get_name() : SpinUtil.get_spinner_selected_item(profile_spin);
        }
        if (ret == null) {
            return "UNDEFINED_PROFILE";
        }
        return ret;
    }

    private Profile selected_profile() {
        ProfileList proflist = profile_list();
        if (proflist != null) {
            return proflist.get_profile_by_name(selected_profile_name());
        }
        return null;
    }

    private void clear_auth() {
        this.username_edit.setText(BuildConfig.FLAVOR);
        this.pk_password_edit.setText(BuildConfig.FLAVOR);
        this.password_edit.setText(BuildConfig.FLAVOR);
        this.response_edit.setText(BuildConfig.FLAVOR);
    }

    private void ui_setup(boolean active, int flags, String profile_override) {
        boolean orig_active = active;
        boolean autostart = RETAIN_AUTH;
        cancel_ui_reset();
        if (!((UIF_RESET & flags) == 0 && orig_active == this.last_active)) {
            clear_auth();
            if (!(active || this.autostart_profile_name == null)) {
                autostart = true;
                profile_override = this.autostart_profile_name;
                this.autostart_profile_name = null;
            }
            ProfileList proflist = profile_list();
            Profile prof = null;
            if (proflist == null || proflist.size() <= 0) {
                this.profile_group.setVisibility(View.GONE);
            } else {
                ProfileSource ps = ProfileSource.UNDEF;


                //SpinUtil.show_spinner(this, this.profile_spin, proflist.profile_names());

                if (active) {
                    ps = ProfileSource.SERVICE;
                    prof = current_profile();
                }
                if (prof == null && profile_override != null) {
                    ps = ProfileSource.PRIORITY;
                    prof = proflist.get_profile_by_name(profile_override);
                    if (prof == null) {
                        Log.d(TAG, "CLI: profile override not found");
                        autostart = RETAIN_AUTH;
                    }
                }
                /*if (prof == null) {
                    if ((UIF_PROFILE_SETTING_FROM_SPINNER & flags) != 0) {
                        ps = ProfileSource.SPINNER;
                        prof = proflist.get_profile_by_name(SpinUtil.get_spinner_selected_item(this.profile_spin));
                    } else {
                        ps = ProfileSource.PREFERENCES;
                        prof = proflist.get_profile_by_name(this.prefs.get_string("profile"));
                    }
                }*/
                if (SpinUtil.get_spinner_selected_item(profile_spin).contains("Auto")) {
                    try {
                        prof = proflist.get_profile_by_name(servers.getJSONObject(mRandowmServer).getString("Name"));
                    } catch (JSONException e) {
                    }
                } else {
                    prof = proflist.get_profile_by_name(SpinUtil.get_spinner_selected_item(profile_spin));
                }
                if (prof == null) {
                    ps = ProfileSource.LIST0;
                    prof = proflist.get(0);
                }
                if (ps != ProfileSource.PREFERENCES && (UIF_REFLECTED & flags) == 0) {
                    this.prefs.set_string("profile", prof.get_name());
                    gen_ui_reset_event(true);
                }
               /* if (ps != ProfileSource.SPINNER) {
                    SpinUtil.set_spinner_selected_item(this.profile_spin, prof.get_name());
                }*/
                this.profile_group.setVisibility(View.VISIBLE);
                //this.profile_spin.setEnabled(!active ? true : RETAIN_AUTH);
                this.profile_edit.setVisibility(active ? View.GONE : View.VISIBLE);
            }
            if (prof != null) {
                if ((UIF_RESET & flags) != 0) {
                    prof.reset_dynamic_challenge();
                }
                EditText focus = null;
                if (!active && (flags & 32) != 0) {
                    this.post_import_help_blurb.setVisibility(View.VISIBLE);
                } else if (active) {
                    this.post_import_help_blurb.setVisibility(View.GONE);
                }
                ProxyList proxy_list = get_proxy_list();
                if (active || proxy_list.size() <= 0) {
                    this.proxy_group.setVisibility(View.GONE);
                } else {
                    SpinUtil.show_spinner(this, this.proxy_spin, proxy_list.get_name_list(true));
                    String name = proxy_list.get_enabled(true);
                    if (name != null) {
                        SpinUtil.set_spinner_selected_item(this.proxy_spin, name);
                    }
                    this.proxy_group.setVisibility(View.VISIBLE);
                }
                if (active || !prof.server_list_defined()) {
                    this.server_group.setVisibility(View.GONE);
                } else {
                    SpinUtil.show_spinner(this, this.server_spin, prof.get_server_list().display_names());
                    String server = this.prefs.get_string_by_profile(prof.get_name(), "server");
                    if (server != null) {
                        SpinUtil.set_spinner_selected_item(this.server_spin, server);
                    }
                    this.server_group.setVisibility(View.VISIBLE);
                }
                if (active) {
                    this.username_group.setVisibility(View.GONE);
                    this.pk_password_group.setVisibility(View.GONE);
                    this.password_group.setVisibility(View.GONE);
                } else {
                    boolean is_pwd_save;
                    String saved_pwd;
                    boolean udef = prof.userlocked_username_defined();
                    boolean autologin = prof.get_autologin();
                    boolean pk_pwd_req = prof.get_private_key_password_required();
                    boolean dynamic_challenge = prof.is_dynamic_challenge();
                    if ((!autologin || (autologin && udef)) && !dynamic_challenge) {
                        if (udef) {
                            this.username_edit.setText(prof.get_userlocked_username());
                            set_enabled(this.username_edit, RETAIN_AUTH);
                        } else {
                            set_enabled(this.username_edit, true);
                            String pref_username = this.prefs.get_string_by_profile(prof.get_name(), "username");
                            if (pref_username != null) {
                                this.username_edit.setText(pref_username);
                            } else if (null == null) {
                                focus = this.username_edit;
                            }
                        }
                        this.username_group.setVisibility(View.VISIBLE);
                    } else {
                        this.username_group.setVisibility(View.GONE);
                    }
                    if (pk_pwd_req) {
                        is_pwd_save = this.prefs.get_boolean_by_profile(prof.get_name(), "pk_password_save", RETAIN_AUTH);
                        saved_pwd = null;
                        this.pk_password_group.setVisibility(View.VISIBLE);
                        this.pk_password_save_checkbox.setChecked(is_pwd_save);
                        if (is_pwd_save) {
                            saved_pwd = this.pwds.get("pk", prof.get_name());
                        }
                        if (saved_pwd != null) {
                            this.pk_password_edit.setText(saved_pwd);
                        } else if (focus == null) {
                            focus = this.pk_password_edit;
                        }
                    } else {
                        this.pk_password_group.setVisibility(View.GONE);
                    }
                    if (autologin || dynamic_challenge) {
                        this.password_group.setVisibility(View.GONE);
                    } else {
                        boolean is_auth_pw_save = prof.get_allow_password_save();
                        is_pwd_save = is_auth_pw_save && this.prefs.get_boolean_by_profile(prof.get_name(), "auth_password_save", RETAIN_AUTH) || RETAIN_AUTH;
                        saved_pwd = null;
                        this.password_group.setVisibility(View.VISIBLE);
                        this.password_save_checkbox.setEnabled(is_auth_pw_save);
                        this.password_save_checkbox.setChecked(is_pwd_save);
                        if (is_pwd_save) {
                            saved_pwd = this.pwds.get("auth", prof.get_name());
                        }
                        if (saved_pwd != null) {
                            this.password_edit.setText(saved_pwd);
                        } else if (focus == null) {
                            focus = this.password_edit;
                        }
                    }
                }
                if (active || prof.get_autologin() || !prof.challenge_defined()) {
                    this.cr_group.setVisibility(View.GONE);
                } else {
                    this.cr_group.setVisibility(View.VISIBLE);
                    Challenge chal = prof.get_challenge();
                    this.challenge_view.setText(chal.get_challenge());
                    this.challenge_view.setVisibility(View.VISIBLE);
                    if (chal.get_response_required()) {
                        if (chal.get_echo()) {
                            this.response_edit.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                        } else {
                            this.response_edit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }
                        this.response_edit.setVisibility(View.VISIBLE);
                        if (focus == null) {
                            focus = this.response_edit;
                        }
                    } else {
                        this.response_edit.setVisibility(View.GONE);
                    }
                    if (prof.is_dynamic_challenge()) {
                        schedule_ui_reset(prof.get_dynamic_challenge_expire_delay());
                    }
                }
                this.button_group.setVisibility(View.VISIBLE);

                if (!ConfigUtil.isV2RAY() && !(config.getTunnelType() == ConfigUtil.MODE_PSIPHON) && !config.isUDP() && !isSSHorOC()) {
                    if (orig_active) {
                        this.conn_details_group.setVisibility(View.VISIBLE);
                        this.connect_button.setVisibility(View.GONE);
                        this.disconnect_button.setVisibility(View.VISIBLE);
                    } else {
                        this.conn_details_group.setVisibility(View.GONE);
                        this.connect_button.setVisibility(View.VISIBLE);
                        this.disconnect_button.setVisibility(View.GONE);
                    }
                }

                if (focus != null) {
                    autostart = RETAIN_AUTH;
                }
                req_focus(focus);
            } else {
                if (!ConfigUtil.isV2RAY() && !(config.getTunnelType() == ConfigUtil.MODE_PSIPHON) && !config.isUDP() && !isSSHorOC()) {
                    this.post_import_help_blurb.setVisibility(View.GONE);
                    this.proxy_group.setVisibility(View.GONE);
                    this.server_group.setVisibility(View.GONE);
                    this.username_group.setVisibility(View.GONE);
                    this.pk_password_group.setVisibility(View.GONE);
                    this.password_group.setVisibility(View.GONE);
                    this.cr_group.setVisibility(View.GONE);
                    this.conn_details_group.setVisibility(View.GONE);
                    this.button_group.setVisibility(View.GONE);
                    show_status_icon(R.drawable.info);
                    //show_status(R.string.no_profiles_loaded);
                    show_status(R.string.state_disconnected);
                }
            }
            if (orig_active) {
                schedule_stats();
            } else {
                cancel_stats();
            }
        }
        this.last_active = orig_active;
        if (autostart && !this.last_active) {
            this.finish_on_connect = FinishOnConnect.ENABLED;
            start_connect();
        }
        ConfigUtil cu = new ConfigUtil(this);
        if (cu.isUDP()) {
            setStarterButton();
        }
    }

    private boolean isSSHorOC() {
        return protocol == 1 || protocol == 2;
    }

    private void enabledWidgets(boolean enabled) {
        if (enabled) {
            hideGraph(true);
            status(false);
            disconnect_button.setVisibility(View.GONE);
            connect_button.setVisibility(View.VISIBLE);
        } else {
            hideGraph(false);
            disconnect_button.setVisibility(View.VISIBLE);
            connect_button.setVisibility(View.GONE);
        }
        //mServerLayout.setEnabled(enabled);
        mNetworkLayout.setEnabled(enabled);
        network_spin.setEnabled(enabled);
        profile_spin.setEnabled(enabled);
        // mCustomTweakSw.setEnabled(enabled);
        setDraweeView1(!enabled);

        myUser.setEnabled(enabled);
        myPass.setEnabled(enabled);
    }

    private void status(boolean isOn) {
        con_status.setText(isOn ? "VPN IS ON" : "VPN IS OFF");
        con_status.setTextColor(getResources().getColor(isOn ? R.color.connect_color : R.color.jx_start_bg));
    }

    private void hideGraph(boolean yes) {
        findViewById(R.id.graph_layout).setVisibility(yes ? View.GONE : View.VISIBLE);
        // TODO: Implement this method
    }

    private void set_enabled(EditText editText, boolean state) {
        editText.setEnabled(state);
        editText.setFocusable(state);
        editText.setFocusableInTouchMode(state);
    }

    private void raise_file_selection_dialog(int requestCode) {
        switch (requestCode) {
            case S_ONSTART_CALLED /*2*/:
                raise_file_selection_dialog(S_ONSTART_CALLED, R.string.select_profile);
                return;
            case REQUEST_IMPORT_PKCS12 /*3*/:
                raise_file_selection_dialog(REQUEST_IMPORT_PKCS12, R.string.select_pkcs12);
                return;
            default:
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0) {
            switch (requestCode) {
                case REQUEST_OFFLINE_UPDATE:
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        } else {
                            showToast("You need to Grant the permission to use Offline update");
                        }
                    }
                    return;
                case S_ONSTART_CALLED /*2*/:
                case REQUEST_IMPORT_PKCS12 /*3*/:
                    int i = 0;
                    while (i < grantResults.length) {
                        if (permissions[i].equals("android.permission.READ_EXTERNAL_STORAGE") && grantResults[i] == 0) {
                            raise_file_selection_dialog(requestCode);
                        }
                        i += S_BIND_CALLED;
                    }
                    return;
                default:
            }
        }
    }

    private void request_file_selection_dialog(int requestCode) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0) {
            raise_file_selection_dialog(requestCode);
            return;
        }
        String[] perms = new String[S_BIND_CALLED];
        perms[0] = "android.permission.READ_EXTERNAL_STORAGE";
        ActivityCompat.requestPermissions(this, perms, requestCode);
    }


    public void onClick(View v) {
        cancel_ui_reset();
        this.autostart_profile_name = null;
        this.finish_on_connect = FinishOnConnect.DISABLED;
        int viewid = v.getId();
        if (viewid == R.id.connect) {

            /*if (!timerService.isTimeAvailable()) {
                Toast.makeText(this, "Please add time first!", Toast.LENGTH_SHORT).show();
                return;
            }*/

            if (myUser.getText().toString().isEmpty() || myPass.getText().toString().isEmpty()) {
                Toast.makeText(this, "Invalid username or password!", Toast.LENGTH_SHORT).show();
                return;
            }

            myPrefs.edit().putString(USERNAME, myUser.getText().toString()).apply();
            myPrefs.edit().putString(PASSWORD, myPass.getText().toString()).apply();

            String user = myPrefs.getString(USERNAME, "");
            String pass = myPrefs.getString(PASSWORD, "");

            myPrefs.edit().putString("X_USERNAME", user).apply();
            myPrefs.edit().putString("X_PASSWORD", pass).apply();

            //mRandowmServer = new Random().nextInt(getServersArray().length());

            clicked = true;
            mRandowmServer = new Random().nextInt(servers.length());
            showExpireDate();
            startVerifyingAccount();
            startInjector();
        } else if (viewid == R.id.disconnect) {
            status(false);
            ConfigUtil cu = new ConfigUtil(this);
            if (cu.isUDP()) {
                stopUdp();

            } else {
                stopVPN();
            }

        } else if (viewid == R.id.profile_edit || viewid == R.id.proxy_edit) {
            openContextMenu(v);
        } else if (viewid == R.id.btn_right_top) {
            FlipShareView share = new FlipShareView.Builder(this, flipmenu)
                    .addItem(new ShareItem("UPDATE", Color.WHITE, 0xff43549C, BitmapFactory.decodeResource(getResources(), R.drawable.ic_box_download)))
                    //.addItem(new ShareItem("Telegram", Color.WHITE, 0xff4999F0, BitmapFactory.decodeResource(getResources(), R.drawable.ic_telegram)))
                    .addItem(new ShareItem("Privacy", Color.WHITE, 0xff51208A, getBitmap(getResources().getDrawable(R.drawable.privacy))))
                    .addItem(new ShareItem("Tweaks", Color.WHITE, 0xff4999F0, BitmapFactory.decodeResource(getResources(), R.drawable.tweakers)))
                    .addItem(new ShareItem("Logs", Color.WHITE, 0xff57406A, getBitmap(getResources().getDrawable(R.drawable.ic_logs_icon))))
                    .addItem(new ShareItem("Clear data", Color.WHITE, 0xff57708A, BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete)))
                    .addItem(new ShareItem("Exit", Color.WHITE, 0xffD9392D, BitmapFactory.decodeResource(getResources(), R.drawable.ic_exit)))
                    .setBackgroundColor(0x60000000)
                    .setItemDuration(200)
                    .setSeparateLineColor(0x30000000)
                    .setAnimType(FlipShareView.TYPE_SLIDE)
                    .create();

            share.setOnFlipClickListener(new FlipShareView.OnFlipClickListener() {
                @Override
                public void onItemClick(int position) {
                    switch (position) {
                        default: {
                            return;
                        }
                        case 0: {
                            showUpdateDialog();
                            break;
                            //}
                            //case 1: {
                            //intentTele();
                            //break;
                        }
                        case 1: {
                            startActivity(new Intent(OpenVPNClient.this, PrivacyActivity.class));
                            break;
                        }
                        case 2: {
                            customTweak();
                            break;
                        }
                        case 3: {
                            startActivity(new Intent(OpenVPNClient.this, LogActivity.class));
                            break;
                        }
                        case 4: {
                            showClearDataDialog();
                            break;
                        }
                        case 5: {
                            AlertDialog.Builder builder = new AlertDialog.Builder(OpenVPNClient.this, R.style.technore_dialog);
                            builder.setCancelable(true);
                            builder.setMessage("Do you want to minimize or exit?");
                            builder.setPositiveButton("Exit", new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (android.os.Build.VERSION.SDK_INT >= 21) {
                                                finishAndRemoveTask();
                                            } else {
                                                android.os.Process.killProcess(android.os.Process.myPid());
                                            }
                                            System.exit(0);
                                        }
                                    });
                            builder.setNeutralButton("Minimize", new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                                            startMain.addCategory(Intent.CATEGORY_HOME);
                                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(startMain);
                                        }
                                    });
                            builder.show();
                            break;
                        }
                    }
                }

                @Override
                public void dismiss() {

                }
            });
        }
    }

    private Bitmap getBitmap(Drawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private void stopVPN() {

        ConfigUtil cu = new ConfigUtil(this);
        if (cu.getTunnelType() == ConfigUtil.MODE_PSIPHON) {
            stopTimer();
            psiphonHelper.stopTunnelService();
        } else if (cu.isUDP()) {
            stopUdp();
        } else if (ConfigUtil.isV2RAY()) {
            SkStatus.logInfo("<b>Disconnected</b>");
            stopV2ray();
        } else {
            if (cu.getTunnelType() == 9) {
                stopDNS();
            }
            stopInjector();
            if (protocol == 0) {
                stopTimer();
                submitDisconnectIntent(true);
            } else if (protocol == 1) {
                stopTimer();
                mConn.service.stopVPN();
            } else {
                stopTimer();
                if (mSSHService != null) {
                    mSSHService.onDisconnect();
                }
            }
        }
        status_view.setTextColor(getResources().getColor(R.color.jx_start_bg));
        show_status("Disconnected");
        enabledWidgets(true);
    }

    private void stopV2ray() {
        stopTimer();
        V2rayController.StopV2ray(OpenVPNClient.this);
        StatisticsGraphData.getStatisticData().getDataTransferStats().stop();
    }

    @Override
    public void onGeneratePayload(String payload) {

        // TODO: Implement this method
    }

    @Override
    public void onGeneratorClose() {
        enableNetworks(true);
        // TODO: Implement this method
    }

    @Override
    public void showToast(String str) {
        // TODO: Implement this method
        super.showToast(str);
    }

    private void showClearDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(OpenVPNClient.this, R.style.technore_dialog);
        builder.setTitle("Clear Data");
        builder.setMessage("Are you sure you want to Clear App Data?");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface p1, int p2) {
                try {
                    // clearing app data
                    if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData(); // note: it has a return value!
                    } else {
                        String packageName = getApplicationContext().getPackageName();
                        Runtime runtime = Runtime.getRuntime();
                        runtime.exec("pm clear " + packageName);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                // TODO: Implement this method
            }


        }).setNegativeButton("Cancel", null).show();
        // TODO: Implement this method
    }


    private void stopInjector() {
        stopTimer();
        StatisticsGraphData.getStatisticData().getDataTransferStats().stop();
        if (mInjector != null && InjectorService.isRunning) {
            mInjector.stopInjector();
        }
        stopService(new Intent(this, InjectorService.class));
    }

    private void startInjector() {

        //startActivity(new Intent(OpenVPNClient.this,LogActivity.class));
        try {
            isStart = true;
            StatisticsGraphData.getStatisticData().getDataTransferStats().startConnected();
            ConfigUtil config = ConfigUtil.getInstance(this);
            JSONObject server = null;
            if (profile_spin.getSelectedItemPosition() == 0) {
                server = servers.getJSONObject(mRandowmServer);
            } else {
                server = getServer();
            }
            if (server.optBoolean("AutoLogin", false) && !decrypt(server.optString("Username", "")).isEmpty() && !decrypt(server.optString("Password", "")).isEmpty()) {
                myPrefs.edit().putString("X_USERNAME", decrypt(server.getString("Username"))).apply();
                myPrefs.edit().putString("X_PASSWORD", decrypt(server.getString("Password"))).apply();
            }

            String serverName = server.getString("Name");
            config.setServerSelectedName(serverName);

            JSONObject payloadJs = (JSONObject) network_spin.getSelectedItem();
            int tunnel_type = payloadJs.getInt("TunnelType");
            config.setTunnelType(tunnel_type);

            protocol = tunnel_type == 9 ? 2 : payloadJs.optInt("Protocol", 0);
            config.setProtocol(protocol);

            config.setNameserver(decrypt(server.optString("Nameserver", "")));
            config.setPublicKey(decrypt(server.optString("PublicKey", "")));

            if (tunnel_type == ConfigUtil.MODE_V2RAY || tunnel_type == ConfigUtil.MODE_UDP) {
                if (payloadJs.optBoolean("isCustomConfig", false)) {
                    config.setHTTPayload(decrypt(payloadJs.optString("Payload", "")));
                } else {
                    String con = decrypt(server.getString("Payload"));
                    if (con.contains("~!~")) {
                        String[] split = con.split("~!~");
                        if (payloadJs.optInt("Mode", 1) == 1) {
                            config.setHTTPayload(split.length > 0 ? split[0] : "");
                        } else if (payloadJs.optInt("Mode", 1) == 2) {
                            config.setHTTPayload(split.length > 1 ? split[1] : "");
                        } else {
                            config.setHTTPayload(split.length > 2 ? split[2] : "");
                        }
                    } else {
                        config.setHTTPayload(con);
                    }
                }
            }

            //ServerHostDNS (cf) 1 ServerCloudFrontHost (ws) 2 ServerHTTPHost (http) 3

            if (tunnel_type == ConfigUtil.MODE_V2RAY) {
                config.setV2RAY(true);
                config.setUdp(false);
                startV2ray();
                enabledWidgets(false);
                return;
            } else if (tunnel_type == ConfigUtil.MODE_UDP) {
                config.setV2RAY(false);
                config.setUdp(true);
                startUDP();
                enabledWidgets(false);
                return;
            } else if (tunnel_type == ConfigUtil.MODE_PSIPHON) {
                config.setServerEntry(server.optBoolean("UseDefaultServers", false) ? "" : decrypt(server.optString("ServerEntry", "")));
                config.setV2RAY(false);
                config.setUdp(false);
                startPsiphon();
                enabledWidgets(false);
                return;
            }

            String serverType = payloadJs.optString("ServerHostDNS", "");
            String sshHost = decrypt(server.getString("ServerIPHost"));

            if (serverType.equals("ws")) {
                sshHost = decrypt(server.getString("ServerCloudFrontHost"));
            } else if (serverType.equals("http")) {
                sshHost = decrypt(server.getString("ServerHTTPHost"));
            }

            config.setSSHHost(sshHost);
            config.setSSHPort((server.getString("OpenVPNTCPPort")));
            config.setSSLPort(server.getString("OpenVPNSSLPort"));
            if (tunnel_type == ConfigUtil.MODE_V2RAY) {
                config.setUdp(false);
                config.setV2RAY(true);
                startV2ray();
                enabledWidgets(false);
            } else {
                config.setV2RAY(false);
                if (tunnel_type == ConfigUtil.MODE_UDP) {
                    config.setUdp(true);
                    startUDP();
                    enabledWidgets(false);
                } else {
                    config.setUdp(false);
                    if (myPrefs.getBoolean("isCustomSwitchOn", false)) {
                        int i = myPrefs.getInt("selected_custom_mode", 0);
                        if (i == 0) {
                            tunnel_type = ConfigUtil.MODE_SSH_HTTP_PROXY;
                        }
                        if (i == 1) {
                            tunnel_type = ConfigUtil.MODE_SSL_DIRECT;
                        }
                        if (i == 2) {
                            tunnel_type = ConfigUtil.MODE_SSH_DIRECT;
                        }
                    }

                    config.setNetworkSelectedName(payloadJs.getString("Name"));
                    if (payloadJs.has("FrontQuery") && payloadJs.has("BackQuery")) {
                        String front_query = decrypt(payloadJs.getString("FrontQuery"));
                        String back_query = decrypt(payloadJs.getString("BackQuery"));
                        if (front_query.isEmpty() && back_query.isEmpty()) {
                            config.setIsQueryMode(false);
                        } else if (!front_query.isEmpty()) {
                            config.setIsQueryMode(true);
                            config.setFrontQuery(front_query);
                            config.setBackQuery("");
                        } else if (!back_query.isEmpty()) {
                            config.setIsQueryMode(true);
                            config.setBackQuery(back_query);
                            config.setFrontQuery("");
                        }
                    } else {
                        config.setIsQueryMode(false);
                    }
                    if (tunnel_type == ConfigUtil.MODE_SSH_HTTP_PROXY || tunnel_type == ConfigUtil.MODE_SSL_HTTP_PROXY) {
                        if (payloadJs.has("ProxySettings")) {
                            JSONObject proxySettings = payloadJs.getJSONObject("ProxySettings");
                            String proxy = decrypt(proxySettings.getString("Squid"));
                            config.setProxyPort(proxySettings.getString("Port"));
                            if (proxy.contains("Default") || proxy.isEmpty()) {
                                config.setProxy(sshHost);
                                //config.setProxyPort("8080");
                            } else {
                                config.setProxy(proxy);
                            }
                        } else {
                            config.setProxy(sshHost);
                            config.setProxyPort("8080");
                        }
                    }
                    if (tunnel_type == ConfigUtil.MODE_SSL_HTTP_PROXY) {
                        if (payloadJs.has("CustomSSLPort")) {
                            if (!payloadJs.getString("CustomSSLPort").isEmpty()) {
                                config.setCustomSSLPortEnable(payloadJs.has("CustomSSLPort"));
                                config.setSSLPort(payloadJs.getString("CustomSSLPort"));
                            } else {
                                config.setCustomSSLPortEnable(false);
                                config.setSSLPort(server.getString("OpenVPNSSLPort"));
                            }
                        } else {
                            config.setSSLPort(server.getString("OpenVPNSSLPort"));
                        }
                    } else {
                        config.setCustomSSLPortEnable(false);
                    }
                    if (myPrefs.getBoolean("isCustomSwitchOn", false)) {
                        String c_inject = myPrefs.getString("custom_inject", "");
                        String c_ssl = myPrefs.getString("custom_ssl", "");
                        config.setSni(c_ssl);
                        config.setHTTPayload(c_inject);
                    } else {
                        if (payloadJs.has("SNIHost")) {
                            config.setSni(decrypt(payloadJs.getString("SNIHost")).replace("[host]", sshHost).replace("JX", sshHost).replace("[cf]", sshHost).replace("[rlb]", sshHost).replace("[rk]", sshHost));
                        }
                        if (payloadJs.has("Payload")) {
                            config.setHTTPayload(decrypt(payloadJs.getString("Payload")).replace("[host]", sshHost).replace("JX", sshHost).replace("[cf]", sshHost).replace("[rlb]", sshHost).replace("[rk]", sshHost));
                        }
                    }

                    if (config.getTunnelType() == 9) {
                        config.setSSHHost("127.0.0.1");
                        config.setSSHPort("2222");
                        config.setTunnelType(1);
                        String dnsAddress = config.getPayload();
                        config.setHTTPayload("");
                        startDNS(dnsAddress, config.getPublicKey(), config.getNameserver());
                    }

                    startService(new Intent(this, InjectorService.class).setAction("START"));
                }
            }
        } catch (Exception e) {
            Toast.makeText(OpenVPNClient.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private JSONObject getNetworkSelectedJson() throws JSONException {
        for (int i = 0; i < listNetwork.size(); i++) {
            if (listNetwork.get(i).getString("Name").equals(myPrefs.getString(SELECTED_NETWORK, ""))) {
                return listNetwork.get(i);
            }
        }
        /*for (int i = 0; i < getNetworksArray().length(); i++) {
            JSONObject js = getNetworksArray().getJSONObject(i);
            if (js.getString("Name").equals(myPrefs.getString(SELECTED_NETWORK, ""))) {
                return js;
            }
        }
        for (int i = 0; i < getSSLNetworks().length(); i++) {
            JSONObject js = getSSLNetworks().getJSONObject(i);
            if (js.getString("Name").equals(myPrefs.getString(SELECTED_NETWORK, ""))) {
                return js;
            }
        }*/
        return listNetwork.get(0);
        // TODO: Implement this method

    }

    private void startV2ray() throws JSONException {
        SkStatus.logInfo("Connecting...");
        StatisticsGraphData.getStatisticData().getDataTransferStats().startConnected();
        status_view.setText("Authenticating..");
        status_view.setTextColor(getResources().getColor(R.color.getting_started));
        progress.setProgressWithAnimation((float) 30);
        if (V2rayController.IsPreparedForConnection(OpenVPNClient.this)) {
            String user = myPrefs.getString(USERNAME, "");
            String pass = myPrefs.getString(PASSWORD, "");
            V2rayController.StartV2ray(getApplicationContext(), "Default", V2ray2Json.convert(OpenVPNClient.this, config.getPayload().replace("\u2008", " ")), null, user, pass, config.getServerSelectedName(), myPrefs.getString("auth_api", auth_api));
        } else {
            prepareForConnection();
        }
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            try {
                startV2ray();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            Toast.makeText(this, "Permission not granted.", Toast.LENGTH_LONG).show();
        }
    });

    private void prepareForConnection() {
        Intent vpnServicePrepareIntent = VpnService.prepare(this);
        if (vpnServicePrepareIntent != null) {
            activityResultLauncher.launch(vpnServicePrepareIntent);
        }
    }

    private void showLog() {
        startActivity(new Intent(this, LogActivity.class));
        // TODO: Implement this method
    }

    private JSONObject getServer() throws JSONException {
        JSONArray array = servers;
        for (int i = 0; i < array.length(); i++) {
            JSONObject server = array.getJSONObject(i);
            if (server.getString("Name").equals(SpinUtil.get_spinner_selected_item(profile_spin))) {
                return server;
            }
        }
        // TODO: Implement this method
        return null;
    }

    private RequestQueue requestQueue;

    private void showExpireDate() {
        String format = myPrefs.getString("auth_api", auth_api);

        String user = myPrefs.getString(USERNAME, "");
        String pass = myPrefs.getString(PASSWORD, "");

        if (user.isEmpty() || pass.isEmpty()) {
            //      if (user.isEmpty()) {
            return;
        }

        String model = Build.MODEL;
        String id = getHWID();

        String jsonUrl = String.format(format, user, pass, id, model);
        //    String jsonUrl = String.format(format, user, id, model);

        //((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setText(jsonUrl);

        try {
            if (requestQueue == null) {
                requestQueue = Volley.newRequestQueue(this);
            }
            StringRequest req = new StringRequest(jsonUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            try {
                                JSONObject js = new JSONObject(response);
                                if (js.getString("device_match").equals("none")) {
                                    stopVPN();
                                    showAuthFailedDialog();
                                    //    showToast("Invalid username/password");
                                    logout();
                                    return;
                                }
                                if (js.getString("device_match").equals("false")) {
                                    stopVPN();
                                    //showDeviceIdNotMatch();
                                    showToast("This account already used in another device");
                                    logout();
                                    return;
                                }
                                onExpireDate(js.getString("expiry"));
                            } catch (Exception e) {
                                //onError(e.getClass().getSimpleName() + ": " +e.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onError("Expire Date: " + error.getMessage());
                }
            });
            requestQueue.add(req);
        } catch (Exception ignored) {
        }
    }

    public static String getHWID() {
        return md5(Build.SERIAL + Build.BOARD.length() % 5 + Build.BRAND.length() % 5 + Build.DEVICE.length() % 5 + Build.MANUFACTURER.length() % 5 + Build.MODEL.length() % 5 + Build.PRODUCT.length() % 5 + Build.HARDWARE).toUpperCase(Locale.getDefault());
    }

    public static final String md5(String str) {
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

    void showAuthFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.technore_dialog);
        View inflate = getLayoutInflater().inflate(R.layout.dialog_auth_failed, null);
        builder.setView(inflate);
        AlertDialog create = builder.create();
        create.setCancelable(true);
        (inflate.findViewById(R.id.reset)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(USERNAME, "").apply();
                editor.putString(PASSWORD, "").apply();
                create.dismiss();
            }
        });
        create.show();
    }

    void showDeviceIdNotMatch() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning!");
        builder.setMessage("Account is used in another device, Please recheck your account");
        builder.setPositiveButton("Reset Account", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface p1, int p2) {
                editor.putString(USERNAME, "").apply();
                editor.putString(PASSWORD, "").apply();
            }
        });
        builder.show();
    }

    @Override
    public void onExpireDate(String expiry) {
        TextView date_text = findViewById(R.id.date_text);
        TextView time_text = findViewById(R.id.time_text);
        TextView mExpireDate = findViewById(R.id.expire_date);
        if (mExpireDate == null) {
            return;
        }
        if (expiry.equals("none")) {
            date_text.setText("Date: --/--/--");
            time_text.setText("Time: --/--/--");
            mExpireDate.setText("Expiry: -- Days Left");
        } else {
            expiry = convertedDateAndTime(expiry);
            date_text.setText(expiry.split(" \\| ")[0]);
            time_text.setText(expiry.split(" \\| ")[1]);
            mExpireDate.setText(expiry.split(" \\| ")[2]);
        }
    }

    @Override
    public void onDeviceNotMatch(String message) {
        stopVPN();

        //Snackbar.make(connect_button, message, Snackbar.LENGTH_LONG).show();
        // TODO: Implement this method
    }

    private String convertedDateAndTime(String str) {
        String str2;
        String[] split = str.split(" ");
        String[] split2 = split[0].split("-");
        switch (Integer.valueOf(split2[1]).intValue()) {
            case 1:
                str2 = "January ";
                break;
            case 2:
                str2 = "February ";
                break;
            case 3:
                str2 = "March ";
                break;
            case 4:
                str2 = "April ";
                break;
            case 5:
                str2 = "May ";
                break;
            case 6:
                str2 = "June ";
                break;
            case 7:
                str2 = "July ";
                break;
            case 8:
                str2 = "August ";
                break;
            case 9:
                str2 = "Septemper ";
                break;
            case 10:
                str2 = "October ";
                break;
            case 11:
                str2 = "November ";
                break;
            case 12:
                str2 = "December ";
                break;
            default:
                str2 = "";
                break;
        }
        String stringBuilder = str2 +
                split2[2] +
                ", " +
                split2[0] +
                " | " +
                split[1] +
                " | " +
                RemainDays(split[0]) +
                " Days Left";
        return stringBuilder;
    }

    private long RemainDays(String str) {
        String[] split = str.split(" ")[0].split("-");
        Calendar instance = Calendar.getInstance();
        instance.set(Integer.valueOf(split[0]).intValue(), Integer.valueOf(split[1]).intValue() - 1, Integer.valueOf(split[2]).intValue());
        return (instance.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 86400000;
    }

    private String getDaysLeft(String thatDate) {
        if (thatDate.contains(" ")) {
            thatDate = thatDate.split(" ")[0];
        }
        String[] split = thatDate.split("-");
        Calendar instance = Calendar.getInstance();
        instance.set(Integer.valueOf(split[0]).intValue(), Integer.valueOf(split[1]).intValue() - 1, Integer.valueOf(split[2]).intValue());
        return String.format("Expiry: %s Days Left", (instance.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / ((long) 86400000));
    }

    @Override
    public void onAuthFailed(String message) {
        stopVPN();
        status_view.setTextColor(Color.RED);
        status_view.setText("Invalid Account!");
        //Snackbar.make(connect_button, "Invalid Account!", Snackbar.LENGTH_LONG).show();
        // TODO: Implement this method
    }

    @Override
    public void onError(String error) {
        //showExpireDate();
        //Snackbar.make(connect_button, error, Snackbar.LENGTH_LONG).show();
        // TODO: Implement this method
    }

    @Override
    public void startOpenVPN() {
        if (protocol == 0) {
            start_connect();
        } else {
            connect();
        }
        super.startOpenVPN();
    }

    private void connect() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                Log.d(TAG, "CLI: requesting VPN actor rights");
                startActivityForResult(intent, 123);
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "CLI: requesting VPN actor rights failed", e);
                return;
            }
        }
        onActivityResult(123, Activity.RESULT_OK, null);
    }

    private void start_connect() {
        cancel_ui_reset();
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                Log.d(TAG, "CLI: requesting VPN actor rights");
                startActivityForResult(intent, S_BIND_CALLED);
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "CLI: requesting VPN actor rights failed", e);
                ok_dialog(resString(R.string.vpn_permission_dialog_missing_title), resString(R.string.vpn_permission_dialog_missing_text));
                return;
            }
        }
        Log.d(TAG, "CLI: app is already authorized as VPN actor");
        resolve_epki_alias_then_connect();
    }

    public boolean onTouch(View v, MotionEvent event) {
        boolean new_expand_stats = RETAIN_AUTH;
        if (v.getId() != R.id.conn_details_boxed || event.getAction() != 0) {
            return RETAIN_AUTH;
        }
        if (!this.prefs.get_boolean("expand_stats", RETAIN_AUTH)) {
            new_expand_stats = true;
        }
        this.prefs.set_boolean("expand_stats", new_expand_stats);
        set_visibility_stats_expansion_group();
        return true;
    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        cancel_ui_reset();
        int viewid = parent.getId();
        if (viewid == R.id.profile) {
            ui_setup(is_active(), 327680, null);
            try {
                String server_name = listProfiles.get(position).getString("Name");
                editor.putString(SELECTED_PROFILE, server_name).apply();
                editor.putInt("MyProfile", position).apply();
            } catch (Exception e) {

            }
        } else if (viewid == R.id.proxy) {
            ProxyList proxy_list = get_proxy_list();
            if (proxy_list != null) {
                proxy_list.set_enabled(SpinUtil.get_spinner_list_item(this.proxy_spin, position));
                proxy_list.save();
                gen_ui_reset_event(true);
            }
        } else if (viewid == R.id.server) {
            String server = SpinUtil.get_spinner_list_item(this.server_spin, position);
            this.prefs.set_string_by_profile(SpinUtil.get_spinner_selected_item(this.profile_spin), "server", server);
            gen_ui_reset_event(true);
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void menu_add(ContextMenu menu, int id, boolean enabled, String menu_key) {
        MenuItem item = menu.add(0, id, 0, id).setEnabled(enabled);
        if (menu_key != null) {
            item.setIntent(new Intent().putExtra("net.openvpn.openvpn.MENU_KEY", menu_key));
        }
    }

    private String get_menu_key(MenuItem item) {
        if (item != null) {
            Intent intent = item.getIntent();
            if (intent != null) {
                return intent.getStringExtra("net.openvpn.openvpn.MENU_KEY");
            }
        }
        return null;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        boolean z = RETAIN_AUTH;
        Log.d(TAG, "CLI: onCreateContextMenu");
        super.onCreateContextMenu(menu, v, menuInfo);
        int viewid = v.getId();
        if (!is_active() && (viewid == R.id.profile || viewid == R.id.profile_edit)) {
            Profile prof = selected_profile();
            if (prof != null) {
                String profile_name = prof.get_name();
                menu.setHeaderTitle(profile_name);
                if (SpinUtil.get_spinner_count(this.profile_spin) > S_BIND_CALLED) {
                    z = true;
                }
                menu_add(menu, R.string.profile_context_menu_change_profile, z, null);
                menu_add(menu, R.string.profile_context_menu_create_shortcut, true, profile_name);
                menu_add(menu, R.string.profile_context_menu_delete, prof.is_deleteable(), profile_name);
                menu_add(menu, R.string.profile_context_menu_rename, prof.is_renameable(), profile_name);
                menu_add(menu, R.string.profile_context_forget_creds, true, profile_name);
            } else {
                menu.setHeaderTitle(R.string.profile_context_none_selected);
            }
            menu_add(menu, R.string.profile_context_cancel, true, null);
        } else if (!is_active()) {
            if (viewid == R.id.proxy || viewid == R.id.proxy_edit) {
                ProxyList proxy_list = get_proxy_list();
                if (proxy_list != null) {
                    String proxy_name = proxy_list.get_enabled(true);
                    boolean is_none = proxy_list.is_none(proxy_name);
                    menu.setHeaderTitle(proxy_name);
                    menu_add(menu, R.string.proxy_context_change_proxy, SpinUtil.get_spinner_count(this.proxy_spin) > S_BIND_CALLED || RETAIN_AUTH, null);
                    menu_add(menu, R.string.proxy_context_edit, !is_none || RETAIN_AUTH, proxy_name);
                    if (!is_none) {
                        z = true;
                    }
                    menu_add(menu, R.string.proxy_context_delete, z, proxy_name);
                    menu_add(menu, R.string.proxy_context_forget_creds, proxy_list.has_saved_creds(proxy_name), proxy_name);
                } else {
                    menu.setHeaderTitle(R.string.proxy_context_none_selected);
                }
                menu_add(menu, R.string.proxy_context_cancel, true, null);
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "CLI: onContextItemSelected");
        String prof_name;
        String proxy_name;
        int itemId = item.getItemId();/*2131034278*/
        if (itemId == R.string.profile_context_cancel || itemId == R.string.proxy_context_cancel) { /*2131034308*/
            return true;
        } else if (itemId == R.string.profile_context_forget_creds) { /*2131034279*/
            ProfileList proflist = profile_list();
            if (proflist == null) {
                return true;
            }
            Profile prof = proflist.get_profile_by_name(get_menu_key(item));
            if (prof == null) {
                return true;
            }
            prof_name = prof.get_name();
            this.pwds.remove("pk", prof_name);
            this.pwds.remove("auth", prof_name);
            prof.forget_cert();
            ui_setup(is_active(), UIF_RESET, null);
            return true;
        } else if (itemId == R.string.profile_context_menu_change_profile) { /*2131034280*/
            this.profile_spin.performClick();
            return true;
        } else if (itemId == R.string.profile_context_menu_create_shortcut) { /*2131034281*/
            prof_name = get_menu_key(item);
            if (prof_name == null) {
                return true;
            }
            launch_create_profile_shortcut_dialog(prof_name);
            return true;
        } else if (itemId == R.string.profile_context_menu_delete) { /*2131034282*/
            prof_name = get_menu_key(item);
            if (prof_name == null) {
                return true;
            }
            submitDeleteProfileIntentWithConfirm(prof_name);
            return true;
        } else if (itemId == R.string.profile_context_menu_rename) { /*2131034283*/
            prof_name = get_menu_key(item);
            if (prof_name == null) {
                return true;
            }
            launch_rename_profile_dialog(prof_name);
            return true;
        } else if (itemId == R.string.proxy_context_change_proxy) { /*2131034309*/
            this.proxy_spin.performClick();
            return true;
        } else if (itemId == R.string.proxy_context_delete) { /*2131034310*/
            delete_proxy_with_confirm(get_menu_key(item));
            return true;
        } else if (itemId == R.string.proxy_context_forget_creds) { /*2131034313*/
            proxy_name = get_menu_key(item);
            ProxyList proxy_list = get_proxy_list();
            if (proxy_list == null) {
                return true;
            }
            proxy_list.forget_creds(proxy_name);
            proxy_list.save();
            return true;
        }
        return RETAIN_AUTH;
    }

    private void launch_create_profile_shortcut_dialog(final String prof_name) {
        View view = getLayoutInflater().inflate(R.layout.create_shortcut_dialog, null);
        final EditText name_field = view.findViewById(R.id.shortcut_name);
        name_field.setText(prof_name);
        name_field.selectAll();
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case -1:
                        OpenVPNClient.this.createConnectShortcut(prof_name, name_field.getText().toString());
                        return;
                    default:
                }
            }
        };
        new Builder(this).setTitle(R.string.create_shortcut_title).setView(view).setPositiveButton(R.string.create_shortcut_yes, dialogClickListener).setNegativeButton(R.string.create_shortcut_cancel, dialogClickListener).show();
    }

    private void launch_rename_profile_dialog(final String orig_prof_name) {
        View view = getLayoutInflater().inflate(R.layout.rename_profile_dialog, null);
        final EditText name_field = view.findViewById(R.id.rename_profile_name);
        name_field.setText(orig_prof_name);
        name_field.selectAll();
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case -1:
                        OpenVPNClient.this.submitRenameProfileIntent(orig_prof_name, name_field.getText().toString());
                        return;
                    default:
                }
            }
        };
        new Builder(this).setTitle(R.string.rename_profile_title).setView(view).setPositiveButton(R.string.rename_profile_yes, dialogClickListener).setNegativeButton(R.string.rename_profile_cancel, dialogClickListener).show();
    }

    private void delete_proxy_with_confirm(final String proxy_name) {
        final ProxyList proxy_list = get_proxy_list();
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case -1:
                        if (proxy_list != null) {
                            proxy_list.remove(proxy_name);
                            proxy_list.save();
                            OpenVPNClient.this.gen_ui_reset_event(OpenVPNClient.RETAIN_AUTH);
                            return;
                        }
                        return;
                    default:
                }
            }
        };
        new Builder(this).setTitle(R.string.proxy_delete_confirm_title).setMessage(proxy_name).setPositiveButton(R.string.proxy_delete_confirm_yes, dialogClickListener).setNegativeButton(R.string.proxy_delete_confirm_cancel, dialogClickListener).show();
    }


    @SuppressLint({"WrongConstant", "UnsafeIntentLaunch"})
    public PendingIntent get_configure_intent(int requestCode) {
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE | 268435456 : 268435456;
        return PendingIntent.getActivity(this, requestCode, getIntent(), flags);
    }

    private void resolve_epki_alias_then_connect() {
        resolveExternalPkiAlias(selected_profile(), new EpkiPost() {
            public void post_dispatch(String alias) {
                OpenVPNClient.this.do_connect(alias);
            }
        });
    }

    private void do_connect(String epki_alias) {
        String app_name = "net.openvpn.connect.android";
        String proxy_name = null;
        String server = null;
        String username = myPrefs.getString("X_USERNAME", "");
        String password = myPrefs.getString("X_PASSWORD", "");
        String pk_password = null;
        String response = null;
        boolean is_auth_pwd_save = RETAIN_AUTH;
        String profile_name = selected_profile_name();
        if (this.proxy_group.getVisibility() == View.VISIBLE) {
            ProxyList proxy_list = get_proxy_list();
            if (proxy_list != null) {
                proxy_name = proxy_list.get_enabled(RETAIN_AUTH);
            }
        }
        if (this.server_group.getVisibility() == View.VISIBLE) {
            server = SpinUtil.get_spinner_selected_item(this.server_spin);
        }

        if (this.pk_password_group.getVisibility() == View.VISIBLE) {
            pk_password = this.pk_password_edit.getText().toString();
            boolean is_pk_pwd_save = this.pk_password_save_checkbox.isChecked();
            this.prefs.set_boolean_by_profile(profile_name, "pk_password_save", is_pk_pwd_save);
            if (is_pk_pwd_save) {
                this.pwds.set("pk", profile_name, pk_password);
            } else {
                this.pwds.remove("pk", profile_name);
            }
        }

        if (this.cr_group.getVisibility() == View.VISIBLE) {
            response = this.response_edit.getText().toString();
        }
        clear_auth();
        String vpn_proto = this.prefs.get_string("vpn_proto");
        String ipv6 = this.prefs.get_string("ipv6");
        String conn_timeout = this.prefs.get_string("conn_timeout");
        String compression_mode = this.prefs.get_string("compression_mode");
        clear_stats();

        submitConnectIntent(profile_name, server, vpn_proto, ipv6, conn_timeout, username, password, is_auth_pwd_save, pk_password, response, epki_alias, compression_mode, proxy_name, null, null, true, get_gui_version(app_name));
    }


    private void import_profile(String path) {
        submitImportProfileViaPathIntent(path);
    }

    protected void onActivityResult(int request, int result, Intent data) {
        String str = TAG;
        Object[] objArr = new Object[S_ONSTART_CALLED];
        objArr[0] = Integer.valueOf(request);
        objArr[S_BIND_CALLED] = Integer.valueOf(result);
        Log.d(str, String.format("CLI: onActivityResult request=%d result=%d", objArr));
        String path;

        switch (request) {
            case 0101:
                TunnelManagerHelper.startSocksHttp(this);
                break;
            case 122:
                psiphonHelper.startTunnelService();
                break;
            case 123:
                if (protocol == 1) {
                    Intent intent = new Intent(this, OpenVpnService.class);
                    intent.putExtra(OpenVpnService.EXTRA_UUID, ProfileManager.create("127.0.0.1").getUUID().toString());
                    startService(intent);
                } else {
                    Intent intent = new Intent(this, SSHService.class);
                    intent.setAction(SSHService.START_SSH);
                    startService(intent);
                }
                break;
            case SelectNetworkActivity.SELECT_NETWORK_CODE:
                if (RESULT_OK == result) {
                    setSelectedNetworkInfo();
                }
                break;
            case S_BIND_CALLED /*1*/:
                if (result == -1) {
                    resolve_epki_alias_then_connect();
                    return;
                } else if (result != 0) {
                    return;
                } else {
                    if (this.finish_on_connect == FinishOnConnect.ENABLED) {
                        finish();
                        return;
                    } else if (this.finish_on_connect == FinishOnConnect.ENABLED_ACROSS_ONSTART) {
                        this.finish_on_connect = FinishOnConnect.ENABLED;
                        start_connect();
                        return;
                    } else {
                        return;
                    }
                }
            case S_ONSTART_CALLED /*2*/:
                if (result == -1) {
                    path = data.getStringExtra(FileDialog.RESULT_PATH);
                    str = TAG;
                    objArr = new Object[S_BIND_CALLED];
                    objArr[0] = path;
                    Log.d(str, String.format("CLI: IMPORT_PROFILE: %s", objArr));
                    import_config(path);
                    return;
                }
                return;
            case REQUEST_IMPORT_PKCS12 /*3*/:
                if (result == -1) {
                    path = data.getStringExtra(FileDialog.RESULT_PATH);
                    str = TAG;
                    objArr = new Object[S_BIND_CALLED];
                    objArr[0] = path;
                    Log.d(str, String.format("CLI: IMPORT_PKCS12: %s", objArr));
                    import_pkcs12(path);
                    return;
                }
                return;
            default:
                super.onActivityResult(request, result, data);
        }
    }

    private void startDNS(String dnsAddress, String publicKey, String nameServer) {
        new Thread(new Runnable() {
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getApplicationInfo().nativeLibraryDir).append("/libstartdns.so");
                stringBuilder.append(" -udp " + dnsAddress + ":53   -pubkey " + publicKey + " " + nameServer + " 127.0.0.1:2222");
                try {
                    Runtime.getRuntime().exec(stringBuilder.toString());
                } catch (IOException e) {
                }
            }
        }).start();
    }

    private void stopDNS() {
        File file = CustomNativeLoader.loadNativeBinary(this, "libstartdns", new File(this.getFilesDir(), "libstartdns"));
        if (file != null) {
            try {
                KillThis.killProcess(file);
            } catch (Exception e) {
            }
        }
    }

    private void import_config(String path) {
        try {
            File file = new File(path);
            if (file.getPath().endsWith(".ovpn")) {
                ConfigParser parser = new ConfigParser();
                parser.parseConfig(new InputStreamReader(new FileInputStream(path)));
                VpnProfile vp = parser.convertProfile();
                vp.mName = file.getName();
                if (vp.mConnections[0].mUseCustomConfig) {
                    String proxy = "http-proxy-retry 1\nhttp-proxy 127.0.0.1 8989";
                    vp.mConnections[0].mCustomConfiguration = proxy;
                }
                String file_name = vp.mName;
                String vp_content = String.format("imported\n%s", vp.getConfigFile(this, false));
                if (getOpenVPNService() != null) {
                    getOpenVPNService().addProfile(file_name, vp_content);
                }
                showToast("Import Success!");
            }
        } catch (Exception e) {
            showToast("Import Profile Error: " + e.getMessage());
        }
        // TODO: Implement this method
    }

    private TextView last_visible_edittext() {
        for (int i = 0; i < this.textgroups.length; i += S_BIND_CALLED) {
            if (this.textgroups[i].getVisibility() == View.VISIBLE) {
                return this.textviews[i];
            }
        }
        return null;
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
       /* if (v != last_visible_edittext()) {
            return RETAIN_AUTH;
        }
        if (action_enter(actionId, event) && this.connect_button.getVisibility() == 0) {
            onClick(this.connect_button);
        }*/
        return true;
    }

    private void req_focus(EditText editText) {
        /*boolean auto_keyboard = this.prefs.get_boolean("auto_keyboard", RETAIN_AUTH);
        if (editText != null) {
            editText.requestFocus();
            if (auto_keyboard) {
                raise_keyboard(editText);
                return;
            }
            return;
        }
        this.main_scroll_view.requestFocus();
        if (auto_keyboard) {
            dismiss_keyboard();
        }*/
    }

    private void raise_keyboard(EditText editText) {
        /*InputMethodManager mgr = (InputMethodManager) getSystemService("input_method");
        if (mgr != null) {
            mgr.showSoftInput(editText, S_BIND_CALLED);
        }*/
    }

    private void dismiss_keyboard() {
       /* InputMethodManager mgr = (InputMethodManager) getSystemService("input_method");
        if (mgr != null) {
            TextView[] textViewArr = this.textviews;
            int length = textViewArr.length;
            for (int i = 0; i < length; i += S_BIND_CALLED) {
                mgr.hideSoftInputFromWindow(textViewArr[i].getWindowToken(), 0);
            }
        }*/
    }

    private void load_ui_elements() {
        this.status_01 = findViewById(R.id.status_01);
        this.main_scroll_view = findViewById(R.id.main_scroll_view);
        this.post_import_help_blurb = findViewById(R.id.post_import_help_blurb);
        this.profile_group = findViewById(R.id.profile_group);
        this.proxy_group = findViewById(R.id.proxy_group);
        this.server_group = findViewById(R.id.server_group);
        this.username_group = findViewById(R.id.username_group);
        this.password_group = findViewById(R.id.password_group);
        this.pk_password_group = findViewById(R.id.pk_password_group);
        this.cr_group = findViewById(R.id.cr_group);
        this.conn_details_group = findViewById(R.id.conn_details_group);
        this.stats_group = findViewById(R.id.stats_group);
        this.stats_expansion_group = findViewById(R.id.stats_expansion_group);
        this.info_group = findViewById(R.id.info_group);
        this.button_group = findViewById(R.id.button_group);
        this.profile_spin = findViewById(R.id.profile);
        this.profile_edit = findViewById(R.id.profile_edit);
        this.proxy_spin = findViewById(R.id.proxy);
        this.proxy_edit = findViewById(R.id.proxy_edit);
        this.server_spin = findViewById(R.id.server);
        this.challenge_view = findViewById(R.id.challenge);
        this.username_edit = findViewById(R.id.username);
        this.password_edit = findViewById(R.id.password);
        this.pk_password_edit = findViewById(R.id.pk_password);
        this.response_edit = findViewById(R.id.response);
        this.password_save_checkbox = findViewById(R.id.password_save);
        this.pk_password_save_checkbox = findViewById(R.id.pk_password_save);
        this.status_view = findViewById(R.id.status);
        this.status_icon_view = findViewById(R.id.status_icon);
        this.progress_bar = findViewById(R.id.progress);
        this.connect_button = findViewById(R.id.connect);
        this.disconnect_button = findViewById(R.id.disconnect);
        this.details_more_less = findViewById(R.id.details_more_less);
        this.last_pkt_recv_view = findViewById(R.id.last_pkt_recv);
        this.duration_view = findViewById(R.id.duration);
        this.bytes_in_view = findViewById(R.id.bytes_in);
        this.bytes_out_view = findViewById(R.id.bytes_out);
        this.connect_button.setOnClickListener(this);
        this.disconnect_button.setOnClickListener(this);
        this.profile_spin.setOnItemSelectedListener(this);
        this.proxy_spin.setOnItemSelectedListener(this);
        this.server_spin.setOnItemSelectedListener(this);
        registerForContextMenu(this.profile_spin);
        registerForContextMenu(this.proxy_spin);
        findViewById(R.id.conn_details_boxed).setOnTouchListener(this);
        this.profile_edit.setOnClickListener(this);
        registerForContextMenu(this.profile_edit);
        this.proxy_edit.setOnClickListener(this);
        registerForContextMenu(this.proxy_edit);
        this.username_edit.setOnEditorActionListener(this);
        this.password_edit.setOnEditorActionListener(this);
        this.pk_password_edit.setOnEditorActionListener(this);
        this.response_edit.setOnEditorActionListener(this);
        this.textgroups = new View[]{this.cr_group, this.password_group, this.pk_password_group, this.username_group};
        this.textviews = new EditText[]{this.response_edit, this.password_edit, this.pk_password_edit, this.username_edit};
    }

    public void setBottomLayout() {
        LinearLayout updatebtn = findViewById(R.id.b_update);
        LinearLayout tweaksbtn = findViewById(R.id.b_tweaks);
        LinearLayout privacybtn = findViewById(R.id.b_privacy);
        LinearLayout exitbtn = findViewById(R.id.b_exit);

        findViewById(R.id.b_logs).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OpenVPNClient.this, LogActivity.class));
            }
        });

        updatebtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(OpenVPNClient.this,LogActivity.class));
                showUpdateDialog();
            }
        });

        tweaksbtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                customTweak();
            }
        });

        privacybtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OpenVPNClient.this, PrivacyActivity.class));
            }
        });

        exitbtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
               /* AlertDialog.Builder ab = new AlertDialog.Builder(OpenVPNClient.this);
                ab.setMessage("Do you want to minimize or exit?");
                ab.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                ab.setNegativeButton("Minimize", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                    }
                });
                ab.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopVPN();
                        System.exit(0);
                    }
                });
                ab.show();

                */

                AlertDialog.Builder ab = new AlertDialog.Builder(OpenVPNClient.this);
                ab.setTitle("User Policy and Agreement");
                ab.setIcon(R.drawable.ic_launcher);
                ab.setMessage("These terms and conditions outline the rules and regulations for the use of our Software. By accessing this Software we assume you accept these terms and conditions. Do not continue to use our service if you do not agree to take all of the terms and conditions stated on this page. The following terminology applies to these Terms and Conditions, Privacy Statement and Disclaimer Notice and all Agreements: Client, You and Your refers to you, the person log on this Software and compliant to the Company's terms and conditions. The Company, Ourselves, We, Our and Us, refers to our Company. Party, Parties, or Us, refers to both the Client and ourselves. All terms refer to the offer, acceptance and consideration of payment necessary to undertake the process of our assistance to the Client in the most appropriate manner for the express purpose of meeting the Client's needs in respect of provision of the Company's stated services, in accordance with and subject to, prevailing law of Netherlands. Any use of the above terminology or other words in the singular, plural, capitalization and/or he/she or they, are taken as interchangeable and therefore as referring to same.\n\nDeveloped by SM-Build");
                ab.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                ab.show();
            }
        });
    }

    @SuppressLint({"MissingSuperCall", "GestureBackNavigation"})
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    public void showNotice() {

        try {
            StringRequest req = new StringRequest(myPrefs.getString("notice_api", ""),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            myPrefs.edit().putString("new_notice", response).apply();
                            String newNotif = myPrefs.getString("new_notice", "");
                            String oldNotif = myPrefs.getString("old_notice", "");

                            if (!newNotif.equals(oldNotif)) {

                                View view = LayoutInflater.from(OpenVPNClient.this).inflate(R.layout.layout_dialog, null);

                                AlertDialog alertDialog = new AlertDialog.Builder(OpenVPNClient.this)
                                        .setView(view)
                                        .show();

                                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                                ((TextView) view.findViewById(R.id.title_text)).setText("Notification!");
                                ((TextView) view.findViewById(R.id.desc_text)).setText(response);
                                ((TextView) view.findViewById(R.id.positive_text)).setText("Okay");

                                view.findViewById(R.id.close_icon).setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        alertDialog.dismiss();
                                    }
                                });

                                view.findViewById(R.id.positive_button).setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        myPrefs.edit().putString("old_notice", response).apply();
                                        alertDialog.dismiss();
                                    }
                                });
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // onError("Expire Date: "+ error.getMessage());
                }

            });
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(req);
        } catch (Exception e) {
        }
    }

    private void loadBanners() {

        List<String> newUrls = Arrays.asList(
                myPrefs.getString("banner1_url", "").trim(),
                myPrefs.getString("banner2_url", "").trim(),
                myPrefs.getString("banner3_url", "").trim()
        );

        bannerUrls.clear();
        for (String url : newUrls) {
            if (!url.isEmpty()) {
                bannerUrls.add(url);
            }
        }

        if (!bannerUrls.isEmpty()) {
            currentIndex = 0;
            startBanners();
        }
    }

    private void startBanners() {
        if (bannerRunnable != null) {
            handler.removeCallbacks(bannerRunnable);
        }
        if (!bannerUrls.isEmpty()) {
            bannerImage.setVisibility(View.VISIBLE);
        }
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!bannerUrls.isEmpty()) {
                    Glide.with(getApplicationContext())
                            .load(bannerUrls.get(currentIndex))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(bannerImage);

                    currentIndex = (currentIndex + 1) % bannerUrls.size();
                }
                handler.postDelayed(this, BANNER_INTERVAL);
            }
        };
        handler.post(bannerRunnable);
    }

    public void startUDP() {
        try {
            TextView subtitle = findViewById(R.id.status);
            subtitle.setText("UDP Connecting...");
            ConfigUtil config = ConfigUtil.getInstance(this);
            config.setUDPConfig(config.getPayload().replace("auth_xxx", myPrefs.getString("X_USERNAME", "") + ":" + myPrefs.getString("X_PASSWORD", "")));
            launchVPN();
        } catch (Exception e) {
            Toast.makeText(OpenVPNClient.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void startPsiphon() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                startActivityForResult(intent, 122);
            } catch (ActivityNotFoundException ane) {
            }
        } else {
            onActivityResult(122, Activity.RESULT_OK, null);
        }
    }

    public void stopUdp() {
        stopTimer();
        TextView subtitle = findViewById(R.id.status);
        subtitle.setText("Disconnected");
        TunnelManagerHelper.stopSocksHttp(OpenVPNClient.this);
        //stopService(new Intent(this, SocksDNSService.class));
    }

    private void stopTimer() {
        /*if (timerService != null) {
            timerService.stop();
            timerService.stopNotification();
            stopService(new Intent(this, TimerService.class));
        }*/
    }

    private void launchVPN() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            SkStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, 0101);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                SkStatus.logError(R.string.no_vpn_support_image);

            }
        } else {
            onActivityResult(0101, Activity.RESULT_OK, null);
        }

    }

    public static String encrypt(String str) {
        return TeaBase64.encryptToBase64String(str, "s!02a");
    }

    public static String decrypt(String str) {
        return TeaBase64.decryptBase64StringToString(str, "s!02a");
    }

    public void runBanner() {
        if (!consentInformation.canRequestAds()) {
            return;
        }
        if (mAdView != null && mAdView.getResponseInfo() == null) {
            mAdView.loadAd(new AdRequest.Builder().build());
        }
    }

    public void runRewardedAd() {
        if (!consentInformation.canRequestAds()) {
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, myPrefs.getString("rewarded_ad", rewardedAdsID),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;

                    }
                });
    }

    public void showRewardedAd() {
        if (!consentInformation.canRequestAds()) {
            return;
        }
        if (mRewardedAd != null) {
            mRewardedAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    runRewardedAd();
                }
            });
        } else {
            runRewardedAd();

        }
    }

    public void runInterstitialAds(boolean show) {
        if (!consentInformation.canRequestAds()) {
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, myPrefs.getString("interstitial_ad", interstitialID), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.

                        mInterstitialAd = interstitialAd;
                        if (show) {
                            showInterstitialAds();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error

                        mInterstitialAd = null;
                    }
                });
    }

    public void showInterstitialAds() {
        if (!consentInformation.canRequestAds()) {
            return;
        }
        if (mInterstitialAd != null) {
            mInterstitialAd.show(OpenVPNClient.this);
            mInterstitialAd = null;
        } else {
            runInterstitialAds(true);
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
    }
}


