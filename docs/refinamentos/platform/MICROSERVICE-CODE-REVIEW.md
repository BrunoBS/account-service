> Documento histórico. Consulte [Service Context](../service-context/SERVICE-CONTEXT.md) para nomes e contratos atuais.

# Revisão de Platform — Microservices

> Registro da revisão anterior à separação dos quatro fluxos. A estrutura e os contratos atuais, incluindo a retirada de `featureIdentifiers` do cadastro de contexto, estão em [Platform — separação de responsabilidades](PLATFORM-STRUCTURE-REFACTOR.md). As evidências abaixo são históricas.


Data: 13/10/2026. Critérios: [padrão de revisão](../../REVIEW-STANDARD.md).

## Escopo e arquitetura

Revisão das sete operações de `/api/v1/platform/microservices`, incluindo entrada HTTP, autorização, facade, command/query, validação de negócio, domínio, persistência, schema e auditoria. Features e contexts terão revisões próprias; nesta etapa, features receberam somente os ajustes necessários para garantir a integridade dos vínculos com microservices.

| Camada | Responsabilidade |
| --- | --- |
| `MicroserviceController` | Recebe requests tipados e paginação; traduz outputs para HTTP. |
| Requests + `MicroserviceNormalizer` | Validam a árvore JSON original pela capability da biblioteca; normalizam somente textos conhecidos. |
| `MicroserviceFacade` | Exige policy `OWNER` em leitura e escrita; delega para o caso de uso. |
| `MicroserviceCommandService` | Coordena transação, busca com lock, validação, mutação, flush e snapshot. |
| `MicroserviceQueryService` | Aplica o filtro central de lifecycle; usa paginação, projeções e consultas de existência. |
| `MicroserviceValidator` | Verifica versão, unicidade, imutabilidade do código, settings configuráveis e impedimentos de exclusão. |
| `MicroserviceFinder` | Separa leitura ativa da busca atual com lock para escrita. |
| `MicroserviceMapper` | Constrói `MicroserviceDetails` e produz output, convertendo settings em objeto JSON. |
| `Microservice` | Mantém identidade, atributos e transições de lifecycle. |
| Repositories | Aplicam queries limitadas, locks, paginação e integridade persistente. |

## Fluxo dos endpoints

Todos os caminhos abaixo são relativos a `/api/v1/platform/microservices`. `{microserviceIdentifier}` é o UUID público; o ID numérico permanece interno.

| Endpoint | Entrada e condição | Fluxo e resultado |
| --- | --- | --- |
| `POST /` | `code`, `name`, `settings`; `description` opcional; nenhuma versão não nula. | Normalização → schema → OWNER → unicidade/settings → criação ACTIVE → save/flush → auditoria `MICROSERVICE_CREATED` → 201. |
| `GET /{microserviceIdentifier}` | Microservice ACTIVE. | OWNER → Finder com filtro ACTIVE → output materializado → 200; ausente/inativo/quarentena retorna 404. |
| `GET /` | `lifecycle`, `page`, `size`, `sort`. | OWNER → validação da paginação/ordenação → `LifecycleFilter` → página filtrada no banco → 200. |
| `PUT /{microserviceIdentifier}` | ACTIVE; `version` atual e código registrado; nome/settings válidos. | OWNER → lock/reload → validação → update com `MicroserviceDetails` → flush → `MICROSERVICE_UPDATED` → 200 com nova versão. Versão desatualizada retorna 409. |
| `PATCH /{microserviceIdentifier}/inactivate` | ACTIVE. | OWNER → lock/reload → INACTIVE → flush → `MICROSERVICE_DEACTIVATED` → 200. Mantém vínculos existentes. |
| `PATCH /{microserviceIdentifier}/activate` | INACTIVE. | OWNER → lock/reload → ACTIVE → flush → `MICROSERVICE_ACTIVATED` → 200. ACTIVE/QUARANTINED não são origens válidas. |
| `DELETE /{microserviceIdentifier}` | INACTIVE, sem feature associada em qualquer lifecycle. | OWNER → lock/reload → consulta limitada de dependência → QUARANTINED → flush → `MICROSERVICE_DELETED` → 204. |

A exclusão mantém a linha no banco. A anotação de auditoria usa `DELETE`, pois não há remoção física nem endpoint de `PURGE`. O command retorna o objeto em quarentena para captura do snapshot; a facade/controller preserva a resposta HTTP sem body.

## Contrato de entrada e saída

