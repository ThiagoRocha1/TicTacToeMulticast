# TicTacToeMulticast

Projeto acadêmico de **Sistemas Distribuídos**: jogo da velha distribuído para dois jogadores, com pareamento por **Java RMI**, comunicação direta por **sockets TCP** (e, em uma fase posterior, UDP) e publicação das jogadas para um ou mais **displays multicast**.

> **Estado atual (24/10/2026):** planejamento, **item 1 em consolidação**. Decisões técnicas foram registradas, mas **o item 2 não está autorizado**. Não há implementação Java ou teste manual registrado como executado.

## Documentação para pessoas e agentes

- [`AGENTS.md`](AGENTS.md): regras de trabalho para agentes neste repositório; **leitura obrigatória antes de qualquer alteração**.
- [`PLANO_DO_PROJETO.md`](PLANO_DO_PROJETO.md): fonte de continuidade, decisões aprovadas, pendências, etapas e status dos testes manuais.
- **Enunciado:** `trab-2-rmi-multi-2026-1.pdf`, do Prof. Alexandre Sztajnberg, quatro páginas, incluindo a **Figura 1 (Arquitetura)**. O arquivo foi fornecido na conversa; sua presença efetiva no repositório deve ser verificada.

O PDF é a fonte primária dos **requisitos do professor**; decisões explícitas do responsável registradas no plano complementam os pontos que o PDF deixa em aberto. Não confundir escolhas do projeto com exigências textuais do enunciado. Embora o cabeçalho do PDF diga `2023/1`, o responsável **confirmou que ele continua válido para 2026/1**.

## O que o sistema fará

| Programa | Responsabilidade | Comunicação |
| --- | --- | --- |
| **Servidor de match** | Recebe pedidos de partida, mantém chamadas pendentes, pareia **dois jogadores com o mesmo protocolo** e retorna os dados do adversário para **ambos**. | RMI |
| **Jogador** | Solicita um adversário, joga **uma partida** de jogo da velha, troca mensagens diretamente com o oponente e publica cada jogada aos displays. | RMI + TCP (UDP futuramente) + multicast |
| **Display** | Recebe jogadas pelo grupo multicast e apresenta o tabuleiro em texto; podem existir vários displays. | Multicast |

**Fluxo de alto nível, conforme o PDF:** os jogadores chamam `getOponent` no servidor RMI; o servidor finaliza as duas solicitações quando encontra um par; `inicia=true` joga primeiro e `inicia=false` aguarda; as jogadas vão diretamente entre os jogadores por socket; as jogadas são publicadas aos displays por multicast. O servidor de match **não precisa retransmitir cada jogada**.

## Ambiente e parâmetros definidos

| Parâmetro | Valor |
| --- | --- |
| Linguagem / JDK nas VMs | Java 17 (`17.0.20.1` informado) |
| Número de conta (`NN`) | `21` |
| Protocolo da primeira implementação | `TCP` |
| Protocolo previsto para uma segunda fase | `UDP` |
| VM escolhida para o servidor de match | `152.92.236.14` |
| Porta do RMIRegistry (`98NN`) | `9821` |
| Endereço multicast (`225.1.1.NN`) | `225.1.1.21` |
| Porta multicast (`88NN`) | `8821` |
| Outras VMs do ambiente | `152.92.236.16` e `152.92.236.17` |

As portas de escuta dos jogadores ainda não foram fixadas. Primeiro serão feitos testes em `localhost`; depois, nas três VMs Linux. Para RMI, considerar explicitamente o IP anunciado pelo servidor (`java.rmi.server.hostname`). Multicast entre VMs requer verificação da rede, grupo, interface e TTL.

## Contratos e escolhas já acordados

### 1. Servidor de match (RMI)

A interface remota terá **um método** chamado `getOponent`. A decisão atual é representar a requisição e a resposta como **`byte[]` contendo JSON UTF-8**, e não como `String` diretamente no contrato RMI. A conversão para texto ocorrerá **dentro do componente receptor**, para interpretação do JSON.

**Assinatura acordada, apenas como contrato (não implementada):**

```java
byte[] getOponent(byte[] jsonIn) throws RemoteException;
```

Nome planejado para a interface: `MatchService` (estendendo `java.rmi.Remote`). A requisição e a resposta mantêm os campos definidos pelo PDF:

```json
{"ip":"152.92.236.16","porta":5000,"protocolo":"tcp"}
```

```json
{"ip":"152.92.236.17","porta":5001,"inicia":true}
```

Endereços e portas dos exemplos são **ilustrativos**. O servidor considerará os protocolos `tcp` e `udp` no pareamento, embora **o jogador UDP seja implementado apenas depois da versão TCP**.

### 2. JSON e biblioteca escolhida

**Escolha expressa do responsável:** utilizar **`org.json`** para interpretar e construir JSON. Trata-se de **dependência externa**, autorizada pelo responsável em substituição à restrição inicial de usar somente bibliotecas do JDK. **Ainda não foi definida a versão do artefato nem como disponibilizar seu JAR**; não adicionar Maven/Gradle ou outras dependências por conta própria. Caso a disciplina exija aprovação adicional do professor para bibliotecas externas, confirmar antes da implementação.

Serialização proposta/aprovada no transporte: JSON como texto codificado em **UTF-8**, transmitido em `byte[]`. O contrato de enquadramento das mensagens nos sockets (por exemplo, JSON por linha no TCP) **ainda precisa ser consolidado** antes de implementar.

### 3. Mensagens de jogada e controle

O JSON **de jogada** preserva os nomes e os valores previstos no PDF:

