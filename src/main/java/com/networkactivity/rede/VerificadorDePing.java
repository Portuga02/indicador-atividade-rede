package com.networkactivity.rede;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifica a conectividade executando o comando "ping" do sistema operacional
 * em segundo plano, a cada intervalo configurado, e avisa o resultado por
 * callback (que sera chamado em uma thread separada, NAO na thread da
 * interface grafica).
 */
public class VerificadorDePing {

	/** Resultado de uma verificacao de ping. */
	public static final class Resultado {
		private final String host;
		private final boolean sucesso;
		private final long latenciaMs;

		public Resultado(String host, boolean sucesso, long latenciaMs) {
			this.host = host;
			this.sucesso = sucesso;
			this.latenciaMs = latenciaMs;
		}

		public String getHost() {
			return host;
		}

		public boolean isSucesso() {
			return sucesso;
		}

		/** Latencia em ms (valor sem significado quando sucesso == false). */
		public long getLatenciaMs() {
			return latenciaMs;
		}
	}

	private static final String SO = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
	private static final boolean WINDOWS = SO.contains("win");
	private static final boolean MAC = SO.contains("mac");

	private static final int TEMPO_LIMITE_PING_MS = 2000;

	// Aceita "time=12ms", "tempo=12ms", "tempo<1ms", "time=12.3 ms", etc.
	private static final Pattern PADRAO_TEMPO = Pattern.compile(
			"(?:time|tempo|zeit|tiempo)\\s*([=<])\\s*([0-9]+(?:[.,][0-9]+)?)\\s*ms", Pattern.CASE_INSENSITIVE);
	private static final Pattern PADRAO_HOST_VALIDO = Pattern.compile("[A-Za-z0-9._:\\-]+");

	private ScheduledExecutorService executor;
	private volatile long geracaoAtual = 0;

	/** Inicia (ou reinicia) as verificacoes periodicas. */
	public synchronized void iniciar(String host, int intervaloMs, Consumer<Resultado> aoReceber) {
		parar();
		final long geracao = ++geracaoAtual;

		executor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "verificador-ping");
			t.setDaemon(true);
			return t;
		});

		executor.scheduleWithFixedDelay(() -> {
			try {
				Resultado resultado = executarPing(host);
				// descarta resultado de uma configuracao antiga
				if (geracao == geracaoAtual && aoReceber != null) {
					aoReceber.accept(resultado);
				}
			} catch (Throwable ignorado) {
				// nunca deixa a tarefa agendada morrer
			}
		}, 0, Math.max(500, intervaloMs), TimeUnit.MILLISECONDS);
	}

	/** Para as verificacoes. */
	public synchronized void parar() {
		geracaoAtual++;
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
	}

	/** Executa um unico ping (bloqueante, ate ~2-5 segundos). */
	public static Resultado executarPing(String host) {
		if (host == null || host.isBlank() || host.startsWith("-") || !PADRAO_HOST_VALIDO.matcher(host).matches()) {
			return new Resultado(host, false, -1);
		}

		List<String> comando = new ArrayList<>();
		comando.add("ping");
		if (WINDOWS) {
			comando.add("-n");
			comando.add("1");
			comando.add("-w");
			comando.add(String.valueOf(TEMPO_LIMITE_PING_MS));
		} else if (MAC) {
			comando.add("-c");
			comando.add("1");
			comando.add("-W");
			comando.add(String.valueOf(TEMPO_LIMITE_PING_MS)); // no macOS e em ms
		} else {
			comando.add("-c");
			comando.add("1");
			comando.add("-W");
			comando.add(String.valueOf(TEMPO_LIMITE_PING_MS / 1000)); // no Linux e em segundos
		}
		comando.add(host);

		Process processo = null;
		try {
			processo = new ProcessBuilder(comando).redirectErrorStream(true).start();

			StringBuilder saida = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(processo.getInputStream(), Charset.defaultCharset()))) {
				String linha;
				while ((linha = br.readLine()) != null) {
					saida.append(linha).append('\n');
				}
			}
			processo.waitFor(5, TimeUnit.SECONDS);

			Matcher m = PADRAO_TEMPO.matcher(saida);
			if (m.find()) {
				if ("<".equals(m.group(1))) {
					return new Resultado(host, true, 1);
				}
				double valor = Double.parseDouble(m.group(2).replace(',', '.'));
				return new Resultado(host, true, Math.max(1, Math.round(valor)));
			}

			// Sem "tempo=" legivel (idioma diferente), mas com TTL = houve resposta
			if (saida.toString().toUpperCase(Locale.ROOT).contains("TTL=")) {
				return new Resultado(host, true, 0);
			}
			return new Resultado(host, false, -1);

		} catch (Exception e) {
			return new Resultado(host, false, -1);
		} finally {
			if (processo != null && processo.isAlive()) {
				processo.destroyForcibly();
			}
		}
	}
}