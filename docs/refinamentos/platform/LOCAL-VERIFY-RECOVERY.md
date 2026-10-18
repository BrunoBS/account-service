> Documento histórico. Consulte [Service Context](../service-context/SERVICE-CONTEXT.md) para nomes e contratos atuais.

# Recuperação da verificação local

Data: 14/10/2026, após o merge local da revisão na main.

O usuário relatou falha no `mvn verify`. A validação anterior em lotes não
substitui a execução desse comando completo; a reprodução e sua correção são
registradas neste documento.

## Histórico de migrations identificado durante o diagnóstico

A compilação limpa de produção/testes passou. A consulta somente de leitura ao
banco de desenvolvimento identificou 44 migrations aplicadas e 35 divergências
de checksum entre esse histórico e os arquivos atuais.

O commit `32ac504f` havia reformatado essas 35 migrations. A revisão anterior
comparou os arquivos com HEAD, que já continha essa reformatação, e portanto
usou uma referência inadequada para afirmar a preservação do histórico aplicado.
Testes de banco limpo e upgrade que usam apenas os arquivos atuais não detectam
essa divergência com os checksums de uma instalação anterior.

Os arquivos anteriores a `32ac504f`, também disponíveis no backup da revisão,
foram comparados ao banco com o calculador de checksum do Flyway: os 44 arquivos
aplicados correspondem integralmente ao histórico. A comparação dos tokens SQL
confirmou que a restauração dos 35 arquivos altera somente a formatação.

A correção recupera esses arquivos históricos. Não executa repair, não altera
flyway_schema_history e não desabilita a validação na aplicação. As migrations
V45–V65 continuam pendentes no banco local diagnosticado; não foram aplicadas
durante essa análise. Um backup desse banco foi salvo em
`/private/tmp/workspace-db-before-local-start-20261014.sql`.

`PublishedMigrationChecksumTest` compara os arquivos do classpath com a fixture
dos 44 checksums aplicados, protegendo o histórico contra futuras reformatações.
A fixture foi obtida do histórico existente, não gerada a partir da implementação
que o teste verifica.

## Verificações

- `mvn --offline --batch-mode --no-transfer-progress clean test-compile`: aprovado.
- `mvn --offline --batch-mode --no-transfer-progress test`: 108 testes aprovados.
- `mvn --offline --batch-mode --no-transfer-progress -Dit.test=DatabaseMigrationIT,DatabaseUpgradeMigrationIT,EnvironmentTypeCodeMigrationIT verify`:
  108 unitários/arquitetura e três integrações aprovados; banco limpo, upgrade com
  dados e alteração da identidade de tipos de ambiente.
- Comparação somente de leitura com o banco local: 44 checksums, zero divergências
  no conjunto histórico recuperado.

## Falha reproduzida no verify completo

A execução única de `clean verify` chegou a 41 classes de integração concluídas,
incluindo os dez cenários de onboarding, e bloqueou ao iniciar outro container
MySQL em PlatformCodeMigrationIT. O container registrou `io_setup() failed with
EAGAIN` e `Cannot initialize AIO sub-system`.

O Docker mantinha 24 containers MySQL de testes abertos pelo cache de contextos
Spring. A leitura de `/proc/sys/fs/aio-nr` indicou 63.769 slots ocupados, para um
limite de 65.536 em `/proc/sys/fs/aio-max-nr`. Essa condição explica por que os
lotes anteriores passavam e o comando completo não terminava.

`src/test/resources/spring.properties` limita o cache de contextos a oito. Ao
evictar um contexto, Spring fecha seus beans e libera o container correspondente.
A configuração pertence somente ao classpath de testes e vale também para a
IDE. A suíte mantém todas as classes, o mesmo processo e os cenários concorrentes.

A leitura do DefaultContextCache no classpath de testes confirmou `maxSize = 8`.
Durante a nova execução, foram observados oito MySQL de teste ativos e 22.793
slots AIO ocupados, abaixo dos 65.536 disponíveis.

A reprodução foi interrompida após confirmar a causa. Seus containers foram
liberados; o banco mysql-local foi preservado. Não houve alteração no limite de
AIO do Docker nem nos parâmetros de MySQL da aplicação.

A validação final usa o comando completo, sem modo offline nem seleção de
classes:

```text
mvn --batch-mode --no-transfer-progress clean verify
```

Log da reprodução: `/private/tmp/account-service-full-verify-reproduce.log`.
Log da verificação final: `/private/tmp/account-service-full-verify-fixed.log`.

Resultado final: **BUILD SUCCESS**, com **108 testes unitários/arquitetura** e
**340 testes de integração em 57 classes**, zero falhas, erros ou skips.
Onboarding passou na mesma execução das demais classes. Não houve separação em
lotes, isolamento de classes ou desativação de cenários para obter esse resultado.
O consolidado está em `/private/tmp/account-service-full-verify-fixed-results.json`.
