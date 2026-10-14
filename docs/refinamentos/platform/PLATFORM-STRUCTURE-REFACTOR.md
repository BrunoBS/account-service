# Platform — separação de responsabilidades

Data: 13/10/2026. Referência: [padrão de revisão](../../REVIEW-STANDARD.md).

## Resultado e escopo

Os cadastros de microservice, feature e context têm fluxos próprios. As associações
feature/context foram extraídas para um quarto fluxo de escrita, com controller,
fachada, command e validator próprios. As consultas pertencem ao recurso retornado:
features em Feature e contextos em Context.

A separação estrutural e de contratos foi complementada pela
[política de exclusão](PLATFORM-LIFECYCLE-REVIEW.md): microservices/features não têm
DELETE e context usa purge físico quando não há vínculos. O vínculo usa o estado
da feature global: inativação preserva a relação e oculta a feature na listagem
padrão; remoção física retira apenas aquele par feature/context. O detalhe de
participação em Shared também verifica a disponibilidade da feature/microservice
antes de montar a resposta. Referência indisponível gera exceção de negócio tratada
pelo messaging, conforme a decisão posterior de bloquear o detalhe.

## Organização

```text
entrypoint/web/platform/
  microservice/       MicroserviceController, request, response
  feature/            FeatureController, request, response
  context/            ContextController, request, response
  featurecontext/     FeatureContextController

feature/platform/
  domain/             microservice, feature, context, featurecontext
  facade/             microservice, feature, context, featurecontext
  repository/         microservice, feature, context, featurecontext
  usecase/model/      microservice, feature, context, featurecontext
  usecase/operations/ microservice, feature, context, featurecontext
  usecase/validation/ microservice, feature, context, featurecontext
```

`PlatformMessageKeys` e `PlatformValidation` continuam compartilhados dentro de
Platform. Não foram criados módulos independentes, ports artificiais ou dependências
noop. Testes específicos acompanham o pacote da responsabilidade; testes que
atravessam mais de um fluxo continuam no pacote geral.

## Responsabilidades

| Fluxo | Responsabilidade |
| --- | --- |
| Microservice | Atributos, settings, consultas e lifecycle do microservice. |
| Feature | Atributos, settings, microservice pai, elegibilidade Shared, lifecycle e consultas de features, inclusive por contexto. |
| Context | Atributos, consultas de contextos, inclusive por feature, lifecycle e purge sem vínculos. |
| FeatureContext | Somente associação e remoção física do vínculo entre um par de recursos. |

O cadastro de contexto, antes chamado `FeatureContext`, passa a usar `Context`,
`ContextController`, `ContextFacade`, `ContextCommandService`, `ContextQueryService`,
`ContextFinder`, `ContextValidator`, `ContextNormalizer` e DTOs `Context*`.
`FeatureContextRelation` continua representando a associação. Os nomes das tabelas,
colunas, UUIDs públicos e eventos históricos de auditoria foram preservados.

## Contratos HTTP

Os três cadastros mantêm suas rotas de recurso. `FeatureContextController` tem
somente `associate` e `remove`, ambos a partir de contexts. `FeatureController`
possui `findAll` com `contextIdentifier`; `ContextController` possui `findAll` com
`featureIdentifier`. Cada consulta
passa pela fachada, query service e repositório do recurso retornado.

Caminhos relativos a `/api/v1/platform`:

| Método e caminho | Controller / método | Operação |
| --- | --- | --- |
| `GET /features?contextIdentifier={contextIdentifier}` | FeatureController.findAll | Página de features vinculadas; lifecycle filtra a feature global. |
| `GET /contexts?featureIdentifier={featureIdentifier}` | ContextController.findAll | Página de contextos vinculados; lifecycle filtra o contexto. |
| `POST /contexts/{contextIdentifier}/features/{featureIdentifier}` | FeatureContextController.associate | Vincular um par de recursos elegíveis. |
| `DELETE /contexts/{contextIdentifier}/features/{featureIdentifier}` | FeatureContextController.remove | Remover somente o vínculo; HTTP 204 sem corpo. |

Há 21 handlers e combinações método/caminho: seis de microservice, seis de feature,
sete de context e duas de associação. Todos os métodos da fachada exigem OWNER,
incluindo ambos os sentidos de consulta. Não há aliases de escrita nem operação
`replace`; a antiga rota de substituição retorna 404 e as antigas rotas de escrita
a partir de features retornam 404.

### Mudança para os consumidores de contexto

`featureIdentifiers` deixa de ser aceito no POST/PUT de contexto, inclusive com
valor null ou lista vazia. Enviá-lo retorna 400 pelo schema original, antes da
coerção ou descarte pelo DTO. O cadastro cria somente o contexto; PUT altera
somente seus atributos.

Depois de criar o contexto, associar cada feature separadamente, sem body:

```http
POST /api/v1/platform/contexts/{contextIdentifier}/features/{featureIdentifier}
```

POST de vínculo retorna a representação da feature; DELETE retorna 204 sem corpo.
As duas operações não alteram atributos do contexto. Não recebem versão do cliente:
o par é verificado com lock na
transação, e uma mudança real avança as versões da feature e do contexto.
Leituras retornam Page, com sort validado e lifecycle centralizado; registros
ausentes retornam 404. O filtro administrativo aceita ACTIVE, INACTIVE e
QUARANTINED para dados históricos e não representa o estado de um vínculo.

