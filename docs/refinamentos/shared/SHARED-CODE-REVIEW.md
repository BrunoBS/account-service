# Revisão dos use cases Shared

## Organização

| Serviço                             | Responsabilidade                                                                                |
| ----------------------------------- | ----------------------------------------------------------------------------------------------- |
| `SharedContractCommandService`      | Criar, editar, ativar, inativar e excluir contratos.                                            |
| `SharedContractQueryService`        | Consultar contratos próprios e descobrir contratos disponíveis.                                 |
| `SharedParticipationCommandService` | Solicitar, solicitar novamente, aprovar, rejeitar, revogar, configurar e excluir participações. |
| `SharedParticipationQueryService`   | Consultar participações do proprietário e do participante e detalhar ambientes disponíveis.     |
| `SharedContractAccessService`       | Resolver e verificar o escopo, a propriedade, a atividade e a disponibilidade do contrato.      |
| `SharedEnvironmentMappingService`   | Montar os mapeamentos entre ambientes previamente validados, com resolução em lote.             |

Os antigos `SharedCommandService` e `SharedQueryService` foram substituídos pelos
quatro serviços específicos. Os controllers mantêm suas rotas e as facades
continuam aplicando as políticas de autorização. Aprovação e configuração
reutilizam a mesma aplicação de configuração e mantêm eventos de auditoria
distintos.

Os serviços seguem o padrão `usecase.operations` organizado por responsabilidade:

```text
usecase/operations/
├── contract/
│   ├── SharedContractCommandService
│   ├── SharedContractQueryService
│   └── SharedContractAccessService
├── participation/
│   ├── SharedParticipationCommandService
│   ├── SharedParticipationQueryService
│   └── SharedParticipationStatusService
└── mapping/
    └── SharedEnvironmentMappingService
```

## Problemas corrigidos

- A edição não validava o tipo da aplicação. Criação, edição e reativação agora
  exigem aplicação proprietária ativa do tipo `SHARED`.
- Uma aplicação podia mudar de tipo com contratos existentes. A mudança é
  bloqueada enquanto houver qualquer contrato, inclusive inativo.
- Leituras comuns no MySQL poderiam usar um snapshot anterior à aquisição do
  bloqueio. Os testes reproduziram criação e mudança de tipo aprovadas juntas.
  A referência da aplicação é atualizada sob bloqueio, e as verificações de
  contratos usam leituras atuais com bloqueio.
- A verificação de contratos ao desativar `shareable` também usa uma leitura
  atual. A edição de contratos mantém a ordem de bloqueio: aplicação, feature
  e contrato.
- Solicitações de participação mantêm um bloqueio compartilhado sobre o
  contrato após verificar seu lifecycle atual, protegendo a validação contra
  inativação ou exclusão concorrente. Escritas de contratos usam bloqueio
  exclusivo; as consultas comuns não usam esse bloqueio.
- Aplicações eram carregadas com tags apenas para validar sua atividade.
  As referências de escopo agora retornam somente IDs e tipo necessários ao
  comando ou à consulta, preservando o filtro de visibilidade do chamador.
- O ID da aplicação participante era resolvido várias vezes. Cada operação
  reutiliza a referência local já validada.
- Respostas de comandos de contrato reutilizam os UUIDs da rota já verificados,
  sem buscar novamente o workspace e a aplicação do proprietário.
- Validações redundantes de transição foram removidas. O reenvio usa a política
  central de transições, que permite `REJECTED/REVOKED -> PENDING`.
- A montagem de mapeamentos consultava cada ambiente e depois resolvia seu ID.
  Agora são consultados somente os identificadores informados: um lote de
  origem e um lote de destino, já com os IDs internos. As verificações de
  atividade dos ancestrais permanecem aplicadas.
- O detalhe do proprietário consultava a participação novamente para recuperar
  o ID interno da aplicação. Agora usa a participação já carregada.
- Helpers e dependências sem consumidores foram removidos; variáveis e
  dependências identificam sua responsabilidade explicitamente.

## Regras preservadas

- Um contrato por workspace, aplicação e feature, independentemente do
  lifecycle; validação transacional e chave única no banco.
- Feature obrigatória, ativa, compartilhável e com microserviço ativo ao criar
  ou editar; a feature vinculada não pode mudar.
- Nome das respostas obtido da feature, sem nome próprio no contrato.
- Edição restrita ao proprietário e a contratos ativos; descrição de até 500
  caracteres e atualização sem alterações parciais.
