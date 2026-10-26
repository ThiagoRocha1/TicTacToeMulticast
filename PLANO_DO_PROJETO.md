# PLANO_DO_PROJETO.md — TicTacToeMulticast

> **Registro de continuidade inicial**, preparado em **24/10/2026** porque o responsável pediu para registrar as decisões no repositório. A criação antecipada deste arquivo **não constitui avanço ao item 2 nem conclusão do item 6** da sequência pedagógica.
>
> **Atualização em 25/10/2026:** o responsável solicitou a implementação do match e seu uso neste workspace. A base Java foi inicialmente importada com o método de match dentro de `Main`; por pedido posterior foi separada em `MatchServer`, `Player` e `Display`. Por instrução posterior explícita, prevalecem as decisões anteriores desta pasta: **300 s** para match, **120 s** para resposta durante a partida, **utilitário JSON próprio (pedido posterior substitui org.json)** e testes **manuais**. Depois dessas alterações, o responsável aprovou as políticas de match registradas na seção 4. Posteriormente o responsável aprovou o protocolo TCP e sua implementação; multicast implementado posteriormente nesta etapa; permanece validação/configuração remota.

## 1. Fonte de verdade e organização

- **Enunciado oficial:** `trab-2-rmi-multi-2026-1.pdf`, Prof. Alexandre Sztajnberg, quatro páginas, incluindo Figura 1 (Arquitetura). O PDF é a fonte do que **o professor pede**.
- **Decisões do responsável:** complementam aspectos que o professor não especificou; identificadas como tal neste plano.
- **Regras para agentes:** `AGENTS.md`.
- **Visão geral:** `README.md`.
- **Vigência:** cabeçalho interno do PDF `2023/1`; o responsável **confirmou** que o documento vale para **2026/1**.
- **Interoperabilidade:** definida pelo responsável como **interna aos componentes do próprio sistema**, não como obrigação de testes com colegas.

Não supor que o PDF está comitado no repositório sem verificar.

## 2. Resumo dos requisitos do PDF

- Jogo da velha distribuído de dois jogadores; um jogador participa de **uma partida por execução**.
- **Servidor de match (RMI):** método remoto único `getOponent`; recebe `ip`, `porta`, `protocolo`; encontra dois interessados de **mesmo protocolo**; retorna IP/porta do oponente e `inicia` diferente para os dois; conclui as **duas** chamadas pendentes e remove o par da fila.
- **Jogadores:** solicitam pareamento por RMI e realizam jogadas diretamente entre si via socket TCP/UDP; `inicia=true` joga primeiro; publicam **cada jogada** por multicast.
- **Displays:** recebem jogadas via multicast e apresentam o tabuleiro; múltiplas instâncias possíveis.
- Mensagem de jogada: `Numero`, `Jogada`, `Tabuleiro`, `Resultado` (`jog`/`vit`/`der`/`emp`); casas `0..8`; contador inicial zero.
- Controle de jogadas inválidas e aceite final são descritos no PDF, mas detalhes de protocolo exigem consolidação.
- Infraestrutura com tolerância a falhas; o PDF **não determina uma política única** de timeout/cancelamento.
- Linux nas VMs `152.92.236.14`, `.16`, `.17`; página Web simples na conta da `.14`, documentação e pitch de cerca de 10 minutos.

## 3. Parâmetros de ambiente

| Parâmetro | Valor | Origem/status |
| --- | --- | --- |
| NN | `21` | Informado pelo responsável |
| Protocolo da primeira entrega | `TCP` | Decisão do responsável |
| Protocolo para etapa posterior | `UDP` | Decisão do responsável (não iniciar agora) |
| VM servidor match | `152.92.236.14` | Escolha do responsável |
| Porta RMIRegistry | `9821` | Fórmula do PDF `98NN` |
| Grupo multicast | `225.1.1.21` | Fórmula do PDF `225.1.1.NN` |
| Porta multicast | `8821` | Fórmula do PDF `88NN` |
| Versão Java nas VMs | `17.0.20.1` | Informado pelo responsável |
| Portas de jogadores | **A definir** | Dependem da execução |

## 4. Registro de decisões

| ID | Decisão | Situação | Origem / observação |
| --- | --- | --- | --- |
| D01 | Usar **TCP primeiro** e ampliar para UDP depois | **Aprovado** | Responsável |
| D02 | Match considera pedidos TCP e UDP, pareando protocolos iguais | **Aprovado** | Responsável; pareamento por protocolo já consta no PDF |
| D03 | Servidor RMI na VM `.14`; `NN=21` | **Aprovado** | Responsável |
| D04 | Interface `MatchService` com método `getOponent` recebendo e retornando **`String`** | **Atualizado pelo responsável** | Pedido posterior substitui a decisão anterior de byte[] |
| D05 | JSON textual em String no RMI; transporte feito pelo RMI, interpretação feita pelo utilitário | **Atualizado pelo responsável** | Sem conversão manual de bytes no match; TCP posteriormente aprovado: UTF-8 por linha |
| D06 | Utilizar **`JsonUtil`**, somente JDK | **Aprovado pelo responsável** | Pedido posterior em 25/10/2026 substitui org.json; sem JAR externo |
| D07 | `Tabuleiro`: `0` vazio, `1` X, `2` O | **Aprovado** | Responsável |
| D08 | `Numero` é **global por partida**, começa em `0` e vai até `8` | **Aprovado** | Responsável |
| D09 | Separar JSONs **de jogada** dos JSONs **de controle** | **Aprovado** | Responsável |
| D10 | Jogada inválida: retornar mensagem ao autor com motivo e solicitação de refazer, sem avanço de `Numero` | **Aprovado** | Responsável; diferencia-se do eco literal sugerido no PDF |
| D11 | Ao encerrar, informar a **ambos** o status e o vencedor, quando houver | **Aprovado** | Responsável; não garantir entrega se rede falhou |
| D12 | Espera pela resposta do oponente limitada a **120 segundos** | **Aprovado** | Responsável |
| D13 | Inatividade encerra a partida; falha encerra por **erro** | **Aprovado** | Responsável |
| D14 | Testar interoperabilidade **somente interna** | **Aprovado** | Responsável |
| D15 | Enunciado continua válido para **2026/1** | **Confirmado** | Responsável |
| D16 | O item 2 só começa mediante confirmação expressa | **Vigente** | Responsável |
| D17 | Timeout para encontrar adversário: **300 segundos (5 minutos)**; ao expirar, remover solicitação da fila e informar timeout | **Aprovado** | Responsável |
| D18 | Cada partida terá um identificador **`partidaId` único**, incluído nas mensagens JSON de controle | **Aprovado** | Responsável; posteriormente definido como UUID no controle inicio |
| D19 | Protocolo de encerramento com eventos de controle **`fim`** e **`confirmacao_fim`** | **Aprovado** | Responsável; payload TCP posteriormente consolidado na seção 8 |
| D20 | Campo **`vencedor`** nas mensagens finais: `1` = X, `2` = O, `null` = sem vencedor | **Aprovado** | Responsável |
| D21 | Fila por protocolo em ordem de registro; primeiro inicia=true, segundo false | **Aprovado** | Responsável; não define X/O |
| D22 | Erros JSON com strings erro/mensagem; códigos INVALID_REQUEST, DUPLICATE, TIMEOUT, INTERRUPTED | **Aprovado** | Responsável; texto explicativo em português |
| D23 | Duplicata é IP normalizado + porta + protocolo de pedido pendente; preservar original e prazo | **Aprovado** | Responsável |
| D24 | Sem cancelamento imediato ao encerrar cliente; pedido permanece até match ou timeout e pode parear com cliente encerrado | **Aprovado, com limitação aceita** | Responsável |
| D25 | Sem retries automáticos; novo pedido deliberado após TIMEOUT; resultado possivelmente desconhecido em falha RMI | **Aprovado** | Responsável |
| D26 | Interrupção da espera no servidor remove pedido pendente, restaura flag e retorna INTERRUPTED se possível; falha RMI tratada localmente | **Aprovado** | Responsável |
| D27 | iniciador conecta/X; outro aceita/O; escuta antes do RMI | **Aprovado** | Responsável confirmou proposta TCP |
| D28 | UTF-8 por linha, UUID no controle inicio, tabuleiro posterior e Resultado do emissor | **Aprovado** | Responsável confirmou proposta TCP; esquema implementado no README |
| D29 | Rejeição com motivo/refazer preserva estado e prazo120s; fim com status/vencedor, seguido de confirmacao_fim | **Aprovado** | Responsável confirmou proposta TCP |

