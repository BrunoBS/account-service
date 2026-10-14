# Revisão de Platform — Features

> Registro da revisão anterior à separação dos quatro fluxos. A estrutura e os contratos atuais, incluindo a retirada de `featureIdentifiers` do cadastro de contexto, estão em [Platform — separação de responsabilidades](PLATFORM-STRUCTURE-REFACTOR.md). As evidências abaixo são históricas.


Data: 13/10/2026. Critérios: [padrão de revisão](../../REVIEW-STANDARD.md).

## Escopo e responsabilidades

Revisão das dez operações de `/api/v1/platform/features`: controller → facade → command/query → validação → domínio → persistência, incluindo autorização, JSON Schema, auditoria, lifecycle, contratos Shared e vínculos com contexts. O fluxo de microservices foi revisado anteriormente. Contexts receberam os ajustes necessários à integridade das associações; sua revisão completa continua sendo a próxima etapa de Platform.

| Componente | Responsabilidade |
| --- | --- |
| `FeatureController` | Requests tipados, parâmetros HTTP, paginação e tradução dos outputs. |
| Requests e `FeatureNormalizer` | Validar o JSON original pelo mecanismo central da biblioteca; normalizar apenas textos conhecidos. |
| `FeatureFacade` | Exigir OWNER e a ação correspondente em todas as rotas públicas. |
| `FeatureCommandService` | Coordenar transação, referências, locks, validação, mutação e flush. |
| `FeatureQueryService` | Consultas paginadas, filtro central de lifecycle e projeções para integrações. |
| `FeatureValidator` | Unicidade, versão do cliente, código imutável, settings configuráveis e proteção de contratos Shared. |
| `FeatureFinder` | Buscar estado atual para escrita e adquirir locks na ordem definida para referências. |
| `FeatureMapper` | Converter inputs em `FeatureDetails` e entidades em outputs com settings JSON. |
| `FeatureContextAssociationOperations` | Executar e auditar somente alterações reais de vínculos, individuais ou em lote. |
| `Feature` | Encapsular atributos, identidade e pré-condições de lifecycle. |
| Repositories | Paginação e filtros no banco, existência, projeções e leituras bloqueantes atuais. |

Commands usam somente `@Transactional`. Queries comuns usam `readOnly=true`; a resolução de referência para criar contrato Shared participa da transação de escrita do consumidor. Não foi imposto isolamento específico nem `Propagation.MANDATORY`.

## Fluxo dos endpoints

Rotas relativas a `/api/v1/platform/features`. Identificadores públicos são UUID; IDs numéricos ficam nas consultas e relações internas.

| Endpoint | Pré-condição | Resultado e auditoria |
| --- | --- | --- |
| `POST /` | Código/nome únicos, settings válidos, microservice ACTIVE; nenhuma versão não nula. | Cria ACTIVE com `FeatureDetails`, save/flush, `FEATURE_CREATED`, 201. |
| `GET /{featureIdentifier}` | Feature ACTIVE. | Retorna representação, 200; ausente/INACTIVE/QUARANTINED: 404. O pai inativo não impede consultar uma feature existente. |
| `GET /` | Paginação válida; filtros opcionais `contextCode` e `lifecycle`. | Página filtrada no banco, sem duplicação por vínculos; 200. |
| `PUT /{featureIdentifier}` | Feature ACTIVE, versão atual, código registrado, microservice de destino ACTIVE. | Atualiza detalhes e pai em uma única mutação, flush, `FEATURE_UPDATED`, 200 com versão atualizada. |
| `PATCH /{featureIdentifier}/inactivate` | Feature ACTIVE. | INACTIVE, preserva vínculos, flush, `FEATURE_DEACTIVATED`, 200. |
| `PATCH /{featureIdentifier}/activate` | Feature INACTIVE e microservice ACTIVE. | ACTIVE, flush, `FEATURE_ACTIVATED`, 200. Não restaura QUARANTINED. |
| `DELETE /{featureIdentifier}` | Feature INACTIVE. | QUARANTINED, preserva linha e vínculos, flush, `FEATURE_DELETED`, 204. Command retorna snapshot para o aspect. |
| `GET /{featureIdentifier}/contexts` | Feature existente em qualquer lifecycle e paginação válida; OWNER. | Página de contexts com filtro próprio de lifecycle; 200. Permite inspecionar vínculos históricos para limpeza. |
| `POST /{featureIdentifier}/contexts/{contextIdentifier}` | Context, feature e microservice ACTIVE. | Cria vínculo ausente, incrementa versão da feature, `FEATURE_CONTEXT_ASSOCIATED`, 200. Vínculo já existente não altera versão nem gera novo evento. |
| `DELETE /{featureIdentifier}/contexts/{contextIdentifier}` | Context e feature existentes, independentemente do lifecycle. | Remove vínculo existente, incrementa versão da feature, `FEATURE_CONTEXT_REMOVED`, 200. Ausência do vínculo é idempotente. |

