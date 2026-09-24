package com.alinaj.dev.activities;


import android.os.*;
import android.widget.*;
import android.webkit.*;
import android.preference.*;
import android.content.*;

import android.view.View.*;
import android.view.*;
import com.alinaj.dev.R;
import androidx.appcompat.app.*;
public class ResellerPanelActivity extends AppCompatActivity implements OnClickListener
{

    private ProgressBar pb;
    private WebView webview;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        pb = findViewById(R.id.web_progress);
        webview = findViewById(R.id.webview);
        //refresh = (FloatingActionButton)findViewById(R.id.refresh);

        //refresh.setOnClickListener(this);
        webview.setWebViewClient(new MyWebViewClient());
        webview.setWebChromeClient(new MyWebChromeClient());
        webview.setSaveEnabled(true);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setEnableSmoothTransition(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.enableSmoothTransition();
        webSettings.setSaveFormData(true); 
        webSettings.setSavePassword(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webview.loadUrl("https://rktunnelvip.xyz");

    }

    @Override
    public void onClick(View p1)
    {
        /*switch (p1.getId()) {
            case R.id.refresh:
                webview.reload();
                break;
        }*/
        // TODO: Implement this method
    }
    @Override
    public void onBackPressed()
    {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }
    private class MyWebViewClient extends WebViewClient
    {

        @Override
        public void onPageFinished(WebView view, String url)
        {
            editor.putString("url", url).apply();
            // TODO: Implement this method
            super.onPageFinished(view, url);
        }
    }
    private class MyWebChromeClient extends WebChromeClient
    {

        @Override
        public void onReceivedTitle(WebView view, String title)
        {
            getSupportActionBar().setTitle(title);
            // TODO: Implement this method
            super.onReceivedTitle(view, title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress)
        {
            if (newProgress == 100) {
                pb.setProgress(0);
            } else {
                pb.setProgress(newProgress);
            }
            // TODO: Implement this method
            super.onProgressChanged(view, newProgress);
        }
    }
}
