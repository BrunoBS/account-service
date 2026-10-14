# Revisão de Publisher

Referência: `docs/REVIEW-STANDARD.md`. Escopo: todas as classes de `core/publisher`, seus contratos HTTP, persistência e schemas.

## Contrato e decisões

- Recurso global de plataforma: leitura com policy `OPEN` e autenticação; escrita com policy `OWNER`. Não possui grupo autorizador ou vínculo com workspace. Não aplicar `ResourceVisibility` destinado a recursos restritos por grupo a esse cadastro global.
- Um único arquivo `src/main/resources/schemas/publisher.json` atende cadastro e edição. `code` é obrigatório nas duas operações e imutável após o cadastro. No cadastro, só são aceitos `KAFKA`, `HTTP` ou `APPCONFIG`; no update, o valor permanece imutável. `version` é rejeitada na criação e obrigatória na edição pelo validator de negócio.
- A biblioteca valida o JSON original, normalizado antes da desserialização. `settings` obrigatório como objeto; as condições `if/then` no schema validam KAFKA, APPCONFIG e HTTP, incluindo propriedades desconhecidas e tipos originais.
- Não há `$defs`, extração de sub-schemas ou seleção adicional de schemas de settings pelo validator. O contrato completo é publicado uma vez e a biblioteca realiza sua validação.
- O JSON Schema é a fonte de validação do código e enumera `KAFKA`, `HTTP` e `APPCONFIG`, com `maxLength: 50`. O validator não duplica essa regra e o banco não restringe o formato nem os valores do código.
- `deprecated` é independente do lifecycle: criação assume `false` quando ausente; edição preserva o valor atual quando ausente. `null` explícito é rejeitado pelo schema.
- Unicidade global do código inclui registros inativos e em quarentena. A constraint existente protege concorrência; o handler da plataforma traduz violações sem expor SQL ou nomes internos.
- Transições permitidas: ACTIVE → INACTIVE; INACTIVE → ACTIVE ou QUARANTINED. Registros em quarentena não são restaurados nem editados.
- Listagem usa `Pageable`/`Page` do Spring e retorna `PagedModel`, substituindo o array integral. Filtro de estado padronizado como `lifecycle=ACTIVE|INACTIVE|QUARANTINED`; `scope` preservado. Ordenação padrão por código e whitelist de propriedades ordenáveis.

## Limites

| Campo | Limite | Observação |
| --- | --- | --- |
| identifier | 36 | UUID gerado pelo servidor |
| code | 50 | Identidade funcional imutável; requisições aceitam apenas os três códigos listados |
| name | 50 | Mínimo 3; alinhado a Workspace/Application |
| description | 200 | Obrigatória; alinhada a Workspace/Application |
| publisher_scope | 50 | Compatível com a coluna do catálogo referenciado |
| lifecycle_code | 50 | Compatível com a coluna do catálogo referenciado |
| settings | JSON | `JsonNode`, obrigatório, cópia defensiva na entidade |
| KAFKA.topic | 249 | Limite do contrato de configuração |
| KAFKA.address | 1000 | Permite configuração de endereços de brokers |
| APPCONFIG.arn | 2048 | Limite do contrato de configuração |
| APPCONFIG.name | 64 | Limite do contrato de configuração |
| HTTP.clientId | 100 | Limite do contrato de configuração |
| HTTP.timeout | 1–60.000 ms | Limite superior adotado para o contrato HTTP |

Redução de `description` entra na migration V50. V51 amplia a coluna `code` a 50, remove a constraint de formato do banco e publica a versão 2 do mesmo schema, com os três códigos enumerados e `HTTP.timeout` limitado a 1–60.000 ms. Toda validação de entrada do código fica no schema; códigos antigos são preservados. Migrations anteriores permanecem como estavam antes desta revisão. Um banco existente com descrição acima de 200 deve adequar o dado antes do upgrade; a migration não trunca dados silenciosamente.

## Achados por classe e método

