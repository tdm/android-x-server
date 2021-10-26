package tdm.xserver;

import android.graphics.Bitmap;

abstract class X11Format
{
    byte                mDepth;
    byte                mBPP;
    byte                mPad;

    X11Format(byte d, byte b, byte p) {
        mDepth = d;
        mBPP = b;
        mPad = p;
    }

    protected void decodePlane(short w, short h, byte[] buf, byte plane, Bitmap bmp) {
        int plane_shift = (mDepth - plane);
        short y, x;
        byte val;

        int plane_off = plane * h * w;
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                val = buf[(plane_off + x + w) * mDepth / 8];
                int shift = (7 - (x%8));

                int tmp = bmp.getPixel(x, y);
                tmp |= ((val >> shift) & 0x1) << plane_shift;
                bmp.setPixel(x, y, tmp);
            }
        }
    }

    Bitmap decodeImageXY(short w, short h, byte[] buf) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        byte plane;
        for (plane = 0; plane < mBPP; ++plane) {
            decodePlane(w, h, buf, plane, bmp);
        }
        return bmp;
    }

    abstract Bitmap decodeImageZ(short w, short h, byte[] buf);

    protected void encodePlane(X11Rect r, Bitmap bmp, byte plane, byte[] buf) {
        int plane_shift = (mDepth - plane);
        int bytes_per_row = MathX.divceil(r.w, 8);
        int bytes_per_plane = r.h * bytes_per_row;
        short y, x;
        int plane_off, y_off, x_off;
        int val;

        plane_off = plane * bytes_per_plane;
        for (y = 0; y < r.h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < r.w; ++x) {
                x_off = x/8;
                val = bmp.getPixel(r.x + x, r.y + y);
                int shift = (7 - (x%8));
                byte v;
                v = buf[plane_off + y_off + x_off];
                v |= ((val >> plane_shift) & 0x1) << shift;
                buf[plane_off + y_off + x_off] = v;
            }
        }
    }

    byte[] encodeImageXY(X11Rect r, int plane_mask, Bitmap bmp) {
        byte[] buf = new byte[r.w*r.h*mBPP/8]; //XXX
        byte plane;
        for (plane = 0; plane < mBPP; ++plane) {
            if ((plane_mask & (1 << plane)) != 0) {
                encodePlane(r, bmp, plane, buf);
            }
        }
        return buf;
    }

    abstract byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp);
}

class X11Format_1 extends X11Format
{
    X11Format_1() { super((byte)1, (byte)1, (byte)8); }

    Bitmap decodeImageZ(short w, short h, byte[] buf) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        decodePlane(w, h, buf, (byte)0, bmp);
        return bmp;
    }

    byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp) {
        int bytes_per_row = MathX.divceil(r.w, 8);
        byte[] buf = new byte[bytes_per_row*r.h];
        if ((plane_mask & 1) != 0) {
            encodePlane(r, bmp, (byte)0, buf);
        }
        return buf;
    }
}

class X11Format_4 extends X11Format
{
    X11Format_4() { super((byte)4, (byte)4, (byte)8); }

    Bitmap decodeImageZ(short w, short h, byte[] buf) {
        int bytes_per_row = MathX.divceil(w, 2);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < w; ++x) {
                x_off = x/2;
                val = buf[y_off + x_off];
                bmp.setPixel(x, y, (val >> (4*(1-x%2))) & 0xf);
            }
        }
        return bmp;
    }

    byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp) {
        int bytes_per_row = MathX.divceil(r.w, 2);
        byte[] buf = new byte[bytes_per_row*r.h];
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < r.h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < r.w; ++x) {
                x_off = x/2;
                val = bmp.getPixel(r.x + x, r.y + y) & 0xf;
                val &= plane_mask;
                buf[y_off + x_off] = (byte)(val << 4*(1-x%2));
            }
        }
        return buf;
    }
}

class X11Format_8 extends X11Format
{
    X11Format_8() { super((byte)8, (byte)8, (byte)8); }

