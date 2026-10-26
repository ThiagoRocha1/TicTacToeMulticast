# Guia do código e roteiro de apresentação — TicTacToeMulticast

Preparado em 26/10/2026 a partir dos nove fontes Java do repositório. O guia explica a implementação atual; não substitui o enunciado nem transforma decisões do projeto em requisitos do professor.

**Apresentação e vídeo:** a estrutura de slides e o roteiro do vídeo são um único documento, [ROTEIRO_DO_VIDEO.md](ROTEIRO_DO_VIDEO.md), com oito segmentos e oito slides.

**Como usar:** leia as seções 1–5 para entender o sistema e estudar cada segmento da apresentação. As seções 6 e 7 indicam onde estão o roteiro e a demonstração. A seção 8 ajuda a responder perguntas.

## 1. O sistema em uma frase

Dois jogadores encontram um adversário usando RMI, jogam diretamente entre si por TCP e publicam as jogadas aceitas por multicast para os displays.

Há **três programas executáveis e nove arquivos Java**. Ter nove arquivos não significa ter nove servidores: os demais arquivos são classes de apoio carregadas pelos programas.

| Arquivo | Responsabilidade | Tem `main`? |
| --- | --- | --- |
| MatchService.java | Contrato da chamada remota de pareamento | Não |
| MatchServer.java | Inicialização RMI, fila e pareamento | Sim |
| Player.java | Descoberta do endereço, chamada RMI e conexão TCP | Sim |
| TcpGameSession.java | Turnos, entrada humana, mensagens TCP e encerramento | Não |
| TicTacToeRules.java | Estado do tabuleiro e regras do jogo | Não |
| GameMessages.java | Criação e validação de mensagens da partida | Não |
| Display.java | Publicação/recepção multicast e apresentação do tabuleiro | Sim |
| JsonMessages.java | Criação e validação de mensagens do match | Não |
| JsonUtil.java | Conversão entre texto JSON e valores Java | Não |

### As três comunicações

```text
Player A ── RMI ──> MatchServer <── RMI ── Player B
   │                                         │
   └────────── partida direta por TCP ────────┘
   │                                         │
   └──────── publicação multicast ───────────┘
                        │
                 Display 1, 2, ...
```

O servidor de match não recebe as jogadas nem escolhe o vencedor. Cada jogador mantém seu próprio estado e verifica as mensagens do adversário. O Display observa os dados publicados; não participa das decisões da partida.

## 2. Caminho completo de uma partida

1. `MatchServer.main` exporta o serviço RMI e cria o Registry.
2. `Player.main` lê as variáveis de ambiente.
3. Sem `PLAYER_IP`, `InetAddress.getLocalHost()` obtém o IP associado ao nome local. Não abre conexão de descoberta ao Registry.
4. Sem `PLAYER_PORT`, o jogador abre um `ServerSocket` na porta `0`. O sistema operacional reserva uma porta real, obtida por `getLocalPort()`.
5. O jogador registra `ip`, `porta` e `protocolo` usando `MatchService.getOponent`.
6. O primeiro pedido válido de um protocolo espera. Quando chega o segundo, ambos recebem o endereço do adversário. O primeiro recebe `inicia=true`; o segundo recebe `false`.
7. Quem recebe `true` conecta ao socket do outro e usa X. O outro aceita a conexão e usa O.
8. `TcpGameSession` compartilha o UUID da partida em um controle `inicio`.
9. A cada turno, a entrada é validada localmente e a jogada é enviada por TCP. O adversário calcula o estado esperado e valida a mensagem antes de aceitá-la.
10. Quem aceita a jogada recebida publica o snapshot por multicast, com o ID da partida. Tentativas rejeitadas não são publicadas.
11. O Display valida a mensagem e mostra ID, número da jogada e tabuleiro.
12. Uma jogada conclusiva gera a troca `fim` / `confirmacao_fim`. Os jogadores apresentam o resultado e liberam os recursos.

**Por que abrir o socket antes do match?** O endereço anunciado já precisa estar reservado para receber a conexão. Procurar uma porta livre e abri-la só depois criaria uma janela para outro processo ocupá-la.

## 3. Explicação de cada arquivo

### 3.1. MatchService.java — a interface compartilhada

Fonte: [MatchService.java](/home/thiago/Desktop/TicTacToeMulticast/code/MatchService.java).

```java
public interface MatchService extends Remote {
    String getOponent(String requestJson) throws RemoteException;
}
```

