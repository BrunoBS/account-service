> Decisão vigente da integração main: Maven **1.0.0** em todo o conjunto; nenhuma release 1.0.1 será criada e nenhum pacote será publicado ou sobrescrito. As propostas anteriores de nova versão estão supersedidas. O gate e a integração Git estão registrados na seção final deste documento.

> Registro operacional do consumidor. Os [contratos canônicos Golden](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/README.md) continuam na documentação central.

# Consolidação e preparação final da Golden Reference

Estado: preparação local, sem commits, push, merge, PR, alteração remota ou publicação.
Referência obrigatória preservada: [relatório de remediação](golden-reaudit-remediation.md).
Veredito: **NOT READY FOR GOLDEN REAUDIT** — consumo remoto da biblioteca corrigida
continua sem prova; as limitações e aprovações necessárias são discriminadas abaixo.

## Estado inicial e limites da consolidação

O baseline confirmado era 828 testes do serviço, 307 de bibliotecas/consumidores,
92,64% de linhas e 64,30% de branches. As correções funcionais, PLT-AUTH-005,
autorização, logs e probes não foram refeitas. Nenhum Java, schema, teste Java ou
migration existente foi alterado nesta etapa; as novas mudanças são de build,
workflows e documentação. Todas as alterações locais anteriores foram preservadas.

| Repositório | Branch | HEAD preservado |
| --- | --- | --- |
| account-service | main | b122bc8b76ef195eb8253d5ff548634cf3726a3e |
| platform-libraries | main | 902ef5830cfdc97cea0f672534fd110bc32f6ee9 |
| golden-platform | main | 3f2f9052423e161d89c3796263f95ed2ac3c1625 |
| platform-build | fix/manage-platform-capabilities | cf94f92b0d508fe0155ab02675058481f8da7fca |
| guideline-java | feature/poc-schema-renderer | 9e72ca938e8edb1e1305b17862c8f38ba275eb5b |

Os gitlinks continuam em Account Service 5c78b97840492c970c16d0101be54a919745f8bb e
Platform Libraries 217fee1cbe9049600e108560579e2f8baf9500e5. Eles não contêm os diffs
locais desta preparação nem constituem commits de release corrigida.

## Classificação das pendências

| Item | Estado | Evidência / limite |
| --- | --- | --- |
| Diagnóstico da credencial local | RESOLVIDO E VALIDADO | API de usuário 200; API de pacotes 403 exigindo read:packages; Maven 401 |
| Resolução remota isolada e conteúdo publicado | BLOQUEADO — CREDENCIAL OU INFRAESTRUTURA | Parent/BOM/parameter-client não puderam ser obtidos com a credencial disponível |
| Patch de autorização e compatibilidade dos consumidores selecionados | RESOLVIDO E VALIDADO | 307 testes nesta etapa, incluindo 116 de autorização; bytes consumidos localmente conferidos |
| Release Maven corrigida | PREPARADO — AGUARDANDO PUBLICAÇÃO | Candidata coordenada 1.0.1, sujeita a aprovação, disponibilidade e validação completa do reactor de release |
| Consumo pelo Account Service | PREPARADO — AGUARDANDO PUBLICAÇÃO | Gestão integral pelo BOM preparada, versões efetivas mantidas em 1.0.0 |
| Workflow do serviço | PREPARADO — AGUARDANDO PUBLICAÇÃO | Pré-validação autenticada, repository isolado, checksums e evidências; nenhum CI remoto executado |
| Segurança do workflow de publicação | PREPARADO — AGUARDANDO PUBLICAÇÃO | Versão nova explícita, revisão do fonte coerente, leitura fail-closed, trava de coordenadas e deployAtEnd; só validado localmente |
| Banco novo / Cenário A | RESOLVIDO E VALIDADO | MySQL vazio pela fixture, V1 imutável, Hibernate, metadata e suíte HTTP; não equivale a homologação final |
| Escopo A ou A+B e upgrade histórico | DEPENDENTE DE DECISÃO FUNCIONAL/OPERACIONAL | Histórico não é obrigação automática da primeira homologação; sem inventário/base para B |
| Três documentos legados | DEPENDENTE DE DECISÃO FUNCIONAL/OPERACIONAL | Conteúdo preservado, aviso de histórico e destino canônico definidos; retirada e gitlink exigem integração autorizada |
| Atualização de gitlinks | PREPARADO — AGUARDANDO PUBLICAÇÃO | Sequência definida; C_LIB/C_SVC ainda não existem como releases aprovadas/publicadas |
| Alterar regras funcionais, migrations ou criar módulos novos | NÃO APLICÁVEL — JUSTIFICADO | Baseline funcional já validado; nenhuma mudança dessa natureza era necessária |