- Exclusão de contrato somente após inativação, com cascades existentes.
- Aplicações participantes podem ter outros tipos; não podem participar de seu
  próprio contrato.
- Configuração exige participação aprovada; aprovação exige modo de publicação
  e mapeamentos válidos. Os destinos não podem se repetir.
- Origens pertencem ao workspace participante e destinos ao proprietário,
  admitindo os ambientes globais permitidos. Ambientes e ancestrais precisam
  estar ativos, e a base de origem e destino deve ser compatível.
- Histórico continua consultável para contratos ou participantes inativos.
- Falhas de validação e conflitos abortam a transação e sua auditoria.

## Validação

O conjunto executado inclui os testes unitários e de arquitetura, `SharedApiIT`,
`PlatformApiIT` e `PlatformQueryIT`. Os cenários de criação duplicada, mudança
concorrente do tipo da aplicação e desativação concorrente de `shareable` são
repetidos cinco vezes cada. Os testes de Shared cobrem isolamento de escopo,
visibilidade, transições, auditoria, rollback e mapeamentos.

A melhoria de desempenho foi validada pela estrutura das consultas e pelos
fluxos de integração; não foi executado um benchmark de carga. A contagem de consultas das listagens é verificada no cenário descrito abaixo. A resolução dos
ambientes usa dois lotes dos identificadores solicitados. A verificação dos
ancestrais pode exigir consultas adicionais conforme a hierarquia persistida.

## Revisão das listagens e concorrência

- Listagens próprias, de participantes e de vínculos são paginadas, com o mesmo
  contrato de resposta já usado na descoberta: `content`, `page`, `size`,
  `totalElements` e `totalPages`. Há um único `SharedPageInput` e validação de
  paginação compartilhada.
- Os filtros de participantes são validados antes de consultar o banco e
  aplicados junto à paginação. A busca por nome é literal, sem expansão de
  curingas. O filtro não exige que a aplicação participante continue ativa.
- As páginas de participação primeiro selecionam IDs e depois carregam o grafo
  de contratos e mapeamentos desses IDs. Assim, não há paginação sobre fetch de
  coleção, que poderia carregar todos os participantes em memória.
- A descoberta aplica os filtros e a disponibilidade no SQL de leitura, com
  joins de workspace, aplicação, feature e microserviço. Não busca todas as
  referências de contratos ativos antes de selecionar a página. O escopo do
  chamador continua validado pelo use case de Application; os joins de leitura
  não substituem os use cases de validação dos comandos nem autorizam escritas
  nos módulos referenciados.
- As referências históricas de aplicações, workspaces, features e ambientes são
  resolvidas em lote para os itens da página, preservando recursos inativos.
- Variáveis genéricas do resolver foram substituídas por nomes dos serviços;
  filtros, páginas e referências locais identificam sua finalidade.
- Os comandos de participação usam isolamento `READ_COMMITTED` e bloqueio
  exclusivo da participação nas alterações e exclusões. Assim, a leitura dos
  mapeamentos após esperar pelo bloqueio não reutiliza o snapshot antigo do
  `REPEATABLE_READ` do MySQL; o bloqueio do agregado permanece até o commit. Reenvios verificam o contrato antes de
  bloquear a participação, preservando a ordem contrato → participação, e
  atualizam a referência sob bloqueio antes de validar a transição.
- Há cenários concorrentes de aprovação × exclusão, configuração × revogação e
  configuração × configuração, repetidos cinco vezes cada, verificando o estado
  final, os mapeamentos e os eventos de auditoria das operações efetivadas.

O teste `sharedListQueryCountsDoNotGrowWithTheNumberOfPageItems` utiliza as
estatísticas de SQL do Hibernate para comparar páginas com 1 e 4 itens nas quatro
listagens. Esse controle detecta crescimento de consultas por item; ele não
substitui um benchmark de latência ou carga em volumes representativos.

Na validação final, 78 testes unitários/de arquitetura e 54 testes de integração
(`SharedApiIT`, `PlatformApiIT` e `PlatformQueryIT`) passaram, totalizando 132.

| Listagem                           | Consultas SQL com 1 item | Consultas SQL com 4 itens |
| ---------------------------------- | -----------------------: | ------------------------: |
| Contratos próprios                 |                        7 |                         7 |
| Vínculos da aplicação participante |                        9 |                         9 |
| Participantes de um contrato       |                       10 |                        10 |
| Contratos disponíveis              |                        7 |                         7 |

Essas contagens incluem as leituras de escopo e a contagem da página no ambiente
de integração; não são um limite universal para outros filtros ou configurações.
