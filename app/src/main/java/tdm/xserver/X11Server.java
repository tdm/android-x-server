package tdm.xserver;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import android.util.DisplayMetrics;
import android.view.Display;

import androidx.annotation.Nullable;

import com.chedim.android.xserver.HomeActivity;
import com.chedim.android.xserver.R;
import com.chedim.android.xserver.X11WindowActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class X11Server extends Service
{
    public static final String X11WINDOW_ID = "X11WINDOW_ID";
    private static final int ONGOING_NOTIFICATION_ID = 1234;
    private static final String NOTIFICATION_CHANNEL_ID = "com.chedim.android.xserver";
    private static final String TAG = X11Server.class.getName();
    private static X11Server mInstance;
    private static final Map<Integer, UIHandler>      mUIHandlers = new ConcurrentHashMap<>();

    Context                     mContext;

    X11Client[]                 mClients;

    Map<Integer,X11Visual>      mVisuals;
    ArrayList<X11Format>        mPixmapFormats;

    Map<Integer,X11Atom>        mAtomsById;
    Map<String,X11Atom>         mAtomsByName;
    int                         mLastAtomId;

    Map<Integer,X11Selection>   mSelections;

    Map<String,FontData>        mFonts;
    Map<String,String>          mFontAliases;

    boolean                     mRunning;
    long                        mStartTime;
    ServerSocket                mSock;

    X11Keyboard                 mKeyboard;

    X11Screen                   mDefaultScreen;
    X11Window                   mInputFocus;

    X11Client                   mGrabClient;
    X11Client                   mGrabKey;

    public static X11Server getInstance() {
        return mInstance;
    }

    public X11Server() {
    }

    public void run() {
        mInstance = this;
        mClients = new X11Client[X11Client.MAX_CLIENTS];
        mClients[0] = new X11Client(this);

        mVisuals = new HashMap<Integer,X11Visual>();

        mPixmapFormats = new ArrayList();
        mPixmapFormats.add(new X11Format_1()); //XXX
        mPixmapFormats.add(new X11Format_4());
        mPixmapFormats.add(new X11Format_8());
        mPixmapFormats.add(new X11Format_16());
        mPixmapFormats.add(new X11Format_24());
        mPixmapFormats.add(new X11Format_32());

        X11Colormap.globalInit(this);

        mAtomsById = new HashMap<Integer,X11Atom>();
        mAtomsByName = new HashMap<String,X11Atom>();
        mLastAtomId = 0;
        X11Atom.globalInit(this);

        mSelections = new HashMap<Integer,X11Selection>();

        mFonts = new HashMap<String,FontData>();
        mFontAliases = new HashMap<String,String>();
        FontData.globalInit(this);

        // NB: PixelFormat (see dpy.getPixelFormat()) has bitsPerPixel/bytesPerPixel etc.
        WindowManager wm =  (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display dpy = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        dpy.getMetrics(dm);

        mKeyboard = new X11Keyboard();

        try {
            mDefaultScreen = new X11Screen(this, mClients[0],
                (short)dm.widthPixels,
                (short)dm.heightPixels,
                (short)dm.densityDpi);
        }
        catch (X11Error e) {
            Log.e(TAG, "Cannot create screen", e);
            return;
        }

        try {
            mSock = new ServerSocket(6000);
        }
        catch (Exception e) {
            Log.e(TAG, "Cannot create server socket");
            return;
        }

        mStartTime = System.currentTimeMillis();
        mRunning = true;
        while (mRunning) {
            try {
                for (int i = 1; i < X11Client.MAX_CLIENTS; ++i) {
                    if (mClients[i] == null) {
                        X11Client c = new X11Client(this, i, mSock.accept());
                        mClients[i] = c;
                        c.start();
                        break;
                    }
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Exception in run");
                mRunning = false;
                continue;
            }
        }
    }

    void onStop() {
        mRunning = false;
    }

    int currentTime() {
        return (int)(System.currentTimeMillis() - mStartTime);
    }

    void clientClosed(X11Client c) {
        //XXX: locking
        if (mGrabClient == c) {
            mGrabClient = null;
        }
        //XXX: delete selections
        //XXX: ...???
        for (int i = 1; i < X11Client.MAX_CLIENTS; ++i) {
            if (mClients[i] == c) {
                c = null;
                break;
            }
        }
    }

    void addVisual(X11Visual v) throws X11Error {
        if (mVisuals.containsKey(v.mId)) {
            throw new X11Error(X11Error.IDCHOICE, v.mId);
        }
        mVisuals.put(v.mId, v);
    }

    void delVisual(int id) {
        mVisuals.remove(id);
    }

    X11Visual getVisual(int id) {
        return mVisuals.get(id);
    }

    ArrayList<X11Format> getPixmapFormats() {
        return mPixmapFormats;
    }

    void registerFont(FontData f) {
        mFonts.put(f.mName, f);
    }
    void registerFontAlias(String alias, String name) {
        mFontAliases.put(alias, name);
    }

    void handleInternAtom(X11Client c, X11RequestMessage msg) {
        byte onlyifexist = msg.headerData();
        if (msg.mData.remain() == 0) {
            X11ReplyMessage reply = new X11ReplyMessage(msg);
            c.send(reply);
            return;
        }
        short namelen = msg.mData.deqShort();
        msg.mData.deqSkip(2);
        String name = msg.mData.deqString(namelen);

        int id = X11Atom.NONE;
        boolean created = false;
        if (!mAtomsByName.containsKey(name)) {
            if (onlyifexist == 0 /* False */) {
                id = doInternAtom(name);
                created = true;
            }
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqInt(id);
        c.send(reply);
    }

    void handleSetSelectionOwner(X11Client c, X11RequestMessage msg) throws X11Error {
        X11Window owner = c.getWindow(msg.mData.deqInt());
        X11Atom selection_name = mAtomsById.get(msg.mData.deqInt());
        int time = msg.mData.deqInt();

        X11Selection selection = mSelections.get(selection_name.mId);
        if (selection == null) {
            selection = new X11Selection();
            selection.mSelection = selection_name;
            selection.mOwnerClient = c;
            selection.mOwnerWindow = owner;
            selection.mLastChangeTime = currentTime();
            mSelections.put(selection_name.mId, selection);
        }

        if (time < selection.mLastChangeTime) {
            return;
        }

        if (selection.mOwnerClient != null && selection.mOwnerClient != c) {
            X11EventMessage event = new X11EventMessage(selection.mOwnerClient.mProt.mEndian, X11Event.SELECTION_CLEAR);
            event.mData.enqInt(time);
            event.mData.enqInt(owner.mId);
            event.mData.enqInt(selection_name.mId);
            selection.mOwnerClient.send(event);
        }

        selection.mLastChangeTime = time;
        if (owner == null) {
            selection.mOwnerClient = null;
            selection.mOwnerWindow = null;
        }
        else {
            selection.mOwnerClient = c;
            selection.mOwnerWindow = owner;
        }
    }

    void handleGetSelectionOwner(X11Client c, X11RequestMessage msg) {
        int id = X11Resource.NONE;
        X11Selection selection = mSelections.get(msg.mData.deqInt());
        if (selection != null) {
            id = selection.mOwnerWindow.mId;
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqInt(id);
        c.send(reply);
    }

    void handleGrabServer(X11Client c, X11RequestMessage msg) throws X11Error {
        if (mGrabClient != null) {
            //XXX: handle recursion?
            Log.e("X", "Error: GrabServer while server is grabbed");
            throw new X11Error(X11Error.IMPLEMENTATION, 0);
        }
        mGrabClient = c;
    }

    void handleUngrabServer(X11Client c, X11RequestMessage msg) throws X11Error {
        if (mGrabClient != c) {
            //XXX: ignore this?
            Log.e("X", "Error: UngrabServer while server is not grabbed");
            throw new X11Error(X11Error.IMPLEMENTATION, 0);
        }
        mGrabClient = null;
    }

    void handleSetInputFocus(X11Client c, X11RequestMessage msg) throws X11Error {
        //XXX
        byte revert_to = msg.headerData();
        X11Window w = c.getWindow(msg.mData.deqInt());
        int timestamp = msg.mData.deqInt();
        mInputFocus = w;
    }

    void handleGetInputFocus(X11Client c, X11RequestMessage msg) {
        //XXX
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData((byte)0); // revert-to = None
        reply.mData.enqInt(mInputFocus.mId); // None
        c.send(reply);
    }

    void handleOpenFont(X11Client c, X11RequestMessage msg) throws X11Error {
        int fid = msg.mData.deqInt();
        short len = msg.mData.deqShort();
        msg.mData.deqSkip(2);
        String name = msg.mData.deqString(len);
        name = name.toLowerCase();

        if (mFontAliases.containsKey(name)) {
            Log.d("X", "OpenFont: alias: <" + name + "> => <" + mFontAliases.get(name) + ">");
            name = mFontAliases.get(name);
        }

        FontData data = null;
        for (FontData i : mFonts.values()) {
            if (i.match(name)) {
                data = i;
                break;
            }
        }

        if (data == null) {
            Log.d("X", "OpenFont: cannot find font <" + name + ">: trying fixed instead");
            data = mFonts.get(mFontAliases.get("fixed"));
        }

        if (data == null) {
            Log.d("X", "OpenFont: cannot find font <" + name + ">");
            throw new X11Error(X11Error.NAME, fid);
        }
        Log.d("X", "OpenFont: opening " + data.mName);
        try {
            data.loadMetadata(this);
            data.loadGlyphs(this);
        }
        catch (Exception e) {
            Log.e("X", "OpenFont: failed to load metadata");
            e.printStackTrace();
            throw new X11Error(X11Error.IMPLEMENTATION, 0);
        }
        X11Font font = new X11Font(fid, data);
        c.addResource(font);
    }

    void handleListFonts(X11Client c, X11RequestMessage msg) {
        short maxnames = msg.mData.deqShort();
        short len = msg.mData.deqShort();
        String name = msg.mData.deqString(len);
        name = name.toLowerCase();

        ArrayList<String> v = new ArrayList<String>();

        for (FontData f : mFonts.values()) {
            if (f.match(name)) {
                v.add(f.mName);
                if (v.size() == maxnames) {
                    break;
                }
            }
        }

        //XXX: how should this be done?
        if (v.size() < maxnames) {
            String realname = mFontAliases.get(name);
            if (realname != null) {
                v.add(realname); //XXX?
            }
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqShort((short)v.size());
        reply.mData.enqSkip(22);

        for (String s : v) {
            reply.mData.enqByte((byte)s.length());
            reply.mData.enqString(s);
        }
        reply.mData.enqAlign(4);
        c.send(reply);
    }

    void handleListFontsWithInfo(X11Client c, X11RequestMessage msg) {
        short maxnames = msg.mData.deqShort();
        short len = msg.mData.deqShort();
        String pattern = msg.mData.deqString(len);
        pattern = pattern.toLowerCase();

        ArrayList<FontData> v = new ArrayList<FontData>();

        for (FontData f : mFonts.values()) {
            if (f.match(pattern)) {
                try {
                    f.loadMetadata(this);
                    v.add(f);
                    if (v.size() == maxnames) {
                        break;
                    }
                }
                catch (Exception e) {
                    Log.e("X", "Failed to read font " + f.mName);
                }
            }
        }

        X11ReplyMessage reply = new X11ReplyMessage(msg);

        for (int n = 0; n < v.size(); ++n) {
            FontData f = v.get(n);
            reply.mData.clear();
            f.enqueueInfo(reply.mData);
            reply.mData.enqInt(v.size() - n);
            f.enqueueProperties(reply.mData);
            reply.mData.enqString(f.mName);
            reply.headerData((byte)f.mName.length());
            c.send(reply);
        }

        reply.mData.clear();
        reply.mData.enqSkip(52);
        reply.headerData((byte)0);
        c.send(reply);
    }

    void handleQueryExtension(X11Client c, X11RequestMessage msg) {
        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqByte((byte)0); // present
        reply.mData.enqByte((byte)0); // major-opcode
        reply.mData.enqByte((byte)0); // first-event
        reply.mData.enqByte((byte)0); // first-error
        c.send(reply);
    }

    void doInternAtom(int id, String name) {
        X11Atom atom = new X11Atom(id, name);
        mAtomsById.put(id, atom);
        mAtomsByName.put(name, atom);
    }

    int doInternAtom(String name) {
        int id = ++mLastAtomId;
        doInternAtom(id, name);
        return id;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mInstance == null) {
            Log.i(TAG, "starting X11 service...");
            mContext = this;
            Intent notificationIntent = new Intent(this, HomeActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);

            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "X11 Server", NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);


            Notification notification =
                    new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(getText(R.string.notification_title))
                            .setContentText(getText(R.string.notification_message))
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentIntent(pendingIntent)
                            .setTicker(getText(R.string.notification_message))
                            .build();

            // Notification ID cannot be 0.
            startForeground(ONGOING_NOTIFICATION_ID, notification);
            new Thread(() -> run()).start();
            Log.i(TAG, "Launched X11 service");
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void registerUiHandler(int w, UIHandler handler) {
        mUIHandlers.put(w, handler);
        if (X11Window.windows.containsKey(w)) {
            X11Window.windows.get(w).updateHandler(handler);
        }
    }

    public static UIHandler getUiHandler(int X11Window) {
        return mUIHandlers.get(X11Window);
    }

    public static void launchActivity(int X11Window) {
        Intent launcher = new Intent(getInstance().mContext, X11WindowActivity.class);
        launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        launcher.putExtra(X11WINDOW_ID, X11Window);
        getInstance().mContext.startActivity(launcher);
    }

    public void handleGrabKey(X11Client x11Client, X11RequestMessage msg) {
        mGrabKey = x11Client;
    }

    public void handleQueryTree(X11Client x11Client, X11RequestMessage msg) {
        int handle = msg.mData.deqInt();
        X11Window window = X11Window.windows.get(handle);

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.mData.enqInt(X11Resource.ID_ROOT_WINDOW);
        reply.mData.enqInt(window.mParent != null ? window.mParent.mId : X11Resource.ID_ROOT_WINDOW);
        reply.mData.enqShort((short) window.mChildren.size());
        for (X11Window child : window.mChildren) {
            reply.mData.enqInt(child.mId);
        }
        x11Client.send(reply);
    }

    public void handleSendEvent(X11Client x11Client, X11RequestMessage msg) {
        boolean propagate = msg.headerData() > 0;
        short requestLength = msg.mData.deqShort();
        int destId = msg.mData.deqInt();
        int mask = msg.mData.deqInt();
        byte type = msg.mData.deqByte();
        ByteQueue evdata = msg.mData.deqData(msg.mData.remain());
        X11Window dest = null;
        if (destId < 2) {
            dest = mInputFocus;
        } else {
            X11Window.windows.get(destId);
        }
        X11Client dc = dest.mView.mClient;
        X11EventMessage event = new X11EventMessage(dc.mProt.mEndian, type);
        event.mData.enqData(evdata);
        dc.send(event);
    }
}
