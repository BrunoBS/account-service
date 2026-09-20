# account-service

Serviço de referência para o domínio de Account da Golden Platform.

Este repositório implementa a fase `GOLDEN-REFERENCE-V1` sobre o checkpoint `FOUNDATION-GOLDEN-V1`.

## Baseline

- Java 25
- Spring Boot 4.1.1
- Maven >= 3.9.9
- `com.empresa.platform:platform-parent:1.0.1`
- `com.empresa.platform:platform-starter:1.0.0`

## Princípios

- Foundation fornece capacidades; este serviço demonstra padrões de aplicação.
- `platform-crud` não é permitido.
- regras de Account permanecem explícitas no serviço.
- capabilities opcionais da Foundation só entram com caso de uso real.
- `account-api` é referência funcional do legado, não base estrutural desta aplicação.

## Verificação

Com credenciais de leitura do GitHub Packages:

```bash
export GITHUB_PACKAGES_USERNAME=BrunoBS
export GITHUB_PACKAGES_TOKEN=<token>
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

A documentação da Golden está em [`docs/golden`](docs/golden).