| Campo | Contrato |
| --- | --- |
| `code` | Obrigatório, string de até 50 caracteres, kebab-case com letra inicial; normalização para minúsculas e trim. Imutável. |
| `name` | Obrigatório, texto não branco, até 100 caracteres; trim e unicidade global. |
| `description` | Opcional, string ou null, até 500 caracteres; trim. |
| `settings` | Obrigatório, objeto JSON. Valores e objetos aninhados são preservados. Além do contrato HTTP, aplica o schema configurável do microservice. |
| `version` | Inteiro não negativo dentro do limite de Long, ou null no schema compartilhado. Create rejeita versão não nula; update exige valor atual. |
| Campos adicionais | Rejeitados pelo schema antes da desserialização, incluindo tentativas de definir `identifier`, lifecycle, ator ou permissões. |

Os limites de nome/descrição permanecem 100/500, correspondentes às colunas atuais e aos demais elementos de Platform. A V62 publica o contrato sem reduzir colunas ou modificar registros existentes.

Exemplo de criação:

```json
{
  "code": "workspace-service",
  "name": "Workspace Service",
  "description": "Serviço de workspaces",
  "settings": {}
}
```

Na edição, enviar os mesmos campos com `"version": 0`, usando a versão retornada pela última consulta. O código deve permanecer `workspace-service`. `identifier`, lifecycle e timestamps são produzidos pelo servidor.

A listagem retorna `Page`, com `content` e metadados de paginação. O padrão é `lifecycle=ACTIVE`, página 0, tamanho 20 e ordenação por ID. O tamanho máximo configurado no resolver de paginação é 100. Os filtros aceitos são `ACTIVE`, `INACTIVE` e `QUARANTINED`, resolvidos exclusivamente pelo `LifecycleFilter`. A ordenação permite `id`, `identifier`, `code`, `name`, `createdAt` e `updatedAt`; propriedades desconhecidas, caminhos internos e offsets superiores a Integer.MAX_VALUE são rejeitados.

**Impacto para consumidores:** a listagem deixa de retornar array e passa a retornar página; o update exige `version` e `code`; consultas individuais/edições passam a exigir estado ACTIVE; DELETE exige INACTIVE. Integrar esses contratos aos consumidores antes da publicação.

## Schemas e settings

| Uso | Resolução | Origem |
| --- | --- | --- |
| Contrato completo de create/update | `PLATFORM / microservice` | V62, definição `microservice-resource`, referência `schemas/microservice.json`. |
| Conteúdo de `settings` | `MICROSERVICE / {code registrado}` | Configuração específica, ou fallback DEFAULT existente. |

A separação evita colisão quando um microservice tem código `microservice` ou `microservice-resource`. A normalização mantém tipos inválidos e propriedades desconhecidas para que o schema os rejeite. Não há construtor alternativo que dispense a validação de settings.

Settings persistidos em formato diferente de objeto geram erro localizado de servidor (`PLATFORM-0120`); o mapper não entrega dados silenciosamente corrompidos. A falha ao gerar snapshot reverte a transição e não produz evento parcial.

## Lifecycle, relações e concorrência

| Regra | Proteção |
| --- | --- |
| Atualizar somente ACTIVE | Estado atual carregado com lock exclusivo e pré-condição de domínio. |
| Cliente não sobrescrever atualização anterior | Versão do input comparada à versão atual; `@Version` continua protegendo a persistência. |
| Unicidade de código/nome, inclusive INACTIVE/QUARANTINED | Consultas de existência para mensagens de negócio; constraints únicas como garantia concorrente final. |
| Novos vínculos exigirem microservice ACTIVE | Query escalar com predicado ACTIVE e lock compartilhado, mantido até a transação do vínculo terminar. |
| Desativação não interromper criação já autorizada em outra transação | Escrita espera o lock compartilhado; após o commit, o vínculo permanece e o pai pode ser inativado. |
| Criação esperar desativação em andamento | Após a espera, a consulta com lock lê o estado atual e rejeita o pai INACTIVE. |
| Quarentena impedir novas associações | Predicado ACTIVE para novas referências e transições que não reativam QUARANTINED. |
| Exclusão enxergar uma feature recém-confirmada | Lock exclusivo no pai seguido de query limitada com lock compartilhado no vínculo; leitura atual, mesmo com snapshot anterior em REPEATABLE READ. |
| Não carregar todas as features | Removida a coleção reversa do pai; associação JPA unidirecional e consulta limitada do primeiro ID existente. |

O Finder obtém primeiro o ID por consulta escalar com lock exclusivo, desassocia a instância eventualmente em cache e faz uma nova leitura da entidade com lock. Isso evita o conflito antecipado do Hibernate ao combinar uma entidade antiga com o resultado atualizado de uma consulta bloqueante. Um `refresh` posterior à consulta não solucionava esse cenário.

No update de feature, a referência de microservice é bloqueada antes do registro da feature. Na exclusão do microservice, o pai é bloqueado antes da consulta do vínculo. Essa ordem evita introduzir o ciclo pai/filho no fluxo afetado.

Commands usam somente `@Transactional`, sem impor isolamento específico. Leituras usam `readOnly=true`; a operação que protege referências participa da transação de escrita do consumidor. Chamá-la isoladamente não mantém o lock depois de seu retorno. Leituras comuns no MySQL não precisam adquirir os locks de escrita e seguem o isolamento da conexão.

