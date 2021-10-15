package tdm.xserver;

import java.nio.ByteOrder;

class X11ErrorMessage extends X11Message
{
    static final String[] message_names = {
        "NONE",                         // 0
        "Request",
        "Value",
        "Window",
        "Pixmap",
        "Atom",
        "Cursor",
        "Font",
        "Match",
        "Drawable",
        "Access",                       // 10
        "Alloc",
        "Colormap",
        "GContext",
        "IDChoice",
        "Name",
        "Length",
        "Implementation"
    };

    String name() { return message_names[mHeaderData]; }

    byte                mHeaderData;
    short               mSeqNo;

    X11ErrorMessage(ByteOrder endian, byte code) {
        super(endian);
        mHeaderData = code;
    }

    void read(ByteQueue q) {
        byte event_type = q.deqByte(); // 0x00
        mHeaderData = q.deqByte();
        mSeqNo = q.deqShort();
        mData = q.deqData(28);
    }
    void write(ByteQueue q) {
        q.enqByte((byte)0x00);
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
