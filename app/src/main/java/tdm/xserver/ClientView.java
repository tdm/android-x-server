package tdm.xserver;

import android.util.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import android.view.View;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

class ClientView extends View {
  private static final String TAG = ClientView.class.getName();
  X11Window mWindow;
  X11Client mClient;

  ClientView(Context ctx, X11Window w) {
    super(ctx);
    mWindow = w;
    Log.i(TAG, "Creating view " + w.mId + " in context " + ctx);
  }

  void destroy() {
    Log.i(TAG, "Destroying view " + mWindow.mId + " of context#" + getContext());
    mClient = null;
    mWindow = null;
  }

  protected void onMeasure(int wms, int hms) {
    try {
      Log.d("X", "ClientView#" + mWindow.mId + ".onMeasure: wms=" + wms + ", hms=" + hms + ", w=" + mWindow.mRect.w
              + ", h=" + mWindow.mRect.h);
      setMeasuredDimension(mWindow.mRect.w, mWindow.mRect.h);
    } catch (NullPointerException e) {
      setMeasuredDimension(0, 0);
    }
  }

  public void onDraw(Canvas canvas) {
    Log.d("X", "ClientView#" + mWindow.mId + ".onDraw");
    canvas.drawBitmap(mWindow.mBitmap, 0, 0, null);
  }

  public boolean onGenericMotionEvent(MotionEvent event) {
    if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0) {
      // Do not handle non-pointer motion (eg. joystick)
      return true; // XXX? super.onGenericMotionEvent(event);
    }
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN: // 0
        Log.d("X", "MotionEvent: ACTION_DOWN");
        mWindow.onButtonPress(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      case MotionEvent.ACTION_UP: // 1
        Log.d("X", "MotionEvent: ACTION_UP");
        mWindow.onButtonRelease(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      case MotionEvent.ACTION_MOVE: // 2
        Log.d("X", "MotionEvent: ACTION_MOVE");
        mWindow.onMotion(mClient, (int) event.getX(), (int) event.getY());
        break;
      // ACTION_CANCEL == 3
      // ACTION_OUTSIDE == 4
      case MotionEvent.ACTION_POINTER_DOWN: // 5
        Log.d("X", "MotionEvent: ACTION_POINTER_DOWN");
        mWindow.onButtonPress(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      case MotionEvent.ACTION_POINTER_UP: // 6
        Log.d("X", "MotionEvent: ACTION_POINTER_UP");
        mWindow.onButtonRelease(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      // XXX: MotionEvent.ACTION_HOVER_MOVE
      // XXX: MotionEvent.ACTION_SCROLL
    }
    return true;
  }

  public boolean onTouchEvent(MotionEvent event) {
    // XXX: MotienEvent.getSource() is apparently not in FroYo :/
    // E/AndroidRuntime( 1992): java.lang.NoSuchMethodError:
    // android.view.MotionEvent.getSource
    if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0) {
      // Do not handle non-pointer motion (eg. joystick)
      return true; // XXX? super.onTouchEvent(event);
    }
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN: // 0
        Log.d("X", "TouchEvent: ACTION_DOWN");
        mWindow.onButtonPress(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      case MotionEvent.ACTION_UP: // 1
        Log.d("X", "TouchEvent: ACTION_UP");
        mWindow.onButtonRelease(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      case MotionEvent.ACTION_MOVE: // 2
        Log.d("X", "TouchEvent: ACTION_MOVE");
        mWindow.onMotion(mClient, (int) event.getX(), (int) event.getY());
        break;
      // ACTION_CANCEL == 3
      // ACTION_OUTSIDE == 4
      case MotionEvent.ACTION_POINTER_DOWN: // 5
        Log.d("X", "TouchEvent: ACTION_POINTER_DOWN");
        mWindow.onButtonPress(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      case MotionEvent.ACTION_POINTER_UP: // 6
        Log.d("X", "TouchEvent: ACTION_POINTER_UP");
        mWindow.onButtonRelease(mClient, (int) event.getX(), (int) event.getY(), 0);
        break;
      // XXX: MotionEvent.ACTION_HOVER_MOVE
      // XXX: MotionEvent.ACTION_SCROLL
    }
    return true;
  }

  public boolean onKeyDown(int code, KeyEvent event) {
    Log.d("X", "ClientView#" + mWindow.mId + ".onKeyDown(" + code + "): char=" + event.getUnicodeChar());
    mWindow.onKeyPress(mClient, code);
    mClient.mServer.mKeyboard.onKeyDown(code);
    return true;
  }

  public boolean onKeyUp(int code, KeyEvent event) {
    Log.d("X", "ClientView#" + mWindow.mId + ".onKeyUp(" + code + ")");
    mWindow.onKeyRelease(mClient, code);
    mClient.mServer.mKeyboard.onKeyUp(code);
    return true;
  }

  protected void onFocusChanged(boolean gainFocus, int direction, Rect prev) {
    Log.d("X", "ClientView#" + mWindow.mId + ".onFocusChanged(" + gainFocus + ")");
  }
}
