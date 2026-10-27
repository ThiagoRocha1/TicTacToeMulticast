# Trechos de código para o vídeo

Lista dos trechos mostrados nos slides com código e das linhas exatas de cada arquivo, para gerar as imagens no Carbon caso a disposição dos slides não sirva. Os números de linha valem para os arquivos de `code/` na versão atual; se algum arquivo mudar, confira de novo.

Como usar no Carbon: cole só as linhas da coluna "Linhas", escolha Java e use a fonte JetBrains Mono. Se o trecho ficar grande, divida em duas imagens (quando houver duas faixas, cada faixa vira uma imagem). As linhas da coluna "Destacar" são as que a fala aponta.

Nos slides, o arquivo e as linhas aparecem no topo do bloco de código. Cada slide com código tem a mesma informação nas notas do orador.

## A história em ordem

Os títulos dos slides de cada seção trazem a progressão no início (por exemplo, "RMI 3/6", "Socket 5/6", "Consistência 2/4", "Multicast 6/7", "Falhas 1/2"). A abertura de cada seção não entra na contagem.

O deck conta a partida na ordem em que as coisas acontecem. Cada capítulo segue o mesmo caminho: conceito, mecanismo por dentro (inicialização, execução, funcionamento) e os trechos de código na ordem dos passos.

| Capítulo | Slides |
| --- | --- |
| 01 e 02 Abertura, arquitetura e arquivos de apoio | `cover`, `arquitetura`, `c-papeis-apoio` |
| 03 RMI | `rmi` (abertura), `m-rmi`, `c-rmi-contrato`, `c-rmi-servidor`, `c-rmi-jogador`, `c-rmi-fila`, `j-match` |
| 04 Socket | `tcp` (abertura), `m-tcp`, `c-tcp-reserva`, `c-tcp-conexao`, `c-tcp-inicio`, `c-tcp-leitura`, `j-partida` |
| 05 Consistência | `consistencia` (abertura), `c-cons-envio`, `c-cons-validate`, `c-cons-regras`, `c-cons-aceitar` |
| 06 Multicast | `multicast` (abertura), `m-mc`, `c-mc-config`, `c-mc-socket`, `c-mc-quem`, `c-mc-publica`, `j-multicast`, `c-mc-display` |
| 07 Falhas | `falhas` (tabela com a coluna "No código", que resume timeout e multicast), `c-f-encerrar` |
| 08 Demonstração | `demo` |

Os slides com código mostram só o título, a lista numerada com a sequência que será explicada (na ordem do código) e o bloco de código com arquivo e linhas no topo. O slide `c-papeis-apoio` não tem código para o Carbon: lista os nove arquivos e o motivo de cada um. Os três slides `m-*` (RMI, socket e multicast) mostram a sequência inicialização, execução e funcionamento por meio de três recortes de código, cada um com seu arquivo e suas linhas. Para o Carbon, use as faixas da tabela abaixo (a mesma informação está nas notas do orador): RMI = `c-rmi-servidor`, `c-rmi-jogador` e `c-rmi-fila`; socket = `c-tcp-reserva`, `c-tcp-conexao` e `c-tcp-leitura`; multicast = `c-mc-socket`, `c-mc-publica` e `c-mc-display`.

## JSON trocados

Os slides `j-*` mostram as mensagens JSON no ponto da história em que elas são trocadas. Para o Carbon, cole o JSON da coluna "Mensagem" e use as linhas do código que a produz como legenda.

| Slide | Mensagem | Quem produz (arquivo e linhas) | De quem para quem |
| --- | --- | --- | --- |
| `j-match` | Pedido `{"ip","porta","protocolo"}` | `Player.java` 63 a 68 | Player para MatchServer (RMI) |
| `j-match` | Resposta com par `{"ip","porta","inicia"}` | `JsonMessages.java` 161 a 168 | MatchServer para os dois jogadores |
| `j-match` | Resposta de erro `{"erro","mensagem"}` | `JsonMessages.java` 170 a 176 | MatchServer para o jogador |
| `j-partida` | `inicio` | `TcpGameSession.java` 94 e 95 | quem inicia para o adversário (TCP) |
| `j-partida` | Jogada `{"Numero","Jogada","Tabuleiro","Resultado"}` | `GameMessages.java` 59 a 73 | quem joga para quem recebe |
| `j-partida` | `rejeicao` | `TcpGameSession.java` 181 a 184 | quem recebe para quem jogou |
| `j-partida` | `fim` | `GameMessages.java` 121 a 132 | quem recebe a jogada conclusiva (ou quem falha) |
| `j-partida` | `confirmacao_fim` | `TcpGameSession.java` 149 | autor da jogada |
| `j-multicast` | Envelope `{"partidaId","jogada"}` | `Display.java` 109 a 111 | jogador que validou para o grupo |

