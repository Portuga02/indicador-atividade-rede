package com.networkactivity.rede;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Le as estatisticas reais de trafego das interfaces de rede da maquina, sem
 * depender de bibliotecas externas (100% Java + comandos nativos do SO).
 * <p>
 * No Windows, tentamos primeiro manter um UNICO processo do PowerShell rodando
 * em loop continuo (mais detalhado: por interface). Se isso nao conseguir
 * entregar nenhuma leitura em alguns segundos (por exemplo, politica de
 * execucao bloqueada, antivirus, PowerShell ausente, etc.), o programa troca
 * SOZINHO para um metodo mais simples e universal: "netstat -e", nativo do
 * Windows, sem depender do PowerShell (perde a quebra por interface, mas
 * garante que o programa nao fique mudo).
 */
public class MonitorDeRede {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final boolean MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private static final int INTERVALO_LOOP_POWERSHELL_MS = 100;
    private static final long TEMPO_LIMITE_ANTES_DO_FALLBACK_MS = 4000;

    private static final String SCRIPT_POWERSHELL =
            "$ErrorActionPreference='SilentlyContinue';" +
            "$macMap = @{};" +
            "$contador = 0;" +
            "while($true){" +
            "  if ($contador % 50 -eq 0) {" +
            "    $macMap = @{};" +
            "    Get-NetAdapter | ForEach-Object { $macMap[$_.Name] = $_.MacAddress };" +
            "  }" +
            "  $contador++;" +
            "  Write-Output 'BEGIN';" +
            "  Get-NetAdapterStatistics | ForEach-Object {" +
            "    $mac = if ($macMap.ContainsKey($_.Name)) { $macMap[$_.Name] } else { '' };" +
            "    Write-Output ('{0}|{1}|{2}|{3}' -f $_.Name,$mac,$_.ReceivedBytes,$_.SentBytes)" +
            "  }" +
            "  Write-Output 'END';" +
            "  Start-Sleep -Milliseconds " + INTERVALO_LOOP_POWERSHELL_MS + ";" +
            "}";

    private final AtomicReference<List<InterfaceDeRede>> ultimaLeituraWindows = new AtomicReference<>(List.of());
    private final long inicioMs = System.currentTimeMillis();
    private volatile boolean powerShellFuncionou = false;
    private volatile boolean usarFallbackNetstat = false;

    public MonitorDeRede() {
        if (WINDOWS) {
            System.out.println("[MonitorDeRede] Iniciando leitura de rede via PowerShell...");
            iniciarLeituraContinuaWindows();
        }
    }

