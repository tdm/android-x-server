package tdm.xserver;

class X11Selection
{
    X11Atom                     mSelection;
    X11Client                   mOwnerClient;
    X11Window                   mOwnerWindow;
    int                         mLastChangeTime;
}
