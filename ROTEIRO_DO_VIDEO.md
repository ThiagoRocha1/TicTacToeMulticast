# Roteiro de gravação — comunicação e tratamento de falhas

Baseado no código atual em 26/10/2026 e no enunciado trab-2-rmi-multi-2026-1.pdf. O professor pede infraestrutura tolerante a falhas e pitch de aproximadamente 10 minutos. As políticas de 300/120 segundos, controles JSON e ausência de reconexão são decisões deste projeto, não prescrições literais do professor.

Este documento é o roteiro de estudo, fala e edição; os textos de slide abaixo podem ser copiados para sua apresentação. O guia GUIA_DE_APRESENTACAO.md complementa a explicação dos nove arquivos. Não é necessário explicar cada linha no vídeo.

## 1. Organização dos minivídeos

Grave seis clipes independentes. Não há clipe separado de encerramento; os limites ficam na parte5 e a última cena conclui o vídeo. A soma planejada é de 10 minutos, incluindo as demonstrações; ensaie a fala para ajustar o tempo.

| Clipe | Posição no vídeo | Duração | Tela principal | O que o espectador deve entender |
| --- | --- | --- | --- | --- |
| 01 | 0:00–0:40 | 40 s | Slide 1 | Os três programas e o caminho dos dados |
| 02 | 0:40–2:10 | 1 min 30 s | Slide 2 e código RMI | Como dois jogadores encontram um adversário |
| 03 | 2:10–4:00 | 1 min 50 s | Slide 3 e código TCP | Como conectam e trocam jogadas válidas |
| 04 | 4:00–5:20 | 1 min 20 s | Slide 4 e código multicast | Como vários displays recebem sem bloquear o jogo |
| 05 | 5:20–7:20 | 2 min | Slide 5 e código de falhas | Onde detectar, corrigir, encerrar ou continuar |
| 06 | 7:20–10:00 | 2 min 40 s | Terminais | Vitória/derrota, empate e falhas demonstradas |

RMI é uma forma de invocar métodos remotamente; socket é uma API de comunicação. Neste projeto, a partida usa sockets TCP e o multicast usa datagramas UDP. Não são três protocolos do mesmo nível. O JDK também utiliza transporte de rede internamente no RMI; nosso código não controla suas mensagens como controla os sockets da partida.

## 2. Clipe 01 — arquitetura

### Slide 1: Três programas, três responsabilidades

Texto para o slide:

- MatchServer: encontra o adversário por RMI
- Player: joga diretamente com o outro Player por TCP
- Display: recebe atualizações por multicast

Distribuição: MatchServer na VM14; Players nas VMs16/17; Displays opcionais nas VMs dos jogadores.

Para montar o visual, use três conjuntos de nomes e setas: Players → MatchServer para o pareamento; Player ↔ Player para a partida; Players → grupo multicast → Displays para a exibição. Diferencie as comunicações por cor e mantenha os mesmos nomes e cores nos próximos slides.

### Fala-base

“Meu projeto implementa um jogo da velha distribuído. O servidor de match encontra dois jogadores compatíveis. Depois do pareamento, os jogadores trocam as jogadas diretamente por TCP. Os displays acompanham a partida por multicast. Cada comunicação tem uma responsabilidade e um tratamento de falhas próprio.”

Transição: “Vou começar pelo encontro dos jogadores, que acontece por RMI.”

## 3. Clipe 02 — RMI e pareamento

### Slide 2: RMI — localizar o serviço e encontrar um par

Texto para o slide:

- Registry: catálogo de serviços, porta 9821
- MatchService.getOponent(String): pedido e resposta em JSON
- Fila por protocolo; prazo padrão de 300 segundos
- Dois retornos: adversário e indicação de quem começa

O Registry e o objeto remoto são diferentes. O Registry fornece a referência do serviço; a chamada getOponent é executada no MatchServer. A porta do objeto remoto é configurada separadamente por RMI_OBJECT_PORT.

### Mostrar no código, nesta ordem

1. code/MatchService.java: interface Remote e assinatura com RemoteException.
2. code/MatchServer.java, startMatchServer: exportObject, createRegistry e rebind. O programa já inicializa o Registry; não é necessário executar rmiregistry à parte.
3. code/Player.java, findOpponentAndStartGame: LocateRegistry.getRegistry, lookup e getOponent.
4. code/MatchServer.java, getOponent: seleção da fila, synchronized, pareamento, timedWait e retorno TIMEOUT. Mostre blocos pequenos, não o método inteiro em uma tela.

