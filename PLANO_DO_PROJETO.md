# PLANO_DO_PROJETO.md — TicTacToeMulticast

> **Registro de continuidade inicial**, preparado em **24/10/2026** porque o responsável pediu para registrar as decisões no repositório. A criação antecipada deste arquivo **não constitui avanço ao item 2 nem conclusão do item 6** da sequência pedagógica.
>
> **Atualização em 25/10/2026:** o responsável solicitou a implementação do match e seu uso neste workspace. A base Java foi inicialmente importada com o método de match dentro de `Main`; por pedido posterior foi separada em `MatchServer`, `Player` e `Display`. Por instrução posterior explícita, prevalecem as decisões anteriores desta pasta: **300 s** para match, **120 s** para resposta durante a partida, **utilitário JSON próprio (pedido posterior substitui org.json)** e testes **manuais**. Depois dessas alterações, o responsável aprovou as políticas de match registradas na seção 4. As pendências de partida e ambiente permanecem abertas.

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
| D05 | JSON textual em String no RMI; transporte feito pelo RMI, interpretação feita pelo utilitário | **Atualizado pelo responsável** | Sem conversão manual de bytes no match; sockets futuros mantêm decisões próprias pendentes |
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
| D18 | Cada partida terá um identificador **`partidaId` único**, incluído nas mensagens JSON de controle | **Aprovado** | Responsável; forma de criação/troca ainda pendente |
| D19 | Protocolo de encerramento com eventos de controle **`fim`** e **`confirmacao_fim`** | **Aprovado** | Responsável; detalhes adicionais de payload ainda pendentes |
| D20 | Campo **`vencedor`** nas mensagens finais: `1` = X, `2` = O, `null` = sem vencedor | **Aprovado** | Responsável |
| D21 | Fila por protocolo em ordem de registro; primeiro inicia=true, segundo false | **Aprovado** | Responsável; não define X/O |
| D22 | Erros JSON com strings erro/mensagem; códigos INVALID_REQUEST, DUPLICATE, TIMEOUT, INTERRUPTED | **Aprovado** | Responsável; texto explicativo em português |
| D23 | Duplicata é IP normalizado + porta + protocolo de pedido pendente; preservar original e prazo | **Aprovado** | Responsável |
| D24 | Sem cancelamento imediato ao encerrar cliente; pedido permanece até match ou timeout e pode parear com cliente encerrado | **Aprovado, com limitação aceita** | Responsável |
| D25 | Sem retries automáticos; novo pedido deliberado após TIMEOUT; resultado possivelmente desconhecido em falha RMI | **Aprovado** | Responsável |
| D26 | Interrupção da espera no servidor remove pedido pendente, restaura flag e retorna INTERRUPTED se possível; falha RMI tratada localmente | **Aprovado** | Responsável |

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

Os JSONs acima são **exemplos ilustrativos**, não resultados de testes. Os eventos `fim` e `confirmacao_fim`, o campo `partidaId` nas mensagens de controle e o campo `vencedor` com valores 1/2/null foram **aprovados**. Outros campos (como `tipo`, `motivo`, `refazer`), o esquema JSON completo e o mecanismo de criação/propagação de `partidaId` ainda dependem de consolidação.

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

Três programas previstos: **Servidor de Match**, **Jogador** e **Display**. As seis fontes estão diretamente em `code/`: `MatchServer`, `Player`, `Display`, `MatchService`, `JsonMessages` e `JsonUtil`. O servidor mantém as filas e a espera sincronizada; Player contém somente a busca RMI. Display é apenas uma entrada reservada com mensagem de funcionalidade pendente. A implementação e as políticas de fila, duplicidade, erros, timeout, desistência e ausência de retries automáticos foram aprovadas pelo responsável. A partida do jogador e a recepção multicast do Display ainda não foram implementadas; seus fluxos e contratos pendentes permanecem abertos.

## 6. Mapa de falhas (validação parcial; demais cenários não executados)