    Bitmap decodeImageZ(short w, short h, byte[] buf) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < h; ++y) {
            y_off = y * w;
            for (x = 0; x < w; ++x) {
                x_off = x;
                val = buf[y_off + x_off];
                bmp.setPixel(x, y, val);
            }
        }
        return bmp;
    }

    byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp) {
        int bytes_per_row = r.w;
        byte[] buf = new byte[bytes_per_row*r.h];
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < r.h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < r.w; ++x) {
                x_off = x;
                val = bmp.getPixel(r.x + x, r.y + y);
                val &= plane_mask;
                buf[y_off + x_off] = (byte)val;
            }
        }
        return buf;
    }
}

class X11Format_16 extends X11Format
{
    X11Format_16() { super((byte)16, (byte)16, (byte)16); }

    Bitmap decodeImageZ(short w, short h, byte[] buf) {
        int bytes_per_row = w*2;
        //XXX: This is wrong.  Use RGB_565 and copyPixelsFromBuffer?
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < w; ++x) {
                x_off = x*2;
                val = (buf[y_off + x_off + 0] << 8) |
                      buf[y_off + x_off + 1];
                bmp.setPixel(x, y, val);
            }
        }
        return bmp;
    }

    byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp) {
        int bytes_per_row = r.w*2;
        byte[] buf = new byte[bytes_per_row*r.h];
        //XXX: This is wrong.  Use RGB_565 and copyPixelsToBuffer?
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < r.h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < r.w; ++x) {
                x_off = x*2;
                val = bmp.getPixel(r.x + x, r.y + y);
                val &= plane_mask;
                buf[y_off + x_off + 0] = (byte)(val >> 8);
                buf[y_off + x_off + 1] = (byte)val;
            }
        }
        return buf;
    }
}

class X11Format_24 extends X11Format
{
    X11Format_24() { super((byte)24, (byte)32, (byte)32); }

    Bitmap decodeImageZ(short w, short h, byte[] buf) {
        int bytes_per_row = w*4;
        //XXX: Use copyPixelsFromBuffer?
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        short x, y;
        int base;
        int val;
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                base = bytes_per_row * y + x*4;
                val = (buf[base + 1] << 16) |
                      (buf[base + 2] <<  8) |
                      buf[base + 3];
                bmp.setPixel(x, y, 0xff000000 | val);
            }
        }
        return bmp;
    }

    byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp) {
        int bytes_per_row = r.w*4;
        byte[] buf = new byte[bytes_per_row*r.h];
        //XXX: Use copyPixelsToBuffer?
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < r.h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < r.w; ++x) {
                x_off = x*4;
                val = bmp.getPixel(r.x + x, r.y + y);
                val &= plane_mask;
                buf[y_off + x_off + 0] = (byte)0;
                buf[y_off + x_off + 1] = (byte)(val >> 16);
                buf[y_off + x_off + 2] = (byte)(val >>  8);
                buf[y_off + x_off + 3] = (byte)val;
            }
        }
        return buf;
    }
}

class X11Format_32 extends X11Format
{
    X11Format_32() { super((byte)32, (byte)32, (byte)32); }

    Bitmap decodeImageZ(short w, short h, byte[] buf) {
        int bytes_per_row = w*4;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < w; ++x) {
                x_off = x*4;
                //XXX: what about alpha?
                val = (buf[y_off + x_off + 0] << 24) |
                      (buf[y_off + x_off + 1] << 16) |
                      (buf[y_off + x_off + 2] <<  8) |
                      buf[y_off + x_off + 3];
                bmp.setPixel(x, y, val);
            }
        }
        return bmp;
    }

    byte[] encodeImageZ(X11Rect r, int plane_mask, Bitmap bmp) {
        int bytes_per_row = r.w*4;
        byte[] buf = new byte[bytes_per_row*r.h];
        short y, x;
        int y_off, x_off;
        int val;
        for (y = 0; y < r.h; ++y) {
            y_off = y * bytes_per_row;
            for (x = 0; x < r.w; ++x) {
                x_off = x*4;
                //XXX: what about alpha?
                val = bmp.getPixel(r.x + x, r.y + y);
                val &= plane_mask;
                buf[y_off + x_off + 0] = (byte)(val >> 24);
                buf[y_off + x_off + 1] = (byte)(val >> 16);
                buf[y_off + x_off + 2] = (byte)(val >>  8);
                buf[y_off + x_off + 3] = (byte)val;
            }
        }
        return buf;
    }
}
