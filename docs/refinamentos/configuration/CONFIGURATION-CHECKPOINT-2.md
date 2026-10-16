# Configuration V1 — checkpoint 2: domínio, persistência e CRUD

> Registro histórico anterior à renomeação. O contrato atual é [DynamicParameter / ParameterKey](../dynamic-parameter/DYNAMIC-PARAMETER.md).

Referência: `guideline-java/docs/refinamento/14-configuration/configuration-refinement-v1.md` e [plano aprovado](CONFIGURATION-CHECKPOINT-1.md).

Status: **checkpoint 2 concluído**, com verificação focada e regressão completa aprovadas. O usuário decidiu **manter as colunas JSON nativas do MySQL** após a demonstração de arredondamento decimal. A alternativa LONGTEXT não foi aplicada. Após a aprovação do usuário, publicação/Redis foram implementados no [checkpoint 3](CONFIGURATION-CHECKPOINT-3.md); este documento registra a entrega original do checkpoint 2.

## Implementação realizada

Na branch `feature/segments-categories-v1`, preservando segmentos e ajustes anteriores de persistência:

- Catálogos Golden ConfigurationType (`CONFIGURATION`, `EXPRESSION`) e ConfigurationStatusType (`DRAFT`, `PUBLISHED`), com code como PK, entidades/Code/Enum, repository, EnumCatalogService, facade e controller existentes reutilizados.
- V68 cria os catálogos e garante `platform` sem sobrescrever registro existente. A proteção contra inativação está no domínio de Microservice; seu código permanece imutável.
- V69 cria Configuration com UUID, versão técnica, FK do microsserviço, chave case-sensitive única por pai, tipo/status por code, JSON editável/publicado, tipo publicado e timestamps. Constraints protegem consistência de status, atividade e snapshot; não há cascade de exclusão.
- V70 registra os contratos iniciais. V71 publica a versão 2 de `PLATFORM/configuration` com o JSON Schema único enviado pelo usuário: criação/edição e expressão condicional no mesmo arquivo. O contrato independente de expressão é inativado; versões históricas são preservadas. Referências e schemas publicados são comparados no teste de upgrade. V72 publica a versão 3 com `multipleOf: 0.01` para o valor DECIMAL escalar e os itens das listas, conforme solicitação do usuário. V73 publica a versão 4 com chave somente em minúsculas e as condicionais if/then/else revisadas pelo usuário; também alinha a constraint de chave. Migrations anteriores não foram editadas.
- Domínio copia JsonNode na entrada, publicação e getters. Descrição isolada preserva PUBLISHED; edição de tipo/valor volta a DRAFT e preserva tipo/valor publicados. Inativação limpa o snapshot; reativação não publica.
- Commands separados de queries. Escritas usam saveAndFlush, transições repetidas não persistem nem geram evento adicional. Delete exige inatividade e retorna metadados internos para PURGE/auditoria antes de responder HTTP 204.
- Finders recarregam o estado atual sob lock, mesmo após snapshot/cache JPA anterior; update exige versão atual. Duplicidade usa validator e constraint/handler central, sem tratamento de driver/constraint no command.
- Microservice é acessado através de Port/Adapter e query pública do próprio módulo. Listagens usam projeções, paginação no banco e referências obtidas em lote; o teste registra três statements constantes e zero entity fetches.
- Contratos tipados por UUID, OWNER em todas as operações administrativas, mensagens pt-BR/en-US e validação do JSON original. ConfigurationWebSettings foi removido. Por solicitação do usuário, o serviço usa os defaults do Jackson, sem use-big-decimal-for-floats ou imposição global de FAIL_ON_UNKNOWN_PROPERTIES: o schema valida o JSON original e proíbe propriedades extras; o use case exige versão no PUT quando o corpo satisfaz somente o ramo de criação. O validator mantém apenas regras de negócio, sem segunda validação estrutural de expressão.
- Comandos/respostas de mutação e snapshots de auditoria não carregam `value`/`publishedValue`. Detalhe e página administrativos incluem o valor editável. ConfigurationPayloadLoggingFilter foi removido por solicitação explícita do usuário. O serviço reutiliza a sanitização existente da platform-observability: os conversores passam por LogSanitizers.sanitize(), que aplica DefaultLogSanitizer. O padrão da biblioteca mantém request-body logging desabilitado em PlatformObservabilityProperties; não há filtro/sanitizador específico da feature nem configuração redundante de logging no application.yml. O teste usa a configuração efetiva da biblioteca, verifica que o logging de body está desabilitado e renderiza o contexto pelo JsonMdcConverter real na thread HTTP para comprovar ausência de valores sentinela.

