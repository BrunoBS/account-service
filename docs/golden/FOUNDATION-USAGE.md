# Golden Reference — Foundation Usage

**Fase:** G1 revalidada para a Foundation consolidada  
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
br.com.portalmanager.core
```

A Golden não deve introduzir aliases ou compatibilidade com o namespace provisório anterior.

## 3. Parent de build

O serviço usa:

```xml
<parent>
    <groupId>br.com.portalmanager.core</groupId>
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
            <groupId>br.com.portalmanager.core</groupId>
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
    <groupId>br.com.portalmanager.core</groupId>
    <artifactId>platform-starter</artifactId>
</dependency>
```

O starter agrega o baseline transversal aprovado:

- `platform-logging`;
- `platform-messaging`;
- `platform-authorization`.

As capabilities opcionais continuam explícitas e entram somente quando houver caso real.

## 6. platform-test-support

Dependência de teste:

```xml
<dependency>
    <groupId>br.com.portalmanager.core</groupId>
    <artifactId>platform-test-support</artifactId>
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

Infraestrutura pesada permanece opt-in. Consumir `platform-test-support` sozinho não deve forçar JDBC, MySQL, Kafka ou Testcontainers no classpath do consumidor.

Quando a Golden realmente usar `@WithMySql` ou `@WithKafka`, deve declarar explicitamente as dependências de teste necessárias.

## 7. Capabilities opcionais

### platform-audit

Adicionar somente quando as mutações reais de Account forem implementadas.

### platform-catalog

Usar somente para conceitos realmente administráveis. `AccountType` permanece o principal candidato do slice.

### platform-tagging

Usar para tags manuais e de sistema de Account, sem reimplementar normalização transversal.

### platform-authorization

Já chega pelo starter. Regras dependentes de Account permanecem explícitas no domínio/aplicação.

### platform-messaging

Já chega pelo starter. Erros e mensagens específicas de Account usam os contratos da Foundation sem duplicar mecanismo transversal.

### platform-logging

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
- dependências de teste específicas exigidas por persistência.

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
→ resolve platform-test-support:1.0.0
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
