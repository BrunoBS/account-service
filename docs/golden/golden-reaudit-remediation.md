> Decisão vigente da integração main: Maven **1.0.0** em todo o conjunto; nenhuma release 1.0.1 será criada e nenhum pacote será publicado ou sobrescrito. As propostas anteriores de nova versão estão supersedidas. O gate e a integração Git estão registrados na seção final deste documento.

> Registro operacional do consumidor. Os [contratos canônicos Golden](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/README.md) continuam na documentação central.

# Golden Reference — correções para reauditoria

Estado: relatório de alterações locais, sem commit, push, merge, PR ou publicação remota.
Veredito final: **NOT READY FOR GOLDEN REAUDIT**, devido às dependências externas
registradas abaixo. As correções executáveis foram validadas localmente.

## Baseline preservado

| Repositório | Branch | HEAD antes das alterações |
| --- | --- | --- |
| account-service | main | b122bc8b76ef195eb8253d5ff548634cf3726a3e |
| platform-libraries | main | 902ef5830cfdc97cea0f672534fd110bc32f6ee9 |
| golden-platform | main | 3f2f9052423e161d89c3796263f95ed2ac3c1625 |
| platform-build | fix/manage-platform-capabilities | cf94f92b0d508fe0155ab02675058481f8da7fca |
| guideline-java | feature/poc-schema-renderer | 9e72ca938e8edb1e1305b17862c8f38ba275eb5b |

A alteração de formatação preexistente em ApplicationQueryService.java foi preservada.
platform-build possuía .idea/ não rastreado; guideline-java possuía arquivos locais
não rastreados. Nenhum desses arquivos foi sobrescrito. Os gitlinks Golden continuam
em serviço 5c78b97840492c970c16d0101be54a919745f8bb e bibliotecas
217fee1cbe9049600e108560579e2f8baf9500e5.

Ambiente: Java Oracle 25.0.2, Maven 3.9.14, Spring Boot 4.1.1, Spring 7.0.9,
Hibernate 7.4.5.Final, JUnit 6.0.3, JaCoCo 0.8.15, MySQL 8.4.11.
Parent/BOM/capabilities mantêm a coordenada 1.0.0; nenhuma versão de release foi inventada.
A validação local usa artefatos de desenvolvimento do workspace no Maven local;
isso não prova que os mesmos bytes estão disponíveis no registry.

Referências consultadas: AGENTS.md e documentação canônica da Golden, issue #55
lida diretamente no GitHub, platform-build/README e POMs, guideline-java/README,
platform-libraries, schemas, controllers, use cases, repositories e testes do serviço.
As zonas core/feature/foundation/entrypoint e a semântica dos endpoints foram preservadas.

## AUD-APP-001 — CORRIGIDO E VALIDADO

Anterior: o adapter exigia existência da Workspace Configuration, mas ignorava seu
lifecycle. Mesmo sem copiar, carregava a lista de publishers para descartá-la.

Correção executável:

- WorkspaceEnvironmentConfigurationPort passa a oferecer requireActive.
- WorkspaceEnvironmentConfigurationRepository consulta por Workspace, Environment e
  LifecycleTypeCode.active(), com PESSIMISTIC_READ mantido na transação do comando.
- O adapter preserva o NotFoundException e a chave padronizada já existentes.
- copyPublishers só lê associações depois de resolver origem ativa.
- ApplicationEnvironmentConfigurationCommandService valida a origem sem consultar
  publishers quando copyWorkspacePublishers=false; a opção true copia a origem ativa.

Arquivos: core/applicationconfiguration/usecase/{port,operations},
core/workspaceconfiguration/{integration,repository}, scenario de integração de
Application Configuration e os testes novos relacionados abaixo. Nenhuma migration,
request, response, chave de catálogo ou fronteira de persistência foi contornada.
Não há dependência do comando de Application no repository de Workspace Configuration.

Testes: os casos ACTIVE explícito/cópia já existentes foram preservados. O novo
ApplicationEnvironmentConfigurationLifecycleIntegrationTest cobre ausência na criação,
INACTIVE/QUARANTINED em criação/atualização com ambas as flags; confere lista vazia
após criação rejeitada e igualdade completa da resposta após updates rejeitados
(incluindo versão, settings e associações). Cobre pais inativos, restauração,
isolamento entre Applications/Workspaces, ancestral de Environment e falha do provedor.

