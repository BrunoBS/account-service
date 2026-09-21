# workspace-service

Serviço de referência para o domínio de Workspace da Golden Platform.

Este repositório implementa a fase `GOLDEN-REFERENCE-V1` sobre o checkpoint `FOUNDATION-GOLDEN-V1`.

## Identidade

- Maven: `br.com.portalmanager:workspace-service:0.1.0-SNAPSHOT`
- Java package root: `br.com.itau.portalmanager.workspace`
- hospedagem atual: `BrunoBS/account-service` (nome histórico do repositório; não representa o domínio ativo)

## Baseline

- Java 25
- Spring Boot 4.1.1
- Maven >= 3.9.9
- `br.com.portalmanager.platform:platform-parent:1.0.0`
- `br.com.portalmanager.platform:platform-libraries-bom:1.0.0`
- `br.com.portalmanager.platform:platform-starter:1.0.0`
- `br.com.portalmanager.platform:platform-testing:1.0.0`

## Arquitetura

A aplicação utiliza as macrozonas:

```text
br.com.itau.portalmanager.workspace
├── foundation
├── core
├── feature
└── input
```

Pacotes vazios não são criados apenas para completar a árvore.

Princípios principais:

- Foundation fornece capacidades; este serviço demonstra padrões de aplicação.
- `platform-crud` não é permitido.
- regras de Workspace permanecem explícitas no serviço.
- comunicação interna entre módulos ocorre por Use Cases públicos.
- Request/Response pertencem à Web; Input/Output pertencem aos Use Cases.
- Repository representa persistência do próprio módulo.
- Integration representa fronteira externa ao módulo.
- capabilities opcionais da Foundation só entram com caso de uso real.
- `account-api` é referência funcional histórica, não base estrutural desta aplicação.

## Foundation remota

O consumidor usa exclusivamente:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

Com credenciais de leitura:

```bash
export GITHUB_PACKAGES_USERNAME=BrunoBS
export GITHUB_PACKAGES_TOKEN=<token>
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Não é permitido usar checkout ou `mvn install` local da Foundation como evidência de integração.

A documentação da Golden está em [`docs/golden`](docs/golden).
