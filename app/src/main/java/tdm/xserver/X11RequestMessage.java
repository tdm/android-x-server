package tdm.xserver;

import android.util.Log;

import java.nio.ByteOrder;

class X11RequestMessage extends X11Message
{
    static final String[] message_names = {
        "NONE",                         // 0
        "CreateWindow",
        "ChangeWindowAttributes",
        "GetWindowAttributes",
        "DestroyWindow",
        "DestroySubwindows",
        "ChangeSaveSet",
        "ReparentWindow",
        "MapWindow",
        "MapSubwindows",
        "UnmapWindow",                  // 10
        "UnmapSubwindows",
        "ConfigureWindow",
        "CirculateWindow",
        "GetGeometry",
        "QueryTree",
        "InternAtom",
        "GetAtomName",
        "ChangeProperty",
        "DeleteProperty",
        "GetProperty",                  // 20
        "ListProperties",
        "SetSelectionOwner",
        "GetSelectionOwner",
        "ConvertSelection",
        "SendEvent",
        "GrabPointer",
        "UngrabPointer",
        "GrabButton",
        "UngrabButton",
        "ChangeActivePointerGrab",      // 30
        "GrabKeyboard",
        "UngrabKeyboard",
        "GrabKey",
        "UngrabKey",
        "AllowEvents",
        "GrabServer",
        "UngrabServer",
        "QueryPointer",
        "GetMotionEvents",
        "TranslateCoords",              // 40
        "WarpPointer",
        "SetInputFocus",
        "GetInputFocus",
        "QueryKeymap",
        "OpenFont",
        "CloseFont",
        "QueryFont",
        "QueryTextExtents",
        "ListFonts",
        "ListFontsWithInfo",            // 50
        "SetFontPath",
        "GetFontPath",
        "CreatePixmap",
        "FreePixmap",
        "CreateGC",
        "ChangeGC",
        "CopyGC",
        "SetDashes",
        "SetClipRectangles",
        "FreeGC",                       // 60
        "ClearArea",
        "CopyArea",
        "CopyPlane",
        "PolyPoint",
        "PolyLine",
        "PolySegment",
        "PolyRectangle",
        "PolyArc",
        "FillPoly",
        "PolyFillRectangle",            // 70
        "PolyFillArc",
        "PutImage",
        "GetImage",
        "PolyText8",
        "PolyText16",
        "ImageText8",
        "ImageText16",
        "CreateColormap",
        "FreeColormap",
        "CopyColormapAndFree",          // 80
        "InstallColormap",
        "UninstallColormap",
        "ListInstalledColormaps",
        "AllocColor",
        "AllocNamedColor",
        "AllocColorCells",
        "AllocColorPlanes",
        "FreeColors",
        "StoreColors",
        "StoreNamedColor",              // 90
        "QueryColors",
        "LookupColor",
        "CreateCursor",
        "CreateGlyphCursor",
        "FreeCursor",
        "RecolorCursor",
        "QueryBestSize",
        "QueryExtension",
        "ListExtensions",
        "ChangeKeyboardMapping",        // 100
        "GetKeyboardMapping",
        "ChangeKeyboardControl",
        "GetKeyboardControl",
        "Bell",
        "ChangePointerControl",
        "GetPointerControl",
        "SetScreenSaver",
        "GetScreenSaver",
        "ChangeHosts",
        "ListHosts",                    // 110
        "SetAccessControl",
        "SetCloseDownMode",
        "KillClient",
        "RotateProperties",
        "ForceScreenSaver",
        "SetPointerMapping",
        "GetPointerMapping",
        "SetModifierMapping",
        "GetModifierMapping",
        "120",                          // 120
        "121",
        "122",
        "123",
        "124",
        "125",
        "126",
        "NoOperation"
    };

    String name() { return message_names[mRequestType]; }

    byte                mRequestType;
    byte                mHeaderData;
    boolean             mBigReq;

    X11RequestMessage(ByteOrder endian, boolean bigreq) {
        super(endian);
        mBigReq = bigreq;
    }

    void read(ByteQueue q) throws Exception {
        mRequestType = q.deqByte();
        mHeaderData = q.deqByte();
        short reqlen = q.deqShort();
        if (reqlen == 0) {
            if (!mBigReq) {
                throw new Exception("X11 protocol error: invalid message length");
            }
            int bigreqlen = q.deqInt();
            if (bigreqlen < 2) {
                throw new Exception("X11 protocol error: invalid message length");
            }
            mData = q.deqData(bigreqlen*4-8);
        }
        else {
            mData = q.deqData((int)reqlen*4-4); //XXX: cast needed and functional?
        }
    }
    void write(ByteQueue q) {
        q.enqByte(mRequestType);
        q.enqByte(mHeaderData);

        int reqlen = ((4+mData.length())+3)/4;
        if (reqlen > 0xffff) {
            if (!mBigReq) {
                //XXX: throw new Exception("X11 protocol error: message too big");
                System.exit(-1);
            }
            reqlen += 1;
            q.enqShort((short)0);
            q.enqInt(reqlen);
        }
        else {
            q.enqShort((short)reqlen);
        }
        q.enqData(mData);
        q.enqAlign(4);
    }
    void dispatch(X11ProtocolHandler h) { h.onMessage(this); }

    void requestType(byte val) { mRequestType = val; }
    byte requestType() { return mRequestType; }
    void headerData(byte val) { mHeaderData = val; }
    byte headerData() { return mHeaderData; }
}
