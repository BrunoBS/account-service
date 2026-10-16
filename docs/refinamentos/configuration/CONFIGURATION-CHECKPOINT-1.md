# Configuration V1 — checkpoint 1: análise

> Registro histórico anterior à renomeação. O contrato atual é [DynamicParameter / ParameterKey](../dynamic-parameter/DYNAMIC-PARAMETER.md).

Referência: `guideline-java/docs/refinamento/14-configuration/configuration-refinement-v1.md`, especialmente seções 2–8. Padrões locais: [REVIEW-STANDARD](../../REVIEW-STANDARD.md).

Status: análise e decisões aprovadas pelo usuário antes do checkpoint 2. Nenhum código de produção foi alterado durante esta análise. A prova de precisão usa arquivos temporários em `/private/tmp`. O [checkpoint 2](CONFIGURATION-CHECKPOINT-2.md) registra a implementação, a perda decimal encontrada no round-trip MySQL e a decisão posterior do usuário de manter o armazenamento JSON nativo.

## Repositórios e baseline

| Repositório | Estado encontrado | Encaminhamento |
| --- | --- | --- |
| `account-service` (artifact/aplicação `workspace-service`) | Branch `feature/segments-categories-v1`, com segmentos e revisão de persistência ainda não commitados; migrations até V67. | Implementar Configuration na mesma branch, preservando os trabalhos existentes. |
| `platform-libraries` | Branch `main`, working tree limpa; Java 25, Boot 4.1.1, Jackson 3.2.3, networknt 3.0.7; capabilities em `modules/`. | No checkpoint da biblioteca, criar branch com o mesmo nome da branch de trabalho do serviço; não alterar/mesclar main. |
| `guideline-java` | Refinamento exige cinco checkpoints, aprovação entre eles e nenhuma dependência nova sem autorização. | Usado como referência de leitura; não alterar o refinamento. |

Não existem `AGENTS.md` nos repositórios inspecionados ou nos diretórios ancestrais da workspace. Esta análise não cria commit, push ou merge.

## Inventário e divergências

| Área | Evidência local | Decisão proposta |
| --- | --- | --- |
| Catálogos | `CatalogEntity`, `EnumCatalogService`, repository/facade/controller compartilhados. Desde V6, a PK dos catálogos é `code`, sem ID numérico. | Criar `configurationtype` e `configurationstatustype` em `foundation.catalog`, usando FK `type_code` / `status_code`, em vez de introduzir `type_id` / `status_id` e um padrão paralelo. |
| Microservice | Entidade em `feature.platform`, tabela `platform_microservices`, referência interna BIGINT e UUID público. | Configuration guarda somente `microservice_id`; integração via Port/Adapter e use case público do módulo Platform, sem acesso ao repository de outro módulo. |
| Código do microsserviço | `Microservice.code` tem `updatable=false`; o validator rejeita mudança e o domínio não oferece setter. | Preservar a imutabilidade atual. A feature não habilita alteração de código. Mudança operacional direta continua exigindo análise de consumidores/Redis/mensagens. |
| Microsserviço global | Schema, normalizador e convenção atual usam códigos em minúsculas. V12 migra o catálogo antigo e V15 normaliza os códigos. Não foi identificada proteção específica do registro global. | Usar o código físico `platform`, correspondente aos exemplos `platform.*`; garantir sua existência sem sobrescrever dados existentes e impedir sua inativação. A grafia literal `PLATFORM` do refinamento precisa ser alinhada a essa decisão. |
| Snapshot publicado | O refinamento permite editar `type` preservando `published_value`. Sem guardar o tipo anterior, a reconciliação montaria um envelope com o tipo novo e o valor antigo. | Adicionar `published_type_code` nullable, FK do mesmo catálogo. Publicar copia tipo e valor; edição preserva ambos; inativação limpa ambos. Não introduz histórico nem outra versão de publicação. |
| Persistência | Golden usa `@Version`, UUID VARCHAR(36), timestamps, Finder com leitura atual após lock e saveAndFlush. | Reutilizar os padrões; a versão técnica de concorrência não é histórico de publicação. JsonNode exige mapeamento que diferencie Java null/SQL NULL de NullNode/JSON null; comprovar em MySQL. |
| Redis | Mensagens/schema já oferecem integração opcional com Redis; essas dependências não são transitivas para o serviço. Seu classpath atual não contém Redis/Lettuce. | O checkpoint 3 requer declarar `spring-boot-starter-data-redis`, usando o baseline já gerenciado. Solicitar aprovação antes da inclusão. Não adicionar biblioteca de lock. |
| Agendamento | Não há scheduler/coordenação distribuída para Configuration no serviço ou biblioteca. Spring Context já está disponível. | Scheduler próprio da feature, aproximadamente horário e habilitação explícita; sincronização coordenada por primitives Redis e Lua, conforme proposta abaixo. Validar em Redis real antes de liberar. |
| Validação | `SchemaValidator` resolve schema publicado e mapeia erros sem reproduzir o valor inválido. O adapter antigo de Schema utiliza mensagens brutas da engine. | Reutilizar a validação HTTP da biblioteca com o contrato único `PLATFORM/configuration`; as regras de expressão são condicionais nesse schema, conforme ajuste do usuário antes do checkpoint 3. Não usar mensagens brutas que possam incluir configuração em logs. O valor de CONFIGURATION permanece JSON livre. |
| Logs/auditoria | Logging de request body é opcional, desabilitado por padrão; o mascaramento atual protege campos textuais conhecidos, não todo JSON arbitrário. Auditoria captura o retorno do command. | Proibir logging de body nos fluxos de Configuration, inclusive erros; testar valores sentinela em logs. Snapshots de comando serão metadados, sem `value`/`published_value`. Reutilizar auditoria existente; não criar outbox de publicação ou histórico de valores. |