| Módulo / falha | Reação desejada | Como testar futuramente (manual) | Status |
| --- | --- | --- | --- |
| Match: servidor/registry fora do ar | Reportar falha claramente, evitar espera infinita; sem retries automáticos | Não iniciar ou encerrar o servidor e tentar conectar jogadores | **Passou em localhost na última validação**: erro RMI contextualizado e saída 1; VMs não executadas |
| Match: fila sem oponente | Após **300 s** sem par, remover pedido e informar timeout | Iniciar jogador isolado por 5 min; repetir registro após expiração | **String: passou com 10 s**. Prazo real de 300 s e novo registro após expiração passaram antes da mudança de assinatura; repetir na nova versão |
| Match: registro duplicado | Rejeitar duplicata, preservando pedido original e prazo; política aprovada | Registrar duas vezes o mesmo IP, porta e protocolo | **Passou em localhost na última validação**: DUPLICATE; primeiro pedido preservado e depois pareado |
| Match: desistência do cliente | Política aprovada: sem cancelamento imediato; pedido pode parear até ser removido por match ou timeout | Interromper cliente enquanto aguarda o par | **Limitação observada**: duplicata após Ctrl+C e pareamento ao endpoint encerrado; variante de limpeza por timeout de cliente encerrado não executada |
| Match: acessos concorrentes | Parear sem duplicar clientes e retornar para os dois | Iniciar vários jogadores simultaneamente | Par básico de duas chamadas: passou; carga maior não executada |
| Jogador: oponente sem responder | Encerrar por **inatividade após 120 s** de espera de resposta | Deixar oponente sem responder por 120 s | Não executado |
| Jogador: conexão interrompida | Encerrar por erro e liberar recursos; log local | Fechar um jogador durante a partida | Não executado |
| Jogador: JSON inválido / campos faltantes | Rejeitar ou informar erro de protocolo, sem travar | Enviar mensagem malformada no fluxo manual aprovado | Não executado |
| Jogador: casa ocupada / fora de turno / contador incorreto / tabuleiro inconsistente | Rejeitar, justificar ao autor e solicitar correção sem avançar contador | Reproduzir mensagens inválidas em teste manual autorizado | Não executado |
| Display: ausente ou falha multicast | Partida continua; registrar erro de publicação quando detectável | Jogar sem displays; interromper display no meio | Não executado |
| Rede multicast entre VMs | Diagnosticar grupo, interface, TTL, regras de rede; plano B localhost | Rodar display na outra VM; repetir em localhost | Não executado |

**Observação:** os timeouts são **distintos e aprovados**: **300 s** aguardando match no servidor; **120 s** aguardando resposta do adversário durante a partida.

## 7. Plano de etapas (proposta, SEM avanço automático)

| Etapa | Objetivo / aprendizagem | Entregável planejado | Teste manual básico previsto | Situação |
| --- | --- | --- | --- | --- |
| **0** | Requisitos, decisões e ambiguidades | Documentação e contratos acordados | Revisão com responsável | **Em andamento (item 1)** |
| **1** | Regras do jogo e JSON | Lógica do tabuleiro + mensagens | Rodar manualmente e inspecionar saída no terminal | Utilitário JSON e mensagens de match implementados/testados; tabuleiro e mensagens de partida não iniciados |
| **2** | Publicação/recepção multicast | Display simples | Executar display e remetente em localhost | Não iniciada |
| **3** | Comunicação direta | Dois jogadores via TCP local | Jogar partida manual em terminais | Não iniciada |
| **4** | RMI e concorrência | Servidor de match | Iniciar servidor e dois clientes em localhost | Políticas de match aprovadas; validação manual local em andamento |
| **5** | Integração | Match + jogo em localhost | Partida completa, dois jogadores e display | Não iniciada |
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
| Tabuleiro / mensagens de partida | **Não executado** | Gameplay não implementado |
| Socket TCP / partida | **Não executado** | Gameplay não implementado |
| Multicast / display | **Não executado** | Display permanece entrada reservada |
| Jogo por UDP | **Não iniciado / não executado** | Classificação de pedidos UDP não implementa jogo UDP |

## 8. Pendências e perguntas em aberto

1. **Mensagens de controle:** `partidaId`, eventos `fim` / `confirmacao_fim` e `vencedor` 1/2/null já aprovados; ainda definir campos restantes, tratamento de erro/refazer, empate e desconexão; alinhar a alternativa escolhida com o PDF, que sugere devolver a própria mensagem em caso de erro.
2. **Perspectiva de `Resultado`:** `vit`/`der` é do emissor ou do receptor? Como detalhar as mensagens de confirmação e empate? Quem é X/O (a regra `inicia=true` = X foi sugerida, ainda não confirmada)?
3. **Tabuleiro no pacote:** estado anterior ou posterior à `Jogada`? Validar consistência e vez de cada jogador.
4. **Servidor de match:** políticas aprovadas na seção 4; falta concluir os testes manuais e validar configuração nas VMs. Cancelamento imediato e retries automáticos foram excluídos desta versão por decisão do responsável.
5. **Timeout após rejeição:** relógio do turno de 120 s reinicia ou permanece correndo? Proposta anterior era não reiniciar; não foi confirmada.
6. **Formato no socket TCP:** delimitação de mensagens, framing, organização das respostas, encerramento. `JSON por linha` foi proposto, mas não confirmado.
7. **JSON:** dependência externa removida por pedido do responsável; não há mais escolha de versão/JAR pendente.
8. **Ambiente:** portas dos jogadores, anúncio IP de RMI, multicast entre VMs, interface e TTL.
9. **Resiliência:** no match, já aprovado encerrar a tentativa em falha RMI sem retry automático, reconhecendo resultado possivelmente desconhecido. Permanecem as decisões de tolerância a falhas durante a partida e UDP quando sua etapa chegar.
10. **`partidaId`:** decidir como gerar e comunicar entre jogadores, sem acrescentar campos à resposta RMI do professor sem aprovação.

