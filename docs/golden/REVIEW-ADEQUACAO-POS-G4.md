# Review — Adequação Arquitetural Pós-G4

- **Data:** 2026-09-21
- **Branch:** `refactor/architectural-alignment-post-g4`
- **PR:** #5
- **Baseline:** `b49949638818f1229c78dfb93b0b6238e1a5e1fe`
- **Status:** findings bloqueadores corrigidos; pendências de checkpoint registradas

## Objetivo

Revisar a adequação pós-G4 depois da implementação, confrontando código, testes,
documentação vigente e governança, sem reabrir a Golden Platform Foundation.

## Findings

| Severidade | Finding | Resultado |
|---|---|---|
| bloqueador | wiring de Messaging colocado em application Foundation sem base explícita | corrigido pelo ADR-006 e composition root |
| bloqueador | independência de Domain ficou menos protegida que no baseline | fitness function restaurada e ampliada |
| alto | V3 testada somente em banco vazio | criado teste V2 populada -> V3 |
| alto | cross-module não distinguia módulos aninhados | identificação estrutural corrigida |
| médio/alto | `usecase.support` podia virar API cross-module | cross-module limitado a UseCase/Input/Output |
| médio | Input podia acessar internals sem regra abrangente | bloqueio para internals Core/Feature |
| médio | `groupId` Maven sem decisão explícita | pendência registrada; sem mudança inventada |
| médio | SQL direto em `tags` é contrato físico frágil | dívida aceita, isolada em Integration e coberta |
| baixo | descrição administrativa do repo ainda usa Account | follow-up administrativo, sem impacto arquitetural |

## Decisão sobre wiring técnico

O package raiz exato é o composition root. Ele pode conter somente bootstrap e
`@Configuration` de composição técnica. Isso não cria quinta macrozona.

`WorkspaceMessagingConfiguration` reside no root. A application foundation macrozone
permanece voltada às responsabilidades arquiteturais/de negócio aprovadas, como Catalog
e futuramente Schema quando houver implementação real.

## Fitness functions finais

`GoldenArchitectureTest` possui 11 testes/regras:

1. topologia de primeiro nível fechada;
2. composition root restrito;
3. Foundation sem dependência de Core/Feature/Input;
4. Core sem Feature/Input;
5. zonas internas sem Input;
6. Domain sem UseCase/Repository/Integration/Input/Spring Web;
7. Input sem bypass para Domain/Repository/Integration de Core/Feature;
8. RestController sem Repository;
9. RestController sem Integration;
10. cross-module somente por contratos de Use Case;
11. módulos aninhados identificados de forma independente.

## Migration

`DatabaseUpgradeMigrationIT` executa:

```text
Flyway -> V2
seed Account + Approver + Tag ACCOUNT
Flyway -> V3
assert Workspace + WorkspaceApprover + Tag WORKSPACE
assert FK/cascade
```

Isso complementa `DatabaseMigrationIT`, que continua provando instalação limpa V1→V3.

## Evidências

### Verify #81 — run 35668223125

Falhou de forma útil durante o review: a primeira regra de Input bloqueava também
`foundation.catalog.domain`. O resultado mostrou que a fitness function estava mais
restritiva que a matriz macro permitida.

### Verify #82 — run 35668527818

Resultado final do código corrigido:

- Java 25;
- 34 fontes principais;
- 10 fontes de teste;
- 11 testes arquiteturais;
- 3 testes unitários;
- 21 testes de integração;
- 35 testes totais;
- 0 falhas / 0 erros;
- MySQL 8;
- V2 populada -> V3 validada;
- **BUILD SUCCESS**.

## Pendências

### Maven groupId

O Java root é `br.com.itau.portalmanager.workspace`, enquanto a coordenada Maven atual é
`br.com.portalmanager:workspace-service`. Não existe decisão vigente suficiente para
alterar esse contrato automaticamente. Resolver antes do checkpoint
`GOLDEN-REFERENCE-V1`.

### Tag reverse lookup

`WorkspaceTagSearchIntegration` conhece o contrato físico de `tags`. A dívida é
aceita porque `TagManager` não expõe busca reversa. Não alterar `platform-libraries`
nesta atividade.

### Metadado do repositório

O nome físico `account-service` permanece por decisão. A descrição administrativa do
repositório ainda menciona gestão de contas e deve ser ajustada como follow-up
administrativo; isso não altera código, contratos ou arquitetura.

## Conclusão

Os findings bloqueadores e altos identificados no review foram corrigidos e validados.
A PR permanece em draft/review. Merge e checkpoint exigem a etapa de review/aprovação
correspondente e não são executados automaticamente por este documento.
