# Golden Reference — Foundation Usage

**Fase:** G1  
**Status:** Validado  
**Objetivo:** registrar como a Golden deve consumir a Foundation sem reabrir decisões do checkpoint `FOUNDATION-GOLDEN-V1`.

## 1. Baseline Maven

A nova Golden deverá utilizar:

```xml
<parent>
    <groupId>com.empresa.platform</groupId>
    <artifactId>platform-parent</artifactId>
    <version>1.0.2</version>
    <relativePath/>
</parent>
```

Regras:

- resolução pelo GitHub Packages;
- não depender de checkout local de `platform-build` ou `platform-libraries`;
- não usar `mvn install` local da Foundation como mecanismo oficial;
- preservar Maven Enforcer e dependency convergence fornecidos pelo parent.

### 1.1 Registries remotos

A topologia validada na G1 separa os artefatos pelos repositórios que os produzem:

```text
platform-build
  -> https://maven.pkg.github.com/brunobs/platform-build
  -> platform-parent:1.0.2 / platform-dependencies:1.0.2

platform-libraries
  -> https://maven.pkg.github.com/brunobs/platform-libraries
  -> platform-starter:1.0.1 / platform-test-support:1.0.1 / capabilities 1.0.1
```

O consumer consulta ambos os registries e autentica a leitura com `PLATFORM_PACKAGES_TOKEN`.

Fluxo comprovado:

```text
platform-build
  -> GitHub Packages / platform-build
  -> platform-libraries
  -> GitHub Packages / platform-libraries
  -> account-service
```

## 2. Baseline de runtime

A aplicação deve declarar explicitamente:

```text
platform-starter
```

O starter agrega somente:

- `platform-logging`;
- `platform-messaging`;
- `platform-authorization`.

A aplicação não deve redeclarar esses módulos separadamente sem necessidade concreta de contrato/configuração.

## 3. Dependências de aplicação

A Foundation não substitui starters funcionais do Spring Boot necessários ao serviço.

A Golden poderá declarar explicitamente, conforme o slice aprovado:

- Spring Web;
- Spring Data JPA;
- Validation;
- MySQL runtime;
- ferramenta de migrations aprovada.

Essas são dependências de infraestrutura/aplicação, não novas capabilities da Foundation.

## 4. platform-logging

Responsabilidade Foundation:

- logging estruturado;
- baseline transversal.

Uso Golden:

- consumir pelo starter;
- não criar framework de logging próprio;
- validar startup e correlação/estrutura disponível conforme contrato atual.

## 5. platform-messaging

Responsabilidade Foundation:

- mensagens;
- i18n;
- tratamento padronizado de exceções.

A capability pode operar sem JDBC, utilizando fallback NoOp de repositório de mensagens.

Uso Golden:

- consumir pelo starter;
- usar exceções/contratos corporativos para erros de validação/not-found/conflitos quando apropriado;
- não ativar JDBC/Redis de messaging sem caso real;
- registrar mensagens específicas de Account sem duplicar o mecanismo transversal.

## 6. platform-authorization

Responsabilidade Foundation:

- autorização;
- contexto do usuário;
- níveis de autorização.

APIs públicas confirmadas na branch da Foundation:

- `@AuthorizationRequired`;
- `AuthorizationLevel` com `OPEN`, `DEV`, `TST`, `ADM`, `OWNER`;
- `UserContext`;
- `UserSession`;
- `UserSession.isOwner()`;
- `UserSession.hasAuthorizer(...)`;
- `@ResourceVisibility` existe tecnicamente.

Uso Golden:

- consumir pelo starter;
- aplicar autorização de endpoint onde houver contrato aprovado;
- manter regras de acesso dependentes de Account explícitas no caso de uso;
- não tratar `ResourceVisibility` como obrigatório apenas porque existe na library.

### Pendência

A matriz exata de nível por endpoint e o uso de `ResourceVisibility` serão decididos antes de G4.

## 7. platform-audit

Capability opcional e explícita.

APIs públicas confirmadas:

- `@Auditable`;
- `@AuditField`;
- `AuditFieldSource`;
- sources `PATH`, `BODY`, `RESPONSE`, `HEADER`.

Uso Golden:

- declarar `platform-audit` explicitamente;
- auditar mutações reais do domínio;
- preferir extração de resource id coerente com o caso de uso;
- não habilitar Redis fallback apenas para demonstrar a capability.

Casos candidatos:

- create;
- update;
- deactivate;
- restore;
- conclusão de onboarding.

## 8. platform-tagging

Capability opcional e explícita.

API pública confirmada:

```text
TagManager
TagNormalizer
TagOwnerType
TagOriginType
```

`TagManager` oferece, entre outros:

- `reconcile(...)`;
- `findAll(...)`;
- `findManual(...)`;
- `findSystem(...)`;
- `findManualByOwners(...)`;
- `deleteAll(...)`.

`TagNormalizer.normalize(...)`:

- trim;
- vazio → null;
- lowercase;
- whitespace → `-`.

