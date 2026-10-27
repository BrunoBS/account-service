# Performance e compartilhamento do MySQL nos testes

## Resultado

A suíte de integração do `account-service` agora executa em um único fork Failsafe e usa um único container MySQL por JVM. O container pertence à JVM de testes e não ao `ApplicationContext`: contextos Spring distintos reutilizam a mesma instância e as mesmas informações de conexão. Cada contexto pode manter seu próprio pool de conexões JDBC. O container é encerrado pelo hook de shutdown da JVM; `withReuse(true)` não é usado.

Foram observados dois contextos Spring na suíte completa e dois processos de teste no `clean verify` (Surefire para arquitetura e Failsafe para integração). Somente o fork Failsafe inicia MySQL; portanto há exatamente um container MySQL durante a suíte de integração. Flyway é iniciado pelo Spring para cada contexto que o exige; no banco compartilhado a migração V1 é aplicada uma vez, e o próximo contexto encontra o schema já atualizado.

## Causa anterior

Antes desta mudança, `MySqlTestConfiguration` criava um `MySQLContainer` como bean Spring com `destroyMethod="stop"`. Assim, o container acompanhava o contexto: contextos com e sem Redis recebiam containers separados. Além disso, Surefire e Failsafe executavam em JVMs diferentes, e um singleton Java não poderia compartilhar estado entre elas. A configuração anterior — após o ajuste do cache Spring para 2 — gerava quatro containers reais: dois contextos em cada uma das duas JVMs. Uma medição histórica anterior ao ajuste de cache registrou cinco containers e 13m14s; ela não é a baseline usada para a comparação principal abaixo.

## Implementação

- `platform-testing-database` agora fornece um gerenciador thread-safe, estático por JVM, que cria e inicia uma única instância. Requisições concorrentes recebem o mesmo objeto. Uma imagem diferente ou parada prematura falha explicitamente.
- `MySqlTestConfiguration` obtém essa instância compartilhada e desativa o encerramento automático do bean pelo Spring. O hook da JVM registra e encerra o container ao final do processo.
- O POM do serviço encaminha todas as classes `*IntegrationTest` e `*IT` para um único fork Failsafe (`forkCount=1`, `reuseForks=true`). Surefire exclui `*IntegrationTest` e mantém os testes arquiteturais. Isso evita tentar compartilhar um singleton entre processos distintos.
- `.github/workflows/verify.yml` fixa o checkout usado pela pipeline no commit `217fee1cbe9049600e108560579e2f8baf9500e5` da Platform Libraries e instala esses módulos antes de validar o serviço.
- `spring.test.context.cache.maxSize=2` foi preservado, permitindo reter os contextos sem Redis e com Redis. O paralelismo JUnit permanece desativado porque a extensão de banco limpa tabelas compartilhadas.
- As extensões oficiais continuam responsáveis pela limpeza antes/depois de cada teste; não foi adicionado rollback transacional genérico. Flyway permanece habilitado.

## Evidências e medições

Os IDs abaixo foram lidos dos eventos reais de inicialização do Testcontainers. Logs JSON/console duplicados foram deduplicados pelo ID. Cada execução listada teve um único ID MySQL no fork Failsafe, e inspeção posterior confirmou que o container foi removido ao terminar a JVM.

| Execução após a mudança | Resultado | Casos | Falhas / erros / skips | Container MySQL |
|---|---|---:|---:|---|
| Smoke de seletores Failsafe | BUILD SUCCESS, 1m05s | 9 | 0 / 0 / 0 | `76f2be41cbb3587240916dda53c5fb65cc693d14e1f3d7ef232d6b5fff62b23b` |
| `clean verify` rodada 1 | BUILD SUCCESS, 10m41s | 832 | 0 / 0 / 0 | `f3b772fb1cb5e11f88407af212c5d1cc9511a63a5cc2d08899eb67e7871055f3` |
| `clean verify` rodada 2 | BUILD SUCCESS, 10m30s | 832 | 0 / 0 / 0 | `5409503e00ffb99e6fc3e405da229c8556e946642343ca36cea1e3035f2fe617` |
| Revalidação sobre `origin/main` atualizado | BUILD SUCCESS, 8m27s | 947 (269 Surefire + 678 Failsafe) | 0 / 0 / 0 | `8e960f6d93f52eeb0ede8bffb9883d3ba417ce409fe2aa61ad87c1625dda199d` |
| Integração + JaCoCo rodada 1 | BUILD SUCCESS, 13m13s | 782 | 0 / 0 / 0 | `8d6509f3d3c01783f5773c93dacafc69a32017808c6bdc984991d36b6fabebb5` |
| Integração + JaCoCo tentativa intermediária | BUILD SUCCESS, 11m57s | 771 | 0 / 0 / 0 | Não aceita para comparação: três classes estavam ausentes da árvore naquele momento |
| Integração + JaCoCo rodada 2 corrigida | BUILD SUCCESS, 13m00s | 782 | 0 / 0 / 0 | `3f5aaffd570b0be2d27b253ffc756d7efea91a376c68a41c79a6d928c440e2e9` |
| Integração + JaCoCo em `origin/main` atualizado | BUILD SUCCESS, 9m17s | 678 | 0 / 0 / 0 | `5e954979811fd075009add8bf8a1d05697dd37cc2d1a6c2d83abc776fb574ff5` |