A interface declara o que pode ser chamado remotamente. `extends Remote` identifica o contrato RMI. O parâmetro é uma `String` contendo JSON; o retorno também. `RemoteException` representa possíveis falhas da comunicação remota.

Ela precisa estar no servidor e nos jogadores porque ambos precisam conhecer o mesmo contrato. Isso não copia a implementação do servidor para os jogadores. O objeto obtido pelo `lookup` é uma referência remota, normalmente um stub/proxy criado pelo RMI.

**Distinção importante:** o RMI transporta a `String` e faz seu marshalling/unmarshalling. A interpretação de campos como `ip` e `porta` é responsabilidade das classes JSON do projeto. Não existe um arquivo skeleton escrito manualmente neste projeto.

**Fala sugerida:** “Esta interface define a operação remota. O cliente conhece o contrato; a implementação e a fila ficam no servidor.”

### 3.2. MatchServer.java — pareamento e concorrência

Fonte: [MatchServer.java](/home/thiago/Desktop/TicTacToeMulticast/code/MatchServer.java).

**Estado principal:** `requestsByProtocol` guarda uma fila TCP e uma UDP. Cada `PendingMatchRequest` guarda o jogador, o instante de expiração e a resposta que será devolvida. O timeout padrão é 300 segundos.

| Método ou trecho | O que faz |
| --- | --- |
| `main` | Lê configuração e trata falhas de inicialização |
| `startMatchServer` | Define o hostname anunciado, exporta o objeto, cria o Registry e registra o serviço |
| `getOponent` | Valida o pedido, limpa expirados, rejeita duplicidade e forma o par |
| `createTimeoutResponse` | Produz o JSON de timeout |
| `run` / `shutdownServer` / `closeRemoteObject` | Liberam os objetos RMI no encerramento |

`LocateRegistry.createRegistry` cria o catálogo de serviços. `rebind` associa o nome `MatchService21` à referência remota. `UnicastRemoteObject.exportObject` disponibiliza a implementação em uma porta própria. Por isso existem duas portas: Registry para localizar o serviço e objeto remoto para suas chamadas.

`MatchServer` também implementa `Runnable`: a JVM chama `run` na thread registrada como shutdown hook, e esse método delega a limpeza a `shutdownServer`. Não existe mais a classe interna `ServerShutdown`. O Registry fica guardado na instância do servidor. Esse `run` serve apenas ao encerramento; não inicia o atendimento RMI nem altera a interface remota.

`CountDownLatch(1).await()` mantém a execução principal aguardando. O código não faz `countDown`; o encerramento normal por término do processo aciona o shutdown hook.

#### Como `synchronized (matchmakingMonitor)` funciona

O RMI pode atender vários jogadores em threads diferentes. Sem proteção, dois pedidos poderiam modificar a mesma fila ao mesmo tempo. O monitor permite que apenas uma thread por vez execute o trecho protegido.

Se não há adversário, a chamada entra na fila e usa `timedWait`. **A espera libera o monitor**, permitindo que a chamada do próximo jogador entre. Ao encontrar um par, o servidor preenche as duas respostas e chama `notifyAll`. A thread acordada readquire o monitor antes de continuar.

O `while (currentRequest.responseJson == null)` reavalia a condição depois de acordar. Receber uma notificação não significa automaticamente que aquela chamada já tem resposta.

Exemplo: A chega e espera; B chega, retira A da fila, prepara para A o endereço de B com `true` e para B o endereço de A com `false`; B retorna e A acorda para retornar também. A ordem de chegada é considerada na fila de cada protocolo.

A espera usa `System.nanoTime()` para medir duração. O prazo não depende do relógio de calendário da máquina.

**Falhas tratadas:** JSON inválido, registro pendente duplicado, timeout e interrupção da espera. Fechar o cliente não cancela imediatamente o pedido remoto: ele pode ficar na fila até expirar ou ser pareado. Não há retry automático.

**Fala sugerida:** “O ponto central é completar duas chamadas RMI. A primeira espera liberando o monitor; a segunda prepara as duas respostas e acorda quem estava aguardando.”

### 3.3. Player.java — preparação e conexão

Fonte: [Player.java](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java).

O `Player` coordena a entrada no sistema. A lógica detalhada dos turnos fica em `TcpGameSession`.

