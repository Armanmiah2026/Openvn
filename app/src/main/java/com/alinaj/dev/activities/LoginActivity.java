package com.alinaj.dev.activities;

import static com.alinaj.dev.activities.OpenVPNClient.decrypt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.alinaj.dev.json.JsonManager;
import com.alinaj.dev.utils.ConfigUtil;
import com.alinaj.dev.view.MaterialEditText;
import com.alinaj.dev.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class LoginActivity extends OpenVPNClientBase implements OnClickListener {

    private EditText mUsername;
    private EditText mPassword;
    private Button loginBtn;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic("all");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        mUsername = findViewById(R.id.login_username);
        mPassword = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.loginBtn);

        mUsername.setText(prefs.getString(OpenVPNClient.USERNAME, ""));
        mPassword.setText(prefs.getString(OpenVPNClient.PASSWORD, ""));
        loginBtn.setOnClickListener(this);
        checkUpdates();

        if (prefs.getBoolean("isLogin", false)) {
            startActivity(new Intent(getApplicationContext(), OpenVPNClient.class));
            finish();
        }
    }

    @Override
    public void onClick(View p1) {
        doLogin();
    }

    private void doLogin() {

        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();

        if (username.isEmpty()) {
            Toast.makeText(this, "Username is empty!", Toast.LENGTH_SHORT).show();
        } else if (password.isEmpty()) {
            Toast.makeText(this, "Password is empty!", Toast.LENGTH_SHORT).show();
        } else {

            editor.putString(OpenVPNClient.USERNAME, username);
            editor.putString(OpenVPNClient.PASSWORD, password);
            editor.putBoolean("isLogin", true);
            editor.apply();

            startActivity(new Intent(getApplicationContext(), OpenVPNClient.class));
            finish();

            /*ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected()) {
                Toast.makeText(this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Checking...", Toast.LENGTH_SHORT).show();

            String format = "https://rktunnelvip.xyz/api/auth.php?username=%s&password=%s&device_id=%s&device_model=%s";

            format = prefs.getString("auth_api", format);

            String model = Build.MODEL;
            String id = getHWID();

            String jsonUrl = String.format(format, username, password, id, model);
            //String jsonUrl = String.format(format, user, id, model);

            StringRequest req = new StringRequest(jsonUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            try {
                                JSONObject js = new JSONObject(response);
                                if (js.getString("device_match").equals("none")) {
                                    showAuthFailedDialog();
                                    //Toast.makeText(LoginActivity.this, "Wrong username or password!, Please recheck your account", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (js.getString("device_match").equals("false")) {
                                    Toast.makeText(LoginActivity.this, "Account is used in another device, Please recheck your account", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                editor.putString(OpenVPNClient.USERNAME, username);
                                editor.putString(OpenVPNClient.PASSWORD, password);
                                editor.putBoolean("isLogin", true);
                                editor.apply();

                                startActivity(new Intent(getApplicationContext(), OpenVPNClient.class));
                                finish();
                            } catch (Exception e) {}
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(LoginActivity.this, "Network error!", Toast.LENGTH_SHORT).show();
                }
            });
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(req);*/
        }
    }

    private void showAuthFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.technore_dialog);
        View inflate = getLayoutInflater().inflate(R.layout.dialog_auth_failed, null);
        builder.setView(inflate);
        AlertDialog create = builder.create();
        create.setCancelable(true);
        inflate.findViewById(R.id.reset).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(OpenVPNClient.USERNAME,"").apply();
                editor.putString(OpenVPNClient.PASSWORD,"").apply();
                mUsername.setText("");
                mPassword.setText("");
                create.dismiss();
            }
        });
        create.show();
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

    public void checkUpdates() {

        final JsonManager.ServerUpdate su = new JsonManager.ServerUpdate(this);
        su.setURL(prefs.getString("update_api", "https://penel-demo.ggff.net/api/files/app?json=smtunnel"));
        su.setUpdateListener(new JsonManager.ServerUpdate.OnUpdateListener() {
            @Override
            public void onShowUpdate(JSONObject obj) {
                for (File f: getFilesDir().listFiles()) {
                    if (f.getAbsolutePath().endsWith(".ovpn")) {
                        f.delete();
                    }
                }
                prefs.edit().putString("banner_ad", obj.optString("banner_ad", "")).apply();
                prefs.edit().putString("interstitial_ad", obj.optString("interstitial_ad", "")).apply();
                prefs.edit().putString("rewarded_ad", obj.optString("rewarded_ad", "")).apply();
                prefs.edit().putString("app_open_ad", obj.optString("app_open_ad", "")).apply();
                prefs.edit().putString("auth_api", obj.optString("auth_api", "")).apply();
                if (!obj.optString("update_api", "").isEmpty()) {
                    prefs.edit().putString("update_api", obj.optString("update_api", "")).apply();
                }
                prefs.edit().putString("notice_api", obj.optString("notice_api", "")).apply();
                prefs.edit().putString("banner1_url", obj.optString("banner1_url", "")).apply();
                prefs.edit().putString("banner2_url", obj.optString("banner2_url", "")).apply();
                prefs.edit().putString("banner3_url", obj.optString("banner3_url", "")).apply();
                ConfigUtil.getInstance(LoginActivity.this).setOVPNCert(decrypt(obj.optString("OVPNCert", "")));
                Toast.makeText(LoginActivity.this, "Updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoUpdateAvailable(String oldVersion) {

            }

            @Override
            public void onUpdateError(String error) {

            }
        });
        try
        {
            su.setCurrentVersion(getJSONObject().getString("Version"));
        } catch (JSONException e)
        {}
        su.start();
    }
}
