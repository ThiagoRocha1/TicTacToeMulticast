# AGENTS.md — Orientações obrigatórias para agentes

Estas instruções se aplicam à raiz de `TicTacToeMulticast` e às subpastas, a qualquer agente que desenvolva, revise, documente ou teste o projeto. **Antes de agir, verifique o estado real do repositório e a autorização do responsável.**

## 1. Estado atual: não avançar o projeto sem confirmação

- O projeto está no **item 1 (entendimento e consolidação de decisões)**.
- **Item 2 não autorizado**. O responsável pediu expressamente para não avançar sem confirmação.
- Está autorizado **registrar as decisões nos documentos**, como nesta atualização. Isso **não autoriza** escrever código Java, elaborar a implementação, executar/alterar etapas seguintes ou criar testes automatizados.
- `PLANO_DO_PROJETO.md` foi criado **para registrar o progresso a pedido do responsável**. Sua criação antecipada não significa que o item 6 do planejamento pedagógico tenha sido realizado.
- Na retomada, confirmar o estado com o responsável antes de prosseguir. Não inferir autorização por ter acesso a arquivos.

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
| Interface RMI | `MatchService`, método `getOponent` com entrada e saída **`byte[]`** |
| Payload | JSON codificado em **UTF-8**; converter bytes para `String` **internamente** para interpretar o JSON |
| JSON | **`org.json`** (biblioteca externa **expressamente autorizada pelo responsável**) |
| Tabuleiro | Nove inteiros: `0` vazia, `1` X, `2` O |
| `Numero` | Contador **global da partida**, `0..8`, apenas jogadas aceitas avançam |
| Mensagens | JSON de jogada conforme PDF **mais JSON separado de controle** para rejeições/erros/encerramento |
| Jogada inválida | Rejeitar, informar a causa e exigir refazer sem avançar o contador |
| Fim de partida | Informar status e vencedor (se existir) **aos dois jogadores**, dentro das limitações da rede |
| Timeout de espera de resposta | **120 segundos**; expiração encerra por inatividade |
| Falha durante a partida | Encerrar com **erro** e liberar recursos |
| Interoperabilidade | Entre componentes **do próprio sistema**, sem exigir teste com outros alunos |
| Testes | **Manuais**, usando programas nos terminais; primeiro `localhost`, depois as VMs |

**Cuidado com `org.json`:** a diretriz inicial era usar somente biblioteca padrão. O responsável **mudou expressamente essa decisão** e selecionou `org.json`. Não proibir `org.json` com base em textos antigos; também não introduzir **outras** bibliotecas, Maven, Gradle ou ferramentas sem aprovação. Artefato/versão do JAR ainda não foram escolhidos. Se o professor exigir aprovação de dependências externas, confirmar antes de implementar.

**Assinatura RMI conceitual (não implementada):**

```java
byte[] getOponent(byte[] jsonIn) throws RemoteException;
```

O servidor só deve efetuar o match de jogadores com o mesmo protocolo; deve retornar aos **dois** participantes, com um `inicia=true` e outro `inicia=false`. Evitar supor que chamadas RMI pendentes podem ser simplesmente descartadas sem tratar os clientes.

## 4. Requisitos do PDF que precisam ser preservados

Há **três programas**:

1. **Servidor de match**: RMI, método `getOponent`; json-in com `ip`, `porta`, `protocolo`; json-ret com `ip`, `porta`, `inicia`.
2. **Jogador**: busca adversário por RMI, joga uma partida diretamente por socket; envia cada jogada aos displays por multicast; quem recebe `inicia=true` começa.
3. **Display**: escuta multicast, mostra tabuleiro, permite múltiplas instâncias; ausência de display não pode impedir o jogo.

A mensagem de jogada usa **exatamente** `Numero`, `Jogada`, `Tabuleiro`, `Resultado`, respeitando capitalização. Índices de casas `0..8`, resultados `"jog"`, `"vit"`, `"der"`, `"emp"`. Não alterar esses campos sem aprovação.

**Atenção à diferença de protocolo:** o PDF sugere devolver a **mesma** mensagem em caso de jogada inválida; o responsável optou por uma **mensagem de controle separada, com motivo e pedido de refazer**. Documentar e, antes de congelar o protocolo de wire, revisar essa diferença para garantir a aderência ao trabalho.

## 5. Pendências que NÃO devem ser inventadas

- Formato final dos **JSONs de controle** (campos, tipos, identificador de jogador, evento de encerramento).
- Perspectiva de `Resultado` (`vit`/`der`), confirmação final, empate, significado de quem inicia como X/O (foi **proposto**, não confirmado como requisito definitivo).
- Se `Tabuleiro` descreve o estado **antes ou depois** da jogada e como detectar inconsistências.
- Enquadramento das mensagens TCP e extensão equivalente para UDP (JSON por linha foi **recomendado**, ainda não aprovado como regra final).
- Timeout da espera por **match**, cancelamento/desistência, retries de RMI e registros duplicados.
- Se uma rejeição de jogada reinicia o prazo de 120 segundos (a proposta anterior era **não reiniciar**, sem aceite explícito).
- Versão e obtenção de `org.json`; montagem do classpath sem introduzir ferramenta não autorizada.
- Número da porta de cada jogador, configuração de `java.rmi.server.hostname`, TTL/interface multicast e limitações das VMs.

Não implementar essas decisões pendentes silenciosamente.

## 6. Regras de engenharia e falhas

- Java 17, com `java.net`, `java.rmi`, `java.util.concurrent` etc., **mais somente a dependência `org.json` já autorizada**. Não introduzir frameworks ou outras bibliotecas sem autorização.
- Código simples e didático; sem arquitetura sofisticada, abstrações de conveniência ou padrões avançados sem necessidade.
- **Sem JUnit, Mockito ou testes automatizados.** Testes **manuais** mediante execução dos módulos em terminais. Consultar antes de sugerir ferramentas como `nc` ou `tcpdump`.
- Cada módulo deve ser executável pelo terminal com parâmetros configuráveis. Primeiro demonstrar `localhost`, depois VMs `152.92.236.14`, `.16`, `.17`.
- Tratar em cada módulo: **o que pode falhar / reação esperada / roteiro manual para provocar a falha**.
- Match: fila sincronizada, chamadas pendentes, cancelamento, duplicidade e timeout; RMI fora do ar e limites de tentativas.
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

**Política Git:** não executar `reset --hard`, `clean -fd`, `commit`, `push`, `merge`, `rebase` nem reescrever histórico sem pedido específico. Nunca descartar alterações do usuário ou de outros agentes.

## 8. Estado atual e próxima ação

- **Item 1:** em consolidação; decisões principais foram tomadas e documentadas.
- **Item 2:** **bloqueado até confirmação explícita**.
- **Nenhum Java implementado / teste executado** pelo processo descrito aqui.
- `PLANO_DO_PROJETO.md`: criado para continuidade documental, não por avanço pedagógico ao item 6.

**Próxima ação permitida:** revisar e registrar dúvidas/decisões técnicas do **item 1** ou aguardar autorização expressa para iniciar **item 2**. Verificar primeiro o estado real do Git antes de qualquer mudança.
