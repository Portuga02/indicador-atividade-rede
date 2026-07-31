package com.networkactivity.ui;

import javax.swing.*;
import java.awt.*;

/** Tela "Sobre", com informacoes do programa e creditos. */
public class DialogoSobre extends JDialog {

    public DialogoSobre(Frame dono) {
        super(dono, "Sobre o Indicador de Atividade de Rede", true);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titulo = new JLabel("Indicador de Atividade de Rede");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versao = new JLabel("Versao 3.0 (edicao Java)");
        versao.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea texto = new JTextArea(
                "\nMonitor de trafego de rede com icone piscante na bandeja " +
                "do sistema, no estilo classico do Windows XP.\n\n" +
                "Baseado no projeto GabNetStats (GabSoftware) e em seu fork " +
                "Network Activity Indicator (boroda74).\n\n" +
                "Agradecimentos:\n" +
                "- GabSoftware, pelo aplicativo original GabNetStats\n" +
                "- Igor Tolmachev, pela ideia original\n" +
                "- boroda74, pelo tema visual do Windows XP\n"
        );
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setOpaque(false);
        texto.setFont(UIManager.getFont("Label.font"));
        texto.setAlignmentX(Component.CENTER_ALIGNMENT);
        texto.setMaximumSize(new Dimension(360, 220));

        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        fechar.setAlignmentX(Component.CENTER_ALIGNMENT);

        conteudo.add(titulo);
        conteudo.add(Box.createVerticalStrut(4));
        conteudo.add(versao);
        conteudo.add(Box.createVerticalStrut(10));
        conteudo.add(texto);
        conteudo.add(Box.createVerticalStrut(10));
        conteudo.add(fechar);

        add(conteudo, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(dono);
    }
}
