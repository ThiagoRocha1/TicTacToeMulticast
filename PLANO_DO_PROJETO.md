# PLANO_DO_PROJETO.md — TicTacToeMulticast

> **Registro de continuidade inicial**, preparado em **24/10/2026** porque o responsável pediu para registrar as decisões no repositório. A criação antecipada deste arquivo **não constitui avanço ao item 2 nem conclusão do item 6** da sequência pedagógica.
>
> **Estado:** item 1 (entendimento do enunciado / consolidação de decisões). **Próxima etapa bloqueada até confirmação explícita.** Não há código Java implementado ou teste manual executado por este registro.

## 1. Fonte de verdade e organização

- **Enunciado oficial:** `trab-2-rmi-multi-2026-1.pdf`, Prof. Alexandre Sztajnberg, quatro páginas, incluindo Figura 1 (Arquitetura). O PDF é a fonte do que **o professor pede**.
- **Decisões do responsável:** complementam aspectos que o professor não especificou; identificadas como tal neste plano.
- **Regras para agentes:** `AGENTS.md`.
- **Visão geral:** `README.md`.
- **Vigência:** cabeçalho interno do PDF `2023/1`; o responsável **confirmou** que o documento vale para **2026/1**.
- **Interoperabilidade:** definida pelo responsável como **interna aos componentes do próprio sistema**, não como obrigação de testes com colegas.

Não supor que o PDF está comitado no repositório sem verificar.

## 2. Resumo dos requisitos do PDF

- Jogo da velha distribuído de dois jogadores; um jogador participa de **uma partida por execução**.
- **Servidor de match (RMI):** método remoto único `getOponent`; recebe `ip`, `porta`, `protocolo`; encontra dois interessados de **mesmo protocolo**; retorna IP/porta do oponente e `inicia` diferente para os dois; conclui as **duas** chamadas pendentes e remove o par da fila.
- **Jogadores:** solicitam pareamento por RMI e realizam jogadas diretamente entre si via socket TCP/UDP; `inicia=true` joga primeiro; publicam **cada jogada** por multicast.
- **Displays:** recebem jogadas via multicast e apresentam o tabuleiro; múltiplas instâncias possíveis.
- Mensagem de jogada: `Numero`, `Jogada`, `Tabuleiro`, `Resultado` (`jog`/`vit`/`der`/`emp`); casas `0..8`; contador inicial zero.
- Controle de jogadas inválidas e aceite final são descritos no PDF, mas detalhes de protocolo exigem consolidação.
- Infraestrutura com tolerância a falhas; o PDF **não determina uma política única** de timeout/cancelamento.
- Linux nas VMs `152.92.236.14`, `.16`, `.17`; página Web simples na conta da `.14`, documentação e pitch de cerca de 10 minutos.

## 3. Parâmetros de ambiente

| Parâmetro | Valor | Origem/status |
| --- | --- | --- |
| NN | `21` | Informado pelo responsável |
| Protocolo da primeira entrega | `TCP` | Decisão do responsável |
| Protocolo para etapa posterior | `UDP` | Decisão do responsável (não iniciar agora) |
| VM servidor match | `152.92.236.14` | Escolha do responsável |
| Porta RMIRegistry | `9821` | Fórmula do PDF `98NN` |
| Grupo multicast | `225.1.1.21` | Fórmula do PDF `225.1.1.NN` |
| Porta multicast | `8821` | Fórmula do PDF `88NN` |
| Versão Java nas VMs | `17.0.20.1` | Informado pelo responsável |
| Portas de jogadores | **A definir** | Dependem da execução |

## 4. Registro de decisões

