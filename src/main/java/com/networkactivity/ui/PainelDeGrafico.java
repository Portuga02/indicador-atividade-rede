package com.networkactivity.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Grafico de trafego de rede em tempo real, desenhado a mao com Graphics2D.
 * Porte do componente customizado GabTracker.cs (grade + linhas preenchidas + legenda).
 */
public class PainelDeGrafico extends JComponent {

    /** Uma serie de dados do grafico (ex: "Download" ou "Upload"). */
    public static class Serie {
        public final String legenda;
        public final String unidade;
        public final Color cor;
        public double valorAtual;
        final LinkedList<Double> historico = new LinkedList<>();

        public Serie(String legenda, String unidade, Color cor) {
            this.legenda = legenda;
            this.unidade = unidade;
            this.cor = cor;
        }
    }

    private final List<Serie> series = new ArrayList<>();
    private int maxAmostrasNaMemoria = 60;
    private boolean maximoAutomatico = true;
    private double maximoAutoPercentual = 110.0;
    private double maximo = 100.0;

    private final Color corGrade = new Color(35, 35, 35);
    private final Color corGradeGrossa = new Color(70, 70, 70);
    private final Color corFundo = Color.BLACK;
    private final Color corTexto = Color.WHITE;

    public PainelDeGrafico() {
        setDoubleBuffered(true);
        setBackground(corFundo);
        setPreferredSize(new Dimension(400, 220));
    }

    public Serie adicionarSerie(String legenda, String unidade, Color cor) {
        Serie s = new Serie(legenda, unidade, cor);
        series.add(s);
        return s;
    }

    /** Adiciona uma nova amostra a cada serie (na mesma ordem em que foram criadas) e redesenha. */
    public void registrarAmostra(double... valores) {
        for (int i = 0; i < series.size() && i < valores.length; i++) {
            Serie s = series.get(i);
            s.valorAtual = valores[i];
            s.historico.addLast(valores[i]);
            while (s.historico.size() > maxAmostrasNaMemoria) {
                s.historico.removeFirst();
            }
        }

        if (maximoAutomatico) {
            double max = 1;
            for (Serie s : series) {
                for (double v : s.historico) {
                    if (v > max) max = v;
                }
            }
            maximo = max * (maximoAutoPercentual / 100.0);
            if (maximo < 1) maximo = 1;
        }

        repaint();
    }

    public void setMaxAmostrasNaMemoria(int max) {
        this.maxAmostrasNaMemoria = max;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g.setColor(corFundo);
        g.fillRect(0, 0, w, h);

        // grade fina
        g.setColor(corGrade);
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x < w; x += 20) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 20) g.drawLine(0, y, w, y);

        // grade grossa
        g.setColor(corGradeGrossa);
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x < w; x += 100) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 100) g.drawLine(0, y, w, y);

        for (Serie s : series) {
            desenharSerie(g, s, w, h);
        }

        desenharLegenda(g);

        g.setColor(new Color(80, 80, 80));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(0, 0, w - 1, h - 1);

        g.dispose();
    }

    private void desenharSerie(Graphics2D g, Serie s, int w, int h) {
        if (s.historico.size() < 2) return;

        int n = s.historico.size();
        float[] xs = new float[n];
        float[] ys = new float[n];

        int i = 0;
        for (double v : s.historico) {
            float xx = (float) i / (maxAmostrasNaMemoria - 1) * w;
            float yy = (float) (h - (v / maximo * h));
            if (yy < 0) yy = 0;
            if (yy > h) yy = h;
            xs[i] = xx;
            ys[i] = yy;
            i++;
        }

        // area preenchida abaixo da linha
        GeneralPath poligono = new GeneralPath();
        poligono.moveTo(xs[0], h);
        for (int k = 0; k < n; k++) poligono.lineTo(xs[k], ys[k]);
        poligono.lineTo(xs[n - 1], h);
        poligono.closePath();

        g.setColor(new Color(s.cor.getRed(), s.cor.getGreen(), s.cor.getBlue(), 70));
        g.fill(poligono);

        // linha
        GeneralPath linha = new GeneralPath();
        linha.moveTo(xs[0], ys[0]);
        for (int k = 1; k < n; k++) linha.lineTo(xs[k], ys[k]);

        g.setColor(s.cor);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linha);
    }

    private void desenharLegenda(Graphics2D g) {
        int legendaY = 8;
        g.setFont(getFont() != null ? getFont() : new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        for (Serie s : series) {
            g.setColor(s.cor);
            g.fillRect(8, legendaY + 4, 12, 12);

            String txt = s.legenda;
            if (s.unidade != null && !s.unidade.isEmpty()) {
                txt += " : " + String.format("%.2f", s.valorAtual) + " " + s.unidade;
            }

            g.setColor(new Color(corTexto.getRed(), corTexto.getGreen(), corTexto.getBlue(), 230));
            g.drawString(txt, 28, legendaY + 14);

            legendaY += 20;
        }
    }
}
