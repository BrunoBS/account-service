> Documento histórico anterior à refatoração. Consulte [Dynamic Bundle](../dynamic-bundle/DYNAMIC-BUNDLE.md) para nomes e contratos atuais.

# Revisão de feature.message

Data: 2026-10-13. Critérios: [REVIEW-STANDARD.md](../../REVIEW-STANDARD.md).

Status: **revisão concluída no código e nos testes**. Contrato para consumidores e aplicação das migrations: [MESSAGE-INTEGRATION.md](MESSAGE-INTEGRATION.md).

## Escopo e resultado

Revisão dos dois fluxos completos: `MessageController → MessageFacade → MessageCommandService/MessageQueryService` e `MessageTranslationController → MessageTranslationFacade → MessageTranslationCommandService/MessageTranslationQueryService`. Inclui requests/responses, finder, normalizer, mapper, validator, domínio, repositórios, integração pública com microsserviços, schemas, migrations e auditoria.

Os achados M01–M09 foram corrigidos no workspace. A segunda passagem revisou todos os métodos dos dois controllers/facades e os commands/queries, melhorando nomes e legibilidade. A exclusão física está implementada. Foi mantida a regra de atualização separada: version numérica identifica a condição do schema que rejeita translations no update; o arquivo de referência foi alinhado à V58, sem alterar a migration publicada. Mensagens e traduções agora possuem somente ACTIVE/INACTIVE e DELETE físico. As alterações ainda dependem do processo normal de commit e implantação.

## Contrato HTTP e fluxo de dados

Todas as 14 operações exigem autorização `ADM`, com a ação correspondente na facade. O serviço de autorização decide a permissão a partir da sessão confiável; não há ator ou nível de acesso definido pelo body. Controllers traduzem requests tipados e outputs em responses, sem acessar banco nem decidir regras de negócio.

Para os bodies: árvore JSON original → `MessageNormalizer` → schema publicado pela biblioteca → request tipado → input → facade autorizada → command. A normalização preserva campos desconhecidos e tipos originais, inclusive em traduções inline, para que o schema rejeite o payload inválido antes do binding.

| Mensagem: rota `/api/v1/messages` | Método Java em controller/facade/use case | Regra e resposta |
| --- | --- | --- |
| POST coleção | `create` | Microsserviço ACTIVE; chave/código únicos dentro dele; cria mensagem ACTIVE e traduções opcionais na mesma transação; 201 |
| GET coleção | `findAll` | Filtros `microserviceIdentifier`, `lifecycle`, `code`, `messageKey`; página com conteúdo e metadados; 200 |
| GET `/{identifier}` | `findByIdentifier` | Detalhe administrativo de ACTIVE/INACTIVE; 200 ou 404 |
| PUT `/{identifier}` | `update` | Mensagem ACTIVE, versão enviada igual à atual e microsserviço ACTIVE; 200 |
| PATCH `/{identifier}/activate` | `activate` | Somente INACTIVE → ACTIVE; 200 |
| PATCH `/{identifier}/inactivate` | `inactivate` | Somente ACTIVE → INACTIVE; preserva traduções; 200 |
| DELETE `/{identifier}` | `delete` | Mensagem INACTIVE; remove fisicamente a mensagem e todas as traduções, inclusive ACTIVE; 204 |

| Tradução: rota `/api/v1/messages/{messageIdentifier}/translations` | Método Java em controller/facade/use case | Regra e resposta |
| --- | --- | --- |
| POST coleção | `create` | Mensagem ACTIVE; locale único dentro da mensagem; cria tradução ACTIVE; 201 |
| GET coleção | `findAll` | Escopo da mensagem; filtros `locale` e `lifecycle`; página; 200 |
| GET `/{translationIdentifier}` | `findByIdentifier` | Escopo da mensagem, inclusive histórico; identificador de outra mensagem retorna 404 |
| PUT `/{translationIdentifier}` | `update` | Tradução ACTIVE e versão atual; mensagem existente pode estar ACTIVE ou INACTIVE; 200 |
| PATCH `/{translationIdentifier}/activate` | `activate` | Tradução INACTIVE e mensagem ACTIVE; 200 |
| PATCH `/{translationIdentifier}/inactivate` | `inactivate` | Tradução ACTIVE e mensagem existente; 200 |
| DELETE `/{translationIdentifier}` | `delete` | Tradução INACTIVE e mensagem existente; remove fisicamente somente a tradução; 204 |

