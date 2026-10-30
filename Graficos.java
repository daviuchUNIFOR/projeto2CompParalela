import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleFunction;

public class Graficos {
    private static final DecimalFormat FMT =
            new DecimalFormat("#,##0.####", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

    private static final Color GRADE = new Color(0xE3E3E3);
    private static final Color EIXO = new Color(0x555555);
    private static final Color TEXTO = new Color(0x333333);

    static final class Serie {
        final String nome;
        final Color cor;
        final double[] x, y;
        final boolean tracejada;

        Serie(String nome, Color cor, double[] x, double[] y, boolean tracejada) {
            this.nome = nome; this.cor = cor; this.x = x; this.y = y; this.tracejada = tracejada;
        }
    }

    static final class Dados {
        String titulo = "", eixoX = "", eixoY = "";
        boolean barras, logX, logY;
        double[] ticksX;
        DoubleFunction<String> formatoX = Graficos::num;
        final List<String> categorias = new ArrayList<>();
        final List<Serie> series = new ArrayList<>();

        void adicionar(Serie s) {
            for (double v : s.y) {
                if (!Double.isNaN(v)) {
                    series.add(s);
                    return;
                }
            }
        }
    }

    static Dados tempoPorTamanho(Resultados r, String tipo, int threads) {
        Dados d = new Dados();
        d.titulo = "Tempo × tamanho da entrada  (dados: " + nomeTipo(tipo) + "; paralelo com "
                + threads + (threads == 1 ? " thread)" : " threads)");
        d.eixoX = "Tamanho da entrada (n) — escala log";
        d.eixoY = "Tempo médio (ms) — escala log";
        d.logX = true;
        d.logY = true;
        d.formatoX = Graficos::tamanho;
        List<Integer> tams = r.tamanhos();
        for (String alg : r.algoritmos()) {
            double[] x = new double[tams.size()], ys = new double[tams.size()], yp = new double[tams.size()];
            for (int i = 0; i < tams.size(); i++) {
                int n = tams.get(i);
                x[i] = n;
                ys[i] = r.mediaMs(alg, "serial", 1, tipo, n);
                yp[i] = r.mediaMs(alg, "paralelo", threads, tipo, n);
            }
            d.adicionar(new Serie(alg + " (serial)", cor(alg), x, ys, true));
            d.adicionar(new Serie(alg + " (paralelo, " + threads + " thr.)", cor(alg), x, yp, false));
        }
        return d;
    }

    static Dados speedupPorThreads(Resultados r, String tipo, int tamanho) {
        Dados d = new Dados();
        d.titulo = "Speedup × número de threads  (dados: " + nomeTipo(tipo) + "; n = " + num(tamanho) + ")";
        d.eixoX = "Número de threads";
        d.eixoY = "Speedup (tempo serial ÷ tempo paralelo)";
        List<Integer> ths = r.threads();
        double[] x = new double[ths.size()];
        for (int i = 0; i < x.length; i++) x[i] = ths.get(i);
        d.ticksX = x;
        for (String alg : r.algoritmos()) {
            double serial = r.mediaMs(alg, "serial", 1, tipo, tamanho);
            double[] y = new double[x.length];
            for (int i = 0; i < x.length; i++) {
                y[i] = serial / r.mediaMs(alg, "paralelo", ths.get(i), tipo, tamanho);
            }
            d.adicionar(new Serie(alg, cor(alg), x, y, false));
        }
        d.adicionar(new Serie("Speedup ideal", new Color(0x9CA3AF), x, x.clone(), true));
        return d;
    }

    static Dados tempoPorTipo(Resultados r, int tamanho, int threads) {
        Dados d = new Dados();
        d.barras = true;
        d.titulo = "Tempo por tipo de dado  (n = " + num(tamanho) + "; paralelo com " + threads
                + (threads == 1 ? " thread)" : " threads)");
        d.eixoX = "Tipo de dado";
        d.eixoY = "Tempo médio (ms) — escala log";
        d.logY = true;
        List<String> tipos = r.tipos();
        for (String t : tipos) d.categorias.add(nomeTipo(t));
        for (String alg : r.algoritmos()) {
            double[] ys = new double[tipos.size()], yp = new double[tipos.size()];
            for (int i = 0; i < tipos.size(); i++) {
                ys[i] = r.mediaMs(alg, "serial", 1, tipos.get(i), tamanho);
                yp[i] = r.mediaMs(alg, "paralelo", threads, tipos.get(i), tamanho);
            }
            d.adicionar(new Serie(alg + " (serial)", clarear(cor(alg)), null, ys, false));
            d.adicionar(new Serie(alg + " (paralelo, " + threads + " thr.)", cor(alg), null, yp, false));
        }
        return d;
    }

    static String num(double v) { return FMT.format(v); }

    static String tamanho(double v) {
        if (v >= 1e6 && v % 1e6 == 0) return (long) (v / 1e6) + "M";
        if (v >= 1e3 && v % 1e3 == 0) return (long) (v / 1e3) + "k";
        return num(v);
    }

    static String nomeTipo(String t) {
        switch (t) {
            case "aleatorio": return "aleatório";
            case "ordenado": return "ordenado";
            case "inverso": return "inverso";
            default: return t;
        }
    }

    static Color cor(String alg) {
        switch (alg) {
            case "MergeSort": return new Color(0x2A6FDB);
            case "QuickSort": return new Color(0xE8710A);
            case "SelectionSort": return new Color(0x2E9E5B);
            case "BubbleSort": return new Color(0x8B5CF6);
            default: return new Color(0x6B7280);
        }
    }

    static Color clarear(Color c) {
        return new Color((int) (c.getRed() * 0.45 + 255 * 0.55),
                (int) (c.getGreen() * 0.45 + 255 * 0.55),
                (int) (c.getBlue() * 0.45 + 255 * 0.55));
    }

    static final class Painel extends JPanel {
        private Dados dados;

        Painel() {
            setPreferredSize(new Dimension(940, 600));
            setBackground(Color.WHITE);
        }

        void setDados(Dados d) { this.dados = d; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (dados == null) return;
            Graphics2D g = (Graphics2D) g0.create();
            desenhar(g, dados, getWidth(), getHeight());
            g.dispose();
        }
    }

    private static final class Escala {
        final boolean log;
        final double lo, hi;

        Escala(boolean log, double lo, double hi) { this.log = log; this.lo = lo; this.hi = hi; }

        double frac(double v) {
            return log ? (Math.log10(v) - Math.log10(lo)) / (Math.log10(hi) - Math.log10(lo))
                       : (v - lo) / (hi - lo);
        }
    }

    private static double[] potencias(int expLo, int expHi) {
        double[] t = new double[expHi - expLo + 1];
        for (int e = expLo; e <= expHi; e++) t[e - expLo] = Math.pow(10, e);
        return t;
    }

    private static double[] ticksLineares(double max, int alvo) {
        if (!(max > 0)) max = 1;
        double bruto = max / alvo;
        double mag = Math.pow(10, Math.floor(Math.log10(bruto)));
        double r = bruto / mag;
        double passo = (r <= 1 ? 1 : r <= 2 ? 2 : r <= 5 ? 5 : 10) * mag;
        int n = (int) Math.ceil(max / passo - 1e-9);
        double[] t = new double[n + 1];
        for (int i = 0; i <= n; i++) t[i] = i * passo;
        return t;
    }

    private static BasicStroke estilo(boolean tracejada) {
        return tracejada
                ? new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{8f, 6f}, 0f)
                : new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    static void desenhar(Graphics2D g, Dados d, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        Font normal = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        g.setFont(normal.deriveFont(Font.BOLD, 15f));
        FontMetrics fmTitulo = g.getFontMetrics();
        g.setColor(new Color(0x222222));
        g.drawString(d.titulo, (w - fmTitulo.stringWidth(d.titulo)) / 2f, 26);

        g.setFont(normal);
        FontMetrics fm = g.getFontMetrics();

        List<int[]> posLegenda = new ArrayList<>();
        int lx = 20, linha = 0;
        for (Serie s : d.series) {
            int largura = 34 + fm.stringWidth(s.nome) + 18;
            if (lx + largura > w - 20 && lx > 20) { lx = 20; linha++; }
            posLegenda.add(new int[]{lx, linha});
            lx += largura;
        }
        int alturaLegenda = (d.series.isEmpty() ? 0 : linha + 1) * 20 + 6;

        int esq = 80, dir = 30, topo = 48, base = 58 + alturaLegenda;
        int px = esq, py = topo, pw = w - esq - dir, ph = h - topo - base;

        double ymin = Double.MAX_VALUE, ymax = -Double.MAX_VALUE;
        for (Serie s : d.series) {
            for (double v : s.y) {
                if (Double.isNaN(v) || (d.logY && v <= 0)) continue;
                ymin = Math.min(ymin, v);
                ymax = Math.max(ymax, v);
            }
        }
        if (ymax < ymin) { ymin = 1; ymax = 1; }
        double[] ticksY;
        Escala ey;
        if (d.logY) {
            int elo = (int) Math.floor(Math.log10(ymin)), ehi = (int) Math.ceil(Math.log10(ymax));
            if (ehi <= elo) ehi = elo + 1;
            ticksY = potencias(elo, ehi);
            ey = new Escala(true, ticksY[0], ticksY[ticksY.length - 1]);
        } else {
            ticksY = ticksLineares(ymax, 6);
            ey = new Escala(false, 0, ticksY[ticksY.length - 1]);
        }

        Escala ex = null;
        double[] ticksX = null;
        if (!d.barras) {
            double xmin = Double.MAX_VALUE, xmax = -Double.MAX_VALUE;
            for (Serie s : d.series) {
                for (double v : s.x) { xmin = Math.min(xmin, v); xmax = Math.max(xmax, v); }
            }
            if (d.logX) {
                int elo = (int) Math.floor(Math.log10(xmin)), ehi = (int) Math.ceil(Math.log10(xmax));
                if (ehi <= elo) ehi = elo + 1;
                ticksX = potencias(elo, ehi);
                ex = new Escala(true, ticksX[0], ticksX[ticksX.length - 1]);
            } else {
                ticksX = d.ticksX != null ? d.ticksX : ticksLineares(xmax, 6);
                double a = ticksX[0], b = ticksX[ticksX.length - 1];
                if (b <= a) { a -= 1; b += 1; }
                double pad = 0.04 * (b - a);
                ex = new Escala(false, a - pad, b + pad);
            }
        }

        g.setStroke(new BasicStroke(1f));
        for (double t : ticksY) {
            float yy = (float) (py + ph - ey.frac(t) * ph);
            g.setColor(GRADE);
            g.draw(new Line2D.Float(px, yy, px + pw, yy));
            String s = num(t);
            g.setColor(TEXTO);
            g.drawString(s, px - 8 - fm.stringWidth(s), yy + 4);
        }

        if (!d.barras) {
            for (double t : ticksX) {
                float xx = (float) (px + ex.frac(t) * pw);
                g.setColor(GRADE);
                g.draw(new Line2D.Float(xx, py, xx, py + ph));
                String s = d.formatoX.apply(t);
                g.setColor(TEXTO);
                g.drawString(s, xx - fm.stringWidth(s) / 2f, py + ph + 18);
            }
        } else {
            int n = d.categorias.size();
            double grupo = (double) pw / Math.max(1, n);
            for (int i = 0; i < n; i++) {
                String s = d.categorias.get(i);
                g.setColor(TEXTO);
                g.drawString(s, (float) (px + i * grupo + grupo / 2 - fm.stringWidth(s) / 2.0), py + ph + 18);
            }
        }

        if (d.barras) {
            int n = d.categorias.size(), m = d.series.size();
            double grupo = (double) pw / Math.max(1, n);
            double usada = grupo * 0.72;
            double larg = usada / Math.max(1, m);
            for (int j = 0; j < m; j++) {
                Serie s = d.series.get(j);
                g.setColor(s.cor);
                for (int i = 0; i < n; i++) {
                    double v = s.y[i];
                    if (Double.isNaN(v)) continue;
                    double bx = px + i * grupo + (grupo - usada) / 2 + j * larg;
                    double alt = ey.frac(v) * ph;
                    g.fill(new Rectangle2D.Double(bx, py + ph - alt, larg - 2, alt));
                }
            }
        } else {
            for (Serie s : d.series) {
                g.setColor(s.cor);
                g.setStroke(estilo(s.tracejada));
                Path2D.Double caminho = new Path2D.Double();
                boolean aberto = false;
                for (int i = 0; i < s.x.length; i++) {
                    if (Double.isNaN(s.y[i]) || (d.logY && s.y[i] <= 0)) { aberto = false; continue; }
                    double X = px + ex.frac(s.x[i]) * pw, Y = py + ph - ey.frac(s.y[i]) * ph;
                    if (!aberto) { caminho.moveTo(X, Y); aberto = true; } else caminho.lineTo(X, Y);
                }
                g.draw(caminho);
                g.setStroke(new BasicStroke(1.5f));
                for (int i = 0; i < s.x.length; i++) {
                    if (Double.isNaN(s.y[i]) || (d.logY && s.y[i] <= 0)) continue;
                    double X = px + ex.frac(s.x[i]) * pw, Y = py + ph - ey.frac(s.y[i]) * ph;
                    g.fill(new Ellipse2D.Double(X - 4, Y - 4, 8, 8));
                }
            }
        }

        g.setColor(EIXO);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Line2D.Float(px, py, px, py + ph));
        g.draw(new Line2D.Float(px, py + ph, px + pw, py + ph));

        g.setColor(TEXTO);
        g.drawString(d.eixoX, px + (pw - fm.stringWidth(d.eixoX)) / 2f, py + ph + 40);
        AffineTransform antigo = g.getTransform();
        g.rotate(-Math.PI / 2);
        g.drawString(d.eixoY, -(py + ph / 2f) - fm.stringWidth(d.eixoY) / 2f, 18);
        g.setTransform(antigo);

        int ly0 = py + ph + 54;
        for (int i = 0; i < d.series.size(); i++) {
            Serie s = d.series.get(i);
            int x0 = posLegenda.get(i)[0], y0 = ly0 + posLegenda.get(i)[1] * 20 + 8;
            g.setColor(s.cor);
            if (d.barras) {
                g.fill(new Rectangle2D.Double(x0 + 4, y0 - 5, 18, 11));
            } else {
                g.setStroke(estilo(s.tracejada));
                g.draw(new Line2D.Float(x0, y0, x0 + 26, y0));
            }
            g.setColor(TEXTO);
            g.drawString(s.nome, x0 + 32, y0 + 4);
        }
    }
}