## 9. Última mudança registrada

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

1. Atualizar MatchService e os componentes correspondentes nas três VMs, recompilar e reiniciar com a assinatura String; repetir o pareamento. Validação local String concluída conforme seção 9; resultados anteriores com byte[] são históricos.
2. Revalidar as políticas aprovadas na nova assinatura conforme necessário: prazo completo de 300 s, novo pedido após expiração, protocolos distintos e falhas. Maior concorrência, IP alternativo, interrupção real e limpeza de pedido abandonado ainda precisam de execução própria. Não reabrir decisões aprovadas.
3. Definir a porta do objeto remoto nas VMs e executar os roteiros remotos quando houver acesso disponível. Portas 5021/5121 foram usadas apenas em testes locais. Não antecipar jogo/display.

## BLOCO DE RETOMADA (copiar para outro chat)

> Workspace `/home/thiago/Desktop/TicTacToeMulticast`; enunciado válido para 2026/1. Seis fontes diretamente em `code/`: `MatchServer`, `Player`, `Display`, `MatchService`, `JsonMessages`, `JsonUtil`. `Main.java` foi removido por pedido de separação dos programas. Lógica em `MatchServer.getOponent`, RMI com entrada/saída String contendo JSON; o RMI cuida do transporte e o utilitário faz o parsing. Esta decisão substitui byte[]; não há conversão manual de bytes no match. Somente JDK 17; sem lib/JAR/Maven/Gson/JUnit ou testes automatizados. Sempre sugerir mensagem de commit após alterações, sem executar commit/push automaticamente. Estilo aprovado na seção 9 de AGENTS.md: nomes descritivos, métodos simples, comentários/mensagens em português, sem notação de generics/lambdas/futures; final apenas para constantes nomeadas. Coleções sem parâmetros de tipo usam casts; avisos unchecked são esperados. Manter somente fontes em code; criar `/tmp/tictactoe-classes`, compilar com `javac -encoding UTF-8 -d /tmp/tictactoe-classes code/*.java`, executar com `java -cp /tmp/tictactoe-classes MatchServer`, `java -cp /tmp/tictactoe-classes Player` ou `java -cp /tmp/tictactoe-classes Display` e as variáveis do README.
>
> Políticas de match **aprovadas**: fila por protocolo em ordem de registro, primeiro inicia=true; erros com strings erro/mensagem e códigos INVALID_REQUEST, DUPLICATE, TIMEOUT, INTERRUPTED; duplicata por IP normalizado/porta/protocolo preserva pedido e prazo; timeout padrão de 300 s remove pendência. Sem cancelamento imediato ao fechar cliente: pedido pode parear até expirar, limitação aceita. Sem retries automáticos; usuário pode pedir novamente após TIMEOUT; falha RMI pode deixar resultado desconhecido. Cliente valida respostas e mostra endpoint/iniciador ou erro explicativo, saídas 0/1. Sucesso mantém apenas ip/porta/inicia; nenhum método/campo novo de cancelamento. Essas políticas não estão mais pendentes.
>
> Validação atual String em localhost: compilação, par TCP 5010/5011, duplicidade, IP inválido, timeout reduzido de 10 s, mensagens com acentos e verificações de JSON válido/malformado/null/limite passaram. Servidor de teste encerrado. Prazo padrão continua 300 s; o teste completo e demais resultados anteriores com byte[] permanecem históricos, sem reexecução nessa assinatura. O responsável relatou pareamento anterior nas VMs, sem saídas anexadas; ainda não testado com String. Atualizar a mesma versão de MatchService, recompilar e reiniciar servidor e jogadores em todas as VMs antes de repetir. Gameplay e multicast não implementados. Resultados na seção 9, quadro na seção 7, seção 10 apenas histórico org.json; roteiros atualizados em TESTES_MANUAIS.md.

>
> Parâmetros aprovados: NN=21, servidor 152.92.236.14, Registry 9821, multicast 225.1.1.21:8821, partida 120 s, TCP primeiro e jogo UDP depois. Porta do objeto remoto nas VMs continua pendente; 5021/5121 são exemplos locais. Próximo passo: definir/configurar essa porta e preparar validação nas VMs, completando cenários locais faltantes conforme necessário. Player tem somente a busca RMI e Display somente a entrada reservada; partida TCP e recepção multicast continuam pendentes, sem avançar sem escopo. Continuam pendentes mensagens da partida, criação/troca de partidaId, perspectiva de Resultado, tabuleiro antes/depois e framing TCP, conforme seção 8. Não executar commit/push/merge/rebase ou descartar alterações sem pedido específico.
