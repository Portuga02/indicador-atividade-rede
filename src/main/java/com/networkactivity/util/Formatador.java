package com.networkactivity.util;

import java.util.Locale;

/** Utilitarios de formatacao de velocidade e tamanho, em portugues. */
public final class Formatador {

    private Formatador() {}

    /** Formata bytes/segundo em algo legivel, ex: "1,4 MB/s". */
    public static String formatarVelocidade(double bytesPorSegundo) {
        String[] unidades = {"B/s", "KB/s", "MB/s", "GB/s"};
        double valor = bytesPorSegundo;
        int i = 0;
        while (valor >= 1024 && i < unidades.length - 1) {
            valor /= 1024;
            i++;
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f %s", valor, unidades[i]);
    }

    /** Formata um total de bytes, ex: "2,3 GB". */
    public static String formatarBytes(long bytes) {
        String[] unidades = {"B", "KB", "MB", "GB", "TB"};
        double valor = bytes;
        int i = 0;
        while (valor >= 1024 && i < unidades.length - 1) {
            valor /= 1024;
            i++;
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.2f %s", valor, unidades[i]);
    }
}