DELETE da feature usa ação de auditoria `DELETE`, não `PURGE`: não há remoção física. Remover uma associação continua permitido após a quarentena, para possibilitar limpeza sem restaurar o recurso.

## Contrato HTTP

| Campo | Contrato |
| --- | --- |
| `code` | Obrigatório, string até 50, kebab-case com letra inicial. Trim e minúsculas; imutável depois do cadastro. |
| `name` | Obrigatório, não branco, até 100; trim e unicidade global. |
| `description` | Opcional, string ou null, até 500; trim. |
| `microserviceIdentifier` | Obrigatório, string UUID do pai ativo. Pode mudar no update validado. |
| `settings` | Obrigatório, objeto JSON; estrutura interna preservada e validada pelo schema configurável da feature. |
| `shareable` | Boolean opcional, padrão false. Null, string ou número são rejeitados. |
| `version` | Inteiro não negativo até Long.MAX_VALUE ou null no schema compartilhado. Create rejeita versão não nula; update exige versão atual. |
| Campos desconhecidos | Rejeitados antes da desserialização; cliente não define identidade, lifecycle, timestamps ou ator. |

```json
{
  "code": "account-management",
  "name": "Gestão de contas",
  "description": "Funcionalidades de contas",
  "microserviceIdentifier": "11111111-1111-1111-1111-111111111111",
  "settings": {},
  "shareable": true
}
```

Para editar, enviar os mesmos campos com a `version` obtida na última leitura e o código registrado. Versão desatualizada retorna 409. O Hibernate continua usando `@Version`; a comparação do input protege também o intervalo entre a leitura do cliente e o início da transação de edição.

Ambas as listagens retornam `Page`, com `content` e metadados. Padrões: ACTIVE, página 0, tamanho 20, sort por ID; tamanho máximo configurado no resolver: 100. `LifecycleFilter` resolve ACTIVE, INACTIVE e QUARANTINED para todos os consumidores. Sort aceita `id`, `identifier`, `code`, `name`, `createdAt`, `updatedAt`; caminhos internos, campos desconhecidos e offsets acima de Integer.MAX_VALUE são rejeitados. `contextCode` filtra a associação existente e não aplica implicitamente outro filtro ao lifecycle do context.

**Impacto para consumidores:** GET da coleção e dos contexts deixa de devolver array e passa a devolver página; PUT exige `version` e `code`; detalhe/edição exigem feature ACTIVE; DELETE exige INACTIVE. Atualizar os consumidores antes da publicação.

## Schemas, settings e integridade

| Uso | Namespace | Origem |
| --- | --- | --- |
| Contrato HTTP completo | `PLATFORM / feature` | V63, definição `feature-resource`, arquivo `schemas/feature.json`. |
| Conteúdo de settings | `FEATURE / {code registrado}` | Configuração específica ou fallback DEFAULT existente. |

A normalização mantém campos desconhecidos e tipos inválidos para rejeição pelo schema original. Não há construtor de produção que substitua a validação de settings ou os contratos Shared por dependências silenciosas. Limites 50/100/500 correspondem às colunas existentes; V63 publica o schema sem alterar registros ou reduzir colunas.

Código, nome e UUID continuam protegidos por constraints únicas. Consultas `exists` produzem mensagens de negócio, enquanto o banco garante a unicidade em gravações simultâneas. FKs e a chave composta da relação feature/context permanecem como garantia persistente.

O mapper converte settings uma única vez, dentro do caso de uso. Conteúdo persistido diferente de objeto JSON gera erro localizado de servidor `PLATFORM-0124`; falha de snapshot reverte a mutação inteira, inclusive alterações de vínculos.

