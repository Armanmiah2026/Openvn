package com.alinaj.dev.view;

import android.widget.*;
import android.content.*;
import android.view.*;
import android.view.View.*;
import android.widget.AdapterView.*;
import android.preference.*;

import java.net.*;

import com.alinaj.dev.R;
import androidx.appcompat.app.*;

public class PayloadGenerator implements DialogInterface.OnCancelListener, OnClickListener, OnItemSelectedListener, RadioGroup.OnCheckedChangeListener
{

    private View closeDialog;
    private RadioGroup split;
    private AlertDialog dialog;

    public interface GeneratorListener
    {
        void onGeneratePayload(String payload);
        void onGeneratorClose();
    }
    public static final String URL_HOST = "URL_HOST";
    public static final String INJECT_METHOD = "SELECTED_INJECT_METHOD";
    public static final String REQUEST_METHOD = "SELECTED_REQUEST_METHOD";
    public static final String METHOD = "METHOD";
    public static final String QUERY_MODE = "QUERY_MODE";
    public static final String ONLINE_HOST = "ONLINE_HOST";
    public static final String FORWARD_HOST = "FORWARD_HOST";
    public static final String REVERSE_PROXY = "REVERSE_PROXY";
    public static final String KEEP_ALIVE = "KEEP_ALIVE";
    public static final String DUAL_CONNECT = "DUAL_CONNECT";
    public static final String FULL_HOST = "FULL_HOST";
    public static final String DEFAULT_PROXY = "DEFAULT_PROXY";
    public static final String PROXY = "PROXY";
    public static final String PORT = "PORT";
    public static final String PROXY_AUTH = "PROXY_AUTH";
    public static final String FIXED_PAYLOAD = "FIXED_PAYLOAD";
    private final Context context;
    private GeneratorListener listener;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private EditText url_host;
    private Spinner request_method, inject_method;
    private RadioGroup query_mode;
    private CheckBox online_host,forward_host,reverse_proxy,keep_alive,proxy_auth,dual_connect;
    private Button generate_btn;
    public PayloadGenerator(Context context)
    {
        this.context = context;
        init();
    }
    public void setGeneratorListener(GeneratorListener GeneratorListener)
    {
        listener = GeneratorListener;
    }

    private void init()
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        View v = LayoutInflater.from(context).inflate(R.layout.generator_main, null);
        url_host = (EditText)findId(v,R.id.url_host);
        //server = (EditText)v.findViewById(R.id.vpn_server);
        split = (RadioGroup)findId(v, R.id.split_group);
        request_method = (Spinner)findId(v, R.id.request_method);
        inject_method = (Spinner)findId(v,R.id.inject_method);
        query_mode = (RadioGroup)findId(v, R.id.query_mode);
        online_host = (CheckBox)findId(v, R.id.online_host);
        forward_host = (CheckBox)findId(v, R.id.forward_host);
        reverse_proxy = (CheckBox)findId(v, R.id.reverse_proxy);
        keep_alive = (CheckBox)findId(v, R.id.keep_alive);
        proxy_auth = (CheckBox)findId(v, R.id.paygen_proxy_auth);
        dual_connect = (CheckBox)findId(v, R.id.dual_connect);
        generate_btn = (Button)findId(v, R.id.generate_payload);
        closeDialog = findId(v, R.id.close_generator_dialog);
        //server.setText(String.format("%s:%s", vp.mConnections[0].mServerName, vp.mConnections[0].mServerPort));
        request_method.setAdapter(getRequestAdapter());
        inject_method.setAdapter(getInjectAdapter());
        ((RadioButton)query_mode.getChildAt(prefs.getInt(QUERY_MODE, 0))).setChecked(true);
        ((RadioButton)split.getChildAt(prefs.getInt("Split", 2))).setChecked(true);
        url_host.setText(prefs.getString(URL_HOST, ""));
        request_method.setSelection(prefs.getInt(REQUEST_METHOD, 0), false);
        inject_method.setSelection(prefs.getInt(INJECT_METHOD, 0), false);
        online_host.setChecked(prefs.getBoolean(ONLINE_HOST, false));
        forward_host.setChecked(prefs.getBoolean(FORWARD_HOST, false));
        reverse_proxy.setChecked(prefs.getBoolean(REVERSE_PROXY, false));
        keep_alive.setChecked(prefs.getBoolean(KEEP_ALIVE, false));
        proxy_auth.setChecked(prefs.getBoolean(PROXY_AUTH, false));
        dual_connect.setChecked(prefs.getBoolean(DUAL_CONNECT, false));

