# 🖥️ Indicador de Atividade de Rede

> Monitor de tráfego de rede com ícone piscante na bandeja do sistema, no estilo clássico do Windows XP.

Porte para **Java (Swing)** do projeto original **Network Activity Indicator**
(C#/.NET, fork do *GabNetStats* por GabSoftware), com toda a interface
traduzida para português e rodando de forma multiplataforma (Windows, Linux
e macOS).

---

## ✨ Funcionalidades

- 🟢🟡🟠🔴 Ícone na bandeja do sistema que muda de cor conforme a velocidade da conexão
- 📈 Gráfico de tráfego de rede em tempo real (download/upload)
- ⚙️ Tela de configurações: limites de banda personalizados, tema de ícones, interfaces habilitadas
- 📊 Tela de estatísticas avançadas por interface de rede
- 🌐 Leitura de tráfego real da máquina, **sem nenhuma biblioteca externa** em produção
- 🇧🇷 Interface 100% em português

## 🧱 Estrutura do projeto

```
IndicadorDeAtividadeDeRede/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/networkactivity/
    │   │   ├── Main.java
    │   │   ├── config/    → ConfiguracoesApp.java
    │   │   ├── icones/    → GerenciadorDeIcones.java
    │   │   ├── rede/      → MonitorDeRede, InterfaceDeRede, CalculadoraDeNivel
    │   │   ├── ui/        → JanelaPrincipal, DialogoConfiguracoes, DialogoAvancado, DialogoSobre, PainelDeGrafico
    │   │   └── util/      → Formatador.java
    │   └── resources/icons/   → ícones do tema xp-color (.png)
    └── test/
        └── java/com/networkactivity/   → testes JUnit 5 (mesma estrutura de pacotes)
```

| Original (C#/.NET) | Java |
|---|---|
| `MainForm.cs` | `JanelaPrincipal.java` |
| `GabTracker.cs` (gráfico) | `PainelDeGrafico.java` |
| `SettingsForm.cs` | `DialogoConfiguracoes.java` |
| `frmAdvanced.cs` | `DialogoAvancado.java` *(versão simplificada)* |
| `AboutForm.cs` | `DialogoSobre.java` |
| `Properties.Settings.Default` | `ConfiguracoesApp.java` |
| `NetworkInterface.GetIPStatistics()` | `MonitorDeRede.java` |
| Ícone na bandeja (`NotifyIcon`) | `java.awt.TrayIcon` |

## 🚀 Como rodar

Requer **Java 17+** ([adoptium.net](https://adoptium.net) se não tiver).

**Pelo Eclipse/IntelliJ:** importe como projeto Maven existente e rode
`Main.java` (pacote `com.networkactivity`) com **Run As → Java Application**.

**Pelo terminal, com Maven:**

```bash
mvn package
java -jar target/IndicadorDeAtividadeDeRede.jar
```

> 💡 Prefira sempre `mvn package` para gerar o `.jar` final — é mais confiável
> que o "Export → Runnable JAR" da IDE, porque não depende de nenhuma
> configuração interna do projeto (source folders etc.), só da estrutura
> real de pastas.

O programa minimiza direto pra bandeja do sistema. Dê **duplo-clique no
ícone** para abrir a janela principal com o gráfico de tráfego.

## 🧪 Como rodar os testes

O projeto usa **JUnit 5 (Jupiter)**. Para rodar todos os testes:

```bash
mvn test
```

Isso compila e executa os testes em `src/test/java`, e mostra um resumo no
terminal:

```
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

Pelo Eclipse/IntelliJ: clique com botão direito na pasta `src/test/java`
(ou em uma classe de teste específica) → **Run As → JUnit Test**.

### O que está coberto

| Classe testada | O que os testes garantem |
|---|---|
| `CalculadoraDeNivelTest` | Os limiares de cor (azul/verde/amarelo/laranja/vermelho) batem com a porcentagem de uso da banda |
| `FormatadorTest` | Velocidades e tamanhos formatam certo em português (vírgula decimal, unidades B/KB/MB/GB) |
| `GerenciadorDeIconesTest` | Todos os 40 ícones do tema carregam do classpath sem faltar nenhum |
| `InterfaceDeRedeTest` | Os dados de uma interface de rede são armazenados e recuperados corretamente |

> `MonitorDeRede` (leitura real de rede via PowerShell/`/proc/net/dev`/`netstat`)
> não tem teste automatizado, pois depende do sistema operacional e do
> hardware da máquina — é validado manualmente rodando o programa.

## ⚠️ Observações importantes

1. **Leitura de tráfego por sistema operacional** (sem bibliotecas externas):
   - **Windows**: processo único e contínuo de PowerShell (`Get-NetAdapterStatistics`), com fallback automático para `netstat -e` se o PowerShell não responder
   - **Linux**: leitura direta de `/proc/net/dev`
   - **macOS**: via `netstat -ib`

2. **Tela "Avançado"** é uma versão simplificada do `frmAdvanced.cs` original,
   que dependia de APIs bem específicas do Windows (tabelas de conexão
   TCP/UDP por processo). A versão Java mostra uma tabela com todas as
   interfaces de rede, MAC, status de IP e totais de tráfego.

3. **"Iniciar com o Windows"** existe na tela de Configurações, mas ainda não
   mexe de fato no registro/pasta de inicialização do Windows — por enquanto
   é só um checkbox salvo nas preferências.

## 🙏 Créditos

- **GabSoftware**, pelo aplicativo original *GabNetStats*
- **boroda74**, pelo fork *Network Activity Indicator* e pelo tema visual do Windows XP
