package tdm.xserver;

import android.content.Intent;
import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

import com.chedim.android.xserver.HomeActivity;
import com.chedim.android.xserver.X11WindowActivity;

import java.lang.Thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class X11Window extends X11Drawable
{
    private static final String TAG = X11Window.class.getName();
    public static final Map<Integer, UIHandler> handlers = new ConcurrentHashMap<>();
    static final int EVENT_EXPOSURE     = 0x00008000;

    static final short COPY_FROM_PARENT = 0;
    static final short INPUT_OUTPUT     = 1;
    static final short INPUT_ONLY       = 2;
    public static final ConcurrentHashMap<Integer, X11Window> windows = new ConcurrentHashMap<>();

    static void create(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        X11Window r = new X11Window(id);
        r.handleCreate(c, msg);
        c.addResource(r);
    }

    X11Window                   mParent;
    short                       mWndClass;
    X11Pixmap                   mBgPixmap;
    int                         mBgPixel;
    X11Pixmap                   mBorderPixmap;
    int                         mBorderPixel;
    byte                        mBitGravity;
    byte                        mWinGravity;
    byte                        mBackingStore;
    int                         mBackingPlanes;
    int                         mBackingPixel;
    byte                        mOverrideRedirect;
    byte                        mSaveUnder;
    int                         mEventMask;
    short                       mDoNotPropagateMask;
    X11Colormap                 mColormap;
    X11Cursor                   mCursor;

    Map<Integer,X11Property>    mProperties;

    // sibling position
    ArrayList<X11Window>        mChildren;

    ClientView                  mView;

    boolean                     mMapped;
    boolean                     mRealized;
    boolean                     mIsRoot;

    X11WindowActivity           mActivity;
    View                        mLayout;

    X11Window(int id) {
        super(X11Resource.WINDOW, id);
        windows.put(id, this);
        mProperties = new HashMap<Integer,X11Property>();
        mChildren = new ArrayList<X11Window>();
    }

    void destroy() {
        windows.remove(mId);
        if (mView != null) {
            sendViewMessage(UIHandler.MSG_VIEW_REMOVE);
            while (mView != null) { Thread.yield(); }
        }
        mChildren = null;
        mProperties = null;
        mCursor = null;
        mColormap = null;
        mBorderPixmap = null;
        mBgPixmap = null;
        mParent = null;
        super.destroy();
    }

    void createRoot(X11Client c, short w, short h, byte d, X11Visual v) throws X11Error {
        mDepth = d;
        mRect = new X11Rect((short)0, (short)0, w, h);
        mBitmap = Bitmap.createBitmap(mRect.w, mRect.h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mBorderWidth = 0;
        mWndClass = INPUT_OUTPUT;
        mVisual = v;

        initBackground();

        mMapped = true;
        mRealized = true;
        mIsRoot = true;


        Log.d("X", "Creating root ClientView");
        sendViewMessage(UIHandler.MSG_VIEW_CREATE_ROOT);
        while (mView == null) { Thread.yield(); }
        mView.mClient = c;
        c.mServer.mInputFocus = this;
    }

    void repaint() {
        Log.i(TAG, "Repainting window#" + mId + " (Root? " + mIsRoot + ")");
        postRender();
    }

    void initBackground() {
        mBgPixmap = new X11Pixmap(0); //XXX: should use a real (global) id
        mBgPixmap.doCreate(mDepth, (short)3, (short)3, this);
        mBgPixmap.mBitmap.setPixel((short)0, (short)0, Color.WHITE);
        mBgPixmap.mBitmap.setPixel((short)1, (short)0, Color.BLACK);
        mBgPixmap.mBitmap.setPixel((short)2, (short)0, Color.WHITE);
        mBgPixmap.mBitmap.setPixel((short)0, (short)1, Color.BLACK);
        mBgPixmap.mBitmap.setPixel((short)1, (short)1, Color.WHITE);
        mBgPixmap.mBitmap.setPixel((short)2, (short)1, Color.BLACK);
        mBgPixmap.mBitmap.setPixel((short)0, (short)2, Color.WHITE);
        mBgPixmap.mBitmap.setPixel((short)1, (short)2, Color.BLACK);
        mBgPixmap.mBitmap.setPixel((short)2, (short)2, Color.WHITE);
        paintBackgroundArea(mRect);
    }

    void handleCreate(X11Client c, X11RequestMessage msg) throws X11Error {
        mDepth = msg.headerData();
        mParent = c.getWindow(msg.mData.deqInt());
        mParent.mChildren.add(this);

        mRect = new X11Rect();
        mRect.x = msg.mData.deqShort();
        mRect.y = msg.mData.deqShort();
        mRect.w = msg.mData.deqShort();
        mRect.h = msg.mData.deqShort();
        mBorderWidth = msg.mData.deqShort();

        mBitmap = Bitmap.createBitmap(mRect.w, mRect.h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        mWndClass = msg.mData.deqShort();
        int vid = msg.mData.deqInt();
        if (mWndClass == COPY_FROM_PARENT) {
            mWndClass = mParent.mWndClass;
        }
        if (mWndClass == INPUT_ONLY) {
            if (mDepth != 0) {
                throw new X11Error(X11Error.MATCH, mDepth);
            }
            if (mBorderWidth != 0) {
                throw new X11Error(X11Error.MATCH, mBorderWidth);
            }
        }
        else if (mWndClass == INPUT_OUTPUT) {
            if (mDepth == 0) {
                mDepth = mParent.mDepth;
            }
        }
        else {
            throw new X11Error(X11Error.MATCH, mWndClass);
        }

        if (vid == 0 /* CopyFromParent */) {
            mVisual = mParent.mVisual;
        }
        else {
            mVisual = c.getVisual(vid);
        }
        if (mRect.w == 0 && mRect.h == 0) {
            throw new X11Error(X11Error.VALUE, 0);
        }

        //XXX: Add checks per spec here?

        mBorderPixmap = mParent.mBorderPixmap;
        mColormap = mParent.mColormap;

        doChangeAttributes(c, msg.mData);

        sendViewMessage(UIHandler.MSG_VIEW_CREATE);

        while (mView == null) { Thread.yield(); }
        mView.mClient = c;

        paintBackgroundArea(mRect);
        //XXX: xorg server does not seem to send a CreateNotify event?

    }

    void handleChangeWindowAttributes(X11Client c, X11RequestMessage msg) throws X11Error {
        if (mParent == null) {
            return;
        }

        doChangeAttributes(c, msg.mData);
    }

    void handleGetWindowAttributes(X11Client c, X11RequestMessage msg) {
        byte map_state = (byte)0 /* Unmapped */;
        if (mMapped) {
            map_state = 1 /* Unviewable */;
            if (mRealized) {
                map_state = 2 /* Viewable */;
            }
        }
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData(mBackingStore);
        reply.mData.enqInt(mVisual.mId);
        reply.mData.enqShort(mWndClass);
        reply.mData.enqByte(mBitGravity);
        reply.mData.enqByte(mWinGravity);
        reply.mData.enqInt(mBackingPlanes);
        reply.mData.enqInt(mBackingPixel);
        reply.mData.enqByte(mSaveUnder);
        reply.mData.enqByte((byte)1 /* True */); //XXX: map-is-installed
        reply.mData.enqByte(map_state);
        reply.mData.enqByte(mOverrideRedirect);
        reply.mData.enqInt(mColormap.mId);
        reply.mData.enqInt(mEventMask);
        reply.mData.enqInt(mEventMask);
        reply.mData.enqShort(mDoNotPropagateMask);
        reply.mData.enqSkip(2);
        c.send(reply);
    }

    void handleDestroyWindow(X11Client c, X11RequestMessage msg) {
        doDestroy(c);
    }

    void handleDestroySubwindows(X11Client c, X11RequestMessage msg) {
        doDestroySub(c);
    }

    void handleMapWindow(X11Client c, X11RequestMessage msg) throws X11Error {
        doMap(c);
    }

    void handleMapSubwindows(X11Client c, X11RequestMessage msg) throws X11Error {
        doMapSub(c);
    }

    void handleUnmapWindow(X11Client c, X11RequestMessage msg) throws X11Error {
        doUnmap(c);
    }

    void handleUnmapSubwindows(X11Client c, X11RequestMessage msg) throws X11Error {
        doUnmapSub(c);
    }

    void handleConfigureWindow(X11Client c, X11RequestMessage msg) throws X11Error {
        boolean size_changed = false;
        boolean position_changed = false;
        short mask = msg.mData.deqShort();
        if ((mask & 0x0001) != 0) {     // x
            mRect.x = (short)(msg.mData.deqInt() & 0xffff);
            position_changed = true;
        }
        if ((mask & 0x0002) != 0) {     // y
            mRect.y = (short)(msg.mData.deqInt() & 0xffff);
            position_changed = true;
        }
        if ((mask & 0x0004) != 0) {     // width
            mRect.w = (short)(msg.mData.deqInt() & 0xffff);
            size_changed = true;
        }
        if ((mask & 0x0008) != 0) {     // height
            size_changed = true;
            mRect.h = (short)(msg.mData.deqInt() & 0xffff);
        }
        if ((mask & 0x0010) != 0) {     // border-width
            mBorderWidth = (short)(msg.mData.deqInt() & 0xffff);
        }
        if ((mask & 0x0020) != 0) {    // sibling
            //XXX
            c.getWindow(msg.mData.deqInt());
        }
        if ((mask & 0x0040) != 0) {     //  stack-mode
            //XXX
            msg.mData.deqInt();
        }

        if (size_changed && mRect.w > 0 && mRect.h > 0) {
            mBitmap = Bitmap.createBitmap(mRect.w, mRect.h, Bitmap.Config.ARGB_8888);
            mCanvas.setBitmap(mBitmap);
            paintBackgroundArea(mRect);
        }
        if (size_changed || position_changed) {
            sendViewMessage(UIHandler.MSG_VIEW_CONFIGURE);
        }
    }

    void handleChangeProperty(X11Client c, X11RequestMessage msg) throws X11Error {
        byte mode = msg.headerData(); // Replace, Prepend, Append
        int name = msg.mData.deqInt();
        int type = msg.mData.deqInt();
        byte fmt = msg.mData.deqByte();
        msg.mData.deqSkip(3);
        if (fmt != 8 && fmt != 16 && fmt != 32) {
            throw new X11Error(X11Error.VALUE, fmt);
        }
        int datalen = msg.mData.deqInt();
        ByteQueue data = new ByteQueue(); //XXX endian?
        while (datalen-- != 0) {
            switch (fmt) {
            case  8: data.enqByte(msg.mData.deqByte()); break;
            case 16: data.enqShort(msg.mData.deqShort()); break;
            case 32: data.enqInt(msg.mData.deqInt()); break;
            }
        }

        //XXX: lots of checks are missing here

        X11Property prop = mProperties.get(name);
        if (prop == null) {
            prop = new X11Property(type, fmt);
            mProperties.put(name, prop);
        }

        switch (mode) {
        case 0: prop.setValue(data.getBytes()); break;
        case 1: prop.appendValue(data.getBytes()); break;
        case 2: prop.prependValue(data.getBytes()); break;
        }

        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.PROPERTY_NOTIFY);
        evt.mData.enqInt(mId);
        evt.mData.enqInt(name);
        evt.mData.enqInt(c.mServer.currentTime());
        evt.mData.enqByte((byte)0); // NewValue
        c.send(evt);
    }

    void handleDeleteProperty(X11Client c, X11RequestMessage msg) throws X11Error {
        int name = msg.mData.deqInt();
        if (mProperties.containsKey(name)) {
            mProperties.remove(name);

            X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.PROPERTY_NOTIFY);
            evt.mData.enqInt(mId);
            evt.mData.enqInt(name);
            evt.mData.enqInt(c.mServer.currentTime());
            evt.mData.enqByte((byte)1); // Deleted
            c.send(evt);
        }
    }

    void handleGetProperty(X11Client c, X11RequestMessage msg) throws X11Error {
        byte del = msg.headerData();
        int name = msg.mData.deqInt();
        int type = msg.mData.deqInt();
        int off = msg.mData.deqInt();
        int len = msg.mData.deqInt();

        X11ReplyMessage reply = new X11ReplyMessage(msg);

        X11Property prop = mProperties.get(name);
        if (prop != null) {
            int rpos = off * 4;
            if (rpos > prop.mVal.length) {
                throw new X11Error(X11Error.VALUE, off);
            }
            int rlen = Math.min(prop.mVal.length - rpos, len*4);
            byte[] rval = new byte[rlen];
            System.arraycopy(prop.mVal, rpos, rval, 0, rlen); //XXX: endian?
            if (type != 0 /* AnyPropertyType */ && type != prop.mType) {
                rpos = 0;
                rlen = 0;
                del = 0 /* False */;
            }
            else {
                if (prop.mVal.length < (rpos+rlen)) {
                    del = 0 /* False */;
                }
            }

            reply.headerData(prop.mFormat);
            reply.mData.enqInt(prop.mType);
            reply.mData.enqInt(prop.mVal.length - (rpos + rlen));
            reply.mData.enqInt(rval.length / (prop.mFormat/8));
            reply.mData.enqSkip(12);
            reply.mData.enqArray(rval);

            if (del != 0 /* False */) {
                mProperties.remove(name);
                X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.PROPERTY_NOTIFY);
                evt.mData.enqInt(mId);
                evt.mData.enqInt(name);
                evt.mData.enqInt(c.mServer.currentTime());
                evt.mData.enqByte((byte)1); // Deleted
                c.send(evt);
            }
        }
        else {
            reply.mData.enqInt(0); // None
            reply.mData.enqInt(0);
            reply.mData.enqInt(0);
        }
        c.send(reply);
    }

    void handleListProperties(X11Client c, X11RequestMessage msg) throws X11Error {
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqShort((short)mProperties.size());
        reply.mData.enqSkip(22);
        for (Integer name : mProperties.keySet()) {
            reply.mData.enqInt(name);
        }
        c.send(reply);
    }

    void handleTranslateCoordinates(X11Client c, X11RequestMessage msg) throws X11Error {
        X11Window dst = c.getWindow(msg.mData.deqInt());
        short src_x = msg.mData.deqShort();
        short src_y = msg.mData.deqShort();

        X11Point src_origin = absolutePosition(c);
        X11Point dst_origin = dst.absolutePosition(c);

        short dst_x = (short)(src_origin.x + src_x - dst_origin.x);
        short dst_y = (short)(src_origin.y + src_y - dst_origin.y);

        int childid = 0 /* None */;
        for (X11Window child : mChildren) {
            if (src_x >= child.mRect.x && src_y >= child.mRect.y &&
                    src_x < child.mRect.x + child.mRect.w &&
                    src_y < child.mRect.y + child.mRect.h) {
                childid = child.mId;
                break; //XXX: which child(ren) to return here?
            }
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData((byte)1 /* True */); // same-screen
        reply.mData.enqInt(childid); // WINDOW or None
        reply.mData.enqShort(dst_x);
        reply.mData.enqShort(dst_y);
        c.send(reply);
    }

    void handleClearArea(X11Client c, X11RequestMessage msg) throws X11Error {
        if (mWndClass == INPUT_ONLY) {
            throw new X11Error(X11Error.MATCH, mId);
        }
        byte exposures = msg.headerData();
        X11Rect r = new X11Rect();
        r.x = msg.mData.deqShort();
        r.y = msg.mData.deqShort();
        r.w = msg.mData.deqShort();
        if (r.w == 0) {
            r.w = (short)(mRect.x - r.x);
        }
        r.h = msg.mData.deqShort();
        if (r.h == 0) {
            r.h = (short)(mRect.y - r.y);
        }
        paintBackgroundArea(r);
        mView.postInvalidate(r.x, r.y, r.x+r.w, r.y+r.h);
        if (exposures != 0 /* False */) { //XXX: independent of EXPOSURES event mask?
            X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.EXPOSE);
            evt.mData.enqInt(mId);
            evt.mData.enqShort((short)0); // x
            evt.mData.enqShort((short)0); // y
            evt.mData.enqShort(mRect.w);  // width
            evt.mData.enqShort(mRect.h);  // height
            evt.mData.enqShort((short)0); // count
            c.send(evt); // XXX: thread sync?
        }
    }

    void postRender(X11Rect r) {
        mView.postInvalidate(r.x, r.y, r.x+r.w, r.y+r.h);
    }

    void postRender() {
        mView.postInvalidate();
    }

    X11Point absolutePosition(X11Client c) throws X11Error {
        X11Point pos = new X11Point();
        if (mParent != null) {
            pos = c.getWindow(mParent.mId).absolutePosition(c);
        }
        pos.x += mRect.x;
        pos.y += mRect.y;
        return pos;
    }

    private void doChangeAttributes(X11Client c, ByteQueue q) throws X11Error {
        int mask = q.deqInt();
        int val;

        if (mWndClass == INPUT_ONLY) {
            if ((mask & 0x25df) != 0) {
                throw new X11Error(X11Error.MATCH, mask);
            }
        }

        if ((mask & 0x0001) != 0) { // background-pixmap
            val = q.deqInt();
            if (val == 0 /* None */) {
                mBgPixmap = null;
            }
            else if (val == 1 /* ParentRelative */) {
                //XXX
                mBgPixmap = null;
            }
            else {
                mBgPixmap = c.getPixmap(val);
            }
            paintBackgroundArea(mRect);
        }
        if ((mask & 0x0002) != 0) { // background-pixel
            mBgPixel = (q.deqInt() | 0xff000000);
            paintBackgroundArea(mRect);
        }
        if ((mask & 0x0004) != 0) { // border-pixmap
            mBorderPixmap = c.getPixmap(q.deqInt());
        }
        if ((mask & 0x0008) != 0) { // border-pixel
            mBorderPixel = (q.deqInt() | 0xff000000);
        }
        if ((mask & 0x0010) != 0) { // bit-gravity
            mBitGravity = (byte)q.deqInt();
        }
        if ((mask & 0x0020) != 0) { // win-gravity
            mWinGravity = (byte)q.deqInt();
        }
        if ((mask & 0x0040) != 0) { // backing-store
            mBackingStore = (byte)q.deqInt();
        }
        if ((mask & 0x0080) != 0) { // backing-planes
            mBackingPlanes = q.deqInt();
        }
        if ((mask & 0x0100) != 0) { // backing-pixel
            mBackingPixel = (q.deqInt() | 0xff000000);
        }
        if ((mask & 0x0200) != 0) { // override-redirect
            mOverrideRedirect = (byte)q.deqInt();
        }
        if ((mask & 0x0400) != 0) { // save-under
            mSaveUnder = (byte)q.deqInt();
        }
        if ((mask & 0x0800) != 0) { // event-mask
            mEventMask = q.deqInt();
        }
        if ((mask & 0x1000) != 0) { // do-not-propagate-mask
            mDoNotPropagateMask = (short)(q.deqInt() & ~0xffffc0b0);
        }
        if ((mask & 0x2000) != 0) { // colormap
            //XXX: 0 = CopyFromParent
            mColormap = c.getColormap(q.deqInt());
        }
        if ((mask & 0x4000) != 0) { // cursor
            mCursor = c.getCursor(q.deqInt());
        }
    }

    private void doDestroy(X11Client c) {
        if (mParent == null) {
            return;
        }
        for (X11Window child : mChildren) {
            child.doDestroy(c);
        }
        doUnmap(c);

        sendViewMessage(UIHandler.MSG_VIEW_REMOVE);
        mCursor = null;
        mColormap = null;
        mBorderPixmap = null;
        mBgPixmap = null;
        mVisual = null;
        mCanvas = null;
        mBitmap = null;
        mRect = null;
        mParent.mChildren.remove(this);
        mParent= null;

        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.DESTROY_NOTIFY);
        evt.mData.enqInt(mId); // event
        evt.mData.enqInt(mId); // window
        c.send(evt);
    }

    private void doDestroySub(X11Client c) {
        //XXX: bottom-to-top stacking order
        for (X11Window child : mChildren) {
            child.doDestroy(c);
        }
    }

    private void doMap(X11Client c) {
        if (mMapped) {
            return;
        }
        //XXX: check override-redirect
        mMapped = true;
        if (mParent == null) {
            mRealized = true;
        }
        else {
            if (mParent.mRealized) {
                doRealizeSub(c);
                doRealize(c);
            }
        }

        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.MAP_NOTIFY);
        evt.mData.enqInt(mId); // event
        evt.mData.enqInt(mId); // window
        evt.mData.enqByte(mOverrideRedirect);
        c.send(evt);
    }

    private void doMapSub(X11Client c) {
        for (X11Window child : mChildren) {
            child.doMapSub(c);
            child.doMap(c);
        }
    }

    private void doUnmap(X11Client c) {
        if (!mMapped) {
            return;
        }
        if (mParent == null) {
            return;
        }

        mMapped = false;

        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.UNMAP_NOTIFY);
        evt.mData.enqInt(mId); // event
        evt.mData.enqInt(mId); // window
        evt.mData.enqByte((byte)0); // from-configure
        c.send(evt);
    }

    private void doUnmapSub(X11Client c) {
        for (X11Window child : mChildren) {
            child.doUnmap(c);
            child.doUnmapSub(c);
        }
    }

    private void doRealizeSub(X11Client c) {
        for (X11Window child : mChildren) {
            child.doRealizeSub(c);
            child.doRealize(c);
        }
    }

    private void doRealize(X11Client c) {
        Log.d("X", "Window#"+mId+".doRealize: set visible");
        sendViewMessage(UIHandler.MSG_VIEW_SET_VISIBLE);
        while (!mRealized) { Thread.yield(); }

        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.EXPOSE);
        evt.mData.enqInt(mId);
        evt.mData.enqShort((short)0); // x
        evt.mData.enqShort((short)0); // y
        evt.mData.enqShort(mRect.w);  // width
        evt.mData.enqShort(mRect.h);  // height
        evt.mData.enqShort((short)0); // count
        c.send(evt); // XXX: thread sync?
    }

    void onKeyPress(X11Client c, int code) {
        if ((mEventMask & X11Event.KEY_PRESS) == 0) {
            return; //XXX ???
        }
        if (X11Server.getInstance().mGrabKey != null) {
            c = X11Server.getInstance().mGrabKey;
        }
        X11Window root = c.mServer.mDefaultScreen.mRoot;
        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.KEY_PRESS);
        evt.headerData((byte)(X11Keyboard.X_MIN_KEYCODE+code));
        evt.mData.enqInt(c.mServer.currentTime());
        evt.mData.enqInt(root.mId);
        evt.mData.enqInt(mId);
        evt.mData.enqInt(0 /*None*/);
        evt.mData.enqShort((short)0); // root-x
        evt.mData.enqShort((short)0); // root-y
        evt.mData.enqShort((short)0); // event-x
        evt.mData.enqShort((short)0); // event-y
        evt.mData.enqShort(c.mServer.mKeyboard.mModState);
        evt.mData.enqByte((byte)1); // same-screen = True
        evt.mData.enqSkip(1);
        c.send(evt);
    }

    void onKeyRelease(X11Client c, int code) {
        if ((mEventMask & X11Event.KEY_RELEASE) == 0) {
            return; //XXX ???
        }
        if (X11Server.getInstance().mGrabKey != null) {
            c = X11Server.getInstance().mGrabKey;
        }
        X11Window root = c.mServer.mDefaultScreen.mRoot;
        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.KEY_RELEASE);
        evt.headerData((byte)(X11Keyboard.X_MIN_KEYCODE+code));
        evt.mData.enqInt(c.mServer.currentTime());
        evt.mData.enqInt(root.mId);
        evt.mData.enqInt(mId);
        evt.mData.enqInt(0 /*None*/);
        evt.mData.enqShort((short)0); // root-x
        evt.mData.enqShort((short)0); // root-y
        evt.mData.enqShort((short)0); // event-x
        evt.mData.enqShort((short)0); // event-y
        evt.mData.enqShort(c.mServer.mKeyboard.mModState); // state
        evt.mData.enqByte((byte)1); // same-screen = True
        evt.mData.enqSkip(1);
        c.send(evt);
    }

    void onButtonPress(X11Client c, int x, int y, int num) {
        if ((mEventMask & X11Event.BUTTON_PRESS) == 0) {
            return; //XXX ???
        }
        X11Window root = c.mServer.mDefaultScreen.mRoot;
        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.BUTTON_PRESS);
        evt.headerData((byte)num);
        evt.mData.enqInt(c.mServer.currentTime());
        evt.mData.enqInt(root.mId);
        evt.mData.enqInt(mId);
        evt.mData.enqInt(0 /*None*/);
        evt.mData.enqShort((short)(mRect.x+x)); // root-x
        evt.mData.enqShort((short)(mRect.y+y)); // root-y
        evt.mData.enqShort((short)x); // event-x
        evt.mData.enqShort((short)y); // event-y
        evt.mData.enqShort((short)0); // state
        evt.mData.enqByte((byte)1); // same-screen = True
        evt.mData.enqSkip(1);
        c.send(evt);
    }

    void onButtonRelease(X11Client c, int x, int y, int num) {
        if ((mEventMask & X11Event.BUTTON_RELEASE) == 0) {
            return; //XXX ???
        }
        X11Window root = c.mServer.mDefaultScreen.mRoot;
        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.BUTTON_RELEASE);
        evt.headerData((byte)num);
        evt.mData.enqInt(c.mServer.currentTime());
        evt.mData.enqInt(root.mId);
        evt.mData.enqInt(mId);
        evt.mData.enqInt(0 /*None*/);
        evt.mData.enqShort((short)(mRect.x+x)); // root-x
        evt.mData.enqShort((short)(mRect.y+y)); // root-y
        evt.mData.enqShort((short)x); // event-x
        evt.mData.enqShort((short)y); // event-y
        evt.mData.enqShort((short)0); // state
        evt.mData.enqByte((byte)1); // same-screen = True
        evt.mData.enqSkip(1);
        c.send(evt);
    }

    void onMotion(X11Client c, int x, int y) {
        if ((mEventMask & X11Event.MOTION_NOTIFY) == 0) {
            return; //XXX ???
        }
        X11Window root = c.mServer.mDefaultScreen.mRoot;
        X11EventMessage evt = new X11EventMessage(c.mProt.mEndian, X11Event.MOTION_NOTIFY);
        evt.headerData((byte)0 /*Normal*/);
        evt.mData.enqInt(c.mServer.currentTime());
        evt.mData.enqInt(root.mId);
        evt.mData.enqInt(mId);
        evt.mData.enqInt(0 /*None*/);
        evt.mData.enqShort((short)(mRect.x+x)); // root-x
        evt.mData.enqShort((short)(mRect.y+y)); // root-y
        evt.mData.enqShort((short)x); // event-x
        evt.mData.enqShort((short)y); // event-y
        evt.mData.enqShort((short)0); // state
        evt.mData.enqByte((byte)1); // same-screen = True
        evt.mData.enqSkip(1);
        c.send(evt);
    }

    private void paintBackgroundArea(X11Rect r) {
        mCanvas.save();
        mCanvas.clipRect(r.x, r.y, r.x+r.w, r.y+r.h);
        if (mBgPixel != 0) { //XXX: need a flag, 0 is a valid value
            mCanvas.drawColor(0xff000000 | (mBgPixel & 0x00ffffff));
        }
        else if (mBgPixmap != null) {
            for (int x = 0; x < mRect.w; x += mBgPixmap.mRect.w) {
                for (int y = 0; y < mRect.h; y += mBgPixmap.mRect.h) {
                    mCanvas.drawBitmap(mBgPixmap.mBitmap, x, y, null);
                }
            }
        }
        mCanvas.restore();
    }

    UIHandler getHandler() {
        if (handlers.containsKey(mId)) {
            return handlers.get(mId);
        }
        return handlers.get(X11Window.ID_ROOT_WINDOW);
    }

    public void setHandler(UIHandler handler) {
       handlers.put(mId, handler);
    }

    public void sendViewMessage(int func) {
        Message msg = Message.obtain(getHandler(), func, this);
        getHandler().sendMessage(msg);
    }
}
