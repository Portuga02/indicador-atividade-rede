package com.networkactivity.ui;

import com.networkactivity.rede.InterfaceDeRede;
import com.networkactivity.rede.MonitorDeRede;
import com.networkactivity.util.Formatador;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tela de estatisticas avancadas: lista todas as interfaces de rede da maquina
 * com seus detalhes (endereco MAC, status, total enviado/recebido).
 * Versao simplificada do frmAdvanced.cs original (que dependia de APIs
 * especificas do Windows para tabelas de conexao TCP/UDP por processo).
 */
public class DialogoAvancado extends JDialog {

    private final DefaultTableModel modelo;
    private final MonitorDeRede monitor;
    private javax.swing.Timer timerAtualizacao;

    public DialogoAvancado(Frame dono, MonitorDeRede monitor) {
        super(dono, "Estatisticas Avancadas", false);
        this.monitor = monitor;
        setLayout(new BorderLayout(8, 8));
        setSize(680, 380);
        setLocationRelativeTo(dono);

        modelo = new DefaultTableModel(
                new Object[]{"Interface", "Endereco MAC", "Possui IP?", "Recebido (total)", "Enviado (total)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(22);
        tabela.getTableHeader().setReorderingAllowed(false);

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton atualizar = new JButton("Atualizar agora");
        atualizar.addActionListener(e -> atualizarDados());
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        rodape.add(atualizar);
        rodape.add(fechar);
        add(rodape, BorderLayout.SOUTH);

        atualizarDados();

        timerAtualizacao = new javax.swing.Timer(3000, e -> atualizarDados());
        timerAtualizacao.start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                timerAtualizacao.stop();
            }
        });
    }

    private void atualizarDados() {
        List<InterfaceDeRede> lista = monitor.listarInterfaces();
        modelo.setRowCount(0);
        for (InterfaceDeRede itf : lista) {
            modelo.addRow(new Object[]{
                    itf.getNomeExibicao(),
                    itf.getMacAddress().isEmpty() ? "-" : itf.getMacAddress(),
                    itf.isPossuiIp() ? "Sim" : "Nao",
                    Formatador.formatarBytes(itf.getBytesRecebidos()),
                    Formatador.formatarBytes(itf.getBytesEnviados())
            });
        }
    }

    @Override
    public void dispose() {
        if (timerAtualizacao != null) timerAtualizacao.stop();
        super.dispose();
    }
}
