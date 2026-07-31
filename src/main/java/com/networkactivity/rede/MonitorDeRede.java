package com.networkactivity.rede;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Le as estatisticas reais de trafego das interfaces de rede da maquina, sem
 * depender de bibliotecas externas (100% Java + comandos nativos do SO).
 * <p>
 * Equivale ao uso de System.Net.NetworkInformation.NetworkInterface.GetIPStatistics()
 * no projeto original em C#. O Java puro nao expoe contadores de bytes, entao
 * lemos:
 * - Linux: /proc/net/dev
 * - Windows: PowerShell (Get-NetAdapterStatistics)
 * - macOS: netstat -ib
 */
public class MonitorDeRede {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final boolean MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    /** Retorna a lista de interfaces de rede disponiveis na maquina, com seus contadores atuais. */
    public List<InterfaceDeRede> listarInterfaces() {
        Map<String, long[]> contadoresPorNome = lerContadoresPorInterface();
        List<InterfaceDeRede> resultado = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> todas = NetworkInterface.getNetworkInterfaces();
            while (todas.hasMoreElements()) {
                NetworkInterface nif = todas.nextElement();

                String mac = formatarMac(nif);
                boolean possuiIp = nif.getInterfaceAddresses() != null && !nif.getInterfaceAddresses().isEmpty();

                long[] contadores = contadoresPorNome.getOrDefault(nif.getName(), new long[]{0, 0});
                // No Windows o nome amigavel pode nao bater exatamente; tenta pelo displayName tambem
                if (contadores[0] == 0 && contadores[1] == 0) {
                    contadores = contadoresPorNome.getOrDefault(nif.getDisplayName(), contadores);
                }

                resultado.add(new InterfaceDeRede(
                        nif.getName(),
                        nif.getDisplayName() != null ? nif.getDisplayName() : nif.getName(),
                        mac,
                        contadores[0],
                        contadores[1],
                        possuiIp
                ));
            }
        } catch (SocketException e) {
            System.err.println("Falha ao listar interfaces de rede: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Soma os bytes recebidos/enviados de um conjunto de interfaces (identificadas por MAC).
     * Se a lista estiver vazia, soma todas as interfaces.
     */
    public long[] lerContadoresAgregados(List<String> macsHabilitados) {
        long totalRecebido = 0;
        long totalEnviado = 0;

        for (InterfaceDeRede itf : listarInterfaces()) {
            if (macsHabilitados.isEmpty() || macsHabilitados.contains(itf.getMacAddress())) {
                totalRecebido += itf.getBytesRecebidos();
                totalEnviado += itf.getBytesEnviados();
            }
        }

        return new long[]{totalRecebido, totalEnviado};
    }

    private String formatarMac(NetworkInterface nif) {
        try {
            byte[] bytes = nif.getHardwareAddress();
            if (bytes == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02X", bytes[i]));
                if (i < bytes.length - 1) sb.append(":");
            }
            return sb.toString();
        } catch (SocketException e) {
            return "";
        }
    }

    // ---------------------------------------------------------------
    // Leitura de contadores especifica de cada sistema operacional
    // ---------------------------------------------------------------

    private Map<String, long[]> lerContadoresPorInterface() {
        try {
            if (WINDOWS) return lerContadoresWindows();
            if (MAC) return lerContadoresMac();
            return lerContadoresLinux();
        } catch (Exception e) {
            System.err.println("Falha ao ler contadores de rede: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** Le /proc/net/dev (formato: "  iface: rxBytes ... txBytes ..."). */
    private Map<String, long[]> lerContadoresLinux() throws IOException {
        Map<String, long[]> resultado = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream("/proc/net/dev"), StandardCharsets.UTF_8))) {
            String linha;
            int numeroLinha = 0;
            while ((linha = br.readLine()) != null) {
                numeroLinha++;
                if (numeroLinha <= 2) continue; // pula cabecalho (2 linhas)
                int idxDoisPontos = linha.indexOf(':');
                if (idxDoisPontos < 0) continue;
                String nome = linha.substring(0, idxDoisPontos).trim();
                String[] campos = linha.substring(idxDoisPontos + 1).trim().split("\\s+");
                if (campos.length < 9) continue;
                long rx = Long.parseLong(campos[0]);
                long tx = Long.parseLong(campos[8]);
                resultado.put(nome, new long[]{rx, tx});
            }
        }
        return resultado;
    }

    /** Executa Get-NetAdapterStatistics via PowerShell e devolve bytes recebidos/enviados por adaptador. */
    private Map<String, long[]> lerContadoresWindows() throws IOException, InterruptedException {
        Map<String, long[]> resultado = new HashMap<>();
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                "Get-NetAdapterStatistics | ForEach-Object { \"$($_.Name)|$($_.ReceivedBytes)|$($_.SentBytes)\" }"
        );
        pb.redirectErrorStream(true);
        Process processo = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split("\\|");
                if (partes.length != 3) continue;
                try {
                    resultado.put(partes[0].trim(), new long[]{Long.parseLong(partes[1].trim()), Long.parseLong(partes[2].trim())});
                } catch (NumberFormatException ignorado) {
                    // linha de log/erro do PowerShell, ignora
                }
            }
        }
        processo.waitFor();
        return resultado;
    }

    /** Executa netstat -ib no macOS e agrega as colunas Ibytes/Obytes por interface. */
    private Map<String, long[]> lerContadoresMac() throws IOException, InterruptedException {
        Map<String, long[]> resultado = new HashMap<>();
        ProcessBuilder pb = new ProcessBuilder("netstat", "-ib");
        pb.redirectErrorStream(true);
        Process processo = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha = br.readLine(); // cabecalho
            while ((linha = br.readLine()) != null) {
                String[] campos = linha.trim().split("\\s+");
                if (campos.length < 10) continue;
                String nome = campos[0];
                try {
                    long rx = Long.parseLong(campos[6]);
                    long tx = Long.parseLong(campos[9]);
                    resultado.merge(nome, new long[]{rx, tx}, (a, b) -> new long[]{Math.max(a[0], b[0]), Math.max(a[1], b[1])});
                } catch (NumberFormatException ignorado) {
                    // linha em formato inesperado, ignora
                }
            }
        }
        processo.waitFor();
        return resultado;
    }
}
