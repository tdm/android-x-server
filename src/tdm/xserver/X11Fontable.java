package tdm.xserver;

abstract class X11Fontable extends X11Resource
{
    X11Fontable(int t, int n) {
        super(t, n);
    }

    void destroy() {
        super.destroy();
    }

    abstract void handleQueryFont(X11Client c, X11RequestMessage msg);
}