Posição dos slides: `j-match` depois de `c-rmi-fila`, `j-partida` depois de `c-tcp-leitura` e `j-multicast` depois de `c-mc-publica`. Os endereços, portas e UUID dos exemplos são ilustrativos.

## 03 RMI

| Slide | Arquivo | Linhas | Destacar | Ideia da fala |
| --- | --- | --- | --- | --- |
| `c-rmi-contrato` | `MatchService.java` | 1 a 7 | 4, 6 | Um método compartilhado pelos dois lados. |
| `c-rmi-servidor` | `MatchServer.java` | 149 a 173 | 157, 161, 168 | O servidor exporta o objeto, cria o próprio Registry e registra o nome do serviço. |
| `c-rmi-jogador` | `Player.java` | 74 a 94 | 74, 75, 78, 94 | O jogador procura o nome no Registry e chama `getOponent` como se fosse local. |
| `c-rmi-fila` | `MatchServer.java` | 68 a 109 (42 linhas; se apertar, use 89 a 109 e cite a linha 68 na fala) | 68, 94, 95, 96, 109 | Fila por protocolo sob `synchronized`; o segundo pedido forma o par e acorda o primeiro. |

## 04 Socket

| Slide | Arquivo | Linhas | Destacar | Ideia da fala |
| --- | --- | --- | --- | --- |
| `c-tcp-reserva` | `Player.java` | 57 a 66 | 60, 65 | A porta é reservada antes do pedido RMI e a porta real vai no pedido. |
| `c-tcp-conexao` | `Player.java` | 131 a 150 | 133, 135, 147, 148 | Quem recebeu `inicia` verdadeiro conecta e joga com X; o outro aceita e confere o IP. |
| `c-tcp-inicio` | `TcpGameSession.java` | 90 a 104 | 94, 95, 98, 99 | Os dois combinam o ID da partida antes da primeira jogada. |
| `c-tcp-leitura` | `TcpGameSession.java` | 277 a 308 e 350 a 358 (duas imagens) | 282, 283, 289, 290, 293, 295, 353, 354 | A leitura acumula até a quebra de linha e respeita um prazo absoluto de 120 s. |

## 05 Consistência

| Slide | Arquivo | Linhas | Destacar | Ideia da fala |
| --- | --- | --- | --- | --- |
| `c-cons-envio` | `TcpGameSession.java` | 106 a 127 e 134 a 144 (duas imagens) | 107, 115, 116, 123, 125 e 142 | Quem joga testa em uma cópia, envia e só então adota o estado; rejeitado, volta ao anterior. |
| `c-cons-validate` | `GameMessages.java` | 76 a 107 | 77, 78, 81, 82, 96 a 98, 102, 103 | Confiro o número esperado, aplico a casa em uma cópia e comparo tabuleiro e resultado. |
| `c-cons-regras` | `TicTacToeRules.java` | 22 a 46 e 74 a 86 (duas imagens) | 35 a 37, 43, 78, 79, 84, 85 | As regras não conhecem rede nem JSON. |
| `c-cons-aceitar` | `TcpGameSession.java` | 176 a 190 | 179, 182, 183, 190 | Só depois de validar aceito o novo estado; inválida é rejeitada com motivo, sem avançar o contador. |

## 06 Multicast

