# Revisão de Environment

> Registro histórico: para o comportamento atual de ambientes, incluindo a separação dos comandos e as regras de inativação, consulte [a revisão completa](ENVIRONMENT-FULL-CODE-REVIEW.md). As decisões desta revisão posterior substituem os pontos divergentes abaixo.

Contrato vigente: tipos usam code imutável (DEFAULT/CUSTOM/SHARD/CELL); requests usam environmentTypeCode e parentTypeCode/childTypeCode. Version não nula é rejeitada no create e obrigatória no update. Inativar pai com filhos é permitido; a exclusão continua bloqueada por filhos, incluindo QUARANTINED. As quatro listagens gerais usam LifecycleFilter com ACTIVE/INACTIVE/QUARANTINED. Veja também a [simulação dos 29 endpoints](ENVIRONMENT-DATA-FLOW-SIMULATION.md).

Referência: [REVIEW-STANDARD.md](../../REVIEW-STANDARD.md). Escopo: ambientes globais e por workspace, tipos, compatibilidades, requests, persistência, visibilidade e integrações com Shared.

## Fluxos de revisão

- [Tipos de ambientes](ENVIRONMENT-TYPE-CODE-REVIEW.md): revisão específica do cadastro global e suas invariantes.
- Ambientes: operações globais e por workspace, hierarquia e settings.
- Compatibilidades: relações permitidas entre tipos e proteção contra ciclos.

## Schemas de request

Cada entidade com body de entrada usa um único schema completo para create/update quando aplicável. A biblioteca valida o JSON original antes do binding tipado. `settings` faz parte do schema do body e é validado na mesma passagem; não há uma segunda validação dinâmica de settings no fluxo de Environment.

| Recurso | Schema | Resource type / code | Requests e endpoints |
| --- | --- | --- | --- |
| `Environment` | [`environment.json`](../../../src/main/resources/schemas/environment.json) | `ENVIRONMENT / environment` | `CreateEnvironmentRequest` e `UpdateEnvironmentRequest`: defaults globais e ambientes do workspace |
| `EnvironmentType` | [`environment-type.json`](../../../src/main/resources/schemas/environment-type.json) | `ENVIRONMENT / environment-type` | `CreateEnvironmentTypeRequest` e `UpdateEnvironmentTypeRequest` |
| `EnvironmentTypeCompatibility` | [`environment-type-compatibility.json`](../../../src/main/resources/schemas/environment-type-compatibility.json) | `ENVIRONMENT / environment-type-compatibility` | `CreateEnvironmentTypeCompatibilityRequest` |

Os três arquivos são schemas completos, cada um em um único arquivo e sem `$defs`. As migrations V52, V54 e V55 registram definições equivalentes e associam cada schema ao request correspondente. O request de compatibilidade recebe `parentTypeIdentifier` e `childTypeIdentifier` como UUIDs; a resposta também identifica os tipos por identifier. Os validators e use cases continuam responsáveis por regras de negócio: unicidade, existência, escopo, estado, versão, topologia e ciclos.

`settings` é `JsonNode` da entrada à entidade e resposta, armazenado em coluna MySQL `JSON`. O schema do body exige um objeto JSON, mas deixa suas propriedades abertas porque não há uma estrutura estável específica de Environment. Isso mantém o JSON tipado sem inventar campos específicos.

## Revisão por classe e método

