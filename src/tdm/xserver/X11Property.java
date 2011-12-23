package tdm.xserver;

class X11Property
{
    int                 mType;
    byte                mFormat;
    byte[]              mVal;

    X11Property(int t, byte f) {
        mType = t;
        mFormat = f;
        mVal = null;
    }

    void setValue(byte[] buf) {
        mVal = buf;
    }
    void appendValue(byte[] buf) {
        byte[] newval = new byte[mVal.length + buf.length];
        System.arraycopy(mVal, 0, newval, 0, mVal.length);
        System.arraycopy(buf, 0, newval, mVal.length, buf.length);
        mVal = newval;
    }
    void prependValue(byte[] buf) {
        byte[] newval = new byte[mVal.length + buf.length];
        System.arraycopy(buf, 0, newval, 0, buf.length);
        System.arraycopy(mVal, 0, newval, buf.length, mVal.length);
        mVal = newval;
    }
}
