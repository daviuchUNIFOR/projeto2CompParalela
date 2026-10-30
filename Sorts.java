import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Sorts {
    static final int LIMITE_SERIAL = 2048;
    static final int LIMITE_MINIMO = 4096;
    static final int LIMITE_FASE = 1024;

    private Sorts() {}

    static void mergeSortSerial(int[] a) {
        mergeSortSerial(a, new int[a.length], 0, a.length);
    }

    private static void mergeSortSerial(int[] a, int[] tmp, int lo, int hi) {
        if (hi - lo < 2) return;
        int mid = (lo + hi) >>> 1;
        mergeSortSerial(a, tmp, lo, mid);
        mergeSortSerial(a, tmp, mid, hi);
        merge(a, tmp, lo, mid, hi);
    }

    private static void merge(int[] a, int[] tmp, int lo, int mid, int hi) {
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) tmp[k++] = (a[i] <= a[j]) ? a[i++] : a[j++];
        while (i < mid) tmp[k++] = a[i++];
        while (j < hi) tmp[k++] = a[j++];
        System.arraycopy(tmp, lo, a, lo, hi - lo);
    }

    private static final class TarefaMerge extends RecursiveAction {
        private final int[] a, tmp;
        private final int lo, hi;

        TarefaMerge(int[] a, int[] tmp, int lo, int hi) {
            this.a = a; this.tmp = tmp; this.lo = lo; this.hi = hi;
        }

        @Override
        protected void compute() {
            if (hi - lo <= LIMITE_SERIAL) {
                mergeSortSerial(a, tmp, lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            invokeAll(new TarefaMerge(a, tmp, lo, mid), new TarefaMerge(a, tmp, mid, hi));
            merge(a, tmp, lo, mid, hi);
        }
    }

    static void mergeSortParalelo(int[] a, ForkJoinPool pool) {
        pool.invoke(new TarefaMerge(a, new int[a.length], 0, a.length));
    }

    static void quickSortSerial(int[] a) {
        quickSortSerial(a, 0, a.length - 1);
    }

    private static void quickSortSerial(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int pivo = a[(lo + hi) >>> 1];
        int i = lo, j = hi;
        while (i <= j) {
            while (a[i] < pivo) i++;
            while (a[j] > pivo) j--;
            if (i <= j) {
                int t = a[i]; a[i] = a[j]; a[j] = t;
                i++; j--;
            }
        }
        quickSortSerial(a, lo, j);
        quickSortSerial(a, i, hi);
    }

    private static final class TarefaQuick extends RecursiveAction {
        private final int[] a;
        private final int lo, hi;

        TarefaQuick(int[] a, int lo, int hi) {
            this.a = a; this.lo = lo; this.hi = hi;
        }

        @Override
        protected void compute() {
            if (hi - lo < LIMITE_SERIAL) {
                quickSortSerial(a, lo, hi);
                return;
            }
            int pivo = a[(lo + hi) >>> 1];
            int i = lo, j = hi;
            while (i <= j) {
                while (a[i] < pivo) i++;
                while (a[j] > pivo) j--;
                if (i <= j) {
                    int t = a[i]; a[i] = a[j]; a[j] = t;
                    i++; j--;
                }
            }
            invokeAll(new TarefaQuick(a, lo, j), new TarefaQuick(a, i, hi));
        }
    }

    static void quickSortParalelo(int[] a, ForkJoinPool pool) {
        pool.invoke(new TarefaQuick(a, 0, a.length - 1));
    }

    static void selectionSortSerial(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int m = indiceMinimo(a, i, n);
            if (m != i) {
                int t = a[i]; a[i] = a[m]; a[m] = t;
            }
        }
    }

    private static int indiceMinimo(int[] a, int lo, int hi) {
        int m = lo;
        for (int k = lo + 1; k < hi; k++) {
            if (a[k] < a[m]) m = k;
        }
        return m;
    }

    private static final class TarefaMinimo extends RecursiveTask<Integer> {
        private final int[] a;
        private final int lo, hi, fatias;

        TarefaMinimo(int[] a, int lo, int hi, int fatias) {
            this.a = a; this.lo = lo; this.hi = hi; this.fatias = fatias;
        }

        @Override
        protected Integer compute() {
            if (fatias <= 1 || hi - lo < 2) return indiceMinimo(a, lo, hi);
            int mid = (lo + hi) >>> 1;
            int fe = fatias / 2;
            TarefaMinimo esq = new TarefaMinimo(a, lo, mid, fe);
            esq.fork();
            int d = new TarefaMinimo(a, mid, hi, fatias - fe).compute();
            int e = esq.join();
            return a[d] < a[e] ? d : e;
        }
    }

    private static final class TarefaSelection extends RecursiveAction {
        private final int[] a;
        private final int fatias;

        TarefaSelection(int[] a, int fatias) {
            this.a = a; this.fatias = fatias;
        }

        @Override
        protected void compute() {
            int n = a.length;
            for (int i = 0; i < n - 1; i++) {
                int m = (fatias > 1 && n - i >= LIMITE_MINIMO)
                        ? new TarefaMinimo(a, i, n, fatias).invoke()
                        : indiceMinimo(a, i, n);
                if (m != i) {
                    int t = a[i]; a[i] = a[m]; a[m] = t;
                }
            }
        }
    }

    static void selectionSortParalelo(int[] a, ForkJoinPool pool) {
        pool.invoke(new TarefaSelection(a, pool.getParallelism()));
    }

    static void bubbleSortSerial(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            boolean trocou = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t;
                    trocou = true;
                }
            }
            if (!trocou) break;
        }
    }

    private static final class TarefaFase extends RecursiveAction {
        private final int[] a;
        private final int inicio, pLo, pHi, fatias;
        private final AtomicBoolean trocou;

        TarefaFase(int[] a, int inicio, int pLo, int pHi, int fatias, AtomicBoolean trocou) {
            this.a = a; this.inicio = inicio; this.pLo = pLo; this.pHi = pHi;
            this.fatias = fatias; this.trocou = trocou;
        }

        @Override
        protected void compute() {
            if (fatias <= 1 || pHi - pLo < 2) {
                boolean t = false;
                for (int p = pLo; p < pHi; p++) {
                    int j = inicio + 2 * p;
                    if (a[j] > a[j + 1]) {
                        int x = a[j]; a[j] = a[j + 1]; a[j + 1] = x;
                        t = true;
                    }
                }
                if (t) trocou.set(true);
                return;
            }
            int mid = (pLo + pHi) >>> 1;
            int fe = fatias / 2;
            TarefaFase esq = new TarefaFase(a, inicio, pLo, mid, fe, trocou);
            esq.fork();
            new TarefaFase(a, inicio, mid, pHi, fatias - fe, trocou).compute();
            esq.join();
        }
    }

    private static final class TarefaBubble extends RecursiveAction {
        private final int[] a;
        private final int fatias;

        TarefaBubble(int[] a, int fatias) {
            this.a = a; this.fatias = fatias;
        }

        @Override
        protected void compute() {
            int n = a.length;
            AtomicBoolean trocou = new AtomicBoolean();
            int semTroca = 0;
            for (int fase = 0; fase < n && semTroca < 2; fase++) {
                int f = fase & 1;
                int pares = (n - f) / 2;
                int fat = pares >= LIMITE_FASE ? fatias : 1;
                trocou.set(false);
                new TarefaFase(a, f, 0, pares, fat, trocou).invoke();
                semTroca = trocou.get() ? 0 : semTroca + 1;
            }
        }
    }

    static void bubbleSortParalelo(int[] a, ForkJoinPool pool) {
        pool.invoke(new TarefaBubble(a, pool.getParallelism()));
    }
}
