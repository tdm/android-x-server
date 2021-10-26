package tdm.xserver;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class UIHandler extends Handler
{
    public static final int MSG_VIEW_CREATE_ROOT = 0x1001;
    public static final int MSG_VIEW_CREATE      = 0x1002;
    public static final int MSG_VIEW_REMOVE      = 0x1003;
    public static final int MSG_VIEW_CONFIGURE   = 0x1004;
    public static final int MSG_VIEW_SET_VISIBLE = 0x1010;
    public static final int MSG_VIEW_SET_BACKGROUND_COLOR = 0x1011;
    public static final int MSG_VIEW_SET_BACKGROUND_BITMAP = 0x1012;
    public static final int MSG_VIEW_INVALIDATE  = 0x1020;
    public static final int MSG_VIEW_RECREATE_ROOT = 0x1021;
    public static final int MSG_VIEW_RECREATE = 0x1022;
    private static final String TAG = UIHandler.class.getName();

    Integer                     mWindowId;
    ViewGroup                   mViewGroup;

    public UIHandler(Integer w, ViewGroup vg) {
        mWindowId = w;
        mViewGroup = vg;
    }

    public Context getContext() {
        return mViewGroup.getContext();
    }

    public void handleMessage(Message msg) {
        X11Window w;
        RelativeLayout.LayoutParams lp;
        switch (msg.what) {
        case MSG_VIEW_CREATE_ROOT:
            w = (X11Window)msg.obj;
            viewCreateRoot(w);
            break;
        case MSG_VIEW_CREATE:
            w = (X11Window)msg.obj;
            viewCreate(w);
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
            w.mView.setBackground(
                    new BitmapDrawable(getContext().getResources(),
                            w.mBgPixmap.mBitmap));
            break;
        case MSG_VIEW_INVALIDATE:
            w = (X11Window)msg.obj;
            w.mView.invalidate();
            break;
        case MSG_VIEW_RECREATE_ROOT:
            w = (X11Window) msg.obj;
            viewCreateRoot(w);
            recreateViews(w);
            break;
        case MSG_VIEW_RECREATE:
            w = (X11Window) msg.obj;
            viewCreate(w);
            recreateViews(w);
            break;
        }
    }

    protected void viewCreateRoot(X11Window w) {
        w.mView = new ClientView(getContext(), w);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w.mRect.x, w.mRect.y);
        mViewGroup.addView(w.mView, lp);
        w.mView.setFocusable(true);
        w.mView.setFocusableInTouchMode(true);
        w.mView.setVisibility(View.VISIBLE);
        Log.d("X", "UI: w="+w.mId+": Attached ClientView to root window");
    }

    protected void viewCreate(X11Window w) {
        if (w.mRect == null) {
            return;
        }
        w.mView = new ClientView(getContext(), w);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w.mRect.w, w.mRect.h);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, -1);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, -1);
        lp.setMargins(w.mRect.x, w.mRect.y, 0, 0);
        mViewGroup.addView(w.mView, lp);
        w.mView.setFocusable(true);
        w.mView.setFocusableInTouchMode(true);
        w.mView.setVisibility(View.INVISIBLE);
        Log.d("X", "UI: w="+w.mId+": Attached ClientView to window at x="+w.mRect.x+", y=" + w.mRect.y);
    }

    protected void recreateViews(X11Window window) {
        Log.i(TAG, "Recreating " + window);
        window.repaint();
        for (X11Window child: window.mChildren) {
            UIHandler handler = child.getHandler();
            if (handler != null && handler != this && child.mRect != null) {
                Log.i(TAG, "Delegating re-creation to " + handler);
                handler.handleMessage(Message.obtain(handler, MSG_VIEW_RECREATE, child));
            } else {
                handler.viewCreate(child);
                child.repaint();
            }
        }
    }

    public void updateViewGroup(ViewGroup newViewGroup) {
        mViewGroup = newViewGroup;
        Log.i(TAG, "Updating view group to " + newViewGroup);
        X11Window window = X11Window.windows.get(mWindowId);
        if (window != null) {
            if (window.mIsRoot) {
                Log.i(TAG, "Updating ROOT view group to " + newViewGroup);
                viewCreateRoot(window);
            } else {
                viewCreate(window);
            }
            recreateViews(window);
        }
    }
}
