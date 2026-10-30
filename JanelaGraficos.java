import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Supplier;

public class JanelaGraficos extends JFrame {
    public JanelaGraficos(Resultados res) {
        super("Ordenacao serial x paralelo");

        JComboBox<String> tipo1 = comboTipos(res);
        JComboBox<Integer> threads1 = comboInteiros(res.threads());

        JComboBox<String> tipo2 = comboTipos(res);
        JComboBox<Integer> tamanho2 = comboInteiros(res.tamanhos());

        JComboBox<Integer> tamanho3 = comboInteiros(res.tamanhos());
        JComboBox<Integer> threads3 = comboInteiros(res.threads());

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Tempo × tamanho",
                aba(new String[]{"Tipo de dado:", "Threads (paralelo):"},
                        new JComboBox<?>[]{tipo1, threads1},
                        () -> Graficos.tempoPorTamanho(res, (String) tipo1.getSelectedItem(),
                                (Integer) threads1.getSelectedItem())));
        abas.addTab("Speedup × threads",
                aba(new String[]{"Tipo de dado:", "Tamanho (n):"},
                        new JComboBox<?>[]{tipo2, tamanho2},
                        () -> Graficos.speedupPorThreads(res, (String) tipo2.getSelectedItem(),
                                (Integer) tamanho2.getSelectedItem())));
        abas.addTab("Tempo por tipo de dado",
                aba(new String[]{"Tamanho (n):", "Threads (paralelo):"},
                        new JComboBox<?>[]{tamanho3, threads3},
                        () -> Graficos.tempoPorTipo(res, (Integer) tamanho3.getSelectedItem(),
                                (Integer) threads3.getSelectedItem())));

        add(abas);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private static JComboBox<String> comboTipos(Resultados res) {
        JComboBox<String> cb = new JComboBox<>(res.tipos().toArray(new String[0]));
        return cb;
    }

    private static JComboBox<Integer> comboInteiros(java.util.List<Integer> valores) {
        JComboBox<Integer> cb = new JComboBox<>(valores.toArray(new Integer[0]));
        if (cb.getItemCount() > 0) cb.setSelectedIndex(cb.getItemCount() - 1);
        return cb;
    }

    private JPanel aba(String[] rotulos, JComboBox<?>[] combos, Supplier<Graficos.Dados> gerador) {
        Graficos.Painel painel = new Graficos.Painel();
        Runnable atualizar = () -> painel.setDados(gerador.get());

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        for (int i = 0; i < combos.length; i++) {
            topo.add(new JLabel(rotulos[i]));
            topo.add(combos[i]);
            combos[i].addActionListener(e -> atualizar.run());
        }

        atualizar.run();

        JPanel aba = new JPanel(new BorderLayout());
        aba.add(topo, BorderLayout.NORTH);
        aba.add(painel, BorderLayout.CENTER);
        return aba;
    }
}
