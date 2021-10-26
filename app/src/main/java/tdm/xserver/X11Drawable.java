package tdm.xserver;

import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;

import java.util.ArrayList;

abstract class X11Drawable extends X11Resource
{
    byte                mDepth;
    byte                mBPP;
    X11Rect             mRect;
    short               mBorderWidth;   // This is here for GetGeometry
    X11Visual           mVisual;        // This is here for GetImage

    Bitmap              mBitmap;
    Canvas              mCanvas;

    X11Drawable(int type, int id) {
        super(type, id);
    }

    void destroy() {
        mCanvas = null;
        mBitmap = null;
        mVisual = null;
        mRect = null;
        super.destroy();
    }

    void handleGetGeometry(X11Client c, X11RequestMessage msg) {
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData(mDepth);
        reply.mData.enqInt(X11Resource.ID_ROOT_WINDOW);
        reply.mData.enqShort(mRect.x);
        reply.mData.enqShort(mRect.y);
        reply.mData.enqShort(mRect.w);
        reply.mData.enqShort(mRect.h);
        reply.mData.enqShort(mBorderWidth);
        c.send(reply);
    }

    void handleCopyArea(X11Client c, X11RequestMessage msg) throws X11Error {
        X11Drawable dst = c.getDrawable(msg.mData.deqInt());
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        short src_x = msg.mData.deqShort();
        short src_y = msg.mData.deqShort();
        short dst_x = msg.mData.deqShort();
        short dst_y = msg.mData.deqShort();
        short w = msg.mData.deqShort();
        short h = msg.mData.deqShort();

        //XXX: Use Canvas.drawBitmap with a clip mask?
        //XXX: handle window with tiled background
        short x, y;
        final int maxX = Math.min(dst.mBitmap.getWidth() - dst_x, mBitmap.getWidth() - src_x);
        final int maxY = Math.min(dst.mBitmap.getHeight() - dst_y, mBitmap.getHeight() - src_y);
        for (y = 0; y < h; ++y) {
            if (y > maxY) break;
            for (x = 0; x < w; ++x) {
                if (x > maxX) break;
                int pixel = mBitmap.getPixel(src_x + x, src_y + y);
                dst.mBitmap.setPixel(dst_x + x, dst_y + y, pixel);
            }
        }
    }

    void handlePolyPoint(X11Client c, X11RequestMessage msg) throws X11Error {
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        int count = msg.mData.remain()/4;
        float[] points = new float[count*2];
        for (int n = 0; n < count; ++n) {
            points[n*2+0] = (float)msg.mData.deqShort();
            points[n*2+1] = (float)msg.mData.deqShort();
        }
        mCanvas.drawPoints(points, gc.mPaint);
        postRender();
    }

    void handlePolyLine(X11Client c, X11RequestMessage msg) throws X11Error {
        byte coord_mode = msg.headerData();
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        int count = msg.mData.remain()/4 - 1;
        float[] points = new float[count*4];
        float lastx = (float)msg.mData.deqShort();
        float lasty = (float)msg.mData.deqShort();
        for (int n = 0; n < count; ++n) {
            points[n*4+0] = lastx;
            points[n*4+1] = lasty;
            points[n*4+2] = (float)msg.mData.deqShort();
            points[n*4+3] = (float)msg.mData.deqShort();
            if (coord_mode == 0 /* Origin */) {
                lastx = points[n*4+2];
                lasty = points[n*4+3];
            }
            else {
                lastx += points[n*4+2];
                lasty += points[n*4+3];
            }
        }
        mCanvas.drawLines(points, gc.mPaint);
        postRender();
    }

    void handlePolySegment(X11Client c, X11RequestMessage msg) throws X11Error {
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        int count = msg.mData.remain()/8;
        float[] points = new float[count*4];
        for (int n = 0; n < count; ++n) {
            points[n*4+0] = (float)msg.mData.deqShort();
            points[n*4+1] = (float)msg.mData.deqShort();
            points[n*4+2] = (float)msg.mData.deqShort();
            points[n*4+3] = (float)msg.mData.deqShort();
        }
        mCanvas.drawLines(points, gc.mPaint);
        postRender();
    }

