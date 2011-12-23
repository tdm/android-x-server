package tdm.xserver;

import java.nio.ByteOrder;

class X11SetupResponse extends X11Message //XXX: extends X11Consumer?
{
    byte                mSuccess;
    short               mProtoMajor;
    short               mProtoMinor;
    String              mReason;

    X11SetupResponse(X11SetupRequest msg) {
        super(msg.mData.endian());
    }

    void read(ByteQueue q) throws Exception {
        mSuccess = q.deqByte();
        if (mSuccess != 1 /* Success */) {
            if (mSuccess == 2 /* Authenticate */) {
                short add_len;
                q.deqSkip(2);
                add_len = q.deqShort();
                mReason = q.deqString(add_len*4);
            }
            else { /* Failed */
                short add_len;
                byte reason_len;
                reason_len = q.deqByte();
                mProtoMajor = q.deqShort();
                mProtoMinor = q.deqShort();
                add_len = q.deqShort();
                mReason = q.deqString(reason_len);
            }
            return;
        }

        q.deqSkip(1);
        mProtoMajor = q.deqShort();
        mProtoMinor = q.deqShort();
        short add_len = q.deqShort();
        mData = q.deqData(add_len*4);
    }
    void write(ByteQueue q) {
        q.enqByte(mSuccess);
        if (mSuccess != 1 /* Success */) {
            if (mSuccess == 2 /* Authenticate */) {
                short add_len = (short)MathX.divceil(mReason.length(), 4);
                q.enqSkip(5);
                q.enqShort(add_len);
                q.enqString(mReason);
            }
            else { /* Failed */
                short add_len = (short)MathX.divceil(mReason.length(), 4);
                byte reason_len = (byte)mReason.length();
                q.enqByte(reason_len);
                q.enqShort(mProtoMajor);
                q.enqShort(mProtoMinor);
                q.enqShort(add_len);
                q.enqString(mReason);
            }
            return;
        }

        q.enqSkip(1);
        q.enqShort(mProtoMajor);
        q.enqShort(mProtoMinor);
        short add_len = (short)MathX.divceil(mData.pos(), 4);
        q.enqShort(add_len);
        q.enqData(mData);
    }
    void dispatch(X11ProtocolHandler h) { h.onMessage(this); }
}
