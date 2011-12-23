package tdm.xserver;

import java.nio.ByteOrder;

class X11SetupRequest extends X11Message //XXX: extends X11Consumer?
{
    ByteOrder           mByteOrder;
    short               mProtoMajor;
    short               mProtoMinor;
    String              mAuthProtoName;
    byte[]              mAuthProtoData;

    X11SetupRequest(ByteOrder endian) {
        super(endian);
    }

    void read(ByteQueue q) throws Exception {
        short name_len, data_len;

        mByteOrder = (q.deqByte() == (byte)0x42) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        q.deqSkip(1);
        mProtoMajor = q.deqShort();
        mProtoMinor = q.deqShort();
        name_len = q.deqShort();
        data_len = q.deqShort();
        q.deqSkip(2);
        mAuthProtoName = q.deqString(name_len);
        q.deqAlign(4);
        mAuthProtoData = q.deqArray(data_len);
        q.deqAlign(4);
    }
    void write(ByteQueue q) {
        short name_len = (short)mAuthProtoName.length();
        short data_len = (short)mAuthProtoData.length;

        q.enqByte((mByteOrder == ByteOrder.BIG_ENDIAN) ? (byte)0x42 : (byte)0x6c);
        q.enqSkip(1);
        q.enqShort(mProtoMajor);
        q.enqShort(mProtoMinor);
        q.enqShort(name_len);
        q.enqShort(data_len);
        q.enqSkip(2);
        q.enqString(mAuthProtoName);
        q.enqArray(mAuthProtoData);
    }
    void dispatch(X11ProtocolHandler h) { h.onMessage(this); }
}