### Fala-base

“RMI permite chamar um método que executa em outra máquina. Os dois lados compartilham a interface MatchService. O servidor exporta sua implementação e registra o nome no Registry. O jogador consulta esse nome e recebe uma referência remota para chamar getOponent.

O pedido contém o IP e a porta de escuta do próprio jogador, além do protocolo. O servidor mantém uma fila por protocolo e só combina jogadores compatíveis. Quando encontra um par, retorna o endereço do adversário para cada um, com inicia verdadeiro para um deles e falso para o outro.

A fila é protegida por synchronized para evitar alterações concorrentes inconsistentes. Enquanto o primeiro jogador espera, timedWait libera o monitor. O segundo consegue entrar, formar o par e acordar quem estava esperando. A chamada é reavaliada em um laço.

Se não aparecer adversário em 300 segundos, o pedido é removido e recebe TIMEOUT. Um pedido duplicado pendente é rejeitado. Se houver falha RMI, o cliente informa o problema e não repete automaticamente, porque o pedido pode ter chegado ao servidor.”

### O que compreender antes de gravar

O RMI transporta a String e cuida do marshalling/unmarshalling. JsonUtil interpreta o texto JSON; não substitui o transporte do RMI. JSON de erro de aplicação, como TIMEOUT, é diferente de RemoteException por falha na chamada.

Fechar um Player não cancela imediatamente seu pedido pendente. O servidor o remove pelo prazo, salvo se houver pareamento antes. Não existe replicação nem persistência da fila. Os 300 segundos limitam a espera de pareamento no servidor, não constituem um timeout universal da comunicação RMI.

Transição: “Com o endereço do adversário, os jogadores passam a conversar diretamente.”

## 4. Clipe 03 — socket TCP e consistência da partida

### Slide 3: TCP — uma conexão para os dois sentidos

Texto para o slide:

- IP local: getLocalHost; porta livre: ServerSocket(0)
- inicia=true conecta e usa X; o outro aceita e usa O
- UTF-8; um JSON por linha
- Validação de sequência, casa, tabuleiro e resultado
- Conexão, aceite e espera de resposta: até 120 segundos

### Mostrar no código

1. code/Player.java: readLocalPlayerAddress, new ServerSocket(localPlayerRequestedPort), getLocalPort e criação do pedido RMI.
2. openConnectionWithOpponent: accept, verificação do IP, bind da saída e connect com timeout.
3. code/TcpGameSession.java: construtor com reader/writer, exchangeSessionIdentifier e sendMessage.
4. code/GameMessages.java: validateMove; code/TicTacToeRules.java: placeMove. Explique a relação entre os dois sem percorrer todas as regras.
5. code/TcpGameSession.java: readMessage e ensureTimeRemaining.

### Fala-base

“O jogador obtém o IP associado ao nome da máquina e reserva uma porta livre antes de procurar um adversário. A porta zero é um pedido ao sistema operacional; getLocalPort informa a porta realmente atribuída. Esses dados são enviados ao match.

Quem recebe inicia verdadeiro conecta ao adversário e joga com X. O outro aguarda em accept e joga com O. O ServerSocket aceita a conexão; o Socket retornado faz a troca de dados. A mesma conexão permite enviar e receber. O socket de saída é associado ao IP anunciado para manter a verificação do adversário.

Depois, o Player entrega a conexão ao TcpGameSession. Ele prepara os streams em UTF-8 e o iniciador envia um identificador de partida. Cada mensagem termina com uma quebra de linha, porque TCP fornece um fluxo de bytes e não preserva os limites dos JSONs.

Cada jogador mantém seu tabuleiro. Ao receber uma jogada, verifica o número esperado, a casa, o tabuleiro resultante e o resultado informado. TCP mantém a ordem dos dados da conexão, mas não sabe se a jogada respeita o jogo. Essa verificação é nossa.

Uma tentativa legível, mas inválida, recebe uma rejeição com motivo. Ela pode ser refeita sem avançar o contador. JSON ilegível ou falha de conexão encerram a sessão. Uma jogada final válida leva à troca dos controles fim e confirmacao_fim.”

### Exemplo que pode aparecer por alguns segundos

```json
{"Numero":0,"Jogada":4,"Tabuleiro":[0,0,0,0,1,0,0,0,0],"Resultado":"jog"}
```

