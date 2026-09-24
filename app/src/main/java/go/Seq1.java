package go;

import android.content.Context;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.logging.Logger;

public class Seq1 {
    private static final Logger log = Logger.getLogger("GoSeq");
    private static final int NULL_REFNUM = 41;
    public static final Ref nullRef = new Ref(NULL_REFNUM, null);
    private static final GoRefQueue goRefQueue = new GoRefQueue();
    static final RefTracker tracker = new RefTracker();

    public interface GoObject {
        int incRefnum();
    }

    public interface Proxy extends GoObject {
    }

    private static native void init();

    static native void setContext(Object obj);

    public static native void incGoRef(int i, GoObject goObject);

    static native void destroyRef(int i);

    static {
        System.loadLibrary("psiphon");
        init();
        Universe1.touch();
    }

    public static void setContext(Context context) {
        setContext((Object) context);
    }

    public static void touch() {
    }

    private Seq1() {
    }

    public static void incRefnum(int i) {
        tracker.incRefnum(i);
    }

    public static int incRef(Object obj) {
        return tracker.inc(obj);
    }

    public static int incGoObjectRef(GoObject goObject) {
        return goObject.incRefnum();
    }

    public static void trackGoRef(int i, GoObject goObject) {
        if (i > 0) {
            throw new RuntimeException("trackGoRef called with Java refnum " + i);
        }
        goRefQueue.track(i, goObject);
    }

    public static Ref getRef(int i) {
        return tracker.get(i);
    }

    static void decRef(int i) {
        tracker.dec(i);
    }

    public static final class Ref {
        public final int refnum;
        private int refcnt;
        public final Object obj;

        static int access$110(Ref ref) {
            int i = ref.refcnt;
            ref.refcnt = i - 1;
            return i;
        }

        Ref(int i, Object obj) {
            if (i < 0) {
                throw new RuntimeException("Ref instantiated with a Go refnum " + i);
            }
            this.refnum = i;
            this.refcnt = 0;
            this.obj = obj;
        }

        void inc() {
            if (this.refcnt == Integer.MAX_VALUE) {
                throw new RuntimeException("refnum " + this.refnum + " overflow");
            }
            this.refcnt++;
        }
    }

    static final class RefTracker {
        private static final int REF_OFFSET = 42;
        private int next = REF_OFFSET;
        private final RefMap javaObjs = new RefMap();
        private final IdentityHashMap<Object, Integer> javaRefs = new IdentityHashMap<>();

        RefTracker() {
        }

        synchronized int inc(Object obj) {
            if (obj == null) {
                return Seq1.NULL_REFNUM;
            }
            if (obj instanceof Proxy) {
                return ((Proxy) obj).incRefnum();
            }
            Integer num = this.javaRefs.get(obj);
            if (num == null) {
                if (this.next == Integer.MAX_VALUE) {
                    throw new RuntimeException("createRef overflow for " + obj);
                }
                int i = this.next;
                this.next = i + 1;
                num = Integer.valueOf(i);
                this.javaRefs.put(obj, num);
            }
            int intValue = num.intValue();
            Ref ref = this.javaObjs.get(intValue);
            if (ref == null) {
                ref = new Ref(intValue, obj);
                this.javaObjs.put(intValue, ref);
            }
            ref.inc();
            return intValue;
        }

        synchronized void incRefnum(int i) {
            Ref ref = this.javaObjs.get(i);
            if (ref == null) {
                throw new RuntimeException("referenced Java object is not found: refnum=" + i);
            }
            ref.inc();
        }

        synchronized void dec(int i) {
            if (i <= 0) {
                Seq1.log.severe("dec request for Go object " + i);
            } else if (i == Seq1.nullRef.refnum) {
            } else {
                Ref ref = this.javaObjs.get(i);
                if (ref == null) {
                    throw new RuntimeException("referenced Java object is not found: refnum=" + i);
                }
                Ref.access$110(ref);
                if (ref.refcnt <= 0) {
                    this.javaObjs.remove(i);
                    this.javaRefs.remove(ref.obj);
                }
            }
        }