## Lifecycle, referências e concorrência

| Situação | Regra e proteção |
| --- | --- |
| Feature ou microservice inativado | Vínculos já existentes permanecem. Novas associações exigem as referências ACTIVE. |
| Contrato Shared novo | Exige feature ACTIVE, shareable e microservice ACTIVE; locks do pai e da feature permanecem até o commit do contrato. |
| Tornar feature não compartilhável | Bloqueado com 409 enquanto houver qualquer contrato, inclusive inativo. Consulta do port observa relações atuais sob lock. |
| Disponibilidade Shared | Projeção filtra feature ACTIVE, shareable e pai ACTIVE diretamente no banco. Não carrega entidades para depois filtrar. |
| Histórico de contratos | Projeções de identidade/nome não exigem lifecycle ACTIVE; preservam resolução dos vínculos existentes. |
| Update com instância JPA previamente carregada | Leitura escalar com lock exclusivo, detach da instância e consulta atual da entidade; compara a versão atual. |
| Pai alterado depois da projeção inicial | Depois de bloquear pai e feature, confere se o pai atual corresponde ao protegido; divergência retorna conflito 409. |
| Associação individual | Bloqueia context, depois microservice, depois feature; verifica relação em leitura atual bloqueante. |
| Remoção individual | Bloqueia context e feature; permite limpar relações históricas sem validar novamente elegibilidade para criação. |
| Substituição em lote dos vínculos de context | Projeta IDs iniciais, bloqueia pais das inclusões, depois features alteradas por ID crescente e somente então confirma as relações em leitura atual bloqueante. Snapshot divergente retorna 409 sem mutação. |
| Retry de associação | Consulta bloqueante observa relação recém-confirmada mesmo com snapshot REPEATABLE READ anterior; retorno não produz nova versão/evento. |
| Exclusão de context com vínculo recém-confirmado | Context bloqueado antes de consulta limitada com lock compartilhado na relação; impede quarentena mesmo com snapshot anterior sem vínculos. |

O teste de trocas cruzadas inicialmente reproduziu um deadlock causado por locks das relações adquiridos antes das features. A confirmação das relações foi movida para depois dos locks de pais/features; a leitura inicial usa somente IDs e qualquer divergência é rejeitada com 409. A ordem validada evita o ciclo em que uma transação adiciona A/remove B enquanto outra adiciona B/remove A. Apenas recursos envolvidos na operação são bloqueados. Leituras comuns continuam seguindo o isolamento da conexão; esta revisão verifica integridade concorrente, não throughput ou capacidade por volume.

Na edição de context, `featureIdentifiers` omitido/null preserva vínculos, lista vazia remove todos e uma lista informada substitui o conjunto atual. Duplicados resultam em um vínculo. Features já vinculadas podem continuar INACTIVE/QUARANTINED; somente inclusões são revalidadas. Falha em qualquer inclusão, remoção ou auditoria reverte toda a transação. Alterações reais em lote geram os mesmos eventos de feature das rotas individuais, além do evento do context.

## Achados e ajustes

