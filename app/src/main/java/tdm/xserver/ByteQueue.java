package tdm.xserver;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ByteQueue
{
    private static final String TAG = ByteQueue.class.getName();
    ByteBuffer          mData;

    ByteQueue() {
        mData = ByteBuffer.allocate(1);
    }
    ByteQueue(int len) {
        mData = ByteBuffer.allocate(len);
        mData.limit(0);
    }
    ByteQueue(byte[] src, int pos, int len) {
        mData = ByteBuffer.allocate(len);
        mData.put(src, pos, len);
        mData.position(0);
    }
    ByteQueue(byte[] src) {
        mData = ByteBuffer.allocate(src.length);
        mData.put(src);
        mData.position(0);
    }

    void dump() {
        Log.e(TAG, "ByteQueue.dump: limit=" + mData.limit() +
                ", capacity=" + mData.capacity() + ", pos=" + mData.position());
        StringBuffer hexbuf = new StringBuffer();
        int x, y;
        for (y = 0; y < mData.limit(); y += 16) {
            hexbuf.append(String.format("%04x: ", y));
            for (x = 0; x < 15 && y+x < mData.limit(); ++x) {
                if (x > 0) {
                    hexbuf.append(" ");
                }
                hexbuf.append(String.format("%02x", mData.get(y+x)));
            }
            Log.e(TAG, "    " + hexbuf);
            hexbuf = new StringBuffer();
        }
    }

    void clear() { mData.position(0); mData.limit(0); }
    void resize(int len) {
        if (len == 0) {
            Log.e("X", "ByteQueue.resize: new len is zero");
            len = 1;
        }
        if (mData.capacity() < len) {
            int newlen = mData.capacity();
            if (newlen == 0) {
                Log.e("X", "ByteQueue.resize: capacity is zero");
                Thread.currentThread().dumpStack();
                newlen = 1;
            }
            while (newlen < len) { newlen *= 2; } //XXX: better way? overflow?
            ByteBuffer newbuf = ByteBuffer.allocate(newlen);
            newbuf.order(mData.order());
            System.arraycopy(mData.array(), 0, newbuf.array(), 0, mData.limit());
            newbuf.position(mData.position());
            mData = newbuf;
        }
        mData.limit(len);
    }

    ByteOrder endian() { return mData.order(); }
    void endian(ByteOrder val) { mData.order(val); }

    int pos() { return mData.position(); }
    void pos(int val) { mData.position(val); }

    int length() { return mData.limit(); }
    int remain() { return length() - pos(); }

    void compact() {
        mData.compact();
        mData.limit(mData.position());
    }

    byte[] getBytes() {
        byte[] b = new byte[mData.position()];
        System.arraycopy(mData.array(), 0, b, 0, mData.position());
        return b;
    }

    void deqSkip(int n) {
        int pos = mData.position();
        mData.position(pos+n);
    }
    void deqAlign(int n) {
        int pos = mData.position();
        mData.position(MathX.roundup(pos, n));
    }

    byte deqByte() { return mData.get(); }
    short deqShort() { return mData.getShort(); }
    int deqInt() { return mData.getInt(); }

    byte[] deqArray(int len) {
        byte[] val = new byte[len];
        mData.get(val);
        return val;
    }
    ByteQueue deqData(int len) {
        byte[] val = deqArray(len);
        ByteQueue data = new ByteQueue(val);
        data.mData.order(mData.order());
        return data;
    }
    String deqString(int len) {
        byte[] val = deqArray(len);
        return new String(val);
    }

    void enqSkip(int n) {
        int pos = mData.position();
        int newpos = pos+n;
        resize(newpos);
        mData.position(newpos);
    }
    void enqAlign(int n) {
        int pos = mData.position();
        int newpos = MathX.roundup(pos, n);
        resize(newpos);
        mData.position(newpos);
    }

    void enqByte(byte val) { resize(mData.position() + 1); mData.put(val); }
    void enqShort(short val) { resize(mData.position() + 2); mData.putShort(val); }
    void enqInt(int val) { resize(mData.position() + 4); mData.putInt(val); }

    void enqArray(byte[] val, int pos, int len) {
        resize(mData.position() + len);
        mData.put(val, pos, len);
    }
    void enqArray(byte[] val) {
        resize(mData.position() + val.length);
        mData.put(val);
    }
    void enqData(ByteQueue val) {
        resize(mData.position() + val.pos());
        mData.put(val.mData.array(), 0, val.pos());
    }
    void enqString(String val) {
        enqArray(val.getBytes());
    }
}
