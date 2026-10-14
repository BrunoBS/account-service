# workspace-service

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

## Arquitetura

A aplicação utiliza as macrozonas:

```text
br.com.portalmanager.platform.workspace
├── WorkspaceServiceApplication
├── foundation
│   ├── catalog
│   │   └── <catalog-type>/{domain,repository,usecase}
│   ├── integration/*Port
│   └── schema/{domain,repository,integration/*{Port,Adapter},usecase/{model,operations,validation}}
├── core/workspace/{domain,repository,integration,usecase/{model,operations,validation}}
├── feature
│   ├── message/{domain,repository,usecase/{model,operations,validation}}
│   ├── onboarding/{domain,repository,integration,usecase/{model,operations,validation}}
│   ├── platform/{domain,repository,usecase/{model,operations,validation}}
│   └── shared/{domain,repository,integration,usecase/{model,operations,validation}}
└── entrypoint/web/{catalog,workspace,onboarding,message,platform,schema}
```

Pacotes vazios não são criados apenas para completar a árvore.

O cadastro administrativo de microserviços usa `feature/platform` e
`/api/v1/platform/microservices`. A migração V19 renomeia as referências de
Feature e Message e remove os catálogos de linguagem e infraestrutura. Consulte
[`MICROSERVICE-MIGRATION-V19.md`](docs/refinamentos/foundation/MICROSERVICE-MIGRATION-V19.md)
para os contratos de atualização.

### Organização interna do domínio

Módulos com mais de uma responsabilidade de domínio agrupam suas classes em
`domain/<responsabilidade>`. Entidades dependentes e objetos de valor permanecem
junto ao agregado ao qual pertencem; ter várias classes não exige criar subpacotes.

| Módulo              | Subpacotes de domínio                                      |
| ------------------- | ---------------------------------------------------------- |
| `feature.platform`  | `feature`, `microservice`, `featurecontext`                |
| `feature.shared`    | `contract`, `participation` (inclui os mapeamentos)        |
| `core.environment`  | `environment`, `environmenttype` (inclui compatibilidades) |
| `foundation.schema` | `schema`, `version`, `configuration`                       |

Módulos com um único agregado mantêm as classes diretamente em `domain`.
As chaves de mensagens compartilhadas entre responsabilidades permanecem na raiz
`<modulo>.domain`.

### Chaves de mensagens

As classes `*MessageKeys` devem ficar em `<modulo>.domain`, junto ao domínio que
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
As listagens de `/api/v1/messages` e `/api/v1/messages/{messageIdentifier}/translations`
retornam `content` e `page`. Create aceita traduções inline; update exige `version`
e edita somente os atributos da mensagem. Traduções têm endpoints e versões próprios.

DELETE remove fisicamente uma mensagem INACTIVE e todas as suas traduções.
DELETE isolado de tradução exige que ela esteja INACTIVE. As exclusões são auditadas
como PURGE, com snapshots preservados antes da remoção e rollback transacional.

V58 publica os schemas; V59 preserva registros antigos em quarentena como INACTIVE
e restringe o lifecycle desses dois recursos a ACTIVE/INACTIVE. O Flyway habilitado
aplica as migrations na inicialização do serviço no banco configurado.
Consulte o [guia de integração](docs/refinamentos/message/MESSAGE-INTEGRATION.md)
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
[análise detalhada do fluxo](docs/refinamentos/onboarding/ONBOARDING-CODE-REVIEW.md)
para os ajustes implementados de arquitetura, nomes, mensagens e validação.

## Platform — Microservices

A revisão do fluxo de microservices está documentada em
[Microservices](docs/refinamentos/platform/MICROSERVICE-CODE-REVIEW.md), incluindo
as seis rotas atuais, autorização OWNER, schema HTTP, settings configuráveis, lifecycle,
auditoria e concorrência dos vínculos com features/messages. A V62 publica o
contrato `PLATFORM/microservice`; settings continuam usando `MICROSERVICE/{code}`.

A listagem retorna página com filtro central de lifecycle. Update exige `version`
atual e código imutável. Microservices podem ser ativados e inativados, preservando
features e mensagens; não há endpoint de exclusão.

