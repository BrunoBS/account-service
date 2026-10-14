# Revisão das compatibilidades de tipos de ambiente

## Contrato e nomes

Controller, facade e command expõem create, inactivate e delete. QueryService expõe find e list. O domínio usa restore para reativar uma compatibilidade inativa. Finder concentra findForUpdate e findByTypePairForUpdate; nomes de variáveis distinguem compatibility, parentType, childType, typePair e lockedTypes.

As rotas HTTP permanecem POST /api/v1/environment-types/compatibilities, GET da coleção/detalhe, POST /{identifier}/inactivate e DELETE /{identifier}. Create retorna 201 e output; inactivate/delete continuam retornando 204. List usa LifecycleFilter comum e ordenação estável por id. Find/list carregam parentType/childType na mesma consulta por EntityGraph, evitando consultas lazy adicionais ao montar outputs.

## Métodos e regras

| Local | Resultado da revisão |
| --- | --- |
| Command.create | Bloqueia tipos em ordem de código, valida referências ativas, permissão de filhos, coerência de escopo e ausência de ciclo, consulta o par com lock. Novo par é persistido; par ativo é idempotente quanto à linha; par INACTIVE é restaurado preservando identifier. Outro lifecycle é rejeitado pelo domínio. saveAndFlush garante estado persistido antes do snapshot. |
| Validator.validateForCreate | Concentra resolução dos dois tipos e regras de elegibilidade, escopo e ciclos, sem receber booleans calculados pelo command. Autorrelação é rejeitada. A busca iterativa do grafo usa visitedTypeCodes, incluindo reativação. |
| Command.inactivate | Carrega a compatibilidade com lock, exige ACTIVE no domínio e grava INACTIVE. Retorna output para auditoria, enquanto a facade/controller respondem 204. Repetição em INACTIVE retorna 400. |
| Command.delete | Exige INACTIVE e ausência de ambientes que usem o par, em qualquer lifecycle. Remove fisicamente e usa AuditAction.PURGE/ENVIRONMENT_TYPE_COMPATIBILITY_PURGED. |
| Validator.validateForDelete | Centraliza estado e consulta de uso atual, antes da remoção. Relações existentes impedem a exclusão; não há mudança em cascata. |
| Finder.findForUpdate / findByTypePairForUpdate | Carregamentos de entidade com lock e recarga, dentro da transação do command. |
| TypeFinder.findAllForUpdate | Descarta instâncias de snapshot antes do lock e retorna tipos atuais. Mantém ordem dos locks. |
| Query.find / list | Read-only, outputs, filtro centralizado, erro 404 para identifier ausente e ordem estável. |
| EnvironmentTopologyValidator.validateForNewLink | Exige compatibilidade ACTIVE com leitura atual FOR SHARE. Usado na criação e na troca de tipo. |
| EnvironmentTopologyValidator.validateForExistingLink | Preserva o vínculo na edição sem troca de tipo e no restore. Mantém validação de scope, parent e allowsChildren; dispensa revalidar a disponibilidade da compatibilidade para um vínculo que já existe. |

Inativação da compatibilidade barra novos vínculos, mas mantém ambientes já relacionados. Leitura, edição mantendo o tipo e restauração dos ambientes existentes continuam possíveis; exclusão da compatibilidade permanece bloqueada por esses registros.

## Persistência e concorrência

A constraint única parent_type_code/child_type_code permanece como proteção de unicidade após a migration V56. Os comandos mantêm apenas @Transactional, sem isolamento explícito. As alterações do grafo são serializadas pelos locks exclusivos ordenados dos tipos. Esse conjunto está limitado aos quatro códigos permitidos no contrato HTTP; não foi removido o lock global do grafo, pois bloquear apenas o par permitiria ciclos em alterações concorrentes de pares diferentes.

Consultas do grafo ativo, da disponibilidade de um par e de ambientes que usam o par são escalares e usam FOR SHARE. Assim, não reutilizam um snapshot anterior à espera por locks no isolamento padrão. Não se materializam ambientes para verificar uso.

O teste com EnvironmentType previamente gerenciado e alterado por outra transação reproduziu ObjectOptimisticLockingFailureException na primeira consulta com lock, antes do detach. O TypeFinder foi ajustado para descartar a instância de snapshot por leitura sem lock antes da consulta bloqueada. A consulta com lock continua sendo a leitura autoritativa; não se usa refresh para supor estado atual. As demais entidades gerenciadas não são limpas do EntityManager.

## Auditoria

Inactivate devolvia void ao coletor de snapshots; agora devolve EnvironmentCompatibilityOutput com estado persistido. Os eventos ALLOWED/DISALLOWED foram preservados como fatos de negócio. A criação idempotente mantém a linha e o identifier, mas cada chamada bem-sucedida continua auditando o fato ALLOWED, conforme o comportamento existente.

Delete é remoção física: PURGE substitui DELETE. O command recebe identifier, busca sob lock, valida, monta o output antes da remoção, executa delete/flush e retorna o objeto removido. O aspect captura o retorno na mesma transação. Facade/controller descartam o output e mantêm HTTP 204.

