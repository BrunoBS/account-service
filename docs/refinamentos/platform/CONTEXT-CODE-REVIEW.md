# Revisão de Platform — Contexts

> Registro da revisão anterior à separação dos quatro fluxos. A estrutura e os contratos atuais, incluindo a retirada de `featureIdentifiers` do cadastro de contexto, estão em [Platform — separação de responsabilidades](PLATFORM-STRUCTURE-REFACTOR.md). As evidências abaixo são históricas.


Data: 13/10/2026. Critérios: [padrão de revisão](../../REVIEW-STANDARD.md).

## Escopo e responsabilidades

Revisão completa das sete rotas de `/api/v1/platform/contexts`, do controller à facade e aos use cases, incluindo domínio, repositories, contrato JSON, autorização, lifecycle, vínculos com features, concorrência e auditoria. As revisões anteriores de [microservices](MICROSERVICE-CODE-REVIEW.md) e [features](FEATURE-CODE-REVIEW.md) foram preservadas e tiveram as integrações necessárias atualizadas.

| Componente | Responsabilidade final |
| --- | --- |
| `FeatureContextController` | Requests tipados, parâmetros HTTP, paginação padrão e tradução de outputs. |
| Requests + `FeatureContextNormalizer` | Validar a árvore JSON original pela biblioteca; normalizar somente strings conhecidas. |
| `FeatureContextFacade` | Exigir OWNER e a ação correspondente em todas as operações. |
| `FeatureContextCommandService` | Coordenar transação, busca atual com lock, validação, mutação, sincronização dos vínculos, flush e snapshot. |
| `FeatureContextQueryService` | Consultar detalhe ACTIVE e páginas com o `LifecycleFilter` central. |
| `FeatureContextFinder` | Separar a leitura ativa da leitura atual com lock exclusivo, inclusive para associações individuais de features. |
| `FeatureContextValidator` | Encapsular consultas de unicidade, versão do cliente, código imutável, pré-condição de associação, dependências de exclusão e sort permitido. |
| `FeatureContextFeatureOperations` | Resolver seleção em lote, calcular inclusões/remoções e aplicar somente as diferenças na ordem segura. |
| `FeatureContextAssociationOperations` | Auditar alterações reais, individuais ou em lote; devolver snapshot que identifica também o context de destino. |
| `FeatureContext` | Encapsular identidade, atributos e transições de lifecycle. |
| Repositories | Paginação, projeções, constraints e leituras atuais necessárias à integridade. |

Não foi criado mapper para dois atributos textuais: a construção de context e seu update já são curtos e explícitos. O output continua sendo um record de valores escalares, sem entidade JPA ou parsing no controller. A extração da sincronização removeu do command a complexidade real de diferenças de conjuntos, resolução de referências e locks.

## Endpoints e fluxo

Rotas relativas a `/api/v1/platform/contexts`. `{contextIdentifier}` é UUID público; o ID numérico permanece interno.

| Endpoint | Entrada e pré-condição | Fluxo e resultado |
| --- | --- | --- |
| `POST /` | `code`, `name`; descrição e lista de features opcionais. Versão não nula é rejeitada. | Normalização → schema → OWNER → unicidade → novo context ACTIVE → vínculos elegíveis em lote → flush → `FEATURE_CONTEXT_CREATED` → 201. |
| `GET /{contextIdentifier}` | Context ACTIVE. | OWNER → Finder ativo → output → 200. Ausente, INACTIVE ou QUARANTINED: 404. |
| `GET /` | `lifecycle`, `page`, `size`, `sort`. | OWNER → paginação/sort → filtro central → página no banco → 200. |
| `PUT /{contextIdentifier}` | ACTIVE, versão atual, código registrado e nome único. | OWNER → leitura atual bloqueante → validação → substituição opcional de vínculos → atualização dos atributos → flush → `FEATURE_CONTEXT_UPDATED` → 200 com versão atualizada. |
| `PATCH /{contextIdentifier}/inactivate` | ACTIVE. | Lock → INACTIVE → flush → `FEATURE_CONTEXT_DEACTIVATED` → 200. Mantém vínculos existentes. |
| `PATCH /{contextIdentifier}/activate` | INACTIVE. | Lock → ACTIVE → flush → `FEATURE_CONTEXT_ACTIVATED` → 200. ACTIVE e QUARANTINED são origens inválidas. |
| `DELETE /{contextIdentifier}` | INACTIVE e sem vínculo com qualquer feature, independentemente do lifecycle da feature. | Lock → consulta limitada de dependência atual → QUARANTINED → flush → `FEATURE_CONTEXT_DELETED` → 204. |

