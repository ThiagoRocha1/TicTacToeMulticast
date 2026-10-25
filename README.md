# TicTacToeMulticast

Projeto acadêmico de **Sistemas Distribuídos**: jogo da velha distribuído para dois jogadores, com pareamento por **Java RMI**, comunicação direta por **sockets TCP** (e, em uma fase posterior, UDP) e publicação das jogadas para um ou mais **displays multicast**.

> **Estado atual (25/10/2026):** base do servidor de match importada para este workspace por pedido do responsável, com pareamento em `MatchServer.getOponent`. Mantidas as decisões anteriores desta pasta: utilitário JSON próprio, timeout de match de **300 s**, prazo de partida de **120 s**, compilação direta e testes manuais. As políticas de match foram aprovadas posteriormente pelo responsável; partida TCP aprovada e implementada posteriormente. Publicação e Display multicast implementados na etapa seguinte; validação integrada nas VMs permanece pendente.

## Documentação para pessoas e agentes

- [`AGENTS.md`](AGENTS.md): regras de trabalho para agentes neste repositório; **leitura obrigatória antes de qualquer alteração**.
- [`PLANO_DO_PROJETO.md`](PLANO_DO_PROJETO.md): fonte de continuidade, decisões aprovadas, pendências, etapas e status dos testes manuais.
- **Enunciado:** `trab-2-rmi-multi-2026-1.pdf`, do Prof. Alexandre Sztajnberg, quatro páginas, incluindo a **Figura 1 (Arquitetura)**. O arquivo foi fornecido na conversa; sua presença efetiva no repositório deve ser verificada.

O PDF é a fonte primária dos **requisitos do professor**; decisões explícitas do responsável registradas no plano complementam os pontos que o PDF deixa em aberto. Não confundir escolhas do projeto com exigências textuais do enunciado. Embora o cabeçalho do PDF diga `2023/1`, o responsável **confirmou que ele continua válido para 2026/1**.

## O que o sistema fará

| Programa | Responsabilidade | Comunicação |
| --- | --- | --- |
| **Servidor de match** | Recebe pedidos de partida, mantém chamadas pendentes, pareia **dois jogadores com o mesmo protocolo** e retorna os dados do adversário para **ambos**. | RMI |
| **Jogador** | Solicita um adversário, joga **uma partida** de jogo da velha, troca mensagens diretamente com o oponente e publica cada jogada aos displays. | RMI + TCP (UDP futuramente) + multicast |
| **Display** | Recebe jogadas pelo grupo multicast e apresenta o tabuleiro em texto; podem existir vários displays. | Multicast |

**Fluxo de alto nível, conforme o PDF:** os jogadores chamam `getOponent` no servidor RMI; o servidor finaliza as duas solicitações quando encontra um par; `inicia=true` joga primeiro e `inicia=false` aguarda; as jogadas vão diretamente entre os jogadores por socket; as jogadas são publicadas aos displays por multicast. O servidor de match **não precisa retransmitir cada jogada**.

## Ambiente e parâmetros definidos

| Parâmetro | Valor |
| --- | --- |
| Linguagem / JDK nas VMs | Java 17 (`17.0.20.1` informado) |
| Número de conta (`NN`) | `21` |
| Protocolo da primeira implementação | `TCP` |
| Protocolo previsto para uma segunda fase | `UDP` |
| VM escolhida para o servidor de match | `152.92.236.14` |
| Porta do RMIRegistry (`98NN`) | `9821` |
| Endereço multicast (`225.1.1.NN`) | `225.1.1.21` |
| Porta multicast (`88NN`) | `8821` |
| Outras VMs do ambiente | `152.92.236.16` e `152.92.236.17` |

As portas de escuta dos jogadores ainda não foram fixadas. Primeiro serão feitos testes em `localhost`; depois, nas três VMs Linux. Para RMI, considerar explicitamente o IP anunciado pelo servidor (`java.rmi.server.hostname`). Multicast entre VMs requer verificação da rede, grupo, interface e TTL.

## Contratos e escolhas já acordados

### 1. Servidor de match (RMI)