| ID | Decisão | Situação | Origem / observação |
| --- | --- | --- | --- |
| D01 | Usar **TCP primeiro** e ampliar para UDP depois | **Aprovado** | Responsável |
| D02 | Match considera pedidos TCP e UDP, pareando protocolos iguais | **Aprovado** | Responsável; pareamento por protocolo já consta no PDF |
| D03 | Servidor RMI na VM `.14`; `NN=21` | **Aprovado** | Responsável |
| D04 | Interface planejada `MatchService` com método `getOponent` recebendo e retornando **`byte[]`** | **Aprovado** | Responsável decidiu contra `String` como tipo de transporte |
| D05 | JSON em **UTF-8**, convertido `byte[]` → `String` internamente para interpretação | **Aprovado** | Responsável concordou com a proposta apresentada |
| D06 | Utilizar a biblioteca **`org.json`** | **Aprovado pelo responsável** | Exceção explícita à restrição inicial de apenas JDK; versão/JAR ainda não definidos |
| D07 | `Tabuleiro`: `0` vazio, `1` X, `2` O | **Aprovado** | Responsável |
| D08 | `Numero` é **global por partida**, começa em `0` e vai até `8` | **Aprovado** | Responsável |
| D09 | Separar JSONs **de jogada** dos JSONs **de controle** | **Aprovado** | Responsável |
| D10 | Jogada inválida: retornar mensagem ao autor com motivo e solicitação de refazer, sem avanço de `Numero` | **Aprovado** | Responsável; diferencia-se do eco literal sugerido no PDF |
| D11 | Ao encerrar, informar a **ambos** o status e o vencedor, quando houver | **Aprovado** | Responsável; não garantir entrega se rede falhou |
| D12 | Espera pela resposta do oponente limitada a **120 segundos** | **Aprovado** | Responsável |
| D13 | Inatividade encerra a partida; falha encerra por **erro** | **Aprovado** | Responsável |
| D14 | Testar interoperabilidade **somente interna** | **Aprovado** | Responsável |
| D15 | Enunciado continua válido para **2026/1** | **Confirmado** | Responsável |
| D16 | O item 2 só começa mediante confirmação expressa | **Vigente** | Responsável |

**Interface conceitual, ainda não implementada:**

```java
// Interface MatchService estenderá java.rmi.Remote.
byte[] getOponent(byte[] jsonIn) throws RemoteException;
```

**JSON do match (do PDF):**

```json
{"ip":"152.92.236.16","porta":5000,"protocolo":"tcp"}
```

```json
{"ip":"152.92.236.17","porta":5001,"inicia":true}
```

**JSON de jogada (campos do PDF, valores conforme decisão D07):**

```json
{"Numero":0,"Jogada":4,"Tabuleiro":[0,0,0,0,1,0,0,0,0],"Resultado":"jog"}
```

Os JSONs acima são **exemplos ilustrativos**, não resultados de testes. Formatos de **controle** discutidos (por exemplo, `tipo`, `motivo`, `refazer`, `vencedor`) são **propostas ainda não congeladas**.

## 5. Arquitetura atual — somente conceitual

```text
Jogador A -- RMI --> Servidor de Match <-- RMI -- Jogador B
     |                                              |
     +----------- socket TCP direto ----------------+
     |                                              |
     +------ publicação multicast 225.1.1.21:8821 -+
                           |
                      Display(s)
```

Três programas previstos: **Servidor de Match**, **Jogador** e **Display**. Ainda não foram aprovados pacotes, classes internas, estruturas de fila, política de threads ou fluxo completo de controle. Essas definições serão feitas somente na etapa autorizada.

## 6. Mapa preliminar de falhas (planejamento, NÃO testado)

