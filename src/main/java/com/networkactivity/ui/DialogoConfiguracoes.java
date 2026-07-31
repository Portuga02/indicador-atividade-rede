package com.networkactivity.ui;

import com.networkactivity.config.ConfiguracoesApp;
import com.networkactivity.rede.InterfaceDeRede;
import com.networkactivity.rede.MonitorDeRede;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Tela de configuracoes do programa (equivalente a SettingsForm.cs / frmAdvanced parcial). */
public class DialogoConfiguracoes extends JDialog {

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

    public DialogoConfiguracoes(Frame dono, ConfiguracoesApp config, Runnable aoSalvar) {
        super(dono, "Configuracoes", true);
        this.config = config;
        this.aoSalvar = aoSalvar;
        this.interfacesDisponiveis = new MonitorDeRede().listarInterfaces();

        setLayout(new BorderLayout());
        setSize(460, 420);
        setLocationRelativeTo(dono);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Geral", montarAbaGeral());
        abas.addTab("Interfaces de Rede", montarAbaInterfaces());
        abas.addTab("Ping Automatico", montarAbaPing());

        add(abas, BorderLayout.CENTER);
        add(montarBarraBotoes(), BorderLayout.SOUTH);

        carregarValoresAtuais();
    }

    private JPanel montarAbaGeral() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;

        chkIniciarComWindows = new JCheckBox("Iniciar automaticamente com o sistema");
        painel.add(chkIniciarComWindows, c);

        c.gridy++;
        chkLimitesPersonalizados = new JCheckBox("Usar limites de banda personalizados para colorir o icone");
        painel.add(chkLimitesPersonalizados, c);

        c.gridwidth = 1;
        c.gridy++;
        painel.add(new JLabel("Limite de download (Mbps):"), c);
        c.gridx = 1;
        spnBandaDownload = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 1));
        painel.add(spnBandaDownload, c);

        c.gridx = 0;
        c.gridy++;
        painel.add(new JLabel("Limite de upload (Mbps):"), c);
        c.gridx = 1;
        spnBandaUpload = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 1));
        painel.add(spnBandaUpload, c);

        c.gridx = 0;
        c.gridy++;
        painel.add(new JLabel("Tema dos icones:"), c);
        c.gridx = 1;
        cmbConjuntoIcones = new JComboBox<>(new String[]{"xp-color", "win8", "win10-color", "gnome-color"});
        painel.add(cmbConjuntoIcones, c);

        c.gridx = 0;
        c.gridy++;
        painel.add(new JLabel("Intervalo de atualizacao (ms):"), c);
        c.gridx = 1;
        spnIntervaloAtualizacao = new JSpinner(new SpinnerNumberModel(1000, 250, 10000, 250));
        painel.add(spnIntervaloAtualizacao, c);

        return painel;
    }

    private JPanel montarAbaInterfaces() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel dica = new JLabel("Selecione as interfaces que devem ser contabilizadas:");
        painel.add(dica, BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));

        if (interfacesDisponiveis.isEmpty()) {
            lista.add(new JLabel("Nenhuma interface de rede encontrada."));
        }

        for (InterfaceDeRede itf : interfacesDisponiveis) {
            String rotulo = itf.getNomeExibicao() + "  (" + itf.getMacAddress() + ")";
            JCheckBox cb = new JCheckBox(rotulo);
            cb.putClientProperty("mac", itf.getMacAddress());
            checkboxesInterfaces.add(cb);
            lista.add(cb);
        }

        painel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return painel;
    }

    private JPanel montarAbaPing() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;

        chkPingAtivo = new JCheckBox("Verificar conectividade periodicamente (ping)");
        painel.add(chkPingAtivo, c);

        c.gridwidth = 1;
        c.gridy++;
        painel.add(new JLabel("Servidor (host):"), c);
        c.gridx = 1;
        txtPingHost = new JTextField(16);
        painel.add(txtPingHost, c);

        c.gridx = 0;
        c.gridy++;
        painel.add(new JLabel("Intervalo (ms):"), c);
        c.gridx = 1;
        spnPingIntervalo = new JSpinner(new SpinnerNumberModel(5000, 1000, 60000, 500));
        painel.add(spnPingIntervalo, c);

        return painel;
    }

    private JPanel montarBarraBotoes() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton salvar = new JButton("Salvar");
        JButton cancelar = new JButton("Cancelar");

        salvar.addActionListener(e -> {
            salvarValores();
            dispose();
            if (aoSalvar != null) aoSalvar.run();
        });
        cancelar.addActionListener(e -> dispose());

        painel.add(salvar);
        painel.add(cancelar);
        return painel;
    }

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
