package tdm.xserver;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class X11Client extends Thread implements X11ProtocolHandler
{
    // NR_BITS_RESOURCES + NR_BITS_CLIENTS must be 29
    static final int NR_BITS_RESOURCES  = 22;

    static final int NR_BITS_CLIENTS    = (29-NR_BITS_RESOURCES);
    static final int CLIENT_ID_SHIFT    = NR_BITS_RESOURCES;
    static final int MAX_CLIENTS        = (1<<NR_BITS_CLIENTS);
    private static final String TAG = X11Client.class.getName();

    X11Server                   mServer;
    int                         mId;
    X11Protocol                 mProt;
    short                       mSeqNo;
    Map<Integer,X11Resource>    mResources;

    X11Client(X11Server server) {
        mServer = server;
        mResources = new HashMap<Integer,X11Resource>();
    }
    X11Client(X11Server server, int id, Socket sock) throws Exception {
        Log.e(TAG, "new client from " + sock.getRemoteSocketAddress().toString());
        mServer = server;
        mId = id;
        mProt = new X11Protocol(this, sock);
        mSeqNo = 0;
        mResources = new HashMap<Integer,X11Resource>();
    }

    public void run() {
        while (mProt != null) {
            try {
                mProt.read();
            }
            catch (Exception e) {
                Log.e(TAG, "X11Client: terminating on exception: " + e.toString());
                e.printStackTrace();
                close();
            }
        }

        //XXX: release grabs
        //XXX: delete selection
        //XXX: free resources
        // ...
        for (X11Resource r : mResources.values()) {
            r.destroy();
        }

        mServer.clientClosed(this);
        mServer = null;
    }

    private final void close() {
        Log.i(TAG, "Closing connection to client#" + mId);
        mProt = null;
    }

    void addResource(X11Resource r) throws X11Error {
        if (mResources.containsKey(r.mId)) {
            throw new X11Error(X11Error.IDCHOICE, r.mId);
        }
        mResources.put(r.mId, r);
    }

    void delResource(int id) {
        mResources.remove(id);
    }

    X11Resource getResource(int id) throws X11Error {
        X11Client c = id < 0 ? this : mServer.mClients[id >> CLIENT_ID_SHIFT];
        X11Resource r = c.mResources.get(id);
        if (r == null) {
            throw new X11Error(X11Error.MATCH, id);
        }
        return r;
    }

    X11Resource getResource(int id, int type) throws X11Error {
        X11Resource r = getResource(id);
        if (r.mType != type) {
            throw new X11Error(X11Error.MATCH, id);
        }
        return r;
    }

    X11Pixmap getPixmap(int id) throws X11Error {
        return (X11Pixmap)getResource(id, X11Resource.PIXMAP);
    }

    X11Window getWindow(int id) throws X11Error {
        return (X11Window)getResource(id, X11Resource.WINDOW);
    }

    X11Colormap getColormap(int id) throws X11Error {
        //return (X11Colormap) getResource(id, X11Resource.COLORMAP);
        return X11Colormap.ROOT;
    }

    X11Cursor getCursor(int id) throws X11Error {
        return (X11Cursor)getResource(id, X11Resource.CURSOR);
    }

    X11Font getFont(int id) throws X11Error {
        return (X11Font)getResource(id, X11Resource.FONT);
    }

    X11GContext getGContext(int id) throws X11Error {
        return (X11GContext)getResource(id, X11Resource.GCONTEXT);
    }

    X11Drawable getDrawable(int id) throws X11Error {
        X11Resource r = getResource(id);
        if (r.mType != X11Resource.PIXMAP && r.mType != X11Resource.WINDOW) {
            throw new X11Error(X11Error.MATCH, id);
        }
        return (X11Drawable)r;
    }

    X11Fontable getFontable(int id) throws X11Error {
        X11Resource r = getResource(id);
        if (r.mType != X11Resource.GCONTEXT && r.mType != X11Resource.FONT) {
            throw new X11Error(X11Error.MATCH, id);
        }
        return (X11Fontable)r;
    }

    void send(X11ReplyMessage msg) {
        msg.seqno(mSeqNo);
        Log.d(TAG, "Send reply: seqno=" + mSeqNo);
        try {
            mProt.send(msg);
        }
        catch (IOException e) {
            close();
        }
    }
    void send(X11EventMessage msg) {
        msg.seqno(mSeqNo);
        Log.d(TAG, "Send event: seqno=" + mSeqNo + ", name=" + msg.name());
        try {
            mProt.send(msg);
        }
        catch (IOException e) {
            close();
        }
    }
    void send(X11ErrorMessage msg) {
        msg.seqno(mSeqNo);
        Log.d(TAG, "Send error: seqno=" + mSeqNo + ", name=" + msg.name());
        try {
            mProt.send(msg);
        }
        catch (IOException e) {
            close();
        }
    }

    X11Visual getVisual(int id) throws X11Error {
        return mServer.getVisual(id);
    }

    public void onMessage(X11SetupRequest msg) {
        //XXX: always allow for now
        Log.e(TAG, "Got setup request");

        String vendor = "My Vendor";

        ArrayList<X11Format> pixmap_formats = mServer.getPixmapFormats();

        X11SetupResponse r = new X11SetupResponse(msg);

        r.mSuccess = 0x01; // Success
        r.mProtoMajor = 11;
        r.mProtoMinor = 0;

        r.mData.enqInt((int)0);                         // release-number
        r.mData.enqInt(mId << CLIENT_ID_SHIFT);         // resource-id-base
        r.mData.enqInt((1 << CLIENT_ID_SHIFT) - 1);     // resource-id-mask
        r.mData.enqInt((int)0);                         // motion-buffer-size
        r.mData.enqShort((short)vendor.length());
        r.mData.enqShort((short)0xffff);                // maximum-request-length
        r.mData.enqByte((byte)1);                       // number of SCREENs in roots
        r.mData.enqByte((byte)pixmap_formats.size());   // number of FORMATs in pixmap-formats
        r.mData.enqByte((byte)0);                       // image-byte-order = LSBFirst
        r.mData.enqByte((byte)0);                       // bitmap-format-bit-order = LeastSignificant
        r.mData.enqByte((byte)32);                      // bitmap-format-scanline-unit
        r.mData.enqByte((byte)32);                      // bitmap-format-scanline-pad
        r.mData.enqByte((byte)mServer.mKeyboard.minKeycode());
        r.mData.enqByte((byte)mServer.mKeyboard.maxKeycode());
        r.mData.enqSkip(4);
        r.mData.enqString(vendor);
        r.mData.enqAlign(4);

        for (X11Format fmt : pixmap_formats) {
            r.mData.enqByte(fmt.mDepth);
            r.mData.enqByte(fmt.mBPP);
            r.mData.enqByte(fmt.mPad);
            r.mData.enqSkip(5);
        }

        mServer.mDefaultScreen.enqueue(r.mData);

        Log.e(TAG, "Sending setup response");
        try {
            mProt.send(r);
        }
        catch (Exception e) {
            Log.e(TAG, "Exception: ");
            e.printStackTrace();
            close();
        }
    }
    public void onMessage(X11SetupResponse msg) { close(); }
    public void onMessage(X11RequestMessage msg) {
        while (mServer.mGrabClient != null && mServer.mGrabClient != this) {
            Log.d(TAG, "Client#"+mId+" waiting on server grab");
            try {
                Thread.sleep(1);
            }
            catch (InterruptedException e) {
                // Ignore
            }
        }
        ++mSeqNo;
        Log.d(TAG, "Message: seqno=" + mSeqNo + ", name=" + msg.name());
        try {
            switch (msg.requestType()) {
            case   1: X11Window.create(this, msg); break;
            case   2: getWindow(msg.mData.deqInt()).handleChangeWindowAttributes(this, msg); break;
            case   3: getWindow(msg.mData.deqInt()).handleGetWindowAttributes(this, msg); break;
            case   4: getWindow(msg.mData.deqInt()).handleDestroyWindow(this, msg); break;
            case   8: getWindow(msg.mData.deqInt()).handleMapWindow(this, msg); break;
            case   9: getWindow(msg.mData.deqInt()).handleMapSubwindows(this, msg); break;
            case  10: getWindow(msg.mData.deqInt()).handleUnmapWindow(this, msg); break;
            case  11: getWindow(msg.mData.deqInt()).handleUnmapSubwindows(this, msg); break;
            case  12: getWindow(msg.mData.deqInt()).handleConfigureWindow(this, msg); break;
            case  14: getDrawable(msg.mData.deqInt()).handleGetGeometry(this, msg); break;
            case  15: mServer.handleQueryTree(this, msg);
            case  16: mServer.handleInternAtom(this, msg); break;
            case  18: getWindow(msg.mData.deqInt()).handleChangeProperty(this, msg); break;
            case  19: getWindow(msg.mData.deqInt()).handleDeleteProperty(this, msg); break;
            case  20: getWindow(msg.mData.deqInt()).handleGetProperty(this, msg); break;
            case  21: getWindow(msg.mData.deqInt()).handleListProperties(this, msg); break;
            case  22: mServer.handleSetSelectionOwner(this, msg); break;
            case  23: mServer.handleGetSelectionOwner(this, msg); break;
            case  25: mServer.handleSendEvent(this, msg); break;
            case  33: mServer.handleGrabKey(this, msg); break;
            case  36: mServer.handleGrabServer(this, msg); break;
            case  37: mServer.handleUngrabServer(this, msg); break;
            case  38: mServer.mKeyboard.handleQueryPointer(this, msg); break;
            case  40: getWindow(msg.mData.deqInt()).handleTranslateCoordinates(this, msg); break;
            case  42: mServer.handleSetInputFocus(this, msg); break;
            case  43: mServer.handleGetInputFocus(this, msg); break;
            case  45: mServer.handleOpenFont(this, msg); break;
            case  46: getFont(msg.mData.deqInt()).handleCloseFont(this, msg); break;
            case  47: getFontable(msg.mData.deqInt()).handleQueryFont(this, msg); break;
            case  49: mServer.handleListFonts(this, msg); break;
            case  50: mServer.handleListFontsWithInfo(this, msg); break;
            case  53: X11Pixmap.create(this, msg); break;
            case  54: getPixmap(msg.mData.deqInt()).handleFreePixmap(this, msg); break;
            case  55: X11GContext.create(this, msg); break;
            case  56: getGContext(msg.mData.deqInt()).handleChangeGC(this, msg); break;
            case  59: getGContext(msg.mData.deqInt()).handleSetClipRectangles(this, msg); break;
            case  60: getGContext(msg.mData.deqInt()).handleFreeGC(this, msg); break;
            case  61: getWindow(msg.mData.deqInt()).handleClearArea(this, msg); break;
            case  62: getDrawable(msg.mData.deqInt()).handleCopyArea(this, msg); break;
            case  64: getDrawable(msg.mData.deqInt()).handlePolyPoint(this, msg); break;
            case  65: getDrawable(msg.mData.deqInt()).handlePolyLine(this, msg); break;
            case  66: getDrawable(msg.mData.deqInt()).handlePolySegment(this, msg); break;
            case  67: getDrawable(msg.mData.deqInt()).handlePolyRectangle(this, msg); break;
            case  68: getDrawable(msg.mData.deqInt()).handlePolyArc(this, msg); break;
            case  69: getDrawable(msg.mData.deqInt()).handleFillPoly(this, msg); break;
            case  70: getDrawable(msg.mData.deqInt()).handlePolyFillRectangle(this, msg); break;
            case  71: getDrawable(msg.mData.deqInt()).handlePolyFillArc(this, msg); break;
            case  72: getDrawable(msg.mData.deqInt()).handlePutImage(this, msg); break;
            case  73: getDrawable(msg.mData.deqInt()).handleGetImage(this, msg); break;
            case  76: getDrawable(msg.mData.deqInt()).handleImageText8(this, msg); break;
            case  78: X11Colormap.create(this, msg); break;
            case  84: X11Colormap.handleAllocColor(this, msg); break;
            case  85: X11Colormap.handleAllocNamedColor(this, msg); break;
            case  91: X11Colormap.handleQueryColors(this, msg); break;
            case  92: getColormap(msg.mData.deqInt()).handleLookupColor(this, msg); break;
            case  93: X11PixmapCursor.create(this, msg); break;
            case  94: X11GlyphCursor.create(this, msg); break;
            case  95: getCursor(msg.mData.deqInt()).handleFreeCursor(this, msg); break;
            case  96: getCursor(msg.mData.deqInt()).handleRecolorCursor(this, msg); break;
            case  98: mServer.handleQueryExtension(this, msg); break;
            case 101: mServer.mKeyboard.handleGetKeyboardMapping(this, msg); break;
            case 104: mServer.mKeyboard.handleBell(this, msg); break;
            case 119: mServer.mKeyboard.handleGetModifierMapping(this, msg); break;
            default : throw new X11Error(X11Error.IMPLEMENTATION, msg.requestType());
            }
        }
        catch (X11Error e) {
            Log.e(TAG, "X11Error: " + e.name() + ": " + e.mVal, e);
            X11ErrorMessage err = new X11ErrorMessage(mProt.mEndian, e.mCode);
            err.mData.enqInt(e.mVal);
            err.mData.enqShort(msg.requestType() <= 127 ? 0 : msg.headerData());
            err.mData.enqShort(msg.requestType());
            send(err);
        }
        catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
            close();
        }
    }
    public void onMessage(X11ReplyMessage msg) { close(); }
    public void onMessage(X11EventMessage msg) { close(); }
    public void onMessage(X11ErrorMessage msg) { close(); }
}
