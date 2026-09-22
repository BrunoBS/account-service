# workspace-service

Serviço de referência para o domínio de Workspace da Golden Platform.

Este repositório implementa a fase `GOLDEN-REFERENCE-V1` sobre o checkpoint `FOUNDATION-GOLDEN-V1`.

## Identidade

- Maven: `br.com.portalmanager:workspace-service:0.1.0-SNAPSHOT`
- Java package root: `br.com.itau.portalmanager.workspace`
- hospedagem atual: `BrunoBS/account-service` (nome histórico do repositório; não representa o domínio ativo)

## Baseline

- Java 25
- Spring Boot 4.1.1
- Maven >= 3.9.9
- `br.com.portalmanager.platform:platform-parent:1.0.0`
- `br.com.portalmanager.platform:platform-libraries-bom:1.0.0`
- `br.com.portalmanager.platform:platform-starter:1.0.0`
- `br.com.portalmanager.platform:platform-testing:1.0.0`
- `br.com.portalmanager.platform:platform-catalog:1.0.0`

## Arquitetura

A aplicação utiliza as macrozonas:

```text
br.com.itau.portalmanager.workspace
├── WorkspaceServiceApplication            # composition root
├── WorkspaceMessagingConfiguration        # wiring técnico do consumidor
├── foundation
│   ├── catalog
│   │   ├── workspacetype
│   │   │   ├── domain
│   │   │   ├── repository
│   │   │   └── usecase
│   │   ├── lifecycletype
│   │   │   ├── domain
│   │   │   ├── repository
│   │   │   └── usecase
│   │   └── <catalog-type>
│   │       ├── domain
│   │       ├── repository
│   │       └── usecase
│   └── schema
│       ├── domain
│       └── usecase
├── core
├── feature
└── entrypoint
    └── web
        └── catalog
```

Pacotes vazios não são criados apenas para completar a árvore.

Princípios principais:

- Golden Platform Foundation fornece capabilities; este serviço demonstra padrões de aplicação.
- A application foundation macrozone é distinta da Golden Platform Foundation e não recebe wiring Spring por conveniência.
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

- 12 `EnumCatalogService`;
- 1 `DynamicCatalogService`;
- 3 `BaseCatalogService` para contratos avançados.

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

Packages técnicos compartilhados, como `foundation.catalog.support`, não representam
catálogos concretos e não seguem essa convenção.

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


## Pendências antes do checkpoint GOLDEN-REFERENCE-V1

- definir explicitamente o `groupId` Maven oficial para serviços Golden; a coordenada
  atual permanece `br.com.portalmanager:workspace-service` até decisão;
- manter visível a dívida da busca reversa de tags baseada no contrato físico da tabela
  `tags`.

A adequação pós-G4 permanece como baseline. A migração dos catálogos foi validada no
Verify #110 (run `35674577937`), head
`3cd68f9aa0470b4689b8d360100747d13a21de31`, com 45 testes verdes.
