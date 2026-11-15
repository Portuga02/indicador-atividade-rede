package com.networkactivity;

import com.networkactivity.ui.JanelaPrincipal;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Collections;

public class Main {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignorado) {

			}

			aplicarFontePadrao();

			JanelaPrincipal janela = new JanelaPrincipal();

			if (SystemTray.isSupported()) {
				janela.setVisible(false);
			} else {
				janela.setVisible(true);
			}
		});
	}

	/** Define Tahoma (fonte classica do XP) em todos os componentes Swing. */
	private static void aplicarFontePadrao() {
		FontUIResource tahoma = new FontUIResource(new Font("Tahoma", Font.PLAIN, 12));
		for (Object chave : Collections.list(UIManager.getDefaults().keys())) {
			if (UIManager.get(chave) instanceof FontUIResource) {
				UIManager.put(chave, tahoma);
			}
		}
	}
}