**Interface implementada na base importada:**

```java
// Interface MatchService estende java.rmi.Remote.
String getOponent(String jsonIn) throws RemoteException;
```

**JSON do match (do PDF):**

```json
{"ip":"152.92.236.16","porta":5000,"protocolo":"tcp"}
```

```json
{"ip":"152.92.236.17","porta":5001,"inicia":true}
```

**JSON de jogada (campos do PDF, valores conforme decisão D07):**

```json
{"Numero":0,"Jogada":4,"Tabuleiro":[0,0,0,0,1,0,0,0,0],"Resultado":"jog"}
```

Registro inicial, anterior à consolidação TCP abaixo: os JSONs acima são **exemplos ilustrativos**, não resultados de testes. Os eventos `fim` e `confirmacao_fim`, o campo `partidaId` nas mensagens de controle e o campo `vencedor` com valores 1/2/null foram **aprovados**. Outros campos (como `tipo`, `motivo`, `refazer`), o esquema JSON completo e o mecanismo de criação/propagação de `partidaId` ainda dependem de consolidação.

**Exemplo ilustrativo de término** (esquema ainda não congelado):

```json
{"tipo":"controle","evento":"fim","partidaId":"a7f3c92b","resultado":"vitoria","vencedor":1}
```

```json
{"tipo":"controle","evento":"confirmacao_fim","partidaId":"a7f3c92b"}
```

Fluxo aprovado em termos gerais: autor da última jogada → adversário valida e envia `fim` → autor confirma com `confirmacao_fim` → encerramento. Falha de conexão pode impedir a confirmação remota; nesses casos deve haver tratamento local.


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

## 5. Arquitetura atual — somente conceitual

```text
Jogador A -- RMI --> Servidor de Match <-- RMI -- Jogador B
     |                                              |
     +----------- socket TCP direto ----------------+
     |                                              |
     +------ publicação multicast 225.1.1.21:8821 -+
                           |
                      Display(s)
```

Três programas previstos: **Servidor de Match**, **Jogador** e **Display**. Os nove fontes estão diretamente em code: MatchServer, Player, Display, MatchService, JsonMessages, JsonUtil, TicTacToeRules, GameMessages e TcpGameSession. O servidor mantém as filas e a espera sincronizada; Player busca oponente e abre a conexão TCP, delegando a sessão a TcpGameSession e as regras a TicTacToeRules. Display escuta multicast e valida/exibe cada snapshot recebido. A implementação e as políticas de fila, duplicidade, erros, timeout, desistência e ausência de retries automáticos foram aprovadas pelo responsável. A partida TCP foi implementada após aprovação posterior. Publicação/recepção multicast implementadas e verificadas em loopback nesta etapa.

## 6. Mapa de falhas (validação parcial; demais cenários não executados)

| Módulo / falha | Reação desejada | Como testar futuramente (manual) | Status |
| --- | --- | --- | --- |
| Match: servidor/registry fora do ar | Reportar falha claramente, evitar espera infinita; sem retries automáticos | Não iniciar ou encerrar o servidor e tentar conectar jogadores | **Passou em localhost na última validação**: erro RMI contextualizado e saída 1; VMs não executadas |
| Match: fila sem oponente | Após **300 s** sem par, remover pedido e informar timeout | Iniciar jogador isolado por 5 min; repetir registro após expiração | **String: passou com 10 s**. Prazo real de 300 s e novo registro após expiração passaram antes da mudança de assinatura; repetir na nova versão |
| Match: registro duplicado | Rejeitar duplicata, preservando pedido original e prazo; política aprovada | Registrar duas vezes o mesmo IP, porta e protocolo | **Passou em localhost na última validação**: DUPLICATE; primeiro pedido preservado e depois pareado |
| Match: desistência do cliente | Política aprovada: sem cancelamento imediato; pedido pode parear até ser removido por match ou timeout | Interromper cliente enquanto aguarda o par | **Limitação observada**: duplicata após Ctrl+C e pareamento ao endpoint encerrado; variante de limpeza por timeout de cliente encerrado não executada |
| Match: acessos concorrentes | Parear sem duplicar clientes e retornar para os dois | Iniciar vários jogadores simultaneamente | Par básico de duas chamadas: passou; carga maior não executada |
| Jogador: oponente sem responder | Encerrar por **inatividade após 120 s** de espera de resposta | Deixar oponente sem responder por 120 s | **Passou localhost**, prazo real e saída 1 nos dois jogadores |
| Jogador: conexão interrompida | Encerrar por erro e liberar recursos; log local | Fechar um jogador durante a partida | **Passou localhost**, inclusive durante entrada humana |
| Jogador: JSON inválido / campos faltantes | Rejeitar ou informar erro de protocolo, sem travar | Enviar mensagem malformada no fluxo manual aprovado | Não executado |
| Jogador: casa ocupada / fora de turno / contador incorreto / tabuleiro inconsistente | Rejeitar, justificar ao autor e solicitar correção sem avançar contador | Reproduzir mensagens inválidas em teste manual autorizado | Não executado |
| Display: ausente ou falha multicast | Partida continua; registrar erro de publicação quando detectável | Jogar sem displays; interromper display no meio | **Passou localhost**: display encerrado durante empate; partida sem displays com publicador mal configurado |
| Rede multicast entre VMs | Diagnosticar grupo, interface, TTL, regras de rede; plano B localhost | Rodar display na outra VM; repetir em localhost | Não executado |

**Observação:** os timeouts são **distintos e aprovados**: **300 s** aguardando match no servidor; **120 s** aguardando resposta do adversário durante a partida.

## 7. Plano de etapas (proposta, SEM avanço automático)

