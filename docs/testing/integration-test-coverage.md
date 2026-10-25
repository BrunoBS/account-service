# Integration Test Coverage

## Result

This report was generated from the account-service checkout on 2026-10-25. JaCoCo measured production line coverage using a dedicated execution file produced while running only the integration-test selectors. The figures below are therefore integration-only; they do not combine unit or architecture-test execution data.

| Camada | Módulo | Testes de integração | Cobertura (%) |
|---|---|---:|---:|
| Foundation | Catalog | 270 | 75.00% (216/288 linhas) |
| Foundation | Schema | 55 | 94.58% (506/535 linhas) |
| Foundation | Audit | 5* | 100.00% (1/1 linha) |
| Core | Workspace | 102 | 96.00% (432/450 linhas) |
| Core | Application | 36 | 84.89% (309/364 linhas) |
| Core | Environment | 55 | 86.49% (826/955 linhas) |
| Core | Workspace Configuration | 19 | 95.82% (298/311 linhas) |
| Core | Application Configuration | 19 | 96.19% (328/341 linhas) |
| **TOTAL** | **Média geral ponderada do projeto** | **615** | **70.42% (5,051/7,173 linhas)** |

The total is weighted by executable production lines across the complete JaCoCo report, including feature and other production packages outside the modules listed above. It is not the simple average of module percentages. Test-case counts identify executed cases, not coverage percentages.

`*` Audit has five explicitly named integration cases that assert audit behavior in Workspace create, update, inactivate, delete, and restore flows. They are included in the Workspace count and are not additional unique test cases. JaCoCo reports only one executable line in the `foundation.audit` production packages (the entity); its 100% is a very small denominator and does not establish broad audit-path coverage. The repository package has no executable lines in JaCoCo. Audit is applicable and has integration assertions, so it is not marked N/A.

## Test execution evidence

Full validation, including unit, architecture, and integration tests:

```shell
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Result: `BUILD SUCCESS`, 11m18s; Surefire ran 812 tests and Failsafe ran 70 tests. Both reports show zero failures, zero errors, and zero skipped tests (882 total).

Integration-only JaCoCo measurement:

```shell
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify '-Dtest=*IntegrationTest' '-Dit.test=*IT' -Djacoco.destFile=target/jacoco-integration.exec -Djacoco.dataFile=target/jacoco-integration.exec -Djacoco.outputDirectory=target/site/jacoco-integration
```

Result: `BUILD SUCCESS`, 9m20s; Surefire ran 545 cases and Failsafe ran 70 cases, with zero failures, errors, or skips (615 total). The JaCoCo agent wrote `target/jacoco-integration.exec`; `target/site/jacoco/jacoco.xml` was generated from this isolated run. No unit or architecture selectors were included in this measurement. The report XML is a build artifact and is not committed; rerun the command to regenerate it.

The integration-only run exercised MySQL through Testcontainers. Test reports show no skipped cases. There are no `@Disabled` or `@Ignore` annotations in the test sources.

## Coverage review and gaps

- Lowest measured module coverage is Foundation Catalog at 75.00% (72 missed of 288 executable lines).
- Core Application is next lowest at 84.89% (55 missed of 364 lines), followed by Core Environment at 86.49% (129 missed of 955 lines).
- Foundation Schema and both configuration modules exceed 94%; Workspace is at 96.00%.
- The project-wide 70.42% includes all production packages. It is lower than the listed core/foundation modules because the feature and remaining packages also contribute executable lines; many are covered by non-HTTP `*IT` cases but are not represented as rows in this module table.
- Audit's 1-line denominator makes its percentage unsuitable as a standalone quality signal. The five explicit audit assertions validate Workspace audit emission and snapshots, while JaCoCo line coverage for generated entity accessors does not measure the completeness of audit semantics.

## Pipeline configuration

`.github/workflows/verify.yml` runs on pushes to `main` and feature branches and on pull requests targeting `main`. It installs the required local Platform Libraries artifacts, uses Java 25, and executes `mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify` with tests enabled. It uploads Surefire and Failsafe reports. The local `clean verify` result matches the configured validation command. GitHub Actions status for the commit is reported separately after push; local success alone is not pipeline evidence.
