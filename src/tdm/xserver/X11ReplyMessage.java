package tdm.xserver;

import java.nio.ByteOrder;

class X11ReplyMessage extends X11Message
{
    byte                mHeaderData;
    short               mSeqNo;

    X11ReplyMessage(X11RequestMessage msg) {
        super(msg.mData.endian());
        mData.resize(28);
    }

    void read(ByteQueue q) {
        byte event_type = q.deqByte(); // 0x01
        mHeaderData = q.deqByte();
        mSeqNo = q.deqShort();
        int exlen = q.deqInt();
        mData = q.deqData(24 + exlen*4);
    }
    void write(ByteQueue q) {
        q.enqByte((byte)0x01);
        q.enqByte(mHeaderData);
        q.enqShort(mSeqNo);

        mData.enqAlign(4);
        int datalen = Math.max(24, mData.pos());
        q.enqInt((datalen-24)/4);
        q.enqData(mData);
        q.enqSkip(datalen - mData.pos());
    }
    void dispatch(X11ProtocolHandler h) { h.onMessage(this); }

    void headerData(byte val) { mHeaderData = val; }
    byte headerData() { return mHeaderData; }
    void seqno(short val) { mSeqNo = val; }
    short seqno() { return mSeqNo; }
}
