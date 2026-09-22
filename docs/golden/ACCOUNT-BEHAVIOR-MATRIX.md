# Account — Behavior Matrix

**Fase:** G0  
**Fonte funcional:** `BrunoBS/account-api` (`main`)  
**Objetivo:** separar comportamento a preservar de implementação a redesenhar.

> **Nota pós-G4:** `Account` é a nomenclatura histórica desta matriz de G0. Os comportamentos PRESERVAR/REDESENHAR aplicáveis continuam válidos para o agregado atualmente denominado `Workspace`; o arquivo não define a nomenclatura ativa pós-G4.

## Legenda

| Decisão | Significado |
|---|---|
| PRESERVAR | O comportamento faz parte do slice Golden V1. |
| REDESENHAR | A intenção permanece, mas a implementação antiga não. |
| ADIAR | Comportamento conhecido, fora do slice Golden V1. |
| DESCARTAR | Não deve ser reproduzido como padrão. |
| REAVALIAR | Exige decisão antes da implementação correspondente. |

## Matriz

| Área | Comportamento observado | Classificação | Decisão Golden V1 | Observação |
|---|---|---|---|---|
| Create | Criar Account e gerar id técnico no servidor | COMPORTAMENTO OBSERVADO | PRESERVAR | O id enviado pelo cliente não deve controlar a identidade persistida. |
| Create | Gerar identifier único | COMPORTAMENTO OBSERVADO | PRESERVAR | Implementação pode usar UUID tipado em vez de String, conforme modelagem. |
| Create | Lifecycle inicial ACTIVE | REGRA/COMPORTAMENTO | PRESERVAR | Resolvido em G2: `AccountLifecycle.ACTIVE` explícito no domínio. |
| Create | onboarding inicia falso | COMPORTAMENTO OBSERVADO | PRESERVAR | Mantém o fluxo não-CRUD. |
| Create | registrar fase ACCOUNT_REGISTRATION como concluída | COMPORTAMENTO OBSERVADO | PRESERVAR | Implementação explícita, sem hook CRUD. |
| Create | authorizerGroup nulo vira `""` | COMPORTAMENTO OBSERVADO | REDESENHAR | G3 preserva ausência como `null`; não replica normalização legada para string vazia. |
| Validation | AccountType somente ADMIN/MANAGER | REGRA DE NEGÓCIO | PRESERVAR | CATALOG é inválido para Account. |
| Validation | name obrigatório 3..100 | REGRA/COMPORTAMENTO | PRESERVAR | Validar após normalização. |
| Validation | description 10..500 | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Validation | requester mínimo 5 | REGRA/COMPORTAMENTO | PRESERVAR | Não há máximo documentado no legado analisado. |
| Validation | acronym obrigatório máx. 5 | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Validation | emailGroup deve ser email válido | REGRA/COMPORTAMENTO | PRESERVAR | A regex antiga não precisa ser copiada; usar mecanismo adequado e testar comportamento. |
| Validation | pelo menos um approver | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Validation | approver funcional obrigatório | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Validation | approver email válido | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Validation | nome único independente de lifecycle | REGRA/COMPORTAMENTO | PRESERVAR | Constraint física + validação amigável. |
| Normalization | trim em name/description/requester/acronym/authorizerGroup/emailGroup | COMPORTAMENTO OBSERVADO | PRESERVAR | Tornar explícito. |
| Normalization | filtros vazios são ignorados | COMPORTAMENTO OBSERVADO | PRESERVAR | `typeName` e `tagName`. |
| Query | listar ACTIVE por padrão | COMPORTAMENTO OBSERVADO | PRESERVAR | Contrato exato do default será registrado em API conventions. |
| Query | listar INACTIVE com `active=false` | COMPORTAMENTO OBSERVADO | PRESERVAR |  |
| Query | filtrar por AccountType ignorando case e whitespace | COMPORTAMENTO OBSERVADO | PRESERVAR |  |
| Query | filtrar por tag normalizada | COMPORTAMENTO OBSERVADO | PRESERVAR | Implementado em G4 com `platform-tagging` e `TagNormalizer`. |
| Query | `simplify=true` com contadores de Environment/Publisher/Application | COMPORTAMENTO OBSERVADO | ADIAR | Dependência indevida para o slice mínimo da V1. |
| Get | get por id trata INACTIVE como não encontrado | COMPORTAMENTO OBSERVADO | PRESERVAR | Mesmo comportamento usado pelo onboarding. |
| Update | atualizar Account existente | COMPORTAMENTO OBSERVADO | PRESERVAR | Caso de uso explícito. |
| Update | manter próprio nome é permitido | COMPORTAMENTO OBSERVADO | PRESERVAR | Integridade por id atual. |
| Update | nome usado por outra Account é rejeitado | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Update | substituir coleção de approvers | COMPORTAMENTO OBSERVADO | PRESERVAR | Semântica de PUT. |
| Update | versão obsoleta retorna conflito | COMPORTAMENTO OBSERVADO | PRESERVAR | JPA `@Version`; sem `OptimisticLockable`. |
| Delete | delete é inativação lógica | REGRA/COMPORTAMENTO | PRESERVAR | Endpoint poderá continuar DELETE, mas comportamento é deactivate. |
| Delete | id inexistente retorna not found | COMPORTAMENTO OBSERVADO | PRESERVAR |  |
| Restore | apenas Account INACTIVE pode ser restaurada | REGRA/COMPORTAMENTO | PRESERVAR | Conta ACTIVE em restore é inválida. |
| Restore | restaurar preserva identidade e nome | COMPORTAMENTO OBSERVADO | PRESERVAR |  |
| Tagging | persistir tags manuais | COMPORTAMENTO OBSERVADO | PRESERVAR | Implementado em G4 via `TagManager.reconcile`. |
| Tagging | normalizar/deduplicar tags | COMPORTAMENTO OBSERVADO | PRESERVAR | Delegado ao `platform-tagging` em G4. |
| Tagging | gerar tags de sistema a partir de identifier/name/authorizerGroup/acronym | COMPORTAMENTO OBSERVADO | PRESERVAR INTENÇÃO | Conjunto confirmado e implementado em G4. |
| Tagging | tags de sistema não são devolvidas como tags manuais | COMPORTAMENTO OBSERVADO | PRESERVAR | Comprovado em G4. |
| Tagging | update remove tags de sistema antigas e recalcula novas | COMPORTAMENTO OBSERVADO | PRESERVAR | Implementado e testado em G4 com `TagManager.reconcile`. |
| Auth | requisição sem autenticação pode ser rejeitada mesmo em endpoint OPEN | COMPORTAMENTO OBSERVADO | PRESERVAR | Confirmado em G4: OPEN é policy de autorização, mas interceptor exige correlation id + Bearer token. |
| Auth | owner possui acesso ao recurso | COMPORTAMENTO OBSERVADO | PRESERVAR INTENÇÃO | `UserSession.isOwner()`. |
| Auth | authorizer group correto possui acesso | COMPORTAMENTO OBSERVADO | PRESERVAR INTENÇÃO | Regra do domínio/aplicação deve permanecer explícita. |
| Auth | usuário sem grupo apropriado recebe forbidden | COMPORTAMENTO OBSERVADO | PRESERVAR INTENÇÃO |  |
| Auth | matriz OPEN/DEV/ADM por endpoint | COMPORTAMENTO OBSERVADO | PRESERVAR | Reavaliada em G4: create/list OPEN, get DEV, mutações ADM. |
| Auth | `@ResourceVisibility` em leitura/listagem | PADRÃO A SER REAVALIADO | PRESERVAR | Adotado em G4 porque `authorizerGroup` é caso real de visibilidade de Account. |
| Audit | auditar create/update/delete/restore/onboarding update | COMPORTAMENTO OBSERVADO | PRESERVAR INTENÇÃO | G4 implementa create/update/delete/restore; onboarding update permanece para G5. |
| Onboarding | consultar progresso somente para Account ACTIVE | COMPORTAMENTO OBSERVADO | PRESERVAR | Inativa/inexistente → not found. |
| Onboarding | conclusão exige lista não vazia | COMPORTAMENTO OBSERVADO | PRESERVAR | Evita conclusão vazia. |
| Onboarding | conclusão exige todos os status `COMPLETED` | REGRA/COMPORTAMENTO | PRESERVAR |  |
| Onboarding | legado retorna quatro fases após criação | COMPORTAMENTO OBSERVADO | REAVALIAR | Quantidade depende do catálogo/configuração; não transformar “4” em regra sem evidência. |
| Persistence | `ddl-auto=update` | DÉBITO TÉCNICO | DESCARTAR | Golden usa migrations versionadas + validation. |
| Architecture | `BaseCrudService` | DECISÃO ARQUITETURAL ANTIGA | DESCARTAR | Casos de uso explícitos. |
| Architecture | `BaseCrudValidator` | DECISÃO ARQUITETURAL ANTIGA | DESCARTAR | Validação explícita. |
| Architecture | `BaseCrudRepository` | DECISÃO ARQUITETURAL ANTIGA | DESCARTAR | Repositório Spring Data específico. |
| Architecture | `CrudNormalizer` | DECISÃO ARQUITETURAL ANTIGA | DESCARTAR | Normalização explícita. |
| Architecture | `OptimisticLockable` | DECISÃO ARQUITETURAL ANTIGA | DESCARTAR | JPA `@Version` basta. |
| Architecture | um `AccountDTO` para create/update/read | PADRÃO A SER REAVALIADO | DESCARTAR | Contratos de API separados. |
| Architecture | packages `common/core/feature/message/web` | DECISÃO ARQUITETURAL ANTIGA | DESCARTAR COMO PADRÃO | Golden usa organização por feature/responsabilidade. |

## Cenários mínimos de regressão funcional

A Golden deve possuir testes equivalentes de comportamento para, no mínimo:

1. create + get;
2. list active;
3. update;
4. deactivate + not found + restore;
5. nome duplicado;
6. account type inválido;
7. validações de limites;
8. approver obrigatório e válido;
9. filtro por type;
10. filtro por tag;
11. tags manuais e de sistema;
12. stale update → conflict;
13. autorização owner / authorizer / forbidden;
14. onboarding de conta inexistente/inativa;
15. onboarding incompleto;
16. restore de conta ACTIVE inválido.

O endpoint de summary legado não entra nesse conjunto para a V1 porque depende de domínios adiados.