| Local | Severidade | Evidência e impacto | Ajuste / verificação |
| --- | --- | --- | --- |
| Requests/create/update | Alta | Coerção do DTO e descarte de campos ocultavam entrada inválida. | Validação do JSON original, schema V63 e normalizador; testes de tipos, desconhecidos e limites. |
| Command/validator/construtores | Alta | Construtores alternativos dispensavam settings/contratos Shared. | Dependências obrigatórias; doubles explícitos nos testes. |
| Update/Finder | Alta | Sem versão do cliente e com entidade em cache, edição podia sobrescrever dados anteriores. | Versão obrigatória, código imutável, leitura atual sob lock; API concorrente e cache JPA testados. |
| Domínio/lifecycle | Alta | Operações aceitavam origens inadequadas ou restauravam quarentena. | Transições explícitas e guards ACTIVE/INACTIVE; testes de domínio e API. |
| Referências Shared e context | Alta | Validação sem proteger o pai permitia novos vínculos durante desativação. | Pai antes de filho, predicado ACTIVE e locks até commit; testes em transações reais. |
| Context/substituição de vínculos | Alta | Inclusões podiam usar feature inativa; ordem de locks em trocas cruzadas podia formar ciclo. | Validação somente das inclusões e ordem global dos recursos; troca simultânea repetida em teste. |
| Context/delete | Alta | Consulta de existência não bloqueante podia ignorar associação confirmada depois de snapshot anterior. | Projeção limitada da primeira relação sob lock compartilhado; teste concorrente preserva context e vínculo. |
| Auditoria/vínculos | Média | Retries geravam eventos indevidos; alteração em lote não usava a mesma auditoria da associação individual. | Operações auditadas chamadas somente para mudanças reais; flush e rollback cobertos. |
| Query/listagens | Média | Coleções ilimitadas e associação lazy multiplicavam consultas; ordenação de contexts precisa usar o alias da entidade retornada. | Page no banco, EXISTS por context, fetch do pai e filtros centrais; página com dois pais verifica duas queries, e contexts têm cobertura de sort por nome com duas páginas. |
| Integração Shared/consulta em lote | Média | Carregava entidades para filtrar e podia inicializar pais individualmente. | Projeções com elegibilidade no SQL e resolução histórica separada; regressão Shared. |
| Mapper/output | Média | Parsing no response e snapshots sem flush espalhavam responsabilidades e versão antiga. | JSON no output e flush antes do snapshot; auditoria/JSON persistido inválido cobertos. |
| Controller/facade/usecases | Baixa | Abreviações e mutações fragmentadas prejudicavam leitura. | Nomes descritivos, `FeatureDetails`, Finder/Mapper e uma mutação de update. |

## Verificação

- `FeatureApiIT`: 19 cenários de contratos, autorização, unicidade, concorrência, lifecycle, vínculos, paginação, schemas/settings, contagem de queries e auditoria/rollback.
- `FeatureConcurrencyIT`: 13 execuções aprovadas, cobrindo versão/cache JPA, snapshot antigo, desativação concorrente, retenção do lock do pai, mudança de pai, retry de vínculo, contrato recém-confirmado, exclusão de context após associação recém-confirmada, substituição com snapshot antigo e trocas cruzadas de context em lote.
- `FeatureResourceSchemaMigrationIT`: upgrade V62 → V63, igualdade do schema publicado com o arquivo e preservação dos registros e vínculos existentes nos limites de coluna.
- Resultado final: **105 testes unitários/arquitetura e 307 testes de integração em 52 classes**, sem falhas, erros ou skips. Execução inicial com clean e verify dos quatro lotes aprovados.
- Confirmação após os últimos ajustes de consulta e locks: `mvn --offline --batch-mode --no-transfer-progress -Dit.test=FeatureApiIT,PlatformApiIT verify`, com **21 testes de integração e 105 unitários/arquitetura aprovados**. Inclui ordenação por nome dos contexts em duas páginas e associação em lote; log `/private/tmp/feature-api-final.log`.
- `git diff --check` aprovado. Não restam achados bloqueadores ou de alta severidade abertos no fluxo de features revisado.
- A compilação com clean também identificou arquivos obsoletos de refatorações anteriores de ambientes, schema e onboarding. Foram retirados os commands/validator/milestone substituídos e sem consumidores atuais, preservando cópias em `/private/tmp/feature-review-obsolete-sources`. Os adapters e commands atuais permanecem responsáveis por esses fluxos.

Os testes de integração são executados em quatro lotes de até 16 classes, liberando os containers entre JVMs para respeitar os recursos locais do Docker. Logs: `/private/tmp/feature-final-batch-{1,2,3,4}.log`; agregado: `/private/tmp/feature-final-results.json`.

Não foram adicionados índices por especulação, alteração de isolamento ou purge de feature.

Na [revisão posterior de contexts](CONTEXT-CODE-REVIEW.md), seus requests, versão,
paginação e transições foram revisados. Alterações individuais de vínculos passaram
a avançar também a versão do context, e o snapshot de associação inclui o
context de destino. A sincronização usa a versão do snapshot sob lock do context
para proteger a lista inicial sem locks de intervalo sobre a tabela de relações;
essa evolução substitui a confirmação bloqueante de conjunto descrita nesta etapa.

A [revisão geral dos endpoints](PLATFORM-ENDPOINTS-REVIEW.md) também corrigiu
a consulta de contexts de features inativas/em quarentena: seus vínculos
permanecem consultáveis por OWNER para permitir limpeza. GET do detalhe e PUT
da feature continuam restritos a ACTIVE.