| Etapa | Objetivo / aprendizagem | Entregável planejado | Teste manual básico previsto | Situação |
| --- | --- | --- | --- | --- |
| **0** | Requisitos, decisões e ambiguidades | Documentação e contratos acordados | Revisão com responsável | **Em andamento (item 1)** |
| **1** | Regras do jogo e JSON | Lógica do tabuleiro + mensagens | Rodar manualmente e inspecionar saída no terminal | Utilitário JSON e mensagens de match implementados/testados; regras locais em TicTacToeRules implementadas e verificadas manualmente; mensagens de partida implementadas na etapa TCP |
| **2** | Publicação/recepção multicast | Display simples | Executar display e remetente em localhost | **Implementada e verificada em loopback**; VMs pendentes |
| **3** | Comunicação direta | Dois jogadores via TCP local | Jogar partida manual em terminais | Implementada e verificada em localhost; validações adicionais pendentes |
| **4** | RMI e concorrência | Servidor de match | Iniciar servidor e dois clientes em localhost | Políticas de match aprovadas; validação manual local em andamento |
| **5** | Integração | Match + jogo em localhost | Partida completa, dois jogadores e display | **Passou localhost** com duas instâncias de Display; demais falhas pendentes |
| **6** | Robustez | Timeouts, validações, tratamentos de falha | Cenários do mapa de falhas | Validações selecionadas do match executadas; demais módulos e cenários não iniciados |
| **7** | Execução remota | Implantação nas três VMs | Match, partida e displays entre VMs | Não iniciada |
| **8** | Interoperabilidade interna | Compatibilidade entre módulos do projeto | Testes cruzados de mensagens e módulos próprios | Não iniciada |
| **9** | Segundo protocolo (posterior) | Suporte UDP, se autorizado na ocasião | Perda/duplicação/reordenação simuladas manualmente | Não iniciada |
| **10** | Entrega | Página Web, documentação e pitch | Abrir a página na VM `.14` e revisar pitch | Não iniciada |

A divisão acima é **proposta de desenvolvimento**, não a sequência de itens didáticos já percorrida. Cada etapa futura deverá registrar objetivo, o que aprender, critérios de pronto, mapa de falhas, roteiro de teste manual e resultados de fato.

### Quadro de resultados de testes

A versão atual usa **String no RMI**. Evidências da versão byte[] permanecem na seção 9 como histórico, sem comprovar esta assinatura. Resultados locais observados pelo agente e relatos do responsável são identificados separadamente.

| Grupo | Resultado observado na versão String | Pendente / histórico |
| --- | --- | --- |
| Compilação | **Passou**, JDK 17.0.21+9; avisos unchecked esperados | Compilar a nova interface e todos os componentes nas VMs |
| JSON / mensagens de match | **Passou**: pedido válido, sucesso/erro com acentos, rejeição de JSON malformado, null, mensagem ausente e mais de 16384 unidades UTF-16 | Parsing geral, tipos restantes e limites de aninhamento têm evidência anterior; bytes UTF-8 inválidos não se aplicam à entrada String |
| RMI / pareamento | **Passou**: par TCP localhost 5010/5011, endpoints corretos, primeiro inicia e segundo aguarda | Separação TCP/UDP, IP alternativo, maior concorrência e interrupção real não reexecutados com String |
| Falhas do match | **Passou**: DUPLICATE e INVALID_REQUEST por IP inválido, saídas 1 | Desistência, serviço ausente, servidor indisponível e demais falhas têm evidência anterior com byte[]; não reexecutados com String |
| Timeout do match | **Passou com 10 s**, valor reduzido apenas para teste; TIMEOUT sem retry | Prazo real de 300 s e novo pedido após expiração passaram na versão byte[]; ainda não repetidos com String |
| Três VMs | **Não executado com String** | Pareamento anterior relatado pelo responsável; atualizar e recompilar todas as VMs antes de repetir |
| Tabuleiro / mensagens de partida | **Regras locais: passou** no JShell; oito linhas vencedoras, empate e rejeições | Integradas ao TCP e verificadas em partidas locais; demais cenários de mensagens pendentes |
| Socket TCP / partida | **Passou em localhost**: vitórias X/O, empate, rejeição/correção, desconexão, timeout real 120 s | VMs e demais cenários de rede pendentes |
| Multicast / display | **Passou em loopback**: dois displays, vitória/empate, descarte/recuperação de datagramas inválidos, fechamento de display e ausência de display | VMs, perda/reordenação e falha real de envio não testadas |
| Jogo por UDP | **Não iniciado / não executado** | Classificação de pedidos UDP não implementa jogo UDP |

## 8. Pendências e perguntas em aberto

As escolhas TCP propostas foram aprovadas expressamente: iniciador conecta/X, outro aceita/O, UTF-8 por linha, UUID inicial, tabuleiro posterior, Resultado do emissor, rejeição sem reiniciar prazo e controles de encerramento. Contrato implementado em AGENTS.md seção 5 e README; complementa o PDF. A diferença entre rejeição por controle e eco da mesma mensagem sugerido pelo PDF continua documentada; não atribuir aceite ao professor.

1. Configurar/validar portas remotas e acesso entre VMs; não apresentar exemplos locais como portas definitivas.
2. Publicação e Display implementados. Definir/testar interface e TTL nas VMs. Não implementar UDP antecipadamente.
3. Completar casos manuais de robustez ainda não executados; preservar evidências históricas do match.

## 9. Última mudança registrada

**26/10/2026 — revisão do roteiro de gravação:** retirado clipe7 e redistribuídos10min em seis partes. Parte5 ampliada com arquivos, métodos, linhas conferidas e fala correspondente ao tratamento de falhas. Parte6 inclui comandos completos e sequências de vitória X, vitória O e empate, tabuleiros finais e resultados por terminal; sem novas partidas executadas nesta revisão. Incluídas orientações de edição no Canva, identificação de capítulos/terminais, destaques e transições. Fontes Java inalterados; guia e README sincronizados.

**26/10/2026 — roteiro de apresentação solicitado:** criado ROTEIRO_DO_VIDEO.md com sete clipes totalizando10 minutos, seis propostas de slides, fala-base, navegação por métodos, matriz falha/detecção/reação/limite e cenas de demonstração. Foco em RMI, TCP e multicast; distingue continuidade sem Display de encerramento por falha do oponente. Guia e README vinculados. Código inalterado, sem novos testes ou gravação; validação nas VMs continua pendente.

**26/10/2026 — identificação local simplificada:** substituído socket de descoberta por InetAddress.getLocalHost(), preservando PLAYER_IP opcional. Porta reservada com ServerSocket(porta), padrão0, preservando PLAYER_PORT opcional. Primeiro teste **falhou**: anúncio127.0.1.1, origem127.0.0.1, rejeição por IP divergente. Corrigido associando o socket de saída ao IP anunciado antes de connect com timeout. Repetição **passou**: IP127.0.1.1, portas46557/38065, entradasabc/9 rejeitadas, sequência0/3/1/4/2, vitória X e derrota O, ambos saída0. Compilação JDK17 em /tmp/tictactoe-simple-address **passou**, avisos unchecked esperados. Sem Display em execução; nenhum teste multicast novo. Timeout120s preservado no código, não reexecutado; VMs pendentes. Loopback não funciona entre máquinas. Documentação/guia e caso23 sincronizados.

**26/10/2026 — nomes explícitos na comunicação do jogador:** Player distingue rmiRegistryHost/rmiRegistryPort, localPlayerAddress/localPlayerRequestedPort/localPlayerListeningSocket e opponentIpAddress/opponentListeningPort/opponentConnectionSocket. Métodos privados e parâmetros renomeados; TcpGameSession distingue entrada/saída do adversário e terminal/símbolo do jogador local. Guia de apresentação sincronizado. Somente nomes e quebras de linha alterados, sem mudança nos contratos JSON/RMI, variáveis de ambiente, descoberta automática ou timeouts. Compilação JDK17 em /tmp/tictactoe-explicit-names: **passou**, com avisos unchecked esperados. Partidas/VMs: **não reexecutadas** nesta refatoração. Próximo passo funcional permanece validação nas VMs.

