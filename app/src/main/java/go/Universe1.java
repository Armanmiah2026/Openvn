package go;

public abstract class Universe1 {
    private static native void _init();

    static {
        Seq1.touch();
        _init();
    }

    private Universe1() {
    }

    public static void touch() {
    }

    private static final class proxyerror extends Exception implements Seq1.Proxy, error1 {
        private final int refnum;

        @Override
        public native String error();

        @Override
        public int incRefnum() {
            Seq1.incGoRef(this.refnum, this);
            return this.refnum;
        }

        proxyerror(int i) {
            this.refnum = i;
            Seq1.trackGoRef(i, this);
        }

        @Override
        public String getMessage() {
            return error();
        }
    }
}