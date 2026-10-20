# workspace-service

> **Contrato de implementação vigente:** [docs/IMPLEMENTATION-BASELINE.md](docs/IMPLEMENTATION-BASELINE.md). O nome Git `account-service` é histórico; a aplicação ativa é `workspace-service`. Planos G0–G4 e refinamentos versionados são registros históricos quando divergirem do código da `main`.


Serviço de referência para o domínio de Workspace da Golden Platform.

Este repositório implementa a fase `GOLDEN-REFERENCE-V1` sobre o checkpoint `FOUNDATION-GOLDEN-V1`.

## Identidade

- Maven: `br.com.portalmanager.platform.workspace:workspace-service:0.1.0-SNAPSHOT`
- Java package root: `br.com.portalmanager.platform.workspace`
- hospedagem atual: `BrunoBS/account-service` (nome histórico do repositório; não representa o domínio ativo)

## Baseline

- Java 25
- Spring Boot 4.1.1
- Maven >= 3.9.9
- `br.com.portalmanager.platform.library:platform-parent:1.0.0`
- `br.com.portalmanager.platform.library:platform-libraries-bom:1.0.0`
- `br.com.portalmanager.platform.library:platform-starter:1.0.0`
- `br.com.portalmanager.platform.library:platform-testing-http:1.0.0`;
- `br.com.portalmanager.platform.library:platform-testing-database:1.0.0`;
- `br.com.portalmanager.platform.library:platform-testing-authorization:1.0.0`
- `br.com.portalmanager.platform.library:platform-catalog:1.0.0`
- `br.com.portalmanager.platform.library:platform-redis:1.0.0` (runtime de Parameter)
- `br.com.portalmanager.platform.library:platform-testing-redis:1.0.0` (teste)

## Arquitetura

A aplicação utiliza as macrozonas:

```text
br.com.portalmanager.platform.workspace
├── WorkspaceServiceApplication
├── config/{WorkspaceConfiguration,WorkspaceProperties}
├── foundation
│   ├── audit/{domain,repository}
│   ├── catalog/<catalog-type>/{domain,facade,repository,usecase}
│   ├── integration/*Port
│   └── schema/{domain,facade,integration,repository,usecase}
├── core
│   ├── workspace/{config,domain,facade,integration,repository,usecase}
│   ├── application/{config,domain,facade,repository,usecase}
│   ├── publisher/{domain,facade,repository,usecase}
│   └── environment/{domain,facade,integration,repository,usecase}
├── feature
│   ├── dynamicbundle/{domain,facade,repository,usecase/{model,operations,validation}}
│   ├── onboarding/{domain,repository,integration,usecase/{model,operations,validation}}
│   ├── servicecontext/{domain,repository,usecase/{model,operations,validation}}
│   ├── parameter/{domain,repository,integration,facade,usecase/{model,operations,validation}}
│   ├── segmentation/{domain,repository,facade,usecase/{model,operations,validation}}
│   └── sharing/{domain,repository,integration,usecase/{model,operations,validation}}
└── entrypoint
    ├── catalog                 # controllers genéricos ficam diretamente aqui
    ├── workspace/{request,response}
    ├── application/{request,response}
    ├── dynamicbundle/{message,messagetranslator}
    ├── servicecontext/{microservice,feature,featuregroup}
    ├── segmentation/{segment,category}
    ├── sharing/{contract,participant}
    └── environment, onboarding, parameter, publisher, schema
```

Pacotes vazios não são criados apenas para completar a árvore.

O cadastro administrativo de microserviços usa `feature/servicecontext` e
`/api/v1/service-contexts/microservices`. A migração V19 renomeia as referências de
Feature e Message e remove os catálogos de linguagem e infraestrutura. Consulte
[`MICROSERVICE-MIGRATION-V19.md`](docs/refinamentos/foundation/MICROSERVICE-MIGRATION-V19.md)
para os contratos de atualização.

### Organização interna do domínio

Módulos com mais de uma responsabilidade de domínio agrupam suas classes em
`domain/<responsabilidade>`. Entidades dependentes e objetos de valor permanecem
junto ao agregado ao qual pertencem; ter várias classes não exige criar subpacotes.