```json
{"Numero":0,"Jogada":4,"Tabuleiro":[0,0,0,0,1,0,0,0,0],"Resultado":"jog"}
```

- `Numero`: contador **global da sessão** de `0` a `8`, avançando somente para jogadas válidas aceitas.
- `Jogada`: índice da casa `0..8`.
- `Tabuleiro`: vetor de **nove inteiros**; `0` = vazia, `1` = X, `2` = O.
- `Resultado`: valores do professor: `jog`, `vit`, `der`, `emp`.
- **Ainda pendente:** determinar definitivamente se o tabuleiro transportado é o estado anterior ou posterior à jogada e a perspectiva de `vit` / `der`.

**Escolha do responsável:** utilizar **mensagens JSON separadas de controle** para rejeição de jogadas inválidas, pedido de correção e término da partida. A jogada rejeitada deve produzir uma explicação ao seu autor e pedido de refazimento, sem avançar o número global. O formato exato da mensagem de controle não foi congelado; exemplos apresentados na conversa são **propostas**, não o contrato final.

O encerramento deve informar **a ambos os jogadores** o status da partida e o vencedor, se houver. Em falha de conexão, pode ser impossível entregar a mensagem remota; registrar o erro e informar o participante local.

**Novas decisões aprovadas em 24/10/2026:**

- Cada partida terá um **`partidaId` único**, usado nas **mensagens de controle** para identificar a sessão. Seu formato e o procedimento de criação/troca entre jogadores ainda serão detalhados; **não acrescentar o campo à resposta RMI** sem nova aprovação.
- O encerramento normal terá os eventos de controle **`fim`** e **`confirmacao_fim`**: após validar a jogada conclusiva, o adversário comunica o resultado por `fim`, e quem fez a jogada confirma por `confirmacao_fim`.
- O campo **`vencedor`** terá valor **`1`** (X), **`2`** (O) ou **`null`** (sem vencedor). Seu uso em mensagens `fim` está aprovado. Os demais nomes de campos e os formatos completos de controle ainda não foram congelados.

**Exemplos apenas ilustrativos** do protocolo acordado (não equivalem ao esquema JSON definitivo):

```json
{"tipo":"controle","evento":"fim","partidaId":"a7f3c92b","resultado":"vitoria","vencedor":1}
```

```json
{"tipo":"controle","evento":"confirmacao_fim","partidaId":"a7f3c92b"}
```

Em empate ou erro, não há vencedor automático (`vencedor: null`). A confirmação pode não ser entregue se a conexão cair; cada jogador deverá tratar essa falha localmente.

> **Diferença a documentar:** o PDF sugere devolver **a mesma mensagem** em caso de jogada inválida; a decisão atual prevê **mensagem de controle separada, explicativa**. A equivalência/compatibilidade com a intenção do professor deve ser verificada quando o protocolo for consolidado.

### 4. Timeout, falhas e displays

- Na espera por um adversário no **servidor de match**, limite de **300 segundos (5 minutos)**; ao esgotar, retirar a solicitação pendente da fila e informar timeout (formato do erro RMI ainda por definir).
- Na espera pela resposta do outro jogador durante a partida, limite de **120 segundos (2 minutos)**; ao esgotar, partida encerrada por **inatividade**.
- Falha durante a partida deve encerrá-la com **erro**, em vez de travar indefinidamente.
- Falha ou ausência de display **não** pode bloquear a partida: publicação multicast é desacoplada.
- O escopo de tolerância a falhas abrange RMI indisponível, registros duplicados, fila concorrente, desconexão do oponente, JSON inválido, jogadas ilegais, problemas no multicast e encerramento de recursos.

### 5. Interoperabilidade

O responsável definiu que a interoperabilidade será verificada **entre os componentes do próprio sistema**; **não** planejar como obrigatório um teste com implementações de outros alunos. Essa é uma decisão de escopo do projeto; o PDF apenas menciona a possibilidade de testes entre implementações.

## Restrições de trabalho e avaliação

- **Nenhuma implementação Java antes de autorização explícita**; respeitar o progresso item a item. O item 2 continua bloqueado.
- Preferir a solução mais simples e direta, sem frameworks e sem abstrações desnecessárias.
- Testes **exclusivamente manuais** executando servidor, jogadores e displays em terminais. Sem JUnit/Mockito ou testes automatizados.
- Para cada falha prevista, registrar **o que pode falhar**, **a reação esperada** e **como reproduzir/testar manualmente**.
- Não declarar um teste como `passou` sem executar o comando e observar o resultado.
- Nas etapas autorizadas, fornecer comandos de compilação/execução `javac`/`java` e casos primeiro em `localhost`, depois nas VMs.
- Projeto final: página Web simples e organizada na conta da VM `152.92.236.14`, com documentação e pitch de cerca de 10 minutos (ou link).

## Situação e retomada

| Marco | Estado |
| --- | --- |
| Item 1 — entendimento, decisões e dúvidas | **Em consolidação; decisões atualizadas** |
| Item 2 — funcionamento explicado ponta a ponta | **Aguardando confirmação explícita** |
| Arquitetura detalhada e classes | **Não iniciado** |
| Java, integração e testes | **Não iniciado / não executado** |
| `PLANO_DO_PROJETO.md` | **Registro de acompanhamento criado antecipadamente a pedido do responsável; isso não inicia o item 6** |

Leia [`PLANO_DO_PROJETO.md`](PLANO_DO_PROJETO.md) para pendências e **BLOCO DE RETOMADA**. Toda nova decisão deve atualizar os três documentos quando afetar suas instruções. Não confundir criação de documentação com execução de testes ou finalização de etapas.
