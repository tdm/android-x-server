package tdm.xserver;

import android.util.Log;

import java.util.ArrayList;

class X11Screen
{
    X11Window                   mRoot;
    X11Colormap                 mDefaultColormap;
    int                         mWhitePixel;
    int                         mBlackPixel;
    int                         mCurrentInputMasks;
    short                       mWidthPx;
    short                       mHeightPx;
    short                       mWidthMM;
    short                       mHeightMM;
    short                       mMinInstalledMaps;
    short                       mMaxInstalledMaps;
    int                         mRootVisual; //XXX: property of root?
    byte                        mBackingStores;
    byte                        mSaveUnders;
    byte                        mRootDepth; //XXX: property of root?
    ArrayList<X11Depth>         mAllowedDepths;
    // ...


    X11Screen(X11Server server, X11Client client, short w, short h, short dpi) throws X11Error {
        mAllowedDepths = new ArrayList();

        mDefaultColormap = new X11Colormap(X11Resource.ID_DEF_COLORMAP);
        client.addResource(mDefaultColormap);

        mRoot = new X11Window(X11Resource.ID_ROOT_WINDOW);
        client.addResource(mRoot);

        mDefaultColormap.initDefault();
        mRoot.mColormap = mDefaultColormap;

        int vid = 0;
        X11Depth d;
        X11Visual v;

        d = new X11Depth((byte)24);

        v = new X11Visual(++vid, (byte)24);
        v.mVisClass = X11Visual.DIRECT_COLOR;
        v.mRedMask = (0xff << 16);
        v.mGreenMask = (0xff << 8);
        v.mBlueMask = (0xff);
        v.mBitsPerRgbValue = 8;
        v.mColormapEntries = (1 << 8);
        server.addVisual(v);
        d.visuals.add(v);

        v = new X11Visual(++vid, (byte)24);
        v.mVisClass = X11Visual.TRUE_COLOR;
        v.mRedMask = (0xff << 16);
        v.mGreenMask = (0xff << 8);
        v.mBlueMask = (0xff);
        v.mBitsPerRgbValue = 8;
        v.mColormapEntries = (1 << 8);
        server.addVisual(v);
        d.visuals.add(v);

        mAllowedDepths.add(d);

        // root is depth 24, visual DirectColor
        mRoot.createRoot(client, w, h, d.depth, v);

        //XXX: depth 1, 4, 8, 15, 16?

        d = new X11Depth((byte)32);

        v = new X11Visual(++vid, (byte)32);
        v.mVisClass = X11Visual.DIRECT_COLOR;
        v.mRedMask = (0xff << 16);
        v.mGreenMask = (0xff << 8);
        v.mBlueMask = (0xff);
        v.mBitsPerRgbValue = 8;
        v.mColormapEntries = (1 << 8);
        server.addVisual(v);
        d.visuals.add(v);

        v = new X11Visual(++vid, (byte)32);
        v.mVisClass = X11Visual.TRUE_COLOR;
        v.mRedMask = (0xff << 16);
        v.mGreenMask = (0xff << 8);
        v.mBlueMask = (0xff);
        v.mBitsPerRgbValue = 8;
        v.mColormapEntries = (1 << 8);
        server.addVisual(v);
        d.visuals.add(v);

        mAllowedDepths.add(d);

        mWhitePixel = 0x00ffffff;
        mBlackPixel = 0;
        mCurrentInputMasks = 0;
        mWidthPx = w;
        mHeightPx = h;
        mWidthMM = (short)((254*w)/(10*dpi));
        mHeightMM = (short)((254*h)/(10*dpi));
        mMinInstalledMaps = 1;
        mMaxInstalledMaps = 1;
        mRootVisual = mRoot.mVisual.mId;
        mBackingStores = X11Resource.NEVER;
        mSaveUnders = 0;
        mRootDepth = mRoot.mDepth;
    }

    void enqueue(ByteQueue q) {
        q.enqInt(mRoot.mId);
        q.enqInt(mDefaultColormap.mId);
        q.enqInt(mWhitePixel);
        q.enqInt(mBlackPixel);
        q.enqInt(mCurrentInputMasks);
        q.enqShort(mWidthPx);
        q.enqShort(mHeightPx);
        q.enqShort(mWidthMM);
        q.enqShort(mHeightMM);
        q.enqShort(mMinInstalledMaps);
        q.enqShort(mMaxInstalledMaps);
        q.enqInt(mRoot.mVisual.mId);
        q.enqByte(mBackingStores);
        q.enqByte(mSaveUnders);
        q.enqByte(mRoot.mDepth);

        q.enqByte((byte)mAllowedDepths.size());
        for (X11Depth dep : mAllowedDepths) {
            q.enqByte(dep.depth);
            q.enqSkip(1);
            q.enqShort((short)dep.visuals.size());
            q.enqSkip(4);

            for (X11Visual vis : dep.visuals) {
                q.enqInt(vis.mId);
                q.enqByte(vis.mVisClass);
                q.enqByte(vis.mBitsPerRgbValue);
                q.enqShort(vis.mColormapEntries);
                q.enqInt(vis.mRedMask);
                q.enqInt(vis.mGreenMask);
                q.enqInt(vis.mBlueMask);
                q.enqSkip(4);
            }
        }
    }
}