Numero é global da partida; Tabuleiro já inclui a jogada do emissor. As chaves e sua capitalização seguem o contrato. Os controles inicio/rejeicao/fim/confirmacao_fim são mensagens separadas escolhidas pelo projeto.

### O que compreender antes de gravar

getLocalHost pode retornar loopback, como 127.0.1.1. Funciona para um teste na mesma máquina, mas as VMs precisam anunciar endereços alcançáveis entre si. PLAYER_IP e PLAYER_PORT continuam opcionais para ajustes.

connect(endereco, prazo) limita a conexão; ServerSocket.setSoTimeout limita accept; Socket.setSoTimeout limita leituras. A leitura recalcula o tempo restante para não renovar o prazo a cada caractere ou rejeição. Esses mecanismos não estabelecem um limite para toda operação de escrita. Durante a entrada humana, uma thread lê o terminal enquanto a sessão observa a rede, permitindo perceber EOF ou encerramento remoto.

Não confundir rollback local de uma tentativa rejeitada com recuperação de partida após desconexão: não há retomada automática.

Transição: “O socket TCP mantém a partida entre os dois jogadores. Os displays usam outro canal.”

## 5. Clipe 04 — multicast e independência do Display

### Slide 4: Multicast — uma publicação, vários observadores

Texto para o slide:

- Grupo 225.1.1.21, porta 8821
- Datagramas UDP; interface e TTL configuráveis
- Snapshot completo com partidaId e jogada
- Sem ACK; ausência de Display não impede o jogo

### Mostrar no código

1. code/TcpGameSession.java, acceptOpponentMove: publicar somente depois de validar a jogada recebida.
2. code/Display.java, openSocket: MulticastSocket, joinGroup, interface e TTL.
3. publishMove: envelope, DatagramPacket, send e tratamento de IOException.
4. main: receivePacket, validação do snapshot, descarte de mensagem inválida e showMove.

### Fala-base

“Multicast permite publicar para um grupo que pode ter vários receptores. Os displays entram no grupo e recebem datagramas UDP. Não é aberta uma conexão TCP com cada Display.

Nesta implementação, quem recebe e valida a jogada TCP publica essa jogada para os displays. Isso evita publicar tentativas rejeitadas. O envelope inclui o ID da partida e o tabuleiro completo, então cada atualização identifica a partida e permite acompanhar mesmo começando depois.

A partida não espera confirmação do Display. Se um Display for fechado, o jogo pode continuar. Se a publicação falhar, o jogador registra o problema e mantém a partida TCP. Já um pacote inválido no Display é descartado e a recepção continua.

Multicast não garante entrega, ordem ou ausência de duplicação. O próximo snapshot pode atualizar a visualização após uma perda, mas não recupera o histórico perdido e não há retransmissão.”

### O que compreender antes de gravar

TCP, UDP e multicast usam sockets. Gameplay UDP não foi implementado, mas o multicast já utiliza UDP. A entrega multicast depende da rede; TTL não garante que os roteadores encaminhem o tráfego. Nos testes locais use lo/TTL0; nas VMs enX0/TTL1, sujeito à validação remota.

Um snapshot antigo pode chegar depois de um novo: o Display não mantém histórico nem faz filtro de ordem. IDs identificam partidas intercaladas; não há menu de seleção. UUID e validação por IP não autenticam participantes.

Transição: “Essas separações determinam como cada tipo de falha afeta o sistema.”

## 6. Clipe 05 — tolerância a falhas no código

### Slide 5: Falha, detecção e reação

Use no slide apenas estas quatro linhas; mantenha a matriz completa como material de estudo.

| Situação | Reação |
| --- | --- |
| Sem par | Retorna TIMEOUT e remove o pedido |
| Jogada inválida | Rejeita e solicita correção |
| Adversário desconectado/inativo | Encerra com erro e libera recursos |
| Display indisponível | Partida continua |

### Fala-base

“O tratamento de falhas varia conforme a função do componente. Sem adversário, o servidor limita a espera e remove o registro. Uma jogada inválida pode ser corrigida, mantendo o estado anterior. Se a conexão com o adversário falhar ou o prazo acabar, a sessão encerra e tenta avisar o outro lado quando possível.

A exibição é independente: perder um Display ou falhar a publicação multicast não deve derrubar a partida. Também validamos mensagens antes de modificar o estado.

Esses mecanismos evitam algumas esperas indefinidas e estados incorretos. Não implementam reconexão, recuperação da partida ou alta disponibilidade do servidor. Uma queda de rede pode impedir a entrega até da mensagem de encerramento.”