### Prova de precisão executada

Executada com o classpath do último build aprovado do workspace e com `ResourceSchemaValidator` real da biblioteca. O schema de referência foi extraído do refinamento e seus limites LONG corrigidos para `-9223372036854775808` e `9223372036854775807`, como exige a seção 7.

- **12 casos aprovados:** limites exatos, vizinhos internos e externos de LONG, em operandos escalares e listas. Os dois vizinhos externos são rejeitados. Não foi encontrado bloqueio de precisão inteira na engine atual.
- `ObjectMapper` padrão **perde precisão** em `19.900000000000000000001`.
- A prova inicial com `USE_BIG_DECIMAL_FOR_FLOATS` preservou esse decimal na leitura Java. Posteriormente o usuário aprovou a precisão nativa do JSON MySQL e solicitou remover esse override global do parser HTTP, antes do checkpoint 3.
- `readTree("null")` produz NullNode presente, distinto de referência Java null.

Evidência: `/private/tmp/configuration-precision-analysis.log`; probe: `/private/tmp/ConfigurationPrecisionProbe.java`. Essa prova não substitui testes de HTTP, round-trip MySQL e Redis, a executar nos checkpoints seguintes.

Proposta: habilitar leitura precisa de números no mapper do serviço, com regressão das APIs existentes, e no mapper usado pela nova capability. Não normalizar, converter para Double ou reserializar números antes da validação. Conversões integrais rejeitarão overflow e truncamento; asBigDecimal preservará precisão e asDouble terá a aproximação explicitamente solicitada pelo consumidor, rejeitando resultado não finito.

## Contratos e decisões de domínio propostas

- Base administrativa: `/api/v1/configurations`; detalhe/update/delete por UUID público; PATCH `/{identifier}/activate`, `/{identifier}/inactivate`, `/{identifier}/publish`; publicação em lote por `POST /publish-batch`.
- Todas as operações administrativas usam OWNER e contexto confiável. A biblioteca é o único contrato runtime de consumo e lê exclusivamente Redis.
- Criação recebe `microserviceIdentifier`, `key`, `description`, `type`, `value`. Update recebe `version` técnica e apenas `description`, `type`, `value`; identidade/pai/chave no body são rejeitados. Use um schema estrutural por recurso, sem validar JSON livre como expressão.
- Configuration não tem `name` nem cópia de `microservice_code`. FK do pai referencia o registro existente; inativar o Microservice não dispara cascata de Configuration, regra não prevista no refinamento.
- Chave: trim, somente minúsculas, 1–80 caracteres, padrão `^[a-z0-9]+(?:[._-][a-z0-9]+)*$`, conforme alteração posterior solicitada pelo usuário. Maiúsculas e camelCase são rejeitados. Unicidade no banco usa comparação sensível à caixa, coerente com Redis. O code do microsserviço não admite pontos; a primeira ocorrência de ponto delimita o namespace sem ambiguidade. Não aceitar prefixo `config:` na chave pública do consumidor.
- Create nasce ativo/DRAFT e não publica. Mudança efetiva de tipo/valor volta a DRAFT e preserva **tipo e valor publicados**; descrição isolada preserva o status. Publish exige ativo/DRAFT e revalida expressão.
- Activate/inactivate idempotentes: repetição preserva versão e não cria evento adicional; retry de inativação pode remover resíduo runtime. Reativação real mantém DRAFT e ausência de publicação. Delete exige inatividade.
- Lote: UUIDs não vazios, sem repetição; locks em ordem determinística; carregar estado atual e validar todo o conjunto antes da primeira mudança; saveAllAndFlush e auditoria na mesma transação. Redis não participa dessa transação.
- Comandos auditados retornam metadados após flush; detalhe administrativo inclui o valor editável. PublishedValue não precisa ser exposto na UI. Publicação responde separadamente confirmação no banco e sincronização `SYNCED`/`PENDING`/`FAILED`, por chave no lote.
- JSON literal null é armazenável e publicável no tipo CONFIGURATION. Na biblioteca, ele é **presente**, mas não convertível pelos getters V1: sem default gera erro de conversão `JSON_NULL`; com default retorna o valor explícito e registra a categoria. Ausência continua sendo NOT_FOUND. Defaults de referência null serão rejeitados. Expressões/operandos null são inválidos.
- `path("retry.attempts")` navega objetos por pontos. JSON Pointer oferece nomes literais e arrays: `path("/retry.attempts")` seleciona a propriedade literal e `path("/accounts/0/id")` acessa array; `~0`/`~1` escapam `~`/`/`. Não escolher automaticamente entre propriedade literal e caminho aninhado.
- Comparações: somente os oito operadores V1; strings exatas; Integer/Long/BigDecimal comparados por valor; sem string numérica implícita, grupos, AND/OR ou Boolean no avaliador. Conversões/defaults geram erros/logs padronizados, sem payload.