Inativar o microservice mantém suas features e mensagens. Novos vínculos, ativação de feature e validação da referência para edição exigem pai ativo. Queries históricas de mensagens continuam resolvendo identificadores sem exigir lifecycle ACTIVE.

Features em quarentena também impedem a exclusão do pai. Essa regra preexistente foi preservada; remover uma feature logicamente não libera sua associação histórica. Não existe nesta etapa purge de feature/microservice ou mudança dessa política de retenção.

## Achados e ajustes

| Local / método | Severidade | Evidência e impacto | Ajuste |
| --- | --- | --- | --- |
| `MicroserviceCommandService` / construtores | Alta | Construtor alternativo criava validator sem dependência de settings, permitindo caminho sem validação. | Um construtor com dependências obrigatórias; removido bypass do validator. |
| Requests / create e update | Alta | Validação após coerção/descarte do DTO não garantia o contrato do JSON original. | Advice central da biblioteca, schema único, normalizador que preserva tipos e campos desconhecidos. |
| Update / input, validator e Finder | Alta | Ausência de versão do cliente permitia sobrescrever uma edição anterior. Entidade previamente carregada também podia estar desatualizada. | Versão obrigatória, comparação atual sob lock e recarga antes da consulta de entidade. |
| Lifecycle / domínio e commands | Alta | Update/ativação podiam atingir quarentena; DELETE aceitava ativo. | Pré-condições explícitas ACTIVE/INACTIVE e nenhuma restauração de quarentena por activate. |
| `FeatureCommandService` / referência de microservice | Alta | Leitura não bloqueante podia autorizar associação ao mesmo tempo que o pai era inativado. | Projeção escalar ACTIVE com lock compartilhado e ordem de aquisição pai antes de filho. |
| Delete / dependências | Média | Carregava coleção inteira e podia observar snapshot antigo. | Query limitada com lock, sem materializar entidades ou coleções. |
| `Microservice` / coleção reversa e `Feature` / vínculo | Média | Attach/detach inicializava a coleção de filhos no cadastro ou troca de pai. | Relação unidirecional; setter de vínculo atualiza apenas o agregado filho. |
| Command / outputs auditados | Média | Sem flush, snapshot podia registrar versão anterior à gravada. | Flush antes de mapear todos os outputs; auditoria na mesma transação. |
| Query / listagem e auxiliares | Média | Listagem ilimitada e carregamento de entidades apenas para consultar existência/identidade. | Paginação no banco, filtro central, allowlist de sort, projeções e exists. |
| Mapper / settings | Média | Conversão no response espalhava parsing; formato persistido inadequado podia chegar ao cliente. | Conversão central para JsonNode; erro localizado para formato inválido. |
| Controller, facade e operações | Baixa | Abreviações de nomes prejudicavam entendimento; construção e tradução espalhadas. | Parâmetros legíveis, command/query nomeados, `MicroserviceDetails`, Finder e Mapper com responsabilidade concreta. |

## Verificação

- Unitários e arquitetura: 104 testes aprovados na execução final.
- `MicroserviceApiIT`: contrato original de JSON em create/update, limites de colunas, normalização, versão, unicidade, lifecycle, paginação, autorização negada, settings específicos, concorrência e auditoria/rollback.
- `MicroserviceConcurrencyIT`: criação versus desativação em ambas as ordens; snapshot REPEATABLE READ anterior ao vínculo; atualização de entidade anteriormente armazenada no contexto JPA.
- `MicroserviceResourceSchemaMigrationIT`: upgrade de V61 para V62, igualdade entre schema publicado e arquivo de referência, preservação integral de registro existente nos limites de 100/500.
- Regressões: Platform, messages, shared e listagem central de lifecycle.
- Resultado final da bateria completa: **274 testes de integração em 49 classes, sem falhas, erros ou skips**, além dos **104 testes unitários/arquitetura**. Build e verify aprovados.
- A execução completa foi dividida em quatro lotes de até 16 classes de integração, liberando os containers entre JVMs para respeitar os recursos locais do Docker. Logs: `/private/tmp/microservice-final-batch-{1,2,3,4}.log`; resultados agregados: `/private/tmp/microservice-final-results.json`.
- `git diff --check` aprovado. A revisão de microservices está concluída, sem achados bloqueadores ou de alta severidade abertos neste fluxo. Features e contexts permanecem como etapas seguintes da revisão de Platform.

Os testes concorrentes verificam integridade e sincronização entre transações; não medem throughput nem representam um benchmark de carga. Não foi adicionada promessa de capacidade por volume ou índice especulativo. Os índices únicos de identidade/código/nome e as FKs existentes permanecem como suporte das consultas e restrições.