## Rotas disponíveis neste checkpoint

Base `/api/v1/configurations`:

| Operação | Contrato |
| --- | --- |
| POST `/` | `microserviceIdentifier`, `key`, `description` opcional, `type`, `value`; cria ativo/DRAFT, sem publicação. |
| PUT `/{identifier}` | `version`, `description` opcional, `type`, `value`; proíbe microservice/key/UUID/status/active. |
| GET `/{identifier}` | Detalhe administrativo, inclusive inativo. |
| GET `/` | Page com filtros microserviceIdentifier, status, type, active (true por padrão) e key exata/case-sensitive. |
| PATCH `/{identifier}/activate` e `/inactivate` | Idempotentes, com versão/evento preservados em repetição. |
| DELETE `/{identifier}` | Apenas inativa; 204, com auditoria e rollback transacional. |

Catálogos em `/api/v1/configuration-type` e `/api/v1/configuration-status-type`. Publish individual/lote, sincronização Redis e reconciliação pertencem ao checkpoint 3. O método puro de publicação da entidade é testado para preparar as invariantes; não existe endpoint/caso de uso de publicação ainda.

## Evidências

A revisão solicitada antes do checkpoint 3 unificou o schema, removeu ConfigurationWebSettings, ConfigurationPayloadLoggingFilter, configurações redundantes de logging e o override global do Jackson, além da validação separada no use case, e reforçou o teste de logs. A verificação focada abaixo foi aprovada com os defaults da biblioteca e do Jackson, incluindo centésimos e o schema v4 revisado pelo usuário.

- Compile: **BUILD SUCCESS**, sem novas dependências. Log `/private/tmp/configuration-compile.log`.
- Unitários/arquitetura: **123 testes aprovados**, sem falhas/erros/skips, incluindo cinco novos testes de domínio e três de arquitetura. Log `/private/tmp/configuration-unit.log`.
- Verify focado após a revisão de chaves e condicionais do schema v4: **BUILD SUCCESS**, **147 testes** (123 unitários/arquiteturais + 24 integrações: 17 API, 4 concorrência, 2 migration/prova de armazenamento de Configuration e 1 upgrade de Publisher), sem falhas, erros ou skips. Concluído em 14/10/2026 às 22:04:26 (America/Sao_Paulo), em 1min14s. O teste decimal comprova o resultado nativo aprovado em configuração escalar e expressão aninhada. Expressões inválidas em POST e PUT são rejeitadas pelo schema antes do validator do use case. O teste de centésimos cobre negativos, zero, inteiros, 0.29, 19.9/19.90/19.99, notação científica e rejeição de 19.901/-0.001/0.291/19.999 no escalar e nas listas. Rejeições preservam a versão técnica. ConfigurationPayloadLoggingFilter foi removido; o teste de logs usa o padrão de logging de body desabilitado da plataforma e renderiza JsonMdcConverter na thread HTTP. Log `/private/tmp/configuration-schema-focused.log` e relatórios Surefire/Failsafe.
- A regressão completa original do checkpoint foi aprovada com 510 testes antes desta revisão. As execuções completas iniciadas durante a revisão foram interrompidas para incorporar as alterações solicitadas durante o trabalho; o log `/private/tmp/configuration-full.log` passou a registrar a regressão completa aprovada do checkpoint 3. A evidência desta revisão está no verify focado acima, que executa unitários/arquitetura e as integrações de Configuration/upgrade de Publisher.
- API cobre JSON livre em todas as formas e literal null, key somente em minúsculas e única por pai, imutabilidade, versões, oito operadores, limites/erros de expressão, paginação, OWNER/401/403, i18n, idempotência e rollback de create/update/activate/inactivate/delete quando auditoria falha.
- Os 12 cenários HTTP de limites LONG escalares/listas passaram usando o schema publicado no banco, incluindo os vizinhos externos rejeitados.
- Concorrência: constraint quando pré-consultas passam; um vencedor por versão; versão atual depois de leitura antiga; update aguardando inativação sem restaurar publicação removida.
- Upgrade V67 → V71 → V73 preserva o microsserviço global inteiro, compara schemas e comprova FKs/unique/checks, SQL NULL versus JSON null e ausência de cascade. O teste começa sem INACTIVE no catálogo; V71 garante esse código sem sobrescrever registros existentes. O upgrade de Publisher (V49 → atual) também passou. As versões 1 e 2 de Configuration e o contrato antigo de expressão são preservados como histórico, e somente o contrato único de Configuration permanece exposto na view ativa.
- Valores sentinela não aparecem em mensagem/MDC/causa ou no contexto renderizado por JsonMdcConverter na thread HTTP, inclusive nos logs WARN/ERROR de expressão e JSON malformado, usando o request-body logging desabilitado por padrão pela biblioteca.

