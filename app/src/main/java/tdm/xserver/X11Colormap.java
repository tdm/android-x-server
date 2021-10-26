package tdm.xserver;

import android.content.res.AssetManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

class X11Colormap extends X11Resource
{
    public static final X11Colormap ROOT = new X11Colormap(0);
    private static final String TAG = X11Colormap.class.getName();

    List<String> names = new ArrayList<>();
    List<X11Color> colors = new ArrayList<>();

    static void globalInit(X11Server server) {


        BufferedReader br;
        String s;

        try {
            AssetManager am = server.mContext.getAssets();
            br = new BufferedReader(new InputStreamReader(am.open("rgb.txt")));
            while ((s = br.readLine()) != null) {
                if (s.length() == 0 || s.charAt(0) == '#' || s.charAt(0) == '!') {
                    continue;
                }
                String[] fields = s.trim().split("[ \t]+", 4);
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
                ROOT.addColor(name, color);
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
                ROOT.addColor(name, color);
            }
        }
        catch (Exception e) {
            Log.e("X", "Failed to init RGB data from the system");
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

    public static void handleAllocNamedColor(X11Client x11Client, X11RequestMessage msg) throws X11Error {
        int cmapId = msg.mData.deqInt();
        short nameLen = msg.mData.deqShort();
        msg.mData.deqShort();
        String name = msg.mData.deqString(nameLen);
        X11Colormap cmap = x11Client.getColormap(cmapId);
        X11Color target = cmap.getColor(name);
        X11Color closest = cmap.allocColor(target.r, target.g, target.b);
        int pixel = cmap.colors.indexOf(closest);

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqInt(pixel);
        reply.mData.enqShort(closest.r);
        reply.mData.enqShort(closest.g);
        reply.mData.enqShort(closest.b);
        reply.mData.enqShort(closest.r);
        reply.mData.enqShort(closest.g);
        reply.mData.enqShort(closest.b);
        reply.mData.enqSkip(8);
        x11Client.send(reply);
    }

    public static void handleAllocColor(X11Client x11Client, X11RequestMessage msg) throws X11Error {
        int cMap = msg.mData.deqInt();
        short r = msg.mData.deqShort(),
                g = msg.mData.deqShort(),
                b = msg.mData.deqShort();

        X11Colormap map = x11Client.getColormap(cMap);
        X11Color closest = map.allocColor(r, g, b);

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqShort(closest.r);
        reply.mData.enqShort(closest.g);
        reply.mData.enqShort(closest.b);
        reply.mData.enqShort((short) 0);
        reply.mData.enqInt(cMap);
        x11Client.send(reply);
    }

    public X11Color allocColor(short r, short g, short b) {
        int minDistance = Integer.MAX_VALUE;
        X11Color closest = null;
        for (X11Color color : colors) {
            int distance = Math.abs(color.r + color.g + color.b - r - g - b);
            if (distance < minDistance) {
                minDistance = distance;
                closest = color;
            }
        }
        return closest;
    }

    public static void handleQueryColors(X11Client x11Client, X11RequestMessage msg) throws X11Error {
        Log.i(TAG, "pixel count:" + (msg.mReqLen -2));
        X11Colormap cmap = x11Client.getColormap(msg.mData.deqInt());
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqShort((short)  (msg.mReqLen - 2));
        reply.mData.enqSkip(22);
        for (int i = 0; i < msg.mReqLen - 2; i++) {
            byte r = msg.mData.deqByte(),
                    g = msg.mData.deqByte(),
                    b = msg.mData.deqByte(),
                    a = msg.mData.deqByte();
            reply.mData.enqShort(r < 0 ? r : cmap.colors.get(r).r);
            reply.mData.enqShort(g < 0 ? g : cmap.colors.get(g).g);
            reply.mData.enqShort(b < 0 ? b : cmap.colors.get(b).b);
            reply.mData.enqSkip(1);
        }
        x11Client.send(reply);
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
        Log.d(TAG, "LookupColor: name=<"+name+">");
        X11Color color = colors.get(names.indexOf(name.toLowerCase()));
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

    public void addColor(X11Color color) {
        names.add("rgb:" + color.r + "/" + color.g + "/" + color.b);
        colors.add(color);
    }

    public void addColor(String name, X11Color color) {
        names.add(name);
        colors.add(color);
    }

    public X11Color getColor(String name) {
        return colors.get(names.indexOf(name));
    }

    public X11Color getColor(int pixel) {
        return colors.get(pixel);
    }
}
