package tdm.xserver;

import android.util.Log;

import android.graphics.Paint;
import android.graphics.Path;

class X11GContext extends X11Fontable
{
    static void create(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        X11GContext r = new X11GContext(id);
        r.handleCreate(c, msg);
        c.addResource(r);
    }

    Paint                       mPaint;

    X11Drawable                 mDrawable;
    byte                        mFunction;
    int                         mPlaneMask;
    int                         mForePixel;
    int                         mBackPixel;
    short                       mLineWidth;
    byte                        mLineStyle;
    byte                        mCapStyle;
    byte                        mJoinStyle;
    byte                        mFillStyle;
    byte                        mFillRule;
    X11Pixmap                   mTile;
    X11Pixmap                   mStipple;
    X11Point                    mTileStippleOrigin;
    X11Font                     mFont;
    byte                        mSubWindowMode;
    byte                        mGraphicsExposures;
    X11Point                    mClipOrigin;
    X11Pixmap                   mClipMask;
    Path                        mClipPath;
    short                       mDashOffset;
    byte                        mDashes;
    byte                        mArcMode;

    X11GContext(int n) {
        super(X11Resource.GCONTEXT, n);
        mPaint = new Paint();
        mForePixel = 0xff000000;
        mPaint.setColor(mForePixel);
        mBackPixel = 0xffffffff;
        mTileStippleOrigin = new X11Point();
    }

    void destroy() {
        mClipPath = null;
        mClipMask = null;
        mClipOrigin = null;
        mFont = null;
        mTileStippleOrigin = null;
        mStipple = null;
        mTile = null;
        mDrawable = null;
        mPaint = null;
        super.destroy();
    }

    void handleQueryFont(X11Client c, X11RequestMessage msg) {
        mFont.handleQueryFont(c, msg);
    }

    void handleCreate(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        mDrawable = c.getDrawable(id);
        doChangeAttributes(c, msg.mData);
    }

    void handleChangeGC(X11Client c, X11RequestMessage msg) throws X11Error {
        doChangeAttributes(c, msg.mData);
    }

    void handleSetClipRectangles(X11Client c, X11RequestMessage msg) {
        byte ordering = msg.headerData();
        mClipOrigin = new X11Point();
        mClipOrigin.x = msg.mData.deqShort();
        mClipOrigin.y = msg.mData.deqShort();
        mClipPath = new Path();
        while (msg.mData.remain() > 0) {
            short x = msg.mData.deqShort();
            short y = msg.mData.deqShort();
            short w = msg.mData.deqShort();
            short h = msg.mData.deqShort();
            mClipPath.addRect(x, y, x+w, y+h, Path.Direction.CW);
        }
    }

    void handleFreeGC(X11Client c, X11RequestMessage msg) {
        c.delResource(mId);
    }

    private void doChangeAttributes(X11Client c, ByteQueue q) throws X11Error {
        int mask = q.deqInt();
        int val;

        if ((mask & 0x000001) != 0) { // function
            mFunction = (byte)q.deqInt();
        }
        if ((mask & 0x000002) != 0) { // plane-mask
            mPlaneMask = q.deqInt();
        }
        if ((mask & 0x000004) != 0) { // foreground
            mForePixel = (q.deqInt() | 0xff000000);
            mPaint.setColor(mForePixel);
        }
        if ((mask & 0x000008) != 0) { // background
            mBackPixel = (q.deqInt() | 0xff000000);
        }
        if ((mask & 0x000010) != 0) { // line-width
            mLineWidth = (short)q.deqInt();
        }
        if ((mask & 0x000020) != 0) { // line-style
            mLineStyle = (byte)q.deqInt();
        }
        if ((mask & 0x000040) != 0) { // cap-style
            mCapStyle = (byte)q.deqInt();
        }
        if ((mask & 0x000080) != 0) { // join-style
            mJoinStyle = (byte)q.deqInt();
        }
        if ((mask & 0x000100) != 0) { // fill-style
            mFillStyle = (byte)q.deqInt();
        }
        if ((mask & 0x000200) != 0) { // fill-rule
            mFillRule = (byte)q.deqInt();
        }
        if ((mask & 0x000400) != 0) { // tile
            mTile = c.getPixmap(q.deqInt());
        }
        if ((mask & 0x000800) != 0) { // stipple
            mStipple = c.getPixmap(q.deqInt());
        }
        if ((mask & 0x001000) != 0) { // tile-stipple-x-origin
            mTileStippleOrigin.x = (short)q.deqInt();
        }
        if ((mask & 0x002000) != 0) { // tile-stipple-y-origin
            mTileStippleOrigin.y = (short)q.deqInt();
        }
        if ((mask & 0x004000) != 0) { // font
            mFont = c.getFont(q.deqInt());
        }
        if ((mask & 0x008000) != 0) { // subwindow-mode
            mSubWindowMode = (byte)q.deqInt();
        }
        if ((mask & 0x010000) != 0) { // graphics-exposures
            mGraphicsExposures = (byte)q.deqInt();
        }
        if ((mask & 0x020000) != 0) { // clip-x-origin
            mClipOrigin.x = (short)q.deqInt();
        }
        if ((mask & 0x040000) != 0) { // clip-y-origin
            mClipOrigin.y = (short)q.deqInt();
        }
        if ((mask & 0x080000) != 0) { // clip-mask
            val = q.deqInt();
            if (val == X11Resource.NONE) {
                mClipMask = null;
            }
            else {
                mClipMask = c.getPixmap(val);
            }
        }
        if ((mask & 0x100000) != 0) { // dash-offset
            mDashOffset = (short)q.deqInt();
        }
        if ((mask & 0x200000) != 0) { // dashes
            mDashes = (byte)q.deqInt();
        }
        if ((mask & 0x400000) != 0) { // arc-mode
            mArcMode = (byte)q.deqInt();
        }
    }
}
