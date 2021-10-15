package tdm.xserver;

class X11Atom
{
    static final int NONE               = 0;

    static final int NR_PREDEFINED      = 68;
    static final String predefined_names[] = {
        "(none)",           // Invalid, placeholder
        "PRIMARY",          // 1
        "SECONDARY",
        "ARC",
        "ATOM",
        "BITMAP",
        "CARDINAL",
        "COLORMAP",
        "CURSOR",
        "CUT_BUFFER0",
        "CUT_BUFFER1",      // 10
        "CUT_BUFFER2",
        "CUT_BUFFER3",
        "CUT_BUFFER4",
        "CUT_BUFFER5",
        "CUT_BUFFER6",
        "CUT_BUFFER7",
        "DRAWABLE",
        "FONT",
        "INTEGER",
        "PIXMAP",           // 20
        "POINT",
        "RECTANGLE",
        "RESOURCE_MANAGER",
        "RGB_COLOR_MAP",
        "RGB_BEST_MAP",
        "RGB_BLUE_MAP",
        "RGB_DEFAULT_MAP",
        "RGB_GRAY_MAP",
        "RGB_GREEN_MAP",
        "RGB_RED_MAP",      // 30
        "STRING",
        "VISUALID",
        "WINDOW",
        "WM_COMMAND",
        "WM_HINTS",
        "WM_CLIENT_MACHINE",
        "WM_ICON_NAME",
        "WM_ICON_SIZE",
        "WM_NAME",
        "WM_NORMAL_HINTS",  // 40
        "WM_SIZE_HINTS",
        "WM_ZOOM_HINTS",
        "MIN_SPACE",
        "NORM_SPACE",
        "MAX_SPACE",
        "END_SPACE",
        "SUPERSCRIPT_X",
        "SUPERSCRIPT_Y",
        "SUBSCRIPT_X",
        "SUBSCRIPT_Y",      // 50
        "UNDERLINE_POSITION",
        "UNDERLINE_THICKNESS",
        "STRIKEOUT_ASCENT",
        "STRIKEOUT_DESCENT",
        "ITALIC_ANGLE",
        "X_HEIGHT",
        "QUAD_WIDTH",
        "WEIGHT",
        "POINT_SIZE",
        "RESOLUTION",       // 60
        "COPYRIGHT",
        "NOTICE",
        "FONT_NAME",
        "FAMILY_NAME",
        "FULL_NAME",
        "CAP_HEIGHT",
        "WM_CLASS",
        "WM_TRANSIENT_FOR"
    };

    static final boolean predefined(int id) {
        return (id >= 1 && id < NR_PREDEFINED);
    }

    static void globalInit(X11Server server) {
        int i;
        for (i = 1; i <= NR_PREDEFINED; ++i) {
            server.doInternAtom(i, predefined_names[i]);
        }
        server.mLastAtomId = NR_PREDEFINED;
    }

    int                         mId;
    String                      mName;

    X11Atom(int id, String name) {
        mId = id;
        mName = name;
    }
}