DELETE não remove fisicamente a linha. Usa ação de auditoria `DELETE`, não `PURGE`. O command retorna o objeto em quarentena para o aspect, enquanto facade/controller preserva HTTP 204 sem body. Não existe restauração de QUARANTINED pelo endpoint activate.

## Contrato HTTP e mudanças para consumidores

| Campo | Contrato |
| --- | --- |
| `code` | Obrigatório, string até 50, kebab-case com letra inicial. Trim e minúsculas; imutável depois do cadastro. |
| `name` | Obrigatório, texto não branco, até 100; trim e unicidade global. |
| `description` | Opcional, string ou null, até 500; trim. |
| `version` | Inteiro não negativo até Long.MAX_VALUE ou null no schema compartilhado. Create rejeita valor não nulo; update exige versão atual. |
| `featureIdentifiers` | Opcional, array ou null, até 100 itens por requisição; cada item string UUID. Trim e minúsculas dos UUIDs textuais; duplicados, inclusive com diferença de caixa, resultam em um único vínculo. |
| Campos desconhecidos | Rejeitados antes de coerção/descarte do DTO, incluindo identifier, lifecycle, timestamps, ator e settings. |

```json
{
  "code": "account-management",
  "name": "Gestão de contas",
  "description": "Contexto de administração de contas",
  "featureIdentifiers": ["22222222-2222-2222-2222-222222222222"]
}
```

Na edição, enviar também `version`, usando a versão da leitura mais recente, e o mesmo código registrado. A versão controla tanto atributos quanto alterações dos vínculos. A existência de `@Version` no Hibernate continua protegendo a persistência; o input versionado protege o intervalo entre a leitura do cliente e sua edição posterior.

O schema único `schemas/context.json` é publicado pela V64 em `PLATFORM/context`, definição `context-resource`, versão 1. A migration não altera colunas, registros ou relações existentes. Limites 50/100/500 coincidem com as colunas atuais. Context não tem settings configuráveis; não foi introduzida validação fictícia de settings.

GET da coleção retorna `Page`, com `content` e metadados. Padrões: ACTIVE, página 0, tamanho 20 e ordenação por ID. O resolver limita size a 100. Sort permite `id`, `identifier`, `code`, `name`, `createdAt`, `updatedAt`; caminhos internos, propriedades desconhecidas e offsets acima de Integer.MAX_VALUE são rejeitados. Lifecycle aceita exclusivamente ACTIVE, INACTIVE e QUARANTINED pelo mecanismo central.

**Impacto de contrato:** coleção deixa de devolver array; PUT exige `version` e `code`; detalhe/update exigem ACTIVE; DELETE exige INACTIVE e ausência de vínculos; lotes de referências têm limite de 100 itens. Associar/remover uma feature individualmente também avança a versão do context e invalida um PUT baseado em leitura anterior.

## Regras de vínculos e atomicidade

| Situação | Comportamento |
| --- | --- |
| Lista omitida/null no create | Nenhuma associação. |
| Lista omitida/null no update | Preserva todos os vínculos. |
| Lista vazia no update | Remove todos os vínculos. |
| Lista informada no update | Substitui o conjunto atual; altera somente diferenças. |
| Referência repetida | Um vínculo, sem evento duplicado. |
| Feature inexistente | 404 e rollback integral. |
| Nova inclusão | Exige context, feature e microservice ACTIVE. |
| Vínculo existente com feature/pai inativo | Pode permanecer; a elegibilidade é revalidada somente para novas inclusões. |
| Context inativado | Mantém vínculos e impede novas inclusões. |
| Remoção individual | Permitida em qualquer lifecycle, para possibilitar limpeza antes da exclusão. |
| Repetição individual sem alteração real | Não muda versão de feature/context nem gera novo evento de vínculo. |

