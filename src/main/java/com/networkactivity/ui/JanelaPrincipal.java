package com.networkactivity.ui;

import com.networkactivity.config.ConfiguracoesApp;
import com.networkactivity.icones.GerenciadorDeIcones;
import com.networkactivity.rede.CalculadoraDeNivel;
import com.networkactivity.rede.MonitorDeRede;
import com.networkactivity.util.Formatador;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Janela principal do aplicativo: mostra o grafico de trafego em tempo real
 * e gerencia o icone piscante na bandeja do sistema.
 * Porte de MainForm.cs, adaptado ao modelo de bandeja do Java (java.awt.SystemTray).
 */
public class JanelaPrincipal extends JFrame {

    private final ConfiguracoesApp config = new ConfiguracoesApp();
    private final MonitorDeRede monitor = new MonitorDeRede();
    private final CalculadoraDeNivel calculadora = new CalculadoraDeNivel();
    private GerenciadorDeIcones icones;

    private TrayIcon trayIcon;
    private PainelDeGrafico.Serie serieDownload;
    private PainelDeGrafico.Serie serieUpload;
    private PainelDeGrafico grafico;

    private JLabel lblDownloadAtual;
    private JLabel lblUploadAtual;
    private JLabel lblTotalRecebido;
    private JLabel lblTotalEnviado;

    private javax.swing.Timer timerMonitoramento;

    private long bytesRecebidosAnterior = -1;
    private long bytesEnviadosAnterior = -1;
    private int contadorOcioso = 0;

    public JanelaPrincipal() {
        super("Indicador de Atividade de Rede");
        icones = new GerenciadorDeIcones();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(montarBarraSuperior(), BorderLayout.NORTH);
        add(montarPainelCentral(), BorderLayout.CENTER);
        add(montarPainelInferior(), BorderLayout.SOUTH);

        configurarFechamentoParaBandeja();
        configurarBandeja();
        iniciarMonitoramento();
    }

    // ---------------------------------------------------------------
    // Montagem da interface
    // ---------------------------------------------------------------

    private JPanel montarBarraSuperior() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        barra.setBackground(new Color(49, 106, 197)); // azul estilo XP "Luna"

        JButton btnConfig = criarBotaoBarra("Configuracoes");
        btnConfig.addActionListener(e -> abrirConfiguracoes());

        JButton btnAvancado = criarBotaoBarra("Avancado");
        btnAvancado.addActionListener(e -> new DialogoAvancado(this).setVisible(true));

        JButton btnSobre = criarBotaoBarra("Sobre");
        btnSobre.addActionListener(e -> new DialogoSobre(this).setVisible(true));

