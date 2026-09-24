package com.alinaj.dev.activities;

import android.os.*;
import android.widget.*;
import android.widget.LinearLayout.*;
import androidx.appcompat.app.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alinaj.dev.R;

public class ExceptionActivity extends AppCompatActivity {
    TextView error;
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 17;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(layoutParams);
        setContentView(linearLayout);



        ScrollView sv = new ScrollView(this);
        TextView error = new TextView(this);
        sv.addView(error);
        linearLayout.addView(sv);
        error.setText(getIntent().getStringExtra("error"));
    }
}