| Método | Papel |
| --- | --- |
| `main` | Lê o endereço do Registry e apresenta erros de configuração/rede |
| `readLocalPlayerAddress` | Usa `PLAYER_IP` quando fornecido; caso contrário, chama `InetAddress.getLocalHost()` |
| `readLocalPlayerRequestedPort` | Usa a porta configurada ou `0` para atribuição automática |
| `findOpponentAndStartGame` | Reserva a escuta, cria o pedido JSON, procura o serviço, chama RMI e inicia a sessão |
| `openConnectionWithOpponent` | Conecta se inicia; aceita se não inicia; verifica o IP do par aceito |

`LocateRegistry.getRegistry` obtém uma referência ao Registry; `lookup` procura o serviço pelo nome. Depois de receber a resposta do match, o jogador abre a comunicação direta com o oponente.

O `ServerSocket` recebe conexões. O `Socket` representa uma conexão estabelecida. Depois de conectar ou aceitar o par, o socket de escuta é fechado e o socket da partida é usado pela sessão.

O `try` com recursos fecha os sockets ao sair do bloco, inclusive quando ocorre exceção. Conexão e aceite têm limite de 120 segundos.

**Limite da identificação:** o IP vem da resolução do nome da máquina; pode ser loopback, como `127.0.1.1`, inadequado entre VMs. Conferir o IP anunciado e corrigir a resolução local ou usar `PLAYER_IP` se necessário. A descoberta não configura NAT/firewall nem comprova que o adversário alcance aquela porta. Os overrides permitem selecionar endereço e porta em redes que precisem disso.

**Fala sugerida:** “O Player reserva o endereço antes de anunciá-lo, procura o adversário por RMI e passa a conexão TCP pronta para a classe que controla a partida.”

### 3.4. TcpGameSession.java — turnos e ciclo da partida

Fonte: [TcpGameSession.java](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java).

Esta é a classe que organiza a conversa entre os jogadores. Ela mantém o socket, leitores/escritores, regras locais, símbolos, UUID e um publicador multicast.

| Método | Ponto principal |
| --- | --- |
| Construtor | Prepara leitura/escrita UTF-8 e define os símbolos |
| `play` | Organiza a partida e fecha o publicador no `finally` |
| `exchangeSessionIdentifier` | O iniciador cria um UUID e envia o controle `inicio` |
| `playLocalTurn` | Lê a escolha, cria o estado candidato, envia e processa a resposta |
| `receiveOpponentTurn` | Inicia a espera pela primeira jogada do adversário |
| `acceptOpponentMove` | Valida, aceita, publica e trata o fim da partida |
| `readLocalMove` | Trata entrada inválida e observa mensagens/desconexão durante a escolha |
| `readMessage` / `sendMessage` | Lê/escreve um JSON por linha |
| `newResponseDeadline` / `ensureTimeRemaining` | Mantêm prazos absolutos de espera |
| `processRemoteFailure` / `sendFailureWhenPossible` | Tratam encerramentos por erro/inatividade |
| `showBoard` / `showResult` | Mostram tabuleiro e resultado local |

#### Uma jogada é primeiro uma tentativa

Antes de enviar, a sessão guarda o estado anterior e aplica a escolha em uma cópia. Se o adversário rejeitar, restaura o estado anterior e pede correção. A tentativa não consome uma jogada aceita.

Se a próxima resposta for uma jogada válida do adversário, ela também confirma implicitamente a aceitação da jogada anterior. Não existe ACK separado para cada jogada normal. No fim há uma troca específica de confirmação.

#### Por que um JSON por linha?

TCP transporta um fluxo de bytes, não mensagens prontas. Uma escrita pode chegar em partes, e várias escritas podem chegar juntas. A quebra de linha delimita cada objeto. `sendMessage` escreve JSON, escreve `\n` e faz `flush`; `readMessage` acumula até o delimitador.

A leitura rejeita UTF-8 inválido, desconexão, JSON ilegível e mensagens acima de 16384 unidades UTF-16. O prazo absoluto evita reiniciar os 120 segundos a cada pedaço recebido. Uma rejeição também não reinicia o prazo original de correção/resposta.

#### Por que existe `TerminalLineReader`?

`readLine()` pode ficar esperando o usuário pressionar Enter. Uma thread daemon lê o terminal enquanto a sessão observa a rede com esperas curtas. Assim, um erro remoto pode ser percebido sem o usuário digitar algo.

A flag `volatile completed` permite observar a conclusão entre threads. Aqui não há uma thread da partida inteira por jogada: a thread auxiliar tem a função específica de ler uma linha de entrada.

O timeout é de espera de resposta; não é um cronômetro central do jogo. Enquanto uma pessoa escolhe a primeira tentativa do seu turno, o adversário que aguarda sua mensagem pode atingir o timeout e comunicar o encerramento.

