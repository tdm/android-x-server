package tdm.xserver;

import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

class X11Font extends X11Fontable
{
    class CacheKey {
        char            ch;
        int             fore;
        int             back;

        CacheKey(char c, int f, int b) { ch = c; fore = f; back = b; }
        public int hashCode() { return ((ch << 24) | (fore ^ back)); }
        public boolean equals(Object obj) {
            CacheKey other = (CacheKey)obj;
            return (other.ch == ch && other.fore == fore && other.back == back);
        }
    }

    FontData                    mData;
    Map<CacheKey,Bitmap>        mImageCache;

    X11Font(int id, FontData data) {
        super(X11Resource.FONT, id);
        mData = data;
        mImageCache = new HashMap<CacheKey,Bitmap>();
    }

    void destroy() {
        mImageCache = null;
        mData = null;
        super.destroy();
    }

    void handleCloseFont(X11Client c, X11RequestMessage msg) {
        c.delResource(mId);
    }

    void handleQueryFont(X11Client c, X11RequestMessage msg) {
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        mData.enqueueInfo(reply.mData);
        mData.enqueueCharInfoCount(reply.mData);
        mData.enqueueProperties(reply.mData);
        mData.enqueueCharInfo(reply.mData);
        c.send(reply);
    }

    X11CharInfo getCharInfo(char ch) {
        if (ch < mData.mCharInfos.size()) {
            return mData.mCharInfos.get(ch);
        }
        return null;
    }

    Bitmap getCharImage(char ch, int fore, int back) {
        CacheKey key = new CacheKey(ch, fore, back);
        Bitmap bmp = mImageCache.get(key);
        if (bmp == null) {
            Bitmap glyph = mData.getBitmap(ch);
            if (glyph == null) {
                return null;
            }
            int w = glyph.getWidth();
            int h = glyph.getHeight();
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int x, y;
            for (y = 0; y < h; ++y) {
                for (x = 0; x < w; ++x) {
                    int pixel = glyph.getPixel(x, y);
                    bmp.setPixel(x, y, (pixel == Color.BLACK ? back : fore));
                }
            }
            mImageCache.put(key, bmp);
        }
        return bmp;
    }

    void showBitmap(char ch) {
        Log.d("X", "Show bitmap for <"+ch+">...");
        X11CharInfo info = getCharInfo(ch);
        Bitmap bmp = mData.getBitmap(ch);
        if (bmp == null) {
            Log.d("X", "  (null)");
            return;
        }
        Log.d("X", "  w="+bmp.getWidth()+", h="+bmp.getHeight()+", a="+info.ascent+", d="+info.descent+" ...");
        for (int y = 0; y < bmp.getHeight(); ++y) {
            StringBuffer buf = new StringBuffer();
            for (int x = 0; x < bmp.getWidth(); ++x) {
                if (bmp.getPixel(x, y) == Color.BLACK) {
                    buf.append(".");
                }
                else {
                    buf.append("X");
                }
            }
            Log.d("X", "  " + buf);
        }
    }
}
