# PLANO_DO_PROJETO.md — TicTacToeMulticast

> **Registro de continuidade inicial**, preparado em **24/10/2026** porque o responsável pediu para registrar as decisões no repositório. A criação antecipada deste arquivo **não constitui avanço ao item 2 nem conclusão do item 6** da sequência pedagógica.
>
> **Atualização em 25/10/2026:** o responsável solicitou a implementação do match e seu uso neste workspace. A base Java foi importada com o método de match dentro de `Main`. Por instrução posterior explícita, prevalecem as decisões anteriores desta pasta: **300 s** para match, **120 s** para resposta durante a partida, **utilitário JSON próprio (pedido posterior substitui org.json)** e testes **manuais**. As pendências anteriores continuam pendentes até consolidação; os detalhes trazidos do protótipo não são novas aprovações.

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
| D04 | Interface planejada `MatchService` com método `getOponent` recebendo e retornando **`byte[]`** | **Aprovado** | Responsável decidiu contra `String` como tipo de transporte |
| D05 | JSON em **UTF-8**, convertido `byte[]` → `String` internamente para interpretação | **Aprovado** | Responsável concordou com a proposta apresentada |
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

**Interface implementada na base importada:**

```java
// Interface MatchService estende java.rmi.Remote.
byte[] getOponent(byte[] jsonIn) throws RemoteException;
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

Três programas previstos: **Servidor de Match**, **Jogador** e **Display**. A base do match já está implementada em `Main`, `MatchService`, `JsonMessages` e `JsonUtil`, diretamente em `code/`, com filas por protocolo e espera sincronizada. A implementação foi autorizada, mas os detalhes importados de fila, duplicidade e erros continuam sujeitos à consolidação do contrato. Jogador e Display ainda não foram implementados; suas estruturas internas e o fluxo completo de controle permanecem pendentes.

## 6. Mapa de falhas (validação parcial; demais cenários não executados)

| Módulo / falha | Reação desejada | Como testar futuramente (manual) | Status |
| --- | --- | --- | --- |
| Match: servidor/registry fora do ar | Reportar falha claramente, evitar espera infinita; limite de retries pendente | Não iniciar ou encerrar o servidor e tentar conectar jogadores | **Passou em localhost na última validação**: erro RMI contextualizado e saída 1; VMs não executadas |
| Match: fila sem oponente | Após **300 s** sem par, remover pedido e informar timeout | Iniciar jogador isolado por 5 min; repetir registro após expiração | **Histórico: passou com 2 s**, inclusive novo registro após expiração; não reexecutado na última refatoração; prazo de 300 s não executado |
| Match: registro duplicado | Comportamento atual rejeita duplicata sem substituir o primeiro pedido; contrato definitivo pendente | Registrar duas vezes o mesmo IP, porta e protocolo | **Passou em localhost na última validação**: DUPLICATE; primeiro pedido preservado e depois pareado |
| Match: desistência do cliente | Política de cancelamento ainda pendente; não prometer remoção imediata ao fechar cliente | Interromper cliente enquanto aguarda o par | **Não executado**; cancelamento explícito não implementado |
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
| **4** | RMI e concorrência | Servidor de match | Iniciar servidor e dois clientes em localhost | Base importada por solicitação; pendências de contrato preservadas |
| **5** | Integração | Match + jogo em localhost | Partida completa, dois jogadores e display | Não iniciada |
| **6** | Robustez | Timeouts, validações, tratamentos de falha | Cenários do mapa de falhas | Validações selecionadas do match executadas; demais módulos e cenários não iniciados |
| **7** | Execução remota | Implantação nas três VMs | Match, partida e displays entre VMs | Não iniciada |
| **8** | Interoperabilidade interna | Compatibilidade entre módulos do projeto | Testes cruzados de mensagens e módulos próprios | Não iniciada |
| **9** | Segundo protocolo (posterior) | Suporte UDP, se autorizado na ocasião | Perda/duplicação/reordenação simuladas manualmente | Não iniciada |
| **10** | Entrega | Página Web, documentação e pitch | Abrir a página na VM `.14` e revisar pitch | Não iniciada |

A divisão acima é **proposta de desenvolvimento**, não a sequência de itens didáticos já percorrida. Cada etapa futura deverá registrar objetivo, o que aprender, critérios de pronto, mapa de falhas, roteiro de teste manual e resultados de fato.

### Quadro de resultados de testes

Este quadro resume as execuções registradas na seção 9. **Passou** vale somente para o cenário observado; não significa validação completa do módulo. A seção 10 preserva o histórico anterior com org.json. Nenhum teste novo foi executado durante esta sincronização documental.

| Grupo | Resultado observado | Pendente / não executado |
| --- | --- | --- |
| Compilação | **Passou na última validação**, JDK 17; avisos unchecked esperados | Execução/compilação nas VMs |
| Utilitário JSON / mensagens de match | **Passou na última validação**: leitura/escrita com escapes, Unicode, objetos, listas, números, booleanos e null; pedido válido; rejeição de chaves duplicadas, vírgula final, conteúdo extra e UTF-8 inválido | Outros limites de entrada/aninhamento e demais mensagens inválidas não registrados como executados |
| Tabuleiro / mensagens de partida | **Não executado** | Módulo do jogo ainda não implementado |
| RMI / pareamento | **Passou na última validação**: dois clientes TCP localhost, endpoints corretos, primeiro inicia=true e segundo=false | Protocolos distintos, IP alternativo, carga maior e interrupção não executados nesta base atual |
| Falhas do match / configuração | **Passou na última validação**: duplicidade, servidor indisponível, porta/timeout não numéricos e timeout excessivo | Falha de registro/exportação, serviço ausente no Registry e desistência do cliente não executados |
| Timeout do match | **Histórico: passou com 2 s**, incluindo novo registro após expiração, antes das últimas refatorações | Não reexecutado na última validação; prazo padrão de **300 s não executado** |
| Socket TCP / partida | **Não executado** | Todos os cenários; módulo não implementado |
| Multicast / display | **Não executado** | Todos os cenários; módulo não implementado |
| Três VMs | **Não executado** | Todos os cenários |
| Jogo por UDP | **Não iniciado / não executado** | A classificação TCP/UDP da fila não implementa jogo UDP |

## 8. Pendências e perguntas em aberto

1. **Mensagens de controle:** `partidaId`, eventos `fim` / `confirmacao_fim` e `vencedor` 1/2/null já aprovados; ainda definir campos restantes, tratamento de erro/refazer, empate e desconexão; alinhar a alternativa escolhida com o PDF, que sugere devolver a própria mensagem em caso de erro.
2. **Perspectiva de `Resultado`:** `vit`/`der` é do emissor ou do receptor? Como detalhar as mensagens de confirmação e empate? Quem é X/O (a regra `inicia=true` = X foi sugerida, ainda não confirmada)?
3. **Tabuleiro no pacote:** estado anterior ou posterior à `Jogada`? Validar consistência e vez de cada jogador.
4. **Servidor de match:** timeout de **300 s já definido**; pendentes formato da resposta de timeout no RMI, cancelamento de pedido, jogador duplicado, fila e duas chamadas pendentes.
5. **Timeout após rejeição:** relógio do turno de 120 s reinicia ou permanece correndo? Proposta anterior era não reiniciar; não foi confirmada.
6. **Formato no socket TCP:** delimitação de mensagens, framing, organização das respostas, encerramento. `JSON por linha` foi proposto, mas não confirmado.
7. **JSON:** dependência externa removida por pedido do responsável; não há mais escolha de versão/JAR pendente.
8. **Ambiente:** portas dos jogadores, anúncio IP de RMI, multicast entre VMs, interface e TTL.
9. **Resiliência:** política de tentativas RMI, falha em match e tratamento UDP quando sua etapa chegar.
10. **`partidaId`:** decidir como gerar e comunicar entre jogadores, sem acrescentar campos à resposta RMI do professor sem aprovação.

## 9. Última mudança registrada

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

O código importa filas TCP/UDP, espera com `wait`/`notifyAll`, FIFO, primeiro inicia, rejeição de duplicidade e JSON de erro (`erro`/`mensagem`, códigos `TIMEOUT`, `DUPLICATE`, `INVALID_REQUEST`, `INTERRUPTED`). Esses detalhes são comportamentos da base importada e devem ser revisados, sem marcar as pendências anteriores de contrato como aprovadas. Cancelamento explícito e retries continuam pendentes. Os parâmetros locais são exemplos substituíveis; a porta do objeto remoto nas VMs ainda precisa ser escolhida.

## 10. Histórico de validação da base org.json (anterior à substituição)

- Compilação Java 17 com o JAR candidato: **passou no workspace Desktop**, via `javac -encoding UTF-8 -cp lib/json-20240303.jar -d code` e os três arquivos fonte. Usado JDK local 17.0.21+9 para validação; a versão informada nas VMs continua 17.0.20.1.
- **Caso 1 manual: passou.** Servidor iniciado em `127.0.0.1`, Registry `1099`, objeto remoto `5021`, serviço `MatchServiceLocal`, com timeout padrão de **300 s**. Cliente TCP 5010 aguardou; após registrar o cliente 5011, ambos retornaram. O primeiro recebeu porta 5011 e `inicia=true`; o segundo, porta 5010 e `inicia=false`. Servidor encerrado ao final. A expiração dos 300 s não foi testada.
- Casos 2 a 5 de `TESTES_MANUAIS.md`: **não executados nesta versão**. Não declarar validação do IP alternativo nem de falhas com base no protótipo anterior.
- Testes automatizados anteriores do protótipo Gson não contam como evidência desta base.
- Partida TCP, display multicast, timeout de partida de 120 s, VMs e jogador UDP: não executados / não implementados nesta etapa.
- Roteiros: `TESTES_MANUAIS.md`.

## 11. Próximo passo exato

1. Refatoração aos padrões pessoais, compilação, JSON, par RMI local e falhas selecionadas concluídos; resultados atuais na seção 9. Timeout padrão e demais cenários ainda exigem execução própria, sem reaproveitar evidência histórica.
2. Consolidar as pendências do match já listadas na seção 8 antes de congelar o contrato; não apresentar os detalhes importados como escolhas previamente aprovadas.
3. Definir a porta do objeto remoto e validar nas VMs quando houver ambiente disponível. Não antecipar jogo/display.

## BLOCO DE RETOMADA (copiar para outro chat)

> Workspace `/home/thiago/Desktop/TicTacToeMulticast`; enunciado válido para 2026/1. Base do match implementada em quatro fontes diretamente em `code/`: `Main`, `MatchService`, `JsonMessages`, `JsonUtil`. Lógica de pareamento em `Main.getOponent`, RMI com entrada/saída `byte[]` e JSON UTF-8. Somente JDK 17, sem lib/JAR/Maven/Gson/JUnit ou testes automatizados. Estilo aprovado na seção 9 de AGENTS.md: nomes descritivos, métodos simples, comentários/mensagens em português, sem notação de generics/lambdas/futures; final apenas para constantes nomeadas. Coleções sem parâmetros de tipo usam casts; avisos unchecked são esperados.
>
> Parâmetros aprovados: NN=21, servidor 152.92.236.14, Registry 9821, multicast 225.1.1.21:8821, match 300 s, partida 120 s, TCP primeiro e jogo UDP somente depois. Porta do objeto remoto nas VMs ainda pendente. Para compilar, criar `/tmp/tictactoe-classes` e executar `javac -encoding UTF-8 -d /tmp/tictactoe-classes code/*.java`; executar servidor/cliente com `java -cp /tmp/tictactoe-classes Main server` ou `client`, configurando as variáveis descritas no README. Manter somente fontes em code.
>
> Última validação do código: compilação, JSON, par RMI local TCP 5010/5011, duplicidade, configuração numérica inválida e servidor indisponível **passaram**. Timeout de **2 s** e novo registro após expiração passaram **em uma revisão anterior**, sem reexecução na última validação. Prazo padrão de **300 s nunca testado**. Protocolos distintos, IP alternativo, carga maior, interrupção, cancelamento e VMs continuam sem validação registrada na base atual. Resultados atuais e histórico das refatorações na seção 9; quadro consolidado na seção 7; seção 10 é apenas histórico org.json. Roteiros em TESTES_MANUAIS.md. Esta sincronização documental não executou testes.
>
> Próximo passo: consolidar formato de erros/timeout do match, duplicidade, cancelamento e retries antes de congelar o contrato; completar os testes manuais pendentes e depois validar nas VMs. Comportamentos existentes não equivalem a aprovação definitiva dessas políticas. Jogador, partida TCP, display multicast e jogo UDP ainda não implementados; não avançar sem escopo. Permanecem pendentes os detalhes de mensagens de controle, criação/troca de partidaId e demais decisões listadas na seção 8. Não executar commit/push/merge/rebase nem descartar alterações sem pedido específico.