#### Como termina

Quem recebe a última jogada valida o resultado, publica o snapshot e envia `fim`. Quem a enviou valida esse controle e devolve `confirmacao_fim`. Vitória usa vencedor `1` ou `2`; empate usa `null`. Erros/inatividade também tentam avisar o outro lado, mas uma rede interrompida pode impedir a entrega.

**Fala sugerida:** “A sessão separa tentativa de jogada aceita, controla os turnos, valida as respostas e confirma o encerramento para que os dois lados apresentem o resultado.”

### 3.5. TicTacToeRules.java — regras sem rede

Fonte: [TicTacToeRules.java](/home/thiago/Desktop/TicTacToeMulticast/code/TicTacToeRules.java).

O estado é um vetor de nove inteiros: `0` vazia, `1` X e `2` O. A classe também guarda o número de jogadas aceitas e o último símbolo que jogou.

`placeMove` verifica símbolo, posição de 0 a 8, partida ainda aberta, casa vazia e alternância de símbolo. Só depois dessas verificações altera o tabuleiro e incrementa o contador.

`hasWinningLine` verifica três linhas, três colunas e duas diagonais; `findLineWinner` compara três posições. `getWinner` identifica X/O ou retorna `0` sem vencedor. `isDraw` exige nove jogadas aceitas sem vitória. `isGameOver` reúne vitória e empate.

O construtor de cópia permite simular uma jogada sem mudar o estado atual. `getBoardCopy` devolve outro vetor, evitando que um chamador altere o tabuleiro privado por referência.

A classe permite o primeiro símbolo informado; é a sessão que define X como iniciador. Não há sockets nem JSON nas regras.

**Fala sugerida:** “As regras ficam independentes da comunicação. Posso calcular o próximo estado e verificar vitória sem depender da rede.”

### 3.6. GameMessages.java — significado das mensagens da partida

Fonte: [GameMessages.java](/home/thiago/Desktop/TicTacToeMulticast/code/GameMessages.java).

O JSON pode estar sintaticamente correto e ainda descrever uma jogada impossível. Esta classe faz a validação do conteúdo.

| Métodos | Responsabilidade |
| --- | --- |
| `readObject`, `readText`, `readInteger` | Exigem os tipos e campos necessários |
| `createMove` | Monta os quatro campos da jogada |
| `validateMove` | Compara número, posição, tabuleiro calculado e resultado |
| `createControl`, `validateControl` | Criam/verificam evento e ID da sessão |
| `createFinish`, `validateFinish` | Criam/verificam encerramento e vencedor |
| `readDisplayBoard` | Valida um snapshot sem depender do histórico do Display |

Na primeira jogada aceita, o contador interno passa de 0 para 1, mas o campo `Numero` é `0`. Por isso `createMove` usa contador menos um. Ao receber, `validateMove` exige o próximo número esperado, cria uma cópia das regras e aplica a posição recebida. Compara as nove casas calculadas com o tabuleiro anunciado. Só retorna o candidato se tudo for coerente.

`readInteger` usa `BigDecimal.intValueExact()` porque os números lidos pelo parser são `BigDecimal`; isso evita aceitar um valor fracionário onde se exige um inteiro.

No Display, `readDisplayBoard` verifica quantidade e valores das casas, contagem de X/O, última casa, vencedores e resultado. Como não acompanha uma sessão TCP, essa validação examina a coerência do snapshot; não prova que ele corresponde a uma partida autêntica.

**Fala sugerida:** “Eu não confio apenas no tabuleiro enviado. Aplico a jogada ao meu estado e comparo o resultado esperado com o recebido.”

### 3.7. Display.java — multicast e apresentação

Fonte: [Display.java](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java).

A mesma classe concentra os métodos multicast, conforme a organização escolhida para o projeto. Ela tem dois usos: o jogador chama `openPublisher`; o programa independente executa `main` para receber e exibir. Criar um publicador não executa o `main` do Display.

| Método | Papel |
| --- | --- |
| Construtor | Valida grupo, porta, interface e TTL |
| `openPublisher` / `openReceiver` | Abrem os sockets para cada uso |
| `openSocket` | Configura compartilhamento da porta, interface, TTL e entrada no grupo |
| `publishMove` | Cria o envelope com ID/jogada e envia o datagrama |
| `receivePacket` / `readPacket` | Recebem e interpretam UTF-8/JSON |
| `main` | Valida envelope e snapshot, descartando datagramas inválidos |
| `showMove` | Mostra linha em branco, ID, jogada, tabuleiro e resultado |
| `close` | Fecha o socket multicast |