**26/10/2026 — espaçamento dos nove fontes Java solicitado pelo responsável:** acrescentadas linhas em branco entre grupos de imports, constantes/atributos, validações, laços e etapas lógicas dos métodos. Nenhuma instrução, string ou ordem de execução alterada. Comparação com cópia do estado imediatamente anterior ignorando somente linhas vazias: **passou nos nove arquivos**. Compilação JDK17 em /tmp/tictactoe-spacing: **passou**, avisos unchecked esperados. Partidas/VMs: **não reexecutadas**, por ser alteração exclusivamente de apresentação do código. Preferência registrada em AGENTS.md; JsonUtil próprio e alterações anteriores do MatchServer preservados.

**26/10/2026 — migração org.json desfeita por pedido do responsável:** restaurado JsonUtil com parser/gerador próprios e sem JAR. Removidos dependência e comandos de implantação introduzidos na alteração cancelada; guia e instruções retornaram ao uso somente do JDK. Refatoração do encerramento e remoção do teto explícito do timeout preservadas. Compilação sem JAR: **passou**, avisos unchecked esperados. Partidas completas não reexecutadas nesta reversão; resultados da migração cancelada não comprovam o estado restaurado. Próximo passo permanece validação nas VMs.

**26/10/2026 — remoção do limite máximo explícito de timeout, solicitada durante a refatoração do encerramento:** retirado o teste timeoutSeconds > Long.MAX_VALUE / 1000000000L de readMatchTimeoutSeconds. Mantida positividade, leitura long e padrão300s. Overflow de Duration.toNanos agora vira IllegalArgumentException contextual, tratado como configuração inválida, sem comparação com teto configurado. Compilação final em /tmp/tictactoe-shutdown-method: **passou**, avisos unchecked esperados. Execuções com0 e Long.MAX_VALUE segundos: **passou**, saída1 com causa clara (não positivo/representação em nanossegundos). Partidas completas e VMs: **não executadas**. Caso22 atualizado.

**26/10/2026 — encerramento do servidor em método:** a pedido do responsável, removida a classe interna ServerShutdown. MatchServer implementa Runnable; hook usa a própria instância, run delega a shutdownServer e Registry fica guardado na instância. Sem lambda/classe anônima; contratos RMI preservados. Análise encontrou ainda quatro classes internas com estado: PendingMatchRequest (servidor), PlayerRegistration (JsonMessages), TerminalLineReader (TcpGameSession) e JsonParser (JsonUtil); mantidas e explicadas no guia. Compilação Java17 em /tmp/tictactoe-shutdown-method: **passou**, avisos unchecked esperados. Início local em1199/5121, Ctrl+C, reinício nas mesmas portas e novo Ctrl+C: **passou**, sem erros de encerramento; isso verifica o ciclo do processo, não prova isoladamente a liberação antes da saída da JVM. Processos encerrados. Partidas completas/VMs não reexecutadas. Roteiro22 em TESTES_MANUAIS.md.

**26/10/2026 — guia de estudo e apresentação solicitado pelo responsável:** criado GUIA_DE_APRESENTACAO.md, com explicação dos nove arquivos Java, métodos principais, concorrência RMI, descoberta de endpoint, sessão TCP, regras, validação JSON e Display com ID. Inclui exemplos, roteiro de10 minutos, falas sugeridas, perguntas e demonstração manual com comandos. Conferido contra os fontes atuais e as quatro páginas do PDF, incluindo Figura1; separado requisito expresso de decisão do projeto. Nenhum fonte alterado; testes/compilação nesta etapa documental: **não executados**. Revisão documental de nomes, links e delimitadores: **passou**. Não comprova validação remota, vídeo gravado ou publicação Web. Próximo: ensaiar/gravar a explicação e validar a execução nas VMs antes de afirmar seus resultados no vídeo.

**25/10/2026 — ID antes do tabuleiro no Display:** pedido do responsável implementado com envelope multicast partidaId/jogada, preservando campos internos, RMI e TCP. Display valida ID não vazio/objeto de jogada antes de mostrar o snapshot. Sem seleção/lista persistente; atualizações intercaladas têm identificação por ID. Compilação em /tmp/tictactoe-display-id: **passou**, avisos unchecked esperados. Integração manual localhost: **passou**, Registry1199/objeto5121/MatchDisplayId, portas TCP automáticas40065/39135; sequência0/3/1/4/2, ambos jogadores saída0. Display em lo/TTL0 exibiu o mesmo UUID f413abe4-f6fe-4296-838f-f3453ffec617 nas cinco jogadas0..4 e vitóriaX. Linha vazia entre tabuleiros do jogador também observada. Todos os processos encerrados. VMs, partidas simultâneas, envelopes inválidos e regressão completa: **não executados**. Roteiro21. Atualizar Display e TcpGameSession em todas as VMs jogadoras e Display nos receptores isolados; versões anteriores multicast incompatíveis.

**25/10/2026 — separação visual dos turnos:** a pedido do responsável, showBoard em TcpGameSession imprime uma linha em branco antes de cada tabuleiro no terminal do jogador. Nenhuma regra ou mensagem de rede alterada. Compilação Java17: **passou**, avisos unchecked esperados; verificação visual durante partida: **não executada**. Para verificar manualmente, iniciar MatchServer e dois Players conforme caso20 e alternar jogadas: cada tabuleiro deve começar após uma linha vazia, mantendo suas três linhas consecutivas.

**25/10/2026 — descoberta automática do endpoint do jogador, solicitada pelo responsável:** Player obtém o IP local da rota TCP até o Registry e reserva uma porta com bind 0 antes do pedido RMI. Anuncia a porta real e mantém a escuta aberta. PLAYER_IP/PLAYER_PORT opcionais; valor0 também seleciona porta automática. Nenhum campo/método RMI novo; multicast permanece configurado separadamente. Compilação JDK17 em /tmp/tictactoe-discovery: **passou**, avisos unchecked esperados. Teste manual sem ambas as variáveis: **passou**, endpoints 127.0.0.1:37465 e :40295, pareamento, TCP e vitória X (0/3/1/4/2), resultado nos dois terminais, ambos saída0. Publicação lo/TTL0, sem receptor Display. Servidor encerrado. VMs, falhas de descoberta, overrides nesta alteração e firewall: **não executados**. Roteiro20. Próximo passo: atualizar Player nas VMs, retirar overrides antigos, recompilar e validar alcance das portas dinâmicas.


**25/10/2026 — métodos multicast incorporados diretamente em Display:** por pedido do responsável, removido MulticastChannel.java; configuração, abertura, publicação, recepção, leitura e fechamento dos sockets agora são métodos da própria classe Display, sem classe interna. TcpGameSession mantém uma instância de Display para publicação e a fecha ao terminar. O programa receptor continua iniciado por Display.main, independentemente do publicador. Nove fontes; distribuição/documentação sincronizadas. Compilação limpa em /tmp/tictactoe-display-methods: **passou**, avisos unchecked esperados. Verificação manual de um datagrama publicado pelos métodos da própria Display e recebido em loopback: **passou**, snapshot X0 exibido. Partidas completas não reexecutadas nesta movimentação; verificações anteriores abaixo são históricas. A primeira tentativa de teste parou por timeout da revisão automática de permissão; repetição permitida executada com sucesso, processos encerrados. Mantidos os protocolos e configurações.

