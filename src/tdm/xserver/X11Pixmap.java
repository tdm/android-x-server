package tdm.xserver;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;

class X11Pixmap extends X11Drawable
{
    static void create(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        X11Pixmap r = new X11Pixmap(id);
        r.handleCreate(c, msg);
        c.addResource(r);
    }


    X11Drawable         mDrawable;

    X11Pixmap(int id) {
        super(X11Resource.PIXMAP, id);
        mDrawable = null;
    }

    void destroy() {
        mDrawable = null;
        super.destroy();
    }

    void handleCreate(X11Client c, X11RequestMessage msg) throws X11Error {
        byte depth = msg.headerData();
        X11Drawable d = c.getDrawable(msg.mData.deqInt());
        short w = msg.mData.deqShort();
        short h = msg.mData.deqShort();
        if (w == 0 || h == 0) {
            throw new X11Error(X11Error.VALUE, 0);
        }
        doCreate(depth, w, h, d);
    }

    void handleFreePixmap(X11Client c, X11RequestMessage msg) {
        c.delResource(mId);
    }

    void doCreate(byte depth, short w, short h, X11Drawable d) {
        mDepth = depth;
        mRect = new X11Rect((short)0, (short)0, w, h);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mDrawable = d;
    }
}