O canal padrão é `225.1.1.21:8821`. A interface indica por qual placa de rede operar; nas VMs foi informado `enX0`. O TTL limita o alcance em saltos de roteamento: `1` é o padrão operacional e `0` foi usado nos testes apenas locais.

O receptor configura `SO_REUSEADDR` antes do bind e entra no grupo multicast. Isso permite múltiplas instâncias nas plataformas usadas, sujeito ao comportamento da rede/sistema. O loopback multicast está habilitado para também receber publicações locais.

A publicação ocorre **no jogador que recebeu e validou a jogada TCP**. Portanto, o IP mostrado como publicador pode ser o do adversário de quem fez a jogada. O `partidaId`, e não esse IP, identifica a sessão.

Uma falha de publicação gera aviso e desativa aquele publicador, sem derrubar a partida TCP. No programa receptor, um pacote inválido é descartado e a escuta continua; falha do socket pode encerrar o Display.

**Estado atual para múltiplas partidas:** cada snapshot mostra seu ID. Não há menu, filtro, histórico ou quadro persistente por partida. Atualizações podem aparecer intercaladas; não há controle de ordem/duplicidade no Display.

**Fala sugerida:** “O multicast permite publicar para vários observadores sem esperar uma resposta deles. O ID identifica a partida, mas o Display não interfere no jogo.”

### 3.8. JsonMessages.java — mensagens do pareamento

Fonte: [JsonMessages.java](/home/thiago/Desktop/TicTacToeMulticast/code/JsonMessages.java).

Esta classe trata o contrato do match, enquanto `GameMessages` trata o contrato da partida.

`readPlayerRequest` exige IP numérico, porta válida e protocolo TCP/UDP; `validateNumericIpAddress` normaliza o IP e rejeita endereço genérico/multicast. `PlayerRegistration` guarda os dados já interpretados; `hasSameEndpoint` compara IP, porta e protocolo para detectar duplicidade pendente.

`createOpponentResponse` monta `ip`, `porta` e `inicia`. `createErrorResponse` monta `erro` e `mensagem`. `readMatchResponse` verifica o formato de sucesso ou um dos quatro códigos conhecidos: `TIMEOUT`, `DUPLICATE`, `INVALID_REQUEST`, `INTERRUPTED`. A leitura de objetos aplica o limite de 16384 unidades UTF-16.

**Fala sugerida:** “Esta classe evita espalhar a leitura e a validação dos campos de pareamento pelo servidor e pelo jogador.”

### 3.9. JsonUtil.java — parser e gerador JSON próprios

Fonte: [JsonUtil.java](/home/thiago/Desktop/TicTacToeMulticast/code/JsonUtil.java).

É um utilitário geral, sem regras de jogo ou de rede. `parseJson` transforma texto em valores Java. `toJson` faz o caminho inverso.

| Valor JSON | Representação Java usada |
| --- | --- |
| Objeto | `Map`; parser usa `LinkedHashMap` |
| Array | `List`; parser usa `ArrayList` |
| Número | `BigDecimal` na leitura |
| Texto | `String` |
| Booleano | `Boolean` |
| Nulo | `null` |

A classe interna `JsonParser` mantém o texto e a posição atual. `readValue` escolhe o tipo conforme o próximo caractere; `readObject` e `readArray` chamam novamente a leitura de valores para estruturas aninhadas. Isso é recursão: um objeto pode conter outro objeto.

`readString` trata aspas e escapes; `readUnicodeCharacter` interpreta escapes Unicode; `readNumber` valida a escrita do número. O parser rejeita campos duplicados, separadores inválidos e conteúdo extra após o valor. Há limite de 32 níveis de aninhamento.

Na escrita, `appendJsonValue` escolhe o tratamento pelo tipo Java. Métodos auxiliares escrevem objetos, listas e strings, escapando caracteres especiais.

**Diferença entre os três arquivos JSON:** `JsonUtil` pergunta “este texto é JSON?”; `JsonMessages` pergunta “este é um pedido/resposta de match válido?”; `GameMessages` pergunta “esta jogada/controle é coerente com a partida?”.

**Fala sugerida:** “Usei um utilitário próprio para não depender de bibliotecas externas. Ele converte o formato; as outras classes validam o significado.”

## 4. Mensagens para mostrar no vídeo

### Match por RMI