## Registry: autenticação, autorização e existência

Foram verificados os settings Maven do consumidor e do publicador, URLs e IDs de server.
O consumidor usa github-platform-libraries com username/token em variáveis de ambiente;
o ID corresponde ao repository HTTPS de brunobs/platform-libraries. Não existe
settings.xml/settings-security.xml pessoal fornecendo outra credencial local, nem
GITHUB_PACKAGES_TOKEN/GITHUB_TOKEN/GH_TOKEN exportados neste ambiente.

A credencial do GitHub CLI autentica na API de usuário (HTTP 200), mas a API de pacotes
retorna HTTP 403 com a mensagem explícita de que é necessário read:packages. Seus escopos
observados são gist, read:org, repo e workflow; não há read:packages. O segredo
PLATFORM_PACKAGES_TOKEN consta na metadata dos dois repositórios, mas seu conteúdo,
validade e permissões não são recuperáveis pela API e não foram presumidos.

A documentação oficial exige PAT classic com read:packages para esse consumo privado,
ou GITHUB_TOKEN com acesso apropriado ao pacote no contexto Actions. packages: read
no job configura o GITHUB_TOKEN; não acrescenta escopos a um PAT colocado em secret.
Fonte: [GitHub — Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).

| Coordenada | GET autenticado do POM 1.0.0 | Conteúdo/checksum remoto |
| --- | --- | --- |
| br.com.portalmanager.platform.library:platform-parent:pom:1.0.0 | 401 | Não obtido |
| br.com.portalmanager.platform.library:platform-libraries-bom:pom:1.0.0 | 401 | Não obtido |
| br.com.portalmanager.platform.library:platform-parameter-client:pom:1.0.0 | 401 | Não obtido |

Também foi executado validate com settings do consumidor, strict-checksums e um diretório
Maven novo e vazio. A resolução parou no parent com 401, antes de compilação ou testes.
Isso comprova o impedimento de acesso atual; **não comprova pacote inexistente**.
Não foi possível inventariar versões, certificar 1.0.1 livre ou investigar alterações
históricas de bytes sob o mesmo GAV. O conflito de origem local versus remoto sob 1.0.0
continua uma ambiguidade real de desenvolvimento, sem prova de republicação no registry.

### Histórico remoto observado sem escrita

Foram lidos os runs existentes, sem dispatch ou reexecução:

