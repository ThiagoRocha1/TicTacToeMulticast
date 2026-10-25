# AGENTS.md — Orientações obrigatórias para agentes

Estas instruções se aplicam à raiz de `TicTacToeMulticast` e às subpastas, a qualquer agente que desenvolva, revise, documente ou teste o projeto. **Antes de agir, verifique o estado real do repositório e a autorização do responsável.**

## 1. Estado atual e autorização vigente

- Em 25/10/2026, nesta conversa, o responsável pediu a implementação do servidor de match, o pareamento dentro de `Main` e o uso do workspace `/home/thiago/Desktop/TicTacToeMulticast`.
- Posteriormente, o responsável pediu separar os programas: `MatchServer.java`, `Player.java` e `Display.java`. Isso substitui a organização anterior em `Main`: lógica de match em `MatchServer.getOponent`; `Player` contém a busca RMI existente; `Display` é somente uma entrada reservada, com aviso de funcionalidade ainda não implementada. Essa reorganização originalmente não incluía jogo/multicast; posteriormente o responsável aprovou a partida TCP descrita na seção 5.
- Em seguida, determinou expressamente usar as decisões já existentes nesta pasta. Elas prevalecem sobre as sugestões do protótipo: **300 s para match**, **120 s durante a partida**, **utilitário JSON próprio (pedido posterior substitui org.json)**, `javac`/`java` e **testes manuais**.
- A importação do código foi autorizada. Posteriormente o responsável aprovou as políticas de match da seção 3.1; essas políticas não estão mais pendentes. As decisões TCP foram aprovadas posteriormente, conforme seção 5; o responsável solicitou agora a fase multicast; publicação e Display foram implementados. Validação remota permanece pendente.
- O bloqueio histórico de implementação foi superado para o código de match solicitado nesta conversa. Novos módulos exigem escopo próprio.
- Não importar Maven, Gson, JUnit ou scripts de testes automatizados. Não alterar as decisões anteriores sem pedido do responsável.

## 2. Ler antes de alterar qualquer arquivo

1. Verificar o Git e os arquivos reais (`git status`, árvore do projeto, diffs relevantes). **Não presumir que a pasta local do Windows está montada na sessão de um agente.**
2. Consultar o enunciado `trab-2-rmi-multi-2026-1.pdf`, incluindo todas as páginas e a **Figura 1 — Arquitetura**. Se faltar acesso, informar o bloqueio.
3. Ler `README.md`, `AGENTS.md` e `PLANO_DO_PROJETO.md`.
4. Separar **requisitos expressos no PDF** das **decisões posteriores do responsável**; nunca apresentar uma escolha nossa como texto do professor.
5. Para qualquer divergência com o PDF, documentar a diferença e perguntar antes de concluir que está aceita pelo professor.

**Vigência:** o responsável confirmou que o enunciado segue válido para **2026/1**, apesar do cabeçalho interno `2023/1`.

## 3. Decisões atuais do responsável — não reabrir sem motivo

| Assunto | Decisão vigente |
| --- | --- |
| Conta | `NN = 21` |
| Protocolo inicial | **TCP**; **UDP futuramente** |
| Pareamento | O servidor de match considera TCP/UDP, mas combina somente protocolos iguais |
| Servidor de match | `152.92.236.14` |
| RMIRegistry | `9821` |
| Multicast | Grupo `225.1.1.21`, porta `8821` |
| JDK informado | Java `17.0.20.1` nas VMs |
| Interface RMI | `MatchService`, método `getOponent` com entrada e saída **`String`**, por pedido posterior do responsável |
| Payload | JSON textual em `String` no RMI; transporte/marshalling e unmarshalling feitos pelo RMI, parsing e validação feitos pelo `JsonUtil`/`JsonMessages` |
| JSON | **`JsonUtil`**, somente JDK, por pedido posterior do responsável |
| Tabuleiro | Nove inteiros: `0` vazia, `1` X, `2` O |
| `Numero` | Contador **global da partida**, `0..8`, apenas jogadas aceitas avançam |
| Mensagens | JSON de jogada conforme PDF **mais JSON separado de controle** para rejeições/erros/encerramento |
| Jogada inválida | Rejeitar, informar a causa e exigir refazer sem avançar o contador |
| Identificação de sessão | Cada partida terá `partidaId` **único** nas mensagens JSON de controle; UUID criado pelo iniciador e enviado no controle inicio pelo TCP |
| Fim de partida | Informar status e vencedor (se existir) **aos dois jogadores**, dentro das limitações da rede |
| Encerramento acordado | Eventos JSON de controle `fim` e `confirmacao_fim`; vencedor `1` (X), `2` (O) ou `null` (sem vencedor) |
| Timeout de match | **300 segundos (5 minutos)**; remover registro pendente e informar timeout |
| Timeout de espera de resposta | **120 segundos**; expiração encerra por inatividade |
| Falha durante a partida | Encerrar com **erro** e liberar recursos |
| Interoperabilidade | Entre componentes **do próprio sistema**, sem exigir teste com outros alunos |
| Testes | **Manuais**, usando programas nos terminais; primeiro `localhost`, depois as VMs |

