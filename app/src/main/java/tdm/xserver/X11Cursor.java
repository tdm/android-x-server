package tdm.xserver;

class X11Cursor extends X11Resource
{
    X11Color                    mFgColor;
    X11Color                    mBgColor;

    X11Cursor(int id) {
        super(X11Resource.CURSOR, id);
    }

    void destroy() {
        mBgColor = null;
        mFgColor = null;
        super.destroy();
    }

    void handleFreeCursor(X11Client c, X11RequestMessage msg) {
        c.delResource(mId);
    }

    void handleRecolorCursor(X11Client c, X11RequestMessage msg) {
        doRecolor(msg.mData);
    }

    void doRecolor(ByteQueue q) {
        mFgColor = new X11Color();
        mFgColor.r = q.deqShort();
        mFgColor.g = q.deqShort();
        mFgColor.b = q.deqShort();
        mBgColor = new X11Color();
        mBgColor.r = q.deqShort();
        mBgColor.g = q.deqShort();
        mBgColor.b = q.deqShort();
    }
}

class X11PixmapCursor extends X11Cursor
{
    static void create(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        X11PixmapCursor r = new X11PixmapCursor(id);
        r.handleCreate(c, msg);
        c.addResource(r);
    }

    X11Pixmap                   mSource;
    X11Pixmap                   mMask;
    X11Point                    mHotSpot;

    X11PixmapCursor(int id) {
        super(id);
    }

    void destroy() {
        mHotSpot = null;
        mMask = null;
        mSource = null;
        super.destroy();
    }

    void handleCreate(X11Client c, X11RequestMessage msg) throws X11Error {
        mSource = c.getPixmap(msg.mData.deqInt());
        mMask = c.getPixmap(msg.mData.deqInt());
        doRecolor(msg.mData);
        mHotSpot = new X11Point();
        mHotSpot.x = msg.mData.deqShort();
        mHotSpot.y = msg.mData.deqShort();
    }
}

class X11GlyphCursor extends X11Cursor
{
    static void create(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        X11GlyphCursor r = new X11GlyphCursor(id);
        r.handleCreate(c, msg);
        c.addResource(r);
    }

    X11Font                     mSource;
    short                       mSourceChar;
    X11Font                     mMask;
    short                       mMaskChar;

    X11GlyphCursor(int id) {
        super(id);
    }

    void destroy() {
        mMask = null;
        mSource = null;
        super.destroy();
    }

    void handleCreate(X11Client c, X11RequestMessage msg) throws X11Error {
        mSource = c.getFont(msg.mData.deqInt());
        mMask = c.getFont(msg.mData.deqInt());
        mSourceChar = msg.mData.deqShort();
        mMaskChar = msg.mData.deqShort();
        doRecolor(msg.mData);
    }
}
