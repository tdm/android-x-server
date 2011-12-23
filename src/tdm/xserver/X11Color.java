package tdm.xserver;

class X11Color
{
    short               r;
    short               g;
    short               b;

    void set(byte _r, byte _g, byte _b) {
        r = (short)(_r << 8 | _r);
        g = (short)(_g << 8 | _g);
        b = (short)(_b << 8 | _b);
    }
    void set(short _r, short _g, short _b) {
        r = _r;
        g = _g;
        b = _b;
    }
}