### Onde apontar no código durante a parte 5

Os números abaixo foram conferidos em26/10/2026; podem mudar se o arquivo for editado. Use o nome do método para localizar. Os links abrem no início do trecho; amplie apenas as linhas citadas na coluna “destacar”. Em dois minutos, use os seis destaques da tabela seguinte; os detalhes extras são para estudo.

| Tempo dentro da parte5 | Abrir e destacar | O que apontar com o cursor | Fala curta e texto na edição |
| --- | --- | --- | --- |
| 0:00–0:18 | [MatchServer.java:98](/home/thiago/Desktop/TicTacToeMulticast/code/MatchServer.java:98), getOponent, linhas98–109 | remainingNanoseconds, remoção da fila e createTimeoutResponse | “Se não houver par no prazo, removo o pedido e retorno TIMEOUT.” Texto: “Match: prazo padrão300s” |
| 0:18–0:36 | [TcpGameSession.java:277](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:277), readMessage, linhas282–290; depois [ensureTimeRemaining:350](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:350) | Tempo restante, setSoTimeout e EOFException | “A espera de resposta tem prazo; leitura com -1 indica conexão encerrada.” Texto: “TCP: prazo e desconexão” |
| 0:36–0:58 | [TcpGameSession.java:169](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:169), acceptOpponentMove, linhas178–190 | validateMove antes de gameRules=candidateRules; rejeicao/refazer | “Valido em uma cópia. Se inválida, rejeito sem aceitar o estado.” Texto: “Estado e contador preservados” |
| 0:58–1:18 | [Display.java:104](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:104), publishMove, linhas122–126; [openPublisher:49](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:49), linhas57–67 | catch de envio; aviso, close e retorno null na abertura | “Falha de publicação desativa o canal multicast, mas não encerra o TCP.” Texto: “Partida continua sem publicação” |
| 1:18–1:40 | [TcpGameSession.java:74](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:74), play, linhas74–86; [Player.java:98](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:98), linhas98–105 | Catch por inatividade/erro, finally e try com Socket | “Na falha da sessão, tento avisar, fecho recursos e encerro. O aviso pode não chegar.” Texto: “Encerramento controlado” |
| 1:40–2:00 | Slide5 e a limitação correspondente | Mostrar a reação e uma linha de limite | “Não há reconexão, persistência da fila ou garantia de entrega multicast.” Texto: “Limites da implementação” |

O texto da tabela é a fala principal deste trecho. Use a fala-base anterior para estudar ou substituir palavras, sem ler as duas versões no vídeo: não caberiam em dois minutos.

Detalhes adicionais se o professor perguntar:

- [Player.java:33](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:33): catch RemoteException e aviso de resultado desconhecido, sem repetição automática; linha43 trata serviço ausente.
- [Player.java:60](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:60): try com ServerSocket e setSoTimeout no aceite. [openConnectionWithOpponent:128](/home/thiago/Desktop/TicTacToeMulticast/code/Player.java:128): IP esperado, connect com prazo e fechamento em falha.
- [TcpGameSession.java:134](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:134): quando a própria jogada é rejeitada, restaura previousRules e solicita outra entrada; não apenas imprime o erro.
- [TcpGameSession.java:301](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:301): limite de tamanho; linhas293–297 tratam JSON ilegível. O decoder UTF-8 estrito fica no construtor, linhas39–41.
- [TcpGameSession.java:229](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:229): observa a rede enquanto o usuário digita; linhas244–245 detectam EOF. Linhas255–268 tratam EOF e entrada inválida do terminal.
- [Display.java:189](/home/thiago/Desktop/TicTacToeMulticast/code/Display.java:189): laço de recepção; linhas205–208 descartam datagramas inválidos sem parar o laço. Não confundir com falha do socket receptor, que encerra o Display.
- [TcpGameSession.java:330](/home/thiago/Desktop/TicTacToeMulticast/code/TcpGameSession.java:330): sendFailureWhenPossible também trata falha ao enviar o aviso; não promete entregar o encerramento.

Apresente como tratamento/tolerância a falhas. “Segurança” aqui não significa autenticação ou criptografia: verificação de IP, UUID e consistência de tabuleiro não oferecem essas garantias.

### Matriz para estudar e responder ao professor

