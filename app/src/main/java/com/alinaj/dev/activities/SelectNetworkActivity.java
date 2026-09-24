package com.alinaj.dev.activities;
import android.os.*;
import com.alinaj.dev.R;
import java.util.*;
import org.json.*;

import static com.alinaj.dev.adapter.Adapter.NetworkAdapter;
import android.widget.*;
import android.widget.AdapterView.*;
import android.view.*;
import android.preference.*;
import android.content.*;

import androidx.appcompat.widget.Toolbar;
public class SelectNetworkActivity extends OpenVPNClientBase
{

    private ArrayList<JSONObject> listNetwork;

    private NetworkAdapter networkAdapter;

    private ListView mListView;
    public static final int SELECT_NETWORK_CODE = 60;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private Toolbar mToolbar;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_network);
        
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();
        
        mToolbar = findViewById(R.id.select_network_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        
        mListView = findViewById(R.id.network_list);
        listNetwork = new ArrayList<JSONObject>();
        networkAdapter = new NetworkAdapter(this, listNetwork, false);
        mListView.setAdapter(networkAdapter);
        loadNetworks();
        
        
        mListView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    try {
                        JSONObject js = listNetwork.get(p3);
                        editor.putString(OpenVPNClient.SELECTED_NETWORK, js.getString("Name")).apply();
                        setResult(RESULT_OK);
                        finish();
                    } catch (Exception e) {
                        showToast(e.getMessage());
                    }
                    // TODO: Implement this method
                }
                
            
        });
        findViewById(R.id.select_network_home_btn).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1)
                {
                    setResult(RESULT_CANCELED);
                    finish();
                    // TODO: Implement this method
                }
                
            
        });
    }
    public void loadNetworks()
    {
        try {
            if (listNetwork.size() > 0) {
                listNetwork.clear();
            }

            JSONArray network = getNetworksArray();
            for (int i = 0; i < network.length(); i++) {
                listNetwork.add(network.getJSONObject(i));
            }
            JSONArray sslnetwork = getSSLNetworks();
            for (int i = 0; i < sslnetwork.length(); i++) {
                listNetwork.add(sslnetwork.getJSONObject(i));
            }
            //Collections.sort(listNetwork, NetworkNameComparator());
            networkAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            showToast(e.getMessage());
        }
        // TODO: Implement this method
    }
}
