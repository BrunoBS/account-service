# Revisão de core/workspace

## Segurança e integridade

- `ResourceVisibility` protege detalhe, listagem e busca do agregado antes de editar, inativar, restaurar ou colocar em quarentena. A unicidade do nome continua global.
- Alterar tipo ou grupo autorizador exige `UserSession.isOwner()`. A regra compara os valores normalizados e não impede um ADM autorizado de editar os demais campos. Sessão ausente não autoriza alterações sensíveis.
- Tipos aceitos: ADMIN e MANAGER. A V44 desativa CATALOG no catálogo de tipos de workspace e adiciona uma constraint para impedir outros tipos.
- A versão é obrigatória na edição e seu conflito é verificado antes das consultas de catálogo e duplicidade.
- A constraint UNIQUE de nome protege requisições concorrentes. Duplicidades detectadas previamente recebem o erro de negócio de nome duplicado. Violações de integridade na gravação ou no flush usam o handler global da biblioteca de mensagens: HTTP 409, código GLOBAL-0004 e mensagem genérica, sem expor nomes de constraints, SQL ou valores do banco. A transação perdedora é revertida.
- Os aprovadores inalterados mantêm seus IDs. Remoções utilizam orphanRemoval com flush antes das substituições, permitindo troca de e-mails sem violar a unicidade.

## Schema e limites

Criação e edição usam o schema comum WORKSPACE/workspace na entrada HTTP via ResourceSchemaRequestBodyAdvice da biblioteca. O advice delega ao WorkspaceNormalizer, preservando tipos inválidos e campos desconhecidos para o schema rejeitar antes da desserialização. O validador Java contém regras de negócio, autorização das alterações sensíveis, versão e filtros da consulta.

Limites: nome 50, descrição 200, requester 10, sigla 5, grupo autorizador 10, e-mails 100, códigos de catálogo 20 e funcional do aprovador 10. Tags manuais têm até 50 caracteres e limite de 20 itens; aprovadores têm limite de 5 itens. Tags de sistema armazenam até 50 caracteres para comportar o nome do workspace. A V49 atualiza os tamanhos físicos e publica o schema Workspace v2, e o mapeamento de settings usa JSON.

A V42 publica a definição; schemas/workspace.json contém o contrato de referência. O teste de migration verifica a igualdade dos dois e os limites das colunas. A versão é opcional no schema comum, mas obrigatória na edição por regra de negócio.

## Desempenho e API

GET /api/v1/workspaces recebe Pageable, utiliza Page no core e retorna PagedModel do Spring Data. A resposta contém content e page (number, size, totalElements e totalPages). Foram removidos o parser manual e os três modelos próprios de paginação. Padrões: page 0, size 20; máximo 100 itens. O Spring resolve page, size e sort, com limite máximo configurado em application.yml. A consulta adiciona id como desempate na ordenação; filtros e paginação são aplicados no banco, incluindo filtro por tag com EXISTS, sem carregar todos os identificadores da tag em memória.

O repository retorna Page<Workspace> diretamente, sem busca intermediária de IDs nem montagem manual de página. A consulta paginada não faz fetch join de coleções; os aprovadores são carregados em lote com BatchSize 100 dentro da transação. As tags são obtidas em lote, e a ordem da página é mantida. Conteúdo e contagem respeitam ResourceVisibility. Resoluções de referências usam projeções sem carregar entidades ou coleções; referências operacionais exigem ACTIVE e referências históricas podem incluir recursos inativos ou em quarentena.

A restauração reutiliza as tags manuais carregadas. Os nomes abreviados de facade e controller foram substituídos por nomes legíveis. Workspace mantém um único agregado, portanto o pacote domain continua plano.

## Verificação

Os testes cobrem limites na criação/edição, permissões de owner e ADM, ausência de sessão, paginação com visibilidade e contagem, filtros de tags, aprovadores completos por página, concorrência de criação/renomeação, rollback da requisição perdedora, versões, referências, tags e auditoria. A regressão inclui aplicações com workspace pai inativo ou em quarentena.

A suíte completa clean verify passou: 81 testes unitários/arquitetura e 149 testes de integração (230 no total), sem falhas, erros ou testes ignorados. A formatação e git diff --check também passaram.

Após adotar Pageable, Page e PagedModel do Spring Data, passaram 81 testes unitários/arquitetura e 42 testes de integração de workspace e schema. Foram verificadas ordenação, limite máximo configurado, parâmetros tratados pelo resolver nativo, páginas vazias e contagem com visibilidade.

A consulta direta de Page<Workspace> passou em 81 testes unitários/arquitetura e 42 testes de integração de workspace. O teste de persistência confirma paginação no banco com fail_on_pagination_over_collection_fetch habilitado: três workspaces carregados e uma única consulta em lote para seus aprovadores, além da consulta de conteúdo e contagem (três consultas no total).

Correção de inicialização: V1–V36 com checksums divergentes foram restauradas ao conteúdo anterior à formatação, com os 35 checksums confirmados contra o log do banco. Migrations aplicadas devem permanecer imutáveis. Após autorização para recriar o banco local de teste, as migrations até V44 foram aplicadas com sucesso e a aplicação inicializou. A suíte completa passou com 81 testes unitários/arquitetura e 150 testes de integração (231 no total).

A V45 fixa a collation utf8mb4_unicode_ci na coluna workspaces.name. A constraint UNIQUE existente garante unicidade global sem distinguir maiúsculas/minúsculas, inclusive em requisições concorrentes e para nomes de workspaces inativos. A grafia enviada é preservada; editar somente o case do próprio nome é permitido. A collation mantém também a comparação sem distinção de acentos já utilizada pelo banco.

Verificação da unicidade sem case: 81 testes unitários/arquitetura e 41 testes de integração passaram, incluindo criação, edição, nome inativo e concorrência com grafias diferentes.

Criação e edição recebem CreateWorkspaceRequest/UpdateWorkspaceRequest tipados. O advice da biblioteca ResourceSchemaRequestBodyAdvice valida o JSON antes da desserialização; aprovadores usam ApproverRequest e traduzem para ApproverInput. Facade e command recebem inputs tipados; não fazem parsing JSON nem repetem a validação de schema.

Verificação dos requests tipados: clean verify passou com 255 testes (87 unitários/arquiteturais e 168 de integração), incluindo validação estrutural antes da desserialização, autorização, auditoria e concorrência. Log: `/private/tmp/typed-requests-full-verify.log`.