**Decisão atual sobre JSON:** em 25/10/2026 o responsável pediu substituir `org.json` por um utilitário próprio. Usar `JsonUtil` e somente o JDK; não reintroduzir bibliotecas externas sem autorização. Esta decisão substitui a aprovação anterior de `org.json`.

**Assinatura RMI na base importada:**

```java
String getOponent(String jsonIn) throws RemoteException;
```

O servidor só deve efetuar o match de jogadores com o mesmo protocolo; deve retornar aos **dois** participantes, com um `inicia=true` e outro `inicia=false`. Evitar supor que chamadas RMI pendentes podem ser simplesmente descartadas sem tratar os clientes.

**Transporte atualizado:** o responsável pediu explicitamente usar `String` no RMI em vez de byte[]. Recompilar a interface e os componentes em todas as máquinas e reiniciar os processos antigos; não misturar versões. Essa alteração não define framing/codificação dos futuros sockets TCP/UDP. O limite defensivo local passa a ser 16384 unidades de `String.length()` (UTF-16), em vez de bytes.

### 3.1. Políticas de match aprovadas posteriormente

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


## 4. Requisitos do PDF que precisam ser preservados

Há **três programas**:

1. **Servidor de match**: RMI, método `getOponent`; json-in com `ip`, `porta`, `protocolo`; json-ret com `ip`, `porta`, `inicia`.
2. **Jogador**: busca adversário por RMI, joga uma partida diretamente por socket; envia cada jogada aos displays por multicast; quem recebe `inicia=true` começa.
3. **Display**: escuta multicast, mostra tabuleiro, permite múltiplas instâncias; ausência de display não pode impedir o jogo.

A mensagem de jogada usa **exatamente** `Numero`, `Jogada`, `Tabuleiro`, `Resultado`, respeitando capitalização. Índices de casas `0..8`, resultados `"jog"`, `"vit"`, `"der"`, `"emp"`. Não alterar esses campos sem aprovação.

**Atenção à diferença de protocolo:** o PDF sugere devolver a **mesma** mensagem em caso de jogada inválida; o responsável optou por uma **mensagem de controle separada, com motivo e pedido de refazer**. Documentar e, antes de congelar o protocolo de wire, revisar essa diferença para garantir a aderência ao trabalho.

## 5. Pendências que NÃO devem ser inventadas

As decisões de partida TCP anteriormente listadas foram aprovadas expressamente e estão registradas abaixo. Permanecem pendentes portas definitivas nas VMs, configuração de interface/TTL multicast e limitações da rede remota. Não implementar UDP nem reabrir decisões aprovadas sem motivo.

### Protocolo TCP aprovado e implementado em 25/10/2026

O responsável confirmou expressamente a proposta TCP nesta conversa. São escolhas do projeto, complementares ao PDF:

- Ambos reservam PLAYER_IP:PLAYER_PORT antes de chamar RMI. Quem recebe inicia=true conecta e usa X; o outro aceita e usa O. Aceite/conexão limitados a 120 s, sem reconexão automática. O IP da conexão aceita deve coincidir com o adversário retornado pelo match.
- UTF-8, um objeto JSON por linha, no máximo 16384 unidades UTF-16 por mensagem. Codificação inválida, JSON ilegível ou mensagem excessiva encerram por erro; jogada legível com dados inválidos recebe rejeição e pode ser corrigida. A leitura usa um prazo absoluto, inclusive durante mensagens incompletas.
- O iniciador gera um UUID e envia `{"evento":"inicio","partidaId":"UUID"}` antes da primeira jogada. O JSON RMI permanece inalterado.
- Jogadas usam exatamente Numero, Jogada, Tabuleiro, Resultado. Numero é o índice global antes de aplicar a jogada; Tabuleiro é posterior à jogada; Resultado é do emissor. Esta implementação envia jog, vit ou emp; der permanece reservado no vocabulário do professor, mas não representa uma jogada válida própria neste fluxo.
- Rejeição: `{"evento":"rejeicao","partidaId":"UUID","motivo":"texto","refazer":true}`. Tabuleiro e contador preservados; prazo original de 120 s não reinicia. O emissor mantém uma cópia anterior para desfazer a tentativa rejeitada; a próxima jogada válida confirma implicitamente a sua jogada anterior.
- Quem recebe uma jogada conclusiva valida e envia `{"evento":"fim","partidaId":"UUID","status":"vitoria","vencedor":1}`; status empate usa vencedor null. Autor da jogada valida e responde `{"evento":"confirmacao_fim","partidaId":"UUID"}`. Vitória usa vencedor 1/2. Cada espera de confirmação tem limite de 120 s.
- Falha/inatividade: fim com status erro/inatividade, vencedor null e motivo, enviado quando possível; encerrar localmente com saída 1. Não há confirmação obrigatória para erro de rede nem garantia de entrega. Resultado normal confirmado termina com saída 0. EOF do terminal encerra por erro. Uma thread daemon simples lê a entrada enquanto a sessão observa a rede, permitindo detectar desconexão ou fim remoto durante a escolha humana.
- Regras locais em TicTacToeRules; protocolo/validação em GameMessages; troca TCP/terminal em TcpGameSession; Player configura, busca match e abre conexão. Publicação e Display multicast implementados posteriormente, conforme seção 8.

## 6. Regras de engenharia e falhas

- Java 17, com `java.net`, `java.rmi`, `java.util.concurrent` etc., **somente bibliotecas do JDK e o utilitário próprio `JsonUtil`**. Não introduzir frameworks ou outras bibliotecas sem autorização.
- Por pedido do responsável, não usar notação de generics (parâmetros de tipo ou diamond), lambdas, switch com seta nem futures; usar `final` apenas para constantes nomeadas (exceção aprovada posteriormente); preferir coleções sem parâmetros de tipo com casts explícitos, laços, espera sincronizada e nomes descritivos. A assinatura RMI agora usa `String`, por pedido posterior que substitui a decisão anterior de byte[]. Não adicionar conversões manuais de bytes ao contrato RMI. Ao verificar compilação, usar uma pasta temporária fora do projeto para manter somente fontes em `code/`.
- Código simples e didático; sem arquitetura sofisticada, abstrações de conveniência ou padrões avançados sem necessidade.
- **Sem JUnit, Mockito ou testes automatizados.** Testes **manuais** mediante execução dos módulos em terminais. Consultar antes de sugerir ferramentas como `nc` ou `tcpdump`.
- Cada módulo deve ser executável pelo terminal com parâmetros configuráveis. Primeiro demonstrar `localhost`, depois VMs `152.92.236.14`, `.16`, `.17`.
- Tratar em cada módulo: **o que pode falhar / reação esperada / roteiro manual para provocar a falha**.
- Match: fila sincronizada, chamadas pendentes, cancelamento, duplicidade e timeout **fixado em 300 s**; RMI fora do ar e limites de tentativas.
- TCP: desconexão do adversário, timeout de leitura, erro de mensagem, fechamento limpo; **120 s** esperando resposta do oponente.
- UDP futuro: perda, duplicação, reordenação, ACK/reenvio com limite; usar `Numero` quando aplicável — **não implementar UDP antecipadamente**.
- Mensagens: JSON malformado, campos faltantes, casa fora de `0..8`, ocupada, sequência errada, tabuleiro divergente, jogada fora de turno.
- Display: falhas de multicast não podem derrubar a partida; log simples e tratamento de exceção.
- Sempre fechar sockets/streams e tratar exceções; não inventar garantias de entrega se o outro jogador estiver desconectado.

## 7. Como atuar a cada etapa autorizada