A interface remota tem um método chamado `getOponent`, recebendo e retornando **`String` contendo JSON**. Por pedido posterior do responsável, esta assinatura substitui a versão anterior com byte[]. O RMI faz o marshalling/unmarshalling dos argumentos e resultados; `JsonUtil` e `JsonMessages` continuam responsáveis por criar, interpretar e validar o JSON. Não há conversão manual entre texto e bytes no match.

**Assinatura implementada na base importada:**

```java
String getOponent(String jsonIn) throws RemoteException;
```

Nome planejado para a interface: `MatchService` (estendendo `java.rmi.Remote`). A requisição e a resposta mantêm os campos definidos pelo PDF:

```json
{"ip":"152.92.236.16","porta":5000,"protocolo":"tcp"}
```

```json
{"ip":"152.92.236.17","porta":5001,"inicia":true}
```

Endereços e portas dos exemplos são **ilustrativos**. O servidor considerará os protocolos `tcp` e `udp` no pareamento, embora **o jogador UDP seja implementado apenas depois da versão TCP**.


### Contrato de match aprovado em 25/10/2026

Estas são decisões do responsável para o sistema, complementares ao enunciado; não atribuir ao professor os códigos de erro ou as políticas abaixo.

- Sucesso: JSON textual em `String` com `ip` (texto), `porta` (inteiro 1..65535) e `inicia` (booleano). Parear por ordem de registro na fila de cada protocolo; primeiro recebe true, segundo false. Isso não define quem usa X/O na futura partida.
- Erro de aplicação: JSON com `erro` e `mensagem`, ambos strings. `mensagem` explica a causa em português; o cliente decide pela string `erro`, sem depender do texto da mensagem.
- Códigos: `INVALID_REQUEST` para entrada inválida (não entra na fila), `DUPLICATE` para registro pendente repetido, `TIMEOUT` para falta de par no prazo e `INTERRUPTED` para interrupção da espera no servidor quando possível retornar ao cliente.
- Duplicidade: comparar IP numérico normalizado, porta e protocolo de pedidos pendentes. Rejeitar a nova chamada, preservando o pedido original e seu prazo. Um endpoint TCP e um UDP são registros distintos e nunca formam um par entre si.
- Timeout: prazo padrão de **300 s**, contado a partir do recebimento do pedido válido; remover o pedido ao expirar e retornar TIMEOUT. Um pedido novo após receber TIMEOUT inicia outro prazo. Não prometer entrega se o cliente estiver desconectado.
- Desistência: encerrar o cliente **não cancela imediatamente** o pedido no servidor. A limpeza ocorre pelo timeout, salvo se houver pareamento antes. Até expirar, o pedido abandonado pode ser pareado com outro jogador; esta limitação foi aceita. Não acrescentar método RMI ou campos de cancelamento.
- Interrupção da thread de espera no servidor: remover o pedido ainda pendente, restaurar o estado de interrupção e retornar INTERRUPTED quando possível. Fechar o cliente não equivale a interromper essa thread no servidor. Se o par já foi concluído, preservar sua resposta.
- Nenhuma nova tentativa automática. Após TIMEOUT, o usuário pode iniciar deliberadamente outro pedido. Após falha RMI, o resultado pode ser desconhecido se o pedido já tiver sido enviado; não reenviar automaticamente.
- Falha de transporte/Registry: tratar localmente por exceção RMI, mostrar contexto e encerrar a tentativa; não inventar um JSON retornado pelo servidor. O cliente de diagnóstico mostra erros de aplicação com código/mensagem e termina com saída 1; sucesso termina com saída 0.

Exemplo de erro (texto ilustrativo; os tipos e nomes dos campos são o contrato):

```json
{"erro":"TIMEOUT","mensagem":"Nenhum oponente encontrado em 300 segundos."}
```

### 2. JSON sem dependências externas