Ausência física da origem no UPDATE não é produzível pelo HTTP: o lifecycle não
remove fisicamente Workspace Configuration. ApplicationConfigurationMissingSourceTest
injeta essa falha na porta para ambas as flags, verifica settings inalterados e
nenhuma escrita/alteração de associações ou fase. Isso complementa a integração real,
sem introduzir SQL ou persistência direta no setup HTTP.

Contratos na Golden: environment-configurations.md substitui a regra antiga e
identifica o novo contrato como preparação local ainda não publicada. Configurações
continuam sem restore próprio; restaurar superior não reativa filhos individualmente.

## AUD-SEC-001 — CORRIGIDO E VALIDADO

Anterior: perfil local implícito, MOCK/OWNER como fallback no YAML comum. A análise
complementar reproduziu também modo vazio sem seleção de nenhum aspect na biblioteca.

Correção: application.yml usa REAL, URL sem fallback e não seleciona local. Identidade
mock fica em application-local.yml, carregado deliberadamente, com GUEST por padrão.
As regras de perfis e a autorização continuam centralizadas na biblioteca.

Em platform-libraries, PlatformAuthorizationAutoConfiguration rejeita modo configurado
vazio ou incompatível com a seleção do aspect, usando AuthorizationTechnicalErrors.MODE_INVALID
(PLT-AUTH-005). Configuração ausente da biblioteca continua REAL com URL obrigatória.
Não houve fallback REAL→MOCK nem alteração de AuthorizationRequired.

Arquivos: application.yml, application-local.yml; na biblioteca,
PlatformAuthorizationAutoConfiguration.java, AuthorizationTechnicalErrors.java e
PlatformAuthorizationAutoConfigurationTest.java.

Testes: ServiceConfigurationTest carrega os YAML reais e a autoconfiguração Golden:
local explícito/GUEST, prod MOCK negado, URL ausente negada, sem perfil não ativa
local, modo inválido/vazio negado, produção REAL e defaults operacionais.
Teste HTTP usa provedor retornando 500: resposta 503 e dados/versão/associações intactos.
Os testes de contexto da biblioteca reproduziram o problema de modo vazio antes da correção.

Risco residual: a proteção nova da biblioteca existe somente no workspace/Maven local.
O consumidor publicado só terá essa proteção depois de release compatível autorizado.

## AUD-GOLD-001 — NÃO CORRIGIDO — DEPENDÊNCIA EXTERNA

Correções locais executadas no workflow: removidos checkout/install da revisão antiga
217fee1c e cache que poderia reintroduzir artefatos locais sob 1.0.0. O CI usa repository
Maven novo, strict-checksums, pacotes publicados e registra commit consumidor/hashes SHA-256.
O workflow não substitui falha do registry por instalação local.

A requisição autenticada dos POMs platform-parent, platform-libraries-bom e
platform-parameter-client:1.0.0 retornou HTTP 401. Um mvn validate com settings do
serviço e Maven repository vazio também falhou no parent com HTTP 401. A credencial
disponível não demonstrou permissão de leitura dos pacotes; não foi possível verificar
versão publicada, conteúdo do BOM ou checksums remotos. Não se infere ausência do
artefato a partir de 401, nem se declara BUILD-001 encerrada.

A documentação Golden foi alinhada aos SHAs já fixados pelos gitlinks quando o arquivo
existe no tree correspondente; caminhos movidos foram resolvidos por classe única.
Referências a testes removidos ficaram identificadas como históricas, sem alegar que
comprovam a revisão atual. Os gitlinks não foram atualizados.

Sequência externa necessária: fornecer credencial read:packages; verificar artefatos
imutáveis disponíveis e BOM/parent; definir/publicar nova revisão compatível da biblioteca
com a proteção de startup e fixtures usadas; alinhar versões do consumidor deliberadamente;
executar clean verify remoto isolado; só então aprovar commits/publicação e gitlinks.
Nenhuma dessas etapas remotas foi executada automaticamente.

## AUD-DB-001 — NÃO CORRIGIDO — DEPENDÊNCIA EXTERNA

Cenário A: FreshSchemaIntegrationTest adicionado, sobre o MySQL vazio da fixture
existente, verifica tabelas Core, views de runtime, índice único e FKs usando metadata
JDBC. A suíte HTTP completa valida a criação da base por Flyway e Hibernate.

