> **Atualização multicast:** a versão atual inclui Display.java e Display funcional. Usar casos17/18 para integração. Roteiros anteriores que esperam Display reservado são históricos; testesTCP continuam válidos, mas sem MULTICAST_INTERFACE o publicador apenas avisa e o jogo continua.

> **Atualização TCP (25/10/2026):** executar os casos 14 e 15 para a versão atual. Player agora abre a porta antes do RMI, aceita somente TCP e joga após o match. Casos 1–12 descrevem verificações anteriores do cliente de diagnóstico e são históricos: UDP/duplicidade via Player não reproduzem mais aquele fluxo; sucesso agora permanece na partida até o resultado. Casos de contrato RMI isolado devem usar JShell, como no caso 15. Caso 13 continua válido para regras locais. Não executar roteiros históricos como se fossem testes atuais completos.

# Testes manuais do servidor de match

Os formatos de erro, duplicidade, ordem de início, desistência por timeout e ausência de retries automáticos foram aprovados pelo responsável. O JSON no transporte permanece como documentado; o cliente agora mostra mensagens legíveis e encerra erros com saída 1. Não há garantia de entrega a clientes desconectados.

O contrato RMI atual usa `String getOponent(String requestJson)`. Após atualizar, recompilar e reiniciar servidor e clientes com a mesma versão de MatchService; os resultados antigos com byte[] não comprovam esta assinatura. Nenhum teste de UTF-8 inválido se aplica diretamente à entrada String.

Os comandos abaixo são executados em terminais separados, na raiz de `TicTacToeMulticast`. Exigem somente o JDK 17. O cliente é somente de diagnóstico: não executa jogadas.

## Preparação

```bash
mkdir -p /tmp/tictactoe-classes
javac -encoding UTF-8 -d /tmp/tictactoe-classes code/*.java
```

Esperado: compilação sem erros. Avisos de operações unchecked são esperados porque o responsável pediu coleções sem notação de generics; os valores são lidos com casts explícitos. Falha: `javac` ausente indica JDK não configurado. Não há dependências externas.

## 1. Parear dois clientes

Objetivo: verificar espera do primeiro, retorno para ambos, endpoints e ordem de início.

1. Terminal 1 — iniciar servidor:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_OBJECT_PORT=5021 RMI_SERVICE_NAME=MatchServiceLocal java -cp /tmp/tictactoe-classes MatchServer
```

Esperado: anúncio do Registry e do objeto remoto. Manter aberto.

2. Terminal 2 — primeiro cliente:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_SERVICE_NAME=MatchServiceLocal PLAYER_IP=127.0.0.1 PLAYER_PORT=5010 PLAYER_PROTOCOL=tcp java -cp /tmp/tictactoe-classes Player
```

Esperado: aguarda sem retorno até o segundo registro (ou timeout).