Pedido ilustrativo; a porta real pode mudar a cada execução:

```json
{"ip":"152.92.236.16","porta":40065,"protocolo":"tcp"}
```

Resposta para um dos jogadores:

```json
{"ip":"152.92.236.17","porta":39135,"inicia":true}
```

### Primeira jogada por TCP

```json
{"Numero":0,"Jogada":0,"Tabuleiro":[1,0,0,0,0,0,0,0,0],"Resultado":"jog"}
```

`Numero` é a sequência global, `Jogada` é a posição e `Tabuleiro` é o estado após aplicar a jogada. `Resultado` usa a perspectiva do autor da jogada: o fluxo atual envia `jog`, `vit` ou `emp`. O vocabulário do enunciado inclui `der`, mas a derrota é apresentada ao humano a partir do estado/encerramento, sem o jogador enviar uma jogada própria com `der`.

### Publicação multicast

```json
{
  "partidaId":"f413abe4-f6fe-4296-838f-f3453ffec617",
  "jogada":{
    "Numero":0,
    "Jogada":0,
    "Tabuleiro":[1,0,0,0,0,0,0,0,0],
    "Resultado":"jog"
  }
}
```

O envelope identifica a partida sem alterar os quatro campos da jogada TCP.

### Rejeição e fim

```json
{"evento":"rejeicao","partidaId":"UUID-da-partida","motivo":"Tabuleiro divergente da jogada","refazer":true}
```

```json
{"evento":"fim","partidaId":"UUID-da-partida","status":"vitoria","vencedor":1}
```

```json
{"evento":"confirmacao_fim","partidaId":"UUID-da-partida"}
```

UUID-da-partida é um marcador explicativo; em execução é usado o UUID real compartilhado no início.

## 5. Requisitos, escolhas e limites para explicar corretamente

**Do enunciado:** três módulos; pareamento por mesmo protocolo e retorno aos dois; partida direta; JSON de jogada com os quatro campos; multicast para múltiplos displays; Registry98NN e multicast225.1.1.NN/88NN; documentação Web e pitch de aproximadamente10 minutos. Cada execução do jogador disputa uma partida.

**Escolhas do projeto:** Java17, TCP para jogar, String no RMI, parser próprio, timeouts300/120s, X para quem inicia, JSON por linha, cópia de estado, controles de rejeição/fim, UUID, descoberta de endpoint e envelope multicast. O enunciado deixa aspectos da sessão a cargo do programador; não descreve todos esses detalhes. O controle separado de rejeição difere do exemplo de devolver a mesma mensagem sugerido pelo PDF; não afirmar aprovação específica do professor para essa diferença.

**Não prometer no vídeo:**

- UDP para jogar: a fila reconhece o protocolo, mas o Player implementa somente TCP. Multicast usa datagramas; isso não implementa a variante de jogo por UDP.
- Entrega garantida aos displays: multicast não tem ACK/reenvio neste sistema. Se o último datagrama se perder, o Display pode não exibir o resultado final.
- Cancelamento imediato de um pedido abandonado no match, reconexão ou retry automático.
- Autenticação pelo UUID ou pela verificação de IP. São identificação/verificação básica; não há protocolo de autenticação implementado.
- Seleção de partidas no Display ou organização persistente de vários tabuleiros.
- Comprovação das VMs por testes localhost. Os registros do projeto ainda deixam a validação remota da versão atual pendente.

Há evidências históricas de testes locais de regras, vitória X/O, empate, falhas e multicast. A última alteração de ID no Display foi verificada com uma vitória X e cinco snapshots identificados. Esses registros não significam que todos os cenários tenham sido repetidos em cada alteração. Este guia foi conferido contra os fontes; não executou novos testes.

### 5.1. Matriz de falhas

Base para o segmento 07 do roteiro e para responder perguntas.