A resolução inicial dos UUIDs usa uma projeção em lote; não faz uma consulta de existência por item nem carrega todas as features da base. Somente as features alteradas são carregadas e bloqueadas. Referências preservadas não geram mutação, incremento de versão de feature ou evento de associação.

Context, vínculos, versões de features e eventos de auditoria pertencem à mesma transação. Falha em qualquer referência, gravação ou auditoria reverte toda a operação, inclusive eventos de feature já acrescentados antes do evento final do context.

## Concorrência e desempenho

| Regra | Proteção |
| --- | --- |
| Estado atual depois de esperar lock | Finder obtém ID com lock exclusivo, detach da instância em cache e nova consulta bloqueante da entidade. |
| Duas edições com a mesma versão | Apenas uma aplica a mutação; a seguinte lê a versão atual e retorna 409. |
| Mudança individual de vínculo depois da leitura do cliente | Marca o context atualizado na mesma transação, avança sua versão e impede PUT desatualizado. |
| Transação com snapshot de relações anterior | Com context já bloqueado, compara a versão do snapshot com a versão atual antes de usar a lista de relações; divergência retorna 409 sem mutação. |
| Trocas de vínculos A/B entre contexts | Todos os pais das inclusões antes de qualquer feature; features alteradas por ID crescente; relações individuais alteradas depois desses locks. |
| Contexts independentes recebendo features diferentes | Sem consulta bloqueante de intervalo sobre a tabela de relações para montar o conjunto inicial. Cenários de criação e atualização concorrentes têm testes repetidos. |
| Novo context com vínculos | Sua linha ainda está sem commit; não há relação externa anterior a confirmar. Evita consulta de intervalo desnecessária. |
| Delete após vínculo recém-confirmado | Context bloqueado antes de consultar a primeira relação com lock compartilhado; vê dependência atual mesmo com snapshot anterior. |
| Unicidade concorrente | Queries de negócio para mensagens amigáveis e constraints únicas como garantia final; resposta sem SQL/constraint interna. |
| Listagem | Página e count no banco, sem carregar relações nem inicializar features. |

O protocolo de snapshot depende da regra explícita de que toda mudança real de relações também altera a versão do context na mesma transação, sob seu lock. Os dois pontos de escrita — associação individual em features e substituição em contexts — seguem essa regra. Os testes incluem input com versão atual dentro de uma transação que ainda mantém snapshot antigo, para provar que o segundo controle é necessário.

Commands usam somente `@Transactional`; queries comuns usam `readOnly=true`. Não foi imposto isolamento específico ou MANDATORY. O bloqueio é dos recursos envolvidos, até o commit; leituras comuns seguem o isolamento da conexão. Os testes verificam integridade e sincronização, não throughput ou capacidade por volume. Não foram adicionados índices especulativos.

## Auditoria

Os cinco comandos de mutação do context retornam snapshot depois de flush, com identidade pública, lifecycle e versão persistida. Alterações reais de associação seguem os mesmos eventos individuais e em lote. O snapshot de vínculo agora inclui `contextIdentifier`, além dos atributos da feature; anteriormente, o payload identificava somente a feature e não permitia determinar qual context foi associado/removido.

`FeatureContextAssociationOutput` usa o snapshot da feature e o context de destino para o aspect. O controller de features continua recebendo a representação normal de feature. A mudança enriquece a auditoria sem acrescentar campos de implementação às respostas HTTP.

## Achados e ajustes