3. Terminal 3 — segundo cliente:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_SERVICE_NAME=MatchServiceLocal PLAYER_IP=127.0.0.1 PLAYER_PORT=5011 PLAYER_PROTOCOL=tcp java -cp /tmp/tictactoe-classes Player
```

Esperado no terminal 2: `Oponente encontrado: 127.0.0.1:5011` e `Você inicia a partida.` No terminal 3: `Oponente encontrado: 127.0.0.1:5010` e `O adversário inicia a partida.` Ambos encerram com saída 0. No transporte, isso corresponde a inicia=true/false; a ordem dos campos JSON não importa.

Falha: endpoint trocado incorretamente, valores iguais de `inicia`, apenas uma chamada retorna ou timeout mesmo com dois registros TCP no prazo.

## 2. Outro IP e outras portas

Objetivo: verificar configuração sem alterar código. Encerrar o servidor anterior com Ctrl+C e repetir o caso 1 substituindo em todos os comandos:

| Parâmetro | Valor alternativo |
| --- | --- |
| `RMI_HOST` e `PLAYER_IP` | `127.0.0.2` |
| `RMI_REGISTRY_PORT` | `1199` |
| `RMI_OBJECT_PORT` no servidor | `5121` |
| `PLAYER_PORT` do primeiro cliente | `6010` |
| `PLAYER_PORT` do segundo cliente | `6011` |

Esperado: mesmo comportamento, retornando o IP e as portas alternativos. Falha: tentativa de usar os endpoints anteriores. Esse IP é outro loopback no Linux; não comprova conectividade entre VMs.

## 3. Timeout e registro após expiração

Objetivo: provocar falta de oponente e verificar remoção do registro. Encerrar o servidor anterior. Iniciar o comando do servidor do caso 1 acrescentando `MATCH_TIMEOUT_SECONDS=2` antes de `java`.

1. Executar somente o primeiro cliente do caso 1.
2. Aguardar pelo menos 2 segundos.
3. Repetir o mesmo comando após o retorno.

Esperado em ambas as tentativas: `Falha no pareamento [TIMEOUT]:` e mensagem explicativa, saída 1, após aproximadamente 2 segundos. A segunda tentativa deve aguardar novamente, sem `DUPLICATE`. Falha: espera indefinida ou registro expirado bloqueando o novo pedido. O valor reduzido é somente para teste; o padrão é 300 s.

## 4. Duplicidade e protocolos distintos

Objetivo: verificar que duplicatas não substituem o primeiro cliente e TCP não pareia com UDP. Reiniciar o servidor do caso 1 com o timeout padrão.

1. Iniciar o primeiro cliente TCP e mantê-lo aguardando.
2. Em outro terminal, repetir exatamente o mesmo comando do cliente: esperado `Falha no pareamento [DUPLICATE]:` e saída 1.
3. Iniciar outro cliente com `PLAYER_PORT=5012 PLAYER_PROTOCOL=udp`: deve continuar aguardando; não pode liberar o TCP.
4. Iniciar o segundo cliente TCP do caso 1: deve parear com o primeiro TCP.
5. Iniciar um cliente com `PLAYER_PORT=5013 PLAYER_PROTOCOL=udp`: deve liberar o pedido UDP do passo 3.

Esperado: pares TCP 5010/5011 e UDP 5012/5013, um `inicia=true` por par. Falha: protocolo cruzado, duplicata aceita como oponente ou primeiro registro perdido. O teste UDP exercita somente a classificação de pedidos no RMI; não implementa jogo por UDP.

## 5. Entrada inválida e servidor indisponível

Objetivo: provocar erro de aplicação e de transporte sem espera indevida.

1. Com o servidor do caso 1 ativo, executar o cliente com `PLAYER_PROTOCOL=http`: esperado `Falha no pareamento [INVALID_REQUEST]:` e saída 1.
2. Repetir com `PLAYER_IP=999.0.0.1`: esperado `[INVALID_REQUEST]` e saída 1.
3. Encerrar o servidor com Ctrl+C e executar um cliente: esperado erro RMI de conexão, sem busca infinita de oponente. Sem retries automáticos por decisão aprovada. O cliente avisa que, se o pedido já foi enviado, o resultado pode ser desconhecido.

Falha: pedido inválido entra na fila, servidor encerra por entrada inválida ou cliente fica indefinidamente aguardando um Registry desligado em localhost.

## Resultados

Registrar apenas execuções observadas na seção 9 do `PLANO_DO_PROJETO.md`, com ambiente e parâmetros usados, e refletir no quadro consolidado da seção 7. A seção 10 é histórico da versão org.json. O timeout de 2 s é histórico anterior. O prazo real de 300 s, novo registro após expiração, protocolos separados e desistência têm evidência com byte[]. Com String foram executados par local, duplicidade, IP inválido e timeout reduzido de 10 s, além das verificações de mensagens descritas na seção 9 do plano; repetir os demais casos antes de marcá-los como validados. Compilação não comprova funcionamento entre VMs. Limpeza por timeout de pedido abandonado sem par, interrupção real, partida e multicast continuam exigindo execução própria.

## 6. Utilitário JSON sem biblioteca

Objetivo: conferir leitura, escrita e rejeição de entradas inválidas. Pré-requisito: JDK 17 e compilação da preparação concluída; executar na raiz do projeto.

1. Em um terminal, executar `jshell --execution local`.
2. Carregar as classes compiladas e liberar os métodos para inspeção manual:

```java
java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of("/tmp/tictactoe-classes").toAbsolutePath().toUri().toURL()});
java.lang.reflect.Method parse = loader.loadClass("JsonUtil").getDeclaredMethod("parseJson", String.class);
parse.setAccessible(true);
java.lang.reflect.Method write = loader.loadClass("JsonUtil").getDeclaredMethod("toJson", Object.class);
write.setAccessible(true);
```

3. Executar leitura e escrita:

```java
parse.invoke(null, "{\"ip\":\"127.0.0.1\",\"porta\":5010,\"protocolo\":\"tcp\"}")
write.invoke(null, parse.invoke(null, "{\"texto\":\"a\\\"b\\\\c\\n\\u00e7\",\"lista\":[0,true,null,-1.5e2]}"))
```

Esperado neste terminal: mapa com os três campos; JSON válido preservando aspas, barra, quebra de linha, ç, lista e valores (a representação do expoente pode mudar).

4. Provocar falhas, uma expressão por vez:

```java
parse.invoke(null, "{\"a\":1,}")
parse.invoke(null, "{\"a\":1,\"a\":2}")
parse.invoke(null, "{} lixo")
java.lang.reflect.Method player = loader.loadClass("JsonMessages").getDeclaredMethod("readPlayerRequest", String.class);
player.setAccessible(true);
player.invoke(null, (Object) null)
```

Esperado: cada entrada inválida gera `InvocationTargetException` cuja causa é `IllegalArgumentException`; a última informa JSON ausente. Sinais de falha: aceitar essas entradas, perder conteúdo no passo 3 ou encerrar o JShell inesperadamente.

5. Encerrar com `/exit`. Repetir o caso 1 para verificar RMI com o utilitário; não usar o resultado histórico da versão org.json como prova desta implementação.


## 7. Configuração numérica inválida

Objetivo: verificar tratamento específico das conversões de configuração. Pré-requisitos: JDK 17, compilação da preparação concluída; nenhum servidor é necessário.

1. No terminal, executar:

```bash
RMI_REGISTRY_PORT=abc java -cp /tmp/tictactoe-classes Player
```

Esperado: `Configuração inválida: RMI_REGISTRY_PORT deve conter uma porta inteira`, encerramento com código 1. Conferir imediatamente com `echo $?`.

2. Provocar timeout não numérico:

```bash
MATCH_TIMEOUT_SECONDS=abc RMI_OBJECT_PORT=5021 java -cp /tmp/tictactoe-classes MatchServer
```

Esperado: `Configuração inválida: MATCH_TIMEOUT_SECONDS deve conter um inteiro positivo`, código 1, sem iniciar servidor.

3. Provocar valor que não cabe no prazo interno em nanossegundos:

```bash
MATCH_TIMEOUT_SECONDS=9223372036854775807 RMI_OBJECT_PORT=5021 java -cp /tmp/tictactoe-classes MatchServer
```

Esperado: `Configuração inválida: MATCH_TIMEOUT_SECONDS fora do intervalo permitido`, código 1.

Sinais de falha em qualquer passo: anunciar servidor ativo, ignorar o valor inválido ou exibir somente um stack trace sem contexto da configuração. Este módulo usa variáveis de ambiente, não leitura interativa: EOF e normalização com `toLowerCase` não se aplicam a estes casos.


## 8. Timeout real de 300 segundos e novo pedido

Objetivo: observar o prazo padrão completo sem interferência de outros pares. Pré-requisitos: preparação compilada e terminais na raiz; portas 1199/5121 livres. Não configurar MATCH_TIMEOUT_SECONDS nem registrar outros clientes neste servidor durante os cinco minutos.

1. Terminal 1 — iniciar o servidor isolado:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchTimeoutLocal java -cp /tmp/tictactoe-classes MatchServer
```

