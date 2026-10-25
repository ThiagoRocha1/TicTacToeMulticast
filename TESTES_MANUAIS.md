# Testes manuais do servidor de match

Os formatos de erro, duplicidade e ordem de início abaixo descrevem o comportamento da base importada; sua adoção definitiva ainda está pendente conforme os documentos anteriores do projeto.

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
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_OBJECT_PORT=5021 RMI_SERVICE_NAME=MatchServiceLocal java -cp /tmp/tictactoe-classes Main server
```

Esperado: anúncio do Registry e do objeto remoto. Manter aberto.

2. Terminal 2 — primeiro cliente:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_SERVICE_NAME=MatchServiceLocal PLAYER_IP=127.0.0.1 PLAYER_PORT=5010 PLAYER_PROTOCOL=tcp java -cp /tmp/tictactoe-classes Main client
```

Esperado: aguarda sem retorno até o segundo registro (ou timeout).

3. Terminal 3 — segundo cliente:

```bash
RMI_HOST=127.0.0.1 RMI_REGISTRY_PORT=1099 RMI_SERVICE_NAME=MatchServiceLocal PLAYER_IP=127.0.0.1 PLAYER_PORT=5011 PLAYER_PROTOCOL=tcp java -cp /tmp/tictactoe-classes Main client
```

Esperado no terminal 2: `ip=127.0.0.1`, `porta=5011`, `inicia=true`. No terminal 3: `porta=5010`, `inicia=false`. A ordem dos campos JSON não importa. Ambos encerram.

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

Esperado em ambas as tentativas: JSON `erro=TIMEOUT` e mensagem explicativa após aproximadamente 2 segundos. A segunda tentativa deve aguardar novamente, sem `DUPLICATE`. Falha: espera indefinida ou registro expirado bloqueando o novo pedido. O valor reduzido é somente para teste; o padrão é 300 s.

## 4. Duplicidade e protocolos distintos

Objetivo: verificar que duplicatas não substituem o primeiro cliente e TCP não pareia com UDP. Reiniciar o servidor do caso 1 com o timeout padrão.

1. Iniciar o primeiro cliente TCP e mantê-lo aguardando.
2. Em outro terminal, repetir exatamente o mesmo comando do cliente: esperado `erro=DUPLICATE`.
3. Iniciar outro cliente com `PLAYER_PORT=5012 PLAYER_PROTOCOL=udp`: deve continuar aguardando; não pode liberar o TCP.
4. Iniciar o segundo cliente TCP do caso 1: deve parear com o primeiro TCP.
5. Iniciar um cliente com `PLAYER_PORT=5013 PLAYER_PROTOCOL=udp`: deve liberar o pedido UDP do passo 3.

Esperado: pares TCP 5010/5011 e UDP 5012/5013, um `inicia=true` por par. Falha: protocolo cruzado, duplicata aceita como oponente ou primeiro registro perdido. O teste UDP exercita somente a classificação de pedidos no RMI; não implementa jogo por UDP.

## 5. Entrada inválida e servidor indisponível

Objetivo: provocar erro de aplicação e de transporte sem espera indevida.

1. Com o servidor do caso 1 ativo, executar o cliente com `PLAYER_PROTOCOL=http`: esperado JSON `erro=INVALID_REQUEST`.
2. Repetir com `PLAYER_IP=999.0.0.1`: esperado `INVALID_REQUEST`.
3. Encerrar o servidor com Ctrl+C e executar um cliente: esperado erro RMI de conexão, sem busca infinita de oponente. Retries não foram implementados.

Falha: pedido inválido entra na fila, servidor encerra por entrada inválida ou cliente fica indefinidamente aguardando um Registry desligado em localhost.

## Resultados

Registrar apenas execuções observadas na seção 9 do `PLANO_DO_PROJETO.md`, com ambiente e parâmetros usados, e refletir no quadro consolidado da seção 7. A seção 10 é histórico da versão org.json. O timeout de 2 s já registrado pertence a uma revisão anterior e não foi reexecutado na última validação. Compilação não comprova funcionamento entre VMs. Testes com o timeout padrão de 300 s, desistência por fechamento do cliente, tráfego JSON malformado, partida e multicast precisam de registro próprio quando executados.

## 6. Utilitário JSON sem biblioteca

Objetivo: conferir leitura, escrita e rejeição de entradas inválidas. Pré-requisito: JDK 17 e compilação da preparação concluída; executar na raiz do projeto.

1. Em um terminal, executar `jshell --execution local`.
2. Carregar as classes compiladas e liberar os métodos para inspeção manual:

```java
var loader = new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of("/tmp/tictactoe-classes").toAbsolutePath().toUri().toURL()});
var parse = loader.loadClass("JsonUtil").getDeclaredMethod("parseJson", String.class);
parse.setAccessible(true);
var write = loader.loadClass("JsonUtil").getDeclaredMethod("toJson", Object.class);
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
var player = loader.loadClass("JsonMessages").getDeclaredMethod("readPlayerRequest", byte[].class);
player.setAccessible(true);
player.invoke(null, (Object) new byte[]{(byte)0xc3,(byte)0x28})
```

Esperado: cada entrada inválida gera `InvocationTargetException` cuja causa é `IllegalArgumentException`; a última informa UTF-8 inválido. Sinais de falha: aceitar essas entradas, perder conteúdo no passo 3 ou encerrar o JShell inesperadamente.

5. Encerrar com `/exit`. Repetir o caso 1 para verificar RMI com o utilitário; não usar o resultado histórico da versão org.json como prova desta implementação.


## 7. Configuração numérica inválida

Objetivo: verificar tratamento específico das conversões de configuração. Pré-requisitos: JDK 17, compilação da preparação concluída; nenhum servidor é necessário.

1. No terminal, executar:

```bash
RMI_REGISTRY_PORT=abc java -cp /tmp/tictactoe-classes Main client
```

Esperado: `Configuração inválida: RMI_REGISTRY_PORT deve conter uma porta inteira`, encerramento com código 1. Conferir imediatamente com `echo $?`.

2. Provocar timeout não numérico:

```bash
MATCH_TIMEOUT_SECONDS=abc RMI_OBJECT_PORT=5021 java -cp /tmp/tictactoe-classes Main server
```

Esperado: `Configuração inválida: MATCH_TIMEOUT_SECONDS deve conter um inteiro positivo`, código 1, sem iniciar servidor.

3. Provocar valor que não cabe no prazo interno em nanossegundos:

```bash
MATCH_TIMEOUT_SECONDS=9223372036854775807 RMI_OBJECT_PORT=5021 java -cp /tmp/tictactoe-classes Main server
```

Esperado: `Configuração inválida: MATCH_TIMEOUT_SECONDS fora do intervalo permitido`, código 1.

Sinais de falha em qualquer passo: anunciar servidor ativo, ignorar o valor inválido ou exibir somente um stack trace sem contexto da configuração. Este módulo usa variáveis de ambiente, não leitura interativa: EOF e normalização com `toLowerCase` não se aplicam a estes casos.
