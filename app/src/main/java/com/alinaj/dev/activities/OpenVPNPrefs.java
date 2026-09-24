package com.alinaj.dev.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.alinaj.dev.R;

public class OpenVPNPrefs extends PreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        if (OpenVPNClientBase.themeSet) {
            setTheme(OpenVPNClientBase.themeResId);
        }
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