Uso Golden:

- declarar `platform-tagging` explicitamente;
- definir owner type `ACCOUNT`;
- usar identifier estável como owner id;
- reconciliar tags manuais e de sistema;
- preservar distinção MANUAL/SYSTEM;
- não reimplementar normalização já pertencente à capability.

## 9. platform-catalog

Capability opcional e explícita para catálogos persistidos e gerenciados.

Contratos próprios incluem serviços de catálogo:

- `BaseCatalogService`;
- `DynamicCatalogService`;
- `EnumCatalogService`.

A capability possui lifecycle, restore, ordering, name lookup, filtros e validação próprios de catálogo e não depende de CRUD genérico.

### Uso Golden recomendado

Usar somente para conceitos que realmente sejam catálogos.

Candidato aprovado para avaliação:

- AccountType.

Candidato condicionado:

- OnboardingPhase, se o fluxo for configurável/administrável.

Não usar automaticamente para:

- estado ACTIVE/INACTIVE de Account;
- origem de tags;
- qualquer enum estático apenas para “demonstrar” catalog.

### Pendência

Decidir se Account lifecycle é:

1. estado explícito do domínio; ou
2. catálogo persistido realmente justificável.

A opção 1 é a preferência arquitetural da análise G0 enquanto não houver requisito de administrabilidade.

## 10. platform-test-support

Dependência somente de teste.

Capabilities confirmadas:

- `@PlatformUnitTest`;
- `@PlatformIntegrationTest`;
- `@PlatformArchitectureTest`;
- `@WithMySql`;
- `@WithKafka`;
- `@WithMockAuthorization`;
- suporte Testcontainers;
- MySQL;
- Kafka;
- WireMock;
- RestAssured;
- builders/factories/scenarios;
- validação arquitetural opt-in.

Uso Golden:

- aproveitar infraestrutura reutilizável;
- consumir `platform-test-support` sem trazer JDBC/MySQL/Kafka/Testcontainers quando essas capacidades não forem declaradas pela aplicação;
- declarar explicitamente as dependências necessárias ao usar `@WithMySql` ou `@WithKafka`;
- não obrigar todos os testes a usar annotations da plataforma;
- complementar `@PlatformArchitectureTest` com regras ArchUnit específicas da Golden quando necessário.

Contrato validado em G1: infraestrutura de teste pesada é opt-in. Um serviço sem banco inicia com `@PlatformIntegrationTest` sem `DataSourceAutoConfiguration` exclude.

Observação: `@PlatformArchitectureTest` atual é um guard opt-in relacionado a overrides de tipos base da plataforma. Ele não substitui regras próprias como “controller não acessa repository”.

## 11. platform-crud

Situação:

```text
REMOVIDO DA FOUNDATION
```

Regra Golden:

- nenhuma dependência;
- nenhum import;
- nenhuma cópia de `BaseCrud*`;
- nenhuma abstração equivalente com outro nome.

Deve existir teste arquitetural/dependência que impeça regressão.

## 12. Mapa Foundation × Golden

| Necessidade | Responsável | Uso na Golden |
|---|---|---|
| Java/Spring/Maven baseline | platform-build | consumir parent |
| Maven Enforcer / convergence | platform-build | herdado |
| logging transversal | platform-logging | starter |
| mensagens/exceções | platform-messaging | starter |
| autorização/contexto | platform-authorization | starter |
| auditoria | platform-audit | dependência explícita |
| catálogo administrável | platform-catalog | dependência explícita quando necessário |
| tagging | platform-tagging | dependência explícita |
| infra reutilizável de testes | platform-test-support | test scope |
| regras de Account | Golden | explícitas no domínio/aplicação |
| controller/service/repository pattern | Golden | demonstrado, não library |
| migrations do serviço | Golden | infraestrutura da aplicação |
| CRUD genérico | nenhum | proibido como Foundation/padrão |

## 13. Critério de integração G1 — validado

A G1 foi concluída após validar:

```text
novo repositório Golden
→ resolve platform-parent:1.0.2 remotamente
→ resolve platform-starter
→ compila
→ inicia aplicação mínima
→ executa testes mínimos
→ mvn clean verify
→ CI verde
```

Sem checkout ou `mvn install` local da Foundation.

### Evidência de fechamento G1

- `platform-build` Publish #12 — run `35535997475`: `BUILD SUCCESS` para `1.0.2`;
- `platform-libraries` Verify #50 — run `35536158485`: `BUILD SUCCESS` para `1.0.1`;
- `platform-libraries` Publish #3 — run `35536158484`: `BUILD SUCCESS` para `1.0.1`;
- `account-service` Verify #11 — run `35536464540`: `BUILD SUCCESS` sem exclusão de DataSource;
- resolução remota de parent, starter e test-support;
- Maven Enforcer e dependency convergence verdes;
- infraestrutura JDBC/Kafka/Testcontainers opt-in;
- nenhum `mvn install` local entre repositórios.
