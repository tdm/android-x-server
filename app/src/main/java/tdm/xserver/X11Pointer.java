package tdm.xserver;

import android.util.Log;

import java.util.ArrayList;

class X11Pointer
{
    int                 mButtonState;
    X11Point            mLocation;

    X11Pointer() {
        mLocation = new X11Point();
    }
}
