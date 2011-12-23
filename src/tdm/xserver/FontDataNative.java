package tdm.xserver;

import android.graphics.Typeface;

import java.io.File;

class FontDataNative extends FontData
{
    static void globalInit(X11Server server) {
        // Built-in Android fonts
        server.registerFont(new FontDataNative("sans", Typeface.SANS_SERIF));
        server.registerFont(new FontDataNative("serif", Typeface.SERIF));
        server.registerFont(new FontDataNative("monospace", Typeface.MONOSPACE));
        //server.registerFontAlias("fixed", "monospace");

        File dir = new File("/system/fonts/");
        for (File f : dir.listFiles()) {
            String filename = f.getName();
            if (filename.endsWith(".ttf")) {
                String fontname = filename.substring(0, filename.lastIndexOf('.'));
                server.registerFont(new FontDataNative(fontname, f));
            }
        }
    }

    Typeface                    mTypeFace;

    FontDataNative(String name, Typeface tf) {
        super(name);
        mTypeFace = tf;
    }
    FontDataNative(String name, File f) {
        super(name);
        mTypeFace = Typeface.createFromFile(f);
    }
}
