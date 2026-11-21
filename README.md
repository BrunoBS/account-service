# Workspace Service

Implementação do domínio Workspace; repositório Git com nome histórico account-service.

A documentação humana vigente está na [Golden Platform](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/services/account-service/README.md).

## Desenvolvimento local

Java 25, Maven 3.9.9–3.x e Docker para testes de integração. Configure as credenciais de leitura do GitHub Packages no ambiente usado por `.github/maven-settings.xml` (`GITHUB_PACKAGES_USERNAME` e `GITHUB_PACKAGES_TOKEN`).

```sh
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Para execução local, configure MySQL com WORKSPACE_DB_URL/USERNAME/PASSWORD e Redis conforme application.yml. O perfil local usa autorização MOCK por padrão. A auditoria exige
`AUDIT_STORAGE_BUCKET` e usa AWS na região `AUDIT_STORAGE_REGION` (padrão
`us-east-1`), com as credenciais da cadeia padrão do SDK. Os testes HTTP
substituem o envio externo e verificam os registros confirmados no outbox.

Para usar S3 local via LocalStack, suba LocalStack e Redis e execute com o perfil `local`
(o MySQL deve estar disponível conforme a configuração acima):

```sh
docker compose -f compose.local.yml up -d --wait
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_EC2_METADATA_DISABLED=true \
  SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

O script de inicialização cria o bucket `workspace-audit-local` se ele não existir.
Também habilita o versionamento para preservar snapshots sucessivos do mesmo recurso.
O perfil `local` usa o endpoint `http://127.0.0.1:4566` e esse bucket por padrão;
`AUDIT_STORAGE_ENDPOINT` e `AUDIT_STORAGE_BUCKET` permitem substituí-los. Na IDE,
configure as mesmas variáveis de ambiente do comando acima. Os dados do LocalStack
ficam em um volume Docker entre reinicializações do container.

O LocalStack também cria a fila SQS `workspace-audit-events` e configura o bucket
para publicar eventos `s3:ObjectCreated:*` nela. A mensagem informa o bucket e a
chave do objeto recebido. Para listar filas e consultar mensagens pelo terminal:

```sh
docker compose -f compose.local.yml exec localstack awslocal sqs list-queues
docker compose -f compose.local.yml exec localstack awslocal sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/workspace-audit-events \
  --max-number-of-messages 10 --wait-time-seconds 5
```

## Testes HTTP

Os testes em `src/test/java` ficam separados por tipo: `architecture` para
regras ArchUnit, `unit` para testes sem infraestrutura externa e `integration`
para testes com Spring, HTTP, banco ou Redis. Dentro de `unit` e `integration`,
os pacotes acompanham os domínios reais `foundation`, `core` e `feature`; os
clients, responses, builders, factories e scenarios de integração ficam em
`support` do domínio correspondente. O suporte transversal fica em
`integration/support`.

Os testes de controller usam `@PlatformIntegrationTest` para subir o serviço em
porta aleatória. `@WithMockAuthorization` usa WireMock para simular o provedor de
autorização; cenários de acesso permitido, negado, escopo e visibilidade verificam
o comportamento do Workspace Service diante dessa simulação, não a integração
com o provedor real.

Os cenários de Workspace, Environment Configuration e publicação de parâmetros usam clients
de teste por recurso, responses tipadas, factories de payload e cenários de
preparo. Payloads inválidos permanecem valores brutos, para verificar a
validação HTTP sem conversão ou correção silenciosa. Versões usadas em updates
são sempre fornecidas pelos testes após uma leitura explícita.

## Publicação de parâmetros

A publicação encapsula Redis em `ParameterPublicationStore`, implementado por
`RedisParameterPublicationStore`. O adaptador obtém uma view com
`RedisStore.withNamespace("dynamic-parameter")`. Essa view usa o namespace
compartilhado de publicação; o store padrão mantém o nome da aplicação. A chave lógica permanece
`<microservice.code>.<key>` e o envelope `{type,value}`, persistente e sem TTL.

A publicação assíncrona envia snapshots confirmados em lotes; remoções ocorrem
após commit. Redis e MySQL não participam de uma transação distribuída. A
reconciliação restaura os snapshots publicados após falhas de envio. Os testes
`ParameterPublicationInteropIT`, `ParameterKeyRedisIT`, `ParameterKeyPublicationIT` e
`ParameterKeyReconciliationIT` verificam integração, isolamento e lifecycle.
A dependência de teste `platform-parameter-client` requer a mesma revisão V2 das
bibliotecas; teste em reactor local não comprova disponibilidade no registry.

## Configuração operacional e preparação da reauditoria

O serviço inicia com autorização REAL e exige `AUTHORIZATION_SERVICE_URL`.
Para desenvolvimento com mock, ative explicitamente `SPRING_PROFILES_ACTIVE=local`;
o grupo padrão local é GUEST. Configure `LOCAL_AUTHORIZATION_GROUPS` deliberadamente
quando precisar exercer operações administrativas. Nunca ative `local` em produção.

Os probes ficam em `/actuator/health/liveness` e `/actuator/health/readiness`.
Somente status é exposto; detalhes de banco/Redis e endpoints de configuração não
são publicados. Liveness considera o estado da aplicação. Readiness inclui MySQL e
Redis; falha de dependência retira tráfego sem pedir reinício do processo.
Restrinja o acesso aos probes pela rede de operação.

Consulte o [relatório de correções e pendências](docs/golden/golden-reaudit-remediation.md),
o [plano de instalação e migração](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/database-transition.md) e a
[configuração por ambiente](https://github.com/BrunoBS/golden-platform/blob/main/docs/services/account-service/operations.md).

A etapa atual está em [consolidação da release, registry e gitlinks](docs/golden/release-consolidation.md).
