package tdm.xserver;

import android.util.Log;

import android.content.Context;

import android.os.Handler;
import android.os.Message;

import android.graphics.drawable.BitmapDrawable;

import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;

import android.widget.RelativeLayout;

class UIHandler extends Handler
{
    public static final int MSG_VIEW_CREATE_ROOT = 0x1001;
    public static final int MSG_VIEW_CREATE      = 0x1002;
    public static final int MSG_VIEW_REMOVE      = 0x1003;
    public static final int MSG_VIEW_CONFIGURE   = 0x1004;
    public static final int MSG_VIEW_SET_VISIBLE = 0x1010;
    public static final int MSG_VIEW_SET_BACKGROUND_COLOR = 0x1011;
    public static final int MSG_VIEW_SET_BACKGROUND_BITMAP = 0x1012;
    public static final int MSG_VIEW_INVALIDATE  = 0x1020;

    Context                     mContext;
    ViewGroup                   mViewGroup;

    UIHandler(Context ctx, ViewGroup vg) {
        mContext = ctx;
        mViewGroup = vg;
    }

    public void handleMessage(Message msg) {
        X11Window w;
        RelativeLayout.LayoutParams lp;
        switch (msg.what) {
        case MSG_VIEW_CREATE_ROOT:
            w = (X11Window)msg.obj;
            w.mView = new ClientView(mContext, w);
            lp = new RelativeLayout.LayoutParams(w.mRect.x, w.mRect.y);
            mViewGroup.addView(w.mView, lp);
            w.mView.setFocusable(true);
            w.mView.setFocusableInTouchMode(true);
            w.mView.setVisibility(View.VISIBLE);
            Log.d("X", "UI: w="+w.mId+": Attached ClientView to root window");
            break;
        case MSG_VIEW_CREATE:
            w = (X11Window)msg.obj;
            w.mView = new ClientView(mContext, w);
            lp = new RelativeLayout.LayoutParams(w.mRect.w, w.mRect.h);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, -1);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, -1);
            lp.setMargins(w.mRect.x, w.mRect.y, 0, 0);
            mViewGroup.addView(w.mView, lp);
            w.mView.setFocusable(true);
            w.mView.setFocusableInTouchMode(true);
            w.mView.setVisibility(View.INVISIBLE);
            Log.d("X", "UI: w="+w.mId+": Attached ClientView to window at x="+w.mRect.x+", y=" + w.mRect.y);
            break;
        case MSG_VIEW_REMOVE:
            w = (X11Window)msg.obj;
            w.mRealized = false;
            mViewGroup.removeView(w.mView);
            w.mView.destroy();
            w.mView = null;
            break;
        case MSG_VIEW_CONFIGURE:
            w = (X11Window)msg.obj;
            lp = new RelativeLayout.LayoutParams(w.mRect.w, w.mRect.h);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, -1);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, -1);
            lp.setMargins(w.mRect.x, w.mRect.y, 0, 0);
            //XXX: better way?
            mViewGroup.removeView(w.mView);
            mViewGroup.addView(w.mView, lp);
            break;
        case MSG_VIEW_SET_VISIBLE:
            w = (X11Window)msg.obj;
            w.mView.setVisibility(View.VISIBLE);
            w.mRealized = true;
            w.mView.requestFocus();
            Log.d("X", "UI: w="+w.mId+": Set window visible");
            break;
        case MSG_VIEW_SET_BACKGROUND_COLOR:
            w = (X11Window)msg.obj;
            w.mView.setBackgroundColor(w.mBgPixel);
            break;
        case MSG_VIEW_SET_BACKGROUND_BITMAP:
            w = (X11Window)msg.obj;
            w.mView.setBackgroundDrawable(
                    new BitmapDrawable(mContext.getResources(),
                            w.mBgPixmap.mBitmap));
            break;
        case MSG_VIEW_INVALIDATE:
            w = (X11Window)msg.obj;
            w.mView.invalidate();
            break;
        }
    }
}