| Classe / método | Resultado da revisão |
| --- | --- |
| `CreateEnvironmentRequest`, `UpdateEnvironmentRequest`, `CreateEnvironmentTypeRequest`, `UpdateEnvironmentTypeRequest`, `CreateEnvironmentTypeCompatibilityRequest` | Tipos concretos nos controllers, validação do JSON original pela biblioteca e um schema por entidade. O update de Environment inclui `parentIdentifier` para que a regra de imutabilidade rejeite a alteração explicitamente. |
| `EnvironmentNormalizer` | Normaliza nomes e códigos no payload antes da validação e novamente os inputs tipados na fronteira do use case. Faz cópia defensiva de `settings`; valores desconhecidos ou tipos errados não são descartados antes da validação do schema. |
| `Environment` e `EnvironmentResponse` | `settings` permanece como `JsonNode`, é copiado defensivamente e persistido como JSON nativo. Migration V53 alinha também o nome persistido ao limite de 50 caracteres do contrato. |
| `EnvironmentValidator` / `EnvironmentCommandService` | Schema cobre estrutura e tipos; regras de domínio cobrem versão, nomes duplicados, escopo do pai/tipo, imutabilidade do pai no update, filhos e estado. Criação e atualização não fazem uma segunda validação de settings. |
| `EnvironmentCommandService.delete` / `EnvironmentFinder.findInactiveForDeletion` | Ambiente com filhos não pode ser colocado em quarentena nem removido. A linha do pai é bloqueada para serializar a operação concorrente de inclusão de filho e evitar uma árvore inacessível. Erro `ENVIRONMENT-131` e cenário de integração coberto. |
| `EnvironmentType` / `EnvironmentTypeDetails` / `EnvironmentTypeMapper` | `Details` agrupa os campos compartilhados por create/update. O mapper traduz `EnvironmentTypeInput`; o construtor e `update(details, now)` aplicam o mesmo objeto por um método interno. O código permanece imutável e separado dos detalhes editáveis. Lifecycle continua protegido no domínio. |
| `EnvironmentType.allowsChildren` | Campo obrigatório no schema e na resposta. `EnvironmentTopologyValidator` bloqueia filhos de tipos terminais e trava o tipo pai durante a validação. A atualização rejeita desligar a permissão enquanto houver filhos. O lock comum serializa a alteração com a criação concorrente de um filho. |
| `EnvironmentTypeCommandService` | Operações administrativas têm auditoria. Criação serializa a checagem de código duplicado; update/inactivate/restore/delete carregam o tipo com lock e aplicam as pré-condições de lifecycle. Inativação preserva ambientes/compatibilidades existentes e bloqueia novos vínculos; manutenção/restauração do ambiente atual permite o tipo inativo. Exclusão continua bloqueada enquanto houver referências. |
| `EnvironmentTypeCompatibilityCommandService` / `EnvironmentTypeCompatibilityValidator` | Alterações bloqueiam os tipos em ordem transacional comum, carregam uma vez o grafo e checam ciclos em memória. Compatibilidade ativa existente é idempotente; estado inválido é rejeitado. |
| `EnvironmentTypeCompatibilityQueryService` | O filtro de lifecycle é validado e retorna erro de request consistente quando não é `ACTIVE` ou `INACTIVE`. |
| `EnvironmentQueryService.tree` | A árvore agora carrega ambientes ativos visíveis em uma consulta e agrupa os filhos em memória, evitando uma consulta por nó. Acessibilidade ainda exige que todos os ancestrais estejam ativos. |
| Facades e visibilidade | Policies distinguem leitura, escrita custom ADM e administração global OWNER. Consultas de workspace mantêm `@ResourceVisibility(Environment.class)`; operações Shared só expõem referências depois da autorização de contrato/participação. |

## Pontos de regra e integridade

- Nomes são únicos entre irmãos por escopo e pai, inclusive para registros inativos; consulta prévia melhora a mensagem e a constraint do banco fecha a corrida concorrente.
- Um ambiente global não pode ter pai custom. Um ambiente custom deve manter o workspace do pai e respeitar os requisitos de escopo e raiz do tipo.
- Compatibilidades precisam referenciar tipos ativos, não podem ligar um tipo a si mesmo nem formar ciclos.
- O código de `EnvironmentType` é editável e não limita os tipos aceitos pela API de ambiente. O request seleciona o tipo por `environmentTypeIdentifier`; `allowsChildren` complementa a compatibilidade: a compatibilidade valida o par de tipos, enquanto o atributo controla se instâncias do tipo pai podem receber filhos.
- A edição usa controle otimista de versão. `environmentTypeIdentifier` não pode mudar para outro tipo enquanto o ambiente tiver filhos; `parentIdentifier` não pode mudar depois da criação.
- A inativação de um pai com filhos é bloqueada. A navegação também filtra ancestrais inativos, evitando expor descendentes inacessíveis.

## Auditoria de exclusão de tipos

A exclusão física de EnvironmentType usa AuditAction.PURGE e o evento ENVIRONMENT_TYPE_PURGED. O command busca o registro sob lock pelo identificador, valida, preserva o output, remove/flush e retorna o objeto removido. O aspect captura o retorno na mesma transação; o controller mantém 204. Rejeições não publicam evento e falha da auditoria causa rollback da remoção.

## Versão na criação de tipos

Na criação de EnvironmentType, version enviada pelo cliente é ignorada na montagem da entidade; o Hibernate define a versão inicial em 0. O schema compartilhado mantém a validação estrutural desse campo. Na edição, version permanece obrigatória e é comparada com a versão persistida.

## Verificação

`mvn --offline --batch-mode --no-transfer-progress '-Dit.test=EnvironmentApiIT' verify` passou: 7 testes de integração, sem falhas. A cobertura inclui validação do schema completo e campos desconhecidos, `settings` como JSON nativo, escopo e hierarquia, bloqueio de pai com filhos, `allowsChildren`, atualização de detalhes, concorrência entre desligar a permissão e criar um filho, e proteção concorrente contra ciclo de compatibilidade.

Compatibilidades revisadas: [ENVIRONMENT-TYPE-COMPATIBILITY-CODE-REVIEW.md](ENVIRONMENT-TYPE-COMPATIBILITY-CODE-REVIEW.md). Inativação preserva vínculos existentes; novos vínculos exigem compatibilidade ativa. Exclusão é PURGE físico, permitido apenas para compatibilidade inativa e sem uso.
