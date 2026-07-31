package com.networkactivity;

import com.networkactivity.ui.JanelaPrincipal;

import javax.swing.*;
import java.awt.*;

/**
 * Ponto de entrada do Indicador de Atividade de Rede (edicao Java).
 * Porte do projeto original "Network Activity Indicator" (fork de GabNetStats),
 * originalmente escrito em C#/.NET WinForms.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorado) {
                // usa o look and feel padrao (Metal) se o do sistema falhar
            }

            JanelaPrincipal janela = new JanelaPrincipal();

            // Se a bandeja do sistema estiver disponivel, comeca minimizado nela;
            // caso contrario, mostra a janela principal diretamente.
            if (SystemTray.isSupported()) {
                janela.setVisible(false);
            } else {
                janela.setVisible(true);
            }
        });
    }
}
