package tdm.xserver;

class MathX
{
    public static short divceil(short val, int n) {
        return (short)((val+n-1)/n);
    }
    public static int divceil(int val, int n) {
        return (val+n-1)/n;
    }

    public static short roundup(short val, int n) {
        return (short)(divceil(val,n)*n);
    }
    public static int roundup(int val, int n) {
        return divceil(val,n)*n;
    }
}
