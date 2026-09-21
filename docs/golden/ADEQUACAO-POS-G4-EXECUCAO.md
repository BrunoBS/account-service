# Execução — Adequação Arquitetural Pós-G4

## Estado

Adequação implementada e validada na branch `refactor/architectural-alignment-post-g4`.

## Baseline

- `main`: `b49949638818f1229c78dfb93b0b6238e1a5e1fe`
- Verify baseline: #70 / run `35636832562` / BUILD SUCCESS
- testes baseline: 26

## Registros prévios à alteração

- ADR: commit `75d1aae2e58b9320298c820794cd74206b1a874b`
- Plano: commit `4ac97ef58f614339e727934d5b9442e5d26e2549`

Esses dois documentos foram criados antes da primeira mudança de aplicação.

## Implementação

Commit estrutural:

```text
c1e4a56d97f94e89e38d7ddf1b0c9fab5f66b6ef
```

Incluiu:

- namespace `br.com.itau.portalmanager.workspace`;
- macrozonas Foundation/Core/Input, sem pacotes vazios;
- Core Workspace;
- seis Use Cases separados;
- contratos Input/Output e Request/Response;
- integração explícita de busca por tags;
- renomeação HTTP/persistência/audit/tag owner;
- migration V3;
- fitness functions ampliadas;
- testes G4 adequados ao contrato Workspace.

## Evidência de validação

GitHub Actions Verify #72 / run `35664191912`:

```text
34 fontes principais compiladas
9 fontes de teste compiladas
11 testes unitários/arquiteturais
20 testes de integração
31 testes totais
0 falhas
0 erros
Flyway V1 -> V2 -> V3
BUILD SUCCESS
```

A migration V3 foi executada em MySQL 8 e levou o schema a `v3`.

## Account -> Workspace

A varredura do código ativo confirmou ausência de arquitetura híbrida.

Ocorrências restantes são deliberadas:

- migrations históricas/transicionais;
- documentação histórica G0–G4;
- `accountId` do contrato técnico de Authorization;
- assertions que provam remoção das tabelas legadas;
- nomes históricos/externos dos repositórios GitHub.

## Foundation

Nenhum arquivo de `platform-libraries` foi alterado.
