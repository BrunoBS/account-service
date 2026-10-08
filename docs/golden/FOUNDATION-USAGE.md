# Golden Reference — Foundation Usage

**Fase:** G1 revalidada para a Foundation consolidada; atualizado após G4 para o domínio Workspace  
**Status:** baseline de consumo atualizado  
**Objetivo:** registrar como a Golden Reference consome a Foundation oficial sem reabrir decisões arquiteturais da
Foundation.

## 1. Fonte oficial

A Foundation oficial está no repositório:

```text
BrunoBS/platform-libraries
```

O consumidor não utiliza outro repositório Maven da Foundation.

Registry oficial:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

Autenticação de leitura:

```text
GITHUB_PACKAGES_USERNAME
PLATFORM_PACKAGES_TOKEN
```

Não usar checkout local nem `mvn install` da Foundation como mecanismo de integração.

## 2. Namespace oficial

Coordenadas Maven e packages Java da Foundation usam:

```text
br.com.portalmanager.platform.library
```

A Golden não deve introduzir aliases ou compatibilidade com o namespace provisório anterior.

## 3. Parent de build

O serviço usa:

```xml
<parent>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-parent</artifactId>
    <version>1.0.0</version>
    <relativePath/>
</parent>
```

Responsabilidades do parent:

- Java 25;
- Maven >= 3.9.9;
- plugins de build;
- Surefire/Failsafe;
- Maven Enforcer;
- dependency convergence;
- JaCoCo;
- import de `platform-dependencies:1.0.0`.

O parent não gerencia versões das capabilities.

## 4. BOM das capabilities

A Golden importa explicitamente:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>br.com.portalmanager.platform.library</groupId>
            <artifactId>platform-libraries-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

O `platform-libraries-bom` é a fonte de versões das capabilities reutilizáveis.

O `platform-dependencies` continua exclusivamente responsável por dependências tecnológicas externas e não substitui o
BOM das capabilities.

## 5. Baseline de runtime

A Golden declara:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-starter</artifactId>
</dependency>
```

O starter agrega o baseline transversal aprovado:

- `platform-observability`;
- `platform-messaging`;
- `platform-authorization`.

As capabilities opcionais continuam explícitas e entram somente quando houver caso real.

## 6. platform-testing

A Golden consome somente os módulos de teste que usa, todos em escopo `test`:

- `platform-testing-http`: fornece `@PlatformIntegrationTest` e os utilitários de teste HTTP;
- `platform-testing-database`: fornece `@WithMySql` e a fixture MySQL com Testcontainers;
- `platform-testing-authorization`: fornece `@WithMockAuthorization` e `AuthorizationMock`.

O `platform-testing-core` chega transitivamente por esses módulos. Não é declarado diretamente porque a Golden não usa
nenhuma API exclusiva do Core.

As dependências Testcontainers e o driver MySQL necessários à fixture vêm pelo módulo de database. Não duplicar
`spring-boot-testcontainers` ou `org.testcontainers:testcontainers-mysql` no POM da aplicação.

Os imports atuais são:

```java
br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest
br.com.portalmanager.platform.library.testing.database.annotation.WithMySql
br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization
```

## 7. Capabilities opcionais

### platform-audit

Adicionar somente quando as mutações reais de Workspace forem implementadas.

### platform-catalog

Dependência explícita a partir da migração dos catálogos legados:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-catalog</artifactId>
</dependency>
```

A Golden usa a capability como infraestrutura de CRUD de catálogo, sem copiar sua
implementação e sem reintroduzir `platform-crud`.

Os catálogos concretos pertencem à **application foundation macrozone** em
`foundation/catalog`.

### platform-tagging

Usar para tags manuais e de sistema de Workspace, sem reimplementar normalização transversal.

### platform-authorization

Já chega pelo starter. Regras dependentes de Workspace permanecem explícitas no domínio/aplicação.

### platform-messaging

Já chega pelo starter. Erros e mensagens específicas de Workspace usam os contratos da Foundation sem duplicar mecanismo
transversal.

Na G3, a aplicação possui DataSource próprio de Workspace. Como a auto-configuração JDBC de Messaging é ativada na
presença de `JdbcTemplate`, a Golden declara explicitamente um `NoOpApiMessageRepository` para indicar que **o banco de
Workspace não é o catálogo corporativo de mensagens**.

Após o review pós-G4, esse wiring reside no **composition root** como
`br.com.itau.portalmanager.workspace.WorkspaceMessagingConfiguration`. Ele não pertence
à application foundation macrozone. A decisão está registrada no ADR-006.

Com isso, a resolução usa os bundles classpath, incluindo:

```text
META-INF/platform-messages/workspace-service_pt_BR.properties
```

Essa é uma decisão explícita de integração do consumidor; não cria uma implementação paralela de messaging.

### platform-observability

Já chega pelo starter. Não criar framework de logging paralelo na Golden.

## 8. platform-crud

Situação:

```text
REMOVIDO
```

Regras da Golden:

- nenhuma dependência `platform-crud`;
- nenhum import;
- nenhuma cópia de `BaseCrud*`;
- nenhuma abstração genérica equivalente introduzida para substituir o CRUD removido.

## 9. Dependências próprias da aplicação

A Foundation não substitui dependências funcionais da aplicação.

Conforme o slice aprovado, a Golden poderá declarar explicitamente:

- Spring Web;
- Spring Data JPA;
- Validation;
- MySQL;
- ferramenta de migrations aprovada;
- dependências de teste específicas exigidas por persistência;
- `com.networknt:json-schema-validator`, necessária para validar definições de schema da aplicação; a versão
  `3.0.7` é gerenciada por `platform-dependencies`.