Cenário B: os 87 scripts históricos existem no Git em
ff98f94bc36418972b1942b942f7ada66f98483e. Isso não prova qual cadeia/checksum está
aplicada numa instalação. O plano em [database-transition.md](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/database-transition.md)
exige inventário Flyway, backup representativo, ensaio em clone efêmero, validação de
invariantes e migrations futuras compatíveis com o estado real.

Não foram recriadas migrations, alterados checksums nem executados repair, clean ou
DDL em banco persistente. A V1 foi mantida byte a byte; SHA-256: 16b89bb75adfdc33d2f41583064096759ab7dace66fbfa3bff4a33d25d16a3c1.
Upgrade histórico permanece sem implementação/validação, condicionado à necessidade
real de implantar sobre banco existente e aos artefatos dessa instalação.

## AUD-OPS-001 — CORRIGIDO E VALIDADO

Adicionado spring-boot-starter-actuator gerenciado pelo parent. O platform-starter foi
inspecionado e não oferece essa capacidade; não foi criada implementação concorrente.
Liveness contém apenas livenessState; readiness inclui readinessState,db,redis.
Somente health está exposto; detalhes e componentes nunca são enviados ao cliente.

Arquivos: pom.xml, application.yml, HealthProbesIntegrationTest e
[operations.md](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/operations.md). Nenhum módulo novo ou framework de probes foi criado.
O teste usa o contexto Redis/MySQL já existente, verifica UP real, indisponibilidade
individual injetada no registry, readiness 503, liveness 200 e recuperação. Os indicadores
originais são sempre restaurados em finally. Essa injeção valida composição HTTP;
não simula um corte físico de rede. env/configprops não são expostos.

Probes retornam somente status sem token de usuário. A rede/ingress deve restringir
acesso operacional; manifests, timeouts e topologia finais continuam decisão do deploy.

## AUD-OBS-001 — CORRIGIDO E VALIDADO

WorkspaceFacade registra “Recebida requisição para criar workspace” e mantém o log de
identifier. O marcador BRUNO-TESTE foi removido. Busca adicional em produção não encontrou
System.out, printStackTrace ou marcadores temporários equivalentes. Logs úteis e MDC
não foram removidos.

## AUD-OBS-002 — CORRIGIDO E VALIDADO

YAML comum preserva JSON e usa INFO para br.com.portalmanager.platform.
PLATFORM_LOG_LEVEL permite DEBUG explícito. Test profile preserva filtros de ruído;
ServiceConfigurationTest valida defaults a partir do arquivo real. Não foram adicionados
payloads, credenciais ou tokens aos logs. O sanitizer compartilhado segue com seus
limites documentados; não se promete sanitização universal de exceptions arbitrárias.

## Cobertura complementar e issue #55

A issue foi lida antes dos testes novos. Os testes usam clients/factories/scenario
existentes, sem SQL nativo no setup, H2 ou container/contexto adicional por cenário.
A cadeia Workspace → Application → Environment → Workspace Configuration → Application
Configuration é exercitada, incluindo bloqueio e restauração dos pais. O filho
individualmente inativo continua inativo após restaurar o Workspace.
O teste de hierarquia usa CUSTOM → SHARD, compatibilidade já preparada pelo cenário.

Não foram removidos nem desabilitados testes. Permanecem auditoria/outbox,
schema validation, autorização, controles de versão, escopo de identificadores e
limites Ports/Adapters. Consultas SQL nativas produtivas não foram alteradas.

## Validação e evidências finais

| Execução | Resultado | Testes | Tempo |
| --- | --- | --- | --- |
| Serviço: mvn clean verify | BUILD SUCCESS; zero falhas/erros/ignorados | 59 Surefire + 769 Failsafe = **828** | **29:05** |
| Bibliotecas e consumidores: reactor selecionado com install local | BUILD SUCCESS; zero falhas/erros/ignorados | **307** | **1:35** |
| Repetição diagnóstica de três classes antigas | BUILD SUCCESS; zero falhas/erros/ignorados | **15** | **3:22** |
| Testes do validador documental Golden | OK | **32** | 8,2 s |
| Validação estrutural documental Golden | **Pendente**: três erros preexistentes de documentos não centrais | 0 links com SHA incorreto restantes | — |

O reactor das bibliotecas foi executado com:

```sh
mvn -pl modules/platform-authorization,modules/platform-starter,modules/platform-testing/platform-testing-authorization -am install
```

O install foi somente no Maven local; nenhum pacote foi enviado ao registry.
A suíte local do serviço usa a proteção nova desse artefato. A sintaxe YAML do workflow foi verificada; o workflow remoto
não está certificado enquanto a versão compatível não for publicada/consumida.