        synchronized Ref get(int i) {
            if (i < 0) {
                throw new RuntimeException("ref called with Go refnum " + i);
            }
            if (i == Seq1.NULL_REFNUM) {
                return Seq1.nullRef;
            }
            Ref ref = this.javaObjs.get(i);
            if (ref == null) {
                throw new RuntimeException("unknown java Ref: " + i);
            }
            return ref;
        }
    }

    public static class GoRefQueue extends ReferenceQueue<GoObject> {
        private final Collection<GoRef> refs = Collections.synchronizedCollection(new HashSet());

        void track(int i, GoObject goObject) {
            this.refs.add(new GoRef(i, goObject, this));
        }

        GoRefQueue() {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            GoRef goRef = (GoRef) GoRefQueue.this.remove();
                            GoRefQueue.this.refs.remove(goRef);
                            Seq1.destroyRef(goRef.refnum);
                            goRef.clear();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            thread.setDaemon(true);
            thread.setName("GoRefQueue Finalizer Thread");
            thread.start();
        }
    }

    public static class GoRef extends PhantomReference<GoObject> {
        final int refnum;

        GoRef(int i, GoObject goObject, GoRefQueue goRefQueue) {
            super(goObject, goRefQueue);
            if (i > 0) {
                throw new RuntimeException("GoRef instantiated with a Java refnum " + i);
            }
            this.refnum = i;
        }
    }

    public static final class RefMap {
        private int next = 0;
        private int live = 0;
        private int[] keys = new int[16];
        private Ref[] objs = new Ref[16];

        RefMap() {
        }

        Ref get(int i) {
            int binarySearch = Arrays.binarySearch(this.keys, 0, this.next, i);
            if (binarySearch >= 0) {
                return this.objs[binarySearch];
            }
            return null;
        }

        void remove(int i) {
            int binarySearch = Arrays.binarySearch(this.keys, 0, this.next, i);
            if (binarySearch >= 0 && this.objs[binarySearch] != null) {
                this.objs[binarySearch] = null;
                this.live--;
            }
        }

        void put(int i, Ref ref) {
            if (ref == null) {
                throw new RuntimeException("put a null ref (with key " + i + ")");
            }
            int binarySearch = Arrays.binarySearch(this.keys, 0, this.next, i);
            if (binarySearch >= 0) {
                if (this.objs[binarySearch] == null) {
                    this.objs[binarySearch] = ref;
                    this.live++;
                }
                if (this.objs[binarySearch] != ref) {
                    throw new RuntimeException("replacing an existing ref (with key " + i + ")");
                }
                return;
            }
            if (this.next >= this.keys.length) {
                grow();
                binarySearch = Arrays.binarySearch(this.keys, 0, this.next, i);
            }
            int i2 = binarySearch ^ (-1);
            if (i2 < this.next) {
                System.arraycopy(this.keys, i2, this.keys, i2 + 1, this.next - i2);
                System.arraycopy(this.objs, i2, this.objs, i2 + 1, this.next - i2);
            }
            this.keys[i2] = i;
            this.objs[i2] = ref;
            this.live++;
            this.next++;
        }

        private void grow() {
            int[] iArr;
            Ref[] refArr;
            if (2 * roundPow2(this.live) > this.keys.length) {
                iArr = new int[this.keys.length * 2];
                refArr = new Ref[this.objs.length * 2];
            } else {
                iArr = this.keys;
                refArr = this.objs;
            }
            int i = 0;
            for (int i2 = 0; i2 < this.keys.length; i2++) {
                if (this.objs[i2] != null) {
                    iArr[i] = this.keys[i2];
                    refArr[i] = this.objs[i2];
                    i++;
                }
            }
            for (int i3 = i; i3 < iArr.length; i3++) {
                iArr[i3] = 0;
                refArr[i3] = null;
            }
            this.keys = iArr;
            this.objs = refArr;
            this.next = i;
            if (this.live != this.next) {
                throw new RuntimeException("bad state: live=" + this.live + ", next=" + this.next);
            }
        }

        private static int roundPow2(int i) {
            int i2 = 1;
            while (true) {
                int i3 = i2;
                if (i3 < i) {
                    i2 = i3 * 2;
                } else {
                    return i3;
                }
            }
        }
    }
}