**25/10/2026 — fase multicast solicitada e implementada:** criado MulticastChannel para configurar grupo/porta/interface/TTL, publicar e receber datagramas UTF-8. Interface obrigatória, TTL operacional padrão1, valores reais das VMs não fixados; grupo225.1.1.21:8821 preservado. O receptor TCP de cada jogada aceita publica o mesmo JSON, evitando tentativa rejeitada; decisão de implementação, não exigência textual do professor. Display valida snapshot independente e descarta entradas inválidas sem sair. Não adiciona campos às jogadas nem rastreia sessão/histórico. Regras de vitória reutilizadas por GameMessages; fontes seguem sem comentários, sem generics/lambdas e somente JDK17. Compilação em /tmp/tictactoe-multicast: **passou**, avisos unchecked esperados. Testes manuais executados com lo/TTL0, Registry1199/objeto5121/MatchServiceMulticast, TCP6010/6011: **passou** vitória X (cinco jogadas, dois displays receberam0..4), empate (um display fechado após jogada1, outro recebeu0..8 e empate), partida sem displays com interface inexistente no publicadorX (aviso sem interromper partida, ambos saída0). Display com interface inexistente saiu1 com contexto. Injeção manual via JShell: JSON malformado, objeto sem campos, UTF-8 inválido,16385bytes e tabuleiro inconsistente foram descartados nos dois displays; pacote válido seguinte exibido. Entrada local9 não apareceu no multicast. Todos os processos de teste encerrados. VMs, falha real de envio após abertura, perda/duplicação/reordenação, início tardio e demais falhasTCP/RMI não reexecutados nesta etapa. A ausência de ACK/reenvio multicast não promete entrega. Roteiros17/18. Próximo passo: distribuir fontes atualizados, configurar interface/TTL e verificar integração nas VMs.


**25/10/2026 — comentários removidos por pedido do responsável:** removidos todos os comentários dos fontes Java, incluindo Javadoc, sem alteração da lógica. AGENTS.md atualizado para preservar essa preferência em código futuro. Compilação limpa em /tmp/tictactoe-no-comments com JDK17: **passou**, avisos unchecked esperados. Busca nos fontes confirmou ausência dos delimitadores de comentários. Partidas não reexecutadas nesta mudança exclusivamente textual. Mensagem de commit solicitada deve resumir o conjunto preparado: partida TCP integrada ao RMI, regras separadas, mensagens validadas, timeout/encerramento, sessão em arquivo separado, documentação e fontes sem comentários.

**25/10/2026 — sessão TCP mantida em arquivo separado:** por instrução posterior do responsável, extraída a classe interna para code/TcpGameSession.java. Player volta a chamar a classe separada; lógica preservada. Documentação e distribuição sincronizadas para nove fontes. Compilação limpa em /tmp/tictactoe-separate-session: **passou**, avisos unchecked esperados. Partidas não reexecutadas nesta reorganização. A instrução anterior de classe interna abaixo é apenas histórico, substituída por esta decisão.

**25/10/2026 — sessão TCP movida para Player.java a pedido do responsável:** TcpGameSession agora é classe interna private static de Player; fonte separado removido. Métodos/estado da sessão preservados, apenas imports e recuo ajustados. TicTacToeRules e GameMessages continuam em arquivos separados. Documentação de distribuição atualizada para oito fontes. Compilação limpa em /tmp/tictactoe-player-session com JDK17: **passou**, avisos unchecked esperados. Partidas, falhas e timeout não reexecutados nesta reorganização; resultados anteriores abaixo são da organização anterior. Próxima validação manual: caso16 e partidas do caso14 nas VMs.

**25/10/2026 — partida TCP aprovada e implementada:** responsável respondeu sim à proposta de integração. Player reserva porta antes do RMI e conecta como X quando inicia=true; O aceita. TcpGameSession troca UUID e JSON UTF-8 por linha; GameMessages valida jogada, tabuleiro posterior e resultado do emissor. TicTacToeRules continua separada, com cópia de estado para tentativa/rejeição. Prazo de resposta de 120 s preservado nas correções; fim/confirmacao_fim validam resultado. Entrada humana em thread daemon permite observar falha remota enquanto o usuário pensa. Multicast não implementado. Compilação Java 17 em /tmp/tictactoe-tcp: **passou**, avisos unchecked esperados. Verificações manuais localhost, Registry 1199, objeto 5121, serviço MatchServiceTcp: **passou** vitória X (0/3/1/4/2), vitória O (0/3/1/4/8/5), empate (0/1/2/4/3/5/7/6/8), resultados normais confirmados nos dois terminais e saídas 0; entrada abc, casa 9 e casa ocupada rejeitadas; timeout real de 120 s encerrou ambos com saída 1, inclusive quem aguardava teclado. JShell como adversário: rejeição de X0 e correção X1 conservaram Numero 0; sequência 5 em vez de 1 e tabuleiro divergente foram rejeitados, correção aceita; fechamento remoto durante entrada humana encerrou com saída 1. Diagnóstico inicial JShell falhou por classloader e foi corrigido colocando contexto/carga RMI no mesmo snippet; não era falha do jogo. O pedido abandonado durante essa preparação demonstrou novamente a limitação aprovada de cancelamento do match; servidor de teste reiniciado antes do diagnóstico válido. Todos os processos de teste encerrados. VMs, multicast, UDP, mensagens incompletas/UTF-8 inválido/limite de tamanho em rede, EOF de terminal e expiração após rejeições: **não executados**. Roteiros 14 e 15 em TESTES_MANUAIS.md.


**25/10/2026 — regras em classe separada, por pedido do responsável:** criada `code/TicTacToeRules.java`. Estado privado, cópia defensiva, validações de posição/símbolo/casa ocupada/alternância/partida encerrada, contador somente de jogadas aceitas, identificação de vitória e empate. Nenhuma atribuição de X/O aos participantes ou decisão de wire foi consolidada. Não integrada ao Player. Verificação manual no JShell: estado inicial, posições -1 e 9, símbolo 0, casa ocupada, repetição de símbolo, cópia independente, oito linhas vencedoras com O, vitória com X, rejeição após vitória e empate com nove jogadas: **passou**. Compilação Java 17 em pasta temporária: **passou**, com avisos unchecked dos fontes existentes. RMI/VMs não reexecutados; implementação TCP/multicast não realizada. Roteiro no caso 13 de TESTES_MANUAIS.md.


**25/10/2026 — contrato RMI alterado para String a pedido do responsável:** `MatchService`, `MatchServer`, `Player` e `JsonMessages` passam a receber/retornar JSON textual sem conversão manual de bytes. O RMI faz o marshalling/unmarshalling de String; o utilitário continua criando e validando JSON. A nova decisão substitui a assinatura byte[] anteriormente registrada. O limite defensivo agora usa 16384 unidades UTF-16 de String.length(), em vez de bytes. Recompilar e reiniciar todos os componentes nas VMs; não misturar interfaces antigas e novas. Registrada também a obrigação de sempre sugerir mensagem de commit após alterações, sem executar commit/push automaticamente. Compilação Java 17 para `/tmp/tictactoe-string-rmi`: **passou**, com avisos unchecked esperados. Testes manuais locais com a assinatura String: **passou** pareamento TCP 5010/5011 (ambos recebem endpoint e papel corretos), DUPLICATE para 5012 repetido, INVALID_REQUEST para IP 999.0.0.1 e TIMEOUT após 10 s sem par (redução somente no teste). Registry 1099, objeto 5021, serviço MatchServiceLocal. Textos com acentos preservados. Servidor encerrado. JShell confirmou retorno String na interface, leitura de pedido/sucesso/erro e rejeição de JSON malformado, null, mensagem ausente e comprimento maior que 16384: **passou**. O teste local precisou ser repetido após timeout da revisão automática de permissão; a repetição foi autorizada e executada. Prazo padrão permanece 300 s, sem reexecução completa nesta assinatura; evidências anteriores com byte[] são históricas. Pareamento nas VMs com a versão anterior foi **relatado pelo responsável**, sem saídas anexadas; a nova assinatura exige atualização, recompilação e novo teste nas VMs.