    /** Retorna a lista de interfaces de rede disponiveis na maquina, com seus contadores atuais. */
    public List<InterfaceDeRede> listarInterfaces() {
        if (WINDOWS) {
            verificarSeDevePassarParaFallback();
            if (usarFallbackNetstat) {
                return listarInterfacesFallbackNetstat();
            }
            return ultimaLeituraWindows.get();
        }
        return listarInterfacesSobDemanda();
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

    private void verificarSeDevePassarParaFallback() {
        if (usarFallbackNetstat || powerShellFuncionou) return;
        long decorrido = System.currentTimeMillis() - inicioMs;
        if (decorrido > TEMPO_LIMITE_ANTES_DO_FALLBACK_MS) {
            System.out.println("[MonitorDeRede] PowerShell nao respondeu em " + TEMPO_LIMITE_ANTES_DO_FALLBACK_MS
                    + "ms. Trocando para o modo simples (netstat -e), sem detalhe por interface.");
            usarFallbackNetstat = true;
        }
    }

    // ---------------------------------------------------------------
    // Windows: processo unico e continuo do PowerShell (modo detalhado)
    // ---------------------------------------------------------------

    private void iniciarLeituraContinuaWindows() {
        Thread thread = new Thread(this::executarLoopPowerShell, "monitor-rede-powershell");
        thread.setDaemon(true);
        thread.start();
    }

    private void executarLoopPowerShell() {
        while (!usarFallbackNetstat) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                        "-ExecutionPolicy", "Bypass", "-Command", SCRIPT_POWERSHELL
                );
                pb.redirectErrorStream(true);
                Process processo = pb.start();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {

                    List<InterfaceDeRede> blocoAtual = new ArrayList<>();
                    String linha;
                    boolean primeiraLeitura = true;
                    while ((linha = br.readLine()) != null) {
                        if (linha.equals("BEGIN")) {
                            blocoAtual = new ArrayList<>();
                            continue;
                        }
                        if (linha.equals("END")) {
                            ultimaLeituraWindows.set(blocoAtual);
                            if (!powerShellFuncionou) {
                                powerShellFuncionou = true;
                                System.out.println("[MonitorDeRede] PowerShell OK - " + blocoAtual.size()
                                        + " interface(s) encontrada(s): " + blocoAtual);
                            }
                            continue;
                        }
                        String[] campos = linha.split("\\|", -1);
                        if (campos.length != 4) {
                            if (primeiraLeitura) {
                                System.out.println("[MonitorDeRede] Linha inesperada do PowerShell (ignorada): " + linha);
                            }
                            continue;
                        }
                        primeiraLeitura = false;

                        String nome = campos[0];
                        String mac = campos[1].replace('-', ':').toUpperCase(Locale.ROOT);
                        long rx = parseLongSeguro(campos[2]);
                        long tx = parseLongSeguro(campos[3]);

                        blocoAtual.add(new InterfaceDeRede(nome, nome, mac, rx, tx, true));
                    }
                }
                processo.waitFor();
                if (!powerShellFuncionou) {
                    System.out.println("[MonitorDeRede] O processo do PowerShell encerrou sem entregar nenhum dado.");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("[MonitorDeRede] Falha ao rodar o PowerShell (" + e.getMessage()
                        + "). Vai tentar de novo em instantes, ou cair no modo simples se persistir.");
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignorado) {
                return;
            }
        }
    }

    private long parseLongSeguro(String texto) {
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ---------------------------------------------------------------
    // Windows: modo simples de emergencia (netstat -e, sem PowerShell)
    // ---------------------------------------------------------------

    /** Devolve uma unica "interface virtual" com o total agregado de toda a maquina. */
    private List<InterfaceDeRede> listarInterfacesFallbackNetstat() {
        long[] contadores = lerContadoresNetstatWindows();
        return List.of(new InterfaceDeRede("todas", "Todas as interfaces (modo simples)", "", contadores[0], contadores[1], true));
    }

    private long[] lerContadoresNetstatWindows() {
        try {
            Process processo = new ProcessBuilder("netstat.exe", "-e").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    String semEspacos = linha.trim();
                    if (semEspacos.toLowerCase(Locale.ROOT).startsWith("bytes")) {
                        String[] partes = semEspacos.split("\\s+");
                        if (partes.length >= 3) {
                            long rx = parseLongSeguro(partes[1]);
                            long tx = parseLongSeguro(partes[2]);
                            processo.waitFor();
                            return new long[]{rx, tx};
                        }
                    }
                }
            }
            processo.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("[MonitorDeRede] Falha tambem no modo simples (netstat -e): " + e.getMessage());
        }
        return new long[]{0, 0};
    }

    // ---------------------------------------------------------------
    // Linux / macOS: leitura sob demanda (rapida o suficiente sem cache)
    // ---------------------------------------------------------------

    private List<InterfaceDeRede> listarInterfacesSobDemanda() {
        Map<String, long[]> contadoresPorNome = lerContadoresPorInterface();
        List<InterfaceDeRede> resultado = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> todas = NetworkInterface.getNetworkInterfaces();
            while (todas.hasMoreElements()) {
                NetworkInterface nif = todas.nextElement();

                String mac = formatarMac(nif);
                boolean possuiIp = nif.getInterfaceAddresses() != null && !nif.getInterfaceAddresses().isEmpty();

                long[] contadores = contadoresPorNome.getOrDefault(nif.getName(), new long[]{0, 0});
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

    private Map<String, long[]> lerContadoresPorInterface() {
        try {
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