package com.networkactivity.rede;

/**
 * Calcula o nivel de cor (0=Azul ocioso .. 4=Vermelho critico) de acordo com a
 * velocidade atual em relacao ao limite de banda configurado pelo usuario.
 * Porte direto de ObterNivelCor() no MainForm.cs original.
 */
public class CalculadoraDeNivel {

    public static final int AZUL = 0;
    public static final int VERDE = 1;
    public static final int AMARELO = 2;
    public static final int LARANJA = 3;
    public static final int VERMELHO = 4;

    /**
     * @param bytesPorSegundo velocidade atual medida
     * @param limiteMbps      limite configurado pelo usuario, em Mbps
     */
    public int calcularNivel(long bytesPorSegundo, long limiteMbps) {
        long limiteBps = limiteMbps * 1_000_000L;
        if (limiteBps <= 0) return AZUL;

        double porcentagem = (bytesPorSegundo * 8.0) / limiteBps;

        if (porcentagem >= 0.90) return VERMELHO;
        if (porcentagem >= 0.70) return LARANJA;
        if (porcentagem >= 0.40) return AMARELO;
        if (porcentagem >= 0.10) return VERDE;
        return AZUL;
    }
}
