package com.chedim.android.xserver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import tdm.xserver.UIHandler;
import tdm.xserver.X11Server;

public class X11WindowActivity extends AppCompatActivity {
    private int windowId;
    private ViewGroup rootView;
    @Override
    protected void onStart() {
        windowId = getIntent().getIntExtra(X11Server.X11WINDOW_ID, -1);
        if (windowId == -1) {
            throw new RuntimeException("No window id provided");
        }
        super.onStart();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = new RelativeLayout(this);
        setContentView(rootView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        X11Server.registerUiHandler(windowId, new UIHandler(this, rootView));
    }
}