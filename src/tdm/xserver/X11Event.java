package tdm.xserver;

class X11Event
{
    static final byte ERROR             = 0; // pseudo-event
    static final byte REPLY             = 1; // pseudo-event
    static final byte KEY_PRESS         = 2;
    static final byte KEY_RELEASE       = 3;
    static final byte BUTTON_PRESS      = 4;
    static final byte BUTTON_RELEASE    = 5;
    static final byte MOTION_NOTIFY     = 6;
    static final byte ENTER_NOTIFY      = 7;
    static final byte LEAVE_NOTIFY      = 8;
    static final byte FOCUS_IN          = 9;
    static final byte FOCUS_OUT         = 10;
    static final byte KEYMAP_NOTIFY     = 11;
    static final byte EXPOSE            = 12;
    static final byte GRAPHICS_EXPOSE   = 13;
    static final byte NO_EXPOSE         = 14;
    static final byte VISIBILITY_NOTIFY = 15;
    static final byte CREATE_NOTIFY     = 16;
    static final byte DESTROY_NOTIFY    = 17;
    static final byte UNMAP_NOTIFY      = 18;
    static final byte MAP_NOTIFY        = 19;
    static final byte MAP_REQUEST       = 20;
    static final byte REPARENT_NOTIFY   = 21;
    static final byte CONFIGURE_NOTIFY  = 22;
    static final byte CONFIGURE_REQUEST = 23;
    static final byte GRAVITY_NOTIFY    = 24;
    static final byte RESIZE_REQUEST    = 25;
    static final byte CIRCULATE_NOTIFY  = 26;
    static final byte CIRCULATE_REQUEST = 27;
    static final byte PROPERTY_NOTIFY   = 28;
    static final byte SELECTION_CLEAR   = 29;
    static final byte SELECTION_REQUEST = 30;
    static final byte SELECTION_NOTIFY  = 31;
    static final byte COLORMAP_NOTIFY   = 32;
    static final byte CLIENT_MESSAGE    = 33;
    static final byte MAPPING_NOTIFY    = 34;
}
