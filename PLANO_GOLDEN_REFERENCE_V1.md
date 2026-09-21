# Plano de Execução — GOLDEN-REFERENCE-V1

**Status:** Aprovado para execução  
**Checkpoint anterior:** `FOUNDATION-GOLDEN-V1`  
**Fase atual:** Golden Reference  
**Data de início:** 2026-09-20

## 1. Objetivo

Construir uma aplicação real de referência sobre a Foundation estabilizada da Golden Platform e provar, em código executável, como novos serviços devem utilizar:

- Foundation consolidada no repositório `BrunoBS/platform-libraries`;
- `platform-libraries`;
- os padrões arquiteturais aprovados;
- capacidades transversais somente quando houver caso real de uso.

A Golden Reference não será um framework genérico, uma continuação do `platform-crud`, um protótipo de Scaffold nem uma cópia da `account-api`.

Princípio orientador:

> Foundation fornece capacidades. Golden demonstra padrões. Scaffold automatiza padrões comprovados.

## 2. Fontes de autoridade

A execução deve obedecer, nesta ordem, às fontes vigentes do projeto:

1. ADR aprovado e vigente;
2. `GOLDEN_PLATFORM_GOVERNANCE.md`;
3. especificação/checkpoint vigente;
4. este plano de execução;
5. código oficial atual;
6. documentação histórica do projeto;
7. referências externas.

Documentos-base desta fase:

- `GOLDEN_PLATFORM_ROADMAP.md`;
- `GOLDEN_PLATFORM_GOVERNANCE.md`;
- `PLANO_FOUNDATION_GOLDEN_V1.md`;
- `platform-libraries/docs/foundation/FOUNDATION-CHECKPOINT.md`;
- `platform-libraries/docs/foundation/FOUNDATION-ARCHITECTURE.md`;
- `platform-libraries/docs/foundation/PLATFORM-LIBRARIES-MODULES.md`.

## 3. Foundation validada

A Golden parte do checkpoint `FOUNDATION-GOLDEN-V1`, tecnicamente concluído.

Baseline oficial:

- Java 25;
- Spring Boot 4.1.1;
- Maven >= 3.9.9;
- `br.com.portalmanager.core:platform-parent:1.0.0`;
- GitHub Packages como repositório Maven remoto oficial;
- `platform-starter:1.0.0`;
- `platform-crud` ausente da Foundation.

O starter obrigatório contém apenas:

```text
platform-starter
├── platform-logging
├── platform-messaging
└── platform-authorization
```

Capabilities explícitas:

- `platform-audit`;
- `platform-catalog`;
- `platform-tagging`;
- `platform-test-support` em escopo de teste.

A Golden não deve modificar a Foundation por preferência arquitetural. Caso seja encontrado um gap real, ele deverá ser registrado com evidência e discutido separadamente antes de qualquer alteração.

## 4. Papel da account-api

O repositório `BrunoBS/account-api` é referência funcional do legado.

Pode ser utilizado para recuperar:

- regras de negócio;
- comportamento observado;
- integrações;
- casos de borda;
- contratos relevantes;
- cenários de teste.

Não deve ser utilizado como base estrutural da nova Golden.

Toda referência extraída do legado deve ser classificada como:

```text
REGRA DE NEGÓCIO
COMPORTAMENTO OBSERVADO
DECISÃO ARQUITETURAL ANTIGA
DÉBITO TÉCNICO
PADRÃO A SER REAVALIADO
```

Preservar comportamento necessário não significa preservar a implementação anterior.

## 5. Escopo funcional mínimo

A Golden V1 utilizará um vertical slice centrado no domínio de Account.

Escopo candidato:

- criação de Account;
- consulta por id;
- listagem e filtros relevantes;
- atualização;
- inativação lógica;
- restauração;
- validações e normalização;
- unicidade;
- approvers;
- optimistic locking;
- autorização;
- tagging;
- auditoria;
- catálogo realmente necessário;
- onboarding mínimo como fluxo não-CRUD real.

O escopo final de cada comportamento é definido em `ACCOUNT-BEHAVIOR-MATRIX.md`.

## 6. Fora de escopo da V1

Não fazem parte da primeira Golden Reference:

- Application;
- Environment;
- Configuration;
- Equalizer;
- Publisher;
- Schema;
- Sharing;
- todos os catálogos do legado sem uso concreto;
- Scaffold;
- MCP;
- Spring AI;
- migração geral de outros serviços;
- reconstrução de abstrações CRUD genéricas.

Também não devem ser introduzidos equivalentes disfarçados de `platform-crud`, como:

- `BaseService`;
- `BaseController`;
- `BaseRepository`;
- `BaseValidator`;
- `BaseMapper`;
- `GenericCrud*`;
- `CrudSupport*`.

## 7. Arquitetura proposta

### 7.1 Forma do projeto

A Golden começa como uma aplicação Spring Boot Maven executável em um único módulo.

Não há evidência, neste checkpoint, que justifique arquitetura multi-module.