        barra.add(btnConfig);
        barra.add(btnAvancado);
        barra.add(btnSobre);
        return barra;
    }

    private JButton criarBotaoBarra(String texto) {
        JButton botao = new JButton(texto);
        botao.setFocusPainted(false);
        return botao;
    }

    private JPanel montarPainelCentral() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        grafico = new PainelDeGrafico();
        serieDownload = grafico.adicionarSerie("Download", "KB/s", new Color(0, 176, 80));
        serieUpload = grafico.adicionarSerie("Upload", "KB/s", new Color(0, 112, 219));

        painel.add(grafico, BorderLayout.CENTER);
        return painel;
    }

    private JPanel montarPainelInferior() {
        JPanel painel = new JPanel(new GridLayout(2, 2, 10, 4));
        painel.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        lblDownloadAtual = new JLabel("Download atual: 0 B/s");
        lblUploadAtual = new JLabel("Upload atual: 0 B/s");
        lblTotalRecebido = new JLabel("Total recebido: 0 B");
        lblTotalEnviado = new JLabel("Total enviado: 0 B");

        painel.add(lblDownloadAtual);
        painel.add(lblUploadAtual);
        painel.add(lblTotalRecebido);
        painel.add(lblTotalEnviado);
        return painel;
    }

    private void configurarFechamentoParaBandeja() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (SystemTray.isSupported()) {
                    setVisible(false);
                } else {
                    encerrarAplicativo();
                }
            }
        });
    }

    // ---------------------------------------------------------------
    // Bandeja do sistema
    // ---------------------------------------------------------------

    private void configurarBandeja() {
        if (!SystemTray.isSupported()) {
            System.err.println("Bandeja do sistema nao suportada nesta plataforma.");
            return;
        }

        PopupMenu menu = new PopupMenu();

        MenuItem itemAbrir = new MenuItem("Abrir janela principal");
        itemAbrir.addActionListener(e -> mostrarJanela());

        MenuItem itemConfig = new MenuItem("Configuracoes...");
        itemConfig.addActionListener(e -> abrirConfiguracoes());

        MenuItem itemAvancado = new MenuItem("Estatisticas avancadas...");
        itemAvancado.addActionListener(e -> new DialogoAvancado(this).setVisible(true));

        MenuItem itemSobre = new MenuItem("Sobre...");
        itemSobre.addActionListener(e -> new DialogoSobre(this).setVisible(true));

        MenuItem itemSair = new MenuItem("Sair");
        itemSair.addActionListener(e -> encerrarAplicativo());

        menu.add(itemAbrir);
        menu.addSeparator();
        menu.add(itemConfig);
        menu.add(itemAvancado);
        menu.addSeparator();
        menu.add(itemSobre);
        menu.addSeparator();
        menu.add(itemSair);

        Image iconeInicial = icones.getIconeInativo();
        trayIcon = new TrayIcon(iconeInicial, "Indicador de Atividade de Rede - ocioso", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> mostrarJanela()); // duplo clique

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Nao foi possivel adicionar o icone na bandeja: " + e.getMessage());
        }
    }

    private void mostrarJanela() {
        setVisible(true);
        setState(Frame.NORMAL);
        toFront();
    }

    private void abrirConfiguracoes() {
        DialogoConfiguracoes dialogo = new DialogoConfiguracoes(this, config, this::reiniciarMonitoramento);
        dialogo.setVisible(true);
    }

    private void encerrarAplicativo() {
        if (timerMonitoramento != null) timerMonitoramento.stop();
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        dispose();
        System.exit(0);
    }

    // ---------------------------------------------------------------
    // Monitoramento de trafego (equivalente ao Timer_Tick do MainForm.cs)
    // ---------------------------------------------------------------

    private void iniciarMonitoramento() {
        timerMonitoramento = new javax.swing.Timer(config.getIntervaloAtualizacaoMs(), e -> atualizarTrafego());
        timerMonitoramento.start();
    }

    private void reiniciarMonitoramento() {
        if (timerMonitoramento != null) {
            timerMonitoramento.stop();
        }
        iniciarMonitoramento();
    }

    private void atualizarTrafego() {
        List<String> interfacesHabilitadas = config.getInterfacesHabilitadas();
        long[] contadores = monitor.lerContadoresAgregados(interfacesHabilitadas);
        long bytesRecebidos = contadores[0];
        long bytesEnviados = contadores[1];

        if (bytesRecebidosAnterior < 0) {
            // primeira leitura: apenas guarda a referencia, sem calcular velocidade
            bytesRecebidosAnterior = bytesRecebidos;
            bytesEnviadosAnterior = bytesEnviados;
            return;
        }

        double intervaloSegundos = config.getIntervaloAtualizacaoMs() / 1000.0;
        long deltaRecebido = bytesRecebidos - bytesRecebidosAnterior;
        long deltaEnviado = bytesEnviados - bytesEnviadosAnterior;

        boolean recebendo = deltaRecebido > 0;
        boolean enviando = deltaEnviado > 0;

        double velocidadeDownload = recebendo ? Math.abs(deltaRecebido / intervaloSegundos) : 0;
        double velocidadeUpload = enviando ? Math.abs(deltaEnviado / intervaloSegundos) : 0;

        bytesRecebidosAnterior = bytesRecebidos;
        bytesEnviadosAnterior = bytesEnviados;

        grafico.registrarAmostra(velocidadeDownload / 1024.0, velocidadeUpload / 1024.0);

        lblDownloadAtual.setText("Download atual: " + Formatador.formatarVelocidade(velocidadeDownload));
        lblUploadAtual.setText("Upload atual: " + Formatador.formatarVelocidade(velocidadeUpload));
        lblTotalRecebido.setText("Total recebido: " + Formatador.formatarBytes(bytesRecebidos));
        lblTotalEnviado.setText("Total enviado: " + Formatador.formatarBytes(bytesEnviados));

        atualizarIconeBandeja(recebendo, enviando, velocidadeDownload, velocidadeUpload);
    }

    private void atualizarIconeBandeja(boolean recebendo, boolean enviando,
                                        double velocidadeDownload, double velocidadeUpload) {
        if (trayIcon == null) return;

        if (!recebendo && !enviando) {
            contadorOcioso++;
            if (contadorOcioso >= 5) {
                trayIcon.setImage(icones.getIconeInativo());
                trayIcon.setToolTip("Indicador de Atividade de Rede - ocioso");
            }
            return;
        }
        contadorOcioso = 0;

        if (config.isLimitesPersonalizados()) {
            int nivelDownload = calculadora.calcularNivel((long) velocidadeDownload, config.getBandaDownloadMbps());
            int nivelUpload = calculadora.calcularNivel((long) velocidadeUpload, config.getBandaUploadMbps());

            Image novoIcone;
            if (recebendo && enviando) {
                novoIcone = icones.getIconeAtivo(nivelUpload, nivelDownload);
            } else if (recebendo) {
                novoIcone = icones.getIconeDownload(nivelDownload);
            } else {
                novoIcone = icones.getIconeUpload(nivelUpload);
            }
            trayIcon.setImage(novoIcone);
        } else {
            trayIcon.setImage(icones.getIconeAtivo(CalculadoraDeNivel.AZUL, CalculadoraDeNivel.AZUL));
        }

        trayIcon.setToolTip(String.format(
                "Indicador de Atividade de Rede%n\u2193 %s   \u2191 %s",
                Formatador.formatarVelocidade(velocidadeDownload),
                Formatador.formatarVelocidade(velocidadeUpload)));
    }
}