EnvironmentAuditSnapshotProvider e DeleteEnvironmentTypeCompatibilityInput foram removidos. Não há busca adicional pela auditoria nem inferência do recurso pelos argumentos. A biblioteca rejeita assinaturas PURGE void/Void antes de executar o command; retorno nulo ou coleção vazia provoca erro e rollback. Falha ao gravar a outbox também reverte a remoção.

## Verificação

EnvironmentApiIT ganhou cenários de manutenção/restauração sob compatibilidade inativa, bloqueio de novos vínculos e exclusão em uso, criação idempotente e reativação com identifier preservado, autorrelação, ciclo na reativação, grafo/tipos alterados depois de um snapshot e disponibilidade atual de compatibilidade. O teste concorrente de criação de ciclo já existente permanece.

EnvironmentTypeCompatibilityAuditIT cobre snapshot de inativação, PURGE físico com snapshot/identifier corretos, ausência de evento para rejeição e rollback quando a outbox falha. EnvironmentTypePurgeIT verifica regressão da captura de tipos; SharedApiIT e LifecycleListingIT verificam integrações e consultas.

Validação final concluída: 90 testes unitários/arquiteturais e 81 testes de integração passaram, sem falhas. Log: /private/tmp/environment-compatibility-review-final.log. Nenhuma migration aplicada foi alterada nesta revisão.

Validação do novo contrato de retorno PURGE concluída: 22 testes unitários e 4 de integração da biblioteca platform-audit; 90 testes unitários/arquiteturais e 29 de integração do serviço, todos sem falhas. Integrações: EnvironmentApiIT, EnvironmentTypePurgeIT, EnvironmentTypeCompatibilityAuditIT e WorkspaceAuditIT. O teste da biblioteca comprova rollback da remoção quando o command retorna nulo. Logs: /private/tmp/platform-audit-purge-return-final.log e /private/tmp/environment-purge-return-verify.log. A biblioteca foi instalada somente no repositório Maven local.

## Coerência de tipo e compatibilidade — 2026-10-12

A criação e reativação de compatibilidades agora rejeitam o pai com `allowsChildren=false`, com erro no campo `parentTypeCode`. Também rejeitam pai que exige workspace com filho global (`workspaceRequired=false`), com erro no campo `childTypeCode`: por exemplo, CUSTOM → DEFAULT não representa uma hierarquia válida. Os tipos precisam existir e estar ativos antes dessas verificações; o grafo de ciclos é consultado somente depois da elegibilidade do par.

No update do tipo, `allowsChildren=false` exige ausência de compatibilidades ACTIVE em que ele aparece como pai, além da ausência de ambientes filhos de qualquer lifecycle. A consulta `findActiveChildLinkForTypeValidation` retorna no máximo um ID com `LIMIT 1 FOR SHARE`, verificando o estado atual sem carregar entidades de compatibilidade. Compatibilidades inativas e vínculos em que o tipo aparece apenas como filho não impedem essa configuração. O bloqueio vale também para um tipo que já esteja com a flag falsa, evitando conservar uma configuração antiga incoerente pela edição.

As verificações permanecem nos validators, dentro da transação dos commands e depois dos locks exclusivos dos tipos em ordem de código. Criar/reativar/inativar uma compatibilidade e alterar a configuração do tipo utilizam o mesmo conjunto de locks, coordenando essas mudanças. Não foram alterados schemas, migrations, rotas ou o tratamento de vínculos históricos de ambientes.

Mensagens específicas foram adicionadas em português e inglês: `environment.compatibility.scope-invalid` (ENVIRONMENT-143) e `environment.type.child-compatibilities-active` (ENVIRONMENT-144). A rejeição do pai que não permite filhos reutiliza `environment.parent-type.cannot-have-children` (ENVIRONMENT-141).

Nesta etapa, `mvn --offline --batch-mode --no-transfer-progress -DskipTests test-compile` passou, compilando 393 arquivos principais e 62 arquivos de teste; `git diff --check` também passou. As suítes de integração registradas acima são verificações anteriores; não foram executadas novamente para estas regras. Os cenários de elegibilidade, reativação e concorrência precisam de validação em integração.

## Fechamento do contrato atual

O purge de compatibilidade agora possui mensagens cadastradas nos dois idiomas para exigir INACTIVE (ENVIRONMENT-145) e rejeitar uso (ENVIRONMENT-146). A mensagem de uso inclui referências de ambientes inativos/quarentenados. O schema local foi alinhado à versão publicada pela V57, mantendo os códigos enumerados e as mesmas restrições de texto.

As fixtures de API usam a versão publicada com parentTypeCode/childTypeCode. Os testes de ciclo foram ajustados para SHARD/CELL com CELL permitindo filhos e sem a aresta inicial entre eles; assim, as duas tentativas são individualmente elegíveis e a rejeição verifica ciclo, não o escopo global proibido. A cobertura também exercita compatibilidade ativa impedindo allowsChildren=false, vínculos somente de entrada, reativação rejeitada enquanto o pai não permite filhos e purge impedido por referências em quarentena.

A verificação vigente e seus limites estão no [fechamento da revisão completa](ENVIRONMENT-FULL-CODE-REVIEW.md). Os resultados das seções anteriores são históricos.
