package tdm.xserver;

import java.nio.ByteOrder;

abstract class X11Message
{
    ByteQueue           mData;

    abstract void read(ByteQueue q) throws Exception;
    abstract void write(ByteQueue q);
    abstract void dispatch(X11ProtocolHandler h);

    X11Message(ByteOrder endian) {
        mData = new ByteQueue(32-4);
        mData.endian(endian);
    }

    int QUADLEN(int val) {
        return ((val+3)/4);
    }
}
