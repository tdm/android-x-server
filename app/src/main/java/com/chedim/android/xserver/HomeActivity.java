package com.chedim.android.xserver;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

import tdm.xserver.UIHandler;
import tdm.xserver.X11Resource;
import tdm.xserver.X11Server;

/**
 * Launcher activity that starts all services
 */
public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup rootView = new RelativeLayout(this);
        setContentView(rootView);

        X11Server.registerUiHandler(X11Resource.ID_ROOT_WINDOW, new UIHandler(this, rootView));
        startForegroundService(new Intent(this, X11Server.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if Android M or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show alert dialog to the user saying a separate permission is needed
            // Launch the settings activity if the user prefers
            if (!Settings.canDrawOverlays(this)) {
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(myIntent);
            }
        }
    }
}