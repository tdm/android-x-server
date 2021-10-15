package tdm.xserver;

class X11Visual
{
    static final int NONE       = 0;

    static final byte STATIC_GRAY       = 0;
    static final byte GRAYSCALE         = 1;
    static final byte STATIC_COLOR      = 2;
    static final byte PSEUDO_COLOR      = 3;
    static final byte TRUE_COLOR        = 4;
    static final byte DIRECT_COLOR      = 5;

    int                 mId;
    byte                mDepth;

    byte                mVisClass;
    byte                mBitsPerRgbValue;
    short               mColormapEntries;
    int                 mRedMask;
    int                 mGreenMask;
    int                 mBlueMask;

    X11Visual(int n, byte d) {
        mId = n;
        mDepth = d;
    }
}
