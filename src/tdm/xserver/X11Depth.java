package tdm.xserver;

import java.util.ArrayList;

class X11Depth
{
    byte                        depth;
    ArrayList<X11Visual>        visuals;

    X11Depth(byte d) {
        depth = d;
        visuals = new ArrayList<X11Visual>();
    }
}