Esperado: anúncio do Registry 1199 e objeto 5121.

2. Terminal 2 — registrar somente este cliente e anotar o horário:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchTimeoutLocal PLAYER_IP=127.0.0.1 PLAYER_PORT=6010 PLAYER_PROTOCOL=tcp java -cp /tmp/tictactoe-classes Player
```

Esperado: após cerca de 300 s, `Falha no pareamento [TIMEOUT]: Nenhum oponente encontrado no prazo de 300000 ms`, saída 1. Sem nova tentativa automática.

3. Repetir o comando do terminal 2. Em um terceiro terminal, executar:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchTimeoutLocal PLAYER_IP=127.0.0.1 PLAYER_PORT=6011 PLAYER_PROTOCOL=tcp java -cp /tmp/tictactoe-classes Player
```

Esperado: primeiro recebe porta 6011 e inicia; segundo recebe porta 6010 e aguarda. Isso confirma novo registro sem DUPLICATE após expiração. Encerrar o servidor com Ctrl+C.

Sinais de falha: timeout muito antes do prazo sem ajuste de teste, espera indefinida, tentativa automática ou rejeição do novo pedido como duplicata.

## 9. Desistência sem cancelamento imediato

Objetivo: observar a limitação aceita. Pré-requisitos: preparação e servidor do caso 1 com timeout padrão, sem outros clientes pendentes.

1. Terminal 2 — executar o primeiro cliente do caso 1 (porta 5010) e aguardar.
2. Encerrar esse cliente com Ctrl+C antes do prazo.
3. Repetir imediatamente o mesmo comando: esperado `[DUPLICATE]`, saída 1; encerrar o processo local não cancelou o pedido remoto.
4. Terminal 3 — executar o segundo cliente do caso 1 (porta 5011). Esperado: recebe o endpoint 5010 e informa que o adversário inicia, embora o processo desse adversário já esteja encerrado.
5. Encerrar o servidor com Ctrl+C após observar o resultado.

O passo 4 confirma o comportamento limitado aprovado, não uma partida funcional nem detecção de disponibilidade. Sem novo par, o pedido abandonado deve ser removido pelo prazo de 300 s. Essa variante exige observação própria; não inferir sua execução a partir deste roteiro. Sinais de falha contratual: a repetição substituir o prazo/pedido original ou surgir um mecanismo não aprovado de cancelamento/retry.

## 10. Respostas de match válidas e inválidas

Objetivo: verificar tipos das respostas antes da apresentação no cliente. Pré-requisitos: preparação compilada e `jshell --execution local`, na raiz.

1. Carregar o leitor:

```java
java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of("/tmp/tictactoe-classes").toUri().toURL()});
java.lang.reflect.Method readResponse = loader.loadClass("JsonMessages").getDeclaredMethod("readMatchResponse", String.class);
readResponse.setAccessible(true);
```

2. Executar, uma expressão por vez:

```java
readResponse.invoke(null, (Object) "{\"ip\":\"127.0.0.1\",\"porta\":5011,\"inicia\":true}")
readResponse.invoke(null, (Object) "{\"erro\":\"TIMEOUT\",\"mensagem\":\"Prazo esgotado\"}")
```

Esperado: mapa com os campos e tipos corretos. Repetir o segundo comando substituindo TIMEOUT por DUPLICATE, INVALID_REQUEST e INTERRUPTED; todos devem ser aceitos como respostas de aplicação.

3. Provocar falhas:

```java
readResponse.invoke(null, (Object) "{\"ip\":\"127.0.0.1\",\"porta\":5011,\"inicia\":\"true\"}")
readResponse.invoke(null, (Object) "{\"erro\":\"TIMEOUT\"}")
readResponse.invoke(null, (Object) null)
```

Esperado: cada expressão gera InvocationTargetException com causa IllegalArgumentException, indicando respectivamente inicia não booleano, mensagem ausente e JSON nulo. Sinais de falha: aceitar uma dessas respostas ou tentar usá-la como sucesso. Encerrar com `/exit`. Aceitar um exemplo INTERRUPTED não comprova o fluxo de interrupção real da thread do servidor.


## 11. Entradas separadas por programa

Objetivo: verificar a organização solicitada. Pré-requisitos: compilação da preparação e portas locais livres.

1. Executar o caso 1 usando MatchServer em um terminal e Player nos dois terminais dos jogadores, conforme os comandos atualizados. Esperado: mesmos endpoints e ordem de início, saídas 0.
2. Encerrar MatchServer com Ctrl+C.
3. Em outro terminal, executar:

```bash
java -cp /tmp/tictactoe-classes Display
```

Esperado: `Display multicast ainda não implementado.`, saída 0. Esta é somente uma entrada reservada; não há recepção nem teste de multicast.

