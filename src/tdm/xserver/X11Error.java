package tdm.xserver;

class X11Error extends Throwable
{
    static final byte NONE              = (byte)0;
    static final byte REQUEST           = (byte)1;
    static final byte VALUE             = (byte)2;
    static final byte WINDOW            = (byte)3;
    static final byte PIXMAP            = (byte)4;
    static final byte ATOM              = (byte)5;
    static final byte CURSOR            = (byte)6;
    static final byte FONT              = (byte)7;
    static final byte MATCH             = (byte)8;
    static final byte DRAWABLE          = (byte)9;
    static final byte ACCESS            = (byte)10;
    static final byte ALLOC             = (byte)11;
    static final byte COLORMAP          = (byte)12;
    static final byte GCONTEXT          = (byte)13;
    static final byte IDCHOICE          = (byte)14;
    static final byte NAME              = (byte)15;
    static final byte LENGTH            = (byte)16;
    static final byte IMPLEMENTATION    = (byte)17;

    static final String[] error_names = {
        "NONE",
        "Request",
        "Value",
        "Window",
        "Pixmap",
        "Atom",
        "Cursor",
        "Font",
        "Match",
        "Drawable",
        "Access",
        "Alloc",
        "Colormap",
        "GContext",
        "IDChoice",
        "Name",
        "Length",
        "Implementation"
    };

    String name() { return error_names[mCode]; }

    byte        mCode;
    int         mVal;

    X11Error(byte code, int val) {
        mCode = code;
        mVal = val;
    }
}
