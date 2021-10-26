package tdm.xserver;

import android.content.res.AssetManager;
import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class FontDataPCF extends FontData
{
    static final int PATH_TYPE_FILE             = 1;
    static final int PATH_TYPE_ASSET            = 2;

    static final int PCF_PROPERTIES             = (1<<0);
    static final int PCF_ACCELERATORS           = (1<<1);
    static final int PCF_METRICS                = (1<<2);
    static final int PCF_BITMAPS                = (1<<3);
    static final int PCF_INK_METRICS            = (1<<4);
    static final int PCF_BDF_ENCODINGS          = (1<<5);
    static final int PCF_SWIDTHS                = (1<<6);
    static final int PCF_GLYPH_NAMES            = (1<<7);
    static final int PCF_BDF_ACCELERATORS       = (1<<8);

    static final int PCF_DEFAULT_FORMAT         = 0x00000000;
    static final int PCF_INKBOUNDS              = 0x00000200;
    static final int PCF_ACCEL_W_INKBOUNDS      = 0x00000100;
    static final int PCF_COMPRESSED_METRICS     = 0x00000100;

    static final int PCF_GLYPH_PAD_MASK         = (3<<0); // See the bitmap table for explanation
    static final int PCF_BYTE_MASK              = (1<<2); // If set then Most Sig Byte First
    static final int PCF_BIT_MASK               = (1<<3); // If set then Most Sig Bit First
    static final int PCF_SCAN_UNIT_MASK         = (3<<4); // See the bitmap table for explanation
    private static final String TAG = FontDataPCF.class.getName();

    class pcf_toc_entry
    {
        int             type;
        int             format;
        int             size;
        int             offset;
    }
    class pcf_toc_order_by_offset implements Comparator
    {
        public int compare(Object o1, Object o2) {
            pcf_toc_entry e1 = (pcf_toc_entry)o1;
            pcf_toc_entry e2 = (pcf_toc_entry)o2;
            return e1.offset - e2.offset;
        }
    }

    class pcf_property
    {
        int             name_offset;
        byte            is_string;
        int             value;
    }

    class pcf_metrics
    {
        short           left_side_bearing;
        short           right_side_bearing;
        short           character_width;
        short           character_ascent;
        short           character_descent;
        short           character_attributes;
    }

    class pcf_accelerator
    {
        byte            no_overlap;
        byte            constant_metrics;
        byte            terminal_font;
        byte            constant_width;
        byte            ink_inside;
        byte            ink_metrics;
        byte            draw_direction;
        byte            padding;
        int             font_ascent;
        int             font_descent;
        int             max_overlap;
        pcf_metrics     min_bounds;
        pcf_metrics     max_bounds;
        pcf_metrics     ink_min_bounds;
        pcf_metrics     ink_max_bounds;

        pcf_accelerator() {
            min_bounds = new pcf_metrics();
            max_bounds = new pcf_metrics();
            ink_min_bounds = new pcf_metrics();
            ink_max_bounds = new pcf_metrics();
        }
    }

    class pcf_encoding
    {
        short           min_char_or_byte2;
        short           max_char_or_byte2;
        short           min_byte1;
        short           max_byte1;
        short           default_char;
        short[]         glyph_indices;
    }

    static void globalInit(X11Server server) {
        BufferedReader br;
        String s;
        try {
            AssetManager am = server.mContext.getAssets();

            br = new BufferedReader(new InputStreamReader(am.open("fonts/fonts.dir")));
            br.readLine(); // Skip first line
            while ((s = br.readLine()) != null) {
                String[] fields = s.split(" ", 2);
                String pathname = "fonts/" + fields[0];
                Log.i(TAG, "Loading PCF font: " + pathname);
                FontDataPCF pcf = new FontDataPCF(fields[1], PATH_TYPE_ASSET, pathname);
                server.registerFont(pcf);
            }

            br = new BufferedReader(new InputStreamReader(am.open("fonts/fonts.alias")));
            while ((s = br.readLine()) != null) {
                if (s.length() == 0 || s.charAt(0) == '!') {
                    continue;
                }
                String[] fields = s.split("[ \t]{1,}", 2);
                Log.i(TAG, "Loading PCF font alias: " + fields);
                server.registerFontAlias(fields[0].toLowerCase(), fields[1].toLowerCase());
            }
        }
        catch (Exception e) {
            Log.e("X", "Failed to init PCF fonts from assets", e);
        }

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream("/data/local/fonts/fonts.dir")));
            br.readLine(); // Skip first line
            while ((s = br.readLine()) != null) {
                if (s.length() == 0 || s.charAt(0) == '!') {
                    continue;
                }
                String[] fields = s.split(" ", 2);
                if (fields.length != 2) {
                    Log.e("X", "bad line in fonts.dir: fields=" + fields.length + ", line=" + s);
                    continue;
                }
                String pathname = "/data/local/fonts/" + fields[0];
                FontDataPCF pcf = new FontDataPCF(fields[1], PATH_TYPE_FILE, pathname);
                server.registerFont(pcf);
            }

            br = new BufferedReader(new InputStreamReader(new FileInputStream("/data/local/fonts/fonts.alias")));
            while ((s = br.readLine()) != null) {
                if (s.length() == 0 || s.charAt(0) == '!') {
                    continue;
                }
                String[] fields = s.split("[ \t]{1,}", 2);
                if (fields.length != 2) {
                    Log.e("X", "bad line in fonts.alias: fields=" + fields.length + ", line=" + s);
                    continue;
                }
                server.registerFontAlias(fields[0].toLowerCase(), fields[1].toLowerCase());
            }
        }
        catch (Exception e) {
            Log.e("X", "Failed to init PCF fonts from dir");
            e.printStackTrace();
        }
    }

    int                         mPathtype; // FILE or ASSET
    String                      mPathname;

    ArrayList<X11CharInfo>      mGlyphInfos;

    FontDataPCF(String name, int pathtype, String pathname) {
        super(name);
        mPathtype = pathtype;
        mPathname = pathname;
        mGlyphInfos = new ArrayList<X11CharInfo>();
    }

    private void readPropertyTable(X11Server server, int fmt, ByteBuffer b) {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int format = b.getInt();
        b.order((format & PCF_BYTE_MASK) != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int nprops = b.getInt();
        pcf_property[] props = new pcf_property[nprops];

        int n;
        for (n = 0; n < nprops; ++n) {
            pcf_property prop = new pcf_property();
            prop.name_offset = b.getInt();
            prop.is_string = b.get();
            prop.value = b.getInt();
            props[n] = prop;
        }
        b.position(MathX.roundup(b.position(), 4));

        int string_size = b.getInt();

        byte[] string_table = new byte[string_size];
        b.get(string_table);

        mProperties = new ArrayList<X11FontProp>(nprops);
        for (n = 0; n < nprops; ++n) {
            String s = new String(string_table, props[n].name_offset);
            X11FontProp prop = new X11FontProp();
            prop.name = server.doInternAtom(s);
            if (props[n].is_string != 0) {
                String val = new String(string_table, props[n].value);
                prop.value = server.doInternAtom(val);
            }
            else {
                prop.value = props[n].value;
            }
            mProperties.add(n, prop);
        }
    }

    private void readAcceleratorTable(int fmt, ByteBuffer b) {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int format = b.getInt();
        b.order((format & PCF_BYTE_MASK) != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        pcf_accelerator a = new pcf_accelerator();

        a.no_overlap = b.get();
        a.constant_metrics = b.get();
        a.terminal_font = b.get();
        a.constant_width = b.get();
        a.ink_inside = b.get();
        a.ink_metrics = b.get();
        a.draw_direction = b.get();
        a.padding = b.get();
        a.font_ascent = b.getInt();
        a.font_descent = b.getInt();
        a.max_overlap = b.getInt();

        a.min_bounds.left_side_bearing = b.getShort();
        a.min_bounds.right_side_bearing = b.getShort();
        a.min_bounds.character_width = b.getShort();
        a.min_bounds.character_ascent = b.getShort();
        a.min_bounds.character_descent = b.getShort();
        a.min_bounds.character_attributes = b.getShort();

        a.max_bounds.left_side_bearing = b.getShort();
        a.max_bounds.right_side_bearing = b.getShort();
        a.max_bounds.character_width = b.getShort();
        a.max_bounds.character_ascent = b.getShort();
        a.max_bounds.character_descent = b.getShort();
        a.max_bounds.character_attributes = b.getShort();

        if ((fmt & PCF_ACCEL_W_INKBOUNDS) != 0) {
            a.ink_min_bounds.left_side_bearing = b.getShort();
            a.ink_min_bounds.right_side_bearing = b.getShort();
            a.ink_min_bounds.character_width = b.getShort();
            a.ink_min_bounds.character_ascent = b.getShort();
            a.ink_min_bounds.character_descent = b.getShort();
            a.ink_min_bounds.character_attributes = b.getShort();

            a.ink_max_bounds.left_side_bearing = b.getShort();
            a.ink_max_bounds.right_side_bearing = b.getShort();
            a.ink_max_bounds.character_width = b.getShort();
            a.ink_max_bounds.character_ascent = b.getShort();
            a.ink_max_bounds.character_descent = b.getShort();
            a.ink_max_bounds.character_attributes = b.getShort();
        }
        else {
            a.ink_min_bounds = a.min_bounds;
            a.ink_max_bounds = a.max_bounds;
        }

        mMinBounds.left_side_bearing = a.min_bounds.left_side_bearing;
        mMinBounds.right_side_bearing = a.min_bounds.right_side_bearing;
        mMinBounds.character_width = a.min_bounds.character_width;
        mMinBounds.ascent = a.min_bounds.character_ascent;
        mMinBounds.descent = a.min_bounds.character_descent;
        mMinBounds.attributes = a.min_bounds.character_attributes;

        mMaxBounds.left_side_bearing = a.max_bounds.left_side_bearing;
        mMaxBounds.right_side_bearing = a.max_bounds.right_side_bearing;
        mMaxBounds.character_width = a.max_bounds.character_width;
        mMaxBounds.ascent = a.max_bounds.character_ascent;
        mMaxBounds.descent = a.max_bounds.character_descent;
        mMaxBounds.attributes = a.max_bounds.character_attributes;

        mDrawDirection = a.draw_direction;
        mAllCharsExist = 0 /* false */; //XXX?
        mFontAscent = (short)a.font_ascent;
        mFontDescent = (short)a.font_descent;
    }

    short decompress_metric(byte b) {
        if (b < 0) {
            return (short)(b & 0x7f);
        }
        return (short)(b - 0x80);
    }

    private void readMetricsTable(int fmt, ByteBuffer b, ArrayList<X11CharInfo> infos) {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int format = b.getInt();
        b.order((format & PCF_BYTE_MASK) != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        if ((fmt & PCF_COMPRESSED_METRICS) != 0) {
            short count = b.getShort();
            short n;
            for (n = 0; n < count; ++n) {
                X11CharInfo info = new X11CharInfo();
                info.left_side_bearing = decompress_metric(b.get());
                info.right_side_bearing = decompress_metric(b.get());
                info.character_width = decompress_metric(b.get());
                info.ascent = decompress_metric(b.get());
                info.descent = decompress_metric(b.get());
                info.attributes = 0;
                infos.add(n, info);
            }
        }
        else {
            int count = b.getInt();
            int n;
            for (n = 0; n < count; ++n) {
                X11CharInfo info = new X11CharInfo();
                info.left_side_bearing = b.getShort();
                info.right_side_bearing = b.getShort();
                info.character_width = b.getShort();
                info.ascent = b.getShort();
                info.descent = b.getShort();
                info.attributes = b.getShort();
                infos.add(n, info);
            }
        }
    }

    private void showBitmap(char ch) {
        Bitmap bmp = mImages.get((int)ch);
        Log.d("X", "Show bitmap for <"+ch+">: w="+bmp.getWidth()+", h="+bmp.getHeight()+" ...");
        Log.d("X", "-----------");
        for (int y = 0; y < bmp.getHeight(); ++y) {
            StringBuffer buf = new StringBuffer();
            for (int x = 0; x < bmp.getWidth(); ++x) {
                if (bmp.getPixel(x, y) == Color.BLACK) {
                    buf.append(".");
                }
                else {
                    buf.append("X");
                }
            }
            Log.d("X", "   " + buf);
        }
        Log.d("X", "-----------");
    }

    private void readBitmapTable(int fmt, ByteBuffer b) {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int format = b.getInt();
        b.order((format & PCF_BYTE_MASK) != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int glyph_count = b.getInt();
        int n;

        int[] offsets = new int[glyph_count];
        for (n = 0; n < glyph_count; ++n) {
            offsets[n] = b.getInt();
        }

        int[] bitmap_sizes = new int[4];
        bitmap_sizes[0] = b.getInt();
        bitmap_sizes[1] = b.getInt();
        bitmap_sizes[2] = b.getInt();
        bitmap_sizes[3] = b.getInt();

        int bitmap_data_len = bitmap_sizes[format&3];
        ByteBuffer bitmap_data = b.slice();
        bitmap_data.limit(bitmap_data_len);

        int bitmap_byte_order = (format&4) >> 2; // 1=LSByteFirst, 0=MSByteFirst
        int bitmap_bit_order  = (format&8) >> 3; // 1=LSBitFirst, 0=MSBitFirst

        mImages = new ArrayList<Bitmap>(glyph_count);

        int row_pad_bytes = (1<<(format&3));
        int elem_bytes = (1<<((format>>4)&3));
        int elem_bits = elem_bytes*8;

Log.d("X", "readBitmapTable: name="+mName+", glyph_count="+glyph_count+", bitmap_data_len="+bitmap_data_len);

        for (n = 0; n < glyph_count; ++n) {
            X11CharInfo info = mGlyphInfos.get(n);
            //XXX: should width be rsb-lsb or width?
            short w = info.character_width;
            short h = (short)(info.ascent + info.descent);
            if (w == 0 || h == 0) {
                mImages.add(n, null);
                continue;
            }
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

            int elem_per_row = (w+elem_bits-1)/elem_bits;
            int bytes_per_row = ((elem_per_row*elem_bytes+row_pad_bytes-1)/row_pad_bytes)*row_pad_bytes;

            short row, col;
            int row_off, col_off, idx;
            int elem;
            int nbits, bit;
            for (row = 0; row < h; ++row) {
                row_off = row * bytes_per_row;
                for (col = 0; col < w; ++col) {
                    int elem_off = (col/elem_bits)*elem_bytes;
                    int elem_byte_off;
                    if (bitmap_byte_order == 1 /*LSByteFirst*/) {
                        elem_byte_off = (col%elem_bits)/8;
                    }
                    else {
                        elem_byte_off = elem_bytes-1 - (col%elem_bits)/8;
                    }
                    int elem_bit_off;
                    if (bitmap_bit_order == 1 /*LSBitFirst*/) {
                        elem_bit_off = 8-1 - (col%8);
                    }
                    else {
                        elem_bit_off = (col%8);
                    }
                    int valoff = offsets[n] + row_off + elem_off + elem_byte_off;
                    byte val = bitmap_data.get(offsets[n] + row_off + elem_off + elem_byte_off);
                    int pixel = (val >> elem_bit_off) & 1;
                    int color = (pixel != 0 ? Color.WHITE : Color.BLACK);
                    bmp.setPixel(col, row, color);
                }
            }
            mImages.add(n, bmp);
        }
    }

    private void readEncodingTable(int fmt, ByteBuffer b) {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int format = b.getInt();
        b.order((format & PCF_BYTE_MASK) != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        mMinCharOrByte2 = b.getShort();
        mMaxCharOrByte2 = b.getShort();
        mMinByte1 = (byte)b.getShort();
        mMaxByte1 = (byte)b.getShort();
        mDefaultChar = b.getShort();

        //XXX: this fails on cu-pua12.pcf.gz
        int num_indices = (mMaxCharOrByte2 -
                           mMinCharOrByte2 + 1) *
                          (mMaxByte1 -
                           mMinByte1 + 1);
        short[] glyph_indices = new short[num_indices];
        int n;
        for (n = 0; n < num_indices; ++n) {
            glyph_indices[n] = b.getShort();
        }
    }

    private void readScalableWidthsTable(int fmt, ByteBuffer b) {
        //XXX: Skip this data
    }

    private void readGlyphNamesTable(int fmt, ByteBuffer b) {
        //XXX: Skip this data
    }

    void loadMetadata(X11Server server) throws Exception {
        if (mMetadataLoaded) {
            return;
        }
        InputStream is = null;
        if (mPathtype == PATH_TYPE_ASSET) {
            AssetManager am = server.mContext.getAssets();
            is = am.open(mPathname);
        }
        else {
            is = new FileInputStream(mPathname);
        }
        //XXX: should check for gzip signature, but what about rewind?
        if (mPathname.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }

        long pos = 0; //XXX: seek() sure would be nice
        ByteBuffer b;

        b = ByteBuffer.allocate(4);
        is.read(b.array(), 0, 4); pos += 4;
        byte[] sig = b.array();
        if (sig[0] != 1 || sig[1] != 'f' || sig[2] != 'c' || sig[3] != 'p') {
            throw new Exception("Bad PCF header");
        }

        b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        is.read(b.array(), 0, 4); pos += 4;
        int table_count = b.getInt();

        int n;
        pcf_toc_entry[] toc = new pcf_toc_entry[table_count];
        for (n = 0; n < table_count; ++n) {
            b = ByteBuffer.allocate(16);
            b.order(ByteOrder.LITTLE_ENDIAN);
            is.read(b.array(), 0, 16); pos += 16;
            pcf_toc_entry ent = new pcf_toc_entry();
            ent.type = b.getInt();
            ent.format = b.getInt();
            ent.size = b.getInt();
            ent.offset = b.getInt();
            toc[n] = ent;
        }
        Arrays.sort(toc, new pcf_toc_order_by_offset());

        for (pcf_toc_entry e : toc) {
            if (pos != e.offset) {
                is.skip(e.offset - pos);
                pos = e.offset;
            }

            b = ByteBuffer.allocate(e.size);
            int off = 0;
            while (off < e.size) {
                int nread = is.read(b.array(), off, e.size-off);
                if (nread <= 0) {
                    Log.w("X", "FontDataPCF.loadMetadata: short read in " + mPathname);
                    break;
                }
                off += nread;
                pos += nread;
            }

            switch (e.type) {
            case PCF_PROPERTIES: readPropertyTable(server, e.format, b); break;
            case PCF_ACCELERATORS: readAcceleratorTable(e.format, b); break;
            case PCF_METRICS: readMetricsTable(e.format, b, mGlyphInfos); break;
            case PCF_BITMAPS: /* Ignore */ break;
            case PCF_INK_METRICS: readMetricsTable(e.format, b, mCharInfos); break;
            case PCF_BDF_ENCODINGS: readEncodingTable(e.format, b); break;
            case PCF_SWIDTHS: readScalableWidthsTable(e.format, b); break;
            case PCF_GLYPH_NAMES: readGlyphNamesTable(e.format, b); break;
            case PCF_BDF_ACCELERATORS: readAcceleratorTable(e.format, b); break;
            default: throw new Exception("Bad PCF toc");
            }
        }

        mMetadataLoaded = true;
    }

    void loadGlyphs(X11Server server) throws Exception {
        if (mGlyphsLoaded) {
            return;
        }
        Log.d("X", "FontDataPCF.loadGlyphs");

        InputStream is = null;
        if (mPathtype == PATH_TYPE_ASSET) {
            AssetManager am = server.mContext.getAssets();
            is = am.open(mPathname);
        }
        else {
            is = new FileInputStream(mPathname);
        }
        //XXX: should check for gzip signature, but what about rewind?
        if (mPathname.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }

        long pos = 0; //XXX: seek() sure would be nice
        ByteBuffer b;

        b = ByteBuffer.allocate(4);
        is.read(b.array(), 0, 4); pos += 4;
        byte[] sig = b.array();
        if (sig[0] != 1 || sig[1] != 'f' || sig[2] != 'c' || sig[3] != 'p') {
            throw new Exception("Bad PCF header");
        }

        b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        is.read(b.array(), 0, 4); pos += 4;
        int table_count = b.getInt();

        int n;
        pcf_toc_entry[] toc = new pcf_toc_entry[table_count];
        for (n = 0; n < table_count; ++n) {
            b = ByteBuffer.allocate(16);
            b.order(ByteOrder.LITTLE_ENDIAN);
            is.read(b.array(), 0, 16); pos += 16;
            pcf_toc_entry ent = new pcf_toc_entry();
            ent.type = b.getInt();
            ent.format = b.getInt();
            ent.size = b.getInt();
            ent.offset = b.getInt();
            toc[n] = ent;
        }
        Arrays.sort(toc, new pcf_toc_order_by_offset());

        for (pcf_toc_entry e : toc) {
            if (pos != e.offset) {
                is.skip(e.offset - pos);
                pos = e.offset;
            }

            b = ByteBuffer.allocate(e.size);
            int off = 0;
            while (off < e.size) {
                int nread = is.read(b.array(), off, e.size-off);
                if (nread <= 0) {
                    Log.w("X", "FontDataPCF.loadGlyphs: short read in " + mPathname);
                    break;
                }
                off += nread;
                pos += nread;
            }

            switch (e.type) {
            case PCF_BITMAPS: readBitmapTable(e.format, b); break;
            default: /* Ignore */ break;
            }
        }

        mGlyphsLoaded = true;
    }
}