Cobertura nova do clean verify, sem reutilização de jacoco.exec anterior:

| Métrica | Coberto / total | Percentual |
| --- | --- | --- |
| Linhas | 6.654 / 7.183 | **92,64%** |
| Branches | 1.104 / 1.717 | **64,30%** |
| Instruções | 28.328 / 31.372 | **90,30%** |

A execução integral preservou os 804 testes anteriores e adicionou 24; os 50 testes
ArchUnit anteriores passaram. O clean verify teve **dois ApplicationContexts completos**,
confirmados pelos logs de startup. Os sete contextos mínimos de ServiceConfigurationTest
são deliberados para provar startup/falha com configurações distintas e não abrem banco.
Docker registrou um container MySQL, um Redis e um auxiliar Ryuk durante o build integral.
Não há container/contexto por caso novo. O baseline anterior não tinha contagem Docker
instrumentada para uma comparação numérica equivalente.

### Investigação do tempo

O aumento bruto foi de 779 s (+80,6%), com 24 testes adicionais (+3,0%). As três novas
classes de integração somaram 20,6 s. Os maiores tempos ocorreram em classes existentes:
WorkspaceFindAll 438,4 s, ContractList 266,1 s e WorkspaceSchemaCreate 143,8 s.
O log de energia do macOS confirmou suspensões entre 10:14:09–10:20:29,
10:30:53–10:33:13 e 10:34:00–10:38:18, entre outros intervalos durante a execução.
O startup registrou 427 s de relógio, mas apenas 47,9 s de uptime do processo.

Para investigar, sem alterar código/testes, foram repetidas essas três classes:

```sh
mvn initialize failsafe:integration-test failsafe:verify -Djacoco.skip=true -DskipTests=false -DskipITs=false -Dit.test=WorkspaceFindAllIntegrationTest,ContractListIntegrationTest,WorkspaceSchemaCreateIntegrationTest
```

Passaram os 15 testes. ContractList executou em 9,3 s e WorkspaceSchemaCreate em
4,4 s; WorkspaceFindAll ainda totalizou 187 s, incluindo startup, nesta repetição. A variação e os
registros de suspensão impedem atribuir o aumento bruto à mudança de código.
Não se declara benchmark estável nem se desconta artificialmente tempo do resultado.
Uma comparação controlada requer host sem suspensão. Nenhum teste foi desabilitado.

### Artefatos locais de evidência

Em target/build-evidence, preservados antes da repetição diagnóstica:

- clean-verify.log, clean-verify-results.json e clean-verify-metadata.json;
- clean-verify-reports.zip: XMLs Surefire/Failsafe e relatório JaCoCo integral;
- local-platform-artifacts.json: 18 POMs/JARs efetivamente resolvidos e seus SHA-256;
- remote-registry-result.json: comando isolado e falha HTTP 401;
- clean-verify-containers.json e clean-verify-power-events.json.

São artefatos gerados locais, não versionados. O relatório HTML fica em
target/site/jacoco/index.html. O CSV JaCoCo permaneceu inalterado durante a repetição,
com SHA-256 d9732fe86b891cdd3d6c8d0c8e24231874fffe2662600499810e454c01dd0aa5.
Os números acima vêm do snapshot do clean verify, não da soma de relatórios repetidos.


Baseline anterior: 50 Surefire + 754 Failsafe = 804 testes; 16:06; zero falhas.
A cobertura anterior (92,63% linhas / 64,24% branches) veio de execução não limpa;
a comparação é informativa e não demonstra regressão por si só.

O primeiro build limpo foi interrompido deliberadamente para incluir a proteção de
modo vazio descoberta durante a revisão de segurança; seu resultado parcial não é
usado como evidência de sucesso. As falhas iniciais dos testes novos foram corrigidas
nas expectativas HTTP e fixtures (204/200, indicador Redis reativo e tipo compatível
de filho), preservando os contratos existentes.

O validador documental Golden tinha 1.137 erros preexistentes: 1.134 referências a
SHAs diferentes dos gitlinks e três documentos de testes fora da organização central.
As URLs foram tratadas. A pendência dos três documentos locais preexistentes exige
decisão de governança sobre localização; eles não foram removidos/movidos para ocultar
o diagnóstico. O relatório e guias locais foram solicitados expressamente nesta missão.

## Limites e critérios para reauditoria

