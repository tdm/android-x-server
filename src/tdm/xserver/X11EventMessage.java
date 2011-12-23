package tdm.xserver;

import java.nio.ByteOrder;

class X11EventMessage extends X11Message
{
    static final String[] message_names = {
        "NONE",
        "NONE",
        "KeyPress",
        "KeyRelease",
        "ButtonPress",
        "ButtonRelease",
        "MotionNotify",
        "EnterNotify",
        "LeaveNotify",
        "FocusIn",
        "FocusOut",                     // 10
        "KeymapNotify",
        "Expose",
        "GraphicsExposure",
        "NoExposure",
        "VisibilityNotify",
        "CreateNotify",
        "DestroyNotify",
        "UnmapNotify",
        "MapNotify",
        "MapRequest",                   // 20
        "ReparentNotify",
        "ConfigureNotify",
        "ConfigureRequest",
        "GravityNotify",
        "ResizeRequest",
        "CirculateNotify",
        "CirculateRequest",
        "PropertyNotify",
        "SelectionClear",
        "SelectionRequest",             // 30
        "SelectionNotify",
        "ColormapNotify",
        "ClientMessage",
        "MappingNotify"
    };

    String name() { return message_names[mEventType]; }

    byte                mEventType;
    byte                mHeaderData;
    short               mSeqNo;

    X11EventMessage(ByteOrder endian, byte evtype) {
        super(endian);
        mEventType = evtype;
        mData.resize(28);
    }

    void read(ByteQueue q) {
        mEventType = q.deqByte();
        mHeaderData = q.deqByte();
        mSeqNo = q.deqShort();
        mData = q.deqData(28);
    }
    void write(ByteQueue q) {
        q.enqByte(mEventType);
        q.enqByte(mHeaderData);
        q.enqShort(mSeqNo);
        q.enqData(mData);
        q.enqSkip(28 - mData.pos());
    }
    void dispatch(X11ProtocolHandler h) { h.onMessage(this); }

    void headerData(byte val) { mHeaderData = val; }
    byte headerData() { return mHeaderData; }
    void seqno(short val) { mSeqNo = val; }
    short seqno() { return mSeqNo; }
}
