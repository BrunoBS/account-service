# Workspace Service

Implementação do domínio Workspace; repositório Git com nome histórico account-service.

A documentação humana vigente está na [Golden Platform](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/services/account-service/README.md).

## Desenvolvimento local

Java 25, Maven 3.9.9–3.x e Docker para testes de integração. Configure as credenciais de leitura do GitHub Packages no ambiente usado por `.github/maven-settings.xml` (`GITHUB_PACKAGES_USERNAME` e `GITHUB_PACKAGES_TOKEN`).

```sh
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Para execução local, configure MySQL com WORKSPACE_DB_URL/USERNAME/PASSWORD e Redis conforme application.yml. O perfil local usa autorização MOCK por padrão.

## Publicação de parâmetros

A publicação encapsula Redis em `ParameterPublicationStore`, implementado por
`RedisParameterPublicationStore`. Somente o adaptador injeta um store anotado
`@RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED)`.
O store padrão continua seguindo `platform.redis.*`; não configure seu namespace
global apenas para publicar parâmetros. A chave lógica permanece
`<microservice.code>.<key>` e o envelope `{type,value}`, persistente e sem TTL.

A publicação assíncrona envia snapshots confirmados em lotes; remoções ocorrem
após commit. Redis e MySQL não participam de uma transação distribuída. A
reconciliação restaura os snapshots publicados após falhas de envio. Os testes
`ParameterPublicationInteropIT`, `ParameterKeyRedisIT`, `ParameterKeyPublicationIT` e
`ParameterKeyReconciliationIT` verificam integração, isolamento e lifecycle.
A dependência de teste `platform-parameter-client` requer a mesma revisão V2 das
bibliotecas; teste em reactor local não comprova disponibilidade no registry.