| Classe / métodos | Achado e ajuste | Verificação |
| --- | --- | --- |
| PublisherController: create/update | Requests tipados anotados com a capability da biblioteca; nomes de parâmetros legíveis. Sem parsing ou regras no controller. | API: tipos originais, campos desconhecidos, limites e code imutável |
| PublisherController: findAll | Listagem integral substituída por paginação Spring; resposta e contagem filtradas no banco. | API: paginação, filtros, ordenação inválida |
| PublisherController: find/inactivate/restore/delete | HTTP mantém 200/204 conforme representação da operação. | API: ciclo de vida, inexistentes, autorização |
| CreatePublisherRequest/UpdatePublisherRequest: toInput | Conversão direta de JSON tipado; `settings` permanece JsonNode. Edição recebe code obrigatório. | API: contratos de criação e edição |
| PublisherResponse: from | Removidos mapper JSON estático e parsing de String. | API: round trip de settings |
| PublisherFacade: todos | Policies explícitas preservadas; parâmetros e dependências com nomes legíveis. | API: policy OWNER enviada, escrita negada, leitura OPEN autenticada |
| PublisherCommandService: create/update | Uma chamada agrupada ao validator; mapper cria detalhes; nenhum parsing nem segunda validação estrutural. | API: settings condicionais, concorrência, conflito de versão |
| PublisherCommandService: inactivate/restore/delete | Variáveis legíveis; snapshots retornados ao mecanismo de auditoria, inclusive quando HTTP é 204. | API: auditoria e estados finais |
| PublisherMapper: toDetails | Conversão pura para detalhes compartilhados entre cadastro e edição; ausência de deprecated tem semântica explícita. | API: cadastro/edição |
| PublisherValidator: validateForCreate | Rejeita `version` na criação e valida unicidade e escopo; a lista permitida de códigos é responsabilidade do JSON Schema. | API: código inválido pelo schema, duplicidade e escopo inválido |
| PublisherValidator: validateForUpdate | Versão obrigatória e comparação otimista; rejeição de mudança do code; catálogo ativo. | API: conflito de versão, code imutável |
| PublisherValidator: validateQuery | Whitelist de sort, proteção de offset e validação de escopo quando informado. | API: paginação e sort inválido |
| PublisherNormalizer: normalizePayload | Preserva campos desconhecidos e tipos originais; normaliza apenas textos conhecidos. | API: números em campos textuais e booleans em strings |
| PublisherNormalizer: normalizeScope | Trim e uppercase consistentes em filtros. | API: filtro por escopo |
| PublisherFinder: findActive/findInactive/findInactiveForDeletion/find | Uma consulta por identificação; invariantes de restauração e exclusão pertencem à entidade. | API e testes de domínio |
| PublisherQueryService: find/findAll | Transações read-only, paginação no repositório e sem regras duplicadas de escopo. | API: filtros, conteúdo e contagem |
| PublisherRepository: findByIdentifier/existsByCode/findByLifecycle/findByLifecycleAndScope | Optional/exists/Page adequados; consulta única de página e contagem gerida pelo Spring, sem fetch de coleções. | API e teste concorrente |
| Publisher: construtor/update/applyDetails | PublisherDetails reduz repetição de atributos; settings é JSON nativo com cópia defensiva. Update direto exige ACTIVE. | Testes de domínio e round trip real |
| Publisher: inactivate/restore/quarantine/requireRestorable/requireDeletable | Guardas explícitas impedem transições inválidas sem criar máquina de estados genérica. | Testes de domínio e API |
| PublisherDetails / inputs / output | Tipos explícitos, referências de catálogo via VO no domínio, nenhum String de JSON ou var. | Compilação e arquitetura |
| PublisherMessageKeys | Erros de negócio no domínio; removidas chaves sem uso para validação estrutural. Erros de paginação e code imutável localizados. | API |
| V50–V51 / publisher.json / SchemaDefaultFixture | Um único contrato publicado corresponde ao arquivo de referência; V51 publica v2 com enum fechado. Upgrade preserva registros de Publisher. | PublisherMigrationIT e comparação de schema na API |

## Validação

- O baseline desta revisão passou nos comandos focados e em `clean verify` antes dos últimos ajustes de enum/version. Não foram reexecutados testes após V51 e a atualização das fixtures.
- Após os últimos ajustes: `mvn --offline --batch-mode --no-transfer-progress -DskipTests compile` passou; schema JSON parseado e `git diff --check` sem problemas de whitespace.
- Confirmado: somente `publisher.json` como arquivo de referência do recurso; nenhuma extração de `$defs` ou implementação paralela de validação.

Revisão concluída no escopo definido. Alterações do serviço permanecem disponíveis para revisão, sem commit ou push nesta etapa.
