package com.networkactivity.ui;

import com.networkactivity.config.ConfiguracoesApp;
import com.networkactivity.rede.InterfaceDeRede;
import com.networkactivity.rede.MonitorDeRede;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de configuracoes do programa (equivalente a SettingsForm.cs /
 * frmAdvanced parcial).
 */
public class DialogoConfiguracoes extends JDialog {

	private static final Color AZUL_BORDA = new Color(0, 60, 116);

	private final ConfiguracoesApp config;
	private final Runnable aoSalvar;

	private JCheckBox chkIniciarComWindows;
	private JCheckBox chkLimitesPersonalizados;
	private JSpinner spnBandaDownload;
	private JSpinner spnBandaUpload;
	private JComboBox<String> cmbConjuntoIcones;
	private JSpinner spnIntervaloAtualizacao;

	private JCheckBox chkPingAtivo;
	private JTextField txtPingHost;
	private JSpinner spnPingIntervalo;

	private final List<JCheckBox> checkboxesInterfaces = new ArrayList<>();
	private final List<InterfaceDeRede> interfacesDisponiveis;

	public DialogoConfiguracoes(Frame dono, ConfiguracoesApp config, MonitorDeRede monitor, Runnable aoSalvar) {
		super(dono, "Configuracoes", true);
		this.config = config;
		this.aoSalvar = aoSalvar;
		this.interfacesDisponiveis = monitor.listarInterfaces();

		setLayout(new BorderLayout());
		setSize(480, 470);
		setResizable(false);
		setLocationRelativeTo(dono);

		JTabbedPane abas = new JTabbedPane();
		abas.addTab("Geral", montarAbaGeral());
		abas.addTab("Interfaces de Rede", montarAbaInterfaces());
		abas.addTab("Ping Automatico", montarAbaPing());

		JPanel conteudo = new JPanel(new BorderLayout());
		conteudo.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		conteudo.add(abas, BorderLayout.CENTER);

		add(conteudo, BorderLayout.CENTER);
		add(montarBarraBotoes(), BorderLayout.SOUTH);

		// ESC fecha o dialogo (equivale a Cancelar)
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		carregarValoresAtuais();
	}

	// ---------------------------------------------------------------
	// Helpers visuais
	// ---------------------------------------------------------------

