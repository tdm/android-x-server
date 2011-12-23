package tdm.xserver;

import android.util.Log;

import android.graphics.Bitmap;

import java.util.ArrayList;

import java.io.File;

class FontData
{
    static void globalInit(X11Server server) {
        FontDataPCF.globalInit(server);
        FontDataNative.globalInit(server);
    }

    String                      mName;
    boolean                     mMetadataLoaded;
    boolean                     mGlyphsLoaded;

    X11CharInfo                 mMinBounds;
    X11CharInfo                 mMaxBounds;
    short                       mMinCharOrByte2;
    short                       mMaxCharOrByte2;
    short                       mDefaultChar;
    byte                        mDrawDirection;
    byte                        mMinByte1;
    byte                        mMaxByte1;
    byte                        mAllCharsExist;
    short                       mFontAscent;
    short                       mFontDescent;
    ArrayList<X11FontProp>      mProperties;
    ArrayList<X11CharInfo>      mCharInfos;

    ArrayList<Bitmap>           mImages;

    FontData(String name) {
        mName = name;
        mMetadataLoaded = false;
        mGlyphsLoaded = false;

        mMinBounds = new X11CharInfo();
        mMinBounds.left_side_bearing = 0;
        mMinBounds.right_side_bearing = 0;
        mMinBounds.character_width = 0;
        mMinBounds.ascent = 0;
        mMinBounds.descent = 0;
        mMinBounds.attributes = 0;

        mMaxBounds = new X11CharInfo();
        mMaxBounds.left_side_bearing = 0;
        mMaxBounds.right_side_bearing = 0;
        mMaxBounds.character_width = 0;
        mMaxBounds.ascent = 0;
        mMaxBounds.descent = 0;
        mMaxBounds.attributes = 0;

        mMinCharOrByte2 = 0;
        mMaxCharOrByte2 = 0;
        mDefaultChar = 0;
        mDrawDirection = 0;
        mMinByte1 = 0;
        mMaxByte1 = 0;
        mAllCharsExist = 1;
        mFontAscent = 0;
        mFontDescent = 0;
        mProperties = new ArrayList<X11FontProp>();
        mCharInfos = new ArrayList<X11CharInfo>();
        mImages = new ArrayList<Bitmap>();
    }

    void loadMetadata(X11Server server) throws Exception { mMetadataLoaded = true; }
    void loadGlyphs(X11Server server) throws Exception { mGlyphsLoaded = true; }

    boolean match(String pattern) {
        String[] patv = pattern.split("-");
        String[] v = mName.split("-");

        if (patv.length != v.length) {
            return false;
        }

        for (int n = 0; n < v.length; ++n) {
            if (!patv[n].equals("*") && !patv[n].equals(v[n])) {
                return false;
            }
        }
        return true;
    }

    Bitmap getBitmap(int idx) {
        return mImages.get(idx);
    }

    void enqueueInfo(ByteQueue q) {
        q.enqShort(mMinBounds.left_side_bearing);
        q.enqShort(mMinBounds.right_side_bearing);
        q.enqShort(mMinBounds.character_width);
        q.enqShort(mMinBounds.ascent);
        q.enqShort(mMinBounds.descent);
        q.enqShort(mMinBounds.attributes);
        q.enqSkip(4);
        q.enqShort(mMaxBounds.left_side_bearing);
        q.enqShort(mMaxBounds.right_side_bearing);
        q.enqShort(mMaxBounds.character_width);
        q.enqShort(mMaxBounds.ascent);
        q.enqShort(mMaxBounds.descent);
        q.enqShort(mMaxBounds.attributes);
        q.enqSkip(4);
        q.enqShort(mMinCharOrByte2);
        q.enqShort(mMaxCharOrByte2);
        q.enqShort(mDefaultChar);
        q.enqShort((short)mProperties.size());
        q.enqByte(mDrawDirection);
        q.enqByte(mMinByte1);
        q.enqByte(mMaxByte1);
        q.enqByte(mAllCharsExist);
        q.enqShort(mFontAscent);
        q.enqShort(mFontDescent);
    }
    void enqueueProperties(ByteQueue q) {
        for (X11FontProp prop : mProperties) {
            q.enqInt(prop.name);
            q.enqInt(prop.value);
        }
    }
    void enqueueCharInfoCount(ByteQueue q) {
        q.enqInt((int)mCharInfos.size());
    }
    void enqueueCharInfo(ByteQueue q) {
        for (X11CharInfo info : mCharInfos) {
            q.enqShort(info.left_side_bearing);
            q.enqShort(info.right_side_bearing);
            q.enqShort(info.character_width);
            q.enqShort(info.ascent);
            q.enqShort(info.descent);
            q.enqShort(info.attributes);
        }
    }
}
