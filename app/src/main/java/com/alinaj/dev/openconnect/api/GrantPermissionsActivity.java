package com.alinaj.dev.openconnect.api;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import com.alinaj.dev.openconnect.core.OpenVpnService;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/01/16
 */

public class GrantPermissionsActivity extends Activity {

    public static final String EXTRA_START_ACTIVITY = ".start_activity";
    public static final String EXTRA_UUID = ".UUID";
    private String mStartActivity;
    private String mUUID;

    private void reportBadRom(Exception e) {
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExternalOpenVPNService.m76a(this);
        Intent myIntent = getIntent();
        String stringExtra = myIntent.getStringExtra(getPackageName() + EXTRA_UUID);
        this.mUUID = stringExtra;
        if (stringExtra == null) {
            finish();
            return;
        }
        this.mStartActivity = myIntent.getStringExtra(getPackageName() + EXTRA_START_ACTIVITY);
        try {
            Intent prepIntent = VpnService.prepare(this);
            if (prepIntent != null) {
                try {
                    startActivityForResult(prepIntent, 0);
                } catch (Exception e) {
                    reportBadRom(e);
                    finish();
                }
            } else {
                onActivityResult(0, -1, null);
            }
        } catch (Exception e2) {
            reportBadRom(e2);
            finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode);
        if (resultCode == -1) {
            Intent intent = new Intent(getBaseContext(), OpenVpnService.class);
            intent.putExtra(OpenVpnService.EXTRA_UUID, this.mUUID);
            startService(intent);
            if (this.mStartActivity != null) {
                Intent intent2 = new Intent();
                intent2.setClassName(this, this.mStartActivity);
                startActivity(intent2);
            }
        }
        finish();
    }
}