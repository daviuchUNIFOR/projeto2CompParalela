import javax.swing.SwingUtilities;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Resultados res = Benchmark.executar(Path.of("resultados.csv"));

        res.salvarResumo(Path.of("resumo.csv"));
        System.out.println("Resumo estatistico salvo em: " + Path.of("resumo.csv").toAbsolutePath());

        SwingUtilities.invokeLater(() -> new JanelaGraficos(res).setVisible(true));
    }
}