As duas execuções completas observaram dois contextos Spring no mesmo PID Failsafe, uma criação de `flyway_schema_history` e uma aplicação da migração V1. Na primeira: cache final 9.830 hits / 2 misses, inicialização do container 4,670 s e inicialização acumulada dos contextos 28,747 s. Na segunda: cache 9.830 / 2, container 4,729 s e contextos 19,667 s. A rodada isolada corrigida também reportou 9.830 hits / 2 misses, criou um histórico Flyway e aplicou uma migração V1. Os tempos de contexto incluem componentes de inicialização e não isolam o custo do Spring.

| Métrica | Baseline observado antes | Rodada após 1 | Rodada após 2 |
|---|---:|---:|---:|
| `clean verify` | 11m50s | 10m41s | 10m30s |
| JVMs de teste no `clean verify` | 2, ambas iniciavam MySQL | 2; só Failsafe inicia MySQL | 2; só Failsafe inicia MySQL |
| Containers MySQL em integração | 4 | **1** | **1** |
| Contextos Spring na suíte | 4 | 2 | 2 |
| Criação do histórico Flyway / migrações aplicadas | 4 / 4 | 1 / 1 | 1 / 1 |
| Casos do `clean verify` | 832 | 832 | 832 |
| Cobertura de linhas / branches | 89,98% / 61,50% | preservada | preservada |

Os 832 casos das rodadas históricas são 50 arquiteturais em Surefire e 782 de integração em Failsafe (712 `*IntegrationTest` e 70 `*IT`). Os relatórios dessas duas rodadas completas registraram zero testes ignorados. Nenhum teste unitário foi criado nesta tarefa. No comparativo principal, a redução do tempo total foi de 1m09s (9,7%) e 1m20s (11,3%). Depois dessas rodadas, o `origin/main` avançou cinco commits que removem testes legados e contêm mais testes Surefire; a revalidação atual executou 947 casos. Seu tempo e total não são comparados diretamente aos 832 casos históricos.

### Cobertura isolada JaCoCo

A execução isolada elimina `target/jacoco-integration.exec` antes de rodar somente os seletores de integração. Nas rodadas válidas do checkout anterior, os relatórios mediram 6.454/7.173 linhas (**89,98%**) e 1.056/1.717 branches (**61,50%**). Na validação isolada da base atualizada, 678 testes Failsafe passaram; o relatório JaCoCo mediu 6.413/7.173 linhas (**89,40%**) e 1.037/1.717 branches (**60,40%**). São reduções de 41 linhas cobertas (0,58 ponto percentual) e 19 branches cobertos (1,10 p.p.). A base atualizada removeu 17 classes de integração legadas em cinco commits já presentes no `origin/main`; esse é um delta de cobertura da suíte atual, não uma alteração de código de produção desta tarefa. Uma tentativa intermediária da medição anterior reportou 771 casos porque três classes `*IT` estavam ausentes da árvore de trabalho. As fontes foram restauradas para aquela medição corrigida e removidas novamente a pedido do usuário.

Na rodada atual, o Testcontainers iniciou uma única instância MySQL com ID `5e954979811fd075009add8bf8a1d05697dd37cc2d1a6c2d83abc776fb574ff5`; houve dois contextos Spring (`missCount=2`), uma criação de `flyway_schema_history` e uma aplicação de V1. O container desta execução não estava mais presente em `docker ps -a` após o término. Nenhum container foi parado manualmente.

## Comandos executados

```bash
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress -pl modules/platform-testing/platform-testing-database -am verify
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress -pl modules/platform-testing/platform-testing-database -am install
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress -Pintegration-coverage '-Dit.test=WorkspaceFindAllIntegrationTest,ParameterKeyPublishIntegrationTest,ParameterKeyDeleteIntegrationTest' verify
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress -Pintegration-coverage '-Dit.test=*IntegrationTest,*IT' -Djacoco.destFile=target/jacoco-integration.exec -Djacoco.dataFile=target/jacoco-integration.exec -Djacoco.outputDirectory=target/site/jacoco-integration verify
```

O comando isolado foi repetido uma terceira vez após restaurar as três fontes de teste na árvore de trabalho. As duas rodadas válidas executaram 782 casos; a tentativa intermediária de 771 não é usada como evidência de cobertura.

Após atualizar para o `origin/main` atual, a medição isolada foi executada novamente com o mesmo seletor e com um arquivo JaCoCo limpo.

```bash
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress -Pintegration-coverage '-Dit.test=*IntegrationTest,*IT' -Djacoco.destFile=target/jacoco-integration.exec -Djacoco.dataFile=target/jacoco-integration.exec -Djacoco.outputDirectory=target/site/jacoco-integration verify
```

O `verify` e o `install` locais da library passaram. Os dois `clean verify` executaram 832 testes cada. As duas medições isoladas válidas executaram 782 casos cada; a tentativa intermediária sem três fontes rodou 771, não falhou testes e não é considerada resultado de cobertura.

Sobre o `origin/main` atualizado, `clean verify` executou 947 testes (269 Surefire e 678 Failsafe), e a medição JaCoCo isolada executou os 678 testes Failsafe. Ambas passaram sem falhas, erros ou skips.

## Arquivos de configuração envolvidos

- `platform-libraries/modules/platform-testing/platform-testing-database`: gerenciador por JVM, bean Spring sem destroy automático e contrato de compartilhamento coberto por testes.
- `account-service/pom.xml`: separação de descoberta Surefire/Failsafe, um fork reutilizado para integração e seletores JaCoCo exclusivos.
- `account-service/src/test/resources/spring.properties`: cache Spring máximo 2 e execução serial de testes.

Não houve alteração de produção, contratos HTTP, versões Maven, exclusões JaCoCo, testes removidos/ignorados, commit, push ou merge.