**25/10/2026 — programas separados a pedido do responsável:** substituído `Main.java` por `MatchServer.java` (servidor e getOponent) e `Player.java` (busca RMI e apresentação). Criado `Display.java` somente com entrada reservada que informa recepção multicast ainda não implementada; não abre sockets. Dados internos renomeados para `JsonMessages.PlayerRegistration`, evitando confusão com o programa Player. Políticas e assinatura RMI preservadas. Comandos atualizados; não usar mais os argumentos server/client de Main.

Compilação Java 17 para `/tmp/tictactoe-separated`: **passou**, com avisos unchecked esperados. Teste manual local com MatchServer, Registry 1099, objeto 5021, serviço MatchServiceLocal, Players TCP 5010/5011: **passou**, ambos retornaram os endpoints corretos; primeiro inicia, segundo aguarda. Servidor encerrado. Execução de Display: **passou somente como entrada reservada**, mostrando o aviso previsto; isso não valida multicast. Timeout real de 300 s e demais falhas da etapa anterior não foram reexecutados nesta reorganização; evidências anteriores preservadas abaixo. Fontes apenas em code, sem classes compiladas no projeto.

**25/10/2026 — contrato de match aprovado e aplicado:** o responsável aprovou as políticas D21–D26 após a proposta nesta conversa. Documentação atualizada para remover essas decisões das pendências. `JsonMessages` valida respostas de sucesso/erro e compartilha a leitura UTF-8 entre pedidos e respostas; códigos nomeados como constantes. O cliente mostra endpoint e quem inicia no sucesso, ou código/mensagem no erro com saída 1; falha de comunicação avisa sobre possível resultado desconhecido, sem retry. Nenhum campo ou método novo de cancelamento foi acrescentado. Partida e display continuam fora desta etapa.

Compilação com JDK 17.0.21+9 para `/tmp/tictactoe-approved-match`: **passou**, avisos unchecked esperados. Testes manuais locais: dois pares separados TCP 5010/5011 e UDP 5012/5013, com pedidos de protocolos distintos pendentes simultaneamente; duplicata preservando o pedido TCP; entrada com protocolo http; serviço ausente no Registry; servidor indisponível sem retry: **passou**. Registry 1099, objeto 5021, serviço MatchServiceLocal. Respostas validadas no JShell: sucesso, quatro códigos de erro, rejeição de inicia textual, mensagem ausente e UTF-8 inválido: **passou**. Isso não executa o fluxo real de interrupção da thread.

Desistência: encerrado o cliente TCP 5014 por Ctrl+C; novo registro idêntico recebeu DUPLICATE; cliente 5015 foi pareado ao endpoint do cliente encerrado: **passou quanto à limitação aprovada**, sem comprovar uma partida ou disponibilidade. Servidor desses testes encerrado. Timeout padrão real de **300 s: passou** no Registry 1199, objeto 5121, serviço MatchTimeoutLocal, cliente TCP 6010; recebido TIMEOUT após aproximadamente cinco minutos, saída 1, sem retry. Depois, o mesmo endpoint 6010 entrou novamente e pareou com 6011: ambos retornaram corretamente, 6010 iniciou e não houve DUPLICATE. Servidor isolado encerrado. Testes de carga maior, interrupção real e VMs não executados nesta etapa.

**25/10/2026 — sincronização documental solicitada:** atualizados o mapa de falhas, o quadro de testes, o estado da arquitetura/etapas e o bloco de retomada com as execuções já registradas. Separados resultados da última validação, timeout histórico de 2 s e cenários ainda não executados. Corrigida a referência dos resultados atuais para a seção 9; seção 10 mantida como histórico org.json. Nenhum fonte, contrato ou decisão de protocolo alterado; nenhum teste novo executado. README, AGENTS e roteiro manual apontam para esse registro atual.

**25/10/2026 — aplicação dos padrões ao código autorizada pelo responsável:** `Main` separa inicialização do servidor e pedido do cliente em métodos auxiliares; `getOponent` permanece em `Main`. `JsonMessages` separa a validação da porta e usa construção explícita de mapas. `JsonUtil` separa leitura/escrita de objetos, listas, strings, números e escapes Unicode. Removido pattern matching de instanceof, mantidos casts explícitos e ausência de generics/lambdas/futures. `final` aparece somente em constantes nomeadas. Erros de configuração numérica e de transporte/serviço RMI são tratados especificamente com mensagens em português e saída de processo 1. Falha ao criar ou registrar o serviço libera os objetos exportados. JSONs e regras de pareamento preservados; não foram consolidadas novas decisões de protocolo.

Compilação Java 17 para `/tmp/tictactoe-personal-standards`: **passou**, com avisos unchecked esperados. Verificações manuais: JSON com escapes, Unicode, objetos/listas/números/booleanos/null, rejeição de chaves duplicadas, vírgula final, conteúdo extra e UTF-8 inválido; pedido de jogador válido; par RMI local (TCP 5010/5011, Registry 1099, objeto 5021, serviço MatchServiceLocal), duplicidade; servidor indisponível; portas/timeout não numéricos e timeout excessivo: **passou**. Ambos os clientes pareados retornaram, primeiro inicia=true; servidor encerrado. Timeout padrão de 300 s, interrupção, protocolos distintos, falha de registro e VMs: não reexecutados nesta mudança. Roteiros em `TESTES_MANUAIS.md`, incluindo caso 7. Comandos documentados agora usam `/tmp/tictactoe-classes`; fontes permanecem sozinhos em `code/`.

**25/10/2026 — padrões pessoais documentados:** o responsável forneceu o programa `P3nX` como referência. Adicionada a seção 9 de `AGENTS.md` com estilo imperativo explícito, nomes descritivos, métodos auxiliares simples, comentários/mensagens em português e orientação didática baseada em fundamentos de Java/OO (inferência aproximada: iniciante a intermediário inicial; não avaliação formal). Após consulta, o responsável aprovou correções de flags booleanas, entrada numérica/EOF, retorno de `toLowerCase`, tratamento explícito de exceções, fall-through e duplicação/aninhamento. Também autorizou `final` **somente para constantes nomeadas**, substituindo a proibição total anterior. Nenhum fonte Java alterado; testes não executados por ser atualização documental.

**25/10/2026 — remoção de notação de generics solicitada:** `Main`, `JsonMessages` e `JsonUtil` usam coleções sem parâmetros de tipo e casts explícitos; removidos wildcards e diamond. Compilação Java 17 para `/tmp/tictactoe-no-generics`: **passou**, com avisos esperados de operações unchecked em coleções sem parâmetros de tipo. Verificações manuais: JSON com escapes/lista/valores e rejeição de chaves duplicadas; RMI localhost, Registry 1099, objeto 5021, clientes TCP 5010/5011 e rejeição de duplicidade: **passou**. Primeiro recebeu porta 5011/inicia=true; segundo porta 5010/inicia=false. Servidor encerrado. Timeouts e protocolos distintos não reexecutados nesta alteração; resultados anteriores permanecem históricos. `code/` contém somente os quatro fontes `.java`.