**Decisão atual do responsável (25/10/2026):** substituir `org.json` por `JsonUtil`, implementado com o JDK. Não há JAR para baixar nem pasta `lib/`. `JsonUtil.parseJson` lê objetos como `Map`, arrays como `List`, números como `BigDecimal`, além de strings, booleanos e `null`; `toJson` faz a conversão inversa. Chaves duplicadas, JSON malformado e aninhamento acima de 32 níveis são rejeitados. `JsonMessages` limita a entrada a 16384 unidades de `String.length()` (UTF-16) e valida os campos do match. O limite passou de bytes para comprimento de String ao remover a conversão manual. A decisão anterior de usar a biblioteca foi substituída por este pedido.

No RMI, o JSON é passado como `String` e o próprio RMI cuida do transporte. A codificação e delimitação dos futuros sockets não são definidas por essa assinatura. O TCP agora usa UTF-8 com um JSON por linha, conforme aprovação posterior descrita abaixo.

### 3. Mensagens de jogada e controle

Campos do professor preservados: Numero, Jogada, Tabuleiro e Resultado. O protocolo TCP aprovado posteriormente está detalhado na seção “Protocolo TCP aprovado e implementado” abaixo. Tabuleiro posterior à jogada, resultado do emissor, iniciador X, UUID inicial e controles separados foram confirmados pelo responsável.

**Diferença em relação ao PDF:** o exemplo do professor sugere devolver a mesma mensagem quando inválida. O responsável escolheu controle separado com motivo e refazer. Esta escolha está documentada, mas não constitui confirmação de aprovação do professor.

### 4. Timeout, falhas e displays

- Na espera por um adversário no **servidor de match**, limite de **300 segundos (5 minutos)**; ao esgotar, retirar a solicitação pendente da fila e informar timeout pelo JSON de erro aprovado.
- Na espera pela resposta do outro jogador durante a partida, limite de **120 segundos (2 minutos)**; ao esgotar, partida encerrada por **inatividade**.
- Falha durante a partida deve encerrá-la com **erro**, em vez de travar indefinidamente.
- Falha ou ausência de display **não** pode bloquear a partida: publicação multicast é desacoplada.
- O escopo de tolerância a falhas abrange RMI indisponível, registros duplicados, fila concorrente, desconexão do oponente, JSON inválido, jogadas ilegais, problemas no multicast e encerramento de recursos.

### 5. Interoperabilidade

O responsável definiu que a interoperabilidade será verificada **entre os componentes do próprio sistema**; **não** planejar como obrigatório um teste com implementações de outros alunos. Essa é uma decisão de escopo do projeto; o PDF apenas menciona a possibilidade de testes entre implementações.

## Restrições de trabalho e avaliação

- O responsável aprovou o match e posteriormente a integração TCP descrita abaixo. Multicast e UDP exigem etapas próprias.
- Preferir a solução mais simples e direta, sem frameworks e sem abstrações desnecessárias.
- Testes **exclusivamente manuais** executando servidor, jogadores e displays em terminais. Sem JUnit/Mockito ou testes automatizados.
- Para cada falha prevista, registrar **o que pode falhar**, **a reação esperada** e **como reproduzir/testar manualmente**.
- Não declarar um teste como `passou` sem executar o comando e observar o resultado.
- Nas etapas autorizadas, fornecer comandos de compilação/execução `javac`/`java` e casos primeiro em `localhost`, depois nas VMs.
- Projeto final: página Web simples e organizada na conta da VM `152.92.236.14`, com documentação e pitch de cerca de 10 minutos (ou link).

## Situação e retomada

| Marco | Estado |
| --- | --- |
| Item 1 — entendimento, decisões e dúvidas | **Em consolidação; decisões atualizadas** |
| Item 2 — funcionamento explicado ponta a ponta | **Aguardando confirmação explícita** |
| Arquitetura e classes | Match, regras e sessão TCP separados; publicação e Display multicast implementados |
| Java do match | **Base implementada; políticas de match aprovadas, validação em andamento** |
| Integração e testes | Resultados atuais na seção 9 e quadro da seção 7 de `PLANO_DO_PROJETO.md`; comandos em `TESTES_MANUAIS.md` |
| `PLANO_DO_PROJETO.md` | **Registro de acompanhamento criado antecipadamente a pedido do responsável; isso não inicia o item 6** |