- Credencial/registry e release compatível da biblioteca ainda pendentes.
- Nenhum CI remoto foi executado; o workflow local está preparado.
- Migração histórica depende de inventário/base representativa e decisão de implantação.
- Gitlinks permanecem nos SHAs originais; os novos contratos estão identificados como draft local.
- Paginação de árvores, SCA e política de entrega externa da outbox continuam decisões
  operacionais/funcionais sem incidente ou requisito novo demonstrado nesta correção;
  não houve mudança silenciosa dessas semânticas.

Mesmo com build local verde, os impedimentos de registry/release e upgrade histórico
condicionado impedem declarar prontidão irrestrita. Veredito: NOT READY FOR GOLDEN REAUDIT.

## Arquivos locais alterados

A lista abaixo exclui artefatos de target e alterações preexistentes fora desta missão.
ApplicationQueryService.java continua com a alteração local de formatação inicial.

### account-service

```text
.github/workflows/verify.yml
README.md
docs/golden/database-transition.md
docs/golden/golden-reaudit-remediation.md
docs/golden/operations.md
pom.xml
src/main/java/br/com/portalmanager/platform/workspace/core/applicationconfiguration/usecase/operations/ApplicationEnvironmentConfigurationCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/core/applicationconfiguration/usecase/port/WorkspaceEnvironmentConfigurationPort.java
src/main/java/br/com/portalmanager/platform/workspace/core/workspace/facade/WorkspaceFacade.java
src/main/java/br/com/portalmanager/platform/workspace/core/workspaceconfiguration/integration/WorkspaceEnvironmentConfigurationAdapter.java
src/main/java/br/com/portalmanager/platform/workspace/core/workspaceconfiguration/repository/WorkspaceEnvironmentConfigurationRepository.java
src/main/resources/application-local.yml
src/main/resources/application.yml
src/test/java/br/com/portalmanager/platform/workspace/config/ServiceConfigurationTest.java
src/test/java/br/com/portalmanager/platform/workspace/core/applicationconfiguration/usecase/operations/ApplicationConfigurationMissingSourceTest.java
src/test/java/br/com/portalmanager/platform/workspace/integration/core/applicationconfiguration/ApplicationEnvironmentConfigurationLifecycleIntegrationTest.java
src/test/java/br/com/portalmanager/platform/workspace/integration/core/applicationconfiguration/support/ApplicationEnvironmentConfigurationScenario.java
src/test/java/br/com/portalmanager/platform/workspace/integration/foundation/operations/FreshSchemaIntegrationTest.java
src/test/java/br/com/portalmanager/platform/workspace/integration/foundation/operations/HealthProbesIntegrationTest.java
```

### platform-libraries

```text
modules/platform-authorization/src/main/java/br/com/portalmanager/platform/library/authorization/config/PlatformAuthorizationAutoConfiguration.java
modules/platform-authorization/src/main/java/br/com/portalmanager/platform/library/authorization/message/AuthorizationTechnicalErrors.java
modules/platform-authorization/src/test/java/br/com/portalmanager/platform/library/authorization/config/PlatformAuthorizationAutoConfigurationTest.java
```

### golden-platform

```text
docs/architecture/workspace.md
docs/decisions/0001-workspace-boundaries.md
docs/decisions/0002-library-build-contracts.md
docs/libraries/README.md
docs/libraries/modules/platform-audit.md
docs/libraries/modules/platform-authorization.md
docs/libraries/modules/platform-catalog.md
docs/libraries/modules/platform-dependencies.md
docs/libraries/modules/platform-libraries-bom.md
docs/libraries/modules/platform-libraries.md
docs/libraries/modules/platform-message-queue.md
docs/libraries/modules/platform-messaging.md
docs/libraries/modules/platform-observability.md
docs/libraries/modules/platform-parameter-client.md
docs/libraries/modules/platform-parent.md
docs/libraries/modules/platform-redis.md
docs/libraries/modules/platform-schema-validation.md
docs/libraries/modules/platform-starter.md
docs/libraries/modules/platform-tagging.md
docs/libraries/modules/platform-testing-aggregator.md
docs/libraries/modules/platform-testing-authorization.md
docs/libraries/modules/platform-testing-cloud-aws.md
docs/libraries/modules/platform-testing-cloud-azure.md
docs/libraries/modules/platform-testing-core.md
docs/libraries/modules/platform-testing-http.md
docs/libraries/modules/platform-testing-kafka.md
docs/libraries/modules/platform-testing-redis.md
docs/services/account-service/README.md
docs/services/account-service/api.md
docs/services/account-service/architecture-preliminary-assessment.md
docs/services/account-service/architecture.md
docs/services/account-service/data.md
docs/services/account-service/domain.md
docs/services/account-service/feature-coverage.md
docs/services/account-service/features/dynamic-bundles.md
docs/services/account-service/features/dynamic-parameters.md
docs/services/account-service/features/environment-configurations.md
docs/services/account-service/features/environments-publishers.md
docs/services/account-service/features/onboarding.md
docs/services/account-service/features/resource-sharing.md
docs/services/account-service/features/schemas-catalogs.md
docs/services/account-service/features/segmentation.md
docs/services/account-service/features/service-context.md
docs/services/account-service/features/workspaces-applications.md
docs/services/account-service/integration-tests.md
docs/services/account-service/integrations.md
docs/services/account-service/reaudit-preparation.md
docs/services/account-service/review.md
docs/services/account-service/security.md
docs/technical-debt.md
```