Sinais de falha: ClassNotFoundException, necessidade dos antigos argumentos server/client, pareamento divergente ou anúncio de display funcional sem implementação. Falha de pareamento pode ser provocada encerrando MatchServer antes de executar Player; este deve mostrar falha RMI sem retry.


## 12. Atualização do contrato RMI para String

Objetivo: assegurar que a interface é a mesma no servidor e jogadores. Pré-requisitos: encerrar os processos antigos, atualizar MatchService.java e os fontes correspondentes em cada máquina; não misturar versões byte[] e String.

1. Localmente, compilar conforme a preparação e executar o caso 1. Nas VMs com layout plano, em `~/tictactoe`, compilar com:

```bash
javac -encoding UTF-8 *.java
```

2. Recarregar a configuração de cada VM conforme o ambiente já preparado (`source ~/.bashrc` quando ele carrega `.env`). Na .14 iniciar:

```bash
java -cp . MatchServer
```

3. Na .16 e depois na .17, dentro de 300 s:

```bash
java -cp . Player
```

Esperado: ambos recebem IP/porta do adversário e papéis distintos, saídas 0; nenhuma conversão manual ou arquivo de stub precisa ser criado. Confirmar erros pelos casos 3 a 5, observando o prazo reduzido somente quando configurado explicitamente.

4. Para conferir a assinatura no JShell do caso 10:

```java
loader.loadClass("MatchService").getDeclaredMethod("getOponent", String.class).getReturnType()
```

Esperado: `class java.lang.String`.

5. Para provocar falha de validação sem usar bytes, executar no JShell com o método readResponse preparado no caso 10:

```java
readResponse.invoke(null, (Object) null)
readResponse.invoke(null, " ".repeat(16385))
```

Esperado: InvocationTargetException com causa IllegalArgumentException, indicando JSON ausente ou maior que 16384 caracteres. String.length conta unidades UTF-16; isso é um limite local de entrada e não define a codificação do transporte RMI.

Sinais de falha: método remoto não reconhecido ou incompatibilidade de interface, campos inválidos aceitos ou apenas um jogador retornando. Para investigar mistura de versões, verificar todos os fontes/compilados, parar processos antigos, recompilar e reiniciar; não copiar uma interface antiga para testar em serviço compartilhado. Encerrar o servidor de teste com Ctrl+C após observar os resultados.
# Caso 13 — regras locais separadas (sem rede)

Objetivo: verificar o tabuleiro e as rejeições sem executar RMI ou TCP. Pré-requisitos: JDK 17 com javac e jshell; terminal na raiz do projeto. Nenhum servidor é necessário.

1. Compilar sem gerar arquivos no repositório:

```bash
mkdir -p /tmp/tictactoe-rules
javac -encoding UTF-8 -d /tmp/tictactoe-rules code/*.java
jshell --execution local
```

2. No JShell, carregar a classe e provocar falhas:

```java
/open code/TicTacToeRules.java
TicTacToeRules rules = new TicTacToeRules();
rules.getAcceptedMoveCount()
rules.isGameOver()
rules.placeMove(-1, 1)
rules.placeMove(9, 1)
rules.placeMove(0, 0)
rules.placeMove(0, 1)
rules.placeMove(0, 2)
rules.placeMove(1, 1)
rules.getAcceptedMoveCount()
int[] copy = rules.getBoardCopy();
copy[0] = 2;
rules.getBoardCopy()[0]
```

Esperado no único terminal: inicialmente 0 e false; exceções explicativas para -1, 9, símbolo 0, casa ocupada e repetição de símbolo. Contador continua 1; a alteração da cópia não muda a casa original, que continua 1.

3. Completar uma vitória de X:

```java
rules.placeMove(3, 2)
rules.placeMove(1, 1)
rules.placeMove(4, 2)
rules.placeMove(2, 1)
rules.getWinner()
rules.isGameOver()
rules.isDraw()
rules.getAcceptedMoveCount()
rules.placeMove(8, 2)
```

Esperado: 1, true, false, 5 e rejeição de jogada após encerramento. Repetir com novas instâncias para as linhas 3/4/5, 6/7/8, 0/3/6, 1/4/7, 2/5/8, 0/4/8 e 2/4/6, alternando com duas casas fora da linha. Repetir com símbolo 2 vencedor; a sessão ainda não determina quem será X/O.

4. Verificar empate:

```java
TicTacToeRules draw = new TicTacToeRules();
int[] positions = {0,1,2,4,3,5,7,6,8};
for (int index = 0; index < positions.length; index++) { draw.placeMove(positions[index], index % 2 + 1); }
draw.isDraw()
draw.getWinner()
draw.getAcceptedMoveCount()
/exit
```

Esperado: true, 0 e 9. Sinais de falha: rejeição altera estado/contador, cópia altera original, vitória não identificada, empate com vencedor, ou jogada aceita após encerramento. A verificação local não comprova rede nem os JSONs da partida.

## Caso 14 — partida TCP completa após RMI

Objetivo: confirmar conexão, turnos, resultado em ambos os jogadores e encerramento normal. Pré-requisitos: JDK 17, três terminais na raiz do projeto, portas locais 1199, 5121, 6010 e 6011 disponíveis. Nenhum display necessário. Os exemplos de portas não fixam portas das VMs.

1. Terminal 1 — compilar e iniciar match:

```bash
mkdir -p /tmp/tictactoe-tcp
javac -encoding UTF-8 -d /tmp/tictactoe-tcp code/*.java
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchServiceTcp MATCH_TIMEOUT_SECONDS=300 java -cp /tmp/tictactoe-tcp MatchServer
```

2. Terminal 2 — iniciar primeiro jogador (X):