Leia [`PLANO_DO_PROJETO.md`](PLANO_DO_PROJETO.md) para pendências e **BLOCO DE RETOMADA**. Toda nova decisão deve atualizar os três documentos quando afetar suas instruções. Não confundir criação de documentação com execução de testes ou finalização de etapas.

## Base Java importada em 25/10/2026

Os programas possuem entradas separadas: `MatchServer.java` executa o servidor e mantém a lógica em `MatchServer.getOponent`; `Player.java` executa a busca RMI e inicia a partida TCP pela classe `TcpGameSession` em arquivo separado, com mensagens validadas por `GameMessages` e regras em `TicTacToeRules`; `Display.java` recebe snapshots multicast e apresenta tabuleiro e resultado; `Display` configura e opera os sockets. `MatchService` é a interface remota; `JsonMessages` interpreta e valida as strings JSON via `JsonUtil`. O registro transportado é representado por `JsonMessages.PlayerRegistration`, distinto do programa Player. A antiga classe `Main` foi removida. `PendingMatchRequest` guarda jogador, prazo e resposta; a espera usa `wait`/`notifyAll`, sob um único monitor. Não são usadas lambdas, futures de arrays nem notação de generics; `final` é permitido somente para constantes nomeadas, conforme a seção 9 de `AGENTS.md`. As coleções usam casts explícitos; por isso o compilador pode informar operações unchecked.

A compilação e a execução exigem somente o JDK 17. Os comandos usam `/tmp/tictactoe-classes` para manter apenas fontes em `code/`; após reiniciar a máquina ou limpar `/tmp`, recompilar. As rotinas de servidor, cliente e JSON foram separadas em métodos simples, com constantes nomeadas e erros de configuração explícitos em português.

Compilar com JDK 17 ou superior:

```bash
mkdir -p /tmp/tictactoe-classes
javac -encoding UTF-8 -d /tmp/tictactoe-classes code/*.java
```

Servidor local (terminal 1):

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_OBJECT_PORT=5021 RMI_SERVICE_NAME=MatchServiceLocal \
  java -cp /tmp/tictactoe-classes MatchServer
```

Jogador — busca de oponente, sem partida ainda (terminal 2):

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_SERVICE_NAME=MatchServiceLocal \
  PLAYER_IP=127.0.0.1 PLAYER_PORT=5010 PLAYER_PROTOCOL=tcp \
  java -cp /tmp/tictactoe-classes Player
```

No terminal 3, repetir o cliente com `PLAYER_PORT=5011`. Ambos recebem o endpoint do oponente. O cliente é só de diagnóstico; não abre socket de jogadas. Ele valida a resposta JSON e mostra endpoint/iniciador no sucesso; erros aparecem com código e explicação em português, saída 1 e sem nova tentativa automática.

As políticas de fila, ordem de início, duplicidade e erros foram aprovadas pelo responsável em 25/10/2026 conforme o contrato acima. A implementação usa `wait`/`notifyAll`. Cancelamento imediato e novas tentativas automáticas não fazem parte desta versão; o pedido de cliente encerrado pode permanecer elegível para pareamento até expirar.

`MATCH_TIMEOUT_SECONDS` tem padrão **300**, conforme a decisão anterior desta pasta. Testes podem reduzir esse parâmetro temporariamente. IP/portas são configuráveis; os padrões de VM permanecem `.14`, Registry `9821` e serviço `MatchService21`. A porta definitiva do objeto remoto nas VMs está pendente (`RMI_OBJECT_PORT` é obrigatório). Os valores locais `1099`/`5021` são exemplos. O prazo de 120 s da partida é aplicado separadamente por TcpGameSession.

Os roteiros completos, incluindo teste com outro IP e portas, estão em `TESTES_MANUAIS.md`. Não foram importados Maven, Gradle, Gson, JUnit ou scripts de testes automatizados.


Entrada reservada ao display:

```bash
java -cp /tmp/tictactoe-classes Display
```

Definir MULTICAST_INTERFACE antes de executar. Para teste local, usar `MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-classes Display`. O programa permanece escutando e mostra os tabuleiros recebidos; Ctrl+C encerra o display. Podem existir várias instâncias.