| Falha | Onde detectar | Reação implementada | Limitação |
| --- | --- | --- | --- |
| Pedido malformado | JsonMessages/getOponent | INVALID_REQUEST; não enfileira | Não é autenticação |
| Registro pendente duplicado | getOponent | DUPLICATE; mantém o original | Desistência não cancela imediatamente |
| Ausência de par | timedWait e prazo | Remove pedido e retorna TIMEOUT | Depende de o servidor e a comunicação ainda funcionarem |
| Registry/serviço indisponível | lookup/getOponent e exceções | Mensagem local, encerra tentativa sem retry | Resultado pode ser desconhecido se chamada já foi enviada |
| Falha ao conectar/aceitar | connect/accept e prazo | Exceção, fechamento, saída1 | Não tenta outro oponente automaticamente |
| Adversário fecha conexão | read retorna -1 ou IOException | Erro; aviso remoto quando possível | Não garante detecção instantânea de toda falha de rede |
| Adversário sem resposta | Prazo absoluto e setSoTimeout | Encerra por inatividade | Não há garantia de prazo para todas as escritas |
| Jogada inconsistente legível | GameMessages.validateMove | Rejeita; preserva estado e contador | Prazo original não reinicia |
| JSON TCP ilegível/UTF-8 inválido/excessivo | readMessage e decoder | Encerra por erro | Não tenta continuar um fluxo inválido |
| Entrada local inválida | readLocalMove | Pede nova entrada | EOF do terminal encerra por erro |
| Publicador multicast indisponível | openPublisher/publishMove | Aviso e publicação desativada | Displays podem deixar de atualizar |
| Datagrama inválido | Display.main/readPacket | Descarta e segue recebendo | Falha real do socket receptor encerra o Display |
| Perda/reordenação multicast | Não há detecção completa | Próximo snapshot pode melhorar a visualização | Sem ACK, reenvio ou filtro de ordem |
| Queda do MatchServer | Exceções nas chamadas; fila em memória | Clientes podem falhar; nova execução é necessária | Sem réplica/persistência; partida já conectada não depende de novas chamadas ao match |

O enunciado pede tolerância a falhas de forma ampla; não afirmar que estes mecanismos esgotam a exigência ou foram individualmente aprovados pelo professor. Nosso controle separado de rejeição é uma diferença em relação ao exemplo de devolver a mesma mensagem sugerido no PDF. Explicar como decisão do projeto, sem atribuir a escolha ao professor.

## 7. Clipe 06 — ordem de execução e resultados

Esta parte tem160 segundos editados. Grave as cenas com calma e corte pausas de digitação; não acelere a leitura dos resultados. Plano sugerido:45s para vitória X/derrota O,35s para vitória O/derrota X,45s para empate,15s para fechar Display e20s para desconexão. Os últimos dois testes usam uma quarta partida; os três primeiros são partidas completas diferentes.

### Preparação e ordem dos programas

Objetivo: demonstrar três resultados normais e depois duas falhas. Pré-requisitos: JDK17 no PATH, portas1199/5121/8821 livres, fontes atuais e multicast local permitido. São quatro terminais; um quinto Display é opcional. Execute a partir da raiz do repositório.

1. Compile uma vez antes de abrir os programas:

```bash
mkdir -p /tmp/tictactoe-video
javac -encoding UTF-8 -d /tmp/tictactoe-video code/*.java
```

2. Terminal1 — MatchServer; mantenha aberto durante todas as partidas:

```bash
env RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchVideo MATCH_TIMEOUT_SECONDS=300 java -cp /tmp/tictactoe-video MatchServer
```

Esperado: Registry127.0.0.1:1199, serviço MatchVideo e objeto remoto na porta5121. Falha: exceção de inicialização ou porta ocupada.

3. Terminal2 — Display; mantenha aberto entre as partidas:

```bash
env MULTICAST_GROUP=225.1.1.21 MULTICAST_PORT=8821 MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-video Display
```

Esperado: escuta no grupo pela interface lo. Pode repetir em um quinto terminal para demonstrar dois displays. Falha: erro de interface/configuração/socket ou ausência de atualização apesar de jogadas aceitas.

4. Terminal3 — primeiro Player. Espere aparecer “Buscando adversário por RMI...” antes de iniciar o segundo:

```bash
env -u PLAYER_IP -u PLAYER_PORT RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchVideo PLAYER_PROTOCOL=tcp MULTICAST_GROUP=225.1.1.21 MULTICAST_PORT=8821 MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-video Player
```

5. Terminal4 — execute exatamente o mesmo comando do passo4, antes de300s. Confira no texto de inicialização qual terminal usa X e qual usa O; as instruções abaixo seguem esses símbolos. Com o primeiro pedido já aguardando, Terminal3 será X e Terminal4 será O.