| Módulo              | Subpacotes de domínio                                      |
| ------------------- | ---------------------------------------------------------- |
| `feature.servicecontext` | `feature`, `microservice`, `featuregroup`                |
| `feature.sharing` | `contract`, `participant` (inclui os mapeamentos)        |
| `feature.segmentation` | `segment`, `category` (catálogo global)                   |
| `feature.parameter` | sem subpacote no domínio; `ParameterKey` representa parâmetros globais ou de um microsserviço. |
| `core.environment`  | `environment`, `environmenttype` (inclui compatibilidades) |
| `foundation.schema` | `schema`, `version`, `binding`                       |

As features organizam as camadas por subdomínio ou responsabilidade; Dynamic Bundle usa `message`/`messagetranslator`, e Onboarding usa `completion`/`progress`/`phase`.
As chaves de mensagens compartilhadas entre responsabilidades permanecem na raiz
`<modulo>.domain`.

### Chaves de mensagens

As classes `*MessageKeys` ficam em `<modulo>.domain` ou `domain/<subdominio>`, junto ao domínio que
possui os erros. Esse padrão vale para módulos de `core`, `feature` e `foundation`.
Cada classe contém apenas constantes com as chaves; a resolução e a tradução das
mensagens permanecem no mecanismo de messaging.

O pacote `usecase.model` concentra contratos de entrada e saída (`Input` e `Output`).
Validadores, use cases e integrações utilizam as chaves do domínio do próprio módulo.
A localização é verificada pelo teste de arquitetura
`GoldenArchitectureTest.messageKeysMustResideInModuleDomain`.

### Listagem de workspaces

`GET /api/v1/workspaces` retorna o `PagedModel` do Spring Data: `content` e
`page` com `number`, `size`, `totalElements` e `totalPages`. Os parâmetros `page` e `size` usam os padrões
0 e 20, com tamanho máximo de 100. Os filtros `active`, `typeName` e `tagName`
continuam disponíveis, e `sort` permite ordenar pelos campos simples do workspace.
Conteúdo e contagem respeitam a visibilidade do usuário. O Spring interpreta os
parâmetros de paginação e aplica seus padrões e limites.

O payload aceita até 20 tags manuais e 5 aprovadores. Na edição, somente uma
sessão com `UserSession.isOwner()` pode alterar `workspaceType` ou
`authorizerGroup`; a autorização ADM continua necessária para os demais campos.
Na criação, usuários comuns podem criar somente workspaces `MANAGER`, sem grupo
autorizador. Somente uma sessão owner pode criar `ADMIN` ou informar
`authorizerGroup`.

### Listagem e revisão de aplicações

`GET /api/v1/workspaces/{workspaceIdentifier}/applications` usa o mesmo formato
`PagedModel` (`content` e `page`) da listagem de workspaces. `page`, `size` e
`sort` são tratados pelo Spring Data; filtros `active` e `tagName` permanecem
disponíveis. A contagem respeita a visibilidade do usuário. O endpoint `/summary`
continua retornando uma lista de representações pequenas para seleção.

Workspace e Application recebem requests tipados em create/update. O mecanismo
`ResourceSchemaRequestBodyAdvice` da biblioteca normaliza e valida o JSON recebido com o
`SchemaValidator` da biblioteca antes da desserialização, preservando a rejeição
de tipos inválidos e propriedades desconhecidas. Facade e command recebem inputs
tipados.