Ao atualizar uma VM da antiga assinatura byte[] para String, encerrar os processos Java antigos, atualizar os fontes correspondentes (incluindo `MatchService.java`), recompilar e reiniciar primeiro MatchServer e depois os Players. A interface compartilhada deve ter a mesma versão em todas as VMs. O nome do serviço e as variáveis de ambiente continuam iguais.

## Regras locais do jogo

`code/TicTacToeRules.java` concentra as regras, separadas de `Player`: `placeMove(casa, simbolo)` valida e aplica uma jogada; `getBoardCopy()` fornece uma cópia; `getAcceptedMoveCount()` informa quantas jogadas foram aceitas; `getWinner()`, `isDraw()` e `isGameOver()` consultam o resultado. Valores: 0 vazio/sem vencedor local, 1 X, 2 O. A primeira jogada aceita o símbolo informado pela sessão; as seguintes exigem alternância. Jogadas rejeitadas não alteram estado. A classe não define quem inicia, o JSON da partida ou o transporte TCP e é utilizada pelo Player por meio de TcpGameSession. O zero retornado por getWinner não substitui o null já aprovado no JSON de encerramento.

Compilação e verificação manual: caso 13 de `TESTES_MANUAIS.md`.

### Protocolo TCP aprovado e implementado em 25/10/2026

O responsável confirmou expressamente a proposta TCP nesta conversa. São escolhas do projeto, complementares ao PDF:

- Ambos reservam PLAYER_IP:PLAYER_PORT antes de chamar RMI. Quem recebe inicia=true conecta e usa X; o outro aceita e usa O. Aceite/conexão limitados a 120 s, sem reconexão automática. O IP da conexão aceita deve coincidir com o adversário retornado pelo match.
- UTF-8, um objeto JSON por linha, no máximo 16384 unidades UTF-16 por mensagem. Codificação inválida, JSON ilegível ou mensagem excessiva encerram por erro; jogada legível com dados inválidos recebe rejeição e pode ser corrigida. A leitura usa um prazo absoluto, inclusive durante mensagens incompletas.
- O iniciador gera um UUID e envia `{"evento":"inicio","partidaId":"UUID"}` antes da primeira jogada. O JSON RMI permanece inalterado.
- Jogadas usam exatamente Numero, Jogada, Tabuleiro, Resultado. Numero é o índice global antes de aplicar a jogada; Tabuleiro é posterior à jogada; Resultado é do emissor. Esta implementação envia jog, vit ou emp; der permanece reservado no vocabulário do professor, mas não representa uma jogada válida própria neste fluxo.
- Rejeição: `{"evento":"rejeicao","partidaId":"UUID","motivo":"texto","refazer":true}`. Tabuleiro e contador preservados; prazo original de 120 s não reinicia. O emissor mantém uma cópia anterior para desfazer a tentativa rejeitada; a próxima jogada válida confirma implicitamente a sua jogada anterior.
- Quem recebe uma jogada conclusiva valida e envia `{"evento":"fim","partidaId":"UUID","status":"vitoria","vencedor":1}`; status empate usa vencedor null. Autor da jogada valida e responde `{"evento":"confirmacao_fim","partidaId":"UUID"}`. Vitória usa vencedor 1/2. Cada espera de confirmação tem limite de 120 s.
- Falha/inatividade: fim com status erro/inatividade, vencedor null e motivo, enviado quando possível; encerrar localmente com saída 1. Não há confirmação obrigatória para erro de rede nem garantia de entrega. Resultado normal confirmado termina com saída 0. EOF do terminal encerra por erro. Uma thread daemon simples lê a entrada enquanto a sessão observa a rede, permitindo detectar desconexão ou fim remoto durante a escolha humana.
- Regras locais em TicTacToeRules; protocolo/validação em GameMessages; troca TCP/terminal em TcpGameSession; Player configura, busca match e abre conexão. Publicação e Display multicast implementados na etapa seguinte, conforme seção abaixo.

## Arquivos para executar a versão TCP nas VMs

Na pasta única ~/tictactoe da VM .14: MatchServer.java, MatchService.java, JsonMessages.java, JsonUtil.java e a configuração .env do servidor.