    void handlePolyRectangle(X11Client c, X11RequestMessage msg) throws X11Error {
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        while (msg.mData.remain() > 0) {
            short x = msg.mData.deqShort();
            short y = msg.mData.deqShort();
            short w = msg.mData.deqShort();
            short h = msg.mData.deqShort();
            mCanvas.drawRect(x, y, x+w, y+h, gc.mPaint);
        }
        postRender();
    }

    void handlePolyArc(X11Client c, X11RequestMessage msg) throws X11Error {
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        int count = msg.mData.remain()/12;
        float[] points = new float[count*6];
        for (int n = 0; n < count; ++n) {
            points[n*4+0] = (float)msg.mData.deqShort();
            points[n*4+1] = (float)msg.mData.deqShort();
            points[n*4+2] = (float)msg.mData.deqShort();
            points[n*4+3] = (float)msg.mData.deqShort();
            points[n*4+4] = (float)msg.mData.deqShort();
            points[n*4+5] = (float)msg.mData.deqShort();
        }
        throw new X11Error(X11Error.IMPLEMENTATION, 0);
    }

    void handleFillPoly(X11Client c, X11RequestMessage msg) throws X11Error {
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        byte shape = msg.mData.deqByte();
        byte coord_mode = msg.mData.deqByte();
        msg.mData.deqSkip(2);

        Path path = new Path();
        path.setFillType(Path.FillType.WINDING); //XXX: ???

        float x, y;
        x = (float)msg.mData.deqShort();
        y = (float)msg.mData.deqShort();
        path.moveTo(x, y);
        while (msg.mData.remain() > 0) {
            x = (float)msg.mData.deqShort();
            y = (float)msg.mData.deqShort();
            path.lineTo(x, y);
        }
        mCanvas.drawPath(path, gc.mPaint);
        postRender();
    }

    void handlePolyFillRectangle(X11Client c, X11RequestMessage msg) throws X11Error {
        X11GContext gc = c.getGContext(msg.mData.deqInt());

        Paint.Style oldstyle = gc.mPaint.getStyle();
        gc.mPaint.setStyle(Paint.Style.FILL);

        while (msg.mData.remain() > 0) {
            short x = msg.mData.deqShort();
            short y = msg.mData.deqShort();
            short w = msg.mData.deqShort();
            short h = msg.mData.deqShort();
            mCanvas.drawRect(x, y, x+w, y+h, gc.mPaint);
        }
        gc.mPaint.setStyle(oldstyle);
        postRender();
    }

    void handlePolyFillArc(X11Client c, X11RequestMessage msg) throws X11Error {
        throw new X11Error(X11Error.IMPLEMENTATION, msg.requestType());
    }

    void handlePutImage(X11Client c, X11RequestMessage msg) throws X11Error {
        byte fmt = msg.headerData();
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        X11Rect rect = new X11Rect();
        rect.w = msg.mData.deqShort();
        rect.h = msg.mData.deqShort();
        rect.x = msg.mData.deqShort();
        rect.y = msg.mData.deqShort();
        byte left_pad = msg.mData.deqByte();
        byte depth = msg.mData.deqByte();
        msg.mData.deqSkip(2);
        int bitLength = rect.w * rect.h * depth;
        byte[] buf = msg.mData.deqArray(MathX.divceil(bitLength, 8));

        if (fmt == 0 /* Bitmap */) {
            if (depth != (byte)1) {
                throw new X11Error(X11Error.VALUE, depth);
            }
            fmt = (byte)1 /* XYPixmap */;
        }

        ArrayList<X11Format> formats = c.mServer.getPixmapFormats();
        X11Format f = null;
        for (X11Format cur : formats) {
            if (cur.mDepth == depth) {
                f = cur;
                break;
            }
        }
        //XXX: handle not found

        Bitmap bmp;

        switch (fmt) {
        case 1 /* XYPixmap */:
            bmp = f.decodeImageXY(rect.w, rect.h, buf);
            break;
        case 2 /* ZPixmap */ :
            bmp = f.decodeImageZ(rect.w, rect.h, buf);
            break;
        default:
            throw new X11Error(X11Error.VALUE, fmt);
        }

        mCanvas.drawBitmap(bmp, rect.x, rect.y, null);
        postRender(rect);
    }