Application persiste `settings` como JSON e usa um schema único de criação/edição,
com limite de 20 tags manuais de até 50 caracteres. Nome e alias têm até 50
caracteres, sigla até 5 e grupo autorizador até 10, alinhados a Workspace. Os
campos opcionais de grupo e tags aceitam null. Settings é obrigatório e exige
`language` (JAVA, C#, NODE ou GO) e `infrastructure` (VM, ECS ou EKS). Nomes são únicos por
workspace, ignorando case,
inclusive para aplicações inativas. A revisão por classe e método está em
[`APPLICATION-CODE-REVIEW.md`](docs/refinamentos/application/APPLICATION-CODE-REVIEW.md).

Application aceita description opcional com até 200 caracteres. Workspace mantém description obrigatória (até 200), requester até 10 e e-mails de grupo/aprovadores até 100.

### Mensagens e traduções

A revisão dos dois fluxos administrativos está concluída no código e nos testes.
As listagens de `/api/v1/dynamic-bundles/messages` e `/api/v1/dynamic-bundles/messages/{messageIdentifier}/translators`
retornam `content` e `page`. Create aceita traduções inline; update exige `version`
e edita somente os atributos da mensagem. Traduções têm endpoints e versões próprios.

DELETE remove fisicamente uma mensagem INACTIVE e todas as suas traduções.
DELETE isolado de tradução exige que ela esteja INACTIVE. As exclusões são auditadas
como PURGE, com snapshots preservados antes da remoção e rollback transacional.

V58 publica os schemas; V59 preserva registros antigos em quarentena como INACTIVE
e restringe o lifecycle desses dois recursos a ACTIVE/INACTIVE. O Flyway habilitado
aplica as migrations na inicialização do serviço no banco configurado.
Consulte o [guia de integração](docs/refinamentos/dynamic-bundle/DYNAMIC-BUNDLE.md)
e o [relatório da revisão](docs/refinamentos/message/MESSAGE-CODE-REVIEW.md).

### Onboarding

O progresso e a conclusão ficam em `/api/v1/workspaces/{workspaceIdentifier}/onboarding`,
com GET e PATCH sem body. O fluxo aplica-se a MANAGER e preserva o histórico de
fases com `requester` autenticado de até 10 caracteres e data da conclusão.
Onboarding não gera eventos de auditoria. A V60 alinha a coluna do responsável e
recusa identidades antigas que não atendam ao novo limite.
As orientações das fases usam o schema de settings publicado pela V61, com
validação central no catálogo e mensagens localizadas para configuração inválida.

As duas fases de configuração de ambientes aguardam as integrações correspondentes;
criar um Environment isolado não as conclui. Consulte a
[organização atual](docs/refinamentos/onboarding/ONBOARDING.md)
para os ajustes implementados de arquitetura, nomes, mensagens e validação.

## Service Context

Microservice, Feature e FeatureGroup ficam em `feature.servicecontext`; a associação N:N é
`feature_group_feature` e o agregado usa a tabela `feature_group`. A API usa `/api/v1/service-contexts/microservices`, `/features`
e `/feature-groups`. Alterações exigem OWNER e versionamento; códigos são imutáveis.
Microservice e Feature preservam lifecycle e referências, sem DELETE.
FeatureGroup permite purge somente sem vínculos, inclusive com features inativas.

Associar/remover usa POST/DELETE em
`/api/v1/service-contexts/feature-groups/{featureGroupIdentifier}/features/{featureIdentifier}`.
Listar features por grupo usa `featureGroupIdentifier`; a consulta inversa usa
`featureIdentifier`. Locks, filtros de lifecycle, idempotência e auditoria são preservados.
V78 renomeia as tabelas do módulo, V81 renomeia FeatureGroup e sua associação e V82 alinha os títulos dos schemas, preservando dados. O binding HTTP passa
para `SERVICE_CONTEXT`; settings continuam em `MICROSERVICE/{code}` e `FEATURE/{code}`.
Consulte [Service Context](docs/refinamentos/service-context/SERVICE-CONTEXT.md).

## Resource Sharing

`feature.sharing` organiza Contract e Participant. Contract representa o destino;
Participants são origens autorizadas. Autorização, escopos, transições e mapeamentos de
ambiente permanecem iguais. As rotas usam `resource-sharing/contracts` e
`resource-sharing/participants` dentro do escopo workspace/application. V80 renomeia
objetos físicos preservando dados e cascatas.
Consulte [Resource Sharing](docs/refinamentos/resource-sharing/RESOURCE-SHARING.md).

## Segmentos e categorias

O catálogo global usa `/api/v1/segmentation/segments` e
`/api/v1/segmentation/segments/{segmentIdentifier}/categories`, com create,
update versionado, detalhe/listagem e PATCH activate/inactivate. Mutações exigem
OWNER; leituras autenticadas passam pela autorização OPEN. Detalhes e updates
administrativos incluem registros inativos; listas usam ACTIVE por padrão e
aceitam INACTIVE. Não há DELETE ou vínculo com workspace/aplicação.

Códigos são imutáveis e reservados inclusive em registros inativos: globais para
segmentos e únicos por segmento para categorias. Categoria só nasce sob segmento
ACTIVE, mas suas transições são independentes; inativar o pai não altera filhas.
`available` na resposta da categoria exige ambos ACTIVE. V66 cria tabelas e
constraints; V67 publica os schemas dos requests. Consulte os contratos, regras,
concorrência e operação na [Segmentation](docs/refinamentos/segmentation/SEGMENTATION.md).

## Parameter — parâmetros dinâmicos

A feature `feature.parameter` gerencia parâmetros consumidos pelas aplicações em
runtime. Sua entidade principal é `ParameterKey`, persistida em `parameter_keys`.
O CRUD OWNER fica em `/api/v1/dynamic-parameters`, com requests/responses próprios,
listagem paginada, atualização versionada e lifecycle idempotente. Os catálogos são
`/api/v1/parameter-key-type` e `/api/v1/parameter-key-status-type`.

Uma chave nasce ativa em DRAFT, sem snapshot publicado. Editar tipo/valor preserva o
snapshot anterior e volta a DRAFT; editar somente descrição mantém o status.
`POST /api/v1/dynamic-parameters/publish` recebe `identifiers` com 1 ou N chaves e retorna
`202 Accepted` sem corpo. O worker confirma cada item em sua própria transação/auditoria,
relê os snapshots confirmados e usa `RedisStore.setAll` em lotes de 500 por padrão.
`workspace.parameter.runtime.batch-size` configura o tamanho positivo dos lotes.
Inativar limpa o snapshot e remove a chave Redis após commit; reativar aguarda publicação.
Exclusão física continua restrita a registros inativos.

O agendamento horário fica no `ParameterKeyPublisherService`, que consulta chaves ativas
com `publishedValue` preenchido em páginas e envia cada página com `RedisStore.setAll`.
Falhas geram somente logs de erro, com identifier, fullKey e publishedValue sanitizado.
O processamento permanece em memória nesta etapa, sem tabelas de solicitações ou retries duráveis.

O envelope Redis `{type,value}`, a chave `<microservice.code>.<key>`, o namespace físico
`dynamic-parameter` e a ausência de TTL compõem o contrato atual. O código de valor `CONFIGURATION`
continua distinguindo valores de expressões; não representa configuração de infraestrutura.
A biblioteca Redis permanece genérica. Migrations V75–V76 adotam os novos nomes de
persistência/schema sem perder dados nem reescrever as migrations anteriores.
Não há aliases das rotas/propriedades antigas, conforme a decisão de refatorar antes do uso.

Consulte o [contrato atual e a refatoração](docs/refinamentos/dynamic-parameter/DYNAMIC-PARAMETER.md).

## Persistência de entidades

Atualizações de entidades em core/feature usam `repository.saveAndFlush(entity)`
antes de montar responses e snapshots. A [revisão de persistência](docs/refinamentos/persistence/SAVE-AND-FLUSH-REVIEW.md)
registra a verificação de todos os módulos e os ajustes em Service Context, Dynamic Bundle,
Resource Sharing e Segmentation. Essa regra também faz parte do [padrão de revisão](docs/REVIEW-STANDARD.md).

## Ponto único de validação

Workspace, Application, Publisher, Environment, Message, Microservice, Feature, FeatureGroup, Segment, Category e ParameterKey validam
o JSON original antes da desserialização, com `@ValidateResourceSchemaRequest`
da biblioteca e normalizadores específicos quando necessários. Campos permitidos,
tipos, formatos e limites pertencem ao schema publicado; tipos inválidos e campos
desconhecidos são preservados para rejeição. Requests tipados traduzem a entrada
para os inputs dos use cases.

Os validators dos use cases verificam regras de negócio, como unicidade, estado
das referências, conflitos de versão e pré-condições da operação. Entidades
protegem suas transições de lifecycle. Constraints e locks completam as garantias
de persistência e concorrência. Os
catálogos usam contratos de validação da `platform-catalog`.

Princípios principais:

- Golden Platform Foundation fornece capabilities; este serviço demonstra padrões de aplicação.
- A application foundation macrozone é distinta da Golden Platform Foundation e não recebe wiring Spring por
  conveniência.
- `platform-crud` não é permitido.
- regras de Workspace permanecem explícitas no serviço.
- comunicação interna entre módulos ocorre por Use Cases públicos.
- Request/Response pertencem à Web; Input/Output pertencem aos Use Cases.
- Repository representa persistência do próprio módulo.
- Integration representa fronteira externa ao módulo.
- Contratos de integração compartilhados terminam em `Port`; implementações que conectam módulos terminam em `Adapter`.
- Regras de validação permanecem como `*Validator` em `usecase/validation`, fora da convenção de adapters.
- capabilities opcionais da Foundation só entram com caso de uso real.
- `account-api` é referência funcional histórica, não base estrutural desta aplicação.

## Catálogos

A migração inicial trouxe 16 CRUDs de catálogo da referência funcional histórica para
a application Foundation. Hoje, 14 módulos concretos permanecem em `foundation.catalog`,
reutilizando `platform-catalog`:

Na estrutura atual da Golden, os catálogos concretos presentes em `foundation.catalog` usam `EnumCatalogService`; o modelo Included/Dynamic permanece disponível na Foundation para catálogos cujo banco governa novos códigos em runtime. A antiga referência a `BaseCatalogService` não representa mais a API pública atual da `platform-catalog`.

A validação de `settings` utiliza
`com.networknt:json-schema-validator:3.0.7`, organizada em
`foundation/schema`. Essa é uma dependência direta consciente do consumidor e deve
ser reavaliada quando a capability Schema for consolidada integralmente.

Não existe dependência de `platform-crud`.

Para catálogos cuja entidade principal termina em `Type`, o módulo em
`foundation.catalog` preserva esse sufixo no package. Exemplos:

```text
ApplicationScopeType -> foundation.catalog.applicationscopetype
WorkspaceType        -> foundation.catalog.workspacetype
SchemaScopeType      -> foundation.catalog.schemascopetype
```

Todos os módulos concretos de catálogo terminam em `type`. Mesmo quando o nome
histórico da entidade não termina em `Type`, o módulo explicita a natureza catalogar.
Exemplo: `OnboardingPhase -> foundation.catalog.onboardingphasetype`.

## Foundation remota

O consumidor usa exclusivamente:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

Com credenciais de leitura:

```bash
export GITHUB_PACKAGES_USERNAME=BrunoBS
export GITHUB_PACKAGES_TOKEN=<token>
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Não é permitido usar checkout ou `mvn install` local da Foundation como evidência de integração.

A documentação da Golden está em [`docs/golden`](docs/golden).
O [índice da documentação](docs/README.md) separa os guias vigentes dos registros históricos.
O roteiro reutilizável para revisar features está em
[`docs/REVIEW-STANDARD.md`](docs/REVIEW-STANDARD.md).

## Estado da referência

O namespace e as coordenadas Maven acima refletem o código atual. ADRs e registros
de checkpoints em `docs/golden` documentam decisões anteriores e podem mostrar
nomes históricos; a árvore vigente é a deste README e do código-fonte.

## Migrations aplicadas

Migrations versionadas já aplicadas são imutáveis, incluindo espaços e formatação.
Não executar formatadores em `src/main/resources/db/migration`. Mudanças de
estrutura ou de dados devem ser adicionadas em uma nova migration. Não desabilitar
a validação do Flyway nem executar repair automaticamente na inicialização.

`PublishedMigrationChecksumTest` protege os 44 scripts do histórico aplicado
anterior à revisão. A [recuperação da verificação local](docs/refinamentos/platform/LOCAL-VERIFY-RECOVERY.md)
registra a restauração dos arquivos anteriores à reformatação de `32ac504f`.

O cache de contextos Spring dos testes fica limitado a oito em
`src/test/resources/spring.properties`, liberando os containers MySQL dos
contextos descartados para que o `mvn verify` completo respeite os recursos locais.

## Refinamento integral das features Golden

[Checkpoints, migrations incrementais, testes e relatório consolidado](docs/refinamentos/features-golden/EXECUCAO.md). As migrations históricas permanecem imutáveis. Contratos antigos não têm aliases.