```bash
PLAYER_IP=127.0.0.1 PLAYER_PORT=6010 PLAYER_PROTOCOL=tcp RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchServiceTcp java -cp /tmp/tictactoe-tcp Player
```

3. Terminal 3 — iniciar segundo jogador (O), dentro de 300 s:

```bash
PLAYER_IP=127.0.0.1 PLAYER_PORT=6011 PLAYER_PROTOCOL=tcp RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchServiceTcp java -cp /tmp/tictactoe-tcp Player
```

Esperado: terminal 1 mantém Registry ativo; terminais 2/3 mostram mesmo UUID e símbolos X/O, respectivamente. X escolhe a casa; O aguarda. Cada entrada abaixo é digitada apenas no jogador indicado, seguida de Enter. Efetuar cada jogada dentro de 120 s.

4. Vitória X: terminal 2 digita 0; terminal 3 digita 3; terminal 2 digita 1; terminal 3 digita 4; terminal 2 digita 2. Esperado: tabuleiros iguais, terminal 2 venceu, terminal 3 perdeu; ambos encerram com saída 0. Conferir `echo $?` após cada processo. Terminal 1 continua ativo.
5. Reiniciar os dois jogadores pelos mesmos comandos e testar vitória O: X0, O3, X1, O4, X8, O5. Esperado: X perdeu e O venceu, saídas 0.
6. Reiniciar jogadores e testar empate: X0, O1, X2, O4, X3, O5, X7, O6, X8. Esperado: empate nos dois terminais e saídas 0.
7. Provocar falhas de entrada em nova partida: em X digitar abc, 9 e depois 0; em O digitar 0 e depois 3. Esperado: número inválido, casa fora do intervalo e casa ocupada são recusados; primeira jogada efetiva continua X0, segunda O3. Para encerrar este ensaio, Ctrl+C em um jogador; o outro deve detectar desconexão e sair com 1, mesmo aguardando o teclado.
8. Reiniciar jogadores e não digitar nada em X. Esperado após aproximadamente 120 s: O informa inatividade e encerra; X recebe fim remoto enquanto espera teclado e também encerra; saídas 1. Não reduzir prazo neste caso.
9. Para provocar EOF, iniciar outra partida e pressionar Ctrl+D no terminal da vez em uma linha vazia. Esperado: erro local, tentativa de avisar adversário e saída 1. **Este caso EOF ainda não foi executado pelo agente.**
10. Encerrar servidor de teste com Ctrl+C. Se houver clientes abandonados durante o match, reiniciar o servidor de teste isolado antes de repetir; a política aprovada não cancela imediatamente pedidos abandonados.

Sinais de falha: tabuleiros divergentes, dois participantes jogando como X, resultado normal com saída 1, falta de confirmação final, jogador bloqueado após timeout/desconexão, ou erro de entrada avançando contador.

Nas VMs: usar a distribuição de arquivos do README em ~/tictactoe; em cada máquina executar `cd ~/tictactoe`, `javac -encoding UTF-8 *.java` e `source ~/.bashrc`. Na .14 executar `java -cp . MatchServer`; na .16 e depois .17 executar `java -cp . Player`. Usar PLAYER_IP da própria VM, portas disponíveis e acessíveis entre jogadores, Registry9821 e a configuração do serviço já preparada. Repetir passos 4–8; resultados TCP nas VMs ainda **não executados** nesta etapa.

## Caso 15 — rejeição, correção e falha de protocolo por TCP

Objetivo: verificar o wire manualmente usando JShell como adversário. Pré-requisitos: compilação/servidor do caso 14, fila vazia, portas 6022/6023 livres, terminais separados para Player e JShell. Não há programa de teste automatizado.

1. No terminal do jogador:

```bash
PLAYER_IP=127.0.0.1 PLAYER_PORT=6022 PLAYER_PROTOCOL=tcp RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchServiceTcp java -cp /tmp/tictactoe-tcp Player
```

2. Em outro terminal iniciar:

```bash
jshell --execution local
```

3. No JShell carregar a interface e registrar o adversário. O contexto de classloader e a chamada RMI devem estar no mesmo bloco, pois o JShell executa snippets em threads próprias:

```java
java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{new java.io.File("/tmp/tictactoe-tcp").toURI().toURL()});
java.net.ServerSocket listener = new java.net.ServerSocket(6023);
{ Thread.currentThread().setContextClassLoader(loader); Object service = java.rmi.registry.LocateRegistry.getRegistry("127.0.0.1",1199).lookup("MatchServiceTcp"); System.out.println(loader.loadClass("MatchService").getMethod("getOponent",String.class).invoke(service,"{\"ip\":\"127.0.0.1\",\"porta\":6023,\"protocolo\":\"tcp\"}")); }
java.net.Socket peer = listener.accept();
peer.setSoTimeout(10000);
java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(peer.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(peer.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8),true);
String initial = reader.readLine();
String id = initial.split("\"partidaId\":\"")[1].split("\"")[0];
System.out.println(initial);
```

Esperado: resposta RMI inicia=false para JShell e controle inicio com UUID. O Player joga com X. Não deixar este diagnóstico parado por 120 s entre os passos, salvo ao testar expiração deliberadamente.

4. No Player digitar 0. No JShell ler e rejeitar:

```java
System.out.println(reader.readLine());
writer.println("{\"partidaId\":\""+id+"\",\"evento\":\"rejeicao\",\"motivo\":\"Correção manual\",\"refazer\":true}");
```

Esperado: Numero0, X na casa0; depois Player explica rejeição e restaura tabuleiro vazio. No Player digitar 1. No JShell:

