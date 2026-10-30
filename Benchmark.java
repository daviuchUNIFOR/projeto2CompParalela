import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class Benchmark {
    static final int[] TAMANHOS = {100, 1_000, 10_000, 100_000};
    static final String[] TIPOS = {"aleatorio", "ordenado", "inverso"};
    static final String[] ALGORITMOS = {"MergeSort", "QuickSort", "SelectionSort", "BubbleSort"};
    static final int AMOSTRAS = 5;
    static final long SEMENTE = 42L;
    static final Locale BR = Locale.forLanguageTag("pt-BR");
    static final String LINHA = "-".repeat(78);
    static final String LINHA_DUPLA = "=".repeat(78);

    interface Ordenador { void ordenar(int[] a); }

    static int[] gerar(String tipo, int n) {
        Random rnd = new Random(SEMENTE);
        int[] a = new int[n];
        switch (tipo) {
            case "aleatorio":
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt();
                break;
            case "ordenado":
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt();
                Arrays.sort(a);
                break;
            case "inverso":
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt();
                Arrays.sort(a);
                for (int i = 0, j = n - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
                break;
            default:
                throw new IllegalArgumentException("Tipo de dado desconhecido: " + tipo);
        }
        return a;
    }

    static List<Integer> listaThreads(int nucleos) {
        Set<Integer> s = new LinkedHashSet<>();
        for (int t = 1; t < nucleos; t *= 2) s.add(t);
        s.add(nucleos);
        return new ArrayList<>(s);
    }

    static Ordenador serial(String alg) {
        switch (alg) {
            case "MergeSort": return Sorts::mergeSortSerial;
            case "QuickSort": return Sorts::quickSortSerial;
            case "SelectionSort": return Sorts::selectionSortSerial;
            case "BubbleSort": return Sorts::bubbleSortSerial;
            default: throw new IllegalArgumentException("Algoritmo desconhecido: " + alg);
        }
    }

    static Ordenador paralelo(String alg, ForkJoinPool pool) {
        switch (alg) {
            case "MergeSort": return a -> Sorts.mergeSortParalelo(a, pool);
            case "QuickSort": return a -> Sorts.quickSortParalelo(a, pool);
            case "SelectionSort": return a -> Sorts.selectionSortParalelo(a, pool);
            case "BubbleSort": return a -> Sorts.bubbleSortParalelo(a, pool);
            default: throw new IllegalArgumentException("Algoritmo desconhecido: " + alg);
        }
    }

    static Resultados executar(Path csv) throws IOException {
        int nucleos = Runtime.getRuntime().availableProcessors();
        List<Integer> threads = listaThreads(nucleos);
        System.out.println(LINHA_DUPLA);
        System.out.println(" BENCHMARK DE ORDENACAO: SERIAL x PARALELO");
        System.out.println(LINHA_DUPLA);
        System.out.println(" Nucleos disponiveis : " + nucleos);
        System.out.println(" Threads (paralelo)  : " + threads);
        System.out.println(" Algoritmos          : " + String.join(", ", ALGORITMOS));
        System.out.println(" Tipos de dado       : " + String.join(", ", TIPOS));
        System.out.println(" Tamanhos            : " + Arrays.toString(TAMANHOS));
        System.out.println(" Amostras            : " + AMOSTRAS + " por configuracao");
        System.out.println(LINHA_DUPLA);

        Map<Integer, ForkJoinPool> pools = new LinkedHashMap<>();
        for (int t : threads) pools.put(t, new ForkJoinPool(t));

        Resultados res = new Resultados();
        int total = TIPOS.length * TAMANHOS.length, feito = 0;

        try (BufferedWriter out = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
            out.write(Resultados.CABECALHO);
            out.newLine();
            for (String tipo : TIPOS) {
                for (int n : TAMANHOS) {
                    int[] base = gerar(tipo, n);
                    System.out.println();
                    System.out.println(LINHA);
                    System.out.printf(BR, " ENTRADA %d/%d  |  tipo de dado: %s  |  tamanho: n = %,d%n",
                            ++feito, total, tipo, n);
                    System.out.println(LINHA);
                    for (String alg : ALGORITMOS) {
                        System.out.println("  " + alg);
                        medir(res, out, serial(alg), alg, "serial", 1, tipo, n, base);
                        for (int t : threads) {
                            medir(res, out, paralelo(alg, pools.get(t)), alg, "paralelo", t, tipo, n, base);
                        }
                    }
                }
            }
        } finally {
            pools.values().forEach(ForkJoinPool::shutdown);
        }
        System.out.println("Amostras gravadas em: " + csv.toAbsolutePath());
        return res;
    }

    private static void medir(Resultados res, BufferedWriter out, Ordenador o, String alg, String modo,
                              int threads, String tipo, int n, int[] base) throws IOException {
        String rotulo = modo.equals("serial") ? "serial" : "paralelo " + threads + (threads == 1 ? " thread" : " threads");
        System.out.printf("    %-20s amostras:", rotulo);
        System.out.flush();
        long totalNs = 0, minNs = Long.MAX_VALUE, maxNs = 0;
        for (int amostra = 1; amostra <= AMOSTRAS; amostra++) {
            int[] copia = base.clone();
            long t0 = System.nanoTime();
            o.ordenar(copia);
            long dt = System.nanoTime() - t0;
            System.out.print(" " + amostra);
            System.out.flush();
            totalNs += dt;
            minNs = Math.min(minNs, dt);
            maxNs = Math.max(maxNs, dt);
            Resultados.Registro reg = new Resultados.Registro(alg, modo, threads, tipo, n, amostra, dt);
            res.adicionar(reg);
            out.write(reg.paraCsv());
            out.newLine();
        }
        out.flush();
        System.out.printf(BR, "  ->  media %12.3f ms  (min %.3f | max %.3f)%n",
                totalNs / (double) AMOSTRAS / 1e6, minNs / 1e6, maxNs / 1e6);
    }
}