| Módulo / falha | Reação desejada | Como testar futuramente (manual) | Status |
| --- | --- | --- | --- |
| Match: servidor/registry fora do ar | Reportar falha claramente, evitar espera infinita; limite de retries pendente | Não iniciar ou encerrar o servidor e tentar conectar jogadores | Não executado |
| Match: fila sem oponente / desistência / duplicidade | Timeout/cancelamento/limpeza definidos antes de implementar | Iniciar jogador isolado; interromper cliente; registrar o mesmo endpoint | Não executado |
| Match: acessos concorrentes | Parear sem duplicar clientes e retornar para os dois | Iniciar vários jogadores simultaneamente | Não executado |
| Jogador: oponente sem responder | Encerrar por **inatividade após 120 s** de espera de resposta | Deixar oponente sem responder por 120 s | Não executado |
| Jogador: conexão interrompida | Encerrar por erro e liberar recursos; log local | Fechar um jogador durante a partida | Não executado |
| Jogador: JSON inválido / campos faltantes | Rejeitar ou informar erro de protocolo, sem travar | Enviar mensagem malformada no fluxo manual aprovado | Não executado |
| Jogador: casa ocupada / fora de turno / contador incorreto / tabuleiro inconsistente | Rejeitar, justificar ao autor e solicitar correção sem avançar contador | Reproduzir mensagens inválidas em teste manual autorizado | Não executado |
| Display: ausente ou falha multicast | Partida continua; registrar erro de publicação quando detectável | Jogar sem displays; interromper display no meio | Não executado |
| Rede multicast entre VMs | Diagnosticar grupo, interface, TTL, regras de rede; plano B localhost | Rodar display na outra VM; repetir em localhost | Não executado |

**Observação:** o timeout de **match** não é o mesmo que o de **jogada**: o primeiro ainda está **pendente de definição**.

## 7. Plano de etapas (proposta, SEM avanço automático)

| Etapa | Objetivo / aprendizagem | Entregável planejado | Teste manual básico previsto | Situação |
| --- | --- | --- | --- | --- |
| **0** | Requisitos, decisões e ambiguidades | Documentação e contratos acordados | Revisão com responsável | **Em andamento (item 1)** |
| **1** | Regras do jogo e JSON | Lógica do tabuleiro + mensagens | Rodar manualmente e inspecionar saída no terminal | Não iniciada |
| **2** | Publicação/recepção multicast | Display simples | Executar display e remetente em localhost | Não iniciada |
| **3** | Comunicação direta | Dois jogadores via TCP local | Jogar partida manual em terminais | Não iniciada |
| **4** | RMI e concorrência | Servidor de match | Iniciar servidor e dois clientes em localhost | Não iniciada |
| **5** | Integração | Match + jogo em localhost | Partida completa, dois jogadores e display | Não iniciada |
| **6** | Robustez | Timeouts, validações, tratamentos de falha | Cenários do mapa de falhas | Não iniciada |
| **7** | Execução remota | Implantação nas três VMs | Match, partida e displays entre VMs | Não iniciada |
| **8** | Interoperabilidade interna | Compatibilidade entre módulos do projeto | Testes cruzados de mensagens e módulos próprios | Não iniciada |
| **9** | Segundo protocolo (posterior) | Suporte UDP, se autorizado na ocasião | Perda/duplicação/reordenação simuladas manualmente | Não iniciada |
| **10** | Entrega | Página Web, documentação e pitch | Abrir a página na VM `.14` e revisar pitch | Não iniciada |

A divisão acima é **proposta de desenvolvimento**, não a sequência de itens didáticos já percorrida. Cada etapa futura deverá registrar objetivo, o que aprender, critérios de pronto, mapa de falhas, roteiro de teste manual e resultados de fato.

### Quadro de resultados de testes

| Grupo | Passou | Falhou | Não executado | Evidência |
| --- | --- | --- | --- | --- |
| Tabuleiro / JSON | 0 | 0 | **Todos** | Nenhum teste realizado |
| RMI / pareamento | 0 | 0 | **Todos** | Nenhum teste realizado |
| Socket TCP / partida | 0 | 0 | **Todos** | Nenhum teste realizado |
| Multicast / display | 0 | 0 | **Todos** | Nenhum teste realizado |
| Falhas / timeouts | 0 | 0 | **Todos** | Nenhum teste realizado |
| Três VMs | 0 | 0 | **Todos** | Nenhum teste realizado |
| UDP | 0 | 0 | **Todos** | Não iniciado |

## 8. Pendências e perguntas em aberto