Essas dependências entram porque o serviço precisa delas, não por transitividade implícita da Foundation.

## 10. Configuração Maven do consumidor

O settings contém um único server/profile/repository para:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

O consumidor não consulta registry legado da Foundation.

## 11. Validação oficial

A prova de integração deve executar:

```text
checkout limpo
→ repository Maven local vazio/isolado
→ resolve platform-parent:1.0.0
→ resolve platform-libraries-bom:1.0.0
→ resolve platform-starter:1.0.0
→ resolve platform-testing-http:1.0.0
→ resolve platform-testing-database:1.0.0
→ resolve platform-testing-authorization:1.0.0
→ compila
→ inicia contexto Spring Boot
→ Maven Enforcer
→ dependency convergence
→ mvn clean verify
→ BUILD SUCCESS
```

Sem checkout local e sem `mvn install` da Foundation.

## 12. Regra para problemas encontrados

Durante a Golden Reference:

```text
problema
  ↓
é integração/uso do consumidor?
  ├─ sim → corrigir na Golden
  └─ não
      ↓
há defeito reproduzível da Foundation?
      ├─ não → manter explícito na Golden
      └─ sim → registrar evidência antes de propor alteração
```

A Foundation não deve ser alterada nesta etapa sem evidência técnica concreta e decisão explícita.

## 13. Uso efetivo na G4

### Authorization

Continua chegando pelo `platform-starter`.

A Golden usa:

- `@AuthorizationRequired` para policy OPEN/DEV/ADM;
- `@ResourceVisibility` nas leituras;
- `AuthorizableResource` em `WorkspaceOutput`;
- `@WithMockAuthorization` para testes HTTP reais.

### Tagging

Dependência explícita:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-tagging</artifactId>
</dependency>
```

A capability fornece entidade/repository/manager, mas a Golden mantém a evolução física do próprio banco. Por isso a
tabela `tags` é criada pela migration `V2__create_tags.sql`.

### Audit

Dependência explícita:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform.library</groupId>
    <artifactId>platform-audit</artifactId>
</dependency>
```

Configuração de produção:

```text
AUDIT_ENABLED
AUDIT_SERVICE_URL
```

O profile de teste geral desabilita audit; `WorkspaceAuditIT` habilita a auto-configuração e injeta um `AuditPublisher`
capturável para provar os eventos sem serviço externo.

### Catalog

`platform-catalog` passou a ser capability efetivamente consumida.

Foram migrados 16 catálogos do `account-api`:

- 12 usando `EnumCatalogService`;
- `FeatureScopeType` usando `DynamicCatalogService`;
- `FeatureType`, `SchemaType` e `OnboardingPhase` usando
  `BaseCatalogService`.

A estrutura da aplicação é:

```text
foundation/catalog
└── <catalogo>
    ├── domain
    ├── repository
    └── usecase

Quando a entidade principal é `*Type`, o nome do módulo inclui `type`:

```text
ApplicationScopeType -> applicationscopetype
FeatureType          -> featuretype
SchemaType           -> schematype
WorkspaceType        -> workspacetype
```

Todo catálogo concreto termina em `type`. Assim, mesmo a entidade histórica
`OnboardingPhase` pertence ao módulo `onboardingphasetype`.

Packages técnicos compartilhados, como `support`, permanecem fora dessa regra.

entrypoint/web/catalog
└── <catalogo>

```

`AccountType` foi migrado conceitualmente para `WorkspaceType`.

Os catálogos não foram movidos para `platform-libraries`; a lib fornece a capability,
enquanto a Golden declara os catálogos concretos.

### Messaging e Logging

Permanecem pelo starter e já estão exercitados:

- messaging resolve erros globais/Workspace pelo provider classpath, com `NoOpApiMessageRepository` explícito para o banco de Workspace;
- logging produz saída estruturada e recebe MDC preenchido pelo fluxo de authorization.

Nenhum framework paralelo foi criado.


## 14. Terminologia de Foundation após o review pós-G4

Para evitar ambiguidade:

- **Golden Platform Foundation** = `platform-build + platform-libraries`;
- **application foundation macrozone** = package
  `br.com.itau.portalmanager.workspace.foundation`.

A segunda não é um depósito de configurações Spring transversais. Wiring técnico do
consumidor fica no composition root quando não pertence a uma macrozona funcional.

A busca reversa de tags continua encapsulada em
`core.workspace.integration.tagging.WorkspaceTagSearchIntegration`. Como a API pública
de `TagManager` não oferece essa consulta, a integração conhece provisoriamente a tabela
`tags`. Isso é dívida documentada do consumidor e não um gap aberto que autorize
alteração da Golden Platform Foundation nesta atividade.


## 15. Catalog -> Schema na Application Foundation

A validação dos `settings` dos catálogos usa a capability publicada `platform-schema-validation`:

- os serviços de catálogo recebem `SchemaValidator`;
- o serviço informa o código do recurso de schema;
- a capability resolve e valida o payload usando o schema publicado.

O contrato compartilhado não deve ser duplicado em adapters locais como `CatalogSettingsValidator` ou
`SchemaSettingsValidator`.

A aplicação mantém responsabilidades próprias que não foram transferidas para a capability:

- `SchemaResolver` e `SchemaResolutionPort` para resolução local dos schemas persistidos;
- `SchemaDefinitionValidator` para validar definições armazenadas;
- `JsonSchemaValidator` e `SchemaJsonValidator` para as operações locais de validação necessárias.

Essas classes usam diretamente `com.networknt:json-schema-validator`; a dependência é declarada no POM do serviço e sua
versão `3.0.7` é gerenciada por `platform-dependencies`.