| Falha | Onde detectar | Reação implementada | Limitação |
| --- | --- | --- | --- |
| Pedido malformado | JsonMessages/getOponent | INVALID_REQUEST; não enfileira | Não é autenticação |
| Registro pendente duplicado | getOponent | DUPLICATE; mantém o original | Desistência não cancela imediatamente |
| Ausência de par | timedWait e prazo | Remove pedido e retorna TIMEOUT | Depende de servidor e comunicação ainda funcionarem |
| Registry/serviço indisponível | lookup/getOponent e exceções | Mensagem local, encerra tentativa sem retry | Resultado pode ser desconhecido se a chamada já foi enviada |
| Falha ao conectar/aceitar | connect/accept e prazo | Exceção, fechamento, saída 1 | Não tenta outro oponente automaticamente |
| Adversário fecha conexão | read retorna -1 ou IOException | Erro; aviso remoto quando possível | Não garante detecção instantânea de toda falha de rede |
| Adversário sem resposta | Prazo absoluto e setSoTimeout | Encerra por inatividade | Sem prazo garantido para todas as escritas |
| Jogada inconsistente legível | GameMessages.validateMove | Rejeita; preserva estado e contador | Prazo original não reinicia |
| JSON TCP ilegível/UTF-8 inválido/excessivo | readMessage e decoder | Encerra por erro | Não tenta continuar um fluxo inválido |
| Entrada local inválida | readLocalMove | Pede nova entrada | EOF do terminal encerra por erro |
| Publicador multicast indisponível | openPublisher/publishMove | Aviso e publicação desativada | Displays podem deixar de atualizar |
| Datagrama inválido | Display.main/readPacket | Descarta e segue recebendo | Falha real do socket receptor encerra o Display |
| Perda/reordenação multicast | Sem detecção completa | Próximo snapshot pode melhorar a visualização | Sem ACK, reenvio ou filtro de ordem |
| Queda do MatchServer | Exceções nas chamadas; fila em memória | Clientes podem falhar; nova execução necessária | Sem réplica/persistência; partida já conectada não depende do match |

Outros pontos do código: Player.java:33 (RemoteException sem repetição automática), TcpGameSession.java:134 (rollback com previousRules quando a própria jogada é rejeitada), TcpGameSession.java:229 (observa a rede enquanto o usuário digita), TcpGameSession.java:301 (limite de tamanho), TcpGameSession.java:330 (sendFailureWhenPossible). O enunciado pede tolerância a falhas de forma ampla; não afirmar que estes mecanismos esgotam a exigência.

## 6. Roteiro de 10 minutos

O roteiro do vídeo e a estrutura de slides foram unificados em [ROTEIRO_DO_VIDEO.md](ROTEIRO_DO_VIDEO.md): oito segmentos e oito slides; em cada segmento, tempo, tela (slide ou código) e fala aparecem juntos, na ordem da apresentação. Cada slide corresponde ao segmento de vídeo com o mesmo número.

Ao estudar para cada segmento, use as seções deste guia: segmento 03 → 3.1, 3.2 e 3.8; segmento 04 → 3.3 e 3.4; segmento 05 → 3.5, 3.6 e 3.9; segmento 06 → 3.7; segmento 07 → seção 5 e matriz 5.1.

## 7. Demonstração manual

Os comandos, as sequências de jogadas (vitória X, vitória O, empate) e as cenas de falha estão no segmento 08 de [ROTEIRO_DO_VIDEO.md](ROTEIRO_DO_VIDEO.md), junto com a fala de cada cena. Para as VMs, use o .env já configurado e fontes da mesma versão:

```bash
cd ~/tictactoe
source ~/.bashrc
```

Compile em cada VM com `javac -encoding UTF-8 *.java`. Execute `java -cp . MatchServer` na .14; `java -cp . Display` em terminais das .16/.17; depois `java -cp . Player` em outros terminais dessas duas VMs. Se ainda houver overrides antigos, remova-os do .env e execute `unset PLAYER_IP PLAYER_PORT` antes de iniciar os jogadores. Não use TTL0 na demonstração remota.

## 8. Perguntas que podem aparecer

**Por que RMI e TCP juntos?** RMI atende ao serviço de descoberta; TCP atende à partida direta. O servidor não precisa centralizar todas as jogadas.

**Por que MatchService está nos dois lados?** Porque é o contrato compartilhado. A fila e o código de pareamento ficam no MatchServer.

**O que é `-cp`?** Classpath: onde o Java procura classes compiladas. `-cp .` usa a pasta atual; `-cp /tmp/tictactoe-video` usa a pasta temporária da demonstração.

**Por que existem Map, List e casts?** O JSON contém valores de tipos diferentes. As coleções guardam esses valores e o código verifica o tipo antes do cast. Não usar generics foi uma escolha de estilo do projeto; não é uma limitação do Java17.

**O que é um snapshot?** Uma fotografia completa do tabuleiro naquele instante. Um Display que começa depois pode exibir o próximo snapshot sem reconstruir todas as jogadas anteriores.

**Como distinguir duas partidas?** Pelo partidaId mostrado em cada atualização. Hoje não há escolha de uma partida nem rejeição de snapshots antigos/duplicados.

