# Workspace Service

Implementação do domínio Workspace; repositório Git com nome histórico account-service.

A documentação humana vigente está na [Golden Platform](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/services/account-service/README.md).

## Desenvolvimento local

Java 25, Maven 3.9.9–3.x e Docker para testes de integração. Configure as credenciais de leitura do GitHub Packages no ambiente usado por `.github/maven-settings.xml` (`GITHUB_PACKAGES_USERNAME` e `GITHUB_PACKAGES_TOKEN`).

```sh
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Para execução local, configure MySQL com WORKSPACE_DB_URL/USERNAME/PASSWORD e Redis conforme application.yml. O perfil local usa autorização MOCK por padrão.

[Snapshot anterior e proveniência](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/archive/account-service/README.md).