| Local | Severidade | Evidência / impacto | Ajuste e verificação |
| --- | --- | --- | --- |
| Requests | Alta | Ausência de schema original permitia coerção e descarte de campos desconhecidos. | Contrato único V64, normalizador e advice central; API cobre create/update e itens de referências inválidos. |
| Command/update | Alta | Sem versão do cliente, edições posteriores podiam sobrescrever atributos/listas anteriores. | Versão obrigatória e comparação atual sob lock; PUT simultâneo e cache JPA testados. |
| Associações individuais | Alta | Vínculo mudava sem avançar a versão do context, permitindo substituição com versão antiga. | Marca context na mesma transação somente quando há mudança real; retries não avançam versão. |
| Domínio/lifecycle | Alta | Activate restaurava quarentena; update/delete aceitavam origens inadequadas. | Guards ACTIVE/INACTIVE, sem restauração de quarentena; testes de domínio e API. |
| Vínculos/snapshot e locks | Alta | Leituras de relações com intervalo bloqueado podiam formar ciclos em contexts independentes; snapshot antigo não pode determinar a lista atual. | Snapshot versionado sob lock do context, referências em lote e pais/features na ordem definida; testes concorrentes. |
| Auditoria de vínculo | Média | Payload só identificava feature, omitindo o context alterado. | Output auditado inclui contextIdentifier; API confirma os dois recursos e rollback após evento parcial. |
| Query/findAll | Média | Lista ilimitada carregava toda a coleção filtrada. | Page e filtro central no banco; sort/page inválidos rejeitados. |
| Validator | Média | Command calculava flags e validator repetia estrutura HTTP em chamada extensa de booleans. | Consultas encapsuladas no validator, estrutura no schema; eliminado helper estrutural sem consumidores. |
| Sincronização/identificadores | Média | Uma consulta por referência e responsabilidades de relação dentro do command. | Projeção em lote e operação própria para seleção/diferenças; command curto e transacional. |
| Finder | Baixa | Carregamento de context estava dentro do Finder de feature. | Finder próprio de context, reutilizado pelos comandos de ambos os fluxos. |
| Controller/facade | Baixa | Parâmetros `f`, `c`, `q`, `i`, `in`, `r` dificultavam leitura. | `contextIdentifier`, `input`, `request`, `commandService`, `queryService` e imports explícitos. |

## Verificação

- Primeira rodada focada: 51 testes de integração de contexts/features/lifecycle e 106 unitários/arquitetura aprovados.
- `ContextApiIT`: 17 testes aprovados, cobrindo contrato original, normalização, limites, UUIDs, versão, código imutável, paginação, lifecycle, autorização, referências, idempotência, auditoria, rollback e unicidade concorrente.
- `ContextConcurrencyIT`: 8 execuções aprovadas, cobrindo cache JPA, snapshot anterior com input atual e criações/atualizações independentes concorrentes repetidas.
- `ContextResourceSchemaMigrationIT`: upgrade V63 → V64, igualdade do schema publicado com o arquivo e preservação do context/version/limites/vínculos existentes.
- Regressões: FeatureApiIT, FeatureConcurrencyIT, PlatformApiIT, Shared, microservices e listagem central de lifecycle.
- Resultado final: **106 testes unitários/arquitetura e 333 testes de integração em 55 classes**, sem falhas, erros ou skips. Clean inicial e verify dos quatro lotes aprovados.
- Confirmação final: `mvn --offline --batch-mode --no-transfer-progress -Dit.test=ContextApiIT verify`, com 17 testes de API e 106 unitários/arquitetura aprovados. Inclui UUIDs com diferença de caixa, snapshots de vínculo e rollback após auditoria parcial; log `/private/tmp/context-api-final.log`.
- `git diff --check` aprovado. Não restam achados bloqueadores ou de alta severidade abertos no fluxo revisado. As revisões de microservices, features e contexts estão concluídas.

Logs dos lotes: `/private/tmp/context-final-batch-{1,2,3,4}.log`; agregado: `/private/tmp/context-final-results.json`. Cada lote tem até 16 classes, liberando containers entre JVMs para respeitar os recursos locais do Docker.

Nenhum purge, mudança de isolamento, novo endpoint ou alteração de colunas foi incluído. O contrato de retenção do contexto e as relações explícitas existentes permanecem coerentes com os outros elementos de Platform.

A [revisão geral dos endpoints](PLATFORM-ENDPOINTS-REVIEW.md) complementou a
limpeza: GET de contexts vinculados a uma feature passou a aceitar o pai em
qualquer lifecycle para OWNER, preservando o filtro de lifecycle dos contexts.
