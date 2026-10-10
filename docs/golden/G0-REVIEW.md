# G0 Review — GOLDEN-REFERENCE-V1

**Data:** 2026-09-20  
**Estado:** análise principal concluída; package root fechado antes de G2.

## Artefatos produzidos

- `PLANO_GOLDEN_REFERENCE_V1.md`;
- `docs/golden/GOLDEN-INVENTORY.md`;
- `docs/golden/ACCOUNT-BEHAVIOR-MATRIX.md`;
- `docs/golden/FOUNDATION-USAGE.md`;
- `docs/golden/FOUNDATION-GAPS.md`.

## Conclusões de G0

1. `FOUNDATION-GOLDEN-V1` é o baseline técnico da Golden.
2. `account-api` permanece fonte funcional, não base de implementação.
3. A Golden deve nascer em repositório novo.
4. `platform-crud` e abstrações `BaseCrud*` não entram na Golden.
5. O slice V1 fica centrado em Account.
6. O summary legado com Environment/Publisher/Application fica adiado.
7. AccountType é o principal caso real de `platform-catalog`.
8. Lifecycle de Account não deve ser catálogo automaticamente; preferência atual é estado explícito do domínio.
9. Tagging, authorization e audit possuem casos reais.
10. Onboarding permanece como fluxo não-CRUD mínimo.
11. Migrations versionadas substituem `ddl-auto=update`.
12. Nenhum gap real da Foundation foi confirmado até aqui.

## Pendências para avançar

### Necessárias para G1

- criar o novo repositório;
- definir nome do repositório;
- ~~definir package root~~ — definido: `br.com.portalmanager.account`;
- definir política de compatibilidade de API.

### Necessárias antes de G2

- escolher ferramenta de migrations;
- decidir modelagem final de lifecycle.

### Necessárias antes de G4

- aprovar matriz de autorização;
- decidir uso ou não de `ResourceVisibility`.

### Necessárias antes de G5

- definir conjunto mínimo de fases do onboarding e se `OnboardingPhase` será catálogo administrável.

## Defaults recomendados para review

Estas são recomendações da análise, não decisões de governança ainda:

```text
repository: golden-reference
package root: br.com.portalmanager.account
API compatibility: preservar comportamento necessário, sem compromisso de wire compatibility total
ResourceVisibility: não usar na V1 sem necessidade concreta
Account lifecycle: enum/estado explícito no domínio
migration tool: pendente de decisão técnica específica
```

A criação do repositório marca o início operacional da G1.