	/** Painel com borda tipo "GroupBox" do XP. */
	private JPanel criarGrupo(String titulo) {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(AZUL_BORDA), titulo),
				BorderFactory.createEmptyBorder(2, 6, 6, 6)));
		return p;
	}

	private void adicionarLinha(JPanel p, int linha, String rotulo, JComponent campo) {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = linha;

		c.gridx = 0;
		p.add(new JLabel(rotulo), c);

		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(campo, c);
	}

	private void adicionarLinhaUnica(JPanel p, int linha, JComponent comp) {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = linha;
		c.gridwidth = 2;
		c.weightx = 1;
		p.add(comp, c);
	}

	// ---------------------------------------------------------------
	// Abas
	// ---------------------------------------------------------------

	private JPanel montarAbaGeral() {
		JPanel painel = new JPanel(new GridBagLayout());
		painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Grupo: Inicializacao
		JPanel grupoInicio = criarGrupo("Inicializacao");
		chkIniciarComWindows = new JCheckBox("Iniciar automaticamente com o sistema");
		adicionarLinhaUnica(grupoInicio, 0, chkIniciarComWindows);

		// Grupo: Limites de banda
		JPanel grupoBanda = criarGrupo("Limites de banda");
		chkLimitesPersonalizados = new JCheckBox("Usar limites personalizados para colorir o icone");
		adicionarLinhaUnica(grupoBanda, 0, chkLimitesPersonalizados);
		spnBandaDownload = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 1));
		adicionarLinha(grupoBanda, 1, "Limite de download (Mbps):", spnBandaDownload);
		spnBandaUpload = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 1));
		adicionarLinha(grupoBanda, 2, "Limite de upload (Mbps):", spnBandaUpload);

		// Grupo: Aparencia
		JPanel grupoAparencia = criarGrupo("Aparencia e desempenho");
		cmbConjuntoIcones = new JComboBox<>(new String[] { "xp-color", "win8", "win10-color", "gnome-color" });
		adicionarLinha(grupoAparencia, 0, "Tema dos icones:", cmbConjuntoIcones);
		spnIntervaloAtualizacao = new JSpinner(new SpinnerNumberModel(100, 50, 10000, 50));
		adicionarLinha(grupoAparencia, 1, "Intervalo de atualizacao (ms):", spnIntervaloAtualizacao);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 8, 0);

		c.gridy = 0;
		painel.add(grupoInicio, c);
		c.gridy = 1;
		painel.add(grupoBanda, c);
		c.gridy = 2;
		painel.add(grupoAparencia, c);

		// espaco flexivel no fim, para empurrar os grupos para cima
		c.gridy = 3;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		painel.add(new JPanel(), c);

		return painel;
	}

	private JPanel montarAbaInterfaces() {
		JPanel painel = new JPanel(new BorderLayout(0, 8));
		painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel dica = new JLabel("Selecione as interfaces que devem ser contabilizadas:");
		painel.add(dica, BorderLayout.NORTH);

		JPanel lista = new JPanel();
		lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
		lista.setBackground(Color.WHITE);
		lista.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		if (interfacesDisponiveis.isEmpty()) {
			lista.add(new JLabel("Nenhuma interface de rede encontrada."));
		}

		for (InterfaceDeRede itf : interfacesDisponiveis) {
			String mac = itf.getMacAddress();
			String rotulo = itf.getNomeExibicao() + (mac.isEmpty() ? "" : "  (" + mac + ")");
			JCheckBox cb = new JCheckBox(rotulo);
			cb.setBackground(Color.WHITE);
			cb.putClientProperty("mac", mac);
			checkboxesInterfaces.add(cb);
			lista.add(cb);
		}

		JScrollPane rolagem = new JScrollPane(lista);
		rolagem.setBorder(BorderFactory.createLineBorder(AZUL_BORDA));
		painel.add(rolagem, BorderLayout.CENTER);

		JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		JButton marcarTodas = new JButton("Marcar todas");
		marcarTodas.addActionListener(e -> checkboxesInterfaces.forEach(cb -> cb.setSelected(true)));
		JButton desmarcarTodas = new JButton("Desmarcar todas");
		desmarcarTodas.addActionListener(e -> checkboxesInterfaces.forEach(cb -> cb.setSelected(false)));
		botoes.add(marcarTodas);
		botoes.add(desmarcarTodas);
		painel.add(botoes, BorderLayout.SOUTH);

		return painel;
	}

	private JPanel montarAbaPing() {
		JPanel painel = new JPanel(new GridBagLayout());
		painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel grupo = criarGrupo("Verificacao de conectividade");
		chkPingAtivo = new JCheckBox("Verificar conectividade periodicamente (ping)");
		adicionarLinhaUnica(grupo, 0, chkPingAtivo);

		txtPingHost = new JTextField(16);
		adicionarLinha(grupo, 1, "Servidor (host):", txtPingHost);

		spnPingIntervalo = new JSpinner(new SpinnerNumberModel(5000, 1000, 60000, 500));
		adicionarLinha(grupo, 2, "Intervalo (ms):", spnPingIntervalo);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		painel.add(grupo, c);

		c.gridy = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		painel.add(new JPanel(), c);

		return painel;
	}

	private JPanel montarBarraBotoes() {
		JPanel painel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
		JButton salvar = new JButton("Salvar");
		JButton cancelar = new JButton("Cancelar");

		salvar.addActionListener(e -> {
			salvarValores();
			dispose();
			if (aoSalvar != null)
				aoSalvar.run();
		});
		cancelar.addActionListener(e -> dispose());

		painel.add(salvar);
		painel.add(cancelar);

		// Enter aciona "Salvar"
		getRootPane().setDefaultButton(salvar);
		return painel;
	}

	// ---------------------------------------------------------------
	// Carregar / salvar
	// ---------------------------------------------------------------

	private void carregarValoresAtuais() {
		chkIniciarComWindows.setSelected(config.isIniciarComWindows());
		chkLimitesPersonalizados.setSelected(config.isLimitesPersonalizados());
		spnBandaDownload.setValue((int) config.getBandaDownloadMbps());
		spnBandaUpload.setValue((int) config.getBandaUploadMbps());
		cmbConjuntoIcones.setSelectedItem(config.getConjuntoDeIcones());
		spnIntervaloAtualizacao.setValue(config.getIntervaloAtualizacaoMs());

		chkPingAtivo.setSelected(config.isPingAutomaticoAtivo());
		txtPingHost.setText(config.getPingHost());
		spnPingIntervalo.setValue(config.getPingIntervaloMs());

		List<String> habilitadas = config.getInterfacesHabilitadas();
		for (JCheckBox cb : checkboxesInterfaces) {
			String mac = (String) cb.getClientProperty("mac");
			cb.setSelected(habilitadas.isEmpty() || habilitadas.contains(mac));
		}
	}

	private void salvarValores() {
		config.setIniciarComWindows(chkIniciarComWindows.isSelected());
		config.setLimitesPersonalizados(chkLimitesPersonalizados.isSelected());
		config.setBandaDownloadMbps(((Number) spnBandaDownload.getValue()).longValue());
		config.setBandaUploadMbps(((Number) spnBandaUpload.getValue()).longValue());
		config.setConjuntoDeIcones((String) cmbConjuntoIcones.getSelectedItem());
		config.setIntervaloAtualizacaoMs((Integer) spnIntervaloAtualizacao.getValue());

		config.setPingAutomaticoAtivo(chkPingAtivo.isSelected());
		config.setPingHost(txtPingHost.getText().trim());
		config.setPingIntervaloMs((Integer) spnPingIntervalo.getValue());

		List<String> selecionadas = new ArrayList<>();
		for (JCheckBox cb : checkboxesInterfaces) {
			if (cb.isSelected()) {
				selecionadas.add((String) cb.getClientProperty("mac"));
			}
		}
		config.setInterfacesHabilitadas(selecionadas);
	}
}