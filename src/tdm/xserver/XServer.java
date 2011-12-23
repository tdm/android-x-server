package tdm.xserver;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;

import android.widget.RelativeLayout;

import java.net.ServerSocket;

public class XServer extends Activity
{
    static final String         TAG = "XServer";

    static XServer              mInstance;

    RelativeLayout              mLayout;
    X11Server                   mServer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
    }

    protected void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();

        try {
            mLayout = new RelativeLayout(this);
            setContentView(mLayout);

            UIHandler handler = new UIHandler(this, mLayout);

            mServer = new X11Server(this, handler);
            mServer.start();
        }
        catch (Exception e) {
            Log.e(TAG, "Cannot create server", e);
            finish();
            return;
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        // ...?
    }

    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
        // ...?
    }

    protected void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
        mServer.onStop();
        // ...?
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Other manipulations...

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        // Other manipulations...

        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_save:
            // ...
            break;
        case R.id.menu_delete:
            // ...
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