1. Confirmar **etapa/item** e **decisões aprovadas**.
2. Consultar o repositório real, arquivos e diffs; preservar trabalho alheio.
3. Explicar em linguagem didática **o que**, **por quê** e **como testar**; aguardar confirmação antes de escrever código.
4. Modificar somente o necessário, deixar projeto compilável ao concluir etapa de implementação.
5. Fornecer para cada caso de teste: **objetivo**, **pré-requisitos**, **passos numerados com comandos exatos**, **resultado esperado por terminal**, **sinais de falha** e **como provocar falha**.
6. Marcar testes no plano **passou / falhou / não executado**, apenas com base na execução real.
7. Ao final de cada etapa ou quando o responsável pedir **“salvar progresso”**, entregar `PLANO_DO_PROJETO.md` atualizado **completo** e um **BLOCO DE RETOMADA** autocontido.
8. Em outro chat, ao receber o plano ou bloco, resumir o estado em **três linhas**, confirmar e prosseguir somente na etapa aprovada.

**Mensagens de commit:** após qualquer alteração, sempre sugerir uma mensagem de commit no encerramento. Sugerir não autoriza executar commit ou push.

**Política Git:** não executar `reset --hard`, `clean -fd`, `commit`, `push`, `merge`, `rebase` nem reescrever histórico sem pedido específico. Nunca descartar alterações do usuário ou de outros agentes.

## 8. Estado atual e próxima ação

- Pedido mais recente: métodos multicast diretamente na classe Display, sem classe interna ou fonte MulticastChannel separado. TcpGameSession usa Display.openPublisher e uma instância própria de Display para publicar; executar o programa Display continua opcional para a partida.

- Decisão mais recente do responsável: manter TcpGameSession no arquivo separado code/TcpGameSession.java. Esta decisão substitui a reorganização anterior como classe interna. Regras permanecem em TicTacToeRules.java.

- Nove fontes diretamente em code: MatchServer, MatchService, Player, Display, JsonMessages, JsonUtil, TicTacToeRules, GameMessages e TcpGameSession. Somente JDK 17; RMI usa String; nenhuma biblioteca externa.
- Match preservado. Player agora joga uma partida TCP após RMI; não aceita protocolo UDP. Regras separadas, sem JSON/rede na classe TicTacToeRules. Display recebe e valida snapshots multicast.
- Verificações manuais localhost: vitória X, vitória O e empate com resultado nos dois terminais; entrada inválida, casa ocupada/fora do intervalo; rejeição remota com rollback e repetição de Numero; sequência/tabuleiro divergentes rejeitados e correção aceita; desconexão durante entrada humana; prazo real de 120 s com encerramento dos dois jogadores. Compilação passou com avisos unchecked esperados.
- Multicast verificado manualmente em loopback: dois displays, vitória, empate, fechamento de display sem afetar jogo, partida sem display com configuração inválida de publicador, descarte de JSON/campos/UTF-8/tamanho/tabuleiro inválidos e recuperação em pacote válido. VMs, perda/reordenação e falha real de envio não testadas. Próximo passo: validar integração e configuração nas VMs. Roteiros17/18 e resultados no plano.

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

## 9. Estilo do responsável e nível de Java

### 9.1. Base da avaliação e orientação didática

Em 25/10/2026, o responsável forneceu um programa de biblioteca (`P3nX`) como referência de como escreve Java e pediu que os agentes adaptassem o código gerado a esse estilo. As correções abaixo e a exceção de `final` para constantes nomeadas foram **aprovadas expressamente pelo responsável nesta conversa**. O exemplo é referência de estilo; não deve ser importado para o projeto nem tratado como especificação funcional do jogo.

**Nível de referência para explicações: fundamentos de Java e orientação a objetos, aproximadamente iniciante a intermediário inicial.** Esta é uma inferência limitada à amostra, não uma avaliação formal nem uma afirmação de que o responsável desconhece outros recursos. O nível de familiaridade não altera a versão do projeto: continua Java 17.

A amostra demonstra uso de tipos e variáveis explícitos, entrada/saída pelo terminal, conversões numéricas, `if`/`else`, laços `while`, `switch` tradicional, métodos auxiliares `private static`, instanciação e uso de objetos, getters, validação e tratamento de exceções padrão e de domínio. Não há evidência suficiente na amostra para avaliar domínio de concorrência, RMI, generics ou programação funcional. As restrições já solicitadas sobre sintaxe são preferências explícitas do projeto, não prova de desconhecimento.

Ao explicar código, partir desses fundamentos. Explicar recursos de rede, sincronização, casts e bibliotecas do JDK usados no projeto, incluindo o que fazem, por que são necessários e como observar o resultado em um teste manual. Preferir explicações passo a passo e construções conhecidas antes de apresentar recursos mais avançados. Não reduzir correção, validação ou fechamento de recursos para simplificar a explicação.