## Platform — Features

A revisão anterior das dez rotas está documentada em
[Features](docs/refinamentos/platform/FEATURE-CODE-REVIEW.md): autorização OWNER,
versão obrigatória no update, código imutável, lifecycle, settings, auditoria e
proteção concorrente dos vínculos com contexts e contratos Shared. A V63 publica
`PLATFORM/feature`; settings continuam usando `FEATURE/{code}`.

As listagens de features e de seus contexts retornam páginas com filtro central
de lifecycle. Features globais podem ser ativadas e inativadas, mantendo suas
referências; não há endpoint de exclusão. Novos vínculos exigem feature,
microservice e context ativos; remover um vínculo continua permitido após inativação.

## Platform — Contexts

A [revisão de contexts](docs/refinamentos/platform/CONTEXT-CODE-REVIEW.md) cobre
as sete rotas, autorização OWNER, paginação, versão do cliente, lifecycle,
vínculos, auditoria e concorrência. A V64 publica `PLATFORM/context`.

Update exige `version` e código imutável; a listagem retorna Page. Inativação
preserva vínculos, e DELETE executa purge físico somente quando não há vínculos,
inclusive com features inativas. Alterações individuais de vínculos também avançam a versão do
context; retries sem mudança real não geram evento ou incremento de versão.

A [revisão geral anterior de Platform e suas 24 rotas](docs/refinamentos/platform/PLATFORM-ENDPOINTS-REVIEW.md)
registra a finalidade e a necessidade de cada endpoint, a convivência entre
alterações individuais/em lote e a política de retenção. Vínculos de uma feature
inativa/em quarentena continuam consultáveis por OWNER para possibilitar limpeza.

## Platform — separação dos quatro fluxos

A [revisão geral de fechamento](docs/refinamentos/platform/PLATFORM-FINAL-REVIEW.md)
registra os últimos ajustes, a regressão e as condições de entrega da biblioteca.

A [refatoração de Platform](docs/refinamentos/platform/PLATFORM-STRUCTURE-REFACTOR.md)
separa microservice, feature, context e featurecontext, com controllers, fachadas
e pacotes próprios. O cadastro de contexto usa nomes `Context*`.
`FeatureContextController` e sua fachada têm somente `associate` e `remove`.
Os dois controllers usam `findAll` com filtro opcional de vínculo. A busca de features
usa somente `contextIdentifier`, com `findAll` em controller, fachada, query service
e repositório. A busca inversa também usa `findAll` em todas as camadas, com
`featureIdentifier` opcional. Os dois filtros normalizam espaços e verificam a
existência do recurso informado.

POST/PUT de contexto recebem somente seus atributos. Para associar ou remover um
vínculo, usar POST/DELETE em
`/api/v1/platform/contexts/{contextIdentifier}/features/{featureIdentifier}`, sem
body. O DELETE de vínculo retorna HTTP 204 sem corpo, mantendo o retorno interno
para auditoria. Não há replace ou aliases de escrita a partir de features.
A consulta de features usa
`GET /api/v1/platform/features?contextIdentifier=<UUID>`. A consulta inversa de
contextos usa `GET /api/v1/platform/contexts?featureIdentifier=<UUID>`.
Ambas aplicam lifecycle centralizado.
A V65 permanece imutável; seu schema histórico de body de vínculo não tem mais
consumidor HTTP. A
[política atual de exclusão](docs/refinamentos/platform/PLATFORM-LIFECYCLE-REVIEW.md)
remove DELETE de microservice/feature e implementa purge de contexto sem vínculos.
O vínculo usa o estado da feature global: uma feature inativa fica fora da listagem
padrão do contexto, mantendo a relação até sua remoção explícita. A reativação
restaura sua presença nos contextos em que continua vinculada. Consultas OWNER com
`lifecycle=INACTIVE` permitem localizar os vínculos para limpeza.

## Ponto único de validação

Workspace, Application, Publisher, Environment, Message, Microservice, Feature e Context validam
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

A Golden migrou os 16 CRUDs de catálogo da referência funcional histórica para a
application Foundation, reutilizando `platform-catalog`:

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
SchemaType           -> foundation.catalog.schematype
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
