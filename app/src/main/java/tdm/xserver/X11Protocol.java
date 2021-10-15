package tdm.xserver;

import java.net.Socket;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.SocketException;
import java.lang.Exception;
import java.nio.ByteOrder;

interface X11ProtocolHandler
{
    void onMessage(X11SetupRequest msg);
    void onMessage(X11SetupResponse msg);
    void onMessage(X11RequestMessage msg);
    void onMessage(X11ReplyMessage msg);
    void onMessage(X11EventMessage msg);
    void onMessage(X11ErrorMessage msg);
}

class X11Protocol
{
    X11ProtocolHandler          mHandler;
    Socket                      mSock;
    ByteOrder                   mEndian;
    ByteQueue                   mQueue;
    short                       mSendSeqNo;
    short                       mRecvSeqNo;
    boolean                     mBigReq;

    X11Protocol(X11ProtocolHandler handler, Socket sock) {
        mHandler = handler;
        mSock = sock;

        try {
            sock.setTcpNoDelay(true);
        }
        catch (SocketException e) {
            // Ignore
        }
    }

    void close() {
        try {
            mSock.close();
        }
        catch (IOException e) {
            // Ignore
        }
    }

    void send(X11Message msg) throws IOException {
        ByteQueue q = new ByteQueue(32);
        q.endian(mEndian);
        msg.write(q);
        mSock.getOutputStream().write(q.getBytes());
    }

    void read() throws IOException {
        byte[] buf = new byte[1500];
        int len = mSock.getInputStream().read(buf, 0, buf.length);
        if (len < 0) {
            throw new EOFException();
        }
        if (mQueue == null) {
            if (buf[0] == 0x42) {
                mEndian = ByteOrder.BIG_ENDIAN;
            }
            else {
                mEndian = ByteOrder.LITTLE_ENDIAN;
            }
            mQueue = new ByteQueue(32);
            mQueue.endian(mEndian);
        }
        mQueue.pos(mQueue.length());
        mQueue.enqArray(buf, 0, len);

        mQueue.pos(0);
        while (mQueue.remain() >= 4) {
            X11Message msg = null;
            if (mRecvSeqNo == 0) {
                msg = new X11SetupRequest(mEndian);
            }
            else {
                msg = new X11RequestMessage(mEndian, mBigReq);
            }

            int oldpos = mQueue.pos();
            try {
                msg.read(mQueue);
            }
            catch (Exception e) {
                mQueue.pos(oldpos);
                break;
            }

            msg.dispatch(mHandler);
            ++mRecvSeqNo;
        }

        mQueue.compact();
    }
}
