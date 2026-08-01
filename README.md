# Indicador de Atividade de Rede (edição Java)

Porte para Java (Swing) do seu **Network Activity Indicator** (originalmente em
C#/.NET, fork do GabNetStats), com a interface toda traduzida para português.

Projeto no formato padrão Maven — importa direto no Eclipse/IntelliJ sem
configuração extra.

## Estrutura do projeto

```
IndicadorDeAtividadeDeRede/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/networkactivity/
    │   ├── Main.java
    │   ├── config/          -> ConfiguracoesApp.java
    │   ├── icones/          -> GerenciadorDeIcones.java
    │   ├── rede/             -> MonitorDeRede.java, InterfaceDeRede.java, CalculadoraDeNivel.java
    │   ├── ui/               -> JanelaPrincipal, DialogoConfiguracoes, DialogoAvancado, DialogoSobre, PainelDeGrafico
    │   └── util/             -> Formatador.java
    └── resources/icons/      -> ícones do tema xp-color (.png)
```

## Como importar no Eclipse

**File → Import → Maven → Existing Maven Projects** → aponte para a pasta
`IndicadorDeAtividadeDeRede` (a que tem o `pom.xml` direto dentro). O Eclipse
reconhece a estrutura automaticamente — pacotes, pasta de recursos, tudo.

Como o projeto não usa nenhuma biblioteca externa, não precisa baixar nada da
internet para importar ou compilar.

## Como rodar

Direto no Eclipse: ache `Main.java` (pacote `com.networkactivity`) → botão
direito → **Run As → Java Application**.

Ou pelo terminal, com Maven instalado:

```
mvn package
java -jar target/IndicadorDeAtividadeDeRede.jar
```

Ou sem Maven, direto com `javac`:

```
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
cp -r src/main/resources/icons out/icons
cd out
jar cfe ../IndicadorDeAtividadeDeRede.jar com.networkactivity.Main com icons
cd ..
java -jar IndicadorDeAtividadeDeRede.jar
```

O programa minimiza direto pra bandeja do sistema (perto do relógio, como o
original). Dê **duplo-clique no ícone da bandeja** pra abrir a janela
principal com o gráfico de tráfego.

## O que foi portado

| Original (C#/.NET) | Java |
|---|---|
| `MainForm.cs` | `JanelaPrincipal.java` |
| `GabTracker.cs` (gráfico) | `PainelDeGrafico.java` |
| `SettingsForm.cs` | `DialogoConfiguracoes.java` |
| `frmAdvanced.cs` | `DialogoAvancado.java` (versão simplificada) |
| `AboutForm.cs` | `DialogoSobre.java` |
| `Properties.Settings.Default` | `ConfiguracoesApp.java` |
| `NetworkInterface.GetIPStatistics()` | `MonitorDeRede.java` |
| Ícone na bandeja (`NotifyIcon`) | `java.awt.TrayIcon` |

## Observações importantes

1. **Leitura de tráfego por sistema operacional**: o Java não tem uma API
   pronta pra contar bytes de rede como o .NET tem. Implementei a leitura
   sem bibliotecas externas:
   - **Windows**: via PowerShell (`Get-NetAdapterStatistics`)
   - **Linux**: lendo `/proc/net/dev`
   - **macOS**: via `netstat -ib`

2. **Tela "Avançado"** foi simplificada. O `frmAdvanced.cs` original usava
   APIs bem específicas do Windows (tabelas de conexão TCP/UDP por
   processo) que não têm equivalente direto e portável em Java. A versão
   Java mostra uma tabela com todas as interfaces de rede, MAC, status de
   IP e totais de tráfego — atualizada automaticamente a cada 3 segundos.

3. **Ícones**: usei o tema `xp-color` (os 40 ícones originais, convertidos
   de `.ico` para `.png`). Dá pra trocar de tema na tela de Configurações
   (`win8`, `win10-color`, `gnome-color`), mas só o `xp-color` já vem
   empacotado no `.jar` — os outros temas precisam ser adicionados na pasta
   `icons/` dentro do código-fonte se quiser usá-los.

4. **"Iniciar com o Windows"**: a opção existe na tela de Configurações,
   mas ainda não mexe de fato no registro do Windows — isso normalmente é
   feito criando um atalho na pasta de Inicialização
   (`shell:startup`) ou uma chave em
   `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`. Posso implementar
   isso se quiser.