## Continuidade — consolidação da release

A execução seguinte está registrada em [release-consolidation.md](release-consolidation.md).
Ela preserva as correções e evidências deste relatório, detalha a release proposta,
o diagnóstico de credenciais, a preparação dos workflows e a sequência de integração.
A migração histórica não é bloqueio automático para homologação exclusiva de banco novo:
o escopo A ou A+B permanece dependente de decisão aprovada. Os bloqueios de registry,
release compatível e CI remoto não foram convertidos em sucesso local.

## Gate final da integração main — 27/10/2026

Esta seção atualiza os estados históricos acima. A integração Git na main foi
autorizada explicitamente, mantendo parent, BOM e bibliotecas Maven em **1.0.0**.
Não foi criada 1.0.1; não houve deploy Maven nem sobrescrita de pacotes.

| Verificação executada nesta integração | Resultado |
| --- | --- |
| platform-libraries: mvn clean verify, reactor completo | PASS — 24 projetos, 704 testes; zero falhas/erros/skips; 7min48s |
| Artefatos da biblioteca instalados exclusivamente no Maven local | 24 artefatos; JARs e POMs comparados por SHA-256 com a saída validada |
| account-service: mvn clean verify | PASS — 59 Surefire + 769 Failsafe = 828; zero falhas/erros/skips; 15min47s |
| ArchUnit | PASS — 50 testes incluídos no Surefire |
| JaCoCo limpo | Linhas: 6.650/7.179 = 92,63%; branches: 1.104/1.717 = 64,30% |
| Documentação Golden antes dos commits | PASS — zero erros em modo local explícito; 36 testes do validador aprovados |
| Namespace Java das bibliotecas e sintaxe YAML dos workflows | PASS |
| Alterações preexistentes | Preservadas; formatação de ApplicationQueryService excluída do commit |
| Migrations existentes | Não modificadas |

O baseline anterior tinha 6.654/7.183 linhas cobertas/total e as mesmas **529 linhas
não cobertas**. A formatação preexistente de ApplicationQueryService condensou quatro
linhas cobertas, explicando a diferença de 0,01 ponto; não houve perda de instruções
cobertas nem mudança em branches. Essa formatação permanece local e fora da integração.
Nenhum teste foi removido, desativado ou ignorado. O reactor completo substitui a
validação parcial anterior de 307 testes.

O commit da biblioteca é cc7c11bbe0ae5f610144b9fda5c221174a877e13. A correção PLT-AUTH-005
foi consumida pelos testes de configuração do serviço através dos binários locais
conferidos. Isso **não comprova** que o GitHub Packages fornece os mesmos bytes em
1.0.0. O workflow do consumidor mantém resolução isolada dos pacotes publicados,
strict checksums e evidência dos hashes; sua execução remota deve ser examinada após
o push, sem confundir o resultado com a validação das bibliotecas locais corrigidas.

Os três relatórios antigos de docs/testing foram preservados como históricos deprecated
na Golden; operação e transição Flyway também foram centralizadas. Apenas os dois
registros operacionais nominados permanecem neste consumidor, conforme o padrão
canônico. Os gitlinks e SHAs finais são registrados no documento canônico de
[preparação da reauditoria](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/reaudit-preparation.md).

Veredito de homologação permanece **NOT READY FOR GOLDEN REAUDIT** enquanto não houver
prova de consumo remoto da correção e definição do escopo de implantação histórica.
Push de código não equivale à publicação de artefatos nem à homologação independente.