Os testes de integridade usam códigos MySQL apenas para comprovar constraints em teste; nenhum command interpreta SQL/1062/nomes de constraints. A classificação JDBC de CHECK em MySQL é UncategorizedSQLException, e o teste observa sua causa SQL, sem alterar o handler de produção.

## Revisão do schema alterado pelo usuário

- `key` aceita somente `^[a-z0-9]+(?:[._-][a-z0-9]+)*$`. A normalização aplica trim, sem converter maiúsculas silenciosamente. Dígitos e separadores ponto/underscore/hífen são aceitos; maiúsculas, letras acentuadas e separadores inválidos são rejeitados.
- Os novos if/then/else para STRING/INTEGER/LONG/DECIMAL preservam a semântica anterior: o bloco global de operadores exige array em CONTAINS/NOT_CONTAINS; cada ramo tipa os itens. Os demais operadores recebem escalares. Ordenação só aceita tipos numéricos; limites INTEGER/LONG e DECIMAL em centésimos permanecem.
- O meta-schema Draft 2020-12 validou o arquivo. Comparação com o schema v3 na engine networknt 3.0.7 confirmou equivalência das condicionais em **1.408 cenários** de criação/edição, cobrindo tipos, operadores e operandos válidos/inválidos. Artefatos de análise: `/private/tmp/ConfigurationSchemaReview.java` e `/private/tmp/configuration-schema-review.log`.
- V73 publica o arquivo revisado e altera o CHECK do banco em um ALTER TABLE atômico. O teste de migration comprova rejeição de maiúsculas no banco e preservação da versão publicada anterior. A fixture repõe a versão 4 pelo arquivo de referência, sem repetir DDL entre testes.
- Chaves antigas com maiúsculas não são renomeadas automaticamente, pois a chave é imutável. Se existirem numa instalação, precisam ser resolvidas antes de aplicar a constraint mais restrita; o banco rejeita a alteração com dados incompatíveis.

## Regra aprovada: DECIMAL em centésimos

O usuário solicitou `multipleOf: 0.01` no ramo DECIMAL do schema único. A regra se aplica ao operando escalar e aos itens das listas. Aceita inteiros, negativos e valores como 0.29, 19.9, 19.90 e 19.99; rejeita valores como 19.901, -0.001 e 0.291. Não arredonda nem impõe formatação da saída. CONFIGURATION permanece JSON livre.

V72 publica o contrato v3, sem alterar V70/V71 ou as versões publicadas. A migration não modifica valores já persistidos. POST e PUT validam pela biblioteca antes do use case; publicação futura revalidará o mesmo contrato vigente. Parser HTTP, converter JPA e logging continuam com os defaults solicitados pelo usuário.

## Decisão aprovada: precisão nativa da coluna JSON

O teste de round-trip HTTP → JPA → MySQL → HTTP envia:

```text
19.900000000000000000001
```

Na investigação inicial, o mapper Java estava com USE_BIG_DECIMAL_FOR_FLOATS habilitado. Mesmo assim, `SELECT value FROM configurations` e o GET retornam **19.9**. A falha é da representação numérica da coluna JSON nativa, não da validação LONG ou da configuração do parser.

Uma prova isolada em tabela temporária MySQL passou: para número escalar, array e objeto contendo o mesmo decimal, JSON nativo muda o valor e LONGTEXT com CHECK(JSON_VALID(...)) preserva o documento inteiro. O teste `ConfigurationMigrationIT.nativeJsonRoundsDecimalsWhileAValidatedTextDocumentPreservesThem` registra essa comparação sem modificar as colunas reais da feature.

