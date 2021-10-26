package com.chedim.android.xserver;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

import tdm.xserver.UIHandler;
import tdm.xserver.X11Resource;
import tdm.xserver.X11Server;
import tdm.xserver.X11Window;

/**
 * Launcher activity that starts all services
 */
public class HomeActivity extends AppCompatActivity {
    private static final String TAG = HomeActivity.class.getName();
    private static ViewGroup rootView;
    private static UIHandler mUiHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating cyborg launcher home activity " + this);
        rootView = new RelativeLayout(this);
        setContentView(rootView);
        if (mUiHandler == null) {
            Log.i(TAG, "Launching X11 service");
            startForegroundService(new Intent(this, X11Server.class));
            mUiHandler = new UIHandler(X11Window.ID_ROOT_WINDOW, rootView);
            X11Window.handlers.put(X11Window.ID_ROOT_WINDOW, mUiHandler);
        }
    }

    public static ViewGroup getRootView() {
        return rootView;
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
        Log.i(TAG, "Resuming cyborg launcher home activity " + this);
        mUiHandler.updateViewGroup(rootView);
    }
}