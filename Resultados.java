import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Resultados {
    static final String CABECALHO = "algoritmo;modo;threads;tipo_dado;tamanho;amostra;tempo_ns";

    static final class Registro {
        final String algoritmo, modo, tipo;
        final int threads, tamanho, amostra;
        final long ns;

        Registro(String algoritmo, String modo, int threads, String tipo, int tamanho, int amostra, long ns) {
            this.algoritmo = algoritmo; this.modo = modo; this.threads = threads;
            this.tipo = tipo; this.tamanho = tamanho; this.amostra = amostra; this.ns = ns;
        }

        String paraCsv() {
            return algoritmo + ";" + modo + ";" + threads + ";" + tipo + ";" + tamanho + ";" + amostra + ";" + ns;
        }
    }

    private final Map<String, List<Long>> amostras = new LinkedHashMap<>();
    private final Set<String> algoritmos = new LinkedHashSet<>();
    private final Set<String> tipos = new LinkedHashSet<>();
    private final TreeSet<Integer> tamanhos = new TreeSet<>();
    private final TreeSet<Integer> threadsParalelo = new TreeSet<>();

    private static String chave(String alg, String modo, int threads, String tipo, int tamanho) {
        return alg + "|" + modo + "|" + threads + "|" + tipo + "|" + tamanho;
    }

    void adicionar(Registro r) {
        amostras.computeIfAbsent(chave(r.algoritmo, r.modo, r.threads, r.tipo, r.tamanho), k -> new ArrayList<>())
                .add(r.ns);
        algoritmos.add(r.algoritmo);
        tipos.add(r.tipo);
        tamanhos.add(r.tamanho);
        if (r.modo.equals("paralelo")) threadsParalelo.add(r.threads);
    }

    List<String> algoritmos() { return new ArrayList<>(algoritmos); }
    List<String> tipos() { return new ArrayList<>(tipos); }
    List<Integer> tamanhos() { return new ArrayList<>(tamanhos); }
    List<Integer> threads() { return new ArrayList<>(threadsParalelo); }

    double mediaMs(String alg, String modo, int threads, String tipo, int tamanho) {
        List<Long> v = amostras.get(chave(alg, modo, threads, tipo, tamanho));
        if (v == null || v.isEmpty()) return Double.NaN;
        double soma = 0;
        for (long x : v) soma += x;
        return soma / v.size() / 1e6;
    }

    void salvarResumo(Path destino) throws IOException {
        Locale br = Locale.forLanguageTag("pt-BR");
        try (Writer w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            w.write("algoritmo;modo;threads;tipo_dado;tamanho;n_amostras;media_ms;desvio_ms;min_ms;max_ms;speedup;eficiencia\n");
            for (Map.Entry<String, List<Long>> e : amostras.entrySet()) {
                String[] k = e.getKey().split("\\|");
                String alg = k[0], modo = k[1], tipo = k[3];
                int threads = Integer.parseInt(k[2]), tamanho = Integer.parseInt(k[4]);
                List<Long> v = e.getValue();
                int n = v.size();

                double soma = 0, min = Double.MAX_VALUE, max = 0;
                for (long x : v) {
                    double ms = x / 1e6;
                    soma += ms;
                    min = Math.min(min, ms);
                    max = Math.max(max, ms);
                }
                double media = soma / n;
                double var = 0;
                for (long x : v) {
                    double d = x / 1e6 - media;
                    var += d * d;
                }
                double desvio = n > 1 ? Math.sqrt(var / (n - 1)) : 0;

                double serial = mediaMs(alg, "serial", 1, tipo, tamanho);
                double speedup = serial / media;
                double eficiencia = speedup / threads;

                w.write(String.format(br, "%s;%s;%d;%s;%d;%d;%.4f;%.4f;%.4f;%.4f;%.4f;%.4f%n",
                        alg, modo, threads, tipo, tamanho, n, media, desvio, min, max, speedup, eficiencia));
            }
        }
    }
}
