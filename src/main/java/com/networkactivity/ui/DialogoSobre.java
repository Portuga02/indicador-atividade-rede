package com.networkactivity.ui;

import javax.swing.*;
import java.awt.*;

/** Tela "Sobre", com informacoes do programa e creditos. */
public class DialogoSobre extends JDialog {

	private static final Color AZUL_CLARO = new Color(58, 128, 232);
	private static final Color AZUL_ESCURO = new Color(37, 90, 190);

	public DialogoSobre(Frame dono) {
		super(dono, "Sobre o Indicador de Atividade de Rede", true);
		setLayout(new BorderLayout());
		setResizable(false);

		// Cabecalho com degrade azul estilo XP
		JPanel cabecalho = new JPanel(new GridLayout(2, 1)) {
			@Override
			protected void paintComponent(Graphics g0) {
				Graphics2D g = (Graphics2D) g0.create();
				g.setPaint(new GradientPaint(0, 0, AZUL_CLARO, 0, getHeight(), AZUL_ESCURO));
				g.fillRect(0, 0, getWidth(), getHeight());
				g.dispose();
			}
		};
		cabecalho.setOpaque(false);
		cabecalho.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

		JLabel titulo = new JLabel("Indicador de Atividade de Rede");
		titulo.setFont(new Font("Tahoma", Font.BOLD, 16));
		titulo.setForeground(Color.WHITE);

		JLabel versao = new JLabel("Versao 3.0 (edicao Java)");
		versao.setForeground(new Color(220, 235, 255));

		cabecalho.add(titulo);
		cabecalho.add(versao);

		// Corpo
		JPanel conteudo = new JPanel();
		conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
		conteudo.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

		JTextArea texto = new JTextArea("Monitor de trafego de rede com icone piscante na bandeja "
				+ "do sistema, no estilo classico do Windows XP.\n\n"
				+ "Baseado no projeto GabNetStats (GabSoftware) e em seu fork "
				+ "Network Activity Indicator (boroda74).\n\n" + "Agradecimentos:\n"
				+ "- GabSoftware, pelo aplicativo original GabNetStats\n" + "- Igor Tolmachev, pela ideia original\n"
				+ "- boroda74, pelo tema visual do Windows XP");
		texto.setEditable(false);
		texto.setFocusable(false);
		texto.setLineWrap(true);
		texto.setWrapStyleWord(true);
		texto.setOpaque(false);
		texto.setFont(UIManager.getFont("Label.font"));
		texto.setColumns(34);
		texto.setRows(11);
		texto.setAlignmentX(Component.LEFT_ALIGNMENT);

		conteudo.add(texto);

		// Rodape com botao
		JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
		JButton fechar = new JButton("Fechar");
		fechar.addActionListener(e -> dispose());
		rodape.add(fechar);

		getRootPane().setDefaultButton(fechar);
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		add(cabecalho, BorderLayout.NORTH);
		add(conteudo, BorderLayout.CENTER);
		add(rodape, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(dono);
	}
}