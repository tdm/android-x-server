package tdm.xserver;

import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.view.KeyEvent;

import java.util.ArrayList;

class X11Keyboard
{
    static final byte NO_SYMBOL         = 0;

    static final int X_MIN_KEYCODE      = 8;

    static final int NUM_KEYCODES       = 100;
    static final int NUM_MODIFIERS      = 8;

    /*
     * NB: Many keycodes are undefined in v2.3.
     *     (ESCAPE, Fn keys, numpad, etc.)
     */
    static final int[][] key_codes = {
        { 0, 0 },       // 0
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },       // KEYCODE_CALL
        { 0, 0 },       // KEYCODE_ENDCALL
        { '0', ')' },   // KEYCODE_0
        { '1', '!' },   // KEYCODE_1
        { '2', '@' },   // KEYCODE_2
        { '3', '#' },   // KEYCODE_3    10
        { '4', '$' },   // KEYCODE_4
        { '5', '%' },   // KEYCODE_5
        { '6', '^' },   // KEYCODE_6
        { '7', '&' },   // KEYCODE_7
        { '8', '*' },   // KEYCODE_8
        { '9', '(' },   // KEYCODE_9
        { '*', '*' },   // KEYCODE_STAR (XXX: see KEYCODE_8)
        { '#', '#' },   // KEYCODE_POUND (XXX: see KEYCODE_3)
        { 0, 0 },       // KEYCODE_DPAD_UP
        { 0, 0 },       // KEYCODE_DPAD_DOWN   20
        { 0, 0 },       // KEYCODE_DPAD_LEFT
        { 0, 0 },       // KEYCODE_DPAD_RIGHT
        { 0xffe3, 0 },  // KEYCODE_DPAD_CENTER (=> L.CTRL)
        { 0, 0 },       // KEYCODE_VOLUME_UP
        { 0, 0 },       // KEYCODE_VOLUME_DOWN
        { 0, 0 },       // KEYCODE_POWER
        { 0, 0 },       // KEYCODE_CAMERA
        { 0, 0 },       // KEYCODE_CLEAR
        { 'a', 'A' },   // KEYCODE_A
        { 'b', 'B' },   // KEYCODE_B    30
        { 'c', 'C' },   // KEYCODE_C
        { 'd', 'D' },   // KEYCODE_D
        { 'e', 'E' },   // KEYCODE_E
        { 'f', 'F' },   // KEYCODE_F
        { 'g', 'G' },   // KEYCODE_G
        { 'h', 'H' },   // KEYCODE_H
        { 'i', 'I' },   // KEYCODE_I
        { 'j', 'J' },   // KEYCODE_J
        { 'k', 'K' },   // KEYCODE_K
        { 'l', 'L' },   // KEYCODE_L    40
        { 'm', 'M' },   // KEYCODE_M
        { 'n', 'N' },   // KEYCODE_N
        { 'o', 'O' },   // KEYCODE_O
        { 'p', 'P' },   // KEYCODE_P
        { 'q', 'Q' },   // KEYCODE_Q
        { 'r', 'R' },   // KEYCODE_R
        { 's', 'S' },   // KEYCODE_S
        { 't', 'T' },   // KEYCODE_T
        { 'u', 'U' },   // KEYCODE_U
        { 'v', 'V' },   // KEYCODE_V    50
        { 'w', 'W' },   // KEYCODE_W
        { 'x', 'X' },   // KEYCODE_X
        { 'y', 'Y' },   // KEYCODE_Y
        { 'z', 'Z' },   // KEYCODE_Z
        { ',', '<' },   // KEYCODE_COMMA
        { '.', '>' },   // KEYCODE_PERIOD
        { 0x0101, 0 },  // KEYCODE_ALT_LEFT
        { 0x0102, 0 },  // KEYCODE_ALT_RIGHT
        { 0x0113, 0 },  // KEYCODE_SHIFT_LEFT
        { 0x0114, 0 },  // KEYCODE_SHIFT_RIGHT 60
        { 0xff09, 0xff09 }, // KEYCODE_TAB
        { ' ', ' ' },   // KEYCODE_SPACE
        { 0, 0 },       // KEYCODE_SYM
        { 0, 0 },
        { 0, 0 },       // KEYCODE_ENVELOPE
        { 0xff0d, 0xff0d }, // KEYCODE_ENTER
        { 0xff08, 0xff08 }, // KEYCODE_DEL (backspace)
        { '`', '~' },   // KEYCODE_GRAVE
        { '-', '_' },   // KEYCODE_MINUS
        { '=', '+' },   // KEYCODE_EQUALS 70
        { '[', '{' },   // KEYCODE_LEFT_BRACKET
        { ']', '}' },   // KEYCODE_RIGHT_BRACKET
        { '\\', '|' },  // KEYCODE_BACKSLASH
        { ';', ':' },   // KEYCODE_SEMICOLON
        { '\'', '"' },  // KEYCODE_APOSTROPHE
        { '/', '?' },   // KEYCODE_SLASH
        { '@', '@' },   // KEYCODE_AT (XXX: see KEYCODE_2)
        { 0, 0 },       // KEYCODE_NUM (XXX: not numlock, see docs)
        { 0, 0 },       // KEYCODE_HEADSETHOOK
        { 0, 0 },       // KEYCODE_FOCUS  80
        { '+', '+' },   // KEYCODE_PLUS (XXX: see KEYCODE_EQUALS)
        { 0, 0 },       // KEYCODE_MENU
        { 0, 0 },       // KEYCODE_NOTIFICATION
        { 0, 0 },       // KEYCODE_SEARCH
        { 0, 0 },       // KEYCODE_MEDIA_PLAY_PAUSE
        { 0, 0 },       // KEYCODE_MEDIA_STOP
        { 0, 0 },       // KEYCODE_MEDIA_NEXT
        { 0, 0 },       // KEYCODE_MEDIA_PREVIOUS
        { 0, 0 },       // KEYCODE_MEDIA_REWIND
        { 0, 0 },       // KEYCODE_MEDIA_FAST_FORWARD  90
        { 0, 0 },       // KEYCODE_MUTE
        { 0x0111, 0 },  // KEYCODE_PAGE_UP
        { 0x0110, 0 },  // KEYCODE_PAGE_DOWN
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 }        // 100
    };

    static final int[][] mod_map = {
        { 59, 60 }, // Shift = KEYCODE_SHIFT_LEFT, KEYCODE_SHIFT_RIGHT
        { 0, 0 },   // Lock = (none)
        { 23, 0 },  // Control = KEYCODE_DPAD_CENTER, (none)
        { 57, 58 }, // Mod1 = KEYCODE_ALT_LEFT, KEYCODE_ALT_RIGHT
        { 0, 0 },   // Mod2 = (none)
        { 0, 0 },   // Mod3 = (none)
        { 0, 0 },   // Mod4 = (none)
        { 0, 0 },   // Mod5 = (none)
    };

    byte                        mGlobalAutoRepeat;
    byte                        mKeyClickPercent;

    byte                        mBellVolume;
    short                       mBellPitchHZ;
    short                       mBellDurationMS;
    int                         mBellSampleCount;
    AudioTrack                  mBellAudioTrack;

    int                         mLedMask;

    byte                        mMinKeycode;
    byte                        mMaxKeycode;

    short                       mModState;

    short                       mPointerX;
    short                       mPointerY;

    X11Keyboard() {
        mGlobalAutoRepeat = 0;
        mKeyClickPercent = 50;
        mBellVolume = 50;
        mBellPitchHZ = 2000;
        mBellDurationMS = 100;
        mLedMask = 0x00000000;
        mMinKeycode = X_MIN_KEYCODE;
        mMaxKeycode = X_MIN_KEYCODE + NUM_KEYCODES - 1;
        createBellAudioTrack();
    }

    byte minKeycode() { return mMinKeycode; }
    byte maxKeycode() { return mMaxKeycode; }

    void handleQueryPointer(X11Client c, X11RequestMessage msg) throws X11Error {
        X11Window w = c.getWindow(msg.mData.deqInt());

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData((byte)1 /*True*/);
        reply.mData.enqInt(c.mServer.mDefaultScreen.mRoot.mId);
        reply.mData.enqInt(X11Resource.NONE); //XXX: child
        reply.mData.enqShort(mPointerX);
        reply.mData.enqShort(mPointerY);
        reply.mData.enqShort((short)(mPointerX - w.mRect.x)); // win-x
        reply.mData.enqShort((short)(mPointerY - w.mRect.y)); // win-y
        reply.mData.enqShort(mModState);
        c.send(reply);
    }

    void handleGetKeyboardMapping(X11Client c, X11RequestMessage msg) throws X11Error {
        int first_keycode = msg.mData.deqByte();
        int count = msg.mData.deqByte();
        Log.d("X", "handleGetKeyboardMapping: fc="+first_keycode+", count="+count);
        if (first_keycode < mMinKeycode) {
            throw new X11Error(X11Error.VALUE, first_keycode);
        }
        if (first_keycode+count-1 > mMaxKeycode) {
            throw new X11Error(X11Error.VALUE, count);
        }

        byte keysyms_per_keycode = 2;

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData(keysyms_per_keycode);
        reply.mData.enqSkip(24);
        int kc;
        for (kc = first_keycode; kc < first_keycode+count; ++kc) {
            reply.mData.enqInt(key_codes[kc-X_MIN_KEYCODE][0]);
            reply.mData.enqInt(key_codes[kc-X_MIN_KEYCODE][1]);
        }
        c.send(reply);
    }

    void handleBell(X11Client c, X11RequestMessage msg) {
        byte volumePercent = msg.headerData();
        int volume;
        if (volumePercent >= 0) {
            volume = mBellVolume - ((mBellVolume * volumePercent) / 100) + volumePercent;
        }
        else {
            volume = mBellVolume + ((mBellVolume * volumePercent) / 100);
        }
        int frame_count = mBellSampleCount/2; //XXX ???
        int loop_count = mBellDurationMS * mBellPitchHZ / 1000;
        mBellAudioTrack.setStereoVolume(volume, volume);
        mBellAudioTrack.setLoopPoints(0, frame_count, loop_count);
        mBellAudioTrack.play();
    }

    void handleGetModifierMapping(X11Client c, X11RequestMessage msg) {
        byte keycodes_per_modifier = 2;

        X11ReplyMessage reply = new X11ReplyMessage(msg);
        reply.headerData(keycodes_per_modifier);
        reply.mData.enqSkip(24);
        int m;
        for (m = 0; m < NUM_MODIFIERS; ++m) {
            reply.mData.enqInt(mod_map[m][0] + X_MIN_KEYCODE);
            reply.mData.enqInt(mod_map[m][1] + X_MIN_KEYCODE);
        }
        c.send(reply);
    }

    void onKeyDown(int code) {
        int m;
        for (m = 0; m < NUM_MODIFIERS; ++m) {
            if (mod_map[m][0] == code || mod_map[m][1] == code) {
                mModState |= (1 << m);
            }
        }
    }

    void onKeyUp(int code) {
        int m;
        for (m = 0; m < NUM_MODIFIERS; ++m) {
            if (mod_map[m][0] == code || mod_map[m][1] == code) {
                mModState &= ~(1 << m);
            }
        }
    }

    void createBellAudioTrack() {
        int sample_rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_NOTIFICATION);
        mBellSampleCount = (sample_rate / mBellPitchHZ);
        byte[] pcm_data = new byte[2*mBellSampleCount];
        for (int i = 0; i < mBellSampleCount; ++i) {
            short val = (short)(32767 * Math.sin(2 * Math.PI * i / mBellSampleCount));
            pcm_data[2*i+0] = (byte)(val & 0xff);
            pcm_data[2*i+1] = (byte)(val >> 8);
        }
        mBellAudioTrack = new AudioTrack(
                AudioManager.STREAM_NOTIFICATION,
                sample_rate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mBellSampleCount,
                AudioTrack.MODE_STATIC);
        mBellAudioTrack.write(pcm_data, 0, mBellSampleCount);
    }
}