Falhas de regra/estrutura retornam 400; versão desatualizada ou conflito concorrente de integridade retorna 409; falta de autenticação retorna 401 e autorização negada retorna 403. Respostas não expõem entidades JPA, IDs internos, SQL nem nomes de constraints.

### Paginação e filtros

As duas listagens passaram de array para `PagedModel`, seguindo o padrão Spring Data dos módulos revisados. Consumidores devem ler `content` e `page`, com paginação por `page`, `size` e `sort`.

```json
{
  "content": [],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

`LifecycleFilter` permanece compartilhado: `ACTIVE`, `INACTIVE` ou `QUARANTINED`; ausência significa ACTIVE e valores inválidos são rejeitados. Nesta feature só existem ACTIVE/INACTIVE: o filtro QUARANTINED retorna página vazia. Predicados e contagem são aplicados pelo banco antes da paginação.

| Recurso | Ordem padrão | Propriedades de sort permitidas |
| --- | --- | --- |
| Mensagem | `messageKey`, depois `id` | `id`, `identifier`, `messageKey`, `code`, `httpStatus`, `createdAt`, `updatedAt` |
| Tradução | `locale`, depois `id` | `id`, `identifier`, `locale`, `title`, `createdAt`, `updatedAt` |

A ordenação recebe `id` como desempate se ele ainda não estiver presente. São rejeitados pedidos sem paginação, tamanho acima de 100, offset acima de `Integer.MAX_VALUE` e propriedades fora da lista permitida. Não há paginação manual ou carregamento integral para filtrar em memória.

## Regras de lifecycle e referências

| Estado da mensagem | Editar mensagem | Nova tradução / ativar tradução | Editar tradução ACTIVE / inativar / excluir tradução INACTIVE | Consultar histórico |
| --- | --- | --- | --- | --- |
| ACTIVE | Sim, com versão atual | Sim | Sim, respeitando o estado próprio da tradução | Sim |
| INACTIVE | Não | Não | Sim | Sim |

As entidades protegem as transições mesmo quando chamadas fora do HTTP. Ativar registro já ACTIVE ou inativar registro já INACTIVE é rejeitado. A entidade valida INACTIVE antes da exclusão; o command preserva o snapshot, remove fisicamente e faz flush. Chamadas posteriores ao identificador removido retornam 404. Não há transição para quarentena. A exclusão da mensagem exige apenas a inatividade dela, removendo todas as traduções; a exclusão isolada exige a inatividade da tradução, sem exigir mensagem inativa.

Inativar a mensagem preserva traduções e seus estados. Inativar o microsserviço preserva mensagens e traduções. A view de runtime exige microsserviço, mensagem e tradução ACTIVE; a leitura administrativa continua permitindo referências históricas. A elegibilidade ACTIVE do microsserviço é exigida para create/update de mensagem, não para consultar histórico.

Microsserviço, chave e código da mensagem continuam editáveis quando ela está ACTIVE, respeitando unicidade e versão. A mudança de microsserviço/chave altera a chave de publicação em runtime; as traduções continuam vinculadas à mesma mensagem.

## Revisão por classe e método

| Local | Implementação e avaliação |
| --- | --- |
| Ambos os controllers: `create/update` | Apenas `request.toInput()`, chamada de facade e conversão de response; schema antes do binding; HTTP 201/200 |
| Ambos os controllers: `findAll/findByIdentifier` | Nomes mantidos por expressarem intenção; `PagedModel` e responses próprios; nenhuma entidade retornada |
| Ambos os controllers: `activate/inactivate/delete` | Delegação direta; DELETE 204; sem regra de lifecycle no controller |
| Ambas as facades: todos os sete métodos | Autorização ADM por ação e delegação ao command/query; nomes completos e parâmetros legíveis |
| `MessageCommandService.create` | Normaliza, valida uma vez, resolve microsserviço elegível, usa mapper de detalhes, salva/flush, cria traduções auditáveis e retorna snapshot |
| `MessageCommandService.update` | Carrega estado atual sob lock, normaliza, valida estado/versão/referência/unicidade, atualiza e faz flush antes do output |
| `MessageCommandService.activate/inactivate` | Lock da mensagem, transição na entidade, flush e output para auditoria |
| `MessageCommandService.delete` | Lock do pai, exige INACTIVE, preserva snapshot, remove/audita traduções em lote e depois remove/flush da mensagem; retorno é o objeto removido |
| `MessageCommandService.toOutput` | Resolve identificador público do microsserviço apenas quando necessário; create/update reaproveitam o input validado |
| `MessageTranslationCommandService.create` | Lock do pai, valida elegibilidade e locale único, cria via detalhes e faz save/flush |
| `MessageTranslationCommandService.update` | Lock do pai e depois da tradução escopada; valida versão, estado e unicidade; update/flush/output |
| `MessageTranslationCommandService.activate/inactivate` | Mesma ordem de locks, escopo por pai e transições protegidas na entidade |
| `MessageTranslationCommandService.delete` | Lock do pai/tradução, exige tradução INACTIVE, preserva snapshot, delete/flush e retorna objeto removido |
| `MessageTranslationCommandService.deleteAllForMessage` | Recebe snapshots atuais não vazios, com pai/traduções já bloqueados; exige pai INACTIVE, remove lote e emite PURGE por tradução; não exige filhos INACTIVE. Se não há traduções, o command da mensagem não invoca um PURGE de coleção vazia |
| `MessageTranslationCommandService.createAllForNewMessage` | Entrada interna para mensagem recém-persistida e inputs já normalizados; valida locales duplicados e versões de criação; salva lote e retorna snapshots auditáveis. Chamada entre beans, preservando o aspect |
| `MessageQueryService.findByIdentifier` | Leitura administrativa e resolução da referência pública, sem exigir microsserviço ativo |
| `MessageQueryService.findAll` | Valida paginação, ordena com desempate, normaliza filtros e pagina no banco; resolve identificadores de microsserviços em lote |
| `MessageTranslationQueryService.findAll` | Busca pai, pagina apenas traduções dele e aplica filtros no banco |
| `MessageTranslationQueryService.findByIdentifier` | Busca por identificador público e ID do pai; impede leitura de tradução de outra mensagem |
| `MessageFinder.findMessage/findTranslation` | Busca de leitura, com erro NotFound de domínio e escopo explícito |
| `MessageFinder.findMessageForUpdate/findTranslationForUpdate` | Resolve identidade técnica, descarta referência gerenciada antiga e lê estado atual com lock de escrita |
| `MessageFinder.findTranslationSnapshotsForDeletion` | Pai já bloqueado; lê projeções escalares atuais das traduções com lock, sem reutilizar entidades antigas do Hibernate |
| `MessageNormalizer.normalizePayload/normalize` | Normalização textual previsível; árvore mantém desconhecidos/tipos inválidos; DTOs também são normalizados para chamadas internas |
| `MessageNormalizer.normalize*Filter` | Filtros coerentes com o armazenamento; locale usa parser estrito BCP 47, aceitando script e normalizando `_` para `-` |
| `MessageMapper.toDetails` | Conversão pura, sem regra nem acesso ao banco; create/update compartilham `MessageDetails` e `MessageTranslationDetails` |
| `MessageValidator.validateForCreate/validateForUpdate` | Unicidade encapsulada; referência elegível resolvida uma vez e seu ID retornado ao command; sem repetição dos limites do schema |
| `MessageValidator.validateTranslationForCreate/Update/validateInlineTranslations` | Pré-condições da operação e duplicidade de locale, excluindo a própria tradução no update |
| `MessageValidator.validateVersion` | Compara versão enviada pelo cliente; `@Version` continua protegendo a gravação. São proteções complementares |
| `MessageValidator.validateMessagePagination/validateTranslationPagination` | Limites de paginação e lista explícita de sort |
| `Message` e `MessageTranslation` | UUID público imutável, PK interna, `@Version`; métodos de domínio protegem lifecycle. Tradução possui associação lazy; removida coleção bidirecional não utilizada |
| Repositories: `findFiltered` | Page/contagem no banco e parâmetros de query; aliases legíveis |
| Repositories: `exists*` | Consulta específica de existência, sem carregar a tabela inteira; constraints permanecem como proteção concorrente final |
| Outputs/responses | Conversão pura; snapshot de tradução inclui identificador do pai; response HTTP da tradução mantém seu contrato de detalhe |

Nomes genéricos foram substituídos onde havia ambiguidade: `messageIdentifier`, `translationIdentifier`, `messageId`, `translationId`, `savedMessage`, `messageRepository`, `translationRepository`, `microserviceIdentifiers`, `sortedPageable`, `excludedMessageId` e `excludedTranslationId`. `resolveActiveMicroserviceId`, `rejectVersionOnCreate` e `requireVersionForUpdate` explicitam a intenção das validações auxiliares. Não foram criadas camadas genéricas para pequenas sequências locais.

## Integridade, concorrência e desempenho

Relacionamentos: `platform_microservices → messages.microservice_id → message_translations.message_id`. Message acessa somente seus próprios repositories; a integração com platform utiliza `MicroserviceQueryService` público.

| Invariante | Proteção |
| --- | --- |
| Chave e código únicos por microsserviço | Pré-validação encapsulada e constraints únicas, inclusive para registros inativos; chave/código/locale ficam livres após a exclusão física |
| Locale único por mensagem | Normalização estrita, pré-validação e constraint; lote inline também detecta repetição antes de salvar traduções |
| Referências persistentes válidas | FKs; IDs internos não são aceitos no contrato público |
| Limites e tipos HTTP | Schema original antes do binding; UUID de microsserviço, tamanhos de texto, HTTP status 100–599, máximo 100 traduções inline |
| Concorrência com mudança do microsserviço | Leitura de ID escalar elegível com lock compartilhado, mantido até o commit de create/update |
| Concorrência entre pai e tradução | Escritas obtêm primeiro lock da mensagem e depois da tradução; mudança do pai e criação/alteração de filho ficam coordenadas |
| Estado já carregado pelo Hibernate | Finder descarta referência anterior antes da leitura com lock; os snapshots da cascata são projeções escalares sob lock, sem reutilizar entidades antigas; testes reais cobrem ambos sob isolamento do MySQL |
| Lifecycle de mensagem/tradução | V59 preserva legados em INACTIVE e adiciona CHECK que aceita somente ACTIVE/INACTIVE |
| Exclusão em cascata | Remoção explícita em lote permite auditar cada tradução; FK ON DELETE CASCADE continua protegendo a integridade do banco |
| Concorrência de versão | Comparação da versão do cliente com estado atual e `@Version` no flush |
| Erro de integridade concorrente | Handler compartilhado traduz conflito sem SQL no contrato de erro |

Commands usam apenas `@Transactional`, sem override de isolation nem `Propagation.MANDATORY`. Queries usam `readOnly = true`. Leituras comuns do MySQL não utilizam os locks de escrita dos commands. Escritas na mesma mensagem são serializadas; mensagens diferentes podem prosseguir em paralelo. O lock compartilhado de microsserviço admite leitores simultâneos e faz uma alteração concorrente desse registro esperar pela gravação dependente.

Na listagem de mensagens, as referências de microsserviços são resolvidas em uma consulta em lote por página, evitando N+1. O teste com dois microsserviços distintos e três mensagens confirmou três statements: conteúdo, contagem e referências. Nenhum índice ou cache foi adicionado sem evidência; não foi feito teste de carga de produção.

## Schemas e migração

Arquivos de referência: `src/main/resources/schemas/message.json` e `message-translation.json`. A nova `V58__message_resource_schemas.sql` publica os dois contratos e registra a configuração `MESSAGE`, códigos `message` e `message-translation`. A fixture de testes utiliza a mesma migration; o teste de upgrade compara o JSON persistido com os arquivos de referência e confirma preservação dos registros.

Um schema por recurso atende create/update. No create, `version` não pode conter um valor; no update, ela é obrigatória e deve ser atual. Traduções inline pertencem somente ao create: a condição allOf do schema rejeita sua presença quando há version numérica. A rota define a operação: enviar version numérica em POST não transforma create em update; o validator de create rejeita esse valor com 400 antes de consultar a referência ou gravar. version null equivale a ausência no create; versões ausentes/nulas em update são rejeitadas pelo validator. A migration V58 precisa ser aplicada para disponibilizar os contratos no ambiente. A nova `V59__message_physical_deletion_lifecycle.sql` converte os legados QUARANTINED em INACTIVE, incrementando versão e atualizando data, sem excluir nenhum dado; adiciona CHECK ACTIVE/INACTIVE nas duas tabelas. O teste de upgrade V58 → V59 cobre preservação, rejeição de quarentena e a cascata do banco.

## Auditoria e atomicidade

Commands de edição/transição retornam output após flush. Nos dois DELETEs, o snapshot é preservado antes da remoção, o banco recebe delete/flush e o command retorna o objeto removido para o aspect. A facade descarta o output para HTTP 204. A auditoria usa `AuditAction.PURGE`, eventos `MESSAGE_PURGED` e `MESSAGE_TRANSLATION_PURGED`; o snapshot mantém estado e versão anteriores à remoção, sem criar uma versão de quarentena.

Criação inline chama outro bean auditável e emite um evento para cada tradução, com identificador próprio, identificador da mensagem e conteúdo inicial, além do evento de mensagem. Todos os dados e eventos de outbox ficam na mesma transação. O teste força falha no terceiro append, após dois eventos de tradução, e confirma rollback de mensagem, traduções e todos os eventos. Update e exclusão física também têm rollback integral quando a auditoria falha. Na cascata, um teste faz falhar o evento da mensagem depois dos dois eventos de tradução: todas as linhas e eventos parciais são restaurados. Cada tradução removida registra estado, versão, conteúdo e identificador do pai, mesmo quando estava ACTIVE.

## Achados resolvidos

| ID | Severidade original | Correção | Evidência de validação |
| --- | --- | --- | --- |
| M01 | Alta | Guardas de estado no domínio; exclusão física exige INACTIVE e identificadores excluídos retornam 404 | Unitários, API e auditoria |
| M02 | Alta | Elegibilidade do pai e coordenação de escrita com locks/estado atual | API e concorrência real |
| M03 | Média | Validação de árvore original pela biblioteca, dois schemas, V58 e fixture | API estrutural e upgrade de schema |
| M04 | Média | Parser estrito de locale, sem truncar sufixos inválidos; suporte a script | Unitários e API |
| M05 | Média | Validação única, resolução de referência uma vez e unicidade encapsulada | API de duplicidade/versão/referência |
| M06 | Média | Paginação nas duas consultas e referências em lote | API e estatísticas Hibernate |
| M07 | Média | Eventos individuais das traduções inline na mesma transação | Snapshots e rollback após append parcial |
| M08 | Baixa | Textos pt_BR/en de DELETE descrevem remoção física somente após inativação | Revisão dos bundles e fluxos de erro |
| M09 | Baixa | Nomes explícitos, imports próprios e assinaturas legíveis | Compilação, arquitetura e regressões |

## Verificação e limites

Comando da verificação final:

```shell
mvn --offline --batch-mode --no-transfer-progress \
  -Dit.test=MessageApiIT,MessageAuditIT,MessageConcurrencyIT,MessageQueryIT,MessageResourceSchemaMigrationIT,MessagePhysicalDeletionMigrationIT,MessageServiceRelationMigrationIT,LifecycleListingIT,PlatformApiIT,PlatformQueryIT \
  clean verify
