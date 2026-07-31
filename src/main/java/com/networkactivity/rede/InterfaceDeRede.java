package com.networkactivity.rede;

/** Representa uma interface de rede da maquina e seus contadores acumulados. */
public class InterfaceDeRede {

    private final String nomeTecnico;
    private final String nomeExibicao;
    private final String macAddress;
    private final long bytesRecebidos;
    private final long bytesEnviados;
    private final boolean possuiIp;

    public InterfaceDeRede(String nomeTecnico, String nomeExibicao, String macAddress,
                            long bytesRecebidos, long bytesEnviados, boolean possuiIp) {
        this.nomeTecnico = nomeTecnico;
        this.nomeExibicao = nomeExibicao;
        this.macAddress = macAddress;
        this.bytesRecebidos = bytesRecebidos;
        this.bytesEnviados = bytesEnviados;
        this.possuiIp = possuiIp;
    }

    public String getNomeTecnico() { return nomeTecnico; }
    public String getNomeExibicao() { return nomeExibicao; }
    public String getMacAddress() { return macAddress; }
    public long getBytesRecebidos() { return bytesRecebidos; }
    public long getBytesEnviados() { return bytesEnviados; }
    public boolean isPossuiIp() { return possuiIp; }

    @Override
    public String toString() {
        return nomeExibicao;
    }
}