## Proposta de sincronização concorrente — checkpoint 3

Um serviço único atende callbacks após commit e reconciliação. O callback usa a chave como referência e consulta novamente o estado **atual e commitado**, em vez de carregar o snapshot antigo do comando. A consulta considera `active=true` e `published_value IS NOT NULL`, inclusive DRAFT, e monta o envelope com `published_type_code` e `published_value`.

Todas as instâncias compartilham uma lease de coordenação Redis com token aleatório de proprietário. Aquisição/release usam primitives Redis; cada escrita/remoção executa Lua que confirma o token ainda vigente antes da mudança. Lease expirada, callback atrasado ou comando executado após timeout não podem modificar Redis sem propriedade atual. Contenção/indisponibilidade resultam em PENDING/FAILED e recuperação pelo scheduler; não desfazem o commit do banco.

A lease tem expiração de coordenação; **os valores runtime não têm TTL**. Nenhuma biblioteca adicional de lock, contador/versão de publicação no banco ou outbox é necessária. Redis é acessado depois das transações de leitura/escrita do banco, e não dentro delas.

Reconciliação usa paginação de registros publicados e SCAN limitado a `config:*`. Antes de remover qualquer candidato obsoleto, relê sua situação atual; não usa apenas um conjunto antigo de chaves capturado no início da varredura. Scripts de exclusão também verificam a lease. Isso protege publicação concorrente, recriação após delete e outros namespaces. Cada instalação deve ter seu namespace `config:*` exclusivo na instância/database Redis configurada, sem introduzir ambiente lógico no modelo.

Essa é uma proposta arquitetural, **ainda não uma garantia comprovada**. O checkpoint 3 deve testar duas instâncias, lease expirada durante processamento, comandos atrasados, timeout, publicação/inativação concorrentes, Redis vazio e recuperação. Se a prova falhar, corrigir ou apresentar o bloqueio antes de liberar.

## Plano por arquivo e repositório

### Checkpoint 2 — workspace-service

| Arquivos/pacotes previstos | Responsabilidade |
| --- | --- |
| `db/migration/V68__configuration_catalogs_and_platform_microservice.sql` | Catálogos/seeds Golden; garantir microsserviço global sem recriar cadastro existente. |
| `db/migration/V69__create_configurations.sql` | Entidade, FKs por code, unique UUID e `(microservice_id, configuration_key)`, campos JSON, snapshot de tipo publicado e índices de consultas. Não editar V1–V67. |
| `db/migration/V70__configuration_resource_schemas.sql`, `db/migration/V71__unify_configuration_resource_schema.sql`, `db/migration/V72__configuration_decimal_multiple_of_schema.sql`, `db/migration/V73__configuration_lowercase_key_schema.sql`, `schemas/configuration.json` e schemas dos dois catálogos | Contrato único de criação/edição com expressão condicional e limites numéricos exatos, conforme ajuste solicitado antes do checkpoint 3; versões anteriores preservadas; DECIMAL em centésimos por solicitação posterior do usuário. |
| `foundation/catalog/configurationtype/{domain,repository,usecase,facade}` e `configurationstatustype/...`; controllers em `entrypoint/web/catalog/...` | Reutilizar CatalogEntity/EnumCatalogService e policies Golden. |
| `feature/configuration/domain/{Configuration,ConfigurationMessageKeys,Operator,ValueType}` | Invariantes, transições, cópia independente de JsonNode e enums do avaliador. |
| `feature/configuration/repository/ConfigurationRepository` e mapeamento JSON local | Locks, projeções, filtros/páginas e persistência exata, inclusive SQL NULL versus JSON null. |
| `feature/configuration/usecase/{model,operations,validation}` e `facade/ConfigurationFacade` | CRUD, versão/locks atuais, normalização apenas dos campos conhecidos, saveAndFlush, outputs/auditoria sem valores. |
| `feature/configuration/integration/ConfigurationMicroservicePort` / adapter; query público em `feature/platform/usecase/operations/microservice` | Resolver referências públicas e internas em lote pela fronteira do módulo. |
| `feature/platform/domain/microservice/Microservice.java` e mensagens/testes correspondentes | Proteger o microsserviço global contra inativação, preservando imutabilidade de código existente. |
| `entrypoint/web/configuration/{ConfigurationController,request,response}` | Requests tipados, schema sobre JSON original e contratos por UUID. |
| Defaults de parsing JSON, bundles `messages_{pt_BR,en}.properties`, fixtures de schema, README e docs | Parsing padrão solicitado pelo usuário, i18n, reposição de seeds/schema e registro de decisões. |
| `ConfigurationDomainTest`, `ConfigurationArchitectureTest`, `ConfigurationApiIT`, `ConfigurationMigrationIT`, `ConfigurationConcurrencyIT` | Transições, imutabilidade, autorização, limites/JSON, upgrade, FKs/unique, round-trip e conflitos concorrentes. CRUD sem publicação automática. |

