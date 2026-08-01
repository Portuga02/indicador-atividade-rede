package com.networkactivity.config;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Configuracoes persistentes do usuario. Equivale ao Properties.Settings.Default
 * do projeto original em C# (arquivo Settings.settings), porem gravado via
 * java.util.prefs.Preferences (funciona em Windows/Linux/Mac sem precisar de arquivo .config).
 */
public class ConfiguracoesApp {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ConfiguracoesApp.class);

    // ---- Chaves ----
    private static final String INICIAR_COM_WINDOWS = "iniciarComWindows";
    private static final String LIMITES_PERSONALIZADOS = "limitesPersonalizados";
    private static final String BANDA_DOWNLOAD_MBPS = "bandaDownloadMbps";
    private static final String BANDA_UPLOAD_MBPS = "bandaUploadMbps";
    private static final String CONJUNTO_ICONES = "conjuntoDeIcones";
    private static final String INTERFACES_HABILITADAS = "interfacesHabilitadasMac";
    private static final String PING_AUTOMATICO_ATIVO = "pingAutomaticoAtivo";
    private static final String PING_HOST = "pingHost";
    private static final String PING_INTERVALO_MS = "pingIntervaloMs";
    private static final String INTERVALO_ATUALIZACAO_MS = "intervaloAtualizacaoMs";

    // ---- Iniciar com o sistema ----
    public boolean isIniciarComWindows() {
        return PREFS.getBoolean(INICIAR_COM_WINDOWS, false);
    }

    public void setIniciarComWindows(boolean valor) {
        PREFS.putBoolean(INICIAR_COM_WINDOWS, valor);
    }

    // ---- Limites de banda personalizados (para colorir o icone) ----
    public boolean isLimitesPersonalizados() {
        return PREFS.getBoolean(LIMITES_PERSONALIZADOS, true);
    }

    public void setLimitesPersonalizados(boolean valor) {
        PREFS.putBoolean(LIMITES_PERSONALIZADOS, valor);
    }

    /** Limite de download configurado pelo usuario, em Mbps. */
    public long getBandaDownloadMbps() {
        return PREFS.getLong(BANDA_DOWNLOAD_MBPS, 100);
    }

    public void setBandaDownloadMbps(long valor) {
        PREFS.putLong(BANDA_DOWNLOAD_MBPS, valor);
    }

    /** Limite de upload configurado pelo usuario, em Mbps. */
    public long getBandaUploadMbps() {
        return PREFS.getLong(BANDA_UPLOAD_MBPS, 100);
    }

    public void setBandaUploadMbps(long valor) {
        PREFS.putLong(BANDA_UPLOAD_MBPS, valor);
    }

    // ---- Tema/conjunto de icones ----
    public String getConjuntoDeIcones() {
        return PREFS.get(CONJUNTO_ICONES, "xp-color");
    }

    public void setConjuntoDeIcones(String valor) {
        PREFS.put(CONJUNTO_ICONES, valor);
    }

    // ---- Interfaces de rede habilitadas (lista de MACs separada por ;) ----
    public List<String> getInterfacesHabilitadas() {
        String bruto = PREFS.get(INTERFACES_HABILITADAS, "");
        if (bruto.isBlank()) return List.of();
        return Arrays.stream(bruto.split(";")).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    public void setInterfacesHabilitadas(List<String> macs) {
        PREFS.put(INTERFACES_HABILITADAS, String.join(";", macs));
    }

    // ---- Ping automatico ----
    public boolean isPingAutomaticoAtivo() {
        return PREFS.getBoolean(PING_AUTOMATICO_ATIVO, false);
    }

    public void setPingAutomaticoAtivo(boolean valor) {
        PREFS.putBoolean(PING_AUTOMATICO_ATIVO, valor);
    }

    public String getPingHost() {
        return PREFS.get(PING_HOST, "google.com");
    }

    public void setPingHost(String valor) {
        PREFS.put(PING_HOST, valor);
    }

    public int getPingIntervaloMs() {
        return PREFS.getInt(PING_INTERVALO_MS, 5000);
    }

    public void setPingIntervaloMs(int valor) {
        PREFS.putInt(PING_INTERVALO_MS, valor);
    }

    // ---- Intervalo de atualizacao do monitor (equivalente ao "BlinkDuration" do app original) ----
    public int getIntervaloAtualizacaoMs() {
        return PREFS.getInt(INTERVALO_ATUALIZACAO_MS, 100);
    }

    public void setIntervaloAtualizacaoMs(int valor) {
        PREFS.putInt(INTERVALO_ATUALIZACAO_MS, valor);
    }
}
