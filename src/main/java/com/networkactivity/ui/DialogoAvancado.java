package com.networkactivity.ui;

import com.networkactivity.rede.InterfaceDeRede;
import com.networkactivity.rede.MonitorDeRede;
import com.networkactivity.util.Formatador;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tela de estatisticas avancadas: lista todas as interfaces de rede da maquina
 * com seus detalhes (endereco MAC, status, total enviado/recebido). Versao
 * simplificada do frmAdvanced.cs original (que dependia de APIs especificas do
 * Windows para tabelas de conexao TCP/UDP por processo).
 */
public class DialogoAvancado extends JDialog {

	private static final Color AZUL_BORDA = new Color(0, 60, 116);
	private static final Color LINHA_ZEBRA = new Color(240, 245, 255);

	private final DefaultTableModel modelo;
	private final MonitorDeRede monitor;
	private javax.swing.Timer timerAtualizacao;

	/** Renderer com linhas zebradas e alinhamento configuravel. */
	private static class RendererZebra extends DefaultTableCellRenderer {
		RendererZebra(int alinhamento) {
			setHorizontalAlignment(alinhamento);
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
			super.getTableCellRendererComponent(t, v, sel, foc, r, c);
			setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
			if (!sel) {
				setBackground(r % 2 == 0 ? Color.WHITE : LINHA_ZEBRA);
			}
			return this;
		}
	}

	public DialogoAvancado(Frame dono, MonitorDeRede monitor) {
		super(dono, "Estatisticas Avancadas", false);
		this.monitor = monitor;
		setLayout(new BorderLayout(8, 8));
		setSize(720, 400);
		setLocationRelativeTo(dono);

		modelo = new DefaultTableModel(
				new Object[] { "Interface", "Endereco MAC", "Possui IP?", "Recebido (total)", "Enviado (total)" }, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JTable tabela = new JTable(modelo);
		tabela.setRowHeight(24);
		tabela.setShowGrid(false);
		tabela.setIntercellSpacing(new Dimension(0, 0));
		tabela.setFillsViewportHeight(true);
		tabela.getTableHeader().setReorderingAllowed(false);
		tabela.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));

		RendererZebra esquerda = new RendererZebra(SwingConstants.LEFT);
		RendererZebra centro = new RendererZebra(SwingConstants.CENTER);
		RendererZebra direita = new RendererZebra(SwingConstants.RIGHT);
		tabela.getColumnModel().getColumn(0).setCellRenderer(esquerda);
		tabela.getColumnModel().getColumn(1).setCellRenderer(esquerda);
		tabela.getColumnModel().getColumn(2).setCellRenderer(centro);
		tabela.getColumnModel().getColumn(3).setCellRenderer(direita);
		tabela.getColumnModel().getColumn(4).setCellRenderer(direita);

		tabela.getColumnModel().getColumn(0).setPreferredWidth(220);
		tabela.getColumnModel().getColumn(1).setPreferredWidth(150);
		tabela.getColumnModel().getColumn(2).setPreferredWidth(80);

		JScrollPane rolagem = new JScrollPane(tabela);
		rolagem.setBorder(BorderFactory.createLineBorder(AZUL_BORDA));

		JPanel centroPainel = new JPanel(new BorderLayout());
		centroPainel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		centroPainel.add(rolagem, BorderLayout.CENTER);
		add(centroPainel, BorderLayout.CENTER);

		JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
		JButton atualizar = new JButton("Atualizar agora");
		atualizar.addActionListener(e -> atualizarDados());
		JButton fechar = new JButton("Fechar");
		fechar.addActionListener(e -> dispose());
		rodape.add(atualizar);
		rodape.add(fechar);
		add(rodape, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(fechar);
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

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
			modelo.addRow(
					new Object[] { itf.getNomeExibicao(), itf.getMacAddress().isEmpty() ? "-" : itf.getMacAddress(),
							itf.isPossuiIp() ? "Sim" : "Nao", Formatador.formatarBytes(itf.getBytesRecebidos()),
							Formatador.formatarBytes(itf.getBytesEnviados()) });
		}
	}

	@Override
	public void dispose() {
		if (timerAtualizacao != null)
			timerAtualizacao.stop();
		super.dispose();
	}
}