Esperado nos Players: IP do hostname, portas atribuídas distintas, adversário encontrado, mesmo ID de partida e símbolos diferentes. O IP pode ser127.0.1.1 neste teste local; não exigir127.0.0.1. Para VMs, usar as envs reais do projeto, interface enX0 e TTL1; endereços127.x não servem entre máquinas. Estes comandos são somente para localhost.

### Casas usadas nas tabelas

```text
0 | 1 | 2
3 | 4 | 5
6 | 7 | 8
```

Cada linha da tabela é uma entrada seguida de Enter no terminal indicado. Espere aparecer a pergunta de jogada antes da próxima entrada. Não cole toda a sequência em um único terminal. As casas começam em0, não em1.

### Cena A — vitória de X e derrota de O

Na primeira vez que X pedir uma casa, você pode digitar abc e9 para mostrar correção local. Essas entradas não contam como jogada. Depois:

| Ordem | Terminal | Digitar |
| --- | --- | --- |
| 1 | X | 0 |
| 2 | O | 3 |
| 3 | X | 1 |
| 4 | O | 4 |
| 5 | X | 2 |

```text
X | X | X
O | O | 5
6 | 7 | 8
```

Esperado: X informa “Partida encerrada: você venceu.”; O informa “Partida encerrada: você perdeu.”; Display mostra vitória de X. Os dois Players terminam normalmente. No Canva, identifique os terminais e destaque as duas mensagens reais de resultado. Uma partida já demonstra vitória e derrota simultaneamente, uma em cada jogador.

### Cena B — vitória de O e derrota de X

Reexecute o comando Player no Terminal3, espere a busca e depois reexecute no Terminal4. Não reinicie o MatchServer nem o Display. A nova partida deve mostrar outro ID.

| Ordem | Terminal | Digitar |
| --- | --- | --- |
| 1 | X | 0 |
| 2 | O | 3 |
| 3 | X | 1 |
| 4 | O | 4 |
| 5 | X | 8 |
| 6 | O | 5 |

```text
X | X | 2
O | O | O
6 | 7 | X
```

Esperado: O informa vitória; X informa derrota; Display mostra vitória de O. Ambos terminam normalmente. Esta cena mostra que quem inicia não recebe uma vitória predeterminada.

### Cena C — empate

Inicie outra vez os dois Players, na mesma ordem e com os mesmos comandos. Não reutilize processos que já encerraram.

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

```text
X | O | X
X | O | O
O | X | X
```

Esperado: nove casas ocupadas, nenhuma linha vencedora, ambos mostram “Partida encerrada: empate.” e o Display mostra empate. Ambos terminam normalmente.

### Cena D — fechar Display sem interromper a partida

Inicie uma quarta partida com os mesmos comandos. Faça X0 e O3. Feche apenas o Display com Ctrl+C; depois faça X1 e confira a atualização nos Players. Esperado: a partida continua. Se abriu dois displays, feche um e mostre o outro recebendo.

Não é preciso concluir esta partida. Use a mesma conexão para a próxima cena. Texto na edição: “Display encerrado; comunicação TCP continua”. Fechar um receptor não provoca necessariamente erro no envio UDP; não apresente essa cena como prova de catch de envio.

### Cena E — desconexão do adversário

Depois de X1, O estará escolhendo sua jogada e X aguardando resposta. Pressione Ctrl+C em X e grave a reação de O. Esperado: O percebe desconexão/erro e encerra, inclusive durante a escolha humana; não deve declarar vitória normal. A redação da exceção pode variar. Texto na edição: “Adversário desconectado → encerramento controlado”.

Ao terminar, encerre o MatchServer com Ctrl+C e qualquer Display restante. Não deixe jogadores esperando match após a gravação.

### O que conta como evidência

Sinais de falha nas partidas normais: casa divergente, bloqueio sem reação, resultado diferente nos terminais, erro de conexão ou ausência da confirmação final. Se ocorrer, registre e investigue antes de regravar. Entrada abc/9 demonstra validação do teclado, não rejeição de JSON remoto.

As sequências foram conferidas por análise dos tabuleiros; este ajuste de roteiro não executou novas partidas. Há testes históricos de vitória X, vitória O e empate, mas a alteração mais recente de IP repetiu apenas vitória X e entradas inválidas. Reexecute e grave as três partidas com os fontes atuais. Não apresentar testes locais como resultados nas VMs.

