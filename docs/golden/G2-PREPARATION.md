# G2 Preparation — Persistência e modelo mínimo

**Fase:** G2 — Análise / Inventário  
**Estado:** pronto para decisão; implementação ainda não iniciada  
**Data:** 2026-09-20

## 1. Entry gate

A G2 só é aberta após a revalidação da Foundation consolidada.

Evidência:

- `br.com.portalmanager.core:platform-parent:1.0.0`;
- `br.com.portalmanager.core:platform-libraries-bom:1.0.0`;
- `platform-starter:1.0.0`;
- `platform-test-support:1.0.0`;
- registry único `https://maven.pkg.github.com/brunobs/platform-libraries`;
- GitHub Actions Verify #17, run `35546171874`: sucesso com repository Maven local isolado;
- GitHub Actions Verify #22, run `35546296942`: sucesso no head documental final;
- nenhuma instalação local da Foundation.

## 2. Namespace da aplicação

Identidade aprovada antes da implementação da G2:

```text
Maven: br.com.portalmanager:account-service:0.1.0-SNAPSHOT
Java:  br.com.portalmanager.account
```

O namespace da aplicação permanece separado da Foundation (`br.com.portalmanager.core`).

## 3. Escopo G2 já aprovado

Implementar somente o modelo mínimo necessário para Account:

- Account;
- approvers;
- account type quando a modelagem estiver fechada;
- estado/lifecycle aprovado;
- migrations versionadas;
- repositories explícitos;
- optimistic locking com JPA `@Version`.

Não implementar nesta onda:

- controllers;
- CRUD genérico;
- onboarding;
- tagging;
- audit;
- regras completas de autorização;
- outros domínios do legado.

## 4. Guardrails

- zero `platform-crud`;
- nenhuma classe `BaseCrud*`;
- nenhuma abstração genérica criada para esconder JPA;
- MySQL como banco alvo;
- testes de persistência em MySQL real/Testcontainers;
- `spring.jpa.hibernate.ddl-auto=validate` após migrations;
- `ddl-auto=update` proibido;
- migrations devem criar um banco vazio de forma reproduzível;
- entity não é contrato HTTP;
- optimistic locking usa `@Version` diretamente.

## 5. Inventário mínimo de Account

Comportamentos que a persistência precisa permitir:

- id técnico gerado pelo servidor;
- identifier estável;
- nome único independentemente do lifecycle;
- Account inicia ACTIVE;
- Account pode ser desativada para INACTIVE;
- restore somente de INACTIVE;
- leitura funcional padrão ignora INACTIVE;
- approvers pertencem à Account;
- update substitui approvers;
- concorrência obsoleta deve resultar em conflito por optimistic locking;
- timestamps de criação/atualização;
- campos funcionais já catalogados na matriz G0.

O desenho físico definitivo só deve ser fechado depois das decisões abaixo.

## 6. Decisão pendente — ferramenta de migrations

O plano exige migrations SQL versionadas, mas ainda não registrou ferramenta.

### Recomendação técnica

**Flyway** é a recomendação para review porque:

- o plano já privilegia migrations SQL explícitas;
- o fluxo esperado é linear e versionado;
- a Golden precisa demonstrar um padrão simples e reproduzível, não uma DSL de mudanças;
- Spring Boot 4.1.1 possui integração oficial com Flyway;
- para MySQL, a integração atual requer o módulo específico de banco além do starter Flyway.

Esta é uma recomendação técnica para decisão da Golden, não uma decisão da Foundation.

### Alternativa

Liquibase continua tecnicamente possível, mas não há requisito documentado no projeto que exija changelog XML/YAML/JSON, rollback declarativo ou abstração cross-database.

### Status

```text
PENDENTE DE APROVAÇÃO
recomendação: Flyway
```

## 7. Decisão pendente — lifecycle de Account

A G0 registrou explicitamente que o legado usar catálogo para ACTIVE/INACTIVE não prova que lifecycle seja administrável.

### Recomendação arquitetural já indicada pela G0

Modelar lifecycle como estado explícito do domínio:

```text
ACTIVE
INACTIVE
```

Razões já suportadas pelo projeto:

- domínio deve permanecer explícito;
- `platform-catalog` deve ser usado para conceitos realmente administráveis;
- ACTIVE/INACTIVE é estado intrínseco do agregado Account no slice atual;
- soft delete e restore dependem diretamente desse estado;
- não há requisito atual para criar/editar estados de lifecycle em runtime.

### Status

```text
PENDENTE DE APROVAÇÃO FINAL
recomendação: enum/estado explícito de domínio
```

## 8. AccountType

`AccountType` permanece o principal candidato real para `platform-catalog`.

A G2 não deve antecipar integração artificial com Catalog apenas para montar schema. A modelagem deve preservar o requisito funcional de tipo e ser compatível com a decisão de capability que será aplicada na onda apropriada.

Nenhuma nova abstração de catálogo deve ser criada na Golden.

## 9. Sequência após decisões

Após aprovação das duas decisões:

```text
decisões G2
→ POM de persistência
→ configuração MySQL
→ migrations iniciais
→ entidades Account/Approver
→ repositories
→ optimistic locking
→ testes de migration
→ testes de persistência
→ architecture checks
→ mvn clean verify
→ review G2
```

## 10. Critério de saída G2

A G2 só fecha quando:

- banco MySQL vazio sobe;
- migrations aplicam do zero;
- Hibernate valida o schema;
- Account/Approver persistem;
- uniqueness é protegida;
- lifecycle aprovado está persistido;
- stale update é detectado por `@Version`;
- repositories são explícitos;
- testes usam infraestrutura opt-in;
- `mvn clean verify` está verde;
- documentação da decisão está registrada.

## 11. Foundation

Nenhuma alteração na Foundation está autorizada nesta etapa.

Qualquer problema encontrado deve ser primeiro classificado como integração/uso da Golden. Somente evidência reproduzível de defeito da Foundation pode reabrir essa frente.