O repositório deve ser novo e separado da `account-api`, preservando o legado como referência comparável.

Repositório definido: `BrunoBS/account-service`. Coordenada Maven da aplicação: `br.com.portalmanager:account-service:0.1.0-SNAPSHOT`. Package root Java oficial: `br.com.portalmanager.account`.

### 7.2 Organização por feature

Estrutura candidata:

```text
<root-package>
├── GoldenReferenceApplication
├── account
│   ├── api
│   │   ├── controller
│   │   ├── request
│   │   └── response
│   ├── application
│   ├── domain
│   └── persistence
├── onboarding
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── catalog
└── configuration
```

Fluxo de dependência desejado:

```text
HTTP
  ↓
Controller
  ↓
Application Service
  ↓
Domain + Repository contract
  ↓
Persistence
```

A estrutura não deve criar camadas sem responsabilidade concreta.

### 7.3 API e DTOs

Não reutilizar um único DTO para criação, atualização, leitura e persistência.

Preferir contratos explícitos, por exemplo:

- `CreateAccountRequest`;
- `UpdateAccountRequest`;
- `AccountResponse`;
- `AccountSummaryResponse` somente se houver caso funcional aprovado.

A entidade JPA permanece interna.

Mapeamento deve ser explícito e simples. Não introduzir biblioteca de mapping apenas para reduzir linhas.

### 7.4 Serviços

Casos de uso devem ser explícitos, por exemplo:

- `create`;
- `findById`;
- `findAll`;
- `update`;
- `deactivate`;
- `restore`;
- operações de onboarding.

Não utilizar herança de serviço CRUD nem ciclo de hooks `beforeCreate`, `afterCreate`, `beforeUpdate`, etc.

### 7.5 Validação

Separar:

- validação estrutural de entrada, usando Jakarta Bean Validation quando adequada;
- regras de negócio e integridade, mantidas explicitamente na aplicação/domínio.

Não utilizar `BaseCrudValidator`.

Erros devem seguir os contratos de `platform-messaging`.

### 7.6 Persistência

Baseline:

- Spring Data JPA;
- MySQL;
- migrations SQL versionadas;
- Hibernate em validação de schema;
- `@Version` JPA para optimistic locking.

Não usar `ddl-auto=update` como mecanismo de evolução de schema.

A ferramenta de migration deverá ser decidida e documentada antes da implementação da persistência.

Não utilizar `OptimisticLockable` ou abstração equivalente apenas para envolver `@Version`.

## 8. Uso da Foundation

O consumo esperado está detalhado em `FOUNDATION-USAGE.md`.

Regras principais:

- parent `platform-parent:1.0.0` + BOM `platform-libraries-bom:1.0.0`;
- `platform-starter` como baseline;
- audit/catalog/tagging adicionados explicitamente apenas quando usados;
- `platform-test-support` em escopo de teste;
- nenhuma dependência ou import de `platform-crud`;
- nenhum `mvn install` local da Foundation como mecanismo oficial de integração.

## 9. Estratégia de testes

### Unitários

Cobrir lógica sem necessidade de Spring:

- normalização;
- regras de validação;
- regras de approver;
- regras de autorização de domínio;
- onboarding;
- mappers que possuam lógica.

### Persistência

Validar com MySQL real via Testcontainers:

- queries;
- constraints;
- lifecycle;
- filtros;
- projections quando existirem;
- optimistic locking.

Evitar H2 quando o comportamento relevante for MySQL.

### API / integração

Spring Boot + MySQL Testcontainer + RestAssured para cenários como:

- create;
- read;
- list/filter;
- update;
- deactivate;
- restore;
- validation;
- duplicate;
- authorization;
- optimistic lock;
- tagging;
- onboarding.

### Integração Foundation

Validar uso real de:

- authorization;
- audit;
- catalog;
- tagging;
- messaging;
- test-support.

Usar WireMock somente quando houver contrato HTTP externo real a simular.

### Migrations

O fluxo de teste deve provar:

```text
database vazio
→ migrations
→ aplicação inicia
→ Hibernate valida schema
→ testes executam
```

### Arquitetura

Usar `platform-test-support` onde fizer sentido e complementar com regras específicas da Golden, incluindo:

- controller não acessa repository diretamente;
- domínio não depende de web;
- ausência de qualquer dependência/import de `platform-crud`;
- ausência do artefato `platform-crud`;
- dependências opcionais da Foundation são explícitas;
- regras arquiteturais devem detectar problemas reais, não apenas estética.

## 10. Documentação prevista

```text
docs/golden/
├── GOLDEN-INVENTORY.md
├── ACCOUNT-BEHAVIOR-MATRIX.md
├── GOLDEN-ARCHITECTURE.md
├── FOUNDATION-USAGE.md
├── API-CONVENTIONS.md
├── TEST-STRATEGY.md
├── FOUNDATION-GAPS.md
├── MIGRATION-NOTES.md
└── GOLDEN-CHECKPOINT.md
```

