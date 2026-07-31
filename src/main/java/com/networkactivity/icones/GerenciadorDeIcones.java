package com.networkactivity.icones;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carrega o conjunto de icones (tema visual) usado na bandeja do sistema.
 * Portado de applyIconSet() / ObterIconeDownload() / ObterIconeUpload() do MainForm.cs original.
 * <p>
 * As cores seguem a mesma logica do C#: 0=Azul(ocioso) 1=Verde 2=Amarelo 3=Laranja 4=Vermelho
 */
public class GerenciadorDeIcones {

    private static final String[] CORES = {"blue", "green", "yellow", "orange", "red"};

    private final Map<String, Image> icones = new HashMap<>();
    private Image[][] matrizAtiva;      // [nivelUpload][nivelDownload] -> combinacao "active_X_Y"
    private Image[] iconesDownload;     // receive_X
    private Image[] iconesUpload;       // send_X
    private Image iconeInativo;
    private Image iconeCirculoVerde;
    private Image iconeCirculoAmarelo;
    private Image iconeCirculoLaranja;
    private Image iconeCirculoVermelho;
    private Image iconeCirculoCinza;

    public GerenciadorDeIcones() {
        carregarTudo();
    }

    private void carregarTudo() {
        for (String u : CORES) {
            for (String d : CORES) {
                carregarRecurso("active_" + u + "_" + d);
            }
        }
        for (String c : CORES) {
            carregarRecurso("receive_" + c);
            carregarRecurso("send_" + c);
        }
        carregarRecurso("inactive");
        carregarRecurso("circle_green");
        carregarRecurso("circle_orange");
        carregarRecurso("circle_red");
        carregarRecurso("circle_grey");

        matrizAtiva = new Image[5][5];
        for (int u = 0; u < 5; u++) {
            for (int d = 0; d < 5; d++) {
                matrizAtiva[u][d] = icones.get("active_" + CORES[u] + "_" + CORES[d]);
            }
        }

        iconesDownload = new Image[5];
        iconesUpload = new Image[5];
        for (int i = 0; i < 5; i++) {
            iconesDownload[i] = icones.get("receive_" + CORES[i]);
            iconesUpload[i] = icones.get("send_" + CORES[i]);
        }

        iconeInativo = icones.get("inactive");
        iconeCirculoVerde = icones.get("circle_green");
        iconeCirculoAmarelo = icones.get("circle_orange"); // paleta original nao tem amarelo p/ circulo; usa laranja como intermediario
        iconeCirculoLaranja = icones.get("circle_orange");
        iconeCirculoVermelho = icones.get("circle_red");
        iconeCirculoCinza = icones.get("circle_grey");
    }

    private void carregarRecurso(String nome) {
        String caminho = "/icons/" + nome + ".png";
        try (InputStream is = GerenciadorDeIcones.class.getResourceAsStream(caminho)) {
            if (is == null) {
                System.err.println("Icone nao encontrado: " + caminho);
                return;
            }
            icones.put(nome, ImageIO.read(is));
        } catch (IOException e) {
            System.err.println("Falha ao carregar icone " + caminho + ": " + e.getMessage());
        }
    }

    /** Icone para quando ha upload E download simultaneos. */
    public Image getIconeAtivo(int nivelUpload, int nivelDownload) {
        return matrizAtiva[clamp(nivelUpload)][clamp(nivelDownload)];
    }

    /** Icone para quando ha apenas download. */
    public Image getIconeDownload(int nivel) {
        return iconesDownload[clamp(nivel)];
    }

    /** Icone para quando ha apenas upload. */
    public Image getIconeUpload(int nivel) {
        return iconesUpload[clamp(nivel)];
    }

    public Image getIconeInativo() {
        return iconeInativo;
    }

    public Image getIconeCirculoStatus(StatusConexao status) {
        return switch (status) {
            case OK -> iconeCirculoVerde;
            case ALERTA -> iconeCirculoLaranja;
            case FALHA -> iconeCirculoVermelho;
            case DESCONHECIDO -> iconeCirculoCinza;
        };
    }

    private int clamp(int nivel) {
        return Math.max(0, Math.min(4, nivel));
    }

    public enum StatusConexao { OK, ALERTA, FALHA, DESCONHECIDO }
}
