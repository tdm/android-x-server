package tdm.xserver;

import android.content.res.AssetManager;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

class X11Colormap extends X11Resource
{
    static Map<String,X11Color>         rgbmap;

    static void globalInit(X11Server server) {
        rgbmap = new HashMap<String,X11Color>();

        BufferedReader br;
        String s;

        try {
            AssetManager am = server.mContext.getAssets();
            br = new BufferedReader(new InputStreamReader(am.open("rgb.txt")));
            while ((s = br.readLine()) != null) {
                if (s.length() == 0 || s.charAt(0) == '#' || s.charAt(0) == '!') {
                    continue;
                }
                String[] fields = s.trim().split("[ \t]{1,}", 4);
                if (fields.length != 4) {
                    Log.d("X", "Bad line in rgb.txt: " + s);
                    continue;
                }
                X11Color color = new X11Color();
                byte r = (byte)(Short.parseShort(fields[0]) & 0xff);
                byte g = (byte)(Short.parseShort(fields[1]) & 0xff);
                byte b = (byte)(Short.parseShort(fields[2]) & 0xff);
                color.set(r, g, b);
                String name = fields[3].toLowerCase();
                rgbmap.put(name, color);
            }
        }
        catch (Exception e) {
            Log.e("X", "Failed to init RGB data from assets");
        }

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream("/data/local/rgb.txt")));
            while ((s = br.readLine()) != null) {
                if (s.length() == 0 || s.charAt(0) == '#' || s.charAt(0) == '!') {
                    continue;
                }
                String[] fields = s.trim().split("[ \t]{1,}", 4);
                if (fields.length != 4) {
                    Log.d("X", "Bad line in rgb.txt: " + s);
                    continue;
                }
                X11Color color = new X11Color();
                byte r = (byte)(Short.parseShort(fields[0]) & 0xff);
                byte g = (byte)(Short.parseShort(fields[1]) & 0xff);
                byte b = (byte)(Short.parseShort(fields[2]) & 0xff);
                color.set(r, g, b);
                String name = fields[3].toLowerCase();
                rgbmap.put(name, color);
            }
        }
        catch (Exception e) {
            Log.e("X", "Failed to init RGB data from dir");
            e.printStackTrace();
        }
    }

    static void create(X11Client c, X11RequestMessage msg) throws X11Error {
        int id = msg.mData.deqInt();
        X11Colormap r = new X11Colormap(id);
        r.handleCreate(c, msg);
        c.addResource(r);
    }

    int                 mVisual;
    byte                mAlloc;

    X11Colormap(int id) {
        super(X11Resource.COLORMAP, id);
    }

    void destroy() {
        super.destroy();
    }

    void initDefault() {
        mVisual = X11Visual.NONE;
        mAlloc = (byte)1; // All
    }

    void handleCreate(X11Client c, X11RequestMessage msg) {
        //XXX: parse and handle window and visual
        mVisual = X11Visual.NONE;
        mAlloc = (byte)1; // All
    }

    void handleLookupColor(X11Client c, X11RequestMessage msg) throws X11Error {
        short len = msg.mData.deqShort();
        msg.mData.deqSkip(2);
        String name = msg.mData.deqString(len);
        Log.d("X", "LookupColor: name=<"+name+">");
        X11Color color = rgbmap.get(name);
        if (color == null) {
            throw new X11Error(X11Error.NAME, 92 /* LookupColor */); //XXX???
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqShort(color.r);
        reply.mData.enqShort(color.g);
        reply.mData.enqShort(color.b);
        reply.mData.enqShort(color.r);
        reply.mData.enqShort(color.g);
        reply.mData.enqShort(color.b);
        c.send(reply);
    }
}