        split.setOnCheckedChangeListener(this);
        request_method.setOnItemSelectedListener(this);
        inject_method.setOnItemSelectedListener(this);
        query_mode.setOnCheckedChangeListener(this);
        generate_btn.setOnClickListener(this);
        closeDialog.setOnClickListener(this);

        dialog = new AlertDialog.Builder(context).create();
        dialog.setView(v);
        dialog.setOnCancelListener(this);
    }
    public void show()
    {
        dialog.show();
    }
    private View findId(View v, int id)
    {
        return v.findViewById(id);
    }

    @Override
    public void onCancel(android.content.DialogInterface p1)
    {
        listener.onGeneratorClose();
        dialog.dismiss();
        // TODO: Implement this method
    }
    @Override
    public void onClick(View p1)
    {
        String url = url_host.getText().toString();

        if (p1.getId() == R.id.generate_payload) {
            if (url.isEmpty()) {
                showToast("URL/Host is empty");
            } else {
                editor.putString(URL_HOST, url);
                editor.putBoolean(ONLINE_HOST, online_host.isChecked());
                editor.putBoolean(FORWARD_HOST, forward_host.isChecked());
                editor.putBoolean(REVERSE_PROXY, reverse_proxy.isChecked());
                editor.putBoolean(KEEP_ALIVE, keep_alive.isChecked());
                editor.putBoolean(PROXY_AUTH, proxy_auth.isChecked());
                editor.putBoolean(DUAL_CONNECT, dual_connect.isChecked());
                RadioButton rb = split.findViewById(split.getCheckedRadioButtonId());
                int split_selected = split.indexOfChild(rb);
                editor.putInt("Split", split_selected);
                editor.putString(FIXED_PAYLOAD, getPayload());
                editor.apply();
                //getPM().saveProfile(getContext(), profile);
                listener.onGeneratePayload(getPayload());
                dialog.dismiss();
            }
        } else if (p1.getId() == R.id.close_generator_dialog) {
            listener.onGeneratorClose();
            dialog.dismiss();
        }
        // TODO: Implement this method
    }


    @Override
    public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4)
    {
        int id = p1.getId();
        if (id == R.id.request_method) {
            editor.putInt(REQUEST_METHOD, p3).apply();
            editor.putString(METHOD, (String) p1.getSelectedItem()).apply();
        } else if (id == R.id.inject_method) {
            int injection = p3;
            //int split_mode = split.indexOfChild(findViewById(split.getCheckedRadioButtonId()));
            switch (injection) {
                case 0:
                    ((RadioButton) split.getChildAt(2)).setChecked(true);
                    request_method.setSelection(0);
                    break;
                case 1:
                case 2:
                    request_method.setSelection(1);
                    break;
            }
            editor.putInt(INJECT_METHOD, p3).apply();
        }
        // TODO: Implement this method
    }

    @Override
    public void onNothingSelected(AdapterView<?> p1)
    {
        // TODO: Implement this method
    }

    @Override
    public void onCheckedChanged(RadioGroup p1, int p2)
    {
        int id = p1.getId();
        if (id == R.id.query_mode) {
            editor.putInt(QUERY_MODE, p1.indexOfChild(p1.findViewById(p2))).apply();
        } else if (id == R.id.split_group) {
            int split_mode = p1.indexOfChild(p1.findViewById(p1.getCheckedRadioButtonId()));
            switch (split_mode) {
                case 0:
                case 1:
                    inject_method.setSelection(2);
                    request_method.setSelection(1);
                    break;
                case 2:
                    inject_method.setSelection(0);
            }
        }
        // TODO: Implement this method
    }


    private String getHost()
    {
        String host = String.format("http://%s/", url_host.getText().toString());
        return host;
    }
    private String getHostHeader()
    {
        String host = String.format("http://%s/", url_host.getText().toString());
        try {
            URL url = new URL(host);
            host = url.getHost();
        } catch (Exception e) {

        }
        return host;
    }
    private String getPayload()
    {
        String str = "";
        String url = getHostHeader();
        String server = "[host_port]";
        String crlf = "[crlf]";
        String proto = "[protocol]";
        if (server != null) {
            String host = "Host: " + url + crlf;
            String monline_host = online_host.isChecked() ? "X-Online-Host: " + url + crlf : "";
            String mforward_host = forward_host.isChecked() ? "X-Forward-Host: " + url + crlf : "";
            String mreverse_proxy = reverse_proxy.isChecked() ? "X-Forwarded-For: " + url + crlf : "";
            String mProxyAuth = proxy_auth.isChecked() ? "Proxy-Authorization: [auth]" + crlf : "";
            String mkeep_alive = keep_alive.isChecked() ? "Connection: Keep-Alive" + crlf : "";
            String method = request_method.getSelectedItem() + " " + getHost() + " " + getProtocolByInject() + crlf;
            String mdual_connect = dual_connect.isChecked() ? "CONNECT " + server + " " + "[protocol]" + crlf + crlf : crlf;
            int mQuery = query_mode.indexOfChild(query_mode.findViewById(query_mode.getCheckedRadioButtonId()));
            String split_mode = null;
            if (split.getCheckedRadioButtonId() == R.id.split_instant) {
                split_mode = "[instant_split]";
            } else if (split.getCheckedRadioButtonId() == R.id.split_delay) {
                split_mode = "[delay_split]";
            } else if (split.getCheckedRadioButtonId() == R.id.split_none) {
                split_mode = "";
            }
            switch (inject_method.getSelectedItemPosition()){
                case 0:
                    str = "CONNECT " +  setup(server, mQuery, url, getCrlf()) + host + monline_host + mforward_host + mreverse_proxy + mProxyAuth + mkeep_alive + mdual_connect;
                    break;
                case 1:
                    str = method + host + monline_host + mforward_host + mreverse_proxy + mProxyAuth + mkeep_alive + crlf + split_mode +  "CONNECT " + setup(server, mQuery, url, crlf) + mdual_connect;
                    break;
                case 2:
                    str = "CONNECT " + setup(server, mQuery, url, getCrlf()) + split_mode + method + host + monline_host + mforward_host + mreverse_proxy + mProxyAuth + mkeep_alive + mdual_connect;
                    break;
            }
            return str;
        }
        return null;
    }
    private String setup(String server, int mQuery, String url, String crlf)
    {
        String proto = getProtocolByQuery();
        String str = null;

        switch (mQuery) {
            case 0:
                str = server + " " + proto + crlf;
                break;
            case 1:
                str = url + "@" + server + " " + proto + crlf;
                break;
            case 2:
                str = server + "@" + url + " " + proto + crlf;
                break;
        }
        // TODO: Implement this method
        return str;
    }
    private String getProtocolByInject()
    {
        int injection = inject_method.getSelectedItemPosition();
        int split_mode = split.indexOfChild(split.findViewById(split.getCheckedRadioButtonId()));
        switch (split_mode) {
            case 0:
            case 1:
                return "HTTP/1.1";
            case 2:
                switch (injection) {
                    case 0:
                        return "[protocol]";
                    case 1:
                        return "HTTP/1.1";
                    case 2:
                        return "[protocol]";
                }
                break;
        }

        return null;
    }
    private String getProtocolByQuery()
    {
        int mQuery = query_mode.indexOfChild(query_mode.findViewById(query_mode.getCheckedRadioButtonId()));
        int injection = inject_method.getSelectedItemPosition();
        int split_mode = split.indexOfChild(split.findViewById(split.getCheckedRadioButtonId()));
        if (injection == 0) {
            return  "[protocol]";
        } else if (injection == 1) {
            return "[protocol]";
        } else if (injection == 2) {
            switch (mQuery) {
                case 0:
                    if (split_mode == 2) {
                        return "HTTP/1.1";
                    } else {
                        return "[protocol]";
                    }
                case 1:
                case 2:
                    if (split_mode == 2) {
                        return "HTTP/1.1";
                    } else {
                        return "[protocol]";
                    }
            }
        }
        return null;
    }
    private String getCrlf()
    {
        int injection = inject_method.getSelectedItemPosition();
        int split_mode = split.indexOfChild(split.findViewById(split.getCheckedRadioButtonId()));
        switch (injection) {
            case 0:
                return "[crlf]";
            case 1:
                return "[crlf][crlf]";
            case 2:
                if (split_mode == 2) {
                    return "[crlf][crlf]";
                } else {
                    return "[crlf]";
                }
        }
        return null;
    }
    private ArrayAdapter<String> getInjectAdapter()
    {
        String[] injects = {"Normal","Front Inject","Back Inject"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, injects);
        return adapter;
    }
    private ArrayAdapter<String> getRequestAdapter()
    {
        String[] requests = {"CONNECT","GET","POST","HEAD","PUT","PATCH","DELETE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, requests);
        return adapter;
    }
    private void showToast(String message)
    {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}
