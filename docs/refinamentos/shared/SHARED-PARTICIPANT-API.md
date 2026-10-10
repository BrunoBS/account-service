# API de compartilhamento

## Participante

Prefixo: `/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}`.

| HTTP   | Rota após o prefixo                                        | Operação                                                                |
| ------ | ---------------------------------------------------------- | ----------------------------------------------------------------------- |
| GET    | `/shared-contracts/available`                              | Descobrir contratos ativos de outras aplicações com proprietário ativo. |
| GET    | `/shared-contracts/available/{contractIdentifier}`         | Consultar um contrato disponível para compartilhamento.                 |
| POST   | `/shared-contracts/{contractIdentifier}/participations`    | Solicitar participação.                                                 |
| GET    | `/shared-participations`                                   | Listar os vínculos da aplicação, inclusive com contratos inativos.      |
| GET    | `/shared-participations/{participationIdentifier}`         | Consultar um vínculo.                                                   |
| POST   | `/shared-participations/{participationIdentifier}/request` | Solicitar novamente após rejeição ou revogação.                         |
| DELETE | `/shared-participations/{participationIdentifier}`         | Excluir o vínculo e seus mapeamentos.                                   |

A solicitação usa apenas o `contractIdentifier` do caminho para resolver o
proprietário, sem exigir query params de conta ou aplicação. As respostas usam identificadores públicos;
as entidades persistem referências pelos IDs internos.

### Paginação das listagens

Todas as listagens de Shared usam `page` (padrão 0) e `size` (padrão 20,
entre 1 e 100). O limite de 100 vale por página; não limita o total de vínculos.
As respostas retornam `content`, `page`, `size`, `totalElements` e `totalPages`:

- `GET /shared-contracts`: contratos do proprietário, por ID crescente.
- `GET /shared-contracts/available`: contratos disponíveis, por ID crescente.
- `GET /shared-contracts/{contractIdentifier}/participations`: participantes do
  contrato, por data de criação e ID crescentes.
- `GET /shared-participations`: vínculos da aplicação, por data de criação e ID
  decrescentes.

As três listagens que anteriormente retornavam uma lista direta agora retornam
esse objeto paginado. Seus consumidores devem ler os itens em `content`.
Os filtros são aplicados no banco antes da paginação, inclusive na contagem.
Filtros inválidos e parâmetros de paginação inválidos retornam 400.
O filtro `participantName` ignora diferenças entre maiúsculas e minúsculas e busca
um trecho literal do nome, sem tratar `%` ou `_` como curingas.
O histórico preserva participantes, proprietários e contratos inativos.

### Descoberta de contratos

`GET /shared-contracts/available` aceita `ownerWorkspaceIdentifier` e
`ownerApplicationIdentifier` como filtros opcionais e independentes. Quando ambos
são enviados, os dois critérios devem corresponder ao proprietário. Os filtros
são aplicados antes da paginação. Identificadores sem correspondência retornam
uma página vazia; filtros em branco retornam 400.

A descoberta sempre retorna apenas contratos ativos com conta/workspace e
aplicação proprietários ativos, excluindo a aplicação solicitante. Não há filtro
de status de participação nesta consulta; `status` permanece em
`GET /shared-participations`.

`GET /shared-contracts/available?page=0&size=20` retorna um objeto paginado:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`page` começa em zero. `size` deve estar entre 1 e 100, com padrão 20.
A ordenação é pelo ID interno do contrato, de forma crescente. Esse ID não
é exposto na resposta. Os filtros de disponibilidade são aplicados antes da
paginação; contratos da própria aplicação e proprietários inativos não compõem
os totais. Uma página além do último resultado tem `content` vazio.

### Vínculos

`GET /shared-participations` aceita os filtros opcionais `status` e
`contractIdentifier`, combinados por AND. Os status aceitos são `PENDING`,
`APPROVED`, `REJECTED` e `REVOKED`.

Além de `contractIdentifier`, cada participação inclui o resumo `contract`:

```json
{
  "identifier": "<participationIdentifier>",
  "contractIdentifier": "<contractIdentifier>",
  "contract": {
    "identifier": "<contractIdentifier>",
    "lifecycle": "INACTIVE",
    "ownerWorkspaceIdentifier": "<ownerWorkspaceIdentifier>",
    "ownerApplicationIdentifier": "<ownerApplicationIdentifier>"
  },
  "status": "APPROVED"
}
```

A inativação do contrato preserva a consulta dos vínculos e seu status;
o lifecycle do contrato informa sua indisponibilidade. A descoberta de novos
contratos continua limitada aos ativos. A exclusão do contrato remove os
vínculos e mapeamentos por cascade.

## Decisão do proprietário

Prefixo: `/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/shared-contracts`.

Aprovação, rejeição e revogação usam o mesmo endpoint:

`PATCH /{contractIdentifier}/participations/{participationIdentifier}/status`.

Para aprovar, o body exige `action`, `publicationModeCode` e
`environmentMappings`, com ao menos um mapeamento:

```json
{
  "action": "APPROVE",
  "publicationModeCode": "AUTOMATIC",
  "environmentMappings": {
    "mappings": [
      {
        "sourceEnvironmentIdentifier": "<sourceEnvironmentIdentifier>",
        "destinationEnvironmentIdentifiers": ["<destinationEnvironmentIdentifier>"]
      }
    ]
  }
}
```

