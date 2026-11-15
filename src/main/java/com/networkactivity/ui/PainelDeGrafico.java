package com.networkactivity.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class PainelDeGrafico extends JComponent {

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

	private final Color corGrade = new Color(0, 50, 0);
	private final Color corGradeGrossa = new Color(0, 90, 0);
	private final Color corFundo = Color.BLACK;
	private final Color corTexto = Color.WHITE;
	private final Color corEscala = new Color(150, 200, 150);

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
					if (v > max)
						max = v;
				}
			}
			maximo = max * (maximoAutoPercentual / 100.0);
			if (maximo < 1)
				maximo = 1;
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
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();

		g.setColor(corFundo);
		g.fillRect(0, 0, w, h);

		g.setColor(corGrade);
		g.setStroke(new BasicStroke(1f));
		for (int x = 0; x < w; x += 20)
			g.drawLine(x, 0, x, h);
		for (int y = 0; y < h; y += 20)
			g.drawLine(0, y, w, y);

		g.setColor(corGradeGrossa);
		g.setStroke(new BasicStroke(2f));
		for (int x = 0; x < w; x += 100)
			g.drawLine(x, 0, x, h);
		for (int y = 0; y < h; y += 100)
			g.drawLine(0, y, w, y);

		for (Serie s : series) {
			desenharSerie(g, s, w, h);
		}

		desenharEscala(g, w, h);
		desenharLegenda(g);

		g.setColor(new Color(80, 80, 80));
		g.setStroke(new BasicStroke(1f));
		g.drawRect(0, 0, w - 1, h - 1);

		g.dispose();
	}

	private void desenharSerie(Graphics2D g, Serie s, int w, int h) {
		if (s.historico.size() < 2)
			return;

		int n = s.historico.size();
		float[] xs = new float[n];
		float[] ys = new float[n];

		int i = 0;
		for (double v : s.historico) {
			float xx = (float) i / (maxAmostrasNaMemoria - 1) * w;
			float yy = (float) (h - (v / maximo * h));
			if (yy < 0)
				yy = 0;
			if (yy > h)
				yy = h;
			xs[i] = xx;
			ys[i] = yy;
			i++;
		}
		GeneralPath poligono = new GeneralPath();
		poligono.moveTo(xs[0], h);
		for (int k = 0; k < n; k++)
			poligono.lineTo(xs[k], ys[k]);
		poligono.lineTo(xs[n - 1], h);
		poligono.closePath();

		g.setColor(new Color(s.cor.getRed(), s.cor.getGreen(), s.cor.getBlue(), 70));
		g.fill(poligono);

		GeneralPath linha = new GeneralPath();
		linha.moveTo(xs[0], ys[0]);
		for (int k = 1; k < n; k++)
			linha.lineTo(xs[k], ys[k]);

		g.setColor(s.cor);
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(linha);
	}

	private void desenharEscala(Graphics2D g, int w, int h) {
		g.setFont(new Font("Tahoma", Font.PLAIN, 10));
		FontMetrics fm = g.getFontMetrics();
		g.setColor(corEscala);

		String topo = formatarVelocidadeKB(maximo, true);
		String meio = formatarVelocidadeKB(maximo / 2, true);

		g.drawString(topo, w - fm.stringWidth(topo) - 6, 14);
		g.drawString(meio, w - fm.stringWidth(meio) - 6, h / 2 - 4);
		g.drawString("0", w - fm.stringWidth("0") - 6, h - 5);
	}

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	static String formatarVelocidadeKB(double kb, boolean mostrarEquivalenteEmKB) {
		double valor = kb;
		String unidade = "KB/s";
		if (kb >= 1024.0 * 1024.0) {
			valor = kb / (1024.0 * 1024.0);
			unidade = "GB/s";
		} else if (kb >= 1024.0) {
			valor = kb / 1024.0;
			unidade = "MB/s";
		}

		String texto = String.format(PT_BR, "%.2f %s", valor, unidade);
		if (!"KB/s".equals(unidade) && mostrarEquivalenteEmKB) {
			texto += String.format(PT_BR, " (%,.0f KB/s)", kb);
		}
		return texto;
	}

	private void desenharLegenda(Graphics2D g) {
		Font fonte = new Font("Tahoma", Font.PLAIN, 12);
		g.setFont(fonte);
		FontMetrics fm = g.getFontMetrics();

		List<String> textos = new ArrayList<>();
		int larguraMax = 0;
		for (Serie s : series) {
			String txt = s.legenda;
			if (s.unidade != null && !s.unidade.isEmpty()) {
				if ("KB/s".equals(s.unidade)) {
					txt += " : " + formatarVelocidadeKB(s.valorAtual, true);
				} else {
					txt += " : " + String.format("%.2f", s.valorAtual) + " " + s.unidade;
				}
			}
			textos.add(txt);
			larguraMax = Math.max(larguraMax, fm.stringWidth(txt));
		}
		int larguraCaixa = larguraMax + 20 + 12 + 12;
		int alturaCaixa = series.size() * 20 + 8;
		g.setColor(new Color(0, 0, 0, 160));
		g.fillRoundRect(4, 4, larguraCaixa, alturaCaixa, 8, 8);

		int legendaY = 8;
		int i = 0;
		for (Serie s : series) {
			g.setColor(s.cor);
			g.fillRect(10, legendaY + 4, 12, 12);

			g.setColor(new Color(corTexto.getRed(), corTexto.getGreen(), corTexto.getBlue(), 230));
			g.drawString(textos.get(i), 30, legendaY + 14);

			legendaY += 20;
			i++;
		}
	}
}