```java
System.out.println(reader.readLine());
writer.println("{\"Numero\":5,\"Jogada\":0,\"Tabuleiro\":[2,1,0,0,0,0,0,0,0],\"Resultado\":\"jog\"}");
System.out.println(reader.readLine());
writer.println("{\"Numero\":1,\"Jogada\":0,\"Tabuleiro\":[0,1,0,0,0,0,0,0,0],\"Resultado\":\"jog\"}");
System.out.println(reader.readLine());
writer.println("{\"Numero\":1,\"Jogada\":0,\"Tabuleiro\":[2,1,0,0,0,0,0,0,0],\"Resultado\":\"jog\"}");
```

Esperado: nova tentativa X1 ainda Numero0, sem X0. Receber duas rejeições explicativas com mesmo partidaId e refazer true: sequência incorreta e tabuleiro divergente. Correção O0/Numero1 aceita, Player mostra O0/X1 e pede próxima jogada.

5. No JShell fechar:

```java
peer.close();
listener.close();
/exit
```

Esperado: Player detecta encerramento mesmo aguardando teclado, sai1. Sinais de falha: X0 mantido após rejeição, contador avança na tentativa corrigida, tabuleiro inválido aceito, motivo ausente ou bloqueio após desconexão.

6. Cenários adicionais, cada um em uma sessão nova preparada pelos passos1–3: enviar `writer.println("{");` para provocar JSON ilegível; enviar objeto de jogada sem campos; enviar Numero/Jogada fracionários; enviar controle com partidaId divergente. Esperado: JSON ilegível ou controle incompatível encerram com erro; jogada legível inválida recebe rejeição. Para mensagem incompleta, usar `writer.print("{"); writer.flush();` e aguardar120s sem finalizar linha: deve expirar. Para limite usar `writer.println(" ".repeat(16385));`: deve encerrar por erro. **Esses cenários adicionais ainda não foram executados em rede pelo agente.**
7. Para verificar prazo preservado, repetir passo4 com rejeições/correções espaçadas até superar120s a partir da primeira tentativa, sem reiniciar sessão. Esperado: expiração sem conceder outro prazo integral. **Não executado nesta etapa.**

Ao terminar, fechar JShell e servidor de teste. Não usar esses comandos de injeção num serviço compartilhado das VMs.

## Caso 16 — sessão TCP em arquivo separado

Objetivo: confirmar compilação de Player com TcpGameSession em arquivo separado. Pré-requisitos: JDK17 e terminal na raiz do projeto. Não exige rede.

1. Compilar em pasta temporária nova:

```bash
javac -encoding UTF-8 -d /tmp/tictactoe-separate-session code/*.java
```

2. Conferir fontes e classes geradas:

```bash
ls code
ls /tmp/tictactoe-separate-session/Player.class /tmp/tictactoe-separate-session/TcpGameSession.class
```

Esperado: nove fontes, incluindo Player.java e TcpGameSession.java separados; compilação concluída, com avisos unchecked possíveis. Sinais de falha: erro de compilação ou dependência de classe interna removida. Para provocar falha de distribuição, omitir TcpGameSession.java em uma pasta de implantação temporária limpa; a compilação deve acusar classe ausente. Não remover arquivos do workspace para esse ensaio.

3. Para validar comportamento, repetir o caso14 usando /tmp/tictactoe-separate-session como classpath ou recompilar nas VMs conforme README. Compilação desta reorganização: **passou**; partidas: **não reexecutadas**.

## Caso 17 — partida com múltiplos displays multicast

Objetivo: verificar RMI, TCP e publicação/recepção multicast juntos. Pré-requisitos: JDK17, cinco terminais na raiz do projeto, interface lo ativa e portas1199/5121/6010/6011/8821 livres. Configuração abaixo é só local: TTL0 não deve ser usado como configuração remota. Não exige nc/tcpdump nem programa de teste automatizado.

1. Terminal1 — compilar e iniciar match:

```bash
mkdir -p /tmp/tictactoe-multicast
javac -encoding UTF-8 -d /tmp/tictactoe-multicast code/*.java
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchServiceMulticast MATCH_TIMEOUT_SECONDS=300 java -cp /tmp/tictactoe-multicast MatchServer
```

2. Terminais2 e3 — executar o mesmo comando, um display em cada terminal:

```bash
MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-multicast Display
```

Esperado: ambos informam que escutam225.1.1.21:8821 pela interface lo; permanecem ativos.

3. Terminal4 — jogadorX:

```bash
PLAYER_IP=127.0.0.1 PLAYER_PORT=6010 PLAYER_PROTOCOL=tcp RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchServiceMulticast MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-multicast Player
```

4. Terminal5 — jogadorO, dentro de300s:

```bash
PLAYER_IP=127.0.0.1 PLAYER_PORT=6011 PLAYER_PROTOCOL=tcp RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchServiceMulticast MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-multicast Player
```

5. Digitar9 em X, depois jogar X0/O3/X1/O4/X2. Esperado:9 recusado no jogador, sem publicação; displays mostram exatamente as jogadas0..4 e vitória deX. Terminais4/5 mostram venceu/perdeu e encerram com saída0 (`echo $?`). Terminal1 e displays continuam ativos. Nenhuma confirmação de display é necessária.
6. Reiniciar jogadores pelos mesmos comandos e jogar X0/O1. Fechar um display com Ctrl+C. Continuar X2/O4/X3/O5/X7/O6/X8. Esperado: partida continua, display restante mostra0..8 e empate; ambos os jogadores saem0. O display encerrado não recebe novas mensagens e não afeta o outro.
7. Encerrar o display restante. Reiniciar X com MULTICAST_INTERFACE=interface_inexistente no comando do passo3; iniciar O normalmente. Jogar X0/O3/X1/O4/X2. Esperado: X avisa publicação indisponível, mas ambos concluem normalmente com saída0, sem display. Falha de publicação não equivale a falhaTCP.
8. Em terminal livre executar:

```bash
MULTICAST_INTERFACE=interface_inexistente java -cp /tmp/tictactoe-multicast Display
```

Esperado: mensagem de configuração inválida e saída1, sem afetar servidor/jogadores. Para testar TTL inválido, substituir pela interface lo e MULTICAST_TTL=256; esperado rejeitar configuração. **TTL inválido não executado nesta etapa.**
9. Encerrar match com Ctrl+C e todos os displays restantes. Se abandonar cliente antes de match, reiniciar apenas o servidor de teste isolado antes de repetir; pedido abandonado não é cancelado imediatamente.

Sinais de falha: apenas um display consegue abrir a porta, snapshots inconsistentes, tentativa inválida exibida, falta de resultado após último datagrama recebido, encerramento do jogo causado por display ausente ou publicador mal configurado. A perda de um datagrama em outra rede não significa que a partidaTCP falhou; multicast não confirma entrega.

Entre VMs: usar arquivos por máquina do README, manter grupo225.1.1.21/porta8821 e substituir lo pelo nome da interface real em cada .env. Escolher TTL compatível com a topologia, recompilar (`javac -encoding UTF-8 *.java`) e recarregar ambiente (`source ~/.bashrc`). Iniciar `java -cp . MatchServer` na.14, `java -cp . Display` nos terminais de display da.16/.17 e `java -cp . Player` em outro terminal de cada jogador. Repetir passos5–7. **Não executado entre VMs; TTL/interface definitivos não confirmados.**

## Caso 18 — descarte de datagramas inválidos e recuperação

Objetivo: assegurar que entrada inválida não encerra o Display. Pré-requisitos: compilação e displays do caso17 ativos em lo/TTL0, nenhuma partida em andamento; terminal extra com JDK17. Não enviar estes exemplos no grupo remoto compartilhado.

1. No terminal extra iniciar:

```bash
jshell --execution local
```

2. Configurar emissor local e provocar falhas, digitando no JShell:

```java
java.net.MulticastSocket sender = new java.net.MulticastSocket();
sender.setNetworkInterface(java.net.NetworkInterface.getByName("lo"));
sender.setTimeToLive(0);
java.net.InetAddress group = java.net.InetAddress.getByName("225.1.1.21");
byte[] malformed = "{".getBytes(java.nio.charset.StandardCharsets.UTF_8);
sender.send(new java.net.DatagramPacket(malformed,malformed.length,group,8821));
byte[] missingFields = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
sender.send(new java.net.DatagramPacket(missingFields,missingFields.length,group,8821));
byte[] badEncoding = {(byte) 0xc3};
sender.send(new java.net.DatagramPacket(badEncoding,badEncoding.length,group,8821));
byte[] tooLarge = " ".repeat(16385).getBytes(java.nio.charset.StandardCharsets.UTF_8);
sender.send(new java.net.DatagramPacket(tooLarge,tooLarge.length,group,8821));
byte[] inconsistent = "{\"Numero\":0,\"Jogada\":0,\"Tabuleiro\":[2,0,0,0,0,0,0,0,0],\"Resultado\":\"jog\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
sender.send(new java.net.DatagramPacket(inconsistent,inconsistent.length,group,8821));
```

Esperado em cada display: cinco avisos de descarte (JSON inválido, campo ausente, UTF-8 inválido, tamanho excessivo, tabuleiro incompatível), sem encerramento. O JShell apenas envia; não recebe ACK.

3. Ainda no JShell, enviar snapshot válido e fechar:

```java
byte[] valid = "{\"Numero\":0,\"Jogada\":0,\"Tabuleiro\":[1,0,0,0,0,0,0,0,0],\"Resultado\":\"jog\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
sender.send(new java.net.DatagramPacket(valid,valid.length,group,8821));
sender.close();
/exit
```

Esperado: ambos os displays exibem jogada0 com X0 após os avisos e continuam ativos. Sinais de falha: mensagem inválida exibida como tabuleiro, erro derruba receptor ou pacote válido posterior não pode ser processado. Ao terminar, encerrar displays com Ctrl+C.

Cenários ainda não executados: interface removida após abrir socket, falha real no envio, perda/reordenação/duplicação e início tardio. O Display mostra snapshots independentes, sem histórico ou separação de partidas simultâneas; não há promessa de recuperação de datagramas perdidos.

## Caso 19 — métodos multicast diretamente em Display

Objetivo: confirmar a reorganização sem classe interna ou arquivo MulticastChannel. Pré-requisitos: JDK17, terminal na raiz e pasta temporária nova.

1. Compilar e conferir os fontes:

```bash
javac -encoding UTF-8 -d /tmp/tictactoe-display-methods code/*.java
ls code
```

Esperado: nove fontes, métodos multicast na classe Display; nenhum MulticastChannel.java. Compilação passou com avisos unchecked esperados. Sinais de falha: classe ausente ou dependência do fonte removido. Para provocar falha de distribuição, omitir Display.java numa pasta temporária de implantação limpa e compilar os fontes do jogador: TcpGameSession deve acusar a classe ausente. Não remover fontes do workspace.

2. Repetir casos17/18 com classpath /tmp/tictactoe-display-methods para partidas completas. **Partidas completas não reexecutadas nesta reorganização.** Uma verificação manual via JShell chamou Display.openPublisher, publishMove e close por reflexão e um processo Display separado recebeu/exibiu o snapshot X0 em lo/TTL0: **passou**. Os comandos de inicialização e as variáveis de ambiente dos casos17/18 permanecem válidos.

