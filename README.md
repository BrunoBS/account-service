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
│   │   ├── <catalog-type>/{domain,repository,usecase}
│   │   └── integration/CatalogSettingsValidator
│   ├── integration/WorkspaceReferenceResolver
│   └── schema/{domain,repository,usecase/{model,operations,validation}}
├── core/workspace/{domain,repository,integration,usecase/{model,operations,validation}}
├── feature
│   ├── message/{domain,repository,usecase/{model,operations,validation}}
│   └── platform/{domain,repository,usecase/{model,operations,validation}}
└── entrypoint/web/{catalog,workspace,message,platform,schema}
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

### Ponto único de validação

Para Workspace, Message, Platform e Schema, as decisões de validade pertencem a
`usecase/validation`: formato e obrigatoriedade dos dados, duplicidade, lifecycle,
relações e pré-condições de transição. As operações consultam repositórios quando
necessário, passam os dados ao validador e só então modificam as entidades.
Entidades executam as transições sem repetir essas regras. Toda escrita deve passar
pelo use case; as restrições do banco permanecem como proteção de integridade e de
concorrência. Os catálogos ficam fora deste padrão por enquanto: usam contratos de
validação próprios da `platform-catalog`.

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

O adaptador compartilhado `foundation.catalog.integration.CatalogSettingsValidator`
não representa um catálogo concreto e não segue essa convenção.

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

## Estado da referência

O namespace e as coordenadas Maven acima refletem o código atual. ADRs e registros
de checkpoints em `docs/golden` documentam decisões anteriores e podem mostrar
nomes históricos; a árvore vigente é a deste README e do código-fonte.
