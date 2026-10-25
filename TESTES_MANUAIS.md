# Testes manuais do servidor de match

Os formatos de erro, duplicidade, ordem de início, desistência por timeout e ausência de retries automáticos foram aprovados pelo responsável. O JSON no transporte permanece como documentado; o cliente agora mostra mensagens legíveis e encerra erros com saída 1. Não há garantia de entrega a clientes desconectados.

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

Registrar apenas execuções observadas na seção 9 do `PLANO_DO_PROJETO.md`, com ambiente e parâmetros usados, e refletir no quadro consolidado da seção 7. A seção 10 é histórico da versão org.json. O timeout de 2 s é histórico anterior. O prazo real de 300 s, novo registro após expiração, protocolos separados e a limitação de desistência foram executados na etapa do contrato aprovado; conferir evidências na seção 9 do plano. Compilação não comprova funcionamento entre VMs. Limpeza por timeout de pedido abandonado sem par, interrupção real, partida e multicast continuam exigindo execução própria.

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
java.lang.reflect.Method player = loader.loadClass("JsonMessages").getDeclaredMethod("readPlayerRequest", byte[].class);
player.setAccessible(true);
player.invoke(null, (Object) new byte[]{(byte)0xc3,(byte)0x28})
```

Esperado: cada entrada inválida gera `InvocationTargetException` cuja causa é `IllegalArgumentException`; a última informa UTF-8 inválido. Sinais de falha: aceitar essas entradas, perder conteúdo no passo 3 ou encerrar o JShell inesperadamente.

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
java.lang.reflect.Method readResponse = loader.loadClass("JsonMessages").getDeclaredMethod("readMatchResponse", byte[].class);
readResponse.setAccessible(true);
```

2. Executar, uma expressão por vez:

```java
readResponse.invoke(null, (Object) "{\"ip\":\"127.0.0.1\",\"porta\":5011,\"inicia\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
readResponse.invoke(null, (Object) "{\"erro\":\"TIMEOUT\",\"mensagem\":\"Prazo esgotado\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
```

Esperado: mapa com os campos e tipos corretos. Repetir o segundo comando substituindo TIMEOUT por DUPLICATE, INVALID_REQUEST e INTERRUPTED; todos devem ser aceitos como respostas de aplicação.

3. Provocar falhas:

```java
readResponse.invoke(null, (Object) "{\"ip\":\"127.0.0.1\",\"porta\":5011,\"inicia\":\"true\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
readResponse.invoke(null, (Object) "{\"erro\":\"TIMEOUT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
readResponse.invoke(null, (Object) new byte[]{(byte)0xc3,(byte)0x28})
```

Esperado: cada expressão gera InvocationTargetException com causa IllegalArgumentException, indicando respectivamente inicia não booleano, mensagem ausente e UTF-8 inválido. Sinais de falha: aceitar uma dessas respostas ou tentar usá-la como sucesso. Encerrar com `/exit`. Aceitar um exemplo INTERRUPTED não comprova o fluxo de interrupção real da thread do servidor.


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