ADRs devem ser criados para decisões estruturais relevantes.

## 11. Política de gaps da Foundation

Quando surgir um problema:

```text
problema
  ↓
é problema da aplicação?
  ├─ sim → resolver na Golden
  └─ não
      ↓
há deficiência reproduzível da Foundation?
      ├─ não → manter explícito na Golden
      └─ sim
          ↓
documentar em FOUNDATION-GAPS.md
          ↓
evidência + impacto + alternativas
          ↓
review humano
          ↓
somente então considerar alteração na Foundation
```

Preferência arquitetural não constitui gap.

## 12. Ondas de execução

### G0 — Inventário funcional e contratos

Sem código de produção.

Objetivos:

- inventariar comportamentos relevantes da `account-api`;
- separar regra de negócio, comportamento, arquitetura antiga e débito;
- fechar escopo da Golden V1;
- mapear uso das capabilities da Foundation;
- registrar pendências estruturais.

Saída:

- `GOLDEN-INVENTORY.md`;
- `ACCOUNT-BEHAVIOR-MATRIX.md`;
- `FOUNDATION-USAGE.md`;
- decisões/pêndencias necessárias para G1/G2.

### G1 — Skeleton + consumo da Foundation

Criar o novo repositório/aplicação somente após G0.

Provar:

- `platform-parent:1.0.0`;
- resolução remota da Foundation;
- `platform-starter`;
- aplicação mínima inicia;
- `platform-test-support`;
- CI;
- `mvn clean verify`.

Critério de saída: serviço vazio, compilável, executável e consumindo a Foundation publicada sem checkout ou install local.

### G2 — Persistência e modelo mínimo

Implementar somente o modelo aprovado pela G0:

- Account;
- approvers;
- account type quando aplicável;
- estado/lifecycle aprovado;
- migrations;
- repositories;
- optimistic locking.

### G3 — Ciclo de vida explícito de Account

Implementar:

- create;
- get;
- list/filter;
- update;
- deactivate;
- restore;
- contratos de request/response;
- normalização;
- validação;
- transações;
- tratamento de erros.

Critério central: CRUD real e explícito sem abstração CRUD genérica.

### G4 — Capabilities transversais

Adicionar somente casos reais:

- authorization;
- audit;
- tagging;
- catalog;
- messaging;
- logging.

### G5 — Fluxo não-CRUD

Implementar onboarding mínimo aprovado pela G0.

### G6 — Hardening

Executar:

- unit tests;
- persistence tests;
- API/integration tests;
- migration tests;
- architecture tests;
- startup/smoke;
- dependency review;
- config review;
- observability review;
- comparação com matriz funcional.

### G7 — Review, documentação e checkpoint

Concluir:

- arquitetura;
- API conventions;
- test strategy;
- migration notes;
- ADRs;
- checkpoint;
- classificação final entre regra de Account, padrão Golden, capability Foundation e infraestrutura.

Somente após aprovação poderá ser declarado `GOLDEN-REFERENCE-V1`.

## 13. Critérios objetivos do checkpoint GOLDEN-REFERENCE-V1

### Foundation

- `platform-parent:1.0.0`;
- artefatos oficiais publicados;
- sem checkout/install local da Foundation;
- starter baseline;
- capabilities opcionais explícitas.

### Arquitetura

- zero dependências/imports de `platform-crud`;
- nenhum BaseCrud equivalente recriado;
- controllers sem persistência direta;
- domínio explícito;
- regras arquiteturais testadas;
- nenhuma mudança na Foundation sem decisão aprovada.

### Funcional

Cobertura do comportamento aprovado em `ACCOUNT-BEHAVIOR-MATRIX.md`.

### Persistência

- migrations versionadas;
- banco sobe do zero;
- aplicação inicia após migrations;
- Hibernate não usa `ddl-auto=update` para evolução;
- schema testado em MySQL.

### Testes

- unitários;
- persistência;
- integração/API;
- migrations;
- arquitetura;
- cenários funcionais selecionados do legado.

### Build/CI

- `mvn clean verify`;
- Maven Enforcer;
- dependency convergence;
- Java 25;
- Spring Boot 4.1.1;
- CI verde;
- aplicação inicia.

### Documentação

- inventário;
- matriz de comportamento;
- arquitetura;
- uso da Foundation;
- convenções de API;
- estratégia de testes;
- gaps;
- ADRs aplicáveis;
- checkpoint.

## 14. Pendências de G0

Antes de avançar de forma definitiva para a implementação, registrar decisão sobre:

1. ~~nome do novo repositório e package root~~ — definido: `BrunoBS/account-service` / `br.com.portalmanager.account`;
2. ferramenta de migrations;
3. política de compatibilidade de API com `account-api`;
4. uso ou não de `ResourceVisibility`;
5. modelagem de lifecycle de Account: domínio explícito vs catálogo persistido.

Essas decisões não autorizam alterações na Foundation.