Na pasta ~/tictactoe de cada VM jogadora (.16 e .17): Player.java, MatchService.java, JsonMessages.java, JsonUtil.java, TicTacToeRules.java, GameMessages.java, TcpGameSession.java, Display.java e a configuração .env daquele jogador. Display.java pode ficar na mesma pasta e executar em outro terminal de cada jogador.

Recompilar com `javac -encoding UTF-8 *.java` em cada máquina. Iniciar `java -cp . MatchServer` na .14 e depois `java -cp . Player` na .16 e .17. PLAYER_IP deve ser local à própria VM e PLAYER_PORT deve estar disponível/acessível ao adversário. Roteiro completo e resultados esperados no caso 14 de TESTES_MANUAIS.md. Não executar processos antigos com arquivos novos.

Por decisão mais recente do responsável, TcpGameSession permanece em code/TcpGameSession.java, separado de Player.java. Distribuir ambos aos jogadores e compilar juntos com os demais fontes; executar Player normalmente.

## Multicast implementado

A etapa solicitada inclui publicação e Display. `Display.java` concentra configuração e sockets. Após validar a jogada recebida por TCP, o adversário publica aquele JSON uma vez; assim, tentativas rejeitadas não são transmitidas ao display. Essa é uma escolha de implementação desta etapa, não uma regra textual do professor. Os quatro campos permanecem Numero/Jogada/Tabuleiro/Resultado; Resultado mantém a perspectiva do autor da jogada TCP, não de quem publica o datagrama.

O Display valida cada snapshot independentemente (campos, tipos, contagem de peças, última casa, vencedor e resultado), exibe o tabuleiro e continua escutando após o fim de uma partida. Pode iniciar no meio do jogo: a próxima mensagem já contém o tabuleiro completo. Não reconstrói histórico, não solicita reenvio e não separa partidas simultâneas pelo mesmo canal, pois o JSON de jogada não contém partidaId. Datagrama perdido pode ser compensado visualmente pelo snapshot seguinte, mas não há garantia de entrega, ordem ou ausência de duplicação. Não altera o protocolo TCP nem adiciona partidaId às jogadas.

| Variável | Configuração |
| --- | --- |
| MULTICAST_GROUP | Padrão aprovado 225.1.1.21; ajustável para testes isolados |
| MULTICAST_PORT | Padrão aprovado 8821; inteiro 1..65535 |
| MULTICAST_INTERFACE | Nome obrigatório da interface local, diferente conforme a máquina |
| MULTICAST_TTL | Inteiro 0..255, padrão operacional 1; 0 para teste somente local |

Interface e TTL definitivos das VMs continuam sujeitos à verificação da rede. Nos testes desta etapa foi usado MULTICAST_INTERFACE=lo e MULTICAST_TTL=0, sem transmitir para outras máquinas. Loopback multicast fica habilitado, permitindo display e jogador na mesma máquina. Múltiplos displays compartilham a porta com SO_REUSEADDR configurado antes de bind.

No Player, configuração/envio multicast com falha gera aviso e desativa a publicação, sem encerrar a partida TCP. Não há espera por displays nem ACK multicast. Os sockets de publicação são fechados ao terminar a sessão. No Display, falha ao abrir/receber no socket encerra o processo com saída1; JSON/UTF-8 inválido, campos inconsistentes ou datagrama acima de16384bytes são descartados com aviso, sem encerrar o receptor.

Para uma máquina apenas de display, em ~/tictactoe: Display.java, GameMessages.java, TicTacToeRules.java, JsonUtil.java e .env com a interface local. O display não precisa de MatchServer, Player nem RMI.

Nas VMs jogadoras, acrescentar ao .env `MULTICAST_INTERFACE` com o nome real da interface da própria VM e `MULTICAST_TTL` apropriado à rede (1 é um ponto inicial, não comprova roteamento entre VMs). Preservar grupo225.1.1.21 e porta8821. Recompilar com `javac -encoding UTF-8 *.java`, recarregar o ambiente e executar `java -cp . Display` em terminal separado. Iniciar displays antes dos jogadores para acompanhar a partida completa. Roteiros locais completos: casos17/18 de TESTES_MANUAIS.md.