**Por que o Display recebe bytes se o RMI usa String?** São transportes diferentes. O contrato RMI aceita String; sockets TCP/multicast precisam codificar texto em bytes. A codificação escolhida é UTF-8.

**O contador detecta qualquer fraude?** Não. Ele ajuda a rejeitar sequências erradas; a verificação do tabuleiro detecta inconsistências com o estado local. Isso não equivale a autenticação ou proteção contra todos os ataques.

**Uma falha de rede informa sempre o outro jogador?** Não. O código tenta avisar quando possível; com conexão perdida não pode garantir a entrega. Os prazos permitem encerrar localmente a espera.

**O trabalho exige múltiplas partidas simultâneas?** Não explicitamente. Ele pede múltiplos pedidos de match e permite vários displays. A identificação multicast por partida é uma extensão escolhida no projeto.

**Respostas rápidas para ensaiar**

| Pergunta | Resposta |
| --- | --- |
| Por que não só TCP para tudo? | O trabalho separa funções: RMI no match, socket direto na partida e multicast na exibição |
| Como o servidor devolve duas chamadas? | A primeira espera com timedWait liberando o monitor; a segunda preenche as duas respostas e chama notifyAll |
| Quem escolhe a porta? | O sistema, ao receber 0; o Player mantém o ServerSocket aberto e anuncia getLocalPort |
| TCP já valida a jogada? | Não; entrega dados em ordem. As regras e o protocolo validam o conteúdo |
| Por que cópia do tabuleiro? | Para validar uma tentativa antes de aceitar seu estado e permitir desfazer rejeições |
| Por que rejeição diferente do PDF? | Decisão do projeto: informar o motivo; o PDF sugere devolver a mesma mensagem |
| O que acontece se o match cair? | Chamadas pendentes falham e a fila em memória se perde; partidas TCP já conectadas continuam |
| A partida se recupera de queda? | Não; encerra com erro/inatividade, tenta avisar e fecha recursos |

## 9. Referências e estado deste material

- Fontes atuais: os nove arquivos em `/home/thiago/Desktop/TicTacToeMulticast/code`.
- Enunciado: [PDF do trabalho](/home/thiago/Desktop/TicTacToeMulticast/trab-2-rmi-multi-2026-1.pdf), quatro páginas, incluindo a Figura1. O cabeçalho2023/1 não altera a vigência2026/1 confirmada pelo responsável.
- Decisões e execução real: [PLANO_DO_PROJETO.md](/home/thiago/Desktop/TicTacToeMulticast/PLANO_DO_PROJETO.md).
- Roteiros: [TESTES_MANUAIS.md](/home/thiago/Desktop/TicTacToeMulticast/TESTES_MANUAIS.md).

Este material prepara a explicação e a gravação. Não representa vídeo já gravado, publicação Web concluída ou nova comprovação de funcionamento nas VMs.

## 10. Classes internas encontradas na revisão

A tarefa ServerShutdown foi substituída por shutdownServer no próprio MatchServer. As outras ocorrências guardam dados de uma operação; um método sozinho não substitui esses dados.

| Classe interna restante | Arquivo | Estado e motivo |
| --- | --- | --- |
| PendingMatchRequest | MatchServer.java | Registro do jogador, prazo e resposta enquanto a chamada RMI aguarda |
| PlayerRegistration | JsonMessages.java | IP, porta e protocolo validados, compartilhados com o match |
| TerminalLineReader | TcpGameSession.java | Resultado/erro da leitura e flag de conclusão compartilhados entre threads |
| JsonParser | JsonUtil.java | Texto e posição atual de uma leitura JSON, inclusive chamadas recursivas |

Podem ser extraídas para arquivos próprios se desejado. Mover os campos do parser ou leitor para variáveis estáticas compartilhadas seria inadequado: chamadas simultâneas poderiam interferir umas nas outras.

Atualização de configuração: removido o teste explícito de limite máximo de MATCH_TIMEOUT_SECONDS, mantendo valor positivo e representação como long. Se Duration.toNanos não conseguir representar a duração, a inicialização retorna erro de configuração contextual. Isso é uma limitação da representação numérica, não um novo teto configurado pelo projeto. Padrão continua300s.

A escuta é criada com `new ServerSocket(localPlayerRequestedPort)`, padrão0. A conexão de saída usa bind no IP anunciado antes de connect, mantendo a verificação de identidade por IP mesmo quando o Linux resolve o hostname para127.0.1.1. Isso não autentica o jogador.