**25/10/2026 — refatoração didática solicitada:** removidos `final`, lambdas, switches com seta, record e `CompletableFuture<byte[]>`. Renomeados variáveis e métodos internos; `PendingMatchRequest` mantém a resposta em `byte[]` e aguarda com `wait`/`notifyAll` no monitor da fila. A assinatura `getOponent` e os campos JSON foram preservados. Compilação com JDK 17 e `-Xlint:all`: **passou, sem avisos**. Testes manuais localhost: pareamento TCP 5010/5011 (ambos retornam, primeiro inicia), duplicidade e timeout reduzido de 2 s: **passou**. Novo registro do mesmo endpoint após expiração: **passou**, recebeu novamente TIMEOUT após 2 s, sem DUPLICATE. Servidores de teste encerrados. JSON: escapes/listas/valores e rejeições de JSON malformado, chaves duplicadas e conteúdo extra: **passou**. Timeout padrão de 300 s, interrupção e carga maior: não executados nesta refatoração.

**25/10/2026 — utilitário JSON solicitado pelo responsável:** adicionado `code/JsonUtil.java`, integrado em `JsonMessages`, removidos JAR e pasta `lib/`. Somente JDK 17. Compilação com JDK local 17.0.21+9: **passou**. Verificação manual no JShell: leitura de pedido, escapes, listas, números, booleanos, null e rejeição de JSON malformado/chaves duplicadas/conteúdo extra/UTF-8 inválido: **passou**. RMI não reexecutado nesta mudança; os resultados antigos abaixo são históricos e não validam o novo utilitário.

**25/10/2026 — remoção de `out` solicitada pelo responsável:** removidos os cinco arquivos `.class` antigos e as pastas vazias. Comandos atualizados para compilar e executar em `code/`. Compilação não executada nesta sessão (`javac` indisponível). `lib/` preservada para a dependência `org.json`.

**25/10/2026 — organização solicitada pelo responsável:** fontes Java diretamente em `code/`, sem subpastas de pacote; declarações de pacote removidas, comandos e referências documentais atualizados. Recompilação após esta mudança: **não executada**, pois `javac` não está disponível no PATH desta sessão. Lógica de match preservada; executar servidor e clientes com `Main`.

**25/10/2026:** importação do servidor de match a pedido do responsável, com lógica em `Main.getOponent`. O responsável determinou manter as decisões anteriores desta pasta. Portanto, preservados: timeout de **300 s** no match, **120 s** durante a partida, `org.json`, compilação direta `javac`/`java`, testes manuais, TCP inicial e UDP futuro.

Não foram importados Maven/Gson/JUnit nem scripts de testes automatizados. A versão `org.json:json:20240303` é apenas candidata para compilar a base importada; a versão definitiva continua pendente. O PDF foi preservado.

O código importa filas TCP/UDP, espera com `wait`/`notifyAll`, FIFO, primeiro inicia, rejeição de duplicidade e JSON de erro (`erro`/`mensagem`, códigos `TIMEOUT`, `DUPLICATE`, `INVALID_REQUEST`, `INTERRUPTED`). Na importação esses detalhes ainda eram propostas. Posteriormente foram aprovadas as políticas da seção 4: sem cancelamento imediato e sem retries automáticos. Os parâmetros locais são exemplos substituíveis; a porta do objeto remoto nas VMs ainda precisa ser escolhida.

## 10. Histórico de validação da base org.json (anterior à substituição)

- Compilação Java 17 com o JAR candidato: **passou no workspace Desktop**, via `javac -encoding UTF-8 -cp lib/json-20240303.jar -d code` e os três arquivos fonte. Usado JDK local 17.0.21+9 para validação; a versão informada nas VMs continua 17.0.20.1.
- **Caso 1 manual: passou.** Servidor iniciado em `127.0.0.1`, Registry `1099`, objeto remoto `5021`, serviço `MatchServiceLocal`, com timeout padrão de **300 s**. Cliente TCP 5010 aguardou; após registrar o cliente 5011, ambos retornaram. O primeiro recebeu porta 5011 e `inicia=true`; o segundo, porta 5010 e `inicia=false`. Servidor encerrado ao final. A expiração dos 300 s não foi testada.
- Casos 2 a 5 de `TESTES_MANUAIS.md`: **não executados nesta versão**. Não declarar validação do IP alternativo nem de falhas com base no protótipo anterior.
- Testes automatizados anteriores do protótipo Gson não contam como evidência desta base.
- Partida TCP, display multicast, timeout de partida de 120 s, VMs e jogador UDP: não executados / não implementados nesta etapa.
- Roteiros: `TESTES_MANUAIS.md`.

## 11. Próximo passo exato

1. Distribuir os fontes conforme README. Nas VMs jogadoras, incluir Display.java, além dos fontesTCP já existentes; um display isolado precisa Display, GameMessages, TicTacToeRules e JsonUtil.
2. Configurar MULTICAST_INTERFACE em cada máquina e verificar TTL/roteamento. Recompilar/recarregar .env conforme bashrc já preparado; repetir casos17/18 entre VMs. Não afirmar que loopback comprova a rede remota.
3. Completar os cenários de robustez não executados. Após integração remota validada, preparar documentação Web e pitch previstos no PDF. Não iniciar jogadorUDP sem pedido específico.

## BLOCO DE RETOMADA (copiar para outro chat)

> 26/10/2026: roteiro atual possui seis partes/10min e cinco slides propostos. Parte5 mapeia código de falhas; parte6 traz comandos, vitória X/O e empate. Edição no Canva com títulos, rótulos e transições documentada. Sem clipe7 e sem novos testes nesta revisão; registrar evidências reais ao gravar.

> 26/10/2026: preparado ROTEIRO_DO_VIDEO.md para sete minivídeos (10min), alternando seis slides propostos, código e testes manuais. Foco em RMI/TCP/multicast e falhas; inclui falas e limites reais. Sem alteração Java, novos testes, deck ou vídeo produzido. Usar como base de ensaio e gravar somente evidências reais; VMs ainda pendentes.

> 26/10/2026: identificação do Player agora usa getLocalHost e ServerSocket(0), sem descoberta pelo Registry. Overrides preservados. Saída TCP vinculada ao IP anunciado para manter validação do adversário. Compilação e partida local com entradas inválidas passaram após corrigir divergência de IP. getLocalHost retornou127.0.1.1; VMs precisam anunciar IP alcançável. Novos testes remotos/timeout/multicast não executados.

> 26/10/2026: Player e TcpGameSession receberam nomes explícitos para Registry, jogador local e adversário. Compilação passou; partidas não reexecutadas. Contratos, descoberta automática de IP/porta e timeouts preservados. Guia sincronizado; próxima validação funcional nas VMs permanece pendente.

> 26/10/2026: nove arquivos Java receberam espaçamento por grupos lógicos, somente inserção de linhas em branco. Comparação com estado anterior e compilação passaram; sem nova partida. Manter esse padrão de leitura, sem reordenar execução. Parser próprio sem JAR, shutdownServer em método e timeout sem teto explícito continuam vigentes. Próximo passo de funcionamento permanece validação nas VMs.