| Slide | Arquivo | Linhas | Destacar | Ideia da fala |
| --- | --- | --- | --- | --- |
| `c-mc-config` | `Display.java` | 29 a 47 | 30, 36, 39, 46 | Grupo, porta, interface e TTL vêm do ambiente; interface é obrigatória. |
| `c-mc-socket` | `Display.java` | 83 a 101 | 85, 93, 94, 98 | Interface, TTL, reuso da porta e entrada no grupo. |
| `c-mc-quem` | `TcpGameSession.java` | 178 a 196 | 190, 192 a 194 | Quem valida é quem publica; rejeições não chegam aos displays. |
| `c-mc-publica` | `Display.java` | 104 a 128 | 110, 111, 123, 126 | Envelope com o ID e o tabuleiro completo; um único `send` ao grupo. |
| `c-mc-display` | `Display.java` | 188 a 210 e 220 a 224 (duas imagens) | 191, 203 a 206, 223, 224 | O Display valida cada snapshot, descarta o inválido e mostra ID e tabuleiro. |

## 07 Falhas

| Slide | Arquivo | Linhas | Destacar | Ideia da fala |
| --- | --- | --- | --- | --- |
| `c-f-timeout` (sem slide próprio; resumido em `falhas`) | `MatchServer.java` | 99 a 109 e 125 a 128 (duas imagens) | 103, 104, 126 | Sem par no prazo, removo o pedido e retorno TIMEOUT. |
| `c-f-multicast` (sem slide próprio; resumido em `falhas`) | `Display.java` | 57 a 67 e 122 a 127 (duas imagens) | 67, 123, 126 | Falha de publicação desativa o multicast, mas a partida TCP continua. |
| `c-f-encerrar` | `TcpGameSession.java` 74 a 87 e `Player.java` 98 a 105 (duas imagens) | ver colunas ao lado | TcpGameSession: 75, 78, 83 a 86. Player: 98, 103 | Tento avisar o adversário, fecho os recursos e encerro; o aviso pode não chegar. |

### Trechos já cobertos por outros slides na parte de falhas

| Momento da fala | Reaproveite o slide | Destacar |
| --- | --- | --- |
| Leitura com prazo; `-1` indica conexão fechada | `c-tcp-leitura` | 282 a 290 e 350 a 355 |
| Jogada inválida rejeitada sem aceitar o estado | `c-cons-aceitar` | 179 a 184 |

## O que cada slide omite

Os slides mostram recortes, e as reticências marcam o que ficou de fora. Para mostrar o trecho completo no Carbon, use as faixas de linhas da tabela; elas incluem o que o slide omite.

## Observações

- A cor dos destaques segue o canal: azul para RMI, verde para TCP e consistência, laranja para multicast e vermelho para falhas.
- Os slides com código fazem parte do deck de prévia e não substituem o deck original. A decisão de qual usar na gravação é sua.

## Versão com Carbon (trechos contíguos)

O deck "Jogo da velha distribuído (Carbon)" usa imagens geradas pelo Carbon com trechos contíguos do código. Cada slide mostra o arquivo e as linhas no topo, e as linhas destacadas ficam por cima da imagem. As imagens têm os números de linha reais do arquivo (o primeiro número da imagem é a primeira linha da faixa) e o código é deslocado para a esquerda (a indentação comum é removida). Para refazer uma imagem à mão no Carbon, cole exatamente as linhas da coluna "Linhas".