## Caso 20 — descoberta automática de IP e porta

Objetivo: reservar e anunciar o endpoint real sem PLAYER_IP/PLAYER_PORT. Pré-requisitos: JDK17 no PATH, fontes atuais, três terminais no projeto; portas1199/5121 livres. Estes comandos são para execução manual.

1. Compilar:

```bash
mkdir -p /tmp/tictactoe-discovery
javac -encoding UTF-8 -d /tmp/tictactoe-discovery code/*.java
```

2. Terminal1, iniciar o servidor:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchDiscovery java -cp /tmp/tictactoe-discovery MatchServer
```

3. Terminal2, e depois terminal3, executar o mesmo comando:

```bash
env -u PLAYER_IP -u PLAYER_PORT RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchDiscovery MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-discovery Player
```

Esperado no terminal1: Registry iniciado. No terminal2: Jogador escutando em127.0.0.1 e uma porta não zero, buscando adversário; após terminal3, conexão como X. Terminal3: outra porta não zero, conexão como O. Não exigir números específicos de porta.

4. Alternar entradas: terminal2=0, terminal3=3, terminal2=1, terminal3=4, terminal2=2. Esperado: vitória no terminal2, derrota no terminal3, ambos encerram normalmente. Encerrar servidor com Ctrl+C após concluir. Sinais de falha: porta anunciada0, endpoint errado, erro de bind ou falta de conexão entre jogadores.

5. Para provocar falha de descoberta, com servidor desligado repetir comando do passo3: esperado erro contextual de descoberta pelo Registry, saída1, sem pedido de match. Para verificar override, reiniciar servidor e repetir passo3 usando PLAYER_IP=127.0.0.1 PLAYER_PORT=5010 no lugar de `-u PLAYER_IP -u PLAYER_PORT` em um terminal e porta5011 no outro. Esperado: portas configuradas anunciadas e partida normal. Para provocar erro de porta, usar PLAYER_PORT=abc: esperado configuração inválida e saída1 antes do registro. Esses cenários de falha/override não foram executados nesta alteração.

6. Nas VMs, recompilar em ~/tictactoe com `javac -encoding UTF-8 *.java`, retirar PLAYER_IP/PLAYER_PORT do .env e executar `unset PLAYER_IP PLAYER_PORT` seguido de `source ~/.bashrc` em cada terminal existente. Iniciar MatchServer na .14 e Player nas .16/.17. Esperado: IP local de cada VM, porta atribuída e partida completa. Se o match funcionar mas TCP falhar, verificar alcance do endereço/porta e regras da rede; porta automática não abre firewall. VMs: não executado.

Resultado local real: compilação e passos2–4 passaram; portas37465/40295, vitóriaX, saída0 de ambos. Não foi executado receptor Display nesta verificação; multicast usou lo/TTL0. Processos encerrados.

## Caso 21 — ID da partida antes do tabuleiro no Display

Objetivo: identificar cada snapshot pelo UUID compartilhado pelos jogadores, preservando a jogada TCP. Pré-requisitos: JDK17, quatro terminais na raiz do projeto, fontes novos nos jogadores e Display; portas1199/5121 livres. Roteiros17–19 que injetam datagramas sem envelope são históricos: o formato novo exige partidaId e jogada.

1. Compilar:

```bash
mkdir -p /tmp/tictactoe-display-id
javac -encoding UTF-8 -d /tmp/tictactoe-display-id code/*.java
```

2. Terminal1:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_OBJECT_PORT=5121 RMI_SERVICE_NAME=MatchDisplayId java -cp /tmp/tictactoe-display-id MatchServer
```

Esperado: Registry iniciado com o serviço indicado.

3. Terminal2:

```bash
MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-display-id Display
```

Esperado: escutando225.1.1.21:8821 pela interface lo.

4. Terminais3 e4, nesta ordem, executar:

```bash
env -u PLAYER_IP -u PLAYER_PORT RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1199 RMI_SERVICE_NAME=MatchDisplayId MULTICAST_INTERFACE=lo MULTICAST_TTL=0 java -cp /tmp/tictactoe-display-id Player
```

Esperado: portas atribuídas automaticamente, mesmo ID nos dois jogadores; terminal3=X, terminal4=O. Alternar entradas3=0,4=3,3=1,4=4,3=2. Display deve mostrar `Partida: UUID` antes de cada snapshot, sempre com o mesmo UUID desta partida, jogadas0..4 e vitóriaX. Jogadores: vitória/derrota e saída0. Sinais de falha: ID ausente/diferente dos jogadores ou datagramas válidos descartados. Encerrar Display/servidor com Ctrl+C.

5. Para verificar jogos simultâneos, manter servidor/Display e abrir quatro jogadores com o comando do passo4 em quatro terminais: esperar o primeiro par conectar antes de iniciar o segundo. Alternar jogadas nos dois pares. Esperado: cada atualização identificada pelo ID do par correto, mesmo com snapshots intercalados. Não há menu nem separação fixa na tela. Este cenário não foi executado nesta alteração.

6. Para provocar incompatibilidade de formato, executar um publicador compilado da versão anterior (sem envelope) contra o Display novo, em ambiente local separado: esperado aviso de datagrama descartado, sem encerrar receptor. Não misturar versões nas VMs. Este cenário e envelopes com tipos inválidos não foram executados nesta alteração.

Resultado real: compilação e passos2–4 passaram em localhost, cinco snapshots com o mesmo UUID e vitóriaX. Todos os processos encerrados. VMs e passos5–6 não executados.
