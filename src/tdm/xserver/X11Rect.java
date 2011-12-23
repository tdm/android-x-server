package tdm.xserver;

class X11Rect
{
    short               x;
    short               y;
    short               w;
    short               h;

    X11Rect() {
        x = 0;
        y = 0;
        w = 0;
        h = 0;
    }
    X11Rect(short _x, short _y, short _w, short _h) {
        x = _x;
        y = _y;
        w = _w;
        h = _h;
    }
}
