# Golden Reference — contratos verificados na implementação

> Estado: revisão incremental baseada em arquivos de código reais da branch `main` em 2026-10-19. Este documento **não** representa certificação de 100% das classes. Evidências não inspecionadas são explicitadas como pendências.

## Fonte de verdade e baseline

- Consumidor: `BrunoBS/account-service`, artefato Maven `workspace-service`.
- Foundation: `BrunoBS/platform-libraries`, versão de desenvolvimento `1.0.0` fixa por decisão de governança; republicações são permitidas enquanto não há operação produtiva.
- Java 25, parent `platform-parent:1.0.0`, BOM `platform-libraries-bom:1.0.0`.
- Autorização `MOCK` é intencional na fase atual de desenvolvimento/validação. `REAL` é gate obrigatório para entrada efetiva em produção.

## 1. Schema Validation

**Evidência:** `src/main/resources/application.yml` e `modules/platform-schema-validation/README.md` da Foundation.

```yaml
platform:
  schema-validation:
    fallback-code: DEFAULT
    view-name: vw_platform_resource_schemas
```

A view e o fallback estão alinhados com as propriedades públicas documentadas. O PR #50 foi integrado. A documentação da library diferencia `@ValidateResourceSchemaRequest` (validação de request HTTP) e `@ValidateResourceSchema` (fronteira de use case). A cobertura real de cada endpoint ainda exige inventário por classe.

## 2. Tagging — Workspace

**Evidência:** `core/workspace/usecase/operations/WorkspaceCommandService.java`, `core/workspace/domain/Workspace.java`, README de `platform-tagging`.

- `Workspace implements TagOwner`, fornece ID interno e identifier público; a entidade mantém `@AuthorizerGroup`.
- `create`: valida entrada, cria e salva o Workspace com `saveAndFlush`, depois invoca `TagManager.createInitialTags(savedWorkspace, input.tags(), WorkspaceSystemTags.resolve(savedWorkspace))` dentro de `@Transactional`.
- `update`: encontra Workspace ativo para alteração, valida, atualiza e salva, depois invoca `TagManager.reconcileAndReturnManualTags` com tags manuais solicitadas e tags sistêmicas resolvidas.
- `inactivate` e `restore`: usam `findManual` para compor a resposta.
- `delete`: **não é exclusão física imediata**; encontra Workspace inativo e chama `quarantine`. No domínio, a transição é `INACTIVE -> QUARANTINED`.
- `create`, `update`, `inactivate`, `restore` e `delete` estão anotados com `@Auditable`.

## 3. Tagging — Application

**Evidência:** `core/application/usecase/operations/ApplicationCommandService.java`.

- `create`: resolve Workspace ativo, valida, salva Application com `saveAndFlush`, chama `TagManager.createInitialTags` com `ApplicationSystemTags.resolve`; registra conclusão da fase de onboarding quando se trata da primeira aplicação.
- `update`: resolve Workspace, carrega Application ativa para alteração, valida, salva e chama `TagManager.reconcileAndReturnManualTags`.
- `inactivate` e `restore`: consultam tags manuais via `TagManager.findManual`.
- Operações inspecionadas são transacionais e possuem eventos de auditoria.

**Limite:** a implementação interna do `TagManager`, o repository de tags, filtros em queries de tags e os testes correspondentes não foram inspecionados nesta rodada. Não inferir segurança de `findOwnerKeysByTag` apenas do contrato da library: o consumidor precisa aplicar `ResourceVisibility` na consulta final.

## 4. ResourceVisibility e autorização

**Evidência:** `core/workspace/domain/Workspace.java`, `GoldenArchitectureTest.java` e README de `platform-authorization`.

- `Workspace.authorizerGroup` possui `@AuthorizerGroup`.
- `GoldenArchitectureTest` impede controllers de dependerem diretamente de repositories e proíbe dependências das zonas internas para `entrypoint`.
- O teste arquitetural exige métodos de facade de catálogo anotados com `@AuthorizationRequired(level = OWNER)`.
- `@ResourceVisibility` e `@AuthorizationRequired` são mecanismos distintos segundo o contrato público da Foundation; **não há evidência suficiente nesta revisão para afirmar que todos os finders estão cobertos**.

## 5. Redis

**Evidência:** POM do consumidor, `application.yml` e README de `platform-redis`.

- Dependências diretas `platform-redis:1.0.0` e `platform-testing-redis:1.0.0` (teste).
- `platform.redis.namespace=dynamic-parameter` e `namespace-mode=SHARED` no consumidor.
- O README da Foundation descreve `RedisStore.setAll/getAll` como MSET/MGET únicos, sem particionamento automático. O consumidor declara `workspace.parameter.runtime.batch-size: 500`; **não se pode concluir que o batch-size controla MSET/MGET sem inspecionar os serviços de runtime**.
- A inspeção do fluxo real de cache, TTL, fallback, invalidation, reconciliação e testes de integração Redis permanece pendente.

## 6. Estados do Workspace e persistência

**Evidência:** `core/workspace/domain/Workspace.java` e `WorkspaceCommandService.java`.

- Entidade JPA `workspaces`, `id` Long interno, `identifier` UUID público, `@Version` para concorrência otimista, `settings` JSON via Jackson 3.
- `create` inicia lifecycle ACTIVE; `inactivate`: ACTIVE -> INACTIVE; `restore`: INACTIVE -> ACTIVE; `quarantine`: INACTIVE -> QUARANTINED.
- `completeOnboarding` é idempotente no domínio quando já concluído.
- Approvers são uma relação `@OneToMany` com cascade e orphanRemoval.

## 7. CI efetivamente observado

- `account-service`: Verify #39247602027 **success**; a etapa `Verify with isolated Maven repository` concluiu com sucesso (Java 25, `mvn clean verify`, MySQL 8.4.11 pré-carregado).
- `platform-libraries`: Verify #39132515600 **success** no PR de documentação.
- O histórico também registra falha anterior #39132494569 por símbolos ausentes em Schema Validation/Tagging.
- Essas evidências são específicas dos commits executados; **não provam CI verde do HEAD mais recente**.

## 8. Pendências para auditoria ponta a ponta

1. Inventariar todos os controllers, requests, facades e use cases; conferir autorização e Schema Validation por rota.
2. Inventariar todos os finders/repositories e filtros `@ResourceVisibility`, inclusive consultas por tags e criador.
3. Inspecionar código de runtime Redis, lote de 500, expiração, reconciliação e testes.
4. Comparar migrations Flyway e constraints JPA (tags, ambientes, aplicações, referências).
5. Inspecionar implementações e testes internos de Schema Validation, Tagging, Authorization, Audit e Redis na Foundation.
6. Verificar publicação real dos artefatos `1.0.0` e CI do HEAD de ambos os repositórios.
7. Atualizar documentação canônica e exemplos somente após confrontar a implementação correspondente.

**Regra:** nenhum item pendente deve ser marcado como homologado por inferência documental.