    void handleGetImage(X11Client c, X11RequestMessage msg) throws X11Error {
        byte fmt = msg.headerData();
        X11Rect rect = new X11Rect();
        rect.x = msg.mData.deqShort();
        rect.y = msg.mData.deqShort();
        rect.w = msg.mData.deqShort();
        rect.h = msg.mData.deqShort();
        int plane_mask = msg.mData.deqInt();

        rect.x = rect.x < 0 ? 0 : rect.x;
        rect.y = rect.y < 0 ? 0 : rect.y;
        rect.w = rect.w > mBitmap.getWidth() ? (short) mBitmap.getWidth() : rect.w;
        rect.h = rect.h > mBitmap.getHeight() ? (short) mBitmap.getHeight() : rect.h;

        ArrayList<X11Format> formats = c.mServer.getPixmapFormats();
        X11Format f = null;
        for (X11Format cur : formats) {
            if (cur.mDepth == mDepth) {
                f = cur;
                break;
            }
        }
        //XXX: handle not found

        byte[] data;

        switch (fmt) {
        case 1 /* XYPixmap */:
            data = f.encodeImageXY(rect, plane_mask, mBitmap);
            break;
        case 2 /* ZPixmap */ :
            data = f.encodeImageZ(rect, plane_mask, mBitmap);
            break;
        default:
            throw new X11Error(X11Error.VALUE, fmt);
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData(mDepth);
        //XXX: This looks ugly, is it the best way?
        reply.mData.enqInt( (mVisual == null ? 0 : mVisual.mId) );
        reply.mData.enqSkip(20);
        reply.mData.enqArray(data);
        c.send(reply);
    }

    void handleImageText8(X11Client c, X11RequestMessage msg) throws X11Error {
        byte len = msg.headerData();
        X11GContext gc = c.getGContext(msg.mData.deqInt());
        short x = msg.mData.deqShort();
        short y = msg.mData.deqShort();
        String text = msg.mData.deqString(len);

        short x_min, x_max, y_min, y_max;
        x_min = x;
        x_max = x;
        y_min = y;
        y_max = y;
        for (int idx = 0; idx < text.length(); ++idx) {
            char ch = text.charAt(idx);
            X11CharInfo info = gc.mFont.getCharInfo(ch);
            Bitmap bmp = gc.mFont.getCharImage(ch, gc.mForePixel, gc.mBackPixel);
            if (bmp != null) {
                //XXX: This is probably not correct
                mCanvas.drawBitmap(bmp, x, y-bmp.getHeight(), null);
            }
            x += info.character_width;

            x_max += info.character_width;
            y_min = (short)Math.min(y_min, y-bmp.getHeight());
            //y_max = (short)Math.max(y_max, y+info.descent);
        }

        X11Rect r = new X11Rect();
        r.x = x_min;
        r.w = (short)(x_max - x_min);
        r.y = y_min;
        r.h = (short)(y_max - y_min);

        postRender(r);
    }

    X11Rect drawText(short x, short y, X11GContext ctx, String text) {
        X11Font font = ctx.mFont;
        short w = 0, h = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            X11CharInfo info = font.getCharInfo(ch);
            if (info == null) {
                ch = 0;
                info = font.getCharInfo(ch);
                if (info == null) {
                    continue;
                }
            }
            Bitmap bmp = font.getCharImage(ch, ctx.mForePixel, ctx.mBackPixel);
            if (bmp != null) {
                mCanvas.drawBitmap(bmp, x + w, y - bmp.getHeight(), null);
            }
            w += info.character_width;
            h = (short) bmp.getHeight();
        }

        X11Rect r = new X11Rect(x, y, w, h);
        postRender(r);
        return r;
    }

    void postRender(X11Rect r) {}
    void postRender() {}

    public void handlePolyText8(X11Client c, X11RequestMessage msg) throws X11Error {
       X11GContext gc = c.getGContext(msg.mData.deqInt());
       short x = msg.mData.deqShort(),
               y = msg.mData.deqShort();

       while(msg.mData.remain() > 1) {
          int strLen = Byte.toUnsignedInt(msg.mData.deqByte());
          if (strLen == 255) {
              // font change
              int font = msg.mData.deqInt();
              gc.mFont = c.getFont(font);
          } else {
              x += Byte.toUnsignedInt(msg.mData.deqByte());
              String text = msg.mData.deqString(strLen);
              X11Rect box = drawText(x, y, gc, text);
              x += box.w;
          }
       }
    }
}