- [Publish 40144665781](https://github.com/BrunoBS/platform-libraries/actions/runs/40144665781): sucesso em 24/10/2026 no SHA 9a83bf22c8bd0a601880800ed5a1a765d7159f34.
- [Publish 39888498240](https://github.com/BrunoBS/platform-libraries/actions/runs/39888498240): sucesso em 23/10/2026 no SHA 114389d93827e4d9f40ef6e6b4a3bc17eb042186.
- Os dois fontes usavam revision=1.0.0; seus logs registram clean deploy e 24 execuções
  do goal deploy. Os POMs instalados durante esses runs usam GAV 1.0.0. Os BOMs fonte
  examinados são iguais e ambos gerenciam parameter-client. Como --no-transfer-progress
  suprime os eventos de transferência, não há manifesto remoto de bytes nesses logs.
- [Verify da biblioteca](https://github.com/BrunoBS/platform-libraries/actions/runs/40447680560)
  e [Verify do serviço](https://github.com/BrunoBS/account-service/actions/runs/40447682012)
  estão verdes nos HEADs de main, mas antecedem os diffs locais desta correção e usam
  os workflows anteriores. Não certificam o pipeline preparado ou o patch local.

Esse histórico mostra reutilização de 1.0.0 em processos de publicação, sem permitir
concluir se houve recriação de pacote, sobrescrita ou alteração dos bytes hoje servidos.
Não há prova atual de BOM remoto sem parameter-client; esse era um diagnóstico histórico,
que precisa ser revalidado. Por isso o plano não tenta republicar 1.0.0 e exige versão
nova, inventário de versões e manifesto de checksums verificável.

### Resolução objetiva do impedimento

1. Um responsável deve fornecer PAT classic de leitura read:packages com acesso ao
   repositório/pacotes privados, ou validar o acesso do GITHUB_TOKEN no contexto Actions.
   Autorizações adicionais só são necessárias se a organização/repositório as exigir.
2. Configurar username do titular e token exclusivamente no ambiente/secret já previsto;
   não gravar token em POM, settings versionado, comando no histórico ou relatório.
3. Verificar usuário autenticado e permissão de leitura dos pacotes; depois obter os POMs,
   listar versões e comparar conteúdo do BOM com parent e parameter-client.
4. Executar a resolução/verify com outro repository Maven vazio e strict-checksums.
   Guardar GAVs, SHA-256 e origem publicada; comparar com manifesto da release aprovada.
5. Só considerar versão imutável validada com uma referência confiável de release. Comparar
   um JAR de desenvolvimento do .m2 com um remoto não prova que o remoto foi adulterado.

Exemplo seguro, em Bash, depois de obter credencial autorizada (não executado com um
novo token nesta preparação):

```bash
export GITHUB_PACKAGES_USERNAME=seu_usuario
read -r -s -p 'Token de leitura de pacotes: ' GITHUB_PACKAGES_TOKEN
export GITHUB_PACKAGES_TOKEN
registry_repository=$(mktemp -d)
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress --strict-checksums -Dmaven.repo.local="$registry_repository" clean verify
unset GITHUB_PACKAGES_TOKEN
```

O comando usa a versão do POM vigente. A validação da correção publicada deve ocorrer
novamente após a adoção autorizada da release compatível; não reutilizar o .m2 local.

## Artefatos locais e preparação do consumidor

O dependency:list confirmou 15 JARs Golden em 1.0.0. Parent, BOM e BOM tecnológico
permanecem 1.0.0. A entrada local de platform-parameter-client existe no BOM usando
project.version; o modelo Maven resolveu-a efetivamente para 1.0.0 no escopo test.
Esses fatos não demonstram que o BOM remoto possua o mesmo conteúdo.

O POM agora usa platform.libraries.version=1.0.0 para o BOM; as versões explícitas de
platform-redis/platform-testing-redis foram removidas para que sigam o gerenciamento
comum. Não houve mudança da versão efetiva nem apontamento para artefato remoto novo.
O parent continua literal 1.0.0, pois sua resolução não deve depender de propriedade
arbitrária definida somente no filho.

As classes PlatformAuthorizationAutoConfiguration e AuthorizationTechnicalErrors do JAR
local consumido são byte a byte iguais às classes compiladas da fonte validada; o código
PLT-AUTH-005 está presente. O manifesto local registra hashes e origem local explicitamente.
A versão 1.0.0 local recompilada não deve ser publicada por cima da release existente.

Após a release aprovada: mudar parent para a versão publicada e platform.libraries.version
para o BOM correspondente, conferir transitivas/Enforcer e executar clean verify em
repository novo. Nenhuma mudança desse tipo foi aplicada antes da publicação.

## Release de bibliotecas

Proposta: **1.0.1**, patch de segurança para configuração inválida, sem alteração de API
Java ou semântica de AuthorizationRequired. Como revision é compartilhada, propõe-se
release coordenada dos 24 artefatos do reactor, incluindo parent/dependencies/BOM e
módulos sem mudança de código que referenciam versões internas coordenadas.

A matriz completa de 24 artefatos, dependências, compatibilidade, testes por módulo e
ordem Maven está na Golden em docs/libraries/release-preparation.md, estado draft local.
Os módulos não exercitados pelos 307 testes selecionados continuam sujeitos ao verify
completo do reactor na versão/commit candidatos, antes de qualquer publicação.

O workflow local de publicação agora exige uma versão nova, recusa 1.0.0/snapshots,
confere que a revisão aprovada já está registrada no fonte e bloqueia ausência de acesso,
POM/JAR existente (inclusive JAR sem POM) ou erro de registry. A configuração atual 1.0.0 impede publicar
1.0.1 até a aprovação e alteração explícita do fonte. Há serialização, repository de release novo sem cache, deployAtEnd e
manifesto de proveniência. Foram exercitados 12 cenários sintéticos da trava, sem
requisições remotas ou deploy; isso não certifica o workflow real no GitHub Actions.

A ordem de deploy é a do reactor. deployAtEnd evita publicar antes do sucesso dos
projetos, mas não torna o registry transacional. Uma publicação parcial exige parar e
decidir recuperação/nova versão; jamais apagar ou sobrescrever automaticamente.

## CI reproduzível

O workflow do consumidor conserva packages: read, usa settings versionados sem segredo,
não restaura cache Maven nem instala bibliotecas fonte e faz pré-validação do modelo
publicado antes de baixar containers. A ausência do secret bloqueia explicitamente.
O mesmo repository novo do job é usado no clean verify, com strict-checksums.
Mesmo após falha, o diagnóstico guarda POM consumidor, commit, toolchain e hashes dos
artefatos obtidos. A cobertura JaCoCo também fica nos artifacts do job.

YAML e Bash dos dois workflows foram verificados localmente. Nenhuma configuração
remota, workflow_dispatch, secret ou permissão foi alterada. O CI remoto **não está
homologado**, pois esses workflows estão no diff local e o registry ainda não foi lido
com credencial apropriada.

## Flyway: recomendação de homologação condicionada

A V1 continua com SHA-256 16b89bb75adfdc33d2f41583064096759ab7dace66fbfa3bff4a33d25d16a3c1.
O MySQL da fixture nasce vazio, Flyway cria o schema, Hibernate valida e o teste de
metadata verifica tabelas Core, views, índice único e FKs. A suíte HTTP completa
verifica operações sobre esse schema real. Não houve operação em banco persistente.

Recomendação técnica: submeter **primeira homologação restrita a instalações novas**,
se esse escopo for aprovado expressamente. A evidência local é adequada para examinar
esse caminho; não é homologação final automática. As condições e riscos estão em
[database-transition.md](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/database-transition.md).

A alternativa A+B exige inventário e backup representativo, comparação de checksums/
histórico, ensaio de transição e validação de dados. A cadeia Git V1–V87 disponível
não demonstra o estado de uma instalação. Não foi implementado upgrade hipotético.
Enquanto A ou A+B não for decidido, o histórico fica DEPENDENTE DE DECISÃO FUNCIONAL/
OPERACIONAL; não é considerado bloqueio automático para a alternativa A.

## Documentação e sequência dos gitlinks

| Documento legado no serviço | Destino canônico proposto | Tratamento local |
| --- | --- | --- |
| docs/testing/integration-test-coverage.md | Golden: feature-coverage.md e integration-tests.md | Aviso deprecated; corpo original preservado integralmente |
| docs/testing/integration-test-performance.md | Golden: integration-tests.md | Aviso deprecated; números históricos não certificam a revisão atual |
| docs/testing/service-context-integration-coverage.md | Golden: features/service-context.md e feature-coverage.md | Aviso deprecated; referências canônicas definidas |

Não foram apagados relatórios ou relaxadas as regras do validador para obter resultado
artificialmente verde. O validador examina os snapshots fixados nos submódulos; esses
três arquivos continuam neles e sua retirada depende da integração documental aprovada.
Preparou-se a consolidação dos contratos duráveis no guia central, sem transformar
medições históricas em contrato atual. A preservação dos corpos originais foi verificada
por hash/sufixo no workspace do consumidor.

Sequência segura, após autorização específica:

1. Aprovar versão, acesso de pacotes, escopo de banco e retirada dos três documentos
   legados depois de validar o conteúdo canônico. Não alterar gitlinks para ocultar erros.
2. Preparar/validar/publicar commit C_LIB com versão aprovada, patch e fixtures compatíveis.
3. Certificar a release no registry com metadados e hashes. Não adotar release parcial.
4. Atualizar o consumidor para a versão existente, validar remotamente e produzir C_SVC,
   incluindo a transição documental aprovada. O Git preserva os relatórios retirados.
5. Publicar C_LIB e C_SVC somente após revisão/autorização; verificar acessibilidade real
   dos SHAs e artefatos. Atualizar gitlinks e URLs na Golden para esses commits, nunca
   para HEAD local sujo ou apenas para o commit mais recente.
6. Reexecutar validador documental e testes do validador; só então submeter à reauditoria.

C_LIB e C_SVC são identificadores de commits futuros, não SHAs já certificados.

## Validação final desta consolidação

| Validação desta etapa | Resultado | Comparação com baseline confirmado |
| --- | --- | --- |
| Serviço: caffeinate -i mvn clean verify | **BUILD SUCCESS**, 59 Surefire + 769 Failsafe = **828**, zero falhas/erros/ignorados | Mesma quantidade; os 50 testes ArchUnit preservados |
| Duração integral | **17:13** | Antes: 29:05, afetados por suspensão do host; -11:52, sem alegar otimização de código |
| Linhas JaCoCo, execução limpa | **92,64%** — 6.654/7.183 | Igual ao baseline |
| Branches JaCoCo, execução limpa | **64,30%** — 1.104/1.717 | Igual ao baseline |
| Instruções JaCoCo | **90,30%** — 28.328/31.372 | Igual ao baseline |
| Bibliotecas/consumidores selecionados | **307** testes, zero falhas/erros/ignorados, **1:35** | Mesmos módulos e quantidade do baseline |
| Contextos Spring completos / containers do serviço | **2** / **1 MySQL + 1 Redis + 1 Ryuk auxiliar** | Mesmas quantidades instrumentadas anteriormente |
| Testes de configuração mínimos | Sete ApplicationContextRunner, sem banco | Já pertenciam aos 828 testes; nenhum contexto por cenário novo |
| Trava de publicação | **12 cenários sintéticos aprovados**, sem rede/deploy | Validação adicional do workflow; não integra as contagens JUnit |
| Sintaxe dos workflows | YAML/Bash/Python válidos | Nenhum dispatch remoto executado |
| Validador documental Golden | **3 erros preexistentes** de localização dos relatórios em submódulo | Nenhum erro adicional de links/gitlinks; a política não foi relaxada |
| Testes do validador | **32 aprovados** | Mesma suíte |
| Resolução remota com Maven isolado | **Bloqueada por 401 no parent** | Não se substituiu a prova remota por sucesso local |

A execução integral foi única nesta consolidação. caffeinate -i foi usado somente como
wrapper temporário para evitar suspensão por ociosidade durante o comando; nenhuma
configuração persistente de energia foi alterada. O startup principal foi 45,5 s com
46,4 s de uptime, sem a discrepância grande da execução anterior. O tempo não é um
benchmark controlado de performance; não houve mudança de código para acelerá-lo.

Comando direcionado das bibliotecas (verify, sem nova instalação ou publicação):

```sh
mvn -pl modules/platform-authorization,modules/platform-starter,modules/platform-testing/platform-testing-authorization -am verify
```

A extração da ordem do reactor usou apenas validate de inventário de modelo, com o
Enforcer ignorado para essa finalidade; não foi contabilizada como prova de build ou
como substituta dos 307/828 testes. A execução integral do serviço passou pelo Enforcer.

### Evidências preservadas

Em target/build-evidence/consolidation:

- account-clean-verify.log, final-build-results.json e clean-build-metadata.json;
- clean-verify-reports.zip, com os XMLs de teste e o HTML/CSV/XML JaCoCo novos;
- local-platform-artifacts.json: **18 POMs/JARs resolvidos**, todos em 1.0.0, com SHA-256;
- local-artifact-verification.json e local-dependencies.txt;
- registry-api.json e registry-resolution.json, contendo status/escopos públicos e o
  comando isolado, sem token ou conteúdo de secrets;
- libraries-verify.log, publication-guard-tests.json, release-artifacts.json;
- remote-workflow-history.json e historical-release-comparison.json;
- legacy-document-preservation.json, containers.json e resultados documentais.

Os artefatos de target são locais/gerados, não commits ou prova de publicação remota.
O snapshot da remediação anterior foi preservado fora de target antes do clean; seu
relatório não foi substituído por estas medições. O HTML atual está em
target/site/jacoco/index.html. Foram comparados os 40 arquivos Java que já tinham
alterações locais: todos permaneceram com os mesmos hashes desta etapa inicial.
A V1 também permaneceu byte a byte inalterada.


## Ações que ainda exigem aprovação ou infraestrutura

- Credencial de leitura de pacotes válida e acesso do consumidor no CI.
- Versão nova definitiva e seu inventário de coordenadas livres; 1.0.1 é proposta.
- Atualização autorizada de revision e validação de todo o reactor candidato.
- Commits, publicação e adoção da release; nenhuma escrita remota foi executada.
- Definição de escopo de banco A ou A+B; base representativa apenas se B for exigido.
- Retirada dos três documentos legados e integração dos commits/gitlinks homologados.

As correções locais de negócio permanecem prontas para avaliação do diff. A publicação
coordenada e o consumo remoto ainda não estão certificados. Veredito final:
**NOT READY FOR GOLDEN REAUDIT**. Isso não equivale a rejeição do Cenário A de banco novo
nem reabre as correções funcionais já validadas.

## Arquivos modificados nesta consolidação

A relação não repete os diffs Java anteriores, que permaneceram intactos.

### account-service

```text
.github/workflows/verify.yml
README.md
docs/golden/database-transition.md
docs/golden/golden-reaudit-remediation.md
docs/golden/release-consolidation.md
docs/testing/integration-test-coverage.md
docs/testing/integration-test-performance.md
docs/testing/service-context-integration-coverage.md
pom.xml
```

### platform-libraries

```text
.github/workflows/publish.yml
```

### golden-platform

```text
docs/libraries/README.md
docs/libraries/release-preparation.md
docs/services/account-service/data.md
docs/services/account-service/integration-tests.md
docs/services/account-service/reaudit-preparation.md
docs/technical-debt.md
```


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
