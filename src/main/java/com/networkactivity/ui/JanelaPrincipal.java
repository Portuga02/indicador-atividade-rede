package com.networkactivity.ui;

import com.networkactivity.config.ConfiguracoesApp;
import com.networkactivity.icones.GerenciadorDeIcones;
import com.networkactivity.icones.GerenciadorDeIcones.StatusConexao;
import com.networkactivity.rede.CalculadoraDeNivel;
import com.networkactivity.rede.InterfaceDeRede;
import com.networkactivity.rede.MonitorDeRede;
import com.networkactivity.rede.VerificadorDePing;
import com.networkactivity.util.Formatador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Janela principal do aplicativo: mostra o grafico de trafego em tempo real e
 * gerencia o icone piscante na bandeja do sistema. Porte de MainForm.cs,
 * adaptado ao modelo de bandeja do Java (java.awt.SystemTray).
 */
public class JanelaPrincipal extends JFrame {

	private static final Color AZUL_CLARO = new Color(58, 128, 232);
	private static final Color AZUL_ESCURO = new Color(37, 90, 190);
	private static final Color AZUL_BORDA = new Color(0, 60, 116);
	private static final Color BEGE_XP = new Color(236, 233, 216);
	private static final Color BEGE_HOVER = new Color(255, 240, 207);
	private static final Color COR_DOWNLOAD = new Color(0, 176, 80);
	private static final Color COR_UPLOAD = new Color(0, 112, 219);

	/** Acima desta latencia o status do ping passa de OK para ALERTA. */
	private static final long LIMITE_ALERTA_PING_MS = 150;
	/** Quantas falhas seguidas sao necessarias para avisar "conexao perdida". */
	private static final int FALHAS_PARA_AVISAR = 2;

	private final ConfiguracoesApp config = new ConfiguracoesApp();
	private final MonitorDeRede monitor = new MonitorDeRede();
	private final CalculadoraDeNivel calculadora = new CalculadoraDeNivel();
	private final VerificadorDePing verificadorPing = new VerificadorDePing();
	private GerenciadorDeIcones icones;

	private TrayIcon trayIcon;
	private PainelDeGrafico.Serie serieDownload;
	private PainelDeGrafico.Serie serieUpload;
	private PainelDeGrafico grafico;

	private JLabel lblDownloadAtual;
	private JLabel lblUploadAtual;
	private JLabel lblTotalRecebido;
	private JLabel lblTotalEnviado;

	// barra de status
	private JLabel lblStatusPing;
	private JLabel lblAdaptador;
	private final Map<StatusConexao, Icon> cacheIconesStatus = new EnumMap<>(StatusConexao.class);

	private javax.swing.Timer timerMonitoramento;
	private javax.swing.Timer timerAdaptador;

	private long bytesRecebidosAnterior = -1;
	private long bytesEnviadosAnterior = -1;
	private int contadorOcioso = 0;

	// estado do ping / avisos
	private int falhasConsecutivas = 0;
	private boolean conexaoCaida = false;

	// adaptador em uso
	private final Map<String, Long> totaisAnterioresAdaptador = new HashMap<>();
	private String adaptadorAtual = "-";

	public JanelaPrincipal() {
		super("Indicador de Atividade de Rede");
		icones = new GerenciadorDeIcones();

		setIconImage(icones.getIconeInativo());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(540, 470);
		setMinimumSize(new Dimension(500, 420));
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		getContentPane().setBackground(BEGE_XP);

		add(montarBarraSuperior(), BorderLayout.NORTH);
		add(montarPainelCentral(), BorderLayout.CENTER);
		add(montarRodape(), BorderLayout.SOUTH);

		configurarFechamentoParaBandeja();
		configurarBandeja();
		iniciarMonitoramento();
		iniciarPing();
		iniciarAtualizacaoDoAdaptador();
	}

	// ---------------------------------------------------------------
	// Montagem da interface
	// ---------------------------------------------------------------

