# Apresentação e roteiro do vídeo — TicTacToeMulticast

Documento único para os slides e o vídeo (pitch de ~10 minutos pedido no enunciado). Cada slide corresponde ao segmento de mesmo número. Dentro de cada segmento, os passos estão na ordem de apresentação: **tempo, o que está na tela (slide ou código) e o que falar**, tudo na mesma linha.

Slides prontos (mesma ordem e numeração, fala de cada segmento nas notas): [apresentação no claude.ai](https://claude.ai/artifact/6hTV18v5J5XeGCL5A2kY6T).

Material de estudo (explicação dos arquivos, matriz de falhas, perguntas): [GUIA_DE_APRESENTACAO.md](GUIA_DE_APRESENTACAO.md). Os números de linha foram conferidos em 26/10/2026; se o código mudar, localize pelo nome do método.

## Visão geral

| Nº | Tempo | Segmento | Tela |
| --- | --- | --- | --- |
| 01 | 0:00–0:30 | Jogo da velha distribuído | Slide 1 |
| 02 | 0:30–1:10 | Arquitetura e caminho de uma partida | Slide 2 |
| 03 | 1:10–2:30 | RMI — pareamento | Slide 3 → código |
| 04 | 2:30–4:00 | TCP — conexão e protocolo da partida | Slide 4 → código |
| 05 | 4:00–4:50 | Consistência — regras e validação | Slide 5 → código |
| 06 | 4:50–5:50 | Multicast — vários displays | Slide 6 → código |
| 07 | 5:50–7:30 | Tolerância a falhas | Código → Slide 7 |
| 08 | 7:30–10:00 | Demonstração e conclusão | Slide 8 → terminais |

Cores fixas em todos os slides: **azul = RMI**, **verde = TCP**, **laranja = multicast**, sempre acompanhadas de texto.

**Faixa de título no vídeo:** a faixa inferior esquerda (canto de 0 a ~830 px de largura e ~60 px de altura, em 1920×1080) fica reservada para o rótulo que você vai sobrepor. Os slides deixam essa área livre (conteúdo termina bem acima) e o código na tela dividida também não deve ser movido para lá. Texto sugerido por segmento: 01 Abertura · 02 Arquitetura · 03 RMI · 04 TCP · 05 Consistência · 06 Multicast · 07 Falhas · 08 Demonstração.

## 01 — Jogo da velha distribuído (0:00–0:30)

**Slide 1 — Jogo da velha distribuído: RMI, TCP e multicast**

- MatchServer: encontra o adversário por RMI
- Player: joga diretamente com o outro Player por TCP
- Display: acompanha a partida por multicast
- Java 17, somente JDK; VMs 152.92.236.14, .16 e .17


| Tempo | Tela | Fala |
| --- | --- | --- |
| 0:00–0:30 | Slide 1 | "Meu projeto implementa um jogo da velha distribuído com três programas. O servidor de match encontra dois jogadores compatíveis. Depois do pareamento, os jogadores trocam as jogadas diretamente por TCP. Os displays acompanham a partida por multicast. Cada comunicação tem uma responsabilidade e um tratamento de falhas próprio. Antes do código, vou mostrar o caminho de uma partida." |

## 02 — Arquitetura e caminho de uma partida (0:30–1:10)

**Slide 2 — Arquitetura e caminho de uma partida** (baseado na Figura 1 do enunciado)

```text
Player A ── RMI ──> MatchServer + Registry <── RMI ── Player B
   │                    (VM .14)                       │
   └──────────── partida direta por TCP ───────────────┘
   │                                                   │
   └──────────── publicação multicast ─────────────────┘
                 225.1.1.21:8821 → Display 1, 2, ...
```

1. Player reserva IP e porta
2. getOponent por RMI → adversário e quem inicia
3. Partida direta por TCP
4. Jogada aceita → multicast para os displays
5. Fim confirmado pelos dois jogadores


| Tempo | Tela | Fala |
| --- | --- | --- |
| 0:30–0:50 | Slide 2, diagrama | "O MatchServer fica na VM 14 junto com o Registry RMI. Os Players rodam nas VMs 16 e 17. Primeiro, cada Player reserva um endereço e chama getOponent. Quando há par, os dois recebem o endereço do adversário e um deles recebe a indicação de começar." |
| 0:50–1:10 | Slide 2, passos 3–5 | "A partir daí o servidor sai do caminho: as jogadas vão direto de um Player ao outro por TCP, e cada jogada aceita é publicada por multicast para quantos displays estiverem ouvindo. São nove arquivos Java, mas apenas três programas. Vou começar pelo encontro dos jogadores, que acontece por RMI." |

## 03 — RMI: pareamento (1:10–2:30)

**Slide 3 — RMI: localizar o serviço e encontrar um par** (azul)

- Registry na porta 9821: catálogo de serviços
- MatchService.getOponent(String): pedido e resposta em JSON
- Uma fila por protocolo; só pareia protocolos iguais
- Os dois recebem resposta: um com inicia=true, outro com false
- Sem par em 300 s → TIMEOUT

```json
{"ip":"152.92.236.16","porta":40065,"protocolo":"tcp"}
{"ip":"152.92.236.17","porta":39135,"inicia":true}
```

| Tempo | Tela | Fala |
| --- | --- | --- |
| 1:10–1:25 | Slide 3 | "RMI permite chamar um método que executa em outra máquina. O jogador envia seu IP, porta e protocolo; o servidor devolve o endereço do adversário e quem começa." |
| 1:25–1:35 | [MatchService.java](/home/thiago/Desktop/TicTacToeMulticast/code/MatchService.java): interface inteira | "Os dois lados compartilham esta interface. Ela estende Remote e declara RemoteException para falhas da chamada remota." |
| 1:35–1:47 | [MatchServer.java:149](/home/thiago/Desktop/TicTacToeMulticast/code/MatchServer.java:149), startMatchServer: exportObject, createRegistry, rebind | "O servidor exporta sua implementação, cria o próprio Registry e registra o nome do serviço. Não preciso rodar o rmiregistry separado." |
| 1:47–2:00 | [Player.java:49](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:49), findOpponentAndStartGame: getRegistry, lookup, getOponent | "O jogador procura esse nome no Registry e recebe uma referência remota. A chamada parece local, mas executa no servidor." |
| 2:00–2:15 | [MatchServer.java:56](/home/thiago/Desktop/TicTacToeMulticast/code/MatchServer.java:56), getOponent: escolha da fila e synchronized | "O servidor mantém uma fila por protocolo, protegida por synchronized, porque o RMI atende cada jogador em uma thread diferente." |
| 2:15–2:30 | getOponent: timedWait, formação do par e timeout (linhas 98–109) | "O primeiro jogador espera com timedWait, que libera o monitor. O segundo entra, forma o par, preenche as duas respostas e acorda o primeiro. Se ninguém aparecer em 300 segundos, o pedido sai da fila e recebe TIMEOUT. Com o endereço do adversário, os jogadores passam a conversar diretamente." |

### O que entender para explicar este código

- **Interface compartilhada:** MatchService está no servidor e no jogador porque é o contrato. A implementação e a fila ficam só no MatchServer; o objeto que o jogador recebe do `lookup` é uma referência remota (stub), não uma cópia do servidor.
- **Duas portas:** o Registry (9821 nas VMs) é só um catálogo de nomes. O objeto remoto exportado por `exportObject` atende as chamadas em outra porta, configurada por RMI_OBJECT_PORT.
- **String e JSON:** o RMI transporta a String e faz o marshalling/unmarshalling. JsonUtil transforma o texto em Map; JsonMessages confere se os campos `ip`, `porta` e `protocolo` são válidos.
- **Por que synchronized:** o RMI atende cada chamada em uma thread própria. Sem o monitor, dois pedidos poderiam alterar a fila ao mesmo tempo.
- **Por que timedWait libera o monitor:** se a primeira chamada esperasse segurando o monitor, a segunda nunca entraria para formar o par. Ao formar o par, a segunda chamada preenche as duas respostas e chama `notifyAll`; a primeira acorda e retorna.
- **Por que o `while`:** acordar não garante que aquela chamada já tem resposta; a condição é verificada de novo depois de cada espera.
- **Prazo:** medido com `System.nanoTime()`, a partir do recebimento do pedido válido. Expirou → o pedido sai da fila e recebe TIMEOUT.
- **Erros diferentes:** TIMEOUT, DUPLICATE, INVALID_REQUEST e INTERRUPTED são JSON de erro da aplicação, devolvidos pelo servidor. RemoteException é falha da chamada remota (rede, Registry fora do ar); o cliente não repete automaticamente, porque o pedido pode já ter chegado.
- **Limite:** fechar um Player não cancela na hora o pedido dele; ele sai pelo prazo ou é pareado antes. A fila fica só em memória.

Aprofundar: guia, seções 3.1, 3.2 e 3.8.

## 04 — TCP: conexão e protocolo da partida (2:30–4:00)

**Slide 4 — TCP: uma conexão para os dois sentidos** (verde)

- IP local por getLocalHost; porta livre por ServerSocket(0), reservada antes do match
- inicia=true conecta e joga com X; o outro aceita e joga com O
- UTF-8, um JSON por linha
- Controle inicio com UUID da partida, depois as jogadas
- Conexão, aceite e espera de resposta: até 120 s

```json
{"Numero":0,"Jogada":4,"Tabuleiro":[0,0,0,0,1,0,0,0,0],"Resultado":"jog"}
```

| Tempo | Tela | Fala |
| --- | --- | --- |
| 2:30–2:45 | Slide 4 | "A partida usa uma única conexão TCP para os dois sentidos. A jogada usa exatamente os quatro campos do enunciado: Numero, Jogada, Tabuleiro e Resultado." |
| 2:45–3:05 | [Player.java](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java): readLocalPlayerAddress, new ServerSocket(localPlayerRequestedPort), getLocalPort | "O jogador obtém o IP pelo nome da máquina e abre a escuta na porta zero: o sistema operacional escolhe uma porta livre, e getLocalPort informa qual foi. Reservo antes do match para não anunciar uma porta que outro processo poderia ocupar." |
| 3:05–3:25 | [Player.java:128](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:128), openConnectionWithOpponent: connect com prazo, accept e verificação do IP | "Quem recebeu inicia verdadeiro conecta e joga com X. O outro aceita a conexão e joga com O, conferindo se ela veio do IP informado pelo match." |
| 3:25–3:45 | [TcpGameSession.java](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java): construtor UTF-8, [exchangeSessionIdentifier:90](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:90), [sendMessage:310](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:310) | "A sessão prepara leitura e escrita em UTF-8. O iniciador envia um identificador único da partida. Cada JSON termina com uma quebra de linha, porque TCP é um fluxo de bytes e não separa mensagens sozinho." |
| 3:45–4:00 | [readMessage:277](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:277) e [ensureTimeRemaining:350](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:350) | "A leitura acumula até a quebra de linha e respeita um prazo absoluto de 120 segundos. TCP entrega em ordem, mas não sabe se a jogada respeita o jogo. Essa verificação é nossa." |

### O que entender para explicar este código

- **ServerSocket x Socket:** ServerSocket só espera conexões (`accept`); o Socket devolvido por `accept` ou criado no `connect` é a conexão que envia e recebe. Depois de conectar, a escuta é fechada e a sessão usa só o Socket.
- **Porta 0:** é um pedido para o sistema operacional escolher uma porta livre. `getLocalPort()` diz qual foi; essa é a porta enviada ao match. A escuta fica aberta desde antes da chamada RMI, por isso a porta não pode ser tomada por outro processo.
- **getLocalHost:** devolve o IP associado ao nome da máquina. Pode ser 127.0.1.1, que só serve na mesma máquina; nas VMs o IP mostrado precisa ser alcançável pelo outro jogador (PLAYER_IP permite ajustar).
- **Verificação do IP:** quem aceita confere se a conexão veio do IP que o match informou. Para isso funcionar, quem conecta faz `bind` no IP anunciado antes do `connect`. Isso é uma verificação básica, não autenticação.
- **Três prazos diferentes:** `connect(endereco, prazo)` limita a conexão; `ServerSocket.setSoTimeout` limita o `accept`; `Socket.setSoTimeout` limita cada leitura. `ensureTimeRemaining` recalcula o tempo que falta até um prazo absoluto de 120 s, para que receber pedaços de mensagem não renove a espera.
- **try com recursos:** fecha os sockets ao sair do bloco, inclusive quando ocorre exceção.
- **Por que uma linha por JSON:** TCP entrega um fluxo de bytes; uma escrita pode chegar em partes ou junto com outra. `sendMessage` escreve o JSON, `\n` e faz `flush`; `readMessage` acumula até a quebra de linha. UTF-8 inválido, JSON ilegível ou mensagem acima de 16384 caracteres encerram a sessão.
- **Campos da jogada:** `Numero` é o contador global da partida (começa em 0, só avança com jogada aceita); `Jogada` é a casa 0..8; `Tabuleiro` já inclui a jogada do emissor (0 vazia, 1 X, 2 O); `Resultado` é da perspectiva de quem jogou (`jog`, `vit`, `emp`).
- **Controle inicio:** o iniciador gera um UUID e envia `{"evento":"inicio","partidaId":...}` antes da primeira jogada. Os controles são mensagens separadas das jogadas, escolha do projeto.

Aprofundar: guia, seções 3.3 e 3.4.

## 05 — Consistência: regras e validação (4:00–4:50)

**Slide 5 — Uma jogada só vale depois de verificada** (verde)

- Regras separadas da rede: TicTacToeRules
- Ao receber: Numero esperado, casa válida, tabuleiro calculado = recebido, resultado coerente
- Validação em uma cópia; o estado só muda se tudo conferir
- Inválida → rejeicao com motivo; refazer sem avançar o Numero
- Fim: fim → confirmacao_fim nos dois lados

| JsonUtil | JsonMessages | GameMessages |
| --- | --- | --- |
| "É JSON?" | "É um pedido de match válido?" | "A jogada é coerente com a partida?" |

| Tempo | Tela | Fala |
| --- | --- | --- |
| 4:00–4:12 | Slide 5 | "Cada jogador mantém seu próprio tabuleiro e não confia no tabuleiro que recebe." |
| 4:12–4:27 | [GameMessages.java:76](/home/thiago/Desktop/TicTacToeMulticast/code/GameMessages.java:76), validateMove | "Ao receber uma jogada, confiro o número esperado, aplico a casa em uma cópia das minhas regras e comparo as nove casas com a mensagem, além do resultado informado." |
| 4:27–4:35 | [TicTacToeRules.java:22](/home/thiago/Desktop/TicTacToeMulticast/code/TicTacToeRules.java:22), placeMove; [hasWinningLine:74](/home/thiago/Desktop/TicTacToeMulticast/code/TicTacToeRules.java:74) | "As regras ficam separadas: não conhecem rede nem JSON." |
| 4:35–4:50 | [TcpGameSession.java:169](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:169), acceptOpponentMove: `gameRules = candidateRules` e rejeicao | "Só depois de validar aceito o novo estado. Uma tentativa inválida recebe rejeição com motivo e pode ser refeita sem avançar o contador. A última jogada termina com fim e confirmação, para os dois lados mostrarem o mesmo resultado. Para acompanhar de fora, usamos multicast." |

### O que entender para explicar este código

- **Cópia das regras:** `validateMove` cria uma cópia de TicTacToeRules (construtor de cópia) e aplica a casa recebida nela. Se algo não confere, a cópia é descartada e o estado real fica intacto. Só no fim `gameRules = candidateRules` torna o novo estado oficial.
- **O que é comparado:** número esperado, casa dentro de 0..8 e vazia, as nove casas calculadas contra o `Tabuleiro` recebido e o `Resultado` contra vitória/empate calculados localmente.
- **Numero e contador:** o contador interno conta jogadas aceitas; `Numero` é o valor antes da jogada. Por isso `createMove` usa contador menos um, e a primeira jogada tem Numero 0.
- **placeMove:** verifica símbolo, posição, partida aberta, casa vazia e alternância de X/O antes de alterar o vetor. `hasWinningLine` testa 3 linhas, 3 colunas e 2 diagonais.
- **Rejeição dos dois lados:** quem recebe uma jogada inválida envia `rejeicao` com motivo e `refazer: true`. Quem enviou guarda o estado anterior (`previousRules`), restaura e pede outra casa ao usuário. O contador não avança e o prazo de 120 s não reinicia.
- **Confirmação implícita:** não há ACK por jogada; a próxima jogada válida do adversário confirma que a anterior foi aceita.
- **Encerramento:** quem recebe a jogada final valida e envia `fim` (status `vitoria` com vencedor 1/2, ou `empate` com vencedor null); quem jogou confere e responde `confirmacao_fim`. Assim os dois mostram o mesmo resultado.
- **Três níveis de JSON:** JsonUtil só converte texto ↔ Map/List; JsonMessages valida o pedido/resposta do match; GameMessages valida jogadas e controles. Parser próprio para não depender de bibliotecas externas.
- **Diferença do enunciado:** o PDF sugere devolver a mesma mensagem quando a jogada é errada; a rejeição com motivo é decisão do projeto, não do professor.

Aprofundar: guia, seções 3.5, 3.6 e 3.9.

## 06 — Multicast: uma publicação, vários displays (4:50–5:50)

**Slide 6 — Multicast: uma publicação, vários observadores** (laranja)

- Grupo 225.1.1.21, porta 8821; datagramas UDP
- Publica quem recebeu e validou a jogada: rejeições não aparecem
- Envelope: partidaId + snapshot completo da jogada
- Sem ACK: ausência de Display não impede o jogo
- Datagrama inválido é descartado; escuta continua

```json
{"partidaId":"f413abe4-...","jogada":{"Numero":0,"Jogada":0,"Tabuleiro":[1,0,0,0,0,0,0,0,0],"Resultado":"jog"}}
```

| Tempo | Tela | Fala |
| --- | --- | --- |
| 4:50–5:05 | Slide 6 | "Multicast permite publicar para um grupo com vários receptores. Os displays entram no grupo e recebem datagramas UDP; não abro uma conexão com cada Display." |
| 5:05–5:15 | [TcpGameSession.java:169](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:169), acceptOpponentMove: chamada a publishMove após validar | "Quem recebe e valida a jogada é quem publica, então tentativas rejeitadas não aparecem nos displays." |
| 5:15–5:27 | [Display.java:83](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:83), openSocket: SO_REUSEADDR, interface, TTL, joinGroup | "O socket multicast escolhe a interface de rede, limita o alcance com TTL e entra no grupo. Vários displays podem compartilhar a mesma porta." |
| 5:27–5:39 | [Display.java:104](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:104), publishMove | "O envelope leva o ID da partida e o tabuleiro completo. Um Display que começa depois já mostra o estado atual." |
| 5:39–5:50 | [Display.java:188](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:188), main e [showMove:220](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:220) | "O Display valida cada snapshot, descarta o que for inválido e mostra o ID e o tabuleiro. A partida não espera confirmação; multicast não garante entrega nem ordem. Cada canal tem limites; agora mostro onde o código reage às falhas." |

### O que entender para explicar este código

- **Uma classe, dois usos:** o Player chama `Display.openPublisher` para publicar; o programa Display executa `main` para receber. Criar o publicador não executa o `main`.
- **Grupo e porta:** 225.1.1.21:8821 (225.1.1.NN e 88NN, conforme o enunciado). Quem faz `joinGroup` passa a receber os datagramas enviados para esse grupo.
- **Interface e TTL:** a interface diz por qual placa de rede operar (`lo` nos testes locais, `enX0` nas VMs). TTL limita quantos roteadores o pacote atravessa: 0 fica na máquina, 1 fica na rede local.
- **Vários displays:** `SO_REUSEADDR` é configurado antes do `bind`, permitindo várias instâncias na mesma porta. O loopback multicast fica habilitado para receber na mesma máquina.
- **Quem publica:** o jogador que recebeu e validou a jogada por TCP. Por isso o IP de origem mostrado pode ser o do adversário de quem jogou; quem identifica a partida é o `partidaId`.
- **Snapshot:** cada mensagem tem o tabuleiro completo. Um Display que começa depois, ou que perdeu um pacote, se atualiza com o próximo.
- **Validação no Display:** confere campos, tipos, contagem de X/O, última casa, vencedor e resultado de cada snapshot isolado. JSON, UTF-8 ou tamanho inválido → descarta com aviso e continua. Falha do próprio socket receptor encerra o Display.
- **Falha na publicação:** erro ao abrir ou enviar gera aviso, fecha o publicador e desativa o multicast; a partida TCP continua.
- **Limites:** sem ACK, sem reenvio, sem filtro de ordem ou duplicação. O jogo por UDP não foi implementado: o multicast usa UDP, mas a partida é TCP.

Aprofundar: guia, seção 3.7.

## 07 — Tolerância a falhas (5:50–7:30)

**Slide 7 — Falha → detecção → reação**

| Situação | Reação |
| --- | --- |
| Sem par em 300 s | Remove o pedido e retorna TIMEOUT |
| Pedido inválido ou duplicado | INVALID_REQUEST / DUPLICATE, sem entrar na fila |
| Jogada inválida | Rejeita e pede correção; estado preservado |
| Adversário desconectado ou 120 s sem resposta | Avisa quando possível, encerra e libera recursos |
| Display ausente ou falha de publicação | Partida continua |


| Tempo | Tela | Fala |
| --- | --- | --- |
| 5:50–6:05 | [MatchServer.java:98](/home/thiago/Desktop/TicTacToeMulticast/code/MatchServer.java:98), getOponent, linhas 98–109: remainingNanoseconds, remoção da fila, createTimeoutResponse | "Sem par no prazo, removo o pedido e retorno TIMEOUT." |
| 6:05–6:22 | [TcpGameSession.java:277](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:277), readMessage, linhas 282–290; [ensureTimeRemaining:350](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:350) | "A espera de resposta tem prazo. Leitura com -1 indica que o adversário fechou a conexão." |
| 6:22–6:40 | [TcpGameSession.java:169](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:169), acceptOpponentMove, linhas 178–190 | "Valido em uma cópia. Se a jogada é inválida, rejeito sem aceitar o estado e sem avançar o contador." |
| 6:40–6:57 | [Display.java:104](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:104), publishMove, linhas 122–126; [openPublisher:49](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:49), linhas 57–67 | "Uma falha de publicação desativa o multicast, mas não encerra a partida TCP." |
| 6:57–7:15 | [TcpGameSession.java:55](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:55), play, linhas 74–86; [Player.java:98](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:98), linhas 98–105 | "Se a sessão falha, tento avisar o adversário, fecho os recursos e encerro. O aviso pode não chegar se a rede caiu." |
| 7:15–7:30 | Slide 7 | "Não há reconexão, persistência da fila nem garantia de entrega aos displays. Vou provocar algumas dessas situações nos terminais." |

### O que entender para explicar este código

- **Ideia geral:** cada falha tem três partes — onde é detectada, como o código reage e o que não é garantido. Explique sempre nessa ordem.
- **Linhas 98–109 do MatchServer:** `remainingNanoseconds` é o tempo que falta até o prazo do pedido. Chegando a zero, o pedido é removido da fila e `createTimeoutResponse` monta o JSON de TIMEOUT.
- **readMessage:** antes de cada leitura, `ensureTimeRemaining` calcula o tempo restante e configura `setSoTimeout`; se acabou, lança `SocketTimeoutException` (inatividade). `read()` devolvendo -1 significa que o outro lado fechou a conexão → `EOFException` (erro).
- **acceptOpponentMove x readMessage:** jogada legível mas inválida → `acceptOpponentMove` envia rejeição e o jogo segue. JSON ilegível ou mensagem grande demais → `readMessage` lança IOException e a sessão encerra. A diferença é se dá para continuar com segurança.
- **publishMove/openPublisher:** o `catch` de IOException avisa, fecha e desativa só o publicador; nada é propagado para a sessão TCP.
- **play e Player:** os `catch` de `play` separam inatividade (SocketTimeoutException), erro de comunicação (IOException) e controle inválido (IllegalArgumentException). Cada um chama `sendFailureWhenPossible` com status `inatividade` ou `erro` e relança a exceção. O `finally` fecha o publicador; o try com recursos em Player.java fecha o socket da partida. O processo termina com saída 1.
- **sendFailureWhenPossible:** tenta enviar `fim` com o motivo; se esse envio falhar, só registra "Não foi possível avisar o adversário", e a exceção original continua sendo relançada. Não há garantia de que o aviso chegue.
- **Desconexão durante a digitação:** uma thread auxiliar lê o terminal enquanto a sessão continua observando a rede, por isso o jogador percebe a queda do adversário sem precisar digitar.
- **O que não existe:** reconexão, retomada da partida, persistência ou réplica do MatchServer, ACK/reenvio no multicast. "Tolerância a falhas" aqui significa não travar indefinidamente, não aceitar estado errado e encerrar de forma controlada — não é autenticação nem criptografia.

Aprofundar: guia, seção 5 e matriz 5.1.

## 08 — Demonstração e conclusão (7:30–10:00)

### Antes de gravar (fora do vídeo)

Na raiz do repositório, com JDK 17, portas 1199/5121/8821 livres e multicast local permitido. Comandos somente para localhost.

1. Compilar:

```bash
mkdir -p /tmp/tictactoe-video
javac -encoding UTF-8 -d /tmp/tictactoe-video code/*.java
```

2. Terminal 1 — **Servidor de match** (fica aberto até o fim):

```bash
env RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchVideo MATCH_TIMEOUT_SECONDS=300 java -cp /tmp/tictactoe-video MatchServer
```

3. Terminal 2 — **Display** (fica aberto entre as partidas; opcionalmente um segundo em outro terminal):

```bash
env MULTICAST_GROUP=225.1.1.21 MULTICAST_PORT=8821 MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-video Display
```

4. Comando do **Player**, usado no Terminal 3 (X) e depois no Terminal 4 (O) em cada partida. Inicie o Terminal 4 só depois de aparecer "Buscando adversário por RMI..." no Terminal 3:

```bash
env -u PLAYER_IP -u PLAYER_PORT RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchVideo PLAYER_PROTOCOL=tcp MULTICAST_GROUP=225.1.1.21 MULTICAST_PORT=8821 MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-video Player
```

Casas do tabuleiro (começam em 0). Cada entrada é seguida de Enter no terminal indicado; espere a pergunta de jogada antes da próxima.

```text
0 | 1 | 2
3 | 4 | 5
6 | 7 | 8
```

### 7:30–7:33 — Slide 8

**Slide 8 — Demonstração**: A vitória de X, B vitória de O, C empate, D Display fechado, E adversário desconectado. Etiquetas nos terminais: "Servidor de match", "Display", "Jogador X", "Jogador O".

### 7:33–8:08 — Cena A: vitória de X, derrota de O

Fala: "Primeira partida. No X, digito uma entrada inválida e uma casa fora do intervalo: o terminal pede correção sem enviar nada. Agora X completa a linha superior."

Iniciar Player no Terminal 3, depois no Terminal 4. No X, digitar `abc` e depois `9` na primeira jogada. Em seguida:

| Ordem | Terminal | Digitar |
| --- | --- | --- |
| 1 | X | 0 |
| 2 | O | 3 |
| 3 | X | 1 |
| 4 | O | 4 |
| 5 | X | 2 |

Esperado: X mostra "Partida encerrada: você venceu."; O mostra "Partida encerrada: você perdeu."; Display mostra vitória de X. Fala: "X vence, O perde, e o Display mostra o mesmo resultado."

### 8:08–8:33 — Cena B: vitória de O, derrota de X

Fala: "Nova partida, outro ID. Agora O vence: quem inicia não tem vitória garantida."

Reiniciar Player no Terminal 3, depois no 4. Não reiniciar servidor nem Display.

| Ordem | Terminal | Digitar |
| --- | --- | --- |
| 1 | X | 0 |
| 2 | O | 3 |
| 3 | X | 1 |
| 4 | O | 4 |
| 5 | X | 8 |
| 6 | O | 5 |

Esperado: O vence, X perde, Display mostra vitória de O.

### 8:33–9:08 — Cena C: empate

Fala: "Terceira partida: as nove casas ocupadas e empate nos dois lados e no Display."

Reiniciar os dois Players na mesma ordem.

| Ordem | Terminal | Digitar |
| --- | --- | --- |
| 1 | X | 0 |
| 2 | O | 1 |
| 3 | X | 2 |
| 4 | O | 4 |
| 5 | X | 3 |
| 6 | O | 5 |
| 7 | X | 7 |
| 8 | O | 6 |
| 9 | X | 8 |

Esperado: ambos mostram "Partida encerrada: empate."; Display mostra empate.

### 9:08–9:23 — Cena D: fechar o Display

Fala: "Na quarta partida fecho o Display. A partida continua, porque não espera confirmação."

Reiniciar os dois Players. Jogar X `0`, O `3`. Fechar o Display com Ctrl+C. Jogar X `1`. Esperado: os Players continuam normalmente. Não encerrar esta partida; ela é usada na cena E.

### 9:23–9:40 — Cena E: adversário desconectado

Fala: "Agora encerro o Jogador X. O percebe a desconexão e encerra com erro, sem declarar vitória."

Com O escolhendo a jogada, pressionar Ctrl+C no Terminal 3 (X). Esperado: O detecta a desconexão e encerra com erro. Depois, encerrar o servidor com Ctrl+C.

### 9:40–10:00 — Encerramento

Sem slide: mantenha o último terminal (ou o slide 8) na tela durante a fala final.

Fala: "Vimos os três resultados normais e o comportamento quando o Display some e quando o adversário desconecta. O pareamento usa RMI, a partida usa TCP e a exibição usa multicast, cada um com seu tratamento de falhas. Não há reconexão nem garantia de entrega aos displays, e o jogo por UDP não foi implementado. A próxima etapa é validar o sistema completo nas VMs com a mesma versão dos fontes."

Se a demonstração nas VMs já tiver sido gravada, troque a última frase pelo resultado real observado. Não apresentar testes locais como resultados nas VMs.