O modo pode ser `AUTOMATIC` ou `MANUAL`. Cada origem deve ter ao menos um
destino; um destino não pode ser repetido. Os ambientes são validados nos
respectivos escopos, incluindo os globais, e devem compartilhar a mesma base.
A participação passa de `PENDING` para `APPROVED` junto com a configuração,
na mesma transação.

Para revogar, o body exige apenas a ação:

```json
{
  "action": "REVOKE"
}
```

A participação passa de `APPROVED` para `REVOKED`. Para rejeitar uma solicitação
pendente, use o mesmo PATCH `/status` com `{"action": "REJECT"}`; o status passa
de `PENDING` para `REJECTED`, sem exigir modo de publicação ou mapeamentos.
Modo e mapeamentos são definidos na aprovação; os antigos PUTs separados não
fazem parte da API.

Parâmetros ou configuração inválidos retornam 400. Uma transição incompatível
com o status atual retorna 409. Referências fora do escopo retornam 404.

## Detalhamento da participação pelo proprietário

`GET /api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/shared-contracts/{contractIdentifier}/participations/{participationIdentifier}`
retorna os dados da participação, `mappings` com os vínculos configurados e
`sourceEnvironments` com os ambientes globais ativos e os ambientes ativos
acessíveis do workspace participante. Sem vínculos, `mappings` é uma lista vazia.
A listagem e o detalhe do proprietário preservam todos os status, inclusive
com contrato inativo. Para contrato ou participante inativo, os ambientes
disponíveis são uma lista vazia; os mapeamentos existentes continuam visíveis.
Os ambientes são carregados apenas no detalhe, não na listagem nem nas respostas
de alteração de status. Os endpoints separados `/source-environments` e
`/environment-mappings` foram removidos.

## Alteração da configuração após aprovação

A aprovação inicial continua em `PATCH .../participations/{participationIdentifier}/status`
com `action: APPROVE`, `publicationModeCode` e `environmentMappings`, numa única transação.

Para alterar uma participação já aprovada, use
`PUT .../participations/{participationIdentifier}/configuration`, com
`publicationModeCode` e `environmentMappings` obrigatórios e sem `action`.
Os mapeamentos substituem integralmente os anteriores; o status permanece `APPROVED`.
O evento de auditoria é `SHARED_PARTICIPATION_CONFIGURATION_UPDATED`, distinto de
`SHARED_PARTICIPATION_APPROVED`. Outros status retornam 409. Configuração inválida
não altera os dados persistidos. Os dois fluxos reutilizam a validação e a aplicação
atômica da configuração.

## Feature compartilhada pelo contrato

Cada contrato possui `feature_id` obrigatório, com FK para `platform_features.id`.
A entidade persiste `Long featureId`; a API recebe e retorna `featureIdentifier`.

Exemplo de criação:

```json
{
  "description": "Configuração compartilhada com outras aplicações",
  "featureIdentifier": "<featureIdentifier>"
}
```

A feature precisa estar ativa, seu microserviço precisa estar ativo e
`shareable` precisa ser `true`. Features são cadastradas com `shareable: false`
por padrão. O atributo também integra as respostas e a edição de features.
A descoberta filtra features indisponíveis antes da paginação; novas solicitações,
reenvios e aprovação/configuração também verificam essa disponibilidade.

A feature do contrato é imutável: para compartilhar outra feature, crie outro
contrato. A referência pública também aparece no resumo `contract` das participações.

`PUT /api/v1/platform/features/{featureIdentifier}` retorna 409 se tentar mudar
`shareable` para `false` enquanto houver qualquer contrato vinculado, inclusive
inativo. A alteração só fica disponível depois de inativar e excluir todos os
contratos da feature. O vínculo é protegido por FK e a edição/criação adquirem
lock na mesma linha da feature para evitar a corrida entre validação e associação.

A migração V40 pressupõe que não existam contratos legados, conforme a decisão
para este serviço ainda não produtivo; não associa registros a uma feature arbitrária.

## Tipo da aplicação proprietária

A criação, edição e reativação de contratos exigem uma aplicação proprietária ativa com `applicationScope = SHARED`. Aplicações `BACKEND` ou `FRONTEND` recebem HTTP 400 (`SHARED-0012`), sem gravação do contrato. Aplicações participantes podem ter outros tipos. Uma aplicação com contratos vinculados, ativos ou inativos, não pode mudar para outro tipo (HTTP 409, `APPLICATION-0108`). Após excluir todos os contratos, a alteração é permitida.

## Identidade e nome do contrato

Cada combinação de workspace proprietário, aplicação proprietária e feature possui um único contrato, inclusive quando inativo. O request contém `featureIdentifier` e `description` opcional; não contém `name`. A feature permanece imutável no contrato. O campo `name` das respostas e do resumo nas participações vem do nome atual da feature, inclusive no histórico. A migração V41 remove a coluna `name` e estabelece a chave única com `feature_id`.

## Integridade nas operações

- Criação, edição e reativação validam o tipo da aplicação proprietária. A aplicação é bloqueada na transação para serializar essas operações com uma alteração do tipo da aplicação.
- A edição preserva a feature original e rejeita uma feature inativa, não compartilhável ou cujo microserviço esteja inativo. Reativação também exige a disponibilidade da feature.
- A aplicação e o contrato são consultados pelo workspace da rota; um contrato de outro proprietário não pode ser editado.
- A chave única do banco e a validação antes da gravação impedem duplicatas por workspace, aplicação e feature. Contratos inativos continuam reservando essa combinação.
- O bloqueio da aplicação e da feature mantém a validação consistente em requisições simultâneas. Campos inválidos e referências inconsistentes abortam a transação, sem atualização parcial.