	private JPanel montarBarraSuperior() {
		JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7)) {
			@Override
			protected void paintComponent(Graphics g0) {
				Graphics2D g = (Graphics2D) g0.create();
				g.setPaint(new GradientPaint(0, 0, AZUL_CLARO, 0, getHeight(), AZUL_ESCURO));
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(new Color(255, 255, 255, 90)); // brilho na borda de cima
				g.drawLine(0, 0, getWidth(), 0);
				g.setColor(AZUL_BORDA); // linha de separacao embaixo
				g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
				g.dispose();
			}
		};
		barra.setOpaque(false);

		JButton btnConfig = criarBotaoBarra("Configuracoes");
		btnConfig.addActionListener(e -> abrirConfiguracoes());

		JButton btnAvancado = criarBotaoBarra("Avancado");
		btnAvancado.addActionListener(e -> new DialogoAvancado(this, monitor).setVisible(true));

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
		botao.setFont(new Font("Tahoma", Font.BOLD, 11));
		botao.setForeground(Color.BLACK);
		botao.setBackground(BEGE_XP);
		botao.setContentAreaFilled(false);
		botao.setOpaque(true);
		botao.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AZUL_BORDA, 1, true),
				BorderFactory.createEmptyBorder(4, 14, 4, 14)));
		botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		botao.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				botao.setBackground(BEGE_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				botao.setBackground(BEGE_XP);
			}
		});
		return botao;
	}

	private JPanel montarPainelCentral() {
		JPanel painel = new JPanel(new BorderLayout());
		painel.setBackground(BEGE_XP);
		painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));

		grafico = new PainelDeGrafico();
		grafico.setMaxAmostrasNaMemoria(300); // 300 amostras x 100ms = 30 segundos de historico
		grafico.setBorder(BorderFactory.createLineBorder(AZUL_BORDA));
		serieDownload = grafico.adicionarSerie("Download", "KB/s", COR_DOWNLOAD);
		serieUpload = grafico.adicionarSerie("Upload", "KB/s", COR_UPLOAD);

		painel.add(grafico, BorderLayout.CENTER);
		return painel;
	}

	/** Rodape = painel de estatisticas + barra de status (ping / adaptador). */
	private JPanel montarRodape() {
		JPanel rodape = new JPanel(new BorderLayout());
		rodape.setBackground(BEGE_XP);
		rodape.add(montarPainelInferior(), BorderLayout.CENTER);
		rodape.add(montarBarraStatus(), BorderLayout.SOUTH);
		return rodape;
	}

	private JPanel montarPainelInferior() {
		JPanel externo = new JPanel(new BorderLayout());
		externo.setBackground(BEGE_XP);
		externo.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));

		JPanel painel = new JPanel(new GridLayout(2, 2, 14, 6));
		painel.setBackground(BEGE_XP);
		painel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(AZUL_BORDA), "Estatisticas"),
				BorderFactory.createEmptyBorder(4, 8, 6, 8)));

		lblDownloadAtual = new JLabel("Download atual: 0 B/s");
		lblUploadAtual = new JLabel("Upload atual: 0 B/s");
		lblTotalRecebido = new JLabel("Total recebido: 0 B");
		lblTotalEnviado = new JLabel("Total enviado: 0 B");

		Font negrito = new Font("Tahoma", Font.BOLD, 12);
		lblDownloadAtual.setFont(negrito);
		lblDownloadAtual.setForeground(new Color(0, 140, 60));
		lblUploadAtual.setFont(negrito);
		lblUploadAtual.setForeground(new Color(0, 90, 190));

		painel.add(lblDownloadAtual);
		painel.add(lblUploadAtual);
		painel.add(lblTotalRecebido);
		painel.add(lblTotalEnviado);

		externo.add(painel, BorderLayout.CENTER);
		return externo;
	}

	/**
	 * Barra de status no estilo XP: circulo + ping a esquerda, adaptador a direita.
	 */
	private JPanel montarBarraStatus() {
		JPanel barra = new JPanel(new BorderLayout(12, 0));
		barra.setBackground(BEGE_XP);
		barra.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)),
				BorderFactory.createEmptyBorder(5, 12, 5, 12)));

		lblStatusPing = new JLabel("Ping: desativado");
		lblStatusPing.setIcon(iconeStatus(StatusConexao.DESCONHECIDO));
		lblStatusPing.setIconTextGap(6);

		lblAdaptador = new JLabel("Adaptador: -");
		lblAdaptador.setHorizontalAlignment(SwingConstants.RIGHT);
		lblAdaptador.setForeground(new Color(70, 70, 70));

		barra.add(lblStatusPing, BorderLayout.WEST);
		barra.add(lblAdaptador, BorderLayout.CENTER);
		return barra;
	}

	/** Circulo colorido (12x12) correspondente ao status, com cache. */
	private Icon iconeStatus(StatusConexao status) {
		return cacheIconesStatus.computeIfAbsent(status, s -> {
			Image img = icones.getIconeCirculoStatus(s);
			return img == null ? null : new ImageIcon(img.getScaledInstance(12, 12, Image.SCALE_SMOOTH));
		});
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
		itemAvancado.addActionListener(e -> new DialogoAvancado(this, monitor).setVisible(true));

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
		DialogoConfiguracoes dialogo = new DialogoConfiguracoes(this, config, monitor, this::reiniciarMonitoramento);
		dialogo.setVisible(true);
	}

	private void encerrarAplicativo() {
		if (timerMonitoramento != null)
			timerMonitoramento.stop();
		if (timerAdaptador != null)
			timerAdaptador.stop();
		verificadorPing.parar();
		if (trayIcon != null && SystemTray.isSupported()) {
			SystemTray.getSystemTray().remove(trayIcon);
		}
		dispose();
		System.exit(0);
	}

	/** Balao de notificacao nativo ao lado do icone da bandeja. */
	private void notificar(String titulo, String mensagem, TrayIcon.MessageType tipo) {
		if (trayIcon != null) {
			trayIcon.displayMessage(titulo, mensagem, tipo);
		}
	}

	// ---------------------------------------------------------------
	// Ping automatico
	// ---------------------------------------------------------------

	private void iniciarPing() {
		verificadorPing.parar();
		falhasConsecutivas = 0;
		conexaoCaida = false;

		String host = config.getPingHost();
		if (!config.isPingAutomaticoAtivo() || host == null || host.isBlank()) {
			atualizarStatusPing(StatusConexao.DESCONHECIDO, "Ping: desativado");
			return;
		}

		atualizarStatusPing(StatusConexao.DESCONHECIDO, "Ping: verificando " + host + "...");
		verificadorPing.iniciar(host, config.getPingIntervaloMs(),
				resultado -> SwingUtilities.invokeLater(() -> tratarResultadoPing(resultado)));
	}

	private void tratarResultadoPing(VerificadorDePing.Resultado r) {
		if (!r.isSucesso()) {
			falhasConsecutivas++;
			atualizarStatusPing(StatusConexao.FALHA, "Ping: sem resposta (" + r.getHost() + ")");

			if (falhasConsecutivas >= FALHAS_PARA_AVISAR && !conexaoCaida) {
				conexaoCaida = true;
				notificar("Conexao perdida", "Sem resposta de " + r.getHost() + ".", TrayIcon.MessageType.ERROR);
			}
			return;
		}

		falhasConsecutivas = 0;
		StatusConexao status = r.getLatenciaMs() >= LIMITE_ALERTA_PING_MS ? StatusConexao.ALERTA : StatusConexao.OK;
		String texto = r.getLatenciaMs() > 0 ? "Ping: " + r.getLatenciaMs() + " ms (" + r.getHost() + ")"
				: "Ping: OK (" + r.getHost() + ")";
		atualizarStatusPing(status, texto);

		if (conexaoCaida) {
			conexaoCaida = false;
			notificar("Conexao restabelecida", "A conexao com " + r.getHost() + " voltou.", TrayIcon.MessageType.INFO);
		}
	}

	private void atualizarStatusPing(StatusConexao status, String texto) {
		lblStatusPing.setIcon(iconeStatus(status));
		lblStatusPing.setText(texto);
	}

	// ---------------------------------------------------------------
	// Adaptador em uso (o que mais trafegou desde a ultima verificacao)
	// ---------------------------------------------------------------

	private void iniciarAtualizacaoDoAdaptador() {
		timerAdaptador = new javax.swing.Timer(3000, e -> atualizarAdaptador());
		timerAdaptador.start();
	}

	private void atualizarAdaptador() {
		List<String> habilitadas = config.getInterfacesHabilitadas();
		String maisAtivo = null;
		long maiorDelta = 0;

		for (InterfaceDeRede itf : monitor.listarInterfaces()) {
			if (!habilitadas.isEmpty() && !habilitadas.contains(itf.getMacAddress())) {
				continue;
			}
			long total = itf.getBytesRecebidos() + itf.getBytesEnviados();
			Long anterior = totaisAnterioresAdaptador.put(itf.getNomeTecnico(), total);
			if (anterior != null) {
				long delta = total - anterior;
				if (delta > maiorDelta) {
					maiorDelta = delta;
					maisAtivo = itf.getNomeExibicao();
				}
			}
		}

		if (maisAtivo != null) {
			adaptadorAtual = maisAtivo;
		}
		lblAdaptador.setText("Adaptador: " + abreviar(adaptadorAtual, 38));
		lblAdaptador.setToolTipText(adaptadorAtual);
	}

	private String abreviar(String texto, int max) {
		if (texto == null)
			return "-";
		return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
	}

	// ---------------------------------------------------------------
	// Monitoramento de trafego (equivalente ao Timer_Tick do MainForm.cs)
	// ---------------------------------------------------------------

	private void iniciarMonitoramento() {
		timerMonitoramento = new javax.swing.Timer(config.getIntervaloAtualizacaoMs(), e -> atualizarTrafego());
		timerMonitoramento.start();
	}

	/**
	 * Chamado ao salvar as configuracoes: reaplica intervalo de atualizacao e ping.
	 */
	private void reiniciarMonitoramento() {
		if (timerMonitoramento != null) {
			timerMonitoramento.stop();
		}
		iniciarMonitoramento();
		iniciarPing();
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

	private void atualizarIconeBandeja(boolean recebendo, boolean enviando, double velocidadeDownload,
			double velocidadeUpload) {
		if (trayIcon == null)
			return;

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

		trayIcon.setToolTip(String.format("Indicador de Atividade de Rede%n\u2193 %s   \u2191 %s",
				Formatador.formatarVelocidade(velocidadeDownload), Formatador.formatarVelocidade(velocidadeUpload)));
	}
}