Depois de receber essa evidência e a proposta alternativa, o usuário respondeu **“podemos seguir com o tipo JSON”**. O contrato de persistência passa a usar a representação numérica nativa do MySQL, sem garantir decimais arbitrários além dessa precisão. O teste `ConfigurationApiIT.usesNativeMysqlJsonDecimalPrecisionAsApproved` mantém a entrada original de alta precisão e verifica explicitamente o valor 19.9 efetivamente persistido/retornado, no valor livre. Após a decisão de restringir DECIMAL a centésimos, a expressão usa o operando válido 19.90 e comprova seu retorno como 19.9. Não há conversão dos números para strings nem troca de armazenamento.

O usuário solicitou remover a opção global use-big-decimal-for-floats; o parser HTTP e o converter JPA agora utilizam os defaults do Jackson. Um teste cobre alteração exclusiva da descrição preservando PUBLISHED para decimais em CONFIGURATION e EXPRESSION. Limites inteiros LONG continuam exatos e testados com literais inteiros. A publicação/reconciliação deve usar o valor persistido/publicado no banco, incluindo sua representação numérica nativa. O avaliador da biblioteca comparará os números publicados por BigDecimal, sem introduzir coerção de strings ou perda adicional durante a comparação.

## Alternativa analisada e não adotada

Trocar **somente `value` e `published_value` de JSON para LONGTEXT**, adicionando CHECK(JSON_VALID(value)) e CHECK(published_value IS NULL OR JSON_VALID(published_value)), e alinhar as duas anotações JPA. O converter já representa os documentos por strings precisas e mantém a distinção entre Java null/SQL NULL e NullNode/documento `null`.

Mantém-se o contrato HTTP em JSON, JsonNode na entidade, schema da expressão, MySQL como fonte de verdade, tipo/valor publicado separados e nenhuma coluna duplicada/cache/history. Os números não são convertidos em strings dentro do JSON do consumidor. O custo é alterar o tipo físico indicado no refinamento e abrir mão da representação binária/indexação nativa dos valores JSON; os filtros V1 não consultam o conteúdo desses documentos.

Proposta de patch revisável: `/private/tmp/configuration-exact-json-storage.patch`, **não aplicada por decisão do usuário**. V69 mantém JSON e não foi aplicada em instalação de produção; os bancos executados até aqui são isolados de teste.

Não foram adicionados limites numéricos de produto nem um formato paralelo de armazenamento. A manutenção do tipo físico JSON e de sua precisão nativa é a decisão do usuário que encerrou essa pendência.

## Arquivos e pendências

Produção: `foundation/catalog/configuration{,status}type`, `feature/configuration/{domain,repository,integration,usecase,facade}`, `entrypoint/web/configuration`, controllers dos catálogos, três schemas de referência (Configuration e settings dos dois catálogos), V68–V73, bundles e ajustes pontuais no domínio/query/repository de Microservice. Testes: domínio, arquitetura, API, concorrência, migration e fixture de schemas.

No checkpoint 2 não houve alteração de POM ou platform-libraries, commit/push/merge; o starter Redis foi adicionado posteriormente no checkpoint 3. O trabalho de segmentos permanece na mesma branch. A inativação/delete neste checkpoint removem o snapshot do banco; remoção Redis ainda depende do checkpoint 3. Não há runtime de Configuration neste estágio, portanto o checkpoint não é uma entrega completa da V1.

## Próximo checkpoint

Checkpoint 3: PublishById e PublishBatch com validação integral pelo mesmo contrato `PLATFORM/configuration` (envelope de edição contendo version/type/value), locks e transação única; sincronização idempotente Redis após commit, envelope com tipo e valor publicados, resposta separando confirmação no banco e resultado Redis; reconciliação horária de publicações, inclusive DRAFT com snapshot anterior, restauração de chaves e limpeza do namespace gerenciado. Testar falha Redis sem rollback fictício, recuperação e concorrência entre instâncias.

O usuário autorizou o checkpoint 3 após esta revisão; `spring-boot-starter-data-redis`, já previsto no plano apresentado, foi declarado sob o baseline gerenciado. Nenhuma biblioteca adicional de lock foi adicionada. A capability de leitura na platform-libraries continua sendo o checkpoint 4.