```

Log: `/private/tmp/message-physical-deletion-verify.log`.

Resultado: **BUILD SUCCESS**, **135 testes** (95 unitários/arquitetura + 40 integrações), sem falhas, erros ou skips. Tempo: 1min38s. A execução final cobre os dois fluxos completos, exclusão física com e sem traduções, auditoria e rollback da cascata, snapshots atuais após leitura anterior, autorização, versões, schemas e upgrades V57 → V58 e V58 → V59. Integrações executadas com MySQL real de teste fora do sandbox. `git diff --check` do escopo também passou. Não restam pendências identificadas no escopo desta revisão.

O contrato de atualização foi confirmado: PUT edita apenas atributos da mensagem; traduções são editadas nos endpoints próprios. O arquivo de schema e a versão publicada na V58 contêm a mesma condição de version/translations.

| Suíte de integração | Cenários |
| --- | --- |
| `MessageApiIT` | Fluxos dos 14 endpoints, estados inválidos, histórico, escopo de tradução, duplicidade, payloads desconhecidos/tipos inválidos, versão, locale, paginação, autorização negada e autenticação |
| `MessageAuditIT` | Snapshots dos dois recursos, eventos inline, versões após flush, rollback completo mesmo após eventos parcialmente gravados |
| `MessageConcurrencyIT` | Espera pelo lock do pai, releitura de pai/tradução previamente carregados, versão stale, snapshot atual da cascata e elegibilidade de microsserviço até commit |
| `MessageQueryIT` | Paginação/count e eliminação do N+1 por referências em lote |
| `MessageResourceSchemaMigrationIT` | Upgrade V57 → V58, igualdade dos contratos publicados e preservação de dados |
| `MessagePhysicalDeletionMigrationIT` | Upgrade V58 → V59 preserva legados como INACTIVE, restringe lifecycle e mantém ON DELETE CASCADE |
| `MessageServiceRelationMigrationIT` | Migração da relação com microsserviço |
| `LifecycleListingIT` | Regressão do filtro centralizado e respostas paginadas de message |
| `PlatformApiIT` / `PlatformQueryIT` | Regressão da integração com platform |

A execução inclui todos os testes unitários e de arquitetura do projeto, com a seleção acima de integrações. A suíte integral de integrações de todas as features não foi executada porque esta revisão está delimitada a message e suas regressões diretas. Não há benchmark de carga ou validação em produção. Esses limites não substituem o pipeline normal da implantação.

## Fechamento

| Item | Resultado |
| --- | --- |
| Controllers → facades → commands/queries | Dois fluxos revisados; achados M01–M09 resolvidos |
| Contratos e exemplos para consumidores | Guia de integração publicado e vinculado no README |
| Consumidores presentes no repositório | MessageApiIT, MessageAuditIT e LifecycleListingIT adaptados; não há frontend ou SDK cliente neste repositório |
| Migrations | V58/V59 preparadas para o Flyway habilitado; banco limpo e upgrades cobertos pelos testes |
| Banco do ambiente de implantação | Aplicação pelo Flyway na inicialização do serviço; esta revisão aplicou as migrations somente nos bancos de teste |
| Exclusão física e auditoria | Inatividade, cascata, snapshots atuais, eventos PURGE e rollback integral validados |
| Verificação final | 135 testes aprovados; git diff --check sem erros |

Os ajustes finais de fechamento alteram apenas a documentação e seus links. A evidência dos testes acima permanece válida; não houve mudança de comportamento após a verificação final.