> 26/10/2026: responsável desfez a alteração org.json. JsonUtil voltou ao parser próprio, somente JDK, sem dependência/classpath extra. Guia/README/AGENTS restaurados nessa decisão e DEPENDENCIAS.md da migração removido. MatchServer mantém shutdownServer como método e timeout positivo sem teto explícito. Compilação sem JAR passou; partida não reexecutada nesta reversão. Próximo: validar implantação nas VMs usando os comandos sem biblioteca externa.

> Pedido adicional26/10/2026: retirado limite máximo explícito de MATCH_TIMEOUT_SECONDS. Mantida positividade e padrão300s; conversão impossível para nanossegundos produz erro de configuração tratado. Compilação e erros com0/Long.MAX_VALUE passaram; partidas/VMs não reexecutadas. Mudança acompanha refatoração de ServerShutdown para método shutdownServer. Quatro classes internas restantes analisadas no guia, mantidas por guardarem estado.

> 26/10/2026: ServerShutdown interna removida por pedido do responsável. MatchServer implementa Runnable; hook registra a própria instância e run chama shutdownServer para liberar Registry/serviço. Sem lambdas. Compilação e início/Ctrl+C/reinício nas mesmas portas locais passaram; partidas/VMs não reexecutadas. Quatro classes internas restantes têm estado próprio e foram analisadas, sem alteração: PendingMatchRequest, PlayerRegistration, TerminalLineReader, JsonParser. Guia de apresentação atualizado. Próximo: conferir a implantação e validar nas VMs; não ampliar refatoração silenciosamente.

> 26/10/2026: criado GUIA_DE_APRESENTACAO.md por pedido do responsável. Explica os nove fontes e fornece roteiro de vídeo de10 minutos, falas e demonstração manual. Somente documentação alterada; nenhum teste novo ou mudança Java. Fonte atual mantém descoberta automática de endpoint, TCP por linha e multicast com envelope partidaId/jogada. Material não equivale a vídeo gravado, publicação Web ou validação nas VMs. Próximo: estudar/ensaiar e testar a demonstração remota; não implementar novos módulos sem pedido.

> Alteração vigente mais recente: multicast usa envelope com partidaId e jogada (objeto com os quatro campos originais). Display mostra ID antes de cada tabuleiro, sem menu/filtro/lista persistente. TcpGameSession passa o UUID compartilhado ao publicador. RMI/TCP preservados. Registros abaixo sobre ausência de partidaId no multicast são históricos, substituídos por esta decisão. Compilação e vitóriaX local com cinco snapshots identificados passaram; VMs/partidas simultâneas/envelopes inválidos não testados. Atualizar Display.java e TcpGameSession.java nos jogadores e Display.java em receptores isolados, recompilar e reiniciar juntos; wire multicast antigo incompatível. Próximo: testar entre VMs.

> Ajuste visual mais recente: TcpGameSession.showBoard imprime uma linha em branco antes de cada tabuleiro do jogador para separar os turnos. Compilação passou; partida não reexecutada por esta alteração de apresentação. Próximo passo continua sendo a validação entre VMs.

> Última alteração aprovada: Player descobre IP pela conexão TCP ao Registry e reserva porta automática (bind0) antes do RMI. PLAYER_IP/PLAYER_PORT opcionais; remover valores antigos do .env e usar unset nos terminais existentes. Multicast ainda usa interface explícita. Compilação e vitória X local sem overrides passaram; VMs/falhas/overrides não reexecutados. Próximo: distribuir Player atualizado, recompilar nas VMs e testar alcance das portas dinâmicas. Contratos do jogo e RMI preservados.

> Última reorganização: métodos multicast diretamente em Display.java, sem classe interna e sem MulticastChannel.java. Display implementa AutoCloseable; TcpGameSession usa Display.openPublisher e instancia um publicador, sem executar Display.main. Programa receptor continua independente. Nove fontes. Compilação limpa passou em /tmp/tictactoe-display-methods; partidas completas não reexecutadas nesta movimentação.

> Workspace /home/thiago/Desktop/TicTacToeMulticast; Java17, somente JDK, fontes diretamente em code, sem comentários/Javadoc, generics, lambdas ou futures; final somente constantes. Nove fontes: MatchServer, MatchService, Player, Display, JsonMessages, JsonUtil, TicTacToeRules, GameMessages, TcpGameSession. TcpGameSession fica em arquivo separado por decisão mais recente. Compilar em pasta temporária fora do projeto; javac/java e testes manuais, sem JUnit ou scripts automatizados. Sempre sugerir commit após alterações, sem commit/push automático.
>
> Match RMI String preservado: ip/porta/protocolo -> ip/porta/inicia; mesmos protocolos, FIFO,300s, duplicatas rejeitadas, sem cancelamento imediato nem retries automáticos. Pedido abandonado pode parear até expirar. NN21, servidor.14, Registry9821, serviçoMatchService21; objeto remoto configurado por RMI_OBJECT_PORT. Jogador atual somente TCP; não confundir multicast UDP com implementar jogoUDP.
>
> TCP aprovado: ambos escutam antes do match; inicia=true conecta/X, outro aceita/O; UTF-8 por linha; UUID inicial nas mensagens de controle; Numero0..8, Tabuleiro posterior, Resultado do autor TCP. Rejeição preserva estado/prazo120s; fim status/vencedor e confirmacao_fim. Regras em TicTacToeRules, mensagens em GameMessages, sessão em TcpGameSession. Fontes sem comentários; mensagens em português.
>
> Multicast implementado por pedido da próxima fase: A própria classe Display configura grupo225.1.1.21/porta8821 (ajustáveis para testes), MULTICAST_INTERFACE obrigatório e MULTICAST_TTL0..255 padrão operacional1. Valores das VMs continuam a verificar. Nos testes, lo/TTL0. Ao aceitar jogada TCP válida, receptor publica JSON original uma vez; tentativa rejeitada não publicada. Sem ACK/retries. Falha de configuração/envio multicast gera aviso e desativa publicação; partida continua. Display aceita múltiplas instâncias na mesma porta, valida snapshots e descarta dados inválidos sem encerrar; falha do socket/configuração encerra só Display. Sem partidaId na jogada: snapshots não separam partidas simultâneas, não rastreiam ordem ou histórico, sem garantia de entrega. Não adicionar campos silenciosamente.
>
> Compilação e integração loopback passaram: dois displays receberam vitória0..4; empate0..8 com fechamento de um display; partida sem displays e publicador mal configurado continuou até vitória; Display mal configurado saiu1; JSON/campos/UTF8/tamanho/tabuleiro inválidos descartados, pacote válido seguinte exibido. Todos os processos encerrados. Evidências TCP anteriores (vitóriasX/O, rejeição, desconexão,120s real) permanecem históricas, não reexecutadas integralmente nesta etapa. VMs, falha real de envio, perda/reordenação/duplicação e início tardio não testados. Roteiros17/18; resultados na seção9.
>
> Próximo: atualizar fontes nas VMs conforme README, definir MULTICAST_INTERFACE local e TTL apropriado, recompilar e testar RMI+TCP+displays remotamente. Depois completar falhas e preparar Web/pitch. Não iniciar UDP antecipadamente. Diferença de rejeição por controle em relação ao exemplo do PDF permanece documentada, sem afirmar aprovação do professor.