## Segurança, integridade e concorrência

- Cada controller depende somente da fachada de sua responsabilidade; endpoints
  não recebem nem retornam entidades JPA.
- JSON Schema valida o documento de cadastro/edição antes da desserialização.
  A V65 publicou `PLATFORM/context` v2 e `PLATFORM/feature-context` v1; migrations
  aplicadas permanecem imutáveis. O segundo schema é histórico e não possui
  consumidor HTTP após a remoção de replace. As duas escritas de vínculo não têm body.
- O contexto é bloqueado antes das mudanças de vínculo. A ordem existente de
  aquisição é contexto WRITE, microservice READ para associação, feature WRITE e
  inserção atômica do par ou leitura WRITE para remoção. Leituras atuais protegem
  o purge e a remoção contra snapshots antigos.
- Associação usa inserção atômica que retorna se o par foi criado ou já existia,
  depois de bloquear e validar ambos os pais. Isso evita bloquear o intervalo de
  um vínculo ausente antes de inseri-lo no isolamento padrão REPEATABLE_READ do
  MySQL. Apenas inserções reais chegam à operação auditada, na mesma transação.
- Toda alteração real de vínculo avança as versões da feature e do contexto na
  mesma transação. Update de atributos continua exigindo a versão atual do cliente.
- Repetir associação válida ou remover vínculo ausente não avança versões nem
  duplica eventos.
- Novas associações exigem contexto, feature e microservice ativos. Preservar uma
  associação existente não exige reativar referências históricas; a remoção continua
  disponível para limpeza de referências inativas.
- Auditoria pertence às mudanças reais de associação. O snapshot identifica
  feature/context; falha no evento reverte vínculo e versões na mesma transação.
  Os eventos do cadastro de contexto mantêm seus nomes históricos.
- Consultas são paginadas no banco. A consulta de features carrega o microservice
  necessário à resposta na mesma consulta e aplica o mesmo filtro à contagem.

## Verificação atual

Resultados da simplificação, da organização das leituras e da política de exclusão:
[revisão geral de fechamento](PLATFORM-FINAL-REVIEW.md#validação).

## Validação histórica da separação estrutural, anterior à simplificação

- Compilação e **108 testes unitários/arquitetura** aprovados, incluindo proteção
  dos pacotes de fachada e separação dos commands de cadastro e associação.
- **344 testes de integração em 57 classes**, todos verificados com resultado
  final sem falhas, erros ou skips. A regressão cobre Platform, Shared, demais
  features/core, persistência e migrations.
- A primeira rodada HTTP focada aprovou 46 cenários nos quatro fluxos afetados.
  Na regressão geral, um cenário legado de `PlatformApiIT` ainda enviava a lista
  no cadastro do contexto: foi adaptado ao novo contrato e repetido com sucesso,
  junto com `ContextApiIT`, em 13 cenários.
- O lote agrupado apresentou 404 do mock de autorização nos dez cenários de
  `OnboardingApiIT`, antes das regras de onboarding. Essa classe foi executada
  isoladamente em outro processo Maven e os dez cenários passaram. Nenhum código
  de onboarding foi alterado; esse resultado de isolamento fica separado da
  aprovação das outras classes do lote.
- `ContextResourceSchemaMigrationIT` verifica upgrade até V65, igualdade dos dois
  schemas com seus arquivos de referência, manutenção da versão publicada anterior
  e preservação integral dos contextos/vínculos existentes.
- `FeatureContextQueryIT` verifica respostas utilizáveis fora da transação e três
  consultas para existência, página com microservice e contagem com o mesmo filtro.
- Concorrência: oito cenários de contexto, treze de feature e quatro de microservice,
  incluindo snapshot antigo, versão desatualizada e alterações simultâneas de vínculos.
- Inventário histórico da separação: 28 combinações método/caminho, sem colisões;
  26 rotas canônicas e dois aliases. A política posterior removeu dois DELETE. `git diff --check` aprovado.

Comandos/evidências da rodada:

```text
mvn --offline --batch-mode --no-transfer-progress clean test
mvn --offline --batch-mode --no-transfer-progress -Dit.test=FeatureContextApiIT,ContextApiIT,FeatureApiIT,PlatformEndpointContractIT verify
mvn --offline --batch-mode --no-transfer-progress -Dit.test=PlatformApiIT,ContextApiIT verify
mvn --offline --batch-mode --no-transfer-progress -Dit.test=OnboardingApiIT verify
python3 /private/tmp/verify-platform-separation-batches.py
```

A bateria completa usa lotes de até 16 classes por processo. Logs:
`/private/tmp/platform-separation-batch-{1..4}.log`,
`/private/tmp/platform-separation-repaired-api.log` e
`/private/tmp/platform-separation-onboarding-isolated.log`.
O consolidado `/private/tmp/platform-separation-results.json` registra o resultado
final de cada classe; `/private/tmp/platform-separation-controller-inventory.json`
registra as rotas. A execução isolada substitui somente os resultados de onboarding
que falharam no mock agrupado; a repetição corrigida substitui o cenário legado
anterior, sem contar testes repetidos como novos cenários.
