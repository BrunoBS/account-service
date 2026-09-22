# Golden Reference — Foundation Usage

**Fase:** G1 revalidada para a Foundation consolidada; atualizado após G4 para o domínio Workspace  
**Status:** baseline de consumo atualizado  
**Objetivo:** registrar como a Golden Reference consome a Foundation oficial sem reabrir decisões arquiteturais da Foundation.

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
br.com.portalmanager.platform
```

A Golden não deve introduzir aliases ou compatibilidade com o namespace provisório anterior.

## 3. Parent de build

O serviço usa:

```xml
<parent>
    <groupId>br.com.portalmanager.platform</groupId>
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
            <groupId>br.com.portalmanager.platform</groupId>
            <artifactId>platform-libraries-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

O `platform-libraries-bom` é a fonte de versões das capabilities reutilizáveis.

O `platform-dependencies` continua exclusivamente responsável por dependências tecnológicas externas e não substitui o BOM das capabilities.

## 5. Baseline de runtime

A Golden declara:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-starter</artifactId>
</dependency>
```

O starter agrega o baseline transversal aprovado:

- `platform-observability`;
- `platform-messaging`;
- `platform-authorization`.

As capabilities opcionais continuam explícitas e entram somente quando houver caso real.

## 6. platform-testing

Dependência de teste:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-testing</artifactId>
    <scope>test</scope>
</dependency>
```

APIs relevantes incluem:

- `@PlatformUnitTest`;
- `@PlatformIntegrationTest`;
- `@PlatformArchitectureTest`;
- `@WithMySql`;
- `@WithKafka`;
- `@WithMockAuthorization`.

Infraestrutura pesada permanece opt-in. Consumir `platform-testing` sozinho não deve forçar JDBC, MySQL, Kafka ou Testcontainers no classpath do consumidor.

Quando a Golden realmente usar `@WithMySql` ou `@WithKafka`, deve declarar explicitamente as dependências de teste necessárias.

Para MySQL com o baseline Testcontainers 2.x gerenciado pelo Spring Boot 4.1.1, a Golden declara:

```text
spring-boot-testcontainers
org.testcontainers:testcontainers-mysql
```

A coordenada antiga `org.testcontainers:mysql` não deve ser usada no baseline atual.

## 7. Capabilities opcionais

### platform-audit

Adicionar somente quando as mutações reais de Workspace forem implementadas.

### platform-catalog

Dependência explícita a partir da migração dos catálogos legados:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
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

Já chega pelo starter. Erros e mensagens específicas de Workspace usam os contratos da Foundation sem duplicar mecanismo transversal.

Na G3, a aplicação possui DataSource próprio de Workspace. Como a auto-configuração JDBC de Messaging é ativada na presença de `JdbcTemplate`, a Golden declara explicitamente um `NoOpApiMessageRepository` para indicar que **o banco de Workspace não é o catálogo corporativo de mensagens**.

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
- `com.networknt:json-schema-validator:3.0.7`, necessária para preservar a validação
  de `settings` dos catálogos migrados.

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
→ resolve platform-testing:1.0.0
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
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-tagging</artifactId>
</dependency>
```

A capability fornece entidade/repository/manager, mas a Golden mantém a evolução física do próprio banco. Por isso a tabela `tags` é criada pela migration `V2__create_tags.sql`.

### Audit

Dependência explícita:

```xml
<dependency>
    <groupId>br.com.portalmanager.platform</groupId>
    <artifactId>platform-audit</artifactId>
</dependency>
```

Configuração de produção:

```text
AUDIT_ENABLED
AUDIT_SERVICE_URL
```

O profile de teste geral desabilita audit; `WorkspaceAuditIT` habilita a auto-configuração e injeta um `AuditPublisher` capturável para provar os eventos sem serviço externo.

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

input/web/catalog
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

A migração dos catálogos trouxe uma dependência funcional do legado: validação de
`settings` com JSON Schema Draft 2020-12.

A implementação foi organizada conforme a direção já definida na arquitetura:

```text
foundation.catalog
        ↓
foundation.schema
```

Componentes locais:

```text
foundation.schema.domain.SchemaDefaults
foundation.schema.usecase.SchemaValidator
foundation.catalog.support.CatalogSchemaValidationSupport
```

`SchemaValidator` não depende de Catalog.

A dependência externa utilizada é:

```text
com.networknt:json-schema-validator:3.0.7
```

Ela permanece explicitamente no consumidor. Isso **não** constitui alteração da Golden
Platform Foundation (`platform-build + platform-libraries`) e não autoriza promover a
dependência para a Foundation da plataforma sem decisão própria.

O review final da migração está documentado em
`REVIEW-MIGRACAO-CATALOGOS-FOUNDATION.md`.
