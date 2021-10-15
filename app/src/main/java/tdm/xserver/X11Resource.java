package tdm.xserver;

public class X11Resource
{
    // Resource constants
    static final int MIN_ID             = 4;
    static final int ID_DEF_COLORMAP    = MIN_ID+0;
    public static final int ID_ROOT_WINDOW     = MIN_ID+1;

    static final int NONE       = 0;

    static final int NEVER      = 0;

    // Resource types
    static final int WINDOW     = 1;
    static final int PIXMAP     = 2;
    static final int GCONTEXT   = 3;
    static final int FONT       = 4;
    static final int CURSOR     = 5;
    static final int COLORMAP   = 6;
    static final int CMAPENTRY  = 7;
    static final int OTHERCLIENT = 8;
    static final int PASSIVEGRAB = 9;

    int                 mType;
    int                 mId;

    X11Resource(int t, int n) {
        mType = t;
        mId = n;
    }

    void destroy() {}
}