| Slide | Arquivo | Linhas | Destacadas por cima |
| --- | --- | --- | --- |
| `c-rmi-contrato` | `MatchService.java` | 1 a 7 | 4, 6 |
| `c-rmi-main` | `MatchServer.java` | 130 a 147 | 132, 133, 134, 135 |
| `c-rmi-servidor` | `MatchServer.java` | 149 a 173 | 154, 156, 157, 161, 168 |
| `c-rmi-jogador` | `Player.java` | 74 a 96 | 74, 75, 78, 92, 93, 94 |
| `c-rmi-fila-1` | `MatchServer.java` | 56 a 80 | 56, 60, 62, 65, 68, 74, 75, 76 |
| `c-rmi-fila-2` | `MatchServer.java` | 81 a 97 | 84, 85, 89, 92, 94, 95, 96 |
| `c-rmi-fila-3` | `MatchServer.java` | 98 a 123 | 99, 100, 102, 103, 104, 109 |
| `c-tcp-reserva` | `Player.java` | 49 a 72 | 51, 57, 58, 60, 65, 68 |
| `c-tcp-conexao-1` | `Player.java` | 128 a 142 | 131, 133, 135, 136, 137, 140 |
| `c-tcp-conexao-2` | `Player.java` | 143 a 155 | 143, 146, 147, 148, 150, 151, 152, 153 |
| `c-tcp-play` | `TcpGameSession.java` | 55 a 73 | 56, 59, 65, 66, 67, 69, 70, 71, 73 |
| `c-tcp-inicio` | `TcpGameSession.java` | 90 a 104 | 94, 95, 98, 99, 100 |
| `c-tcp-leitura-1` | `TcpGameSession.java` | 273 a 296 | 277, 281, 282, 283, 286, 289, 290, 293, 295 |
| `c-tcp-leitura-2` | `TcpGameSession.java` | 297 a 314 | 301, 302, 305, 306, 310, 311, 312, 313 |
| `c-tcp-prazo` | `TcpGameSession.java` | 346 a 358 | 347, 350, 351, 353, 354, 357 |
| `c-cons-envio-1` | `TcpGameSession.java` | 106 a 127 | 107, 113, 115, 116, 122, 123, 125 |
| `c-cons-envio-2` | `TcpGameSession.java` | 128 a 151 | 129, 131, 134, 135, 137, 141, 142, 147, 148, 149 |
| `c-cons-envio-3` | `TcpGameSession.java` | 152 a 167 | 153, 157, 162, 165, 166 |
| `c-cons-validate-1` | `GameMessages.java` | 76 a 99 | 77, 81, 82, 84, 86, 90, 91, 93, 96, 97, 98 |
| `c-cons-validate-2` | `GameMessages.java` | 100 a 119 | 102, 103, 106, 109, 110, 114, 118 |
| `c-cons-regras-1` | `TicTacToeRules.java` | 22 a 46 | 27, 31, 35, 39, 43, 44, 45 |
| `c-cons-regras-2` | `TicTacToeRules.java` | 62 a 86 | 63, 67, 71, 74, 78, 79, 84, 85 |
| `c-cons-aceitar-1` | `TcpGameSession.java` | 169 a 189 | 171, 172, 176, 178, 179, 180, 181, 182, 183, 184, 186, 187 |
| `c-cons-aceitar-2` | `TcpGameSession.java` | 190 a 213 | 190, 192, 193, 194, 196, 198, 205, 206, 208, 211 |
| `c-mc-config` | `Display.java` | 29 a 47 | 30, 32, 36, 39, 40, 42, 46 |
| `c-mc-abrir` | `Display.java` | 49 a 68 | 50, 53, 54, 56, 58, 60, 63, 67 |
| `c-mc-socket` | `Display.java` | 83 a 102 | 84, 85, 88, 90, 93, 94, 95, 98 |
| `c-mc-quem` | `TcpGameSession.java` | 190 a 213 | 190, 192, 193, 194 |
| `c-mc-publica` | `Display.java` | 104 a 128 | 110, 111, 113, 120, 123, 126 |
| `c-mc-display-1` | `Display.java` | 188 a 210 | 189, 191, 194, 195, 196, 203, 204, 205, 206 |
| `c-mc-display-2` | `Display.java` | 220 a 238 | 221, 222, 223, 224, 226, 229, 230, 231, 233, 235 |
| `c-mc-display-3` | `Display.java` | 239 a 262 | 239, 240, 243, 246, 249, 251, 252, 254, 255, 258, 259, 260 |
| `c-f-encerrar-1` | `TcpGameSession.java` | 74 a 88 | 74, 75, 77, 78, 80, 81, 83, 84, 85 |
| `c-f-encerrar-2` | `TcpGameSession.java` | 330 a 344 | 331, 333, 337, 338, 339, 340, 341, 342 |
| `c-f-encerrar-3` | `Player.java` | 95 a 106 | 98, 100, 102, 103 |

O slide `c-mc-quem` reaproveita a imagem de `c-cons-aceitar-2` com outro destaque.