### 9.2. Padrões de escrita adotados

- Preferir fluxo imperativo explícito: `if`/`else`, laços, variáveis declaradas com seus tipos e `switch` com `case`, `break` e `default`, quando couber. Preservar as restrições da seção 6 sobre generics, lambdas, futures e switch com seta.
- Usar nomes descritivos para classes, métodos, parâmetros e variáveis; classes em PascalCase, métodos e variáveis em camelCase. Preferir nomes que indiquem a ação ou o papel no domínio, evitando abreviações como `b`, `opt` ou `arq` quando escondem o significado. Não renomear campos JSON, métodos RMI ou outros contratos externos por preferência de estilo.
- Usar métodos auxiliares simples para rotinas identificáveis, como leitura, validação, apresentação e operações do domínio. Métodos `private static` são adequados a rotinas que não dependam do estado de uma instância; não transformar todo o projeto em métodos estáticos.
- Para entrada textual no terminal, preferir o padrão familiar `BufferedReader` com `InputStreamReader` quando houver necessidade de leitura interativa. Não adicionar menus ou interação a módulos que não precisem disso.
- Por pedido posterior do responsável, manter os fontes Java sem comentários, incluindo Javadoc. Explicações ficam na documentação e na conversa. Manter mensagens de terminal em português; não impor renomeação em massa dos identificadores existentes, que já misturam idiomas.
- Preservar nomes compreensíveis e organização consistente; o recuo irregular do exemplo não é um padrão a reproduzir. Usar o recuo já consistente no projeto.
- **Permitir `final` somente para constantes nomeadas**, com nomes em MAIUSCULAS_COM_SUBLINHADOS; por exemplo, `final int SAIR = 0;`. Não usar `final` em classes, métodos, parâmetros ou variáveis comuns. Esta exceção substitui a proibição total anterior; ela não exige adicionar constantes a toda operação.

### 9.3. Correções aprovadas para o código gerado

O responsável aprovou as seguintes melhorias em relação à amostra; devem orientar código novo ou alterado dentro do escopo autorizado:

- Usar `boolean` para flags de controle que só precisam de verdadeiro/falso; reservar `Boolean` para situações em que o objeto ou o valor nulo sejam realmente necessários.
- Tratar entrada numérica inválida (`NumberFormatException`) e fim de entrada (`readLine()` retornando `null`). Definir uma reação explícita conforme o módulo, como pedir correção no terminal ou encerrar a operação; não assumir que `IOException` cobre erros de conversão.
- Guardar ou usar o resultado de operações que retornam uma nova `String`. Por exemplo, após verificar que a entrada não é nula, usar `opcao = opcao.toLowerCase();` se a normalização for necessária; chamar apenas `opcao.toLowerCase();` não altera `opcao`. A normalização não autoriza aceitar valores fora do contrato de protocolo.
- Tratar exceções com reação clara e contexto. Preferir os tipos específicos que possam ser tratados naquele ponto. Não criar catches vazios ou um padrão de apenas imprimir uma exceção e continuar sem considerar o estado da operação. Um `catch (Exception ...)` só cabe onde houver uma razão concreta e uma reação definida. Preservar os requisitos existentes de falha e fechamento de recursos.
- Evitar fall-through acidental no `switch`: terminar cada alternativa com `break`, `return` ou `throw`, conforme necessário. Agrupar casos somente quando a execução compartilhada for intencional e clara.
- Extrair blocos repetidos e menus profundamente aninhados para métodos auxiliares simples, mantendo o fluxo legível e sem criar uma arquitetura sofisticada. Não copiar a duplicação entre os caminhos de criar e carregar biblioteca do exemplo.

### 9.4. Como resolver futuras dúvidas sobre o estilo

Se outro padrão observado no exemplo parecer incorreto, ambíguo ou contrariar uma instrução vigente, apresentar o trecho, explicar o problema e perguntar ao responsável **antes de promovê-lo a regra para código gerado**. Não transformar erros ocasionais em preferência nem adotar uma correção de estilo contestável silenciosamente. Seguir as regras já aprovadas enquanto a nova decisão estiver pendente. O pedido inicial autorizou a documentação dos padrões; em seguida, o responsável pediu expressamente atualizar o código existente com esses padrões. Essa refatoração está autorizada, preservando o escopo do match e os contratos vigentes.