Para timeout, opcionalmente grave os120s/300s reais em material extra. Se cortar ou acelerar a espera, indique “espera reduzida na edição” e o tempo real transcorrido. Não altere o padrão do código só para caber no vídeo.

## 8. Edição no Canva — identificação, cortes e transições

As sugestões visuais abaixo são escolhas para sua gravação, não exigências do professor. O Canva oferece corte, divisão e organização de clipes na linha do tempo e transições entre trechos; os controles podem variar conforme a versão do editor. Referências oficiais: [edição de vídeos](https://www.canva.com/help/creating-and-editing-videos/) e [cortar e dividir vídeos](https://www.canva.com/features/video-trimmer/).

### Identidade visual que acompanha a fala

Use formato horizontal16:9, de preferência1920×1080. Grave o editor e terminais nessa proporção, com fonte ampliada. Priorize legibilidade do código; as legendas e sua câmera, se usada, não devem cobrir os trechos destacados.

| Elemento | Texto ou aparência sugerida | Quando usar |
| --- | --- | --- |
| Identificação de capítulo | “02/06 · RMI — pareamento” no canto superior | Primeiro3s de cada parte; reaparecer discretamente após corte longo |
| Nome do arquivo/método | “MatchServer.java · getOponent” | Durante cada trecho de código |
| Cor por comunicação | Azul para RMI, verde para TCP, laranja para multicast | Mesmas cores nos slides e rótulos; sempre acompanhar com texto |
| Etiqueta dos terminais | “Servidor de match”, “Jogador X”, “Jogador O”, “Display” | Durante toda a demonstração relevante |
| Destaque de código | Retângulo com contorno ou seta, sem esconder as letras | Uma operação por vez, sincronizada com a frase |
| Resumo da falha | “Sem par → TIMEOUT” ou “Jogada inválida → refazer” | Durante o catch/condicional correspondente |
| Identificação do cenário | “Teste1 · vitória X / derrota O”, “Teste2 · vitória O”, “Teste3 · empate” | No começo de cada partida, por2–3s |
| Resultado | Contorno nas mensagens reais dos dois Players | Segurar a imagem por2–3s, sem substituir o terminal por resultado inventado |
| Indicação de edição temporal | “Digitação acelerada” ou “Espera reduzida na edição:120s reais” | Sempre que acelerar ou cortar uma espera que esteja sendo avaliada |

No Canva, adicione esses rótulos como elementos de texto sobre o clipe e ajuste a duração de cada elemento na linha do tempo ao trecho da fala. Duplique os rótulos para manter posição, fonte e tamanho. Para focar no código, use um recorte ampliado da própria gravação em outro trecho; evite ampliar tanto que fique pixelado. Contorno e seta devem apontar o código real, não encobri-lo.

### Transições prontas, com fala e imagem

| Mudança | Frase de ligação | O que mostrar na edição |
| --- | --- | --- |
| Arquitetura → RMI | “Primeiro, os jogadores precisam encontrar um adversário.” | Dissolução curta para Slide2, título “02/06 · RMI” |
| RMI → TCP | “Agora cada jogador conhece o endereço do outro; a partida segue diretamente por TCP.” | Destacar Player↔Player no slide de arquitetura, depois corte para openConnectionWithOpponent |
| TCP → multicast | “A partida já funciona entre os jogadores. Para acompanhar de fora, usamos multicast.” | Mostrar rótulo “04/06 · Multicast” e depois joinGroup/publicação |
| Multicast → falhas | “Cada canal tem limites. Agora vou mostrar onde o código reage às falhas.” | Dissolução curta para Slide5, depois corte para o primeiro bloco de código |
| Falhas → demonstração | “Vou provocar algumas dessas situações e mostrar o comportamento nos terminais.” | Título “06/06 · Demonstração”; iniciar captura com terminais identificados |
| Vitória X → vitória O → empate | “Agora vou iniciar uma nova partida para mostrar outro resultado.” | Corte direto, rótulo do teste e novo ID visível; não repetir abertura do servidor |
| Demonstração → fim | “Assim, vimos os resultados normais e o encerramento quando o adversário desconecta.” | Manter o resultado real final por2s e terminar; não criar clipe7 |

Use corte direto entre slide e código dentro de um tema. Nas mudanças de tema, uma dissolução curta de aproximadamente0,3–0,5s é suficiente. Esses tempos já entram na meta de10 minutos; não acrescente vinhetas longas. Evite animações de digitação e transições com giro, que dificultam acompanhar o código.

### Plano de montagem, parte por parte

| Parte | Ordem das imagens | Texto que deve acompanhar |
| --- | --- | --- |
| 1 | Slide de arquitetura | Título, nome e três programas; sem introdução longa |
| 2 | Slide2 → MatchService → export/Registry → lookup → fila | “Contrato”, “Registro do serviço”, “Chamada remota”, “Pareamento” |
| 3 | Slide3 → endereço/porta → connect/accept → envio → validação | “IP local”, “Porta escolhida pelo sistema”, “Conexão TCP”, “JSON por linha” |
| 4 | Slide4 → joinGroup → publishMove → Display | “Grupo multicast”, “Jogada aceita”, “Vários receptores” |
| 5 | Blocos indicados no mapa de código | Falha → detecção → reação; um destaque de cada vez |
| 6 | Vitória X → vitória O → empate → fechar Display → desconectar jogador | Número do teste, jogador e mensagem real observada |

### Passo a passo da edição

1. Grave seis clipes principais, nomeados 01_arquitetura até06_demo. Grave testes como arquivos separados para poder reordenar e cortar sem refazer a explicação.
2. Crie um projeto de vídeo horizontal no Canva e importe os clipes, as imagens dos slides e eventuais recortes de código. Coloque-os na linha do tempo na ordem da tabela inicial.
3. Corte silêncios e erros de fala nas bordas; divida um clipe quando precisar inserir slide ou outra captura. Preserve a ordem causal de cada teste: ação, detecção e reação.
4. Insira títulos e rótulos; ajuste sua duração para coincidir com a fala. No código, mantenha método e linhas relevantes visíveis enquanto a explicação acontece.
5. Acrescente transições só nas mudanças de assunto. Entre duas ações do mesmo teste, prefira corte direto e sinalize quando houver passagem de tempo.
6. Se usar legendas, revise os termos RMI, TCP, multicast, getOponent e nomes de métodos. Posicione-as fora da área de código e dos resultados. Podem ser textos curtos inseridos manualmente; não dependa de recurso automático para concluir a edição.
7. Equilibre o áudio dos clipes. Prefira sem música; se usar, mantenha muito baixa e sem competir com a fala. Evite sons em cada aparição de código.
8. Assista em tela cheia e confira se o texto permanece legível sem pausar. Revise a duração total, os três resultados e a correspondência entre a fala e o trecho mostrado. Exporte na melhor resolução disponível compatível com a captura e confira o arquivo exportado.

Evite afirmar “o sistema nunca trava”, “TCP garante que o programa processou a jogada” ou “o Display sempre fica atualizado”. Os limites estão explicados na parte5; não precisam de uma seção final separada.

## 9. Perguntas para ensaiar

| Pergunta | Resposta que você deve conseguir explicar |
| --- | --- |
| Por que RMI? | É o mecanismo do serviço de pareamento; permite invocar getOponent remotamente pelo contrato compartilhado |
| Registry recebe as jogadas? | Não; localiza o serviço. Depois do match, jogadores comunicam por TCP |
| Por que não só TCP para tudo? | O trabalho separa funções: RMI no match, socket direto na partida e multicast na exibição |
| Por que InetSocketAddress? | Representa IP/porta para bind e connect com timeout; quem comunica é o socket |
| Quem escolhe a porta? | O sistema ao receber0; o Player mantém o ServerSocket aberto e anuncia getLocalPort |
| TCP já valida a jogada? | Não; entrega dados em ordem na conexão. As regras e o protocolo validam o conteúdo |
| Por que quebra de linha? | Para delimitar um JSON dentro do fluxo TCP |
| Por que cópia do tabuleiro? | Para validar uma tentativa antes de aceitar seu estado e permitir desfazer rejeições |
| Como vários displays recebem? | Entram no mesmo grupo/porta multicast; o publicador não abre uma conexão por Display |
| O jogo funciona sem Display? | Sim; não espera ACK e trata falhas de publicação separadamente |
| O que acontece se o match cair? | Chamadas pendentes podem falhar e a fila em memória se perde; partidas TCP já estabelecidas não dependem de novas chamadas ao match |
| A partida se recupera de queda? | Não; encerra com erro/inatividade, tenta avisar e fecha recursos |
| Como foi testada a tolerância? | Relacionar cada cena real à falha provocada, à detecção e à reação; separar testes locais e remotos |