### Checkpoint 3 — workspace-service

| Arquivos/pacotes previstos | Responsabilidade |
| --- | --- |
| `usecase/operations/ConfigurationPublicationService` e inputs/outputs de publish | Publicação individual/lote, pré-validação completa, commit e resultado runtime separado. |
| `integration/ConfigurationRuntimePort`, adapter Redis, scripts Lua, coordenador de lease e serviço de sincronização | Envelope publicado, token de propriedade, remoções idempotentes e falhas sem payload. |
| Configuração/agendamento dentro da feature e `ConfigurationReconciliationService` | Habilitação explícita, execução horária, paginação/SCAN e recuperação. |
| `pom.xml`, `application.yml`, schema/request do lote em migration nova | Redis starter gerenciado, somente após aprovação; sem dependência de lock. |
| `ConfigurationPublicationIT`, `ConfigurationRedisIT`, `ConfigurationReconciliationIT` e extensões de concorrência/API | Lote tudo-ou-nada, falha no meio da persistência, callbacks após commit, divergência, restauração DRAFT e concorrência entre instâncias. Redis real via infraestrutura de teste existente/GenericContainer. |

### Checkpoint 4 — platform-libraries

| Arquivos/pacotes previstos | Responsabilidade |
| --- | --- |
| `modules/platform-configuration/pom.xml`; reactor `pom.xml`; `platform-libraries-bom/pom.xml` | Nova capability opt-in; Jackson, messaging, Redis/Spring e testes sob o baseline existente, sem versões externas novas. |
| `...library.configuration/{ConfigurationProvider,ConfigurationValue}` | API pública aprovada, sem JsonNode no consumidor. |
| Implementação Redis, navegação/conversão e avaliador de expressão | Leitura exclusiva, precisão, operadores V1, defaults explícitos e nenhuma consulta HTTP/MySQL/cache local. |
| Autoconfiguração, properties, `AutoConfiguration.imports`, exceptions/message keys e bundles | Ativação explícita, StringRedisTemplate padrão, MDC/correlation-id e mensagens seguras. |
| Proteção do logging de Configuration na capability existente, se necessária além da configuração desabilitada | Não confiar em mascaramento parcial de JSON arbitrário; evitar body/valores também em falhas técnicas. |
| Testes unitários, Redis integrado, autoconfiguração/arquitetura e README | Getters/objetos/listas/path, null/ausência, limites/decimais, matches, default/falhas/logs e ausência de fallback automático. |

### Checkpoint 5 — fechamento

Executar checks focados por etapa e verify completo dos dois repositórios; instalar artifacts da biblioteca para testar o serviço contra a capability resultante; validar arquitetura, rollback/migrations, contracts e logs. Documentar consistência eventual, configuração operacional do Redis e scheduler, impacto de alterações administrativas diretas do código do microsserviço e rollback compatível. Preparar evidências para PR sem merge/push automático.

## Aprovação necessária

Para avançar ao checkpoint 2, aprovar os alinhamentos ao projeto real: FKs por **code**, registro global **platform**, preservação da imutabilidade atual de Microservice.code e campo **published_type_code** para manter o snapshot publicado correto. Aprovar também as decisões de chave, idempotência e JSON null acima. A dependência Redis será submetida no checkpoint 3, antes de sua inclusão.

A seção 8 do refinamento determina: **“aguardar aprovação explícita antes de avançar”**. Por isso este checkpoint termina com o plano revisável, sem implementar domínio/CRUD ainda.