1. **Mensagens de controle:** estabilizar contrato JSON para erro, pedido de refazer, vitória/derrota/empate, confirmação e desconexão; alinhar a alternativa escolhida com o PDF, que sugere devolver a própria mensagem em caso de erro.
2. **Perspectiva de `Resultado`:** `vit`/`der` é do emissor ou do receptor? Como representar a confirmação de cada lado? Quem é X/O (a regra `inicia=true` = X foi sugerida, ainda não confirmada)?
3. **Tabuleiro no pacote:** estado anterior ou posterior à `Jogada`? Validar consistência e vez de cada jogador.
4. **Timeout do servidor de match:** duração, cancelamento de pedido, jogador duplicado, fila e duas chamadas pendentes.
5. **Timeout após rejeição:** relógio do turno de 120 s reinicia ou permanece correndo? Proposta anterior era não reiniciar; não foi confirmada.
6. **Formato no socket TCP:** delimitação de mensagens, framing, organização das respostas, encerramento. `JSON por linha` foi proposto, mas não confirmado.
7. **`org.json`:** versão específica, como obter/distribuir JAR e configurar classpath sem dependências adicionais; confirmar aceitação do professor se requerida.
8. **Ambiente:** portas dos jogadores, anúncio IP de RMI, multicast entre VMs, interface e TTL.
9. **Resiliência:** política de tentativas RMI, falha em match e tratamento UDP quando sua etapa chegar.

## 9. Última mudança registrada

**24/10/2026 — consolidação documental do item 1:** escolhido `org.json` como biblioteca JSON, além das decisões anteriores (RMI `byte[]` UTF-8, TCP agora/UDP depois, tabuleiro 0/1/2, `Numero` global, controle separado, rejeição explicativa, encerramento comunicado aos dois, 120 s de timeout e interoperabilidade interna). Os documentos foram **preparados fora do repositório local Windows**; a cópia/commit para o Git deve ser confirmada no ambiente que contém o repositório. **Nenhum código compilado/testado.**

## 10. Próximo passo exato

1. Colocar/conciliar `README.md`, `AGENTS.md` e `PLANO_DO_PROJETO.md` na raiz real do repositório e inspecionar o diff; **sem presumir commit/push**.
2. Permanecer no **item 1** para eventuais dúvidas ou revisão dos contratos.
3. **Somente se o responsável autorizar explicitamente**, iniciar **item 2 — explicação didática do funcionamento completo**, sem começar implementação antes da aprovação específica.

## BLOCO DE RETOMADA (copiar para outro chat)

> Projeto TicTacToeMulticast (Sistemas Distribuídos, Java 17), baseado em PDF `trab-2-rmi-multi-2026-1.pdf` de quatro páginas, válido para 2026/1. **Estamos no item 1 — consolidação de decisões; NÃO avançar para o item 2 sem minha autorização. Nenhum código ou teste foi executado.** NN=21; servidor match RMI na 152.92.236.14, registry 9821; multicast 225.1.1.21:8821; TCP primeiro e UDP depois; servidor pareia protocolos iguais. Interface RMI `MatchService`: `byte[] getOponent(byte[] jsonIn) throws RemoteException`, JSON em UTF-8 convertido internamente em String, interpretar via **org.json** (única biblioteca externa autorizada). Tabuleiro `0` vazio, `1` X, `2` O; contador `Numero` global 0..8; mensagens de jogada conforme PDF e **mensagens de controle separadas** para erro/fim; jogada inválida recebe motivo e pedido de refazer; final informar status e vencedor a ambos quando possível. Timeout de **120 s** aguardando resposta do adversário; inatividade/falha encerram partida; interoperabilidade **interna**; testes **manuais**. Pendentes: formato final de controle, semântica de Resultado, estado do Tabuleiro, framing TCP, timeout de match, timeout após rejeição, versão/JAR de org.json e aspectos das VMs. Ler README.md, AGENTS.md, PLANO_DO_PROJETO.md e PDF (inclusive Figura 1), resumir estado em três linhas e pedir confirmação antes de avançar.
